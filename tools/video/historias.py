"""Vídeos curtos do calendário do Instagram, no formato do dia 24 (avisar_videos.py):
uma conversa do dia a dia, o celular com a tela real do app e uma bolinha no papel do dedo,
a resposta na conversa e o final com o link. Fundo claro, sem som (a música entra no
Instagram), 12 a 16 s. Cada dia sai em dois formatos:

- divulgacao/instagram-calendario/diaNN-<nome>-story.mp4  (1080x1920, legendas fora das faixas
  que o Instagram cobre, espaço para a figurinha de link no fim);
- divulgacao/instagram-calendario/diaNN-<nome>-post.mp4   (1080x1350, feed 4:5).

As conversas são genéricas (sem imitar nenhum app) e as telas vêm do app de verdade
(tools/video/capturas/, tools/play/imagens/). Cada dia é uma lista de cenas em DIAS.

    python3 tools/video/historias.py 1 2 3        # vídeos desses dias
    python3 tools/video/historias.py --previa 1   # folha de quadros e vídeo leve em prints/historias/
"""
import math
import os
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(fr.ROOT, "divulgacao", "instagram-calendario")
PREVIA = os.path.join(fr.ROOT, "prints", "historias")
FF = os.environ.get("FFMPEG", "ffmpeg")
FPS = 30
W = 1080
AZUL = (47, 69, 120)
VERMELHO = (200, 20, 30)
AMARELO = (255, 204, 0)
WHITE = (255, 255, 255)
INK = (28, 28, 30)
CINZA = (90, 96, 110)
CLARO = (246, 248, 252)
FUNDO_CHAT = (226, 231, 242)
MEU = (217, 226, 255)
F = fr.font
B, R = fr.BOLD, fr.REG
CORES = {"Lia": (176, 60, 120), "Davi": (30, 120, 90), "Rafa": (40, 100, 170), "Ju": (150, 90, 20),
         "Mãe": (176, 60, 120), "Ana": (30, 120, 90), "Bia": (130, 70, 170), "Pedro": (40, 100, 170)}
AVATARES = [(198, 138, 94), (120, 160, 210), (150, 110, 170)]
_cache = {}


def cap(nome):
    """Uma captura (1080 de largura), de capturas/, capturas/instagram/ ou tools/play/imagens/."""
    if nome not in _cache:
        for pasta in (os.path.join(HERE, "capturas", "instagram"), os.path.join(HERE, "capturas"),
                      os.path.join(fr.ROOT, "tools", "play", "imagens")):
            p = os.path.join(pasta, nome)
            if os.path.exists(p):
                _cache[nome] = Image.open(p).convert("RGB")
                break
        else:
            raise FileNotFoundError(nome)
    return _cache[nome]


def ease(x):
    x = max(0.0, min(1.0, x))
    return 1 - (1 - x) ** 3


def pop(x):
    x = max(0.0, min(1.0, x))
    c = 1.7
    return 1 + (c + 1) * (x - 1) ** 3 + c * (x - 1) ** 2


class Formato:
    def __init__(self, nome, h, story):
        self.nome, self.h, self.story = nome, h, story
        # Nos stories, o Instagram cobre ~250 px em cima (perfil) e ~280 embaixo (responder).
        self.cap_y, self.cap = (270, 84) if story else (60, 70)

    def topo(self):
        """Onde começa o conteúdo, abaixo de uma legenda de até duas linhas."""
        return self.cap_y + 2 * self.cap + (90 if self.story else 50)


def texto_central(d, text, fnt, y, fill, width=W - 120, gap=8, alpha=1.0, bg=CLARO):
    if alpha < 1:
        fill = tuple(int(b + (f - b) * alpha) for f, b in zip(fill, bg))
    for line in fr.wrap(d, text, fnt, width):
        d.text(((W - d.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
        y += fnt.size + gap
    return y


def legenda(im, fm, text, t, cor=AZUL):
    a = ease(t / 0.25)
    texto_central(ImageDraw.Draw(im), text, F(B, fm.cap), fm.cap_y + (1 - a) * 30, fill=cor, alpha=a)


def anel(d, box, t, raio=40):
    """Contorno amarelo que pulsa em volta de uma área."""
    k = 6 + 6 * (0.5 + 0.5 * math.sin(t * 2 * math.pi * 1.6))
    x1, y1, x2, y2 = box
    r = min(raio + k, (y2 - y1) / 2 + k - 1)
    d.rounded_rectangle((x1 - k, y1 - k, x2 + k, y2 + k), r, outline=AMARELO, width=9)


def toque(im, cx, cy, p, cor=AZUL):
    r = 30 + 120 * p
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 255, 255, int(170 * (1 - p))))
    od.ellipse((cx - 34, cy - 34, cx + 34, cy + 34), fill=cor + (int(230 * (1 - p * 0.6)),))
    return Image.alpha_composite(im.convert("RGBA"), ov).convert("RGB")


def dedo(im, x, y, aperta):
    r = 46 * (1 - 0.25 * aperta)
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse((x - r - 8, y - r - 8, x + r + 8, y + r + 8), fill=(255, 255, 255, 230))
    od.ellipse((x - r, y - r, x + r, y + r), fill=AZUL + (200,))
    return Image.alpha_composite(im.convert("RGBA"), ov).convert("RGB")


def anda(t, t0, t1, p0, p1):
    a = ease((t - t0) / (t1 - t0))
    return p0[0] + (p1[0] - p0[0]) * a, p0[1] + (p1[1] - p0[1]) * a


def celular(fm, src, y_tela):
    """Celular escuro com a captura a partir de y_tela. Devolve (quadro, mapa da captura → quadro)."""
    im = Image.new("RGB", (W, fm.h), CLARO)
    pw = 960 if fm.story else 720
    top = fm.cap_y + 2 * fm.cap + (70 if fm.story else 40)
    k = (pw - 40) / 1080
    jan_h = (fm.h - top - 20) / k
    janela = src.crop((0, int(y_tela), 1080, int(min(src.height, y_tela + jan_h))))
    janela = janela.resize((pw - 40, int(janela.height * k)), Image.LANCZOS)
    px = (W - pw) // 2
    d = ImageDraw.Draw(im)
    fundo = top + janela.height + 40
    if fundo > fm.h - 10:
        fundo = fm.h + 200
    d.rounded_rectangle((px + 8, top + 14, px + pw + 8, fundo + 14), 70, fill=(210, 214, 224))
    d.rounded_rectangle((px, top, px + pw, fundo), 70, fill=INK)
    m = Image.new("L", janela.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, janela.width - 1, janela.height - 1), 52, fill=255)
    im.paste(janela, (px + 20, top + 20), m)

    def mapa(box):
        return (px + 20 + box[0] * k, top + 20 + (box[1] - y_tela) * k,
                px + 20 + box[2] * k, top + 20 + (box[3] - y_tela) * k)
    return im, mapa


def balao(d, x1, y, linhas, fnt, quem, fn_nome, nome=True):
    """Balão de `quem` (None = você, à direita). nome=False esconde o nome (conversa a dois)."""
    nome = bool(quem) and nome
    w = max(d.textlength(l, font=fnt) for l in linhas) + 64
    if nome:
        w = max(w, d.textlength(quem, font=fn_nome) + 64)
    h = len(linhas) * (fnt.size + 10) + 44 + (fn_nome.size + 6 if nome else 0)
    if not quem:
        x1 = W - 70 - w
    d.rounded_rectangle((x1, y, x1 + w, y + h), 34, fill=WHITE if quem else MEU)
    yy = y + 22
    if nome:
        d.text((x1 + 30, y + 16), quem, font=fn_nome, fill=CORES.get(quem, CINZA))
        yy += fn_nome.size + 6
    for ln in linhas:
        d.text((x1 + 30, yy), ln, font=fnt, fill=INK)
        yy += fnt.size + 10
    return y + h


# ------------------------------------------------------------------------------------- cenas
def conversa(texto, titulo, msgs, dur=3.0, cor=AZUL, membros=None):
    """msgs: [(t_aparece, [linhas], quem)]; quem=None é você. Com um só amigo, é conversa a dois."""
    amigos = list(dict.fromkeys(q for _, _, q in msgs if q))

    def cena(fm, t):
        im = Image.new("RGB", (W, fm.h), CLARO)
        legenda(im, fm, texto, t, cor)
        d = ImageDraw.Draw(im)
        a = ease(t / 0.3)
        top = fm.topo() + (1 - a) * 60
        linhas = sum(len(l) for _, l, _ in msgs)
        grande = linhas <= 7
        fnt = F(R, (40 if grande else 34) if fm.story else (32 if grande else 28))
        fn_nome = F(B, 30 if fm.story else 24)
        alto = min(fm.h - fm.topo() - (330 if fm.story else 40),
                   190 + sum(len(l) * (fnt.size + 10) + 44 + fn_nome.size + 28 for _, l, _ in msgs))
        alto = max(alto, 600 if fm.story else 520)
        d.rounded_rectangle((60, top, W - 60, top + alto), 44, fill=FUNDO_CHAT)
        n = 3 if membros else (1 if len(amigos) <= 1 else min(3, len(amigos) + 1))
        for i in range(n):
            cx = 112 + 38 * i
            d.ellipse((cx - 34, top + 40, cx + 34, top + 108), fill=AVATARES[i], outline=FUNDO_CHAT, width=5)
        x_t = 112 + 38 * (n - 1) + 56
        if n == 1:
            d.text((x_t, top + 50), titulo, font=F(B, 46), fill=INK)
        else:
            d.text((x_t, top + 34), titulo, font=F(B, 46), fill=INK)
            sub = membros or (", ".join(amigos) + " e você")
            d.text((x_t, top + 88), sub, font=F(R, 28), fill=CINZA)
        d.line((60, top + 150, W - 60, top + 150), fill=(205, 212, 228), width=3)
        y = top + 180
        for ta, lin, quem in msgs:
            if t >= ta:
                y = balao(d, 90, y, lin, fnt, quem, fn_nome, nome=n > 1) + 22
        return im
    return cena, dur


def tela(texto, nome, y_tela, alvo, dur=3.4, aperta=None, texto2=None, t2=None, cor=AZUL):
    """A captura num celular; o dedo vai até o alvo (e aperta, se `aperta` tiver o segundo)."""
    def cena(fm, t):
        seg = texto2 is not None and t >= t2
        im, mapa = celular(fm, cap(nome), y_tela)
        legenda(im, fm, texto2 if seg else texto, t - t2 if seg else t, cor)
        bx = mapa(alvo)
        d = ImageDraw.Draw(im)
        if t > 1.2:
            anel(d, bx, t)
        if aperta is None:
            # Só apontando: logo abaixo do anel, à direita, sem cobrir o texto.
            cx, cy = bx[2] - 70, bx[3] + 62
        else:
            cx, cy = bx[0] + (bx[2] - bx[0]) * 0.78, (bx[1] + bx[3]) / 2
        x, y = anda(t, 0.3, 1.2, (W + 80, fm.h - 40), (cx, cy))
        ap = 0
        if aperta is not None and t >= aperta:
            im = toque(im, cx, cy, ease((t - aperta) / 0.5))
            ap = 1 if t < aperta + 0.3 else 0
        if t > 0.3:
            im = dedo(im, x, y, ap)
        return im
    return cena, dur


def fim(frase="Horários de metrô e VLT no celular", loja="Grátis no Google Play · sem anúncios"):
    def cena(fm, t):
        im = Image.new("RGB", (W, fm.h), CLARO)
        d = ImageDraw.Draw(im)
        s = pop(t / 0.4)
        size = max(10, int((240 if fm.story else 200) * s))
        ic, mask = fr.rounded_icon(size)
        cy = fm.h // 2 - (330 if fm.story else 270)
        im.paste(ic, ((W - size) // 2, cy - size // 2 + 120), mask)
        y = texto_central(d, "Metrô Ceará", F(B, 96), cy + 260, fill=INK)
        y = texto_central(d, frase, F(R, 44), y + 6, fill=CINZA)
        y = texto_central(d, loja, F(B, 44), y + 10, fill=AZUL)
        nota = "Toque no link" if fm.story else "Link na bio"
        fb = F(B, 44)
        tw = d.textlength(nota, font=fb)
        d.rounded_rectangle(((W - tw) / 2 - 44, y + 40, (W + tw) / 2 + 44, y + 130), 45, fill=AZUL)
        d.text(((W - tw) / 2, y + 60), nota, font=fb, fill=WHITE)
        return im
    return cena, 2.4


def seguir(msgs):
    """As mensagens de uma conversa já aparecidas desde o começo da cena seguinte."""
    return [(0, lin, quem) for _, lin, quem in msgs]


# ------------------------------------------------------------------------------------- dias
D1 = [(0.3, ["Bora junto pro trabalho?"], "Rafa"), (1.1, ["Que horas passa o metrô?"], "Rafa")]
D2 = [(0.3, ["Tô saindo de casa agora."], "Ju"), (1.0, ["O metrô já passou?"], "Ju")]
D3 = [(0.3, ["Bora no Iate sábado?"], "Lia"), (1.0, ["Como chega de metrô", "saindo do Mondubim?"], "Davi")]
D4 = [(0.3, ["Não vai dormir e passar", "da estação de novo, hein!"], "Mãe")]
D5 = [(0.3, ["Amanhã tem reunião às 8h30,", "não atrasa!"], "Ana")]
D6 = [(0.3, ["Bora pra praia", "de metrô no domingo?"], "Lia")]
D7 = [(0.3, ["Chego sexta em Fortaleza!"], "Bia"), (1.0, ["Quanto é o metrô aí?", "Tem meia?"], "Bia")]
D8 = [(0.3, ["Posso levar a bike", "no metrô amanhã?"], "Davi")]

D9 = [(0.3, ["Minha consulta é no", "Hospital Walter Cantídio."], "Mãe"), (1.1, ["Qual estação desce?"], "Mãe")]
D10 = [(0.3, ["Qual é o vagão feminino", "da Linha Sul?"], "Ana")]
D11 = [(0.3, ["Já tá voltando pra casa?"], "Mãe")]
D12 = [(0.3, ["Metrô lotado e", "sem nada pra fazer..."], "Ju")]
D13 = [(0.3, ["Tio, por que a estação", "se chama Parangaba?"], "Pedro")]
D14 = [(0.3, ["Amanhã é feriado.", "Tem metrô?"], "Ju")]
D15 = [(0.3, ["Tenho iPhone.", "Dá pra usar esse app?"], "Bia")]

D17 = [(0.3, ["Onde vejo o mapa", "das linhas de metrô?"], "Davi")]
D18 = [(0.3, ["Vou na Parangaba", "pela primeira vez."], "Lia"), (1.0, ["Como é lá?"], "Lia")]
D19 = [(0.3, ["Chego de avião sábado!"], "Bia"), (1.0, ["Dá pra ir do aeroporto", "pra praia de metrô?"], "Bia")]
D20 = [(0.3, ["Tio, tenho trabalho", "da escola sobre Iracema"], "Pedro")]
D21 = [(0.3, ["E a Linha Leste,", "sai quando?"], "Davi")]
D22 = [(0.3, ["Eu tenho iPhone"], "Mãe"), (0.9, ["Eu pego metrô todo dia"], "Davi"),
       (1.5, ["Eu só quero conhecer", "as estações"], "Bia"), (2.1, ["Qual Metrô Ceará é pra mim?"], "Mãe")]

DIAS = {
    1: ("proximo-metro", [
        conversa("Que horas passa o metrô?", "Rafa", D1),
        tela("Abre o app e pronto", "inicio.png", 560, (160, 1100, 1010, 1250),
             texto2="A estação mais perto e quanto falta", t2=1.9),
        conversa("Sem adivinhar horário", "Rafa", seguir(D1) + [
            (0.3, ["Sai em 7 min!", "Te encontro na plataforma"], None), (1.1, ["Fechado!"], "Rafa")], 2.8),
        fim()]),
    2: ("widget", [
        conversa("O metrô já passou?", "Ju", D2),
        tela("Nem precisa abrir o app", "tela-4.png", 430, (55, 530, 1025, 1130),
             texto2="O widget fica na tela inicial", t2=1.9),
        conversa("Olhou, respondeu", "Ju", seguir(D2) + [
            (0.3, ["Próximo às 07:32.", "Corre que dá!"], None), (1.1, ["Valeu!"], "Ju")], 2.8),
        fim()]),
    3: ("para-onde-voce-vai", [
        conversa("Bora pro Iate sábado?", "Praia sábado", D3, 3.0, membros="Lia, Davi e você"),
        tela("Diga para onde vai", "iate-sabado.png", 560, (70, 940, 890, 1005),
             texto2="O app monta a viagem", t2=1.9),
        conversa("Roteiro na mão", "Praia sábado", seguir(D3) + [
            (0.3, ["Metrô até a Parangaba", "e VLT até o Iate.", "A integração é grátis!"], None),
            (1.1, ["Partiu!"], "Lia")], 2.8),
        fim()]),
    4: ("hora-de-descer", [
        conversa("Já cochilou e passou da estação?", "Mãe", D4, 2.8),
        tela("O app avisa antes da sua estação", "tela-3.png", 0, (30, 130, 1050, 330), 3.6,
             texto2="Mesmo com a tela desligada", t2=1.9),
        conversa("Desceu certinho", "Mãe", seguir(D4) + [
            (0.3, ["Desci certinho", "na Vila Pery!"], None), (1.1, ["Milagre!"], "Mãe")], 2.8),
        fim()]),
    5: ("hora-de-sair", [
        conversa("Compromisso cedo?", "Ana", D5, 2.8),
        tela("Toque no horário do metrô", "hora-de-sair-lista.png", 1250, (40, 1655, 1040, 1795), 3.4,
             aperta=1.6),
        tela("15 min antes, ele avisa", "hora-de-sair-aviso.png", 500, (40, 580, 1040, 1035), 3.0),
        conversa("Na hora certa", "Ana", seguir(D5) + [
            (0.3, ["Saindo agora,", "chego no horário!"], None), (1.1, ["Boa!"], "Ana")], 2.8),
        fim()]),
    6: ("domingo", [
        conversa("Metrô no domingo?", "Praia", D6, 2.8, cor=VERMELHO, membros="Lia, Davi e você"),
        tela("Domingo comum não tem metrô", "domingo-1.100.png", 330, (85, 675, 1000, 825), 3.4,
             texto2="E o app avisa a volta", t2=1.9, cor=VERMELHO),
        conversa("Melhor saber antes", "Praia", seguir(D6) + [
            (0.3, ["Domingo não tem metrô.", "Bora no sábado?"], None), (1.1, ["Fechou, sábado!"], "Davi")],
            2.8, cor=VERMELHO, membros="Lia, Davi e você"),
        fim()]),
    7: ("passagem", [
        conversa("Quanto é a passagem?", "Bia", D7),
        tela("Tá tudo em Passagem", "passagem.png", 700, (40, 810, 1040, 990), 3.2,
             texto2="O preço de cada linha", t2=1.8),
        conversa("Respondido em segundos", "Bia", seguir(D7) + [
            (0.3, ["Linha Sul: R$ 3,60,", "meia R$ 1,80.", "Dinheiro ou Pix!"], None), (1.1, ["Anotado!"], "Bia")],
            2.8),
        fim()]),
    8: ("o-que-pode-levar", [
        conversa("Pode levar bike?", "Davi", D8, 2.6),
        tela("O app diz o horário da bike", "estacao-bike.png", 500, (30, 805, 1010, 935), 3.0),
        tela("E o que não pode levar", "o-que-pode-levar.png", 900, (30, 1545, 620, 1615), 2.8),
        conversa("Sem susto na estação", "Davi", seguir(D8) + [
            (0.3, ["Bike comum pode,", "a partir das 9h.", "Elétrica não pode!"], None), (1.1, ["Valeu!"], "Davi")],
            2.8),
        fim()]),
    9: ("buscar-lugares", [
        conversa("Não sabe qual é a estação?", "Mãe", D9),
        tela("Busque o lugar, não a estação", "busca-walter.png", 150, (40, 790, 1040, 1010), 3.2,
             texto2="O app diz onde descer", t2=1.8),
        conversa("Sem se perder", "Mãe", seguir(D9) + [
            (0.3, ["Desce na Porangabussu,", "10 min a pé."], None), (1.1, ["Obrigada, filho!"], "Mãe")], 2.8),
        fim()]),
    10: ("vagao-feminino", [
        conversa("Vagão feminino, qual é?", "Ana", D10, 2.6),
        tela("O app avisa na tela da estação", "estacao.png", 700, (30, 1095, 1050, 1220), 3.2),
        conversa("Viagem mais tranquila", "Ana", seguir(D10) + [
            (0.3, ["O primeiro e o último,", "o dia todo!"], None), (1.1, ["Valeu, amiga!"], "Ana")], 2.8),
        fim()]),
    11: ("casa-trabalho", [
        conversa("Faz o mesmo caminho todo dia?", "Mãe", D11, 2.6),
        tela("Um toque em Casa", "casa-tarde.png", 250, (42, 435, 575, 580), 2.8, aperta=1.5),
        tela("E o metrô certo pra voltar", "casa-viagem.png", 1100, (110, 1640, 1000, 1890), 3.0),
        conversa("Mãe tranquila", "Mãe", seguir(D11) + [
            (0.3, ["Pego o das 17:40,", "chego 17:58 no Mondubim!"], None), (1.1, ["Janta tá pronta!"], "Mãe")],
            2.8),
        fim()]),
    12: ("passatempos", [
        conversa("Metrô lotado, que tédio?", "Ju", D12, 2.6),
        tela("Um passatempo por dia", "caca-abertura.png", 100, (147, 925, 935, 1057), 2.8, aperta=1.5),
        tela("Com a história de uma estação", "caca-quadro.png", 250, (450, 1488, 1030, 1566), 3.0),
        conversa("A viagem passou voando", "Ju", seguir(D12) + [
            (0.3, ["Achei BAIRRO!", "E o Benfica era uma chácara"], None), (1.1, ["Vou jogar também!"], "Ju")],
            2.8),
        fim()]),
    13: ("nomes", [
        conversa("Por que esse nome?", "Pedro", D13, 2.6),
        tela("Toda estação tem a sua história", "site-nome-parangaba.png", 250, (40, 336, 1040, 865), 3.2,
             texto2="No site do Metrô Ceará", t2=1.8),
        conversa("Resposta de tio sabido", "Pedro", seguir(D13) + [
            (0.3, ["Em tupi, quer dizer beleza:", "a lagoa onde Iracema", "se banhava!"], None),
            (1.1, ["Que massa!"], "Pedro")], 2.8),
        fim("Horários e histórias das estações")]),
    14: ("feriado", [
        conversa("Tem metrô no feriado?", "Ju", D14, 2.6, cor=VERMELHO),
        tela("O app avisa o horário especial", "aviso-feriado.png", 150, (30, 330, 1050, 480), 3.2,
             texto2="Logo no topo, com a data", t2=1.8, cor=VERMELHO),
        conversa("Sem perder viagem", "Ju", seguir(D14) + [
            (0.3, ["Tem, mas com horário", "especial: até 18:10."], None), (1.1, ["Vou cedo então!"], "Ju")],
            2.8, cor=VERMELHO),
        fim()]),
    15: ("iphone", [
        conversa("Tem iPhone?", "Bia", D15, 2.6),
        tela("Abre no navegador, sem instalar", "lite-chrome.png", 0, (42, 948, 1040, 1746), 3.2,
             texto2="É a versão lite", t2=1.8),
        conversa("Ninguém fica de fora", "Bia", seguir(D15) + [
            (0.3, ["Dá! Abre no navegador", "e põe na tela inicial."], None), (1.1, ["Já pus!"], "Bia")], 2.8),
        fim("Também no iPhone, pelo navegador", "Versão lite · grátis, sem instalar")]),
    17: ("o-site", [
        conversa("O Metrô Ceará também tem site", "Davi", D17, 2.6),
        tela("Mapa, horários e as 62 estações", "site-home.png", 1250, (25, 1870, 1060, 2330), 3.2,
             texto2="Abre em qualquer celular", t2=1.8),
        conversa("Tudo num lugar só", "Davi", seguir(D17) + [
            (0.3, ["No site do Metrô Ceará!", "Tem mapa e horário agora."], None), (1.1, ["Achei, valeu!"], "Davi")],
            2.8),
        fim("Site, lite e app para Android", "Grátis · sem anúncios · sem cadastro")]),
    18: ("site-estacao", [
        conversa("Cada estação tem a sua página", "Lia", D18, 2.8),
        tela("Fotos, horários e história", "site-parangaba.png", 250, (20, 640, 1060, 1150), 3.2,
             texto2="Até a Rachel de Queiroz fala dela", t2=1.8),
        conversa("Chegou sabendo de tudo", "Lia", seguir(D18) + [
            (0.3, ["Olha a página dela no site:", "horário, fotos, o que tem perto"], None),
            (1.1, ["Que lindo!"], "Lia")], 2.8),
        fim("Horários e histórias das estações", "Grátis · sem anúncios · sem cadastro")]),
    19: ("site-turismo", [
        conversa("Visita chegando?", "Bia", D19, 2.8),
        tela("O guia Fortaleza de metrô", "site-turismo.png", 250, (20, 800, 1060, 1100), 3.2,
             texto2="Roteiros e onde os trilhos chegam", t2=1.8),
        conversa("Passeio planejado", "Bia", seguir(D19) + [
            (0.3, ["Dá! Olha o guia", "Fortaleza de metrô no site"], None), (1.1, ["Já salvei!"], "Bia")], 2.8),
        fim("Guia para quem visita Fortaleza", "Grátis · sem anúncios · sem cadastro")]),
    20: ("site-literatura", [
        conversa("Trabalho da escola?", "Pedro", D20, 2.6),
        tela("O trem na literatura", "site-lit2.png", 250, (60, 450, 1040, 1350), 3.2,
             texto2="Cada trecho conferido no original", t2=1.8),
        conversa("Nota dez", "Pedro", seguir(D20) + [
            (0.3, ["Nas notas de Iracema,", "Porangaba quer dizer beleza!"], None), (1.1, ["Valeu, tio!"], "Pedro")],
            2.8),
        fim("O trem nos livros e nas artes", "Grátis · sem anúncios · sem cadastro")]),
    21: ("site-linha-leste", [
        conversa("E a Linha Leste?", "Davi", D21, 2.6),
        tela("A obra, com a fonte de cada número", "site-leste.png", 250, (20, 1020, 1060, 1720), 3.2),
        conversa("Sem boato", "Davi", seguir(D21) + [
            (0.3, ["41% pronta (out/2026),", "previsão para o fim de 2028."], None),
            (1.1, ["Bom saber!"], "Davi")], 2.8),
        fim("Centro – Papicu, por baixo da Aldeota", "Grátis · sem anúncios · sem cadastro")]),
    22: ("plataformas", [
        conversa("Qual Metrô Ceará é pra você?", "Família", D22, 3.2, membros="Mãe, Davi, Bia e você"),
        tela("Pega metrô todo dia? O app", "inicio.png", 300, (40, 610, 1040, 1430), 2.6),
        tela("Tem iPhone? A versão lite", "lite-chrome.png", 0, (100, 140, 700, 260), 2.6),
        tela("Quer conhecer? O site", "site-home.png", 0, (20, 575, 1060, 870), 2.6),
        conversa("Cada um no seu", "Família", seguir(D22) + [
            (0.3, ["Davi: o app. Mãe: a lite.", "Bia: o site. Tudo grátis!"], None)],
            2.8, membros="Mãe, Davi, Bia e você"),
        fim("App, lite e site", "Grátis · sem anúncios · sem cadastro")]),
}


# ------------------------------------------------------------------------------------- saída
def linha_do_tempo(cenas):
    t, out = 0.0, []
    for fn, dur in cenas:
        out.append((t, t + dur, fn))
        t += dur
    return out, t


def quadro(cenas, fm, t):
    tl, dur = linha_do_tempo(cenas)
    for ini, fimt, fn in tl:
        if ini <= t < fimt:
            return fn(fm, t - ini)
    ini, fimt, fn = tl[-1]
    return fn(fm, fimt - ini)


def video(cenas, fm, dst, largura=None, crf=18):
    _, dur = linha_do_tempo(cenas)
    cmd = [FF, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{fm.h}",
           "-r", str(FPS), "-i", "-"]
    if largura:
        cmd += ["-vf", f"scale={largura}:-2"]
    cmd += ["-c:v", "libx264", "-preset", "slow", "-crf", str(crf), "-pix_fmt", "yuv420p", "-movflags", "+faststart", dst]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    for n in range(int(dur * FPS)):
        p.stdin.write(quadro(cenas, fm, n / FPS).tobytes())
    p.stdin.close()
    p.wait()
    print(dst)


def folha(cenas, fm, dst):
    tl, _ = linha_do_tempo(cenas)
    ts = [round(a + (b - a) * 0.8, 2) for a, b, _ in tl]
    lw = 300
    lh = int(fm.h * lw / W)
    s = Image.new("RGB", (len(ts) * (lw + 16) + 16, lh + 70), WHITE)
    d = ImageDraw.Draw(s)
    for i, t in enumerate(ts):
        s.paste(quadro(cenas, fm, t).resize((lw, lh), Image.LANCZOS), (16 + i * (lw + 16), 16))
        d.text((16 + i * (lw + 16), lh + 26), f"{t:.1f} s", font=F(B, 26), fill=INK)
    s.save(dst)
    print(dst)


def main():
    args = sys.argv[1:]
    previa = "--previa" in args
    dias = [int(a) for a in args if a.isdigit()] or sorted(DIAS)
    formatos = [Formato("story", 1920, True), Formato("post", 1350, False)]
    os.makedirs(PREVIA if previa else OUT, exist_ok=True)
    for n in dias:
        nome, cenas = DIAS[n]
        for fm in formatos:
            base = f"dia{n:02d}-{nome}-{fm.nome}"
            if previa:
                folha(cenas, fm, os.path.join(PREVIA, base + ".png"))
                video(cenas, fm, os.path.join(PREVIA, base + ".mp4"), largura=540, crf=26)
            else:
                video(cenas, fm, os.path.join(OUT, base + ".mp4"))


if __name__ == "__main__":
    main()
