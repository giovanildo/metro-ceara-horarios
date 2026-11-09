// Horários, estações e relógio. Os dados vêm prontos do ramo "dados" do
// repositório (gerados todo dia por tools/build_web_data.py): o navegador não
// consegue baixar o GTFS direto do Metrofor.

export const BASE = "https://raw.githubusercontent.com/giovanildo/metro-ceara-horarios/dados/";
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

/**
 * A grade de um tipo de dia (ou a de dia útil, se ainda não houver). Com [prever], a
 * última de um dia comum ("normais"): a de um feriado não serve de previsão.
 */
async function carregarGrade(indice, tipo, prever = false) {
  const tipos = prever && indice.normais ? { ...indice.tipos, ...indice.normais } : indice.tipos;
  const info = tipos[tipo] || tipos["dia-util"];
  if (!info) return null;
  const g = await json(info.arquivo);
  return new Grade(g);
}

/** Dia com operação especial anunciada que muda o horário ("normal": a notícia diz que não muda). */
export function diaEspecial(regras, data) {
  const op = regras?.operacoes?.[data];
  return !!op && !op.normal;
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
  // Onde o GTFS põe a estação no lugar errado (Sobral, Escola do Cariri), vale a posição de regras.json.
  for (const [id, e] of Object.entries(grade?.estacoes || {})) {
    const posicao = infoEstacao(regras, id, e.nome)?.posicao;
    if (posicao) [e.lat, e.lon] = posicao;
  }
  // O próximo dia com metrô: amanhã, ou a segunda quando amanhã é domingo
  // (metrô e VLT normalmente não circulam, embora o GTFS diga que sim).
  const proximoEhSegunda = (hoje.diaSemana + 1) % 7 === 0;
  const proximoTipo = tipoDoDia(proximoEhSegunda ? 1 : (hoje.diaSemana + 1) % 7);
  // Operação especial anunciada no próximo dia: a grade dele só sai depois da meia-noite.
  const proximoEspecial = diaEspecial(regras, somarDias(hoje.data, proximoEhSegunda ? 2 : 1));
  return {
    indice, grade, regras, bike, tipo, proximoEhSegunda, proximoEspecial,
    // As notícias do Metrofor que explicam as operações especiais (feriado, ENEM, eleição)
    // de hoje em diante, do regras.json: avisadas assim que aparecem, não só na véspera.
    // Dias com a mesma notícia viram um aviso só ({dias: [...]}); o de hoje fica separado,
    // porque vai junto com os horários. Sem operação anunciada, nenhuma.
    avisos: Object.keys(regras?.operacoes || {}).filter((d) => d >= hoje.data).sort()
      .reduce((l, d) => {
        const op = regras.operacoes[d];
        const igual = d !== hoje.data && l.find((x) => !x.hoje && x.titulo === op.titulo && x.link === op.link);
        if (igual) igual.dias.push(d);
        else l.push({ ...op, dias: [d], hoje: d === hoje.data });
        return l;
      }, []),
    proximoDia: () => (proximoEspecial ? Promise.resolve(null) : carregarGrade(indice, proximoTipo, true)),
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
    // A grade do dia às vezes só traz a ida (Linha Norte de Sobral, nos dias úteis):
    // a volta entra sem viagens, para a tela dizer que o Metrofor não a publicou.
    for (const linha of this.linhas) {
      const [so] = linha.sentidos;
      if (linha.sentidos.length === 1 && so.estacoes.length > 1) {
        linha.sentidos.push({ id: 1 - so.id, destino: this.estacoes[so.estacoes[0]]?.nome ?? "", estacoes: [...so.estacoes].reverse(),
          viagens: [], naoPublicado: true });
      }
    }
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
    // Pelo número do sentido: o 0 fica sempre à esquerda no trilho das abas.
    return (lista.length ? lista : linha.sentidos.filter((s) => s.estacoes.includes(stopId))).sort((a, b) => a.id - b.id);
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

// ---------- regras do Metrofor (regras.json, o mesmo arquivo do app)

const minDe = (hhmm) => { const [h, m] = hhmm.split(":").map(Number); return h * 60 + m; };
const simples = (s) => s.normalize("NFD").replace(/[^A-Za-z0-9]/g, "").toLowerCase();

/**
 * Endereço (e posição corrigida, se houver) da estação, por stop_id. O nome tem de
 * bater: se o Metrofor renumerar as estações, nada vai para a estação errada.
 */
export function infoEstacao(regras, stopId, nome) {
  const e = regras?.estacoes?.[stopId];
  return e && simples(e.nome) === simples(nome) ? e : null;
}

/** O Metrofor avisou que esta linha não circula neste dia? */
export function linhaParada(regras, data, nomeLinha) {
  return (regras?.operacoes?.[data]?.paradas || []).some((n) => n.trim().toLowerCase() === nomeLinha.trim().toLowerCase());
}

/**
 * A frase de como a linha volta na segunda, para os avisos de domingo; null se não
 * se sabe. [data]: a segunda; [primeiro]: o primeiro metrô dela, em minutos.
 */
export function voltaDaSegunda(regras, data, nomeLinha, primeiro, verbo = "Voltam") {
  if (nomeLinha && linhaParada(regras, data, nomeLinha)) return "O Metrofor avisou que esta linha também não circula segunda.";
  if (diaEspecial(regras, data)) return "Segunda o horário é especial e só sai depois da meia-noite.";
  return primeiro === undefined ? null : `${verbo} segunda, a partir das ${hora(primeiro)}.`;
}

/** "R$ 3,60 (meia R$ 1,80) · dinheiro ou Pix", ou null. */
export function passagemDaLinha(regras, nomeLinha) {
  const [, texto] = Object.entries(regras?.passagem || {})
    .find(([k]) => !k.startsWith("_") && k.trim().toLowerCase() === nomeLinha.trim().toLowerCase()) || [];
  return texto ?? null;
}

/**
 * As janelas de bike desta estação/linha hoje, ou null se lá não há aviso. Num dia
 * especial anunciado pelo Metrofor (regras.datas_especiais[data]), vale a regra do
 * dia; nos outros, a do dia da semana. O resultado diz também se é dia especial.
 */
export function janelasBike(regras, nomeEstacao, nomeLinha, diaSemana, data) {
  const bike = regras?.bike;
  if (!bike) return null;
  const achar = (tabela, nome) => Object.entries(tabela || {})
    .find(([k]) => !k.startsWith("_") && k.trim().toLowerCase() === nome.trim().toLowerCase())?.[1];
  const id = achar(bike.estacoes, nomeEstacao) ?? achar(bike.linhas, nomeLinha);
  const reg = id && bike.regulamentos[id];
  if (!reg) return null;
  const especial = regras.datas_especiais?.[data];
  const vale = especial && (!especial.linhas?.length
    || especial.linhas.some((l) => l.trim().toLowerCase() === nomeLinha.trim().toLowerCase()));
  const lista = vale ? especial.bike
    : diaSemana === 6 ? reg.sabado : diaSemana === 0 ? (reg.domingo || []) : reg.dias_uteis;
  const janelas = lista.map(([ini, fim]) => ({ ini: minDe(ini), fim: fim == null ? null : minDe(fim) }))
    .sort((a, b) => a.ini - b.ini);
  janelas.especial = Boolean(vale);
  janelas.porViagem = reg.limite_por_viagem || null;
  janelas.fonteId = vale ? null : reg.fonte_id || null;
  return janelas;
}

/** Igual ao app: { pode, limite } — limite = fim da janela atual, ou começo da próxima de hoje. */
export function bikeAgora(janelas, minutos) {
  for (const j of janelas) {
    if (minutos >= j.ini && (j.fim == null || minutos < j.fim)) return { pode: true, limite: j.fim };
  }
  return { pode: false, limite: janelas.map((j) => j.ini).find((i) => i > minutos) ?? null };
}

/** Passagem da integração de uma estação para outra (por nome, nos dois sentidos). */
export function passagem(regras, nomeDe, nomePara) {
  const n = (s) => s.trim().toLowerCase();
  for (const r of regras?.integracoes || []) {
    if (n(r.a) === n(nomeDe) && n(r.b) === n(nomePara)) return { tipo: r.passagem_a_para_b, motivo: r.motivo_a_para_b };
    if (n(r.b) === n(nomeDe) && n(r.a) === n(nomePara)) return { tipo: r.passagem_b_para_a, motivo: r.motivo_b_para_a };
  }
  return { tipo: "desconhecida" };
}

const MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"];
/** "2026-11" ou "2026-11-02" → número de meses (ano*12 + mês-1), ou null. */
function mesNum(texto) {
  const m = /^(\d{4})-(\d{2})/.exec(texto || "");
  return m && +m[2] >= 1 && +m[2] <= 12 ? +m[1] * 12 + (+m[2] - 1) : null;
}
const mesTexto = (n) => `${MESES[n % 12]}/${Math.floor(n / 12)}`;

/**
 * A linha de fonte de uma informação (regras.json "fontes"), igual à do app:
 * "Fonte: site do Metrofor · conferido em nov/2026 · próxima conferência: mai/2027".
 * Estimativa ou observação do app não leva "Fonte:" nem próxima conferência.
 * Devolve { texto, link, titulo, detalhe } ou null.
 */
export function fonteLinha(regras, id, data) {
  const f = regras?.fontes?.[id];
  if (!f || id.startsWith("_")) return null;
  const app = f.tipo === "app";
  const partes = [app ? f.fonte : `Fonte: ${f.fonte}`];
  if (f.desde) partes.push(f.desde);
  const conferido = mesNum(f.conferido);
  if (f.diario) partes.push("acompanhado todo dia");
  else if (app) { if (conferido != null) partes.push(mesTexto(conferido)); }
  else {
    if (conferido != null) partes.push(`conferido em ${mesTexto(conferido)}`);
    if (conferido != null && f.revisar_em_meses > 0) {
      const proxima = conferido + f.revisar_em_meses;
      partes.push(mesNum(data) > proxima ? `conferência atrasada desde ${mesTexto(proxima)}: pode ter mudado`
        : `próxima conferência: ${mesTexto(proxima)}`);
    }
  }
  // O link curto na tela e o que a janela mostra, uma coisa por linha (igual ao app).
  const proxima = conferido != null && f.revisar_em_meses > 0 ? conferido + f.revisar_em_meses : null;
  const atrasada = proxima != null && mesNum(data) > proxima;
  const rotulo = app ? f.fonte : atrasada ? "Fonte · pode ter mudado" : "Fonte";
  const fatos = app ? [f.fonte + (conferido != null ? ` (${mesTexto(conferido)})` : "")] : [
    `Fonte: ${f.fonte}`,
    ...(f.desde ? [f.desde[0].toUpperCase() + f.desde.slice(1)] : []),
    ...(f.diario ? ["Acompanhado todo dia"] : []),
    ...(conferido != null ? [`Conferido em ${mesTexto(conferido)}`] : []),
    ...(proxima != null ? [atrasada ? `Conferência atrasada desde ${mesTexto(proxima)}: pode ter mudado` : `Próxima conferência: ${mesTexto(proxima)}`] : []),
  ];
  return {
    texto: partes.join(" · "), rotulo, fatos, atrasada,
    link: /^https:\/\//.test(f.link || "") ? f.link : null, titulo: f.titulo, detalhe: f.detalhe || null,
  };
}
