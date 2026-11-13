"""Vídeo de divulgação (divulgacao/metro-ceara-divulgacao.mp4, 1080x1920, ~30 s, sem música).

Tela inicial, aviso da hora de sair, "Para onde você vai?", modo viagem,
aviso de descida na tela bloqueada, chegada e o cartão final com o QR da
Play Store. Entrada: $VIDEO_WORK/viagem.mp4 e marcas.txt, de gravar_viagem.sh.
Os deslocamentos depois de cada marca saíram da gravação de 12/11/2026 (1.99):
depois de gravar de novo, confira numa folha de quadros e ajuste.
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = S + "/divulgacao"
os.makedirs(OUT, exist_ok=True)
FINAL = os.path.join(fr.ROOT, "divulgacao", "metro-ceara-divulgacao.mp4")
PLAY = "https://play.google.com/store/apps/details?id=io.github.giova.metroceara"

fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

m = fr.marks(S + "/marcas.txt")
# (trechos, velocidade, título, subtítulo)
SEGMENTS = [
    ([(m["inicio"] + 0.3, m["inicio"] + 3.8)], 1.0,
     "Que horas passa o metrô?", "Abra o app: a estação mais perto e a próxima partida"),
    ([(m["lembrete"] + 3.8, m["lembrete"] + 5), (m["lembrete"] + 11.2, m["lembrete"] + 15.2)], 1.0,
     "Avisa a hora de sair", "Toque no horário e o app avisa antes"),
    ([(m["destino"] + 3.1, m["destino"] + 4.1), (m["destino"] + 7, m["destino"] + 9.3),
      (m["destino"] + 13.3, m["destino"] + 15.6), (m["iniciar"] + 2, m["iniciar"] + 3)], 1.2,
     "Para onde você vai?", "O caminho, a integração e a hora de chegada"),
    ([(m["embarque"] - 3.5, m["embarque"] - 0.3), (m["bloqueio"] - 3.8, m["bloqueio"] - 1.8)], 1.0,
     "Modo viagem", "Acompanha estação por estação pelo GPS"),
    ([(m["aviso"] + 3.6, m["aviso"] + 8.1)], 1.0,
     "Avisa antes de você descer", "Até com a tela desligada"),
    ([(m["chegada"] - 0.3, m["chegada"] + 0.9), (m["chegada"] + 5, m["chegada"] + 8)], 1.0,
     "Você chegou!", None),
]
CARD = dict(title="Metrô Ceará", icon=True,
            body="Horários de metrô e VLT no celular.\nGrátis · sem anúncios · sem cadastro",
            note="Fortaleza · Cariri · Sobral", qr=PLAY, qr_text="Baixe grátis no Google Play")

SRC = OUT + "/cfr.mp4"
fr.to_cfr(FF, S + "/viagem.mp4", SRC)
# A gravação acaba no último quadro que mudou (o resumo da chegada): repete-o por 5 s.
PAD = OUT + "/cfr-pad.mp4"
subprocess.run([FF, "-v", "error", "-y", "-i", SRC, "-vf", "tpad=stop_mode=clone:stop_duration=5",
                "-c:v", "libx264", "-crf", "16", "-pix_fmt", "yuv420p", PAD], check=True)
clips = []
for i, (parts, speed, title, subtitle) in enumerate(SEGMENTS, 1):
    png, mp4 = f"{OUT}/seg{i:02d}.png", f"{OUT}/seg{i:02d}.mp4"
    fr.phone_frame(png, title, subtitle)
    fr.clip(FF, PAD, png, mp4, parts, speed)
    clips.append(mp4)
fr.card(f"{OUT}/final.png", **CARD)
fr.still(FF, f"{OUT}/final.png", 4, f"{OUT}/final.mp4")
clips.append(f"{OUT}/final.mp4")
fr.concat(FF, clips, f"{OUT}/list.txt", FINAL)
print(FINAL)
