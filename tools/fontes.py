#!/usr/bin/env python3
"""Lista as fontes do regras.json e quando cada uma deve ser conferida de novo.

O app mostra, embaixo de cada informação, de onde ela vem e a "próxima
conferência" (conferido + revisar_em_meses, seção "fontes" do regras.json).
Este script diz quais já passaram dessa data, para conferir na fonte: se nada
mudou, basta subir o "conferido" (a próxima conferência anda junto).

Aqui vale a data do repositório. Passagem, guia da passagem e bike podem estar em
dia na tela sem subir esta data: a comparação automática (tools/paginas.py) a
avança só no regras.json publicado. O tools/vigia.py avisa o que está vencendo.

    python3 tools/fontes.py              # todas, com a situação
    python3 tools/fontes.py --vencidas   # só as atrasadas ou do mês; sai com 1 se houver
"""
import argparse
import datetime
import json
import os
import sys

REGRAS = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "regras.json")
MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]


def mes(texto):
    """'2026-11' -> número de meses (ano*12 + mês-1), ou None."""
    try:
        ano, m = texto.split("-")[:2]
        return int(ano) * 12 + int(m) - 1 if 1 <= int(m) <= 12 else None
    except (AttributeError, ValueError):
        return None


def rotulo(n):
    return f"{MESES[n % 12]}/{n // 12}"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--vencidas", action="store_true", help="só as atrasadas ou do mês corrente")
    args = parser.parse_args()

    with open(REGRAS, encoding="utf-8") as fh:
        fontes = json.load(fh).get("fontes", {})
    hoje = datetime.date.today()
    agora = hoje.year * 12 + hoje.month - 1
    vencidas = 0
    for chave, f in fontes.items():
        if chave.startswith("_"):
            continue
        conferido, meses = mes(f.get("conferido")), f.get("revisar_em_meses")
        if f.get("diario"):
            situacao = "acompanhado todo dia"
        elif conferido is None or not meses:
            situacao = "sem conferência (estimativa ou observação do app)"
        else:
            proxima = conferido + meses
            if proxima < agora:
                situacao, vencidas = f"ATRASADA desde {rotulo(proxima)}", vencidas + 1
            elif proxima == agora:
                situacao, vencidas = f"conferir este mês ({rotulo(proxima)})", vencidas + 1
            else:
                situacao = f"em dia; próxima conferência {rotulo(proxima)}"
        if args.vencidas and not situacao.startswith(("ATRASADA", "conferir")):
            continue
        print(f"{chave:15} {situacao}")
        print(f"{'':15} {f.get('titulo')} — {f.get('fonte')}" + (f" — {f['link']}" if f.get("link") else ""))
    if args.vencidas:
        print(f"{vencidas} fonte(s) para conferir." if vencidas else "Nenhuma fonte para conferir.")
    return 1 if args.vencidas and vencidas else 0


if __name__ == "__main__":
    sys.exit(main())
