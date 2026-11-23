"""Explicativo "da Play Store ao próximo metrô" (divulgacao/explicativo-instalar/).

Tom profissional e direto: fundo claro, um passo por legenda, telas reais. O
destaque é a busca digitada: "metrô ceará" e depois "metrô fortaleza", letra
por letra no campo do Google Play e, em grande, numa barra acima do celular.

Entrada: $VIDEO_WORK/explicativo/ de gravar_explicativo.sh. Saída: story
1080x1920 e feed 1080x1350, sem som.

Nomes, ícones, imagens e sugestões de outros apps ficam borrados (sugestões
enquanto digita, resultados das buscas). Os cortes partem das marcas e dos
toques anotados; se gravar de novo, confira os quadros.

    VIDEO_WORK=/tmp/metro-video python3 tools/video/explicativo_instalar.py
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.path.join(os.environ.get("VIDEO_WORK", "/tmp/metro-video"), "explicativo")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = os.path.join(fr.ROOT, "divulgacao", "explicativo-instalar")
os.makedirs(OUT, exist_ok=True)
PLAY = "https://play.google.com/store/apps/details?id=io.github.giova.metroceara&referrer=utm_source%3Dexplicativo"

BG = (244, 246, 250)
INK = (24, 32, 56)
AZUL = (47, 69, 120)
SUB = (88, 98, 122)
AMARELO = (255, 196, 0)
B, R = fr.BOLD, fr.REG
SRC_W, SRC_H = 1080, 2400
CONTEUDO = (0, 420, 1080, 2330)      # abaixo da barra de busca do Play
SUGESTOES = (0, 400, 1080, 1290)     # a lista de sugestões, acima do teclado
CARTAO = (0, 730, 960, 1485)         # o cartão do Metrô Ceará no carrossel (achou.png)


def ler(nome):
    with open(os.path.join(S, nome)) as fh:
        return [line.split(maxsplit=1) for line in fh if line.strip()]


def toques(nome):
    return [tuple(map(float, linha[0:1] + linha[1].split())) for linha in ler(f"toques-{nome}.txt")]


MARCAS = {n.strip(): float(t) for t, n in ler("marcas-loja.txt")}
DIG = {k: [(float(t), txt.rstrip("\n")) for t, txt in ler(f"digitacao-{k}.txt")] for k in (1, 2)}


def duracao(p):
    return float(subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v", "-show_entries",
                                 "stream=duration", "-of", "csv=p=0", p],
                                capture_output=True, text=True, check=True).stdout.strip())


def anel_png(path):
    k = 4
    im = Image.new("RGBA", (180 * k, 180 * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse((10 * k, 10 * k, 170 * k, 170 * k), outline=AMARELO + (255,), width=12 * k)
    d.ellipse((62 * k, 62 * k, 118 * k, 118 * k), fill=(255, 255, 255, 170), outline=AMARELO + (255,), width=4 * k)
    im.resize((180, 180), Image.LANCZOS).save(path)


def preparar(nome, borroes):
    """A gravação em 30 quadros/s, com os borrões [(x1, y1, x2, y2, t0, t1)] e o
    anel em cada toque."""
    cfr = os.path.join(S, f"{nome}-cfr.mp4")
    fr.to_cfr(FF, os.path.join(S, f"{nome}.mp4"), cfr)
    anel = os.path.join(S, "anel.png")
    anel_png(anel)
    filtros, ult = [], "0:v"
    for i, (x1, y1, x2, y2, t0, t1) in enumerate(borroes):
        filtros.append(f"[{ult}]split[a{i}][b{i}];[b{i}]crop={x2 - x1}:{y2 - y1}:{x1}:{y1},boxblur=30:4[bl{i}];"
                       f"[a{i}][bl{i}]overlay={x1}:{y1}:enable='between(t,{t0:.2f},{t1:.2f})'[w{i}]")
        ult = f"w{i}"
    for i, (t, x, y) in enumerate(toques(nome)):
        filtros.append(f"[{ult}][1:v]overlay={int(x) - 90}:{int(y) - 90}:"
                       f"enable='between(t,{t - 0.45:.2f},{t + 0.35:.2f})'[v{i}]")
        ult = f"v{i}"
    out = os.path.join(S, f"{nome}-pronto.mp4")
    subprocess.run([FF, "-v", "error", "-y", "-i", cfr, "-loop", "1", "-i", anel, "-filter_complex",
                    ";".join(filtros), "-map", f"[{ult}]", "-t", f"{duracao(cfr):.2f}", "-c:v", "libx264",
                    "-crf", "16", "-pix_fmt", "yuv420p", "-r", "30", out], check=True)
    return out


def borroes_da_loja():
    m, (t_campo, t_x) = MARCAS, [t for t, _, _ in toques("loja")[:2]]
    x1, y1, x2, y2 = CONTEUDO
    _, cy1, cx2, cy2 = CARTAO
    return [
        SUGESTOES + (0, m["enter1"] + 0.4),                     # sugestões da 1ª busca
        (0, 1480, 1080, 2330, m["enter1"] + 0.4, t_campo + 0.7),  # abaixo do nosso resultado
        SUGESTOES + (t_campo + 0.6, m["enter2"] + 0.4),          # sugestões da 2ª busca
        CONTEUDO + (m["enter2"] + 0.4, m["achou"]),              # outros apps enquanto rola
        (x1, y1, x2, cy1, m["achou"], m["pagina"] + 0.3),        # em volta do nosso cartão
        (x1, cy2, x2, y2, m["achou"], m["pagina"] + 0.3),
        (cx2, cy1, x2, cy2, m["achou"], m["pagina"] + 0.3),
    ]


def barra_busca(path, W, y, texto, cursor=True, completo=False):
    """A busca em grande: barra branca com lupa, o texto digitado e o cursor."""
    im = Image.new("RGBA", (W, y + 130), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    x0, x1, h = 70, W - 70, 116
    d.rounded_rectangle((x0, y, x1, y + h), radius=h // 2, fill=(255, 255, 255),
                        outline=AMARELO if completo else AZUL, width=6)
    cx, cy = x0 + 62, y + h // 2
    d.ellipse((cx - 22, cy - 26, cx + 14, cy + 10), outline=AZUL, width=7)
    d.line((cx + 10, cy + 6, cx + 26, cy + 22), fill=AZUL, width=8)
    ft = fr.font(B, 60)
    tx = x0 + 120
    d.text((tx, y + 18), texto, font=ft, fill=INK)
    if cursor:
        cxr = tx + d.textlength(texto, font=ft) + 8
        d.rectangle((cxr, y + 26, cxr + 5, y + h - 26), fill=AZUL)
    im.save(path)


def moldura(path, W, H, passo, total, titulo, sub=None):
    im = Image.new("RGBA", (W, H), BG + (255,))
    d = ImageDraw.Draw(im)
    story = H > 1500
    y = 60 if story else 34
    t = f"PASSO {passo} DE {total}"
    fp = fr.font(B, 30)
    tw = d.textlength(t, font=fp)
    d.rounded_rectangle(((W - tw) / 2 - 22, y, (W + tw) / 2 + 22, y + 52), radius=26, fill=AZUL)
    d.text(((W - tw) / 2, y + 8), t, font=fp, fill=(255, 255, 255))
    y += 76
    ft = fr.font(B, 60 if story else 52)
    for line in fr.wrap(d, titulo, ft, W - 120):
        d.text(((W - d.textlength(line, font=ft)) / 2, y), line, font=ft, fill=INK)
        y += ft.size + 10
    if sub:
        fs = fr.font(R, 36 if story else 32)
        for line in fr.wrap(d, sub, fs, W - 140):
            d.text(((W - d.textlength(line, font=fs)) / 2, y + 4), line, font=fs, fill=SUB)
            y += fs.size + 10
    d.rounded_rectangle((fr.PH_X - fr.BORDER, fr.PH_Y - fr.BORDER, fr.PH_X + fr.PH_W + fr.BORDER,
                         fr.PH_Y + fr.PH_H + fr.BORDER), radius=fr.RADIUS + fr.BORDER, fill=(20, 22, 28))
    hole = Image.new("L", (W, H), 0)
    ImageDraw.Draw(hole).rounded_rectangle((fr.PH_X, fr.PH_Y, fr.PH_X + fr.PH_W, fr.PH_Y + fr.PH_H),
                                           radius=fr.RADIUS, fill=255)
    im.putalpha(Image.eval(hole, lambda v: 255 - v))
    d = ImageDraw.Draw(im)
    fn = fr.font(R, 28)
    t = "Metrô Ceará · app não oficial, sem vínculo com o Metrofor"
    d.text(((W - d.textlength(t, font=fn)) / 2, H - 52), t, font=fn, fill=SUB)
    im.save(path)


def com_busca(mp4, partes, vel, digitado, W, y, nome):
    """Põe a barra de busca grande sobre o passo, acompanhando a digitação.
    digitado: [(t na gravação, texto até ali)]; partes/vel: os cortes do passo."""
    def saida(t):  # instante na gravação -> instante no passo
        acc = 0.0
        for a, b in partes:
            if a <= t <= b:
                return acc + (t - a) / vel
            acc += (b - a) / vel
        return None
    total = sum(b - a for a, b in partes) / vel
    estados = [(0.0, "", True, False)]
    for t, txt in digitado:
        st = saida(t)
        if st is not None:
            estados.append((st, txt, True, False))
    t_fim = saida(digitado[-1][0])
    if t_fim is not None:
        estados.append((t_fim + 0.5, digitado[-1][1], False, True))
    else:  # a digitação ficou antes do corte: a barra já aparece completa
        estados = [(0.0, digitado[-1][1], False, True)]
    inputs, filtros, ult = [], [], "0:v"
    for i, (t0, txt, cur, comp) in enumerate(estados):
        t1 = estados[i + 1][0] if i + 1 < len(estados) else total + 1
        png = os.path.join(S, f"{nome}-busca{i}.png")
        barra_busca(png, W, y, txt, cur, comp)
        inputs += ["-loop", "1", "-i", png]
        filtros.append(f"[{ult}][{i + 1}:v]overlay=0:0:enable='between(t,{t0:.2f},{t1 - 0.001:.3f})'[s{i}]")
        ult = f"s{i}"
    out = mp4.replace(".mp4", "-busca.mp4")
    subprocess.run([FF, "-v", "error", "-y", "-i", mp4] + inputs + [
        "-filter_complex", ";".join(filtros), "-map", f"[{ult}]", "-map", "0:a", "-t", f"{total:.3f}",
        "-c:v", "libx264", "-crf", "20", "-pix_fmt", "yuv420p", "-r", "30", "-c:a", "copy", out], check=True)
    return out


def final_destacado(dst):
    im = Image.open(os.path.join(S, "fim.png")).convert("RGB")
    ImageDraw.Draw(im).rounded_rectangle((590, 820, 1040, 1225), radius=36, outline=AMARELO, width=12)
    im.save(dst)
    return dst


def cartao_final(path, W, H):
    """Ícone, as duas buscas, QR e a linha do iPhone, centralizados na altura."""
    im = Image.new("RGB", (W, 2 * H), BG)
    d = ImageDraw.Draw(im)
    story = H > 1500
    ic, mask = fr.rounded_icon(220 if story else 160)
    y = 0
    im.paste(ic, ((W - ic.width) // 2, y), mask)
    y += ic.height + 36

    def c(text, fnt, fill, y, gap=12):
        for line in fr.wrap(d, text, fnt, W - 140):
            d.text(((W - d.textlength(line, font=fnt)) / 2, y), line, font=fnt, fill=fill)
            y += fnt.size + gap
        return y
    y = c("Metrô Ceará", fr.font(B, 88 if story else 72), INK, y)
    y = c("Na Play Store, pesquise:", fr.font(R, 42 if story else 34), SUB, y + 8) + 10
    for termo in ("metrô ceará", "metrô fortaleza"):
        p = os.path.join(S, "fim-termo.png")
        barra_busca(p, W, 0, termo, cursor=False, completo=True)
        b = Image.open(p)
        im.paste(b, (0, y), b)
        y += 140
    y = c("Grátis · sem anúncios · sem cadastro", fr.font(B, 40 if story else 32), AZUL, y + 14) + 24
    if True:
        q = fr.qr_image(PLAY, 300 if story else 230)
        pad = 22
        bx = (W - q.size[0]) // 2
        d.rounded_rectangle((bx - pad, y, bx + q.size[0] + pad, y + q.size[1] + 2 * pad), radius=26,
                            fill=(255, 255, 255), outline=(222, 226, 234), width=3)
        im.paste(q, (bx, y + pad))
        y += q.size[1] + 2 * pad + 30
    y = c("Tem iPhone? Use a versão lite no navegador: link na bio", fr.font(R, 32 if story else 28), SUB, y)
    out = Image.new("RGB", (W, H), BG)
    out.paste(im.crop((0, 0, W, y)), (0, max(30, (H - y) // 2)))
    out.save(path)


def montar(nome, W, H, ph_h, ph_y, busca_y):
    fr.W, fr.H = W, H
    fr.PH_H = ph_h
    fr.PH_W = round(SRC_W * ph_h / SRC_H / 2) * 2
    fr.PH_X = (W - fr.PH_W) // 2
    fr.PH_Y = ph_y
    loja, app = PRONTOS
    m = MARCAS
    tl, ta = toques("loja"), toques("app")
    t_campo, t_card, t_inst = tl[0][0], tl[2][0], tl[3][0]
    t_pular, t_loc, t_perm = ta[0][0], ta[1][0], ta[2][0]
    T = 6
    # (fonte, cortes, velocidade, título, subtítulo, digitação)
    passos = [
        (loja, [(0.4, m["enter1"] + 0.3), (m["enter1"] + 1.9, m["enter1"] + 4.2)], 1.0,
         "Pesquise na Play Store", None, DIG[1]),
        (loja, [(t_campo - 0.2, m["enter2"] + 0.3)], 1.0, "Também aparece em", None, DIG[2]),
        (loja, [(m["resultado2"] - 0.4, m["achou"]), (m["achou"] + 1.2, t_card + 0.4)], 1.4,
         "Ache o Metrô Ceará e toque nele", "Os outros resultados estão borrados", None),
        (loja, [(m["pagina"] + 1.8, m["pagina"] + 3.6), (t_inst - 0.8, t_inst + 0.3)], 1.0,
         "Toque em Instalar", "Grátis, sem anúncios e sem cadastro", None),
        (app, [(1.4, t_pular + 0.5)], 1.25, "Abra o app e toque em Pular",
         "As perguntas podem ser respondidas depois", None),
        (app, [(t_loc - 1.6, t_loc + 0.5), (t_perm - 1.4, t_perm + 0.4)], 1.0,
         "Toque em Usar minha localização", "E permita durante o uso do app", None),
    ]
    clips = []
    for i, (src, partes, vel, tit, sub, dig) in enumerate(passos):
        png, mp4 = os.path.join(S, f"{nome}-p{i}.png"), os.path.join(S, f"{nome}-p{i}.mp4")
        moldura(png, W, H, i + 1, T + 1, tit, sub)
        fr.clip(FF, src, png, mp4, partes, vel)
        if dig:
            mp4 = com_busca(mp4, partes, vel, dig, W, busca_y, f"{nome}-p{i}")
        clips.append(mp4)
    png = os.path.join(S, f"{nome}-p6.png")
    moldura(png, W, H, T + 1, T + 1, "Pronto: o próximo metrô da estação mais perto",
            "Nos dois sentidos, com quanto tempo falta")
    fim = os.path.join(S, f"{nome}-fim.mp4")
    fim_src = final_destacado(os.path.join(S, "fim-destaque.png"))
    subprocess.run([FF, "-v", "error", "-y", "-loop", "1", "-t", "3.5", "-i", fim_src, "-loop", "1", "-t", "3.5",
                    "-i", png, "-f", "lavfi", "-t", "3.5", "-i", "anullsrc=r=44100:cl=stereo", "-filter_complex",
                    f"color=c=0x14161c:s={W}x{H}:r=30:d=3.5[bg];[0:v]scale={fr.PH_W}:{fr.PH_H}[ph];"
                    f"[bg][ph]overlay={fr.PH_X}:{fr.PH_Y}[v1];[v1][1:v]overlay=0:0,format=yuv420p[v]",
                    "-map", "[v]", "-map", "2:a", "-c:v", "libx264", "-crf", "20", "-r", "30", "-c:a", "aac",
                    "-shortest", fim], check=True)
    clips.append(fim)
    card_png, card = os.path.join(S, f"{nome}-card.png"), os.path.join(S, f"{nome}-card.mp4")
    cartao_final(card_png, W, H)
    fr.still(FF, card_png, 3.5, card)
    clips.append(card)
    final = os.path.join(OUT, f"explicativo-instalar-{nome}.mp4")
    fr.concat(FF, clips, os.path.join(S, f"{nome}-list.txt"), final)
    print(final, f"{duracao(final):.1f} s")


if __name__ == "__main__":
    PRONTOS = (preparar("loja", borroes_da_loja()), preparar("app", []))
    montar("story", 1080, 1920, 1340, 440, 236)
    montar("feed", 1080, 1350, 930, 330, 180)
