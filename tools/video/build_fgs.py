"""Vídeo para a declaração do serviço em primeiro plano (localização) na Play Store.

Mostra o modo viagem em uso: iniciar, notificação fixa, tela bloqueada, aviso de
descida. Entrada: $VIDEO_WORK/fgs2.mp4 (screenrecord do emulador, 1080x2400).
Saída: tools/play/video-modo-viagem.mp4 (1080x1920).
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
SRC = S + "/fgs2.mp4"
OUT = S + "/fgs"
os.makedirs(OUT, exist_ok=True)
FINAL = os.path.join(fr.ROOT, "tools", "play", "video-modo-viagem.mp4")

fr.PH_H = 1420
fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

# (trechos do vídeo bruto, título em inglês para a análise da Google, subtítulo em português)
SEGMENTS = [
    ([(0.5, 5.5)], "1. The user taps Start (trip mode)", "A pessoa toca em Iniciar (modo viagem)"),
    ([(6.0, 9.6), (11.0, 12.5), (18.4, 20.0)], "2. Picks the destination station", "Escolhe a estação de destino"),
    ([(20.0, 27.0)], "3. Trip is tracked station by station", "A viagem é acompanhada estação por estação"),
    ([(27.6, 30.2)], "4. Persistent notification with Stop button", "Notificação fixa, com o botão Encerrar viagem"),
    ([(30.4, 32.0), (42.0, 44.5)], "5. Works with the screen off / locked", "Funciona com a tela desligada e bloqueada"),
    ([(51.3, 54.0)], "6. Alert before the destination", "Aviso antes da estação de destino"),
    ([(54.0, 58.0)], "7. Tracking continues until arrival; location stays on the device", "Segue até a chegada; a localização fica só no aparelho"),
]


def run(args):
    subprocess.run([FF, "-v", "error", "-y"] + args, check=True)


def segment(i, parts, title, subtitle):
    png = f"{OUT}/seg{i:02d}.png"
    fr.phone_frame(png, title, subtitle)
    mp4 = f"{OUT}/seg{i:02d}.mp4"
    inputs, chains = [], []
    for k, (a, b) in enumerate(parts):
        inputs += ["-ss", str(a), "-to", str(b), "-i", SRC]
        chains.append(f"[{k}:v]scale={fr.PH_W}:{fr.PH_H},setsar=1,setpts=PTS-STARTPTS,fps=30[p{k}]")
    n = len(parts)
    total = sum(b - a for a, b in parts)
    inputs += ["-loop", "1", "-t", f"{total:.3f}", "-i", png,
               "-f", "lavfi", "-t", f"{total:.3f}", "-i", "anullsrc=r=44100:cl=stereo"]
    fc = ";".join(chains) + ";" + "".join(f"[p{k}]" for k in range(n)) + f"concat=n={n}:v=1:a=0[ph];" + \
        f"color=c=0x1c1c1e:s=1080x1920:r=30:d={total:.3f}[bg];[bg][ph]overlay={fr.PH_X}:{fr.PH_Y}[v1];" + \
        f"[v1][{n}:v]overlay=0:0,format=yuv420p[v]"
    run(inputs + ["-filter_complex", fc, "-map", "[v]", "-map", f"{n + 1}:a", "-c:v", "libx264", "-crf", "21",
                  "-pix_fmt", "yuv420p", "-r", "30", "-c:a", "aac", "-shortest", mp4])
    return mp4


# O screenrecord só grava quadro quando a tela muda: numa tela parada não há
# quadros, e o corte pularia para o próximo. Convertendo para 30 quadros/s
# constantes antes, cada trecho começa onde deve.
CFR = OUT + "/cfr.mp4"
run(["-i", SRC, "-vf", "fps=30", "-c:v", "libx264", "-crf", "16", "-pix_fmt", "yuv420p", CFR])
SRC = CFR
clips = [segment(i, *seg) for i, seg in enumerate(SEGMENTS, 1)]
with open(f"{OUT}/list.txt", "w") as fh:
    fh.writelines(f"file '{c}'\n" for c in clips)
run(["-f", "concat", "-safe", "0", "-i", f"{OUT}/list.txt", "-c", "copy", "-movflags", "+faststart", FINAL])
print(FINAL)
