#!/usr/bin/env python3
"""Gera os horarios em JSON para a versao web (PWA), a partir do GTFS oficial.

Uso:
    python3 tools/build_web_data.py SAIDA            # baixa o feed mais recente
    python3 tools/build_web_data.py SAIDA feed.zip   # usa um zip local

Como o app Android, guarda uma grade por tipo de dia: o feed do Metrofor traz
a grade do dia em que e baixado (dia util, sabado ou operacao especial). Cada
execucao atualiza so o arquivo do tipo de hoje e mantem os outros que ja
estiverem em SAIDA. Feed vazio ou quebrado nao substitui a grade boa.

Arquivos em SAIDA:
    indice.json                 data e arquivo de cada tipo de dia
    horarios-<tipo>.json        linhas, sentidos, estacoes e viagens (versao lite)
    gtfs-<tipo>.zip             o GTFS original do Metrofor (o app Android baixa daqui)
    bicicletar.json             estacoes do Bicicletar
    regras.json                 copia de app/src/main/assets/regras.json
    noticias.json               ultimas noticias do site do Metrofor: titulo, data e link
                                (o app e a versao lite mostram ate 2 dos ultimos 7 dias)
"""
import html
import io
import json
import os
import re
import shutil
import ssl
import sys
import urllib.request
import zipfile
from email.utils import parsedate_to_datetime
from collections import defaultdict
from datetime import datetime, timedelta, timezone

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_db as gtfs  # noqa: E402  (mesmo tratamento de nomes, cores e Bicicletar)

FORTALEZA = timezone(timedelta(hours=-3))  # sem horario de verao
TIPOS = {5: "sabado", 6: "domingo"}  # weekday() -> tipo; o resto e dia util


def tipo_do_dia(dia):
    return TIPOS.get(dia.weekday(), "dia-util")


def grade(zf):
    """O feed num formato compacto: por sentido, a ordem das estacoes e cada
    viagem como uma lista de minutos alinhada a essa ordem (null = nao para)."""
    routes = gtfs.read(zf, "routes.txt")
    stops = gtfs.read(zf, "stops.txt")
    trips = gtfs.read(zf, "trips.txt")
    stop_times = gtfs.read(zf, "stop_times.txt")
    feed_info = (gtfs.read(zf, "feed_info.txt") or [{}])[0]

    by_trip = defaultdict(list)
    for row in stop_times:
        by_trip[row["trip_id"]].append(row)
    for rows in by_trip.values():
        rows.sort(key=lambda r: int(r["stop_sequence"]))

    trips_by_dir = defaultdict(list)
    headsign = {}
    for t in trips:
        key = (t["route_id"], int(t["direction_id"]))
        trips_by_dir[key].append(t["trip_id"])
        headsign.setdefault(key, gtfs.pretty(t["trip_headsign"].strip()))

    linhas = []
    for r in sorted(routes, key=lambda r: int(r["route_id"])):
        sentidos = []
        for (route_id, direction_id) in sorted(k for k in trips_by_dir if k[0] == r["route_id"]):
            ids = trips_by_dir[(route_id, direction_id)]
            # A sequencia de estacoes vem da viagem mais longa (como no banco do app).
            ordem = [row["stop_id"] for row in max((by_trip[i] for i in ids), key=len)]
            pos = {stop_id: n for n, stop_id in enumerate(ordem)}
            viagens = []
            for i in ids:
                v = [None] * len(ordem)
                for row in by_trip[i]:
                    if row["stop_id"] in pos:
                        v[pos[row["stop_id"]]] = gtfs.to_minutes(row["departure_time"])
                if any(m is not None for m in v):
                    viagens.append(v)
            viagens.sort(key=lambda v: next(m for m in v if m is not None))
            sentidos.append({
                "id": direction_id,
                "destino": headsign[(route_id, direction_id)],
                "estacoes": ordem,
                "viagens": viagens,
            })
        linhas.append({
            "id": r["route_id"],
            "nome": gtfs.pretty((r["route_short_name"] or r["route_long_name"]).strip()),
            "descricao": gtfs.pretty(r["route_desc"].strip()),
            "cor": r["route_color"].strip() or "777777",
            "cor_texto": r["route_text_color"].strip() or "FFFFFF",
            "sentidos": sentidos,
        })

    estacoes = {
        s["stop_id"]: {"nome": gtfs.pretty(s["stop_name"].strip()),
                       "lat": float(s["stop_lat"]), "lon": float(s["stop_lon"])}
        for s in stops
    }

    by_name = {}
    for s in stops:
        by_name.setdefault(s["stop_name"].strip().lower(), s)
    integracoes = []
    for name_a, name_b, walk, note in gtfs.TRANSFERS:
        a, b = by_name.get(name_a.lower()), by_name.get(name_b.lower())
        if not a or not b or gtfs.haversine(a, b) > gtfs.MAX_TRANSFER_METERS:
            print(f"aviso: integracao ignorada: {name_a} / {name_b}")
            continue
        integracoes.append({"a": a["stop_id"], "b": b["stop_id"], "caminhada_min": walk, "obs": note})

    return {
        "linhas": linhas,
        "estacoes": estacoes,
        "integracoes": integracoes,
        "versao_feed": feed_info.get("feed_version", ""),
    }


def baixar_feed(source):
    """Os bytes do GTFS: de um zip local ou do Metrofor (certificado costuma estar vencido)."""
    if source:
        with open(source, "rb") as fh:
            return fh.read()
    print(f"baixando {gtfs.FEED_URL}")
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(gtfs.FEED_URL, headers={"User-Agent": "metro-ceara-horarios/1.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        return resp.read()


NOTICIAS_URL = "https://www.ce.gov.br/metrofor/feed/"


def avisos_do_metrofor():
    """As noticias mais recentes do feed do site (certificado incompleto: sem verificacao)."""
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(NOTICIAS_URL, headers={"User-Agent": "Mozilla/5.0 (metro-ceara-horarios)"})
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        xml = resp.read().decode("utf-8", "replace")
    avisos = []
    for item in re.findall(r"<item>(.*?)</item>", xml, re.S):
        titulo = re.search(r"<title>(.*?)</title>", item, re.S)
        data = re.search(r"<pubDate>(.*?)</pubDate>", item, re.S)
        link = re.search(r"<link>(.*?)</link>", item, re.S)
        if not titulo or not data:
            continue
        texto = html.unescape(re.sub(r"<!\[CDATA\[|\]\]>", "", titulo.group(1))).strip()
        dia = parsedate_to_datetime(data.group(1).strip()).astimezone(FORTALEZA).date().isoformat()
        aviso = {"data": dia, "titulo": texto}
        if link and link.group(1).strip().startswith("https://"):
            aviso["link"] = link.group(1).strip()
        avisos.append(aviso)
    return avisos[:5]


def escrever(path, data):
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, separators=(",", ":"))
    os.replace(tmp, path)


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    out = sys.argv[1]
    source = sys.argv[2] if len(sys.argv) > 2 else None
    os.makedirs(out, exist_ok=True)

    agora = datetime.now(FORTALEZA)
    hoje, tipo = agora.date().isoformat(), tipo_do_dia(agora)
    indice_path = os.path.join(out, "indice.json")
    indice = {"tipos": {}}
    if os.path.exists(indice_path):
        with open(indice_path, encoding="utf-8") as fh:
            indice = json.load(fh)

    bruto = baixar_feed(source)
    dados = grade(zipfile.ZipFile(io.BytesIO(bruto)))
    total = sum(len(s["viagens"]) for l in dados["linhas"] for s in l["sentidos"])
    if not dados["linhas"] or not dados["estacoes"] or total == 0:
        # Mesma protecao do app: feed vazio ou quebrado nao apaga a grade boa.
        print(f"feed sem viagens ({len(dados['linhas'])} linhas, {total} viagens): grade de {tipo} mantida")
    else:
        arquivo = f"horarios-{tipo}.json"
        escrever(os.path.join(out, arquivo), {"data": hoje, "tipo": tipo, **dados})
        # O zip original vai junto: o app Android baixa daqui, com certificado
        # válido, em vez de ir ao site do Metrofor.
        zip_nome = f"gtfs-{tipo}.zip"
        tmp = os.path.join(out, zip_nome + ".tmp")
        with open(tmp, "wb") as fh:
            fh.write(bruto)
        os.replace(tmp, os.path.join(out, zip_nome))
        indice["tipos"][tipo] = {
            "data": hoje, "arquivo": arquivo, "viagens": total,
            "gtfs": zip_nome, "bytes": len(bruto), "atualizado": agora.isoformat(timespec="seconds"),
        }
        print(f"{arquivo}: {len(dados['linhas'])} linhas, {total} viagens")

    bikes = gtfs.load_bike_stations()
    if bikes:
        escrever(os.path.join(out, "bicicletar.json"), [
            {"numero": n, "nome": nome, "bairro": bairro, "vagas": vagas, "lat": lat, "lon": lon}
            for n, nome, bairro, vagas, lat, lon in bikes
        ])

    shutil.copyfile(gtfs.RULES, os.path.join(out, "regras.json"))
    # Os avisos sao um extra: se o site falhar, fica o arquivo anterior e os
    # horarios seguem normalmente.
    try:
        escrever(os.path.join(out, "noticias.json"), avisos_do_metrofor())
    except Exception as e:  # noqa: BLE001
        print(f"avisos do Metrofor: {e}")
    indice["atualizado"] = agora.isoformat(timespec="seconds")
    escrever(indice_path, indice)


if __name__ == "__main__":
    main()
