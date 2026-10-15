import {
  agora, carregar, dataCurta, distancia, falta, hora, metros, minutosAte, proximas, somarDias, veiculo,
} from "./dados.js";

const PERTO_METROS = 3000; // outras linhas aparecem até esta distância (padrão do app)
const PROXIMAS = 4;
const tela = document.getElementById("tela");
const janela = document.getElementById("janela");

const estado = {
  dados: null,
  amanha: null,
  pos: null, // { lat, lon }
  localizando: false,
  erroLocal: null,
  config: lerConfig(),
};

// ---------- utilidades

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function lerConfig() {
  try {
    return { casa: "", minutos: 10, ...JSON.parse(localStorage.getItem("config") || "{}") };
  } catch {
    return { casa: "", minutos: 10 };
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
  const ga = estado.amanha;
  if (ga) {
    const s = ga.linha(linha.id)?.sentidos.find((x) => x.id === sentido.id);
    if (s) amanha = ga.partidas(s, stopId);
  }
  return { hoje, amanha };
}

function textoPartida(p, minutos) {
  if (!p) return "sem mais partidas hoje";
  return `${hora(p.m)}${p.amanha ? " (amanhã)" : ""} · ${falta(minutosAte(p, minutos))}`;
}

// ---------- telas

function telaInicio() {
  const { grade } = estado.dados;
  const t = agora();
  let html = avisoDomingo(t);

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
            return `<div class="partida"><span>→ ${esc(s.destino)}</span><span class="hora">${textoPartida(p, t.minutos)}</span></div>`;
          }).join("")}
        </a></div>`;
    }
  }

  html += `<h2>Linhas</h2><div class="lista-linhas">${grade.linhas.map((l) =>
    `<a href="#/linha/${l.id}" style="--cor:${cor(l)}"><strong>${esc(l.nome)}</strong>
      <small>${esc(l.descricao)} · ${grade.estacoesDaLinha(l).length} estações</small></a>`).join("")}</div>`;
  tela.innerHTML = html;
  document.getElementById("localizar")?.addEventListener("click", localizar);
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
  const ehCasa = estado.config.casa === stopId;
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
    html += `<p class="vazio">Sem mais partidas hoje.</p>`;
  } else {
    html += `<div class="cartao proximas" style="padding:4px 0">${lista.map((p, i) =>
      `<button data-m="${p.m}" data-amanha="${p.amanha}" ${ehCasa ? "" : "disabled"}>
        <span><span class="hora">${hora(p.m)}</span>${p.amanha ? "<small>amanhã</small>" : ""}</span>
        <span>${falta(minutosAte(p, t.minutos))}</span></button>`).join("")}</div>`;
  }
  // O lembrete é só da estação de casa, como no app.
  if (ehCasa) {
    html += `<p class="dica">Toque em qualquer horário de hoje (aqui ou em "Todos os horários", mais abaixo) para lembrar a hora de sair de casa.</p>`;
  } else if (!estado.config.casa) {
    html += `<p class="dica">Quer um lembrete da hora de sair de casa? <a href="#/config">Escolha a estação perto de casa</a>.</p>`;
  }
  html += `<h2>Todos os horários</h2><div class="todos">${hoje.map((m) => {
    const passou = m < t.minutos;
    return `<button data-m="${m}" data-amanha="false" class="${passou ? "passou" : ""}" ${ehCasa && !passou ? "" : "disabled"}>${hora(m)}</button>`;
  }).join("")}</div>`;
  tela.innerHTML = html;

  if (ehCasa) {
    tela.querySelectorAll("button[data-m]:not([disabled])").forEach((b) => b.addEventListener("click", () =>
      lembrete(linha, sentido, stopId, +b.dataset.m, b.dataset.amanha === "true")));
  }
}

function telaConfig() {
  const { grade } = estado.dados;
  const c = estado.config;
  let perto = "";
  if (estado.pos) {
    const lista = Object.entries(grade.estacoes)
      .filter(([id]) => grade.passagens.has(id))
      .map(([id, e]) => ({ id, nome: e.nome, d: distancia(estado.pos.lat, estado.pos.lon, e.lat, e.lon) }))
      .sort((a, b) => a.d - b.d).slice(0, 3);
    perto = `<optgroup label="Mais perto de você agora">${lista.map((e) =>
      `<option value="${e.id}">${esc(e.nome)} · ${metros(e.d)}</option>`).join("")}</optgroup>`;
  }
  const grupos = grade.linhas.map((l) => `<optgroup label="${esc(l.nome)}">${grade.estacoesDaLinha(l).map((id) =>
    `<option value="${id}">${esc(grade.nome(id))}</option>`).join("")}</optgroup>`).join("");
  tela.innerHTML = `<h2 style="font-size:1.4rem">Configurações</h2>
    <h2>Lembrete da hora de sair</h2>
    <p class="dica">O lembrete aparece só na estação de casa: toque num horário dela e o app cria na agenda a hora de sair de casa.</p>
    <label class="campo"><span>Estação perto de casa</span>
      <select id="casa"><option value="">Nenhuma</option>${perto}${grupos}</select></label>
    <label class="campo"><span>Tempo de casa até a estação (minutos)</span>
      <input id="minutos" type="number" min="1" max="60" inputmode="numeric" value="${c.minutos}"></label>
    <p class="dica">Fica guardado só neste aparelho.</p>`;
  const casa = document.getElementById("casa");
  casa.value = c.casa;
  casa.addEventListener("change", () => { c.casa = casa.value; salvarConfig(); });
  const min = document.getElementById("minutos");
  min.addEventListener("change", () => {
    c.minutos = Math.min(60, Math.max(1, Math.round(+min.value) || 10));
    min.value = c.minutos;
    salvarConfig();
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
  const casaMin = estado.config.minutos;
  const v = veiculo(linha.nome);
  const nome = grade.nome(stopId);
  janela.innerHTML = `<h3>Lembrar a hora de sair</h3>
    <p>${amanha ? "Amanhã, o" : "O"} ${v} sai às ${hora(minutos)} de ${esc(nome)}. Levando ${casaMin} min de casa até a estação, saia às ${hora(minutos - casaMin)}.</p>
    <p class="dica">O lembrete vai para a agenda do celular, com aviso na hora de sair.</p>
    <div class="acoes"><button class="botao secundario" id="cancelar">Cancelar</button>
      <button class="botao" id="agenda">Adicionar à agenda</button></div>`;
  janela.showModal();
  janela.querySelector("#cancelar").onclick = () => janela.close();
  janela.querySelector("#agenda").onclick = () => {
    const dia = amanha ? somarDias(t.data, 1) : t.data;
    baixarIcs({
      inicio: instante(dia, minutos - casaMin),
      fim: instante(dia, minutos),
      titulo: `Sair de casa para o ${v} das ${hora(minutos)}`,
      local: `Estação ${nome}`,
      descricao: `${linha.nome}, sentido ${sentido.destino}. Partida às ${hora(minutos)}; ${casaMin} min de casa até a estação. Lembrete criado pelo app Metrô Fortaleza.`,
    });
    janela.close();
  };
}

/** Instante UTC de um horário de Fortaleza (UTC−3, sem horário de verão). */
function instante(data, minutos) {
  const [a, m, d] = data.split("-").map(Number);
  return new Date(Date.UTC(a, m - 1, d, 0, minutos + 180));
}

function baixarIcs({ inicio, fim, titulo, local, descricao }) {
  const z = (d) => d.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}/, "");
  const txt = (s) => s.replace(/\\/g, "\\\\").replace(/[,;]/g, (c) => "\\" + c).replace(/\n/g, "\\n");
  const ics = [
    "BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Metro Fortaleza//Horarios//PT-BR", "CALSCALE:GREGORIAN",
    "BEGIN:VEVENT",
    `UID:${z(inicio)}-${Math.random().toString(36).slice(2)}@metro-fortaleza`,
    `DTSTAMP:${z(new Date())}`, `DTSTART:${z(inicio)}`, `DTEND:${z(fim)}`,
    `SUMMARY:${txt(titulo)}`, `LOCATION:${txt(local)}`, `DESCRIPTION:${txt(descricao)}`,
    "BEGIN:VALARM", "ACTION:DISPLAY", `DESCRIPTION:${txt(titulo)}`, "TRIGGER:PT0M", "END:VALARM",
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
  else if (partes[0] === "config") telaConfig();
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
  try {
    estado.dados = await carregar();
  } catch {
    tela.innerHTML = `<p class="vazio">Não deu para baixar os horários. Confira a internet e tente de novo.</p>
      <button class="botao" onclick="location.reload()">Tentar de novo</button>`;
    return;
  }
  render();
  estado.dados.amanha().then((g) => { estado.amanha = g; render(); }).catch(() => {});
  // Quem já liberou a localização não precisa tocar no botão de novo.
  const p = await navigator.permissions?.query({ name: "geolocation" }).catch(() => null);
  if (p?.state === "granted" || localStorage.getItem("localizacao") === "sim") localizar();
}

window.addEventListener("hashchange", () => { window.scrollTo(0, 0); render(); });
// O relógio anda sozinho: as contagens ("em 7 min") se atualizam.
setInterval(() => { if (!ocupado()) render(); }, 15000);
iniciar();
