#!/usr/bin/env python3
"""Genera en INGLES los archivos por defecto de recompensas y los comentarios de menu.

Fuente de verdad: los presets en espanol (menus_es.yml, recompensas/*_es.yml).
Solo se sustituyen textos visibles y cabeceras; texturas, slots, comandos e
importes quedan exactamente igual.
"""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
REC = RAIZ / "src" / "main" / "resources" / "recompensas"
MENUS = RAIZ / "src" / "main" / "resources" / "menus.yml"

# --- comentarios de seccion de menus.yml --------------------------------------
COMENTARIOS_MENUS = [
    ("# ---- Piezas compartidas por todos los menus --------------------------------",
     "# ---- Shared by every menu ---------------------------------------------------"),
    ("# ---- Menu principal (hub) --------------------------------------------------",
     "# ---- Main menu (hub) -------------------------------------------------------"),
    ("# ---- Ciclo diario ----------------------------------------------------------",
     "# ---- Daily cycle -----------------------------------------------------------"),
    ("# Los dias del ciclo se colocan solos desde 'slot-inicial' (7 por fila) y se",
     "# The cycle days are laid out automatically from 'slot-inicial' (7 per row) and"),
    ("# reparten en paginas si no caben.",
     "# split into pages when they do not fit."),
    ("# ---- Ciclo semanal ---------------------------------------------------------",
     "# ---- Weekly cycle ----------------------------------------------------------"),
    ("# ---- Hitos del mes ---------------------------------------------------------",
     "# ---- Monthly milestones ----------------------------------------------------"),
    ("# ---- Bonus de racha --------------------------------------------------------",
     "# ---- Streak bonuses --------------------------------------------------------"),
    ("# ---- Informacion -----------------------------------------------------------",
     "# ---- Information -----------------------------------------------------------"),
]

# --- cabeceras de los archivos de recompensas (ingles) ------------------------
CABECERAS = {
    "diarias": """# ============================================================================
#  JPRewards - recompensas/diarias.yml  (English)
# ----------------------------------------------------------------------------
#  DAILY reward cycle. The key of each entry is the day number (1..N).
#  N adjusts itself; set config.yml -> diaria.dias-del-ciclo to force it.
#  A Spanish copy ships as diarias_es.yml (point diaria.archivo at it to use it).
#
#  FIELDS
#    icono.textura / icono.material   menu head or material
#    permiso: ''                      entry exclusive to a permission
#    nombre: '...'                    name shown in messages and titles
#    lore: [...]                      HOW IT LOOKS (use {preview} in menus.yml)
#    comandos: [...]                  WHAT IT GIVES, run from the console
#    items: [...]                     WHAT IT GIVES, as direct items
#    experiencia / niveles            direct XP (config: entrega.experiencia-directa)
#    mensajes: [...]                  extra chat lines when claimed
#    efectos: {...}                   sound, particle, fireworks, title, broadcast
#    rangos: {...}                    per-rank variants (the first match wins)
#
#  Commands run from the CONSOLE with {jugador}, {uuid}, {mundo} and {premio}.
#  Examples: eco give {jugador} 100 | give {jugador} diamond 2 | xp add {jugador} 50 points
# ============================================================================
""",
    "semanales": """# ============================================================================
#  JPRewards - recompensas/semanales.yml  (English)
# ----------------------------------------------------------------------------
#  WEEKLY reward cycle. The key is the week block number (1..N).
#  It can be claimed ONCE per week: the week starts on the day configured in
#  config.yml -> semanal.dia-inicio, at reinicio.hora.
#  A Spanish copy ships as semanales_es.yml.
#
#  Weekly rewards should feel clearly better than the daily ones: they are the
#  reason to come back every week. Same fields as diarias.yml
#  (lore, comandos, items, efectos, rangos...).
# ============================================================================
""",
    "mensuales": """# ============================================================================
#  JPRewards - recompensas/mensuales.yml  (English)
# ----------------------------------------------------------------------------
#  MONTHLY MILESTONES: unlocked by claiming the daily reward on enough days
#  WITHIN the current month. When the month changes, the progress and the
#  claimed milestones reset by themselves (history is kept for /top hitos).
#  A Spanish copy ships as mensuales_es.yml.
#
#  The key of each milestone is its id (used in the menu and in the actions).
#  'dias-requeridos' is the only required field besides the reward itself.
#  Same fields as diarias.yml (lore, comandos, items, efectos, rangos...).
#
#  February has 28 days: keep the milestones reachable in any month.
# ============================================================================
""",
    "rachas": """# ============================================================================
#  JPRewards - recompensas/rachas.yml  (English)
# ----------------------------------------------------------------------------
#  STREAK BONUSES: unlocked by reaching N days (or weeks) IN A ROW.
#  They can be claimed once per streak; if the player loses the streak they
#  become available again.
#  A Spanish copy ships as rachas_es.yml.
#
#  FIELDS
#    tipo: diaria | semanal   which streak counts (daily by default)
#    racha-minima: N          days or weeks in a row required
#    (every other field is the same as in diarias.yml)
#
#  This module can be switched off in config.yml -> modulos.rachas.
# ============================================================================
""",
}

# --- textos visibles (orden importa: lo mas especifico primero) ---------------
COMUNES = [
    (" &7experiencia", " &7experience"),
    ("&6Manzana dorada", "&6Golden apples"),
    ("&bBloque de diamante", "&bDiamond block"),
    ("&bDiamante", "&bDiamond"),
    ("&7monedas", "&7{moneda}"),
]

DIARIAS = [
    ("&7▪ &f4x &fPan", "&7▪ &f4x &fBread"),
    ("&7▪ &f8x &fLingote de hierro", "&7▪ &f8x &fIron ingot"),
    ("&7▪ &bEspada de diamante &7afilada", "&7▪ &bEnchanted diamond sword"),
    ("&8Mitad del ciclo", "&8Half of the cycle"),
    ("&bDiamante del dia 5", "&bDiamond of day 5"),
    ("&7Premio por no fallar.", "&7Reward for not missing a day."),
    ("&bFilo del dia 6", "&bDay 6 blade"),
    ("&8Ciclo completo", "&8Full cycle"),
    ("&d&lCofre del ciclo completo", "&d&lFull cycle chest"),
    ("&a¡Ciclo diario completado!", "&aDaily cycle completed!"),
    ("&d¡Completaste el ciclo diario! &7Vuelve manana para empezar otro.",
     "&dYou completed the daily cycle! &7Come back tomorrow to start another one."),
    ("&7Consejo: &fusa /rewards&7 para ver todo tu progreso.",
     "&7Tip: &fuse /rewards&7 to see all your progress."),
    ("&8Premio del rango &6&lMVIP", "&8Reward for rank &6&lMVIP"),
    ("&8Premio del rango &d&lVIP", "&8Reward for rank &d&lVIP"),
    ("&6Cofre del ciclo &6&lMVIP", "&6Full cycle chest &6&lMVIP"),
    ("&dCofre del ciclo &d&lVIP", "&dFull cycle chest &d&lVIP"),
]

SEMANALES = [
    ("&8Semana completa del ciclo", "&8Full cycle week"),
    ("&d&lCofre de la semana completa", "&d&lFull week chest"),
    ("&d{jugador} &7ha completado el ciclo semanal de recompensas!",
     "&d{jugador} &7completed the weekly rewards cycle!"),
    ("&8Semana completa del rango &6&lMVIP", "&8Full week for rank &6&lMVIP"),
    ("&8Semana completa del rango &d&lVIP", "&8Full week for rank &d&lVIP"),
    ("&6Semana completa &6&lMVIP", "&6Full week &6&lMVIP"),
    ("&dSemana completa &d&lVIP", "&dFull week &d&lVIP"),
]

MENSUALES = [
    ("&6&lMes completo", "&6&lFull month"),
    ("&8Mes casi perfecto", "&8Almost a perfect month"),
    ("&bBloque del mes", "&bBlock of the month"),
    ("&7Recompensa por 25 dias reclamados.", "&7Reward for 25 claimed days."),
    ("&6{jugador} &7ha completado el mes de recompensas!",
     "&6{jugador} &7completed the monthly rewards!"),
    ("&8Mes completo del rango &6&lMVIP", "&8Full month for rank &6&lMVIP"),
    ("&8Mes completo del rango &d&lVIP", "&8Full month for rank &d&lVIP"),
    ("&7▪ &f1x &fLlave mensual &7(por comando)", "&7▪ &f1x &fMonthly key &7(via command)"),
]

RACHAS = [
    ("&eSemana perfecta", "&ePerfect week"),
    ("&6&lMes perfecto", "&6&lPerfect month"),
    ("&dMes de semanas perfectas", "&dMonth of perfect weeks"),
    ("&7▪ &e1x &6Llave de voto &7(por comando)", "&7▪ &e1x &6Vote key &7(via command)"),
    ("&7▪ &f1x &f&lLlave legendaria &7(por comando)", "&7▪ &f1x &f&lLegendary key &7(via command)"),
    ("&8Recompensa por 7 dias seguidos", "&8Reward for 7 days in a row"),
    ("&830 dias seguidos sin fallar", "&830 days in a row without missing"),
    ("&84 semanas seguidas reclamando", "&84 weeks claimed in a row"),
    ("&e¡Racha de 7 dias conseguida!", "&e7 day streak reached!"),
    ("&6{jugador} &7ha conseguido una racha de &630 dias&7!",
     "&6{jugador} &7reached a &630 day&7 streak!"),
    ("&d{jugador} &7ha reclamado 4 semanas seguidas!",
     "&d{jugador} &7claimed 4 weeks in a row!"),
    ("&8Racha de 30 dias del rango &6&lMVIP", "&8Streak of 30 days for rank &6&lMVIP"),
]


COMENTARIOS_EXTRA = [
    ("    # borde (solo el marco) | todas (todo el cofre) | lista de slots",
     "    # borde (frame only) | todas (the whole chest) | a list of slots"),
    ("  # Botones de pagina (el plugin les pone la accion de cambiar de pagina).",
     "  # Page buttons (the plugin adds the page-changing action itself)."),
    ("  # Plantillas de la vista previa de premios que inserta {preview}.",
     "  # Templates for the reward preview that {preview} inserts."),
    ("    # Items directos: forma corta ('MATERIAL cantidad') o completa.",
     "    # Direct items: short form ('MATERIAL amount') or full form."),
    ("      # Forma completa: cualquier opcion de item de Minecraft.",
     "      # Full form: any Minecraft item option."),
    ("      # Item con nombre, lore y encantamientos (une varias entradas con 'items').",
     "      # An item with a name, lore and enchantments."),
    ("        # En 1.8 estos nombres se traducen solos (SHARPNESS -> DAMAGE_ALL).",
     "        # On 1.8 these names are translated automatically (SHARPNESS -> DAMAGE_ALL)."),
    ("  # El dia 7 muestra las VARIANTES POR RANGO (rangos:).",
     "  # Day 7 shows the PER-RANK VARIANTS (rangos:)."),
    ("  # Quien tenga mvip recibe y VE la version MVIP; quien tenga vip, la VIP;",
     "  # A player with mvip gets (and SEES) the MVIP version; with vip, the VIP one;"),
    ("  # quien no tenga ninguno, la version base de arriba. Pon los rangos MAS",
     "  # with no rank at all, the base version above. Put the HIGHEST ranks"),
    ("  # ALTOS primero: gana la primera variante cuyo permiso tenga el jugador.",
     "  # FIRST: the first variant whose permission the player has wins."),
    ("      # Anuncio a TODO el servidor (dejalo vacio o quita la linea para no anunciar).",
     "      # Broadcast to the WHOLE server (leave it empty or remove the line to skip it)."),
    ("          # Ejemplo con el plugin de cofres de tu servidor:",
     "          # Example using your server's crate plugin:"),
    ("  # A los 7 dias seguidos (una semana completa sin fallar).",
     "  # At 7 days in a row (a full week without missing a day)."),
    ("  # A los 30 dias seguidos: el gran premio de constancia.",
     "  # At 30 days in a row: the big consistency reward."),
    ("    # Variantes por rango: los mismos permisos que en los otros archivos.",
     "    # Per-rank variants: the same permissions as in the other files."),
    ("  # Bonus de racha SEMANAL: 4 semanas seguidas reclamando.",
     "  # WEEKLY streak bonus: 4 weeks claimed in a row."),
]

ARCHIVOS = [
    ("diarias", DIARIAS),
    ("semanales", SEMANALES),
    ("mensuales", MENSUALES),
    ("rachas", RACHAS),
]


def aplicar(texto, pares):
    faltan = []
    for viejo, nuevo in pares:
        if viejo in texto:
            texto = texto.replace(viejo, nuevo)
        else:
            faltan.append(viejo)
    return texto, faltan


def main():
    problemas = []

    # 1) comentarios de menus.yml (el resto ya esta en ingles)
    texto = MENUS.read_text(encoding="utf-8")
    texto, faltan = aplicar(texto, COMENTARIOS_MENUS)
    texto, faltan2 = aplicar(texto, COMENTARIOS_EXTRA)
    MENUS.write_text(texto, encoding="utf-8")
    print("menus.yml: comentarios actualizados (%d sin encontrar)" % len(faltan))
    for f in faltan:
        print("   SIN ENCONTRAR:", f[:90])

    # 2) archivos de recompensas
    for nombre, propios in ARCHIVOS:
        origen = REC / ("%s_es.yml" % nombre)
        destino = REC / ("%s.yml" % nombre)
        crudo = origen.read_text(encoding="utf-8")
        corte = crudo.index("config-version: 2")
        cuerpo = crudo[corte:]
        cuerpo, f1 = aplicar(cuerpo, propios)
        cuerpo, f2 = aplicar(cuerpo, COMUNES)
        cuerpo, f3 = aplicar(cuerpo, COMENTARIOS_EXTRA)
        destino.write_text(CABECERAS[nombre] + "\n" + cuerpo, encoding="utf-8")
        print("%s.yml escrito (%d sin encontrar propios, %d comunes)" % (nombre, len(f1), len(f2)))
        for f in f1 + f2:
            print("   SIN ENCONTRAR:", f[:90])
        # control
        salida = destino.read_text(encoding="utf-8")
        for i, linea in enumerate(salida.splitlines(), 1):
            if "textura:" in linea and "'" not in linea:
                problemas.append("%s.yml linea %d sin comillas" % (nombre, i))
            if re.search(r"[áéíóúñ¿¡]", linea) and not linea.strip().startswith("#"):
                problemas.append("%s.yml linea %d con acentos: %s" % (nombre, i, linea[:60]))

    print("problemas estructurales:", len(problemas))
    for p in problemas[:15]:
        print("  ", p)
    return 0 if not problemas else 1


if __name__ == "__main__":
    sys.exit(main())
