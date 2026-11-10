#!/bin/bash
# Grava no emulador a cena do botão ↻ do widget, para o vídeo da declaração do
# serviço em primeiro plano (localização) na Play Store: a pessoa toca no ↻ e o
# widget passa para a estação mais perto. A notificação "Procurando onde você
# está…" existe, mas no emulador a posição chega na hora e ela some antes de a
# cortina abrir; o vídeo cita a notificação na legenda.
#
#   VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_widget.sh
#
# Saída: $VIDEO_WORK/widget.mp4 e $VIDEO_WORK/marcas-widget.txt, que build_fgs.py usa.
# Precisa: emulador ligado, app instalado, o widget 2×2 ou 4×2 na tela inicial, adb root.
set -e
cd "$(dirname "$0")/../.."
. tools/emulador/ui.sh
VIDEO_WORK=${VIDEO_WORK:-/tmp/metro-video}
mkdir -p "$VIDEO_WORK"
MARCAS=$VIDEO_WORK/marcas-widget.txt
marca() { echo "$1 $(python3 -c "import time; print(round(time.time() - $T0, 2))")" >> "$MARCAS"; echo "· $1"; }

$A root >/dev/null; sleep 1
$A shell settings put global auto_time 0
at 0720; demo 0720
$A shell input keyevent WAKEUP; $A shell wm dismiss-keyguard; $A shell input keyevent HOME; sleep 2
wait_text "↻" 10
# Antes de gravar, o widget fica em Parangaba (a pessoa estava lá).
for _ in 1 2 3; do $A emu geo fix -38.5638658 -3.7757912 >/dev/null; sleep 0.5; done
tap "↻"; wait_text "Parangaba" 20; sleep 2

$A shell rm -f /sdcard/widget.mp4
$A shell screenrecord --bit-rate 12000000 /sdcard/widget.mp4 &
REC=$!
sleep 1
T0=$(python3 -c "import time; print(time.time())")
: > "$MARCAS"
marca inicio; sleep 3

# A pessoa está em São Benedito; o widget ainda mostra a estação anterior.
for _ in 1 2 3; do $A emu geo fix -38.5363 -3.7330 >/dev/null; sleep 0.5; done
marca toque
tap "↻"
wait_text "São Benedito" 20
marca atualizado
sleep 4
demo 0721; sleep 1                      # muda a tela: o screenrecord grava o último quadro
marca fim

$A shell pkill -INT screenrecord || true
wait $REC 2>/dev/null || true
sleep 2
$A pull /sdcard/widget.mp4 "$VIDEO_WORK/widget.mp4" >/dev/null
$A shell settings put global auto_time 1
echo "$VIDEO_WORK/widget.mp4"; cat "$MARCAS"
