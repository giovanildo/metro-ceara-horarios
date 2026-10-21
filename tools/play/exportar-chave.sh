#!/usr/bin/env bash
# Exporta a chave de assinatura do app (a mesma do APK do GitHub) para a Play
# Store, para ela assinar as versões da loja com ela ("Usar a sua própria chave"
# no Play App Signing). Assim, quem instalou pelo GitHub recebe as atualizações
# da loja sem desinstalar.
#
# Antes, no Play Console: Configuração > Integridade do app > Assinatura de apps
# > "Usar outra chave" > "Exportar e enviar uma chave do Java keystore". Baixe
# dali o pepk.jar e a chave pública de criptografia (encryption_public_key.pem)
# e ponha os dois nesta pasta (tools/play/). Eles mudam por conta: não vêm no git.
#
# Uso: tools/play/exportar-chave.sh
# Saída: tools/play/chave-play.zip — envie esse arquivo no mesmo lugar do Console.
# Ele contém a chave privada criptografada só para a Google; não vai para o git.
set -euo pipefail
cd "$(dirname "$0")"
RAIZ=../..

[[ -f pepk.jar ]] || { echo "Falta tools/play/pepk.jar (baixe no Play Console)."; exit 1; }
[[ -f encryption_public_key.pem ]] || { echo "Falta tools/play/encryption_public_key.pem (baixe no Play Console)."; exit 1; }
[[ -f $RAIZ/keystore.properties ]] || { echo "Falta keystore.properties na raiz do projeto."; exit 1; }

prop() { grep "^$1=" "$RAIZ/keystore.properties" | cut -d= -f2-; }
LOJA=$(prop storeFile); ALIAS=$(prop keyAlias)
LOJA=${LOJA/#\~/$HOME}

echo "Chave: $LOJA (alias $ALIAS)"
echo "A PEPK vai pedir duas senhas: use storePassword e depois keyPassword do keystore.properties."
java -jar pepk.jar \
  --keystore="$LOJA" \
  --alias="$ALIAS" \
  --output=chave-play.zip \
  --include-cert \
  --rsa-aes-encryption \
  --encryption-key-path=encryption_public_key.pem
echo "Pronto: tools/play/chave-play.zip. Envie no Play Console e apague o arquivo depois."
