#!/bin/bash
# Grava no emulador a tomada usada pelo vídeo de divulgação e pelo da Play Store:
# tela inicial, aviso da hora de sair, "Para onde você vai?" São Benedito → Parangaba,
# Iniciar modo viagem (metrô das 07:32), trilho, notificação fixa, tela bloqueada,
# aviso de descida e chegada. Caminho da tela desde a 1.94 (barra de baixo, trilho).
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
PKG=io.github.giova.metroceara
MARCAS=$VIDEO_WORK/marcas.txt

hora() { # HHMM: relógio do aparelho e da barra de status
  $A shell "date ${DIA}${1}$(date +%Y).00" >/dev/null
  $A shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$1" >/dev/null
}
gps() { $A emu geo fix "$1" "$2" >/dev/null; }
gps_lost() { # segundos sem GPS até avisar (app parado); app recém-instalado ainda não tem o arquivo
  local f=shared_prefs/metrofor_settings.xml
  if $A shell "run-as $PKG ls $f" >/dev/null 2>&1; then
    $A shell "run-as $PKG sed -i 's/name=\"gps_lost_s\" value=\"[0-9]*\"/name=\"gps_lost_s\" value=\"$1\"/' $f"
    $A shell "run-as $PKG grep -q gps_lost_s $f" || \
      $A shell "run-as $PKG sed -i 's|</map>|    <int name=\"gps_lost_s\" value=\"$1\" />\\n</map>|' $f"
  else
    $A shell "run-as $PKG sh -c 'mkdir -p shared_prefs; echo \"<?xml version=\\\"1.0\\\" encoding=\\\"utf-8\\\" standalone=\\\"yes\\\" ?><map><int name=\\\"gps_lost_s\\\" value=\\\"$1\\\" /></map>\" > $f'"
  fi
}
contem() { # espera um texto que contenha $1 (até N tentativas de 0,5 s)
  for _ in $(seq 1 "${2:-30}"); do dump; grep -q "text=\"[^\"]*$1" "$U" && return 0; sleep 0.5; done
  echo "NAO APARECEU: $1"; return 1
}
toca_xy() { # toca no centro da N-ésima ocorrência (1, 2…) do texto exato $1
  dump
  local b; b=$(python3 - "$1" "${2:-1}" "$U" <<'PY'
import re, sys
alvo, n, t = sys.argv[1], int(sys.argv[2]), open(sys.argv[3]).read()
achados = [m.group(0) for m in re.finditer(r'<node [^>]*>', t) if f'text="{alvo}"' in m.group(0)]
if len(achados) >= n:
    x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', achados[n - 1]).groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
)
  if [ -n "$b" ]; then $A shell input tap $b; echo "tap $1 ($2)"; else echo "NAO ACHEI: $1 ($2)"; fi
}
horario_da_lista() { # o N-ésimo horário (HH:MM) de "Próximas partidas"
  dump
  python3 - "${1:-3}" "$U" <<'PY'
import re, sys
n, t = int(sys.argv[1]), open(sys.argv[2]).read()
t = t[t.find('text="Próximas partidas"'):]
hs = re.findall(r'text="(\d\d:\d\d)"', t)
print(hs[n - 1] if len(hs) >= n else "")
PY
}
ao_inicio() { # volta até a barra de baixo e abre o Início
  for _ in 1 2 3 4 5; do wait_text "Início" 2 >/dev/null && break; $A shell input keyevent BACK; sleep 1.5; done
  tap "Início" >/dev/null; wait_text "Para onde você vai? ›" 10
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
# App recém-instalado: sem isso, os pedidos de permissão aparecem no meio da gravação.
for perm in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION POST_NOTIFICATIONS; do
  $A shell pm grant $PKG android.permission.$perm 2>/dev/null || true
done
gps_lost 300
hora 0725; demo 0725
for _ in 1 2 3; do gps -38.5363 -3.7330; sleep 1; done
$A shell monkey -p $PKG 1 >/dev/null 2>&1
wait_text "Para onde você vai? ›" 40
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
H=$(horario_da_lista 3)
tap "$H"; contem "Aviso às"; sleep 4
tap "$H"; sleep 1.5                     # tocar de novo tira o aviso
ao_inicio; sleep 1.5

marca destino
tap "Para onde você vai? ›"; wait_text "Estação ou lugar (ex.: Rodoviária)"; sleep 1.5
$A shell input text "Parangaba"; sleep 2
toca_xy "Parangaba" 2                   # a 1ª ocorrência é o próprio campo
wait_text "Iniciar modo viagem"; sleep 3
marca iniciar
tap "Iniciar modo viagem"; wait_text "Como você quer ir?"; sleep 2.5
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
