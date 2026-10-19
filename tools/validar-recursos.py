#!/usr/bin/env python3
"""Valida los recursos de JPRewards (YAML y coherencia de claves/placeholders).

Uso:  python tools/validar-recursos.py
No necesita instalar nada mas que PyYAML.
"""
import re
import sys
from pathlib import Path

import yaml

RAIZ = Path(__file__).resolve().parent.parent
RECURSOS = RAIZ / "src" / "main" / "resources"
JAVA = RAIZ / "src" / "main" / "java"

errores = []
avisos = []


class LoaderSinDuplicados(yaml.SafeLoader):
    """Detecta claves repetidas en el mismo nivel (YAML invalido en la practica)."""


def _construct_mapping(loader, node, deep=False):
    claves = set()
    for clave_nodo, _ in node.value:
        clave = loader.construct_object(clave_nodo, deep=deep)
        if clave in claves:
            raise yaml.constructor.ConstructorError(
                None, None, "clave duplicada: %r (linea %d)" % (clave, clave_nodo.start_mark.line + 1),
                clave_nodo.start_mark)
        claves.add(clave)
    return yaml.SafeLoader.construct_mapping(loader, node, deep)


LoaderSinDuplicados.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, _construct_mapping)


def cargar(ruta):
    try:
        with ruta.open(encoding="utf-8") as f:
            return yaml.load(f, Loader=LoaderSinDuplicados)
    except yaml.YAMLError as e:
        errores.append("%s: YAML invalido -> %s" % (ruta.relative_to(RAIZ), str(e).replace("\n", " ")))
        return None


def claves_planas(datos, prefijo=""):
    salida = {}
    if isinstance(datos, dict):
        for clave, valor in datos.items():
            ruta = ("%s.%s" % (prefijo, clave)) if prefijo else str(clave)
            if isinstance(valor, dict):
                salida.update(claves_planas(valor, ruta))
            else:
                salida[ruta] = valor
    return salida


# ---------------------------------------------------------------- 1. YAML
archivos = {}
for ruta in sorted(RECURSOS.rglob("*.yml")):
    datos = cargar(ruta)
    if datos is None:
        continue
    archivos[ruta.relative_to(RECURSOS).as_posix()] = datos
    print("OK  %-38s %d claves de primer nivel" % (ruta.relative_to(RECURSOS).as_posix(), len(datos)))
    if ruta.name != "plugin.yml":
        version = datos.get("config-version")
        if version is None:
            errores.append("%s: falta config-version" % ruta.relative_to(RECURSOS).as_posix())
        elif int(version) < 2:
            errores.append("%s: config-version=%s (se esperaba 2 o mas)" % (ruta.relative_to(RECURSOS).as_posix(), version))

# ------------------------------------------------- 2. config.yml vs codigo
ajustes = (JAVA / "me/jp/jprewards/config/Ajustes.java").read_text(encoding="utf-8")
config = archivos.get("config.yml", {})
planas_config = claves_planas(config)
leidas = set(re.findall(r'(?:config\.get(?:String|Int|Boolean|Double|StringList)|texto)\(\s*"([a-zA-Z0-9.\-]+)"', ajustes))
leidas |= set(re.findall(r'(?:limitar|limitarDecimal)\([^,]+,[^,]+,[^,]+,\s*"([a-zA-Z0-9.\-]+)"', ajustes))
leidas.discard("config-version")
faltan = sorted(c for c in leidas if c not in planas_config and not c.startswith("almacenamiento.mysql"))
if faltan:
    errores.append("config.yml no define claves que el codigo lee: %s" % ", ".join(faltan))
mysql = (JAVA / "me/jp/jprewards/storage/AjustesSql.java").read_text(encoding="utf-8")
for clave in re.findall(r'seccion\.get(?:String|Int|Double)\("([a-zA-Z0-9.\-]+)"', mysql):
    if "almacenamiento.mysql." + clave not in planas_config:
        errores.append("config.yml no define almacenamiento.mysql.%s" % clave)

# ------------------------------------- 3. mensajes: claves usadas vs archivos
usadas = set()
for java in JAVA.rglob("*.java"):
    texto = java.read_text(encoding="utf-8")
    for m in re.finditer(r'"([a-z][a-zA-Z0-9_.\-]*)"', texto):
        valor = m.group(1)
        if "." in valor and valor.split(".")[0] in (
                "general", "comando", "diaria", "semanal", "mensual", "rachas", "recompensa", "titulos", "avisos"):
            # Las claves que tambien existen en config.yml son opciones de
            # configuracion, no mensajes.
            if valor not in planas_config:
                usadas.add(valor)
for idioma in ("es", "en"):
    datos = archivos.get("messages_%s.yml" % idioma, {})
    planas = claves_planas(datos)
    sin_definir = sorted(k for k in usadas if k not in planas)
    if sin_definir:
        errores.append("messages_%s.yml no define: %s" % (idioma, ", ".join(sin_definir)))
es = claves_planas(archivos.get("messages_es.yml", {}))
en = claves_planas(archivos.get("messages_en.yml", {}))
solo_es = sorted(set(es) - set(en))
solo_en = sorted(set(en) - set(es))
if solo_es:
    avisos.append("claves solo en espanol: %s" % ", ".join(solo_es))
if solo_en:
    avisos.append("claves solo en ingles: %s" % ", ".join(solo_en))

# ------------------------------------------------- 4. menus.yml coherencia
menus = archivos.get("menus.yml", {})
ids_menus = set(k for k in menus if str(k).startswith("menu-"))
# Nombres canonicos en ingles + los heredados de la 1.x (se siguen aceptando).
ACCIONES_OK = ("open:", "close", "none", "claim:", "command:", "player-command:", "message:", "sound:", "page:",
               "abrir:", "cerrar", "nada", "ninguna", "reclamar:", "comando:", "consola:", "jugador-comando:",
               "mensaje:", "sonido:", "pagina:")
for menu_id, menu in menus.items():
    if not str(menu_id).startswith("menu-") or not isinstance(menu, dict):
        continue
    filas = int(menu.get("filas", 5))
    tamano = filas * 9
    for boton_id, boton in (menu.get("botones") or {}).items():
        if not isinstance(boton, dict):
            continue
        ruta = "%s.botones.%s" % (menu_id, boton_id)
        if "slot" in boton and not (0 <= int(boton["slot"]) < tamano):
            errores.append("menus.yml -> %s.slot=%s esta fuera del cofre (%d slots)" % (ruta, boton["slot"], tamano))
        for clave, valor in boton.items():
            if clave.startswith("accion") and isinstance(valor, str):
                for accion in valor.split(";"):
                    accion = accion.strip()
                    if not accion:
                        continue
                    if not accion.startswith(ACCIONES_OK):
                        errores.append("menus.yml -> %s.%s: accion '%s' no reconocida" % (ruta, clave, accion))
                    destino = None
                    for prefijo in ("open:", "abrir:"):
                        if accion.startswith(prefijo):
                            destino = accion[len(prefijo):].strip()
                    if destino is not None:
                        if not destino.startswith("menu-"):
                            destino = "menu-" + destino
                        if destino not in ids_menus:
                            errores.append("menus.yml -> %s.%s: el menu '%s' no existe" % (ruta, clave, destino))
                    for prefijo in ("claim:milestone:", "reclamar:hito:", "claim:streak:", "reclamar:racha:"):
                        if accion.startswith(prefijo):
                            identificador = accion[len(prefijo):].strip()
                            clave_archivo = "recompensas/mensuales.yml" if "milestone" in prefijo or "hito" in prefijo else "recompensas/rachas.yml"
                            seccion = "hitos" if "milestone" in prefijo or "hito" in prefijo else "bonus"
                            datos = archivos.get(clave_archivo, {}).get(seccion, {})
                            if datos and identificador not in datos:
                                errores.append("menus.yml -> %s.%s: '%s' no existe en %s" % (ruta, clave, identificador, clave_archivo))
        for estado in ("disponible", "bloqueado", "bloqueada", "reclamado", "hoy", "completada", "nada"):
            if "accion-" + estado in boton:
                for accion in str(boton["accion-" + estado]).split(";"):
                    accion = accion.strip()
                    if accion and not accion.startswith(ACCIONES_OK):
                        errores.append("menus.yml -> %s.accion-%s: '%s' no reconocida" % (ruta, estado, accion))
for obligatorio in ("comun", "menu-principal", "menu-diario", "menu-semanal", "menu-mensual", "menu-rachas", "menu-info"):
    if obligatorio not in menus:
        errores.append("menus.yml no define '%s'" % obligatorio)

# ------------------------------------------------- 5. recompensas/*.yml
diarias = archivos.get("recompensas/diarias.yml", {}).get("dias", {})
semanales = archivos.get("recompensas/semanales.yml", {}).get("semanas", {})
mensuales = archivos.get("recompensas/mensuales.yml", {}).get("hitos", {})
rachas = archivos.get("recompensas/rachas.yml", {}).get("bonus", {})
for etiqueta, conjunto in (("dias", diarias), ("semanas", semanales)):
    for clave in conjunto:
        if not str(clave).isdigit():
            errores.append("recompensas: la clave %r no es un numero" % clave)
for hito_id, hito in mensuales.items():
    if "dias-requeridos" not in hito:
        errores.append("mensuales.yml -> %s: falta dias-requeridos" % hito_id)
    elif int(hito["dias-requeridos"]) > 28:
        avisos.append("mensuales.yml -> %s: %s dias no se alcanzan en febrero" % (hito_id, hito["dias-requeridos"]))
for bonus_id, bonus in rachas.items():
    if "racha-minima" not in bonus:
        errores.append("rachas.yml -> %s: falta racha-minima" % bonus_id)
    if bonus.get("tipo", "diaria") not in ("diaria", "semanal"):
        errores.append("rachas.yml -> %s: tipo invalido %r" % (bonus_id, bonus.get("tipo")))

# entradas sin premio
def revisar_entradas(conjunto, archivo):
    for clave, entrada in conjunto.items():
        if not isinstance(entrada, dict):
            continue
        tiene = any(entrada.get(c) for c in ("comandos", "items", "experiencia", "niveles"))
        if not tiene and not entrada.get("rangos"):
            avisos.append("%s -> %s no entrega nada" % (archivo, clave))


revisar_entradas(diarias, "diarias.yml")
revisar_entradas(semanales, "semanales.yml")
revisar_entradas(mensuales, "mensuales.yml")
revisar_entradas(rachas, "rachas.yml")

# ------------------------------------------------- 6. placeholders de menus
conocidos = {
    "jugador", "uuid", "mundo", "premio", "recompensa", "dia", "dias", "faltan", "estado", "cantidad",
    "racha", "mejor", "total", "racha_minima", "racha_semanal", "mejor_semanal", "total_semanal", "semana",
    "semanas", "dias_mes", "dias_mes_total", "hitos", "hitos_total", "hitos_disponibles", "bonus",
    "bonus_total", "bonus_disponibles", "pendientes", "tiempo", "tiempo_largo", "tiempo_semanal", "hora",
    "moneda", "barra_mes", "barra_ciclo", "barra_racha", "barra_racha_semanal", "version", "servidor",
    "almacenamiento", "modulos", "ultimo", "primero", "top", "pagina", "paginas", "preview", "prefijo",
    "modulo", "item", "actual", "entrada", "archivos", "texto", "tipos", "campos", "clave", "valor",
    "puesto", "errores", "avisos", "copiados", "omitidos", "origen", "destino", "error", "hito", "proximo",
    "racha_necesaria", "tipo", "ruta", "cargados", "autoguardado", "tipo_almacenamiento",
    "idioma", "campo", "destinos", "claves", "archivo", "linea",
}
for nombre, datos in archivos.items():
    if nombre == "config.yml":
        continue
    for ruta, valor in claves_planas(datos).items():
        textos = valor if isinstance(valor, list) else [valor]
        for texto in textos:
            if not isinstance(texto, str):
                continue
            for ph in re.findall(r"\{([a-zA-Z0-9_]+)\}", texto):
                if ph not in conocidos:
                    avisos.append("%s -> %s: placeholder {%s} desconocido" % (nombre, ruta, ph))

# ---------------------------------------------------------------- salida
print()
for aviso in avisos:
    print("AVISO  " + aviso)
for error in errores:
    print("ERROR  " + error)
print()
print("RESULTADO: %d error(es), %d aviso(s)" % (len(errores), len(avisos)))
sys.exit(1 if errores else 0)
