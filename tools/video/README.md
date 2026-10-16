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

```
VIDEO_WORK=/tmp/metro-video FFMPEG=/caminho/ffmpeg python3 tools/video/build_iphone.py
```

## Capturas (emulador)

As telas vêm do emulador Android, com o sistema em pt-BR
(`adb root; adb shell setprop persist.sys.locale pt-BR; adb shell setprop ctl.restart zygote`):

- **Versão web:** abrir `giovanildo.github.io/metro-fortaleza-horarios/app/`
  no Chrome e salvar `adb exec-out screencap -p` em
  `$VIDEO_WORK/ios/raw_inicio.png` (antes da localização) e
  `raw_estacoes.png` (depois, com `adb emu geo fix -38.5363 -3.7330`).
- **App Android (gravação):** `adb shell screenrecord`, o modo demo do
  SystemUI para a barra de status, e a viagem simulada com `date` +
  `emu geo fix` a cada estação (os saltos de relógio fazem aparecer "Sem GPS
  há 1 min": cortar esse trecho).

Os scripts dos dois primeiros vídeos (divulgação e como instalar no Android)
seguiam o mesmo modelo — trechos de `screenrecord` recortados com ffmpeg,
acelerados e encaixados com `phone_frame`, mais os cartões de `card` —, mas se
perderam antes de virem para o repositório. Para refazê-los, partir de
`build_iphone.py` trocando as imagens paradas por trechos de vídeo.

Depois do Android 16: ao ligar "Permitir desta fonte", a janela "Quer
instalar esse app?" abre sozinha por cima; um Voltar ali cancela a
instalação.
