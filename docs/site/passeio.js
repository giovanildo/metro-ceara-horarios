// Gerado por tools/site.py (PASSEIO): não edite à mão.
// A faixa do passeio pelo site: só aparece para quem tocou numa parada da página
// "Passeio pelo site" (fica guardado só neste aparelho). Mostra em que parada a
// pessoa está, leva à próxima e o ✕ encerra. Carregada pelas páginas do site, pela
// lite (docs/app/) e pelos passatempos.
(() => {
  const PARADAS = [["app/", "Horários agora"], ["linhas/", "As linhas"], ["nomes-das-estacoes/", "Por que esses nomes?"], ["fortaleza-de-metro/", "Fortaleza de metrô"], ["literatura/", "Na literatura e nas artes"], ["passatempos/", "Passatempos"], ["conheca-o-app/", "Conheça o app"], ["historia/", "História do app"]];
  const CHAVE = "passeio";
  const script = document.currentScript;
  if (!script) return;
  const raiz = new URL("../", script.src);
  const ler = () => { try { const n = parseInt(localStorage.getItem(CHAVE), 10); return n >= 0 && n < PARADAS.length ? n : null; } catch { return null; } };
  const guardar = (n) => { try { n === null ? localStorage.removeItem(CHAVE) : localStorage.setItem(CHAVE, String(n)); } catch { /* sem guardar, sem faixa */ } };

  // Tocar numa parada (na página do passeio ou na faixa) começa ou segue o passeio.
  document.addEventListener("click", (e) => {
    const a = e.target.closest && e.target.closest("[data-passeio]");
    if (a) guardar(parseInt(a.dataset.passeio, 10));
  });

  let atual = ler();
  if (atual === null) return;
  const caminho = location.pathname.startsWith(raiz.pathname)
    ? location.pathname.slice(raiz.pathname.length).replace(/index\.html$/, "") : null;
  const aqui = PARADAS.findIndex((p) => p[0] === caminho);
  if (aqui >= 0) { atual = aqui; guardar(aqui); }
  const naParada = aqui >= 0;

  const estilo = document.createElement("style");
  estilo.textContent = `
    .passeio-faixa { position: sticky; top: 0; z-index: 30; display: flex; align-items: center; gap: 10px;
      background: #1b1a1f; color: #fff; padding: 9px 14px; font: 600 15px/1.3 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
    .topo .passeio-faixa { position: static; margin: 0 -16px; }
    .passeio-faixa .bolinhas { display: flex; gap: 4px; align-items: center; flex: none; }
    .passeio-faixa .bolinhas i { width: 9px; height: 9px; border-radius: 50%; border: 2px solid #fff; box-sizing: border-box; }
    .passeio-faixa .bolinhas i.feita { background: #fff; }
    .passeio-faixa .bolinhas i.aqui { width: 13px; height: 13px; background: #CE0F2E; border-color: #CE0F2E; }
    .passeio-faixa .conta { font-weight: 400; white-space: nowrap; }
    .passeio-faixa a { color: #fff; text-decoration: none; margin-left: auto; text-align: right; }
    .passeio-faixa button { background: none; border: 0; color: #fff; opacity: .75; font-size: 18px; padding: 0 2px 0 6px; cursor: pointer; }
    @media (max-width: 359px) { .passeio-faixa .bolinhas { display: none; } }`;
  document.head.appendChild(estilo);

  const faixa = document.createElement("div");
  faixa.className = "passeio-faixa";
  faixa.setAttribute("role", "navigation");
  faixa.setAttribute("aria-label", "Passeio pelo site");
  const bolinhas = PARADAS.map((_, i) => `<i class="${i < atual ? "feita" : i === atual ? "aqui" : ""}"></i>`).join("");
  const ultima = atual === PARADAS.length - 1;
  let link;
  if (!naParada) {
    link = `<a href="${new URL(PARADAS[atual][0], raiz)}" data-passeio="${atual}">Voltar ao passeio: ${PARADAS[atual][1]} ›</a>`;
  } else if (ultima) {
    link = `<a href="${new URL("passeio/", raiz)}" class="fim">Fim da linha! Ver o passeio ›</a>`;
  } else {
    link = `<a href="${new URL(PARADAS[atual + 1][0], raiz)}" data-passeio="${atual + 1}">Próxima: ${PARADAS[atual + 1][1]} ›</a>`;
  }
  faixa.innerHTML = `<span class="bolinhas" aria-hidden="true">${bolinhas}</span>`
    + `<span class="conta">${atual + 1} de ${PARADAS.length}</span>${link}`
    + `<button type="button" aria-label="Sair do passeio">✕</button>`;
  faixa.querySelector("button").addEventListener("click", () => { guardar(null); faixa.remove(); });
  faixa.querySelector(".fim")?.addEventListener("click", () => guardar(null));

  const topo = document.querySelector("nav.topo .largura") || null;
  if (topo) {
    faixa.style.flexBasis = "calc(100% + 32px)";
    topo.style.flexWrap = "wrap";
    topo.appendChild(faixa);
  } else {
    const cabeca = document.querySelector("body > header");
    if (cabeca) cabeca.after(faixa); else document.body.prepend(faixa);
  }
})();
