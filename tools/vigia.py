#!/usr/bin/env python3
"""Vigia dos dados: avisa quando algo parou sem ninguém perceber.

A rodada de dados (.github/workflows/dados-web.yml) foi feita para não quebrar:
se uma fonte falha, fica o dado anterior e o app segue. O preço é que a falha não
aparece. Este script olha o que foi publicado no ramo "dados" e abre uma issue
(o e-mail chega sozinho) só no que precisa de gente:

    grade         a grade de horários não atualiza há mais de 24 h
    fonte:*       uma fonte não é lida com sucesso há dias
    noticias      nenhuma notícia nova há tempo demais (o site pode ter mudado)
    noticia:*     notícia que parece de operação especial e o leitor não entendeu
    estacao:*     estação do feed sem dados no regras.json, ou com outro nome
    linha:*       linha do feed sem cor no regras.json (linha nova?)
    pagina:*      página oficial diferente da versão conferida (tools/paginas.py)
    regra:*       notícia sobre passagem, gratuidade, bike, linha nova ou Nina
    conferir:*    fonte do regras.json com a conferência vencendo

Falha isolada não é problema (o site do Metrofor às vezes não responde e volta
na rodada seguinte): por isso os limites olham a última leitura BOA, gravada pela
rodada em indice.json ("saude"). Quando o problema some, a issue fecha sozinha.

Notícia é um aviso só: a issue fica aberta até alguém fechar e não volta depois.
Para calar um alerta de estação ou linha que não é problema, feche a issue como
"não planejado" (close as not planned). Os outros não se calam assim: fechados,
voltam no dia seguinte se o problema continuar.

    python3 tools/vigia.py               # só mostra o que encontrou
    python3 tools/vigia.py PASTA         # usa os arquivos de uma pasta local
    python3 tools/vigia.py --issues      # abre e fecha as issues (precisa do gh)

Sai com 1 se não conseguir ler os dados publicados (aí nem dá para vigiar).
"""
import argparse
import difflib
import json
import os
import re
import subprocess
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

import datas_especiais as de
import fontes as fontes_regras
import operacoes
import paginas

RAW = "https://raw.githubusercontent.com/giovanildo/metro-ceara-horarios/dados/"
DONO = "giovanildo"
ROTULO = "vigia"
CALAVEIS = ("estacao:", "linha:")  # "não planejado" cala; os outros voltariam
UNICOS = ("noticia:", "regra:")  # um aviso por notícia: só gente fecha

HORAS_GRADE = 24
# Quanto cada fonte pode ficar sem uma leitura boa. Chave = indice.json "saude".
FONTES = {
    "noticias": ("notícias (feed do site)", 72),
    "operacoes": ("operação especial (notícias do site)", 72),
    "datas_bike": ("regras de bike por data (notícias do site)", 72),
    "bicicletar": ("estações do Bicicletar (portal de dados de Fortaleza)", 7 * 24),
}
# Das notícias de 2023 para cá, o maior intervalo entre duas foi de 98 dias, e o
# segundo maior de 42: acima de 60 vale olhar, e o alerta fecha sozinho na próxima.
DIAS_SEM_NOTICIA = 60
# Recente o bastante para ainda dar tempo de corrigir antes do dia da operação.
DIAS_NOTICIA_NAO_ENTENDIDA = 10
# Nas 32 notícias com estes títulos desde 2023, o leitor tirou a data de todas:
# se uma não sair, é sinal de que o site passou a escrever de outro jeito.
PARECE_OPERACAO = re.compile(
    r"opera[çc][ãa]o especial|hor[áa]rios? especia|feriado|funcionamento especial"
    r"|n[ãa]o (haver[áa]|ter[áa]) opera", re.I)
# Os títulos de assuntos que costumam mudar uma regra do app. Desde 2024 são cerca
# de 16 de 127 notícias, e pegam as que embasaram as regras atuais.
ASSUNTO_DE_REGRA = re.compile(
    r"\btarifas?\b|reajust|pre[çc]o d[ae]|valor d[ae] passagem|aumento d[ae] passagem|bilhetagem|catraca"
    r"|meia[- ]passagem|estudant|gratuidade|embarques? gratuit|de gra[çc]a|\bgr[áa]tis\b|cadastro"
    r"|cart[ãa]o m[úu]ltiplo|vaivem|passe livre|bicicleta|\bbikes?\b|nova esta[çc][ãa]o|inaugura|nova linha"
    r"|linha leste|ramal aeroporto|come[çc]a a operar|entra em opera|passa a operar|novos hor[áa]rios"
    r"|integra[çc][ãa]o|importuna|\bnina\b", re.I)
DIAS_PAGINA_SEM_LEITURA = 7
MESES_AVISO_CONFERENCIA = 1
LIMITE_DIFERENCA = 6000

COMO_CORRIGIR = {
    "grade": "Olhe a última rodada de [Dados da versão web]"
             "(https://github.com/giovanildo/metro-ceara-horarios/actions/workflows/dados-web.yml). "
             "Se ela falha baixando o feed, o site do Metrofor pode estar fora do ar ou ter mudado de endereço. "
             "Enquanto isso, o app e a lite seguem com a última grade que têm.",
    "fonte": "Nada quebra de imediato: o app segue com o último dado lido. Veja o log da última rodada de "
             "[Dados da versão web](https://github.com/giovanildo/metro-ceara-horarios/actions/workflows/dados-web.yml).",
    "noticias": "Confira se o [site do Metrofor](https://www.ce.gov.br/metrofor/) publicou algo nesse período. "
                "Se publicou, o endereço do feed mudou. Se não, não precisa fazer nada: esta issue fecha sozinha na próxima notícia.",
    "noticia": "Se a notícia anuncia um dia especial, ponha o dia em `operacoes` no `app/src/main/assets/regras.json` "
               "e ajuste `tools/operacoes.py`. Se não anuncia, feche como \"não planejado\".",
    "estacao": "Enquanto isso, o app e o site mostram a estação sem endereço, acessibilidade e avisos. "
               "Acerte `estacoes` no `app/src/main/assets/regras.json` (a chave é o id do feed). "
               "Se ela não precisa de dados próprios, feche como \"não planejado\".",
    "pagina": "Leia a página e confira com o app. Se a regra mudou, acerte `app/src/main/assets/regras.json`. "
              "Depois rode `python3 tools/paginas.py --aceitar NOME` e faça o commit: a issue fecha sozinha. "
              "Enquanto isso, a data de conferência dessa informação não anda.",
    "regra": "Veja se a notícia muda alguma regra mostrada no app (`app/src/main/assets/regras.json`). "
             "Quando resolver, ou se não mudar nada, feche esta issue.",
    "conferir": "Confira na fonte. Se nada mudou, suba `conferido` em `fontes` no `app/src/main/assets/regras.json` e faça o commit: "
                "a issue fecha sozinha.",
    "linha": "Linha nova no feed: ponha a cor em `cores` no `app/src/main/assets/regras.json` e confira preço e integrações.",
}


def agora_fortaleza():
    return datetime.now(de.FORTALEZA)


def simples(texto):
    """Igual ao site e à lite: sem acento, caixa ou pontuação."""
    s = unicodedata.normalize("NFD", texto)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]", "", s)


def ler(pasta, nome, opcional=False):
    if pasta:
        if opcional and not os.path.exists(os.path.join(pasta, nome)):
            return None
        with open(os.path.join(pasta, nome), encoding="utf-8") as fh:
            return json.load(fh)
    req = urllib.request.Request(RAW + nome, headers={"User-Agent": "metro-ceara-horarios/1.0"})
    for espera in (20, 60, None):
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                return json.load(resp)
        except urllib.error.HTTPError as e:
            if opcional and e.code == 404:
                return None
            if espera is None:
                raise
            time.sleep(espera)
        except OSError:
            if espera is None:
                raise
            time.sleep(espera)


def tempo(horas):
    return f"{horas / 24:.0f} dias" if horas >= 48 else f"{horas:.0f} h"


def problema(titulo, corpo):
    return {"titulo": titulo, "corpo": corpo}


def ver_grade(indice, agora):
    vistas = [datetime.fromisoformat(t["atualizado"]) for t in indice.get("tipos", {}).values() if t.get("atualizado")]
    if not vistas:
        return {"grade": problema("Nenhuma grade de horários publicada", "O `indice.json` do ramo dados não tem nenhuma grade.")}
    horas = (agora - max(vistas)).total_seconds() / 3600
    if horas <= HORAS_GRADE:
        return {}
    return {"grade": problema(f"Grade de horários parada há mais de {HORAS_GRADE} h",
                              f"A última grade válida é de {max(vistas):%d/%m às %H:%M} (há {tempo(horas)}).")}


def ver_fontes(indice, agora):
    saude = indice.get("saude")
    if not saude:  # primeira rodada depois de instalar: ainda não há registro
        return {}
    saida = {}
    for chave, (nome, limite) in FONTES.items():
        # Fonte que nunca deu certo conta desde o começo do registro.
        ultima = datetime.fromisoformat(saude.get(chave) or saude["desde"])
        horas = (agora - ultima).total_seconds() / 3600
        if horas > limite:
            saida[f"fonte:{chave}"] = problema(f"Fonte sem leitura há {tempo(limite)}: {nome}",
                                               f"Última leitura boa: {ultima:%d/%m às %H:%M} (há {tempo(horas)}).")
    return saida


def ver_noticias(noticias, hoje):
    datas = [n["data"] for n in noticias if n.get("data")]
    ultima = max(datas) if datas else None
    dias = (hoje - date.fromisoformat(ultima)).days if ultima else None
    if dias is not None and dias <= DIAS_SEM_NOTICIA:
        return {}
    detalhe = f"A mais recente é de {date.fromisoformat(ultima):%d/%m/%Y} (há {dias} dias)." if ultima else "A lista publicada está vazia."
    return {"noticias": problema(f"Nenhuma notícia do Metrofor há mais de {DIAS_SEM_NOTICIA} dias", detalhe)}


def ver_noticias_nao_entendidas(posts):
    saida = {}
    for p in posts:
        titulo = de._limpo(p["title"]["rendered"])
        if not PARECE_OPERACAO.search(titulo):
            continue
        publicada = date.fromisoformat(p["date"][:10])
        if operacoes.extrair(titulo, p["content"]["rendered"], publicada, p.get("link", "")):
            continue
        link = p.get("link", "")
        chave = "noticia:" + (link.rstrip("/").rsplit("/", 1)[-1] or p["date"][:10])
        saida[chave] = problema(f"Notícia que o leitor não entendeu: {titulo[:80]}",
                                f"Publicada em {publicada:%d/%m/%Y}: {titulo}\n\n{link}\n\n"
                                "O título parece de operação especial, mas não saiu nenhuma data dela.")
    return saida


def diferencas(antes, depois):
    frases = lambda t: re.split(r"(?<=[.;:?!])\s+", t)  # noqa: E731
    linhas = [l for l in difflib.unified_diff(frases(antes), frases(depois), lineterm="", n=0)
              if not l.startswith(("---", "+++", "@@"))]
    return "\n".join(linhas)[:LIMITE_DIFERENCA].replace("```", "'''")


def ver_paginas(estado, conferidas, agora):
    if estado is None:  # primeira rodada depois de instalar
        return {}
    saida = {}
    for chave, pagina in paginas.PAGINAS.items():
        atual, conferida = estado.get(chave), conferidas.get(chave)
        lida = datetime.fromisoformat(atual["lido_em"]) if atual else None
        if not lida or (agora - lida).days >= DIAS_PAGINA_SEM_LEITURA:
            quando = f"Última leitura boa: {lida:%d/%m às %H:%M}." if lida else "Ainda não foi lida nenhuma vez."
            saida[f"fonte:pagina-{chave}"] = problema(
                f"Página oficial sem leitura há {DIAS_PAGINA_SEM_LEITURA} dias: {pagina['titulo']}",
                f"{quando} Ela pode ter mudado de endereço: {pagina['link']}")
        if not atual:
            continue
        if not conferida:
            saida[f"pagina:{chave}"] = problema(
                f"Página oficial ainda não conferida: {pagina['titulo']}",
                f"O vigia acompanha {pagina['link']}, mas ninguém confirmou ainda que o app bate com ela.")
        elif conferida["hash"] != atual["hash"]:
            if "texto" in atual and "texto" in conferida:
                detalhe = f"```diff\n{diferencas(conferida['texto'], atual['texto'])}\n```"
            else:
                detalhe = "O arquivo é outro: abra para ver o que mudou."
            saida[f"pagina:{chave}"] = problema(f"Página oficial mudou: {pagina['titulo']}",
                                                f"{pagina['link']}\n\n{detalhe}")
    return saida


def ver_noticias_de_regras(posts):
    saida = {}
    for p in posts:
        titulo = de._limpo(p["title"]["rendered"])
        if ASSUNTO_DE_REGRA.search(titulo):
            link = p.get("link", "")
            chave = "regra:" + (link.rstrip("/").rsplit("/", 1)[-1] or p["date"][:10])
            saida[chave] = problema(f"Notícia sobre regra: {titulo[:90]}",
                                    f"Publicada em {date.fromisoformat(p['date'][:10]):%d/%m/%Y}: {titulo}\n\n{link}")
    return saida


def ver_conferencias(regras, agora):
    atual = agora.year * 12 + agora.month - 1
    saida = {}
    for chave, f in regras.get("fontes", {}).items():
        if chave.startswith("_") or f.get("diario"):
            continue
        conferido, meses = fontes_regras.mes(f.get("conferido")), f.get("revisar_em_meses")
        if conferido is None or not meses or conferido + meses > atual + MESES_AVISO_CONFERENCIA:
            continue
        ate = fontes_regras.rotulo(conferido + meses)
        saida[f"conferir:{chave}"] = problema(f"Conferir até {ate}: {f.get('titulo')}",
                                              f"Fonte: {f.get('fonte')}" + (f"\n\n{f['link']}" if f.get("link") else ""))
    return saida


def ver_estacoes_e_linhas(grades, regras):
    estacoes, cores = regras.get("estacoes", {}), regras.get("cores", {})
    saida = {}
    for grade in grades:
        for id_, e in grade.get("estacoes", {}).items():
            conhecida = estacoes.get(id_)
            if not conhecida:
                saida[f"estacao:{id_}"] = problema(f"Estação sem dados: {e['nome']}",
                                                   f"O feed traz a estação **{e['nome']}** (id {id_}), que não está no `regras.json`.")
            elif simples(conhecida["nome"]) != simples(e["nome"]):
                saida[f"estacao:{id_}"] = problema(f"Estação mudou de nome: {e['nome']}",
                                                   f"O feed chama o id {id_} de **{e['nome']}**; o `regras.json`, de **{conhecida['nome']}**. "
                                                   "Sem bater, o endereço, a acessibilidade e os avisos dela somem.")
        for linha in grade.get("linhas", []):
            if linha["nome"] not in cores:
                saida[f"linha:{linha['nome']}"] = problema(f"Linha sem cor: {linha['nome']}",
                                                           f"O feed traz a linha **{linha['nome']}**, que não está em `cores` no `regras.json`.")
    return saida


def verificar(pasta):
    agora = agora_fortaleza()
    indice, regras, noticias = ler(pasta, "indice.json"), ler(pasta, "regras.json"), ler(pasta, "noticias.json")
    arquivos = {t["arquivo"] for grupo in ("tipos", "normais") for t in indice.get(grupo, {}).values()}
    grades = [ler(pasta, a) for a in sorted(arquivos)]

    problemas = {
        **ver_grade(indice, agora), **ver_fontes(indice, agora),
        **ver_noticias(noticias, agora.date()), **ver_estacoes_e_linhas(grades, regras),
        **ver_paginas(ler(pasta, "paginas.json", opcional=True), paginas.ler_conferidas(), agora),
        **ver_conferencias(regras, agora),
    }
    try:
        posts = de.posts_recentes(agora.date(), DIAS_NOTICIA_NAO_ENTENDIDA)
    except Exception as e:  # noqa: BLE001 - site fora do ar é assunto do fonte:*, não daqui
        print(f"notícias: não deu para ler hoje ({e}); fica para amanhã")
        return problemas
    problemas.update(ver_noticias_nao_entendidas(posts))
    problemas.update(ver_noticias_de_regras(posts))
    return problemas


def gh(*args):
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout


def rodape(chave):
    return COMO_CORRIGIR[chave.split(":")[0]]


def sincronizar(problemas):
    gh("label", "create", ROTULO, "--color", "B60205", "--description", "Aberta pelo vigia dos dados", "--force")
    lista = json.loads(gh("issue", "list", "--label", ROTULO, "--state", "all", "--limit", "500",
                          "--json", "number,state,stateReason,body"))
    existentes = {}
    for issue in lista:  # vem da mais nova para a mais antiga
        marca = re.match(r"<!-- vigia:(.+?) -->", issue["body"])
        if marca:
            existentes.setdefault(marca.group(1), issue)

    for chave, p in sorted(problemas.items()):
        issue = existentes.get(chave)
        corpo = f"<!-- vigia:{chave} -->\n{p['corpo']}\n\n{rodape(chave)}"
        if issue and issue["state"] == "OPEN":
            if issue["body"].replace("\r\n", "\n") != corpo:
                gh("issue", "edit", str(issue["number"]), "--body", corpo)
            print(f"aberta (#{issue['number']}): {p['titulo']}")
        elif issue and (chave.startswith(UNICOS) or (chave.startswith(CALAVEIS) and issue.get("stateReason") == "NOT_PLANNED")):
            print(f"calada: {p['titulo']}")
        else:
            print(f"nova: {p['titulo']}: {gh('issue', 'create', '--title', p['titulo'], '--body', corpo, '--label', ROTULO, '--assignee', DONO).strip()}")

    for chave, issue in existentes.items():
        if issue["state"] == "OPEN" and chave not in problemas and not chave.startswith(UNICOS):
            gh("issue", "close", str(issue["number"]), "--comment", "O vigia não vê mais este problema.")
            print(f"fechada (#{issue['number']}): {chave}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("pasta", nargs="?", help="arquivos do ramo dados numa pasta local")
    parser.add_argument("--issues", action="store_true", help="abre e fecha as issues")
    args = parser.parse_args()
    try:
        problemas = verificar(args.pasta)
    except (OSError, ValueError, KeyError) as e:
        print(f"ERRO: não deu para ler os dados publicados: {e!r}", file=sys.stderr)
        return 1
    for chave, p in sorted(problemas.items()):
        print(f"[{chave}] {p['titulo']}")
    print(f"{len(problemas)} problema(s).")
    if args.issues:
        sincronizar(problemas)
    return 0


if __name__ == "__main__":
    sys.exit(main())
