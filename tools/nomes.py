#!/usr/bin/env python3
"""Leva "Por que esse nome?" de tools/site_nomes.json para o regras.json (bloco "nomes").

O site (tools/site.py) já mostra esses textos nas páginas das estações; o app e a
lite leem o regras.json, então o bloco vai para lá, com a chave igual ao slug da
página da estação (o mesmo de tools/site.py: o nome sem acento, em minúsculas,
com hífens). Só entra o que tem fonte, como no site.

    python3 tools/nomes.py

Reescreve só o bloco "nomes" (uma estação por linha); o resto do arquivo fica
como está. Sem o bloco, ele entra antes de "fontes". Muda sem versão nova do app:
o espelho publica o regras.json no próximo dados-web.
"""
import json
import os
import re

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REGRAS = os.path.join(RAIZ, "app", "src", "main", "assets", "regras.json")
NOMES = os.path.join(RAIZ, "tools", "site_nomes.json")
SOBRE = ("Por que cada estação tem esse nome (tools/nomes.py copia de tools/site_nomes.json, a mesma "
         "pesquisa das páginas do site). A chave é o slug da página da estação no site "
         "(estacoes/<slug>/). Só entra o que tem fonte.")


def bloco(estacoes):
    linhas = [f'    "_sobre": {json.dumps(SOBRE, ensure_ascii=False)}']
    for chave, n in estacoes.items():
        f = n["fontes"][0]
        item = {"texto": n["texto"], "fonte": f["texto"], "link": f["link"]}
        linhas.append(f"    {json.dumps(chave)}: {json.dumps(item, ensure_ascii=False)}")
    return '  "nomes": {\n' + ",\n".join(linhas) + "\n  }"


def main():
    estacoes = json.load(open(NOMES, encoding="utf-8"))["estacoes"]
    texto = open(REGRAS, encoding="utf-8").read()
    novo = bloco(estacoes)
    m = re.search(r'^  "nomes": \{\n.*?^  \}', texto, flags=re.S | re.M)
    if m:
        texto = texto[: m.start()] + novo + texto[m.end():]
    else:
        i = texto.index('\n  "fontes": {')
        texto = texto[:i] + "\n" + novo + "," + texto[i:]
    json.loads(texto)  # confere antes de gravar
    open(REGRAS, "w", encoding="utf-8").write(texto)
    print(f"{len(estacoes)} estações no bloco nomes")


if __name__ == "__main__":
    main()
