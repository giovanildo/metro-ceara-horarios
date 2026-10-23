"""Quadros (PNG 1080x1920) para os vídeos: moldura com legenda e cartões."""
import os
import subprocess

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
SITE = "giovanildo.github.io/metro-ceara-horarios"

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


def rounded_icon(size):
    """O ícone do app com os cantos arredondados, como na tela do celular: (imagem, máscara)."""
    ic = Image.open(ICON).convert("RGB").resize((size, size), Image.LANCZOS)
    mask = Image.new("L", ic.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 5, fill=255)
    return ic, mask


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
        ic, mask = rounded_icon(260)
        im.paste(ic, ((W - 260) // 2, y), mask)
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
        y = text_block(d, note, font(REG, 34), y, fill=(255, 225, 227), gap=8) + 30
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


# --- Trechos de gravação (screenrecord) dentro da moldura ---

def marks(path):
    """As marcas de tools/video/gravar_viagem.sh: {cena: segundos desde o início}."""
    with open(path) as fh:
        return {n: float(t) for n, t in (line.split() for line in fh if line.strip())}


def to_cfr(ff, src, dst):
    """O screenrecord só grava quadro quando a tela muda: numa tela parada não há
    quadros, e o corte pularia para o próximo. Em 30 quadros/s constantes, cada
    trecho começa onde deve."""
    subprocess.run([ff, "-v", "error", "-y", "-i", src, "-vf", "fps=30", "-c:v", "libx264", "-crf", "16",
                    "-pix_fmt", "yuv420p", dst], check=True)


def clip(ff, src, png, mp4, parts, speed=1.0):
    """Os trechos (início, fim) de src, emendados e acelerados, no buraco da moldura png."""
    inputs, chains = [], []
    for k, (a, b) in enumerate(parts):
        inputs += ["-ss", f"{a:.2f}", "-to", f"{b:.2f}", "-i", src]
        chains.append(f"[{k}:v]scale={PH_W}:{PH_H},setsar=1,setpts=(PTS-STARTPTS)/{speed},fps=30[p{k}]")
    n = len(parts)
    total = sum(b - a for a, b in parts) / speed
    inputs += ["-loop", "1", "-t", f"{total:.3f}", "-i", png,
               "-f", "lavfi", "-t", f"{total:.3f}", "-i", "anullsrc=r=44100:cl=stereo"]
    fc = ";".join(chains) + ";" + "".join(f"[p{k}]" for k in range(n)) + f"concat=n={n}:v=1:a=0[ph];" + \
        f"color=c=0x1c1c1e:s={W}x{H}:r=30:d={total:.3f}[bg];[bg][ph]overlay={PH_X}:{PH_Y}[v1];" + \
        f"[v1][{n}:v]overlay=0:0,format=yuv420p[v]"
    subprocess.run([ff, "-v", "error", "-y"] + inputs + [
        "-filter_complex", fc, "-map", "[v]", "-map", f"{n + 1}:a", "-c:v", "libx264", "-crf", "21",
        "-pix_fmt", "yuv420p", "-r", "30", "-c:a", "aac", "-ar", "44100", "-ac", "2", "-shortest", mp4], check=True)


def still(ff, png, secs, mp4):
    subprocess.run([ff, "-v", "error", "-y", "-loop", "1", "-t", str(secs), "-i", png, "-f", "lavfi", "-t", str(secs),
                    "-i", "anullsrc=r=44100:cl=stereo", "-vf", "format=yuv420p", "-c:v", "libx264", "-crf", "20",
                    "-r", "30", "-c:a", "aac", "-ar", "44100", "-ac", "2", "-shortest", mp4], check=True)


def concat(ff, clips, listfile, final):
    with open(listfile, "w") as fh:
        fh.writelines(f"file '{c}'\n" for c in clips)
    subprocess.run([ff, "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", listfile, "-c", "copy",
                    "-movflags", "+faststart", final], check=True)
