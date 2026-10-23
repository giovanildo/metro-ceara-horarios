#!/bin/bash
# Grava no emulador a tomada usada pelo vídeo de divulgação e pelo da Play Store:
# tela inicial, lembrete da hora de sair, modo viagem São Benedito → Parangaba
# (metrô das 07:32), notificação fixa, tela bloqueada, aviso de descida e chegada.
#
#   VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_viagem.sh
#
# Saída: $VIDEO_WORK/viagem.mp4 (screenrecord 1080x2400) e $VIDEO_WORK/marcas.txt
# ("nome segundos" de cada cena, contados do início da gravação), que
# build_fgs.py e build_divulgacao.py usam para cortar.
# Precisa: emulador ligado, app instalado, sistema em pt-BR, adb root.
# DIA=MMDD escolhe o dia (padrão: hoje); use um dia cuja grade já foi baixada.
set -e
cd "$(dirname "$0")/../.."
. tools/emulador/ui.sh
VIDEO_WORK=${VIDEO_WORK:-/tmp/metro-video}
mkdir -p "$VIDEO_WORK"
DIA=${DIA:-$(date +%m%d)}
PKG=io.github.giova.metrofortaleza
MARCAS=$VIDEO_WORK/marcas.txt

hora() { # HHMM: relógio do aparelho e da barra de status
  $A shell "date ${DIA}${1}$(date +%Y).00" >/dev/null
  $A shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$1" >/dev/null
}
gps() { $A emu geo fix "$1" "$2" >/dev/null; }
gps_lost() { # segundos sem GPS até avisar (app parado)
  $A shell "run-as $PKG sed -i 's/name=\"gps_lost_s\" value=\"[0-9]*\"/name=\"gps_lost_s\" value=\"$1\"/' shared_prefs/metrofor_settings.xml"
}
marca() { echo "$1 $(python3 -c "import time; print(round(time.time() - $T0, 2))")" >> "$MARCAS"; echo "· $1"; }
estacao() { # HHMM lon lat: o metrô chega à estação
  hora "$1"; for _ in 1 2 3; do gps "$2" "$3"; sleep 1; done
}

$A root >/dev/null; sleep 1
$A shell settings put global auto_time 0
$A shell locksettings set-disabled false >/dev/null   # tela de bloqueio (deslizar) para a cena da tela bloqueada
# "Sem GPS" só depois de 5 min: os saltos de relógio da simulação não contam como GPS perdido.
$A shell am force-stop $PKG
gps_lost 300
hora 0725; demo 0725
for _ in 1 2 3; do gps -38.5363 -3.7330; sleep 1; done
$A shell monkey -p $PKG 1 >/dev/null 2>&1
wait_text "Iniciar" 40
sleep 3

$A shell rm -f /sdcard/viagem.mp4
$A shell screenrecord --bit-rate 12000000 /sdcard/viagem.mp4 &
REC=$!
sleep 1
T0=$(python3 -c "import time; print(time.time())")
: > "$MARCAS"
marca inicio; sleep 4

marca lembrete
tap "São Benedito"; wait_text "Próximas partidas"; sleep 2
tap "07:50"; wait_text "Lembrar a hora de sair"; sleep 4
tap "Cancelar"; sleep 1
for _ in 1 2 3; do $A shell input keyevent BACK; wait_text "Iniciar" 6 && break; done; sleep 1.5

marca iniciar
tap "Iniciar"; wait_text "Para qual estação você vai?"; sleep 2
marca destino
tap "Parangaba"; wait_text "Como você quer ir?"; sleep 3
tap "Direto"; sleep 3
marca embarque
hora 0732; sleep 3
marca viagem
estacao 0735 -38.5396476 -3.7391232   # Benfica
sleep 1
marca notificacao
$A shell cmd statusbar expand-notifications; sleep 4
$A shell cmd statusbar collapse; sleep 1
estacao 0736 -38.5458588 -3.7457007   # Padre Cícero
marca bloqueio
$A shell input keyevent POWER; sleep 2
estacao 0738 -38.55165 -3.7514799     # Porangabussu
$A shell input keyevent WAKEUP; sleep 3
marca aviso
estacao 0740 -38.5575385 -3.75734     # Couto Fernandes: aviso de descida
sleep 8
marca desbloqueio
$A shell wm dismiss-keyguard; sleep 2
estacao 0741 -38.5600252 -3.7662377   # Juscelino Kubitscheck
marca chegada
hora 0743; for _ in $(seq 1 12); do gps -38.5638658 -3.7757912; sleep 1; done   # Parangaba: chegada
marca fim

$A shell pkill -INT screenrecord || true
wait $REC 2>/dev/null || true
sleep 2
$A pull /sdcard/viagem.mp4 "$VIDEO_WORK/viagem.mp4" >/dev/null
gps_lost 60
$A shell settings put global auto_time 1
echo "$VIDEO_WORK/viagem.mp4"; cat "$MARCAS"
