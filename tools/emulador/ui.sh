# Funções para controlar o emulador nos testes e nas gravações. Uso:
#   . tools/emulador/ui.sh
#   tap "Sobre"; txt 10; fix -38.5363 -3.7330; at 0730; demo 0730
# Precisa do Android SDK em ~/Android/Sdk. EMU_WORK: pasta para o dump da tela.
A=~/Android/Sdk/platform-tools/adb
EMU_WORK=${EMU_WORK:-${TMPDIR:-/tmp}/metro-emulador}
mkdir -p "$EMU_WORK"
U=$EMU_WORK/u.xml

# Árvore da tela atual (uiautomator) em $U.
dump() { $A shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $A shell cat /sdcard/u.xml > "$U"; }

# Os primeiros N textos da tela.
txt() { dump; grep -o 'text="[^"]\+"' "$U" | head -"${1:-10}" | tr '\n' ' '; echo; }

# Limites "x1 y1 x2 y2" do elemento com esse texto ou content-desc.
bounds() {
  dump
  python3 - "$1" "$U" <<'PY'
import re, sys
t = open(sys.argv[2]).read()
for m in re.finditer(r'<node [^>]*>', t):
    n = m.group(0)
    if f'text="{sys.argv[1]}"' in n or f'content-desc="{sys.argv[1]}"' in n:
        print(*map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        break
PY
}

# Toca no centro do elemento com esse texto ou content-desc.
tap() {
  local b; b=$(bounds "$1")
  if [ -n "$b" ]; then
    set -- $b "$1"
    $A shell input tap $((($1 + $3) / 2)) $((($2 + $4) / 2)) && echo "tap $5"
  else
    echo "NAO ACHEI: $1"
  fi
}

# Espera um texto aparecer (até N tentativas de 0,5 s).
wait_text() {
  for _ in $(seq 1 "${2:-30}"); do dump; grep -q "text=\"$1\"" "$U" && return 0; sleep 0.5; done
  echo "NAO APARECEU: $1"; return 1
}

# Posição de GPS (longitude latitude), repetida para o app pegar.
fix() { for _ in 1 2 3; do $A emu geo fix "$1" "$2" >/dev/null; sleep 1; done; }

# Relógio do aparelho (precisa de adb root e auto_time 0): at HHMM [SS], no dia de hoje.
at() {
  $A shell "date $(date +%m%d)${1}$(date +%Y).${2:-00}" >/dev/null
  $A shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm "$1" >/dev/null
}

# Barra de status limpa (modo demo do SystemUI) com o relógio em HHMM.
demo() {
  $A shell settings put global sysui_demo_allowed 1
  for c in "enter" "clock -e hhmm $1" "battery -e level 100 -e plugged false" \
           "network -e wifi show -e level 4 -e fully true" "network -e mobile hide" "notifications -e visible false"; do
    set -- $c; local cmd=$1; shift
    $A shell am broadcast -a com.android.systemui.demo -e command "$cmd" "$@" >/dev/null
  done
}

# Captura da tela em $EMU_WORK/<nome>.png.
shot() { $A exec-out screencap -p > "$EMU_WORK/$1.png"; echo "shot $1"; }
