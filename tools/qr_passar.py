"""QR do "Passar o app para alguém" (Mais do app e da lite).

Aponta para a página /play/, que no Android abre a Play Store e no iPhone a
versão lite. A origem vai em ?de= e vira utm_source no referrer da loja.
Uso: python3 tools/qr_passar.py
"""
import os

import qrcode
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "https://giovanildo.github.io/metro-ceara-horarios/play/?de="


def qr(url, size):
    q = qrcode.QRCode(border=2, box_size=1, error_correction=qrcode.constants.ERROR_CORRECT_M)
    q.add_data(url)
    q.make(fit=True)
    img = q.make_image(fill_color="black", back_color="white").convert("1")
    k = max(1, size // img.size[0])
    return img.resize((img.size[0] * k, img.size[1] * k), Image.NEAREST)


qr(BASE + "qr-app", 481).save(os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi", "qr_passar.png"), optimize=True)
qr(BASE + "qr-lite", 400).save(os.path.join(ROOT, "docs", "app", "qr-passar.png"), optimize=True)
