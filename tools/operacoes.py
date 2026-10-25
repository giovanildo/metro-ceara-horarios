#!/usr/bin/env python3
"""Operacao especial do metro e VLT (feriado, domingo, ENEM, eleicao), lida das noticias do Metrofor.

O Metrofor anuncia em noticia todo dia com horario diferente do normal: "Metro e
VLTs terao horarios especiais no feriado de 12 de outubro", com a data no texto
("nesta segunda-feira (12/10)") e as linhas que nao circulam ("Nao havera
operacao no VLT de Sobral e no VLT do Cariri"). Este modulo tira de cada noticia
as datas de que ela fala e, por data, as linhas paradas. O app mostra a
noticia nesse dia (no lugar das noticias sem criterio) e avisa na linha parada.

E conservador: noticia sem titulo de operacao, ou sem data clara, fica de fora.
Uma data citada so como "funcionamento normal" ao lado de outras especiais
(o "na quinta e no sabado tudo funciona normalmente" da Semana Santa) nao vira aviso.

Uso direto (mostra o que tiraria das noticias dos ultimos 45 dias):
    python3 tools/operacoes.py
"""
import html
import json
import re
import sys
from datetime import date, datetime, timedelta

import datas_especiais as de

LINHAS = {
    "Linha Sul": r"\blinhas? sul\b|\bsul,|\bsul e\b|\bmetr[ôo] de fortaleza\b",
    "Linha Oeste": r"\boeste\b",
    "Linha Nordeste": r"\bnordeste\b|parangaba-mucuripe",
    "VLT Aeroporto": r"ramal aeroporto|vlt aeroporto|linha aeroporto",
    "VLT Sobral - Linha Norte": r"sobral",
    "VLT Sobral - Linha Sul": r"sobral",
    "VLT Cariri": r"cariri",
}
GRUPOS = {
    r"fortaleza e regi[ãa]o metropolitana|grande fortaleza": ["Linha Sul", "Linha Oeste", "Linha Nordeste", "VLT Aeroporto"],
    r"sobral e regi[ãa]o do cariri|no interior|interior do estado": ["VLT Sobral - Linha Norte", "VLT Sobral - Linha Sul", "VLT Cariri"],
}
TODAS = list(LINHAS)

# Titulo de noticia de operacao de um dia (e nao de horario novo para sempre, bike, campanha...).
TITULO = re.compile(
    r"hor[áa]rios? (especia|alterad|normais|de funcionamento|do metr)|opera[çc][ãa]o (especial|gratuita|excepcional)"
    r"|funcionamento (especial|normal|excepcional|gratuito|no|do|das|dos|durante|em)|como (ser[áa]|vai) (a opera|o funciona)"
    r"|\b(metr[ôo]s?|vlts?|trens|linhas)\b[^.]{0,40}\b(circula|funciona|opera)|n[ãa]o altera hor|suspens[ãa]o d|veja os hor[áa]rios"
    r"|confira os hor[áa]rios|consulte aqui os hor[áa]rios|circula[çc][ãa]o de metr", re.I)
# Mudanca que vale dali em diante (linha nova, horario novo, volta depois de obra) ou assunto
# que so cita a operacao (bike, campanha, decoracao): nao e aviso de um dia.
NAO_E = re.compile(r"bicicleta|\bbikes?\b|novos hor[áa]rios|amplia|importuna|sanitiza|completa|lan[çc]a|parceria|vagas"
                   r"|come[çc]a(m)? a|volta(m)? a (operar|circular|funcionar)|passa(m)? a|semestre|ilumina|nova esta"
                   r"|entra(m)? em opera|mant[ée]m opera|\ba partir de\b", re.I)

FECHADA = re.compile(
    r"n[ãa]o (haver[áa]|h[áa]|ter[áa]|ter[ãa]o|t[êe]m)\s+(nenhuma\s+)?(opera[çc][ãa]o|funcionamento|circula[çc][ãa]o)(?!\s+(especial|excepcional|gratuita))"
    r"|n[ãa]o (circula|circulam|circular[ãa]o|opera|operam|operar[ãa]o|funciona|funcionam|funcionar[ãa]o|ir[ãa]o? funcionar)\b"
    r"|n[ãa]o\s+t[êe]m\s+funcionamento", re.I)
ESPECIAL = re.compile(r"especia|mais cedo|alterad|excepcion|diferentes? dos? habitua|a partir das? \d|a partir de meio"
                      r"|intervalos? (diferentes|de \d)|encerra|gratuit|catracas", re.I)
NORMAL = re.compile(r"\bnormal\b|normalmente|sem (qualquer )?altera|n[ãa]o altera|habituais|padr[ãa]o di[áa]rio"
                    r"|regulares|mesmos realizados diariamente|sem mudan", re.I)
# "Não haverá operação especial de São João", "não será realizado o esquema especial": nada muda.
SEM_ESPECIAL = re.compile(r"n[ãa]o (haver[áa]|ter[áa]|ser[áa] realizad\w*)[^.]{0,30}(especial|esquema)", re.I)
GERAL = re.compile(r"aos domingos|\btrecho\b|bicicl|\bbikes?\b|n[ãa]o ser[áa] necess", re.I)

DATA_BARRA = re.compile(r"\b(\d{1,2})[º°]?\s*/\s*/?\s*(\d{1,2})(?:\s*/\s*(\d{2,4}))?\b")
DATA_EXTENSO = re.compile(r"\b(\d{1,2})[º°]?\s+de\s+(" + "|".join(de.MESES) + r")(?:\s+de\s+(\d{4}))?", re.I)
SEMANA_RE = r"\b(segunda|ter[çc]a|quarta|quinta|sexta|s[áa]bado|domingo)(?:-feira)?"
# "sexta-feira (7)", "Quarta-feira de Cinzas (18)", "terça-feira de Carnaval (17)", "domingo, dia 5"
DATA_SEMANA = re.compile(SEMANA_RE + r"(?:\s+(?:de\s+\w+|santa|da paix[ãa]o))?,?\s*(?:\(|dia\s+)(\d{1,2})[º°]?\)?", re.I)
DIAS_E = re.compile(r"\bdias\s+(\d{1,2})[º°]?\s+e\s+(\d{1,2})[º°]?(?:\s+de\s+(" + "|".join(de.MESES) + r"))?", re.I)


def _perto(publicada, dia, mes, ano=None):
    """A data com esse dia e mes mais perto da publicacao (o ano pode vir no texto)."""
    if ano:
        ano = int(ano) + (2000 if int(ano) < 100 else 0)
        try:
            return date(ano, mes, dia)
        except ValueError:
            return None
    for a in (publicada.year, publicada.year + 1, publicada.year - 1):
        try:
            d = date(a, mes, dia)
        except ValueError:
            continue
        if -10 <= (d - publicada).days <= 60:
            return d
    return None


def _semana(nome):
    nome = nome.lower().replace("ç", "c").replace("á", "a")
    return {"segunda": 0, "terca": 1, "quarta": 2, "quinta": 3, "sexta": 4, "sabado": 5, "domingo": 6}[nome]


def datas(texto, publicada):
    """As datas citadas no texto, perto da publicacao: de 3 dias antes a 60 depois
    (com o dia da semana conferido, ate 30 dias antes: noticia republicada)."""
    achadas, conferidas = [], []
    t = texto.lower()
    for m in DATA_BARRA.finditer(t):
        if re.match(r"\s*(h|min|km)", t[m.end():]) or not 1 <= int(m.group(2)) <= 12:
            continue
        achadas.append(_perto(publicada, int(m.group(1)), int(m.group(2)), m.group(3)))
    for m in DATA_EXTENSO.finditer(t):
        achadas.append(_perto(publicada, int(m.group(1)), de.MESES[m.group(2).lower()], m.group(3)))
    for m in DIAS_E.finditer(t):
        mes = de.MESES.get((m.group(3) or "").lower())
        for dia in (m.group(1), m.group(2)):
            achadas.append(_perto(publicada, int(dia), mes) if mes else de._data(int(dia), None, None, publicada))
    for m in DATA_SEMANA.finditer(t):
        conferidas.append(de._data(int(m.group(2)), None, _semana(m.group(1)), publicada))
    for m in re.finditer(r"\bamanh[ãa]\s*\((\d{1,2})\)", t):  # "não circulam amanhã (30)"
        if (publicada + timedelta(days=1)).day == int(m.group(1)):
            conferidas.append(publicada + timedelta(days=1))
    return sorted({d for d in achadas if d and -3 <= (d - publicada).days <= 60}
                  | {d for d in conferidas if d and -30 <= (d - publicada).days <= 60})


def linhas(texto):
    t = texto.lower()
    achadas = [nome for nome, padrao in LINHAS.items() if re.search(padrao, t)]
    for padrao, grupo in GRUPOS.items():
        if re.search(padrao, t):
            achadas += grupo
    return [n for n in TODAS if n in achadas]


def _pedacos(conteudo):
    """O texto em linhas (paragrafos, itens, celulas) e cada linha em frases."""
    c = re.sub(r"</(p|li|h\d|tr|div)>|<br\s*/?>", "\n", conteudo)
    c = re.sub(r"</t[dh]>", " ", c)
    texto = html.unescape(re.sub(r"<[^>]+>", " ", c))
    for linha in texto.split("\n"):
        linha = re.sub(r"[ \t\xa0]+", " ", linha).strip()
        if linha:
            # "...Cariri e Sobral.Ja na sexta-feira (9)": ponto sem espaco tambem separa.
            yield [f for f in re.split(r"(?<=[.!?;])\s+|(?<=[a-zà-ú]\.)(?=[A-ZÀ-Ú])", linha) if f.strip()]


def _cabecalho(linha):
    """Titulo de tabela ou secao, sem horario: "SOBRAL E REGIÃO DO CARIRI", "VLT do Cariri"."""
    if re.search(r"\d", linha) or re.search(r"[.!?;,]", linha.rstrip(":")):
        return False
    letras = [c for c in linha if c.isalpha()]
    return bool(letras) and (len(linha) < 40 or (len(linha) < 60 and sum(c.isupper() for c in letras) > 0.6 * len(letras)))


def extrair(titulo, conteudo, publicada, link=""):
    """{"AAAA-MM-DD": {"titulo", "link", "publicada", "normal", "paradas"}} de uma noticia de operacao."""
    if not TITULO.search(titulo) or NAO_E.search(titulo):
        return {}
    especial, normal, paradas, citadas = set(), set(), {}, set()
    contexto, linhas_contexto, geral = [], [], False

    def nota(frase, explicitas):
        """Anota o que a frase diz das suas datas (ou das do contexto); devolve as datas dela."""
        nonlocal geral
        if not explicitas:
            # "Na sexta-feira, ..." depois de "quinta (28) e sexta (29)", ou o cabecalho
            # "OPERAÇÃO ESPECIAL NA SEXTA-FEIRA DA PAIXÃO": so a sexta ja citada.
            dias = {_semana(m.group(1)) for m in re.finditer(SEMANA_RE, frase.lower())}
            so = sorted(d for d in citadas | set(contexto) if d.weekday() in dias)
            if so and so != sorted(contexto):
                explicitas = so
        alvo = explicitas or contexto
        citadas.update(explicitas)
        fechada = FECHADA.search(frase) and not GERAL.search(frase)
        if SEM_ESPECIAL.search(frase):
            return explicitas
        if fechada:
            # "Já os VLTs de Cariri e Sobral não terão operação neste feriado", num dia de
            # funcionamento normal em Fortaleza: a linha parada vale, o dia segue "normal".
            ls = linhas(frase) or linhas_contexto or TODAS
            for d in alvo:
                paradas.setdefault(d, set()).update(ls)
                if explicitas or d not in normal:
                    especial.add(d)
        elif ESPECIAL.search(frase):
            geral = geral or not alvo
            for d in alvo:
                if explicitas or d not in normal:
                    especial.add(d)
        elif NORMAL.search(frase) and explicitas:
            normal.update(explicitas)
        return explicitas

    pedacos = list(_pedacos(conteudo))
    # A data de que a noticia trata (titulo ou abertura); as outras sao de passagem
    # ("a vespera do Enem fecha mais cedo") e perdem para a noticia principal do dia.
    principais = set(datas(titulo, publicada)) or set(datas(" ".join(pedacos[0][:2]) if pedacos else "", publicada))
    for frases in [[titulo]] + pedacos:
        linha = " ".join(frases)
        # Cabecalho de tabela ("SOBRAL E REGIÃO DO CARIRI"): as linhas seguintes falam dessas
        # linhas, ate o proximo cabecalho ou a proxima data em titulo ("Dias 25/12 e 1º/1:").
        if _cabecalho(linha) and linhas(linha):
            linhas_contexto = linhas(linha)
        elif linha.endswith(":") and datas(linha, publicada):
            linhas_contexto = []
        for frase in frases:
            explicitas = nota(frase, datas(frase, publicada))
            if explicitas:
                contexto = explicitas
    # Data citada sem dizer como sera ("Dias 24/12 e 31/12:" e a lista de horarios), numa
    # noticia que fala de horario especial: e dia especial.
    if geral:
        especial |= citadas - normal
    if not especial | normal:
        return {}
    # So "funcionamento normal" ao lado de dias especiais nao vira aviso; se a noticia
    # inteira e de funcionamento normal (feriado sem mudanca), o aviso diz isso.
    dias = sorted(especial) or sorted(normal)
    fonte = {"titulo": titulo, "link": link, "publicada": publicada.isoformat()}
    out = {}
    for d in dias:
        e = dict(fonte, normal=d not in especial, principal=d in principais)
        if d in paradas:
            e["paradas"] = [n for n in TODAS if n in paradas[d]]
        out[d.isoformat()] = e
    return out


def buscar(hoje=None, dias=45):
    """As operacoes especiais anunciadas nas noticias recentes, de ontem em diante."""
    hoje = hoje or datetime.now(de.FORTALEZA).date()
    posts = de.posts_recentes(hoje, dias)
    ops = juntar(extrair(de._limpo(p["title"]["rendered"]), p["content"]["rendered"],
                         date.fromisoformat(p["date"][:10]), p.get("link", ""))
                 for p in sorted(posts, key=lambda p: p["date"]))
    return {d: {k: v for k, v in o.items() if k != "principal"}
            for d, o in sorted(ops.items()) if d >= (hoje - timedelta(days=1)).isoformat()}


def juntar(por_noticia):
    """Varias noticias sobre o mesmo dia: fica a que trata dele (a mais nova, se empatar),
    e as linhas paradas de todas."""
    ops = {}
    for r in por_noticia:
        for d, e in r.items():
            antes = ops.get(d)
            paradas = sorted(set(e.get("paradas", [])) | set((antes or {}).get("paradas", [])), key=TODAS.index)
            if not antes or e["principal"] or not antes["principal"]:
                ops[d] = dict(e)
            if paradas:
                ops[d]["paradas"] = paradas
    return ops


if __name__ == "__main__":
    json.dump(buscar(), sys.stdout, ensure_ascii=False, indent=2)
    print()
