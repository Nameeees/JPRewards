#!/usr/bin/env python3
"""Informa de literales de texto que sigan pareciendo espanoles (no toca nada)."""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
JAVA = RAIZ / "src" / "main" / "java"
LITERAL = re.compile(r'"(?:[^"\\]|\\.)*"')
SOSPECHOSAS = (" no se ", " se usa", " revisa", " archivo", " jugador", " recompensa", " idioma",
               " configuracion", " carpeta", " omite", " ciclo", " valido", " existe", " falta",
               " entrega", " claves", " dias", " semanas", " version nueva", " otra vez")
IGNORAR = ("select ", "insert ", "update ", "delete ", "create table", "where ")


def main():
    restos = []
    for archivo in sorted(JAVA.rglob("*.java")):
        texto = archivo.read_text(encoding="utf-8")
        for m in LITERAL.finditer(texto):
            contenido = m.group(0)
            minusculas = contenido.lower()
            if any(i in minusculas for i in IGNORAR):
                continue
            if any(s in minusculas for s in SOSPECHOSAS):
                restos.append("%s: %s" % (archivo.name, contenido[:130]))
    print("literales posiblemente en espanol:", len(restos))
    for r in restos:
        print("  ", r)
    return 0


if __name__ == "__main__":
    sys.exit(main())
