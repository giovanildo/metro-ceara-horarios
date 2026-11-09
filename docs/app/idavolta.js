// "Sua ida e volta": para onde a pessoa vai agora e qual metrô pegar. A mesma
// regra do app completo (data/Commute.kt).

const DIA = 1440;
const VOLTA_HORAS = 3; // sem localização, a última viagem vira a volta depois disso (padrão do app)
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

const diaEmFortaleza = (ms) => new Date(ms).toLocaleDateString("en-CA", { timeZone: "America/Fortaleza" });

/**
 * A última viagem deve aparecer invertida? Perto de uma das duas estações, sim se
 * a mais perto for o destino; sem localização, ou longe das duas, sim se foi vista
 * hoje, há pelo menos VOLTA_HORAS.
 */
export function deveInverterRecente(dOrigem, dDestino, perto, vistaEm, agoraMs) {
  if (dOrigem != null && dDestino != null && Math.min(dOrigem, dDestino) <= perto) return dDestino < dOrigem;
  return Boolean(vistaEm) && diaEmFortaleza(vistaEm) === diaEmFortaleza(agoraMs) && agoraMs - vistaEm >= VOLTA_HORAS * 3600000;
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
