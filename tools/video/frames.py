"""Quadros (PNG 1080x1920) para os vídeos: moldura com legenda e cartões."""
import os

import qrcode
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

W, H = 1080, 1920
RED = (237, 28, 36)
DARK = (28, 28, 30)
WHITE = (255, 255, 255)
BOLD = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
REG = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
ICON = os.path.join(ROOT, "docs", "img", "icon.png")
SITE = "giovanildo.github.io/metro-fortaleza-horarios"

# Onde o vídeo/tela do celular entra. Cada vídeo ajusta PH_W/PH_X ao formato da tela.
PH_H = 1420
PH_W = round(1080 * PH_H / 2272 / 2) * 2
PH_X = (W - PH_W) // 2
PH_Y = 390
RADIUS = 44
BORDER = 16


def font(path, size):
    return ImageFont.truetype(path, size)


def wrap(draw, text, fnt, width):
    lines = []
    for para in text.split("\n"):
        words, line = para.split(), ""
        for w in words:
            test = (line + " " + w).strip()
            if draw.textlength(test, font=fnt) <= width:
                line = test
            else:
                lines.append(line)
                line = w
        lines.append(line)
    return lines


def text_block(draw, text, fnt, y, width=W - 120, fill=WHITE, gap=10, center=True):
    for line in wrap(draw, text, fnt, width):
        w = draw.textlength(line, font=fnt)
        x = (W - w) / 2 if center else 60
        draw.text((x, y), line, font=fnt, fill=fill)
        y += fnt.size + gap
    return y


def footer(draw):
    f = font(REG, 30)
    t = "App não oficial, sem vínculo com o Metrofor."
    draw.text(((W - draw.textlength(t, font=f)) / 2, H - 70), t, font=f, fill=(255, 220, 222))


def phone_frame(path, title, subtitle=None, step=None):
    """Fundo vermelho com legenda e um buraco transparente arredondado para a tela."""
    im = Image.new("RGBA", (W, H), RED + (255,))
    d = ImageDraw.Draw(im)
    y = 40
    if step is not None:
        r = 36
        d.ellipse((W / 2 - r, y, W / 2 + r, y + 2 * r), fill=WHITE)
        fs = font(BOLD, 42)
        s = str(step)
        d.text((W / 2 - d.textlength(s, font=fs) / 2, y + 6), s, font=fs, fill=RED)
        y += 2 * r + 14
    y = text_block(d, title, font(BOLD, 58), y, gap=4)
    if subtitle:
        text_block(d, subtitle, font(REG, 36), y + 4, gap=4)
    d.rounded_rectangle(
        (PH_X - BORDER, PH_Y - BORDER, PH_X + PH_W + BORDER, PH_Y + PH_H + BORDER),
        radius=RADIUS + BORDER, fill=DARK,
    )
    hole = Image.new("L", (W, H), 0)
    ImageDraw.Draw(hole).rounded_rectangle((PH_X, PH_Y, PH_X + PH_W, PH_Y + PH_H), radius=RADIUS, fill=255)
    im.putalpha(Image.eval(hole, lambda v: 255 - v))
    footer(ImageDraw.Draw(im))
    im.save(path)


def qr_image(url, size):
    q = qrcode.QRCode(border=2, box_size=1, error_correction=qrcode.constants.ERROR_CORRECT_M)
    q.add_data(url)
    q.make(fit=True)
    img = q.make_image(fill_color="black", back_color="white").convert("RGB")
    k = max(1, size // img.size[0])
    return img.resize((img.size[0] * k, img.size[1] * k), Image.NEAREST)


def card(path, title, body=None, steps=None, mock=None, note=None, icon=False, qr=None, qr_text=None):
    """Cartão parado e centralizado: título, texto, passos, ilustração ou QR code."""
    im = Image.new("RGBA", (W, H * 2), RED + (0,))
    d = ImageDraw.Draw(im)
    y = 0
    if icon:
        ic = Image.open(ICON).convert("RGBA").resize((260, 260))
        im.paste(ic, ((W - 260) // 2, y), ic)
        y += 300
    y = text_block(d, title, font(BOLD, 68), y, gap=8) + 30
    if body:
        y = text_block(d, body, font(REG, 44), y, gap=10) + 40
    if steps:
        fb = font(BOLD, 44)
        for i, s in enumerate(steps):
            d.rounded_rectangle((100, y, W - 100, y + 100), radius=26, fill=WHITE)
            d.text((W / 2 - d.textlength(s, font=fb) / 2, y + 22), s, font=fb, fill=RED)
            y += 100
            if i < len(steps) - 1:
                cx = W / 2
                d.polygon([(cx - 26, y + 22), (cx + 26, y + 22), (cx, y + 52)], fill=WHITE)
                y += 70
        y += 40
    if mock:
        y = draw_mock(d, mock, y) + 30
    if note:
        y = text_block(d, note, font(REG, 34), y, fill=(255, 225, 227), gap=8)
    if qr:
        label = qr_text or SITE
        q = qr_image(qr, 440)
        pad = 30
        bx = (W - q.size[0]) // 2
        fs = font(BOLD, 36 if len(label) < 48 else 31)
        d.rounded_rectangle((60, y, W - 60, y + q.size[1] + 2 * pad + 70), radius=30, fill=WHITE)
        im.paste(q, (bx, y + pad))
        d.text((W / 2 - d.textlength(label, font=fs) / 2, y + q.size[1] + pad + 12), label, font=fs, fill=RED)
        y += q.size[1] + 2 * pad + 70
    out = Image.new("RGB", (W, H), RED)
    top = max(60, (H - 100 - y) // 2)
    out.paste(im, (0, top), im)
    footer(ImageDraw.Draw(out))
    out.save(path)


def draw_mock(d, mock, y):
    """Uma janela de aviso desenhada (ilustração), com o botão certo em destaque."""
    x0, x1 = 140, W - 140
    head, text, buttons = mock
    fh, ft, fbtn = font(BOLD, 40), font(REG, 34), font(BOLD, 36)
    lines = wrap(d, text, ft, x1 - x0 - 80)
    h = 90 + len(lines) * 46 + 40 + len(buttons) * 92 + 30
    d.rounded_rectangle((x0, y, x1, y + h), radius=36, fill=WHITE)
    d.text((x0 + 40, y + 36), head, font=fh, fill=DARK)
    ty = y + 100
    for line in lines:
        d.text((x0 + 40, ty), line, font=ft, fill=(70, 70, 75))
        ty += 46
    ty += 30
    for label, highlight in buttons:
        bw = d.textlength(label, font=fbtn) + 70
        bx = x1 - 40 - bw
        if highlight:
            d.rounded_rectangle((bx, ty, bx + bw, ty + 72), radius=36, fill=RED)
            d.text((bx + 35, ty + 12), label, font=fbtn, fill=WHITE)
        else:
            d.text((bx + 35, ty + 12), label, font=fbtn, fill=(110, 110, 115))
        ty += 92
    d.text((x0 + 40, y + h - 46), "ilustração", font=font(REG, 26), fill=(160, 160, 165))
    return y + h
