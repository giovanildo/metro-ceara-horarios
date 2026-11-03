import {
  agora, bikeAgora, carregar, dataCurta, distancia, falta, hora, infoEstacao, janelasBike, metros, minutosAte, passagem,
  linhaParada, passagemDaLinha, fonteLinha, proximas, somarDias, veiculo,
} from "./dados.js";
import { PERFIS, Roteador } from "./roteiro.js";
import { aoLado, buscarLugares, caminhada, emojiLugar, longe, metrosAPe, minutosAPe, pertoDaEstacao, serve } from "./lugares.js";

const PERTO_METROS = 3000; // outras linhas aparecem até esta distância (padrão do app)
const LINK = "https://giovanildo.github.io/metro-ceara-horarios/app/";
const PAGINA_INSTALAR = "../#baixar"; // direto no convite para testar pela Play Store
// O convite da lite abre o WhatsApp com a mensagem pronta (o mesmo botão da página, em #baixar).
const WHATSAPP_TESTADOR = "https://wa.me/5585997619295?text=Ol%C3%A1%21%20Quero%20ser%20testador%20beta%20do%20app%20Metr%C3%B4%20Cear%C3%A1.%20Meu%20Gmail%20da%20Play%20Store%20%C3%A9%3A%20";
const PIX = "giovanildos@gmail.com";
const ANDROID = /Android/i.test(navigator.userAgent);
const IPHONE = /iPhone|iPad|iPod/i.test(navigator.userAgent);
// Aberta pelo ícone da tela inicial: não há o que instalar.
const INSTALADA = matchMedia("(display-mode: standalone)").matches || navigator.standalone === true;
/** O pedido de instalação que o Chrome oferece (só no Android, quando ele acha que pode). */
let pedidoInstalar = null;
window.addEventListener("beforeinstallprompt", (e) => {
  e.preventDefault();
  pedidoInstalar = e;
  render();
});
window.addEventListener("appinstalled", () => { pedidoInstalar = null; render(); });
const PROXIMAS = 4;
const tela = document.getElementById("tela");
const janela = document.getElementById("janela");

/** Só uma comodidade: quanto tempo antes avisar, lembrado da última vez. */
const AVISO_PADRAO = 15;
const AVISOS = [5, 10, 15, 20, 30, 45];

const estado = {
  dados: null,
  proximo: null, // a grade do próximo dia com metrô
  pos: null, // { lat, lon }
  localizando: false,
  erroLocal: null,
  config: lerConfig(),
};

// ---------- utilidades

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function lerConfig() {
  try {
    return { antes: AVISO_PADRAO, ...JSON.parse(localStorage.getItem("config") || "{}") };
  } catch {
    return { antes: AVISO_PADRAO };
  }
}

function salvarConfig() {
  try { localStorage.setItem("config", JSON.stringify(estado.config)); } catch { /* segue */ }
}

// A cor do mapa oficial (regras.json "cores"); sem ela, a do GTFS. No tema escuro,
// o azul e o verde do mapa ficam apagados e usam o tom "escuro".
function cor(linha) {
  const nome = linha.nome.trim().toLowerCase();
  const c = Object.entries(estado.dados?.regras?.cores || {}).find(([n]) => n.trim().toLowerCase() === nome)?.[1];
  const escuro = matchMedia("(prefers-color-scheme: dark)").matches;
  return "#" + ((c && (escuro ? c.escuro || c.claro : c.claro)) || linha.cor);
}

/** O link curto embaixo de uma informação ("Fonte ›"); tocar abre a janela com a origem e as datas. */
function fonte(id, estilo = "", rotulo = null) {
  const f = id === "bike-hoje" ? { rotulo: "Fonte" } : fonteLinha(estado.dados?.regras, id, agora().data);
  return f ? `<p class="fonte" style="${estilo}"><a href="#" data-fonte="${esc(id)}">${esc(rotulo || f.rotulo)} ›</a></p>` : "";
}

/** A janela da fonte. "bike-hoje": a regra de bike do dia especial, que vem da notícia do dia. */
function abrirFonte(id) {
  const { regras } = estado.dados;
  const t = agora();
  const f = id === "bike-hoje"
    ? { titulo: "Bike no metrô hoje", fatos: ["Fonte: notícia do Metrofor de hoje · vale só para hoje"], link: regras?.operacoes?.[t.data]?.link }
    : fonteLinha(regras, id, t.data);
  if (!f) return;
  janela.innerHTML = `<h3>${esc(f.titulo)}</h3>${f.fatos.map((x) => `<p style="margin:6px 0">${esc(x)}</p>`).join("")}
    ${f.detalhe ? `<p class="dica">${esc(f.detalhe)}</p>` : ""}
    <div class="acoes"><button class="botao secundario" id="fechar-fonte">Fechar</button>
      ${f.link ? `<a class="botao" href="${esc(f.link)}" target="_blank" rel="noopener">Abrir a fonte</a>` : ""}</div>`;
  janela.showModal();
  janela.querySelector("#fechar-fonte").onclick = () => janela.close();
}
document.addEventListener("click", (e) => {
  const a = e.target.closest("[data-fonte]");
  if (!a) return;
  e.preventDefault();
  abrirFonte(a.dataset.fonte);
});

/** A pessoa está escolhendo algo (janela aberta ou campo em uso): não redesenhar agora. */
function ocupado() {
  return janela.open || document.activeElement?.matches("select, input");
}

// ---------- localização

function localizar() {
  if (!("geolocation" in navigator)) {
    estado.erroLocal = "Este navegador não informa a localização.";
    render();
    return;
  }
  estado.localizando = true;
  estado.erroLocal = null;
  render();
  navigator.geolocation.watchPosition(
    (p) => {
      const nova = { lat: p.coords.latitude, lon: p.coords.longitude };
      const antes = estado.pos;
      estado.pos = nova;
      estado.localizando = false;
      try { localStorage.setItem("localizacao", "sim"); } catch { /* segue */ }
      // Redesenhar no meio de uma escolha fecharia a lista: só se andou de verdade.
      const andou = !antes || distancia(antes.lat, antes.lon, nova.lat, nova.lon) > 20;
      if (andou && !ocupado()) render();
    },
    (e) => {
      estado.localizando = false;
      estado.erroLocal = e.code === 1
        ? "Sem permissão de localização. Libere nas configurações do navegador para ver a estação mais perto."
        : "Não deu para achar sua localização agora. Tente de novo em um lugar aberto.";
      render();
    },
    { enableHighAccuracy: true, maximumAge: 10000, timeout: 30000 },
  );
}

// ---------- partidas

/**
 * Domingo sem operação anunciada pelo Metrofor: metrô e VLT normalmente não circulam,
 * embora o GTFS traga a grade de dia útil. As partidas de hoje ficam de fora e valem as
 * de segunda; com operação anunciada, vale a grade do dia.
 */
function domingoSemOperacao(t = agora()) {
  return t.diaSemana === 0 && !estado.dados.regras?.operacoes?.[t.data];
}

function partidasComAmanha(linha, sentido, stopId) {
  const g = estado.dados.grade;
  const hoje = domingoSemOperacao() ? [] : g.partidas(sentido, stopId);
  let amanha = null;
  // Amanhã domingo: nada de horários de amanhã na lista; o fim do dia diz a segunda.
  const ga = estado.dados.proximoEhSegunda ? null : estado.proximo;
  if (ga) {
    const s = ga.linha(linha.id)?.sentidos.find((x) => x.id === sentido.id);
    if (s) amanha = ga.partidas(s, stopId);
  }
  return { hoje, amanha };
}

/**
 * A notícia do Metrofor que explica a operação especial de hoje (e a de amanhã):
 * a manchete; tocar abre a notícia no site deles.
 */
function avisosMetrofor(soHoje = false) {
  return estado.dados.avisos.filter((n) => !soHoje || !n.amanha).map((n) => {
    const texto = `<strong>Metrofor, ${n.amanha ? "amanhã" : "hoje"}:</strong> ${esc(n.titulo)}`;
    return /^https:\/\//.test(n.link || "")
      ? `<p class="aviso-metrofor"><a href="${esc(n.link)}" target="_blank" rel="noopener">${texto} ›</a></p>`
      : `<p class="aviso-metrofor">${texto}</p>`;
  }).join("");
}

/** "Último de hoje: 17:12 · primeiro de segunda: 05:44", para quando hoje acabou. */
function fimDoDia(linha, sentido, stopId) {
  const ultimo = domingoSemOperacao() ? undefined : estado.dados.grade.partidas(sentido, stopId).at(-1);
  let primeiro;
  const g = estado.proximo;
  const s = g?.linha(linha.id)?.sentidos.find((x) => x.id === sentido.id);
  if (s) primeiro = g.partidas(s, stopId)[0];
  const partes = [];
  if (ultimo !== undefined) partes.push(`Último de hoje: ${hora(ultimo)}`);
  if (primeiro !== undefined) {
    partes.push(`${partes.length ? "p" : "P"}rimeiro de ${estado.dados.proximoEhSegunda ? "segunda" : "amanhã"}: ${hora(primeiro)}`
      + (estado.dados.proximoEhSegunda ? " (domingo, só em operação especial)" : ""));
  } else if (estado.dados.proximoEspecial) {
    const texto = estado.dados.proximoEhSegunda
      ? "segunda o horário é especial e só sai depois da meia-noite de domingo"
      : "amanhã o horário é especial e só sai depois da meia-noite";
    partes.push(partes.length ? texto : texto[0].toUpperCase() + texto.slice(1));
  }
  return partes.join(" · ");
}

function textoPartida(p, minutos, sentido) {
  if (!p) return sentido?.naoPublicado ? "horários não publicados" : "sem mais partidas hoje";
  return `${hora(p.m)}${p.amanha ? " (amanhã)" : ""} · ${falta(minutosAte(p, minutos))}`;
}

// ---------- telas

function telaInicio() {
  const { grade } = estado.dados;
  const t = agora();
  let html = convite() + avisoDomingo(t) + avisosMetrofor();
  html += `<a class="para-onde" href="#/viagem">${LUPA} Para onde você vai?</a>`;
  html += atalhosLugares(t);

  if (!estado.pos) {
    html += `<div class="cartao">
      <p>Veja de uma vez a próxima partida da estação mais próxima de você.</p>
      <button class="botao" id="localizar" ${estado.localizando ? "disabled" : ""}>
        ${estado.localizando ? "Procurando você…" : "Usar minha localização"}</button>
      ${estado.erroLocal ? `<p class="dica">${esc(estado.erroLocal)}</p>` : ""}
    </div>`;
  } else {
    // A estação mais perto de cada linha; mostra as que estão até 3 km (e sempre a mais perto).
    const porLinha = grade.linhas.map((linha) => {
      let melhor = null;
      for (const id of grade.estacoesDaLinha(linha)) {
        const e = grade.estacoes[id];
        const d = distancia(estado.pos.lat, estado.pos.lon, e.lat, e.lon);
        if (!melhor || d < melhor.d) melhor = { id, d };
      }
      return melhor && { linha, ...melhor };
    }).filter(Boolean).sort((a, b) => a.d - b.d);
    const perto = porLinha.filter((x, i) => i === 0 || x.d <= PERTO_METROS);
    for (const { linha, id, d } of perto) {
      const sentidos = grade.sentidosDePartida(linha, id);
      html += `<div class="cartao">
        <a class="cabeca" href="#/estacao/${linha.id}/${id}">
          <h3>${esc(grade.nome(id))} <span class="ponto" style="background:${cor(linha)}"></span>
            <span class="linha-nome">${esc(linha.nome)}</span></h3>
          <span class="distancia">${metros(d)} de você</span>
          ${avisoParada(linha, t)}
          ${sentidos.map((s) => {
            const { hoje, amanha } = partidasComAmanha(linha, s, id);
            const p = proximas(hoje, t.minutos, 1, amanha)[0];
            const fim = p ? "" : fimDoDia(linha, s, id);
            return `<div class="partida"><span>→ ${esc(s.destino)}</span><span class="hora">${textoPartida(p, t.minutos, s)}</span></div>`
              + (fim ? `<p class="dica fim-do-dia">${fim}</p>` : "");
          }).join("")}
        </a></div>`;
    }
  }

  if (pedidoInstalar && !INSTALADA) {
    html += `<p><button class="botao secundario" id="instalar">📲 Adicionar à tela inicial</button></p>`;
  }
  html += `<h2>Linhas</h2><div class="lista-linhas">${grade.linhas.map((l) =>
    `<a href="#/linha/${l.id}" style="--cor:${cor(l)}"><strong>${esc(l.nome)}</strong>
      <small>${esc(l.descricao)} · ${grade.estacoesDaLinha(l).length ? `${grade.estacoesDaLinha(l).length} estações` : "não opera hoje"}</small></a>`).join("")}</div>`;
  tela.innerHTML = html;
  document.getElementById("localizar")?.addEventListener("click", localizar);
  document.getElementById("instalar")?.addEventListener("click", instalar);
}

// ---------- Casa e Trabalho (a mesma ideia do app)

const LUGARES = [
  { chave: "casa", nome: "Casa", emoji: "🏠", pergunta: "Qual estação fica perto da sua casa?" },
  { chave: "trabalho", nome: "Trabalho", emoji: "💼", pergunta: "Qual estação fica perto do seu trabalho?" },
];

/** { casa: "linha|estação", trabalho: … }: só a estação, guardada neste navegador. */
function lerLugares() {
  try { return JSON.parse(localStorage.getItem("lugares") || "{}"); } catch { return {}; }
}

function salvarLugar(chave, valor) {
  const l = lerLugares();
  if (valor) l[chave] = valor; else delete l[chave];
  try { localStorage.setItem("lugares", JSON.stringify(l)); } catch { /* segue */ }
}

/**
 * Cada lugar com a estação de hoje: o nome, a chegada de quem sai agora da
 * estação de origem, ou "aqui" (já é a estação de onde se sai).
 */
function infoLugares(t, origem = origemPadrao()) {
  const { grade, regras } = estado.dados;
  const salvos = lerLugares();
  const [lo, eo] = origem ? origem.split("|") : [];
  const dia = diaDaViagem(t);
  const r = lo && dia && !dia.domingo ? new Roteador(grade, regras, perfilGuardado()) : null;
  return LUGARES.map((lugar) => {
    const [ld, ed] = (salvos[lugar.chave] || "").split("|");
    if (!grade.linha(ld) || !grade.estacoes[ed]) return { ...lugar, valor: null };
    const nome = semSufixo(grade.nome(ed));
    const aqui = Boolean(eo) && semSufixo(grade.nome(eo)) === nome;
    const o = !aqui && r && !linhaParada(regras, dia.data, grade.linha(ld).nome)
      ? r.planejar({ linha: lo, est: eo }, { linha: ld, est: ed }, t.minutos - 2, 1, dia.evitar)[0] : null;
    return { ...lugar, valor: `${ld}|${ed}`, estacao: nome, aqui, chega: o ? o.chega : null };
  });
}

/** Os atalhos debaixo de "Para onde você vai?"; o lugar onde a pessoa está vai para o fim. */
function atalhosLugares(t) {
  // Sem localização não se sabe de onde a pessoa sai: só o nome da estação.
  const lugares = infoLugares(t, estado.pos ? origemPadrao() : "").sort((a, b) => Boolean(a.aqui) - Boolean(b.aqui));
  const algum = lugares.some((l) => l.valor);
  return `<div class="lugares">${lugares.map((l) => !l.valor
    ? `<a class="lugar vazio" href="#/lugar/${l.chave}"><span class="emoji">${l.emoji}</span>Definir ${l.nome}</a>`
    : `<a class="lugar ${l.aqui ? "" : "cheio"}" href="#/ir/${l.chave}"><span class="emoji">${l.emoji}</span>
        <span><strong>${l.nome}</strong><small>${[l.aqui ? "" : esc(l.estacao),
          l.aqui ? "você está aqui" : l.chega != null ? `chega ${hora(l.chega)}` : ""].filter(Boolean).join(" · ")}</small></span></a>`,
  ).join("")}${algum ? `<a class="trocar-lugar" href="#/lugar/${lugares.find((l) => l.valor).chave}">Trocar</a>` : ""}</div>`;
}

/** Um toque no atalho: o roteiro até lá, saindo da estação mais perto (já nela, a lista). */
function irParaLugar(chave) {
  const l = infoLugares(agora()).find((x) => x.chave === chave);
  if (!l?.valor) { location.replace(`#/lugar/${chave}`); return; }
  estado.viagem = { destino: l.aqui ? "" : l.valor, busca: "", hora: null, escolhido: 0 };
  location.replace("#/viagem");
}

/** Escolher (ou apagar) a estação de Casa ou de Trabalho. */
function telaLugar(chave) {
  const { grade } = estado.dados;
  const lugar = LUGARES.find((l) => l.chave === chave) || LUGARES[0];
  const atual = lerLugares()[lugar.chave];
  const outro = LUGARES.find((l) => l.chave !== lugar.chave);
  tela.innerHTML = `<h2 style="font-size:1.4rem">${lugar.emoji} ${lugar.pergunta}</h2>
    <p class="dica">O app guarda só a estação, não o seu endereço. Fica só neste navegador.</p>
    <div class="chips">${atual ? `<button class="chip" id="l-nenhuma">Nenhuma</button>` : ""}
      <a class="chip" href="#/lugar/${outro.chave}">${outro.emoji} Trocar ${outro.nome}</a>
      <a class="chip" href="#/">Voltar</a></div>
    <label class="campo"><input id="l-busca" type="search" autocomplete="off" placeholder="Digite o nome da estação" style="width:100%"></label>
    <div id="l-lista"></div>`;
  const lista = document.getElementById("l-lista");
  const mostrar = (busca) => {
    const agulha = semAcento(busca.trim());
    lista.innerHTML = grade.linhas.map((l) => {
      const ests = grade.estacoesDaLinha(l).filter((id) => !agulha || semAcento(semSufixo(grade.nome(id))).includes(agulha));
      return ests.length ? `<p class="grupo"><span class="ponto" style="background:${cor(l)}"></span>${esc(l.nome)}</p>
        ${ests.map((id) => `<button class="destino" data-v="${l.id}|${id}"><span>${esc(semSufixo(grade.nome(id)))}</span>
          ${atual === `${l.id}|${id}` ? "<small>escolhida</small>" : ""}</button>`).join("")}` : "";
    }).join("") || `<p class="vazio">Nenhuma estação com esse nome.</p>`;
    lista.querySelectorAll("button[data-v]").forEach((b) => b.addEventListener("click", () => {
      salvarLugar(lugar.chave, b.dataset.v);
      location.replace("#/");
    }));
  };
  const busca = document.getElementById("l-busca");
  busca.addEventListener("input", () => mostrar(busca.value));
  document.getElementById("l-nenhuma")?.addEventListener("click", () => {
    salvarLugar(lugar.chave, null);
    location.replace("#/");
  });
  mostrar("");
}

/**
 * A versão lite é a porta de entrada: uma linha fixa no topo convida a ser
 * testador do app completo, com o WhatsApp e a mensagem pronta. Fora do
 * Android, pergunta antes se a pessoa tem Android.
 */
function convite() {
  const texto = ANDROID ? "Seja testador do app completo" : "Tem Android? Seja testador do app completo";
  return `<a class="convite" href="${WHATSAPP_TESTADOR}" target="_blank" rel="noopener">📲 ${texto} ›</a>`;
}

async function instalar() {
  if (!pedidoInstalar) return;
  pedidoInstalar.prompt();
  await pedidoInstalar.userChoice.catch(() => null);
  pedidoInstalar = null;
  render();
}

function lerNumero(chave) {
  try { return Number(localStorage.getItem(chave)) || 0; } catch { return 0; }
}

function telaLinha(linhaId) {
  const { grade } = estado.dados;
  const linha = grade.linha(linhaId);
  if (!linha) return telaNaoAchei();
  tela.innerHTML = `<h2><span class="ponto" style="background:${cor(linha)}"></span> ${esc(linha.nome)}</h2>
    <p class="dica">${esc(linha.descricao)}</p>
    <div class="lista-estacoes">${grade.estacoesDaLinha(linha).map((id) =>
      `<a href="#/estacao/${linha.id}/${id}">${esc(grade.nome(id))}</a>`).join("")}</div>
    ${grade.estacoesDaLinha(linha).length ? "" : `<p class="vazio">Esta linha não opera hoje.</p>`}`;
}

function telaEstacao(linhaId, stopId, sentidoId) {
  const { grade } = estado.dados;
  const linha = grade.linha(linhaId);
  if (!linha || !grade.estacoes[stopId]) return telaNaoAchei();
  const t = agora();
  const sentidos = grade.sentidosDePartida(linha, stopId);
  const sentido = sentidos.find((s) => String(s.id) === sentidoId) || sentidos[0];
  const { hoje, amanha } = partidasComAmanha(linha, sentido, stopId);
  const lista = proximas(hoje, t.minutos, PROXIMAS, amanha);
  const domingo = domingoSemOperacao(t);

  let html = `<h2 style="font-size:1.4rem;margin-bottom:0">${esc(grade.nome(stopId))}</h2>
    <p class="dica" style="margin-top:2px"><span class="ponto" style="background:${cor(linha)}"></span> ${esc(linha.nome)}</p>`;
  const info = infoEstacao(estado.dados.regras, stopId, grade.nome(stopId));
  const endereco = info?.endereco;
  const tarifa = passagemDaLinha(estado.dados.regras, linha.nome);
  if (endereco || tarifa) {
    html += `<p class="dica">${[endereco && esc(endereco), tarifa && `Passagem: ${esc(tarifa)}`].filter(Boolean).join("<br>")}</p>`;
    html += fonte("passagem", "margin-top:-8px");
  }
  // Meia, gratuidade e onde tirar o cartão: o guia do regras.json.
  if (tarifa && estado.dados.regras?.guia_passagem) {
    html += `<p style="margin-top:-6px"><a class="link-guia" href="#/passagem">Meia, gratuidade e onde tirar o cartão ›</a></p>`;
  }
  // Aviso livre da estação (regras.json), como o de estação subterrânea.
  if (info?.aviso) html += `<p class="aviso">${esc(info.aviso)}</p>${info.subterranea ? fonte("subterranea", "margin-top:-6px") : ""}`;
  html += linhaAcesso(info?.acesso, `#/acesso/${linha.id}/${stopId}`);
  if (sentidos.length > 1) {
    html += `<nav class="abas">${sentidos.map((s) =>
      `<a href="#/estacao/${linha.id}/${stopId}/${s.id}" class="${s === sentido ? "ativa" : ""}">→ ${esc(s.destino)}</a>`).join("")}</nav>`;
  } else {
    html += `<p><strong>→ ${esc(sentido.destino)}</strong></p>`;
  }
  // Domingo comum: a lista é a de segunda, então vale também a notícia de amanhã.
  html += avisoDomingo(t) + avisosMetrofor(!domingo) + avisoParada(linha, t);
  html += `<h2>Próximas partidas</h2>`;
  if (!lista.length) {
    const fim = fimDoDia(linha, sentido, stopId);
    const vazio = sentido.naoPublicado ? "O Metrofor não publicou os horários deste sentido hoje."
      : domingo ? "Sem partidas hoje." : "Sem mais partidas hoje.";
    html += `<p class="vazio">${vazio}</p>${fim ? `<p class="dica">${fim}</p>` : ""}`;
  } else {
    html += `<div class="cartao proximas" style="padding:4px 0">${lista.map((p, i) =>
      `<button data-m="${p.m}" data-amanha="${p.amanha}">
        <span><span class="hora">${hora(p.m)}</span>${p.amanha ? "<small>amanhã</small>" : ""}</span>
        <span>${falta(minutosAte(p, t.minutos))}</span></button>`).join("")}</div>`;
  }
  html += domingo
    ? `<p class="dica">Toque em qualquer horário (aqui ou em "Horários de segunda", mais abaixo) para criar um lembrete na agenda.</p>`
    : `<p class="dica">Toque em qualquer horário de hoje (aqui ou em "Todos os horários", mais abaixo) para criar um lembrete na agenda.</p>`;
  html += avisoBike(linha, stopId, t);
  html += blocoIntegracao(stopId, t);
  html += `<a class="botao secundario largo" href="#/viagem/${linha.id}/${stopId}">${LUPA} Ir daqui para outra estação</a>`;
  html += blocoPerto(stopId);
  if (domingo) {
    // Domingo comum: a grade de segunda, toda futura (o lembrete vai para amanhã).
    html += amanha?.length ? `<h2>Horários de segunda</h2><div class="todos">${amanha.map((m) =>
      `<button data-m="${m}" data-amanha="true">${hora(m)}</button>`).join("")}</div>` : "";
  } else {
    html += `<h2>Todos os horários</h2><div class="todos">${hoje.map((m) => {
      const passou = m < t.minutos;
      return `<button data-m="${m}" data-amanha="false" class="${passou ? "passou" : ""}" ${passou ? "disabled" : ""}>${hora(m)}</button>`;
    }).join("")}</div>`;
  }
  tela.innerHTML = html;

  tela.querySelectorAll("button[data-m]:not([disabled])").forEach((b) => b.addEventListener("click", () =>
    lembrete(linha, sentido, stopId, +b.dataset.m, b.dataset.amanha === "true")));
}

// ---------- acessibilidade da estação (regras.json, "acesso"), como no app

const WHATSAPP_RELATO = "https://wa.me/5585997619295?text=";
const EMAIL_RELATO = "giovanildos@gmail.com";

/** Como a cadeira de rodas chega à plataforma: elevador, plataforma elevatória, rampa ou só escada. */
function formaDeAcesso(a) {
  return a.elevador > 0 ? "elevador" : a.plataforma_elevatoria > 0 ? "plataforma" : a.rampa > 0 ? "rampa" : "escada";
}

/** "2 elevadores e escada rolante", "Acesso por rampa"; null sem elevador nem rampa. */
function resumoAcesso(a) {
  const f = formaDeAcesso(a);
  if (f === "elevador") {
    const e = `${a.elevador} ${a.elevador === 1 ? "elevador" : "elevadores"}`;
    return a.escada_rolante > 0 ? `${e} e escada rolante` : e;
  }
  return f === "rampa" ? "Acesso por rampa" : null;
}

/**
 * Na tela da estação: com elevador ou rampa, uma linha curta; sem elevador, o aviso,
 * para todos (quem leva carrinho de bebê também precisa saber). Sem dados, só para
 * quem escolheu "cadeira de rodas".
 */
function linhaAcesso(a, href) {
  if (!a) {
    return perfilGuardado() === "cadeira_de_rodas"
      ? `<p class="dica">♿ Ainda não temos os dados de elevador e rampa desta estação. <a href="${href}">Você sabe? Conte pra gente ›</a></p>` : "";
  }
  const f = formaDeAcesso(a);
  if (f === "plataforma" || f === "escada") {
    return `<p class="aviso">♿ ${f === "plataforma"
      ? "<strong>Sem elevador.</strong> Para chegar à plataforma há uma plataforma elevatória, ligada por um funcionário. Peça na bilheteria."
      : "<strong>Sem elevador nem rampa.</strong> Peça ajuda a um funcionário da estação."}
      <br><a href="${href}"><strong>Elevador, rampa e escada ›</strong></a></p>`;
  }
  return `<p class="dica"><a href="${href}">♿ ${esc(resumoAcesso(a))} · ver tudo ›</a></p>`;
}

/** Elevador, plataforma elevatória, rampa e escada da estação, quem pode usar e o "Conte pra gente". */
function telaAcesso(linhaId, stopId) {
  const { grade, regras } = estado.dados;
  const linha = grade.linha(linhaId);
  if (!linha || !grade.estacoes[stopId]) return telaNaoAchei();
  const nome = semSufixo(grade.nome(stopId));
  const a = infoEstacao(regras, stopId, grade.nome(stopId))?.acesso;
  const item = (nomeItem, n, desc = "") => `<p class="item-acesso"><span><strong>${nomeItem}</strong>${desc ? `<br><span class="dica">${desc}</span>` : ""}</span>
    <strong class="${n > 0 ? "" : "nao-tem"}">${n > 0 ? n : "não tem"}</strong></p>`;
  let html = `<h2 style="font-size:1.4rem;margin-bottom:0">Acessibilidade</h2>
    <p class="dica" style="margin-top:2px"><span class="ponto" style="background:${cor(linha)}"></span> ${esc(nome)} · ${esc(linha.nome)}</p>`;
  if (!a) {
    html += `<p>Ainda não temos os dados de elevador e rampa desta estação.</p>`;
  } else {
    html += `<div class="tarifas">${item("🛗 Elevador", a.elevador)}
      ${a.plataforma_elevatoria > 0 ? item("♿ Plataforma elevatória", a.plataforma_elevatoria,
        "Sobe a escada com a cadeira de rodas. Um funcionário liga e acompanha: peça na bilheteria.") : ""}
      ${item("↗ Rampa", a.rampa)}${item("🪜 Escada", a.escada)}${item("⇅ Escada rolante", a.escada_rolante)}</div>`;
  }
  html += `<div class="aviso quadro"><strong>Dentro do metrô e do VLT</strong><br>Todos têm espaço para cadeira de rodas, com cinto de segurança.</div>
    <div class="aviso quadro"><strong>Quem pode usar o elevador e a plataforma</strong><br>Pessoa com deficiência ou mobilidade reduzida,
      idoso, gestante e quem está em situação de fragilidade, como quem leva carrinho de bebê. Os outros vão pela escada.</div>`;
  const msg = `Metrô Ceará · acessibilidade da estação ${nome} (${linha.nome}): `;
  html += `<h2>${a ? "Encontrou algo diferente?" : "Você sabe como é?"}</h2>
    <p class="dica">Elevador parado, plataforma sem funcionário, rampa bloqueada… Conte pra gente: ajuda quem vem depois.</p>
    <a class="botao largo" href="${WHATSAPP_RELATO}${encodeURIComponent(msg)}" target="_blank" rel="noopener">Contar pelo WhatsApp</a>
    <a class="botao secundario largo" href="mailto:${EMAIL_RELATO}?subject=${encodeURIComponent(`Acessibilidade: ${nome}`)}&body=${encodeURIComponent(msg)}">Contar por e-mail</a>`;
  if (a) html += fonte("acessibilidade") + `<p class="dica">Um elevador pode estar parado para manutenção.</p>`;
  html += `<p><a href="#/estacao/${linha.id}/${stopId}">‹ ${esc(nome)}</a></p>`;
  tela.innerHTML = html;
}

/** Com cadeira de rodas: onde o roteiro embarca, troca de linha e desce, e como se chega à plataforma. */
function acessoDoRoteiro(o, grade) {
  const { regras } = estado.dados;
  const ids = [o.pernas[0].de, ...o.trocas.flatMap((x) => [x.lig.deEst, x.lig.paraEst]), o.pernas.at(-1).para];
  const ests = [...new Set(ids)].map((id) => ({ nome: semSufixo(grade.nome(id)), a: infoEstacao(regras, id, grade.nome(id))?.acesso }));
  const avisos = [...new Set(ests.map(({ nome, a }) => !a ? `${nome}: ainda não temos os dados de elevador e rampa.`
    : formaDeAcesso(a) === "plataforma" ? `${nome} não tem elevador: peça a plataforma elevatória na bilheteria.`
    : formaDeAcesso(a) === "escada" ? `${nome} não tem elevador nem rampa: peça ajuda a um funcionário.` : null).filter(Boolean))];
  // Uma linha por estação: na integração, vale a primeira das duas plataformas de mesmo nome.
  const vistos = new Set();
  const notas = ests.filter(({ nome }) => !vistos.has(nome) && vistos.add(nome))
    .map(({ nome, a }) => a && resumoAcesso(a) && `${nome}: ${resumoAcesso(a).replace(/^./, (c) => c.toLowerCase())}`).filter(Boolean);
  return avisos.map((x) => `<p class="aviso">♿ ${esc(x)}</p>`).join("")
    + (notas.length ? `<p class="dica">♿ ${esc(notas.join(" · "))}</p>` : "");
}

/** "Perto desta estação": hospital, universidade, shopping a uma caminhada curta (regras.json). */
function blocoPerto(stopId) {
  const { grade, regras } = estado.dados;
  const e = grade.estacoes[stopId];
  const perto = e ? pertoDaEstacao(regras, grade.nome(stopId), e.lat, e.lon).slice(0, 8) : [];
  if (!perto.length) return "";
  const fator = fatorPerfil();
  return `<h2>Perto desta estação</h2><div class="perto">${perto.map((l) =>
    `<div class="destino"><span>${emojiLugar(l)} ${esc(l.nome)}</span><small>${caminhada(l, fator)}</small></div>`).join("")}</div>
    <p class="dica">Distância a pé pelas ruas, aproximada.</p>${fonte("lugares", "margin-top:-8px")}`;
}

/** Quantas vezes o tempo a pé, pelo jeito de se deslocar escolhido (o mesmo do roteiro). */
function fatorPerfil() {
  const perfil = perfilGuardado();
  return estado.dados.regras?.deslocamento?.[perfil] ?? PERFIS.find((p) => p.chave === perfil)?.fator ?? 1;
}

// ---------- roteiro com integração (a mesma regra do app: roteiro.js)

const semSufixo = (nome) => nome.replace(/\s+-\s+(AE|NE)$/i, "");

function perfilGuardado() {
  try { return localStorage.getItem("perfil") || "andando"; } catch { return "andando"; }
}

const LUPA = `<svg class="lupa" viewBox="0 0 24 24" aria-hidden="true"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>`;

/** "Benfica" acha "Benfica", "anto" acha "Antônio Sales": sem acento nem maiúscula. */
const semAcento = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

/** A estação de onde a pessoa sai: a mais perto (com localização) ou a última usada. */
function origemPadrao() {
  const { grade } = estado.dados;
  if (estado.pos) {
    let melhor = null;
    for (const l of grade.linhas) {
      for (const id of grade.estacoesDaLinha(l)) {
        const e = grade.estacoes[id];
        const d = distancia(estado.pos.lat, estado.pos.lon, e.lat, e.lon);
        if (!melhor || d < melhor.d) melhor = { v: `${l.id}|${id}`, d };
      }
    }
    if (melhor) return melhor.v;
  }
  try {
    const o = localStorage.getItem("origem") || "";
    const [l, e] = o.split("|");
    if (grade.linha(l) && grade.estacoes[e]) return o;
  } catch { /* segue */ }
  return "";
}

/**
 * O dia do roteiro: hoje, ou a segunda num domingo sem operação anunciada (grade
 * de segunda, sem linhas paradas amanhã). null enquanto a grade de segunda não chega.
 */
function diaDaViagem(t) {
  const domingo = domingoSemOperacao(t);
  const grade = domingo ? estado.proximo : estado.dados.grade;
  if (!grade) return null;
  const data = domingo ? somarDias(t.data, 1) : t.data;
  const evitar = new Set(grade.linhas.filter((l) => linhaParada(estado.dados.regras, data, l.nome)).map((l) => l.id));
  return { domingo, grade, data, diaSemana: domingo ? 1 : t.diaSemana, evitar };
}

/**
 * "Para onde você vai?": de onde (a estação mais perto, já escolhida), para onde
 * (busca sem acento, as estações por linha) e o roteiro, na mesma linha ou com
 * integração, saindo agora ou no horário escolhido. No lugar do modo viagem, que
 * só o app completo tem, o lembrete na agenda.
 */
function telaViagem(linhaId, estId) {
  const { grade, regras } = estado.dados;
  const t = agora();
  const v = estado.viagem ||= { destino: "", busca: "", hora: null, escolhido: 0, lugar: null, nota: null };
  const origem = linhaId && grade.linha(linhaId) && grade.estacoes[estId] ? `${linhaId}|${estId}` : origemPadrao();
  if (origem) try { localStorage.setItem("origem", origem); } catch { /* segue */ }
  const [lo, eo] = origem.split("|");
  const nome = (id) => semSufixo(grade.nome(id));

  const opcoesOrigem = grade.linhas.filter((l) => grade.estacoesDaLinha(l).length).map((l) =>
    `<optgroup label="${esc(l.nome)}">${grade.estacoesDaLinha(l).map((id) =>
      `<option value="${l.id}|${id}" ${origem === `${l.id}|${id}` ? "selected" : ""}>${esc(nome(id))}</option>`).join("")}</optgroup>`).join("");
  const destino = v.destino && v.destino.split("|");
  tela.innerHTML = `<h2 style="font-size:1.4rem">Sua viagem</h2>
    <div class="viagem-campos">
      <label class="campo">De<select id="v-origem">${origem ? "" : `<option value="">Escolha a estação</option>`}${opcoesOrigem}</select></label>
      <label class="campo">Para<input id="v-busca" type="search" autocomplete="off" placeholder="Estação ou lugar (ex.: Rodoviária)"
        value="${esc(v.lugar || v.nota || (destino ? nome(destino[1]) : v.busca))}"></label>
    </div>
    <div id="v-lista"></div><div id="v-resultado"></div>`;

  const lista = document.getElementById("v-lista");
  const resultado = document.getElementById("v-resultado");
  const mostrarLista = () => {
    resultado.innerHTML = "";
    lista.innerHTML = listaDestinos(lo, eo, v.busca, t);
    lista.querySelectorAll("button[data-dest]").forEach((b) => b.addEventListener("click", () => {
      v.destino = b.dataset.dest;
      v.lugar = b.dataset.lugar || null;
      v.nota = null;
      v.busca = "";
      v.escolhido = 0;
      render();
    }));
    // Lugar sem roteiro (longe dos trilhos, outra cidade, a pé daqui): só a explicação.
    lista.querySelectorAll("button[data-nota]").forEach((b) => b.addEventListener("click", () => {
      v.nota = b.dataset.nota;
      v.destino = "";
      v.lugar = null;
      v.busca = "";
      render();
    }));
  };
  const mostrarResultado = () => {
    lista.innerHTML = "";
    resultado.innerHTML = resultadoViagem(lo, eo, v, t);
    resultado.querySelectorAll("button[data-i]").forEach((b) => b.addEventListener("click", () => {
      v.escolhido = +b.dataset.i;
      mostrarResultado();
    }));
    resultado.querySelector("#v-agora")?.addEventListener("click", () => { v.hora = null; v.escolhido = 0; mostrarResultado(); });
    resultado.querySelector("#v-hora")?.addEventListener("change", (e) => {
      const [h, m] = e.target.value.split(":").map(Number);
      v.hora = Number.isFinite(h) ? h * 60 + m : null;
      v.escolhido = 0;
      mostrarResultado();
    });
    resultado.querySelector("#perfil")?.addEventListener("change", (e) => {
      try { localStorage.setItem("perfil", e.target.value); } catch { /* segue */ }
      mostrarResultado();
    });
    resultado.querySelector("#v-lembrete")?.addEventListener("click", (e) => {
      const { linha, sentido, est, m, amanha } = JSON.parse(e.currentTarget.dataset.p);
      const g = diaDaViagem(agora())?.grade || grade;
      lembrete(g.linha(linha), g.linha(linha).sentidos.find((x) => x.id === sentido), est, m, amanha);
    });
  };

  document.getElementById("v-origem").addEventListener("change", (e) => {
    const [l, id] = e.target.value.split("|");
    location.replace(`#/viagem/${l}/${id}`);
  });
  const busca = document.getElementById("v-busca");
  busca.addEventListener("focus", () => {
    if (v.destino || v.nota) { v.destino = ""; v.lugar = null; v.nota = null; busca.value = ""; mostrarLista(); }
  });
  busca.addEventListener("input", () => { v.busca = busca.value; v.destino = ""; v.lugar = null; v.nota = null; mostrarLista(); });
  if (v.nota) { lista.innerHTML = ""; resultado.innerHTML = notaLugar(v.nota, eo); }
  else if (v.destino && origem) mostrarResultado(); else mostrarLista();
}

/** As estações para onde dá para ir, por linha; com origem, o caminho e a hora de chegada saindo agora. */
function listaDestinos(lo, eo, busca, t) {
  const { grade, regras } = estado.dados;
  const agulha = semAcento(busca.trim());
  const r = lo ? new Roteador(grade, regras, perfilGuardado()) : null;
  const alcance = r ? r.alcancaveis(lo) : null;
  const dia = diaDaViagem(t);
  // Hora de chegada só para quem sai hoje: num domingo sem metrô, não há "agora".
  const rotas = dia && !dia.domingo && r ? r : null;
  const grupos = grade.linhas.filter((l) => !alcance || alcance.has(l.id)).map((l) => {
    const caminho = alcance?.get(l.id) || [];
    const chegaA = caminho.length ? caminho[caminho.length - 1].paraEst : null; // ali ainda nem embarcou
    const parada = linhaParada(regras, dia?.data ?? t.data, l.nome);
    const via = !lo ? "" : !caminho.length ? " · mesma linha"
      : caminho.length === 1 ? ` · integração em ${semSufixo(grade.nome(caminho[0].deEst))}` : ` · ${caminho.length} integrações`;
    const ests = grade.estacoesDaLinha(l).filter((id) => id !== chegaA && !(l.id === lo && id === eo))
      .filter((id) => !agulha || semAcento(semSufixo(grade.nome(id))).includes(agulha));
    if (!ests.length) return "";
    return `<p class="grupo"><span class="ponto" style="background:${cor(l)}"></span>${esc(l.nome)}${parada ? " · não circula" : via}</p>
      ${ests.map((id) => {
        const o = !parada && rotas?.planejar({ linha: lo, est: eo }, { linha: l.id, est: id }, t.minutos - 2, 1, dia.evitar)[0];
        return `<button class="destino" data-dest="${l.id}|${id}" ${parada ? "disabled" : ""}>
          <span>${esc(semSufixo(grade.nome(id)))}</span>${o ? `<small>chega ${hora(o.chega)}</small>` : ""}</button>`;
      }).join("")}`;
  }).join("");
  // Casa e Trabalho no topo, enquanto a busca está vazia.
  const lugares = agulha || !lo ? [] : infoLugares(t, `${lo}|${eo}`).filter((l) => l.valor);
  const topo = lugares.length ? `<p class="grupo">Seus lugares</p>${lugares.map((l) =>
    `<button class="destino" ${l.aqui ? "disabled" : `data-dest="${l.valor}"`}><span>${l.emoji} ${l.nome} · ${esc(l.estacao)}</span>
      <small>${l.aqui ? "você está aqui" : l.chega != null ? `chega ${hora(l.chega)}` : ""}</small></button>`).join("")}` : "";
  const achados = lugaresAchados(lo, eo, busca, t, r, alcance, rotas, dia);
  const blocoLugares = achados.length ? `<p class="grupo">Lugares</p>${achados.map(({ l, alvo, aqui, chega }) => {
    const fator = fatorPerfil();
    const estacao = `Estação ${esc(l.estacao)} · ${longe(l) ? metrosAPe(l.a_pe_m) : caminhada(l, fator)}`;
    const detalhe = [l.bairro || l.cidade, estacao].filter(Boolean).join(" · ");
    const direita = longe(l) ? "longe dos trilhos" : aqui ? "você está aqui" : !alvo ? "não dá para chegar daqui"
      : chega != null ? `chega ${hora(chega)}` : "";
    const acao = alvo && !longe(l) ? `data-dest="${alvo}" data-lugar="${esc(l.nome)}"` : `data-nota="${esc(l.nome)}"`;
    return `<button class="destino lugar-achado" ${acao}><span>${emojiLugar(l)} ${esc(l.nome)}
      <small class="${longe(l) || l.a_pe_m > 1000 ? "alerta" : ""}">${detalhe}</small></span><small>${direita}</small></button>`;
  }).join("")}` : "";
  return topo + (grupos || blocoLugares ? grupos + blocoLugares : `<p class="vazio">Nenhuma estação ou lugar com esse nome.</p>`);
}

/**
 * Os lugares achados pela busca, cada um com a estação de destino ("linha|estação"):
 * na linha da origem, se der (Parangaba tem metrô e VLT), senão a de chegada mais
 * cedo. Sem estação alcançável, outra cidade; com a da origem, a pé daqui.
 */
function lugaresAchados(lo, eo, busca, t, r, alcance, rotas, dia) {
  const { grade, regras } = estado.dados;
  const fator = fatorPerfil();
  return buscarLugares(regras, busca).map((l) => {
    const servidas = Object.keys(grade.estacoes).filter((id) => {
      const e = grade.estacoes[id];
      return serve(l, grade.nome(id), e.lat, e.lon);
    });
    const aqui = !!eo && servidas.some((id) => semSufixo(grade.nome(id)) === semSufixo(grade.nome(eo)));
    let melhor = null;
    if (lo && !aqui) {
      for (const linha of grade.linhas) {
        if (alcance && !alcance.has(linha.id)) continue;
        if (linhaParada(regras, dia?.data ?? t.data, linha.nome)) continue;
        for (const id of grade.estacoesDaLinha(linha).filter((x) => servidas.includes(x))) {
          const o = rotas?.planejar({ linha: lo, est: eo }, { linha: linha.id, est: id }, t.minutos - 2, 1, dia.evitar)[0];
          const nota = linha.id === lo ? -1 : o ? o.chega : Infinity;
          if (!melhor || nota < melhor.nota) melhor = { alvo: `${linha.id}|${id}`, chega: o ? o.chega + minutosAPe(l, fator) : null, nota };
        }
      }
    }
    return { l, aqui, alvo: melhor?.alvo || null, chega: melhor?.chega ?? null };
  }).sort((a, b) => (!a.alvo && !a.aqui) - (!b.alvo && !b.aqui)); // os de outra cidade no fim
}

/** Por que o lugar não tem roteiro: longe dos trilhos, a pé daqui ou fora do alcance desta rede. */
function notaLugar(nome, eo) {
  const { grade, regras } = estado.dados;
  const l = buscarLugares(regras, nome, 50).find((x) => x.nome === nome);
  if (!l) return "";
  const fator = fatorPerfil();
  const origem = eo ? semSufixo(grade.nome(eo)) : "";
  const texto = longe(l)
    ? `<strong>Nenhuma estação fica perto deste lugar.</strong> A mais perto é a estação ${esc(l.estacao)}, a ${metrosAPe(l.a_pe_m)}:
      uns ${minutosAPe(l, fator)} minutos a pé. O metrô e o VLT não são a melhor opção para chegar lá.`
    : origem && semAcento(origem) === semAcento(l.estacao)
      ? `${esc(l.nome)} fica perto daqui: ${caminhada(l, fator)}, saindo da estação ${esc(l.estacao)}. Não precisa pegar o metrô.`
      : `${esc(l.nome)} fica em ${esc(l.cidade || l.estacao)}. Saindo de ${esc(origem)}, o metrô e o VLT não chegam lá.`;
  return `<div class="cartao neutro"><p style="margin:0"><strong>${emojiLugar(l)} ${esc(l.nome)}</strong></p><p>${texto}</p></div>`;
}

/** O roteiro escolhido: horário (agora ou escolhido), o cartão, os outros horários e o lembrete. */
function resultadoViagem(lo, eo, v, t) {
  const { regras } = estado.dados;
  const dia = diaDaViagem(t);
  if (!dia) {
    return estado.dados.proximoEspecial
      ? `<p class="vazio">Segunda o horário é especial e só sai depois da meia-noite.</p>`
      : `<p class="vazio">Carregando os horários de segunda…</p>`;
  }
  const { grade } = dia;
  const [ld, ed] = v.destino.split("|");
  const perfil = perfilGuardado();
  const r = new Roteador(grade, regras, perfil);
  const apos = v.hora ?? (dia.domingo ? 0 : t.minutos - 2);
  const opcoes = r.planejar({ linha: lo, est: eo }, { linha: ld, est: ed }, apos, 4, dia.evitar);
  const lugar = v.lugar ? buscarLugares(regras, v.lugar, 50).find((x) => x.nome === v.lugar) : null;
  const ape = lugar ? { nome: lugar.nome, metros: lugar.a_pe_m, min: minutosAPe(lugar, fatorPerfil()), aoLado: aoLado(lugar) } : null;
  const nomeDestino = lugar?.nome || semSufixo(grade.nome(ed));
  const horaValor = v.hora != null ? hora(v.hora) : "";
  let html = dia.domingo ? `<p class="aviso"><strong>Hoje é domingo, sem metrô e VLT.</strong> Este é o roteiro de segunda.</p>` : "";
  html += `<div class="chips">
    <button class="chip ${v.hora == null ? "sel" : ""}" id="v-agora">${dia.domingo ? "Primeiro horário" : "Sair agora"}</button>
    <label class="chip ${v.hora != null ? "sel" : ""}">${v.hora != null ? "Sair às" : "Escolher horário"}
      <input type="time" id="v-hora" value="${horaValor}"></label></div>`;
  if (!opcoes.length) {
    const ultimo = r.planejar({ linha: lo, est: eo }, { linha: ld, est: ed }, 0, 99, dia.evitar).map((o) => o.sai).sort((a, b) => b - a)[0];
    const quando = dia.domingo ? "na segunda" : "hoje";
    html += `<p class="vazio">${ultimo == null ? `Não há como chegar a ${esc(nomeDestino)} ${quando} saindo daqui.`
      : `Sem metrô ou VLT para ${esc(nomeDestino)} ${quando} depois das ${hora(ultimo)}.`}</p>`;
    return html;
  }
  const i = Math.min(v.escolhido, opcoes.length - 1);
  const o = opcoes[i];
  html += cartaoRoteiro(o, grade, dia, perfil, ape);
  if (opcoes.length > 1) {
    html += `<div class="chips"><span class="dica">Outros horários:</span>${opcoes.map((x, k) =>
      `<button class="chip ${k === i ? "sel" : ""}" data-i="${k}">${hora(x.sai)} → ${hora(x.chega + (ape?.min || 0))}</button>`).join("")}</div>`;
  }
  // No lugar do modo viagem: o lembrete da partida, para quem ainda não saiu.
  if (dia.domingo || o.sai >= t.minutos) {
    const p = o.pernas[0];
    const dados = esc(JSON.stringify({ linha: p.linha, sentido: p.sentido.id, est: p.de, m: o.sai, amanha: dia.domingo }));
    html += `<button class="botao secundario largo" id="v-lembrete" data-p="${dados}">⏰ Lembrete na agenda${dia.domingo ? `, segunda ${hora(o.sai)}` : ""}</button>`;
  }
  html += `<label class="campo">Como você se desloca<select id="perfil">${PERFIS.map((x) =>
    `<option value="${x.chave}" ${x.chave === perfil ? "selected" : ""}>${esc(x.nome)}</option>`).join("")}</select></label>`;
  html += `<p class="dica">${ANDROID ? "No" : "Tem Android? No"} app completo, o <strong>modo viagem</strong> avisa antes da sua estação,
    com a tela desligada. <a href="${WHATSAPP_TESTADOR}" target="_blank" rel="noopener">Seja testador ›</a></p>`;
  return html;
}

/** O cartão do roteiro: cada trecho na cor da linha, a integração com tempo e passagem, a chegada. */
function cartaoRoteiro(o, grade, dia, perfil, ape = null) {
  const { regras } = estado.dados;
  const rotulo = (l) => l.nome.startsWith("Linha") ? `${veiculo(l.nome).replace(/^./, (c) => c.toUpperCase())} da ${l.nome}` : l.nome;
  let html = `<div class="cartao neutro roteiro">`;
  o.pernas.forEach((p, i) => {
    const l = grade.linha(p.linha);
    html += `<div class="perna" style="--cor:${cor(l)}"><strong>${hora(p.sai)} ${esc(semSufixo(grade.nome(p.de)))}</strong>
      <span>${esc(rotulo(l))}, sentido ${esc(p.sentido.destino)}. Desça em ${esc(semSufixo(grade.nome(p.para)))} às ${hora(p.chega)}.</span></div>`;
    const x = o.trocas[i];
    if (x) {
      const tarifa = passagem(regras, grade.nome(x.lig.deEst), grade.nome(x.lig.paraEst));
      const t2 = tarifa.tipo === "gratis" ? " · passagem grátis" : tarifa.tipo === "paga" ? " · passagem paga" : "";
      const justa = x.sobra <= 1;
      html += `<div class="troca">Integração em ${esc(semSufixo(grade.nome(x.lig.deEst)))} · ${x.precisa} min para trocar de linha${t2}
        <span class="selo ${justa ? "justa" : ""}">${justa ? "Integração justa" : "Dá tempo"}</span>
        ${justa ? `<small>${x.seguinte != null ? `Se perder, o próximo ${veiculo(grade.linha(o.pernas[i + 1].linha).nome)} sai às ${hora(x.seguinte)}.` : "É o último do dia: se perder, não há outro."}</small>` : ""}</div>`;
    }
  });
  // O lugar procurado: a caminhada da estação até a porta, já contada na chegada.
  if (ape) {
    html += `<p class="dica">🚶 ${ape.aoLado ? `${esc(ape.nome)} fica ao lado da estação.`
      : `${esc(ape.nome)}: ${metrosAPe(ape.metros)} a pé, uns ${ape.min} min.`}</p>`;
  }
  html += `<p class="chegada">Chega às ${hora(o.chega + (ape?.min || 0))}</p>`;
  html += fonte("tempo_a_pe", "margin-top:-6px");
  if (o.antes != null) {
    html += `<p class="dica">O ${veiculo(grade.linha(o.pernas[0].linha).nome)} das ${hora(o.antes)} chega no mesmo horário, mas espera mais na integração.</p>`;
  }
  if (perfil === "cadeira_de_rodas") html += acessoDoRoteiro(o, grade);
  if (perfil === "bicicleta") {
    for (const p of o.pernas) {
      const l = grade.linha(p.linha);
      const j = janelasBike(regras, grade.nome(p.de), l.nome, dia.diaSemana, dia.data);
      if (!j) html += `<p class="aviso">Com bicicleta: a ${esc(l.nome)} não aceita bike.</p>`;
      else if (!bikeAgora(j, p.sai).pode) html += `<p class="aviso">Com bicicleta: a ${esc(l.nome)} não aceita bike às ${hora(p.sai)}.</p>`;
    }
  }
  return html + `</div>`;
}

/** "Pode embarcar com bike no metrô agora, até as 15:00" — mesmas regras e frases do app. */
function avisoBike(linha, stopId, t) {
  const { grade, regras } = estado.dados;
  // Sem mais partidas (ou com a linha parada hoje), não há o que embarcar.
  const aindaHoje = grade.sentidosDePartida(linha, stopId).some((s) => grade.partidas(s, stopId).some((m) => m >= t.minutos));
  if (!aindaHoje || linhaParada(regras, t.data, linha.nome)) return "";
  const janelas = janelasBike(regras, grade.nome(stopId), linha.nome, t.diaSemana, t.data);
  if (!janelas) return "";
  const v = veiculo(linha.nome);
  if (t.diaSemana === 0 && !janelas.especial) {
    // Domingo é só operação especial: com ela anunciada mas sem regra de bike, não se sabe;
    // sem operação anunciada, já há o aviso de domingo (a grade de domingo comum vem igual
    // à de dia útil, então ter a grade do dia não indica operação).
    return regras?.operacoes?.[t.data]
      ? `<p class="bike">🚲 Operação especial: o Metrofor não informou se dá para embarcar com bike hoje</p>` : "";
  }
  const b = bikeAgora(janelas, t.minutos);
  const texto = b.pode
    ? (b.limite != null ? `Pode embarcar com bike no ${v} agora, até as ${hora(b.limite)}` : `Pode embarcar com bike no ${v} agora, até o fim da operação`)
    : b.limite != null ? `Bike no ${v} só a partir das ${hora(b.limite)}`
      : janelas.length ? `Não há mais horário hoje para embarcar com bike no ${v}` : `Hoje não há embarque com bike no ${v}`;
  // Dá para embarcar com bike hoje: o limite por viagem e as bikes que não entram.
  const extra = b.pode || b.limite != null
    ? [janelas.porViagem && `Até ${janelas.porViagem} bikes por viagem.`, regras?.bike?.proibidas].filter(Boolean).join(" ")
    : "";
  // A regra vem do regulamento ou, num dia especial, da notícia do dia.
  const origem = janelas.especial
    ? fonte("bike-hoje", "margin-top:-8px")
    : fonte(janelas.fonteId, "margin-top:-8px");
  return `<p class="bike">🚲 ${texto}${extra ? `<br><span class="dica">${esc(extra)}</span>` : ""}</p>${origem}`;
}

/** O próximo metrô/VLT da outra linha que ainda dá para pegar a pé, e a passagem. */
function blocoIntegracao(stopId, t) {
  const { grade, regras } = estado.dados;
  const outras = grade.integracoes
    .filter((i) => i.a === stopId || i.b === stopId)
    .map((i) => ({ id: i.a === stopId ? i.b : i.a, caminhada: i.caminhada_min }));
  if (!outras.length) return "";
  let html = `<h2>Integração com</h2>`;
  for (const { id, caminhada } of outras) {
    const tarifa = passagem(regras, grade.nome(stopId), grade.nome(id));
    const textoTarifa = tarifa.tipo === "gratis" ? "Integração gratuita: não paga outra passagem"
      : tarifa.tipo === "paga" ? "Integração paga: é preciso pagar outra passagem" : "";
    for (const { linha } of [...new Map((grade.passagens.get(id) || []).map((p) => [p.linha.id, p])).values()]) {
      const sentidos = grade.sentidosDePartida(linha, id);
      html += `<div class="cartao neutro">
        <a class="cabeca" href="#/estacao/${linha.id}/${id}">
          <h3><span class="ponto" style="background:${cor(linha)}"></span> ${esc(linha.nome)}</h3>
          <span class="distancia">${esc(grade.nome(id))} · ${caminhada} min a pé</span>
          ${textoTarifa ? `<p class="dica" style="margin:6px 0 0">${textoTarifa}${tarifa.motivo ? ` (${esc(tarifa.motivo)})` : ""}</p>` : ""}
          ${sentidos.map((s) => {
            const { hoje, amanha } = partidasComAmanha(linha, s, id);
            const p = proximas(hoje, t.minutos + caminhada, 1, amanha)[0];
            return `<div class="partida"><span>→ ${esc(s.destino)}</span><span class="hora">${textoPartida(p, t.minutos, s)}</span></div>`;
          }).join("")}
          <p class="dica" style="margin:10px 0 0">Toque para ver todos os horários de lá</p>
        </a><div class="fontes-linha">${textoTarifa ? fonte("integracao", "margin:0") : ""}${fonte("tempo_a_pe", "margin:0")}</div></div>`;
    }
  }
  return html;
}

function telaSobre() {
  const { indice } = estado.dados;
  tela.innerHTML = `<div class="sobre">
    <h2 style="font-size:1.4rem">Sobre</h2>
    <div class="aviso"><strong>App não oficial.</strong> Feito por um passageiro, sem ligação com o Metrofor nem com o
      Governo do Ceará. Os horários são os que o próprio Metrofor publica.</div>

    <h2>Versão lite e app completo</h2>
    <p>Esta é a versão lite: abre na hora, no navegador, em qualquer celular. Mostra a estação mais perto, os horários,
      a integração entre linhas, a regra de bike e o lembrete da hora de sair, e abre até sem internet.</p>
    <p>O <strong>app completo para Android</strong> acrescenta o <strong>modo viagem</strong>: escolha o destino e guarde
      o celular, que ele avisa antes da sua estação, mesmo com a tela desligada.
      ${ANDROID ? "" : "No iPhone, use esta versão lite."}</p>
    ${ANDROID ? `<a class="botao" href="${PAGINA_INSTALAR}">Conhecer o app completo</a>` : ""}

    ${INSTALADA ? "" : `<h2>Na tela inicial</h2>
    ${pedidoInstalar ? `<p>Deixe o app com ícone na tela inicial, como os outros apps:</p>
      <button class="botao secundario" id="instalar-sobre">📲 Adicionar à tela inicial</button>`
      : IPHONE ? `<p>Para ter o ícone na tela inicial: toque em <strong>Compartilhar</strong> (o quadrado com a seta;
      nas versões mais novas, dentro do botão •••) e depois em <strong>Adicionar à Tela de Início</strong>.</p>`
      : `<p>Para ter o ícone na tela inicial, use o menu do navegador: <strong>Adicionar à tela inicial</strong>
      (ou <strong>Instalar app</strong>).</p>`}`}

    <h2>Compartilhe</h2>
    <p>Aponte a câmera de outro celular para abrir o app na hora:</p>
    <img class="qr" src="qr.png" alt="QR code para abrir o app no navegador" width="200" height="200">
    <button class="botao" id="divulgar">Divulgar o app</button>

    <h2>De onde vêm os horários</h2>
    <p>Do GTFS público do Metrofor, atualizado várias vezes por dia. São os horários programados: o app não sabe de
      atrasos em tempo real.${indice.atualizado ? ` Última atualização: ${dataCurta(indice.atualizado.slice(0, 10))} às ${indice.atualizado.slice(11, 16)}.` : ""}
      Endereços e passagens: páginas das linhas no site do Metrofor. Posição de algumas estações de Sobral e do Cariri,
      que o GTFS põe no lugar errado: © colaboradores do OpenStreetMap. Lugares da busca e de "Perto desta estação",
      com a caminhada pelas ruas: © colaboradores do OpenStreetMap.</p>
    <a class="botao secundario" href="#/fontes">De onde vem cada informação</a>

    <h2>Privacidade</h2>
    <p>Sem anúncios, sem cadastro e sem rastreadores. Sua localização é usada só no celular, para achar a estação mais
      perto; nada é enviado para ninguém.
      <a href="../privacidade.html">Política de privacidade</a>.</p>

    <h2>Código aberto</h2>
    <p>Qualquer pessoa pode ver e conferir tudo o que o app faz, no
      <a href="https://github.com/giovanildo/metro-ceara-horarios">GitHub</a> (licença GPL-3.0).
      Sugestões e problemas: <a href="https://github.com/giovanildo/metro-ceara-horarios/issues">por aqui</a>.</p>

    <h2>Apoie</h2>
    <p>O app é gratuito e feito no tempo livre. Se ele te ajuda, considere uma doação via Pix: <strong>${PIX}</strong></p>
    <button class="botao secundario" id="copiar-pix">Copiar chave Pix</button>
    <p class="dica" id="copiado" hidden></p>

    <p><a href="../">Página do app</a></p>
  </div>`;
  document.getElementById("instalar-sobre")?.addEventListener("click", instalar);
  const avisar = (texto) => { const el = document.getElementById("copiado"); el.textContent = texto; el.hidden = false; };
  document.getElementById("copiar-pix").addEventListener("click", () =>
    navigator.clipboard?.writeText(PIX).then(() => avisar("Chave Pix copiada")).catch(() => avisar(PIX)));
  document.getElementById("divulgar").addEventListener("click", async () => {
    const texto = "🚇 Metrô Ceará: horários do metrô e do VLT no celular. Abra no navegador e use na hora, sem instalar nada. Grátis, sem anúncios, não oficial:";
    if (navigator.share) {
      try { await navigator.share({ title: "Metrô Ceará", text: texto, url: LINK }); } catch { /* cancelou */ }
    } else {
      navigator.clipboard?.writeText(`${texto} ${LINK}`).then(() => avisar("Link copiado")).catch(() => avisar(LINK));
    }
  });
}

// ---------- guia da passagem

/**
 * Quanto custa cada linha e, em telas próprias, a meia, a gratuidade e o VaiVem
 * Livre (regras.json "guia_passagem"). É muita regra que o app não controla: a
 * tela diz no topo que tudo vem do Metrofor, e cada tema termina num link para a
 * página dele.
 */
function telaPassagem(indice) {
  const regras = estado.dados.regras;
  const guia = regras?.guia_passagem;
  if (!guia) return telaNaoAchei();
  const link = (url) => (/^https:\/\//.test(url || "") ? esc(url) : null);
  const tema = indice != null ? guia.temas?.[Number(indice)] : null;
  if (indice != null && !tema) return telaNaoAchei();
  let html = "";
  if (!tema) {
    html += `<h2 style="font-size:1.4rem">Passagem</h2>
      <div class="cartao"><strong>ⓘ Informações do Metrofor</strong><br>${esc(guia.fonte)}
        ${regras?.fontes?.guia_passagem ? fonte("guia_passagem", "margin:4px 0 0") : `<br><small>Conferido em ${esc(guia.conferido)}</small>`}
        ${link(guia.link) ? `<br><a href="${link(guia.link)}" target="_blank" rel="noopener"><strong>Ver no site do Metrofor ›</strong></a>` : ""}</div>`;
    const linhas = Object.entries(regras.passagem || {}).filter(([k]) => !k.startsWith("_"));
    html += `<div class="tarifas">${linhas.map(([nome, texto]) =>
      `<p><span class="ponto" style="background:${cor({ nome, cor: "888888" })}"></span>
        <span><strong>${esc(nome)}</strong><br><span class="dica">${esc(texto)}</span></span></p>`).join("")}</div>`;
    if (guia.temas?.length) {
      html += `<h2>Pode pagar menos?</h2>` + guia.temas.map((t, i) =>
        `<a class="cartao tema" href="#/passagem/${i}"><strong>${esc([t.icone, t.titulo].filter(Boolean).join(" "))}</strong>
          <br>${esc(t.resumo)}<span class="seta">›</span></a>`).join("");
    }
    const inteira = guia.inteira;
    if (inteira) {
      html += `<div class="aviso quadro"><strong>${esc(inteira.titulo)}</strong><br>${esc(inteira.texto)}
        ${inteira.botao && link(inteira.link) ? `<br><a href="${link(inteira.link)}" target="_blank" rel="noopener">${esc(inteira.botao)} ↗</a>` : ""}</div>`;
    }
  } else {
    html += `<h2 style="font-size:1.4rem">${esc(tema.titulo)}</h2>
      ${(() => {
        // O nome do Metrofor fica à vista; datas e explicação, na janela da fonte.
        const f = fonteLinha(regras, "guia_passagem", agora().data);
        return f ? fonte("guia_passagem", "margin-top:-6px", f.atrasada ? f.rotulo : `Fonte: ${regras.fontes.guia_passagem.fonte}`)
          : `<p class="fonte-guia">Fonte: site do Metrofor · conferido em ${esc(guia.conferido)}</p>`;
      })()}`;
    // Prazos com data, como a renovação anual do cartão de estudante.
    const hoje = agora().data;
    for (const a of tema.avisos || []) {
      if (hoje >= a.de && hoje <= a.ate) html += `<div class="cartao rosa">${esc(a.texto)}</div>`;
    }
    if (tema.texto) html += `<p>${esc(tema.texto)}</p>`;
    for (const sec of tema.secoes || []) {
      html += `<h2>${esc(sec.titulo)}</h2>`;
      html += (sec.quadros || []).map((q) =>
        `<div class="aviso quadro"><strong>${esc(q.titulo)}</strong><br><span class="dica">${esc(q.texto)}</span></div>`).join("");
      if (sec.itens?.length) html += `<ul>${sec.itens.map((i) => `<li>${esc(i)}</li>`).join("")}</ul>`;
    }
    if (tema.alerta) html += `<div class="cartao rosa">${esc(tema.alerta)}</div>`;
    if (tema.nota) html += `<p class="dica">${esc(tema.nota)}</p>`;
    const url = link(tema.link) || link(guia.link);
    if (url) html += `<a class="botao secundario largo" href="${url}" target="_blank" rel="noopener">Conferir no site do Metrofor ↗</a>`;
    html += `<p><a href="#/passagem">‹ Passagem</a></p>`;
  }
  tela.innerHTML = `<div class="guia">${html}</div>`;
}

// ---------- de onde vem cada informação

/** Os horários e cada fonte do regras.json, com a conferência e a próxima (como no app). */
function telaFontes() {
  const { indice, regras, grade } = estado.dados;
  const t = agora();
  const hoje = grade.data === t.data;
  const horaBaixada = indice.tipos?.[grade.tipo]?.atualizado?.slice(11, 16);
  let html = `<h2 style="font-size:1.4rem">De onde vem cada informação</h2>
    <p>Este app não é oficial. Para você poder confiar e conferir, cada informação diz de onde vem e quando foi conferida.
      A próxima conferência é a data em que vamos olhar a fonte de novo; até lá, nenhuma notícia do Metrofor indicou
      mudança. Estimativas do app não são dados do Metrofor.</p>
    <div class="fonte-item"><strong>Horários</strong>
      <p>O app baixa a grade do dia publicada pelo Metrofor. São os horários programados: o app não sabe de atrasos.</p>
      <p class="fonte">${hoje ? `Fonte: GTFS do Metrofor · baixados hoje${horaBaixada ? ` às ${horaBaixada}` : ""} · valem só para hoje`
        : `Fonte: GTFS do Metrofor · grade de ${dataCurta(grade.data)} · o Metrofor publica a grade de cada dia`}</p></div>`;
  for (const id of Object.keys(regras?.fontes || {})) {
    const f = fonteLinha(regras, id, t.data);
    if (!f) continue;
    html += `<div class="fonte-item"><strong>${esc(f.titulo)}</strong>${f.detalhe ? `<p>${esc(f.detalhe)}</p>` : ""}
      <p class="fonte">${esc(f.texto)}</p>${f.link ? `<p><a href="${esc(f.link)}" target="_blank" rel="noopener">Abrir a fonte ↗</a></p>` : ""}</div>`;
  }
  html += `<p><a href="#/sobre">‹ Sobre</a></p>`;
  tela.innerHTML = `<div class="sobre">${html}</div>`;
}

function telaNaoAchei() {
  tela.innerHTML = `<p class="vazio">Não achei essa tela. <a href="#/">Voltar ao início</a></p>`;
}

function avisoDomingo(t) {
  // Domingo com operação anunciada pelo Metrofor: a notícia dela explica.
  if (!domingoSemOperacao(t)) return "";
  return `<div class="aviso"><strong>Hoje é domingo.</strong> Metrô e VLT normalmente não funcionam aos domingos,
    só em operação especial (eleição, ENEM, eventos). Os horários mostrados são os de segunda. Se houver operação especial
    hoje, o Metrofor publica os horários nas notícias.</div>`;
}

/** "O Metrofor avisou que esta linha não circula hoje", se for o caso. */
function avisoParada(linha, t) {
  return linhaParada(estado.dados.regras, t.data, linha.nome)
    ? `<div class="aviso">O Metrofor avisou que esta linha não circula hoje.</div>` : "";
}

// ---------- lembrete na agenda (.ics: abre no Calendário do iPhone e na agenda do Android)

function lembrete(linha, sentido, stopId, minutos, amanha) {
  const { grade } = estado.dados;
  const t = agora();
  const v = veiculo(linha.nome);
  const nome = grade.nome(stopId);
  const antes = AVISOS.includes(estado.config.antes) ? estado.config.antes : AVISO_PADRAO;
  janela.innerHTML = `<h3>Lembrar na agenda</h3>
    <p>${amanha ? "Amanhã, o" : "O"} ${v} sai às ${hora(minutos)} de ${esc(nome)}.</p>
    <label class="campo"><span>Avisar quanto tempo antes</span>
      <select id="antes">${AVISOS.map((m) => `<option value="${m}" ${m === antes ? "selected" : ""}>${m} min antes</option>`).join("")}</select></label>
    <p class="dica">Por exemplo, o tempo que você leva até a estação. O lembrete vai para a agenda do celular.</p>
    <div class="acoes"><button class="botao secundario" id="cancelar">Cancelar</button>
      <button class="botao" id="agenda">Adicionar à agenda</button></div>`;
  janela.showModal();
  janela.querySelector("#cancelar").onclick = () => janela.close();
  janela.querySelector("#agenda").onclick = () => {
    estado.config.antes = +janela.querySelector("#antes").value;
    salvarConfig();
    const dia = amanha ? somarDias(t.data, 1) : t.data;
    baixarIcs({
      inicio: instante(dia, minutos),
      fim: instante(dia, minutos + 10),
      avisoMin: estado.config.antes,
      titulo: `Pegar o ${v} das ${hora(minutos)}`,
      local: `Estação ${nome}`,
      descricao: `${linha.nome}, sentido ${sentido.destino}. Lembrete criado pelo app Metrô Ceará.`,
    });
    janela.close();
  };
}

/** Instante UTC de um horário de Fortaleza (UTC−3, sem horário de verão). */
function instante(data, minutos) {
  const [a, m, d] = data.split("-").map(Number);
  return new Date(Date.UTC(a, m - 1, d, 0, minutos + 180));
}

function baixarIcs({ inicio, fim, avisoMin, titulo, local, descricao }) {
  const z = (d) => d.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
  const txt = (s) => s.replace(/\\/g, "\\\\").replace(/[,;]/g, (c) => "\\" + c).replace(/\n/g, "\\n");
  const ics = [
    "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Metro Ceara//Horarios//PT-BR", "CALSCALE:GREGORIAN",
    "BEGIN:VEVENT",
    `UID:${z(inicio)}-${Math.random().toString(36).slice(2)}@metro-ceara`,
    `DTSTAMP:${z(new Date())}`, `DTSTART:${z(inicio)}`, `DTEND:${z(fim)}`,
    `SUMMARY:${txt(titulo)}`, `LOCATION:${txt(local)}`, `DESCRIPTION:${txt(descricao)}`,
    "BEGIN:VALARM", "ACTION:DISPLAY", `DESCRIPTION:${txt(titulo)}`, `TRIGGER:-PT${avisoMin}M`, "END:VALARM",
    "END:VEVENT", "END:VCALENDAR", "",
  ].join("\r\n");
  const url = URL.createObjectURL(new Blob([ics], { type: "text/calendar;charset=utf-8" }));
  const a = Object.assign(document.createElement("a"), { href: url, download: "lembrete-metro.ics" });
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

// ---------- roteamento e status

function render() {
  if (!estado.dados) return;
  const partes = location.hash.replace(/^#\/?/, "").split("/").map(decodeURIComponent);
  if (partes[0] === "linha") telaLinha(partes[1]);
  else if (partes[0] === "estacao") telaEstacao(partes[1], partes[2], partes[3]);
  else if (partes[0] === "sobre") telaSobre();
  else if (partes[0] === "passagem") telaPassagem(partes[1]);
  else if (partes[0] === "fontes") telaFontes();
  else if (partes[0] === "acesso") telaAcesso(partes[1], partes[2]);
  else if (partes[0] === "viagem") telaViagem(partes[1], partes[2]);
  else if (partes[0] === "lugar") telaLugar(partes[1]);
  else if (partes[0] === "ir") irParaLugar(partes[1]);
  else telaInicio();
  status();
}

function status() {
  const { grade, indice, regras } = estado.dados;
  const t = agora();
  const nomes = { "dia-util": "dia útil", sabado: "sábado", domingo: "domingo" };
  const el = document.getElementById("status");
  // A grade de um dia útil comum é igual à do dia útil anterior: a de outro dia do
  // mesmo tipo só preocupa (e vira aviso) quando o Metrofor anunciou operação para
  // hoje. Num domingo sem operação, a tela mostra a grade de segunda.
  const especial = !domingoSemOperacao(t) && grade.data !== t.data && regras?.operacoes?.[t.data];
  el.classList.toggle("alerta", Boolean(especial));
  if (especial) {
    el.textContent = "Hoje tem operação especial e os horários do dia ainda não chegaram. Veja a notícia do Metrofor.";
  } else if (!domingoSemOperacao(t) && grade.data === t.data) {
    const hora = indice.tipos?.[grade.tipo]?.atualizado?.slice(11, 16);
    el.textContent = `Horários de hoje, baixados do Metrofor${hora ? ` às ${hora}` : ""}.`;
  } else {
    el.textContent = `Horários de ${domingoSemOperacao(t) ? "dia útil" : nomes[grade.tipo]}, publicados pelo Metrofor.`;
  }
}

async function iniciar() {
  try { localStorage.setItem("visitas", String(lerNumero("visitas") + 1)); } catch { /* segue */ }
  try {
    estado.dados = await carregar();
  } catch {
    tela.innerHTML = `<p class="vazio">Não deu para baixar os horários. Confira a internet e tente de novo.</p>
      <button class="botao" onclick="location.reload()">Tentar de novo</button>`;
    return;
  }
  render();
  estado.dados.proximoDia().then((g) => { estado.proximo = g; render(); }).catch(() => {});
  // Quem já liberou a localização não precisa tocar no botão de novo.
  const p = await navigator.permissions?.query({ name: "geolocation" }).catch(() => null);
  if (p?.state === "granted" || localStorage.getItem("localizacao") === "sim") localizar();
}

window.addEventListener("hashchange", () => { window.scrollTo(0, 0); render(); });
// O relógio anda sozinho: as contagens ("em 7 min") se atualizam.
setInterval(() => { if (!ocupado()) render(); }, 15000);
iniciar();

// Guarda o próprio app no celular: abre até sem internet (no túnel, no modo avião).
if ("serviceWorker" in navigator) navigator.serviceWorker.register("sw.js").catch(() => {});
