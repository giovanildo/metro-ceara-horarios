#!/usr/bin/env python3
"""Regras de bike em dias especiais (feriados, ENEM, eleicao), lidas das noticias do Metrofor.

O Metrofor anuncia em noticias as operacoes especiais e, nelas, a regra da bike
do dia: "Nas linhas Sul e Nordeste, o embarque com bicicletas estara liberado
entre 9h e 15h", "Nao havera embarque com bicicletas nos dias 9 e 16". Este
modulo busca as noticias recentes que falam de bicicleta (API do WordPress do
site) e tira de cada uma a data, as linhas e as janelas.

E conservador: sem data clara no texto (ou com regra que ele nao entende), a
noticia fica de fora, e o app segue com a regra normal do dia da semana.

Uso direto (mostra o que tiraria das noticias dos ultimos 45 dias):
    python3 tools/datas_especiais.py
"""
import html
import json
import re
import ssl
import sys
import urllib.parse
import urllib.request
from datetime import date, datetime, timedelta, timezone

FORTALEZA = timezone(timedelta(hours=-3))
API = "https://www.ce.gov.br/metrofor/wp-json/wp/v2/posts"
MESES = {"janeiro": 1, "fevereiro": 2, "março": 3, "marco": 3, "abril": 4, "maio": 5, "junho": 6, "julho": 7,
         "agosto": 8, "setembro": 9, "outubro": 10, "novembro": 11, "dezembro": 12}
SOBRAL = ["VLT Sobral - Linha Norte", "VLT Sobral - Linha Sul"]
# date.weekday(): segunda = 0
SEMANA = {"segunda": 0, "terça": 1, "terca": 1, "quarta": 2, "quinta": 3, "sexta": 4, "sábado": 5, "sabado": 5,
          "domingo": 6}


def _limpo(texto):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", texto))).strip()


def _frases(texto):
    return [f.strip() for f in re.split(r"(?<=[.!?])\s+", texto) if f.strip()]


def _hora(h, m):
    return f"{int(h):02d}:{int(m or 0):02d}"


def janelas(frase):
    """As janelas de bike de uma frase: [] = sem bike; None = a frase nao diz (ou nao entendi)."""
    f = frase.lower()
    if re.search(r"n[ãa]o (haver[áa]|ser[áa] permitido|ser[áa] liberado|est[áa] permitido)[^.]*bicicl", f):
        return []
    if "habituais" in f or "normais" in f:
        return None  # regra de sempre: nada a mudar
    achadas = []
    for m in re.finditer(r"(?:entre|das?)\s+(\d{1,2})h(\d{2})?\s+(?:e|às|as|até)\s+(?:às\s+)?(\d{1,2})h(\d{2})?", f):
        achadas.append([_hora(m.group(1), m.group(2)), _hora(m.group(3), m.group(4))])
    for m in re.finditer(r"a partir das?\s+(\d{1,2})h(\d{2})?", f):
        achadas.append([_hora(m.group(1), m.group(2)), None])
    return sorted(achadas) or None


def linhas(frase):
    """As linhas citadas na frase; [] = todas as que aceitam bike."""
    f = frase.lower()
    achadas = []
    if re.search(r"linhas? sul\b|linhas sul e nordeste|linhas sul, ", f):
        achadas.append("Linha Sul")
    if re.search(r"\bnordeste\b|parangaba-mucuripe", f):
        achadas.append("Linha Nordeste")
    if "sobral" in f:
        achadas += SOBRAL
    return achadas


def _dias(frase):
    """(dia, mes ou None, dia da semana ou None) citados na frase: "quinta-feira (19)",
    "(20/11)", "dias 9 e 16 de novembro", "(1º)"."""
    achados = []
    for m in re.finditer(r"dias?\s+((?:\d{1,2}º?\s*(?:,|e)\s*)*\d{1,2}º?)(?:\s+de\s+([a-zç]+))?", frase.lower()):
        mes = MESES.get(m.group(2) or "")
        achados += [(int(d), mes, None) for d in re.findall(r"\d{1,2}", m.group(1))]
    for m in re.finditer(r"(?:([a-zçá]+)(?:-feira)?\s+)?\((\d{1,2})º?(?:/(\d{1,2}))?\)", frase.lower()):
        achados.append((int(m.group(2)), int(m.group(3)) if m.group(3) else None, SEMANA.get(m.group(1) or "")))
    return achados


def _data(dia, mes, semana, publicada):
    """A data com esse dia (e mes e dia da semana, se dados) mais perto da publicacao:
    primeiro para a frente (ate 60 dias), depois para tras (a noticia pode ter
    sido republicada depois)."""
    for delta in list(range(0, 60)) + [-d for d in range(1, 45)]:
        d = publicada + timedelta(days=delta)
        if d.day == dia and (mes is None or d.month == mes) and (semana is None or d.weekday() == semana):
            return d
    return None


def extrair(titulo, conteudo, publicada, link=""):
    """As regras de bike por data de uma noticia: {"AAAA-MM-DD": {"linhas", "bike", "_fonte"}}."""
    texto = _limpo(conteudo)
    frases = _frases(texto)
    abertura = _dias(" ".join(frases[:2]))
    regras = {}
    for i, frase in enumerate(frases):
        if not re.search(r"bicicl|\bbikes?\b", frase, re.I) or not re.search(r"embarque|transporte", frase, re.I):
            continue
        janela = janelas(frase)
        if janela is None:
            continue
        dias = _dias(frase)
        if not dias and re.match(r"(nesse|nesses|neste|nestes|nessa|nessas)\b", frase, re.I):
            # "Nesses dois dias, ... nao havera embarque com bicicletas.": os dias
            # estao numa das frases anteriores.
            for anterior in reversed(frases[max(0, i - 3):i]):
                dias = _dias(anterior)
                if dias:
                    break
        if not dias and len(abertura) == 1:
            dias = abertura  # "...operacao especial nesta quinta-feira (19), feriado de Sao Jose."
        for dia, mes, semana in dias:
            d = _data(dia, mes, semana, publicada)
            if d:
                regras[d.isoformat()] = {
                    "linhas": linhas(frase), "bike": janela,
                    "_fonte": f"{titulo} ({publicada.strftime('%d/%m/%Y')}) {link}".strip(),
                }
    return regras


def buscar(hoje=None, dias=45):
    """As regras de bike por data das noticias recentes do Metrofor (certificado incompleto: sem verificacao)."""
    hoje = hoje or datetime.now(FORTALEZA).date()
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    depois = (hoje - timedelta(days=dias)).isoformat() + "T00:00:00"
    url = API + "?" + urllib.parse.urlencode({
        "search": "bicicletas", "after": depois, "per_page": 50, "_fields": "date,title,content,link"})
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (metro-ceara-horarios)"})
    with urllib.request.urlopen(req, context=ctx, timeout=60) as resp:
        posts = json.load(resp)
    regras = {}
    for p in sorted(posts, key=lambda p: p["date"]):
        publicada = date.fromisoformat(p["date"][:10])
        regras.update(extrair(_limpo(p["title"]["rendered"]), p["content"]["rendered"], publicada, p.get("link", "")))
    # So o que ainda importa: de ontem em diante.
    return {d: r for d, r in sorted(regras.items()) if d >= (hoje - timedelta(days=1)).isoformat()}


if __name__ == "__main__":
    json.dump(buscar(), sys.stdout, ensure_ascii=False, indent=2)
    print()
