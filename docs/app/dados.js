// Horários, estações e relógio. Os dados vêm prontos do ramo "dados" do
// repositório (gerados todo dia por tools/build_web_data.py): o navegador não
// consegue baixar o GTFS direto do Metrofor.

export const BASE = "https://raw.githubusercontent.com/giovanildo/metro-fortaleza-horarios/dados/";
const DIA = 24 * 60;

// ---------- relógio (sempre no fuso de Fortaleza, seja qual for o do celular)

const partes = new Intl.DateTimeFormat("en-CA", {
  timeZone: "America/Fortaleza", year: "numeric", month: "2-digit", day: "2-digit",
  hour: "2-digit", minute: "2-digit", weekday: "short", hourCycle: "h23",
});

/** { data: "AAAA-MM-DD", minutos desde a meia-noite, diaSemana: 0 = domingo } */
export function agora(date = new Date()) {
  const p = Object.fromEntries(partes.formatToParts(date).map((x) => [x.type, x.value]));
  const semana = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].indexOf(p.weekday);
  return { data: `${p.year}-${p.month}-${p.day}`, minutos: +p.hour * 60 + +p.minute, diaSemana: semana };
}

export function tipoDoDia(diaSemana) {
  return diaSemana === 6 ? "sabado" : diaSemana === 0 ? "domingo" : "dia-util";
}

export function somarDias(data, n) {
  const d = new Date(data + "T12:00:00Z");
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
}

export function hora(min) {
  const m = ((min % DIA) + DIA) % DIA;
  return `${String(Math.floor(m / 60)).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}`;
}

export function falta(min) {
  if (min <= 0) return "agora";
  if (min < 60) return `em ${min} min`;
  return `em ${Math.floor(min / 60)}h${String(min % 60).padStart(2, "0")}`;
}

export function dataCurta(data) {
  const [, m, d] = data.split("-");
  return `${d}/${m}`;
}

// ---------- download (com cópia guardada: sem internet, usa a última)

async function json(nome) {
  const chave = "dados:" + nome;
  try {
    const r = await fetch(BASE + nome, { cache: "no-cache" });
    if (!r.ok) throw new Error(r.status);
    const texto = await r.text();
    try { localStorage.setItem(chave, texto); } catch { /* sem espaço: segue */ }
    return JSON.parse(texto);
  } catch (erro) {
    const guardado = localStorage.getItem(chave);
    if (guardado) return JSON.parse(guardado);
    throw erro;
  }
}

/** A grade de um tipo de dia (ou a de dia útil, se ainda não houver). */
async function carregarGrade(indice, tipo) {
  const info = indice.tipos[tipo] || indice.tipos["dia-util"];
  if (!info) return null;
  const g = await json(info.arquivo);
  return new Grade(g);
}

export async function carregar() {
  const indice = await json("indice.json");
  const hoje = agora();
  const tipo = tipoDoDia(hoje.diaSemana);
  const [grade, regras, bike] = await Promise.all([
    carregarGrade(indice, tipo),
    json("regras.json").catch(() => null),
    json("bicicletar.json").catch(() => []),
  ]);
  const amanhaTipo = tipoDoDia((hoje.diaSemana + 1) % 7);
  return {
    indice, grade, regras, bike, tipo,
    // A de amanhã só é baixada quando precisa (fim do serviço de hoje).
    amanha: () => carregarGrade(indice, amanhaTipo),
  };
}

// ---------- a grade de um dia

export class Grade {
  constructor(g) {
    this.data = g.data;
    this.tipo = g.tipo;
    this.linhas = g.linhas;
    this.estacoes = g.estacoes;
    this.integracoes = g.integracoes || [];
    /** stopId -> [{ linha, sentido, pos }] */
    this.passagens = new Map();
    for (const linha of this.linhas) {
      for (const sentido of linha.sentidos) {
        sentido.estacoes.forEach((id, pos) => {
          if (!this.passagens.has(id)) this.passagens.set(id, []);
          this.passagens.get(id).push({ linha, sentido, pos });
        });
      }
    }
  }

  linha(id) { return this.linhas.find((l) => l.id === id); }
  nome(stopId) { return this.estacoes[stopId]?.nome ?? stopId; }

  /** Estações de uma linha, na ordem do primeiro sentido (a mais longa). */
  estacoesDaLinha(linha) {
    const s = [...linha.sentidos].sort((a, b) => b.estacoes.length - a.estacoes.length)[0];
    return s ? s.estacoes : [];
  }

  /** Sentidos em que dá para partir desta estação (o que termina nela só chega). */
  sentidosDePartida(linha, stopId) {
    const lista = linha.sentidos.filter((s) => {
      const pos = s.estacoes.indexOf(stopId);
      return pos >= 0 && pos < s.estacoes.length - 1;
    });
    return lista.length ? lista : linha.sentidos.filter((s) => s.estacoes.includes(stopId));
  }

  /** Minutos de partida nesta estação, neste sentido, em ordem. */
  partidas(sentido, stopId) {
    const pos = sentido.estacoes.indexOf(stopId);
    if (pos < 0) return [];
    return sentido.viagens.map((v) => v[pos]).filter((m) => m != null).sort((a, b) => a - b);
  }
}

/**
 * As próximas `n` partidas a partir de `minutos`. Acabando as de hoje, segue
 * pelas de amanhã (marcadas), se houver a grade de amanhã. Igual ao app.
 */
export function proximas(hoje, minutos, n, amanha) {
  const lista = hoje.filter((m) => m >= minutos).slice(0, n).map((m) => ({ m, amanha: false }));
  if (lista.length < n && amanha) {
    for (const m of amanha.slice(0, n - lista.length)) lista.push({ m, amanha: true });
  }
  return lista;
}

export function minutosAte(p, minutos) {
  return p.m - minutos + (p.amanha ? DIA : 0);
}

// ---------- distância

export function distancia(lat1, lon1, lat2, lon2) {
  const r = Math.PI / 180;
  const dLat = (lat2 - lat1) * r, dLon = (lon2 - lon1) * r;
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.sin(dLon / 2) ** 2;
  return 2 * 6371000 * Math.asin(Math.min(1, Math.sqrt(a)));
}

export function metros(m) {
  return m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(1).replace(".", ",")} km`;
}

/** "metrô" na Linha Sul, "VLT" nas outras — como no app. */
export function veiculo(nomeLinha) {
  return nomeLinha.trim().toLowerCase() === "linha sul" ? "metrô" : "VLT";
}
