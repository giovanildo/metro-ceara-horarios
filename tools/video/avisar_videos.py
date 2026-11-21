"""Vídeos do "Avisar alguém" (1.109/1.110) para o Instagram, dia 24 do calendário:

- divulgacao/instagram-calendario/dia24-avisar-story.mp4  (1080x1920, stories: legendas fora
  das faixas que o Instagram cobre, espaço para a figurinha de link no fim);
- divulgacao/instagram-calendario/dia24-avisar-post.mp4   (1080x1350, feed 4:5).

19 s, sem som (a música entra no Instagram). Fundo claro e a tela do app dentro de um celular,
com uma bolinha no papel do dedo. A história: um grupo vai de carro do Pirambu e espera na fila
do Cine São Luiz (Praça do Ferreira); você vai de metrô da Maracanaú, responde com "Avisar
alguém", o app avisa a chegada na José de Alencar e a galera fica sabendo. A conversa é genérica
(grupo "Cinema hoje"), sem imitar nenhum app.

Telas: tools/video/capturas/instagram/avisar-saoluiz-viagem.png e avisar-saoluiz-texto.png
(1080x2400, 21/11/2026, terça simulada 24/11 às 17:45, Maracanaú → Cineteatro São Luiz). A
notificação de chegada usa o texto do app (trip_arrived_belongings).

    python3 tools/video/avisar_videos.py              # os dois vídeos
    python3 tools/video/avisar_videos.py quadro 4.0   # prévia: PNGs desse segundo em $PREVIA (/tmp)
"""
import math
import os
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
CAP = os.path.join(HERE, "capturas", "instagram")
OUT = os.path.join(fr.ROOT, "divulgacao", "instagram-calendario")
FF = os.environ.get("FFMPEG", "ffmpeg")
FPS = 30
W = 1080
AZUL = (47, 69, 120)
SUB = (217, 226, 255)
AMARELO = (255, 204, 0)
WHITE = (255, 255, 255)
INK = (28, 28, 30)
CLARO = (246, 248, 252)
F = fr.font
B, R = fr.BOLD, fr.REG

VIAGEM = Image.open(os.path.join(CAP, "avisar-saoluiz-viagem.png")).convert("RGB")
TEXTO = Image.open(os.path.join(CAP, "avisar-saoluiz-texto.png")).convert("RGB")
# Na tela de 1080x2400: o botão "Avisar alguém".
BOTAO = (84, 2178, 996, 2280)
CORES_AMIGOS = {"Lia": (176, 60, 120), "Davi": (30, 120, 90)}
MINHA_SL = ["Vou pegar o metrô das 17:49", "na estação Maracanaú.", "Desço na estação José de Alencar",
            "por volta das 18:23 e vou a pé", "até Cineteatro São Luiz."]
SL_ANTES = [(0.3, ["A gente tá indo de carro,", "saindo do Pirambu"], "Lia"),
            (1.0, ["Vamos esperar na fila", "do São Luiz!"], "Davi"),
            (1.7, ["E você, já tá chegando?"], "Lia")]


def ease(x):
    x = max(0.0, min(1.0, x))
    return 1 - (1 - x) ** 3


def pop(x):
    """0→1 com um pequeno passo além (efeito de "saltar")."""
    x = max(0.0, min(1.0, x))
    c = 1.7
    return 1 + (c + 1) * (x - 1) ** 3 + c * (x - 1) ** 2


def rounded(im, r):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), r, fill=255)
    return m


def texto_central(d, text, fnt, y, fill=WHITE, width=W - 120, gap=8, alpha=1.0, bg=AZUL):
    if alpha < 1:
        fill = tuple(int(b + (f - b) * alpha) for f, b in zip(fill, bg))
    for line in fr.wrap(d, text, fnt, width):
        d.text(((W - d.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
        y += fnt.size + gap
    return y


def anel(d, box, t, raio=40):
    """Contorno amarelo que pulsa em volta de uma área."""
    k = 6 + 6 * (0.5 + 0.5 * math.sin(t * 2 * math.pi * 1.6))
    x1, y1, x2, y2 = box
    r = min(raio + k, (y2 - y1) / 2 + k - 1)
    d.rounded_rectangle((x1 - k, y1 - k, x2 + k, y2 + k), r, outline=AMARELO, width=9)


def bolha(d, x, y, w, linhas, fnt, cor, direita):
    h = len(linhas) * (fnt.size + 10) + 44
    x1 = W - 70 - w if direita else 70
    d.rounded_rectangle((x1, y, x1 + w, y + h), 34, fill=cor)
    yy = y + 22
    for ln in linhas:
        d.text((x1 + 30, yy), ln, font=fnt, fill=INK)
        yy += fnt.size + 10
    return y + h


class Formato:
    def __init__(self, nome, h, story):
        self.nome, self.h, self.story = nome, h, story
        # Nos stories, o Instagram cobre ~250 px em cima (perfil) e ~280 embaixo (responder):
        # nada importante ali.
        self.cap_y, self.cap = (270, 84) if story else (60, 70)


def legenda(im, text, y, size, t_local, fill=WHITE, bg=AZUL, sub=None, subfill=SUB):
    a = ease(t_local / 0.25)
    d = ImageDraw.Draw(im)
    yy = texto_central(d, text, F(B, size), y + (1 - a) * 30, fill=fill, alpha=a, bg=bg)
    if sub:
        texto_central(d, sub, F(R, int(size * 0.55)), yy + 6, fill=subfill, alpha=a, bg=bg)
    return yy


def toque(im, cx, cy, p, cor=(255, 204, 0)):
    """Círculo que cresce e some (p de 0 a 1)."""
    r = 30 + 120 * p
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 255, 255, int(170 * (1 - p))))
    od.ellipse((cx - 34, cy - 34, cx + 34, cy + 34), fill=cor + (int(230 * (1 - p * 0.6)),))
    return Image.alpha_composite(im.convert("RGBA"), ov).convert("RGB")


def celular(fm, src, y_tela):
    """Celular escuro com a captura, saindo pela base do quadro. Devolve (im, mapa)."""
    im = Image.new("RGB", (W, fm.h), CLARO)
    pw = 960 if fm.story else 720
    top = fm.cap_y + 2 * fm.cap + (70 if fm.story else 40)
    k = (pw - 40) / 1080
    jan_h = (fm.h - top - 20) / k
    janela = src.crop((0, int(y_tela), 1080, int(min(2400, y_tela + jan_h))))
    janela = janela.resize((pw - 40, int(janela.height * k)), Image.LANCZOS)
    px = (W - pw) // 2
    d = ImageDraw.Draw(im)
    fundo = top + janela.height + 40
    if fundo > fm.h - 10:
        fundo = fm.h + 200
    d.rounded_rectangle((px + 8, top + 14, px + pw + 8, fundo + 14), 70, fill=(210, 214, 224))  # sombra
    d.rounded_rectangle((px, top, px + pw, fundo), 70, fill=INK)
    m = Image.new("L", janela.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, janela.width - 1, janela.height - 1), 52, fill=255)
    im.paste(janela, (px + 20, top + 20), m)

    def mapa(box):
        return (px + 20 + box[0] * k, top + 20 + (box[1] - y_tela) * k,
                px + 20 + box[2] * k, top + 20 + (box[3] - y_tela) * k)
    return im, mapa


def dedo(im, x, y, aperta):
    """Marca de toque: bolinha escura com borda branca, que encolhe ao apertar."""
    r = 46 * (1 - 0.25 * aperta)
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse((x - r - 8, y - r - 8, x + r + 8, y + r + 8), fill=(255, 255, 255, 230))
    od.ellipse((x - r, y - r, x + r, y + r), fill=(47, 69, 120, 200))
    return Image.alpha_composite(im.convert("RGBA"), ov).convert("RGB")


def legenda_clara(im, fm, text, t):
    legenda(im, text, fm.cap_y, fm.cap, t, fill=AZUL, bg=CLARO)


def anda(t, t0, t1, p0, p1):
    a = ease((t - t0) / (t1 - t0))
    return p0[0] + (p1[0] - p0[0]) * a, p0[1] + (p1[1] - p0[1]) * a


def g_botao(fm, t, cap="Responda com 1 toque"):
    y_tela = 1100 if fm.story else 1500
    im, mapa = celular(fm, VIAGEM, y_tela)
    legenda_clara(im, fm, cap, t)
    bx = mapa(BOTAO)
    cx, cy = (bx[0] + bx[2]) / 2 + 250 * (bx[2] - bx[0]) / 900, (bx[1] + bx[3]) / 2
    d = ImageDraw.Draw(im)
    if t > 1.3:
        anel(d, bx, t)
    x, y = anda(t, 0.3, 1.3, (W + 80, fm.h - 40), (cx, cy))
    ap = 1 if 1.6 <= t < 1.9 else 0
    if t >= 1.6:
        im = toque(im, cx, cy, ease((t - 1.6) / 0.5), cor=(47, 69, 120))
    if t > 0.3:
        im = dedo(im, x, y, ap)
    return im


def g_msg(fm, t, cap1="A resposta sai pronta", cap2="É só mandar no grupo"):
    y_tela = 900 if fm.story else 1180
    im, mapa = celular(fm, TEXTO, y_tela)
    seg = t > 1.4
    legenda_clara(im, fm, cap2 if seg else cap1, t - 1.4 if seg else t)
    msg = mapa((42, 1292, 1038, 1684))
    apps = mapa((10, 2060, 1070, 2280))
    d = ImageDraw.Draw(im)
    if not seg and t > 0.3:
        anel(d, msg, t, raio=30)
    if seg:
        mx, my = 920 / 1080 * (apps[2] - apps[0]) + apps[0], (apps[1] + apps[3]) / 2 - 20
        x, y = anda(t, 1.4, 2.2, (W + 80, fm.h - 40), (mx, my))
        if t >= 2.4:
            im = toque(im, mx, my, ease((t - 2.4) / 0.5), cor=(47, 69, 120))
        im = dedo(im, x, y, 1 if 2.4 <= t < 2.7 else 0)
    return im


def grupo(d, fm, top, msgs, t, alto=900, fonte=(40, 32)):
    """Conversa de grupo genérica. msgs: [(t_aparece, linhas, quem)], quem=None é você."""
    fnt = F(R, fonte[0] if fm.story else fonte[1])
    fn_nome = F(B, 30 if fm.story else 24)
    alto = alto if fm.story else fm.h - top - 40
    d.rounded_rectangle((60, top, W - 60, top + alto), 44, fill=(226, 231, 242))
    for i, (cx, cor) in enumerate(((112, (198, 138, 94)), (150, (120, 160, 210)), (188, (150, 110, 170)))):
        d.ellipse((cx - 34, top + 40, cx + 34, top + 108), fill=cor, outline=(226, 231, 242), width=5)
    d.text((240, top + 34), "Cinema hoje", font=F(B, 46), fill=INK)
    d.text((240, top + 88), "Lia, Davi e você", font=F(R, 28), fill=(90, 96, 110))
    d.line((60, top + 150, W - 60, top + 150), fill=(205, 212, 228), width=3)
    y = top + 180
    for ta, linhas, quem in msgs:
        if t < ta:
            continue
        w = max(d.textlength(l, font=fnt) for l in linhas) + 64
        if quem:
            w = max(w, d.textlength(quem, font=fn_nome) + 64)
            h = len(linhas) * (fnt.size + 10) + 44 + fn_nome.size + 6
            d.rounded_rectangle((70 + 20, y, 90 + w, y + h), 34, fill=WHITE)
            d.text((120, y + 16), quem, font=fn_nome, fill=CORES_AMIGOS[quem])
            yy = y + 22 + fn_nome.size + 6
            for ln in linhas:
                d.text((120, yy), ln, font=fnt, fill=INK)
                yy += fnt.size + 10
            y += h + 22
        else:
            y = bolha(d, 0, y, int(w), linhas, fnt, (217, 226, 255), True) + 22


def g_pergunta(fm, t):
    im = Image.new("RGB", (W, fm.h), CLARO)
    legenda_clara(im, fm, "Cinema no São Luiz com a galera?", t)
    d = ImageDraw.Draw(im)
    a = ease(t / 0.3)
    top = fm.cap_y + 2 * fm.cap + (90 if fm.story else 50)
    grupo(d, fm, top + (1 - a) * 80, SL_ANTES, t, alto=760)
    return im


def g_chat(fm, t):
    im = Image.new("RGB", (W, fm.h), CLARO)
    legenda_clara(im, fm, "Pronto. Galera avisada.", t)
    d = ImageDraw.Draw(im)
    top = fm.cap_y + 2 * fm.cap + (90 if fm.story else 50)
    grupo(d, fm, top, [(0, l, q) for _, l, q in SL_ANTES] +
          [(0.3, MINHA_SL, None), (1.1, ["Show! A gente te espera na fila"], "Davi")], t,
          alto=1130, fonte=(33, 27))
    return im


def g_chegada(fm, t):
    """A notificação real do modo viagem ao chegar (trip_arrived_belongings), descendo do topo."""
    im = Image.new("RGB", (W, fm.h), CLARO)
    legenda_clara(im, fm, "18:23: o app avisa que você chegou", t)
    d = ImageDraw.Draw(im)
    a = ease((t - 0.2) / 0.45)
    cw, ch = 940, 300
    x0 = (W - cw) // 2
    alvo = fm.h // 2 - ch // 2 + (40 if fm.story else 30)
    y0 = int(alvo - (1 - a) * 260)
    if t > 0.2:
        d.rounded_rectangle((x0 + 6, y0 + 10, x0 + cw + 6, y0 + ch + 10), 44, fill=(214, 219, 230))
        d.rounded_rectangle((x0, y0, x0 + cw, y0 + ch), 44, fill=WHITE)
        ic, mask = fr.rounded_icon(84)
        im.paste(ic, (x0 + 40, y0 + 40), mask)
        d.text((x0 + 150, y0 + 52), "Metrô Ceará · agora", font=F(R, 32), fill=(90, 96, 110))
        d.text((x0 + 40, y0 + 150), "Você chegou a José de Alencar.", font=F(B, 46), fill=INK)
        d.text((x0 + 40, y0 + 216), "Confira se está levando tudo.", font=F(R, 40), fill=INK)
    if t > 0.9:
        # Pulso discreto em volta, para marcar o aviso.
        anel(d, (x0, y0, x0 + cw, y0 + ch), t, raio=44)
    return im


def g_chat2(fm, t):
    im = Image.new("RGB", (W, fm.h), CLARO)
    legenda_clara(im, fm, "E a galera fica sabendo", t)
    d = ImageDraw.Draw(im)
    top = fm.cap_y + 2 * fm.cap + (90 if fm.story else 50)
    grupo(d, fm, top, [(0, MINHA_SL, None), (0, ["Show! A gente te espera na fila"], "Davi"),
                       (0.4, ["Cheguei na José de Alencar!", "Já tô indo pra praça"], None),
                       (1.2, ["Corre que a fila tá andando!"], "Lia")], t, alto=1000, fonte=(36, 28))
    return im


def g_fim(fm, t):
    im = Image.new("RGB", (W, fm.h), CLARO)
    d = ImageDraw.Draw(im)
    s = pop(t / 0.4)
    size = max(10, int((240 if fm.story else 200) * s))
    ic, mask = fr.rounded_icon(size)
    cy = fm.h // 2 - (330 if fm.story else 270)
    im.paste(ic, ((W - size) // 2, cy - size // 2 + 120), mask)
    y = texto_central(d, "Metrô Ceará", F(B, 96), cy + 260, fill=INK)
    y = texto_central(d, "Grátis no Google Play", F(B, 56), y + 10, fill=AZUL)
    y = texto_central(d, "sem anúncios · sem cadastro", F(R, 42), y + 4, fill=(90, 96, 110))
    nota = "Toque no link" if fm.story else "Link na bio"
    fb = F(B, 44)
    tw = d.textlength(nota, font=fb)
    d.rounded_rectangle(((W - tw) / 2 - 44, y + 40, (W + tw) / 2 + 44, y + 130), 45, fill=AZUL)
    d.text(((W - tw) / 2, y + 60), nota, font=fb, fill=WHITE)
    return im



CENAS = [(0, g_pergunta), (3.0, g_botao), (6.2, g_msg), (9.6, g_chat), (12.2, g_chegada),
         (14.4, g_chat2), (17.0, g_fim)]
DUR = 19.2


def quadro(fm, t):
    for i, (ini, fn) in enumerate(CENAS):
        fim = CENAS[i + 1][0] if i + 1 < len(CENAS) else DUR
        if ini <= t < fim:
            return fn(fm, t - ini)
    return CENAS[-1][1](fm, DUR - CENAS[-1][0])


def render(fm):
    dst = os.path.join(OUT, f"dia24-avisar-{fm.nome}.mp4")
    cmd = [FF, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{fm.h}",
           "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "slow", "-crf", "18",
           "-pix_fmt", "yuv420p", "-movflags", "+faststart", dst]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    for n in range(int(DUR * FPS)):
        p.stdin.write(quadro(fm, n / FPS).tobytes())
    p.stdin.close()
    p.wait()
    print(dst)


def main():
    formatos = [Formato("story", 1920, True), Formato("post", 1350, False)]
    if len(sys.argv) > 2 and sys.argv[1] == "quadro":
        t = float(sys.argv[2])
        for fm in formatos:
            path = os.path.join(os.environ.get("PREVIA", "/tmp"), f"previa-{fm.nome}-{t}.png")
            quadro(fm, t).save(path)
            print(path)
        return
    for fm in formatos:
        render(fm)


if __name__ == "__main__":
    main()
