# Cartazes do mercantil perto da Carlito Benevides

Dois cartazes A4 para colar lado a lado num comércio a ~200 m da estação.

| Arquivo | QR leva a | Marcação |
|---|---|---|
| `cartaz-android.pdf` | Play Store | `utm_source=mercantil-carlito` (aparece no Console em Aquisição → Campanhas UTM) |
| `cartaz-iphone.pdf` | lite (`/app/`), com os 3 passos do Safari | `?utm_source=mercantil-carlito` (sem contador: o site não mede visitas) |

Links completos em `link.txt`.

## Imprimir

- "Tamanho real" / 100%, nunca "Ajustar à página" (o QR encolhe).
- O azul vai até a borda: em casa sai com bordinha branca (os textos ficam a 14 mm
  da borda); na gráfica, pedir "sem margem".
- Plastificar (sol e umidade). Testar os dois QR com o celular antes.

## Refazer

Editar `cartaz-*.html` e gerar o PDF com o Chrome:

```bash
google-chrome --headless=new --allow-file-access-from-files --no-pdf-header-footer \
  --print-to-pdf=cartaz-android.pdf "file://$PWD/cartaz-android.html"
```

Os QR (`qr-*.svg`/`.png`) saem do pacote `qrcode` do Python. Para outra estação, trocar
o nome no HTML e a marcação `mercantil-<estação>` nos dois links.
