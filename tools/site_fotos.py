"""Fotos das estações para o site, do Wikimedia Commons (licenças abertas: CC BY-SA e CC0).

Percorre as categorias do Metrofor no Commons (uma por estação, mais "Subway/VLT at X station")
e a do VLT de Sobral, baixa cada foto em 960 px, grava em WebP sem os metadados do arquivo
(docs/img/estacoes/<estação>/) e escreve tools/site_fotos.json com autor, licença e link de cada
uma — o tools/site.py lê esse arquivo e mostra as fotos com o crédito na página da estação.

Uso: python3 tools/site_fotos.py   (depois: python3 tools/site.py)
Rodar de novo só baixa o que for novo e apaga o que saiu do Commons.
"""
import io
import json
import os
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request

from PIL import Image

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PASTA = os.path.join(RAIZ, "docs", "img", "estacoes")
SAIDA = os.path.join(RAIZ, "tools", "site_fotos.json")
API = "https://commons.wikimedia.org/w/api.php?"
UA = {"User-Agent": "metro-ceara-site/1.0 (https://giovanildo.github.io/metro-ceara-horarios/)"}
LARGURA = 960
QUALIDADE = 72

CATEGORIAS = ["Category:Fortaleza Metro", "Category:VLT de Sobral"]
# Nome da categoria no Commons → página do site, quando não bate pelo nome.
APELIDOS = {
    "central-chico-da-silva": "chico-da-silva",
    "tirol-moura-brasil": "moura-brasil",
    "esperanca": "conjunto-esperanca",
    "rachel-de-queiroz": "raquel-de-queiroz",
    "juscelino-kubitschek": "juscelino-kubitscheck",
}
# Fotos soltas (sem categoria por estação): começo do nome do arquivo → estação.
POR_NOME = {
    "Estação Campo dos Velhos": "campo-dos-velhos",
    "Estação Dom José": "dom-jose",
    "Estação Novo Recanto": "novo-recanto",
}
# Escolhas feitas olhando as fotos: a capa (1ª da faixa e prévia no WhatsApp) e as que vão para o fim.
CAPA = {"pontes-vieira": "File:Estação Pontes Vieira.jpg"}
NO_FIM = {"File:Estação Pontes Vieira 01.jpg"}  # placa de inauguração
NAO_FOTO = re.compile(r"mapa|diagrama|logo|ícone|icone|\.svg$|\.pdf$", re.I)
# Primeiro o que mostra a estação por fora (para reconhecer ao chegar), depois o resto.
POR_FORA = re.compile(r"acesso|entrada|fachada|exterior|externa", re.I)


def slug(s):
    s = unicodedata.normalize("NFD", s).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def api(**p):
    p["format"] = "json"
    saida, cont = [], {}
    while True:
        url = API + urllib.parse.urlencode({**p, **cont})
        r = json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60))
        saida.append(r)
        if "continue" not in r:
            return saida
        cont = r["continue"]


def membros(cat, tipo):
    return [m["title"] for r in api(action="query", list="categorymembers", cmtitle=cat, cmtype=tipo, cmlimit=500)
            for m in r["query"]["categorymembers"]]


def estacao_da_categoria(cat):
    m = re.match(r"Category:(.+) \(Fortaleza Metro\)$", cat) or re.match(r"Category:(?:Subway|VLT) at (.+) station$", cat)
    if not m:
        return None
    s = slug(m.group(1))
    return APELIDOS.get(s, s)


def listar(estacoes):
    """{estação: [títulos File:...]} percorrendo as categorias."""
    por_estacao, vistos, fila = {}, set(), list(CATEGORIAS)
    while fila:
        cat = fila.pop(0)
        if cat in vistos:
            continue
        vistos.add(cat)
        fila += membros(cat, "subcat")
        dona = estacao_da_categoria(cat)
        for f in membros(cat, "file"):
            alvo = dona or next((e for prefixo, e in POR_NOME.items() if f[5:].startswith(prefixo)), None)
            if alvo and not NAO_FOTO.search(f):
                por_estacao.setdefault(alvo, [])
                if f not in por_estacao[alvo]:
                    por_estacao[alvo].append(f)
    fora = sorted(e for e in por_estacao if e not in estacoes)
    if fora:
        print("no Commons, sem página no site (ignoradas):", ", ".join(fora))
    return {e: fs for e, fs in por_estacao.items() if e in estacoes}


def detalhes(titulos):
    info = {}
    for i in range(0, len(titulos), 40):
        for r in api(action="query", prop="imageinfo", iiprop="url|extmetadata|mime",
                     iiurlwidth=LARGURA, titles="|".join(titulos[i:i + 40])):
            for p in r["query"]["pages"].values():
                ii = (p.get("imageinfo") or [{}])[0]
                md = ii.get("extmetadata", {})
                txt = lambda k: re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", md.get(k, {}).get("value", ""))).strip()
                info[p["title"]] = {
                    "miniatura": ii.get("thumburl"), "pagina": ii.get("descriptionurl"), "mime": ii.get("mime", ""),
                    "autor": txt("Artist") or "autor desconhecido", "licenca": txt("LicenseShortName"),
                    "licenca_url": md.get("LicenseUrl", {}).get("value", ""),
                    "data": (txt("DateTimeOriginal") or txt("DateTime"))[:10],
                }
    return info


def baixar(url, destino):
    for tentativa in range(4):
        try:
            dados = urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=90).read()
            break
        except Exception as erro:  # o Commons às vezes devolve 429: espera e tenta de novo
            if tentativa == 3:
                raise
            print("  espera:", erro)
            time.sleep(5 * (tentativa + 1))
    im = Image.open(io.BytesIO(dados)).convert("RGB")  # converter descarta EXIF/GPS
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    im.save(destino, "WEBP", quality=QUALIDADE, method=6)
    return im.size


def main():
    estacoes = {e for e in os.listdir(os.path.join(RAIZ, "docs", "estacoes")) if e != "index.html"}
    por_estacao = listar(estacoes)
    info = detalhes(sorted({f for fs in por_estacao.values() for f in fs}))
    anterior = {}
    if os.path.exists(SAIDA):
        with open(SAIDA, encoding="utf-8") as fh:
            anterior = {f["commons"]: f for fs in json.load(fh).values() for f in fs}

    saida, guardados = {}, set()
    for est in sorted(por_estacao):
        fotos = []
        for titulo in por_estacao[est]:
            i = info.get(titulo, {})
            nome = slug(titulo[5:])
            outra = next((e for e in estacoes if e != est and not e.startswith(est) and f"estacao-{e}" in nome
                          and f"estacao-{est}" not in nome), None)
            if outra:  # categoria trocada no Commons: o nome do arquivo diz que é de outra estação
                print(f"  {titulo}: está em {est}, mas o nome diz {outra}; pulei")
                continue
            if not i.get("miniatura") or not i["mime"].startswith("image/") or i["mime"] == "image/svg+xml":
                continue
            if not re.match(r"(CC BY(-SA)? [\d.]+|CC0|Public domain)", i["licenca"], re.I):
                print("  licença fora do combinado, pulei:", titulo, i["licenca"])
                continue
            arquivo = f"{est}/{slug(os.path.splitext(titulo[5:])[0])}.webp"
            destino = os.path.join(PASTA, arquivo)
            if os.path.exists(destino) and titulo in anterior:
                largura, altura = anterior[titulo]["largura"], anterior[titulo]["altura"]
            else:
                print("baixando", titulo)
                largura, altura = baixar(i["miniatura"], destino)
                time.sleep(0.3)
            guardados.add(arquivo)
            fotos.append({"arquivo": arquivo, "commons": titulo, "pagina": i["pagina"], "autor": i["autor"],
                          "licenca": i["licenca"], "licenca_url": i["licenca_url"], "data": i["data"],
                          "largura": largura, "altura": altura})
        # Por fora primeiro; dentro de cada grupo, as mais novas.
        fotos.sort(key=lambda f: f["data"], reverse=True)
        fotos.sort(key=lambda f: not POR_FORA.search(f["commons"]))
        fotos.sort(key=lambda f: (f["commons"] != CAPA.get(est), f["commons"] in NO_FIM))
        if fotos:
            # Cópia da capa em JPG para a prévia de link (nem todo app mostra WebP na prévia).
            capa = f"{est}/capa.jpg"
            Image.open(os.path.join(PASTA, fotos[0]["arquivo"])).convert("RGB").save(
                os.path.join(PASTA, capa), "JPEG", quality=80, optimize=True, progressive=True)
            guardados.add(capa)
            fotos[0]["capa_jpg"] = capa
            saida[est] = fotos

    for raiz, _, nomes in os.walk(PASTA):  # tira o que saiu do Commons
        for n in nomes:
            rel = os.path.relpath(os.path.join(raiz, n), PASTA)
            if rel not in guardados:
                os.remove(os.path.join(raiz, n))
    with open(SAIDA, "w", encoding="utf-8") as fh:
        json.dump(saida, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    total = sum(len(v) for v in saida.values())
    print(f"{total} fotos de {len(saida)} estações em docs/img/estacoes/ ({os.path.relpath(SAIDA, RAIZ)})")


if __name__ == "__main__":
    sys.exit(main())
