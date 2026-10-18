"""Genera los banners de llamada a la accion de JPRewards (1280x260).

    python docs/assets/generate_cta.py
    python docs/assets/generate_cta.py --discord-url "discord.gg/tu-invitacion" --docs-url "github.com/tu-usuario/JPRewards#readme"

Salida: discord.png y documentation.png.

Cada banner tiene su propia personalidad (fondo, motivo, forma de la insignia,
boton y color de acento) pero comparten marca, tipografia y estructura, para que
se reconozcan como el mismo producto sin parecer el mismo cartel dos veces.
Los iconos de Discord y GitHub son los oficiales (docs/assets/icons, de
simple-icons); si faltan, el script dibuja un simbolo propio para no fallar.
"""

import argparse
import os
import random
import time

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.dirname(os.path.abspath(__file__))
ICONOS = os.path.join(OUT, "icons")
W, H = 1280, 260

BRAND = "JPRewards"
VERSION = "v2.0.0"

TEXT = (240, 236, 252)
MUTED = (172, 168, 196)
DIM = (128, 122, 152)

BLURPLE = (88, 101, 242)
GITHUB_LIGHT = (240, 246, 252)
GITHUB_DARK = (13, 17, 23)
GITHUB_GREEN = (57, 211, 83)

# Cada tema define el caracter del banner.
TEMAS = {
    "discord": {
        "bg_arriba": (16, 13, 32),
        "bg_abajo": (34, 32, 82),          # tinte blurple
        "halo": (88, 101, 242, 58),
        "patron": "burbujas",
        "insignia": "cuadrado",
        "insignia_color": BLURPLE,
        "secundario": (139, 148, 255),
        "icono": "discord-white.png",
        "glifo": "chat",
        "boton_fondo": BLURPLE,
        "boton_texto": (255, 255, 255),
        "acento": BLURPLE,
    },
    "docs": {
        "bg_arriba": (9, 11, 17),
        "bg_abajo": (21, 26, 35),          # tinte slate de GitHub
        "halo": (255, 255, 255, 34),
        "patron": "commits",
        "insignia": "circulo",
        "insignia_color": GITHUB_LIGHT,
        "secundario": (110, 118, 129),
        "icono": "github-dark.png",
        "glifo": "book",
        "boton_fondo": GITHUB_LIGHT,
        "boton_texto": GITHUB_DARK,
        "acento": GITHUB_GREEN,
    },
}

BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
SEMI = r"C:\Windows\Fonts\seguisb.ttf"
REGULAR = r"C:\Windows\Fonts\segoeui.ttf"
_cache = {}


def guardar(imagen, destino):
    """Guarda con reintentos: en Windows (carpetas sincronizadas, antivirus) el
    archivo destino puede estar bloqueado un instante."""
    temporal = destino + ".tmp.png"
    imagen.convert("RGB").save(temporal, format="PNG")
    for intento in range(6):
        try:
            os.replace(temporal, destino)
            return
        except OSError:
            time.sleep(0.4)
    raise OSError("no se pudo guardar " + destino)


def font(path, size):
    key = (path, size)
    if key not in _cache:
        try:
            _cache[key] = ImageFont.truetype(path, size)
        except OSError:
            _cache[key] = ImageFont.load_default()
    return _cache[key]


def mezclar(uno, otro, cantidad):
    return tuple(round(uno[i] + (otro[i] - uno[i]) * cantidad) for i in range(3))


def marca(nombre, tamano):
    """Icono oficial desde docs/assets/icons (o None si no esta)."""
    ruta = os.path.join(ICONOS, nombre)
    if not os.path.exists(ruta):
        return None
    try:
        return Image.open(ruta).convert("RGBA").resize((tamano, tamano), Image.LANCZOS)
    except Exception:
        return None


# ------------------------------------------------------------------ FONDOS
def fondo(tema):
    """Mismo fondo que el resto del arte: degradado, aurora, rayos, bokeh,
    arcos y viñeta. Sin grano."""
    arriba, abajo = tema["bg_arriba"], tema["bg_abajo"]
    acento = tema["acento"]
    secundario = tema.get("secundario", acento)
    suave = mezclar(acento, (255, 255, 255), 0.45)
    profundo = mezclar(acento, (76, 29, 149), 0.55)
    medio = mezclar(mezclar(arriba, abajo, 0.5), acento, 0.12)

    imagen = Image.new("RGB", (W, H), arriba)
    dibujo = ImageDraw.Draw(imagen)
    for y in range(H):
        proporcion = y / float(H - 1)
        if proporcion < 0.5:
            color = mezclar(arriba, medio, proporcion / 0.5)
        else:
            color = mezclar(medio, abajo, (proporcion - 0.5) / 0.5)
        dibujo.line([(0, y), (W, y)], fill=color)
    imagen = imagen.convert("RGBA")

    aurora = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(aurora)
    pincel.ellipse([-W * 0.26, -H * 0.55, W * 0.42, H * 0.36], fill=acento + (52,))
    pincel.ellipse([W * 0.34, -H * 0.30, W * 0.92, H * 0.22], fill=profundo + (44,))
    pincel.ellipse([W * 0.55, H * 0.52, W * 1.22, H * 1.30], fill=secundario + (22,))
    pincel.ellipse([-W * 0.18, H * 0.46, W * 0.40, H * 1.15], fill=suave + (18,))
    imagen = Image.alpha_composite(imagen, aurora.filter(ImageFilter.GaussianBlur(150)))

    rayos = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(rayos)
    for i in range(6):
        x = W * (0.06 + i * 0.16)
        pincel.polygon([(x, -H * 0.1), (x + W * 0.045, -H * 0.1),
                        (x + W * 0.045 + H * 0.42, H * 1.05), (x + H * 0.42, H * 1.05)], fill=suave + (10,))
    imagen = Image.alpha_composite(imagen, rayos.filter(ImageFilter.GaussianBlur(70)))

    azar = random.Random(20260218)
    bokeh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(bokeh)
    for i in range(40):
        x = azar.uniform(0, W)
        y = azar.uniform(0, H)
        radio = azar.uniform(2.5, 16.0)
        pincel.ellipse([x - radio, y - radio, x + radio, y + radio],
                       fill=azar.choice((acento, suave, secundario, profundo)) + (int(azar.uniform(10, 32)),))
    imagen = Image.alpha_composite(imagen, bokeh.filter(ImageFilter.GaussianBlur(2.0)))

    arcos = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(arcos)
    for radio in (120, 210, 300):
        pincel.arc([W - radio - 40, -radio + 60, W + radio - 40, radio + 60], 120, 330, fill=suave + (20,), width=2)
    imagen = Image.alpha_composite(imagen, arcos)

    mascara = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mascara).ellipse([-W * 0.30, -H * 0.46, W * 1.30, H * 1.46], fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(140))
    sombra = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sombra.putalpha(mascara.point(lambda valor: int((255 - valor) * 0.32)))
    imagen = Image.alpha_composite(imagen, sombra)

    dibujo = ImageDraw.Draw(imagen)
    dibujo.line([(0, 0), (W, 0)], fill=suave + (80,))
    return imagen


def patron(imagen, tema):
    capa = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    t = ImageDraw.Draw(capa)
    if tema["patron"] == "burbujas":
        # Lineas diagonales suaves + burbujas de chat flotando.
        for x in range(-H, W, 46):
            t.line([(x, H), (x + H, 0)], fill=(255, 255, 255, 7), width=1)
        burbujas = [(190, 40, 92, 34), (300, 190, 74, 26), (520, 30, 64, 24), (700, 200, 86, 30),
                    (900, 36, 70, 26), (1080, 196, 96, 34), (1210, 60, 60, 22)]
        for x, y, ancho, alto in burbujas:
            t.rounded_rectangle([x, y, x + ancho, y + alto], radius=alto // 2, fill=(88, 101, 242, 26))
            t.polygon([(x + ancho * 0.22, y + alto - 2), (x + ancho * 0.36, y + alto - 2),
                       (x + ancho * 0.2, y + alto + 9)], fill=(88, 101, 242, 26))
    else:
        # Rejilla de contribuciones de GitHub (todo el ancho) + codigo en las esquinas.
        for fila in range(6):
            for columna in range(56):
                x = 12 + columna * 22
                y = 14 + fila * 22
                if x + 15 > W - 12:
                    continue
                intensidad = 12 + ((fila * 7 + columna * 5) % 5) * 6
                color = GITHUB_GREEN if (fila + columna) % 3 == 0 else (255, 255, 255)
                t.rounded_rectangle([x, y, x + 15, y + 15], radius=3, fill=color + (intensidad,))
        t.text((1082, 198), "< / >", font=font(BOLD, 44), fill=(255, 255, 255, 18))
        t.text((18, 190), "{ }", font=font(BOLD, 40), fill=(255, 255, 255, 16))
    return Image.alpha_composite(imagen, capa)


def glyph(dibujo, nombre, cx, cy, s, color):
    """Simbolo propio, solo si falta el icono oficial."""
    w = max(3, int(s * 0.12))
    if nombre == "chat":
        dibujo.rounded_rectangle([cx - s, cy - s * 0.85, cx + s, cy + s * 0.3], radius=int(s * 0.38), fill=color)
        dibujo.polygon([(cx - s * 0.5, cy + s * 0.2), (cx - s * 0.05, cy + s * 0.2), (cx - s * 0.62, cy + s * 0.95)],
                       fill=color)
        for i in range(3):
            x = cx - s * 0.42 + i * s * 0.42
            dibujo.ellipse([x - w * 0.6, cy - s * 0.35 - w * 0.6, x + w * 0.6, cy - s * 0.35 + w * 0.6],
                           fill=(255, 255, 255, 255))
    elif nombre == "book":
        dibujo.line([cx, cy - s * 0.75, cx, cy + s * 0.8], fill=color, width=w)
        dibujo.polygon([(cx - s * 0.05, cy - s * 0.75), (cx - s * 0.95, cy - s * 0.95), (cx - s * 0.95, cy + s * 0.6),
                        (cx - s * 0.05, cy + s * 0.8)], outline=color, width=w)
        dibujo.polygon([(cx + s * 0.05, cy - s * 0.75), (cx + s * 0.95, cy - s * 0.95), (cx + s * 0.95, cy + s * 0.6),
                        (cx + s * 0.05, cy + s * 0.8)], outline=color, width=w)


def boton(dibujo, derecha, cy, texto, fondo, color_texto):
    fuente = font(BOLD, 22)
    ancho = dibujo.textlength(texto, font=fuente) + 76
    x0 = derecha - ancho
    dibujo.rounded_rectangle([x0, cy - 30, derecha, cy + 30], radius=30, fill=fondo)
    dibujo.text((x0 + 38, cy - 14), texto, font=fuente, fill=color_texto)
    return x0


def banner(nombre, tema, titulo, subtitulo, url, etiqueta):
    imagen = patron(fondo(tema), tema)
    dibujo = ImageDraw.Draw(imagen)

    x0, y0, lado = 86, 74, 112
    if tema["insignia"] == "circulo":
        dibujo.ellipse([x0, y0, x0 + lado, y0 + lado], fill=tema["insignia_color"])
    else:
        dibujo.rounded_rectangle([x0, y0, x0 + lado, y0 + lado], radius=26, fill=tema["insignia_color"])

    png = marca(tema["icono"], 64)
    if png is not None:
        imagen.alpha_composite(png, (x0 + (lado - 64) // 2, y0 + (lado - 64) // 2))
        dibujo = ImageDraw.Draw(imagen)
    else:
        glyph(dibujo, tema["glifo"], x0 + lado // 2, y0 + lado // 2, 30, (255, 255, 255, 255))

    x_texto = x0 + lado + 34
    dibujo.text((x_texto, y0 + 4), titulo, font=font(BOLD, 40), fill=TEXT)
    dibujo.text((x_texto, y0 + 58), subtitulo, font=font(REGULAR, 22), fill=MUTED)
    if url:
        dibujo.text((x_texto, y0 + 90), url, font=font(SEMI, 21), fill=tema["acento"])

    boton(dibujo, W - 86, y0 + 56, etiqueta, tema["boton_fondo"], tema["boton_texto"])

    dibujo.rounded_rectangle([86, 226, 150, 232], radius=3, fill=tema["acento"])
    dibujo.text((166, 218), BRAND + "  \u00b7  " + VERSION, font=font(REGULAR, 18), fill=DIM)

    guardar(imagen, os.path.join(OUT, nombre))
    return imagen.size


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Genera los banners de llamada a la accion")
    parser.add_argument("--discord-url", default="", help="Invitacion que se imprime en el banner de Discord")
    parser.add_argument("--docs-url", default="", help="URL que se imprime en el banner de documentacion")
    args = parser.parse_args()

    print("discord.png       %dx%d" % banner(
        "discord.png",
        TEMAS["discord"],
        "Join the community",
        "Support, bug reports, feature requests and release updates.",
        args.discord_url,
        "JOIN THE DISCORD"))
    print("documentation.png %dx%d" % banner(
        "documentation.png",
        TEMAS["docs"],
        "Documentation, guides and FAQ",
        "Setup, configuration, commands, permissions and changelog.",
        args.docs_url,
        "OPEN THE DOCS"))
