// Guarda o app no celular para abrir sem internet. O app (HTML, CSS, JS,
// ícones) vem do cache na hora e é atualizado por trás; os horários tentam a
// internet primeiro e, sem ela, usam a última cópia.
const CACHE = "metro-fortaleza-v4";
const APP = ["./", "index.html", "app.css", "app.js", "dados.js", "manifest.webmanifest",
  "icon-180.png", "icon-192.png", "icon-512.png", "qr.png"];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(APP)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys()
    .then((nomes) => Promise.all(nomes.filter((n) => n !== CACHE).map((n) => caches.delete(n))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", (e) => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.hostname === "raw.githubusercontent.com") {
    // Horários: internet primeiro, cópia guardada se não houver.
    e.respondWith(fetch(req).then((r) => {
      if (r.ok) { const copia = r.clone(); caches.open(CACHE).then((c) => c.put(url.href, copia)); }
      return r;
    }).catch(() => caches.match(url.href)));
    return;
  }
  if (url.origin !== location.origin) return;
  // App: do cache na hora; a versão nova chega por trás e vale na próxima abertura.
  e.respondWith(caches.open(CACHE).then(async (c) => {
    const guardado = await c.match(req, { ignoreSearch: true });
    const rede = fetch(req).then((r) => { if (r.ok) c.put(req, r.clone()); return r; }).catch(() => guardado);
    return guardado || rede;
  }));
});
