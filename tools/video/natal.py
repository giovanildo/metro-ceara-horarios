"""Vídeos natalinos do Instagram, no formato de conversa de historias.py, com tema de Natal:
pisca-pisca no topo, estrelinhas douradas que piscam no fundo, títulos em verde (e vermelho nos
avisos) e um final com o ícone de gorro de Papai Noel.

Saem em divulgacao/instagram-natal/natalNN-<nome>-story.mp4 (1080x1920) e -post.mp4 (1080x1350).
Legendas e datas sugeridas em divulgacao/instagram-natal/textos.txt.

Fatos (notícias do Metrofor, conferidas em 21/11/2026): em 2024 e 2025, nos dias 24 e 31/12
metrô e VLT pararam por volta das 18h30 (2024: Linha Sul até 19h30) e não rodaram nos dias
25/12 e 1º/1; em 2025 não houve bike nos dias 24 e 31. O de 2026 ainda não saiu: os vídeos
dizem "costuma" e mostram, como exemplo, o aviso real do feriado de 20/11.

    python3 tools/video/natal.py            # todos
    python3 tools/video/natal.py --previa 3 # folha e vídeo leve em prints/natal/
"""
import math
import os
import random
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402
import historias as h  # noqa: E402
from historias import B, F, R, W, conversa, seguir, tela  # noqa: E402

OUT = os.path.join(fr.ROOT, "divulgacao", "instagram-natal")
PREVIA = os.path.join(fr.ROOT, "prints", "natal")
VERDE = (20, 100, 55)
VERMELHO = (190, 25, 40)
DOURADO = (214, 160, 30)
LAMPADAS = [(220, 40, 50), (30, 150, 80), (240, 185, 40), (60, 120, 220)]
h.CORES.update({"Vó": (176, 60, 120)})


# ------------------------------------------------------------------------- decoração
def _estrela(d, x, y, r, cor):
    """Estrelinha de 4 pontas."""
    k = r * 0.28
    d.polygon([(x, y - r), (x + k, y - k), (x + r, y), (x + k, y + k),
               (x, y + r), (x - k, y + k), (x - r, y), (x - k, y - k)], fill=cor)


def pisca_pisca(im, fm, t):
    """Fio que cai em arco entre as bordas, com lâmpadas que acendem e apagam em ondas."""
    d = ImageDraw.Draw(im, "RGBA")
    topo = 150 if fm.story else 6
    queda = 46 if fm.story else 30
    pontos = [(x, topo + queda * math.sin(math.pi * ((x % 360) / 360))) for x in range(-20, W + 21, 6)]
    d.line(pontos, fill=(60, 70, 60, 255), width=4)
    for i, x in enumerate(range(20, W, 60)):
        y = topo + queda * math.sin(math.pi * ((x % 360) / 360))
        cor = LAMPADAS[i % 4]
        brilho = 0.55 + 0.45 * math.sin(t * 5.0 + i * 1.3)
        r = 13 if fm.story else 10
        d.ellipse((x - r * 2.2, y + 4, x + r * 2.2, y + 4 + r * 4.4), fill=cor + (int(70 * brilho),))
        d.rectangle((x - 5, y - 2, x + 5, y + 8), fill=(80, 80, 80, 255))
        c = tuple(int(v * (0.6 + 0.4 * brilho) + 255 * 0.25 * brilho) for v in cor)
        c = tuple(min(255, v) for v in c)
        d.ellipse((x - r, y + 8, x + r, y + 8 + r * 2.2), fill=c + (255,))


_ESTRELAS = None


def estrelinhas(im, fm, t):
    """Estrelas douradas em lugares fixos, só onde o fundo está livre (não cobrem conteúdo)."""
    global _ESTRELAS
    rnd = random.Random(24)
    estrelas = [(rnd.randint(30, W - 30), rnd.randint(260, 1900), rnd.uniform(9, 20), rnd.uniform(0, 6.28))
                for _ in range(70)]
    px = im.load()
    d = ImageDraw.Draw(im)
    for x, y, r, fase in estrelas:
        if y > fm.h - 40:
            continue
        # Só no fundo liso: o ponto e um anel em volta têm de ser exatamente a cor do fundo.
        anel = [(x, y)] + [(int(x + rr * math.cos(a)), int(y + rr * math.sin(a)))
                           for rr in (r + 8, r + 24) for a in [k * math.pi / 8 for k in range(16)]]
        if any(not (0 <= ax < W and 0 <= ay < fm.h) or px[ax, ay] != h.CLARO for ax, ay in anel):
            continue
        b = 0.5 + 0.5 * math.sin(t * 3.2 + fase)
        cor = tuple(int(c * b + cc * (1 - b)) for c, cc in zip(DOURADO, h.CLARO))
        _estrela(d, x, y, r * (0.7 + 0.3 * b), cor)


def natalino(cena):
    fn, dur = cena

    def nova(fm, t):
        im = fn(fm, t)
        estrelinhas(im, fm, t)
        pisca_pisca(im, fm, t)
        return im
    return nova, dur


def gorro(size):
    """Gorro de Papai Noel em RGBA: cone vermelho que tomba para a direita, faixa e pompom brancos."""
    k = 4
    s = size * k
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    corpo = [(0.10, 0.74), (0.20, 0.42), (0.38, 0.20), (0.62, 0.12), (0.80, 0.18), (0.92, 0.34),
             (0.94, 0.52), (0.84, 0.40), (0.72, 0.34), (0.66, 0.46), (0.86, 0.74)]
    d.polygon([(x * s, y * s) for x, y in corpo], fill=(208, 32, 46))
    d.polygon([(x * s, y * s) for x, y in [(0.62, 0.12), (0.80, 0.18), (0.92, 0.34), (0.94, 0.52),
                                           (0.84, 0.40), (0.72, 0.30)]], fill=(168, 22, 36))
    d.rounded_rectangle((0.02 * s, 0.68 * s, 0.98 * s, 0.90 * s), radius=0.11 * s, fill=(252, 252, 252))
    d.ellipse((0.84 * s, 0.46 * s, 1.0 * s, 0.62 * s), fill=(252, 252, 252))
    im = im.resize((size, size), Image.LANCZOS)
    return im.rotate(16, resample=Image.BICUBIC, expand=False)


def fim_natal(frase="Boas festas e boas viagens!"):
    def cena(fm, t):
        im = Image.new("RGB", (W, fm.h), h.CLARO)
        d = ImageDraw.Draw(im)
        s = h.pop(t / 0.4)
        size = max(10, int((240 if fm.story else 200) * s))
        ic, mask = fr.rounded_icon(size)
        cy = fm.h // 2 - (330 if fm.story else 270)
        x0, y0 = (W - size) // 2, cy - size // 2 + 120
        im.paste(ic, (x0, y0), mask)
        if s > 0.6:
            g = gorro(int(size * 0.8))
            im.paste(g, (x0 - int(size * 0.22), y0 - int(size * 0.50)), g)
        y = h.texto_central(d, "Metrô Ceará", F(B, 96), cy + 260, fill=h.INK)
        y = h.texto_central(d, frase, F(B, 50), y + 8, fill=VERDE)
        y = h.texto_central(d, "Grátis no Google Play · sem anúncios", F(R, 40), y + 10, fill=h.CINZA)
        nota = "Toque no link" if fm.story else "Link na bio"
        fb = F(B, 44)
        tw = d.textlength(nota, font=fb)
        d.rounded_rectangle(((W - tw) / 2 - 44, y + 40, (W + tw) / 2 + 44, y + 130), 45, fill=VERMELHO)
        d.text(((W - tw) / 2, y + 60), nota, font=fb, fill=h.WHITE)
        return im
    return cena, 2.6


def cartao(fm, t):
    """Cartão de boas festas: Feliz Natal grande, árvore de estrelas e o agradecimento."""
    im = Image.new("RGB", (W, fm.h), h.CLARO)
    d = ImageDraw.Draw(im)
    cx = W // 2
    topo = 330 if fm.story else 140
    alto = 640 if fm.story else 470
    # Árvore feita de estrelas e luzes, que vão aparecendo de cima para baixo.
    rnd = random.Random(7)
    n = 64
    for i in range(n):
        f = i / n
        if t < 0.15 + f * 1.4:
            continue
        y = topo + 60 + f * alto
        meia = 30 + f * (300 if fm.story else 230)
        x = cx + rnd.uniform(-meia, meia)
        brilho = 0.55 + 0.45 * math.sin(t * 4 + i)
        if i % 3 == 0:
            cor = LAMPADAS[i % 4]
            r = 13
            d.ellipse((x - r, y - r, x + r, y + r), fill=tuple(min(255, int(c * (0.7 + 0.5 * brilho))) for c in cor))
        else:
            _estrela(d, x, y, 11 + 5 * brilho, VERDE)
    _estrela(d, cx, topo + 20, 46 * min(1, max(0, (t - 0.1) / 0.4)) + 1, DOURADO)
    d.rectangle((cx - 26, topo + 60 + alto + 10, cx + 26, topo + 60 + alto + 70), fill=(120, 80, 40))
    y = topo + alto + 150
    a = h.ease((t - 1.6) / 0.4)
    if a > 0:
        y = h.texto_central(d, "Feliz Natal!", F(B, int(120 * h.pop((t - 1.6) / 0.4)) or 1), y,
                            fill=VERMELHO, alpha=a)
        y = h.texto_central(d, "E um Ano-Novo de boas viagens", F(B, 52), y + 10, fill=VERDE, alpha=a)
        h.texto_central(d, "Obrigado por viajar com o Metrô Ceará", F(R, 40), y + 14, fill=h.CINZA, alpha=a)
    return im


# ------------------------------------------------------------------------- vídeos
N1 = [(0.3, ["Bora comprar os presentes", "no Centro sábado?"], "Lia"), (1.1, ["Encontro no Shopping Metrô!"], "Davi")]
N2 = [(0.3, ["O amigo secreto da firma", "começa às 18h30!"], "Ana"), (1.0, ["Já tá vindo?"], "Rafa")]
N3 = [(0.3, ["Ceia dia 24 às 20h", "na casa da vó!"], "Mãe"), (1.0, ["Ninguém atrasa, hein!"], "Vó")]
N4 = [(0.3, ["Depois da confraternização,", "não vai cochilar e passar", "da estação, viu?"], "Mãe")]
N5 = [(0.3, ["Que presente eu dou pro Davi?", "Ele pega metrô todo dia"], "Rafa")]
MSG2 = ["Vou pegar o metrô das 18:10", "na estação Parangaba.", "Desço na estação Benfica",
        "por volta das 18:20 e vou a pé", "até Shopping Benfica."]

NATAL = {
    1: ("compras-de-natal", [
        conversa("Presentes de Natal no Centro?", "Compras de Natal", N1, 3.0, cor=VERDE,
                 membros="Lia, Davi e você"),
        tela("O app diz o metrô certo", "natal-compras.png", 560, (110, 1335, 880, 1395), 3.2,
             texto2="E desce ao lado do shopping", t2=1.8, cor=VERDE),
        conversa("Compras sem estresse", "Compras de Natal", seguir(N1) + [
            (0.3, ["Pego o das 9:18 no Mondubim,", "chego 9:40!"], None), (1.1, ["Te espero na porta!"], "Lia")],
            2.8, cor=VERDE, membros="Lia, Davi e você"),
        fim_natal()]),
    2: ("amigo-secreto", [
        conversa("Amigo secreto da firma?", "Firma", N2, 2.8, cor=VERDE, membros="Ana, Rafa e você"),
        tela("Um toque em Avisar alguém", "natal-amigo.png", 1300, (84, 2060, 996, 2160), 3.0,
             aperta=1.6, cor=VERDE),
        tela("A mensagem sai pronta", "natal-amigo-texto.png", 1150, (42, 1292, 1038, 1684), 2.8, cor=VERDE),
        conversa("Galera avisada", "Firma", seguir(N2) + [
            (0.3, MSG2, None), (1.1, ["Seu presente tá guardado!"], "Ana")],
            3.0, cor=VERDE, membros="Ana, Rafa e você"),
        fim_natal()]),
    3: ("vespera", [
        conversa("Ceia na casa da vó?", "Família", N3, 2.8, cor=VERMELHO, membros="Mãe, Vó e você"),
        tela("Véspera costuma ter horário especial", "aviso-feriado.png", 150, (30, 330, 1050, 480), 3.4,
             texto2="O app avisa (aqui, o feriado de 20/11)", t2=1.9, cor=VERMELHO),
        conversa("Chegou antes da ceia", "Família", seguir(N3) + [
            (0.3, ["Na véspera o metrô costuma", "parar mais cedo. Vou à tarde!"], None),
            (1.1, ["Então vem ajudar na ceia!"], "Vó")], 3.0, cor=VERMELHO, membros="Mãe, Vó e você"),
        fim_natal()]),
    4: ("depois-da-festa", [
        conversa("Confraternização até tarde?", "Mãe", N4, 2.8, cor=VERDE),
        tela("O app avisa antes da sua estação", "tela-3.png", 0, (30, 130, 1050, 330), 3.4,
             texto2="Mesmo com a tela desligada", t2=1.9, cor=VERDE),
        conversa("Desceu certinho", "Mãe", seguir(N4) + [
            (0.3, ["Desci certinho", "na Vila Pery!"], None), (1.1, ["Esse app é um presente!"], "Mãe")],
            2.8, cor=VERDE),
        fim_natal()]),
    5: ("presente", [
        conversa("Presente que não custa nada", "Rafa", N5, 2.8, cor=VERDE),
        tela("Mais › Compartilhar o app", "natal-mais.png", 1300, (40, 2175, 1040, 2275), 3.0,
             aperta=1.6, cor=VERDE),
        conversa("Presente dado", "Rafa", seguir(N5) + [
            (0.3, ["Manda o Metrô Ceará!", "Avisa a hora de descer", "e é grátis."], None),
            (1.1, ["Presente dado!"], "Rafa")], 3.0, cor=VERDE),
        fim_natal("Um presente para quem pega metrô")]),
    6: ("feliz-natal", [
        (cartao, 4.6),
        fim_natal()]),
}
NATAL = {n: (nome, [natalino(c) for c in cenas]) for n, (nome, cenas) in NATAL.items()}


class Formato(h.Formato):
    def __init__(self, nome, hh, story):
        super().__init__(nome, hh, story)
        if not story:
            self.cap_y = 100


def main():
    args = sys.argv[1:]
    previa = "--previa" in args
    dias = [int(a) for a in args if a.isdigit()] or sorted(NATAL)
    formatos = [Formato("story", 1920, True), Formato("post", 1350, False)]
    os.makedirs(PREVIA if previa else OUT, exist_ok=True)
    for n in dias:
        nome, cenas = NATAL[n]
        for fm in formatos:
            base = f"natal{n:02d}-{nome}-{fm.nome}"
            if previa:
                h.folha(cenas, fm, os.path.join(PREVIA, base + ".png"))
                if "--folha" not in args:
                    h.video(cenas, fm, os.path.join(PREVIA, base + ".mp4"), largura=540, crf=26)
            else:
                h.video(cenas, fm, os.path.join(OUT, base + ".mp4"))


if __name__ == "__main__":
    main()
