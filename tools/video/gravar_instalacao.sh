#!/bin/bash
# Grava no emulador a instalação do app pelo site, como alguém faria no celular:
# página do app → Baixar → Abrir → Configurações → Permitir desta fonte →
# Instalar → Abrir → Usar minha localização.
#
#   VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_instalacao.sh
#
# Saída: $VIDEO_WORK/instalacao.mp4 (screenrecord 1080x2400) e
# $VIDEO_WORK/marcas-instalacao.txt, que build_instalacao.py usa para cortar.
# Precisa: emulador ligado, sistema em pt-BR, internet, node (para trocar a capa
# do vídeo da página, que é o vídeo antigo, pela capa nova).
# Desinstala o app e apaga os dados do Chrome do emulador.
set -e
cd "$(dirname "$0")/../.."
. tools/emulador/ui.sh
VIDEO_WORK=${VIDEO_WORK:-/tmp/metro-video}
mkdir -p "$VIDEO_WORK"
PKG=io.github.giova.metroceara
SITE=https://giovanildo.github.io/metro-ceara-horarios/
MARCAS=$VIDEO_WORK/marcas-instalacao.txt
marca() { echo "$1 $(python3 -c "import time; print(round(time.time() - $T0, 2))")" >> "$MARCAS"; echo "· $1"; }

# Estado de quem nunca instalou: sem o app, Chrome sem permissão de instalar e sem downloads.
$A uninstall $PKG >/dev/null 2>&1 || true
$A shell pm clear com.android.chrome >/dev/null
$A shell appops set com.android.chrome REQUEST_INSTALL_PACKAGES default
$A shell pm clear com.google.android.packageinstaller >/dev/null 2>&1 || true
# Apaga pelo MediaStore: com rm o registro do arquivo fica, e o Chrome abre esse registro velho
# ("Ocorreu um problema ao analisar o pacote").
$A shell "content delete --uri content://media/external/downloads --where \"_display_name LIKE 'metro-ceara%'\"" || true
$A shell "rm -f /sdcard/Download/metro-ceara*.apk"
for _ in 1 2 3; do $A emu geo fix -38.5363 -3.7330 >/dev/null; sleep 1; done
demo "$(date +%H%M)"

# Primeira abertura do Chrome (fora da gravação).
$A shell am start -a android.intent.action.VIEW -d "$SITE" com.android.chrome >/dev/null
for t in "Usar sem uma conta" "Agora não" "Não, obrigado"; do
  wait_text "$t" 10 >/dev/null && { tap "$t"; sleep 2; } || true
done
wait_text "App completo para Android" 30
sleep 2
# A capa do vídeo de instalação na página ainda é a do vídeo antigo: só neste Chrome, troca pela nova.
$A forward tcp:9222 localabstract:chrome_devtools_remote >/dev/null
python3 -c "
import base64; print('document.querySelector(\"video.tutorial\").poster=\"data:image/jpeg;base64,'
  + base64.b64encode(open('docs/video/como-instalar-android.jpg','rb').read()).decode() + '\"')" > "$VIDEO_WORK/capa.js"
node tools/video/cdp.js metro-ceara-horarios "$VIDEO_WORK/capa.js" >/dev/null

$A shell rm -f /sdcard/instalacao.mp4
$A shell screenrecord --bit-rate 12000000 /sdcard/instalacao.mp4 &
REC=$!
sleep 1
T0=$(python3 -c "import time; print(time.time())")
: > "$MARCAS"
marca pagina; sleep 3
tap "App completo para Android"; sleep 3
marca baixar
tap "⬇ Baixar o app completo (Android)"
wait_text "Abrir" 40; sleep 1.5
marca abrir
tap "Abrir"
wait_text "Configurações" 20; sleep 2.5
marca configuracoes
tap "Configurações"
wait_text "Permitir desta fonte" 20; sleep 2
marca permitir
tap "Permitir desta fonte"
wait_text "Instalar" 20; sleep 2.5
marca instalar
tap "Instalar"
wait_text "App instalado." 60 || wait_text "Abrir" 30; sleep 2
marca instalado
tap "Abrir"
wait_text "Usar minha localização" 30; sleep 3
marca localizacao
tap "Usar minha localização"
wait_text "Durante o uso do app" 20; sleep 2.5
marca permissao
tap "Durante o uso do app"
for _ in 1 2 3; do $A emu geo fix -38.5363 -3.7330 >/dev/null; sleep 1; done
wait_text "São Benedito" 30; sleep 4
marca fim

$A shell pkill -INT screenrecord || true
wait $REC 2>/dev/null || true
sleep 2
$A pull /sdcard/instalacao.mp4 "$VIDEO_WORK/instalacao.mp4" >/dev/null
$A forward --remove tcp:9222 >/dev/null || true
echo "$VIDEO_WORK/instalacao.mp4"; cat "$MARCAS"
