#!/usr/bin/env python3
"""Gera o site (o guia das estações e linhas) em docs/, a partir dos mesmos dados
do app e da lite: app/src/main/assets/regras.json e a grade do Metrofor publicada
no ramo "dados" (tools/build_web_data.py).

Uso:
    python3 tools/site.py               # baixa a grade do ramo dados (raw.githubusercontent)
    python3 tools/site.py PASTA         # usa indice.json e horarios-*.json de uma pasta local

Gera (e só mexe nestes caminhos; a lite em docs/app/ fica intocada):
    docs/estacoes/index.html            todas as estações, por cidade e linha
    docs/estacoes/<estacao>/index.html  uma página por estação
    docs/linhas/index.html              todas as linhas
    docs/linhas/<linha>/index.html      uma página por linha
    docs/perguntas-frequentes/index.html  as dúvidas comuns, respondidas com os mesmos dados
    docs/sitemap.xml                    reescrito com as páginas fixas e as geradas
    docs/index.html                     só os trechos <!--gerado:...--> (mapas, busca, números);
                                        o resto da página principal é feito à mão

O que é fixo (endereço, acessibilidade, lugares perto, primeiro e último horário,
intervalo) vai no HTML, para o Google ler. O "agora" (próximas partidas de hoje)
a página busca sozinha no navegador, como a lite (docs/site/agora.js). Por isso
basta gerar de novo quando mudar regras.json ou a grade comum de dia útil/sábado,
não todo dia. A tarefa .github/workflows/site.yml faz isso sozinha.
"""
import html
import json
import os
import re
import statistics
import sys
import unicodedata
import urllib.request
from collections import defaultdict
from datetime import datetime, timedelta, timezone

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(RAIZ, "docs")
REGRAS = os.path.join(RAIZ, "app", "src", "main", "assets", "regras.json")
FOTOS = os.path.join(RAIZ, "tools", "site_fotos.json")
HISTORIA = os.path.join(RAIZ, "tools", "site_historia.json")
FOTOS_NO_TOPO = 4  # as outras fotos da estação ficam no fim da página e só carregam ao rolar
RAW = "https://raw.githubusercontent.com/giovanildo/metro-ceara-horarios/dados/"
SITE = "https://giovanildo.github.io/metro-ceara-horarios/"
FORTALEZA = timezone(timedelta(hours=-3))

# Páginas fixas que continuam no sitemap (feitas à mão).
FIXAS = ["", "app/", "privacidade.html"]

# Nomes como o GTFS escreve -> como a placa da estação escreve.
NOMES = {
    "Expedicionarios": "Expedicionários",
    "Alto da Brasilia": "Alto da Brasília",
    "Antonio Vieira": "Antônio Vieira",
}

# Linha (route_id do GTFS) -> cidade/sistema. A Linha Sul e a Oeste passam por
# Maracanaú, Pacatuba e Caucaia, mas o sistema é o de Fortaleza.
SISTEMA = {"1": "Fortaleza", "2": "Fortaleza", "3": "Fortaleza", "7": "Fortaleza",
           "4": "Sobral", "5": "Sobral", "6": "Cariri"}
ORDEM_SISTEMAS = ["Fortaleza", "Cariri", "Sobral"]

LUGAR_TIPO = {
    "onibus": ("🚌", "Terminais e rodoviárias"), "aviao": ("✈️", "Aeroporto"),
    "hospital": ("🏥", "Saúde"), "universidade": ("🎓", "Universidades e faculdades"),
    "escola": ("🏫", "Escolas"), "shopping": ("🛍️", "Shoppings"), "mercado": ("🧺", "Mercados e feiras"),
    "teatro": ("🎭", "Teatros e cinemas"), "museu": ("🏛️", "Museus"), "cultura": ("🎨", "Cultura"),
    "igreja": ("⛪", "Igrejas"), "praca": ("🌳", "Praças"), "parque": ("🌳", "Parques e lagoas"),
    "praia": ("🏖️", "Praias"), "estadio": ("🏟️", "Estádios e esportes"),
    "governo": ("🏢", "Órgãos públicos"), "justica": ("⚖️", "Justiça"),
}
PERTO_M = 1000           # como a lite: em "Perto desta estação", só caminhada curta
METROS_POR_MINUTO = 75   # como a lite e o app
WHATSAPP = "https://wa.me/5585997619295?text="


# ---------- utilidades

def esc(s):
    return html.escape(str(s), quote=True)


def slug(texto):
    s = unicodedata.normalize("NFD", texto)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def simples(s):
    return re.sub(r"[^a-z0-9]", "", slug(s))


def hora(m):
    m %= 24 * 60
    return f"{m // 60:02d}:{m % 60:02d}"


def nome_estacao(nome_gtfs):
    nome = re.sub(r"\s+-\s+(AE|NE)$", "", nome_gtfs, flags=re.I)
    return NOMES.get(nome, nome)


def nome_linha(nome):
    return nome.replace(" - ", " · ")


def da(linha):
    return "do" if linha["nome"].startswith("VLT") else "da"


def para_a(linha):
    return "o" if linha["nome"].startswith("VLT") else "a"


def veiculo(linha_nome):
    return "metrô" if linha_nome.strip().lower() == "linha sul" else "VLT"


def carregar_json(nome, pasta):
    if pasta:
        with open(os.path.join(pasta, nome), encoding="utf-8") as fh:
            return json.load(fh)
    req = urllib.request.Request(RAW + nome, headers={"User-Agent": "metro-ceara-horarios/site"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read().decode("utf-8"))


GERADO = re.compile(r"Página gerada em \d{2}/\d{2}/\d{4}\.")


def escrever(caminho, texto):
    """Página em que só a data de geração mudaria fica como está: o site só muda com conteúdo novo."""
    if os.path.exists(caminho):
        with open(caminho, encoding="utf-8") as fh:
            if GERADO.sub("", fh.read()) == GERADO.sub("", texto):
                return
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    with open(caminho, "w", encoding="utf-8") as fh:
        fh.write(texto)


# ---------- os dados

class Dados:
    def __init__(self, regras, util, sabado):
        self.regras = regras
        self.util = util
        self.sabado = sabado
        self.cores = {k: v for k, v in regras.get("cores", {}).items() if not k.startswith("_")}
        self.linhas = util["linhas"]
        self._estacoes()
        self.fotos = {}  # tools/site_fotos.json, do tools/site_fotos.py (Wikimedia Commons)
        if os.path.exists(FOTOS):
            with open(FOTOS, encoding="utf-8") as fh:
                self.fotos = json.load(fh)

    def cor(self, linha, escuro=False):
        c = self.cores.get(linha["nome"])
        return "#" + (c["escuro" if escuro else "claro"] if c else linha["cor"])

    def info(self, stop_id, nome_gtfs):
        """regras.json "estacoes": o nome tem de bater (igual à lite)."""
        e = self.regras.get("estacoes", {}).get(stop_id)
        return e if e and simples(e["nome"]) == simples(nome_gtfs) else {}

    def _estacoes(self):
        g = self.util
        # As integrações na mesma estação (Parangaba, Expedicionários) viram uma página só.
        junto = {}
        for i in g.get("integracoes", []):
            if i["obs"].lower().startswith(("mesma", "passarela")):
                junto[i["b"]] = i["a"]
        grupos = defaultdict(lambda: {"stops": [], "linhas": []})
        for linha in self.linhas:
            maior = max(linha["sentidos"], key=lambda s: len(s["estacoes"]))
            for stop in maior["estacoes"]:
                chave = junto.get(stop, stop)
                grupo = grupos[chave]
                if stop not in grupo["stops"]:
                    grupo["stops"].append(stop)
                grupo["linhas"].append((linha, stop))
        # Slug: o nome; se dois sistemas têm o mesmo (Padre Cícero), o de fora de Fortaleza leva a cidade.
        por_nome = defaultdict(list)
        for chave, grupo in grupos.items():
            nome = nome_estacao(g["estacoes"][chave]["nome"])
            sistema = SISTEMA.get(grupo["linhas"][0][0]["id"], "Fortaleza")
            grupo.update(chave=chave, nome=nome, sistema=sistema)
            por_nome[slug(nome)].append(grupo)
        for s, lista in por_nome.items():
            for grupo in lista:
                grupo["slug"] = s if len(lista) == 1 or grupo["sistema"] == "Fortaleza" else f"{s}-{slug(grupo['sistema'])}"
        for grupo in grupos.values():
            info = {}
            for stop in grupo["stops"]:
                i = self.info(stop, g["estacoes"][stop]["nome"])
                info = {**i, **{k: v for k, v in info.items() if v}}
            grupo["info"] = info
            e = g["estacoes"][grupo["chave"]]
            grupo["posicao"] = info.get("posicao") or [e["lat"], e["lon"]]
        self.estacoes = list(grupos.values())
        self.por_stop = {stop: grupo for grupo in self.estacoes for stop in grupo["stops"]}

    def estacoes_da_linha(self, linha):
        maior = max(linha["sentidos"], key=lambda s: len(s["estacoes"]))
        # Na ordem do sentido 0 (o da esquerda nas abas do app), quando ele é o maior.
        s0 = next((s for s in linha["sentidos"] if s["id"] == 0), maior)
        return s0["estacoes"] if len(s0["estacoes"]) == len(maior["estacoes"]) else maior["estacoes"]

    @staticmethod
    def partidas(grade, linha_id, sentido_id, stop):
        linha = next((l for l in grade["linhas"] if l["id"] == linha_id), None)
        sentido = linha and next((s for s in linha["sentidos"] if s["id"] == sentido_id), None)
        if not sentido or stop not in sentido["estacoes"]:
            return []
        pos = sentido["estacoes"].index(stop)
        return sorted(v[pos] for v in sentido["viagens"] if v[pos] is not None)

    def sentidos_de_partida(self, linha, stop):
        lista = [s for s in linha["sentidos"] if stop in s["estacoes"] and s["estacoes"].index(stop) < len(s["estacoes"]) - 1]
        return sorted(lista, key=lambda s: s["id"])

    def lugares_perto(self, grupo):
        nomes = {simples(grupo["nome"])} | {simples(nome_estacao(self.util["estacoes"][s]["nome"])) for s in grupo["stops"]}
        lista = [l for l in self.regras.get("lugares", {}).get("lista", [])
                 if l.get("estacao") and l.get("a_pe_m") is not None and l["a_pe_m"] <= PERTO_M
                 and simples(l["estacao"]) in nomes]
        return sorted(lista, key=lambda l: l["a_pe_m"])

    def stop_pelo_nome(self, nome):
        return next((sid for sid, e in self.util["estacoes"].items() if simples(e["nome"]) == simples(nome)), None)

    def linha_do_stop(self, sid):
        return next(l for l in self.linhas if any(sid in s["estacoes"] for s in l["sentidos"]))

    def passagem(self, linha):
        for k, v in self.regras.get("passagem", {}).items():
            if not k.startswith("_") and k.strip().lower() == linha["nome"].strip().lower():
                return v
        # Sem preço publicado (VLT Aeroporto): a integração pode dizer que está gratuita.
        for i in self.regras.get("integracoes", []):
            for chave in ("motivo_a_para_b", "motivo_b_para_a"):
                motivo = i.get(chave, "")
                if linha["nome"].lower() in motivo.lower() and "gratuit" in motivo.lower():
                    return "Gratuito, " + re.sub(r"^.*?gratuit[oa]\s*", "", motivo, flags=re.I)
        return None


def intervalo(partidas, de, ate):
    """O intervalo típico (mediana) entre partidas nessa faixa de horário, em minutos."""
    p = [m for m in partidas if de * 60 <= m < ate * 60]
    gaps = [b - a for a, b in zip(p, p[1:]) if b > a]
    return round(statistics.median(gaps)) if len(gaps) >= 2 else None


def texto_intervalo(partidas):
    pico = intervalo(partidas, 6, 8) or intervalo(partidas, 16, 19)
    fora = intervalo(partidas, 9, 16)
    if pico and fora and abs(pico - fora) >= 3:
        return f"a cada ~{pico} min no pico, ~{fora} min no resto do dia"
    um = fora or pico
    return f"a cada ~{um} min" if um else None


# ---------- moldura das páginas

CABECA = """<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titulo}</title>
<meta name="description" content="{descricao}">
<link rel="canonical" href="{url}">
<link rel="icon" type="image/png" href="{raiz}img/icon.png">
<meta name="theme-color" content="{tema}">
<meta property="og:type" content="website">
<meta property="og:locale" content="pt_BR">
<meta property="og:site_name" content="Metrô Ceará">
<meta property="og:title" content="{titulo}">
<meta property="og:description" content="{descricao}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{imagem}">
<link rel="stylesheet" href="{raiz}site/site.css">
{extra}</head>
<body>
<a class="pular" href="#conteudo">Pular para o conteúdo</a>
<nav class="topo"><div class="largura">
  <a class="marca" href="{raiz}"><img src="{raiz}img/icon.png" alt="" width="32" height="32"><span class="marca-nome">Metrô Ceará</span></a>
  <span class="menu"><a href="{raiz}estacoes/">Estações</a><a href="{raiz}linhas/">Linhas</a><a class="opcional" href="{raiz}perguntas-frequentes/">Dúvidas</a><a class="abrir" href="{raiz}app/">Horários agora</a></span>
</div></nav>
"""

RODAPE = """<footer class="rodape"><div class="largura">
  <p><strong>Metrô Ceará</strong> é um guia independente, feito por um passageiro. <strong>Não é o site oficial do Metrofor</strong>
  nem do Governo do Ceará. Os horários vêm do GTFS que o Metrofor publica; endereços, passagem e acessibilidade, do site
  do Metrofor (conferidos em {conferido}). Lugares por perto: © colaboradores do OpenStreetMap. Fotos das estações: Wikimedia Commons, com o autor e a licença em cada uma.</p>
  <p>Viu algo errado? <a href="{whats}">Conte pelo WhatsApp</a> · <a href="{raiz}perguntas-frequentes/">Perguntas frequentes</a> · <a href="{raiz}">Sobre o app</a> · <a href="{raiz}historia/">História do app</a> · <a href="{raiz}privacidade.html">Privacidade</a></p>
  <p class="gerado">Página gerada em {gerado}.</p>
</div></footer>
{scripts}</body>
</html>
"""


def mes_ano(aaaa_mm):
    """"2026-11" -> "nov/2026"; outro formato volta como veio."""
    if not re.match(r"\d{4}-\d{2}$", aaaa_mm):
        return aaaa_mm
    meses = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]
    return f"{meses[int(aaaa_mm[5:]) - 1]}/{aaaa_mm[:4]}"


def pagina(d, *, caminho, titulo, descricao, corpo, tema="#CE0F2E", extra="", scripts="", imagem=None):
    nivel = caminho.count("/")
    raiz = "../" * nivel
    url = SITE + caminho
    conferido = d.regras.get("fontes", {}).get("passagem", {}).get("conferido", "")
    conferido = mes_ano(conferido)
    return (CABECA.format(titulo=esc(titulo), descricao=esc(descricao), url=esc(url), raiz=raiz, tema=tema, site=SITE, extra=extra,
                          imagem=esc(imagem or SITE + "img/previa.png"))
            + corpo
            + RODAPE.format(conferido=esc(conferido), whats=esc(WHATSAPP + "Metrô Ceará, site: " + url + " — "),
                            raiz=raiz, gerado=datetime.now(FORTALEZA).strftime("%d/%m/%Y"), scripts=scripts))


def migalhas(raiz, itens):
    partes = [f'<a href="{raiz}">Início</a>'] + [f'<a href="{raiz}{h}">{esc(t)}</a>' if h is not None else f"<span>{esc(t)}</span>" for t, h in itens]
    return '<p class="migalhas">' + " › ".join(partes) + "</p>"


def selo(d, linha, raiz, com_link=True):
    estilo = f'style="--cor:{d.cor(linha)};--cor-escuro:{d.cor(linha, True)}"'
    texto = esc(nome_linha(linha["nome"]))
    if com_link:
        return f'<a class="selo" {estilo} href="{raiz}linhas/{slug(linha["nome"])}/">{texto}</a>'
    return f'<span class="selo" {estilo}>{texto}</span>'


def json_ld(obj):
    return '<script type="application/ld+json">' + json.dumps(obj, ensure_ascii=False).replace("</", "<\\/") + "</script>\n"


# ---------- página da estação

def pagina_estacao(d, grupo):
    raiz = "../../"
    nome, info = grupo["nome"], grupo["info"]
    linhas = [l for l, _ in grupo["linhas"]]
    cor = d.cor(linhas[0])
    nomes_linhas = " e ".join(nome_linha(l["nome"]) for l in linhas)
    veic = "metrô" if any(veiculo(l["nome"]) == "metrô" for l in linhas) else "VLT"
    if len({veiculo(l["nome"]) for l in linhas}) > 1:
        veic = "metrô e VLT"

    h = [f'<header class="faixa" style="--cor:{cor};--cor-escuro:{d.cor(linhas[0], True)}"><div class="largura">',
         migalhas(raiz, [("Estações", "estacoes/"), (nome, None)]),
         f'<p class="sobretitulo">Estação de {veic} · {esc(grupo["sistema"])}</p>',
         f"<h1>{esc(nome)}</h1>",
         '<p class="selos">' + "".join(selo(d, l, raiz) for l in linhas) + "</p>"]
    if info.get("endereco"):
        h.append(f'<p class="endereco">📍 {esc(info["endereco"])}</p>')
    lat, lon = grupo["posicao"]
    h.append(f'<p class="acoes"><a class="botao claro" href="{raiz}app/#/estacao/{linhas[0]["id"]}/{grupo["linhas"][0][1]}">Abrir na versão lite</a>'
             f'<a class="botao vazado" href="https://www.google.com/maps/dir/?api=1&amp;destination={lat},{lon}" target="_blank" rel="noopener">Como chegar ›</a></p>')
    h.append("</div></header>")
    h.append('<main id="conteudo" class="largura">')

    # Agora (preenchido pelo agora.js; sem JS, fica o convite para a lite)
    paradas = []
    for linha, stop in grupo["linhas"]:
        sentidos = d.sentidos_de_partida(linha, stop)
        paradas.append({"linha": linha["id"], "stop": stop, "nome": nome_linha(linha["nome"]), "cor": d.cor(linha),
                        "sentidos": [{"id": s["id"], "destino": nome_estacao(s["destino"])} for s in sentidos]})
    h.append(f'<section class="agora" data-paradas="{esc(json.dumps(paradas, ensure_ascii=False))}">'
             '<h2>Próximas partidas</h2><div class="agora-lista"><p class="dica">Carregando os horários de hoje…</p></div>'
             f'<noscript><p>Veja as próximas partidas na <a href="{raiz}app/">versão lite</a>.</p></noscript></section>')

    if info.get("aviso"):
        h.append(f'<p class="aviso">⚠️ {esc(info["aviso"])}</p>')

    fotos = d.fotos.get(grupo["slug"], [])
    if fotos:
        h.append(fotos_do_topo(fotos, nome, raiz))

    # Horários
    h.append('<section><h2>Horários</h2><div class="grade-cartoes">')
    for linha, stop in grupo["linhas"]:
        for s in d.sentidos_de_partida(linha, stop):
            util = d.partidas(d.util, linha["id"], s["id"], stop)
            sab = d.partidas(d.sabado, linha["id"], s["id"], stop)
            linhas_tab = []
            if util:
                linhas_tab.append(f"<tr><th>Segunda a sexta</th><td>{hora(util[0])}</td><td>{hora(util[-1])}</td></tr>")
            linhas_tab.append(f"<tr><th>Sábado</th><td>{hora(sab[0])}</td><td>{hora(sab[-1])}</td></tr>" if sab
                              else '<tr><th>Sábado</th><td colspan="2">sem viagens publicadas</td></tr>')
            linhas_tab.append('<tr><th>Domingo</th><td colspan="2">sem operação (só em dias especiais)</td></tr>')
            iv = texto_intervalo(util)
            h.append(f'<article class="cartao" style="--cor:{d.cor(linha)};--cor-escuro:{d.cor(linha, True)}">'
                     f'<p class="cartao-linha">{esc(nome_linha(linha["nome"]))}</p>'
                     f'<h3>Sentido {esc(nome_estacao(s["destino"]))}</h3>'
                     f'<table class="horarios"><thead><tr><th></th><th>Primeiro</th><th>Último</th></tr></thead><tbody>{"".join(linhas_tab)}</tbody></table>'
                     + (f'<p class="dica">Nos dias úteis, passa {esc(iv)}.</p>' if iv else "")
                     + f'<p class="dica"><a href="{raiz}app/#/estacao/{linha["id"]}/{stop}/{s["id"]}">Ver todos os horários ›</a></p></article>')
    h.append("</div>")
    h.append('<p class="dica">Em feriado, eleição, ENEM e eventos o Metrofor costuma mudar o horário: as próximas partidas acima já '
             'mostram o aviso quando há operação especial anunciada.</p></section>')

    # Na linha: vizinhas
    h.append('<section><h2>Na linha</h2>')
    for linha, stop in grupo["linhas"]:
        ordem = d.estacoes_da_linha(linha)
        i = ordem.index(stop)
        h.append(f'<div class="trilho-mini" style="--cor:{d.cor(linha)};--cor-escuro:{d.cor(linha, True)}">'
                 f'<p class="cartao-linha"><a href="{raiz}linhas/{slug(linha["nome"])}/">{esc(nome_linha(linha["nome"]))} ›</a>'
                 f' <span class="dica">· {i + 1}ª de {len(ordem)} estações</span></p><ol>')
        for j in range(max(0, i - 2), min(len(ordem), i + 3)):
            g = d.por_stop[ordem[j]]
            classe = ' class="aqui"' if j == i else (' class="ponta"' if j in (0, len(ordem) - 1) else "")
            texto = esc(g["nome"])
            h.append(f"<li{classe}>" + (f"<strong>{texto}</strong>" if j == i else f'<a href="{raiz}estacoes/{g["slug"]}/">{texto}</a>') + "</li>")
        h.append("</ol></div>")
    h.append("</section>")

    # Passagem e integração
    h.append('<section><h2>Passagem</h2><ul class="lista">')
    for linha in linhas:
        p = d.passagem(linha)
        h.append(f"<li>{selo(d, linha, raiz, False)} {esc(p) if p else 'veja no site do Metrofor'}</li>")
    h.append("</ul>")
    h.append(texto_integracoes(d, grupo, raiz))
    h.append(f'<p class="dica"><a href="{raiz}app/#/passagem">Meia passagem, gratuidades e como pagar ›</a></p></section>')

    # Acessibilidade
    a = info.get("acesso")
    h.append('<section><h2>Acessibilidade</h2>')
    if a:
        itens = [("🛗", "Elevador", a.get("elevador", 0)), ("↗", "Rampa", a.get("rampa", 0)),
                 ("⇅", "Escada rolante", a.get("escada_rolante", 0)), ("🪜", "Escada", a.get("escada", 0))]
        if a.get("plataforma_elevatoria"):
            itens.insert(1, ("♿", "Plataforma elevatória", a["plataforma_elevatoria"]))
        h.append('<div class="numeros">' + "".join(
            f'<p class="numero{"" if n else " zero"}"><span class="emoji">{e}</span><strong>{n if n else "não tem"}</strong><span>{t}</span></p>'
            for e, t, n in itens) + "</div>")
        h.append('<p class="dica">Dados do Metrofor. Um elevador pode estar parado para manutenção.</p>')
    else:
        h.append("<p>Ainda não temos os dados de elevador e rampa desta estação.</p>")
    if info.get("subterranea"):
        h.append("<p>Estação subterrânea: fica debaixo da terra, e o sinal do celular costuma falhar lá dentro.</p>")
    h.append(f'<p class="dica">Todos os metrôs e VLTs têm espaço para cadeira de rodas. Encontrou algo diferente? '
             f'<a href="{esc(WHATSAPP + "Metrô Ceará · acessibilidade da estação " + nome + ": ")}">Conte pelo WhatsApp</a>.</p></section>')

    # Perto
    perto = d.lugares_perto(grupo)
    if perto:
        h.append(f'<section><h2>Perto da estação</h2><p class="dica">{len(perto)} lugares a até 1 km a pé, pelas ruas.</p><div class="perto">')
        por_tipo = defaultdict(list)
        for l in perto:
            por_tipo[l["tipo"]].append(l)
        for tipo in sorted(por_tipo, key=lambda t: por_tipo[t][0]["a_pe_m"]):
            emoji, titulo = LUGAR_TIPO.get(tipo, ("📍", "Outros"))
            h.append(f'<div class="grupo-perto"><h3>{emoji} {esc(titulo)}</h3><ul>')
            for l in por_tipo[tipo]:
                minutos = max(1, -(-l["a_pe_m"] // METROS_POR_MINUTO))
                h.append(f'<li><span>{esc(l["nome"])}</span><span class="dica">{minutos} min a pé</span></li>')
            h.append("</ul></div>")
        h.append("</div></section>")

    if fotos:
        h.append(fotos_do_fim(fotos, nome, raiz))
    h.append(chamada_app(raiz))
    h.append("</main>")

    descricao = (f"Estação {nome} ({nomes_linhas}): próximas partidas, primeiro e último {veic.split(' ')[0]}, "
                 f"passagem, acessibilidade{', o que tem perto' if perto else ''} e como chegar.")
    ld = {"@context": "https://schema.org", "@type": "TrainStation", "name": f"Estação {nome}",
          "geo": {"@type": "GeoCoordinates", "latitude": lat, "longitude": lon},
          "url": SITE + f"estacoes/{grupo['slug']}/"}
    if info.get("endereco"):
        ld["address"] = info["endereco"]
    if fotos:
        ld["image"] = [SITE + "img/estacoes/" + f["arquivo"] for f in fotos[:3]]
    extra = json_ld(ld) + json_ld({"@context": "https://schema.org", "@type": "BreadcrumbList", "itemListElement": [
        {"@type": "ListItem", "position": 1, "name": "Início", "item": SITE},
        {"@type": "ListItem", "position": 2, "name": "Estações", "item": SITE + "estacoes/"},
        {"@type": "ListItem", "position": 3, "name": nome}]})
    return pagina(d, caminho=f"estacoes/{grupo['slug']}/", titulo=f"Estação {nome} — horários do {veic} ({nomes_linhas}) | Metrô Ceará",
                  descricao=descricao, corpo="\n".join(h), tema=cor, extra=extra,
                  scripts=f'<script type="module" src="{raiz}site/agora.js"></script>\n',
                  imagem=SITE + "img/estacoes/" + fotos[0].get("capa_jpg", fotos[0]["arquivo"]) if fotos else None)


def galeria(fotos, nome, raiz, *, inicio=0, titulo="Fotos", ident="fotos", rodape=""):
    """Grade de fotos com o crédito de cada uma (exigência da licença: autor, licença e link).
    As 4 primeiras da estação carregam na hora; as outras, só quando a pessoa rola até elas."""
    h = [f'<section class="fotos" id="{ident}"><h2>{titulo}</h2><div class="grade-fotos">']
    for n, f in enumerate(fotos, start=inicio):
        src = f'{raiz}img/estacoes/{f["arquivo"]}'
        lic = esc(f["licenca"])
        if f.get("licenca_url"):
            lic = f'<a href="{esc(f["licenca_url"])}" target="_blank" rel="noopener license">{lic}</a>'
        quando = f' · {f["data"][5:7]}/{f["data"][:4]}' if re.match(r"\d{4}-\d{2}", f.get("data", "")) else ""
        h.append(f'<figure><a href="{src}"><img src="{src}" alt="Estação {esc(nome)}, foto {n + 1}" width="{f["largura"]}" '
                 f'height="{f["altura"]}"{"" if n < FOTOS_NO_TOPO else " loading=\"lazy\""} decoding="async"></a>'
                 f'<figcaption><a href="{esc(f["pagina"])}" target="_blank" rel="noopener">{esc(f["autor"])}</a> · {lic}{quando}</figcaption></figure>')
    h.append(f"</div>{rodape}</section>")
    return "".join(h)


def fotos_do_topo(fotos, nome, raiz):
    resto = len(fotos) - FOTOS_NO_TOPO
    mais = (f'<a href="#mais-fotos">Mais {resto} foto{"s" if resto > 1 else ""} no fim da página ↓</a> · ' if resto > 0 else "")
    rodape = (f'<p class="dica">{mais}Fotos do Wikimedia Commons (reduzidas para o site), de quem fotografou e liberou o uso. '
              'Toque no nome para ver a original.</p>')
    return galeria(fotos[:FOTOS_NO_TOPO], nome, raiz, rodape=rodape)


def fotos_do_fim(fotos, nome, raiz):
    resto = fotos[FOTOS_NO_TOPO:]
    if not resto:
        return ""
    return galeria(resto, nome, raiz, inicio=FOTOS_NO_TOPO, ident="mais-fotos",
                   titulo=f'Mais fotos da estação {esc(nome)} <span class="dica">· {len(resto)}</span>',
                   rodape='<p class="dica">Fotos do Wikimedia Commons. <a href="#fotos">Voltar ao começo das fotos ↑</a></p>')


def frase_integracao(i, de, para, chave):
    """Um sentido da integração, com a passagem (regras.json), como item de lista."""
    texto = {"gratis": "não paga outra passagem", "paga": "paga outra passagem"}.get(i.get(f"passagem_{chave}"))
    if not texto:
        return ""
    motivo = i.get(f"motivo_{chave}")
    return (f"<li>{da(de).capitalize()} {esc(nome_linha(de['nome']))} para {para_a(para)} {esc(nome_linha(para['nome']))}: {texto}"
            + (f" ({esc(motivo)})" if motivo else "") + ".</li>")


def texto_integracoes(d, grupo, raiz):
    """As integrações desta estação, com a passagem em cada sentido (regras.json)."""
    itens = []
    for i in d.regras.get("integracoes", []):
        sa, sb = d.stop_pelo_nome(i.get("a", "")), d.stop_pelo_nome(i.get("b", ""))
        if not sa or not sb or (sa not in grupo["stops"] and sb not in grupo["stops"]):
            continue
        la, lb = d.linha_do_stop(sa), d.linha_do_stop(sb)
        outra = d.por_stop[sb if sa in grupo["stops"] else sa]
        onde = "" if outra is grupo else f' com a estação <a href="{raiz}estacoes/{outra["slug"]}/">{esc(outra["nome"])}</a>'
        itens.append(f"<p><strong>Integração</strong>{onde}: {i['caminhada_min']} min a pé.</p><ul class=\"lista\">"
                     + frase_integracao(i, la, lb, "a_para_b") + frase_integracao(i, lb, la, "b_para_a") + "</ul>")
    return "".join(itens)


def chamada_app(raiz):
    return (f'<aside class="chamada"><div><h2>Leve no bolso</h2><p>Na <strong>versão lite</strong>, a próxima partida da estação mais perto, '
            f'em qualquer celular, sem instalar. No <strong>app para Android</strong>, o modo viagem avisa antes da sua estação.</p></div>'
            f'<p class="acoes"><a class="botao" href="{raiz}app/">Abrir a versão lite</a><a class="botao vazado-escuro" href="{raiz}#baixar">App para Android</a></p></aside>')


# ---------- página da linha

def pagina_linha(d, linha):
    raiz = "../../"
    nome = nome_linha(linha["nome"])
    cor, cor_e = d.cor(linha), d.cor(linha, True)
    ordem = d.estacoes_da_linha(linha)
    veic = veiculo(linha["nome"])
    sistema = SISTEMA.get(linha["id"], "Fortaleza")
    viagens = sum(len(s["viagens"]) for s in linha["sentidos"])
    duracoes = [max(m for m in v if m is not None) - min(m for m in v if m is not None)
                for s in linha["sentidos"] for v in s["viagens"] if v[0] is not None and v[-1] is not None]
    duracao = round(statistics.median(duracoes)) if duracoes else None
    passagem = d.passagem(linha)

    h = [f'<header class="faixa" style="--cor:{cor};--cor-escuro:{cor_e}"><div class="largura">',
         migalhas(raiz, [("Linhas", "linhas/"), (nome, None)]),
         f'<p class="sobretitulo">{"Metrô" if veic == "metrô" else "VLT"} · {esc(sistema)}</p>',
         f"<h1>{esc(nome)}</h1>",
         f'<p class="endereco">{esc(linha["descricao"])}</p>',
         '<div class="numeros claros">',
         f'<p class="numero"><strong>{len(ordem)}</strong><span>estações</span></p>']
    if duracao:
        h.append(f'<p class="numero"><strong>~{duracao} min</strong><span>de ponta a ponta</span></p>')
    h.append(f'<p class="numero"><strong>{viagens}</strong><span>viagens por dia útil</span></p>')
    if passagem:
        h.append(f'<p class="numero"><strong>{esc(passagem.split(",")[0] if passagem.startswith("Gratuit") else passagem.split(" (")[0].split(" ·")[0])}</strong><span>passagem</span></p>')
    h.append("</div></div></header>")
    h.append('<main id="conteudo" class="largura">')
    if passagem:
        h.append(f"<p><strong>Passagem:</strong> {esc(passagem)}.</p>")

    # Trilho vertical com todas as estações
    h.append(f'<div class="duas-colunas"><section><h2>Estações</h2><ol class="trilho" style="--cor:{cor};--cor-escuro:{cor_e}">')
    for j, stop in enumerate(ordem):
        g = d.por_stop[stop]
        marcas = []
        outras = [l for l, _ in g["linhas"] if l["id"] != linha["id"]]
        for l in outras:
            marcas.append(selo(d, l, raiz, False))
        if g["info"].get("subterranea"):
            marcas.append('<span class="marca-texto">subterrânea</span>')
        if g["info"].get("acesso", {}).get("elevador"):
            marcas.append('<span class="marca-texto">elevador</span>')
        classe = ' class="ponta"' if j in (0, len(ordem) - 1) else ""
        h.append(f'<li{classe}><a href="{raiz}estacoes/{g["slug"]}/">{esc(g["nome"])}</a> {" ".join(marcas)}</li>')
    h.append("</ol></section>")

    # Horários por sentido
    h.append("<section><h2>Horários</h2>")
    for s in sorted(linha["sentidos"], key=lambda s: s["id"]):
        origem = s["estacoes"][0]
        util = d.partidas(d.util, linha["id"], s["id"], origem)
        sab = d.partidas(d.sabado, linha["id"], s["id"], origem)
        iv = texto_intervalo(util)
        h.append(f'<article class="cartao" style="--cor:{cor};--cor-escuro:{cor_e}"><h3>Sentido {esc(nome_estacao(s["destino"]))}</h3>'
                 f'<p class="dica">Saindo de {esc(d.por_stop[origem]["nome"])}</p><table class="horarios"><thead><tr><th></th><th>Primeiro</th><th>Último</th></tr></thead><tbody>'
                 + (f"<tr><th>Segunda a sexta</th><td>{hora(util[0])}</td><td>{hora(util[-1])}</td></tr>" if util else
                    '<tr><th>Segunda a sexta</th><td colspan="2">sem viagens publicadas</td></tr>')
                 + (f"<tr><th>Sábado</th><td>{hora(sab[0])}</td><td>{hora(sab[-1])}</td></tr>" if sab else
                    '<tr><th>Sábado</th><td colspan="2">sem viagens publicadas</td></tr>')
                 + '<tr><th>Domingo</th><td colspan="2">sem operação</td></tr></tbody></table>'
                 + (f'<p class="dica">Nos dias úteis, passa {esc(iv)}.</p>' if iv else "") + "</article>")
    pico = d.regras.get("pico", {})
    if pico.get("faixas"):
        faixas = " e ".join(f"das {a[:2].lstrip('0')}h às {b[:2].lstrip('0')}h" for a, b in pico["faixas"])
        h.append(f'<p class="dica">Horários mais cheios: {faixas} (Relatório de Demanda do Metrofor, 2025).</p>')
    h.append("<p class=\"dica\">Domingo: o metrô e o VLT só circulam em dias especiais (eleição, ENEM, eventos), "
             "quando o Metrofor anuncia.</p></section></div>")

    outras = [l for l in d.linhas if l["id"] != linha["id"]]
    h.append('<section><h2>Outras linhas</h2><p class="selos">' + "".join(selo(d, l, raiz) for l in outras) + "</p></section>")
    h.append(chamada_app(raiz))
    h.append("</main>")

    pontas = f"{d.por_stop[ordem[0]]['nome']} a {d.por_stop[ordem[-1]]['nome']}"
    descricao = (f"{nome} ({'metrô' if veic == 'metrô' else 'VLT'} de {sistema}): as {len(ordem)} estações de {pontas}, "
                 f"primeiro e último horário, intervalo e passagem.")
    return pagina(d, caminho=f"linhas/{slug(linha['nome'])}/", titulo=f"{nome} — estações e horários | Metrô Ceará",
                  descricao=descricao, corpo="\n".join(h), tema=cor)


# ---------- índices

def indice_estacoes(d):
    raiz = "../"
    h = ['<header class="faixa" style="--cor:#CE0F2E;--cor-escuro:#CE0F2E"><div class="largura">',
         migalhas(raiz, [("Estações", None)]),
         f"<h1>Estações</h1><p class=\"endereco\">As {len(d.estacoes)} estações de metrô e VLT de Fortaleza, do Cariri e de Sobral.</p>",
         '<p><input class="filtro" type="search" placeholder="Procurar estação…" aria-label="Procurar estação" data-filtro></p>',
         '</div></header><main id="conteudo" class="largura">']
    for sistema in ORDEM_SISTEMAS:
        linhas = [l for l in d.linhas if SISTEMA.get(l["id"]) == sistema]
        h.append(f"<section><h2>{esc(sistema)}</h2><div class=\"colunas-linhas\">")
        for linha in linhas:
            h.append(f'<div class="bloco-linha">{selo(d, linha, raiz)}<ol class="trilho" style="--cor:{d.cor(linha)};--cor-escuro:{d.cor(linha, True)}">')
            ordem = d.estacoes_da_linha(linha)
            for j, stop in enumerate(ordem):
                g = d.por_stop[stop]
                classe = ' class="ponta"' if j in (0, len(ordem) - 1) else ""
                h.append(f'<li{classe} data-nome="{esc(slug(g["nome"]))}"><a href="{g["slug"]}/">{esc(g["nome"])}</a></li>')
            h.append("</ol></div>")
        h.append("</div></section>")
    h.append("</main>")
    # ?q= vem da busca da página principal, quando o texto não é o nome exato de uma estação.
    script = ("<script>const f=document.querySelector('[data-filtro]');const filtrar=()=>{const q=f.value.normalize('NFD')"
              ".replace(/[\\u0300-\\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');"
              "document.querySelectorAll('li[data-nome]').forEach(li=>li.hidden=!!q&&!li.dataset.nome.includes(q))};"
              "f.addEventListener('input',filtrar);const q=new URLSearchParams(location.search).get('q');if(q){f.value=q;filtrar()}</script>\n")
    return pagina(d, caminho="estacoes/", titulo="Estações de metrô e VLT do Ceará — horários e endereços | Metrô Ceará",
                  descricao="Todas as estações do metrô e do VLT de Fortaleza, Cariri e Sobral: horários, endereço, passagem, acessibilidade e o que tem perto.",
                  corpo="\n".join(h), scripts=script)


def indice_linhas(d):
    raiz = "../"
    h = ['<header class="faixa" style="--cor:#CE0F2E;--cor-escuro:#CE0F2E"><div class="largura">',
         migalhas(raiz, [("Linhas", None)]),
         f'<h1>Linhas</h1><p class="endereco">{len(d.linhas)} linhas de metrô e VLT em três cidades.</p></div></header>',
         '<main id="conteudo" class="largura">']
    for sistema in ORDEM_SISTEMAS:
        h.append(f'<section><h2>{esc(sistema)}</h2><div class="grade-cartoes">')
        for linha in [l for l in d.linhas if SISTEMA.get(l["id"]) == sistema]:
            ordem = d.estacoes_da_linha(linha)
            p = d.passagem(linha)
            h.append(f'<a class="cartao cartao-link" style="--cor:{d.cor(linha)};--cor-escuro:{d.cor(linha, True)}" href="{slug(linha["nome"])}/">'
                     f'<p class="cartao-linha">{"Metrô" if veiculo(linha["nome"]) == "metrô" else "VLT"}</p>'
                     f'<h3>{esc(nome_linha(linha["nome"]))}</h3>'
                     f'<p>{esc(d.por_stop[ordem[0]]["nome"])} ⟷ {esc(d.por_stop[ordem[-1]]["nome"])}</p>'
                     f'<p class="dica">{len(ordem)} estações' + (f" · {esc(p.split(' ·')[0])}" if p else "") + "</p></a>")
        h.append("</div></section>")
    h.append("</main>")
    return pagina(d, caminho="linhas/", titulo="Linhas de metrô e VLT do Ceará | Metrô Ceará",
                  descricao="As linhas do metrô e do VLT de Fortaleza, Cariri e Sobral: estações, horários e passagem de cada uma.",
                  corpo="\n".join(h))


# ---------- perguntas frequentes

def hora_falada(h):
    """"09:00" -> "9h"; "16:30" -> "16h30"."""
    hh, mm = h.split(":")
    return f"{int(hh)}h" + ("" if mm == "00" else mm)


def janelas_bike(janelas):
    if not janelas:
        return "não pode"
    return " e ".join(f"a partir das {hora_falada(a)}" if b is None else f"das {hora_falada(a)} às {hora_falada(b)}" for a, b in janelas)


def juntar(nomes):
    nomes = list(nomes)
    return nomes[0] if len(nomes) == 1 else ", ".join(nomes[:-1]) + " e " + nomes[-1]


def linha_de_fonte(d, ids):
    """De onde veio a resposta (regras.json "fontes"), com o link e a data da conferência."""
    partes = []
    for i in ids:
        f = d.regras.get("fontes", {}).get(i)
        if not f:
            continue
        nome = esc(f["fonte"])
        if f.get("link"):
            nome = f'<a href="{esc(f["link"])}" target="_blank" rel="noopener">{nome}</a>'
        quando = "acompanhado todo dia" if f.get("diario") else (f"conferido em {mes_ano(f['conferido'])}" if f.get("conferido") else "")
        partes.append(("" if f.get("tipo") == "app" else "Fonte: ") + nome + (f" · {quando}" if quando else ""))
    return "".join(f'<p class="dica">{p}</p>' for p in partes)


def tabela_horarios_linhas(d, raiz):
    """Primeira e última partida de cada linha, saindo das pontas."""
    def faixa(grade, linha):
        p = [m for s in linha["sentidos"] if s["estacoes"] for m in d.partidas(grade, linha["id"], s["id"], s["estacoes"][0])]
        return f"{hora(min(p))}–{hora(max(p))}" if p else "sem viagens"

    h = ['<table class="horarios compacta"><thead><tr><th></th><th>Segunda a sexta</th><th>Sábado</th></tr></thead><tbody>']
    for sistema in ORDEM_SISTEMAS:
        for linha in [l for l in d.linhas if SISTEMA.get(l["id"]) == sistema]:
            h.append(f"<tr><th>{selo(d, linha, raiz)}</th><td>{faixa(d.util, linha)}</td><td>{faixa(d.sabado, linha)}</td></tr>")
    h.append("</tbody></table>")
    return "".join(h)


def resposta_passagem(d, raiz):
    h = ['<ul class="lista">']
    for sistema in ORDEM_SISTEMAS:
        for linha in [l for l in d.linhas if SISTEMA.get(l["id"]) == sistema]:
            p = d.passagem(linha)
            h.append(f"<li>{selo(d, linha, raiz, False)} {esc(p) if p else 'veja no site do Metrofor'}</li>")
    h.append("</ul>")
    temas = {t["titulo"]: t for t in d.regras.get("guia_passagem", {}).get("temas", [])}
    meia = temas.get("Meia de estudante")
    if meia:
        h.append("<p><strong>Meia de estudante:</strong></p><ul class=\"lista\">")
        for q in meia["secoes"][0].get("quadros", []):
            h.append(f"<li>{esc(q['titulo'])}: {esc(q['texto'])}</li>")
        h.append("</ul>")
    if "Gratuidade" in temas:
        h.append(f"<p><strong>Quem não paga:</strong> {esc(temas['Gratuidade']['resumo'])}</p>")
    h.append(f'<p><a href="{raiz}app/#/passagem">Onde tirar o cartão, o que levar e as outras regras ›</a></p>')
    return "".join(h)


def resposta_integracao(d):
    h = ["<p>Depende de onde e em que sentido você troca de linha:</p>"]
    for i in d.regras.get("integracoes", []):
        sa, sb = d.stop_pelo_nome(i.get("a", "")), d.stop_pelo_nome(i.get("b", ""))
        if not sa or not sb:
            continue
        ga, gb = d.por_stop[sa], d.por_stop[sb]
        la, lb = d.linha_do_stop(sa), d.linha_do_stop(sb)
        onde = f"Em {esc(ga['nome'])}" if ga is gb else f"Entre {esc(ga['nome'])} e {esc(gb['nome'])}"
        h.append(f"<p><strong>{onde}</strong> ({i['caminhada_min']} min a pé):</p><ul class=\"lista\">"
                 + frase_integracao(i, la, lb, "a_para_b") + frase_integracao(i, lb, la, "b_para_a") + "</ul>")
    h.append('<p class="dica">Onde está "paga outra passagem", é a nossa conclusão: a estação cobra na entrada e o Metrofor '
             "não anunciou integração de passagem nesses pontos.</p>")
    return "".join(h)


def resposta_bike(d):
    bike = d.regras.get("bike", {})
    por_linha = {k: v for k, v in bike.get("linhas", {}).items() if not k.startswith("_")}
    h = []
    for id_regra, regra in bike.get("regulamentos", {}).items():
        nomes = [nome_linha(k) for k, v in por_linha.items() if v == id_regra]
        if not nomes:
            continue
        h.append(f"<p><strong>{esc(juntar(nomes))}:</strong></p><ul class=\"lista\">"
                 f"<li>Segunda a sexta: {janelas_bike(regra.get('dias_uteis', []))}.</li>"
                 f"<li>Sábado: {janelas_bike(regra.get('sabado', []))}.</li>"
                 f"<li>Até {regra['limite_por_viagem']} bikes por viagem.</li></ul>")
    if bike.get("proibidas"):
        h.append(f"<p><strong>{esc(bike['proibidas'])}</strong></p>")
    sem_regra = [nome_linha(l["nome"]) for l in d.linhas if l["nome"] not in por_linha]
    if sem_regra:
        h.append(f"<p>{esc(juntar(sem_regra))}: não encontramos regra publicada. Pergunte na estação antes de levar a bike.</p>")
    h.append('<p class="dica">Em feriado e operação especial, vale o que o Metrofor anunciar para o dia.</p>')
    return "".join(h)


def pagina_faq(d):
    raiz = "../"
    subterraneas = [g["nome"] for g in d.estacoes if g["info"].get("subterranea")]
    com_dados = sum(1 for g in d.estacoes if g["info"].get("acesso"))
    # As páginas das linhas do Metrofor e a grade publicada discordam sobre o fim do sábado: vale a grade, com o aviso.
    data_sabado = d.sabado.get("data", "")
    quando_sabado = f" de {data_sabado[8:10]}/{data_sabado[5:7]}/{data_sabado[:4]}" if re.match(r"\d{4}-\d{2}-\d{2}", data_sabado) else ""
    aviso_sabado = (f'<p class="dica">Sábado: horários da programação{quando_sabado}. O horário de fim varia de um sábado para outro, '
                    "e em algumas linhas o site do Metrofor diz outro. Confira as próximas partidas no dia.</p>")

    perguntas = [
        ("domingo", "O metrô e o VLT funcionam domingo e feriado?",
         "<p><strong>Domingo: normalmente, não.</strong> O metrô e o VLT só circulam em domingo especial, como eleição, ENEM e eventos grandes, "
         "quando o Metrofor anuncia uma operação especial. Nas eleições de 2026 e no ENEM de 2025, a operação foi gratuita.</p>"
         "<p><strong>Feriado: costuma ter operação especial, com horário menor.</strong> Nos feriados de 2026, as linhas Sul, Nordeste e Oeste "
         "começaram por volta das 5h30 e foram até o início da noite. Na maioria deles, os VLTs de Sobral e do Cariri não circularam. "
         "O horário muda de um feriado para outro.</p>"
         f'<p>A <a href="{raiz}app/">versão lite</a> e o app mostram o aviso assim que o Metrofor anuncia.</p>',
         ["operacoes"]),
        ("horarios", "A que horas começam e terminam o metrô e o VLT?",
         "<p>Da primeira à última partida de cada linha, saindo das pontas:</p>" + tabela_horarios_linhas(d, raiz)
         + f'<p>Na sua estação o horário é outro. Veja em <a href="{raiz}estacoes/">Estações</a> ou nas '
         f'<a href="{raiz}app/">próximas partidas de agora</a>.</p>'
         + aviso_sabado,
         []),
        ("passagem", "Quanto custa a passagem? Como funciona a meia?", resposta_passagem(d, raiz), ["passagem", "guia_passagem"]),
        ("integracao", "A integração entre as linhas é paga?", resposta_integracao(d), ["integracao"]),
        ("bike", "Posso levar bicicleta no metrô e no VLT?", resposta_bike(d), ["bike", "bike_sobral"]),
        ("acessibilidade", "As estações têm elevador e rampa?",
         f'<p>Depende da estação. A <a href="{raiz}estacoes/">página de cada estação</a> mostra quantos elevadores, rampas e escadas ela tem '
         f"(temos os dados de {com_dados} das {len(d.estacoes)} estações). Todos os metrôs e VLTs têm espaço para cadeira de rodas.</p>"
         "<p>Os números vêm de tabelas do Metrofor de 2015, e um elevador pode estar parado para manutenção. "
         f'Encontrou algo diferente? <a href="{esc(WHATSAPP + "Metrô Ceará · acessibilidade: ")}">Conte pelo WhatsApp</a>.</p>',
         ["acessibilidade"]),
        ("exato", "O horário é exato? Por que às vezes muda?",
         "<p>São os horários programados, que o Metrofor publica para cada dia. Eles não mostram onde o metrô está agora, "
         "então um atraso não aparece.</p>"
         "<p>Eles mudam porque o Metrofor pode trocar a programação de um dia para o outro, e às vezes durante o próprio dia. "
         "Por isso os horários são baixados de novo todo dia.</p>",
         []),
        ("instalar", "Precisa instalar? Funciona sem internet e no iPhone?",
         f'<p>Para ver os horários, não precisa instalar nada. A <a href="{raiz}app/">versão lite</a> abre no navegador de qualquer celular, '
         "iPhone também, e pode ficar com um ícone na tela inicial. Depois de aberta uma vez, ela funciona sem internet, com os últimos horários guardados.</p>"
         f'<p>O <a href="{raiz}#baixar">app para Android</a> acrescenta o modo viagem, que avisa antes da sua estação mesmo com a tela desligada. '
         "Ele é grátis e está na Play Store.</p>"
         + (f"<p>Em {esc(juntar(subterraneas))}, que são estações subterrâneas, o sinal do celular costuma falhar. "
            "Leve dinheiro ou deixe o Pix aberto antes de descer.</p>" if subterraneas else ""),
         ["subterranea"]),
        ("oficial", "O Metrô Ceará é do Metrofor?",
         "<p><strong>Não.</strong> É um guia independente, feito por um passageiro, sem vínculo com o Metrofor, o Governo do Ceará ou a Prefeitura de Fortaleza. "
         "As informações vêm do que o Metrofor publica, e cada uma diz a fonte. Não tem anúncio nem cadastro, e o "
         '<a href="https://github.com/giovanildo/metro-ceara-horarios" target="_blank" rel="noopener">código é aberto</a>.</p>'
         '<p>Reclamação sobre o serviço do metrô ou do VLT é com o <a href="https://www.ce.gov.br/metrofor/" target="_blank" rel="noopener">Metrofor</a>. '
         f'Erro no guia, <a href="{esc(WHATSAPP + "Metrô Ceará · perguntas frequentes: ")}">conte pelo WhatsApp</a>.</p>',
         []),
    ]

    h = ['<header class="faixa" style="--cor:#CE0F2E;--cor-escuro:#CE0F2E"><div class="largura">',
         migalhas(raiz, [("Perguntas frequentes", None)]),
         '<h1>Perguntas frequentes</h1><p class="endereco">O que mais se pergunta sobre o metrô e o VLT de Fortaleza, do Cariri e de Sobral.</p>',
         '</div></header><main id="conteudo" class="largura"><nav aria-label="Perguntas"><ul class="lista">']
    h += [f'<li><a href="#{i}">{esc(q)}</a></li>' for i, q, _, _ in perguntas]
    h.append("</ul></nav>")
    h += [f'<section id="{i}"><h2>{esc(q)}</h2>{r}{linha_de_fonte(d, f)}</section>' for i, q, r, f in perguntas]
    h.append(chamada_app(raiz))
    h.append("</main>")

    def so_texto(r):
        return re.sub(r"\s+([.,:;)])", r"\1", " ".join(html.unescape(re.sub(r"<[^>]+>", " ", r)).split()))

    ld = json_ld({"@context": "https://schema.org", "@type": "FAQPage", "mainEntity": [
        {"@type": "Question", "name": q, "acceptedAnswer": {"@type": "Answer", "text": so_texto(r)}} for _, q, r, _ in perguntas]})
    return pagina(d, caminho="perguntas-frequentes/", titulo="Perguntas frequentes sobre metrô e VLT no Ceará | Metrô Ceará",
                  descricao="Funciona domingo? Até que horas? Quanto custa? Pode levar bike? As respostas sobre o metrô e o VLT de Fortaleza, Cariri e Sobral, com a fonte de cada uma.",
                  corpo="\n".join(h), extra=ld)


# ---------- história do app (linha do tempo das versões)

MESES = ["jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"]


def data_curta(iso):
    """"2026-10-25" -> "25 out"."""
    return f"{int(iso[8:10])} {MESES[int(iso[5:7]) - 1]}"


def grafico_commits(commits, inicio, fim):
    """Barras com os commits de cada dia, de inicio a fim (ISO), em SVG que segue o tema."""
    um_dia = timedelta(days=1)
    d0 = datetime.strptime(inicio, "%Y-%m-%d")
    dias = (datetime.strptime(fim, "%Y-%m-%d") - d0).days + 1
    por_dia = dict(commits)
    maior = max(por_dia.values())
    teto = max(20, -(-maior // 20) * 20)
    W, H, L, R, T, B = 820, 170, 30, 6, 14, 26
    cw = (W - L - R) / dias
    alt = lambda n: (H - T - B) * n / teto
    p = [f'<svg viewBox="0 0 {W} {H}" role="img" aria-label="Commits por dia; o dia com mais teve {maior}">']
    for v in range(0, teto + 1, 20):
        y = H - B - alt(v)
        p.append(f'<line x1="{L}" x2="{W - R}" y1="{y:.1f}" y2="{y:.1f}" class="grade"/>'
                 f'<text x="{L - 6}" y="{y + 4:.1f}" text-anchor="end">{v}</text>')
    for i in range(dias):
        dia = d0 + i * um_dia
        iso = dia.strftime("%Y-%m-%d")
        n = por_dia.get(iso, 0)
        x = L + i * cw
        if n:
            classe = "barra pico" if n >= teto * 2 / 3 else "barra"
            p.append(f'<rect x="{x + 1:.1f}" y="{H - B - alt(n):.1f}" width="{max(cw - 2, 2):.1f}" height="{alt(n):.1f}" rx="1.5" class="{classe}">'
                     f"<title>{data_curta(iso)}: {n} commits</title></rect>")
            if n == maior:
                p.append(f'<text x="{x + cw / 2:.1f}" y="{H - B - alt(n) - 4:.1f}" text-anchor="middle" class="forte">{n}</text>')
        if i == 0 or dia.day in (1, 15):
            p.append(f'<text x="{x + cw / 2:.1f}" y="{H - 8}" text-anchor="middle">{data_curta(iso)}</text>')
    p.append("</svg>")
    return "".join(p)


def pagina_historia():
    with open(HISTORIA, encoding="utf-8") as fh:
        h = json.load(fh)
    raiz = "../"
    commits = h["commits"]
    inicio, fim = commits[0][0], h["atualizado"]
    dias = (datetime.strptime(fim, "%Y-%m-%d") - datetime.strptime(inicio, "%Y-%m-%d")).days + 1
    ultima = h["fases"][-1]["paradas"][-1]["versao"].split()[-1]
    atualizacoes = int(ultima.split(".")[1])
    numeros = [(dias, "dias de projeto"), (len(commits), "dias de trabalho"),
               (sum(n for _, n in commits), "mudanças no código (commits)"), (atualizacoes, f"atualizações, da 1.1 à {ultima}")]
    corpo = ['<header class="faixa" style="--cor:#CE0F2E;--cor-escuro:#CE0F2E"><div class="largura">',
             migalhas(raiz, [("História do app", None)]),
             '<h1>História do app</h1>',
             f'<p class="endereco">Do primeiro dia, em {data_curta(inicio)}, à Play Store: o que entrou no Metrô Ceará, dia a dia.</p>',
             '<div class="numeros claros">']
    corpo += [f'<p class="numero"><strong>{n}</strong><span>{esc(t)}</span></p>' for n, t in numeros]
    corpo += ['</div></div></header><main id="conteudo" class="largura">',
              '<section class="ritmo"><h2>Ritmo de trabalho</h2><div class="grafico">', grafico_commits(commits, inicio, fim), '</div>',
              '<p class="dica">Cada barra é um dia, com o número de mudanças salvas no código. Toque numa barra para ver o dia.</p></section>']
    for f in h["fases"]:
        paradas = f["paradas"]
        periodo = f"{data_curta(paradas[0]['data'])} a {data_curta(paradas[-1]['data'])}"
        corpo.append(f'<section class="fase" style="--cor:{f["cor"]};--cor-escuro:{f["cor_escuro"]}">'
                     f'<h2>{esc(f["nome"])} <span class="periodo">{periodo}</span></h2><p class="fase-texto">{esc(f["texto"])}</p><ol class="linha-tempo">')
        for s in paradas:
            versao = f'<span class="versao">v{esc(s["versao"])}</span>' if s.get("versao") else ""
            itens = "".join(f"<li>{esc(i)}</li>" for i in s["itens"])
            classe = ' class="destaque"' if s.get("destaque") else ""
            corpo.append(f'<li{classe}><p class="dia"><time datetime="{s["data"]}">{data_curta(s["data"])}</time>{versao}</p><ul>{itens}</ul></li>')
        corpo.append("</ol></section>")
    corpo.append('<p class="dica">As datas vêm do histórico do <a href="https://github.com/giovanildo/metro-ceara-horarios" target="_blank" rel="noopener">código aberto</a> do app.</p>')
    corpo.append(chamada_app(raiz))
    corpo.append("</main>")
    return corpo


# ---------- mapa (posições reais das estações) e página principal

# Estações com nome escrito no mapa; as outras só têm o ponto (o nome aparece ao tocar).
ROTULOS = {
    "Fortaleza": ["Chico da Silva", "Moura Brasil", "Caucaia", "Carlito Benevides", "Parangaba", "Iate",
                  "Papicu", "Expedicionários", "Aeroporto", "Maracanaú", "Mondubim"],
    "Cariri": ["Crato", "Juazeiro do Norte", "Fátima", "Muriti"],
    "Sobral": ["Novo Recanto", "Cohab III", "Cohab II", "Sumaré", "Campo dos Velhos", "Dom José"],
}
# Lado do nome quando o da direita bate em outro (estações vizinhas).
ESQUERDA = {"Caucaia", "Parangaba", "Mondubim", "Carlito Benevides",
            "Maracanaú", "Crato", "Muriti", "Novo Recanto", "Cohab II", "Campo dos Velhos"}
ACIMA = {"Iate", "Sumaré", "Fátima", "Moura Brasil"}
ABAIXO = {"Juazeiro do Norte", "Chico da Silva"}


def mapa_svg(d, sistema, largura, raiz="", mx=120):
    """As linhas de uma cidade desenhadas pelas posições das estações (latitude e longitude)."""
    import math
    linhas = [l for l in d.linhas if SISTEMA.get(l["id"]) == sistema]
    grupos = {id(d.por_stop[s]): d.por_stop[s] for l in linhas for s in d.estacoes_da_linha(l)}.values()
    lats = [g["posicao"][0] for g in grupos]
    lons = [g["posicao"][1] for g in grupos]
    k = math.cos(math.radians(sum(lats) / len(lats)))
    my = 26  # margem de cima e de baixo; mx (lados) dá espaço para os nomes
    escala = (largura - 2 * mx) / max((max(lons) - min(lons)) * k, 1e-9)
    altura = round((max(lats) - min(lats)) * escala + 2 * my)
    if altura > largura * 1.1:  # cidade "em pé": a escala vem da altura
        escala = (largura * 1.1 - 2 * my) / (max(lats) - min(lats))
        altura = round(largura * 1.1)
    x0 = (largura - (max(lons) - min(lons)) * k * escala) / 2

    def xy(g):
        lat, lon = g["posicao"]
        return round(x0 + (lon - min(lons)) * k * escala, 1), round(my + (max(lats) - lat) * escala, 1)

    partes = [f'<svg class="mapa" viewBox="0 0 {largura} {altura}" role="img" aria-label="Mapa das linhas de {esc(sistema)}">']
    for l in linhas:
        pontos = " ".join(f"{x},{y}" for x, y in (xy(d.por_stop[s]) for s in d.estacoes_da_linha(l)))
        partes.append(f'<polyline class="trilho-mapa" style="--cor:{d.cor(l)};--cor-escuro:{d.cor(l, True)}" points="{pontos}"/>')
    rotulos = set(ROTULOS.get(sistema, []))
    for g in sorted(grupos, key=lambda g: g["nome"] in rotulos):
        x, y = xy(g)
        cores = {d.cor(l) for l, _ in g["linhas"]}
        l0 = g["linhas"][0][0]
        integra = len(g["linhas"]) > 1
        a = [f'<a href="{raiz}estacoes/{g["slug"]}/" style="--cor:{d.cor(l0)};--cor-escuro:{d.cor(l0, True)}"><title>{esc(g["nome"])}</title>',
             f'<circle class="alvo" cx="{x}" cy="{y}" r="11"/>',
             f'<circle class="ponto{" integra" if integra or len(cores) > 1 else ""}" cx="{x}" cy="{y}" r="{6.5 if integra else 4.5}"/>']
        if g["nome"] in rotulos:
            if g["nome"] in ACIMA or g["nome"] in ABAIXO:
                a.append(f'<text x="{x}" y="{y - 12 if g["nome"] in ACIMA else y + 22}" text-anchor="middle">{esc(g["nome"])}</text>')
            else:
                esq = g["nome"] in ESQUERDA
                a.append(f'<text x="{x - 11 if esq else x + 11}" y="{y + 4.5}" text-anchor="{"end" if esq else "start"}">{esc(g["nome"])}</text>')
        a.append("</a>")
        partes.append("".join(a))
    partes.append("</svg>")
    return "".join(partes)


def trechos_da_inicial(d):
    """O que a página principal (docs/index.html, feita à mão) recebe pronto do gerador."""
    raiz = ""
    def legenda(sistema):
        return '<p class="selos">' + "".join(selo(d, l, raiz) for l in d.linhas if SISTEMA.get(l["id"]) == sistema) + "</p>"
    opcoes = "".join(f'<option value="{esc(g["nome"])}" data-slug="{g["slug"]}">' for g in sorted(d.estacoes, key=lambda g: slug(g["nome"])))
    return {
        "estacoes": str(len(d.estacoes)),
        "linhas": str(len(d.linhas)),
        "busca": f'<datalist id="lista-estacoes">{opcoes}</datalist>',
        "mapa-fortaleza": mapa_svg(d, "Fortaleza", 400, mx=62) + legenda("Fortaleza"),
        "mapa-cariri": mapa_svg(d, "Cariri", 480, mx=60) + legenda("Cariri"),
        "mapa-sobral": mapa_svg(d, "Sobral", 480, mx=90) + legenda("Sobral"),
    }


def preencher(caminho, trechos):
    """Troca o miolo de cada <!--gerado:nome-->...<!--/gerado:nome--> do arquivo."""
    with open(caminho, encoding="utf-8") as fh:
        texto = fh.read()
    for nome, conteudo in trechos.items():
        texto, n = re.subn(rf"(<!--gerado:{nome}-->).*?(<!--/gerado:{nome}-->)",
                           lambda m: m.group(1) + conteudo + m.group(2), texto, flags=re.S)
        if not n:
            print(f"aviso: {os.path.basename(caminho)} sem o trecho {nome}")
    escrever(caminho, texto)


def sitemap(caminhos):
    linhas = ['<?xml version="1.0" encoding="UTF-8"?>', '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">']
    linhas += [f"  <url><loc>{SITE}{c}</loc></url>" for c in caminhos]
    return "\n".join(linhas + ["</urlset>", ""])


def carregar_dados(pasta=None):
    with open(REGRAS, encoding="utf-8") as fh:
        regras = json.load(fh)
    indice = carregar_json("indice.json", pasta)
    # A grade comum de cada tipo: a de um dia de operação especial não serve de referência.
    normais = {**indice.get("tipos", {}), **indice.get("normais", {})}
    util = carregar_json(normais["dia-util"]["arquivo"], pasta)
    sabado = carregar_json(normais["sabado"]["arquivo"], pasta) if "sabado" in normais else {"linhas": []}
    return Dados(regras, util, sabado), util, sabado


def main():
    pasta = sys.argv[1] if len(sys.argv) > 1 else None
    d, util, sabado = carregar_dados(pasta)

    caminhos = list(FIXAS) + ["estacoes/", "linhas/", "perguntas-frequentes/", "historia/"]
    escrever(os.path.join(DOCS, "historia", "index.html"),
             pagina(d, caminho="historia/", titulo="História do app | Metrô Ceará",
                    descricao="Como o Metrô Ceará foi feito: as versões do app de horários de metrô e VLT do Ceará, dia a dia, do primeiro dia à Play Store.",
                    corpo="\n".join(pagina_historia())))
    escrever(os.path.join(DOCS, "perguntas-frequentes", "index.html"), pagina_faq(d))
    escrever(os.path.join(DOCS, "estacoes", "index.html"), indice_estacoes(d))
    escrever(os.path.join(DOCS, "linhas", "index.html"), indice_linhas(d))
    for linha in d.linhas:
        c = f"linhas/{slug(linha['nome'])}/"
        escrever(os.path.join(DOCS, c, "index.html"), pagina_linha(d, linha))
        caminhos.append(c)
    for grupo in sorted(d.estacoes, key=lambda g: g["slug"]):
        c = f"estacoes/{grupo['slug']}/"
        escrever(os.path.join(DOCS, c, "index.html"), pagina_estacao(d, grupo))
        caminhos.append(c)
    escrever(os.path.join(DOCS, "sitemap.xml"), sitemap(caminhos))
    preencher(os.path.join(DOCS, "index.html"), trechos_da_inicial(d))
    print(f"{len(d.estacoes)} estações e {len(d.linhas)} linhas em docs/ "
          f"(grade de dia útil de {util.get('data')}, sábado de {sabado.get('data')})")


if __name__ == "__main__":
    main()
