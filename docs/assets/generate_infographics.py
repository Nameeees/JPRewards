"""Genera las infografias de SpigotMC de JPRewards (1280x720).

    python docs/assets/generate_infographics.py

Salida (determinista):
    features.png  commands.png  permissions.png  setup.png
    configuration.png  diagnostics.png  compatibility.png

Para reusar la plantilla en otro plugin: edita FOOTLINE y el diccionario IMAGENES.
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
FOOTLINE = "JPRewards  \u00b7  v2.1.0  \u00b7  MIT  \u00b7  Spigot & Paper"
SERIES = "1.8 \u2192 latest"

OUT = os.path.dirname(os.path.abspath(__file__))
os.makedirs(OUT, exist_ok=True)

W, H = 1280, 720
ACCENT = (217, 70, 239)
ACCENT_SOFT = (240, 171, 252)
GOLD = (250, 204, 21)
TEXT = (240, 236, 252)
MUTED = (170, 160, 194)
DIM = (126, 116, 150)
CARD = (28, 22, 48)
CARD_BORDER = (60, 47, 92)
ICON_BG = (40, 30, 68)
BG_TOP = (13, 11, 24)
BG_BOTTOM = (26, 19, 48)

# Cada infografia mantiene la paleta morada de la marca, pero cambia de TONO y,
# sobre todo, de COMPOSICION: no son seis bandejas iguales con otro color.
TEMAS = {
    "features": {
        "acento": (217, 70, 239),          # fucsia de la marca
        "secundario": (250, 204, 21),      # dorado del premio
        "bg_arriba": (13, 11, 24),
        "bg_abajo": (30, 20, 52),
        "patron": "destellos",
        "diseno": "rejilla",
    },
    "commands": {
        "acento": (168, 85, 247),          # violeta
        "secundario": (240, 171, 252),
        "bg_arriba": (12, 10, 26),
        "bg_abajo": (28, 20, 54),
        "patron": "terminal",
        "diseno": "consola",
    },
    "permissions": {
        "acento": (139, 92, 246),          # violeta profundo
        "secundario": (250, 204, 21),      # el dorado marca los rangos
        "bg_arriba": (14, 11, 26),
        "bg_abajo": (32, 22, 56),
        "patron": "llaves",
        "diseno": "tabla",
    },
    "setup": {
        "acento": (192, 132, 252),         # lavanda
        "secundario": (240, 171, 252),
        "bg_arriba": (13, 11, 26),
        "bg_abajo": (28, 20, 50),
        "patron": "checks",
        "diseno": "linea",
    },
    "configuration": {
        "acento": (192, 38, 211),          # magenta profundo
        "secundario": (240, 171, 252),
        "bg_arriba": (12, 10, 24),
        "bg_abajo": (30, 18, 50),
        "patron": "yaml",
        "diseno": "arbol",
    },
    "diagnostics": {
        "acento": (232, 121, 249),         # orquidea
        "secundario": (250, 204, 21),
        "bg_arriba": (14, 11, 26),
        "bg_abajo": (32, 20, 52),
        "patron": "radar",
        "diseno": "informe",
    },
    "compatibility": {
        "acento": (124, 58, 237),          # violeta indigo
        "secundario": (196, 181, 253),
        "bg_arriba": (12, 10, 24),
        "bg_abajo": (26, 20, 52),
        "patron": "orbitas",
        "diseno": "versiones",
    },
    "integrations": {
        "acento": (190, 24, 93),           # rosa profundo (la puerta a otros plugins)
        "secundario": (250, 204, 21),
        "bg_arriba": (14, 10, 22),
        "bg_abajo": (34, 18, 46),
        "patron": "destellos",
        "diseno": "codigo",
    },
}


def mezclar(uno, otro, cantidad):
    """Mezcla dos colores RGB (0 = uno, 1 = otro)."""
    return tuple(round(uno[i] + (otro[i] - uno[i]) * cantidad) for i in range(3))


def suavizar(color, cantidad=0.45):
    return mezclar(color, (255, 255, 255), cantidad)

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


def fondo(tema, ancho=W, alto=H):
    """Fondo comun de toda la familia: degradado, aurora de varios halos, rayos
    de luz, bokeh, arcos geometricos y viñeta. Sin grano: el detalle viene de
    las capas de luz, no del ruido."""
    arriba, abajo = tema["bg_arriba"], tema["bg_abajo"]
    acento = tema["acento"]
    secundario = tema.get("secundario", acento)
    suave = mezclar(acento, (255, 255, 255), 0.45)
    profundo = mezclar(acento, (76, 29, 149), 0.55)
    medio = mezclar(mezclar(arriba, abajo, 0.5), acento, 0.12)

    # 1) Degradado de tres tramos.
    imagen = Image.new("RGB", (ancho, alto), arriba)
    dibujo = ImageDraw.Draw(imagen)
    for y in range(alto):
        proporcion = y / float(alto - 1)
        if proporcion < 0.5:
            color = mezclar(arriba, medio, proporcion / 0.5)
        else:
            color = mezclar(medio, abajo, (proporcion - 0.5) / 0.5)
        dibujo.line([(0, y), (ancho, y)], fill=color)
    imagen = imagen.convert("RGBA")

    # 2) Aurora: cuatro masas de luz de distinto tono, muy desenfocadas.
    aurora = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(aurora)
    pincel.ellipse([-ancho * 0.26, -alto * 0.55, ancho * 0.42, alto * 0.36], fill=acento + (52,))
    pincel.ellipse([ancho * 0.34, -alto * 0.30, ancho * 0.92, alto * 0.22], fill=profundo + (44,))
    pincel.ellipse([ancho * 0.55, alto * 0.52, ancho * 1.22, alto * 1.30], fill=secundario + (22,))
    pincel.ellipse([-ancho * 0.18, alto * 0.46, ancho * 0.40, alto * 1.15], fill=suave + (18,))
    imagen = Image.alpha_composite(imagen, aurora.filter(ImageFilter.GaussianBlur(165)))

    # 3) Rayos de luz: haces diagonales muy tenues que entran por arriba.
    rayos = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(rayos)
    for i in range(6):
        x = ancho * (0.06 + i * 0.16)
        ancho_haz = ancho * 0.045
        pincel.polygon([(x, -alto * 0.1), (x + ancho_haz, -alto * 0.1),
                        (x + ancho_haz + alto * 0.42, alto * 1.05), (x + alto * 0.42, alto * 1.05)],
                       fill=suave + (10,))
    imagen = Image.alpha_composite(imagen, rayos.filter(ImageFilter.GaussianBlur(70)))

    # 4) Bokeh: puntos de luz de distintos tamaños (unos nitidos, otros no).
    azar = random.Random(20260218)
    bokeh = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(bokeh)
    for i in range(54):
        x = azar.uniform(0, ancho)
        y = azar.uniform(0, alto)
        radio = azar.uniform(2.5, 21.0)
        alfa = int(azar.uniform(10, 34))
        color = azar.choice((acento, suave, secundario, profundo))
        pincel.ellipse([x - radio, y - radio, x + radio, y + radio], fill=color + (alfa,))
    imagen = Image.alpha_composite(imagen, bokeh.filter(ImageFilter.GaussianBlur(2.2)))

    # 5) Detalle geometrico: arcos concentricos en dos esquinas.
    arcos = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(arcos)
    for radio in (150, 250, 350, 470):
        pincel.arc([ancho - radio - 60, -radio + 90, ancho + radio - 60, radio + 90],
                   120, 330, fill=suave + (20,), width=2)
        pincel.arc([-radio + 40, alto - radio - 40, radio + 40, alto + radio - 40],
                   300, 480, fill=acento + (16,), width=2)
    imagen = Image.alpha_composite(imagen, arcos)

    # 6) Viñeta suave: centra la mirada sin apagar el fondo.
    mascara = Image.new("L", (ancho, alto), 0)
    ImageDraw.Draw(mascara).ellipse([-ancho * 0.30, -alto * 0.46, ancho * 1.30, alto * 1.46], fill=255)
    mascara = mascara.filter(ImageFilter.GaussianBlur(150))
    sombra = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    sombra.putalpha(mascara.point(lambda valor: int((255 - valor) * 0.34)))
    imagen = Image.alpha_composite(imagen, sombra)

    # 7) Filo de luz de 1 px, para separar la imagen del fondo del foro.
    dibujo = ImageDraw.Draw(imagen)
    dibujo.line([(0, 0), (ancho, 0)], fill=suave + (80,))
    return imagen


def base(title, subtitle, tema):
    imagen = fondo(tema)

    # El motivo de fondo es lo que le da caracter a cada infografia.
    capa = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    PATRONES[tema["patron"]](capa, tema["acento"])
    imagen = Image.alpha_composite(imagen, capa.filter(ImageFilter.GaussianBlur(0.7)))

    dibujo = ImageDraw.Draw(imagen)
    dibujo.rounded_rectangle([86, 62, 190, 72], radius=5, fill=tema["acento"])
    dibujo.text((86, 92), title, font=font(BOLD, 58), fill=TEXT)
    dibujo.text((88, 172), subtitle, font=font(REGULAR, 25), fill=MUTED)
    return imagen


def footer(imagen, tema, right=SERIES):
    dibujo = ImageDraw.Draw(imagen)
    dibujo.text((86, 672), FOOTLINE, font=font(REGULAR, 19), fill=DIM)
    ancho = dibujo.textlength(right, font=font(SEMI, 19))
    dibujo.text((W - 86 - ancho, 672), right, font=font(SEMI, 19), fill=suavizar(tema["acento"]))


# ------------------------------------------------------------------ motivos
def destellos(capa, acento):
    """Chispas: el punto de "premio" de la marca."""
    t = ImageDraw.Draw(capa)
    puntos = [(150, 120, 13), (330, 300, 9), (520, 90, 11), (700, 330, 8), (900, 130, 12),
              (1080, 320, 9), (1210, 150, 10), (240, 520, 8), (640, 560, 10), (1010, 520, 9),
              (420, 620, 7), (1180, 600, 8)]
    for x, y, s in puntos:
        t.polygon([(x, y - s), (x + s * 0.28, y - s * 0.28), (x + s, y), (x + s * 0.28, y + s * 0.28),
                   (x, y + s), (x - s * 0.28, y + s * 0.28), (x - s, y), (x - s * 0.28, y - s * 0.28)],
                  fill=acento + (30,))
    for x, y in ((210, 210), (470, 430), (830, 250), (1150, 430), (560, 660), (940, 640)):
        t.ellipse([x - 3, y - 3, x + 3, y + 3], fill=acento + (44,))


def terminal(capa, acento):
    """Lineas de consola: el banner de los comandos."""
    t = ImageDraw.Draw(capa)
    for fila in range(9):
        y = 96 + fila * 62
        ancho = 150 + (fila * 97) % 420
        t.rounded_rectangle([120, y, 120 + ancho, y + 12], radius=6, fill=acento + (18,))
        t.text((72, y - 14), ">", font=font(BOLD, 30), fill=acento + (40,))
    t.text((1080, 560), "$ _", font=font(BOLD, 54), fill=acento + (26,))


def llaves(capa, acento):
    """Llaves y escudos: los permisos."""
    t = ImageDraw.Draw(capa)
    for x, y in ((170, 150), (420, 360), (700, 170), (960, 380), (1190, 190), (300, 600), (820, 620)):
        t.ellipse([x - 17, y - 17, x + 17, y + 17], outline=acento + (40,), width=4)
        t.line([x + 13, y + 13, x + 54, y + 54], fill=acento + (40,), width=4)
        t.line([x + 40, y + 40, x + 56, y + 24], fill=acento + (40,), width=4)
    for x, y in ((560, 520), (1080, 620), (240, 300)):
        t.polygon([(x, y - 26), (x + 24, y - 12), (x + 24, y + 14), (x, y + 30), (x - 24, y + 14), (x - 24, y - 12)],
                  outline=acento + (30,), width=3)


def pasos(capa, acento):
    """Camino de pasos numerados: la instalacion."""
    t = ImageDraw.Draw(capa)
    # El camino va por la derecha, para no pisar el titulo.
    for i in range(5):
        x = 700 + i * 116
        y = 78 if i % 2 == 0 else 158
        t.rounded_rectangle([x, y, x + 46, y + 46], radius=12, outline=acento + (34,), width=3)
        t.text((x + 15, y + 8), str(i + 1), font=font(BOLD, 24), fill=acento + (34,))
        if i < 4:
            siguiente_y = 78 if (i + 1) % 2 == 0 else 158
            t.line([x + 46, y + 23, x + 116, siguiente_y + 23], fill=acento + (20,), width=3)



def yaml(capa, acento):
    """Guías de indentacion de YAML: la configuracion."""
    t = ImageDraw.Draw(capa)
    for columna in range(4):
        x = 150 + columna * 120
        for y in range(110, 620, 26):
            t.line([x, y, x, y + 14], fill=acento + (26,), width=2)
    for fila in range(11):
        y = 118 + fila * 46
        sangria = 150 + (fila % 4) * 120
        t.rounded_rectangle([sangria, y, sangria + 90 + (fila * 37) % 150, y + 10], radius=5, fill=acento + (18,))


def radar(capa, acento):
    """Radar de diagnostico: circulos, cruz y avisos."""
    t = ImageDraw.Draw(capa)
    for radio in (70, 130, 190, 250):
        t.ellipse([1150 - radio, 360 - radio, 1150 + radio, 360 + radio], outline=acento + (16,), width=3)
    t.line([860, 360, 1270, 360], fill=acento + (14,), width=2)
    t.line([1150, 70, 1150, 650], fill=acento + (14,), width=2)
    for x, y in ((200, 170), (430, 470), (700, 200), (960, 560)):
        t.polygon([(x, y - 24), (x + 22, y + 16), (x - 22, y + 16)], outline=acento + (34,), width=3)
        t.text((x - 4, y - 10), "!", font=font(BOLD, 20), fill=acento + (34,))


def matriz(capa, acento):
    """Matriz de versiones: la compatibilidad."""
    t = ImageDraw.Draw(capa)
    for fila in range(4):
        for columna in range(9):
            x = 120 + columna * 120
            y = 130 + fila * 130
            t.rounded_rectangle([x, y, x + 84, y + 40], radius=10, outline=acento + (26,), width=2)
    for y in (110, 600):
        t.line([100, y, 1180, y], fill=acento + (16,), width=2)


def checks(capa, acento):
    """Marcas de visto bueno: la instalacion, sin repetir los numeros."""
    t = ImageDraw.Draw(capa)
    for x, y, s in ((200, 150, 20), (430, 320, 16), (660, 170, 18), (890, 330, 15),
                    (1120, 160, 19), (300, 560, 15), (760, 580, 17), (1180, 470, 14)):
        t.line([x - s * 0.7, y, x - s * 0.15, y + s * 0.55], fill=acento + (34,), width=5)
        t.line([x - s * 0.15, y + s * 0.55, x + s * 0.75, y - s * 0.6], fill=acento + (34,), width=5)


def orbitas(capa, acento):
    """Orbitas: versiones girando alrededor del mismo jar."""
    t = ImageDraw.Draw(capa)
    for radio in (150, 260, 380):
        t.ellipse([1180 - radio, 300 - radio // 2, 1180 + radio, 300 + radio // 2],
                  outline=acento + (22,), width=3)
    for x, y in ((980, 210), (1210, 300), (1050, 430), (860, 330), (1150, 140)):
        t.ellipse([x - 6, y - 6, x + 6, y + 6], fill=acento + (44,))


PATRONES = {
    "destellos": destellos,
    "checks": checks,
    "orbitas": orbitas,
    "terminal": terminal,
    "llaves": llaves,
    "pasos": pasos,
    "yaml": yaml,
    "radar": radar,
    "matriz": matriz,
}

# ------------------------------------------------------------------ glifos
def glyph(dibujo, nombre, cx, cy, s, color):
    w = max(2, int(s * 0.11))
    if nombre == "calendar":
        dibujo.rounded_rectangle([cx - s * 0.82, cy - s * 0.62, cx + s * 0.82, cy + s * 0.85], radius=int(s * 0.22),
                                 outline=color, width=w)
        dibujo.line([cx - s * 0.82, cy - s * 0.12, cx + s * 0.82, cy - s * 0.12], fill=color, width=w)
        dibujo.line([cx - s * 0.42, cy - s * 1.0, cx - s * 0.42, cy - s * 0.55], fill=color, width=w)
        dibujo.line([cx + s * 0.42, cy - s * 1.0, cx + s * 0.42, cy - s * 0.55], fill=color, width=w)
        for fila in range(2):
            for columna in range(3):
                x = cx - s * 0.45 + columna * s * 0.45
                y = cy + s * 0.18 + fila * s * 0.42
                dibujo.ellipse([x - w, y - w, x + w, y + w], fill=color)
    elif nombre == "gift":
        dibujo.rounded_rectangle([cx - s * 0.9, cy - s * 0.35, cx + s * 0.9, cy + s * 0.9], radius=int(s * 0.2),
                                 outline=color, width=w)
        dibujo.rounded_rectangle([cx - s * 1.0, cy - s * 0.72, cx + s * 1.0, cy - s * 0.28], radius=int(s * 0.15),
                                 outline=color, width=w)
        dibujo.line([cx, cy - s * 0.72, cx, cy + s * 0.9], fill=color, width=w)
        dibujo.arc([cx - s * 0.62, cy - s * 1.35, cx - s * 0.05, cy - s * 0.72], 180, 360, fill=color, width=w)
        dibujo.arc([cx + s * 0.05, cy - s * 1.35, cx + s * 0.62, cy - s * 0.72], 180, 360, fill=color, width=w)
    elif nombre == "bolt":
        dibujo.polygon([(cx + s * 0.35, cy - s), (cx - s * 0.65, cy + s * 0.12), (cx - s * 0.05, cy + s * 0.12),
                        (cx - s * 0.35, cy + s), (cx + s * 0.62, cy - s * 0.18), (cx + s * 0.02, cy - s * 0.18)],
                       fill=color)
    elif nombre == "star":
        coords = []
        for i in range(10):
            angulo = math.pi / 5 * i - math.pi / 2
            radio = s if i % 2 == 0 else s * 0.44
            coords.append((cx + radio * math.cos(angulo), cy + radio * math.sin(angulo)))
        dibujo.polygon(coords, fill=color)
    elif nombre == "book":
        dibujo.line([cx, cy - s * 0.7, cx, cy + s * 0.75], fill=color, width=w)
        dibujo.polygon([(cx - s * 0.06, cy - s * 0.7), (cx - s * 0.95, cy - s * 0.9), (cx - s * 0.95, cy + s * 0.55),
                        (cx - s * 0.06, cy + s * 0.75)], outline=color, width=w)
        dibujo.polygon([(cx + s * 0.06, cy - s * 0.7), (cx + s * 0.95, cy - s * 0.9), (cx + s * 0.95, cy + s * 0.55),
                        (cx + s * 0.06, cy + s * 0.75)], outline=color, width=w)
    elif nombre == "terminal":
        dibujo.rounded_rectangle([cx - s, cy - s * 0.78, cx + s, cy + s * 0.78], radius=int(s * 0.2), outline=color, width=w)
        dibujo.line([cx - s * 0.55, cy - s * 0.3, cx - s * 0.2, cy], fill=color, width=w)
        dibujo.line([cx - s * 0.2, cy, cx - s * 0.55, cy + s * 0.3], fill=color, width=w)
        dibujo.line([cx + s * 0.05, cy + s * 0.32, cx + s * 0.6, cy + s * 0.32], fill=color, width=w)
    elif nombre == "magnifier":
        dibujo.ellipse([cx - s * 0.85, cy - s * 0.95, cx + s * 0.45, cy + s * 0.35], outline=color, width=w + 1)
        dibujo.line([cx + s * 0.3, cy + s * 0.22, cx + s * 0.95, cy + s * 0.88], fill=color, width=w + 1)
    elif nombre == "play":
        dibujo.polygon([(cx - s * 0.55, cy - s * 0.85), (cx + s * 0.85, cy), (cx - s * 0.55, cy + s * 0.85)], fill=color)
    elif nombre == "sliders":
        for i, y in enumerate((-s * 0.6, 0.0, s * 0.6)):
            dibujo.line([cx - s, cy + y, cx + s, cy + y], fill=color, width=w)
            x = cx + (s * 0.45 if i == 0 else (-s * 0.4 if i == 1 else s * 0.1))
            dibujo.ellipse([x - s * 0.26, cy + y - s * 0.26, x + s * 0.26, cy + y + s * 0.26], fill=color)
    elif nombre == "key":
        dibujo.ellipse([cx - s * 0.9, cy - s * 0.85, cx - s * 0.05, cy], outline=color, width=w + 1)
        dibujo.line([cx - s * 0.35, cy - s * 0.15, cx + s * 0.85, cy + s * 0.8], fill=color, width=w + 1)
        dibujo.line([cx + s * 0.45, cy + s * 0.45, cx + s * 0.65, cy + s * 0.2], fill=color, width=w + 1)
        dibujo.line([cx + s * 0.68, cy + s * 0.66, cx + s * 0.88, cy + s * 0.42], fill=color, width=w + 1)
    elif nombre == "shield":
        dibujo.polygon([(cx, cy - s), (cx + s * 0.8, cy - s * 0.6), (cx + s * 0.62, cy + s * 0.55), (cx, cy + s),
                        (cx - s * 0.62, cy + s * 0.55), (cx - s * 0.8, cy - s * 0.6)], outline=color, width=w + 1)
        dibujo.line([cx - s * 0.34, cy, cx - s * 0.05, cy + s * 0.32], fill=color, width=w + 1)
        dibujo.line([cx - s * 0.05, cy + s * 0.32, cx + s * 0.4, cy - s * 0.34], fill=color, width=w + 1)
    elif nombre == "bell":
        dibujo.arc([cx - s * 0.75, cy - s * 0.9, cx + s * 0.75, cy + s * 0.5], 180, 360, fill=color, width=w + 1)
        dibujo.line([cx - s * 0.75, cy - s * 0.2, cx - s * 0.75, cy + s * 0.45], fill=color, width=w + 1)
        dibujo.line([cx + s * 0.75, cy - s * 0.2, cx + s * 0.75, cy + s * 0.45], fill=color, width=w + 1)
        dibujo.line([cx - s * 0.95, cy + s * 0.45, cx + s * 0.95, cy + s * 0.45], fill=color, width=w + 1)
        dibujo.ellipse([cx - s * 0.22, cy + s * 0.55, cx + s * 0.22, cy + s * 0.95], fill=color)
    elif nombre == "user":
        dibujo.ellipse([cx - s * 0.42, cy - s * 0.95, cx + s * 0.42, cy - s * 0.1], outline=color, width=w + 1)
        dibujo.arc([cx - s * 0.85, cy - s * 0.05, cx + s * 0.85, cy + s * 1.5], 180, 360, fill=color, width=w + 1)
    elif nombre == "users":
        dibujo.ellipse([cx - s * 0.75, cy - s * 0.9, cx - s * 0.15, cy - s * 0.3], outline=color, width=w)
        dibujo.arc([cx - s * 1.05, cy - s * 0.2, cx - s * 0.05, cy + s * 0.95], 180, 360, fill=color, width=w)
        dibujo.ellipse([cx + s * 0.1, cy - s * 0.95, cx + s * 0.8, cy - s * 0.25], outline=color, width=w)
        dibujo.arc([cx + s * 0.02, cy - s * 0.15, cx + s * 1.15, cy + s * 1.1], 180, 360, fill=color, width=w)
    elif nombre == "database":
        dibujo.ellipse([cx - s * 0.8, cy - s * 0.95, cx + s * 0.8, cy - s * 0.45], outline=color, width=w + 1)
        dibujo.line([cx - s * 0.8, cy - s * 0.7, cx - s * 0.8, cy + s * 0.7], fill=color, width=w + 1)
        dibujo.line([cx + s * 0.8, cy - s * 0.7, cx + s * 0.8, cy + s * 0.7], fill=color, width=w + 1)
        dibujo.arc([cx - s * 0.8, cy + s * 0.2, cx + s * 0.8, cy + s * 1.2], 0, 180, fill=color, width=w + 1)
        dibujo.arc([cx - s * 0.8, cy - s * 0.2, cx + s * 0.8, cy + s * 0.8], 0, 180, fill=color, width=w + 1)
    elif nombre == "layers":
        for i, y in enumerate((-s * 0.55, 0.0, s * 0.55)):
            dibujo.polygon([(cx, cy + y - s * 0.42), (cx + s * 0.9, cy + y), (cx, cy + y + s * 0.42),
                            (cx - s * 0.9, cy + y)], outline=color, width=w)
    elif nombre == "refresh":
        dibujo.arc([cx - s * 0.85, cy - s * 0.85, cx + s * 0.85, cy + s * 0.85], 40, 330, fill=color, width=w + 1)
        dibujo.polygon([(cx + s * 0.55, cy - s * 1.0), (cx + s * 1.05, cy - s * 0.55), (cx + s * 0.35, cy - s * 0.45)],
                       fill=color)
    elif nombre == "box":
        dibujo.polygon([(cx, cy - s * 0.95), (cx + s * 0.95, cy - s * 0.45), (cx + s * 0.95, cy + s * 0.55),
                        (cx, cy + s), (cx - s * 0.95, cy + s * 0.55), (cx - s * 0.95, cy - s * 0.45)],
                       outline=color, width=w + 1)
        dibujo.line([cx, cy - s * 0.95, cx, cy + s], fill=color, width=w)
        dibujo.line([cx - s * 0.95, cy - s * 0.45, cx, cy + s * 0.05], fill=color, width=w)
        dibujo.line([cx + s * 0.95, cy - s * 0.45, cx, cy + s * 0.05], fill=color, width=w)
    elif nombre == "chat":
        dibujo.rounded_rectangle([cx - s, cy - s * 0.85, cx + s, cy + s * 0.35], radius=int(s * 0.35),
                                 outline=color, width=w + 1)
        dibujo.polygon([(cx - s * 0.45, cy + s * 0.3), (cx - s * 0.05, cy + s * 0.3), (cx - s * 0.5, cy + s * 0.95)],
                       fill=color)
        for i in range(3):
            x = cx - s * 0.45 + i * s * 0.45
            dibujo.ellipse([x - w, cy - s * 0.4 - w, x + w, cy - s * 0.4 + w], fill=color)
    elif nombre == "info":
        dibujo.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w + 1)
        dibujo.ellipse([cx - w, cy - s * 0.5 - w, cx + w, cy - s * 0.5 + w], fill=color)
        dibujo.line([cx, cy - s * 0.05, cx, cy + s * 0.55], fill=color, width=w + 1)
    elif nombre == "wrench":
        dibujo.line([cx - s * 0.75, cy + s * 0.75, cx + s * 0.35, cy - s * 0.35], fill=color, width=w + 2)
        dibujo.ellipse([cx + s * 0.1, cy - s * 0.95, cx + s * 1.0, cy - s * 0.05], outline=color, width=w + 1)
    elif nombre == "flag":
        dibujo.line([cx - s * 0.6, cy - s, cx - s * 0.6, cy + s], fill=color, width=w + 1)
        dibujo.polygon([(cx - s * 0.55, cy - s * 0.95), (cx + s * 0.9, cy - s * 0.5), (cx - s * 0.55, cy - s * 0.05)],
                       outline=color, width=w)


def tarjeta(imagen, x, y, ancho, alto, icono, titulo, lineas, tema):
    acento = tema["acento"]
    relleno = mezclar(CARD, acento, 0.10)
    borde = mezclar(CARD_BORDER, acento, 0.40)
    insignia = mezclar(ICON_BG, acento, 0.20)
    dibujo = ImageDraw.Draw(imagen)
    dibujo.rounded_rectangle([x, y, x + ancho, y + alto], radius=16, fill=relleno, outline=borde, width=1)
    # insignia del icono
    dibujo.ellipse([x + 22, y + 22, x + 76, y + 76], fill=insignia, outline=borde, width=1)
    glyph(dibujo, icono, x + 49, y + 49, 15, acento)
    # titulo (se reduce solo si no cabe) y descripcion
    tamano = 23
    fuente_titulo = font(SEMI, tamano)
    disponible = ancho - 108
    while dibujo.textlength(titulo, font=fuente_titulo) > disponible and tamano > 16:
        tamano -= 1
        fuente_titulo = font(SEMI, tamano)
    dibujo.text((x + 90, y + 32 + (23 - tamano) // 2), titulo, font=fuente_titulo, fill=TEXT)
    alto_linea = 27
    for i, linea in enumerate(lineas[:3]):
        dibujo.text((x + 24, y + 96 + i * alto_linea), linea, font=font(REGULAR, 18), fill=MUTED)


def envolver(texto, fuente, ancho_max, dibujo):
    palabras = texto.split()
    lineas, actual = [], ""
    for palabra in palabras:
        prueba = (actual + " " + palabra).strip()
        if dibujo.textlength(prueba, font=fuente) <= ancho_max:
            actual = prueba
        else:
            if actual:
                lineas.append(actual)
            actual = palabra
    if actual:
        lineas.append(actual)
    return lineas


# ------------------------------------------------------------------ disenos
# Siete composiciones distintas: la diferencia entre imagenes no es solo el
# color, es como esta contada cada una.

def diseno_rejilla(imagen, tarjetas, tema, dibujo):
    margen, hueco = 86, 24
    ancho = (W - margen * 2 - hueco * 2) // 3
    alto = 196
    for i, (icono, titulo, texto) in enumerate(tarjetas):
        x = margen + (i % 3) * (ancho + hueco)
        y = 236 + (i // 3) * (alto + hueco)
        lineas = envolver(texto, font(REGULAR, 18), ancho - 48, dibujo)
        tarjeta(imagen, x, y, ancho, alto, icono, titulo, lineas, tema)


def diseno_consola(imagen, filas, tema, dibujo):
    acento, secundario = tema["acento"], tema["secundario"]
    x0, x1 = 86, W - 86
    dibujo.rounded_rectangle([x0, 236, x1, 656], radius=18, fill=mezclar(CARD, acento, 0.07),
                             outline=mezclar(CARD_BORDER, acento, 0.45), width=1)
    dibujo.rounded_rectangle([x0, 236, x1, 292], radius=18, fill=mezclar(CARD, acento, 0.16))
    dibujo.rectangle([x0, 274, x1, 292], fill=mezclar(CARD, acento, 0.16))
    for i, color in enumerate((acento, suavizar(acento, 0.35), secundario)):
        x = 116 + i * 26
        dibujo.ellipse([x, 254, x + 14, 268], fill=color)
    dibujo.text((196, 251), "rewards  \u2014  console", font=font(SEMI, 20), fill=MUTED)
    alto_fila = max(38, min(55, (640 - 312) // max(1, len(filas))))
    tamano_comando = 22 if alto_fila >= 50 else 20
    for i, (comando, descripcion) in enumerate(filas):
        y = 310 + i * alto_fila
        dibujo.text((118, y - 4), ">", font=font(BOLD, 24), fill=acento)
        dibujo.text((148, y), comando, font=font(SEMI, tamano_comando), fill=TEXT)
        lineas = envolver(descripcion, font(REGULAR, 17), 630, dibujo)[:2]
        for j, linea in enumerate(lineas):
            dibujo.text((520, y + 2 + j * 20), linea, font=font(REGULAR, 17), fill=MUTED)


def diseno_tabla(imagen, filas, tema, dibujo):
    acento, secundario = tema["acento"], tema["secundario"]
    x0, x1 = 86, W - 86
    dibujo.rounded_rectangle([x0, 236, x1, 290], radius=14, fill=mezclar(CARD, acento, 0.18))
    dibujo.text((x0 + 26, 250), "Permission", font=font(SEMI, 20), fill=suavizar(acento, 0.5))
    dibujo.text((x0 + 430, 250), "Who", font=font(SEMI, 20), fill=suavizar(acento, 0.5))
    dibujo.text((x0 + 600, 250), "What it does", font=font(SEMI, 20), fill=suavizar(acento, 0.5))
    for i, (permiso, quien, descripcion) in enumerate(filas):
        y = 296 + i * 60
        if i % 2 == 0:
            dibujo.rounded_rectangle([x0, y, x1, y + 56], radius=10, fill=mezclar(CARD, acento, 0.05))
        dibujo.text((x0 + 26, y + 17), permiso, font=font(SEMI, 20), fill=TEXT)
        color = secundario if quien == "OP" else acento
        ancho = dibujo.textlength(quien, font=font(SEMI, 17)) + 34
        dibujo.rounded_rectangle([x0 + 430, y + 14, x0 + 430 + ancho, y + 44], radius=15,
                                 fill=mezclar(CARD, color, 0.30), outline=color, width=1)
        dibujo.text((x0 + 447, y + 20), quien, font=font(SEMI, 17), fill=suavizar(color, 0.55))
        lineas = envolver(descripcion, font(REGULAR, 17), 505, dibujo)[:2]
        for j, linea in enumerate(lineas):
            dibujo.text((x0 + 600, y + 16 + j * 21), linea, font=font(REGULAR, 17), fill=MUTED)


def diseno_linea(imagen, pasos, tema, dibujo):
    acento = tema["acento"]
    x = 150
    dibujo.line([x, 262, x, 634], fill=mezclar(CARD_BORDER, acento, 0.75), width=3)
    for i, (titulo, descripcion) in enumerate(pasos):
        y = 254 + i * 66
        dibujo.ellipse([x - 21, y - 21, x + 21, y + 21], fill=mezclar(CARD, acento, 0.35), outline=acento, width=2)
        etiqueta = str(i + 1)
        ancho = dibujo.textlength(etiqueta, font=font(BOLD, 22))
        dibujo.text((x - ancho / 2, y - 15), etiqueta, font=font(BOLD, 22), fill=suavizar(acento, 0.6))
        dibujo.text((x + 48, y - 18), titulo, font=font(SEMI, 23), fill=TEXT)
        dibujo.text((x + 48, y + 12), descripcion, font=font(REGULAR, 18), fill=MUTED)


def diseno_arbol(imagen, cuerpo, tema, dibujo):
    acento = tema["acento"]
    arbol, yaml = cuerpo
    izq = [86, 236, 546, 656]
    der = [574, 236, W - 86, 656]
    for panel in (izq, der):
        dibujo.rounded_rectangle(panel, radius=18, fill=mezclar(CARD, acento, 0.07),
                                 outline=mezclar(CARD_BORDER, acento, 0.45), width=1)
    dibujo.text((izq[0] + 26, 258), "plugins/JPRewards", font=font(SEMI, 21), fill=suavizar(acento, 0.5))
    for i, (nombre, nota) in enumerate(arbol):
        y = 306 + i * 56
        ultimo = i == len(arbol) - 1
        color_rama = mezclar(CARD_BORDER, acento, 0.85)
        # Conectores dibujados: no dependen de la fuente.
        dibujo.line([izq[0] + 34, y + 6, izq[0] + 34, y + (26 if ultimo else 62)], fill=color_rama, width=2)
        dibujo.line([izq[0] + 34, y + 6, izq[0] + 60, y + 6], fill=color_rama, width=2)
        dibujo.text((izq[0] + 88, y - 4), nombre, font=font(SEMI, 19), fill=TEXT)
        dibujo.text((izq[0] + 88, y + 20), nota, font=font(REGULAR, 16), fill=DIM)
    dibujo.text((der[0] + 26, 258), "config.yml", font=font(SEMI, 21), fill=suavizar(acento, 0.5))
    for i, (texto, tipo) in enumerate(yaml):
        y = 296 + i * 35
        if tipo == "comentario":
            color = DIM
        elif tipo == "clave":
            color = suavizar(acento, 0.55)
        elif tipo == "valor":
            color = MUTED
        else:
            color = TEXT
        dibujo.text((der[0] + 26, y), texto, font=font(REGULAR, 17), fill=color)


def diseno_informe(imagen, lineas, tema, dibujo):
    acento, secundario = tema["acento"], tema["secundario"]
    x0, x1 = 86, W - 86
    dibujo.rounded_rectangle([x0, 236, x1, 656], radius=18, fill=mezclar(CARD, acento, 0.06),
                             outline=mezclar(CARD_BORDER, acento, 0.45), width=1)
    dibujo.text((116, 258), "> /rewards validate", font=font(SEMI, 21), fill=suavizar(acento, 0.5))
    for i, (tipo, texto) in enumerate(lineas):
        y = 300 + i * 42
        if tipo == "ok":
            simbolo, color = "OK", suavizar(acento, 0.4)
        elif tipo == "aviso":
            simbolo, color = "!", secundario
        else:
            simbolo, color = "x", (244, 114, 182)
        dibujo.text((118, y), simbolo, font=font(BOLD, 20), fill=color)
        dibujo.text((166, y), texto, font=font(REGULAR, 18), fill=MUTED)


def diseno_codigo(imagen, cuerpo, tema, dibujo):
    """Panel de codigo a la izquierda y lista de puntos a la derecha."""
    acento, secundario = tema["acento"], tema["secundario"]
    codigo, puntos = cuerpo
    izq = [86, 236, 700, 656]
    der = [724, 236, W - 86, 656]
    for panel in (izq, der):
        dibujo.rounded_rectangle(panel, radius=18, fill=mezclar(CARD, acento, 0.07),
                                 outline=mezclar(CARD_BORDER, acento, 0.45), width=1)
    # barra de titulo del panel de codigo
    dibujo.rounded_rectangle([izq[0], izq[1], izq[2], izq[1] + 46], radius=18, fill=mezclar(CARD, acento, 0.16))
    dibujo.rectangle([izq[0], izq[1] + 32, izq[2], izq[1] + 46], fill=mezclar(CARD, acento, 0.16))
    for i, color in enumerate((acento, suavizar(acento, 0.35), secundario)):
        x = izq[0] + 20 + i * 22
        dibujo.ellipse([x, izq[1] + 17, x + 12, izq[1] + 29], fill=color)
    dibujo.text((izq[0] + 100, izq[1] + 13), "MyPlugin.java", font=font(SEMI, 17), fill=MUTED)
    for i, (texto, tipo) in enumerate(codigo):
        y = izq[1] + 62 + i * 26
        if tipo == "comentario":
            color = DIM
        elif tipo == "clave":
            color = suavizar(acento, 0.55)
        elif tipo == "texto":
            color = suavizar(secundario, 0.35)
        else:
            color = MUTED
        dibujo.text((izq[0] + 26, y), texto, font=font(REGULAR, 15), fill=color)
    # lista de puntos
    dibujo.text((der[0] + 26, der[1] + 22), "How to use it", font=font(SEMI, 21), fill=suavizar(acento, 0.5))
    for i, (titulo, texto) in enumerate(puntos):
        y = der[1] + 62 + i * 72
        dibujo.ellipse([der[0] + 28, y + 6, der[0] + 40, y + 18], fill=acento)
        dibujo.text((der[0] + 54, y), titulo, font=font(SEMI, 18), fill=TEXT)
        for j, linea in enumerate(envolver(texto, font(REGULAR, 15), der[2] - der[0] - 90, dibujo)[:2]):
            dibujo.text((der[0] + 54, y + 24 + j * 18), linea, font=font(REGULAR, 15), fill=MUTED)


def diseno_versiones(imagen, cuerpo, tema, dibujo):
    acento = tema["acento"]
    versiones, tarjetas = cuerpo
    y = 306
    x0, x1 = 150, W - 150
    dibujo.line([x0, y, x1, y], fill=mezclar(CARD_BORDER, acento, 0.8), width=3)
    paso = (x1 - x0) / float(len(versiones) - 1)
    for i, (etiqueta, nota) in enumerate(versiones):
        x = x0 + i * paso
        dibujo.ellipse([x - 13, y - 13, x + 13, y + 13], fill=mezclar(CARD, acento, 0.5), outline=acento, width=2)
        ancho = dibujo.textlength(etiqueta, font=font(SEMI, 18))
        dibujo.text((x - ancho / 2, y - 48), etiqueta, font=font(SEMI, 18), fill=suavizar(acento, 0.55))
        if nota:
            ancho_nota = dibujo.textlength(nota, font=font(REGULAR, 15))
            dibujo.text((x - ancho_nota / 2, y + 26), nota, font=font(REGULAR, 15), fill=DIM)
    margen, hueco = 86, 24
    ancho = (W - margen * 2 - hueco * 2) // 3
    for i, (icono, titulo, texto) in enumerate(tarjetas):
        x = margen + i * (ancho + hueco)
        lineas = envolver(texto, font(REGULAR, 18), ancho - 48, dibujo)
        tarjeta(imagen, x, 404, ancho, 232, icono, titulo, lineas, tema)


DISENOS = {
    "rejilla": diseno_rejilla,
    "codigo": diseno_codigo,
    "consola": diseno_consola,
    "tabla": diseno_tabla,
    "linea": diseno_linea,
    "arbol": diseno_arbol,
    "informe": diseno_informe,
    "versiones": diseno_versiones,
}

# ------------------------------------------------------------------ contenido
IMAGENES = {
    "features": ("What you get", "Four reward tracks your players can see and claim in a head menu.", [
        ("calendar", "Four reward tracks", "Daily, weekly, monthly and streak rewards, each one switchable on its own."),
        ("gift", "Rewards in one line", "A single command, a list of commands and items, or a full entry: your choice."),
        ("star", "Packs and conditions", "Define a reward once, reuse it everywhere; add chance, world or one-time only."),
        ("bolt", "Effects and broadcasts", "Sound, particles, fireworks, titles, action bar and server-wide announcements."),
        ("key", "API and extra rewards", "Java API, cancelable events and named rewards for bosses, votes or quests."),
        ("book", "Head menus, paginated", "Textured heads, per-state items, reward previews and automatic pagination."),
    ]),
    "commands": ("Commands", "Everything players need, plus the admin tools that make configuring painless.", [
        ("/rewards", "Opens the main menu. Aliases: jprewards, rec, reward."),
        ("/rewards claim <daily|all>", "Claims without opening the menu, one track or everything ready."),
        ("/rewards top [streak|total]", "Server leaderboard by streak, total, weeks, milestones or bonuses."),
        ("/rewards validate", "Checks every file and names the exact file and key that is wrong."),
        ("/rewards test <track> [key]", "Dry run: prints what a reward would deliver, without delivering it."),
        ("/rewards trigger <player> <id>", "Extra reward from recompensas/extra.yml (API bridge)."),
        ("/rewards backup", "Zips the player data into backups/."),
        ("/rewards reload [all|menus]", "Hot reload of config, messages, menus, rewards or everything at once."),
    ]),
    "permissions": ("Permissions", "Sensible defaults: players get the basics, operators get the rest.", [
        ("jprewards.use", "Everyone", "Open the menus and claim the daily, weekly, monthly and streak rewards."),
        ("jprewards.claim.daily", "Everyone", "Claim the daily reward of the current cycle."),
        ("jprewards.claim.weekly", "Everyone", "Claim the weekly reward and keep the weekly streak."),
        ("jprewards.notify", "Everyone", "Receive the join notice when a reward is ready to claim."),
        ("jprewards.admin", "OP", "status, validate, reload, data and the plugin information."),
        ("jprewards.admin.manage", "OP", "give, test, reset and set for any player, plus the storage migration."),
    ]),
    "setup": ("Setup in a minute", "Drop the jar, start the server, run one command.", [
        ("Drop the jar", "Put JPRewards-2.0.0.jar in plugins/ and start the server."),
        ("Files are created", "config.yml, menus.yml, messages and the reward files appear, never overwriting yours."),
        ("Check the files", "Run /rewards validate: it reports anything that needs your attention."),
        ("Make it yours", "Edit the YAML: rewards, texts, textures, effects. No code and no restarts."),
        ("Set the permissions", "Grant the rank permissions you use (group.vip, jprewards.rewards.vip...)."),
        ("Optional: MySQL", "Switch the storage type, restart and run /rewards migrate confirm."),
    ]),
    "configuration": ("Configuration", "Every text and every reward is a commented YAML file.", (
        [
            ("config.yml", "modules, storage, notices"),
            ("menus.yml", "titles, textures, actions"),
            ("messages_en.yml", "English chat lines"),
            ("messages_es.yml", "Spanish preset"),
            ("recompensas/", "daily, weekly, monthly, streaks"),
            ("extra.yml", "rewards given on demand"),
            ("datos/", "one file per player"),
        ],
        [
            ("# Default language: English (en).", "comentario"),
            ("# Spanish preset included: messages_es.yml", "comentario"),
            ("", "vacio"),
            ("# Storage: YAML files or MySQL.", "comentario"),
            ("# Switching between them never loses data.", "comentario"),
            ("", "vacio"),
            ("# Modules: daily, weekly, monthly, streaks.", "comentario"),
            ("# Reset hour, join notices, sounds, effects.", "comentario"),
            ("", "vacio"),
            ("# Every option is commented in the file.", "comentario"),
        ],
    )),
    "diagnostics": ("Diagnostics", "No more guessing why a reward did not arrive.", [
        ("ok", "config.yml - 41 keys read, every value is valid."),
        ("ok", "menus.yml - 7 menus, 38 buttons, no slot out of range."),
        ("aviso", "menus.yml - a cycle button has no 'slot': the plugin places it for you."),
        ("ok", "the daily reward file - 7 days, every one with a reward."),
        ("aviso", "the weekly reward file - particle HAPPY_VILLAGER is 1.9+ only, skipped on 1.8."),
        ("x", "the monthly reward file - sound 'LEVEL_UP' does not exist on this version."),
        ("ok", "the messages file - every key of the base language is present."),
        ("ok", "Storage - YAML (one file per player) in plugins/JPRewards/datos."),
    ]),
    "integrations": ("Integrations", "Named rewards, a Java API and events any plugin can hook into.", (
        [
            ("JPRewardsAPI api = JPRewardsAPI.get();", "normal"),
            ("if (api != null) {", "normal"),
            ("    api.giveExtra(player,", "clave"),
            ("        \u0022boss-kill\u0022);", "texto"),
            ("}", "normal"),
            ("", "normal"),
            ("// before delivery", "comentario"),
            ("@EventHandler", "clave"),
            ("public void onClaim(", "normal"),
            ("        RewardClaimEvent e) {", "normal"),
            ("    if (e.getTipo().equals(", "normal"),
            ("            \u0022daily\u0022)) e.setCancelled(true);", "texto"),
            ("}", "normal"),
        ],
        [
            ("Named extra rewards", "recompensas/extra.yml gives any plugin, command block or script a reward id to deliver."),
            ("Cancel before it happens", "RewardClaimEvent fires before delivery: cancel it to block a reward."),
            ("React after it happens", "RewardDeliveredEvent tells you what was delivered, to whom and how much."),
            ("No hard dependency", "Check JPRewardsAPI.get(); without the plugin everything else keeps working."),
            ("PlaceholderAPI too", "%jprewards_...% for scoreboards, tabs and holograms."),
        ],
    )),
    "compatibility": ("Compatibility", "One jar from 1.8 to the latest version, with nothing to install.", (
        [
            ("1.8", "Java 8"),
            ("1.12", "legacy names"),
            ("1.16", "hex colours"),
            ("1.19", "modern API"),
            ("1.21+", "tested"),
        ],
        [
            ("box", "No dependencies", "No Vault, no ProtocolLib, no NMS and no shaded libraries inside the jar."),
            ("bolt", "Java 8 bytecode", "The same file loads on a 1.8 server running Java 8 and on a modern Java 21 one."),
            ("shield", "Tested on real servers", "29 automated checks pass on Paper 1.8.8 and Spigot 1.21.1 (see VERIFICATION)."),
        ],
    )),
}


def main():
    for nombre, (titulo, subtitulo, cuerpo) in IMAGENES.items():
        tema = TEMAS[nombre]
        imagen = base(titulo, subtitulo, tema)
        dibujo = ImageDraw.Draw(imagen)
        DISENOS[tema["diseno"]](imagen, cuerpo, tema, dibujo)
        footer(imagen, tema)
        guardar(imagen, os.path.join(OUT, nombre + ".png"))
        print("%-17s %dx%d  %s" % (nombre + ".png", W, H, tema["diseno"]))


if __name__ == "__main__":
    main()
