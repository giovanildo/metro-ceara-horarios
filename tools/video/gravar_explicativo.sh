#!/bin/bash
# Grava no emulador o explicativo "da Play Store ao próximo metrô":
# parte A, a busca digitada ("metrô ceará", depois "metrô fortaleza") e a
# página do app no Google Play (pelo Chrome: o emulador não tem a Play Store
# nem conta Google); parte B, o app recém-instalado:
# primeira abertura, Pular, Usar minha localização, Durante o uso do app e a
# tela inicial com a próxima partida da estação mais perto (São Benedito).
#
#   VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_explicativo.sh
#   VIDEO_WORK=/tmp/metro-video python3 tools/video/explicativo_instalar.py
#
# Saída em $VIDEO_WORK/explicativo/: loja.mp4, app.mp4, fim.png, achou.png,
# digitacao-1/2.txt ("segundos texto-até-aqui"), marcas-loja.txt e toques-*.txt
# ("segundos x y" de cada toque, contados do início de cada gravação).
# Precisa: emulador ligado, internet, adb root e o APK de debug compilado.
set -e
cd "$(dirname "$0")/../.."
. tools/emulador/ui.sh
OUT=${VIDEO_WORK:-/tmp/metro-video}/explicativo
mkdir -p "$OUT"
PKG=io.github.giova.metroceara
APK=app/build/outputs/apk/debug/app-debug.apk
DIA=${DIA:-1123}   # um dia útil; a grade baixada é a do Metrofor de hoje
INICIO="https://play.google.com/store/apps?hl=pt_BR&gl=BR"
PAGINA="https://play.google.com/store/apps/details?id=io.github.giova.metroceara&hl=pt_BR&gl=BR"

agora() { python3 -c "import time; print(time.time())"; }
rec_start() { $A shell rm -f /sdcard/r.mp4; $A shell screenrecord --bit-rate 12000000 /sdcard/r.mp4 & REC=$!
  sleep 0.8; T0=$(agora); : > "$OUT/toques-$1.txt"; TOQ="$OUT/toques-$1.txt"; }
rec_stop() { sleep 0.5; $A shell pkill -INT screenrecord || true; wait $REC 2>/dev/null || true; sleep 1
  $A pull /sdcard/r.mp4 "$OUT/$1.mp4" >/dev/null; echo "gravado $1"; }
toque() { # x y: toca e anota
  echo "$(python3 -c "import time; print(round(time.time() - $T0, 2))") $1 $2" >> "$TOQ"
  $A shell input tap "$1" "$2"; }
centro() { # texto (ou parte): "x y" do centro
  dump
  python3 - "$1" "$U" <<'PY'
import re, sys, html
alvo, t = sys.argv[1], open(sys.argv[2]).read()
for m in re.finditer(r'<node [^>]*>', t):
    g = m.group(0); tx = re.search(r' text="([^"]*)"', g)
    if tx and alvo in html.unescape(tx.group(1)):
        x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', g).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
}
sem_teclado() { # fecha o teclado, se ficou aberto (o Voltar só fecha o teclado)
  if $A shell dumpsys input_method | grep -q "mInputShown=true"; then $A shell input keyevent BACK; sleep 0.8; fi; }
relogio() { $A shell "date ${DIA}0725$(date +%Y).00" >/dev/null; demo 0725; }

$A root >/dev/null; sleep 1
$A shell settings put global auto_time 0
$A shell svc wifi enable; $A shell svc data enable
$A shell input keyevent WAKEUP; $A shell wm dismiss-keyguard
$A shell pm uninstall $PKG >/dev/null 2>&1 || true
relogio

# Parte A: a busca no Google Play. A digitação é pelo DevTools do Chrome
# (digitar_cdp.js), porque o "input text" do adb não escreve acento.
$A forward tcp:9222 localabstract:chrome_devtools_remote >/dev/null
$A shell am start -a android.intent.action.VIEW -d "$INICIO" >/dev/null
sleep 8
$A shell input tap 540 2200; sleep 1          # fecha a dica do Chrome, se aparecer
$A shell input tap 739 348; sleep 2           # a lupa: abre a busca vazia, com o teclado
rec_start loja
: > "$OUT/digitacao-1.txt"; : > "$OUT/digitacao-2.txt"; : > "$OUT/marcas-loja.txt"
marca() { echo "$(python3 -c "import time; print(round(time.time() - $T0, 2))") $1" >> "$OUT/marcas-loja.txt"; }
sleep 1.2
node tools/video/digitar_cdp.js play.google.com "metrô ceará" "$T0" "$OUT/digitacao-1.txt"
sleep 0.8; marca enter1; $A shell input keyevent ENTER
sleep 5.5; sem_teclado; marca resultado1
# Apaga e busca de novo, agora "metrô fortaleza"
toque 280 338; sleep 1.5                      # o campo com "metrô ceará"
toque 1008 338; sleep 1                       # o X que limpa o campo
node tools/video/digitar_cdp.js play.google.com "metrô fortaleza" "$T0" "$OUT/digitacao-2.txt"
sleep 0.8; marca enter2; $A shell input keyevent ENTER
sleep 5.5; sem_teclado; marca resultado2
$A shell input swipe 540 1900 540 700 500; sleep 1.2
for _ in 1 2 3 4; do $A shell input swipe 900 1000 150 1000 400; sleep 1.0; done
sleep 0.5; marca achou
$A exec-out screencap -p > "$OUT/achou.png"
sleep 2.5                                     # o carrossel ainda desliza: um toque nele só o pararia
toque 66 1370; sleep 0.6                      # o ícone do Metrô Ceará no carrossel
# No carrossel do Play web o toque só destaca o cartão: a página do app abre
# pelo endereço (no vídeo, o corte vai do toque para a página).
marca pagina; $A shell am start -a android.intent.action.VIEW -d "$PAGINA" >/dev/null; sleep 6
xy=$(centro "Instalar"); [ -n "$xy" ] || xy="540 1530"
toque $xy; sleep 0.6
rec_stop loja

# Instalação (não gravada: no emulador não há conta Google para instalar pela loja)
$A shell input keyevent HOME
$A install -r "$APK" >/dev/null
relogio
for _ in 1 2 3; do $A emu geo fix -38.5363 -3.7330 >/dev/null; sleep 1; done

# Parte B: primeira abertura até a próxima partida
rec_start app
sleep 1
$A shell monkey -p $PKG 1 >/dev/null 2>&1; sleep 4
xy=$(centro "Pular"); toque $xy; sleep 2.5
xy=$(centro "Usar minha localização"); toque $xy; sleep 2
xy=$(centro "Durante o uso"); toque $xy
sleep 0.3
rec_stop app
# A tela inicial com a próxima partida fica parada: o screenrecord só grava
# quadro quando a tela muda, então o fim é uma captura (vira trecho parado).
for _ in 1 2 3; do $A emu geo fix -38.5363 -3.7330 >/dev/null; sleep 1; done
sleep 3
$A exec-out screencap -p > "$OUT/fim.png"; echo "capturado fim"
cat "$OUT"/toques-*.txt
