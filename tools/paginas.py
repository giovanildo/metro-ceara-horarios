#!/usr/bin/env python3
"""As páginas oficiais em que as regras do app se apoiam, comparadas com a versão conferida.

A rodada de dados lê cada página uma vez por dia e publica o resultado em
paginas.json (ramo dados). Com a página igual à conferida, a data da conferência
anda sozinha no regras.json publicado, e o texto diz que foi uma comparação
automática. Com a página diferente, a data fica parada e tools/vigia.py abre uma
issue com o que mudou.

A versão conferida fica em tools/paginas_conferidas.json e só muda à mão: é o que
alguém leu e viu que bate com o regras.json.

    python3 tools/paginas.py                     # compara o site com o conferido
    python3 tools/paginas.py --aceitar tarifas   # esta página foi lida e o app está certo
    python3 tools/paginas.py --aceitar todas

O site do governo não envia o certificado intermediário: ele vai em
tools/certificados/, e o certificado do site é verificado normalmente.
"""
import argparse
import hashlib
import html
import json
import os
import re
import ssl
import sys
import urllib.request

RAIZ = os.path.dirname(os.path.abspath(__file__))
CERTIFICADO = os.path.join(RAIZ, "certificados", "amazon-rsa-2048-m01.pem")
CONFERIDAS = os.path.join(RAIZ, "paginas_conferidas.json")
SITE = "https://www.ce.gov.br/metrofor/"

PAGINAS = {
    "tarifas": {"titulo": "Tarifas", "link": SITE + "tarifas/"},
    "gratuidades": {"titulo": "Gratuidades", "link": SITE + "gratuidades/"},
    "meia": {"titulo": "Meia passagem estudantil", "link": SITE + "meia-passagem-estudantil/"},
    "bike": {"titulo": "Embarque com bicicletas", "link": SITE + "embarque-com-bicicletas/"},
    "bike_pdf": {"titulo": "Regulamento de bikes (PDF)",
                 "link": SITE + "wp-content/uploads/sites/75/2023/03/Regula_Bikes_2023.pdf", "arquivo": True},
    "cariri": {"titulo": "Bilhetagem eletrônica no Cariri", "link": SITE + "bilhetagem-eletronica-no-cariri/"},
}
# Só as fontes cuja regra está inteira nessas páginas têm a conferência adiada.
FONTES = {
    "passagem": ["tarifas", "cariri"],
    "guia_passagem": ["tarifas", "meia", "gratuidades"],
    "bike": ["bike", "bike_pdf"],
}
MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]


def baixar(url):
    ctx = ssl.create_default_context()
    ctx.load_verify_locations(CERTIFICADO)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (metro-ceara-horarios)"})
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        return resp.read()


def texto_da_pagina(bruto):
    """Só o conteúdo da página, sem menu e rodapé, que mudam sem mudar a regra."""
    regiao = re.search(r"<main\b.*?</main>", bruto, re.S | re.I)
    if not regiao:
        raise ValueError("página sem <main>: o site mudou de formato")
    s = re.sub(r"<(script|style|svg)\b.*?</\1>", " ", regiao.group(0), flags=re.S | re.I)
    s = re.sub(r"<[^>]+>", " ", s)
    s = re.sub(r"\s+", " ", html.unescape(s)).strip()
    if len(s) < 20:
        raise ValueError("página sem texto: o site mudou de formato")
    return s


def ler(chave):
    pagina = PAGINAS[chave]
    bruto = baixar(pagina["link"])
    if pagina.get("arquivo"):
        if not bruto.startswith(b"%PDF"):
            raise ValueError("o endereço não devolveu um PDF")
        return {"hash": hashlib.sha256(bruto).hexdigest()}
    texto = texto_da_pagina(bruto.decode("utf-8", "replace"))
    return {"hash": hashlib.sha256(texto.encode()).hexdigest(), "texto": texto}


def ler_conferidas():
    with open(CONFERIDAS, encoding="utf-8") as fh:
        return json.load(fh)


def atualizar(estado, agora):
    """Lê uma vez por dia o que ainda não foi lido hoje. Página que falhou fica como estava."""
    hoje = agora.date().isoformat()
    novo = dict(estado)
    for chave in PAGINAS:
        if novo.get(chave, {}).get("lido_em", "")[:10] == hoje:
            continue
        try:
            novo[chave] = {**ler(chave), "lido_em": agora.isoformat(timespec="seconds")}
        except Exception as e:  # noqa: BLE001 - o vigia avisa se a leitura não voltar
            print(f"página {chave}: {e}")
    return novo


def rotulo(ano_mes):
    ano, mes = ano_mes.split("-")
    return f"{MESES[int(mes) - 1]}/{ano}"


def adiar_conferencias(regras, estado, conferidas):
    """No regras.json que vai ser publicado, avança a conferência das fontes sem mudança."""
    fontes = regras.get("fontes", {})
    for fonte, chaves in FONTES.items():
        f = fontes.get(fonte)
        vistas = [estado.get(c) for c in chaves]
        if not f or not all(vistas):
            continue
        if any(v["hash"] != conferidas.get(c, {}).get("hash") for c, v in zip(chaves, vistas)):
            continue
        mes = min(v["lido_em"] for v in vistas)[:7]
        if mes <= f.get("conferido", ""):
            continue
        nomes = ", ".join(PAGINAS[c]["titulo"] for c in chaves)
        f["conferido"] = mes
        f["detalhe"] = (f.get("detalhe", "") + f" Comparação automática em {rotulo(mes)}: "
                        f"as páginas oficiais ({nomes}) continuam iguais às da última conferência.").strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--aceitar", nargs="+", metavar="PAGINA", help="grava a página atual como conferida (ou 'todas')")
    args = parser.parse_args()
    try:
        conferidas = ler_conferidas()
    except FileNotFoundError:
        conferidas = {}
    if args.aceitar:
        chaves = list(PAGINAS) if args.aceitar == ["todas"] else args.aceitar
        for chave in chaves:
            if chave not in PAGINAS:
                sys.exit(f"página desconhecida: {chave} (existem: {', '.join(PAGINAS)})")
            conferidas[chave] = ler(chave)
            print(f"{chave}: conferida")
        with open(CONFERIDAS, "w", encoding="utf-8") as fh:
            json.dump(conferidas, fh, ensure_ascii=False, indent=1, sort_keys=True)
            fh.write("\n")
        return 0
    for chave, pagina in PAGINAS.items():
        atual = ler(chave)
        conferida = conferidas.get(chave)
        situacao = "ainda não conferida" if not conferida else "igual" if conferida["hash"] == atual["hash"] else "MUDOU"
        print(f"{chave:12} {situacao:20} {pagina['link']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
