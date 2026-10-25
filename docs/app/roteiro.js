// Roteiro com integração: a mesma regra do app Android (data/Router.kt e
// data/TransferTime.kt). A rede é pequena, então testa cada trem do primeiro
// trecho e segue a pé e no próximo trem de cada integração.

const MAX_INTEGRACOES = 3; // Caucaia → Aeroporto
const MESMA_PLATAFORMA_MIN = 1; // Campo dos Velhos, em Sobral: duas linhas na mesma estação
const JUSTA_MIN = 1;

export const PERFIS = [
  { chave: "andando", nome: "Andando", fator: 1 },
  { chave: "sem_pressa", nome: "Sem pressa, com criança ou idoso", fator: 1.5 },
  { chave: "bicicleta", nome: "Com bicicleta", fator: 1.5 },
  { chave: "cadeira_de_rodas", nome: "Cadeira de rodas ou mobilidade reduzida", fator: 2 },
];

const minDe = (hhmm) => { const [h, m] = hhmm.split(":").map(Number); return h * 60 + m; };
const dentro = (faixas, t) => (faixas || []).some(([a, b]) => t >= minDe(a) && t < minDe(b));

/** Minutos para trocar de linha numa integração alcançada às t: tempo da estação × perfil + pico. */
export function minutosDeTroca(regras, base, t, perfil) {
  const fator = regras?.deslocamento?.[perfil] ?? PERFIS.find((p) => p.chave === perfil)?.fator ?? 1;
  const pico = regras?.pico;
  const extra = dentro(pico?.maximo, t) ? (pico?.integracao_maximo_min ?? 0)
    : dentro(pico?.faixas, t) ? (pico?.integracao_min ?? 0) : 0;
  return Math.ceil(base * fator - 1e-9) + extra;
}

export class Roteador {
  constructor(grade, regras, perfil) {
    this.grade = grade;
    this.regras = regras;
    this.perfil = perfil;
    this.cache = new Map();
    // Ligações entre linhas: as integrações da grade (nos dois sentidos) e a
    // estação que serve duas linhas.
    const linhasDe = (id) => [...new Set((grade.passagens.get(id) || []).map((p) => p.linha.id))];
    this.ligacoes = [];
    for (const i of grade.integracoes) {
      for (const [de, para] of [[i.a, i.b], [i.b, i.a]]) {
        for (const la of linhasDe(de)) for (const lb of linhasDe(para)) {
          if (la !== lb) this.ligacoes.push({ deLinha: la, deEst: de, paraLinha: lb, paraEst: para, base: i.caminhada_min });
        }
      }
    }
    for (const id of grade.passagens.keys()) {
      const ls = linhasDe(id);
      for (const la of ls) for (const lb of ls) {
        if (la !== lb) this.ligacoes.push({ deLinha: la, deEst: id, paraLinha: lb, paraEst: id, base: MESMA_PLATAFORMA_MIN });
      }
    }
  }

  /** Viagens de uma linha que passam por de e depois por para: [{ sai, chega, sentido }], pela partida. */
  viagens(linhaId, de, para) {
    const chave = `${linhaId}|${de}|${para}`;
    if (!this.cache.has(chave)) {
      const lista = [];
      for (const s of this.grade.linha(linhaId)?.sentidos || []) {
        const i = s.estacoes.indexOf(de);
        const j = s.estacoes.indexOf(para);
        if (i < 0 || j < 0 || i >= j) continue;
        for (const v of s.viagens) {
          if (v[i] != null && v[j] != null && v[j] > v[i]) lista.push({ sai: v[i], chega: v[j], sentido: s });
        }
      }
      this.cache.set(chave, lista.sort((a, b) => a.sai - b.sai));
    }
    return this.cache.get(chave);
  }

  caminhos(inicio, alvo) {
    const achados = [];
    const andar = (linha, caminho, vistas) => {
      if (alvo == null || linha === alvo) {
        achados.push(caminho);
        if (alvo != null) return;
      }
      if (caminho.length === MAX_INTEGRACOES) return;
      const feitas = new Set();
      for (const l of this.ligacoes) {
        const k = `${l.paraLinha}|${l.deEst}`;
        if (l.deLinha !== linha || vistas.has(l.paraLinha) || feitas.has(k)) continue;
        feitas.add(k);
        andar(l.paraLinha, [...caminho, l], new Set([...vistas, l.paraLinha]));
      }
    };
    andar(inicio, [], new Set([inicio]));
    return achados;
  }

  /** As linhas que se alcançam a partir desta, com o caminho mais curto até cada uma. */
  alcancaveis(linhaId) {
    const melhor = new Map();
    for (const c of this.caminhos(linhaId, null)) {
      const fim = c.length ? c[c.length - 1].paraLinha : linhaId;
      if (!melhor.has(fim) || c.length < melhor.get(fim).length) melhor.set(fim, c);
    }
    return melhor;
  }

  simular(origem, destino, caminho, apos) {
    const trechos = [];
    let linha = origem.linha, est = origem.est;
    for (const l of caminho) { trechos.push([linha, est, l.deEst]); linha = l.paraLinha; est = l.paraEst; }
    trechos.push([linha, est, destino.est]);
    const [l0, e0, f0] = trechos[0];
    const primeiros = e0 === f0 ? [null] : this.viagens(l0, e0, f0).filter((v) => v.sai >= apos);
    const saida = [];
    for (const p of primeiros) {
      const pernas = [], trocas = [];
      let relogio = apos;
      if (p) { pernas.push({ linha: l0, de: e0, para: f0, ...p }); relogio = p.chega; }
      let ok = true;
      for (let k = 1; k < trechos.length; k++) {
        const [lk, ek, fk] = trechos[k];
        const lig = caminho[k - 1];
        const precisa = minutosDeTroca(this.regras, lig.base, relogio, this.perfil);
        const opcoes = this.viagens(lk, ek, fk);
        const idx = opcoes.findIndex((v) => v.sai >= relogio + precisa);
        if (idx < 0) { ok = false; break; }
        const v = opcoes[idx];
        if (pernas.length) {
          trocas.push({ lig, precisa, sobra: v.sai - relogio - precisa, seguinte: opcoes[idx + 1]?.sai ?? null });
        }
        pernas.push({ linha: lk, de: ek, para: fk, ...v });
        relogio = v.chega;
      }
      if (ok && pernas.length) saida.push({ pernas, trocas, sai: pernas[0].sai, chega: pernas[pernas.length - 1].chega });
    }
    return saida;
  }

  /** Os roteiros, um por horário de chegada (o primeiro chega mais cedo); cada um sai o mais tarde possível. */
  planejar(origem, destino, apos, limite = 4, evitar = new Set()) {
    const todos = this.caminhos(origem.linha, destino.linha)
      .filter((c) => !evitar.has(origem.linha) && c.every((l) => !evitar.has(l.paraLinha)))
      .flatMap((c) => this.simular(origem, destino, c, apos));
    const porChegada = new Map();
    for (const r of todos) {
      if (!porChegada.has(r.chega)) porChegada.set(r.chega, []);
      porChegada.get(r.chega).push(r);
    }
    return [...porChegada.keys()].sort((a, b) => a - b).slice(0, limite).map((t) => {
      const iguais = porChegada.get(t).sort((a, b) => a.sai - b.sai);
      const folgado = [...iguais].reverse().find((r) => r.trocas.every((x) => x.sobra > JUSTA_MIN));
      const escolhido = folgado || iguais[iguais.length - 1];
      return { ...escolhido, antes: iguais[0].sai < escolhido.sai ? iguais[0].sai : null };
    });
  }
}
