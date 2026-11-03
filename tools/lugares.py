#!/usr/bin/env python3
"""Completa os lugares do regras.json ("lugares" > "lista"): a estação mais
perto a pé, a distância andando pelas ruas e o bairro.

Cada lugar precisa só de "nome", "tipo" e "posicao" ([lat, lon]); "busca" (os
apelidos, sem acento) e "osm" (de onde veio a posição) são opcionais. O script
preenche o que falta:

    "estacao"  o nome da estação (sem o " - NE"/" - AE"), a mais perto a pé
               entre as três mais perto em linha reta
    "a_pe_m"   a caminhada até ela, em metros, pelas ruas (rota a pé do
               OpenStreetMap, routing.openstreetmap.de), entre 1,1 e 1,5 vez a
               linha reta; sem a rota, a linha reta vezes 1,3
    "bairro"   e "cidade", do Nominatim (OpenStreetMap)

Uso:
    python3 tools/lugares.py               # só os lugares sem esses campos
    python3 tools/lugares.py --recalcular  # todos (estação mudou de lugar etc.)

Reescreve só o bloco "lugares" (um lugar por linha); o resto do arquivo fica
como está, byte a byte, e o script confere isso antes de gravar.
"""
import json
import math
import os
import re
import sqlite3
import sys
import time
import urllib.parse
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REGRAS = os.path.join(RAIZ, "app", "src", "main", "assets", "regras.json")
BANCO = os.path.join(RAIZ, "app", "src", "main", "assets", "metrofor.db")
AGENTE = "metro-ceara-horarios/1.0 (giovanildo.github.io/metro-ceara-horarios)"
ROTA = "https://routing.openstreetmap.de/routed-foot/route/v1/foot/{},{};{},{}?overview=false"
REVERSO = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=16&accept-language=pt-BR&lat={}&lon={}"
ORDEM = ["nome", "tipo", "busca", "bairro", "cidade", "posicao", "estacao", "a_pe_m", "osm"]


def nome_simples(nome):
    return re.sub(r"\s+-\s+(AE|NE)$", "", nome, flags=re.I)


def distancia(a, b):
    """Metros em linha reta entre dois [lat, lon]."""
    lat = math.radians((a[0] + b[0]) / 2)
    x = math.radians(b[1] - a[1]) * math.cos(lat)
    y = math.radians(b[0] - a[0])
    return 6371000 * math.hypot(x, y)


def estacoes(regras):
    """[(nome sem sufixo, [lat, lon])], com as posições corrigidas do regras.json."""
    banco = sqlite3.connect(BANCO)
    certas = regras.get("estacoes", {})
    lista = []
    for stop_id, nome, lat, lon in banco.execute(
            "SELECT DISTINCT s.stop_id, s.name, s.lat, s.lon FROM stop s JOIN route_stop rs ON rs.stop_id = s.stop_id"):
        posicao = certas.get(stop_id, {}).get("posicao") or [lat, lon]
        lista.append((nome_simples(nome), posicao))
    return lista


def baixar(url):
    pedido = urllib.request.Request(url, headers={"User-Agent": AGENTE})
    for tentativa in range(4):
        try:
            with urllib.request.urlopen(pedido, timeout=60) as resposta:
                return json.load(resposta)
        except Exception as erro:  # noqa: BLE001 - rede instável: tenta de novo
            print(f"  {erro}; tentando de novo", file=sys.stderr)
            time.sleep(5 * (tentativa + 1))
    return None


def a_pe(origem, destino):
    dados = baixar(ROTA.format(origem[1], origem[0], destino[1], destino[0]))
    time.sleep(0.5)
    if dados and dados.get("code") == "Ok":
        return dados["routes"][0]["distance"]
    return None


def mais_perto(lugar, todas):
    """(estação, metros a pé) entre as três mais perto em linha reta."""
    vistas, candidatas = set(), []
    for d, (nome, posicao) in sorted((distancia(lugar, p), (n, p)) for n, p in todas):
        if nome in vistas:
            continue
        vistas.add(nome)
        candidatas.append((d, nome, posicao))
        if len(candidatas) == 3:
            break
    melhor = None
    for d, nome, posicao in candidatas:
        # A posição da estação fica no meio dos trilhos: a rota a pé às vezes engata
        # numa rua do outro lado e dá a volta no quarteirão inteiro (o Terminal do
        # Papicu, colado na estação, dava 1 km). Fica entre 1,1 e 1,5 vez a linha reta.
        metros = min(max(a_pe(lugar, posicao) or d * 1.3, d * 1.1), d * 1.5)
        if melhor is None or metros < melhor[1]:
            melhor = (nome, metros)
    return melhor[0], int(round(melhor[1] / 10) * 10)


def bairro_e_cidade(posicao):
    dados = baixar(REVERSO.format(*posicao)) or {}
    time.sleep(1.1)  # política do Nominatim: uma consulta por segundo
    endereco = dados.get("address", {})
    bairro = next((endereco[k] for k in ("suburb", "neighbourhood", "city_district", "quarter") if k in endereco), "")
    cidade = next((endereco[k] for k in ("city", "town", "municipality", "village") if k in endereco), "")
    return bairro, cidade


def fim_do_objeto(texto, inicio):
    """O índice logo depois do "}" que fecha o objeto que começa em texto[inicio]."""
    profundidade, em_texto, escape = 0, False, False
    for i in range(inicio, len(texto)):
        c = texto[i]
        if em_texto:
            if escape:
                escape = False
            elif c == "\\":
                escape = True
            elif c == '"':
                em_texto = False
            continue
        if c == '"':
            em_texto = True
        elif c in "{[":
            profundidade += 1
        elif c in "}]":
            profundidade -= 1
            if profundidade == 0:
                return i + 1
    raise ValueError("bloco lugares sem fim")


def linha(lugar):
    ordenado = {k: lugar[k] for k in ORDEM if k in lugar}
    ordenado.update({k: v for k, v in lugar.items() if k not in ordenado})
    return json.dumps(ordenado, ensure_ascii=False)


def main():
    recalcular = "--recalcular" in sys.argv
    with open(REGRAS, encoding="utf-8") as arquivo:
        texto = arquivo.read()
    regras = json.loads(texto)
    bloco = regras["lugares"]
    todas = estacoes(regras)
    for i, lugar in enumerate(bloco["lista"], 1):
        if recalcular or "estacao" not in lugar or "a_pe_m" not in lugar:
            lugar["estacao"], lugar["a_pe_m"] = mais_perto(lugar["posicao"], todas)
        if "bairro" not in lugar or "cidade" not in lugar:
            bairro, cidade = bairro_e_cidade(lugar["posicao"])
            lugar.setdefault("bairro", bairro)
            lugar.setdefault("cidade", cidade)
        print(f"{i:3} {lugar['nome'][:45]:45} {lugar['estacao']:20} {lugar['a_pe_m']:5} m  {lugar['bairro']}")

    inicio = texto.index('"lugares": {') + len('"lugares": ')
    fim = fim_do_objeto(texto, inicio)
    cabeca = {k: v for k, v in bloco.items() if k != "lista"}
    partes = [f"    {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}," for k, v in cabeca.items()]
    corpo = ",\n".join("      " + linha(l) for l in bloco["lista"])
    novo = texto[:inicio] + "{\n" + "\n".join(partes) + '\n    "lista": [\n' + corpo + "\n    ]\n  }" + texto[fim:]
    # Não grava arquivo quebrado, nem um que mude outra seção além de "lugares".
    depois = json.loads(novo)
    assert {k: v for k, v in depois.items() if k != "lugares"} == {k: v for k, v in regras.items() if k != "lugares"}
    # Outra pessoa (ou outro processo) pode ter mexido no arquivo durante os minutos
    # das consultas: nesse caso, não grava por cima.
    with open(REGRAS, encoding="utf-8") as arquivo:
        if arquivo.read() != texto:
            sys.exit("regras.json mudou enquanto o script rodava; rode de novo")
    with open(REGRAS, "w", encoding="utf-8") as arquivo:
        arquivo.write(novo)


if __name__ == "__main__":
    main()
