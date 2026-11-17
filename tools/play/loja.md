# Play Store — textos e respostas para o Play Console

Tudo o que o Play Console pede, pronto para colar. Os campos estão na ordem do
Console. As regras mudam: se algum campo não bater com o que está aqui, siga o
Console.

## Criar app

- **Nome do app:** Metrô Ceará Fortaleza horários
- **Idioma padrão:** Português (Brasil) – pt-BR
- **App ou jogo:** App
- **Gratuito ou pago:** Gratuito
- **Declarações:** aceitar as políticas e as leis de exportação dos EUA.

## Página principal da loja (Crescimento > Presença na loja > Página principal)

**Nome (até 30 caracteres):**

```
Metrô Ceará Fortaleza horários
```

Na busca da loja, o título é o que mais pesa: por isso "Fortaleza" e "horários"
("Metrô Ceará (não oficial)" não aparecia ao procurar "metrô fortaleza"). O
"não oficial" saiu só do título, onde alarmava mais do que informava; o aviso
continua no começo da descrição, no destaque e no app.

**Descrição curta (até 80 caracteres):**

```
Horários do metrô e VLT de Fortaleza, Cariri e Sobral e aviso antes da estação.
```

**Descrição completa (até 4.000 caracteres):**

O texto fica num lugar só,
[`fastlane/metadata/android/pt-BR/full_description.txt`](../../fastlane/metadata/android/pt-BR/full_description.txt)
(HTML simples, que o Console aceita). Copie o arquivo inteiro.

Regras que o Google cobra (Política de declarações enganosas, app com
informação de governo):

- **Aviso de não oficial no topo**, fácil de ver: "Não representa o Metrofor,
  o Governo do Ceará nem nenhum órgão público."
- **Fontes oficiais com links que funcionam.** O robô do Google confere o
  certificado HTTPS de forma estrita. Em 17/11/2026 a atualização foi
  rejeitada ("Link da fonte corrompido ou inacessível") porque a descrição
  citava `https://www.ce.gov.br/metrofor/`: o servidor do `ce.gov.br` manda o
  certificado sem o intermediário (Amazon RSA 2048 M01). O navegador do celular
  completa sozinho, o robô não. **Nunca pôr link do `ce.gov.br` em texto da
  loja**; usar o portal de horários `https://info.metrofor.ce.gov.br/`, o GTFS
  `https://info.metrofor.ce.gov.br/gtfs_file` e o Bicicletar
  `https://dados.fortaleza.ce.gov.br/dataset/estacoes-bicicletar`. Dentro do
  app, da lite e do site os links do `ce.gov.br` continuam (lá abrem no
  navegador).
- Antes de enviar, testar cada link sem desligar a verificação:
  `curl -sI <link>` não pode dar erro de certificado.

**Gráficos:**

- Ícone do app: `tools/play/imagens/icone-512.png` (512×512, PNG 32 bits)
- Gráfico de recursos: `tools/play/imagens/destaque-1024x500.png`
- Capturas de tela do telefone: `tools/play/imagens/tela-*.png` (1080×1920, proporção 9:16 exigida)

**Categoria e contato (Página da loja > Configurações da loja):**

- **Categoria:** Mapas e navegação
- **Tags:** transporte público, horários
- **E-mail:** giovanildos@gmail.com
- **Site:** https://giovanildo.github.io/metro-ceara-horarios/
- **Telefone:** deixar em branco (opcional)

## Conteúdo do app (Política > Conteúdo do app)

| Formulário | Resposta |
|---|---|
| **Política de privacidade** | https://giovanildo.github.io/metro-ceara-horarios/privacidade.html |
| **Acesso ao app** | Todas as funcionalidades estão disponíveis sem restrições de acesso (sem login). |
| **Anúncios** | Não, o app não contém anúncios. |
| **Classificação do conteúdo** | Categoria: "Referência, notícias ou educação" (ou "Utilitário/produtividade"). Responder **não** a violência, sexo, linguagem, drogas, jogos de azar, interação entre usuários, compartilhamento de localização com outros usuários e compras. Resultado esperado: Livre. |
| **Público-alvo e conteúdo** | Faixa etária: **13 a 15, 16 a 17 e 18 ou mais** (não marcar menores de 13: isso ativa as regras de apps para crianças). O app não é atraente principalmente para crianças. |
| **App de notícias** | Não. |
| **Apps governamentais** | **Não** é desenvolvido por ou em nome de órgão de governo. |
| **Recursos financeiros** | O app não oferece recursos financeiros. |
| **Saúde** | Não é app de saúde. |
| **ID de publicidade** | Não usa o ID de publicidade. |

### Segurança dos dados

- **O app coleta ou compartilha algum dos tipos de dados exigidos?** **Não.**
  - A localização é usada só no aparelho e nunca sai dele — pela definição da
    Google, dado processado só no dispositivo não conta como "coletado".
  - O download dos horários não envia nada que identifique a pessoa.
- Como a resposta é "não", o resto do formulário fica dispensado.

### Permissões de serviço em primeiro plano (localização)

Tipo: **Localização** (`FOREGROUND_SERVICE_LOCATION`). Texto da justificativa:

```
O app usa a localização num serviço em primeiro plano em duas funções, sempre iniciadas por um toque da pessoa:
1) Modo viagem: depois que a pessoa escolhe o destino e toca em "Iniciar modo viagem", o app acompanha a viagem de metrô ou VLT pelo GPS e avisa duas estações antes do destino, inclusive com a tela desligada. Uma notificação fixa mostra a estação atual e o botão "Encerrar viagem", e o serviço termina sozinho ao chegar.
2) Botão ↻ do widget da tela inicial: ao tocar, o app pega a localização uma vez (no máximo 20 segundos, com a notificação "Procurando onde você está…") para mostrar no widget os horários da estação mais perto, e encerra em seguida.
A localização é usada só no aparelho e não é enviada para ninguém. O app não pede a localização "o tempo todo".
```

- **Vídeo:** link do YouTube (não listado) com `tools/play/video-modo-viagem.mp4`.
- Impacto se a tarefa for adiada/interrompida: no modo viagem, a pessoa perde o aviso de descida; no widget, ele continua mostrando a estação anterior, de onde a pessoa já saiu.

## Assinatura (Configuração > Integridade do app)

"Usar outra chave" > "Exportar e enviar uma chave do Java keystore" — siga
`tools/play/exportar-chave.sh`. Assim a loja usa a mesma chave do APK do
GitHub, e quem já instalou recebe as atualizações da loja.

## Teste fechado

- Criar a faixa **Teste fechado**, enviar `app/build/outputs/bundle/release/app-release.aab`
  (gerado com `./gradlew bundleRelease`).
- Países: Brasil.
- Testadores: lista de e-mails (contas Google) ou um Grupo Google; pelo menos
  12, recomendável 15–20. Eles precisam aceitar o convite pelo link e manter o
  app instalado por 14 dias seguidos.
- Notas da versão: o changelog da versão, de `fastlane/metadata/android/pt-BR/changelogs/`.

**Mensagem para convidar testadores:**

```
Oi! Fiz um app gratuito com os horários do metrô e do VLT de Fortaleza (não oficial), e preciso de testadores para publicá-lo na Play Store. É só: 1) me passar o e-mail da sua conta Google; 2) tocar no link que eu mandar e em "Tornar-se testador"; 3) instalar pela Play Store e deixar instalado por 14 dias, abrindo de vez em quando. Se puder, me conte o que achou. Valeu!
```
