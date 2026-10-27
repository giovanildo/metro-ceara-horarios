# Metrô Ceará — horários

App Android **não oficial** com os horários das linhas do Metrofor (Fortaleza,
Sobral e Cariri). Sempre que é aberto, o app **baixa os horários do dia
publicados pelo Metrofor** (veja [Horários do dia](#horários-do-dia)). Sem internet,
tudo continua funcionando com a última cópia guardada no aparelho e, em último
caso, com a grade que vem no APK.

## Baixar

**Página do app, para divulgar:** <https://giovanildo.github.io/metro-ceara-horarios/>
— explica o que o app faz e como instalar, para quem não é programador, e gera
uma prévia bonita ao colar o link no WhatsApp.

**Na Play Store, em fase de testes:** para ser testador beta, mande um e-mail
para giovanildos@gmail.com. A página do app oferece só esse caminho (e a
versão lite); o APK abaixo fica para quem chega pelo GitHub.

**[⬇ Baixar a última versão (APK)](https://github.com/giovanildo/metro-ceara-horarios/releases/latest)**
— Android 7 ou mais novo. Ao abrir o arquivo, permita instalar apps dessa fonte;
se o Play Protect avisar "app não verificado", toque em *Mais detalhes →
Instalar mesmo assim*. Para atualizar sozinho, use o
[Obtainium](https://github.com/ImranR98/Obtainium) com o endereço deste
repositório.

Para divulgar, há imagens de story (1080×1920) em [`divulgacao/`](divulgacao/):
`story-qrcode.png`, só com o QR code, e `story-qrcode-link.png`, com o QR e o
link escrito e um espaço livre embaixo para a figurinha de link do Instagram. O
QR e o link levam à **versão lite** (`…/metro-ceara-horarios/app/`), que abre
na hora em qualquer celular — a porta de entrada; de lá, quem tem Android chega
ao app completo. Imagem não tem link clicável: no Instagram use a figurinha
"Link"; no status do WhatsApp, poste o link num status de texto. As duas e o
`qrcode.png` saem de [`tools/video/qr_stories.py`](tools/video/qr_stories.py).

Há também sequências com as telas do app e o link da versão lite no rodapé, para o status:
`story-tela-1..3.png` (tela inicial, horários, modo viagem) e
`story-casa-1..4.png`, que conta a ida de casa até o metrô (de Pacatuba, a
1,5 km de Carlito Benevides, até José de Alencar) e termina com o aviso de
descida. No status do WhatsApp, o link escrito na **legenda** da imagem fica
clicável.

E vídeos verticais (1080×1920, sem música — ela entra ao postar, pela
biblioteca do Instagram ou do WhatsApp):
`metro-ceara-divulgacao.mp4` (~55 s: próxima partida, lembrete da hora de
sair, modo viagem com a tela bloqueada até o aviso de descida e a chegada, QR
da versão lite) e `metro-ceara-como-instalar.mp4` (~60 s, cabe no status
do WhatsApp; é também o vídeo da página, em `docs/video/`: os passos reais da
instalação pelo site, com os avisos do Android, e depois os problemas comuns e
a saída de cada um — aviso do navegador, Play Protect, Bloqueador automático
da Samsung, "App não instalado"). As telas foram gravadas no emulador em
pt-BR; os avisos do navegador e do Play Protect, que não aparecem lá, são
ilustrações marcadas como tal.

Para iPhone, `metro-ceara-como-instalar-iphone.mp4` (~39 s) ensina a pôr a
[versão lite](#versão-lite-web) na tela inicial pelo Safari: as telas
do app são reais, as do iPhone são desenhadas e marcadas como "ilustração". Os
scripts que gravam e montam os vídeos e a receita das capturas estão em
[`tools/video/`](tools/video/).

A tela inicial destaca uma estação com a próxima partida de cada sentido — nos
terminais, só o sentido em que dá para partir (o que termina ali só teria
chegadas). A tela de horários de um terminal também mostra só esse sentido. Pelo
GPS, se houver estações de **outras linhas a até 3 km**, cada linha ganha seu
cartão (por exemplo, Chico da Silva na Linha Sul e Moura Brasil na Oeste, a
~200 m uma da outra), e o modo viagem oferece todas como ponto de partida. A
estação em destaque pode vir de duas fontes:

- **A mais próxima de você**, o que exige `ACCESS_FINE_LOCATION`. A permissão só
  é pedida quando você toca no botão, nunca ao abrir o app, e as features de
  localização estão declaradas com `required="false"` — o app instala e funciona
  em aparelho sem GPS.
- **Uma estação fixada à mão**, pela estrela na tela de partidas. Não exige
  permissão nenhuma, e ganha da localização quando existe, porque é uma escolha
  explícita.

Sem permissão e sem estação fixada, o app continua inteiro: o cartão vira um
convite e o resto funciona igual.

A localização usa o `LocationManager` do próprio Android, sem Play Services.
Para achar a posição, o app pede **ao mesmo tempo** ao provedor do Google
(`fused`, o mesmo do Maps, quando o aparelho tem), à rede (Wi-Fi e antenas) e
ao GPS, e fica com a primeira posição boa (até 500 m de erro), esperando até
30 s; sem nada a tempo, usa a última posição guardada de até 30 min. Pedir só
ao GPS fazia a primeira localização falhar em quem acabou de instalar o app —
GPS frio não acha satélites em segundos dentro de casa ou do trem — e só abrir o
Maps destravava. Se mesmo assim falhar, o app continua escutando enquanto a
tela está aberta, e a estação aparece sozinha quando a posição chegar.
Com a tela inicial aberta e a estação vindo do GPS, o app segue a posição
continuamente (a cada 10 s ou 30 m): ao descer em outra estação, o destaque
muda sozinho. Uma estação fixada pela estrela continua tendo prioridade.

### Integração entre linhas

Nas estações de troca de linha, a tela de horários mostra um bloco **"Integração
com"** a outra linha: o nome da estação de lá, o tempo a pé e, em cada sentido, o
primeiro trem **que ainda dá para pegar** (partida depois de agora + caminhada).
Tocar no bloco abre a outra estação já no sentido mostrado. Nos terminais, o
sentido que só chega ali fica de fora. Os pontos vêm da tabela `transfer` do
banco, que o gerador e o download diário montam a partir de
[`app/src/main/assets/regras.json`](app/src/main/assets/regras.json) (seção
`integracoes`):

| Integração | Linhas | A pé |
|---|---|---|
| Parangaba ↔ Parangaba - NE | Sul ↔ Nordeste | 2 min |
| Expedicionários ↔ Expedicionários - AE | Nordeste ↔ VLT Aeroporto | 3 min |
| Chico da Silva ↔ Moura Brasil | Sul ↔ Oeste | 5 min (~200 m) |

**Passagem.** A regra fica no mesmo `regras.json`, em cada integração
(`passagem_a_para_b` / `passagem_b_para_a`: `gratis`, `paga` ou `desconhecida`),
porque não vem do GTFS, e vale **por sentido** (de → para), com um motivo
opcional:

| De → Para | Passagem |
|---|---|
| Chico da Silva → Moura Brasil (Sul → Oeste) | paga |
| Moura Brasil → Chico da Silva (Oeste → Sul) | paga |
| Parangaba → Parangaba - NE (Sul → Nordeste) | **gratuita** — a Linha Nordeste está gratuita durante a implantação |
| Parangaba - NE → Parangaba (Nordeste → Sul) | paga — entrar na estação da Linha Sul cobra |
| Expedicionários ↔ Expedicionários - AE (Nordeste ↔ VLT Aeroporto) | não informada |

Sem regra (`UNKNOWN`) o app não fala de passagem, para não informar errado.
Quando a Nordeste começar a cobrar, basta trocar a entrada que cita o motivo.

### Roteiro com integração (destino em outra linha)

No **Iniciar** do modo viagem, a lista "Para qual estação você vai?" tem uma
**busca** pelo nome ("bor" → Borges de Melo) e, abaixo das linhas em que a
pessoa já está, um grupo fechado para cada linha que só se alcança com
integração ("Linha Nordeste · 10 estações · integração em Parangaba ▾").
O app escolhe o sentido sozinho, e cada estação mostra a chegada prevista
("chega ~08:40"). Ao escolher uma estação de outra linha, abre o **roteiro**:

- cada trecho na cor da linha ("07:53 Benfica · Metrô da Linha Sul, sentido
  Carlito Benevides. Desça em Parangaba às 08:01.");
- a integração com o tempo de troca da pessoa, a passagem e um selo: **Dá
  tempo** ou **Integração justa** (sobra 1 minuto ou menos). Na justa, o roteiro
  diz quando sai o próximo, caso ela perca;
- a chegada, a dica "o metrô das 07:40 chega no mesmo horário, mas espera
  mais na integração" e os avisos de bike;
- **Outros horários**: um roteiro por horário de chegada, para quem vai sair
  depois.

**Como o roteiro é calculado** ([`data/Router.kt`](app/src/main/java/io/github/giova/metroceara/data/Router.kt),
Kotlin puro, com testes em `app/src/test`). A rede é pequena, com até três
integrações (Caucaia → Aeroporto passa por Chico da Silva, Parangaba e
Expedicionários). O app testa cada trem do primeiro trecho e, em cada
integração, o primeiro trem que ainda dá para pegar. Entre os que chegam no
mesmo horário, sugere **o último que sai**, desde que a integração não fique
justa: saindo do Benfica, o metrô das 07:53 pega o mesmo VLT das 08:10 que o
das 07:40, com 9 min de espera em vez de 22. As partidas possíveis são as
estações perto da pessoa a até 10 min a pé, contando essa caminhada. Também
conta como integração a estação que serve duas linhas (Campo dos Velhos, em
Sobral), com 1 min. Uma linha que o Metrofor avisou que não circula hoje fica
de fora. Sem roteiro possível: "Sem integração para Papicu hoje depois das
22:19".

**Tempo de troca de linha**
([`data/TransferTime.kt`](app/src/main/java/io/github/giova/metroceara/data/TransferTime.kt)).
É o tempo a pé da estação (`caminhada_min`) vezes o jeito de se deslocar da
pessoa (arredondado para cima), mais o pico e o ajuste fino. O que é escolha da
pessoa fica nas **Configurações → Integração**: "Como você se desloca" e
"Minutos a mais em toda integração". O que é medida do sistema fica no
`regras.json`, para corrigir sem versão nova:

| Como você se desloca | Tempo base × | `regras.json` |
|---|---|---|
| Andando (padrão) | 1 | `deslocamento.andando` |
| Sem pressa, com criança ou idoso | 1,5 | `deslocamento.sem_pressa` |
| Com bicicleta (o roteiro confere a regra de bike de cada trecho) | 1,5 | `deslocamento.bicicleta` |
| Cadeira de rodas ou mobilidade reduzida | 2 | `deslocamento.cadeira_de_rodas` |

| Horário | A mais | `regras.json` |
|---|---|---|
| Pico (6h–8h e 16h–19h) | +1 min | `pico.integracao_min` |
| Pico máximo (17h–18h, Relatório de Demanda) | +2 min | `pico.maximo`, `pico.integracao_maximo_min` |

**Na viagem (do começo ao fim, sozinha).** "Iniciar viagem" acompanha o
roteiro inteiro, trecho por trecho, sem a pessoa tocar em nada
([`data/Journey.kt`](app/src/main/java/io/github/giova/metroceara/data/Journey.kt)
e `TripService`):

- **No trecho:** a notificação fixa mostra "Benfica → Papicu · chegada 08:40 ·
  Agora: Linha Sul, em Couto Fernandes · Integração em Parangaba às 08:01 ·
  Linha Nordeste às 08:10", com uma barra de progresso da viagem inteira. O
  cartão no app tem a **linha do tempo** dos trechos ("feito", "agora").
- **Antes da integração:** "Prepare-se: integração em Parangaba · Faltam 2
  estações para Parangaba. Depois, Linha Nordeste às 08:10."
- **Na integração:** "Desça em Parangaba e vá para a Linha Nordeste, 2 min a pé.
  VLT às 08:10, sentido Iate. Passagem grátis." O serviço calcula o próximo
  trecho na hora, com o mesmo `Router`, e passa a acompanhá-lo: "Indo para a
  Linha Nordeste · VLT às 08:10, sentido Iate · em 8 min".
- **Perdeu o VLT:** se, depois de uma integração, o GPS ainda vê a pessoa na
  plataforma **3 min depois da partida**, sem tê-la visto na estação seguinte,
  vem "Você perdeu o VLT das 08:10 · Próximo às 08:50, sentido Iate. Chegada
  em Papicu às 09:20.", com vibração e voz. O roteiro é refeito com o trem
  seguinte, e a chegada se atualiza (`TripEstimator.missedDeparture`, com
  testes).
- **No destino final:** o aviso de descida de sempre e "Você chegou".
- Duas ou três integrações (São Benedito → Aeroporto) seguem igual. A
  **simulação** também passa pelos trechos sozinha.

Os textos usam o artigo certo da linha ("vá para a Linha Nordeste", "o VLT
Aeroporto") e o singular ("Falta 1 estação").

**Versão lite.** Na tela da estação, "Ir para outra linha": um campo com as
estações das linhas alcançáveis (por linha, com a integração) e "Como você se
desloca", que o navegador lembra. Mostra o mesmo roteiro, calculado por
[`docs/app/roteiro.js`](docs/app/roteiro.js) com a mesma regra, e os próximos
horários. Na lite não há modo viagem.

### Widget na tela inicial

Dois widgets, escolhidos na lista de widgets do celular (segurar num espaço
vazio da tela inicial → Widgets → Metrô Ceará):

- **Estação: próximos horários** (4×2): a estação, a linha e os **dois
  próximos** horários de cada sentido, mais o aviso do dia.
- **Estação: próximo horário** (2×2): o próximo de cada sentido, em número
  grande.

A estação é a **fixada com a estrela**; sem ela, **a última aberta** no app;
sem nenhuma, **a mais perto de você** na última vez que o app achou a
localização (a do topo da tela inicial). Sem nenhuma das três, o widget pede
para abrir o app com a localização ligada ou fixar a estação. Não usa GPS nem
internet: os horários saem da grade já baixada e o aviso, do `regras.json`.
Depois do último horário do dia, mostra o primeiro de amanhã ("05:47
amanhã") ou de segunda. O aviso do dia é a notícia da operação especial, "O
Metrofor avisou que esta linha não circula hoje." ou, num domingo sem
operação, "Domingo: metrô e VLT normalmente não circulam." Tocar abre a tela
da estação no app. Segue o tema do celular, claro ou escuro.

O widget ([`widget/StationWidget.kt`](app/src/main/java/io/github/giova/metroceara/widget/StationWidget.kt),
com `RemoteViews`, sem biblioteca nova) se redesenha **um minuto depois de cada
partida mostrada**, com um alarme sem hora exata, que quase não gasta bateria e
não exige permissão. Também se redesenha quando o app baixa a grade ou as
regras, quando a estrela muda e quando outra estação é aberta, e de 30 em 30
minutos, que é o mínimo do Android. A versão lite não tem widget, porque os
navegadores não permitem.

### Modo viagem

O cartão **Modo viagem** fica no topo da tela inicial: a origem é a estação em
destaque (GPS ou fixada) e o destino se escolhe entre as estações seguintes,
nos dois sentidos. O mesmo botão existe na tela de partidas de cada estação.
Durante a viagem, o cartão do topo passa a mostrar o andamento, e um serviço em
primeiro plano, com notificação fixa, segue a viagem com a tela desligada (o
ícone pequeno das notificações é a silhueta do trem, `drawable/ic_notification.xml`:
o Android só usa a forma, e o ícone do app, de fundo cheio, virava um quadrado):

- **Onde o trem está.** Pelo GPS, quando a posição é precisa (≤150 m) e está a
  até 300 m de uma estação do caminho. Cada estação confirmada mede o atraso
  em relação à grade. Quando o sinal some (dentro do trem, no trecho
  subterrâneo), a posição passa a sair **da tabela de horários**, corrigida
  por esse atraso. A tela diz qual das duas fontes está valendo.
  Posições da **rede de celular/Wi-Fi** só contam quando não há posição de
  satélite nos últimos 15 s: elas erram até ~150 m e chegaram a pôr a pessoa
  na estação errada. **No começo da viagem**, até o GPS achar a pessoa numa
  estação, a tabela não passa da estação de partida — o trem pode estar
  atrasado, e a tabela não sabe disso; se o GPS não aparecer no tempo de "GPS
  perdido" (quem começa já no túnel), a tabela volta a andar. Os dois ajustes
  vieram do registro de testes de campo.
- **Quando o GPS é dado como perdido.** Depois de **1 minuto** sem nenhuma
  posição boa (12 tentativas, uma a cada 5 s) — menos que os ~2 min entre duas
  estações. A partir daí o cartão mostra um quadro em destaque, de fundo neutro
  e letra legível (ícone vermelho só para chamar atenção): "**Sem GPS há N
  min** — Posição estimada pela tabela de horários. Confira o nome das
  estações pela janela." A notificação fixa ganha
  "sem GPS há N min", e todo aviso baseado na tabela diz isso: a estação vira
  "Provavelmente Parangaba" e o aviso de descida termina com "(pela tabela de
  horários)". A tabela é a "média" da viagem: os horários programados do
  Metrofor, deslocados pelo atraso que o GPS mediu na última estação vista.
- **Aviso duas estações antes do destino**, com notificação e vibração:
  "**Benfica: prepare-se para descer** · Faltam 2 estações." O
  botão de alto-falante no cartão da viagem liga ou desliga o **som**; com som,
  o aviso usa o áudio de alarme, que toca mesmo com o celular no vibrar.
- **Avisar cada estação (voz)**, opcional e desligado por padrão: a cada
  estação alcançada, uma notificação curta e a voz do sistema (o "Conversão de
  texto em voz" do Android, em pt-BR) dizem o nome da estação ("Padre
  Cícero."). Quantas faltam só entra quando faltam 3 ("Porangabussu. Faltam 3
  estações para Parangaba."): em toda estação ficava repetitivo, e com 2 já vem
  o aviso de descida. Com o som ligado, o aviso de duas estações antes
  também é falado. A voz sai pelo canal de navegação: toca no fone e abaixa a
  música. Sem voz pt-BR instalada no aparelho, fica só a notificação.
- **Notificações para ler no relógio.** Os relógios que espelham as
  notificações do celular (Coros, Xiaomi, Amazfit, Galaxy Watch…) mostram só o
  começo do texto, então o essencial vem primeiro: a estação no título do aviso
  ("Benfica: prepare-se para descer", "Parangaba: prepare-se para a
  integração"), a chegada no título da notificação fixa ("Até Parangaba ·
  chegada ~08:01"), e "(simulação)" e "(pela tabela de horários)" no fim. A voz
  continua com a frase completa.
- **Distância até a partida.** Se a pessoa está a mais de **1 km** da estação de
  partida (ajustável), o app pergunta antes de iniciar: **Simular a viagem**,
  **Esperar o GPS** ou Cancelar.
  - **Simulação:** segue só a tabela de horários e ignora o GPS, com estações,
    aviso de descida, voz, integração e próximo trecho, para testar ou mostrar o
    app de casa. Tudo vem marcado: "Simulação · Linha Sul" no cartão,
    "Até Parangaba · chegada ~08:01 (simulação)" e "Parangaba: prepare-se para
    descer (simulação)" nas notificações, e "Simulação." na voz. O registro de testes marca
    `"simulated": true` no `trip_start`. O trecho seguinte de um roteiro simulado
    também é simulado.
  - **Esperar o GPS:** a viagem de verdade, para quem ainda vai até a estação.
  - **Em qualquer viagem que não é simulação**, a tabela só avança sozinha
    quando o GPS some (o túnel). Com o GPS bom mostrando a pessoa longe de todas
    as estações da viagem, e sem ela ter passado por nenhuma, o app espera em
    "Aguardando o metrô em…". Antes disso, quem começava em casa via a viagem
    "andar" pela tabela e o app dizer "Você chegou" (log de 25/10, a 1,4 km da
    Carlito Benevides, com testes em `TripEstimatorTest`).

  A mais de **3 km** (ajustável), a opção some,
  na tela inicial e na de partidas. A distância vem do GPS da tela inicial ou,
  com estação fixada, da última posição guardada no aparelho; sem posição
  conhecida, o app deixa iniciar.
- **Ir sentado pelo terminal.** Perto de um terminal (até 3 estações,
  ajustável; 0 desliga), indo no sentido contrário a ele, o app pergunta "Como
  você quer ir?": **direto** ou **sentado** — seguir até o terminal, onde o trem
  esvazia, e voltar nele. Mostra os horários dos dois e quanto o sentado custa
  (em geral um intervalo, ~18 min na Linha Sul). No terminal avisa "fique no
  trem, ele volta às HH:MM"; na volta, as estações repetidas são reconhecidas
  como volta (o GPS sempre casa com a próxima ocorrência à frente). O trem não
  sai do terminal antes do horário: adiantamento medido na ida é descartado ali.
  **Só no horário de pico**: a pergunta aparece quando a partida cai das 6h às
  8h ou das 16h às 19h, faixas que concentram ~45% dos passageiros segundo o
  Relatório de Demanda do Metrofor (3º tri/2025); fora delas a viagem começa
  direto. As faixas ficam em `regras.json` (`pico`), junto das outras regras do
  Metrofor.
- O destino pode estar nos **dois sentidos**, tanto pelo cartão do topo quanto
  pelo botão da tela de horários (antes, ali, só o sentido da aba aberta).
- Durante a viagem, o cartão da estação em destaque some da tela inicial: o da
  viagem já diz onde a pessoa está.
- **Lembrete dos pertences** (ligado por padrão; desliga nas Configurações): o
  aviso de descida termina com "Não esqueça seus pertences." e, na chegada, vem
  "Você chegou a X. Confira se está levando tudo." — por notificação e, com o
  som ligado, por voz. Não entra no aviso de cada estação, para não virar ruído.
- A viagem **encerra sozinha ao chegar** — uns 8 s depois, só o tempo de a voz
  terminar de falar — e deixa na barra o aviso "Você chegou a X." (com o
  lembrete dos pertences, se ligado). Sem chegar, encerra 30 min depois da
  chegada prevista.
- Precisa da permissão de localização (o Android não deixa o serviço rodar sem
  ela) e, no Android 13+, pede também a de notificações.

O app mostra a **estação do Bicicletar mais próxima** com nome, número e
distância, em dois lugares (são 253 estações no banco, também offline). A
capacidade de vagas, que o banco também guarda, não aparece: não é quantas
bicicletas há agora e confundia.

- **Na tela inicial**, num cartão próprio logo abaixo do da estação. Com GPS, a
  distância é medida a partir de **você**, até 3 km — acima disso (fora de
  Fortaleza, por exemplo) o cartão some. Com estação fixada, é medida a partir
  da estação, até 600 m.
- **Na tela de partidas** de cada estação, medida a partir dela e escondida
  acima de 600 m, porque a essa distância deixa de ajudar.

Nas linhas Sul e Nordeste e no VLT de Sobral, o app diz também **se dá para embarcar com
bicicleta agora** (no metrô ou no VLT, conforme a linha; o aviso some quando
não há mais partidas no dia), na tela de partidas e no cartão da tela inicial. As
janelas vêm do regulamento
[*Bike é bem-vinda no metrô*](https://www.ce.gov.br/metrofor/wp-content/uploads/sites/75/2023/03/Regula_Bikes_2023.pdf)
(Metrofor, 2023): segunda a sexta das 9h às 15h e a partir das 20h; sábado a
partir das 15h. No **VLT de Sobral** (notícia do Metrofor de 19/08/2025):
segunda a sexta das 9h às 16h30 e das 19h às 23h, sábado a partir das 13h, 2
bikes por viagem em cada linha. A **Linha Oeste** e o **VLT do Cariri** ficam sem
aviso: o Relatório de Demanda do Metrofor (3º tri/2025, p. 20) só registra bikes
na Sul, na Nordeste e em Sobral. Quando as fontes do Metrofor divergem, vale a
mais recente. Dobrável fechada na bolsa conta como bagagem. No
domingo o aviso de bike some, porque nesse dia só há operação especial e a regra
dela não é conhecida — a não ser que o Metrofor a tenha anunciado (abaixo). Num
domingo de operação especial com a grade do dia baixada e sem regra anunciada,
o aviso diz "Operação especial: o Metrofor não informou se dá para embarcar com
bike hoje", em vez de calar.

**Feriados e dias especiais.** Nas operações especiais, o Metrofor anuncia em
notícia a regra da bike do dia: nos feriados, "nas linhas Sul e Nordeste, o
embarque com bicicletas estará liberado entre 9h e 15h"; nos domingos do ENEM e
nas vésperas de Natal e Ano-Novo, "não haverá embarque com bicicletas"; na
eleição de 2024, "a partir das 9h até o encerramento". A tarefa do espelho lê
sozinha essas notícias ([`tools/datas_especiais.py`](tools/datas_especiais.py),
pela API do WordPress do site, notícias dos últimos 45 dias com "bicicletas"),
tira a data (`quinta-feira (19)`, `(20/11)`, `dias 9 e 16 de novembro`,
conferindo o dia da semana), as linhas e as janelas, e publica em
`datas_especiais` do `regras.json` do espelho. Nesse dia, o app e a lite usam a
regra do dia no lugar da do dia da semana, até num domingo ("Hoje não há
embarque com bike no metrô", "Não há mais horário hoje…"). É conservador: notícia
sem data clara ou com regra que ele não entende fica de fora, e vale a regra
normal. Conferido com as 31 notícias de bike do site (2012–2026): acertou as 13
datas especiais de out/2024 a jun/2026. Uma data posta à mão em `datas_especiais`
do `regras.json` ganha da tirada das notícias.

As regras ficam na seção `bike` de
[`app/src/main/assets/regras.json`](app/src/main/assets/regras.json): cada
regulamento tem janelas de horário (`["09:00", "15:00"]`, ou `["20:00", null]` =
até o fim da operação) para dias úteis, sábado e domingo. Há uma tabela **por
linha** (`linhas`, hoje Sul e Nordeste com o regulamento de 2023 e as duas
linhas de Sobral com o de 2025; linha
fora dela não tem aviso) e outra de **exceções por estação** (`estacoes`, vazia
hoje), que vale antes da da linha. Mudou o regulamento ou alguma estação tem
regra própria: é só editar o arquivo e gerar uma versão nova. Ficam fora das
Configurações de propósito — são regra do Metrofor, não preferência de quem usa
o app.

### Endereço, passagem e posição das estações

A tela da estação (no app e na lite) mostra, logo abaixo dos sentidos, o
**endereço** da estação e a **passagem** da linha ("Passagem: R$ 3,60 (meia
R$ 1,80) · dinheiro ou Pix"). Os dois vêm das páginas das linhas no site do
Metrofor e ficam no `regras.json`: `estacoes` (por `stop_id` do GTFS) e
`passagem` (por linha). O VLT Aeroporto não tem página e fica sem.

Na mesma seção `estacoes`, `posicao` corrige as estações que o **GTFS põe no
lugar errado**. Conferindo as cinco linhas com mapas do Google My Maps e com o
OpenStreetMap (as duas fontes concordam, a até 70 m), Fortaleza bate com o
GTFS, mas em Sobral seis estações estavam longe (Novo Recanto a 2 km, Junco a
1,2 km, Grendene, Campo dos Velhos, Cohab II e Alto da Brasília a 220–790 m) e,
no Cariri, a Escola a 270 m. Isso confundia a estação mais perto e o modo
viagem. As posições usadas são as do OpenStreetMap. Cada entrada leva também o
`nome` da estação: se um dia o Metrofor renumerar as estações e o nome não
bater mais, a entrada é ignorada, em vez de ir para a estação errada.

O `regras.json` é a **fonte única** das regras que não vêm do GTFS (bike,
integrações, passagem, endereços, posições corrigidas e horários de pico): o app (`data/Regras.kt`), o
`tools/build_db.py` e a versão lite leem o mesmo arquivo.

**Mudar uma regra não exige versão nova do app.** A tarefa do espelho publica
uma cópia de `app/src/main/assets/regras.json` no ramo `dados` a cada rodada, e
o app a baixa sempre que abre (antes da grade, porque a montagem da grade usa
as integrações) e passa a usá-la na hora; a lite lê a mesma cópia. Então basta
editar o arquivo na `main` (pelo próprio site do GitHub, se quiser) e rodar a
tarefa (Actions → "Dados da versão web" → Run workflow) ou esperar a próxima
rodada: em minutos ou horas os celulares recebem. Um arquivo com erro no
espelho é ignorado: o app segue com a última cópia boa e, sem nenhuma, com a do
APK. Com tudo ilegível, segue sem avisos de bike e de passagem, em vez de fechar.

Um ícone de **engrenagem** no topo abre as **Configurações**, com as distâncias
e os tempos que antes eram fixos no código (`data/Settings.kt`; padrões entre
parênteses):

Na seção **Aparência**: **Tema** — "Como o celular" (padrão), "Claro" ou
"Escuro"; os ícones da barra de status acompanham o tema do app — e **Cores do
papel de parede** (Android 12+, ligado por padrão; desligado, o app usa as
próprias cores, em vermelho). Nas outras seções:

| Ajuste | Padrão | Faixa |
|---|---|---|
| Mostrar outras linhas até | 3 km | 500 m – 5 km |
| Bicicletar perto de você até | 3 km | 500 m – 5 km |
| Bicicletar perto da estação até | 600 m | 200 m – 1,5 km |
| Avisar antes do destino | 2 estações | 1 – 4 |
| Perguntar antes de começar a viagem a partir de | 1 km | 500 m – 3 km |
| Esconder o modo viagem a partir de | 3 km | 1 – 10 km |
| Considerar o GPS perdido depois de | 1 min | 30 s – 5 min |
| Contar o trem que saiu há até | 2 min | 0 – 10 min |
| Encerrar a viagem sozinho depois de | 30 min | 10 – 60 min |
| Oferecer ir sentado pelo terminal até | 3 estações | 0 (não oferecer) – 5 |
| Atualizar sua posição (tela inicial) a cada | 10 s | 5 – 60 s |

**Lembrete na agenda:** em qualquer estação, tocar num horário de hoje que
ainda não passou (nas próximas partidas ou em "Todos os horários", onde esses
ficam em destaque e os que já passaram, apagados) abre a janela "Lembrar na
agenda". O botão "Abrir a agenda" manda um evento para o app de agenda do
celular (`ACTION_INSERT` do `CalendarContract`, em `ui/LeaveReminder.kt`):
"Pegar o metrô das 07:50", começando na hora da partida, com a estação como
local. Quanto tempo antes avisar fica com a agenda: a pessoa escolhe ali, ao
salvar (por exemplo, o tempo que leva até a estação). Não há configuração, e
serve para sair de qualquer lugar, não só de casa. Não usa conta, permissão nem
internet.

O aviso de distância nunca fica maior que o limite em que o modo viagem some
(mexer num ajusta o outro). "Restaurar padrões" volta tudo. Os parâmetros
técnicos do GPS (precisão mínima, raio de estação) continuam fixos em
`data/Trip.kt`, porque mexer neles exige entender o algoritmo.

Um ícone de **informação** no topo abre a tela **Sobre**: o aviso de que o app
não é oficial, de onde vêm os dados, a licença, o **endereço do projeto** por
extenso (toque abre; botão "Copiar link"), a seção **"Versão lite, para
qualquer celular"** (para quem tem iPhone ou não quer instalar: o que ela faz,
que só o modo viagem é exclusivo do app, o endereço e os botões "Abrir a
versão lite" e "Copiar link"), um botão **"Divulgar o app"** que
abre o compartilhamento do Android (WhatsApp etc.) com uma mensagem pronta e o
link da [versão lite](https://giovanildo.github.io/metro-ceara-horarios/app/),
um **QR code** para outra pessoa abrir o app apontando a câmera para a tela
(`res/drawable-nodpi/qr_download.png`, sempre sobre fundo branco). **Não há
pedido de doação dentro do app**: a Play Store restringe pedir dinheiro fora do
sistema de pagamento dela, então o Pix fica só na página do app e na versão
lite.

## Horários do dia

O GTFS do Metrofor é gerado a cada requisição e, pelo que observamos, traz a
**grade do dia em que é baixado**, embora o `calendar.txt` diga que vale todos
os dias:

| Baixado em | Linha Sul |
|---|---|
| quinta, 24/09 | 5h30 – 23h18, 53 viagens por sentido (dia útil) |
| sábado, 03/10 | 5h30 – 17h08, 37 viagens por sentido |
| domingo, 04/10 (eleição) | 7h30 – 18h10 (operação especial) |

Por isso o feed é baixado pela tarefa do GitHub que também alimenta a versão
lite ([`dados-web.yml`](.github/workflows/dados-web.yml)), **quatro vezes por
dia** (01:00, 10:00, 15:00 e 20:00 de Fortaleza; o GitHub chega a atrasar ~5 h, e
o Metrofor já publica a grade do dia logo depois da meia-noite):
se o site do Metrofor falhar de manhã ou a grade mudar durante o dia, a rodada
seguinte corrige. Ela guarda o zip original de cada tipo de dia no ramo
`dados` (`gtfs-dia-util.zip`, `gtfs-sabado.zip`, `gtfs-domingo.zip`), e o
`indice.json` diz a data, o tamanho e a hora de cada um. O app
(`data/ScheduleStore.kt`) baixa dali o zip do tipo de hoje — com certificado
válido, sem falar direto com o site do Metrofor —, monta o banco com o mesmo resultado de
`tools/build_db.py` (`data/GtfsImporter.kt`, uma tradução direta do script, que
gera tabelas idênticas para o mesmo feed) e guarda a última grade de cada tipo
de dia: dia útil, sábado e domingo, com a **data do índice** (não a do
celular). **Baixa sempre que o app abre ou volta para a frente**, sem comparar
com a guardada: são ~30 KB (o zip de dia útil tem ~29 KB, o de sábado ~20 KB, e
o índice 0,5 KB), e assim qualquer mudança chega na abertura seguinte; um
download cortado (app fechado no meio) é refeito na próxima abertura. Sem
internet, o app usa a última grade do mesmo tipo e avisa em vermelho de que dia
ela é. O banco do APK fica como último recurso. Uma linha no topo da tela
inicial diz de onde vêm os horários, sem botão de baixar de novo.

Detalhes que valem saber:

- **Certificado vencido.** O certificado HTTPS de `*.metrofor.ce.gov.br`
  costuma estar expirado. Quem lida com isso é só a tarefa do GitHub (o script
  desliga a verificação, são dados públicos); o app fala apenas com
  `raw.githubusercontent.com`, com a verificação normal do Android — nada que a
  análise da Play Store possa barrar.
- **Nada de trava por "grade menor".** Como a grade muda de verdade de um dia
  para outro, o app só recusa um feed que não dá para ler ou que não tem linhas
  nem estações. Zero viagens é aceito: num domingo comum pode ser exatamente a
  verdade.
- **Domingo.** O aviso de domingo ("normalmente não circulam") aparece a não ser
  que o Metrofor tenha anunciado operação especial para o dia (veja
  **Operação especial**, abaixo). A grade do dia não serve de sinal: num domingo
  comum (18/10/2026) o feed trouxe a grade de dia útil inteira, e no da eleição
  (25/10) uma grade própria.
- **Depois da última viagem do dia**, as partidas de "amanhã" vêm da grade do
  tipo de dia de amanhã, e não da de hoje repetida: num domingo de eleição,
  segunda mostra a grade de dia útil. Se amanhã for sábado e essa grade nunca
  tiver sido baixada, o app não inventa horário. **Se amanhã for domingo**, a
  lista não mostra horários de amanhã (metrô e VLT normalmente não circulam,
  embora o GTFS diga que sim).
- **Operação especial (feriado, domingo, ENEM, eleição).** O Metrofor publica
  uma notícia para todo dia com horário diferente do normal, e é ela que
  aparece, abaixo da linha "Horários de hoje…": "**Metrofor, hoje:** Eleições
  2026: Veja os horários do metrô e VLTs em operação gratuita de 2º turno ›" (e
  "**Metrofor, amanhã:**" na véspera). Tocar abre a notícia no navegador. Num dia
  sem operação anunciada não aparece nada: notícias sem relação com o horário
  (campanhas, obras, balanços) ficam de fora. A tela da estação também mostra a
  do dia, e numa linha que a notícia diz que não circula ("Não haverá operação
  no VLT de Sobral e no VLT do Cariri") aparece "O Metrofor avisou que esta
  linha não circula hoje.", sem o aviso de bike. As partidas continuam visíveis.

  A tarefa do espelho lê as notícias dos últimos 45 dias pela API do site
  ([`tools/operacoes.py`](tools/operacoes.py)) e publica em `operacoes` do
  `regras.json`: por data, a manchete, o link, se o horário segue normal e as
  linhas paradas. O leitor pega só títulos de operação ("horários especiais",
  "operação especial", "funcionamento normal", "não altera horários"), deixa de
  fora mudança que vale dali em diante ("novos horários", "passa a operar",
  "volta a operar") e tira as datas de "(21/4)", "19 de março", "sexta-feira
  (7)" ou "Quarta-feira de Cinzas (18)", conferindo o dia da semana. Separa o
  que cada data diz: na Semana Santa de 2024, "na quinta-feira, todas as linhas
  funcionam normalmente" não vira aviso, e só a sexta fica especial. Também
  entende as tabelas do Carnaval ("SOBRAL E REGIÃO DO CARIRI / Terça - 13/2/2024
  Não haverá operação"). Quando duas notícias falam do mesmo dia, vale a que
  trata dele (o feriado de 15/11/2025, e não a do ENEM, que só cita a véspera),
  e as linhas paradas das duas se somam. **Conferido com todas as 1.168
  notícias do site (2008–2026):** desde 2022, as 68 notícias de operação caem
  nas 101 datas certas, incluindo os sábados de encerramento antecipado, como
  as vésperas do ENEM e os dois sábados da eleição de 2026. É conservador: sem
  data clara, a notícia fica de fora. Uma data posta à mão em `operacoes` do
  `regras.json` ganha da tirada das notícias.

  O `noticias.json` do espelho, com as últimas notícias sem filtro, continua
  sendo publicado só para quem ainda tem o app até a versão 1.45.
- **Sem mais partidas hoje**, além da frase, aparece o último horário de hoje e
  o primeiro do próximo dia com metrô — amanhã, ou a segunda quando amanhã é
  domingo: "Último de hoje: 17:12 · primeiro de segunda: 05:44 (domingo, só em
  operação especial)" (tela inicial,
  em cada sentido, e tela da estação; `ScheduleRepository.dayEnd`). A versão
  lite faz igual.
- **Download ruim nunca substitui a grade boa.** A grade nova é montada num
  arquivo temporário e só entra no lugar da atual se passar por três checagens:
  o tamanho recebido bate com o anunciado pelo servidor e com o do índice
  (conexão que cai no meio pode entregar menos bytes sem erro); o zip está completo (o índice no fim do
  arquivo existe e promete o mesmo número de arquivos que foram lidos); e o
  banco montado tem linhas e viagens. Qualquer falha mantém a cópia que já
  existe, e o app tenta de novo depois.
- **Banco corrompido não derruba o app.** Antes de abrir um banco, o app confere
  se ele tem tamanho e as tabelas de horário. Uma cópia ruim do banco do APK é
  refeita a partir do APK; uma grade baixada ruim é descartada, e o app usa a do
  APK até baixar de novo. (Já vimos, no emulador, a cópia do APK ficar com 0
  bytes e o app fechar ao abrir — sem descobrir como se formou.)
- **Bicicletar** não vem do GTFS: as estações são copiadas do banco do APK.
- **Gere o banco do APK num dia útil.** Rodar `tools/build_db.py` num sábado ou
  domingo embarcaria a grade daquele dia como reserva para todos os dias.

## Como os dados chegam aqui

O Metrofor publica um feed [GTFS](https://gtfs.org) em
`https://info.metrofor.ce.gov.br/gtfs_file` (linkado na
[página oficial de GTFS](https://www.ce.gov.br/metrofor/gtfs/)).

`tools/build_db.py` baixa esse feed e o converte num SQLite achatado para a
consulta que o app faz o tempo todo — *"quais as próximas partidas nesta
estação, nesta linha, neste sentido?"*:

```bash
python3 tools/build_db.py            # baixa o feed mais recente
python3 tools/build_db.py feed.zip   # ou usa um zip local
```

A saída é `app/src/main/assets/metrofor.db` (~200 KB), com 7 linhas,
64 estações e 3.877 partidas. É a reserva que vai no APK; no dia a dia o app
usa a grade que ele mesmo baixa.

Junto do banco o script grava `metrofor.db.version`, com o carimbo de geração.
O app lê esse arquivo minúsculo direto dos assets a cada arranque e só reinstala
o banco quando o carimbo muda — então basta rodar o gerador e recompilar, sem
precisar mexer no `versionCode`.

O script também normaliza a capitalização, porque o feed vem todo em Title Case
(`Vlt Sobral`, `Chico Da Silva`, `Cohab Iii`).

## Mantendo os dados atualizados

`tools/check_updates.py` compara as fontes com o último estado conhecido e diz
o que mudou:

```bash
python3 tools/check_updates.py           # compara e informa
python3 tools/check_updates.py --notify  # avisa na área de trabalho
```

Ele compara o **conteúdo** das fontes, nunca os bytes: o Metrofor gera o zip do
GTFS a cada requisição, então o arquivo muda sempre e o conteúdo quase nunca.
Comparar o arquivo daria alarme falso toda semana.

Para agendar isso semanalmente, num timer de usuário do systemd (sem root):

```bash
bash tools/install-weekly-check.sh            # instala e ativa
bash tools/install-weekly-check.sh --remove   # desfaz
```

Roda segunda-feira às 10h e, com `Persistent=true`, recupera a execução se a
máquina estiver desligada na hora. Quando algo mudar, aparece uma notificação —
aí é rodar `tools/build_db.py` e recompilar. Com o download diário no celular,
isso só serve para renovar a reserva do APK; como o conteúdo muda conforme o
dia da semana, espere avisos quando a checagem não cair num dia útil.

## Limitações conhecidas dos dados

Estas vêm do feed oficial, não do app:

1. **O `calendar.txt` tem um único serviço valendo os sete dias da semana.** O
   feed não distingue dia útil, sábado e domingo, então o app mostra o mesmo
   quadro todo dia e exibe um aviso na tela de horários. Se você conferir os
   quadros publicados e eles divergirem, é preciso modelar os dias à mão.
   **Domingo é o caso mais grave:** metrô e VLTs normalmente não circulam, a
   não ser em operação especial (eleição, ENEM, eventos), mas o feed diz que
   sim. Por isso, aos domingos, o app põe um aviso em destaque na tela inicial
   e na de partidas. As partidas continuam visíveis como referência para os
   dias de operação especial.
2. **O `calendar_dates.txt` está desatualizado**: lista feriados de 2025,
   enquanto o feed diz valer até 2027. O app ignora esse arquivo.
3. **Não há `transfers.txt`.** As três baldeações reais foram derivadas por
   distância entre estações de linhas diferentes e estão declaradas em
   `app/src/main/assets/regras.json`, que o gerador lê:
   - Parangaba ↔ Parangaba-NE (Linha Sul ↔ Linha Nordeste)
   - Expedicionários ↔ Expedicionários-AE (Linha Nordeste ↔ VLT Aeroporto)
   - Chico da Silva ↔ Moura Brasil (Linha Sul ↔ Linha Oeste, ~200 m a pé)

   O app as mostra no bloco "Integração com", na tela de horários.
4. **Não há GTFS-RT.** Tudo aqui é horário programado: o app não sabe de
   atrasos nem da posição real dos trens.
5. **O Bicicletar não tem disponibilidade em tempo real.** O GeoJSON da AMC traz
   posição e capacidade de vagas, nunca quantas bicicletas estão na estação
   agora — por isso o app mostra só a distância. O sistema não está no registro oficial do
   [GBFS](https://github.com/MobilityData/gbfs) — só Brasília, no Brasil — e o
   site oficial bloqueia acesso automatizado (HTTP 403). O arquivo usado é de
   22/07/2025.
6. **Não há dados de ônibus.** A integração temporal da Etufor é uma regra
   tarifária, não um horário; o GTFS de ônibus de Fortaleza é
   [publicado à parte](https://dados.fortaleza.ce.gov.br/dataset/?tags=gtfs).
7. **Posições erradas em Sobral e no Cariri.** Veja
   [Endereço, passagem e posição das estações](#endereço-passagem-e-posição-das-estações):
   o app corrige pelo `regras.json`.
8. **Nos dias úteis, a Linha Norte de Sobral só tem um sentido.** A grade traz
   as viagens para Cohab III, mas não as de volta para Novo Recanto (sábado e
   domingo têm os dois). O app não inventa horários: mostra a volta como
   "horários não publicados". Vale para qualquer linha que venha com um sentido só.
9. **Linha sem viagens no dia.** O VLT Aeroporto aparece no GTFS de sábado sem
   nenhuma viagem; o app e a lite mostram "não opera hoje" no lugar do número
   de estações.
10. **As páginas das linhas não batem com a grade no sábado.** Sul, Oeste,
   Nordeste e Sobral dizem "segunda a sábado até 20h/23h", mas a grade de
   sábado acaba à tarde (de 15h a 17h). O app segue a grade, que o Metrofor
   publica para o dia.
11. O domínio `metrofor.ce.gov.br` está com **certificado HTTPS expirado** e
   redireciona para `ce.gov.br/metrofor`. Por isso os scripts desligam a
   verificação de certificado; o app não fala com o Metrofor, baixa o mesmo
   feed do espelho no GitHub (veja [Horários do dia](#horários-do-dia)).

## Build

O jeito mais curto, a partir da raiz do projeto:

```bash
bash tools/setup-dev.sh
```

O script instala **JDK 21**, **Gradle 9.8** e o **Android SDK 37** dentro do seu
diretório de usuário (`~/.local/opt` e `~/Android/Sdk`), sem usar `sudo` e sem
tocar no sistema, confere o checksum de cada download, gera o
`gradle-wrapper.jar` e compila o APK de debug
em `app/build/outputs/apk/debug/`.

Ele é idempotente: se você já tiver um JDK 21 (por exemplo via
`sudo apt install openjdk-21-jdk`), ele reaproveita e pula a etapa.

Os testes do cálculo de roteiros (`app/src/test`, sem Android, com os horários
reais de Benfica → Papicu) rodam com:

```bash
./gradlew testDebugUnitTest
```

Em terminais novos, carregue o ambiente antes de usar `gradle` ou `adb`:

```bash
. ~/.local/opt/android-env.sh
./gradlew assembleDebug
```

Abrindo em Android Studio (Ladybug ou mais novo), nada disso é necessário: ele
resolve o SDK e o wrapper sozinho.

## Estrutura

```
tools/build_db.py                    GTFS oficial -> SQLite embarcado
tools/build_web_data.py              espelho: grade do dia, Bicicletar e regras.json no ramo dados
tools/datas_especiais.py             regra de bike de dias especiais, das notícias do Metrofor
tools/operacoes.py                   dias de operação especial e linhas paradas, das notícias
app/src/main/assets/metrofor.db      banco gerado (não editar à mão)
app/src/main/assets/regras.json      regras fora do GTFS: bike, integrações, passagem,
                                     endereços, operações especiais
app/src/main/java/.../data/          modelos, abertura do banco, consultas, relógio
app/src/main/java/.../ui/            telas Compose: linhas -> estações -> partidas
app/src/main/java/.../widget/        widget da tela inicial (RemoteViews)
```

O app não usa Room nem navigation-compose: são três telas e cinco consultas
SQL, e o banco é somente-leitura.

O build usa **AGP 9**, que compila Kotlin nativamente — por isso não existe
plugin `org.jetbrains.kotlin.android` aqui, só o do compilador do Compose.
`compileSdk` está em 37 para acompanhar as bibliotecas, mas `targetSdk` ficou
em 36 de propósito: subir o `targetSdk` muda comportamento em tempo de
execução, e o app ainda não foi testado em aparelho. É a origem do único aviso
de lint que sobrou.

## Registro de testes de campo (só para desenvolvimento)

Escondido de quem usa o app: na tela **Sobre**, toque 7 vezes na linha da
versão para liberar as **Opções de desenvolvedor**. Lá dá para ligar o
**Registro de testes**, ver quanto está guardado, **salvar na pasta Downloads**
e apagar. Desligado por padrão; quando desligado não grava nada.

Ligado, grava um evento por linha (JSON) em `files/devlog/AAAA-MM-DD.jsonl`,
no fuso de Fortaleza, guardando no máximo 7 dias e 5 MB (sai o mais antigo).
O botão de salvar junta tudo em `Download/metro-ceara-log-AAAA-MM-DD.log`
(no Android 9 ou anterior, na pasta do app em `Android/data/…/files/Download`).
Sem pasta escolhida, nada sai do aparelho sozinho: o arquivo é copiado pelo
cabo ou pelo gerenciador de arquivos.

**Cópia na nuvem** (`data/DevLogCloud.kt`), como o backup do gioBank: em
"Escolher pasta", o seletor de pasta do Android dá ao app permissão para uma
pasta — de preferência no Google Drive; o app do Drive cuida de mandar para a
nuvem, sem login nem biblioteca da Google. O app mantém lá **um arquivo por
dia**, `metro-ceara-log-AAAA-MM-DD.log`, regravado com o conteúdo do dia (o
arquivo local continua sendo a fonte), no máximo uma vez por minuto — durante a
viagem chegam eventos a cada segundo — e na hora em `app_start`, `log_on`,
`trip_end`, `sync_ok` e em "Enviar agora". **Se não der para gravar no arquivo
do dia, cria o mesmo nome com `-2`** (depois `-3`…) e passa a usar esse. A tela
mostra a pasta, a última cópia e o erro, se houver. Se o Drive não aparecer no
seletor de pasta, qualquer pasta do celular funciona igual.

Eventos (`"ev"`): `app_start` (versão, Android, aparelho), `schedule_source`
(de onde veio a grade em uso), `sync_start`/`sync_ok`/`sync_fail` (download dos
horários; o `sync_ok` resume viagens e horários por linha), `loc_request`/
`loc_ok`/`loc_fail` (pedido de localização, provedor, tempo, precisão),
`home_gps` (posição exata e estação mais próxima na tela inicial),
`trip_start` (linha, paradas e horários previstos, aviso), `trip_gps` (cada
posição durante a viagem), `trip_progress` (mudança de estação, se veio do GPS
ou da tabela, atraso, GPS sem sinal), `trip_alert`, `trip_station`,
`trip_turnaround`, `trip_end` (motivo), `crash` (erro que derrubou o app) e
`log_on`/`log_off`.

## Versão lite (web)

Para quem tem iPhone (e para quem prefere não instalar APK) há uma versão
web instalável (PWA), a **versão lite**, neste mesmo repositório. Fica de
fora de propósito o que depende do celular em segundo plano: modo viagem, voz,
registro de testes — esses ficam só no app de Android. O navegador não consegue baixar
o GTFS direto do Metrofor — o certificado costuma estar vencido e o site não
libera acesso a partir de outras páginas —, então os horários chegam prontos:

- [`tools/build_web_data.py`](tools/build_web_data.py) baixa o feed e gera JSON,
  reaproveitando o tratamento do `build_db.py` (nomes, cores, Bicicletar) e as
  integrações do `regras.json`. Como no app, guarda **uma grade por tipo de
  dia** (`horarios-dia-util.json`, `horarios-sabado.json`,
  `horarios-domingo.json`), atualizando só a do dia; `indice.json` diz a data
  de cada uma. Feed vazio ou quebrado não substitui a grade boa. Cada sentido
  traz a ordem das estações e as viagens como listas de minutos alinhadas a
  ela (`null` = não para): a grade de um dia útil inteiro tem ~23 KB. Junto
  vai o zip original (`gtfs-<tipo>.zip`), que o **app Android** baixa.
- A tarefa [`dados-web.yml`](.github/workflows/dados-web.yml) roda o script
  quatro vezes por dia (01:00, 10:00, 15:00 e 20:00 de Fortaleza; ou à mão, em Actions → "Dados da versão
  web" → Run workflow) e publica o resultado no ramo **`dados`**, fora da
  `main`, para não encher o histórico do app. A web lê de
  `raw.githubusercontent.com/giovanildo/metro-ceara-horarios/dados/…`.
  O commit diário também mantém a tarefa viva (o GitHub desliga tarefas
  agendadas de repositórios parados há 60 dias).

Para gerar localmente: `python3 tools/build_web_data.py /tmp/dados`.

A versão web em si fica em [`docs/app/`](docs/app/) e é publicada pelo mesmo
GitHub Pages da página do app, em `giovanildo.github.io/metro-ceara-horarios/app/`.
É HTML, CSS e JavaScript puros, sem framework nem etapa de build:

- `dados.js` — download dos JSON (com a última cópia guardada no aparelho, para
  abrir sem internet), relógio sempre no fuso de Fortaleza, grade do tipo de
  dia, próximas partidas (acabando as de hoje, seguem as de amanhã; com amanhã
  domingo, o fim do dia mostra a segunda) e
  distância. É a tradução das mesmas regras do app.
- `app.js` — as telas: início (estação mais perto de cada linha até 3 km, com a
  próxima partida em cada sentido; a localização só é pedida ao tocar no
  botão), linhas, estação (abas por sentido, sem o sentido que termina nela;
  endereço e passagem; próximas partidas e todos os horários) e configurações.
- **Integração** e **bike**, com as mesmas regras e frases do app (lidas do
  `regras.json` publicado no ramo `dados`): na tela da estação, o próximo
  metrô/VLT da outra linha que ainda dá para pegar a pé, com a passagem, e se
  dá para embarcar com bike agora.
- **Lembrete na agenda**, como no app: em qualquer estação, sem configuração.
  Em vez de abrir a agenda, baixa um arquivo `.ics` com o evento na hora da
  partida e o aviso escolhido na própria janela ("5 a 45 min antes", lembrado
  da última vez) — o iPhone abre no Calendário, o Android na agenda. A versão
  lite não tem mais tela de Configurações.
- `manifest.webmanifest` e os ícones permitem "Adicionar à tela inicial". No
  Android, quando o Chrome oferece a instalação (`beforeinstallprompt`), a
  tela inicial e a tela Sobre mostram o botão **"📲 Adicionar à tela
  inicial"**; no iPhone, a tela Sobre explica o caminho pelo Compartilhar. Aberta
  pelo ícone (modo standalone), nada disso aparece.
- Tela **Sobre**: aviso de não oficial, o que a versão lite faz e o que o app
  completo acrescenta, QR code (`qr.png`) e botão "Divulgar o app"
  (compartilhamento do celular, ou cópia do link), origem dos horários,
  privacidade, código aberto e Pix.
- `sw.js` (service worker) guarda o próprio app no celular: abre sem internet
  (no túnel, no modo avião). O app vem do cache na hora e a versão nova chega
  por trás, valendo na abertura seguinte; os horários tentam a internet
  primeiro e, sem ela, usam a última cópia.

A página do app (`docs/index.html`) aponta para a versão lite no topo ("Tem
iPhone ou não quer instalar?") e ensina a pô-la na tela inicial do iPhone.

Para testar localmente: `python3 -m http.server -d docs 8000` e abrir
`http://localhost:8000/app/` (no emulador, `adb reverse tcp:8000 tcp:8000`).

## Emulador

[`tools/emulador/ui.sh`](tools/emulador/ui.sh) reúne as funções usadas nos
testes e gravações: `tap "texto"` (toca pelo texto ou content-desc), `txt`,
`wait_text`, `fix lon lat` (GPS), `at HHMM` (relógio, com `adb root`), `demo
HHMM` (barra de status limpa) e `shot nome`. Uso: `. tools/emulador/ui.sh`.

## Página de apresentação

`docs/` é a página pública do app (GitHub Pages, publicada a partir de
`main`/`docs`): `giovanildo.github.io/metro-ceara-horarios`. É um HTML só,
sem dependências, feito para quem não é programador — o que o app faz, por que
é seguro, como instalar (com os avisos do Android explicados) e onde baixar.

O endereço antigo, `giovanildo.github.io/metro-fortaleza-horarios/`, é servido
pelo repositório [`metro-fortaleza-horarios`](https://github.com/giovanildo/metro-fortaleza-horarios),
que só leva ao endereço novo: qualquer caminho (inclusive `/app/` e as âncoras)
vai para o mesmo caminho aqui, e o service worker de quem instalou a versão
lite antiga se desliga e abre a nova. Não apague esse repositório enquanto
houver QR codes ou links antigos por aí.

**A versão lite é a porta de entrada.** A instalação de um APK de fora da loja
mostra avisos que espantam o usuário comum; a versão lite abre na hora, sem
nenhum. Por isso o botão principal da página é **"Abrir agora no navegador"**
(`app/`), e o secundário, **"App completo para Android"**, desce até o bloco
`#baixar`. A página não oferece mais instalar o APK direto (desde 27/10):
o bloco `#baixar` da seção "Como instalar o app completo" diz que o app está em
fase de testes na Play Store e convida a mandar um e-mail para virar testador
beta, com o botão "Ver na Play Store" (que só abre para quem já é testador).
O vídeo de instalação do Android saiu da página; o do iPhone ficou. O QR e o "Divulgar o app" do app Android também levam à
versão lite. Dentro dela, no Android, quem volta a usar (a partir da 3ª
abertura) vê um convite discreto para o app completo, com "Agora não" (some
por 30 dias); o convite leva ao mesmo `#baixar`. Tem
as marcações que o Google e as redes sociais leem (título, descrição, Open
Graph com `img/previa.png` de 1200×630 para a prévia do link no WhatsApp,
dados estruturados `SoftwareApplication`, `robots.txt` e `sitemap.xml`).

A **política de privacidade** (`docs/privacidade.html`, link no rodapé da página,
na tela Sobre da versão lite e na tela Sobre do app — a Play Store exige o link
também dentro do app) cobre o app e a versão lite: nenhum dado
pessoal é coletado ou enviado; a localização é usada só no aparelho; a internet
serve só para baixar os horários do espelho no GitHub. É o link exigido pela
Play Store — ao mudar algo que trate dados, atualize a página (e a data) antes.

As capturas em `docs/img/tela-*.jpg` e em `fastlane/…/phoneScreenshots/` são
as mesmas da loja (`tools/play/imagens/tela-*.png`, 1080×1920); ao refazer
essas, refaça aquelas.

## Publicando uma versão no GitHub

A release é assinada com uma chave própria, que fica **só na máquina de quem
publica** (`~/.android/metro-ceara-release.jks`), com as senhas em
`keystore.properties` na raiz — os dois fora do git. **Guarde uma cópia do
`.jks` e das senhas num lugar seguro**: sem eles, as próximas versões não
instalam por cima, e cada pessoa teria que desinstalar o app.

```bash
./gradlew assembleRelease            # app/build/outputs/apk/release/app-release.apk
cp app/build/outputs/apk/release/app-release.apk metro-ceara.apk
gh release create vX.Y metro-ceara.apk --notes-file notas.md
```

O arquivo vai sempre com o nome `metro-ceara.apk`, para o link
`releases/latest/download/metro-ceara.apk` (e o QR code) nunca mudarem.

## Publicação na Play Store (em preparação)

O material para o Play Console fica em [`tools/play/`](tools/play/):

- [`loja.md`](tools/play/loja.md) — tudo o que o Console pede, pronto para
  colar: nome ("Metrô Ceará (não oficial)"), descrições curta e completa,
  categoria e contato, e as respostas de cada formulário de "Conteúdo do app"
  (política de privacidade, anúncios, classificação, público-alvo 13+,
  segurança dos dados — **não coleta dados** —, app governamental — não —, e a
  justificativa do serviço em primeiro plano com localização do modo viagem),
  além da mensagem para convidar os testadores do teste fechado.
- `imagens/` — ícone 512×512 (PNG com transparência), gráfico de recursos
  1024×500 e 4 capturas 1080×1920 (a loja exige proporção 16:9 ou 9:16; as telas
  de 1080×2400 são cortadas embaixo). A tela do aviso de descida é a tela real da
  viagem com o cartão real da notificação, recortado da barra de notificações,
  colado no topo como o balão que aparece no aparelho.
- `video-modo-viagem.mp4` — o modo viagem em uso (iniciar, notificação fixa,
  tela bloqueada, aviso de descida), para subir no YouTube como "não listado" e
  pôr o link na declaração do serviço em primeiro plano.
- [`exportar-chave.sh`](tools/play/exportar-chave.sh) — exporta a chave de
  assinatura do APK do GitHub para a Play Store usar a mesma (Play App
  Signing com chave própria), com o `pepk.jar` e a chave pública que o
  Console fornece. Ferramenta, chave pública e o zip exportado ficam fora do
  git.

O pacote da loja é o AAB: `./gradlew bundleRelease` →
`app/build/outputs/bundle/release/app-release.aab`, assinado com a mesma chave
do APK. O app já não tem nada que a análise costuma barrar: não fala com o site
do Metrofor (certificado vencido) e não pede doação.

## Publicação no F-Droid

O app vai ser distribuído pelo [F-Droid](https://f-droid.org), que compila a
partir deste repositório. O que já está pronto aqui:

- **Textos e imagens da loja** em `fastlane/metadata/android/pt-BR/`: título,
  descrições, ícone, capturas de tela e `changelogs/<versionCode>.txt`.
- **A receita** em `fdroid/io.github.giova.metroceara.yml`, para copiar
  para `metadata/` no [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
- **Uma tag por versão** (`v1.7`, …): com `UpdateCheckMode: Tags`, o F-Droid
  acha versões novas sozinho.

Para cada versão nova: subir `versionCode`/`versionName`, escrever
`changelogs/<versionCode>.txt`, criar a tag `vX.Y` e dar push com `--tags`.

Para o primeiro envio, alguém com conta no GitLab faz um fork do fdroiddata,
copia a receita e abre um *merge request* (o guia é
[Submitting to F-Droid](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/)).
Duas coisas para explicar no pedido: o `app/src/main/assets/metrofor.db` é
**dado**, não código — é gerado por `tools/build_db.py` a partir do GTFS público
e serve só de reserva, porque o app baixa os horários do dia sozinho; e o
build usa AGP 9 com `compileSdk` 37, que o servidor de build do F-Droid precisa
suportar.

## Licença

Copyright (C) 2026 Giovanildo

Este programa é software livre: você pode redistribuí-lo e/ou modificá-lo sob
os termos da Licença Pública Geral GNU, conforme publicada pela Free Software
Foundation, na versão 3 da licença ou (a seu critério) qualquer versão
posterior.

Este programa é distribuído na esperança de que seja útil, mas **sem nenhuma
garantia**, nem mesmo a garantia implícita de comercialização ou adequação a
um fim específico. Veja a [Licença Pública Geral GNU](LICENSE) para mais
detalhes.

### Sobre os dados

A licença acima cobre **o código**, não os dados. O `metrofor.db` versionado
aqui é gerado a partir de fontes públicas de terceiros, cada uma com seus
próprios termos:

- **Horários e estações do metrô** — feed GTFS publicado pelo
  [Metrofor](https://www.ce.gov.br/metrofor/gtfs/), empresa do Governo do
  Estado do Ceará.
- **Endereços e passagens** — páginas das linhas no
  [site do Metrofor](https://www.ce.gov.br/metrofor/linhas/).
- **Posição de algumas estações de Sobral e do Cariri** — © colaboradores do
  [OpenStreetMap](https://www.openstreetmap.org/copyright) (ODbL).
- **Estações do Bicicletar** — GeoJSON publicado pela AMC no
  [portal de dados abertos da Prefeitura de Fortaleza](https://dados.fortaleza.ce.gov.br/organization/amc).

Este é um app **não oficial**, sem vínculo com o Metrofor, a AMC, a Prefeitura
de Fortaleza ou o Governo do Ceará.

### Sobre o nome e a marca

"Metrofor" é o nome da Companhia Cearense de Transportes Metropolitanos. O app
cita o nome **só para dizer de onde vêm os dados** (uso nominativo): ele se
chama "Metrô Ceará", não usa "Metrofor" no nome nem no ícone, não reproduz
o logotipo, e traz o aviso de não oficial na tela inicial e na tela Sobre. As
cores das linhas vêm do próprio GTFS e servem só para identificá-las. Quem for
publicar o app em loja deve manter esses cuidados — nome, ícone e descrição sem
sugerir vínculo oficial.

Onde há espaço, o nome vem acompanhado da linha "Fortaleza · Cariri ·
Sobral", e as descrições citam as três cidades, para a busca.

### Ícone

O trem sobre uma toalhinha de renda de bilro, com a jandaia-verdadeira (ave
símbolo do Ceará) pousada nele e as três faixas das linhas embaixo. A figura é
descrita uma vez só em [`tools/icone/icone.py`](tools/icone/icone.py), que gera
`tools/icone/icone.svg`, o vetor do Android 7 (`drawable/ic_launcher.xml`), o
ícone adaptável do Android 8+ (`drawable-anydpi-v26/ic_launcher.xml`, com fundo
e frente: o desenho encolhe para a zona segura e preenche o círculo, o squircle
ou o quadrado de cada aparelho) e os PNGs do site, da versão lite, da loja e
do F-Droid
(precisa do Google Chrome). Depois, `python3 tools/play/destaque.py` refaz o
gráfico de recursos da loja e a prévia de link do site, e
`python3 tools/video/qr_stories.py` os stories. O ícone da notificação continua
sendo só a silhueta do trem.
