// Vídeos do site (tools/site_videos.json): a página mostra só a miniatura guardada no
// próprio site. No toque, troca pelo vídeo do YouTube em modo sem cookies
// (youtube-nocookie.com). Sem JS, o link abre o vídeo no YouTube.
document.addEventListener("click", (e) => {
  const a = e.target.closest("a.video-tocar");
  if (!a || e.ctrlKey || e.metaKey || e.shiftKey) return;
  e.preventDefault();
  const f = document.createElement("iframe");
  const inicio = a.dataset.inicio ? `&start=${a.dataset.inicio}` : "";
  f.src = `https://www.youtube-nocookie.com/embed/${a.dataset.id}?autoplay=1&rel=0${inicio}`;
  f.title = a.querySelector(".visualmente-oculto")?.textContent || "Vídeo";
  f.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share";
  f.referrerPolicy = "strict-origin-when-cross-origin";
  f.allowFullscreen = true;
  a.replaceWith(f);
});
