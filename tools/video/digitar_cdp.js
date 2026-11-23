// Digita letra por letra (com acento) no campo em foco da aba do Chrome do emulador,
// pelo DevTools (Input.insertText), e anota em <saida> "segundos texto-até-aqui",
// contados a partir de <t0> (segundos desde a época).
// Precisa de: adb forward tcp:9222 localabstract:chrome_devtools_remote
//   node tools/video/digitar_cdp.js <filtro-do-endereço> <texto> <t0> <saida> [ms entre letras]
const fs = require("fs");
const [filtro, texto, t0, saida, ms = "190"] = process.argv.slice(2);
(async () => {
  const abas = await (await fetch("http://127.0.0.1:9222/json")).json();
  const aba = abas.find((a) => a.type === "page" && a.url.includes(filtro));
  if (!aba) { console.error("aba não encontrada:", filtro); process.exit(1); }
  const ws = new WebSocket(aba.webSocketDebuggerUrl);
  let id = 0;
  const send = (method, params) => new Promise((ok) => {
    const meu = ++id;
    const h = (m) => { const d = JSON.parse(m.data); if (d.id === meu) { ws.removeEventListener("message", h); ok(d); } };
    ws.addEventListener("message", h);
    ws.send(JSON.stringify({ id: meu, method, params }));
  });
  ws.onopen = async () => {
    let ate = "";
    for (const c of texto) {
      await send("Input.insertText", { text: c });
      ate += c;
      fs.appendFileSync(saida, `${(Date.now() / 1000 - +t0).toFixed(2)} ${ate}\n`);
      await new Promise((r) => setTimeout(r, +ms));
    }
    ws.close();
  };
})();
