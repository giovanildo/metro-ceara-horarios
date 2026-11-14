"""Imagens do kit da página do bairro (divulgacao/kit-pagina-bairro/): 1080x1350, 4:5 do feed do Facebook.

Capturas da loja (fastlane) + capturas/domingo-1.100.png, tirada no emulador com o relógio
num domingo comum (22/11/2026; 15/11 era domingo de Enem e mostrava horários).
O vídeo do post 3 é um corte do vídeo de divulgação, a partir do modo viagem:
  ffmpeg -ss 14.25 -i divulgacao/metro-ceara-divulgacao.mp4 -c:v libx264 -crf 18 \
    -pix_fmt yuv420p -movflags +faststart -an divulgacao/kit-pagina-bairro/post3-hora-de-descer.mp4
(14.25 s = início do trecho "Modo viagem"; se o vídeo de divulgação mudar, confira o ponto.)
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SCR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "capturas")
OUT = os.path.join(ROOT, "divulgacao", "kit-pagina-bairro")
os.makedirs(OUT, exist_ok=True)
CAPS = os.path.join(ROOT, "fastlane/metadata/android/pt-BR/images/phoneScreenshots")

W, H = 1080, 1350
RED = (237, 28, 36)
WHITE = (255, 255, 255)
PINK = (255, 220, 222)
BOLD = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
ICON = os.path.join(ROOT, "docs", "img", "icon.png")


def f(p, s):
    return ImageFont.truetype(p, s)


def wrap(d, text, fnt, width):
    lines, line = [], ""
    for w in text.split():
        t = (line + " " + w).strip()
        if d.textlength(t, font=fnt) <= width:
            line = t
        else:
            lines.append(line)
            line = w
    lines.append(line)
    return lines


def rounded(im, r):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), r, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), m)
    return out


def card(name, title, sub, shot, crop_frac=(0.0, 1.0)):
    im = Image.new("RGB", (W, H), RED)
    d = ImageDraw.Draw(im)
    y = 56
    ft = f(BOLD, 74)
    for line in wrap(d, title, ft, W - 120):
        d.text(((W - d.textlength(line, font=ft)) / 2, y), line, font=ft, fill=WHITE)
        y += 88
    fs = f(REG, 40)
    y += 6
    for line in wrap(d, sub, fs, W - 140):
        d.text(((W - d.textlength(line, font=fs)) / 2, y), line, font=fs, fill=PINK)
        y += 52
    y += 30
    # Captura: recorta a faixa pedida e encaixa até o rodapé.
    s = Image.open(shot).convert("RGB")
    parts = crop_frac if isinstance(crop_frac[0], tuple) else [crop_frac]
    pieces = [s.crop((0, int(s.height * a), s.width, int(s.height * b))) for a, b in parts]
    s = Image.new("RGB", (pieces[0].width, sum(p.height for p in pieces)), WHITE)
    yy = 0
    for p in pieces:
        s.paste(p, (0, yy)); yy += p.height
    foot = 150
    box_h = H - foot - y - 24
    box_w = 760
    scale = min(box_w / s.width, box_h / s.height)
    s = s.resize((int(s.width * scale), int(s.height * scale)), Image.LANCZOS)
    bw = 12
    frame = Image.new("RGB", (s.width + 2 * bw, s.height + 2 * bw), WHITE)
    frame.paste(s, (bw, bw))
    frame = rounded(frame, 36)
    x = (W - frame.width) // 2
    im.paste(frame, (x, y), frame)
    # Rodapé: ícone + nome + chamada.
    ic = Image.open(ICON).convert("RGBA").resize((96, 96), Image.LANCZOS)
    ic = rounded(ic, 22)
    fy = H - foot + 30
    fn, fc = f(BOLD, 46), f(REG, 34)
    t1, t2 = "Metrô Ceará", "Grátis no Google Play · sem anúncios"
    tw = max(d.textlength(t1, font=fn), d.textlength(t2, font=fc))
    x0 = (W - (96 + 24 + tw)) / 2
    im.paste(ic, (int(x0), fy), ic)
    d.text((x0 + 120, fy - 2), t1, font=fn, fill=WHITE)
    d.text((x0 + 120, fy + 54), t2, font=fc, fill=PINK)
    path = os.path.join(OUT, name)
    im.save(path, optimize=True)
    print(path)


card("post1-apresentacao.png", "Quando passa o próximo metrô?",
     "O app mostra a estação mais perto de você e quanto falta",
     os.path.join(CAPS, "1.png"), (0.0, 0.70))
card("post2-domingo.png", "Domingo não tem metrô",
     "O app avisa e diz a hora que volta na segunda",
     os.path.join(SCR, "domingo-1.100.png"), (0.0, 0.56))
card("post3-hora-de-descer.png", "Cochila no metrô?",
     "O celular avisa antes da sua estação, até com a tela desligada",
     os.path.join(CAPS, "3.png"), ((0.0, 0.16), (0.44, 0.80)))
card("post4-widget.png", "O horário sem abrir o app",
     "Coloque o widget na tela inicial do celular",
     os.path.join(CAPS, "4.png"), (0.22, 0.95))
