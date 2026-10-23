// Roda um trecho de JavaScript na aba do Chrome do emulador cujo endereço contém o filtro.
// Precisa de: adb forward tcp:9222 localabstract:chrome_devtools_remote
//   node tools/video/cdp.js <filtro-do-endereço> <arquivo.js>
const [filtro, arquivo] = process.argv.slice(2);
const codigo = require("fs").readFileSync(arquivo, "utf8");
(async () => {
  const abas = await (await fetch("http://127.0.0.1:9222/json")).json();
  const aba = abas.find((a) => a.type === "page" && a.url.includes(filtro));
  if (!aba) { console.error("aba não encontrada:", filtro); process.exit(1); }
  const ws = new WebSocket(aba.webSocketDebuggerUrl);
  ws.onopen = () => ws.send(JSON.stringify({ id: 1, method: "Runtime.evaluate", params: { expression: codigo } }));
  ws.onmessage = (m) => { console.log(JSON.parse(m.data).result?.result?.value ?? "ok"); ws.close(); };
})();
