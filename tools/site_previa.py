"""Prévia de link do site (docs/img/previa.png, 1200x630): a imagem que aparece ao mandar
o endereço no WhatsApp, com o mapa das linhas de Fortaleza desenhado por tools/site.py.

Monta uma página com o mapa e tira o print com o Chrome sem janela.
Uso: python3 tools/site_previa.py [pasta-com-os-json-da-grade]
"""
import os
import importlib.util
import shutil
import subprocess
import sys
import tempfile

# tools/site.py tem o nome do módulo "site" do Python: carrega pelo caminho.
_spec = importlib.util.spec_from_file_location("gerador_site", os.path.join(os.path.dirname(os.path.abspath(__file__)), "site.py"))
gerador = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gerador)

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DESTINO = os.path.join(RAIZ, "docs", "img", "previa.png")
ICONE = os.path.join(RAIZ, "docs", "img", "icon.png")

PAGINA = """<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><style>
  html, body {{ margin: 0; width: 1200px; height: 630px; overflow: hidden; }}
  body {{ background: #CE0F2E; color: #fff; font-family: "Noto Sans", system-ui, sans-serif;
         display: grid; grid-template-columns: 640px 1fr; align-items: center; gap: 0; }}
  .texto {{ padding: 0 0 0 72px; }}
  .marca {{ display: flex; align-items: center; gap: 16px; font-weight: 700; font-size: 30px; }}
  .marca img {{ width: 64px; height: 64px; border-radius: 16px; }}
  h1 {{ font-size: 76px; line-height: 1; margin: 22px 0 16px; letter-spacing: -1px; }}
  .lead {{ font-size: 31px; line-height: 1.3; margin: 0; }}
  .numeros {{ font-size: 28px; font-weight: 700; margin: 22px 0 0; }}
  .selo {{ display: inline-block; background: #fff; color: #CE0F2E; font-weight: 700; font-size: 24px;
          padding: 10px 24px; border-radius: 999px; margin-top: 24px; }}
  .painel {{ background: #fff; border-radius: 32px; margin: 0 48px 0 8px; padding: 22px 16px 22px 64px; }}
  .mapa {{ width: 100%; height: auto; display: block; max-height: 540px; overflow: visible; }}
  .mapa .trilho-mapa {{ fill: none; stroke: var(--cor); stroke-width: 7; stroke-linecap: round; stroke-linejoin: round; }}
  .mapa .ponto {{ fill: #fff; stroke: var(--cor); stroke-width: 3; }}
  .mapa .ponto.integra {{ stroke: #1d1b1e; stroke-width: 3.5; }}
  .mapa .alvo {{ fill: transparent; }}
  .mapa text {{ font: 700 14px "Noto Sans", sans-serif; fill: #1d1b1e; paint-order: stroke; stroke: #fff;
               stroke-width: 4px; stroke-linejoin: round; }}
</style></head><body>
  <div class="texto">
    <div class="marca"><img src="file://{icone}" alt="">Metrô Ceará</div>
    <h1>Metrô e VLT<br>do Ceará</h1>
    <p class="lead">Horários, estações, integração<br>e o que tem perto</p>
    <p class="numeros">{estacoes} estações · {linhas} linhas<br>Fortaleza · Cariri · Sobral</p>
    <span class="selo">grátis · sem anúncios · não oficial</span>
  </div>
  <div class="painel">{mapa}</div>
</body></html>"""


def main():
    pasta = sys.argv[1] if len(sys.argv) > 1 else None
    d, _, _ = gerador.carregar_dados(pasta)
    html = PAGINA.format(icone=ICONE, estacoes=len(d.estacoes), linhas=len(d.linhas),
                         mapa=gerador.mapa_svg(d, "Fortaleza", 400, mx=62))
    chrome = shutil.which("google-chrome") or shutil.which("chromium") or shutil.which("chromium-browser")
    if not chrome:
        sys.exit("precisa do Chrome (ou Chromium) para tirar o print")
    with tempfile.TemporaryDirectory() as tmp:
        pagina = os.path.join(tmp, "previa.html")
        with open(pagina, "w", encoding="utf-8") as fh:
            fh.write(html)
        saida = os.path.join(tmp, "previa.png")
        subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        "--force-device-scale-factor=1", "--window-size=1200,630",
                        f"--user-data-dir={os.path.join(tmp, 'perfil')}",
                        f"--screenshot={saida}", f"file://{pagina}"],
                       check=True, capture_output=True)
        try:  # otimiza se o Pillow estiver instalado
            from PIL import Image
            Image.open(saida).convert("RGB").save(DESTINO, optimize=True)
        except ImportError:
            shutil.copy(saida, DESTINO)
    print(os.path.relpath(DESTINO, RAIZ))


if __name__ == "__main__":
    main()
