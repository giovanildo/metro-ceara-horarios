#!/bin/bash
# Grava no emulador as 4 telas dos Reels de divulgacao/instagram-calendario/
# (abrir, integracao, lugares, passatempos) em $VIDEO_WORK/insta/.
# Terça simulada 24/11/2026 às 07:25, sem internet, GPS perto de São Benedito.
# Precisa: emulador ligado, app instalado com a grade de dia útil já baixada, adb root.
#
#   VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_instagram.sh
#   VIDEO_WORK=/tmp/metro-video python3 tools/video/instagram_app.py
#
# O caça-estações guarda o jogo do dia: para gravar de novo, use outro DIA ou
# limpe os dados do app. As coordenadas dos toques no quadro são do jogo de
# 24/11 (PORTUGUÊS e BAIRRO); outro dia tem outro quadro.
set -e
cd "$(dirname "$0")/../.."
. tools/emulador/ui.sh
OUTDIR=${VIDEO_WORK:-/tmp/metro-video}/insta
mkdir -p "$OUTDIR"
PKG=io.github.giova.metroceara
DIA=${DIA:-1124}

tapn() { # tapn TEXTO N: toca na N-ésima tela que contém o texto
  dump
  local b; b=$(python3 - "$1" "${2:-1}" "$U" <<'PY'
import re, sys, html
alvo, n, t = sys.argv[1], int(sys.argv[2]), open(sys.argv[3]).read()
ach = []
for m in re.finditer(r'<node [^>]*>', t):
    g = m.group(0); tx = re.search(r' text="([^"]*)"', g)
    if tx and alvo in html.unescape(tx.group(1)): ach.append(g)
if len(ach) >= n:
    x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', ach[n - 1]).groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
)
  if [ -n "$b" ]; then $A shell input tap $b; else echo "NAO ACHEI: $1"; fi
}
rec_start() { $A shell rm -f /sdcard/r.mp4; $A shell screenrecord --bit-rate 12000000 /sdcard/r.mp4 & REC=$!; sleep 1.5; }
rec_stop() { sleep 1; $A shell pkill -INT screenrecord || true; wait $REC 2>/dev/null || true; sleep 1
  $A pull /sdcard/r.mp4 "$OUTDIR/$1.mp4" >/dev/null; echo "gravado $1"; }
digita() { for ((i = 0; i < ${#1}; i++)); do $A shell input text "${1:$i:1}"; sleep 0.15; done; }
relogio() { $A shell am force-stop $PKG; $A shell "date ${DIA}0725$(date +%Y).00" >/dev/null; demo 0725; fix -38.5363 -3.7330; }
campo() { local b; b=$(bounds "Para onde você vai?"); set -- $b; $A shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2)); }
abre() { $A shell monkey -p $PKG 1 >/dev/null 2>&1; sleep 4; tap "Início" >/dev/null; sleep 1; }

$A root >/dev/null; sleep 1
$A shell settings put global auto_time 0
$A shell svc wifi disable; $A shell svc data disable
$A shell input keyevent WAKEUP; $A shell wm dismiss-keyguard

# 1. Abrir o app e a estação
relogio; $A shell input keyevent HOME; sleep 2
rec_start
$A shell monkey -p $PKG 1 >/dev/null 2>&1; sleep 5
tap "São Benedito"; sleep 3.5
$A shell input swipe 540 1700 540 1000 900; sleep 3
rec_stop abrir

# 2. Para onde você vai? São Benedito → Papicu (integração em Parangaba)
relogio; abre
rec_start
tap "Planeje sua viagem"; sleep 2
campo; sleep 1.5
digita Papicu; sleep 2
tapn "chega" 1; sleep 3.5
$A shell input swipe 540 1700 540 900 1200; sleep 3.5
rec_stop integracao

# 3. Buscar lugares: praia, depois Dragão do Mar
relogio; abre
rec_start
tap "Planeje sua viagem"; sleep 2
campo; sleep 1.5
digita praia; sleep 3.5
for _ in 1 2 3 4 5; do $A shell input keyevent DEL; sleep 0.1; done
digita Drag; sleep 2.5
tapn "Centro Dragão do Mar" 1; sleep 3.5
$A shell input swipe 540 1700 540 900 1200; sleep 3.5
rec_stop lugares

# 4. Passatempos: caça-estações (a página é web: toques por coordenada)
relogio; abre
tap "Mais"; sleep 1.5; tap "Passatempos"; sleep 3
$A shell input tap 650 700; sleep 3      # Caça-estações
$A shell input tap 540 990; sleep 3      # Iniciar
rec_start
$A shell input swipe 94 1824 888 1824 1500; sleep 2    # PORTUGUÊS
$A shell input swipe 490 1524 986 1524 1200; sleep 2   # BAIRRO
rec_stop passatempos
