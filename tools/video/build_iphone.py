"""Vídeo "Como instalar no iPhone": telas reais da versão web + partes do iPhone ilustradas."""
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

# Pasta de trabalho: precisa de ios/raw_inicio.png e ios/raw_estacoes.png
# (capturas da versão web no Chrome do emulador, 1080x2400; veja o README).
S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = S + "/ios/out"
os.makedirs(OUT, exist_ok=True)
URL = "giovanildo.github.io/metro-fortaleza-horarios/app"
ICON = os.path.join(fr.ROOT, "docs", "app", "icon-180.png")
PAGE_BG = (251, 248, 253)
BLUE = (10, 122, 255)
SW, SH = 1080, 2338  # tela do "iPhone"

f = fr.font
B, R = fr.BOLD, fr.REG


def page(name):
    """Conteúdo real da página (sem a barra do Chrome do Android)."""
    return Image.open(f"{S}/ios/raw_{name}.png").convert("RGB").crop((0, 280, 1080, 2330))


def status_bar(d, dark=False):
    c = (255, 255, 255) if dark else (0, 0, 0)
    d.text((95, 42), "9:41", font=f(B, 46), fill=c)
    d.rounded_rectangle((SW / 2 - 185, 28, SW / 2 + 185, 128), radius=50, fill=(0, 0, 0))
    for i in range(4):  # sinal
        h = 14 + i * 8
        d.rounded_rectangle((800 + i * 18, 92 - h, 812 + i * 18, 92), radius=3, fill=c)
    d.rounded_rectangle((900, 58, 966, 92), radius=9, outline=c, width=4)  # bateria
    d.rounded_rectangle((907, 65, 950, 85), radius=4, fill=c)


def screen_safari(content):
    im = Image.new("RGB", (SW, SH), PAGE_BG)
    im.paste(content.crop((0, 0, 1080, 1860)), (0, 140))
    d = ImageDraw.Draw(im)
    status_bar(d)
    d.rectangle((0, 2000, SW, SH), fill=(246, 246, 248))
    d.rounded_rectangle((40, 2018, SW - 40, 2108), radius=26, fill=(228, 228, 233))
    t = "giovanildo.github.io"
    d.text((SW / 2 - d.textlength(t, font=f(R, 40)) / 2, 2038), t, font=f(R, 40), fill=(20, 20, 20))
    # Barra de ferramentas: voltar, avançar, compartilhar, favoritos, abas.
    y = 2180
    d.text((95, y - 40), "‹", font=f(R, 90), fill=BLUE)
    d.text((290, y - 40), "›", font=f(R, 90), fill=(180, 180, 185))
    share_icon(d, SW / 2, y + 8, BLUE)
    d.rounded_rectangle((740, y - 22, 800, y + 30), radius=6, outline=BLUE, width=5)
    d.rounded_rectangle((905, y - 26, 955, y + 22), radius=8, outline=BLUE, width=5)
    d.rounded_rectangle((920, y - 12, 970, y + 36), radius=8, outline=BLUE, width=5)
    home_indicator(d)
    return im


def share_icon(d, cx, cy, c, s=1.0):
    w = 30 * s
    d.line((cx - w, cy - 6 * s, cx - w, cy + 40 * s, cx + w, cy + 40 * s, cx + w, cy - 6 * s), fill=c, width=int(5 * s), joint="curve")
    d.line((cx, cy - 46 * s, cx, cy + 16 * s), fill=c, width=int(5 * s))
    d.line((cx - 18 * s, cy - 28 * s, cx, cy - 46 * s, cx + 18 * s, cy - 28 * s), fill=c, width=int(5 * s), joint="curve")


def home_indicator(d, c=(0, 0, 0)):
    d.rounded_rectangle((SW / 2 - 180, SH - 34, SW / 2 + 180, SH - 22), radius=6, fill=c)


def ring(d, box, c=fr.RED):
    x0, y0, x1, y1 = box
    for k in range(3):
        d.rounded_rectangle((x0 - 14 - k * 4, y0 - 14 - k * 4, x1 + 14 + k * 4, y1 + 14 + k * 4), radius=40, outline=c, width=4)


def dim(im, a=110):
    layer = Image.new("RGBA", im.size, (0, 0, 0, a))
    return Image.alpha_composite(im.convert("RGBA"), layer)


def screen_share_sheet(base):
    im = dim(base.convert("RGBA"))
    d = ImageDraw.Draw(im)
    top = 760
    d.rounded_rectangle((0, top, SW, SH + 60), radius=48, fill=(242, 242, 247))
    ic = Image.open(ICON).convert("RGBA").resize((130, 130))
    im.paste(ic, (60, top + 60), ic)
    d = ImageDraw.Draw(im)
    d.text((220, top + 70), "Metrô Fortaleza", font=f(B, 44), fill=(0, 0, 0))
    d.text((220, top + 128), "giovanildo.github.io", font=f(R, 36), fill=(120, 120, 128))
    for i in range(5):  # contatos/apps (genéricos)
        cx = 120 + i * 210
        d.ellipse((cx - 75, top + 260, cx + 75, top + 410), fill=(205, 205, 212))
    items = ["Copiar", "Adicionar à Lista de Leitura", "Adicionar aos Favoritos", "Adicionar à Tela de Início", "Buscar na Página"]
    y = top + 480
    d.rounded_rectangle((40, y, SW - 40, y + 5 * 130), radius=30, fill=(255, 255, 255))
    for i, t in enumerate(items):
        yy = y + i * 130
        if i:
            d.line((80, yy, SW - 40, yy), fill=(225, 225, 230), width=2)
        d.text((80, yy + 38), t, font=f(R, 44), fill=(0, 0, 0))
        if t == "Adicionar à Tela de Início":
            d.rounded_rectangle((SW - 150, yy + 36, SW - 90, yy + 96), radius=12, outline=(0, 0, 0), width=5)
            d.line((SW - 120, yy + 50, SW - 120, yy + 82), fill=(0, 0, 0), width=5)
            d.line((SW - 136, yy + 66, SW - 104, yy + 66), fill=(0, 0, 0), width=5)
            ring(d, (50, yy + 6, SW - 50, yy + 124))
    home_indicator(d)
    return im.convert("RGB")


def screen_add():
    im = Image.new("RGB", (SW, SH), (242, 242, 247))
    d = ImageDraw.Draw(im)
    status_bar(d)
    d.text((60, 210), "Cancelar", font=f(R, 46), fill=BLUE)
    t = "Adicionar à Tela de Início"
    d.text((SW / 2 - d.textlength(t, font=f(B, 34)) / 2, 220), t, font=f(B, 34), fill=(0, 0, 0))
    d.text((SW - 260, 210), "Adicionar", font=f(B, 46), fill=BLUE)
    ring(d, (SW - 268, 205, SW - 50, 270))
    d.rounded_rectangle((40, 360, SW - 40, 640), radius=30, fill=(255, 255, 255))
    ic = Image.open(ICON).convert("RGB").resize((180, 180))
    im.paste(ic, (80, 410))
    d.text((300, 430), "Metrô Fortaleza", font=f(R, 50), fill=(0, 0, 0))
    d.line((300, 510, SW - 80, 510), fill=(225, 225, 230), width=2)
    d.text((300, 535), "giovanildo.github.io", font=f(R, 38), fill=(120, 120, 128))
    d.rounded_rectangle((40, 700, SW - 40, 830), radius=30, fill=(255, 255, 255))
    d.text((80, 740), "Abrir como app web", font=f(R, 46), fill=(0, 0, 0))
    d.rounded_rectangle((SW - 230, 730, SW - 80, 800), radius=35, fill=(52, 199, 89))
    d.ellipse((SW - 152, 734, SW - 86, 796), fill=(255, 255, 255))
    home_indicator(d)
    return im


def screen_home():
    im = Image.new("RGB", (SW, SH))
    top, bottom = (40, 90, 160), (210, 90, 120)
    for y in range(SH):  # papel de parede (degradê)
        t = y / SH
        ImageDraw.Draw(im).line((0, y, SW, y), fill=tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3)))
    d = ImageDraw.Draw(im)
    status_bar(d, dark=True)
    size, gap_x, x0, y0 = 190, 262, 60, 260
    for row in range(5):
        for col in range(4):
            x, y = x0 + col * gap_x, y0 + row * 300
            if (row, col) == (3, 1):
                ic = Image.open(ICON).convert("RGBA").resize((size, size))
                mask = Image.new("L", (size, size), 0)
                ImageDraw.Draw(mask).rounded_rectangle((0, 0, size, size), radius=44, fill=255)
                im.paste(ic, (x, y), mask)
                label = "Metrô Fortaleza"
                d.text((x + size / 2 - d.textlength(label, font=f(R, 30)) / 2, y + size + 12), label, font=f(R, 30), fill=(255, 255, 255))
                ring(d, (x - 6, y - 6, x + size + 6, y + size + 56), c=(255, 255, 255))
            else:
                d.rounded_rectangle((x, y, x + size, y + size), radius=44, fill=(255, 255, 255, 90) if False else (235, 235, 240))
                d.rounded_rectangle((x + 40, y + size + 22, x + size - 40, y + size + 36), radius=6, fill=(230, 230, 236))
    d.rounded_rectangle((40, SH - 330, SW - 40, SH - 80), radius=70, fill=(255, 255, 255))
    for col in range(4):
        x = 95 + col * 245
        d.rounded_rectangle((x, SH - 290, x + size, SH - 290 + size), radius=44, fill=(225, 225, 232))
    home_indicator(d, (255, 255, 255))
    return im


def screen_app(name, alert=False):
    im = Image.new("RGB", (SW, SH), PAGE_BG)
    im.paste(page(name), (0, 140))
    d = ImageDraw.Draw(im)
    status_bar(d)
    home_indicator(d)
    if alert:
        im = dim(im, 120)
        d = ImageDraw.Draw(im)
        x0, x1, y0 = 140, SW - 140, 820
        d.rounded_rectangle((x0, y0, x1, y0 + 720), radius=40, fill=(242, 242, 247))
        title = "Permitir que “Metrô Fortaleza” use sua localização?"
        y = fr.text_block(d, title, f(B, 42), y0 + 50, width=x1 - x0 - 80, fill=(0, 0, 0), gap=6)
        y = fr.text_block(d, "Para mostrar a estação mais perto de você.", f(R, 34), y + 10, width=x1 - x0 - 80, fill=(60, 60, 67), gap=4)
        for i, t in enumerate(["Permitir uma vez", "Permitir ao usar o app", "Não permitir"]):
            yy = y0 + 720 - (3 - i) * 120
            d.line((x0, yy, x1, yy), fill=(210, 210, 216), width=2)
            d.text((SW / 2 - d.textlength(t, font=f(R, 44)) / 2, yy + 34), t, font=f(R, 44), fill=BLUE)
            if i == 1:
                ring(d, (x0 + 30, yy + 14, x1 - 30, yy + 106))
        im = im.convert("RGB")
    return im


def phone_scene(path, screen, title, subtitle=None, step=None, illustr=True):
    """Moldura vermelha com legenda + o "iPhone" desenhado no meio."""
    frame = f"{OUT}/_frame.png"
    fr.phone_frame(frame, title, subtitle, step)
    bg = Image.open(frame).convert("RGBA")
    out = Image.new("RGBA", bg.size, fr.DARK + (255,))
    shot = screen.convert("RGB").resize((fr.PH_W, fr.PH_H), Image.LANCZOS)
    out.paste(shot, (fr.PH_X, fr.PH_Y))
    out = Image.alpha_composite(out, bg)
    if illustr:
        d = ImageDraw.Draw(out)
        t = "ilustração"
        ft = f(R, 26)
        w = d.textlength(t, font=ft)
        x, y = fr.PH_X + 34, fr.PH_Y + fr.PH_H - 42
        d.rounded_rectangle((x - 10, y - 4, x + w + 10, y + 30), radius=12, fill=(0, 0, 0, 150))
        d.text((x, y - 3), t, font=f(R, 24), fill=(255, 255, 255))
    out.convert("RGB").save(path)


# Mesma moldura dos outros vídeos, com a tela no formato do iPhone.
fr.PH_H = 1420
fr.PH_W = round(fr.PH_H * SW / SH / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

safari = screen_safari(page("inicio"))
safari_share = safari.copy()
ring(ImageDraw.Draw(safari_share), (SW / 2 - 50, 2120, SW / 2 + 50, 2230))
safari_url = safari.copy()
ring(ImageDraw.Draw(safari_url), (40, 2018, SW - 40, 2108))

SCENES = [
    ("01", 4.0, safari_url, "Abra o Safari e entre no endereço", URL, 1),
    ("02", 4.0, safari_share, "Toque em Compartilhar", "Não achou? Toque em ••• e depois em Compartilhar", 2),
    ("03", 4.0, screen_share_sheet(safari), "Toque em Adicionar à Tela de Início", "Role a lista, se precisar", 3),
    ("04", 4.0, screen_add(), "Confira o nome e toque em Adicionar", "Se aparecer “Abrir como app web”, deixe ligado", 4),
    ("05", 3.5, screen_home(), "Pronto! Abra pelo ícone", None, 5),
    ("06", 4.0, screen_app("inicio", alert=True), "Toque em Usar minha localização e permita", "A localização fica só no seu celular", 6),
    ("07", 4.0, screen_app("estacoes"), "A estação mais perto e o próximo metrô", None, None),
]
CARDS_BEFORE = [("00", 2.4, dict(title="Como instalar no iPhone", body="Grátis, sem App Store e sem conta", icon=True))]
CARDS_AFTER = [
    ("08", 5.0, dict(title="Bom saber",
                     body="Os horários se atualizam sozinhos todo dia. Sem internet, mostra os últimos que baixou.\n\nO modo viagem, com aviso de descida com a tela desligada, existe só no app de Android.")),
    ("09", 4.5, dict(title="Abra no Safari", body="Aponte a câmera para o código", qr="https://" + URL, qr_text=URL)),
]


def run(args):
    subprocess.run([FF, "-v", "error", "-y"] + args, check=True)


def still(png, secs, mp4):
    run(["-loop", "1", "-t", str(secs), "-i", png, "-f", "lavfi", "-t", str(secs), "-i", "anullsrc=r=44100:cl=stereo",
         "-vf", "format=yuv420p", "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-r", "30",
         "-c:a", "aac", "-ar", "44100", "-ac", "2", "-shortest", mp4])


clips = []
for name, secs, spec in CARDS_BEFORE:
    png = f"{OUT}/{name}.png"
    fr.card(png, **spec)
    still(png, secs, f"{OUT}/{name}.mp4")
    clips.append(f"{OUT}/{name}.mp4")
for name, secs, screen, title, sub, step in SCENES:
    png = f"{OUT}/{name}.png"
    phone_scene(png, screen, title, sub, step, illustr=name not in ("07",))
    still(png, secs, f"{OUT}/{name}.mp4")
    clips.append(f"{OUT}/{name}.mp4")
for name, secs, spec in CARDS_AFTER:
    png = f"{OUT}/{name}.png"
    fr.card(png, **spec)
    still(png, secs, f"{OUT}/{name}.mp4")
    clips.append(f"{OUT}/{name}.mp4")
with open(f"{OUT}/list.txt", "w") as fh:
    fh.writelines(f"file '{c}'\n" for c in clips)
final = S + "/ios/metro-fortaleza-como-instalar-iphone.mp4"
run(["-f", "concat", "-safe", "0", "-i", f"{OUT}/list.txt", "-c", "copy", "-movflags", "+faststart", final])
print(final)
