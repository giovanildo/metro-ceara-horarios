"""Versão natalina do vídeo de divulgação (referencias/metro ceará.mp4, 1080x1920, 25 s).

Sem o projeto original, a decoração vai por cima, quadro a quadro, sem cobrir o conteúdo:
- pisca-pisca no topo (as lâmpadas acendem em ondas e brilham no fundo escuro);
- estrelinhas douradas que descem devagar e piscam, só onde o fundo está livre (escuro na
  parte do celular, vermelho no cartão final);
- no cartão vermelho do fim (a partir de ~21,5 s): gorro de Papai Noel no ícone e
  "Boas festas!" em cima, com uma estrela.
O áudio do original é mantido.

    python3 tools/video/natal_divulgacao.py
    python3 tools/video/natal_divulgacao.py quadro 6 23.5   # prévias desses segundos em /tmp

Sai em dois formatos: metro-ceara-natal.mp4 (story/Reels, 9:16) e metro-ceara-natal-post.mp4
(feed, 4:5: o original em altura cheia no centro, laterais continuando o fundo).
"""
import math
import os
import random
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402
import natal  # noqa: E402

SRC = os.path.join(fr.ROOT, "referencias", "metro ceará.mp4")
DST = os.path.join(fr.ROOT, "divulgacao", "instagram-natal", "metro-ceara-natal.mp4")
DST_POST = os.path.join(fr.ROOT, "divulgacao", "instagram-natal", "metro-ceara-natal-post.mp4")
FF = os.environ.get("FFMPEG", "ffmpeg")
W, H, FPS = 1080, 1920, 30
CARTAO = 21.5              # o cartão vermelho começa aqui
ICONE = (400, 430, 680, 710)  # o ícone no cartão, depois que para de crescer
# Geometria do formato atual: story = o original; post = o original em altura cheia (×0,703)
# no centro de 1080x1350, com as laterais continuando o fundo.
POST_H = 1350
POST_K = POST_H / H
POST_X = round((W - W * POST_K) / 2)
G = {"k": 1.0, "x": 0, "topo": 40, "queda": 52, "festas_y": 150, "festas_fonte": 96}
DOURADO = natal.DOURADO
BRANCO = (255, 255, 255)

rnd = random.Random(2512)
ESTRELAS = [(rnd.uniform(20, W - 20), rnd.uniform(0, H), rnd.uniform(8, 19), rnd.uniform(0, 6.28),
             rnd.uniform(18, 45)) for _ in range(90)]


px_h = [H]


def fundo_livre(px, x, y, r, cartao):
    """Ponto e anel em volta só com o fundo: escuro (celular) ou vermelho liso (cartão)."""
    for rr in (0, r + 10, r + 26, r + 40):
        for k in range(1 if rr == 0 else 12):
            a = k * math.pi / 6
            ax, ay = int(x + rr * math.cos(a)), int(y + rr * math.sin(a))
            if not (0 <= ax < W and 0 <= ay < px_h[0]):
                return False
            c = px[ax, ay]
            if cartao:
                if not (c[0] > 200 and c[1] < 50 and c[2] < 60):
                    return False
            elif sum(c) > 120:
                return False
    return True


def pisca(im, t, cartao):
    d = ImageDraw.Draw(im, "RGBA")
    topo, queda = G["topo"], G["queda"]
    fade = min(1.0, max(0.0, (t - 0.3) / 0.8))
    if fade <= 0:
        return
    pontos = [(x, topo + queda * math.sin(math.pi * ((x % 360) / 360))) for x in range(-20, W + 21, 6)]
    d.line(pontos, fill=(40, 45, 40, int(255 * fade)) if not cartao else (120, 15, 20, 255), width=4)
    cores = natal.LAMPADAS if not cartao else [(255, 255, 255), (40, 170, 90), (250, 200, 50), (90, 150, 240)]
    for i, x in enumerate(range(20, W, 60)):
        y = topo + queda * math.sin(math.pi * ((x % 360) / 360))
        cor = cores[i % 4]
        brilho = 0.55 + 0.45 * math.sin(t * 5.0 + i * 1.3)
        r = 14
        d.ellipse((x - r * 2.6, y + 2, x + r * 2.6, y + 2 + r * 5), fill=cor + (int(90 * brilho * fade),))
        d.rectangle((x - 5, y - 2, x + 5, y + 8), fill=(90, 90, 90, int(255 * fade)))
        c = tuple(min(255, int(v * (0.6 + 0.4 * brilho) + 70 * brilho)) for v in cor)
        d.ellipse((x - r, y + 8, x + r, y + 8 + r * 2.2), fill=c + (int(255 * fade),))


def estrelas(im, t, cartao):
    px = im.load()
    px_h[0] = im.height
    d = ImageDraw.Draw(im)
    for x0, y0, r, fase, vel in ESTRELAS:
        x = x0 + 12 * math.sin(t * 0.8 + fase)
        y = (y0 + t * vel) % H
        if y >= im.height:
            continue
        if y < 140:
            continue
        if not fundo_livre(px, x, y, r, cartao):
            continue
        b = 0.45 + 0.55 * (0.5 + 0.5 * math.sin(t * 3.0 + fase))
        cor = (255, 236, 170) if cartao else DOURADO
        fundo = px[int(x), int(y)]
        c = tuple(int(a * b + f * (1 - b)) for a, f in zip(cor, fundo))
        natal._estrela(d, x, y, r * (0.7 + 0.3 * b), c)


def cartao_natal(im, t):
    """Gorro no ícone e "Boas festas!" em cima, aparecendo meio segundo depois do cartão."""
    p = natal.h.pop((t - CARTAO - 0.6) / 0.45)
    if p <= 0.02:
        return
    k, ox = G["k"], G["x"]
    ix, iy = ox + ICONE[0] * k, ICONE[1] * k
    size = (ICONE[2] - ICONE[0]) * k
    g = natal.gorro(max(8, int(size * 0.8 * p)))
    im.paste(g, (int(ix - size * 0.24), int(iy - size * 0.52)), g)
    a = natal.h.ease((t - CARTAO - 0.9) / 0.4)
    if a <= 0:
        return
    d = ImageDraw.Draw(im)
    fnt = fr.font(fr.BOLD, int(G["festas_fonte"] * natal.h.pop((t - CARTAO - 0.9) / 0.4)) or 1)
    texto = "Boas festas!"
    tw = d.textlength(texto, font=fnt)
    y = G["festas_y"]
    fundo = im.getpixel((20, y + 110))
    cor = tuple(int(c * a + f * (1 - a)) for c, f in zip(BRANCO, fundo))
    d.text(((W - tw) / 2, y), texto, font=fnt, fill=cor)
    s = 26 + 6 * math.sin(t * 4)
    ouro = tuple(int(c * a + f * (1 - a)) for c, f in zip((255, 220, 90), fundo))
    meio = fnt.size * 0.62
    natal._estrela(d, (W - tw) / 2 - 60, y + meio, s, ouro)
    natal._estrela(d, (W + tw) / 2 + 60, y + meio, s, ouro)


def decorar(im, t):
    cart = t >= CARTAO and im.getpixel((20, 100))[0] > 180
    estrelas(im, t, cart)
    pisca(im, t, cart)
    if cart:
        cartao_natal(im, t)
    return im


def para_post(im):
    """O original em altura cheia no centro; cada lateral repete a cor da borda daquela linha."""
    pequeno = im.resize((round(W * POST_K), POST_H), Image.LANCZOS)
    tela = Image.new("RGB", (W, POST_H))
    esq = pequeno.crop((2, 0, 3, POST_H)).resize((POST_X, POST_H))
    dir_ = pequeno.crop((pequeno.width - 3, 0, pequeno.width - 2, POST_H)).resize((W - POST_X - pequeno.width, POST_H))
    tela.paste(esq, (0, 0))
    tela.paste(dir_, (POST_X + pequeno.width, 0))
    tela.paste(pequeno, (POST_X, 0))
    return tela


def formato(nome):
    if nome == "post":
        G.update(k=POST_K, x=POST_X, topo=26, queda=40, festas_y=112, festas_fonte=76)
    else:
        G.update(k=1.0, x=0, topo=40, queda=52, festas_y=150, festas_fonte=96)


def quadros():
    p = subprocess.Popen([FF, "-v", "error", "-i", SRC, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
                         stdout=subprocess.PIPE)
    n = 0
    while True:
        b = p.stdout.read(W * H * 3)
        if len(b) < W * H * 3:
            break
        yield n / FPS, Image.frombytes("RGB", (W, H), b)
        n += 1
    p.wait()


def main():
    if len(sys.argv) > 2 and sys.argv[1] == "quadro":
        alvos = [float(a) for a in sys.argv[2:]]
        for t, im in quadros():
            for a in alvos:
                if abs(t - a) < 0.5 / FPS:
                    for nome in ("story", "post"):
                        formato(nome)
                        q = im.copy() if nome == "story" else para_post(im)
                        out = f"/tmp/natal-divulgacao-{nome}-{a}.png"
                        decorar(q, t).save(out)
                        print(out)
        return
    os.makedirs(os.path.dirname(DST), exist_ok=True)
    for nome, dst, hh in (("story", DST, H), ("post", DST_POST, POST_H)):
        formato(nome)
        enc = subprocess.Popen([FF, "-y", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{hh}",
                                "-r", str(FPS), "-i", "-", "-i", SRC, "-map", "0:v", "-map", "1:a", "-c:a", "copy",
                                "-c:v", "libx264", "-preset", "slow", "-crf", "18", "-pix_fmt", "yuv420p",
                                "-movflags", "+faststart", "-shortest", dst], stdin=subprocess.PIPE)
        for t, im in quadros():
            q = im if nome == "story" else para_post(im)
            enc.stdin.write(decorar(q, t).tobytes())
        enc.stdin.close()
        enc.wait()
        print(dst)


if __name__ == "__main__":
    main()
