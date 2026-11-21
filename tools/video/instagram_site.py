"""Posts do Instagram sobre o site-guia: hoje só a tabela do dia 23 (app × lite × site); os dias
17 a 22 viraram vídeos de conversa em historias.py (21/11/2026).

Tira as capturas sozinho: serve docs/ num servidor local que fixa o relógio da
página (sábado 21/11/2026 às 07:25, para "Próximas partidas" não mostrar a
madrugada) e fotografa cada página no Chrome sem janela, com largura de
celular (412 px × 2,625 = 1082 px). As fotos do Commons e os vídeos vêm da
internet. Depois monta as imagens 4:5 e o Reels com as funções de
instagram_app.py.

    VIDEO_WORK=/tmp/metro-video python3 tools/video/instagram_site.py

Os recortes são em pixels da página fotografada (1082 de largura). Se o site
mudar de layout, confira as capturas em $VIDEO_WORK/insta/site/ e ajuste.
"""
import http.server
import io
import os
import subprocess
import sys
import threading

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import instagram_app as ig  # noqa: E402

DOCS = os.path.join(ig.ROOT, "docs")
SHOTS = os.path.join(ig.WORK, "site")
os.makedirs(SHOTS, exist_ok=True)
QUANDO = "2026-11-21T07:25:00-03:00"
PORTA = 8767
CHROME = os.environ.get("CHROME", "google-chrome")

# (nome, caminho no site, altura da janela em px CSS)
PAGINAS = [
    ("home", "", 6000),
    ("estacao", "estacoes/parangaba/", 9000),
    ("turismo", "fortaleza-de-metro/", 6000),
    ("literatura", "literatura/", 6000),
    ("leste", "linhas/linha-leste/", 5000),
    ("lite", "app/", 915),
]

INJ = ('<script>(()=>{const R=Date,off=new R("%s")-R.now();'
       'class D extends R{constructor(...a){a.length?super(...a):super(R.now()+off)}static now(){return R.now()+off}}'
       'window.Date=D})()</script>' % QUANDO).encode()
# Só na lite: já visitou (sem o Conte sobre você), convite do Android fechado e
# a localização perto de São Benedito, liberada.
INJ_LITE = ('<script>(()=>{try{localStorage.setItem("visitas","3");localStorage.setItem("localizacao","sim");'
            'localStorage.setItem("convite-fechado","1")}catch(e){}'
            'const pos={coords:{latitude:-3.7330,longitude:-38.5363,accuracy:15},timestamp:Date.now()};'
            'Object.defineProperty(navigator,"geolocation",{value:{getCurrentPosition:(ok)=>setTimeout(()=>ok(pos),50),'
            'watchPosition:(ok)=>{setTimeout(()=>ok(pos),50);return 1},clearWatch:()=>{}}});'
            'if(navigator.permissions){const q=navigator.permissions.query.bind(navigator.permissions);'
            'navigator.permissions.query=(d)=>d&&d.name==="geolocation"?Promise.resolve({state:"granted"}):q(d)}'
            '})()</script>').encode()


class Pagina(http.server.SimpleHTTPRequestHandler):
    """docs/ com o relógio da página trocado (só nas .html)."""

    def __init__(self, *a, **k):
        super().__init__(*a, directory=DOCS, **k)

    def send_head(self):
        p = self.translate_path(self.path)
        if os.path.isdir(p):
            p = os.path.join(p, "index.html")
        if p.endswith(".html") and os.path.exists(p):
            extra = INJ_LITE if "/app/" in p.replace(os.sep, "/") else b""
            b = open(p, "rb").read().replace(b"<head>", b"<head>" + INJ + extra, 1)
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(b)))
            self.end_headers()
            return io.BytesIO(b)
        return super().send_head()

    def log_message(self, *a):
        pass


def capturar():
    srv = http.server.ThreadingHTTPServer(("127.0.0.1", PORTA), Pagina)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    try:
        for nome, caminho, altura in PAGINAS:
            out = os.path.join(SHOTS, nome + ".png")
            subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                            "--force-device-scale-factor=2.625", f"--window-size=412,{altura}",
                            "--virtual-time-budget=8000", f"--screenshot={out}",
                            f"http://127.0.0.1:{PORTA}/{caminho}"], check=True, capture_output=True)
            print(out)
    finally:
        srv.shutdown()


def s(nome):
    return os.path.join(SHOTS, nome + ".png")


def rolagem(nome, y0, y1, espera=1.5, vel=550, fim=1.0):
    """Vídeo 1080x2400 descendo a página de y0 a y1 (px da captura)."""
    dur = espera + (y1 - y0) / vel + fim
    out = os.path.join(ig.WORK, f"rolagem-{nome}-{y0}.mp4")
    y = f"min({y1},max({y0},{y0}+(t-{espera})*{vel}))"
    subprocess.run([ig.FF, "-v", "error", "-y", "-loop", "1", "-framerate", "30", "-t", f"{dur:.2f}",
                    "-i", s(nome), "-vf", f"crop=1082:2404:0:'{y}',scale=1080:2400,format=yuv420p",
                    "-c:v", "libx264", "-crf", "16", "-r", "30", out], check=True)
    return out


def final_site(name, n, total):
    ig.final(name, n=n, total=total, title="Tudo isso no site", body=ig.SITE, nota="Link na bio")


from PIL import Image, ImageDraw  # noqa: E402

PLAY_TXT = "Google Play · Android 7 ou mais novo"
LITE_URL = "giovanildo.github.io/metro-ceara-horarios/app"


def plataforma(name, title, publico, piece, itens, onde, n, total, tema=ig.AZUL):
    """Uma plataforma: nome, para quem é, a tela à esquerda e o que ela faz à direita."""
    W, H = ig.W, ig.H
    im = Image.new("RGB", (W, H), tema["bg"])
    d = ImageDraw.Draw(im)
    ig.numero(d, n, total, tema)
    y = ig.centered(d, title, ig.F(ig.B, 76), 60, ig.WHITE, gap=8)
    fp = ig.F(ig.B, 36)
    y = ig.centered(d, publico, fp, y + 6, tema["sub"], width=W - 140, gap=8) + 34
    # tela à esquerda
    src, box = piece
    shot = Image.open(src).convert("RGB").crop(box)
    foot = 170 + 96
    ph = H - foot - y
    k = min(470 / shot.width, ph / shot.height)
    shot = shot.resize((int(shot.width * k), int(shot.height * k)), Image.LANCZOS)
    fr = Image.new("RGB", (shot.width + 20, shot.height + 20), ig.WHITE)
    fr.paste(shot, (10, 10))
    fr = ig.rounded(fr, 30)
    x0 = 50
    im.paste(fr, (x0, y), fr)
    # lista à direita
    cx0, cx1 = x0 + fr.width + 30, W - 50
    fi = ig.F(ig.R, 34)
    lh = 44
    rows = [ig.fr.wrap(d, t, fi, cx1 - cx0 - 104) for t in itens]
    hh = 30 + sum(len(r) * lh + 26 for r in rows) + 10
    ly = y + max(0, (fr.height - hh) // 2)
    d.rounded_rectangle((cx0, ly, cx1, ly + hh), radius=30, fill=ig.WHITE)
    yy = ly + 30
    for r in rows:
        ig.marca(d, cx0 + 18, yy + 2, True)
        for line in r:
            d.text((cx0 + 78, yy), line, font=fi, fill=ig.INK)
            yy += lh
        yy += 26
    # onde
    fo = ig.F(ig.B, 34)
    tw = d.textlength(onde, font=fo)
    oy = H - foot + 10
    d.rounded_rectangle(((W - tw) / 2 - 34, oy, (W + tw) / 2 + 34, oy + 70), radius=35, fill=ig.WHITE)
    d.text(((W - tw) / 2, oy + 13), onde, font=fo, fill=tema["cor"])
    ig.rodape(im, d, tema)
    ig.save(im, name)


# (recurso, app, lite, site): True/False, ou um texto curto.
COMPARA = [
    ("Próximos horários", True, True, True),
    ("Roteiro com integração", True, True, False),
    ("Aviso da hora de sair", True, True, False),
    ("Aviso antes de descer (modo viagem)", True, False, False),
    ("Widget na tela inicial", True, False, False),
    ("Abre sem internet", True, True, False),
    ("Funciona no iPhone", False, True, True),
    ("Fotos, história e turismo", False, False, True),
    ("Precisa instalar?", "Play Store", "Não", "Não"),
]


def tabela(name, n=None, total=None, story=False, tema=ig.AZUL):
    W, H = ig.W, (1920 if story else ig.H)
    im = Image.new("RGB", (W, H), tema["bg"])
    d = ImageDraw.Draw(im)
    ig.numero(d, n, total, tema)
    y = 230 if story else 60
    y = ig.centered(d, "App, lite ou site?", ig.F(ig.B, 80), y, ig.WHITE)
    y = ig.centered(d, "O que cada um faz", ig.F(ig.R, 40), y, tema["sub"]) + 30
    x0, x1 = 40, W - 40
    cw = 150 if not story else 160
    cols = [x1 - 3 * cw, x1 - 2 * cw, x1 - cw]
    rh = 118 if story else 84
    top = y
    d.rounded_rectangle((x0, top, x1, top + 90 + rh * len(COMPARA) + 10), radius=30, fill=ig.WHITE)
    fh = ig.F(ig.B, 40 if story else 32)
    for cx, t in zip(cols, ["App", "Lite", "Site"]):
        d.text((cx + cw // 2 - d.textlength(t, font=fh) / 2, top + 28), t, font=fh, fill=tema["cor"])
    yy = top + 90
    fr_ = ig.F(ig.R, 38 if story else 31)
    fs = ig.F(ig.B, 30 if story else 26)
    for i, (rec, *vals) in enumerate(COMPARA):
        if i % 2 == 0:
            d.rectangle((x0, yy, x1, yy + rh), fill=(240, 243, 250))
        lines = ig.fr.wrap(d, rec, fr_, cols[0] - x0 - 40)
        lh = fr_.size + 8
        ty = yy + (rh - len(lines) * lh) / 2
        for line in lines:
            d.text((x0 + 30, ty), line, font=fr_, fill=ig.INK)
            ty += lh
        for cx, v in zip(cols, vals):
            if isinstance(v, bool):
                ig.marca(d, cx + cw // 2 - 22, yy + rh / 2 - 22, v)
            else:
                d.text((cx + cw // 2 - d.textlength(v, font=fs) / 2, yy + rh / 2 - 17), v, font=fs, fill=ig.INK)
        yy += rh
    y = yy + 40
    if story:
        y = ig.centered(d, "Todos grátis, sem anúncio e sem cadastro", ig.F(ig.B, 40), y + 20, ig.WHITE) + 20
        y = ig.centered(d, "Link na bio", ig.F(ig.R, 40), y, tema["sub"])
        ic, mask = ig.fr.rounded_icon(110)
        im.paste(ic, ((W - 110) // 2, min(H - 150, y + 40)), mask)
    else:
        ig.centered(d, "Todos grátis, sem anúncio e sem cadastro", ig.F(ig.B, 36), y, ig.WHITE)
        ig.rodape(im, d, tema)
    ig.save(im, name)


def main():
    """Só o dia 23 (a tabela app × lite × site). Os dias 17 a 22 viraram, em 21/11/2026, vídeos
    de conversa em historias.py; capturar(), rolagem(), plataforma() e final_site() ficam aqui
    para posts novos do site que precisem ser imagem ou rolagem."""
    # Dia 23 (sex 18/12) — Imagem e story: a tabela, para salvar e compartilhar
    tabela("dia23-app-lite-site.png")
    tabela("dia23-app-lite-site-story.png", story=True)



if __name__ == "__main__":
    main()
