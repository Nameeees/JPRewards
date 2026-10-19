"""Genera el banner y el icono de JPRewards para SpigotMC y el README.

    python docs/assets/generate_banner_icon.py

Salida (determinista: el mismo texto produce el mismo PNG):
    banner.png  1280x420
    icon.png    512x512

Para reusar la plantilla en otro plugin, edita solo el bloque BRAND.
"""

import math
import os
import random
import time

from PIL import Image, ImageDraw, ImageFilter, ImageFont

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


# ------------------------------------------------------------------ BRAND
BRAND = "JPRewards"
TAGLINE = "Rewards that bring players back every day"
HIGHLIGHTS = "One jar  \u00b7  No dependencies  \u00b7  English + Spanish  \u00b7  YAML or MySQL"
FOOTLINE = "v2.1.0  \u00b7  Minecraft 1.8 \u2192 latest  \u00b7  MIT  \u00b7  Spigot & Paper"

OUT = os.path.dirname(os.path.abspath(__file__))
os.makedirs(OUT, exist_ok=True)

# Paleta de la marca: la misma de los menus del plugin (&d y &5)
ACCENT = (217, 70, 239)
ACCENT_DEEP = (126, 34, 206)
ACCENT_SOFT = (240, 171, 252)
GOLD = (250, 204, 21)
BG_TOP = (13, 11, 24)
BG_BOTTOM = (30, 21, 56)
TEXT = (240, 236, 252)
MUTED = (170, 160, 194)
DIM = (126, 116, 150)
BOX = (46, 35, 78)
BOX_LID = (58, 44, 96)
BOX_EDGE = (98, 78, 148)

BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
SEMI = r"C:\Windows\Fonts\seguisb.ttf"
REGULAR = r"C:\Windows\Fonts\segoeui.ttf"
_cache = {}


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


def fondo(ancho, alto, arriba, abajo, acento, secundario):
    """Fondo de la marca: degradado, aurora, rayos de luz, bokeh, arcos y
    viñeta. El mismo tratamiento que las infografias."""
    suave = mezclar(acento, (255, 255, 255), 0.45)
    profundo = mezclar(acento, (76, 29, 149), 0.55)
    medio = mezclar(mezclar(arriba, abajo, 0.5), acento, 0.12)

    imagen = Image.new("RGB", (ancho, alto), arriba)
    dibujo = ImageDraw.Draw(imagen)
    for y in range(alto):
        proporcion = y / float(max(1, alto - 1))
        if proporcion < 0.5:
            color = mezclar(arriba, medio, proporcion / 0.5)
        else:
            color = mezclar(medio, abajo, (proporcion - 0.5) / 0.5)
        dibujo.line([(0, y), (ancho, y)], fill=color)
    imagen = imagen.convert("RGBA")

    textura = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(textura)
    for x in range(-alto, ancho, 54):
        pincel.line([(x, alto), (x + alto, 0)], fill=(255, 255, 255, 6), width=1)
    imagen = Image.alpha_composite(imagen, textura)

    aurora = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(aurora)
    pincel.ellipse([-ancho * 0.26, -alto * 0.55, ancho * 0.42, alto * 0.36], fill=acento + (52,))
    pincel.ellipse([ancho * 0.34, -alto * 0.30, ancho * 0.92, alto * 0.22], fill=profundo + (44,))
    pincel.ellipse([ancho * 0.55, alto * 0.52, ancho * 1.22, alto * 1.30], fill=secundario + (22,))
    pincel.ellipse([-ancho * 0.18, alto * 0.46, ancho * 0.40, alto * 1.15], fill=suave + (18,))
    imagen = Image.alpha_composite(imagen, aurora.filter(ImageFilter.GaussianBlur(150)))

    rayos = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(rayos)
    for i in range(6):
        x = ancho * (0.06 + i * 0.16)
        pincel.polygon([(x, -alto * 0.1), (x + ancho * 0.045, -alto * 0.1),
                        (x + ancho * 0.045 + alto * 0.42, alto * 1.05), (x + alto * 0.42, alto * 1.05)],
                       fill=suave + (10,))
    imagen = Image.alpha_composite(imagen, rayos.filter(ImageFilter.GaussianBlur(70)))

    azar = random.Random(20260218)
    bokeh = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(bokeh)
    for i in range(46):
        x = azar.uniform(0, ancho)
        y = azar.uniform(0, alto)
        radio = azar.uniform(2.5, 18.0)
        pincel.ellipse([x - radio, y - radio, x + radio, y + radio],
                       fill=azar.choice((acento, suave, secundario, profundo)) + (int(azar.uniform(10, 32)),))
    imagen = Image.alpha_composite(imagen, bokeh.filter(ImageFilter.GaussianBlur(2.0)))

    arcos = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(arcos)
    for radio in (140, 240, 340):
        pincel.arc([ancho - radio - 50, -radio + 70, ancho + radio - 50, radio + 70], 120, 330,
                   fill=suave + (20,), width=2)
        pincel.arc([-radio + 30, alto - radio - 30, radio + 30, alto + radio - 30], 300, 480,
                   fill=acento + (16,), width=2)
    imagen = Image.alpha_composite(imagen, arcos)

    mascara = Image.new("L", (ancho, alto), 0)
    ImageDraw.Draw(mascara).ellipse([-ancho * 0.30, -alto * 0.46, ancho * 1.30, alto * 1.46], fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(140))
    sombra = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    sombra.putalpha(mascara.point(lambda valor: int((255 - valor) * 0.32)))
    imagen = Image.alpha_composite(imagen, sombra)

    dibujo = ImageDraw.Draw(imagen)
    dibujo.line([(0, 0), (ancho, 0)], fill=suave + (80,))
    return imagen


def soft_glow(image, cx, cy, radius, color, alpha=120, blur=60):
    """Halo suave de verdad: elipse + desenfoque gaussiano (sin bordes duros)."""
    capa = Image.new("RGBA", image.size, (0, 0, 0, 0))
    dibujo = ImageDraw.Draw(capa)
    dibujo.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                   fill=(color[0], color[1], color[2], alpha))
    capa = capa.filter(ImageFilter.GaussianBlur(blur))
    return Image.alpha_composite(image, capa)


def rounded(draw, box, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def star(draw, cx, cy, size, color, puntiaguda=True):
    """Estrella de cuatro puntas (destello) o de cinco (insignia)."""
    if not puntiaguda:
        coords = []
        for i in range(10):
            angulo = math.pi / 5 * i - math.pi / 2
            radio = size if i % 2 == 0 else size * 0.42
            coords.append((cx + radio * math.cos(angulo), cy + radio * math.sin(angulo)))
        draw.polygon(coords, fill=color)
        return
    largo, corto = size, max(2, int(size * 0.26))
    coords = []
    for i in range(8):
        angulo = math.pi / 4 * i - math.pi / 2
        radio = largo if i % 2 == 0 else corto
        coords.append((cx + radio * math.cos(angulo), cy + radio * math.sin(angulo)))
    draw.polygon(coords, fill=color)


def gift_icon(image, cx, cy, escala=1.0):
    """Simbolo de JPRewards: caja de regalo con estrella dorada y destellos."""
    e = escala
    image = soft_glow(image, cx, cy, int(210 * e), ACCENT, alpha=110, blur=70)
    capa = Image.new("RGBA", image.size, (0, 0, 0, 0))
    dibujo = ImageDraw.Draw(capa)

    def caja(x0, y0, x1, y1, radio, relleno, borde=None, grosor=2):
        rounded(dibujo, [cx + x0 * e, cy + y0 * e, cx + x1 * e, cy + y1 * e],
                int(radio * e), relleno, borde, max(1, int(grosor * e)))

    # cuerpo y tapa
    caja(-152, -18, 152, 182, 28, BOX, BOX_EDGE)
    caja(-176, -104, 176, -10, 24, BOX_LID, (118, 94, 176))
    # brillo superior de la tapa (color solido: dibujar con alfa no mezcla)
    dibujo.rounded_rectangle([cx - 168 * e, cy - 100 * e, cx + 168 * e, cy - 93 * e],
                             radius=int(4 * e), fill=(132, 104, 194))
    # cintas (cruz)
    caja(-28, -104, 28, 182, 10, ACCENT, ACCENT_SOFT, 2)
    caja(-176, -84, 176, -32, 10, ACCENT, ACCENT_SOFT, 2)
    # destellos
    star(dibujo, cx + 208 * e, cy - 120 * e, 30 * e, ACCENT_SOFT)
    star(dibujo, cx - 214 * e, cy - 52 * e, 20 * e, ACCENT_SOFT)
    star(dibujo, cx + 168 * e, cy + 138 * e, 15 * e, ACCENT_SOFT)

    resultado = Image.alpha_composite(image, capa)

    # estrella dorada flotando sobre la caja (con su propio halo)
    halo = Image.new("RGBA", resultado.size, (0, 0, 0, 0))
    star(ImageDraw.Draw(halo), cx, cy - 206 * e, 74 * e, (250, 204, 21, 90), puntiaguda=False)
    resultado = Image.alpha_composite(resultado, halo.filter(ImageFilter.GaussianBlur(18 * e)))
    estrella = Image.new("RGBA", resultado.size, (0, 0, 0, 0))
    estrella_dibujo = ImageDraw.Draw(estrella)
    star(estrella_dibujo, cx, cy - 206 * e, 60 * e, (146, 108, 12), puntiaguda=False)
    star(estrella_dibujo, cx, cy - 206 * e, 54 * e, GOLD, puntiaguda=False)
    return Image.alpha_composite(resultado, estrella)


def make_icon():
    lado = 512
    imagen = fondo(lado, lado, BG_TOP, BG_BOTTOM, ACCENT, GOLD)
    imagen = gift_icon(imagen, lado // 2, int(lado * 0.54), escala=0.84)
    guardar(imagen, os.path.join(OUT, "icon.png"))
    return imagen.convert("RGB")


def make_banner():
    ancho, alto = 1280, 420
    imagen = fondo(ancho, alto, BG_TOP, BG_BOTTOM, ACCENT, GOLD)
    imagen = gift_icon(imagen, 1030, 216, escala=0.66)
    dibujo = ImageDraw.Draw(imagen)

    rounded(dibujo, [86, 72, 190, 82], 5, ACCENT)
    dibujo.text((86, 100), BRAND, font=font(BOLD, 80), fill=TEXT)
    dibujo.text((88, 206), TAGLINE, font=font(REGULAR, 30), fill=MUTED)
    dibujo.text((88, 258), HIGHLIGHTS, font=font(SEMI, 25), fill=ACCENT_SOFT)
    dibujo.text((88, 334), FOOTLINE, font=font(REGULAR, 20), fill=DIM)

    guardar(imagen, os.path.join(OUT, "banner.png"))
    return imagen.convert("RGB")


if __name__ == "__main__":
    icono = make_icon()
    banner = make_banner()
    print("icon.png   %dx%d" % icono.size)
    print("banner.png %dx%d" % banner.size)
