# Dados do Metrofor que embasam o app

Fatos e números de fontes públicas do Metrofor e do Governo do Ceará, consultados
em out/2026, que explicam decisões do app. Os documentos originais (PDFs,
planilhas, prints) ficam numa pasta local `referencias/`, fora do git: não são
nossos para republicar, somam mais de 500 MB e alguns trazem dados pessoais
(lista de credenciados com CPF, endereços de entrevistados). Aqui vão só os
números agregados, com a fonte de cada um.

## Fontes

| Fonte | Onde |
|---|---|
| GTFS oficial (licença Creative Commons Attribution) | [ce.gov.br/metrofor/gtfs](https://www.ce.gov.br/metrofor/gtfs/) → `info.metrofor.ce.gov.br/gtfs_file` |
| Painel de estatísticas (Looker Studio; transcrito em 30/10/2026) | [ce.gov.br/metrofor/institucional/estatisticas](https://www.ce.gov.br/metrofor/institucional/estatisticas/) |
| Relatórios de demanda trimestrais (2022–3º tri/2025) e Pesquisa Origem-Destino da Linha Sul 2022 | mesma página, subpáginas |
| Cartas anuais de políticas públicas e governança (2020–2025), relatórios de sustentabilidade, planejamento estratégico 2021 | [ce.gov.br/metrofor/governanca/relatorios](https://www.ce.gov.br/metrofor/governanca/relatorios/) |
| Demonstrativo de receitas, custos e despesas | [ce.gov.br/metrofor/governanca/demonstracoes-contabeis](https://www.ce.gov.br/metrofor/governanca/demonstracoes-contabeis/) |
| Convênio 01/2026 com o Sindiônibus, relatórios do SIC 2014–2025, atas dos conselhos | [ce.gov.br/metrofor/acesso-a-informacao](https://www.ce.gov.br/metrofor/acesso-a-informacao/) |
| Tabelas de preço, regulamento de negócios 2026, concessão de mídias | [ce.gov.br/metrofor/negocios](https://www.ce.gov.br/metrofor/negocios/) |
| Mapas oficiais da rede (2025) | [ce.gov.br/metrofor/mapas](https://www.ce.gov.br/metrofor/mapas/) |
| Notícias (operações especiais) | [ce.gov.br/metrofor](https://www.ce.gov.br/metrofor/) — lidas por `tools/operacoes.py` |

O site do governo envia o certificado TLS sem o intermediário: para baixar com
verificação, junte o intermediário da Amazon (`crt.r2m01.amazontrust.com/r2m01.cer`,
DER → PEM) ao repositório de CAs do sistema e use `curl --cacert`. Não desligue a
verificação.

## O sistema

| Linha | Veículo | Extensão | Estações | Funcionamento | Passagem (2025–26) |
|---|---|---|---|---|---|
| Sul (Linha 1, vermelha) | trem elétrico (TUE), 2 unidades = 1.780 lugares | 24,1 km, via dupla | 20 (4 subterrâneas) | seg–sáb, 5h30–23h | R$ 3,60 (meia R$ 1,80) |
| Oeste (Linha 2, verde) | VLT diesel, 4 carros = 756 lugares | 19,5 km, via singela | 10 | seg–sáb, 5h30–19h55 | R$ 1,00 |
| Nordeste / Parangaba–Mucuripe (Linha 3, azul) | VLT diesel | 13,3 km, via dupla | 11 | seg–sáb, 5h30–23h09 | gratuita (implantação) |
| Ramal Aeroporto (Linha 5, laranja) | VLT | 2,4–2,5 km | 2 | desde fev/2026 | — |
| Sobral, Norte (laranja) e Sul (vermelha) | VLT diesel, 378 lugares | 12,5 km (13,9 km na carta anual) | 12 | seg–sáb, 5h30–23h | R$ 1,00 |
| Cariri (vermelha) | VLT diesel, 378 lugares | 13,6 km | 9 | seg–sex 6h–19h, sáb 6h–13h30 | R$ 1,00 |

Domingo: sem operação, salvo operação especial (eleição, ENEM, eventos) — embora
o GTFS diga que há. A Linha Sul teve em média 134 viagens programadas por dia em 2025.
A Linha Leste (Centro–Papicu, 7,3 km, 4 estações subterrâneas a partir da Chico
da Silva) está em obras desde 2018.

**Cores.** O mapa oficial afixado nas estações numera e colore as linhas
(tabela acima). O GTFS usa outras cores (Nordeste e Aeroporto roxos, Oeste
verde-limão, Sobral Norte azul, Cariri verde); o app usa as do mapa
(`regras.json`, seção `cores`).

## Demanda

**Passageiros por ano** (painel; 2026 = jan–set):

| 2017 | 2018 | 2019 | 2020 | 2021 | 2022 | 2023 | 2024 | 2025 | 2026 |
|---|---|---|---|---|---|---|---|---|---|
| 9,29 mi | 13,23 mi | 16,74 mi | 9,57 mi | 12,28 mi | 15,73 mi | 16,85 mi | 16,68 mi | 16,50 mi | 12,09 mi |

**Média por dia útil:** 58.899 (2023, máximo da série), 58.106 (2024),
57.302 (2025), 56.569 (2026) — estável, com leve queda. Em 2026: Linha Sul
33.536, Nordeste 14.241, Oeste 4.900, Sobral 2.459, Cariri 1.324.

**Embarques por estação desde 2017** (milhões; painel):

- **Linha Sul** (82,5 mi): José de Alencar 15,3 · Parangaba 8,9 · Maracanaú 7,7 ·
  Esperança 6,3 · Benfica 5,9 · São Benedito 5,3 · Carlito Benevides 4,7 ·
  Virgílio Távora 4,5 · Aracapé 4,1 · Rachel de Queiroz 3,4 · Manoel Sátiro 2,6 ·
  Jereissati 2,4 · Vila Pery 2,2 · Mondubim 1,9 · Porangabussu 1,9 · Alto Alegre 1,4 ·
  Couto Fernandes 1,3 · Juscelino Kubitschek 1,2 · Chico da Silva 1,2 · Padre Cícero 0,34.
- **Nordeste** (24,8 mi): Parangaba 7,5 · Papicu 3,3 · Antônio Sales 2,0 · Vila União 2,0 ·
  Iate 1,9 · Borges de Melo 1,8 · São João do Tauape 1,7 · Montese 1,5 ·
  Pontes Vieira 1,3 · Mucuripe 0,90 · Expedicionários 0,72.
- **Oeste** (17,9 mi): Moura Brasil 4,3 · Caucaia 2,7 · Jurema 2,1 · São Miguel 1,7 ·
  Araturi 1,5 · Álvaro Weyne 1,4 · Parque Albano 1,2 · Antônio Bezerra 1,2 ·
  Conjunto Ceará 0,91 · Padre Andrade 0,90.
- **Sobral** (9,2 mi): Boulevard do Arco, Campo dos Velhos, Cohab III e José Euclides
  (~1,0–1,45 cada) à frente; Dom José, Dom Expedito e Novo Recanto (~0,37) por último.
- **Cariri** (3,8 mi): Crato 1,16 · Juazeiro 0,65 · Fátima 0,50 · Teatro 0,46 ·
  Padre Cícero 0,29 · Muriti 0,29 · Escola 0,21 · Antônio Vieira 0,16 · São José 0,11.

As 6 primeiras da Linha Sul somam 60% dela; as 4 subterrâneas (José de
Alencar, São Benedito, Benfica, Chico da Silva), 34%.

**Quando e para onde** (relatório de demanda do 3º tri/2025):

- Pico das 6h às 8h e das 16h às 19h (45% da demanda); hora mais cheia, 17h–18h.
- Terça é o dia mais cheio; sábado tem ~13% da semana (Cariri 8,7%); domingo, "inexpressivo".
- Viagem média: Linha Sul 12,7 km (60% até ~13 estações); Sobral 2,7 km (~3 estações).
- Pares mais comuns na Linha Sul: José de Alencar–Esperança 5,4%, –Aracapé 4,2%,
  Parangaba–Maracanaú 3,8%, José de Alencar–Maracanaú 3,4%, Parangaba–José de Alencar 3,1%.
- 44% das viagens da Linha Sul ligam Fortaleza e Maracanaú.
- Tendência (3º tri 2025 × 2024): Nordeste +9%, Sul +1,4%, Oeste −13%, Cariri −7,6%, Sobral −32%.
- Bicicletas levadas nos trens, jan–set/2025: 30.709 (Sul 20.466, Nordeste 9.456,
  Sobral 787), +35% no ano.
- Embarques por hora em ago/2019 (apresentação do edital de Wi-Fi): às 6h lideram
  Maracanaú, Carlito e Esperança; às 18h, José de Alencar, São Benedito e Parangaba.

**Pesquisa Origem-Destino da Linha Sul 2022** (1.731 entrevistas, ~23 mil viagens/dia
expandidas): 64% usam de 4 a 6 vezes por semana; 62,5% chegam a pé (ônibus 12,4%,
carro 9,6%); a pé até a estação, mediana de 580 m, 75% até 1 km, 90% até 1,8 km;
31% com renda familiar de até 1 salário mínimo; 84% pagam inteira; 22% vão para a
José de Alencar.

## Qualidade da operação

| Pontualidade | 2019 | 2020 | 2021 | 2022 | 2023 |
|---|---|---|---|---|---|
| Linha Sul | 96,70 | 98,08 | 98,48 | 96,27 | 97,09 |
| Linha Oeste | 96,13 | 93,25 | 95,75 | 96,04 | 94,09 |
| Cariri | 95,43 | 92,94 | 97,82 | 93,92 | 94,24 |
| Sobral | 88,87 | 92,73 | 78,28 | 84,94 | 81,43 |
| Nordeste | 98,33 | 95,52 | 92,34 | 94,59 | 86,01 |

Regularidade (viagens feitas) em 2023: Sul 98,36, Oeste 99,76, Cariri 94,89,
Sobral 98,25, Nordeste 99,77 (carta anual 2023). O acompanhamento da operação
é por rádio, sem rastreamento digital dos trens (planejamento estratégico 2021).

## Passagem e bilhetagem

- Sem reajuste desde 2019. A passagem cobriu ~11% da receita em 2025
  (R$ 29,4 mi de bilhetes × R$ 237,3 mi de subvenção do Estado).
- Hoje só a Linha Sul e Sobral têm catraca eletrônica (cartões do Metrofor);
  pagamento em dinheiro ou Pix. Nas 4 estações subterrâneas o sinal do
  celular falha; Wi-Fi nas estações só existe como chamamento a empresas (2019, 2024).
- **Convênio 01/2026 com o Sindiônibus** (abr/2026): o Metrofor vai usar a
  bilhetagem dos ônibus em todas as linhas (Vai&Vem, Bilhete Único Metropolitano,
  vale-transporte, estudante, gratuidades), para integração tarifária e operacional
  com os ônibus. Quando entrar em operação, a seção `passagem` do `regras.json` muda.

## Outros canais de informação

- **Portal de horários e planejamento de viagens** do Metrofor (2025), em
  `info.metrofor.ce.gov.br` — a mesma base do GTFS.
- **"Metrofor App"** oficial só no iPhone (App Store; nota 1,4 em out/2026); não há
  versão para Android. O estudo da concessão de mídias (consulta pública em 2025)
  prevê que a concessionária modernize esse app, com tempo real e publicidade.
- Moovit também mostra metrô e VLT.

## Como os dados viraram decisões no app

| Dado | Decisão |
|---|---|
| GTFS diz que há metrô aos domingos | aviso de domingo; dias de operação especial lidos das notícias |
| GTFS põe estações de Sobral e do Cariri até 2 km fora do lugar | posição corrigida no `regras.json` (OpenStreetMap) |
| Cores do GTFS ≠ mapa das estações | cores do mapa oficial, com tom mais claro no tema escuro |
| 4 estações subterrâneas = 34% dos embarques da Sul; sem sinal nem Wi-Fi | aviso do Pix nessas estações; modo viagem segue pela tabela quando o GPS some |
| Viagem nº 1: periferia da Sul → José de Alencar | prioridade nos testes de campo (aviso de descida no trecho subterrâneo) |
| Parangaba (Sul + Nordeste) é o maior ponto de troca | roteiro com integração e viagem que segue sozinha entre trechos |
| Pico 6–8h e 16–19h, máximo 17–18h | faixas de `pico` do `regras.json` (sentado pelo terminal, tempo de integração) |
| 64% usam de 4 a 6 vezes por semana | widget e lembrete de saída |
| A pé até a estação: 75% até 1 km, 90% até 1,8 km | modo viagem pergunta acima de 1 km e some acima de 3 km |
| Pontualidade baixa na Nordeste (86%) e em Sobral (81%) | modo viagem segue o GPS, não só a grade |
| 31% com renda de até 1 salário mínimo | app leve, funciona sem internet; versão lite no navegador |
| Viagens curtas (≤ 4 estações) são previsíveis | modo viagem não é otimizado para elas; aviso a cada estação cobre quem não conhece a linha |
| Nenhum app do Metrofor no Android | razão de ser deste app |
