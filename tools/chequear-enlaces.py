#!/usr/bin/env python3
"""Comprueba que los enlaces relativos de la documentacion existan.

    python tools/chequear-enlaces.py

Recorre todos los .md del repositorio, resuelve los enlaces relativos
(ignorando http, mailto y anclas) y avisa de los que apunten a un archivo que
no esta. Sale con codigo 1 si encuentra alguno, para poder usarlo en CI.
"""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
ENLACE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
IGNORAR = ("http://", "https://", "mailto:", "#", "tel:", "{{", "{%")


def main():
    rotos = []
    revisados = 0
    for archivo in sorted(RAIZ.rglob("*.md")):
        if any(parte in (".git", "target", "build", ".test-servers", "node_modules") for parte in archivo.parts):
            continue
        texto = archivo.read_text(encoding="utf-8", errors="replace")
        for destino in ENLACE.findall(texto):
            destino = destino.split(" ")[0].strip()
            if not destino or destino.startswith(IGNORAR):
                continue
            ruta = destino.split("#")[0]
            if not ruta:
                continue
            revisados += 1
            objetivo = (archivo.parent / ruta).resolve()
            if not objetivo.exists():
                rotos.append("%s -> %s" % (archivo.relative_to(RAIZ), destino))
    print("enlaces relativos revisados:", revisados)
    print("enlaces rotos:", len(rotos))
    for roto in rotos:
        print("  ", roto)
    return 1 if rotos else 0


if __name__ == "__main__":
    sys.exit(main())
