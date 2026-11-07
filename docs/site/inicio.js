// Página principal: a busca de estação e os avisos do Metrofor dos próximos dias
// (os mesmos da lite: docs/app/dados.js, regras.json "operacoes").
import { carregar } from "../app/dados.js";

const semAcento = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);

// Busca: nome exato (ou escolhido na lista) vai direto para a estação; o resto, para a lista filtrada.
const form = document.querySelector(".busca");
const opcoes = [...document.querySelectorAll("#lista-estacoes option")];
function estacao(texto) {
  const q = semAcento(texto);
  return q && opcoes.find((o) => semAcento(o.value) === q);
}
form?.addEventListener("submit", (e) => {
  const o = estacao(form.q.value);
  if (o) { e.preventDefault(); location.href = `estacoes/${o.dataset.slug}/`; }
});
form?.q.addEventListener("change", () => {
  const o = estacao(form.q.value);
  if (o) location.href = `estacoes/${o.dataset.slug}/`;
});

// Avisos: só aparecem quando o Metrofor anunciou operação especial de hoje em diante.
const quadro = document.querySelector(".avisos");
carregar().then((dados) => {
  if (!quadro || !dados.avisos.length) return;
  const nomes = ["domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado"];
  const dia = (d) => {
    const [a, m, n] = d.split("-").map(Number);
    return `${nomes[new Date(Date.UTC(a, m - 1, n)).getUTCDay()]} (${n}/${m})`;
  };
  quadro.querySelector(".avisos-lista").innerHTML = dados.avisos.slice(0, 3).map((a) => {
    const quando = a.hoje ? "Hoje" : a.dias.map(dia).join(", ");
    const texto = `<strong>${esc(quando[0].toUpperCase() + quando.slice(1))}:</strong> ${esc(a.titulo)}`;
    return /^https:\/\//.test(a.link || "")
      ? `<p class="aviso-dia"><a href="${esc(a.link)}" target="_blank" rel="noopener">${texto} ›</a></p>`
      : `<p class="aviso-dia">${texto}</p>`;
  }).join("");
  quadro.hidden = false;
}).catch(() => {});
