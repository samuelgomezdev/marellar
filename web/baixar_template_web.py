"""Baixa so o template web do Godot de dentro do .tpz (1,3 GB), lendo o zip por HTTP Range.

Uso: python baixar_template_web.py 4.7.2 <pasta_de_templates>
Grava web_nothreads_release.zip e web_nothreads_debug.zip em <pasta>/<versao>.stable/.
"""
import io
import os
import sys
import urllib.request
import zipfile


class ArquivoHttp(io.RawIOBase):
    """Arquivo somente leitura sobre HTTP, com seek (Range)."""

    def __init__(self, url):
        self.url = url
        self.pos = 0
        pedido = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(pedido) as r:
            self.url = r.geturl()
            self.tamanho = int(r.headers["Content-Length"])

    def seekable(self):
        return True

    def readable(self):
        return True

    def seek(self, deslocamento, origem=0):
        base = {0: 0, 1: self.pos, 2: self.tamanho}[origem]
        self.pos = base + deslocamento
        return self.pos

    def tell(self):
        return self.pos

    def read(self, n=-1):
        if n is None or n < 0:
            n = self.tamanho - self.pos
        if n == 0 or self.pos >= self.tamanho:
            return b""
        fim = min(self.pos + n, self.tamanho) - 1
        pedido = urllib.request.Request(self.url, headers={"Range": f"bytes={self.pos}-{fim}"})
        with urllib.request.urlopen(pedido) as r:
            dados = r.read()
        self.pos += len(dados)
        return dados

    def readinto(self, b):
        dados = self.read(len(b))
        b[: len(dados)] = dados
        return len(dados)


def main():
    versao, pasta = sys.argv[1], sys.argv[2]
    url = (f"https://github.com/godotengine/godot-builds/releases/download/{versao}-stable/"
           f"Godot_v{versao}-stable_export_templates.tpz")
    destino = os.path.join(pasta, f"{versao}.stable")
    os.makedirs(destino, exist_ok=True)
    with zipfile.ZipFile(io.BufferedReader(ArquivoHttp(url), buffer_size=1 << 20)) as tpz:
        for nome in ("templates/web_nothreads_release.zip", "templates/web_nothreads_debug.zip",
                     "templates/version.txt"):
            alvo = os.path.join(destino, os.path.basename(nome))
            with tpz.open(nome) as origem, open(alvo, "wb") as saida:
                saida.write(origem.read())
            print(f"{alvo} ({os.path.getsize(alvo) // 1024} KB)")


if __name__ == "__main__":
    main()
