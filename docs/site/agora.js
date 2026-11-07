// "Próximas partidas" na página da estação do site. Usa o mesmo código e os mesmos
// dados da lite (docs/app/dados.js, ramo "dados"): domingo sem operação, operação
// especial e linha parada seguem as mesmas regras.
import { carregar, agora, hora, falta, proximas, minutosAte, linhaParada } from "../app/dados.js";

const quadro = document.querySelector(".agora");
const lista = quadro?.querySelector(".agora-lista");
const paradas = JSON.parse(quadro?.dataset.paradas || "[]");
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);

let dados = null;
let amanha = null;

function desenhar() {
  const t = agora();
  const { grade, regras } = dados;
  const op = regras?.operacoes?.[t.data];
  const domingo = t.diaSemana === 0 && !op;
  const semGradeDeHoje = op && !op.normal && grade.data !== t.data;
  const proximo = dados.proximoEhSegunda ? "segunda" : "amanhã";
  let html = "";

  for (const aviso of dados.avisos.slice(0, 2)) {
    const quando = aviso.hoje ? "Hoje" : aviso.dias.map((d) => d.slice(8) + "/" + d.slice(5, 7)).join(", ");
    const texto = `<strong>Metrofor, ${esc(quando)}:</strong> ${esc(aviso.titulo)}`;
    html += /^https:\/\//.test(aviso.link || "")
      ? `<p class="aviso-dia"><a href="${esc(aviso.link)}" target="_blank" rel="noopener">${texto} ›</a></p>` : `<p class="aviso-dia">${texto}</p>`;
  }
  if (domingo) html += `<p class="aviso-dia">Hoje é domingo: o metrô e o VLT não circulam. Abaixo, os primeiros de segunda.</p>`;
  else if (semGradeDeHoje) html += `<p class="aviso-dia">Hoje tem operação especial e os horários do dia ainda não chegaram.</p>`;

  for (const p of paradas) {
    const linha = grade.linha(p.linha);
    for (const sp of p.sentidos) {
      const s = linha?.sentidos.find((x) => x.id === sp.id);
      const parada = linha && linhaParada(regras, t.data, linha.nome);
      const hoje = s && !domingo && !parada ? grade.partidas(s, p.stop) : [];
      const sa = !dados.proximoEhSegunda || domingo ? amanha?.linha(p.linha)?.sentidos.find((x) => x.id === sp.id) : null;
      const deAmanha = sa ? amanha.partidas(sa, p.stop) : null;
      const prox = domingo ? (deAmanha || []).slice(0, 3).map((m) => ({ m, amanha: false, seg: true }))
        : proximas(hoje, t.minutos, 3, deAmanha);
      let corpo;
      if (parada) corpo = `<p class="dica">O Metrofor avisou que esta linha não circula hoje.</p>`;
      else if (!prox.length) corpo = `<p class="dica">Sem mais partidas hoje${hoje.length ? ` (a última foi às ${hora(hoje.at(-1))})` : ""}.</p>`;
      else {
        corpo = `<ul class="partidas">${prox.map((x) => `<li><strong>${hora(x.m)}</strong><span>${
          x.seg ? "segunda" : x.amanha ? proximo : falta(minutosAte(x, t.minutos))}</span></li>`).join("")}</ul>`;
      }
      html += `<div class="agora-sentido" style="--cor:${esc(p.cor)}"><p class="linha-nome">${esc(p.nome)}</p>
        <h3>Sentido ${esc(sp.destino)}</h3>${corpo}</div>`;
    }
  }
  const atualizado = dados.indice?.tipos?.[grade.tipo]?.atualizado?.slice(11, 16);
  html += `<p class="dica aviso-dia" style="background:none;padding:0">${grade.data === t.data
    ? `Horários de hoje, publicados pelo Metrofor${atualizado ? ` (baixados às ${atualizado})` : ""}.`
    : "Horários publicados pelo Metrofor."} São os programados: atrasos não aparecem aqui.</p>`;
  lista.innerHTML = html;
}

async function iniciar() {
  if (!quadro) return;
  try {
    dados = await carregar();
  } catch {
    lista.innerHTML = `<p class="dica">Não deu para baixar os horários agora. Tente a <a href="../../app/">versão lite</a>.</p>`;
    return;
  }
  desenhar();
  dados.proximoDia().then((g) => { amanha = g; desenhar(); }).catch(() => {});
  setInterval(desenhar, 30000);
}

iniciar();
