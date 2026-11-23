#!/usr/bin/env python3
"""Confere os links da descrição da loja do jeito que o revisor do Google confere.

A Play rejeitou atualizações duas vezes por "Link da fonte corrompido ou
inacessível" (política de declarações enganosas, ver tools/play/loja.md):

    17/11/2026  www.ce.gov.br manda o certificado sem o intermediário: o
                navegador completa sozinho, o robô não.
    23/11/2026  os links abriam, mas vinham colados em pontuação
                ("…bicicletar.", "…gtfs_file).", que dão 404) e o gtfs_file
                só baixava um .zip.

Por isso, para cada texto em fastlane/metadata/android/*/:

    - todo link precisa abrir com o certificado verificado do jeito normal
      (sem o intermediário de tools/certificados/, de propósito);
    - precisa abrir uma página (HTML), não baixar um arquivo;
    - não pode terminar colado em ponto, vírgula, parêntese etc.;
    - a descrição completa precisa ter pelo menos um link (a fonte oficial).

Roda todo dia dentro do vigia (tools/vigia.py), num servidor do GitHub fora do
Brasil, como o revisor. Para testar antes de colar no Console:

    python3 tools/links_da_loja.py

Sai com 1 se encontrar problema.
"""
import glob
import os
import re
import ssl
import sys
import time
import urllib.error
import urllib.request

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
METADADOS = os.path.join(RAIZ, "fastlane", "metadata", "android")
TEXTOS = ("title.txt", "short_description.txt", "full_description.txt")

LINK = re.compile(r"https?://[^\s<>\"']+")
PONTUACAO_NO_FIM = ".,;:!?)]}"
NAVEGADOR = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
TENTATIVAS = 3  # o site do governo às vezes não responde e volta logo
ESPERA = 20


def textos():
    """(língua, arquivo, conteúdo) de cada texto da loja no repositório."""
    for pasta in sorted(glob.glob(os.path.join(METADADOS, "*"))):
        for nome in TEXTOS:
            caminho = os.path.join(pasta, nome)
            if os.path.exists(caminho):
                with open(caminho, encoding="utf-8") as f:
                    yield os.path.basename(pasta), nome, f.read()


def abrir(url):
    """None se abriu uma página; senão, o motivo em português."""
    ctx = ssl.create_default_context()  # verificação normal, como o robô
    pedido = urllib.request.Request(url, headers={"User-Agent": NAVEGADOR})
    erro = None
    for tentativa in range(TENTATIVAS):
        if tentativa:
            time.sleep(ESPERA)
        try:
            with urllib.request.urlopen(pedido, timeout=30, context=ctx) as r:
                tipo = r.headers.get("Content-Type", "")
                if "html" not in tipo:
                    return f"não abre uma página: baixa um arquivo ({tipo or 'tipo desconhecido'})"
                return None
        except urllib.error.HTTPError as e:
            erro = f"o site respondeu {e.code} ({e.reason})"
            if e.code < 500:
                return erro  # 404, 403: tentar de novo não muda
        except urllib.error.URLError as e:
            motivo = e.reason
            if isinstance(motivo, ssl.SSLCertVerificationError):
                return f"erro de certificado ({motivo.verify_message})"
            erro = f"não conectou ({motivo})"
        except (TimeoutError, OSError) as e:
            erro = f"não conectou ({e})"
    return f"{erro}, em {TENTATIVAS} tentativas"


def verificar():
    """{url ou texto: (título, corpo)} com cada problema encontrado."""
    problemas, testados = {}, {}
    for lingua, nome, texto in textos():
        onde = f"`fastlane/metadata/android/{lingua}/{nome}`"
        achados = LINK.findall(texto)
        if nome == "full_description.txt" and not achados:
            problemas[f"{lingua}/sem-link"] = (
                f"Descrição da loja ({lingua}) sem link de fonte oficial",
                f"{onde} não tem nenhum link. A Play exige a fonte oficial com link funcionando.")
        for url in achados:
            if url[-1] in PONTUACAO_NO_FIM:
                problemas[url] = (
                    f"Link da loja colado em pontuação: {url}",
                    f"Em {onde}, o link termina em “{url[-1]}”. O robô do Google leva o sinal junto "
                    f"e o link quebra. Deixe um espaço depois do link.")
                continue
            if url not in testados:
                testados[url] = abrir(url)
            if testados[url]:
                problemas[url] = (
                    f"Link da descrição da loja não abre: {url}",
                    f"Em {onde}: {testados[url]}.\n\nSe a atualização for para a Play assim, ela pode ser "
                    f"rejeitada por “Link da fonte corrompido ou inacessível”. Troque o link no texto "
                    f"(e no Console) ou espere o site voltar.")
    return problemas, sorted(testados)


def main():
    problemas, testados = verificar()
    for url in testados:
        print(("FALHA " if url in problemas else "ok    ") + url)
    for titulo, corpo in problemas.values():
        print(f"\n{titulo}\n  {corpo}")
    print(f"\n{len(testados)} link(s) testado(s), {len(problemas)} problema(s).")
    return 1 if problemas else 0


if __name__ == "__main__":
    sys.exit(main())
