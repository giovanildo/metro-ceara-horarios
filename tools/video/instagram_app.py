"""Carrossel da Nina (dia 16 de divulgacao/instagram-calendario/), em 1080x1350 (4:5), roxo
da cor do rostinho no app.

Os outros dias do app (1 a 15) viraram, em 21/11/2026, vídeos de conversa feitos por
historias.py; as funções de carrossel (capa, tela, lista, final) ficam aqui para a Nina e
para posts novos que precisem ser imagem.

    VIDEO_WORK=/tmp/metro-video python3 tools/video/instagram_app.py
"""
import os
import shutil
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

ROOT = fr.ROOT
HERE = os.path.dirname(os.path.abspath(__file__))
CAP = os.path.join(HERE, "capturas", "instagram")
OUT = os.path.join(ROOT, "divulgacao", "instagram-calendario")
WORK = os.path.join(os.environ.get("VIDEO_WORK", "/tmp/metro-video"), "insta")
FF = os.environ.get("FFMPEG", "ffmpeg")
os.makedirs(OUT, exist_ok=True)
os.makedirs(WORK, exist_ok=True)
SITE = "giovanildo.github.io/metro-ceara-horarios"

PLAY = "https://play.google.com/store/apps/details?id=io.github.giova.metroceara&referrer=utm_source%3Dinstagram"
LITE = "https://giovanildo.github.io/metro-ceara-horarios/app/?utm_source=instagram"

W, H = 1080, 1350
WHITE = (255, 255, 255)
INK = (28, 28, 30)
AZUL = dict(bg=(47, 69, 120), sub=(217, 226, 255), cor=(47, 69, 120))
AVISO = dict(bg=(237, 28, 36), sub=(255, 220, 222), cor=(200, 20, 30))
VERDE = (22, 130, 70)
# Roxo do rostinho da Nina no app (res/drawable/ic_nina.xml).
ROXO = dict(bg=(107, 74, 158), sub=(232, 222, 248), cor=(107, 74, 158))
F = fr.font
B, R = fr.BOLD, fr.REG


def cap(name):
    return os.path.join(CAP, name)


def rounded(im, r):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), r, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), m)
    return out


def centered(d, text, fnt, y, fill, width=W - 120, gap=12):
    for line in fr.wrap(d, text, fnt, width):
        d.text(((W - d.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
        y += fnt.size + gap
    return y


def rodape(im, d, tema, texto="Grátis no Google Play · sem anúncios"):
    ic, mask = fr.rounded_icon(90)
    fy = H - 125
    fn, fc = F(B, 44), F(R, 32)
    t1 = "Metrô Ceará"
    tw = max(d.textlength(t1, font=fn), d.textlength(texto, font=fc))
    x0 = (W - (90 + 24 + tw)) / 2
    im.paste(ic, (int(x0), fy), mask)
    d.text((x0 + 114, fy - 4), t1, font=fn, fill=WHITE)
    d.text((x0 + 114, fy + 50), texto, font=fc, fill=tema["sub"])


def numero(d, n, total, tema):
    """'2/5' discreto no canto, para quem vê o carrossel saber que tem mais."""
    if not total:
        return
    s = f"{n}/{total}"
    fs = F(B, 30)
    d.text((W - 50 - d.textlength(s, font=fs), 34), s, font=fs, fill=tema["sub"])


def tela(name, title, sub, pieces, tema=AZUL, n=None, total=None, rod=True):
    """Título + recortes de telas do app, emendados, numa moldura branca."""
    im = Image.new("RGB", (W, H), tema["bg"])
    d = ImageDraw.Draw(im)
    numero(d, n, total, tema)
    y = centered(d, title, F(B, 70), 64, WHITE, gap=10)
    if sub:
        y = centered(d, sub, F(R, 38), y + 6, tema["sub"], width=W - 150, gap=10)
    y += 30
    parts = [Image.open(cap(f) if not os.path.isabs(f) else f).convert("RGB").crop(box) for f, box in pieces]
    s = Image.new("RGB", (max(p.width for p in parts), sum(p.height for p in parts)), WHITE)
    yy = 0
    for p in parts:
        s.paste(p, (0, yy))
        yy += p.height
    foot = 170 if rod else 50
    box_h, box_w = H - foot - y - 20, 820
    k = min(box_w / s.width, box_h / s.height)
    s = s.resize((int(s.width * k), int(s.height * k)), Image.LANCZOS)
    bw = 12
    frame = Image.new("RGB", (s.width + 2 * bw, s.height + 2 * bw), WHITE)
    frame.paste(s, (bw, bw))
    frame = rounded(frame, 34)
    # Recorte baixo (uma faixa) fica no meio do espaço, não colado no título.
    y += max(0, (box_h - frame.height) // 2 - 40)
    im.paste(frame, ((W - frame.width) // 2, y), frame)
    if rod:
        rodape(im, d, tema)
    save(im, name)


def marca(d, x, y, ok):
    """Bolinha verde com visto ou vermelha com X (a fonte não tem os símbolos)."""
    r = 22
    d.ellipse((x, y, x + 2 * r, y + 2 * r), fill=VERDE if ok else AVISO["bg"])
    if ok:
        d.line([(x + 11, y + 23), (x + 19, y + 31), (x + 34, y + 14)], fill=WHITE, width=6, joint="curve")
    else:
        d.line([(x + 14, y + 14), (x + 30, y + 30)], fill=WHITE, width=6)
        d.line([(x + 30, y + 14), (x + 14, y + 30)], fill=WHITE, width=6)


def lista(name, title, itens, tema=AZUL, sub=None, n=None, total=None, nota=None, size=42):
    """Título + cartão branco com itens. Cada item: texto, (texto, True/False) com
    visto/X, ou (negrito, texto) para um rótulo com explicação. O bloco todo fica
    centralizado na altura, acima do rodapé."""
    main = Image.new("RGB", (W, H), tema["bg"])
    im = Image.new("RGBA", (W, 2 * H), tema["bg"] + (0,))
    d = ImageDraw.Draw(im)
    y = centered(d, title, F(B, 70), 0, WHITE, gap=8)
    if sub:
        y = centered(d, sub, F(R, 36), y + 4, tema["sub"], width=W - 150, gap=8)
    y += 34
    x0, x1 = 70, W - 70
    fb, fr_ = F(B, size), F(R, size)
    rows = []
    for it in itens:
        if isinstance(it, tuple) and isinstance(it[1], bool):
            rows.append(("marca", it[1], fr.wrap(d, it[0], fr_, x1 - x0 - 140)))
        elif isinstance(it, tuple):
            rows.append(("rotulo", fr.wrap(d, it[0], fb, x1 - x0 - 90), fr.wrap(d, it[1], fr_, x1 - x0 - 90)))
        else:
            rows.append(("texto", None, fr.wrap(d, it, fr_, x1 - x0 - 120)))
    lh = size + 14
    h = 50
    for r in rows:
        h += (len(r[1]) + len(r[2])) * lh + 34 if r[0] == "rotulo" else len(r[2]) * lh + 34
    h += 16
    d.rounded_rectangle((x0, y, x1, y + h), radius=36, fill=WHITE)
    yy = y + 46
    for kind, a, b in rows:
        if kind == "marca":
            marca(d, x0 + 40, yy + 4, a)
            for line in b:
                d.text((x0 + 110, yy), line, font=fr_, fill=INK)
                yy += lh
        elif kind == "rotulo":
            for line in a:
                d.text((x0 + 45, yy), line, font=fb, fill=tema["cor"])
                yy += lh
            for line in b:
                d.text((x0 + 45, yy), line, font=fr_, fill=INK)
                yy += lh
        else:
            d.ellipse((x0 + 48, yy + size // 2 - 4, x0 + 62, yy + size // 2 + 10), fill=tema["cor"])
            for line in b:
                d.text((x0 + 90, yy), line, font=fr_, fill=INK)
                yy += lh
        yy += 34
    end = y + h
    if nota:
        end = centered(d, nota, F(R, 30), y + h + 26, tema["sub"], width=W - 160, gap=6)
    top = max(70, (H - 170 - end) // 2)
    main.paste(im, (0, top), im)
    dm = ImageDraw.Draw(main)
    numero(dm, n, total, tema)
    rodape(main, dm, tema)
    save(main, name)


def capa(name, kicker, title, sub, tema=AZUL, total=None, icone=None):
    """Primeira página de carrossel: só texto grande e o convite para arrastar.
    icone: uma imagem RGBA posta em cima do texto."""
    im = Image.new("RGB", (W, H), tema["bg"])
    d = ImageDraw.Draw(im)
    numero(d, 1, total, tema)
    ft, fs, fk = F(B, 96), F(R, 44), F(B, 36)
    lt = fr.wrap(d, title, ft, W - 140)
    ls = fr.wrap(d, sub, fs, W - 170) if sub else []
    hh = 60 + len(lt) * 112 + 30 + len(ls) * 58 + (icone.height + 40 if icone else 0)
    y = (H - 160 - hh) // 2
    if icone:
        im.paste(icone, ((W - icone.width) // 2, y), icone)
        y += icone.height + 40
    y = centered(d, kicker.upper(), fk, y, tema["sub"]) + 10
    y = centered(d, title, ft, y, WHITE, width=W - 140, gap=16) + 30
    if sub:
        y = centered(d, sub, fs, y, tema["sub"], width=W - 170, gap=14)
    # A fonte não tem a seta: o triângulo é desenhado.
    seta = "Arraste para o lado"
    fa = F(B, 38)
    tw = d.textlength(seta, font=fa) + 50
    x = (W - tw) / 2
    d.rounded_rectangle((x - 36, y + 50, x + tw + 36, y + 130), radius=40, fill=WHITE)
    d.text((x, y + 66), seta, font=fa, fill=tema["cor"])
    ax = x + tw - 26
    d.polygon([(ax, y + 74), (ax + 26, y + 90), (ax, y + 106)], fill=tema["cor"])
    rodape(im, d, tema)
    save(im, name)


def final(name, tema=AZUL, n=None, total=None, qr=None, qr_text=None, title="Metrô Ceará",
          body="Horários de metrô e VLT no celular", nota="Baixe grátis no Google Play: link na bio"):
    im = Image.new("RGB", (W, H), tema["bg"])
    d = ImageDraw.Draw(im)
    numero(d, n, total, tema)
    size = 200 if qr else 280
    ic, mask = fr.rounded_icon(size)
    blocks = size + 40 + 110 + 70 + 70 + (520 if qr else 160)
    y = max(60, (H - 90 - blocks) // 2)
    im.paste(ic, ((W - size) // 2, y), mask)
    y += size + 36
    y = centered(d, title, F(B, 88), y, WHITE)
    y = centered(d, body, F(R, 44), y, tema["sub"])
    y = centered(d, "Grátis · sem anúncios · sem cadastro", F(B, 40), y + 10, WHITE) + 30
    if qr:
        q = fr.qr_image(qr, 380)
        pad = 26
        bx = (W - q.size[0]) // 2
        d.rounded_rectangle((bx - pad, y, bx + q.size[0] + pad, y + q.size[1] + 2 * pad + 60), radius=28, fill=WHITE)
        im.paste(q, (bx, y + pad))
        fs = F(B, 32)
        d.text(((W - d.textlength(qr_text, font=fs)) / 2, y + q.size[1] + pad + 8), qr_text, font=fs, fill=tema["cor"])
        y += q.size[1] + 2 * pad + 60 + 30
    else:
        fb = F(B, 40)
        tw = d.textlength(nota, font=fb)
        d.rounded_rectangle(((W - tw) / 2 - 40, y + 10, (W + tw) / 2 + 40, y + 100), radius=45, fill=WHITE)
        d.text(((W - tw) / 2, y + 30), nota, font=fb, fill=tema["cor"])
        y += 130
    fs = F(R, 28)
    t = "Fortaleza · Cariri · Sobral · app não oficial, feito por um passageiro"
    d.text(((W - d.textlength(t, font=fs)) / 2, H - 70), t, font=fs, fill=tema["sub"])
    save(im, name)


def save(im, name):
    path = os.path.join(OUT, name)
    im.save(path, optimize=True)
    print(path)


def nina_icon(size):
    """O rostinho da Nina do app (ic_nina.xml, viewport 48), redesenhado em Pillow."""
    k = 4 * size / 48
    im = Image.new("RGBA", (4 * size, 4 * size), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def c(cx, cy, r, fill):
        d.ellipse(((cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k), fill=fill)
    d.rounded_rectangle((0, 0, 48 * k - 1, 48 * k - 1), radius=12 * k, fill=(107, 74, 158))
    for cx, cy, r in ((17, 17, 6), (24, 13, 6.5), (31, 17, 6), (14, 24, 5), (34, 24, 5)):
        c(cx, cy, r, (43, 29, 64))
    c(24, 24, 8, (198, 138, 94))
    # Corpo: M12,44 c1,-8 6,-11 12,-11 s11,3 12,11 z (duas curvas, aproximadas por pontos)
    def bez(p0, p1, p2, p3, n=24):
        return [tuple((1 - t) ** 3 * a + 3 * (1 - t) ** 2 * t * b + 3 * (1 - t) * t ** 2 * cc + t ** 3 * dd
                      for a, b, cc, dd in zip(p0, p1, p2, p3)) for t in (i / n for i in range(n + 1))]
    pts = bez((12, 44), (13, 36), (18, 33), (24, 33)) + bez((24, 33), (30, 33), (35, 36), (36, 44))
    d.polygon([(x * k, y * k) for x, y in pts], fill=WHITE)
    d.rounded_rectangle((33 * k, 6 * k, 44 * k, 15 * k), radius=2 * k, fill=WHITE)
    d.polygon([(37 * k, 15 * k), (34 * k, 18 * k), (34 * k, 15 * k)], fill=WHITE)
    d.rounded_rectangle((37.2 * k, 7.5 * k, 38.8 * k, 11.1 * k), radius=0.8 * k, fill=(107, 74, 158))
    c(38, 12.8, 0.9, (107, 74, 158))
    return im.resize((size, size), Image.LANCZOS)


def com_borda(icone, b=14):
    """Uma moldura branca arredondada, para o ícone roxo não sumir no fundo roxo."""
    out = Image.new("RGBA", (icone.width + 2 * b, icone.height + 2 * b), (0, 0, 0, 0))
    ImageDraw.Draw(out).rounded_rectangle((0, 0, out.width - 1, out.height - 1),
                                          radius=icone.width // 4 + b, fill=WHITE)
    out.paste(icone, (b, b), icone)
    return out


def marca_nina(dst):
    """A barra de baixo do app com um anel em volta da Nina."""
    im = Image.open(cap("inicio.png")).convert("RGB").crop((0, 2110, 1080, 2340))
    d = ImageDraw.Draw(im)
    cx, cy = 760, 110
    d.ellipse((cx - 98, cy - 98, cx + 98, cy + 98), outline=ROXO["bg"], width=10)
    im.save(dst)
    return dst


# --- Reels ---

def moldura_reels(path, title, sub, tema):
    """Como frames.phone_frame, mas na cor do tema e sem o rodapé vermelho."""
    im = Image.new("RGBA", (fr.W, fr.H), tema["bg"] + (255,))
    d = ImageDraw.Draw(im)
    y = 50
    for line in fr.wrap(d, title, F(B, 64), fr.W - 100):
        d.text(((fr.W - d.textlength(line, font=F(B, 64))) / 2, y), line, font=F(B, 64), fill=WHITE)
        y += 74
    if sub:
        for line in fr.wrap(d, sub, F(R, 38), fr.W - 140):
            d.text(((fr.W - d.textlength(line, font=F(R, 38))) / 2, y + 6), line, font=F(R, 38), fill=tema["sub"])
            y += 48
    d.rounded_rectangle((fr.PH_X - fr.BORDER, fr.PH_Y - fr.BORDER, fr.PH_X + fr.PH_W + fr.BORDER,
                         fr.PH_Y + fr.PH_H + fr.BORDER), radius=fr.RADIUS + fr.BORDER, fill=fr.DARK)
    hole = Image.new("L", (fr.W, fr.H), 0)
    ImageDraw.Draw(hole).rounded_rectangle((fr.PH_X, fr.PH_Y, fr.PH_X + fr.PH_W, fr.PH_Y + fr.PH_H),
                                           radius=fr.RADIUS, fill=255)
    im.putalpha(Image.eval(hole, lambda v: 255 - v))
    d = ImageDraw.Draw(im)
    t = "Metrô Ceará · grátis no Google Play · app não oficial"
    fs = F(R, 32)
    d.text(((fr.W - d.textlength(t, font=fs)) / 2, fr.H - 76), t, font=fs, fill=tema["sub"])
    im.save(path)


def cartao_final_reels(path, tema):
    im = Image.new("RGB", (fr.W, fr.H), tema["bg"])
    d = ImageDraw.Draw(im)
    ic, mask = fr.rounded_icon(240)
    y = 250
    im.paste(ic, ((fr.W - 240) // 2, y), mask)
    y += 290

    def c(text, fnt, fill, y, gap=14):
        for line in fr.wrap(d, text, fnt, fr.W - 140):
            d.text(((fr.W - d.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
            y += fnt.size + gap
        return y
    y = c("Metrô Ceará", F(B, 96), WHITE, y)
    y = c("Horários de metrô e VLT no celular", F(R, 48), tema["sub"], y + 6)
    y = c("Grátis · sem anúncios · sem cadastro", F(B, 44), WHITE, y + 20) + 50
    q = fr.qr_image(PLAY, 420)
    pad = 30
    bx = (fr.W - q.size[0]) // 2
    d.rounded_rectangle((bx - pad, y, bx + q.size[0] + pad, y + q.size[1] + 2 * pad + 66), radius=30, fill=WHITE)
    im.paste(q, (bx, y + pad))
    t = "Baixe grátis no Google Play"
    fs = F(B, 36)
    d.text(((fr.W - d.textlength(t, font=fs)) / 2, y + q.size[1] + pad + 10), t, font=fs, fill=tema["cor"])
    y += q.size[1] + 2 * pad + 66 + 40
    c("Link na bio · Fortaleza · Cariri · Sobral", F(R, 36), tema["sub"], y)
    im.save(path)


def reels(name, src, title, sub, start, end=None, speed=1.0, tema=AZUL):
    fr.PH_H = 1420
    fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
    fr.PH_X = (fr.W - fr.PH_W) // 2
    fr.PH_Y = 400
    base = os.path.join(WORK, os.path.splitext(name)[0])
    cfr = base + "-cfr.mp4"
    fr.to_cfr(FF, os.path.join(WORK, src), cfr)
    # A gravação termina no último quadro que mudou: segura a tela final por 2 s.
    pad = base + "-pad.mp4"
    subprocess.run([FF, "-v", "error", "-y", "-i", cfr, "-vf", "tpad=stop_mode=clone:stop_duration=2",
                    "-c:v", "libx264", "-crf", "16", "-pix_fmt", "yuv420p", pad], check=True)
    if end is None:
        dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0",
                                    pad], capture_output=True, text=True, check=True).stdout)
        end = dur - 0.1
    png = base + "-moldura.png"
    moldura_reels(png, title, sub, tema)
    seg = base + "-seg.mp4"
    fr.clip(FF, pad, png, seg, [(start, end)], speed)
    fim_png, fim = base + "-fim.png", base + "-fim.mp4"
    cartao_final_reels(fim_png, tema)
    fr.still(FF, fim_png, 3.5, fim)
    final_path = os.path.join(OUT, name)
    fr.concat(FF, [seg, fim], base + "-list.txt", final_path)
    print(final_path)


def main():
    """Só o dia 16 (Nina): os outros dias viraram vídeos de conversa em historias.py (21/11)."""
    # Dia 16 (qui 10/12) — Carrossel: Nina. Pedido dele em 21/11: um post com ênfase na Nina.
    # Texto das notícias do Metrofor (22/12/2025 e 02/06/2026): a Super NINA não chama ajuda
    # na hora; na hora, funcionário da estação ou do metrô, e a PM pelo 190.
    T = 5
    capa("dia16-nina-1.png", "Metrô e VLT de Fortaleza e região", "Conheça a Nina",
         "O canal para denunciar importunação sexual no metrô e no VLT", tema=ROXO, total=T,
         icone=com_borda(nina_icon(220)))
    lista("dia16-nina-2.png", "O que é a Nina", [
        ("Super NINA, pelo WhatsApp", "Recebe denúncias de importunação sexual nas estações e dentro do metrô e do VLT."),
        ("Segura e sigilosa", "Quem sofreu ou quem viu pode contar o dia, a hora, o lugar e o que aconteceu."),
        ("Onde vale", "Fortaleza, Caucaia, Maracanaú e Pacatuba: Linha Sul, Linha Oeste, Linha Nordeste e VLT Aeroporto."),
    ], tema=ROXO, n=2, total=T, size=40,
        nota="Do Metrofor com a Etufor e a Secretaria das Mulheres do Ceará")
    lista("dia16-nina-3.png", "Se acontecer com você ou na sua frente", [
        ("1. Na hora", "Procure um funcionário da estação ou do metrô, ou peça a alguém que avise. A Polícia Militar é chamada pelo 190."),
        ("2. Depois, conte para a Nina", "WhatsApp (85) 93300-7001"),
        ("3. Faça o boletim de ocorrência", "Ajuda a identificar o autor e a aplicar a lei."),
    ], tema=ROXO, n=3, total=T, size=40, nota="A Nina não chama ajuda na hora. Orientação do Metrofor.")
    tela("dia16-nina-4.png", "A Nina a um toque", "No Metrô Ceará, ela fica na barra de baixo: toque e a conversa abre no WhatsApp",
         [(marca_nina(os.path.join(WORK, "inicio-nina.png")), (0, 0, 1080, 230))], tema=ROXO, n=4, total=T)
    final("dia16-nina-5.png", tema=ROXO, n=5, total=T)


if __name__ == "__main__":
    main()
