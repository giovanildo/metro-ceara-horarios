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
  aviso da hora de sair e a viagem São Benedito → Parangaba pelo caminho da
  1.94+ ("Para onde você vai?", Iniciar modo viagem, Direto, trilho,
  notificação fixa, tela bloqueada, aviso de descida, resumo da chegada), simulando relógio e GPS a
  cada estação, e anota em `marcas.txt` o segundo em que cada cena começa. O
  "GPS perdido" vai a 5 min durante a gravação, para os saltos de relógio não
  mostrarem "Sem GPS", e volta a 1 min no fim. `DIA=MMDD` escolhe o dia: use
  um dia cuja grade o app já baixou, senão aparece o aviso de grade de outro
  dia.
- `gravar_widget.sh` — a cena do ↻ do widget (Parangaba → São Benedito, com
  "Procurando onde você está…" no próprio widget), em `widget.mp4` e
  `marcas-widget.txt`. Precisa do widget na tela inicial. Grave **antes** da
  viagem: o relógio só anda para a frente (07:20, depois 07:25). Depois de
  muitos saltos de relógio o GPS do emulador pode parar de aceitar posições
  (`dumpsys location` com a mesma posição de GPS antiga): reinicie o emulador.
- `build_divulgacao.py` — `metro-ceara-divulgacao.mp4` (~32 s), cortado dessa
  gravação pelas marcas, com o cartão final do QR da Play Store. O resumo da
  chegada é o último quadro da gravação: o script o repete por 5 s.

No emulador Android 16 (`metrofor37`, nov/2026), o `screenrecord` morre
(`INVALID_LAYER_STACK`) quando a tela desliga e para de gravar no desbloqueio:
`gravar_viagem.sh` grava em três partes (até desligar, da tela bloqueada até
desbloquear, e depois) e as junta, com preto no intervalo da tela desligada,
para as marcas continuarem valendo. Na tela de bloqueio a tela apaga sozinha em
~10 s; uma tecla `WAKEUP` a mantém acesa. As notificações do próprio emulador
(teclado físico, console serial) somem da cortina com
`cmd notification snooze --for 2592000000 '<key>'` (as chaves em
`dumpsys notification --noredact`), por 30 dias, porque o relógio da gravação
anda alguns dias para a frente.
- `build_fgs.py` — `tools/play/video-modo-viagem.mp4`, o vídeo da declaração
  do serviço em primeiro plano na Play Store, das duas gravações (modo viagem
  e ↻ do widget, os dois usos do serviço), com legendas em inglês (para a
  análise da Google) e português. Na legenda, nada de ↻: a fonte não tem o
  símbolo.

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
VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_widget.sh
VIDEO_WORK=/tmp/metro-video bash tools/video/gravar_viagem.sh
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_divulgacao.py
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_fgs.py
VIDEO_WORK=/tmp/metro-video python3 tools/video/build_iphone.py
```

## Capturas (emulador)

As telas vêm do emulador Android, com o sistema em pt-BR
(`adb root; adb shell setprop persist.sys.locale pt-BR; adb shell setprop ctl.restart zygote`):

- **Versão web:** abrir `giovanildo.github.io/metro-ceara-horarios/app/`
  no Chrome (ou, para mostrar mudanças ainda não publicadas,
  `python3 -m http.server 8000 -d docs` + `adb reverse tcp:8000 tcp:8000` e
  `http://localhost:8000/app/`, que conta como site seguro para a localização) e salvar `adb exec-out screencap -p` em
  `$VIDEO_WORK/ios/raw_inicio.png` (antes da localização) e
  `raw_estacoes.png` (depois, com `adb emu geo fix -38.5363 -3.7330`).
- **Capturas da loja** (`tools/play/imagens/tela-*.png`, refeitas na 1.58):
  emulador sem internet (`svc wifi disable; svc data disable`), relógio manual
  em quinta 29/10 às 07:25 (`settings put global auto_time 0; date 102907252026.00`),
  com a grade de dia útil desse dia já baixada e `synced_at_weekday` trocado
  para as 05:10 em `shared_prefs/metrofor_schedules.xml` (copiar como root com
  `cat >`, para manter o dono do arquivo), sem estação fixada, posição a ~200 m
  da São Benedito por um provedor de teste
  (`cmd location providers add-test-provider fused` +
  `set-test-provider-location fused --location -3.7330,-38.5363`) e a barra de
  status no modo demo (`clock -e hhmm 0725`). A de Parangaba é às 10:00. O aviso
  de descida sai de uma viagem real São Benedito → Parangaba avançando relógio
  e posição estação por estação até Couto Fernandes; o cartão expandido da
  notificação é recortado da barra de notificações e colado no topo. Todas são
  cortadas de 1080×2400 para 1080×1920.
  **Na 1.60** a 1, a 2 e a 6 (tela inicial com "Para onde você vai?", estação
  com "Ir daqui para outra estação" e o resultado São Benedito → Papicu) foram
  refeitas com `wm size 1080x1920` em vez do corte, para a folha do resultado
  caber inteira (depois, `wm size reset`). O ícone de GPS da barra de status e a
  barra de gestos foram cobertos com a cor do fundo, com Pillow.
  **Na 1.61** a 5 (janela "Avisar a hora de sair", partida das 07:50 em São
  Benedito, 15 min) foi refeita do mesmo jeito, e **na 1.63** de novo (o toque no
  07:50 já marcando o aviso, com o sininho e a confirmação embaixo). Como o emulador já tinha a grade
  de Finados (02/11) como a de dia útil, `no_backup/schedules/weekday.db` foi
  trocado pelo `no_backup/metrofor.db` (a grade de dia útil do APK, com os mesmos
  horários de 29/10) e `date_weekday`/`synced_at_weekday` de
  `metrofor_schedules.xml` voltaram para 29/10 às 05:10; depois, a grade de hoje
  foi devolvida.
  **Na 1.96 (10/11)** a 1, a 2, a 5, a 6 e o destaque foram refeitas no
  `metrofor37` com `wm size 1080x1920`, sem internet e com o relógio numa terça
  **depois** do Enem (17/11 às 07:25; a 2 às 10:00), para nenhum aviso de
  operação especial aparecer: em `metrofor_schedules.xml`, `date_weekday` e
  `normal_date_weekday` passaram a 2026-11-17 e `synced_at_weekday` às 05:10
  desse dia (com `run-as … sed -i`, que mantém o dono), e voltaram depois. A 6 é
  São Benedito → Papicu (destino digitado; a 2ª ocorrência de "Papicu" é a
  estação). A 3, a 4 e a 7 continuam as de antes.
- **App Android (gravação):** `adb shell screenrecord`, o modo demo do
  SystemUI para a barra de status, e a viagem simulada com `date` +
  `emu geo fix` a cada estação: é o que `gravar_viagem.sh` faz.

- `gravar_instalacao.sh` + `build_instalacao.py` — "Como instalar no
  Android" (`metro-ceara-como-instalar.mp4`, até 60 s, e o vídeo e a capa de
  `docs/video/`): a gravação desinstala o app, limpa o Chrome e os downloads
  (pelo MediaStore: com `rm` sobra o registro e o instalador acusa "problema
  ao analisar o pacote") e faz a instalação real pelo site, do botão de baixar
  até a permissão de localização. Como a página ainda mostra a capa do vídeo
  antigo, a capa nova é trocada só naquele Chrome pelo DevTools
  (`cdp.js`, com `adb forward tcp:9222 localabstract:chrome_devtools_remote`).

Depois do Android 16: ao ligar "Permitir desta fonte", a janela "Quer
instalar esse app?" abre sozinha por cima; um Voltar ali cancela a
instalação.
