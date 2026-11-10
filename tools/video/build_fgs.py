"""Vídeo para a declaração do serviço em primeiro plano (localização) na Play Store.

Mostra os dois usos do serviço: o modo viagem (destino, iniciar, notificação fixa,
tela bloqueada, aviso de descida, chegada) e o ↻ do widget. Entrada:
$VIDEO_WORK/viagem.mp4 e marcas.txt, de gravar_viagem.sh; $VIDEO_WORK/widget.mp4 e
marcas-widget.txt, de gravar_widget.sh.
Saída: tools/play/video-modo-viagem.mp4 (1080x1920).
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = S + "/fgs"
os.makedirs(OUT, exist_ok=True)
FINAL = os.path.join(fr.ROOT, "tools", "play", "video-modo-viagem.mp4")

fr.PH_H = 1420
fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

m = fr.marks(S + "/marcas.txt")
# (trechos da gravação, título em inglês para a análise da Google, subtítulo em português).
# Os deslocamentos depois de cada marca cobrem a demora dos toques e das notificações.
SEGMENTS = [
    ([(m["destino"] - 0.5, m["destino"] + 6), (m["iniciar"] - 3, m["iniciar"] - 0.5)],
     "1. The user picks the destination", "A pessoa escolhe o destino"),
    ([(m["iniciar"] - 0.5, m["embarque"] + 1)], "2. Taps Start trip mode",
     "Toca em Iniciar modo viagem"),
    ([(m["embarque"] - 0.5, m["notificacao"] + 1)], "3. Trip is tracked station by station",
     "A viagem é acompanhada estação por estação"),
    ([(m["notificacao"] + 1.5, m["notificacao"] + 5)], "4. Persistent notification with Stop button",
     "Notificação fixa, com o botão Encerrar viagem"),
    ([(m["bloqueio"] + 5, m["aviso"] + 5)], "5. Works with the screen off / locked",
     "Funciona com a tela desligada e bloqueada"),
    ([(m["aviso"] + 5.3, m["desbloqueio"] + 1)], "6. Alert before the destination", "Aviso antes da estação de destino"),
    ([(m["desbloqueio"] + 1.5, m["chegada"] + 1), (m["chegada"] + 4, m["chegada"] + 10)],
     "7. Stops by itself on arrival; location stays on the device",
     "Termina sozinho ao chegar; a localização fica só no aparelho"),
]
# O ↻ do widget usa o mesmo tipo de serviço, por poucos segundos.
w = fr.marks(S + "/marcas-widget.txt")
WIDGET = [
    ([(w["toque"] - 1.5, w["atualizado"] + 3)], "8. Widget: tap refresh, one location fix (seconds)",
     "Widget: o botão de atualizar pega a localização uma vez"),
]

SRC = OUT + "/cfr.mp4"
fr.to_cfr(FF, S + "/viagem.mp4", SRC)
SRC_W = OUT + "/cfr-widget.mp4"
fr.to_cfr(FF, S + "/widget.mp4", SRC_W)
clips = []
for i, (src, (parts, title, subtitle)) in enumerate(
        [(SRC, seg) for seg in SEGMENTS] + [(SRC_W, seg) for seg in WIDGET], 1):
    png, mp4 = f"{OUT}/seg{i:02d}.png", f"{OUT}/seg{i:02d}.mp4"
    fr.phone_frame(png, title, subtitle)
    fr.clip(FF, src, png, mp4, parts)
    clips.append(mp4)
fr.concat(FF, clips, f"{OUT}/list.txt", FINAL)
print(FINAL)
