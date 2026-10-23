"""Imagens com o nome e o ícone: o gráfico de recursos da Play Store (1024x500,
ícone, nome e o app num celular) e a prévia de link do site (docs/img/previa.png,
1200x630, a que aparece ao mandar o link no WhatsApp).

Uso: python3 tools/play/destaque.py  (depois de tools/icone/icone.py e das capturas em imagens/)
"""
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "video"))
import frames as fr  # noqa: E402

IMG = os.path.join(fr.ROOT, "tools", "play", "imagens")
W, H = 1024, 500


def arredondar(im, raio):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), radius=raio, fill=255)
    return mask


def main():
    im = Image.new("RGB", (W, H), fr.RED)
    d = ImageDraw.Draw(im)

    # Celular à direita, cortado embaixo, com a tela inicial.
    tela = Image.open(os.path.join(IMG, "tela-1.png")).convert("RGB")
    sw = 300
    tela = tela.resize((sw, round(tela.size[1] * sw / tela.size[0])))
    x, y, b = 650, 46, 12
    d.rounded_rectangle((x - b, y - b, x + sw + b, H + 60), radius=48, fill=(28, 28, 30))
    tela = tela.crop((0, 0, sw, H - y))
    im.paste(tela, (x, y), arredondar(tela, 36))

    ic = Image.open(fr.ICON).convert("RGB").resize((112, 112))
    im.paste(ic, (60, 70), arredondar(ic, 26))
    d.text((60, 206), "Metrô Ceará", font=fr.font(fr.BOLD, 64), fill=fr.WHITE)
    d.text((62, 290), "Fortaleza · Cariri · Sobral", font=fr.font(fr.BOLD, 32), fill=fr.WHITE)
    reg = fr.font(fr.REG, 28)
    d.text((62, 342), "Horários do metrô e VLT da estação", font=reg, fill=fr.WHITE)
    d.text((62, 378), "mais perto e aviso antes de descer", font=reg, fill=fr.WHITE)
    d.text((62, 430), "Grátis · sem anúncios · não oficial", font=fr.font(fr.BOLD, 24), fill=(255, 220, 222))

    destino = os.path.join(IMG, "destaque-1024x500.png")
    im.save(destino, optimize=True)
    print(os.path.relpath(destino, fr.ROOT))
    previa()


def previa():
    im = Image.new("RGB", (1200, 630), fr.RED)
    d = ImageDraw.Draw(im)
    ic = Image.open(fr.ICON).convert("RGB").resize((240, 240))
    im.paste(ic, (80, 195), arredondar(ic, 54))
    d.text((372, 180), "Metrô Ceará", font=fr.font(fr.BOLD, 92), fill=fr.WHITE)
    d.text((375, 300), "Horários de metrô e VLT no celular", font=fr.font(fr.REG, 40), fill=fr.WHITE)
    d.text((375, 356), "Fortaleza · Cariri · Sobral", font=fr.font(fr.BOLD, 34), fill=fr.WHITE)
    f = fr.font(fr.BOLD, 26)
    t = "grátis · sem anúncios · não oficial"
    w = d.textlength(t, font=f)
    d.rounded_rectangle((372, 420, 372 + w + 52, 478), radius=29, fill=fr.WHITE)
    d.text((398, 431), t, font=f, fill=fr.RED)
    destino = os.path.join(fr.ROOT, "docs", "img", "previa.png")
    im.save(destino, optimize=True)
    print(os.path.relpath(destino, fr.ROOT))


if __name__ == "__main__":
    main()
