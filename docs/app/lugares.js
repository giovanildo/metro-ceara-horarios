// Lugares do regras.json ("Rodoviária", "HGF"): a busca da tela "Sua viagem" e o
// "Perto desta estação" — as mesmas regras do app (data/Landmarks.kt).
import { distancia } from "./dados.js";

const LONGE = 2000; // além disso a pé, o app diz que o metrô não é a melhor opção
const AO_LADO = 150;
const PERTO = 1000; // em "Perto desta estação", só caminhada curta
const METROS_POR_MINUTO = 75;

const EMOJI = {
  onibus: "🚌", aviao: "✈️", hospital: "🏥", universidade: "🎓", escola: "🏫", shopping: "🛍️", mercado: "🧺",
  teatro: "🎭", museu: "🏛️", cultura: "🎨", igreja: "⛪", praca: "🌳", parque: "🌳", praia: "🏖️", estadio: "🏟️",
  governo: "🏢", justica: "⚖️",
};

const semAcento = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
const semSufixo = (nome) => nome.replace(/\s+-\s+(AE|NE)$/i, "");

export const emojiLugar = (l) => EMOJI[l.tipo] || "📍";
export const longe = (l) => l.a_pe_m > LONGE;
export const aoLado = (l) => l.a_pe_m <= AO_LADO;

/** Os lugares do regras.json, só os completos (com posição e estação). */
export function lugares(regras) {
  return (regras?.lugares?.lista || []).filter((l) => l.nome && l.posicao?.length === 2 && l.estacao && l.a_pe_m != null);
}

/** Minutos a pé até o lugar, no passo da pessoa ([fator] de "Como você se desloca"). */
export function minutosAPe(l, fator = 1) {
  return Math.max(1, Math.ceil(l.a_pe_m / METROS_POR_MINUTO * fator - 1e-9));
}

/** "300 m", "1,1 km". */
export function metrosAPe(m) {
  return m < 1000 ? `${Math.round(m / 10) * 10} m` : `${(m / 1000).toFixed(1).replace(".", ",")} km`;
}

/** "300 m · 4 min a pé" ou "ao lado da estação". */
export function caminhada(l, fator) {
  return aoLado(l) ? "ao lado da estação" : `${metrosAPe(l.a_pe_m)} · ${minutosAPe(l, fator)} min a pé`;
}

/** Os lugares que batem com a busca (sem acento, palavras em qualquer ordem); os que começam por ela primeiro. */
export function buscarLugares(regras, busca, limite = 8) {
  const agulha = semAcento(busca);
  if (agulha.length < 2) return [];
  const palavras = agulha.split(/\s+/);
  return lugares(regras)
    .filter((l) => { const t = semAcento(`${l.nome} ${l.busca || ""}`); return palavras.every((p) => t.includes(p)); })
    .sort((a, b) => (semAcento(b.nome).startsWith(agulha) - semAcento(a.nome).startsWith(agulha))
      || (longe(a) - longe(b)) || a.nome.localeCompare(b.nome, "pt-BR"))
    .slice(0, limite);
}

/** A estação (nome e posição) serve o lugar? Mesmo nome e perto dele, para não confundir cidades. */
export function serve(l, nomeEstacao, lat, lon) {
  return semAcento(semSufixo(nomeEstacao)) === semAcento(l.estacao)
    && distancia(lat, lon, l.posicao[0], l.posicao[1]) <= Math.max(3000, l.a_pe_m + 1000);
}

/** Os lugares a uma caminhada curta da estação, do mais perto ao mais longe. */
export function pertoDaEstacao(regras, nomeEstacao, lat, lon) {
  return lugares(regras).filter((l) => l.a_pe_m <= PERTO && serve(l, nomeEstacao, lat, lon))
    .sort((a, b) => a.a_pe_m - b.a_pe_m);
}
