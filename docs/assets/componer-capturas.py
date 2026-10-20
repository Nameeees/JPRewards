"""Compone banners con capturas REALES del juego.

    python docs/assets/componer-capturas.py

Toma los PNG de docs/assets/capturas (los hace tools/capturar-juego.ps1 con el
cliente de verdad), recorta la ventana del juego y la monta sobre el fondo de la
marca con una leyenda. Salida: docs/assets/real-<nombre>.png.
"""

import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
CAPTURAS = os.path.join(RAIZ, "docs", "assets", "capturas")
SALIDA = os.path.join(RAIZ, "docs", "assets")

FONDO_ARRIBA = (13, 11, 24)
FONDO_ABAJO = (30, 20, 52)
ACENTO = (217, 70, 239)
TEXTO = (240, 236, 252)
APAGADO = (170, 160, 194)
ORO = (250, 204, 21)

BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

MENUS = [
    # El tooltip del hub salia cortado por el borde de la pantalla: se recorta solo la ventana.
    ("menu-hub.png", "real-hub.png", "Main menu", "Four tracks, live previews and the claim-all button.", False),
    ("menu-diario.png", "real-diario.png", "Daily cycle", "Claimed days, today and locked days, with pagination.", True),
    ("menu-mensual.png", "real-mensual.png", "Monthly milestones", "Unlocked by the days you claim during the month.", True),
    ("menu-rachas.png", "real-rachas.png", "Streak bonuses", "Rewards for claiming day after day.", True),
    ("menu-semanal.png", "real-semanal.png", "Weekly cycle", "A better reward once a week, with its own streak.", True),
    ("menu-info.png", "real-info.png", "How it works", "Progress, streaks and when everything resets.", True),
]


def fondo(ancho, alto):
    imagen = Image.new("RGB", (ancho, alto))
    dibujo = ImageDraw.Draw(imagen)
    for y in range(alto):
        t = y / float(alto - 1)
        color = tuple(int(FONDO_ARRIBA[i] + (FONDO_ABAJO[i] - FONDO_ARRIBA[i]) * t) for i in range(3))
        dibujo.line([(0, y), (ancho, y)], fill=color)
    imagen = imagen.convert("RGBA")
    halo = Image.new("RGBA", (ancho, alto), (0, 0, 0, 0))
    pincel = ImageDraw.Draw(halo)
    pincel.ellipse([-260, -300, 520, 300], fill=ACENTO + (56,))
    pincel.ellipse([ancho - 460, alto - 320, ancho + 200, alto + 200], fill=ORO + (20,))
    return Image.alpha_composite(imagen, halo.filter(ImageFilter.GaussianBlur(150)))


def cajas(imagen):
    """Devuelve la caja de la GUI (gris) y la del tooltip (borde morado), si hay."""
    ancho, alto = imagen.size
    pixeles = imagen.load()
    gui = [ancho, alto, 0, 0]
    tip = [ancho, alto, 0, 0]
    hay_tip = False
    for y in range(0, alto, 2):
        for x in range(0, ancho, 2):
            r, g, b, a = pixeles[x, y]
            if abs(r - 198) < 6 and abs(g - 198) < 6 and abs(b - 198) < 6:
                gui[0] = min(gui[0], x)
                gui[1] = min(gui[1], y)
                gui[2] = max(gui[2], x)
                gui[3] = max(gui[3], y)
            # borde del tooltip: morado muy saturado (80,0,255) / (40,0,127)
            if b > 200 and r < 120 and g < 70:
                hay_tip = True
                tip[0] = min(tip[0], x)
                tip[1] = min(tip[1], y)
                tip[2] = max(tip[2], x)
                tip[3] = max(tip[3], y)
    return gui, (tip if hay_tip else None)


def recortar_ventana(ruta, incluir_tooltip=True):
    """Recorta lo que se ve: la ventana del juego y, si hay, el tooltip."""
    imagen = Image.open(ruta).convert("RGBA")
    ancho, alto = imagen.size
    gui, tip = cajas(imagen)
    if gui[2] <= gui[0]:
        return imagen
    izquierda, arriba, derecha, abajo = gui
    if tip is not None and incluir_tooltip:
        # el tooltip se incluye solo si esta pegado a la ventana
        if tip[0] <= derecha + 260 and tip[2] >= izquierda - 260:
            izquierda = min(izquierda, tip[0])
            arriba = min(arriba, tip[1])
            derecha = max(derecha, tip[2])
            abajo = max(abajo, tip[3])
    margen = 12
    return imagen.crop((max(0, izquierda - margen), max(0, arriba - margen),
                        min(ancho, derecha + margen), min(alto, abajo + margen)))


def componer(origen, destino, titulo, leyenda, incluir_tooltip=True):
    ventana = recortar_ventana(os.path.join(CAPTURAS, origen), incluir_tooltip)
    ancho, alto = 1280, 720
    imagen = fondo(ancho, alto)
    # escalar la ventana para que quepa
    disponible_alto = alto - 200
    escala = min((ancho - 130) / float(ventana.size[0]), disponible_alto / float(ventana.size[1]))
    nueva = (int(ventana.size[0] * escala), int(ventana.size[1] * escala))
    ventana = ventana.resize(nueva, Image.LANCZOS)
    x = (ancho - nueva[0]) // 2
    y = 150
    # sombra y marco
    sombra = Image.new("RGBA", imagen.size, (0, 0, 0, 0))
    ImageDraw.Draw(sombra).rounded_rectangle([x - 10, y - 6, x + nueva[0] + 10, y + nueva[1] + 14],
                                             radius=18, fill=(0, 0, 0, 160))
    imagen = Image.alpha_composite(imagen, sombra.filter(ImageFilter.GaussianBlur(16)))
    imagen.alpha_composite(ventana, (x, y))
    dibujo = ImageDraw.Draw(imagen)
    dibujo.rounded_rectangle([70, 42, 140, 52], radius=5, fill=ACENTO)
    fuente_titulo = ImageFont.truetype(BOLD, 44)
    fuente_texto = ImageFont.truetype(REGULAR, 19)
    fuente_pie = ImageFont.truetype(REGULAR, 16)
    dibujo.text((70, 66), titulo, font=fuente_titulo, fill=TEXTO)
    dibujo.text((72, 118), leyenda, font=fuente_texto, fill=APAGADO)
    pie = "JPRewards 2.1.0  ·  real capture in Minecraft 1.8.9  ·  no mockup"
    dibujo.text((70, alto - 46), pie, font=fuente_pie, fill=(128, 122, 152))
    return imagen


def main():
    os.makedirs(SALIDA, exist_ok=True)
    generados = 0
    for origen, destino, titulo, leyenda, incluir_tooltip in MENUS:
        if not os.path.isfile(os.path.join(CAPTURAS, origen)):
            continue
        imagen = componer(origen, destino, titulo, leyenda, incluir_tooltip)
        ruta = os.path.join(SALIDA, destino)
        temporal = ruta + ".tmp.png"
        imagen.convert("RGB").save(temporal, format="PNG")
        os.replace(temporal, ruta)
        print("generado:", destino)
        generados += 1
    print("banners con captura real:", generados)


if __name__ == "__main__":
    main()
