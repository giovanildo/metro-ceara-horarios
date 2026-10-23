"""Stories de QR code (1080x1920) e o qrcode.png de divulgacao/, apontando para a versão lite.

Uso: python3 tools/video/qr_stories.py
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

OUT = os.path.join(fr.ROOT, "divulgacao")
URL = "https://giovanildo.github.io/metro-fortaleza-horarios/app/"
LINHA1, LINHA2 = "giovanildo.github.io/", "metro-fortaleza-horarios/app"
PINK = (253, 236, 237)
GRAY = (95, 95, 102)
f = fr.font


def centro(d, texto, fonte, y, cor):
    d.text((fr.W / 2 - d.textlength(texto, font=fonte) / 2, y), texto, font=fonte, fill=cor)
    return y + fonte.size


def topo(im, y, icone=150):
    ic = Image.open(fr.ICON).convert("RGBA").resize((icone, icone))
    mask = Image.new("L", ic.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, icone - 1, icone - 1), radius=icone // 5, fill=255)
    im.paste(ic, ((fr.W - icone) // 2, y), mask)
    d = ImageDraw.Draw(im)
    y = centro(d, "Metrô Ceará", f(fr.BOLD, 76), y + icone + 20, fr.WHITE) + 22
    return d, y


def story_qr():
    """Só o QR, com o que o app faz: para o status do WhatsApp."""
    im = Image.new("RGB", (fr.W, fr.H), fr.RED)
    d, y = topo(im, 230, 200)
    y = centro(d, "Horários de metrô e VLT", f(fr.BOLD, 46), y, fr.WHITE) + 14
    y = centro(d, "Fortaleza · Cariri · Sobral", f(fr.REG, 40), y, fr.WHITE) + 40
    for item in ["Próxima partida da estação mais perto", "Todos os horários e a integração entre linhas",
                 "No Android: aviso antes de você descer"]:
        d.ellipse((120, y + 18, 136, y + 34), fill=fr.WHITE)
        d.text((160, y), item, font=f(fr.REG, 38), fill=fr.WHITE)
        y += 58
    y += 40
    q = fr.qr_image(URL, 520)
    box = (150, y, fr.W - 150, y + q.size[1] + 270)
    d.rounded_rectangle(box, radius=40, fill=fr.WHITE)
    im.paste(q, ((fr.W - q.size[0]) // 2, y + 40))
    yy = y + 40 + q.size[1] + 24
    yy = centro(d, "Aponte a câmera para abrir", f(fr.BOLD, 42), yy, fr.DARK) + 14
    yy = centro(d, "Abre na hora, sem instalar nada", f(fr.REG, 34), yy, GRAY) + 10
    centro(d, LINHA1 + LINHA2, f(fr.REG, 28), yy, GRAY)
    fr.footer(d)
    im.save(os.path.join(OUT, "story-qrcode.png"), optimize=True)


def story_qr_link():
    """QR + link escrito, com espaço livre embaixo para a figurinha de link do Instagram."""
    im = Image.new("RGB", (fr.W, fr.H), fr.RED)
    d, y = topo(im, 90)
    y = centro(d, "Horários de metrô e VLT no seu celular", f(fr.REG, 40), y, fr.WHITE) + 40
    q = fr.qr_image(URL, 480)
    box_top = y
    d.rounded_rectangle((90, box_top, fr.W - 90, box_top + q.size[1] + 430), radius=40, fill=fr.WHITE)
    im.paste(q, ((fr.W - q.size[0]) // 2, box_top + 40))
    yy = box_top + 40 + q.size[1] + 30
    yy = centro(d, "Aponte a câmera  ou  acesse o link:", f(fr.BOLD, 40), yy, fr.DARK) + 24
    d.rounded_rectangle((150, yy, fr.W - 150, yy + 150), radius=26, fill=PINK)
    centro(d, LINHA1, f(fr.BOLD, 46), yy + 18, fr.RED)
    centro(d, LINHA2, f(fr.BOLD, 46), yy + 78, fr.RED)
    yy += 150 + 22
    fr.text_block(d, "Abre na hora no navegador. No Android, dá para baixar o app completo.",
                  f(fr.REG, 30), yy, width=fr.W - 260, fill=GRAY, gap=4)
    yy = box_top + q.size[1] + 430 + 40
    centro(d, "Grátis · sem anúncios · qualquer celular", f(fr.BOLD, 40), yy, fr.WHITE)
    # De ~1550 a ~1820 fica livre para a figurinha "Link" do Instagram.
    fr.footer(d)
    im.save(os.path.join(OUT, "story-qrcode-link.png"), optimize=True)
    return yy


def qr_simples():
    fr.qr_image(URL, 390).save(os.path.join(OUT, "qrcode.png"), optimize=True)


story_qr()
fim = story_qr_link()
qr_simples()
print("texto do story-qrcode-link termina em y =", fim + 40)
