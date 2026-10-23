"""Ícone do Metrô Ceará: o trem sobre a toalhinha de renda de bilro, com a
jandaia-verdadeira pousada nele e as três faixas das linhas embaixo.

Uma figura só, descrita aqui, vira:
  - tools/icone/icone.svg (fonte para quem quiser editar);
  - app/src/main/res/drawable/ic_launcher.xml (Android 7: a figura inteira);
  - app/src/main/res/drawable-anydpi-v26/ic_launcher.xml + ic_launcher_fundo.xml
    e ic_launcher_frente.xml (ícone adaptável do Android 8+: preenche o círculo,
    o squircle ou o quadrado de cada aparelho, com o desenho na zona segura);
  - os PNGs do site, da versão lite e da loja (renderizados pelo Google Chrome).

Uso: python3 tools/icone/icone.py
"""
import math
import os
import subprocess
import sys
import tempfile

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")

AREIA = "#F3E6D3"
RENDA = "#FFFFFF"
BORDA = "#E3D2B8"
TREM = "#17202B"
FAIXAS = [(82, 8, "#E3262E"), (90, 8, "#8CC63F"), (98, 10, "#5B4B9F")]
TREM_D = ("M38,24h32a10,10 0,0 1,10 10v38a10,10 0,0 1,-10 10h-32a10,10 0,0 1,-10 -10v-38"
          "a10,10 0,0 1,10 -10z M36,36h36v18h-36z M34,64a4,4 0,1 0,8 0a4,4 0,1 0,-8 0z "
          "M66,64a4,4 0,1 0,8 0a4,4 0,1 0,-8 0z")

# Elementos: ("p", d, fill, extras) ou ("g", transform, [filhos]).
# transform = dict(tx, ty, rot, px, py, s): o mesmo que o <group> do Android.


def circulo(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r},{r} 0,1 0,{2 * r} 0a{r},{r} 0,1 0,{-2 * r} 0z"


def elipse(rx, ry):
    return f"M{-rx},0a{rx},{ry} 0,1 0,{2 * rx} 0a{rx},{ry} 0,1 0,{-2 * rx} 0z"


def p(d, fill, **extras):
    return ("p", d, fill, extras)


def g(filhos, **t):
    return ("g", t, filhos)


def toalhinha(cx=52, cy=50, raio=42, n=18, onda=6.6):
    """A renda: borda em ondas, uma volta de furinhos e uma de pétalas."""
    pts = [(cx + raio * math.cos(2 * math.pi * i / n), cy + raio * math.sin(2 * math.pi * i / n)) for i in range(n)]
    d = f"M{pts[0][0]:.2f},{pts[0][1]:.2f}"
    for x, y in pts[1:] + pts[:1]:
        d += f"A{onda},{onda} 0,0 1,{x:.2f},{y:.2f}"
    els = [p(d + "z", RENDA, stroke=BORDA, width=1)]
    for i in range(n):
        a = 2 * math.pi * (i + .5) / n
        els.append(p(circulo(cx + 36 * math.cos(a), cy + 36 * math.sin(a), 2), AREIA))
    for i in range(12):
        a = 360 * i / 12
        x, y = cx + 27 * math.cos(math.radians(a)), cy + 27 * math.sin(math.radians(a))
        els.append(g([p(elipse(4.2, 2), AREIA)], tx=x, ty=y, rot=a))
    return els


def trem():
    return g([p(TREM_D, TREM, evenodd=True)], tx=-3, ty=-4, s=0.86)


def jandaia():
    return g([
        p("M2,18L-4,44L6,22Z", "#1F8A4C"),
        p("M6,20L4,46L11,21Z", "#2FA35A"),
        g([p(elipse(11, 14), "#F26A1B")], tx=2, ty=10),
        g([g([p(elipse(7, 12), "#2FA35A")], tx=8, ty=12)], rot=-12, px=8, py=12),
        p(circulo(-2, -6, 9), "#FFC31F"),
        p("M-11,-7q-6,1 -5,7q3,-3 6,-2z", "#2B2B2B"),
        p(circulo(-4, -8, 2.6), "#FFFFFF"),
        p(circulo(-4.4, -8, 1.5), "#17202B"),
        p("M-2,24l-3,5M4,24l1,5", None, stroke="#6B6B6B", width=2, cap="round"),
    ], tx=76, ty=26, rot=12, s=0.9)


def faixas(y0=0.0, k=1.0):
    return [p(f"M0,{y0 + y * k:.2f}h108v{h * k:.2f}h-108z", cor) for y, h, cor in FAIXAS]


def figura():
    """A figura inteira em 108×108, de borda a borda (PNGs, SVG e Android 7)."""
    return [p("M0,0h108v108h-108z", AREIA), *toalhinha(), *faixas(), trem(), jandaia()]


# Ícone adaptável: o aparelho mostra só o miolo de 72 de 108 (e corta mais no
# círculo). O desenho encolhe para caber; o fundo areia e as faixas vão até a borda.
K, OFF = 0.7, 16.2


def frente():
    def enc(els):
        return g(els, tx=OFF, ty=OFF, s=K)
    y = OFF + FAIXAS[0][0] * K
    return [enc(toalhinha()), *[p(f"M0,{y0:.2f}h108v{h:.2f}h-108z", cor) for y0, h, cor in
                                 [(y, 8 * K, FAIXAS[0][2]), (y + 8 * K, 8 * K, FAIXAS[1][2]), (y + 16 * K, 108, FAIXAS[2][2])]],
            enc([trem(), jandaia()])]


# --- saída SVG ---

def svg_el(e, ind="  "):
    if e[0] == "p":
        _, d, fill, x = e
        a = f'd="{d}" fill="{fill or "none"}"'
        if x.get("evenodd"):
            a += ' fill-rule="evenodd"'
        if x.get("stroke"):
            a += f' stroke="{x["stroke"]}" stroke-width="{x["width"]}"'
        if x.get("cap"):
            a += f' stroke-linecap="{x["cap"]}"'
        return f"{ind}<path {a}/>\n"
    _, t, filhos = e
    tr = ""
    if t.get("tx") or t.get("ty"):
        tr += f'translate({t.get("tx", 0):.2f} {t.get("ty", 0):.2f}) '
    if t.get("rot"):
        tr += f'rotate({t["rot"]:.2f} {t.get("px", 0)} {t.get("py", 0)}) '
    if t.get("s"):
        tr += f'translate({t.get("px", 0)} {t.get("py", 0)}) scale({t["s"]}) translate({-t.get("px", 0)} {-t.get("py", 0)})'
    return f'{ind}<g transform="{tr.strip()}">\n' + "".join(svg_el(f, ind + "  ") for f in filhos) + f"{ind}</g>\n"


def svg(els):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">\n'
            "  <!-- Gerado por tools/icone/icone.py: edite lá. -->\n"
            + "".join(svg_el(e) for e in els) + "</svg>\n")


# --- saída vetor do Android ---

def vd_el(e, ind="    "):
    if e[0] == "p":
        _, d, fill, x = e
        a = [f'android:pathData="{d}"']
        if fill:
            a.insert(0, f'android:fillColor="{fill}"')
        if x.get("evenodd"):
            a.append('android:fillType="evenOdd"')
        if x.get("stroke"):
            a += [f'android:strokeColor="{x["stroke"]}"', f'android:strokeWidth="{x["width"]}"']
        if x.get("cap"):
            a.append(f'android:strokeLineCap="{x["cap"]}"')
        return f"{ind}<path\n" + "".join(f"{ind}    {s}\n" for s in a[:-1]) + f"{ind}    {a[-1]} />\n"
    _, t, filhos = e
    a = []
    for k, n in (("tx", "translateX"), ("ty", "translateY"), ("rot", "rotation"), ("px", "pivotX"), ("py", "pivotY")):
        if t.get(k):
            a.append(f'android:{n}="{t[k]:.2f}"')
    if t.get("s"):
        a += [f'android:scaleX="{t["s"]}"', f'android:scaleY="{t["s"]}"']
    head = f"{ind}<group" + "".join(f"\n{ind}    {s}" for s in a) + ">\n"
    return head + "".join(vd_el(f, ind + "    ") for f in filhos) + f"{ind}</group>\n"


def vector(els, nota):
    return (f"<!-- {nota} Gerado por tools/icone/icone.py: edite lá. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            + "".join(vd_el(e) for e in els) + "</vector>\n")


ADAPTAVEL = """<!-- Ícone adaptável (Android 8+). Gerado por tools/icone/icone.py. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_fundo" />
    <foreground android:drawable="@drawable/ic_launcher_frente" />
</adaptive-icon>
"""


def escrever(caminho, texto):
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    with open(caminho, "w") as f:
        f.write(texto)
    print(os.path.relpath(caminho, ROOT))


def png(svg_path, lado, destino, modo="RGB"):
    with tempfile.TemporaryDirectory() as t:
        html = os.path.join(t, "p.html")
        with open(html, "w") as f:
            f.write(f'<html><body style="margin:0"><img src="file://{svg_path}" width="{lado}" height="{lado}" '
                    'style="display:block"></body></html>')
        out = os.path.join(t, "o.png")
        subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--no-sandbox", "--hide-scrollbars",
                        "--force-device-scale-factor=1", f"--window-size={lado},{lado}", f"--screenshot={out}",
                        f"file://{html}"], check=True, capture_output=True)
        Image.open(out).convert(modo).save(os.path.join(ROOT, destino), optimize=True)
    print(destino)


def main():
    svg_path = os.path.join(ROOT, "tools", "icone", "icone.svg")
    escrever(svg_path, svg(figura()))
    escrever(os.path.join(RES, "drawable", "ic_launcher.xml"),
             vector(figura(), "Ícone do app no Android 7 (a figura inteira)."))
    escrever(os.path.join(RES, "drawable", "ic_launcher_fundo.xml"),
             vector([p("M0,0h108v108h-108z", AREIA)], "Fundo do ícone adaptável."))
    escrever(os.path.join(RES, "drawable", "ic_launcher_frente.xml"),
             vector(frente(), "Frente do ícone adaptável: o desenho encolhido para a zona segura."))
    escrever(os.path.join(RES, "drawable-anydpi-v26", "ic_launcher.xml"), ADAPTAVEL)
    if "--sem-png" in sys.argv:
        return
    for lado, destino, modo in [(512, "docs/img/icon.png", "RGB"), (512, "docs/app/icon-512.png", "RGB"),
                                (192, "docs/app/icon-192.png", "RGB"), (180, "docs/app/icon-180.png", "RGB"),
                                (512, "tools/play/imagens/icone-512.png", "RGBA")]:
        png(svg_path, lado, destino, modo)


if __name__ == "__main__":
    main()
