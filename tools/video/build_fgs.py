"""Vídeo para a declaração do serviço em primeiro plano (localização) na Play Store.

Mostra o modo viagem em uso: iniciar, notificação fixa, tela bloqueada, aviso de
descida. Entrada: $VIDEO_WORK/viagem.mp4 e marcas.txt, de gravar_viagem.sh.
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
    ([(m["iniciar"] - 0.5, m["iniciar"] + 5)], "1. The user taps Start (trip mode)", "A pessoa toca em Iniciar (modo viagem)"),
    ([(m["destino"] - 1, m["destino"] + 5.5), (m["embarque"] - 3.5, m["embarque"] - 0.5)],
     "2. Picks the destination station", "Escolhe a estação de destino"),
    ([(m["embarque"] - 0.5, m["notificacao"] + 1)], "3. Trip is tracked station by station",
     "A viagem é acompanhada estação por estação"),
    ([(m["notificacao"] + 1.5, m["notificacao"] + 5)], "4. Persistent notification with Stop button",
     "Notificação fixa, com o botão Encerrar viagem"),
    ([(m["bloqueio"] + 5, m["aviso"] + 5)], "5. Works with the screen off / locked",
     "Funciona com a tela desligada e bloqueada"),
    ([(m["aviso"] + 5.3, m["desbloqueio"] + 1)], "6. Alert before the destination", "Aviso antes da estação de destino"),
    ([(m["desbloqueio"] + 1.5, m["chegada"] + 1), (m["chegada"] + 4, m["chegada"] + 10)],
     "7. Tracking continues until arrival; location stays on the device",
     "Segue até a chegada; a localização fica só no aparelho"),
]

SRC = OUT + "/cfr.mp4"
fr.to_cfr(FF, S + "/viagem.mp4", SRC)
clips = []
for i, (parts, title, subtitle) in enumerate(SEGMENTS, 1):
    png, mp4 = f"{OUT}/seg{i:02d}.png", f"{OUT}/seg{i:02d}.mp4"
    fr.phone_frame(png, title, subtitle)
    fr.clip(FF, SRC, png, mp4, parts)
    clips.append(mp4)
fr.concat(FF, clips, f"{OUT}/list.txt", FINAL)
print(FINAL)
