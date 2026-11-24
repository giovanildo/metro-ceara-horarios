"""Reel "vitrine" para páginas de bairro, no padrão do vídeo de referência
(referencias/metro ceará.mp4, 16/11/2026): fundo escuro, celular com moldura e sombra,
título branco com a palavra-chave em vermelho, zoom no detalhe com contorno vermelho e,
no fim, o cartão vermelho da marca (ícone, Grátis · Sem anúncio · Sem cadastro, botão).

Mais acabado que o formato conversa de historias.py, para quem pede um post "profissional".
A pergunta do bairro está no primeiro quadro, a resposta (tela real na estação) antes dos 3 s.

    python3 tools/video/reel_vitrine.py carlito            # story e post em divulgacao/paginas-do-bairro/
    python3 tools/video/reel_vitrine.py --previa carlito   # folha de quadros em prints/historias/

Fonte: Poppins (OFL, tools/video/fontes/), a mesma da referência.
Telas: tools/video/capturas/instagram/ (1080x2400, do emulador).
"""
import math
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
CAPS = os.path.join(HERE, "capturas", "instagram")
FONTES = os.path.join(HERE, "fontes")
ICON = os.path.join(ROOT, "docs", "img", "icon.png")
OUT = os.path.join(ROOT, "divulgacao", "paginas-do-bairro")
PREVIA = os.path.join(ROOT, "prints", "historias")
FF = os.environ.get("FFMPEG", "ffmpeg")
FPS = 30
W = 1080

BOLD = os.path.join(FONTES, "Poppins-Bold.ttf")
SEMI = os.path.join(FONTES, "Poppins-SemiBold.ttf")
MED = os.path.join(FONTES, "Poppins-Medium.ttf")
WHITE = (255, 255, 255)
CORAL = (255, 77, 77)          # vermelho legível sobre o fundo escuro
VERMELHO = (234, 27, 36)       # vermelho da marca (cartão final)
SUB = (238, 228, 214)          # subtítulo, creme
FAIXAS = [(240, 228, 208), (140, 198, 63), (91, 75, 159)]  # faixas do ícone
_fontes, _caps = {}, {}


def F(path, size):
    k = (path, size)
    if k not in _fontes:
        _fontes[k] = ImageFont.truetype(path, size)
    return _fontes[k]


def ease(x):
    x = max(0.0, min(1.0, x))
    return 1 - (1 - x) ** 3


def pop(x):
    x = max(0.0, min(1.0, x))
    c = 1.6
    return 1 + (c + 1) * (x - 1) ** 3 + c * (x - 1) ** 2


def cap(nome):
    if nome not in _caps:
        _caps[nome] = Image.open(os.path.join(CAPS, nome)).convert("RGB")
    return _caps[nome]


def mascara(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), r, fill=255)
    return m


def sombra(size, r, blur, alpha=170, pad=None):
    """Sombra (RGBA) de um retângulo arredondado, com margem para o desfoque."""
    pad = pad or blur * 3
    im = Image.new("RGBA", (size[0] + 2 * pad, size[1] + 2 * pad), (0, 0, 0, 0))
    ImageDraw.Draw(im).rounded_rectangle((pad, pad, pad + size[0], pad + size[1]), r, fill=(0, 0, 0, alpha))
    return im.filter(ImageFilter.GaussianBlur(blur)), pad


def com_alpha(im, a):
    if a >= 1:
        return im
    im = im.copy()
    im.putalpha(im.getchannel("A").point(lambda v: int(v * a)))
    return im


# ----------------------------------------------------------------------------- formatos
class Formato:
    def __init__(self, nome, h, story):
        self.nome, self.h, self.story = nome, h, story
        if story:   # o Instagram cobre ~250 px em cima e ~280 embaixo
            self.tit_y, self.tit, self.sub = 290, 70, 36
            self.tela_w, self.cel_y = 500, 590
        else:
            self.tit_y, self.tit, self.sub = 64, 58, 31
            self.tela_w, self.cel_y = 390, 300
        self.fundo = fundo(h)


def fundo(h):
    """Azul-marinho quase preto, mais claro no meio, com um brilho vermelho bem fraco."""
    small = Image.new("RGB", (W // 4, h // 4))
    px = small.load()
    cx, cy = W / 8, h / 8 * 1.05
    for y in range(h // 4):
        for x in range(W // 4):
            d = math.hypot((x - cx) / (W / 4), (y - cy) / (h / 4))
            k = max(0.0, 1 - d * 1.5)
            red = max(0.0, 1 - math.hypot((x - cx) / (W / 7), (y - cy * 1.1) / (h / 6))) * 0.5
            px[x, y] = (int(9 + 18 * k + 26 * red), int(11 + 17 * k + 4 * red), int(17 + 26 * k + 6 * red))
    return small.resize((W, h), Image.BICUBIC).filter(ImageFilter.GaussianBlur(6))


# ----------------------------------------------------------------------------- celular
class Celular:
    """Moldura de celular genérica (sem marca) com a captura na tela, e a sombra."""

    def __init__(self, nome, tela_w):
        shot = cap(nome)
        self.k = tela_w / shot.width
        tela = shot.resize((tela_w, round(shot.height * self.k)), Image.LANCZOS)
        self.bz = max(10, tela_w // 34)
        r_tela = tela_w // 9
        w, h = tela.width + 2 * self.bz, tela.height + 2 * self.bz
        im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        d.rounded_rectangle((0, 0, w - 1, h - 1), r_tela + self.bz, fill=(58, 62, 72))      # aro
        d.rounded_rectangle((3, 3, w - 4, h - 4), r_tela + self.bz - 3, fill=(16, 17, 21))  # corpo
        im.paste(tela, (self.bz, self.bz), mascara(tela.size, r_tela))
        cam = max(7, tela_w // 48)
        cx, cy = w // 2, self.bz + round(28 * tela_w / 500)
        d.ellipse((cx - cam, cy - cam, cx + cam, cy + cam), fill=(12, 12, 14))
        # brilho fino no aro de cima
        d.rounded_rectangle((1, 1, w - 2, h - 2), r_tela + self.bz - 1, outline=(96, 100, 112), width=1)
        self.im = im
        self.sombra, self.pad = sombra((w, h), r_tela + self.bz, 34, 200)

    def ponto(self, x, y, ox, oy, s=1.0):
        """Coordenada da captura (px) -> coordenada do quadro."""
        w, h = self.im.size
        cx, cy = ox + w / 2, oy + h / 2
        px, py = ox + self.bz + x * self.k, oy + self.bz + y * self.k
        return cx + (px - cx) * s, cy + (py - cy) * s


# ----------------------------------------------------------------------------- texto
def palavras(texto):
    """'Quando passa o *próximo metrô?*' -> [(palavra, destaque)]"""
    out, dest = [], False
    for p in texto.split():
        ini = p.startswith("*")
        if ini:
            dest = True
            p = p[1:]
        fim = p.endswith("*")
        if fim:
            p = p[:-1]
        out.append((p, dest))
        if fim:
            dest = False
    return out


def titulo(base, fm, texto, sub, t, instantaneo=False, sub_ini=None, sub_forte=False):
    """Título com revelação palavra a palavra (subindo e clareando) e subtítulo."""
    ov = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    fnt = F(BOLD, fm.tit)
    esp = d.textlength(" ", font=fnt)
    ws = palavras(texto)
    linhas, linha, larg = [], [], 0
    for p, dest in ws:
        lw = d.textlength(p, font=fnt)
        if linha and larg + esp + lw > W - 110:
            linhas.append((linha, larg))
            linha, larg = [], 0
        larg += (esp if linha else 0) + lw
        linha.append((p, dest, lw))
    linhas.append((linha, larg))
    y, i = fm.tit_y, 0
    for linha, larg in linhas:
        x = (W - larg) / 2
        for p, dest, lw in linha:
            a = 1.0 if instantaneo else ease((t - 0.05 * i) / 0.35)
            cor = CORAL if dest else WHITE
            d.text((x, y + (1 - a) * 22), p, font=fnt, fill=cor + (int(255 * a),),
                   stroke_width=3, stroke_fill=(0, 0, 0, int(110 * a)))  # contraste sobre a foto
            x += lw + esp
            i += 1
        y += int(fm.tit * 1.12)
    if sub:
        fs = F(BOLD, int(fm.sub * 1.5)) if sub_forte else F(MED, fm.sub)
        if sub_ini is not None:
            a = ease((t - sub_ini) / 0.35)
        else:
            a = 1.0 if instantaneo else ease((t - 0.25) / 0.4)
        sw = d.textlength(sub, font=fs)
        if sub_forte:  # a resposta: maior, em vermelho, sobe com um salto
            d.text(((W - sw) / 2, y + 10 + (1 - pop(a)) * 40), sub, font=fs, fill=CORAL + (int(255 * a),),
                   stroke_width=3, stroke_fill=(0, 0, 0, int(110 * a)))
        else:
            d.text(((W - sw) / 2, y + 14 + (1 - a) * 14), sub, font=fs, fill=SUB + (int(255 * a),))
    return Image.alpha_composite(base, ov)


# ----------------------------------------------------------------------------- cenas
class Foto:
    """Foto da estação em tela cheia (recorte vertical), com uma versão escura e desfocada
    para quando o celular entra por cima, e um degradê em cima para o título."""

    def __init__(self, nome, fm, foco=0.5, folga=1.08):
        im = Image.open(os.path.join(HERE, "capturas", "fotos", nome)).convert("RGB")
        k = fm.h * folga / im.height
        if im.width * k < W * folga:
            k = W * folga / im.width
        im = im.resize((round(im.width * k), round(im.height * k)), Image.LANCZOS)
        cw = round(W * folga)
        x0 = int(min(max(im.width * foco - cw / 2, 0), im.width - cw))
        im = im.crop((x0, 0, x0 + cw, im.height))
        # Desce a foto: faixa desfocada em cima, para o título não cair sobre placas da foto.
        pad = int(fm.h * 0.10)
        topo = im.crop((0, 0, im.width, 60)).resize((im.width, pad)).filter(ImageFilter.GaussianBlur(24))
        self.im = Image.new("RGB", (im.width, im.height + pad))
        self.im.paste(topo, (0, 0))
        self.im.paste(im, (0, pad))
        self.fy = (pad + 0.28 * im.height) / self.im.height  # o zoom não mexe nessa altura
        self.escura = Image.blend(self.im.filter(ImageFilter.GaussianBlur(18)),
                                  Image.new("RGB", self.im.size, (8, 10, 16)), 0.55)
        g = Image.new("L", (1, fm.h))
        for y in range(fm.h):
            topo = max(0.0, 1 - y / (fm.h * 0.42)) * 215
            base_ = max(0.0, (y - fm.h * 0.78) / (fm.h * 0.22)) * 150
            g.putpixel((0, y), int(max(topo, base_)))
        self.degrade = Image.new("RGBA", (W, fm.h), (6, 8, 14, 0))
        self.degrade.putalpha(g.resize((W, fm.h)))

    def quadro(self, fm, t, dur, m):
        """Abre aproximada e recua rápido (impacto no 1º segundo), depois aproxima devagar;
        m = 0 foto nítida, 1 escura e desfocada."""
        z = 1 + 0.14 * (1 - ease(t / 0.9)) + 0.04 * (t / dur)
        cw, ch = W * self.im.width / (W * 1.0) / z, fm.h * self.im.width / W / z
        cx = self.im.width / 2
        top = self.fy * (self.im.height - ch)
        box = (int(cx - cw / 2), int(top), int(cx + cw / 2), int(top + ch))
        src = self.im if m <= 0 else Image.blend(self.im, self.escura, m)
        out = src.crop(box).resize((W, fm.h), Image.BILINEAR).convert("RGBA")
        out.alpha_composite(self.degrade)
        return out


def cena_tela(texto, sub, captura, recorte, anel=None, t_zoom=1.2, dur=2.8, primeira=False,
              foto=None, foco=0.5, credito=None, t_cel=0.0, sub_ini=None, sub_forte=False,
              ent=0.45, salto=False):
    """Celular com a tela real; em t_zoom um toque e o recorte salta para fora, ampliado.

    recorte e anel em px da captura (1080x2400). anel = contorno vermelho dentro do recorte.
    foto: abre com a foto da estação em tela cheia; o celular entra por cima em t_cel."""
    cache = {}

    def fn(fm, t):
        if fm.nome not in cache:
            cache[fm.nome] = (Celular(captura, fm.tela_w), Foto(foto, fm, foco) if foto else None)
        cel, ft = cache[fm.nome]
        tt = t - t_cel
        if ft:
            m = ease(tt / 0.5) if tt > 0 else 0
            im = ft.quadro(fm, t, dur, m)
            if credito and m < 1:
                ImageDraw.Draw(im).text((W - 30, fm.h - (300 if fm.story else 40)), credito,
                                        font=F(MED, 22), fill=(255, 255, 255, int(200 * (1 - m))), anchor="rs")
        else:
            im = fm.fundo.convert("RGBA")
        if tt < 0:
            im = titulo(im, fm, texto, sub, t, instantaneo=primeira, sub_ini=sub_ini, sub_forte=sub_forte)
            return im.convert("RGB")
        # entrada do celular: sobe e aparece; depois um zoom bem lento
        a = 0.55 + 0.45 * ease(t / 0.35) if (primeira and not foto) else ease(tt / ent)
        s = 0.94 + 0.06 * (pop(tt / ent) if salto else a) + 0.025 * (tt / dur)
        w, h = cel.im.size
        sw, sh = int(w * s), int(h * s)
        ox = (W - sw) // 2
        oy = int(fm.cel_y + (1 - a) * 90 - (sh - h) / 2)
        sm = cel.sombra.resize((int(cel.sombra.width * s), int(cel.sombra.height * s)))
        im.alpha_composite(com_alpha(sm, a), (ox - int(cel.pad * s), oy - int(cel.pad * s) + int(30 * s)))
        im.alpha_composite(com_alpha(cel.im.resize((sw, sh), Image.BILINEAR), a), (ox, oy))
        base_ox, base_oy = (W - w) / 2, oy + (sh - h) / 2

        # toque antes do zoom
        if anel:
            tx, ty = cel.ponto((anel[0] + anel[2]) / 2, (anel[1] + anel[3]) / 2, base_ox, base_oy, s)
        else:
            tx, ty = cel.ponto((recorte[0] + recorte[2]) / 2, (recorte[1] + recorte[3]) / 2, base_ox, base_oy, s)
        p = (t - (t_zoom - 0.45)) / 0.45
        if 0 <= p <= 1:
            ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
            od = ImageDraw.Draw(ov)
            r = 26 + 40 * p
            od.ellipse((tx - r, ty - r, tx + r, ty + r), fill=(255, 255, 255, int(150 * (1 - p))),
                       outline=(255, 255, 255, int(230 * (1 - p))), width=3)
            od.ellipse((tx - 20, ty - 20, tx + 20, ty + 20), fill=(255, 255, 255, int(200 * (1 - p * 0.8))))
            im.alpha_composite(ov)

        # zoom: escurece o celular e o recorte salta para a frente
        z = (t - t_zoom) / 0.4
        if z > 0:
            ov = Image.new("RGBA", im.size, (6, 8, 14, int(120 * ease(z))))
            im.alpha_composite(ov)
            crop = cap(captura).crop(recorte)
            alvo_w = min(W - 90, int(crop.width * cel.k * s * 1.32))
            esc = alvo_w / crop.width * (0.86 + 0.14 * pop(z))
            cw, ch = int(crop.width * esc), int(crop.height * esc)
            c = crop.resize((cw, ch), Image.LANCZOS).convert("RGBA")
            if anel:
                dc = ImageDraw.Draw(c)
                k = 10
                x1, y1 = (anel[0] - recorte[0]) * esc - k, (anel[1] - recorte[1]) * esc - k
                x2, y2 = (anel[2] - recorte[0]) * esc + k, (anel[3] - recorte[1]) * esc + k
                dc.rounded_rectangle((x1, y1, x2, y2), 16, outline=VERMELHO, width=max(5, int(6 * esc / 1.2)))
            r = max(18, int(26 * esc / 1.3))
            c.putalpha(mascara(c.size, r))
            cx = (recorte[0] + recorte[2]) / 2
            cy = (recorte[1] + recorte[3]) / 2
            px, py = cel.ponto(cx, cy, base_ox, base_oy, s)
            px = min(max(px, cw / 2 + 45), W - cw / 2 - 45)
            x0, y0 = int(px - cw / 2), int(py - ch / 2)
            az = ease(z * 1.6)
            sh_, pad = sombra((cw, ch), r, 26, 210)
            im.alpha_composite(com_alpha(sh_, az), (x0 - pad, y0 - pad + 18))
            borda = Image.new("RGBA", (cw + 8, ch + 8), (0, 0, 0, 0))
            ImageDraw.Draw(borda).rounded_rectangle((0, 0, cw + 7, ch + 7), r + 4, fill=WHITE + (255,))
            im.alpha_composite(com_alpha(borda, az), (x0 - 4, y0 - 4))
            im.alpha_composite(com_alpha(c, az), (x0, y0))

        im = titulo(im, fm, texto, sub, t, instantaneo=primeira, sub_ini=sub_ini, sub_forte=sub_forte)
        return saida(im, fm, t, dur, entrada=not primeira)
    return fn, dur


def saida(im, fm, t, dur, entrada=True, corte=0.22):
    """Funde com o fundo no começo (se entrada) e no fim da cena."""
    a = 1.0
    if t > dur - corte:
        a = 1 - ease((t - (dur - corte)) / corte)
    if a < 1:
        im = Image.blend(fm.fundo.convert("RGBA"), im, a)
    return im.convert("RGB")


def cena_final(dur=3.4, iphone="Tem iPhone? Use pelo navegador", vel=1.0):
    cache = {}

    def base(fm):
        h = fm.h
        im = Image.new("RGBA", (W, h), VERMELHO + (255,))
        fx = 22 if fm.story else 18
        for i, cor in enumerate(FAIXAS):
            ImageDraw.Draw(im).rectangle((0, h - (3 - i) * fx * 1.6, W, h - (2 - i) * fx * 1.6), fill=cor)
        return im

    def fn(fm, t):
        t = t * vel
        if fm.nome not in cache:
            cache[fm.nome] = base(fm)
        im = cache[fm.nome].copy()
        h = fm.h
        st = fm.story
        # posições (story: deixa ~1500-1800 livre para a figurinha de link)
        ic_s = 270 if st else 210
        y_ic = 380 if st else 120
        # ícone com sombra, salta
        a = pop(t / 0.5)
        sz = max(1, int(ic_s * (0.6 + 0.4 * a)))
        ic = Image.open(ICON).convert("RGBA").resize((sz, sz), Image.LANCZOS)
        ic.putalpha(Image.composite(ic.getchannel("A"), Image.new("L", ic.size, 0), mascara(ic.size, int(sz * 0.22))))
        sh_, pad = sombra((sz, sz), int(sz * 0.22), 24, 120)
        x = (W - sz) // 2
        y = y_ic + (ic_s - sz) // 2
        im.alpha_composite(com_alpha(sh_, min(1, t / 0.3)), (x - pad, y - pad + 16))
        im.alpha_composite(com_alpha(ic, min(1, t / 0.2)), (x, y))

        ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
        d = ImageDraw.Draw(ov)
        y = y_ic + ic_s + (50 if st else 30)

        def central(texto, fnt, yy, cor, ta, dy=18):
            a = ease((t - ta) / 0.35)
            tw = d.textlength(texto, font=fnt)
            d.text(((W - tw) / 2, yy + (1 - a) * dy), texto, font=fnt, fill=cor + (int(255 * a),))

        fn_ = F(BOLD, 104 if st else 86)
        central("Metrô Ceará", fn_, y, WHITE, 0.15)
        y += int(fn_.size * 1.25)
        fs = F(SEMI, 42 if st else 36)
        central("Fortaleza · Cariri · Sobral", fs, y, (250, 226, 200), 0.25)
        y += int(fs.size * 2.0)

        # selos
        fp = F(SEMI, 34 if st else 30)
        selos = ["Grátis", "Sem anúncio", "Sem cadastro"]
        ph = int(fp.size * 2.25)
        cr = int(ph * 0.29)
        larg = [d.textlength(s_, font=fp) + ph * 0.55 + 2 * cr + 26 for s_ in selos]
        gap = 18
        x = (W - sum(larg) - gap * 2) / 2
        for i, (s_, lw) in enumerate(zip(selos, larg)):
            a = ease((t - 0.4 - 0.1 * i) / 0.3)
            if a <= 0:
                x += lw + gap
                continue
            yy = y + (1 - a) * 16
            d.rounded_rectangle((x, yy, x + lw, yy + ph), ph // 2, fill=(255, 255, 255, int(46 * a)))
            ccx, ccy = x + ph * 0.28 + cr, yy + ph / 2
            d.ellipse((ccx - cr, ccy - cr, ccx + cr, ccy + cr), fill=(255, 255, 255, int(255 * a)))
            k = cr * 0.45
            d.line([(ccx - k, ccy), (ccx - k * 0.25, ccy + k * 0.7), (ccx + k, ccy - k * 0.6)],
                   fill=VERMELHO + (int(255 * a),), width=max(3, int(cr * 0.28)), joint="curve")
            d.text((ccx + cr + 13, yy + (ph - fp.size) / 2 - fp.size * 0.12), s_, font=fp,
                   fill=WHITE + (int(255 * a),))
            x += lw + gap
        y += ph + (66 if st else 46)

        # botão
        fb = F(BOLD, 44 if st else 38)
        txt = "Baixe grátis no Google Play"
        bh = int(fb.size * 2.6)
        tw = d.textlength(txt, font=fb)
        seta = fb.size * 0.75
        bw = tw + seta + 36 + 2 * bh * 0.55
        a = pop((t - 0.75) / 0.45)
        al = min(1, max(0, (t - 0.75) / 0.2))
        if al > 0:
            bx, by = (W - bw) / 2, y
            cx, cy = W / 2, by + bh / 2
            ww, hh = bw * (0.85 + 0.15 * a), bh * (0.85 + 0.15 * a)
            # sombra do botão
            sh_, pad = sombra((int(ww), int(hh)), int(hh / 2), 22, 90)
            ov.alpha_composite(com_alpha(sh_, al), (int(cx - ww / 2 - pad), int(cy - hh / 2 - pad + 14)))
            d.rounded_rectangle((cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2), hh / 2,
                                fill=(252, 232, 232, int(255 * al)))
            if a > 0.9:
                x0 = (W - (seta + 36 + tw)) / 2
                cor = (205, 22, 32, int(255 * al))
                ax, ay = x0 + seta / 2, cy - seta * 0.45
                lwid = max(4, int(fb.size * 0.1))
                d.line([(ax, ay), (ax, ay + seta * 0.7)], fill=cor, width=lwid)
                d.line([(ax - seta * 0.32, ay + seta * 0.38), (ax, ay + seta * 0.7), (ax + seta * 0.32, ay + seta * 0.38)],
                       fill=cor, width=lwid, joint="curve")
                d.line([(ax - seta * 0.45, ay + seta * 0.95), (ax + seta * 0.45, ay + seta * 0.95)], fill=cor, width=lwid)
                d.text((x0 + seta + 36, cy - fb.size * 0.68), txt, font=fb, fill=cor)
        y += bh + (40 if st else 28)
        fi = F(MED, 32 if st else 28)
        central(iphone, fi, y, (255, 222, 222), 1.05, dy=10)
        # rodapé
        fr_ = F(MED, 26 if st else 22)
        rod = "App não oficial, feito por um passageiro"
        yr = h - 3 * (22 if st else 18) * 1.6 - (70 if st else 50)
        if st:
            yr = 1840 - 3 * 22 * 1.6 - 10
        central(rod, fr_, yr, (255, 210, 210), 1.15, dy=0)
        im = Image.alpha_composite(im, ov)
        # entra fundindo com o fundo escuro da cena anterior
        if t < 0.18:
            im = Image.blend(fm.fundo.convert("RGBA"), im, ease(t / 0.18))
        return im.convert("RGB")
    return fn, dur


# ----------------------------------------------------------------------------- estações
BAIRROS = {
    # Maracanaú → José de Alencar, terça 24/11/2026 (relógio do emulador 06:57 e 07:35, GPS na estação).
    "maracanau": ("maracanau-vitrine", [
        # Os 4 primeiros segundos decidem (tempo médio dos posts antigos: 4 s). Pesa mais a palavra
        # do que a velocidade: a espera é a dor de quem anda de transporte. Foto do embarque com
        # zoom de impacto e a pergunta; 1,0 s o celular entra com salto; 1,7 s a resposta, grande.
        cena_tela("Cansado de *esperar* o metrô em Maracanaú?", "Veja antes de sair de casa",
                  "maracanau-inicio.png", recorte=(42, 474, 1038, 965), anel=(655, 715, 1005, 830),
                  t_zoom=1.55, dur=4.0, primeira=True, foto="estacao-maracanau-08.jpg", foco=0.5,
                  credito="Foto: Joelkaula · CC BY-SA 4.0", t_cel=1.0, sub_ini=1.7, sub_forte=True,
                  ent=0.3, salto=True),
        # Depois, mais calmo: quem ficou quer ver o resto.
        cena_tela("Vai até a *José de Alencar?*", "Hora de embarcar e de chegar",
                  "maracanau-viagem.png", recorte=(84, 1160, 996, 1776), anel=(112, 1378, 530, 1460),
                  t_zoom=1.2, dur=3.2),
        cena_tela("Avisa a hora de *descer*", "Até com a tela desligada",
                  "maracanau-aviso-descer.png", recorte=(42, 570, 1038, 930), t_zoom=1.1, dur=3.0),
        cena_final(3.4),
    ]),
    "carlito": ("carlito-benevides-vitrine", [
        cena_tela("Pega o metrô na *Carlito Benevides?*", "Abre o app e veja quanto falta",
                  "carlito-inicio.png", recorte=(40, 500, 1040, 1010), anel=(650, 840, 1010, 965),
                  t_zoom=1.25, dur=3.4, primeira=True),
        cena_tela("Avisa a hora de *sair* de casa", "Toque num horário e seja avisado",
                  "hora-de-sair-aviso.png", recorte=(45, 568, 1035, 1040), t_zoom=1.0, dur=2.8),
        cena_tela("Planeje a *viagem*", "Hora de chegada e integração",
                  "casa-viagem.png", recorte=(80, 1590, 1000, 2080), anel=(110, 1818, 760, 1898),
                  t_zoom=1.0, dur=2.8),
        cena_final(3.6),
    ]),
}


# ----------------------------------------------------------------------------- saída
def linha(cenas):
    t, out = 0.0, []
    for fn, dur in cenas:
        out.append((t, t + dur, fn))
        t += dur
    return out, t


def quadro(cenas, fm, t):
    tl, _ = linha(cenas)
    for a, b, fn in tl:
        if a <= t < b:
            return fn(fm, t - a)
    a, b, fn = tl[-1]
    return fn(fm, b - a - 1e-3)


def video(cenas, fm, dst, largura=None, crf=18):
    _, dur = linha(cenas)
    cmd = [FF, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{fm.h}",
           "-r", str(FPS), "-i", "-"]
    if largura:
        cmd += ["-vf", f"scale={largura}:-2"]
    cmd += ["-c:v", "libx264", "-preset", "slow", "-crf", str(crf), "-pix_fmt", "yuv420p",
            "-movflags", "+faststart", dst]
    p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    for n in range(int(dur * FPS)):
        p.stdin.write(quadro(cenas, fm, n / FPS).tobytes())
    p.stdin.close()
    p.wait()
    print(dst)


def folha(cenas, fm, dst):
    tl, _ = linha(cenas)
    ts = [0.0]
    for a, b, _ in tl:
        ts += [round(a + (b - a) * 0.3, 2), round(a + (b - a) * 0.85, 2)]
    lw = 270
    lh = int(fm.h * lw / W)
    s = Image.new("RGB", (len(ts) * (lw + 10) + 10, lh + 20), (255, 255, 255))
    for i, t in enumerate(ts):
        s.paste(quadro(cenas, fm, t).resize((lw, lh), Image.LANCZOS), (10 + i * (lw + 10), 10))
    s.save(dst)
    print(dst)


def main():
    args = sys.argv[1:]
    previa = "--previa" in args
    nomes = [a for a in args if a in BAIRROS] or sorted(BAIRROS)
    formatos = [Formato("story", 1920, True), Formato("post", 1350, False)]
    for b in nomes:
        nome, cenas = BAIRROS[b]
        for fm in formatos:
            base = f"{nome}-{fm.nome}"
            if previa:
                os.makedirs(PREVIA, exist_ok=True)
                folha(cenas, fm, os.path.join(PREVIA, base + ".png"))
            else:
                os.makedirs(OUT, exist_ok=True)
                video(cenas, fm, os.path.join(OUT, base + ".mp4"))


if __name__ == "__main__":
    main()
