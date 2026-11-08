# Metrô Ceará — horários

App Android **não oficial** com os horários das linhas do Metrofor (Fortaleza,
Sobral e Cariri). Sempre que é aberto, o app **baixa os horários do dia
publicados pelo Metrofor** (veja [Horários do dia](#horários-do-dia)). Sem internet,
tudo continua funcionando com a última cópia guardada no aparelho e, em último
caso, com a grade que vem no APK.

## Baixar

**Página do app, para divulgar:** <https://giovanildo.github.io/metro-ceara-horarios/>
— o guia das estações e linhas, com a apresentação do app (o que faz e como
instalar, para quem não é programador); gera uma prévia bonita ao colar o link
no WhatsApp.

**Na Play Store, em fase de testes:** para ser testador beta, mande o Gmail
da sua conta da Play Store pelo WhatsApp (85 99761-9295) ou por e-mail para
giovanildos@gmail.com. A página do app oferece só esse caminho (e a
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
chegadas). A tela de horários de um terminal também mostra só esse sentido.
Cada sentido aparece com "sentido" pequeno em cima do nome da ponta da linha (no
app e na lite; o widget mantém a seta, por falta de espaço), para não parecer o
nome de uma estação de destino. Na tela da estação, as abas são um trilho na cor
da linha: a estação é a bolinha no meio (no terminal, na ponta) e cada aba tem
um metrô indo para a sua ponta. O sentido 0 da linha fica sempre à esquerda. Nos
cartões do início e das integrações, uma seta curva com o metrô de frente vira
para o mesmo lado (`ui/DirectionLabel.kt`). Na tela da estação, debaixo das abas, vem por
onde ele passa: "Passa por **Padre Cícero, Porangabussu, Couto Fernandes,
Juscelino Kubitscheck**… até Carlito Benevides · 16 estações" (as 4 primeiras
estações do caminho; `data/DirectionVia.kt`). Pelo
GPS, aparecem **duas estações**: a mais próxima e a mais próxima de **outra
linha**, até 3 km (ajustável); sem outra linha por perto, a segunda mais próxima
da mesma linha (`nearestTwo`, em `data/Geo.kt`). Perto de São Benedito, São
Benedito e Moura Brasil (Oeste); em Parangaba, a Sul e a Nordeste; em Mondubim,
Mondubim e Manoel Sátiro. Uma **terceira** entra quando há outra estação até a
mesma distância (primeiro de uma linha que ainda não apareceu;
`nearestStations`). Como os cartões da viagem, **só uma fica aberta** (no
começo, a mais perto), com o tempo que falta ("em 7 min") grande e a hora
embaixo; as outras ficam **compactas**, com nome, linha e distância numa linha
e cada sentido numa linha curta ("Caucaia · em 50 min"). Tocar numa compacta
abre ela; tocar na aberta leva à estação. No app e na lite. A busca **"Para onde você vai?"** fica na
aba **Planeje sua viagem** e sai da estação em destaque ([abaixo](#para-onde-você-vai-roteiro-na-mesma-linha-ou-com-integração)). A
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

### Barra de baixo e o Mais

Embaixo, uma barra com ícone e nome em cada botão
([`ui/RoutesScreen.kt`](app/src/main/java/io/github/giova/metroceara/ui/RoutesScreen.kt)):
**Início** (a tela inicial, só horário), **Planeje sua viagem** ("Para onde
você vai?" e Casa e Trabalho), **Linhas** (a lista das linhas; tocar abre as
estações) e **Mais**, uma lista que sobe de baixo por cima da tela
([`ui/HomeMenu.kt`](app/src/main/java/io/github/giova/metroceara/ui/HomeMenu.kt)):
Passagem (o guia), Nina, Ajustar para mim, Configurações, De onde vem cada
informação, Sobre o app e Compartilhar o app. Ícone sem nome não serve para
todo mundo, por isso cada botão diz o que faz; com letra grande, "Planeje sua
viagem" quebra em duas linhas. A aba aberta volta depois de abrir uma linha.
Ao começar o modo viagem, o app vai para o Início, onde está o andamento;
durante a viagem, "Planeje sua viagem" mostra o cartão dela no lugar da busca
(não se começa outra viagem por cima).

### Nina (Super NINA)

A **Super NINA** é o canal de denúncia de importunação sexual pelo WhatsApp
((85) 93300-7001), do Metrofor com a Etufor e a Secretaria das Mulheres; desde
dez/2025 vale no metrô e nos VLTs de Fortaleza, Caucaia, Maracanaú e Pacatuba
(não em Sobral e no Cariri). No Mais, o item é só um **rostinho** (desenho do
app, `res/drawable/ic_nina.xml`, não a personagem da campanha) e o nome
**Nina**: nenhuma palavra que chame atenção de quem olha a tela. O toque abre
direto a conversa (`wa.me`); o TalkBack lê "Nina: abre o WhatsApp da Super
NINA". O toque não fica registrado em lugar nenhum. O número e as linhas
atendidas vêm do `regras.json` ("nina", com a fonte), então mudam sem versão
nova; perto de uma estação de Sobral ou do Cariri, o item some. A Super NINA
registra a denúncia mas não manda ajuda na hora (com a importunação
acontecendo, o Metrofor orienta procurar um funcionário); isso fica na fonte,
em "De onde vem cada informação", e não no menu, a pedido, para não alarmar
quem olha a tela.

### Ordem da tela inicial e "Ajustar para mim"

O app faz muita coisa, mas o propósito é simples: a pessoa ver logo quando
passa o próximo metrô. Por isso a tela inicial é uma lista de blocos e, no
padrão, segue o perfil da maioria (quem vai e volta do trabalho ou da escola):

1. **Trabalho e Casa** (também debaixo da busca, em Planeje sua viagem);
2. **as duas estações mais próximas** (sempre aparecem: é o motivo do app). Em
   **dia especial** (operação especial, feriado), o aviso de hoje do Metrofor vem
   logo acima delas, porque os horários são outros;
3. os **avisos do Metrofor dos próximos dias**: toda operação especial anunciada
   (ENEM, eleição, feriado) aparece desde o dia em que a notícia sai, não só na
   véspera, juntando os dias da mesma notícia ("Metrofor, domingo (8/11) e
   domingo (15/11):"). Com o app fechado, a cada ~3 h o app confere o
   `regras.json` (só baixa se mudou) e **notifica** a operação nova, uma vez por
   dia anunciado ([`alerts/`](app/src/main/java/io/github/giova/metroceara/alerts/),
   WorkManager); se o app estiver aberto, o aviso já está na tela e não há
   notificação. No Android 13+, sem a permissão de notificação, debaixo do aviso
   aparece o convite "Quer receber uma notificação…?" (**Avisar** / **Agora
   não**); recusado ou negado, não volta;
4. as **rotinas**;
5. o **Bicicletar** (só com "Levo bike").

"Para onde você vai?" e as linhas ficam nas abas da barra de baixo, não na
tela inicial (`HomeBlock.inRoutes`); uma ordem guardada antes disso continua
valendo para os outros blocos.

**Ajustar para mim** (no Mais ou no topo das Configurações; `ui/HomeLayoutScreens.kt`)
são interruptores, sem cadastro e sem "perfil" (que lembra login): **Vou ao
trabalho ou à escola** (Trabalho e Casa aparecem), **Levo bike** (a regra da
bike no cartão e o Bicicletar; desligado no padrão), **Pago meia ou tenho
gratuidade** (a passagem e o guia da meia no cartão), **Preciso de elevador ou
rampa** (a acessibilidade da estação no cartão). A tela da estação continua
mostrando tudo.

**Ordem da tela inicial** tem a prévia da tela inicial em cima, na largura toda
e sem receber toques, e embaixo cada bloco com liga/desliga à esquerda e **↑ ↓**
à direita. Setas em vez de arrastar: um toque cada, funciona com letra grande e
o TalkBack lê "Subir Trabalho e Casa". Mover mostra "agora é o 1º" com
**Desfazer**; **Voltar ao padrão** pede confirmação e não mexe em Casa, Trabalho
nem nos ajustes. As estações têm cadeado. O bloco de aviso só move e esconde o
de amanhã: o de hoje aparece sempre acima das estações. Tudo fica no
aparelho (`data/HomeLayout.kt`, preferências `metrofor_home_layout`, à parte das
Configurações). A lite só segue a ordem padrão, para continuar enxuta.

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
| Expedicionários → Expedicionários - AE (Nordeste → VLT Aeroporto) | **gratuita** — o VLT Aeroporto está gratuito durante a operação assistida (notícia de 10/03/2026) |
| Expedicionários - AE → Expedicionários (VLT Aeroporto → Nordeste) | **gratuita** — a Linha Nordeste está gratuita durante a implantação (notícia de 03/02/2026) |

Sem regra (`UNKNOWN`) o app não fala de passagem, para não informar errado.
Quando a Nordeste ou o VLT Aeroporto começarem a cobrar, basta trocar as entradas que citam o motivo.

### "Para onde você vai?" (roteiro na mesma linha ou com integração)

O jeito de ir de uma estação a outra é um só, como num app de corrida: a barra
**"Para onde você vai?"**, na aba **Planeje sua viagem**, abre a tela **Sua viagem**
([`ui/JourneyScreen.kt`](app/src/main/java/io/github/giova/metroceara/ui/JourneyScreen.kt)).
A tela de cada estação tem o botão **"Ir daqui para outra estação"**, que abre a
mesma tela saindo dali.

- **De:** já vem com a estação em destaque (GPS ou fixada) ou com a estação de
  onde a pessoa veio. Tocar troca, numa lista de todas as estações com busca.
- **Para:** busca sem acento ("anto" acha Antônio Sales e Antônio Bezerra) numa
  lista por linha, com o caminho no título ("Linha Sul · mesma linha", "Linha
  Nordeste · integração em Parangaba", "VLT Aeroporto · 2 integrações") e a
  chegada de quem sai agora ("chega 13:57"). Linhas que não se alcançam da
  origem ficam de fora; linha que o Metrofor avisou que não circula aparece
  apagada ("não circula hoje").
- **Lugares**, não só estações: "rodoviaria", "hgf", "UFC" ou "castelão" acham
  o lugar e a estação mais perto dele, com a caminhada ("Fátima · Estação Borges
  de Melo · 630 m · 9 min a pé") e a chegada já contando ela. O roteiro termina
  a pé até a porta. Ver [Lugares](#lugares-busca-e-perto-desta-estação).
- **Casa e Trabalho**, como num app de corrida: dois atalhos no topo da tela
  inicial
  ([`ui/PlaceShortcuts.kt`](app/src/main/java/io/github/giova/metroceara/ui/PlaceShortcuts.kt)).
  Na primeira vez, "🏠 Definir Casa" e "💼 Definir Trabalho" abrem a escolha da
  estação ("O app guarda só a estação, não o seu endereço"). Depois, cada
  atalho mostra a estação e a chegada de quem sai agora da estação em destaque
  ("Mondubim · chega 18:12"); um toque abre o roteiro direto, pronto para o
  modo viagem. O lugar onde a pessoa já está vira "você está aqui" e vai para
  o fim, então de manhã, em casa, o Trabalho fica na frente e, no fim do dia,
  a Casa. Tocar e segurar troca a estação (com "Nenhuma" para apagar), e
  **Configurações → Casa e Trabalho** também. Na lista do **Para**, os dois
  aparecem no topo, em "Seus lugares", enquanto a busca está vazia. Guardados
  só no aparelho (`data/Places.kt`, estação e linha).
- **Resultado**, numa folha que sobe por baixo:
  - **Sair agora** ou **Escolher horário** (relógio de 24 h; "Sair às 17:30");
  - **cada horário um cartão**, como nos apps de ônibus
    ([`ui/JourneyOptions.kt`](app/src/main/java/io/github/giova/metroceara/ui/JourneyOptions.kt)):
    número, duração ("11 min", "1h06"), saída → chegada, integrações ("Direto",
    "1 em Parangaba · passagem grátis") e as linhas na cor de cada uma, com os
    selos **Próximo** (o primeiro, saindo agora) e **espera N min** (troca de
    linha de 10 min ou mais). Sem filtros nem km: nos trilhos quase sempre há
    um caminho só e as opções diferem no horário;
  - o cartão tocado abre com o **roteiro**: cada trecho na cor da linha ("13:02
    Carlito Benevides · Metrô da Linha Sul, sentido Chico da Silva. Desça em
    Parangaba às 13:26."), a integração com o tempo de troca da pessoa, a
    passagem e um selo (**Dá tempo** ou **Integração justa**, com o próximo
    caso perca), a chegada, a dica "o metrô das 07:32 chega no mesmo horário,
    mas espera mais na integração", a caminhada até a estação e os avisos de
    bike;
  - **⇅** ao lado do título inverte ida e volta (não aparece quando o destino
    é um lugar);
- **Recentes:** em "Planeje sua viagem", debaixo de Casa e Trabalho, as 3
  últimas viagens vistas ("São Benedito → Papicu", com as linhas na cor de
  cada uma; lugar com "a pé no fim"). Entra quando o resultado aparece, o mesmo
  trajeto sobe em vez de repetir, e Casa ou Trabalho saindo da estação de
  agora não aparecem (já têm atalho). Um toque abre o resultado; tocar e
  segurar remove; **Configurações → Viagens recentes → Limpar** apaga todas.
  Só no aparelho (`data/RecentTrips.kt`, preferências `metrofor_recent_trips`;
  na lite, `localStorage` "recentes", com ✕ para remover).
  - saindo agora: **Iniciar modo viagem** (perto da estação, até o limite das
    Configurações). Num trecho só, a viagem começa como sempre (com a oferta
    de ir sentado pelo terminal); com integração, o roteiro inteiro.
  - no horário escolhido: **Avisar a hora de sair** (o aviso do app), para a
    partida do roteiro.
- **Domingo sem operação anunciada:** nenhum roteiro nem modo viagem. O
  resultado mostra só o aviso "Hoje é domingo: metrô e VLT não circulam.
  Voltam segunda, a partir das 05:30." (o primeiro horário de segunda na
  estação de partida).

**Como o roteiro é calculado** ([`data/Router.kt`](app/src/main/java/io/github/giova/metroceara/data/Router.kt),
Kotlin puro, com testes em `app/src/test`). A rede é pequena, com até três
integrações (Caucaia → Aeroporto passa por Chico da Silva, Parangaba e
Expedicionários). O app testa cada trem do primeiro trecho e, em cada
integração, o primeiro trem que ainda dá para pegar. Entre os que chegam no
mesmo horário, sugere **o último que sai**, desde que a integração não fique
justa: saindo do Benfica, o metrô das 07:53 pega o mesmo VLT das 08:10 que o
das 07:40, com 9 min de espera em vez de 22. Saindo agora, conta a caminhada
da pessoa até a estação de partida (até 3 min, ela já está lá). Também
conta como integração a estação que serve duas linhas (Campo dos Velhos, em
Sobral), com 1 min. Uma linha que o Metrofor avisou que não circula hoje fica
de fora. Sem roteiro possível: "Sem metrô ou VLT para Papicu hoje depois das
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
| Cadeira de rodas ou mobilidade reduzida (o roteiro avisa onde não tem elevador) | 2 | `deslocamento.cadeira_de_rodas` |

| Horário | A mais | `regras.json` |
|---|---|---|
| Pico (6h–8h e 16h–19h) | +1 min | `pico.integracao_min` |
| Pico máximo (17h–18h, Relatório de Demanda) | +2 min | `pico.maximo`, `pico.integracao_maximo_min` |

**Na viagem (do começo ao fim, sozinha).** "Iniciar modo viagem" acompanha o
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

**Versão lite.** A mesma barra **"Para onde você vai?"** (aba Planeje sua viagem, `#/rotas`) e o
botão **"Ir daqui para outra estação"** na estação abrem a tela **Sua viagem**
(`#/viagem`): "De" (a estação mais perto, com localização, ou a última usada),
"Para" com a busca sem acento e a lista por linha, e o mesmo resultado (Sair
agora / Escolher horário, um cartão por horário, ⇅ Inverter ida e volta), calculado por
[`docs/app/roteiro.js`](docs/app/roteiro.js) com a mesma regra. "Como você se
desloca" fica nessa tela e o navegador lembra. Na lite não há modo viagem: no
lugar do botão, o **lembrete na agenda** e o convite para o app completo.
**Casa e Trabalho** também estão na lite, guardados no navegador
(`localStorage` "lugares"): os atalhos no topo da tela inicial levam ao roteiro
(`#/ir/casa`), a escolha da estação é uma tela própria (`#/lugar/casa`, com
"Nenhuma") e o link **Trocar** faz o papel de tocar e segurar. Sem a
localização, a tela inicial não sabe de onde a pessoa sai e os atalhos mostram
só a estação; com ela, a chegada ou "você está aqui". Na lista do **Para**,
"Seus lugares" no topo, como no app. Os **lugares** (busca e "Perto desta
estação") também, pelo mesmo `regras.json`
([`docs/app/lugares.js`](docs/app/lugares.js)).

### Widget na tela inicial

Dois widgets, escolhidos na lista de widgets do celular (segurar num espaço
vazio da tela inicial → Widgets → Metrô Ceará):

- **Estação: próximos horários** (4×2): a estação, a linha e os **dois
  próximos** horários de cada sentido ("18:16  18:34"), mais o aviso do dia.
- **Estação: próximo horário** (2×2): o próximo de cada sentido, em número
  grande.

A estação é a **fixada com a estrela**; sem ela, **a última aberta** no app;
sem nenhuma, **a mais perto de você** na última vez que o app achou a
localização (a do topo da tela inicial). Sem nenhuma das três, o widget pede
para abrir o app com a localização ligada ou fixar a estação. Os horários saem
da grade já baixada e o aviso, do `regras.json`, sem internet.

O **↻** no canto (sempre visível) procura a **estação mais perto pelo GPS**: o
widget mostra "Procurando a estação mais perto…" e, em poucos segundos, troca de
estação. Ela passa na frente da fixada e da última aberta até a pessoa fixar
uma estação com a estrela. O GPS só é ligado quando a pessoa toca no ↻: um
serviço em primeiro plano de poucos segundos
([`widget/WidgetLocateService.kt`](app/src/main/java/io/github/giova/metroceara/widget/WidgetLocateService.kt),
com notificação enquanto procura), que o Android permite porque o toque foi da
pessoa, sem a permissão de localização "o tempo todo". A posição é sempre nova
(sem aproveitar posição guardada: no trem, a de 1 min atrás já está quase 1 km
para trás). Sem localização, fica a estação de antes ("Não achei sua
localização agora."); sem permissão, o ↻ abre o app, que pede.
Depois do último horário do dia, mostra o primeiro de amanhã ("05:47
amanhã") ou de segunda. O aviso do dia é a notícia da operação especial, "O
Metrofor avisou que esta linha não circula hoje." ou, num domingo sem
operação, "Domingo: metrô e VLT não circulam. Voltam segunda às 05:30.", sem
as linhas de horário. Tocar abre a tela
da estação no app. Segue o tema do celular, claro ou escuro.

O widget ([`widget/StationWidget.kt`](app/src/main/java/io/github/giova/metroceara/widget/StationWidget.kt),
com `RemoteViews`, sem biblioteca nova) se redesenha **um minuto depois de cada
partida mostrada**, com um alarme sem hora exata, que quase não gasta bateria e
não exige permissão. Também se redesenha quando o app baixa a grade ou as
regras, quando a estrela muda e quando outra estação é aberta, e de 30 em 30
minutos, que é o mínimo do Android. A versão lite não tem widget, porque os
navegadores não permitem.

### Modo viagem

O modo viagem começa pelo resultado da busca **"Para onde você vai?"**
([acima](#para-onde-você-vai-roteiro-na-mesma-linha-ou-com-integração)): escolhido
o destino e saindo agora, o botão **Iniciar modo viagem**. Durante a viagem, a
barra do topo dá lugar ao cartão com o andamento, e um serviço em
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
  **Depois de embarcar, com o satélite respondendo, só o GPS faz a viagem
  andar**: cada estação passa pelo raio dele. Enquanto o metrô não chega à
  próxima, o atraso sobe (é no mínimo o que já passou do horário dela). Em
  04/11 o metrô passou de Jereissati, ficou 6 min parado em Carlito Benevides
  (fora da viagem) e a tabela já o punha em Maracanaú e Virgílio Távora.
  O atraso vale pela **chegada** à estação: o tempo parado na plataforma não
  conta (na origem, sim: continuar lá depois do horário é atraso da partida).
  **No túnel** (sem satélite, depois de o GPS ter visto o metrô fora da origem),
  a chegada às estações **subterrâneas** (`"subterranea": true` no
  `regras.json`) é estimada **1,5 min antes**: o metrô recupera tempo entre
  Padre Cícero e Chico da Silva, e sem isso a estimativa ficou duas estações
  atrás e o aviso "2 estações antes" saiu a 1 min de Chico. Ali também vale a
  rede de celular com até 300 m de erro, só para andar para a frente. Uma
  posição de satélite solta depois de um tempo sem sinal (pode ser velha: veio
  uma de Padre Cícero com o metrô já perto de Chico) só conta a partir da
  segunda seguida, e posições com mais de 30 s são ignoradas. As duas viagens
  de 04/11 estão em `app/src/test/resources/campo/` e o `TripFieldReplayTest`
  as repassa ao estimador.
- **Quando o GPS é dado como perdido.** Depois de **1 minuto** sem nenhuma
  posição boa (12 tentativas, uma a cada 5 s) — menos que os ~2 min entre duas
  estações. A partir daí o cartão mostra um quadro em destaque, de fundo neutro
  e letra legível (ícone vermelho só para chamar atenção): "**Sem GPS há N
  min** — Posição estimada pela tabela de horários. Confira o nome das
  estações pela janela." A notificação fixa ganha
  "sem GPS há N min", e todo aviso baseado na tabela diz isso: a estação vira
  "Provavelmente Parangaba" e o aviso de descida termina com "(pela tabela de
  horários)"; sem GPS, a chegada diz "presumida", mesmo que a última estação
  tenha vindo dele. A tabela é a "média" da viagem: os horários programados do
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
  integração"); na notificação fixa, no trem, a estação atual no título ("Em
  Benfica") e, no texto, a próxima parada e o destino ("Próxima: Couto
  Fernandes · Até Parangaba em 5 estações · chegada ~07:43 (pelo GPS)"); antes de embarcar, o destino e a chegada no título ("Até
  Parangaba · chegada ~08:01"); e "(simulação)" e "(pela tabela de horários)"
  no fim. A voz continua com a frase completa.
- **Próxima parada.** Desde a 1.79 (pedido de uma anotação de campo), o cartão
  diz onde o metrô está e qual é a próxima: "Em Padre Cícero · próxima: Benfica
  · destino em 4 estações · chegada ~15:58". Quando a próxima já é o destino,
  ela não se repete ("destino em 1 estação" diz isso). Sem GPS, vale o mesmo
  "presumida" da estação atual.
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
  **Em qualquer horário**, com "direto" como primeira opção. Até a 1.78 a
  pergunta só aparecia no pico (6h às 8h e 16h às 19h, ~45% dos passageiros
  segundo o Relatório de Demanda do Metrofor, 3º tri/2025), mas em 04/11 ele
  embarcou em Maracanaú às 15h para voltar sentado e o app não ofereceu: a
  viagem começou direto, sem Carlito, e o app não soube acompanhar o metrô
  parado lá. As faixas continuam em `regras.json` (`pico`) e aparecem na
  pergunta como explicação.
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

- **Na tela inicial**, num cartão próprio, para quem ligou **Levo bike** em
  "Ajustar para mim". Com GPS, a
  distância é medida a partir de **você**, até 3 km — acima disso (fora de
  Fortaleza, por exemplo) o cartão some. Com estação fixada, é medida a partir
  da estação, até 600 m.
- **Na tela de partidas** de cada estação, medida a partir dela e escondida
  acima de 600 m, porque a essa distância deixa de ajudar.

Nas linhas Sul e Nordeste e no VLT de Sobral, o app diz também **se dá para embarcar com
bicicleta agora** (no metrô ou no VLT, conforme a linha; o aviso some quando
não há mais partidas no dia), na tela de partidas e, com **Levo bike**, no cartão da tela inicial. As
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
domingo com operação anunciada e sem regra de bike, o aviso diz "Operação
especial: o Metrofor não informou se dá para embarcar com bike hoje", em vez de
calar. Num domingo comum, só fica o aviso de domingo: a grade do dia não serve
de sinal de operação (o feed de domingo comum vem igual ao de dia útil).

Na tela da estação (app e lite), quando dá para embarcar com bike hoje (agora
ou mais tarde), o aviso ganha uma segunda linha: **"Até 4 bikes por viagem.
Bike elétrica ou de carga não pode."** (2 no VLT de Sobral). Sem ela, quem tem
bike elétrica lia "Pode embarcar com bike agora" e ia até a estação à toa. O
limite vem do regulamento e das notícias (Sul e Nordeste: 4, desde 2022;
Sobral: 2, em 2020 e 2025), em `limite_por_viagem` de cada regulamento; a
proibição vem do regulamento (cargueiras, motorizadas ou com carga presa) e do
Guia do Passageiro, em `bike.proibidas`.

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

**Cores das linhas.** O GTFS pinta a Nordeste e o VLT Aeroporto do mesmo roxo,
a Linha Norte de Sobral de azul e o Cariri de verde, mas o mapa oficial
afixado nas estações numera e colore as linhas de outro jeito: em Fortaleza,
Linha 1 Vermelha (Sul), 2 Verde (Oeste), 3 Azul (Nordeste) e 5 Laranja (Ramal
Aeroporto); em Sobral, 1 Laranja (Norte) e 2 Vermelha (Sul); no Cariri, 1
Vermelha. A seção `cores` do `regras.json` traz a cor do mapa por linha, com
um tom `escuro` para o tema escuro (o azul e o verde do mapa ficam apagados
nele), e vale no lugar da do GTFS em todo o app (lista de linhas, estação,
integração, viagem, widget) e na lite. Linha fora da lista fica com a do GTFS.

Cada estação pode ter também um **`aviso`**, texto livre que aparece logo
abaixo da passagem, destacado, no app e na lite; como o `regras.json` é baixado
do espelho, mudar, tirar ou pôr um aviso chega a todos **sem versão nova**. O
primeiro é o das quatro estações **subterrâneas** da Linha Sul (Benfica, São
Benedito, José de Alencar e Chico da Silva, marcadas com `"subterranea": true`),
onde o sinal do celular falha e o Pix fica difícil: "Estação subterrânea: o
sinal do celular costuma falhar. Leve dinheiro ou deixe o Pix aberto antes de
descer." Nesse trecho o GPS também some: nos registros de 14 a 21/10, as
posições lá embaixo vieram da rede (25–100 m de precisão), o que bastou para
marcar as estações; num caso, 2 min sem posição, e o app seguiu pela tabela.

Na mesma seção `estacoes`, `posicao` corrige as estações que o **GTFS põe no
lugar errado**. Conferindo as cinco linhas com mapas do Google My Maps e com o
OpenStreetMap (as duas fontes concordam, a até 70 m), Fortaleza bate com o
GTFS, mas em Sobral seis estações estavam longe (Novo Recanto a 2 km, Junco a
1,2 km, Grendene, Campo dos Velhos, Cohab II e Alto da Brasília a 220–790 m) e,
no Cariri, a Escola a 270 m. Isso confundia a estação mais perto e o modo
viagem. As posições usadas são as do OpenStreetMap. Cada entrada leva também o
`nome` da estação: se um dia o Metrofor renumerar as estações e o nome não
bater mais, a entrada é ignorada, em vez de ir para a estação errada.

### Acessibilidade das estações (elevador, rampa, plataforma elevatória)

Na tela da estação (no app e na lite), logo abaixo da passagem, uma linha diz
como se chega à plataforma: **"♿ 2 elevadores e escada rolante · ver tudo ›"**
ou "♿ Acesso por rampa". Onde **não tem elevador**, vira um aviso destacado,
para todos (quem leva carrinho de bebê também precisa saber): em Maracanaú,
"Sem elevador. Para chegar à plataforma há uma plataforma elevatória, ligada
por um funcionário. Peça na bilheteria." O toque abre a tela **Acessibilidade**
([`ui/AccessScreen.kt`](app/src/main/java/io/github/giova/metroceara/ui/AccessScreen.kt);
na lite, `#/acesso/<linha>/<estação>`): quantos elevadores, plataformas
elevatórias, rampas, escadas e escadas rolantes, o espaço para cadeira de rodas
dentro do metrô e do VLT, quem pode usar o elevador e a fonte.

- **Cadeira de rodas** em Configurações > Como você se desloca: além de dobrar
  o tempo da integração, o roteiro olha a estação de embarque, as duas da
  integração e a de chegada. Sem elevador, um alerta ("Maracanaú não tem
  elevador: peça a plataforma elevatória na bilheteria."); com elevador ou
  rampa, uma nota ("♿ Parangaba: 2 elevadores e escada rolante · Papicu: 1
  elevador"). Estação sem dado aparece como "ainda não temos os dados", na
  tela da estação só para quem escolheu cadeira de rodas.
- **Conte pra gente**: na tela Acessibilidade, a pessoa escolhe o que encontrou
  (elevador parado, plataforma sem funcionário, rampa bloqueada, dados errados)
  e a mensagem sai pronta pelo WhatsApp ou por e-mail. É como o dado se mantém
  em dia até a resposta oficial.

Os números ficam em `estacoes` > `acesso` do `regras.json` (`elevador`,
`escada_rolante`, `rampa`, `escada`, `plataforma_elevatoria`) e mudam sem
versão nova. Vêm das tabelas de acessibilidade por linha do Metrofor (Gerência
de Projetos, nov/2015; cópia em `referencias/acessibilidade-2015/`, fora do
git), conferidas com as notícias: as inaugurações da Linha Sul em 2012 batem na
maioria das estações, e Maracanaú sem elevador aparece em três notícias
(2012, 2013, 2017). **Conferido no local em 04/11/2026:** Maracanaú tem mesmo
a plataforma elevatória, ligada por um funcionário, e o elevador de Carlito
Benevides funciona e é usado. Três nomes de 2015 foram trocados pelos de hoje (Rodoviária
= Borges de Melo, Bezerra de Menezes = Antônio Bezerra, Cruzamento José
Euclides = José Euclides). Ficaram **sem dado** Padre Cícero, Expedicionários,
Boulevard do Arco (reconstruída em 2021) e Campo dos Velhos (é a "Coração de
Jesus" da tabela, mas ela diz "sem rampa" e o mesmo PDF diz que as estações de
Sobral têm rampas). O `_fonte` da seção lista o que cada notícia confirma ou
contradiz. O próximo passo é pedir a tabela atualizada pelo SIC.

### De onde vem cada informação (fonte e data)

O app não é oficial. Para dar credibilidade, **cada informação diz de onde vem
e até quando foi conferida**. Embaixo da passagem, do aviso de bike, da
integração, do Bicicletar, da acessibilidade, dos lugares, do guia da passagem
e do aviso das estações subterrâneas há um link curto, no app e na lite:
**"Fonte ›"** (ou **"Estimativa do app ›"**, ou **"Fonte · pode ter mudado ›"**
quando a conferência atrasou, que não pode ficar escondido). Tocar abre uma
janela com uma coisa por linha e o botão "Abrir a fonte":

> **Passagem e endereço das estações**
> Fonte: site do Metrofor
> Preço sem reajuste desde mar/2019
> Conferido em nov/2026
> Próxima conferência: mai/2027

A primeira versão (1.69) mostrava tudo isso numa frase embaixo de cada
informação; ficou transparente, mas verboso demais, e virou o link (1.70). No
guia da passagem o nome do Metrofor continua à vista ("Fonte: site e notícias
do Metrofor ›"). A lista inteira, com uma explicação de cada uma, fica em
Sobre → **"De onde vem cada informação"** (na lite, `#/fontes`), começando pelos
horários ("baixados hoje às 14:06 · valem só para hoje").

Decisões, conversadas com o autor do app:

- **Idade não é confiabilidade.** A passagem de 2019 está certa; a grade de
  ontem pode estar errada. Por isso a linha separa *desde quando vale*
  (`desde`), *quando foi conferida* (`conferido`) e *até quando dá para confiar
  sem conferir de novo*, que depende do tipo da informação
  (`revisar_em_meses`: passagem e bike 6, acessibilidade e lugares 12, cores
  24; horários valem para o dia).
- **"Próxima conferência", não "válido até".** O app não controla o Metrofor e
  não pode prometer validade; pode se comprometer a olhar de novo. Passada a
  data sem reconferir, a linha diz "conferência atrasada desde …: pode ter
  mudado".
- **Sem cores** de confiável a defasado: as cores já são das linhas, e cor
  sozinha não chega a todo mundo.
- **Estimativa não parece dado oficial.** O que é conta do app (tempo a pé,
  chegada, "dá tempo") ou observação nas viagens de teste (sinal nas
  subterrâneas) aparece sem "Fonte:" e sem próxima conferência: "Tempo a pé:
  estimativa do app".
- **Regra do dia especial** (bike em feriado) vem da notícia do dia: "Regra de
  hoje: notícia do Metrofor · vale só para hoje".

Tudo fica na seção `fontes` do `regras.json` (`titulo`, `tipo` = `metrofor`,
`terceiros` ou `app`, `fonte`, `detalhe` para a lista, `link`, `desde`,
`conferido` AAAA-MM, `revisar_em_meses`, `diario`), lida por
[`data/Sources.kt`](app/src/main/java/io/github/giova/metroceara/data/Sources.kt)
(com testes) e por `fonteLinha` na lite; cada regulamento de bike aponta para a
sua com `fonte_id`. Reconferir sem mudança só sobe o `conferido`.
[`tools/fontes.py`](tools/fontes.py) lista as fontes e, com `--vencidas`, só as
que passaram da data (sai com 1 se houver).

### Guia da passagem (meia, gratuidade, VaiVem Livre)

Abaixo da passagem, a tela da estação (no app e na lite) tem o link **"Meia,
gratuidade e onde tirar o cartão ›"**. Ele abre a tela **Passagem**
([`ui/FareGuideScreen.kt`](app/src/main/java/io/github/giova/metroceara/ui/FareGuideScreen.kt);
na lite, `#/passagem`), com a passagem de cada linha, um quadro para quem paga
inteira na Linha Sul (o Cartão Múltiplo com nome, pedido pelo site) e três
temas, cada um numa tela própria:

- **Meia de estudante**: onde vale o cartão do Metrofor e onde basta a carteira,
  onde tirar o cartão (só Parangaba e Benfica em Fortaleza, em dia útil), o que
  levar e a renovação anual;
- **Gratuidade**: quem só mostra o documento e quem precisa do Cartão Gratuidade
  (onde tirar e o que levar), além de dizer que declaração não vale;
- **Procurando emprego**: o VaiVem Livre, 40 passagens grátis por mês com
  cadastro no IDT. Esse programa não aparece na página de gratuidades do Metrofor.

São muitas regras que o app não controla. Por isso a tela diz logo no topo que
**tudo vem do site do Metrofor**, com a data da conferência, e cada tema começa
com "Fonte: site do Metrofor · conferido em …" e termina no botão **"Conferir
no site do Metrofor"**, que abre a página daquele assunto.

Todo o texto fica na seção `guia_passagem` do `regras.json` e muda sem versão
nova. Um tema pode ter `avisos` com data (`de` e `ate`, AAAA-MM-DD), que só
aparecem nesse período no topo da tela do tema. Servem para prazos como a
renovação do cartão de estudante, que o Metrofor pede quase todo ano
(2019–2022, 2025), com prazo em junho. Sem a seção, ou com erro nela, o link
some e o resto das regras continua valendo.

**Quanto isso muda** (notícias do Metrofor de 2008 a 2026): o preço da Linha
Sul está em R$ 3,60 desde mar/2019; a regra da meia é a mesma desde 2017; a
lista de gratuidades segue a lei. O que muda com frequência é o calendário: a
renovação dos estudantes e, de vez em quando, a do Cartão Gratuidade, além de
programas novos, como o VaiVem Livre (2025) e o cadastro online do Cartão
Múltiplo (2024). Quando o convênio com o Sindiônibus (Bilhete Único nos
trilhos) entrar em operação, é preciso reescrever a seção.

### Lugares (busca e "Perto desta estação")

Quem não sabe o nome da estação procura o lugar: na tela **Sua viagem**, o
**Para** ("Estação ou lugar (ex.: Rodoviária)") acha também 401 lugares de
Fortaleza e da região metropolitana, de Sobral e do Cariri: hospitais, UPAs e
postos de saúde, universidades e escolas estaduais, terminais e rodoviárias,
aeroporto, shoppings, cinemas, mercados e atacadões, praias, lagoas, praças,
museus, teatros, igrejas, Detran, Vapt Vupt, delegacias, câmaras e fóruns. A busca é
sem acento, por qualquer palavra do nome e por apelidos ("hgf", "hgcc", "ijf",
"pv", "joao thome", "cinema", "upa"); praias, lagoas e parques com calçadão
também respondem a "calçadão", "corrida" e "caminhada". Os lugares vêm depois das estações, cada um com bairro,
estação e caminhada; os de outra cidade vão para o fim ("não dá para chegar
daqui").

- **O roteiro termina a pé**: a última linha do cartão diz "Rodoviária de
  Fortaleza: 630 m a pé, uns 9 min", e a chegada já conta a caminhada, no passo
  de **Como você se desloca**. A estação de destino é a do lugar na linha da
  origem, se houver (Parangaba tem metrô e VLT); senão, a de chegada mais cedo.
- **Longe dos trilhos** (mais de 2 km a pé da estação, como Castelão, Unifor,
  Horto): sem roteiro, só a explicação ("Nenhuma estação fica perto deste
  lugar. A mais perto é a estação Aeroporto, a 5,1 km…").
- **Perto desta estação**: na tela da estação, até 8 lugares a menos de 1 km,
  do mais perto ao mais longe ("Shopping Benfica · ao lado da estação", "UFC ·
  Reitoria · 500 m · 7 min a pé"). É o "entorno das estações" que o estudo da
  concessão pede ao app oficial.

Os lugares ficam na seção `lugares` do `regras.json`, um por linha, e mudam sem
versão nova
([`data/Landmarks.kt`](app/src/main/java/io/github/giova/metroceara/data/Landmarks.kt)).
As posições são do OpenStreetMap, e a escolha foi conferida com os pontos de
referência das páginas das estações do Metrofor (2011 e 2015). A ampliação para
401 seguiu os motivos de viagem da Pesquisa Origem-Destino da Linha Sul (2022):
depois de casa (50%) e trabalho (31%), vêm saúde (5%) e lazer (4%). Ficaram de
preferência lugares a até uns 2 km de uma estação; os famosos mais longe
(Praia do Futuro, Unifor, Castelão) entram como "longe dos trilhos". Para acrescentar
um lugar, basta `nome`, `tipo` e `posicao` ([lat, lon]), e `busca` com os
apelidos, se houver; depois rodar

```sh
python3 tools/lugares.py               # completa só os lugares novos
python3 tools/lugares.py --recalcular  # recalcula todos (estação mudou etc.)
```

O [`tools/lugares.py`](tools/lugares.py) preenche a estação mais perto a pé
(entre as três mais perto em linha reta), a caminhada pelas ruas (rota a pé do
OpenStreetMap, routing.openstreetmap.de) e o bairro (Nominatim). A caminhada fica
entre 1,1 e 1,5 vez a linha reta: a posição da estação fica no meio dos trilhos,
e a rota às vezes engatava numa rua do outro lado e dava a volta no quarteirão
(o Terminal do Papicu, colado na estação, dava 1 km). Para 200 lugares, leva uns
10 minutos (o Nominatim aceita uma consulta por segundo). O script troca só o
bloco `lugares` (as outras seções ficam como estão, e ele confere isso) e não grava
se o arquivo mudou enquanto rodava. A estação do lugar é
gravada pelo nome; o app confere também a posição, porque há nomes repetidos
(Padre Cícero em Fortaleza e no Cariri).

O `regras.json` é a **fonte única** das regras que não vêm do GTFS (bike,
integrações, passagem, endereços, posições corrigidas, acessibilidade, horários de pico e lugares): o app (`data/Regras.kt`), o
`tools/build_db.py` e a versão lite leem o mesmo arquivo.

**Mudar uma regra não exige versão nova do app.** A tarefa do espelho publica
uma cópia de `app/src/main/assets/regras.json` no ramo `dados` a cada rodada, e
o app a confere sempre que abre (antes da grade, porque a montagem da grade usa
as integrações) e passa a usá-la na hora; a lite lê a mesma cópia. Desde a 1.76
o app só baixa o arquivo (~120 KB) quando ele mudou: guarda a etiqueta de
versão (ETag) que o GitHub manda e a reenvia; se nada mudou, a resposta é um
304 sem conteúdo (`regras_sync` no registro de testes). Sem a cópia guardada, ou
com erro nela, baixa inteiro. Então basta
editar o arquivo na `main` (pelo próprio site do GitHub, se quiser) e rodar a
tarefa (Actions → "Dados da versão web" → Run workflow) ou esperar a próxima
rodada: em minutos ou horas os celulares recebem. Um arquivo com erro no
espelho é ignorado: o app segue com a última cópia boa e, sem nenhuma, com a do
APK. Com tudo ilegível, segue sem avisos de bike e de passagem, em vez de fechar.

**Mais → Configurações** abre as distâncias
e os tempos que antes eram fixos no código (`data/Settings.kt`; padrões entre
parênteses):

Na seção **Aparência**: **Tema** — "Como o celular" (padrão), "Claro" ou
"Escuro"; os ícones da barra de status acompanham o tema do app — e **Cores do
papel de parede** (Android 12+, ligado por padrão; desligado, o app usa as
próprias cores, em vermelho). Nas outras seções:

| Ajuste | Padrão | Faixa |
|---|---|---|
| Segunda estação até | 3 km | 500 m – 5 km |
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

**Avisar a hora de sair:** em qualquer estação, **tocar num horário** que ainda
não passou (nas próximas partidas ou em "Todos os horários") marca o aviso do
próprio app, sem janela nem botão (`LeaveToggle`, em `ui/LeaveReminder.kt`). Uma
confirmação curta aparece embaixo: "Aviso às 13:59 (15 min antes) para o metrô das
14:14." O horário ganha o sininho ("🔔 13:59" nas próximas partidas; o botão
destacado em "Todos os horários"), e **tocar de novo tira** o aviso ("Aviso das
13:59 tirado."). A antecedência fica nas **Configurações → Hora de sair** (5 a 45
min, padrão 15, `AppSettings.leaveBeforeMinutes`). No primeiro toque, o Android
pede a permissão de notificação, e o app explica uma vez só "Alarmes e lembretes".
Se a antecedência já passou para aquela partida, o app avisa que não dá ("Faltam
só 8 min para a partida"). No resultado de **"Para onde você vai?"**, o botão
**Avisar a hora de sair** funciona igual: marca, e vira "🔔 Aviso às 13:59 (toque
para tirar)". Até a 1.62 havia também uma janela, o alarme do relógio e a agenda,
que saíram para simplificar.

**Como o aviso funciona.** O app agenda o aviso
  ([`data/LeaveAlerts.kt`](app/src/main/java/io/github/giova/metroceara/data/LeaveAlerts.kt),
  `AlarmManager`). Na hora, [`trip/LeaveReceiver.kt`](app/src/main/java/io/github/giova/metroceara/trip/LeaveReceiver.kt)
  confere a grade do dia (domingo sem operação, linha que o Metrofor avisou que
  parou, horário que sumiu numa operação especial: vale o seguinte) e mostra,
  **mesmo com a tela bloqueada**, "Hora de sair: metrô das 12:14 · São Benedito,
  sentido Carlito Benevides. Se perder, o próximo é às 12:44", com a **contagem
  até a partida** ("parte em 24:40", atualizada pelo próprio Android) e os botões
  **Iniciar viagem** (com destino: abre o roteiro calculado) ou **Ver horários**,
  **Mais 5 min** e **Cancelar**. A notificação some 2 min depois da partida. Para
  chegar no minuto certo, o Android 14+ pede que a pessoa libere "Alarmes e
  lembretes" (`SCHEDULE_EXACT_ALARM`): a janela explica e leva à tela do
  Android; sem isso, o aviso pode atrasar alguns minutos. Pede também a
  permissão de notificação, se faltar. Os avisos voltam a ser marcados depois de
  reiniciar o celular, de atualizar o app e de liberar o horário exato, e um
  aviso já dado não se repete ao abrir o app.

A lite não consegue avisar sozinha (o navegador não deixa): ela baixa um evento de
agenda (`.ics`).

**Rotinas** (o trajeto do dia a dia): no resultado de "Para onde você vai?",
**"Repetir este trajeto (rotina)"** abre a tela da rotina (`ui/RoutinesScreen.kt`)
já preenchida: nome, de, para, dias (segunda a sexta por padrão), **Chegar até**
(a hora do roteiro, arredondada) ou **Sair a partir de**, e quanto tempo antes
avisar. Nos dias marcados, umas horas antes (nunca antes das 00:50, quando a
grade do dia já saiu), o app calcula o metrô do dia pela grade e pelo roteiro
(`RoutinePlanner`): "chegar até" pega o que sai mais tarde e ainda chega a
tempo. **Domingo sem operação não avisa; em feriado e operação especial vale a
grade do dia, e linha parada fica de fora.** O aviso é o mesmo de "Avisar pelo
app", com **Hoje não vou** no lugar de Cancelar. A tela **Suas rotinas** (com uma
linha "Rotinas: …" na tela inicial, quando há alguma) mostra cada
uma com a chave de ligar e o aviso já marcado ("Aviso marcado: sair às 11:44
para o metrô das 12:14").

A caminhada até a estação só entra no "Sair agora" quando a pessoa está a até o
limite do modo viagem (Configurações, 3 km): mais longe, ela está planejando, e
somar 2 h a pé deixaria o roteiro vazio.

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

Por isso o **app baixa o feed direto do Metrofor**
(`https://info.metrofor.ce.gov.br/gtfs_file`, em `data/ScheduleStore.kt`),
**sempre que é aberto ou volta para a frente**, sem comparar com a guardada: são
~30 KB, e qualquer mudança chega na abertura seguinte. A grade muda também
**durante o dia**: em 04/11 o Metrofor publicou seis versões da Linha Sul até as
12h27 (40–50 viagens deslocadas de −2 a +10 min de manhã; às 10h17, 39 viagens
adiadas ~6 min, desfeito às 12h27). Baixando direto, o app vê cada versão na
hora; pelo espelho, só quando a tarefa rodava. Antes das **00:40** o app não
baixa: o feed não diz de que dia é e o Metrofor pode ainda servir a de ontem.
Nesse intervalo, sem a grade de hoje, a tela diz "O Metrofor publica os
horários de hoje depois da meia-noite e quarenta. Até lá, o app mostra os que
já tem." (em cinza, e não "Conecte-se à internet"); com o app aberto, ele
baixa sozinho às 00:40. Num **domingo sem operação anunciada** o app não baixa:
a grade desses dias é cópia da de dia útil e não mostraria nada.
Não há reserva no espelho: se o Metrofor falhar, o app fica com a última grade
guardada do mesmo tipo de dia, e o banco do APK é o último recurso. O banco é
montado com o mesmo resultado de `tools/build_db.py` (`data/GtfsImporter.kt`,
uma tradução direta do script, que gera tabelas idênticas para o mesmo feed), e
o app guarda a última grade de cada tipo de dia (dia útil, sábado e domingo) com
a data do download. Um download cortado (app fechado no meio) é refeito na
próxima abertura. De onde vêm os horários ("Horários
de hoje, baixados do Metrofor às 02:42.") fica em **Fonte ›**, na tela da
estação; no topo da tela inicial só aparece, como **aviso em vermelho**, quando
importa: a grade do APK, ou dia com operação especial anunciada sem a
grade do dia ("Hoje tem operação especial e os horários do dia ainda não
chegaram. Veja a notícia do Metrofor.").

A **versão lite** não consegue baixar do Metrofor (o site não libera acesso de
outras páginas) e lê o **espelho**: a tarefa do GitHub
([`dados-web.yml`](.github/workflows/dados-web.yml)) baixa o feed e guarda, no
ramo `dados`, o zip original e o JSON de cada tipo de dia, com o `indice.json`
dizendo a data, o tamanho e a hora de cada um. Ela roda de madrugada (00:47,
01:17, 01:47, 02:47 e 04:17 de Fortaleza, porque o Metrofor publica a grade do
dia logo depois da meia-noite e o GitHub chega a atrasar ~5 h), às 10:00, 15:00
e 20:00 e, desde 04/11, a cada 30 min das 05:17 às 23:47, para a lite
acompanhar as mudanças do dia. Nada entre 00:00 e 00:40. Um agendador externo
(cron-job.org) dispara a tarefa pela API do GitHub, sem atraso; o agendamento
do GitHub fica como reserva. O download do feed tenta três vezes (espera 30 s e
depois 90 s), porque o site do Metrofor às vezes não responde por alguns
minutos. A lite mostra a origem dos horários no rodapé.

Detalhes que valem saber:

- **Certificado.** Em out/2026 o certificado de `*.metrofor.ce.gov.br` vinha
  com a cadeia incompleta, e por isso o app passou um tempo baixando só do
  espelho. Em 04/11 o de `info.metrofor.ce.gov.br` estava correto (Let's
  Encrypt, cadeia até a ISRG Root X1), e o app voltou a baixar de lá com a
  verificação normal do Android, sem aceitar certificado inválido. A ISRG Root
  X1 só existe no Android a partir do 7.1.1; como o app aceita o 7.0, ela vai
  junto (`res/raw/isrg_root_x1.pem`), valendo só para esse domínio
  (`res/xml/network_security_config.xml`). Se o certificado voltar a falhar, o
  app segue com a última grade guardada (`sync_fail` no registro de testes). A
  tarefa do espelho desliga a verificação (são dados públicos).
- **Nada de trava por "grade menor".** Como a grade muda de verdade de um dia
  para outro, o app só recusa um feed que não dá para ler ou que não tem linhas
  nem estações. Zero viagens é aceito: num domingo comum pode ser exatamente a
  verdade.
- **Domingo.** O aviso de domingo ("não circulam") aparece a não ser
  que o Metrofor tenha anunciado operação especial para o dia (veja
  **Operação especial**, abaixo). A grade do dia não serve de sinal: num domingo
  comum (18/10/2026) o feed trouxe a grade de dia útil inteira, e no da eleição
  (25/10) uma grade própria. Por isso, num domingo **sem operação anunciada**,
  **nenhum horário aparece** (`isSundayWithoutService`, em `data/Clock.kt`):
  hora na tela dá a impressão de que hoje tem metrô, e a pessoa vai à estação
  à toa. Tela inicial, widget, tela da estação e roteiro mostram só "Hoje é
  domingo: metrô e VLT não circulam. Voltam segunda, a partir das 05:30." (sem
  a grade de segunda conhecida, "Só há operação aos domingos quando o Metrofor
  anuncia"). Saem as próximas partidas, os horários de segunda, o próximo da
  integração, o roteiro e o modo viagem; a tela da estação traz também a
  notícia de amanhã (feriado na segunda). Até a v1.79 a tela mostrava os
  horários de segunda. A lite e a página da estação no site fazem o mesmo.
- **Depois da última viagem do dia**, as partidas de "amanhã" vêm da grade do
  tipo de dia de amanhã, e não da de hoje repetida: num domingo de eleição,
  segunda mostra a grade de dia útil. Se amanhã for sábado e essa grade nunca
  tiver sido baixada, o app não inventa horário. A previsão usa a última grade
  de um **dia comum** do mesmo tipo: a de um feriado não serve (a de Finados
  virou a previsão da terça seguinte). Por isso, a cada grade de dia comum
  baixada, o app guarda uma cópia à parte (`normal_<tipo>.db`); se a guardada
  for de um dia especial, a previsão usa essa cópia ou a do APK. A lite usa a
  última comum do espelho (`normais` no índice).
  **Se amanhã tiver operação especial anunciada**, o app não prevê: a grade
  desse dia só sai depois da meia-noite, e o fim do dia diz "amanhã o horário é
  especial e só sai depois da meia-noite" (o widget, "Amanhã: horário
  especial"). A lite faz o mesmo. **Se amanhã for domingo**, a
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
[página oficial de GTFS](https://www.ce.gov.br/metrofor/gtfs/)), gerado a partir
do sistema INFO-Metrofor e sob licença Creative Commons Attribution.

Os números do Metrofor que explicam as escolhas do app (demanda por estação e
por hora, pesquisa origem-destino, pontualidade por linha, cores do mapa
oficial, bilhetagem) estão em [DADOS-DO-METROFOR.md](DADOS-DO-METROFOR.md),
com a fonte de cada um.

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
   e na de partidas e, sem operação anunciada pelo Metrofor, não mostra horário
   nenhum, só quando o metrô volta na segunda.
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
11. O domínio `metrofor.ce.gov.br` já teve **certificado HTTPS expirado ou com
   a cadeia incompleta** e redireciona para `ce.gov.br/metrofor`. Por isso os
   scripts desligam a verificação de certificado. O app baixa o feed de
   `info.metrofor.ce.gov.br` com a verificação normal (veja
   [Horários do dia](#horários-do-dia)).

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
tools/fontes.py                      fontes do regras.json e quais estão com a conferência vencida
tools/site.py                        site: páginas das estações e linhas, mapas e sitemap em docs/
tools/site_previa.py                 prévia de link do site (docs/img/previa.png) com o mapa
tools/site_fotos.py                  fotos das estações do Wikimedia Commons (docs/img/estacoes/)
app/src/main/assets/metrofor.db      banco gerado (não editar à mão)
app/src/main/assets/regras.json      regras fora do GTFS: bike, integrações, passagem,
                                     guia da passagem, endereços, acessibilidade, cores,
                                     operações especiais, lugares
DADOS-DO-METROFOR.md                 números e fontes do Metrofor por trás das decisões
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

## Registro das viagens (enviado por e-mail, se a pessoa quiser)

Para receber registros de outras pessoas sem que nada saia do celular sozinho
([`data/TripLog.kt`](app/src/main/java/io/github/giova/metroceara/data/TripLog.kt)).
Vem **ligado**: guarda cada viagem do modo viagem em `files/viagens/`, um
arquivo por viagem, com as mesmas linhas do registro de testes (os eventos
`trip_*`), **do embarque até o fim**. As posições de antes do embarque (a
caminhada até a estação, que pode mostrar onde a pessoa mora) e as da tela
inicial ficam de fora. Os arquivos somem depois de **7 dias**.

Em **Configurações › Registro das viagens**: desligar, ver quantas viagens há,
**Enviar por e-mail** e Apagar. O envio abre a tela de compartilhar com o
e-mail montado: para giovanildos@gmail.com, assunto "Registro de viagem · v1.56
· 29/10 · Benfica → Carlito Benevides" (ou "… · 5 viagens"), um **resumo** de
cada viagem no corpo (hora, linha, trecho, como terminou, erro da chegada,
minutos sem GPS, versão do app e do Android, aparelho) e as viagens num `.zip`
anexo. A pessoa vê tudo antes de mandar pelo app de e-mail dela, então para a
Play Store não é coleta pelo app.

**Convite só em viagem com problema**
([`trip/TripReview.kt`](app/src/main/java/io/github/giova/metroceara/trip/TripReview.kt)):
3 min ou mais sem GPS, chegada mais de 3 min diferente da prevista na hora do
aviso de descida, viagem encerrada pela pessoa depois de embarcar e antes do
destino, ou o app desistiu de esperar. Uma notificação **sem som** ("Essa
viagem teve algum problema?") abre o e-mail daquela viagem. No máximo **um
convite por semana**; depois de **dois convites seguidos sem resposta**, o app
para de perguntar; mandar o registro (pelo convite ou pelas Configurações)
zera a conta. Viagem simulada não convida. Cada fim de viagem grava
`trip_review` (problemas, erro da chegada, maior tempo sem GPS).

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

Toda linha leva a versão do app em `"v"` (um registro ligado no meio do dia
não tem o `app_start`); é por ela que se sabe o que cada evento grava. Até a
1.54 as linhas não têm `"v"`, o `log_on` não tem os dados do aparelho e não há
`trip_option`. Eventos (`"ev"`): `app_start` (versão, Android,
aparelho), `schedule_source`
(de onde veio a grade em uso), `sync_start`/`sync_ok`/`sync_fail` (download dos
horários; o `sync_ok` resume viagens e horários por linha), `loc_request`/
`loc_ok`/`loc_fail` (pedido de localização, provedor, tempo, precisão),
`home_gps` (posição exata e estação mais próxima na tela inicial),
`trip_start` (linha, paradas e horários previstos, aviso), `trip_gps` (cada
posição durante a viagem), `trip_progress` (mudança de estação, se veio do GPS
ou da tabela, atraso, GPS sem sinal), `trip_alert`, `trip_station`,
`trip_turnaround`, `trip_option` (som do aviso ou "Avisar cada estação" ligado
ou desligado durante a viagem), `trip_review` (desde a 1.56: problemas da
viagem, erro da chegada, maior tempo sem GPS), `trip_end` (motivo),
`widget_locate` (desde a 1.56: o ↻ do widget, estação achada ou `no_fix`),
`crash` (erro que derrubou o app) e `log_on`/`log_off` (o `log_on` leva os
mesmos dados do `app_start`).

## Versão lite (web)

Para quem tem iPhone (e para quem prefere não instalar APK) há uma versão
web instalável (PWA), a **versão lite**, neste mesmo repositório. Fica de
fora de propósito o que depende do celular em segundo plano: modo viagem, voz,
registro de testes — esses ficam só no app de Android. A tela inicial segue a
mesma ordem padrão do app (Trabalho e Casa, o aviso do Metrofor de hoje, as duas
estações mais próximas com o tempo que falta grande, os avisos dos próximos
dias — toda operação anunciada, desde o dia em que a notícia sai),
sem "Ajustar para mim" nem a escolha da ordem. A mesma barra de baixo do app:
Início, Planeje sua viagem (`#/rotas`), Linhas (`#/linhas`) e Mais (Passagem,
Nina, De onde vem cada informação, Sobre, Compartilhar; um `<dialog>` que sobe
de baixo). O navegador não consegue baixar
o GTFS direto do Metrofor — o site não libera acesso a partir de outras
páginas —, então os horários chegam prontos:

- [`tools/build_web_data.py`](tools/build_web_data.py) baixa o feed e gera JSON,
  reaproveitando o tratamento do `build_db.py` (nomes, cores, Bicicletar) e as
  integrações do `regras.json`. Como no app, guarda **uma grade por tipo de
  dia** (`horarios-dia-util.json`, `horarios-sabado.json`,
  `horarios-domingo.json`), atualizando só a do dia; `indice.json` diz a data
  de cada uma. Feed vazio ou quebrado não substitui a grade boa. Em dia de
  operação especial anunciada, a grade vai para `horarios-<tipo>-especial.json`
  e `gtfs-<tipo>-especial.zip`: `tipos` no índice aponta para ela (é a de hoje,
  e as versões antigas do app seguem certas) e `normais` mantém a última grade
  comum de cada tipo, que a lite usa para prever amanhã. Cada sentido
  traz a ordem das estações e as viagens como listas de minutos alinhadas a
  ela (`null` = não para): a grade de um dia útil inteiro tem ~23 KB. Junto
  vai o zip original (`gtfs-<tipo>.zip`), que as versões até a 1.75 do app
  Android baixam (desde a 1.76 o app baixa do Metrofor).
- A tarefa [`dados-web.yml`](.github/workflows/dados-web.yml) roda o script
  de madrugada, às 10:00, 15:00 e 20:00 e a cada 30 min das 05:17 às 23:47 de Fortaleza, disparada pelo cron-job.org (ou à mão, em Actions → "Dados da versão
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
  dia, próximas partidas (acabando as de hoje, seguem as de amanhã, pela grade
  comum; com amanhã domingo, o fim do dia mostra a segunda; com operação
  especial no próximo dia, o aviso no lugar dos horários) e
  distância. É a tradução das mesmas regras do app.
- `app.js` — as telas: início (estação mais perto de cada linha até 3 km, com a
  próxima partida em cada sentido; a localização só é pedida ao tocar no
  botão), linhas, estação (abas por sentido, sem o sentido que termina nela;
  endereço e passagem, com o link para o guia da passagem em `#/passagem`,
  e a acessibilidade da estação em `#/acesso/…`;
  próximas partidas e todos os horários), **Sua viagem**
  (`#/viagem`, a busca "Para onde você vai?", com os atalhos Casa e Trabalho
  em `#/lugar/…` e `#/ir/…`) e configurações.
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

## Site e página de apresentação

`docs/` é o site público (GitHub Pages, publicado a partir de
`main`/`docs`): `giovanildo.github.io/metro-ceara-horarios`. Desde 07/11 é
também um **guia das estações e linhas**, o oposto da lite: a lite é enxuta e
serve a quem está na plataforma; o site é "maximalista", para quem planeja ou
chega pelo Google ("horário metrô Parangaba"). Tudo no mesmo endereço, que o
Google já conhece, em pastas próprias — a lite em `docs/app/` não é tocada.

- **Página principal** (`docs/index.html`, feita à mão): busca de estação,
  mapa de Fortaleza desenhado pelas posições reais das estações (cada ponto
  leva à página da estação), avisos do Metrofor dos próximos dias (ao vivo),
  atalhos, mapas do Cariri e de Sobral e, embaixo, a apresentação do app —
  o que faz, por que é seguro, como instalar, `#baixar`/`#instalar` e apoio.
- **Uma página por estação** (`docs/estacoes/<estação>/`, 62) e **por linha**
  (`docs/linhas/<linha>/`, 7), mais as listas `docs/estacoes/` (com filtro;
  aceita `?q=`) e `docs/linhas/`. A estação traz próximas partidas, primeiro
  e último horário (dia útil, sábado, domingo), intervalo típico, vizinhas na
  linha, passagem, integração com a passagem em cada sentido, acessibilidade,
  lugares a até 1 km a pé e links para a lite e o Google Maps; título,
  descrição e dados estruturados (`TrainStation`, `BreadcrumbList`) para o
  Google. Parangaba e Expedicionários (duas linhas na mesma estação) são uma
  página só; Padre Cícero do Cariri é `padre-cicero-cariri`.
- **Gerado por [`tools/site.py`](tools/site.py)**, a partir do `regras.json`
  e da grade comum (`normais`) de dia útil e sábado do ramo `dados`. O que é
  fixo vai escrito no HTML; as próximas partidas e os avisos cada página busca
  no navegador ([`docs/site/agora.js`](docs/site/agora.js) e
  [`docs/site/inicio.js`](docs/site/inicio.js)) com o **mesmo código da lite**
  (`docs/app/dados.js`) — mudou esse arquivo, confira o site também. Na página
  principal, o gerador só troca os trechos `<!--gerado:nome-->…<!--/gerado:nome-->`
  (mapas, lista da busca, número de estações e linhas). Também reescreve o
  `sitemap.xml`. Rode de novo quando mudar o `regras.json` ou a grade comum:

  ```bash
  python3 tools/site.py          # baixa a grade do ramo dados
  python3 tools/site.py /tmp/d   # ou usa uma pasta gerada pelo build_web_data.py
  ```

  Os nomes que o GTFS escreve sem acento são corrigidos só no site (`NOMES`);
  quais estações têm o nome escrito no mapa e de que lado ficam `ROTULOS`,
  `ESQUERDA`, `ACIMA` e `ABAIXO`. O visual mora em
  [`docs/site/site.css`](docs/site/site.css), com tema escuro.
- **Fotos das estações** (desde 07/11): 409 fotos de 44 estações, do
  [Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:Fortaleza_Metro)
  (CC BY-SA e CC0; a maioria de Joelkaula e Ruan Moreira).
  [`tools/site_fotos.py`](tools/site_fotos.py) percorre as categorias de cada
  estação, baixa em 960 px, grava em WebP sem os metadados do arquivo (some o
  GPS) em `docs/img/estacoes/<estação>/`, faz uma cópia da capa em JPG para a
  prévia de link e escreve `tools/site_fotos.json` com autor, licença, link e
  data. Só aceita CC BY, CC BY-SA, CC0 e domínio público; pula foto cujo nome
  diz que é de outra estação (categoria trocada no Commons). Escolhas feitas
  olhando: `CAPA` (a primeira) e `NO_FIM`. Na página, as 4 primeiras ficam no
  topo e as outras no fim, carregando só ao rolar (`FOTOS_NO_TOPO` no
  `site.py`); cada uma com autor, licença e mês, como a licença pede. Rodar de
  novo baixa só o que é novo e apaga o que saiu do Commons:

  ```bash
  python3 tools/site_fotos.py && python3 tools/site.py
  ```
- **Prévia de link** (a imagem que aparece ao mandar o endereço no WhatsApp):
  `docs/img/previa.png`, 1200×630, com o nome e o mapa de Fortaleza. Sai de
  [`tools/site_previa.py`](tools/site_previa.py), que monta a página com o
  mesmo mapa do `tools/site.py` e tira o print com o Chrome sem janela
  (`python3 tools/site_previa.py`). O WhatsApp guarda a prévia antiga por um
  tempo; para ver a nova logo, mande o link com algo no fim (`?v=2`).
- **Google Search Console** (desde 07/11): propriedade "Prefixo do URL"
  `https://giovanildo.github.io/metro-ceara-horarios/`, verificada pelo arquivo
  [`docs/googlee146230595bf6c09.html`](docs/googlee146230595bf6c09.html) —
  **não apague esse arquivo**: sem ele, o Google desfaz a verificação. O
  `sitemap.xml` foi enviado lá; o relatório Desempenho mostra por quais
  pesquisas as pessoas chegam às páginas.

O endereço antigo, `giovanildo.github.io/metro-fortaleza-horarios/`, é servido
pelo repositório [`metro-fortaleza-horarios`](https://github.com/giovanildo/metro-fortaleza-horarios),
que só leva ao endereço novo: qualquer caminho (inclusive `/app/` e as âncoras)
vai para o mesmo caminho aqui, e o service worker de quem instalou a versão
lite antiga se desliga e abre a nova. Não apague esse repositório enquanto
houver QR codes ou links antigos por aí.

**A versão lite é a porta de entrada.** A instalação de um APK de fora da loja
mostra avisos que espantam o usuário comum; a versão lite abre na hora, sem
nenhum. Por isso o botão principal da página é **"Horários agora"**
(`app/`), e o secundário, **"App para Android"**, desce até o bloco
`#baixar`. A página não oferece mais instalar o APK direto (desde 27/10):
o bloco `#baixar` da seção "Como instalar o app completo" diz que o app está em
fase de testes na Play Store e convida a virar testador beta: o botão **"Quero
ser testador beta"** abre o WhatsApp (`wa.me/5585997619295`) com a mensagem
pronta pedindo o Gmail da Play Store (é ele que entra na lista de testadores do
Console), e o e-mail fica como alternativa.
O vídeo de instalação do Android saiu da página; o do iPhone ficou. O QR e o "Divulgar o app" do app Android também levam à
versão lite. Dentro dela, uma linha fixa no topo da tela inicial convida a ser
testador ("No app completo, o celular avisa a hora de descer. Seja testador beta ›"
no Android, com ✕ para fechar; "Tem Android? Seja testador beta do app completo ›"
nos outros aparelhos, sem ✕) e abre o WhatsApp com a mesma mensagem pronta do
`#baixar`. Tem
as marcações que o Google e as redes sociais leem (título, descrição, Open
Graph com `img/previa.png` de 1200×630 para a prévia do link no WhatsApp,
dados estruturados `SoftwareApplication`, `robots.txt` e `sitemap.xml`).

A **política de privacidade** (`docs/privacidade.html`, link no rodapé da página,
na tela Sobre da versão lite e na tela Sobre do app — a Play Store exige o link
também dentro do app) cobre o app e a versão lite: nenhum dado
pessoal é coletado ou enviado; a localização é usada só no aparelho; a internet
serve só para baixar os horários (do Metrofor, no app; do espelho no GitHub, na
lite) e as regras do espelho. É o link exigido pela
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
  1024×500 e 7 capturas 1080×1920 (a loja exige proporção 16:9 ou 9:16; as telas
  de 1080×2400 são cortadas embaixo): tela inicial, estação com integração,
  aviso de descida, widget, lembrete na agenda, viagem com integração e
  "sentado, pelo terminal"; o site usa as 7, com o widget em 2º lugar. Na do widget, a ponta dos ícones de
  outros apps, que sobra no corte de 1920, foi coberta com o fundo logo acima. A tela do aviso de descida é a tela real da
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
do APK. O app não tem nada que a análise costuma barrar: fala com o site do
Metrofor com a verificação normal de certificado e não pede doação.

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
  Estado do Ceará, sob licença Creative Commons Attribution.
- **Cores das linhas** — mapas oficiais da rede afixados nas estações
  ([site do Metrofor](https://www.ce.gov.br/metrofor/mapas/)).
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
cores das linhas são as do mapa afixado nas estações e servem só para
identificá-las. Quem for publicar o app em loja deve manter esses cuidados —
nome, ícone e descrição sem sugerir vínculo oficial.

Pelo mesmo motivo, o app **não segue o manual de identidade visual do Governo
do Ceará** (2023–2026: brasão, assinatura "CEARÁ – Governo do Estado", fontes
Kanit e Maitree, grafismo verde e laranja). Avaliado em out/2026, ele faria o
app parecer oficial — o que a Play Store não aceita sem comprovação — e vale
só até o fim da gestão. A identidade do app é própria: o ícone de renda com a
jandaia e o vermelho do app.

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
