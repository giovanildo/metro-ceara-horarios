# Vídeos de divulgação

Scripts que montam os vídeos verticais (1080×1920) de `divulgacao/`.
Precisam de Python 3 com Pillow e `qrcode`, da fonte Noto Sans
(`/usr/share/fonts/truetype/noto/`) e do ffmpeg (no `PATH` ou em `FFMPEG=`).
O ffmpeg estático de johnvansickle.com funciona sem instalar nada.

- `frames.py` — a base comum: moldura vermelha com legenda e o buraco do
  celular (`phone_frame`), cartões centralizados com texto, passos, ilustração
  de aviso ou QR code (`card`).
- `build_iphone.py` — "Como instalar no iPhone": telas reais da versão web
  dentro de um iPhone desenhado (Safari, Compartilhar, "Adicionar à Tela de
  Início", tela inicial, pedido de localização), marcadas como "ilustração".

- `gravar_viagem.sh` — grava no emulador, numa tomada só, a tela inicial, o
  lembrete da hora de sair e a viagem São Benedito → Parangaba (notificação
  fixa, tela bloqueada, aviso de descida, chegada), simulando relógio e GPS a
  cada estação, e anota em `marcas.txt` o segundo em que cada cena começa. O
  "GPS perdido" vai a 5 min durante a gravação, para os saltos de relógio não
  mostrarem "Sem GPS", e volta a 1 min no fim. `DIA=MMDD` escolhe o dia: use
  um dia cuja grade o app já baixou, senão aparece o aviso de grade de outro
  dia.
- `build_divulgacao.py` — `metro-ceara-divulgacao.mp4`, cortado dessa
  gravação pelas marcas, com o cartão final do QR da versão lite.
- `build_fgs.py` — `tools/play/video-modo-viagem.mp4`, o vídeo da declaração
  do serviço em primeiro plano na Play Store, da mesma gravação, com legendas
  em inglês (para a análise da Google) e português.

Os cortes partem das marcas com pequenos deslocamentos (a demora dos toques e
das notificações); depois de gravar de novo, confira uma folha de quadros
(`ffmpeg -vf "fps=1,scale=120:-1,tile=20x3"`) e ajuste se preciso. Antes de
cortar, a gravação é convertida para 30 quadros/s constantes: o
`screenrecord` só grava quadro quando a tela muda, e numa tela parada o corte
pularia para o próximo.
- `qr_stories.py` — `story-qrcode.png`, `story-qrcode-link.png` (com espaço
  livre de ~1550 a ~1820 px para a figurinha de link do Instagram) e
  `qrcode.png`, todos levando à versão lite.

```
VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_viagem.sh
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_divulgacao.py
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_fgs.py
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_iphone.py
```

## Capturas (emulador)

As telas vêm do emulador Android, com o sistema em pt-BR
(`adb root; adb shell setprop persist.sys.locale pt-BR; adb shell setprop ctl.restart zygote`):

- **Versão web:** abrir `giovanildo.github.io/metro-fortaleza-horarios/app/`
  no Chrome (ou, para mostrar mudanças ainda não publicadas,
  `python3 -m http.server 8000 -d docs` + `adb reverse tcp:8000 tcp:8000` e
  `http://localhost:8000/app/`, que conta como site seguro para a localização) e salvar `adb exec-out screencap -p` em
  `$VIDEO_WORK/ios/raw_inicio.png` (antes da localização) e
  `raw_estacoes.png` (depois, com `adb emu geo fix -38.5363 -3.7330`).
- **App Android (gravação):** `adb shell screenrecord`, o modo demo do
  SystemUI para a barra de status, e a viagem simulada com `date` +
  `emu geo fix` a cada estação: é o que `gravar_viagem.sh` faz.

O script do vídeo "como instalar no Android" se perdeu antes de vir para o
repositório. Para refazê-lo, grave a instalação pelo link com `screenrecord`
e monte como `build_divulgacao.py` (`fr.clip` para os trechos, `fr.card` para
os cartões de problemas e soluções).

Depois do Android 16: ao ligar "Permitir desta fonte", a janela "Quer
instalar esse app?" abre sozinha por cima; um Voltar ali cancela a
instalação.
