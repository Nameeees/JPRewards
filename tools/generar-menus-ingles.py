#!/usr/bin/env python3
"""Genera los menus en INGLES (menus.yml) a partir del preset en espanol (menus_es.yml).

El preset espanol es la fuente de verdad: este script solo sustituye los textos
visibles y la cabecera, dejando texturas, slots, estados y acciones intactos.
"""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DESTINO = RAIZ / "src" / "main" / "resources" / "menus.yml"
FUENTE = RAIZ / "src" / "main" / "resources" / "menus_es.yml"

CABECERA = """# ============================================================================
#  JPRewards - menus.yml  (English)
# ----------------------------------------------------------------------------
#  EVERY part of the menus is defined here: titles, textures, buttons, states,
#  pagination and navigation. No code changes are ever needed.
#  A Spanish copy of this file ships as menus_es.yml: point
#  config.yml -> menus.archivo at it (or at your own translated copy) to switch.
#
#  MENU STRUCTURE
#    menu-xxx:
#      titulo: '...'            inventory title (accepts {pagina}, {paginas})
#      filas: 1-6               chest height
#      slot-inicial: 10         where the automatic items (cycles) start
#      slots: [10, 11, 12]      (optional) exact slots for those items
#      navegacion: {volver-slot, cerrar-slot}
#      paginacion: {anterior-slot, siguiente-slot, info-slot}
#      botones: { ... }
#
#  BUTTON FIELDS
#    slot / slots         position (or a list: [10, 11] or '10-16')
#    material             modern material name; on 1.8 the legacy equivalent is used
#    textura              a head texture value (base64, from minecraft-heads.com)
#    textura-jugador      a player's head by name (instead of textura)
#    cantidad             1-64
#    nombre / lore        texts with & colours and {placeholders}; {preview} allowed
#    brillo               true = enchanted glow without a visible enchantment
#    encantamientos:      { SHARPNESS: 3, UNBREAKING: 1 }
#    item-flags:          [HIDE_ATTRIBUTES, HIDE_ENCHANTS]
#    custom-model-data    1.14+ (ignored on older versions)
#    irrompible           true = unbreakable (1.11+) or hidden durability
#    icono-recompensa     true = use the reward's own head when available
#    permiso              permission required to see and use the button
#    ocultar-sin-permiso  true = hide the button when the permission is missing
#    accion / accion-<estado>   one action, or several separated by ';'
#
#  STATES (chosen automatically from the player's data)
#    daily:   hoy | reclamado | bloqueado
#    weekly:  disponible | reclamado | bloqueado
#    monthly: disponible | bloqueado | reclamado
#    streaks: disponible | bloqueado | reclamado
#    main:    disponible | reclamada | bloqueada | completada | nada
#  For each state you can define: nombre-<estado>, lore-<estado>,
#  textura-<estado>, brillo-<estado> and accion-<estado>.
#
#  ACTIONS
#    abrir:<menu> | cerrar | nada (or ninguna: the same thing)
#    reclamar:diaria | reclamar:semanal | reclamar:mensual | reclamar:rachas
#    reclamar:todo | reclamar:hito:<id> | reclamar:racha:<id>
#    comando:<cmd> | jugador-comando:<cmd> | mensaje:<messages key> | sonido:<name>
#    (several at once separated by ';' -- they run in order)
#
#  PLACEHOLDERS
#    {jugador} {uuid} {mundo} {dia} {dias} {racha} {mejor} {total}
#    {semana} {semanas} {racha_semanal} {mejor_semanal} {total_semanal}
#    {dias_mes} {dias_mes_total} {hitos} {hitos_total} {hitos_disponibles}
#    {bonus} {bonus_total} {bonus_disponibles} {pendientes} {tiempo}
#    {tiempo_largo} {tiempo_semanal} {hora} {moneda} {premio} {estado}
#    {cantidad} {faltan} {racha_minima} {hito} {pagina} {paginas}
#    {barra_mes} {barra_ciclo} {barra_racha} {barra_racha_semanal}
#    {version} {servidor} {almacenamiento} {modulos}
#    and any %placeholder% from PlaceholderAPI if it is installed.
#
#  The bundled textures come from minecraft-heads.com (free to use).
# ============================================================================
"""

REEMPLAZOS = [
    ("'&c◀ Volver'", "'&c◀ Back'"),
    ("&7Regresa al menu anterior.", "&7Go back to the previous menu."),
    ("&c» Clic para volver", "&c» Click to go back"),
    ("'&c&l✖ Cerrar'", "'&c&l✖ Close'"),
    ("&7Cierra este menu.", "&7Close this menu."),
    ("&c» Clic para cerrar", "&c» Click to close"),
    ("'&e« Pagina anterior'", "'&e« Previous page'"),
    ("'&ePagina siguiente »'", "'&eNext page »'"),
    ("&7Estas en la pagina &f{pagina}&7/&f{paginas}&7.", "&7You are on page &f{pagina}&7/&f{paginas}&7."),
    ("'&7Pagina &f{pagina}&7/&f{paginas}'", "'&7Page &f{pagina}&7/&f{paginas}'"),
    ("&8Usa las flechas para moverte.", "&8Use the arrows to move around."),
    ("'&8Premios:'", "'&8Rewards:'"),
    ("&7▪ &8(sin premios definidos)", "&7▪ &8(no rewards defined)"),
    ("&7▪ &cSolo para rangos con permiso especial", "&7▪ &cRequires a rank with the special permission"),
    ("&7▪ &8Con rango: &7premios mejores", "&7▪ &8With a rank: &7better rewards"),
    ("'&d&l✦ Recompensas ✦'", "'&d&l✦ Rewards ✦'"),
    ("'&a&l✦ Recompensa diaria &8[&aDISPONIBLE&8]'", "'&a&l✦ Daily reward &8[&aAVAILABLE&8]'"),
    ("'&7&lRecompensa diaria &8[&7RECLAMADA&8]'", "'&7&lDaily reward &8[&7CLAIMED&8]'"),
    ("'&7&lRecompensa diaria &8[&8APAGADA&8]'", "'&7&lDaily reward &8[&8DISABLED&8]'"),
    ("'&7Te toca el &fdia {dia} &7de &f{dias}&7.'", "'&7Today is day &f{dia} &7of &f{dias}&7.'"),
    ("'&7Racha: &f{racha} &7dia(s) &8(mejor &f{mejor}&8).'", "'&7Streak: &f{racha} &7day(s) &8(best &f{mejor}&8).'"),
    ("'&a» Clic para reclamar ahora'", "'&a» Click to claim now'"),
    ("'&7Ya reclamaste la recompensa de hoy.'", "'&7You already claimed today''s reward.'"),
    ("'&7Vuelve en &f{tiempo} &8(a las &f{hora}&8).'", "'&7Come back in &f{tiempo} &8(at &f{hora}&8).'"),
    ("'&7Racha actual: &f{racha} &7dia(s).'", "'&7Current streak: &f{racha} &7day(s).'"),
    ("'&8» Reclamada, vuelve manana'", "'&8» Claimed, come back tomorrow'"),
    ("'&7Este modulo esta desactivado en el servidor.'", "'&7This module is disabled on this server.'"),
    ("'&a&l✦ Recompensa semanal &8[&aDISPONIBLE&8]'", "'&a&l✦ Weekly reward &8[&aAVAILABLE&8]'"),
    ("'&7&lRecompensa semanal &8[&7RECLAMADA&8]'", "'&7&lWeekly reward &8[&7CLAIMED&8]'"),
    ("'&7&lRecompensa semanal &8[&8APAGADA&8]'", "'&7&lWeekly reward &8[&8DISABLED&8]'"),
    ("'&7Te toca la &fsemana {semana} &7de &f{semanas}&7.'", "'&7This week is &f{semana} &7of &f{semanas}&7.'"),
    ("'&7Racha semanal: &f{racha_semanal} &7semana(s).'", "'&7Weekly streak: &f{racha_semanal} &7week(s).'"),
    ("'&7Ya reclamaste la recompensa de esta semana.'", "'&7You already claimed this week''s reward.'"),
    ("'&7Vuelve en &f{tiempo_semanal}&7.'", "'&7Come back in &f{tiempo_semanal}&7.'"),
    ("'&8» Reclamada, vuelve la semana que viene'", "'&8» Claimed, come back next week'"),
    ("'&a&l✦ Recompensa mensual &8[&a{hitos_disponibles} LISTO(S)&8]'", "'&a&l✦ Monthly reward &8[&a{hitos_disponibles} READY&8]'"),
    ("'&d&lRecompensa mensual'", "'&d&lMonthly reward'"),
    ("'&7&lRecompensa mensual &8[&7COMPLETADA&8]'", "'&7&lMonthly reward &8[&7COMPLETED&8]'"),
    ("'&7Dias reclamados este mes: &f{dias_mes}&7.'", "'&7Days claimed this month: &f{dias_mes}&7.'"),
    ("'&7Hitos listos: &f{hitos_disponibles}&7.'", "'&7Milestones ready: &f{hitos_disponibles}&7.'"),
    ("'&a» Clic para abrir los hitos'", "'&a» Click to open the milestones'"),
    ("'&7Reclama tu diaria cada dia para desbloquear'", "'&7Claim the daily reward every day to unlock'"),
    ("'&7los hitos mensuales.'", "'&7the monthly milestones.'"),
    ("'&7Dias reclamados este mes: &f{dias_mes}&7/&f{dias_mes_total}&7.'", "'&7Days claimed this month: &f{dias_mes}&7/&f{dias_mes_total}&7.'"),
    ("'&d» Clic para ver los hitos'", "'&d» Click to see the milestones'"),
    ("'&7Todos los hitos del mes estan reclamados.'", "'&7Every milestone of this month is claimed.'"),
    ("'&7Dias reclamados: &f{dias_mes}&7.'", "'&7Days claimed: &f{dias_mes}&7.'"),
    ("'&8» Clic para revisar los hitos'", "'&8» Click to review the milestones'"),
    ("'&a&l✦ Bonus de racha &8[&a{bonus_disponibles} LISTO(S)&8]'", "'&a&l✦ Streak bonus &8[&a{bonus_disponibles} READY&8]'"),
    ("'&d&lBonus de racha'", "'&d&lStreak bonus'"),
    ("'&7&lBonus de racha &8[&7COMPLETADOS&8]'", "'&7&lStreak bonus &8[&7COMPLETED&8]'"),
    ("'&7Bonus listos: &f{bonus_disponibles}&7.'", "'&7Bonuses ready: &f{bonus_disponibles}&7.'"),
    ("'&a» Clic para reclamarlos'", "'&a» Click to claim them'"),
    ("'&7Reclama cada dia seguido para desbloquear'", "'&7Claim every day in a row to unlock'"),
    ("'&7los bonus por racha.'", "'&7the streak bonuses.'"),
    ("'&d» Clic para ver los bonus'", "'&d» Click to see the bonuses'"),
    ("'&7Ya reclamaste todos los bonus de esta racha.'", "'&7You already claimed every bonus of this streak.'"),
    ("'&8» Clic para verlos'", "'&8» Click to see them'"),
    ("'&d&lTu racha: &f{racha} &7dia(s)'", "'&d&lYour streak: &f{racha} &7day(s)'"),
    ("'&7Racha diaria: &f{racha} &8(mejor &f{mejor}&8)'", "'&7Daily streak: &f{racha} &8(best &f{mejor}&8)'"),
    ("'&7Racha semanal: &f{racha_semanal} &8(mejor &f{mejor_semanal}&8)'", "'&7Weekly streak: &f{racha_semanal} &8(best &f{mejor_semanal}&8)'"),
    ("'&8No la pierdas: reclama cada dia.'", "'&8Do not lose it: claim every day.'"),
    ("'&a&l✦ Reclamar todo &8[&a{cantidad}&8]'", "'&a&l✦ Claim everything &8[&a{cantidad}&8]'"),
    ("'&7&lReclamar todo &8[&7NADA PENDIENTE&8]'", "'&7&lClaim everything &8[&7NOTHING PENDING&8]'"),
    ("'&7Tienes &f{cantidad} &7recompensa(s) lista(s).'", "'&7You have &f{cantidad} &7reward(s) ready.'"),
    ("'&a» Clic para reclamarlas todas'", "'&a» Click to claim them all'"),
    ("'&7No hay nada pendiente ahora mismo.'", "'&7There is nothing pending right now.'"),
    ("'&7Vuelve en &f{tiempo}&7.'", "'&7Come back in &f{tiempo}&7.'"),
    ("'&d&lComo funciona'", "'&d&lHow it works'"),
    ("'&7Tu progreso, rachas y reinicios.'", "'&7Your progress, streaks and resets.'"),
    ("'&7Dias del mes: &f{dias_mes}&7/&f{dias_mes_total}&7.'", "'&7Days of the month: &f{dias_mes}&7/&f{dias_mes_total}&7.'"),
    ("'&d» Clic para abrir'", "'&d» Click to open'"),
    ("'&d&l✦ Ciclo diario ✦'", "'&d&l✦ Daily cycle ✦'"),
    ("'&a&lDia {dia} &8[&aHOY&8]'", "'&a&lDay {dia} &8[&aTODAY&8]'"),
    ("'&7&lDia {dia} &8[&7RECLAMADO&8]'", "'&7&lDay {dia} &8[&7CLAIMED&8]'"),
    ("'&7&lDia {dia}'", "'&7&lDay {dia}'"),
    ("'&7Esta es tu recompensa de hoy.'", "'&7This is today''s reward.'"),
    ("'&a» Clic para reclamar'", "'&a» Click to claim'"),
    ("'&7Ya reclamada.'", "'&7Already claimed.'"),
    ("'&8» Vuelve manana'", "'&8» Come back tomorrow'"),
    ("'&7Llega hasta este dia reclamando cada jornada.'", "'&7Reach this day by claiming every day.'"),
    ("'&8» Bloqueado'", "'&8» Locked'"),
    ("'&d&l✦ Ciclo semanal ✦'", "'&d&l✦ Weekly cycle ✦'"),
    ("'&a&lSemana {semana} &8[&aESTA SEMANA&8]'", "'&a&lWeek {semana} &8[&aTHIS WEEK&8]'"),
    ("'&7&lSemana {semana} &8[&7RECLAMADA&8]'", "'&7&lWeek {semana} &8[&7CLAIMED&8]'"),
    ("'&7&lSemana {semana}'", "'&7&lWeek {semana}'"),
    ("'&7Esta es tu recompensa de esta semana.'", "'&7This is this week''s reward.'"),
    ("'&8» Vuelve la semana que viene'", "'&8» Come back next week'"),
    ("'&7Se desbloquea al llegar esta semana del ciclo.'", "'&7It unlocks when you reach this week of the cycle.'"),
    ("'&d&lRacha semanal: &f{racha_semanal}'", "'&d&lWeekly streak: &f{racha_semanal}'"),
    ("'&7Mejor racha: &f{mejor_semanal} &7semana(s).'", "'&7Best streak: &f{mejor_semanal} &7week(s).'"),
    ("'&7Semanas reclamadas: &f{total_semanal}&7.'", "'&7Weeks claimed: &f{total_semanal}&7.'"),
    ("'&7La proxima semana empieza en &f{tiempo_semanal}&7.'", "'&7The next week starts in &f{tiempo_semanal}&7.'"),
    ("'&d&l✦ Recompensa mensual ✦'", "'&d&l✦ Monthly reward ✦'"),
    ("'&d&lProgreso del mes'", "'&d&lMonthly progress'"),
    ("'&7Hitos reclamados: &f{hitos}&7/&f{hitos_total}&7.'", "'&7Milestones claimed: &f{hitos}&7/&f{hitos_total}&7.'"),
    ("'&7Listos para reclamar: &f{hitos_disponibles}&7.'", "'&7Ready to claim: &f{hitos_disponibles}&7.'"),
    ("'&8Los hitos se reinician al cambiar de mes.'", "'&8Milestones reset when the month changes.'"),
    ("'&a&lHito: {dias} dias &8[&aLISTO&8]'", "'&a&lMilestone: {dias} days &8[&aREADY&8]'"),
    ("'&7&lHito: {dias} dias &8[&7BLOQUEADO&8]'", "'&7&lMilestone: {dias} days &8[&7LOCKED&8]'"),
    ("'&7&lHito: {dias} dias &8[&7RECLAMADO&8]'", "'&7&lMilestone: {dias} days &8[&7CLAIMED&8]'"),
    ("'&7Has reclamado &f{dias_mes} &7dia(s) este mes.'", "'&7You claimed &f{dias_mes} &7day(s) this month.'"),
    ("'&a» Clic para reclamar este hito'", "'&a» Click to claim this milestone'"),
    ("'&7Necesitas &f{dias} &7dia(s) reclamados.'", "'&7You need &f{dias} &7day(s) claimed.'"),
    ("'&7Te faltan &f{faltan}&7.'", "'&7You are &f{faltan}&7 short.'"),
    ("'&8» Bloqueado todavia'", "'&8» Still locked'"),
    ("'&7Hito conseguido este mes.'", "'&7Milestone reached this month.'"),
    ("'&8» Ya reclamado'", "'&8» Already claimed'"),
    ("'&d&l✦ Bonus de racha ✦'", "'&d&l✦ Streak bonuses ✦'"),
    ("'&d&lTus rachas'", "'&d&lYour streaks'"),
    ("'&8Los bonus se reclaman una vez por racha.'", "'&8Bonuses can be claimed once per streak.'"),
    ("'&a&lRacha de {racha_minima} &8[&aLISTO&8]'", "'&a&lStreak of {racha_minima} &8[&aREADY&8]'"),
    ("'&7&lRacha de {racha_minima} &8[&7FALTAN {faltan}&8]'", "'&7&lStreak of {racha_minima} &8[&7{faltan} TO GO&8]'"),
    ("'&7&lRacha de {racha_minima} &8[&7RECLAMADO&8]'", "'&7&lStreak of {racha_minima} &8[&7CLAIMED&8]'"),
    ("'&7Tipo: &f{tipo}'", "'&7Type: &f{tipo}'"),
    ("'&a» Clic para reclamar este bonus'", "'&a» Click to claim this bonus'"),
    ("'&7Necesitas una racha de &f{racha_minima} &7{tipo}(s).'", "'&7You need a &f{racha_minima} &7{tipo} streak.'"),
    ("'&7Ya lo reclamaste en esta racha.'", "'&7You already claimed it in this streak.'"),
    ("'&8» Si pierdes la racha, vuelve a estar disponible'", "'&8» If you lose the streak it becomes available again'"),
    ("'&d&l✦ Como funciona ✦'", "'&d&l✦ How it works ✦'"),
    ("'&d&lTu racha'", "'&d&lYour streak'"),
    ("'&7Racha actual: &f{racha} &7dia(s).'", "'&7Current streak: &f{racha} &7day(s).'"),
    ("'&7Mejor racha: &f{mejor} &7dia(s).'", "'&7Best streak: &f{mejor} &7day(s).'"),
    ("'&8Reclama cada dia para subirla'", "'&8Claim every day to keep it growing'"),
    ("'&d&lTus totales'", "'&d&lYour totals'"),
    ("'&7Recompensas diarias: &f{total}&7.'", "'&7Daily rewards: &f{total}&7.'"),
    ("'&7Hitos: &f{hitos}&7/&f{hitos_total}&7. Bonus: &f{bonus}&7/&f{bonus_total}&7.'", "'&7Milestones: &f{hitos}&7/&f{hitos_total}&7. Bonuses: &f{bonus}&7/&f{bonus_total}&7.'"),
    ("'&d&lProximos reinicios'", "'&d&lUpcoming resets'"),
    ("'&7La diaria vuelve en &f{tiempo} &8(a las &f{hora}&8).'", "'&7The daily reward returns in &f{tiempo} &8(at &f{hora}&8).'"),
    ("'&7La semanal vuelve en &f{tiempo_semanal}&7.'", "'&7The weekly reward returns in &f{tiempo_semanal}&7.'"),
    ("'&7Los hitos y su progreso se reinician al cambiar de mes.'", "'&7Milestones and their progress reset when the month changes.'"),
    ("'&8Horarios en config.yml -> reinicio.hora'", "'&8Schedules in config.yml -> reinicio.hora'"),
]


def main():
    texto = FUENTE.read_text(encoding="utf-8")
    marca = "config-version: 2"
    corte = texto.index(marca)
    texto = CABECERA + "\n" + texto[corte:]
    faltan = []
    for viejo, nuevo in REEMPLAZOS:
        if viejo in texto:
            texto = texto.replace(viejo, nuevo)
        else:
            faltan.append(viejo)
    DESTINO.write_text(texto, encoding="utf-8")
    print("menus.yml escrito (%d caracteres, %d reemplazos sin encontrar)" % (len(texto), len(faltan)))
    for f in faltan:
        print("  SIN ENCONTRAR:", f[:80])
    # comprobaciones
    problemas = []
    for i, linea in enumerate(texto.splitlines(), 1):
        if "textura:" in linea and "'" not in linea:
            problemas.append("linea %d sin comillas en textura: %s" % (i, linea[:60]))
        if re.search(r"[áéíóúñ¿¡]", linea) and not linea.strip().startswith("#"):
            problemas.append("linea %d con acentos: %s" % (i, linea[:60]))
    sospechosas = [l for l in texto.splitlines()
                   if not l.strip().startswith("#") and re.search(r"\b(Clic|Pagina|dia|racha|hito|semana|premio|reclamar|Recompensa|Volver|Cerrar)\b", l)]
    print("problemas estructurales:", len(problemas))
    for p in problemas[:10]:
        print("  ", p)
    print("lineas con palabras en espanol:", len(sospechosas))
    for s in sospechosas[:25]:
        print("  ", s.strip()[:110])
    return 0 if not problemas else 1


if __name__ == "__main__":
    sys.exit(main())
