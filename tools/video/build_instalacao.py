"""Vídeo "Como instalar no Android" (divulgacao/metro-ceara-como-instalar.mp4, até 60 s,
para caber no status do WhatsApp; também em docs/video/como-instalar-android.mp4).

Os passos reais da instalação pelo site e, depois, os problemas comuns e a saída
de cada um (ilustrações marcadas como tal: aviso do navegador e Play Protect não
aparecem no emulador). Entrada: $VIDEO_WORK/instalacao.mp4 e
marcas-instalacao.txt, de gravar_instalacao.sh.
"""
import os
import shutil
import subprocess
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = S + "/instalacao"
os.makedirs(OUT, exist_ok=True)
FINAL = os.path.join(fr.ROOT, "divulgacao", "metro-ceara-como-instalar.mp4")
SITE = os.path.join(fr.ROOT, "docs", "video", "como-instalar-android")
LITE = "giovanildo.github.io/metro-ceara-horarios/app"
SPEED = 1.3

fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

m = fr.marks(S + "/marcas-instalacao.txt")
# (trechos, passo, título, subtítulo). Os deslocamentos cobrem a demora dos toques e das janelas.
STEPS = [
    ([(m["pagina"] + 0.5, m["pagina"] + 4), (m["baixar"] + 0.5, m["baixar"] + 5)], 1,
     "Na página do app, toque em Baixar o app completo", "giovanildo.github.io/metro-ceara-horarios"),
    ([(m["abrir"] - 1.5, m["abrir"] + 3.5)], 2, "Quando o download terminar, toque em Abrir", None),
    ([(m["abrir"] + 4, m["configuracoes"] + 2)], 3, "Toque em Configurações",
     "O Android bloqueia apps de fora da Play Store. É normal."),
    ([(m["configuracoes"] + 4.6, m["permitir"] + 3)], 4, "Ligue “Permitir desta fonte”", None),
    ([(m["permitir"] + 3.3, m["instalar"] + 2.5)], 5, "Na janela que aparece, toque em Instalar", None),
    ([(m["instalar"] + 3.5, m["instalado"] + 1.5)], 6, "Instalado! Toque em Abrir", None),
    ([(m["instalado"] + 5.5, m["localizacao"] + 3.5)], 7, "Toque em Usar minha localização", None),
    ([(m["localizacao"] + 4.5, m["permissao"] + 2)], 8, "Escolha “Durante o uso do app”",
     "A localização fica só no seu celular"),
    ([(m["permissao"] + 3, m["permissao"] + 8)], None, "Pronto! A estação mais perto e o próximo metrô", None),
]
INTRO = dict(title="Como instalar o Metrô Ceará", body="Passo a passo, com os avisos do Android", icon=True)
CARDS = [
    dict(title="O navegador avisou que o arquivo pode ser perigoso?",
         body="Toque em Baixar mesmo assim. É o aviso padrão para todo app de fora da Play Store.",
         mock=("Arquivo possivelmente perigoso", "Este tipo de arquivo pode danificar o dispositivo.",
               [("Cancelar", False), ("Baixar mesmo assim", True)])),
    dict(title="Apareceu o Google Play Protect?", body="Toque em Mais detalhes e depois em Instalar mesmo assim.",
         mock=("Google Play Protect", "App não verificado / desconhecido.",
               [("Mais detalhes", True), ("Instalar mesmo assim", True)])),
    dict(title="Celular Samsung bloqueou?",
         body="Desligue o Bloqueador automático. Depois de instalar, pode ligar de novo.",
         steps=["Configurações", "Segurança e privacidade", "Bloqueador automático", "Desligar"]),
    dict(title="Apareceu “App não instalado” ao atualizar?",
         body="Já existe uma versão de outra origem no celular. Desinstale a antiga e instale de novo."),
]
FIM = dict(title="Pronto!", body="Grátis, sem anúncios e de código aberto. Nada sai do seu celular.",
           qr="https://" + LITE + "/", qr_text=LITE)

SRC = OUT + "/cfr.mp4"
fr.to_cfr(FF, S + "/instalacao.mp4", SRC)
clips = []


def card(name, secs, spec):
    png, mp4 = f"{OUT}/{name}.png", f"{OUT}/{name}.mp4"
    fr.card(png, **spec)
    fr.still(FF, png, secs, mp4)
    clips.append(mp4)
    return png


intro = card("00", 2.5, INTRO)
for i, (parts, step, title, subtitle) in enumerate(STEPS, 1):
    png, mp4 = f"{OUT}/seg{i:02d}.png", f"{OUT}/seg{i:02d}.mp4"
    fr.phone_frame(png, title, subtitle, step)
    fr.clip(FF, SRC, png, mp4, parts, SPEED)
    clips.append(mp4)
for k, spec in enumerate(CARDS, 1):
    card(f"c{k}", 3.6, spec)
card("fim", 4.0, FIM)
fr.concat(FF, clips, f"{OUT}/list.txt", FINAL)

shutil.copyfile(FINAL, SITE + ".mp4")
Image.open(intro).convert("RGB").resize((360, 640), Image.LANCZOS).save(SITE + ".jpg", quality=85)
dur = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", FINAL],
                     capture_output=True, text=True).stdout.strip()
print(FINAL, dur, "s")
