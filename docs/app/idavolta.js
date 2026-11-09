// "Sua ida e volta": para onde a pessoa vai agora e qual metrô pegar. A mesma
// regra do app completo (data/Commute.kt).

const DIA = 1440;
const MOSTRA_ULTIMO = 6; // a partir de quantas partidas para o fim do dia o último aparece

/**
 * Só pelo relógio. Entre a saída e a próxima chegada, a primeira metade ainda é a
 * volta e a segunda já é a ida; entre a chegada e a saída, a próxima é a volta.
 */
export function destinoPeloRelogio(agora, chegar, sair) {
  const intervalo = (chegar - sair + DIA) % DIA;
  const desdeSaida = (agora - sair + DIA) % DIA;
  return desdeSaida >= Math.floor(intervalo / 2) && desdeSaida <= intervalo ? "trabalho" : "casa";
}

/** Perto de uma das duas estações, para a outra; sem localização, ou longe das duas, pelo relógio. */
export function destinoIdaVolta(agora, chegar, sair, dCasa, dTrabalho, perto) {
  if (dCasa != null && dTrabalho != null && Math.min(dCasa, dTrabalho) <= perto) {
    return dCasa <= dTrabalho ? "trabalho" : "casa";
  }
  return destinoPeloRelogio(agora, chegar, sair);
}

/**
 * [a] e [b] são a mesma estação para quem sai dela: o mesmo ponto, ou os dois lados
 * de uma integração a pé (Parangaba). O nome não serve: há dois "Padre Cícero".
 */
export function mesmaEstacao(a, b, ligacoes) {
  return a === b || ligacoes.some((l) => l.deEst === a && l.paraEst === b);
}

/**
 * As viagens a sugerir a partir da estação aberta ("linha|id"): para as duas
 * estações da última viagem, menos a que for a própria estação. Na origem sobra o
 * destino; no destino, a volta; em outra estação, as duas, o destino primeiro. Sem
 * estação conhecida (null), a viagem e a volta como foram feitas. Destino em estação
 * com um lado em cada linha (Parangaba e Parangaba - NE): vale o lado da linha de quem
 * sai, senão não há metrô direto. nomeDe: o nome sem o sufixo da linha.
 */
export function viagensSugeridas(ultima, estacao, ligacoes, nomeDe) {
  const volta = { origem: ultima.destino, destino: ultima.origem, lugar: null };
  const ida = { origem: ultima.origem, destino: ultima.destino, lugar: ultima.lugar || null };
  if (!estacao) return [ida, volta];
  const [linha, id] = estacao.split("|");
  return [ida, volta].filter((v) => !mesmaEstacao(id, v.destino.split("|")[1], ligacoes)).map((v) => {
    const [ld, ed] = v.destino.split("|");
    const lado = ligacoes.find((l) => l.deEst === ed && l.deLinha === ld && l.paraLinha === linha && nomeDe(l.paraEst) === nomeDe(ed));
    return { ...v, origem: estacao, destino: lado ? `${lado.paraLinha}|${lado.paraEst}` : v.destino };
  });
}

/** A próxima estação do sentido, de referência. O terminal não vale: já é o nome do sentido. */
export function escolherVia(frente) {
  return frente.length >= 2 ? frente[0] : null;
}

/**
 * Escolhe o metrô de [opcoes] (os do resto do dia, em ordem). [alvo]: a hora
 * marcada (chegar, na ida; sair, na volta), ou null quando não vale agora.
 * motivo: proximo, chegar, tarde (nenhum chega mais a tempo), sair, cedo (o dia
 * acaba antes da hora de sair).
 */
export function escolherMetro(opcoes, ida, alvo, agora) {
  if (!opcoes.length) return null;
  const emVolta = (i) => [opcoes[i - 1], opcoes[i + 1]].filter(Boolean);
  let i = 0, outras = opcoes.slice(1, 3), motivo = "proximo";
  if (alvo != null && agora <= alvo) {
    if (ida) {
      const achado = opcoes.reduce((m, o, k) => (o.chega <= alvo ? k : m), -1);
      if (achado >= 0) { i = achado; outras = emVolta(i); motivo = "chegar"; } else motivo = "tarde";
    } else {
      const achado = opcoes.findIndex((o) => o.sai >= alvo);
      if (achado >= 0) { i = achado; outras = emVolta(i); motivo = "sair"; } else {
        i = opcoes.length - 1; outras = opcoes.slice(0, -1).slice(-2); motivo = "cedo";
      }
    }
  }
  const pertoDoFim = motivo !== "cedo" && i >= opcoes.length - MOSTRA_ULTIMO;
  return { principal: opcoes[i], outras, motivo, ultimo: pertoDoFim ? opcoes[opcoes.length - 1].sai : null };
}
