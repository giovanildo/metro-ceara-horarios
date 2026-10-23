# Play Store — textos e respostas para o Play Console

Tudo o que o Play Console pede, pronto para colar. Os campos estão na ordem do
Console. As regras mudam: se algum campo não bater com o que está aqui, siga o
Console.

## Criar app

- **Nome do app:** Metrô Ceará (não oficial)
- **Idioma padrão:** Português (Brasil) – pt-BR
- **App ou jogo:** App
- **Gratuito ou pago:** Gratuito
- **Declarações:** aceitar as políticas e as leis de exportação dos EUA.

## Página principal da loja (Crescimento > Presença na loja > Página principal)

**Nome (até 30 caracteres):**

```
Metrô Ceará (não oficial)
```

**Descrição curta (até 80 caracteres):**

```
Horários do metrô e VLT de Fortaleza, Cariri e Sobral e aviso antes da estação.
```

**Descrição completa (até 4.000 caracteres):**

```
App NÃO OFICIAL, sem vínculo com o Metrofor nem com o Governo do Ceará. Os horários são os que o próprio Metrofor publica.

Que horas passa o próximo metrô? Abra o app e veja a estação mais perto de você e as próximas partidas em cada sentido — do metrô e dos VLTs de Fortaleza, Sobral e Cariri.

• Próxima partida da estação mais perto, pelo GPS, com as outras linhas que passam a até 3 km.
• Modo viagem: escolha o destino e guarde o celular. O app acompanha a viagem estação por estação, mesmo com a tela desligada, e avisa antes de você descer — com notificação, vibração e, se quiser, voz.
• Lembrete da hora de sair: escolha a estação perto de casa e o tempo até ela; toque num horário e o app cria o lembrete na agenda.
• Integração entre linhas: em Parangaba, Chico da Silva e Expedicionários, o próximo metrô ou VLT da outra linha que ainda dá para pegar, e se a integração é paga ou gratuita.
• Bike no metrô: se dá para embarcar com bicicleta agora, pelas regras do Metrofor, e a estação do Bicicletar mais perto.
• Horários do dia: a grade do próprio dia, inclusive sábado e operação especial. Sem internet, usa a última guardada.

Privacidade: sem anúncios, sem cadastro, sem rastreadores. A localização é usada só no celular; nada é enviado para ninguém.

Gratuito e de código aberto (GPL-3.0): github.com/giovanildo/metro-fortaleza-horarios
```

**Gráficos:**

- Ícone do app: `tools/play/imagens/icone-512.png` (512×512, PNG 32 bits)
- Gráfico de recursos: `tools/play/imagens/destaque-1024x500.png`
- Capturas de tela do telefone: `tools/play/imagens/tela-*.png` (1080×1920, proporção 9:16 exigida)

**Categoria e contato (Página da loja > Configurações da loja):**

- **Categoria:** Mapas e navegação
- **Tags:** transporte público, horários
- **E-mail:** giovanildos@gmail.com
- **Site:** https://giovanildo.github.io/metro-fortaleza-horarios/
- **Telefone:** deixar em branco (opcional)

## Conteúdo do app (Política > Conteúdo do app)

| Formulário | Resposta |
|---|---|
| **Política de privacidade** | https://giovanildo.github.io/metro-fortaleza-horarios/privacidade.html |
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
O modo viagem acompanha a viagem de metrô ou VLT que a pessoa iniciou e avisa duas estações antes do destino escolhido, inclusive com a tela desligada. Para isso usa a localização (GPS) num serviço em primeiro plano, com uma notificação fixa visível o tempo todo e um botão "Encerrar viagem". O serviço só roda depois que a pessoa toca em "Iniciar" e escolhe o destino, e termina sozinho ao chegar. A localização é usada só no aparelho e não é enviada para ninguém.
```

- **Vídeo:** link do YouTube (não listado) com `tools/play/video-modo-viagem.mp4`.
- Impacto se a tarefa for adiada/interrompida: a pessoa perde o aviso de descida.

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
