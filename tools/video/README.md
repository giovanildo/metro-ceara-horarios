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

- `reel_vitrine.py` — reel "vitrine" para páginas de bairro, no estilo de
  `referencias/metro ceará.mp4`: fundo escuro, celular com moldura e sombra, título
  com a palavra-chave em vermelho, toque e zoom no detalhe (contorno vermelho) e o
  cartão vermelho da marca no fim. A abertura pode ser uma foto da estação em tela
  cheia (`capturas/fotos/`, crédito no vídeo) com o celular entrando por cima em 1,2 s:
  a referência teve 4 s de tempo médio. Fonte Poppins (OFL) em `fontes/`. Uma entrada por
  estação em `BAIRROS` (`maracanau`, `carlito`); telas em `capturas/instagram/`
  (`maracanau-*.png`: terça 24/11/2026, 06:57 e 07:35, GPS na estação). `--previa` põe a
  folha de quadros em `prints/historias/`.
- `historias.py` — os vídeos de conversa dos dias 1 a 15 e 17 a 22 de
  `divulgacao/instagram-calendario/` (legendas e calendário em `textos.txt`), no
  formato aprovado em 21/11/2026: uma conversa informal num mensageiro genérico
  (sem imitar nenhum app), a tela real do app, da lite ou do site num celular com
  uma bolinha no papel do dedo e um anel amarelo no ponto certo, a resposta na
  conversa e o final com o link. Cada dia é uma lista de cenas em `DIAS`
  (`conversa`, `tela`, `fim`); sai em story 1080×1920 e feed 1080×1350, 11 a 16 s,
  sem som; vermelho só nos avisos (domingo, feriado). Telas em `capturas/instagram/`:
  do app em terça simulada 24/11/2026 (07:25; Casa às 17:30) ou sábado 21/11
  (Mondubim → Iate às 10:00), sem internet, com a grade de dia útil levada a 24/11
  e devolvida; o site e a lite no Chrome do emulador. `python3
  tools/video/historias.py 3 7` gera esses dias; `--previa` põe folha de quadros e
  vídeo leve em `prints/historias/`.
  Reels para páginas de bairro ficam em `BAIRRO` e saem em
  `divulgacao/paginas-do-bairro/`: `python3 tools/video/historias.py carlito` gera o
  da Carlito Benevides (23/11/2026, 10,6 s), com a pergunta no primeiro quadro, a tela
  real da estação (`capturas/instagram/carlito-inicio.png`) antes dos 4 s e o fim
  "Manda pra quem pega o metrô com você" (`conversa(..., zoom=)` e `fim(..., chamada=)`).
- `instagram_app.py` — o carrossel da Nina (dia 16), em roxo (a cor do rostinho no
  app); as funções de carrossel (`capa`, `tela`, `lista`, `final`) ficam para posts
  que precisem ser imagem. A fonte não tem a seta nem o visto: o script desenha.
- `avisar_videos.py` — o dia 24 (Avisar alguém, 1.109/1.110): dois vídeos de 19 s, um
  para o story (1080×1920, legendas fora das faixas que o Instagram cobre, espaço para a
  figurinha de link no fim) e um para o feed (1080×1350). Fundo claro, a tela do app num
  celular e uma bolinha no papel do dedo. A história: o grupo vai de carro do Pirambu e
  espera na fila do Cine São Luiz; você vem de metrô da Maracanaú, responde com "Avisar
  alguém", o app avisa a chegada na José de Alencar e a galera fica sabendo. A conversa é
  genérica (grupo "Cinema hoje"), sem imitar nenhum app. Feitos quadro a quadro em Pillow
  e passados ao ffmpeg, a partir de `avisar-saoluiz-viagem.png` e `avisar-saoluiz-texto.png`
  (21/11, terça simulada 24/11 às 17:45, Maracanaú → Cineteatro São Luiz, 1080×2400; a
  posição foi trocada pelo ↻ da tela inicial e a grade de dia útil, levada a 24/11 e
  devolvida). A notificação de chegada usa o texto do app (`trip_arrived_belongings`).
  Prévia de um instante: `python3 tools/video/avisar_videos.py quadro 4.0`.
- `instagram_site.py` — a tabela app × lite × site do dia 23 (feed e story). As
  funções que fotografavam o site no Chrome sem janela, com o relógio da página
  fixado, e montavam carrosséis e rolagem ficam para posts do site que precisem
  ser imagem (os dias 17 a 22 viraram vídeos de conversa em 21/11).

- `gravar_explicativo.sh` + `explicativo_instalar.py` — o explicativo "da Play
  Store ao próximo metrô" (`divulgacao/explicativo-instalar/`, story e feed,
  ~40 s, tom profissional): a busca digitada "metrô ceará" e "metrô fortaleza",
  letra por letra no campo e em grande numa barra acima do celular, a página
  do app e Instalar, a primeira abertura com Pular, a permissão de localização
  e a próxima partida da estação mais perto. O emulador não tem a Play Store:
  a loja é a página do Google Play no Chrome, e a digitação com acento vai pelo
  DevTools (`digitar_cdp.js`, `Input.insertText`; o `input text` do adb não
  escreve acento). Sugestões e resultados de outros apps saem borrados. No
  carrossel do Play web o toque não abre o app: a página abre pelo endereço
  (corte no vídeo). O `screenrecord` cai quando a janela da permissão fecha e
  não grava tela parada, então o fim é uma captura.

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
  **Na 1.108 (21/11)** a 1, a 2, a 5, a 7 e o destaque foram refeitas do mesmo
  jeito, para mostrar a Nina na barra de baixo, o Esconder de "Seus lugares" e o
  aviso do vagão feminino nas estações da Linha Sul. O dia foi a terça **24/11**
  (o espelho só guarda operação especial de hoje em diante, e a de 20/11 aparecia
  no topo em 17/11); a grade de dia útil era a de 16/11, com `date_weekday`,
  `normal_date_weekday` e `synced_at_weekday` levados a 24/11 e devolvidos depois
  (cópia do XML em `files/` antes de mexer). As regras foram baixadas com
  internet antes de desligá-la. A 5 é a partida das 07:55 (aviso às 07:40), com a
  tela rolada até o vagão feminino; a 7 sai de "Iniciar modo viagem" em São
  Benedito → Parangaba às 10:25. A 4 (widget) continua a de antes: o widget é o
  mesmo. Como a 1, a 2 e a 5 entram no kit da página do bairro,
  `kit_pagina_bairro.py` pode ser rodado de novo.
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
- `natal.py` — seis vídeos de Natal no mesmo formato de conversa (compras no Centro,
  amigo secreto com Avisar alguém, véspera, volta da confraternização com o modo
  viagem, o app de presente e Feliz Natal), em `divulgacao/instagram-natal/` (legendas
  e datas em `textos.txt`). Usa as cenas de `historias.py` e acrescenta pisca-pisca no
  topo, estrelinhas só no fundo livre, gorro no ícone e cartão final. Telas
  `capturas/instagram/natal-*.png`. `python3 tools/video/natal.py 2` gera um dia;
  `--previa` grava em `prints/natal/`.
- `natal_divulgacao.py` — a versão natalina de `referencias/metro ceará.mp4`, com
  pisca-pisca, estrelinhas onde o fundo está livre, gorro e "Boas festas!" no cartão
  vermelho do fim. Mantém o áudio. Sai em story/Reels (9:16) e feed (4:5, com o original
  no centro e as laterais continuando o fundo). `python3 tools/video/natal_divulgacao.py
  quadro 6 23.5` gera prévias em `/tmp`.
