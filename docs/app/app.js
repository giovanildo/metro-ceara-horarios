import {
  agora, bikeAgora, carregar, dataCurta, distancia, falta, hora, janelasBike, metros, minutosAte, passagem,
  proximas, somarDias, veiculo,
} from "./dados.js";

const PERTO_METROS = 3000; // outras linhas aparecem até esta distância (padrão do app)
const LINK = "https://giovanildo.github.io/metro-ceara-horarios/app/";
const PAGINA_INSTALAR = "../#baixar"; // direto no botão de baixar o APK, com os avisos explicados ao lado
const PIX = "giovanildos@gmail.com";
const ANDROID = /Android/i.test(navigator.userAgent);
const CONVITE_A_PARTIR_DA_VISITA = 3;
const CONVITE_PAUSA_DIAS = 30;
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

function cor(linha) { return "#" + linha.cor; }

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

function partidasComAmanha(linha, sentido, stopId) {
  const g = estado.dados.grade;
  const hoje = g.partidas(sentido, stopId);
  let amanha = null;
  // Amanhã domingo: nada de horários de amanhã na lista; o fim do dia diz a segunda.
  const ga = estado.dados.proximoEhSegunda ? null : estado.proximo;
  if (ga) {
    const s = ga.linha(linha.id)?.sentidos.find((x) => x.id === sentido.id);
    if (s) amanha = ga.partidas(s, stopId);
  }
  return { hoje, amanha };
}

/** Os avisos de operação do site do Metrofor: a manchete; tocar abre a notícia no site deles. */
function avisosMetrofor() {
  return estado.dados.avisos.map((n) => {
    const dia = /^\d{4}-\d{2}-\d{2}$/.test(n.data || "") ? `, ${n.data.slice(8)}/${n.data.slice(5, 7)}` : "";
    const texto = `<strong>Metrofor${dia}:</strong> ${esc(n.titulo)}`;
    return /^https:\/\//.test(n.link || "")
      ? `<p class="aviso-metrofor"><a href="${esc(n.link)}" target="_blank" rel="noopener">${texto} ›</a></p>`
      : `<p class="aviso-metrofor">${texto}</p>`;
  }).join("");
}

/** "Último de hoje: 17:12 · primeiro de segunda: 05:44", para quando hoje acabou. */
function fimDoDia(linha, sentido, stopId) {
  const ultimo = estado.dados.grade.partidas(sentido, stopId).at(-1);
  let primeiro;
  const g = estado.proximo;
  const s = g?.linha(linha.id)?.sentidos.find((x) => x.id === sentido.id);
  if (s) primeiro = g.partidas(s, stopId)[0];
  const partes = [];
  if (ultimo !== undefined) partes.push(`Último de hoje: ${hora(ultimo)}`);
  if (primeiro !== undefined) {
    partes.push(`${partes.length ? "p" : "P"}rimeiro de ${estado.dados.proximoEhSegunda ? "segunda" : "amanhã"}: ${hora(primeiro)}`
      + (estado.dados.proximoEhSegunda ? " (domingo, só em operação especial)" : ""));
  }
  return partes.join(" · ");
}

function textoPartida(p, minutos) {
  if (!p) return "sem mais partidas hoje";
  return `${hora(p.m)}${p.amanha ? " (amanhã)" : ""} · ${falta(minutosAte(p, minutos))}`;
}

// ---------- telas

function telaInicio() {
  const { grade } = estado.dados;
  const t = agora();
  let html = avisoDomingo(t) + avisosMetrofor() + convite();

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
          ${sentidos.map((s) => {
            const { hoje, amanha } = partidasComAmanha(linha, s, id);
            const p = proximas(hoje, t.minutos, 1, amanha)[0];
            const fim = p ? "" : fimDoDia(linha, s, id);
            return `<div class="partida"><span>→ ${esc(s.destino)}</span><span class="hora">${textoPartida(p, t.minutos)}</span></div>`
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
      <small>${esc(l.descricao)} · ${grade.estacoesDaLinha(l).length} estações</small></a>`).join("")}</div>`;
  tela.innerHTML = html;
  document.getElementById("localizar")?.addEventListener("click", localizar);
  document.getElementById("instalar")?.addEventListener("click", instalar);
  document.getElementById("agora-nao")?.addEventListener("click", () => {
    try { localStorage.setItem("convitePausadoAte", String(Date.now() + CONVITE_PAUSA_DIAS * 864e5)); } catch { /* segue */ }
    render();
  });
}

/**
 * A versão lite é a porta de entrada: quem volta a usar no Android é
 * convidado, sem insistência, a conhecer o app completo (modo viagem).
 */
function convite() {
  if (!ANDROID || lerNumero("visitas") < CONVITE_A_PARTIR_DA_VISITA || Date.now() < lerNumero("convitePausadoAte")) return "";
  return `<div class="cartao rosa convite">
    <strong>Quer ser avisado antes da sua estação, mesmo com a tela desligada?</strong>
    <span>O app completo para Android tem o modo viagem. Grátis, como esta versão.</span>
    <div class="acoes"><a class="botao" href="${PAGINA_INSTALAR}">Conhecer o app completo</a>
      <button class="botao secundario" id="agora-nao">Agora não</button></div>
  </div>`;
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
      `<a href="#/estacao/${linha.id}/${id}">${esc(grade.nome(id))}</a>`).join("")}</div>`;
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

  let html = `<h2 style="font-size:1.4rem;margin-bottom:0">${esc(grade.nome(stopId))}</h2>
    <p class="dica" style="margin-top:2px"><span class="ponto" style="background:${cor(linha)}"></span> ${esc(linha.nome)}</p>`;
  if (sentidos.length > 1) {
    html += `<nav class="abas">${sentidos.map((s) =>
      `<a href="#/estacao/${linha.id}/${stopId}/${s.id}" class="${s === sentido ? "ativa" : ""}">→ ${esc(s.destino)}</a>`).join("")}</nav>`;
  } else {
    html += `<p><strong>→ ${esc(sentido.destino)}</strong></p>`;
  }
  html += avisoDomingo(t);
  html += `<h2>Próximas partidas</h2>`;
  if (!lista.length) {
    const fim = fimDoDia(linha, sentido, stopId);
    html += `<p class="vazio">Sem mais partidas hoje.</p>${fim ? `<p class="dica">${fim}</p>` : ""}`;
  } else {
    html += `<div class="cartao proximas" style="padding:4px 0">${lista.map((p, i) =>
      `<button data-m="${p.m}" data-amanha="${p.amanha}">
        <span><span class="hora">${hora(p.m)}</span>${p.amanha ? "<small>amanhã</small>" : ""}</span>
        <span>${falta(minutosAte(p, t.minutos))}</span></button>`).join("")}</div>`;
  }
  html += `<p class="dica">Toque em qualquer horário de hoje (aqui ou em "Todos os horários", mais abaixo) para criar um lembrete na agenda.</p>`;
  html += avisoBike(linha, stopId, t);
  html += blocoIntegracao(stopId, t);
  html += `<h2>Todos os horários</h2><div class="todos">${hoje.map((m) => {
    const passou = m < t.minutos;
    return `<button data-m="${m}" data-amanha="false" class="${passou ? "passou" : ""}" ${passou ? "disabled" : ""}>${hora(m)}</button>`;
  }).join("")}</div>`;
  tela.innerHTML = html;

  tela.querySelectorAll("button[data-m]:not([disabled])").forEach((b) => b.addEventListener("click", () =>
    lembrete(linha, sentido, stopId, +b.dataset.m, b.dataset.amanha === "true")));
}

/** "Pode embarcar com bike no metrô agora, até as 15:00" — mesmas regras e frases do app. */
function avisoBike(linha, stopId, t) {
  const { grade, regras } = estado.dados;
  // Domingo só tem operação especial: sem regra do dia anunciada, a da bike é desconhecida.
  // Sem mais partidas, não há o que embarcar.
  const aindaHoje = grade.sentidosDePartida(linha, stopId).some((s) => grade.partidas(s, stopId).some((m) => m >= t.minutos));
  if (!aindaHoje) return "";
  const janelas = janelasBike(regras, grade.nome(stopId), linha.nome, t.diaSemana, t.data);
  if (!janelas || (t.diaSemana === 0 && !janelas.especial)) return "";
  const v = veiculo(linha.nome);
  const b = bikeAgora(janelas, t.minutos);
  const texto = b.pode
    ? (b.limite != null ? `Pode embarcar com bike no ${v} agora, até as ${hora(b.limite)}` : `Pode embarcar com bike no ${v} agora, até o fim da operação`)
    : b.limite != null ? `Bike no ${v} só a partir das ${hora(b.limite)}`
      : janelas.length ? `Não há mais horário hoje para embarcar com bike no ${v}` : `Hoje não há embarque com bike no ${v}`;
  return `<p class="bike">🚲 ${texto}</p>`;
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
            return `<div class="partida"><span>→ ${esc(s.destino)}</span><span class="hora">${textoPartida(p, t.minutos)}</span></div>`;
          }).join("")}
          <p class="dica" style="margin:10px 0 0">Toque para ver todos os horários de lá</p>
        </a></div>`;
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
      atrasos em tempo real.${indice.atualizado ? ` Última atualização: ${dataCurta(indice.atualizado.slice(0, 10))} às ${indice.atualizado.slice(11, 16)}.` : ""}</p>

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

function telaNaoAchei() {
  tela.innerHTML = `<p class="vazio">Não achei essa tela. <a href="#/">Voltar ao início</a></p>`;
}

function avisoDomingo(t) {
  const { grade } = estado.dados;
  // Com a grade de hoje baixada, ela já diz se há operação especial.
  if (t.diaSemana !== 0 || grade.data === t.data) return "";
  return `<div class="aviso"><strong>Hoje é domingo.</strong> Metrô e VLT normalmente não funcionam aos domingos,
    só em operação especial (eleição, ENEM, eventos). Os horários abaixo são de outro dia.</div>`;
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
  else telaInicio();
  status();
}

function status() {
  const { grade, indice } = estado.dados;
  const t = agora();
  const nomes = { "dia-util": "dia útil", sabado: "sábado", domingo: "domingo" };
  const quando = grade.data === t.data ? "Horários de hoje" : `Horários de ${nomes[grade.tipo]} (${dataCurta(grade.data)})`;
  const atualizado = indice.atualizado ? ` Atualizado em ${dataCurta(indice.atualizado.slice(0, 10))} às ${indice.atualizado.slice(11, 16)}.` : "";
  document.getElementById("status").textContent = `${quando}, publicados pelo Metrofor.${atualizado}`;
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
