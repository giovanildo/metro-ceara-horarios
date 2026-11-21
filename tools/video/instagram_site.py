"""Posts do Instagram que mostram o site-guia (dias 17 a 21 de divulgacao/instagram-calendario/).

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
    capturar()

    # Dia 17 (sex 11/12) — Reels: rolando o site (página inicial e Parangaba)
    partes = [rolagem("home", 0, 4900), rolagem("estacao", 0, 4250)]
    lista_txt = os.path.join(ig.WORK, "site-list.txt")
    with open(lista_txt, "w") as fh:
        fh.writelines(f"file '{p}'\n" for p in partes)
    subprocess.run([ig.FF, "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", lista_txt, "-c", "copy",
                    os.path.join(ig.WORK, "site.mp4")], check=True)
    ig.reels("dia17-reels-o-site.mp4", "site.mp4", "Conheça o site do Metrô Ceará",
             "Horários, fotos e histórias das 62 estações, em qualquer celular", start=0)

    # Dia 18 (sáb 12/12) — Carrossel: a página de uma estação (Parangaba)
    T = 6
    ig.tela("dia18-site-estacao-1.png", "Cada estação tem a sua página",
            "Os próximos horários, o endereço e até um trecho de livro", [(s("estacao"), (0, 0, 1082, 1980))],
            n=1, total=T)
    ig.tela("dia18-site-estacao-2.png", "Fotos de verdade",
            "Mais de 400 fotos do Wikimedia Commons, com o nome de quem fotografou",
            [(s("estacao"), (0, 2780, 1082, 4200))], n=2, total=T)
    ig.tela("dia18-site-estacao-3.png", "Por que esse nome?",
            "Onde a estação fica na linha e de onde vem o nome",
            [(s("estacao"), (0, 8150, 1082, 9950))], n=3, total=T)
    ig.tela("dia18-site-estacao-4.png", "O que tem perto",
            "Igreja, terminal, hospital, faculdade, shopping: com os minutos a pé",
            [(s("estacao"), (0, 12350, 1082, 14100))], n=4, total=T)
    ig.tela("dia18-site-estacao-5.png", "Vídeos e literatura",
            "Vídeos da estação no YouTube e o que os livros contam dela",
            [(s("estacao"), (0, 15600, 1082, 16580)), (s("estacao"), (0, 18330, 1082, 19300))], n=5, total=T)
    final_site("dia18-site-estacao-6.png", 6, T)

    # Dia 19 (seg 14/12) — Carrossel: Fortaleza de metrô (para quem visita)
    T = 5
    ig.tela("dia19-site-turismo-1.png", "Vai receber visita?",
            "O guia Fortaleza de metrô: do aeroporto à praia e ao Centro histórico",
            [(s("turismo"), (0, 0, 1082, 1260))], n=1, total=T)
    ig.tela("dia19-site-turismo-2.png", "Do aeroporto ao Mucuripe",
            "De VLT e sem pagar passagem, com uma integração",
            [(s("turismo"), (0, 3980, 1082, 4980)), (s("turismo"), (0, 8650, 1082, 9420))], n=2, total=T)
    ig.tela("dia19-site-turismo-3.png", "Onde os trilhos chegam",
            "E onde não chegam: a estação mais perto e quanto falta a pé",
            [(s("turismo"), (0, 5760, 1082, 7480))], n=3, total=T)
    ig.tela("dia19-site-turismo-4.png", "Centro histórico a pé",
            "Theatro José de Alencar, Praça do Ferreira, Catedral e Mercado Central",
            [(s("turismo"), (0, 9420, 1082, 10050))], n=4, total=T)
    final_site("dia19-site-turismo-5.png", 5, T)

    # Dia 20 (ter 15/12) — Carrossel: o trem na literatura e nas artes
    T = 5
    ig.tela("dia20-site-literatura-1.png", "O trem na literatura e nas artes",
            "Romances, crônicas, cordéis e murais sobre os trilhos do Ceará",
            [(s("literatura"), (0, 0, 1082, 1440))], n=1, total=T)
    ig.tela("dia20-site-literatura-2.png", "Nos romances",
            "José de Alencar, Rachel de Queiroz, Domingos Olímpio…",
            [(s("literatura"), (0, 1580, 1082, 2480))], n=2, total=T)
    ig.tela("dia20-site-literatura-3.png", "No cordel",
            "O centenário da estação do Crato virou livro de cordel",
            [(s("literatura"), (0, 12030, 1082, 13050))], n=3, total=T)
    ig.tela("dia20-site-literatura-4.png", "Arte nas estações",
            "Os murais da Parangaba contam a história da ferrovia e do bairro",
            [(s("literatura"), (0, 13280, 1082, 14450))], n=4, total=T)
    final_site("dia20-site-literatura-5.png", 5, T)

    # Dia 21 (qua 16/12) — Carrossel: Linha Leste em obras
    T = 4
    ig.tela("dia21-site-linha-leste-1.png", "Linha Leste: como está a obra?",
            "Do Centro ao Papicu por baixo da Aldeota, prevista para o fim de 2028",
            [(s("leste"), (0, 0, 1082, 1600))], n=1, total=T)
    ig.tela("dia21-site-linha-leste-2.png", "8 estações previstas",
            "Tirol-Moura Brasil, Chico da Silva, Sé, Colégio Militar, Luíza Távora, Nunes Valente, "
            "Leonardo Mota e Papicu",
            [(s("leste"), (0, 2380, 1082, 3620))], n=2, total=T)
    ig.tela("dia21-site-linha-leste-3.png", "A história da obra",
            "Dos tatuzões de 2013 ao andamento de hoje, com a fonte de cada número",
            [(s("leste"), (0, 4870, 1082, 6250))], n=3, total=T)
    final_site("dia21-site-linha-leste-4.png", 4, T)


if __name__ == "__main__":
    main()

    # Dia 22 (qui 17/12) — Carrossel: app, lite e site, para quem é cada um
    T = 6
    ig.capa("dia22-plataformas-1.png", "App, lite ou site?", "Qual Metrô Ceará é para você?",
            "São três jeitos de usar. Todos grátis, sem anúncio e sem cadastro.", total=T)
    plataforma("dia22-plataformas-2.png", "App para Android", "Para quem pega metrô ou VLT todo dia",
               (ig.cap("inicio.png"), (0, 100, 1080, 2340)), [
                   "Avisa antes da sua estação, até com a tela desligada",
                   "Widget com os horários na tela inicial",
                   "Avisa a hora de sair de casa",
                   "Casa e Trabalho a um toque",
                   "Passatempos para a viagem",
               ], PLAY_TXT, 2, T)
    plataforma("dia22-plataformas-3.png", "Versão lite", "Para quem tem iPhone ou não quer instalar nada",
               (s("lite"), (0, 0, 1082, 2400)), [
                   "Abre no navegador de qualquer celular",
                   "Estação mais perto e próximos horários",
                   "Roteiro com integração",
                   "Lembrete da hora de sair",
                   "Vira um ícone na tela inicial e abre até sem internet",
               ], LITE_URL, 3, T)
    plataforma("dia22-plataformas-4.png", "Site", "Para quem quer conhecer, planejar ou pesquisar",
               (s("home"), (0, 0, 1082, 2300)), [
                   "62 estações com fotos, endereço e o que tem perto",
                   "Guia para quem visita Fortaleza",
                   "A história dos nomes, literatura e artes",
                   "A obra da Linha Leste",
               ], ig.SITE, 4, T)
    tabela("dia22-plataformas-5.png", n=5, total=T)
    ig.final("dia22-plataformas-6.png", n=6, total=T, qr=ig.PLAY, qr_text="Google Play",
             title="Escolha o seu", body="Lite e site: link na bio")

    # Dia 23 (sex 18/12) — Imagem e story: a tabela, para salvar e compartilhar
    tabela("dia23-app-lite-site.png")
    tabela("dia23-app-lite-site-story.png", story=True)

