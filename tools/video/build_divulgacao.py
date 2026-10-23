"""Vídeo de divulgação (divulgacao/metro-ceara-divulgacao.mp4, 1080x1920, sem música).

Tela inicial, lembrete da hora de sair, modo viagem São Benedito → Parangaba,
aviso de descida, chegada e o cartão final com o QR da versão lite.
Entrada: $VIDEO_WORK/viagem.mp4 e marcas.txt, de gravar_viagem.sh.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import frames as fr  # noqa: E402

S = os.environ.get("VIDEO_WORK", "/tmp/metro-video")
FF = os.environ.get("FFMPEG", "ffmpeg")
OUT = S + "/divulgacao"
os.makedirs(OUT, exist_ok=True)
FINAL = os.path.join(fr.ROOT, "divulgacao", "metro-ceara-divulgacao.mp4")
LITE = "giovanildo.github.io/metro-fortaleza-horarios/app"

fr.PH_W = round(1080 * fr.PH_H / 2400 / 2) * 2
fr.PH_X = (fr.W - fr.PH_W) // 2

m = fr.marks(S + "/marcas.txt")
# (trechos, velocidade, título, subtítulo)
SEGMENTS = [
    ([(m["inicio"] + 0.3, m["lembrete"] + 2)], 1.0,
     "Que horas passa o metrô?", "Abra o app: a estação mais perto e a próxima partida"),
    ([(m["lembrete"] + 2.5, m["lembrete"] + 6), (m["lembrete"] + 9.5, m["lembrete"] + 16)], 1.0,
     "Lembrete da hora de sair", "Toque no horário e o app cria o lembrete na agenda"),
    ([(m["destino"] - 1, m["destino"] + 5.5), (m["embarque"] - 3.5, m["embarque"] - 0.5)], 1.0,
     "Modo viagem", "Escolha o destino e guarde o celular"),
    ([(m["embarque"] - 0.5, m["notificacao"]), (m["bloqueio"] + 5, m["aviso"] + 5)], 1.25,
     "Acompanha estação por estação", "Pelo GPS, até com a tela desligada"),
    ([(m["aviso"] + 5.3, m["desbloqueio"] + 1)], 1.0,
     "Avisa antes de você descer", "Notificação, vibração e voz"),
    ([(m["chegada"] + 4, m["chegada"] + 9)], 1.0, "Você chegou!", None),
]
CARD = dict(title="Metrô Ceará", icon=True,
            body="Horários de metrô e VLT no celular.\nGrátis · sem anúncios · sem cadastro",
            note="Fortaleza · Cariri · Sobral", qr="https://" + LITE + "/", qr_text=LITE)

SRC = OUT + "/cfr.mp4"
fr.to_cfr(FF, S + "/viagem.mp4", SRC)
clips = []
for i, (parts, speed, title, subtitle) in enumerate(SEGMENTS, 1):
    png, mp4 = f"{OUT}/seg{i:02d}.png", f"{OUT}/seg{i:02d}.mp4"
    fr.phone_frame(png, title, subtitle)
    fr.clip(FF, SRC, png, mp4, parts, speed)
    clips.append(mp4)
fr.card(f"{OUT}/final.png", **CARD)
fr.still(FF, f"{OUT}/final.png", 5, f"{OUT}/final.mp4")
clips.append(f"{OUT}/final.mp4")
fr.concat(FF, clips, f"{OUT}/list.txt", FINAL)
print(FINAL)
