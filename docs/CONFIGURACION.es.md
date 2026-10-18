# Guía de configuración de JPRewards

Esta guía explica, paso a paso, cómo dejar el plugin a tu gusto. Está pensada
para leerse con los archivos abiertos al lado: cada ejemplo es copiable.

- [1. Los cinco archivos](#1-los-cinco-archivos)
- [2. Cambiar horarios y ciclos](#2-cambiar-horarios-y-ciclos)
- [3. Editar los premios](#3-editar-los-premios)
- [4. Premios por rango](#4-premios-por-rango)
- [5. Items, experiencia y efectos](#5-items-experiencia-y-efectos)
- [6. Hitos del mes y bonus de racha](#6-hitos-del-mes-y-bonus-de-racha)
- [7. Personalizar los menús](#7-personalizar-los-menús)
- [8. Textos e idioma](#8-textos-e-idioma)
- [9. Avisos, sonidos y efectos globales](#9-avisos-sonidos-y-efectos-globales)
- [10. Guardar los datos en MySQL](#10-guardar-los-datos-en-mysql)
- [11. Probar sin esperar días](#11-probar-sin-esperar-días)
- [12. Problemas comunes](#12-problemas-comunes)
- [13. Antes de publicar en tu servidor](#13-antes-de-publicar-en-tu-servidor)

## 1. Los cinco archivos

| Archivo | Para qué |
| --- | --- |
| `config.yml` | Cómo funciona el plugin (horarios, avisos, sonidos, almacenamiento). |
| `messages_es.yml` | Todo lo que se lee en el **chat**. |
| `menus.yml` | Todo lo que se ve en los **menús** (títulos, texturas, botones). |
| `recompensas/*.yml` | **Qué se entrega** y cuándo. |
| `datos/` | Datos de los jugadores (no lo edites a mano salvo emergencia). |

Regla de oro: **edita valores, no claves**. Si escribes mal una clave, el plugin
usará el valor por defecto y lo avisará en la consola; para verlo todo junto
usa `/rewards validate`.

## 2. Cambiar horarios y ciclos

```yaml
# config.yml
reinicio:
  hora: 4          # el "día" cambia a las 4 de la mañana (0 = medianoche)

diaria:
  dias-del-ciclo: 0   # 0 = automático: tantos días como tenga diarias.yml
  perder-racha-al-faltar: true

semanal:
  semanas-del-ciclo: 0
  dia-inicio: lunes   # también acepta monday, sábado, saturday...
  perder-racha-al-faltar: true

modulos:
  diaria: true
  semanal: true
  mensual: true
  rachas: true
```

Consecuencias útiles de `reinicio.hora`:

- Quien reclama a las 23:50 y a las 00:10 del día siguiente con `hora: 0` **no**
  mantiene la racha (son días distintos).
- Con `hora: 4`, reclamar a la 01:00 cuenta como el día anterior: perfecto para
  servidores que reinician de madrugada.

## 3. Editar los premios

Cada entrada de `recompensas/diarias.yml` es un día. Lo mínimo es:

```yaml
dias:
  '1':
    lore:
      - '&7▪ &f100 &7monedas'
    comandos:
      - 'eco give {jugador} 100'
```

- La clave (`'1'`) es el número de día. Las comillas evitan que YAML lo lea
  como número raro.
- `lore` es **solo visual**: se muestra donde pongas `{preview}` en
  `menus.yml`. Admite los mismos placeholders que los menús: si escribes
  `&7▪ &f500 &7{moneda}`, el nombre de la moneda sale de
  `config.yml -> economia.moneda` y el mismo archivo sirve para cualquier
  idioma o servidor.
- `comandos` es **lo que entrega**, ejecutado desde consola con
  `{jugador}`, `{uuid}`, `{mundo}` y `{premio}`.

Ejemplos de comandos según tu servidor:

```yaml
comandos:
  - 'eco give {jugador} 500'                                   # EssentialsX
  - 'cmi money add {jugador} 500'                              # CMI
  - 'give {jugador} diamond 3'                                 # vanilla
  - 'xp add {jugador} 100 points'                              # 1.13+
  - 'lp user {jugador} permission settemp essentials.fly true 7d'
  - 'crates give {jugador} votos 1'
  - 'broadcast &d{jugador} &7llegó al día 7!'
```

Para **quitar un día**, borra su bloque y baja los siguientes (o deja el ciclo
más corto). Para **añadir un día**, copia un bloque y usa la clave siguiente.

## 4. Premios por rango

Dos formas, según lo que quieras:

**A) Entrada exclusiva** (solo un rango la puede reclamar):

```yaml
'3':
  permiso: 'group.vip'
  lore: [ '&7▪ &f1000 &7monedas' ]
  comandos: [ 'eco give {jugador} 1000' ]
```

Quien no tenga el permiso verá `&7▪ &cSolo para rangos con permiso especial`
(configurable en `menus.yml -> comun.preview.sin-permiso`).

**B) Variantes** (todos la reclaman, pero cada rango recibe algo distinto):

```yaml
'7':
  lore: [ '&7▪ &f500 &7monedas' ]
  comandos: [ 'eco give {jugador} 500' ]
  rangos:
    mvip:
      permiso: 'group.mvip'      # ¡los rangos altos van PRIMERO!
      icono: { textura: '...' }
      lore: [ '&8Rango MVIP', '&7▪ &f2500 &7monedas' ]
      comandos: [ 'eco give {jugador} 2500' ]
    vip:
      permiso: 'group.vip'
      lore: [ '&8Rango VIP', '&7▪ &f1200 &7monedas' ]
      comandos: [ 'eco give {jugador} 1200' ]
```

Gana la **primera** variante cuyo permiso tenga el jugador, así que ordena de
mayor a menor rango. Lo que no definas en una variante se hereda de la base
(icono, lore, comandos…).

## 5. Items, experiencia y efectos

```yaml
'5':
  lore:
    - '&7▪ &f1x &bDiamante'
    - '&7▪ &f100 &7experiencia'
  items:
    - 'DIAMOND 1'                       # forma corta
    - material: DIAMOND_SWORD           # forma completa
      cantidad: 1
      nombre: '&bFilo del día 5'
      lore: [ '&7Premio por constancia.' ]
      encantamientos: { SHARPNESS: 3, UNBREAKING: 2 }
      item-flags: [ HIDE_ATTRIBUTES ]
      irrompible: true
    - textura: 'eyJ0ZXh0dXJlcyI6...'    # cabeza con textura
      cantidad: 1
    - textura-jugador: 'Notch'          # cabeza de un jugador
  experiencia: 100
  niveles: 2
  efectos:
    sonido: ENTITY_PLAYER_LEVELUP
    volumen: 0.8
    tono: 1.2
    particula: HAPPY_VILLAGER
    particulas: 15
    fuegos: true
    fuegos-colores: [ AQUA, YELLOW ]
    titulo: true
    accion-bar: '&a¡Has recibido tu premio!'
    broadcast: '&d{jugador} &7reclamó el día 5!'
  mensajes:
    - '&7Consejo: usa &f/rewards&7 para ver tu progreso.'
```

Los nombres de materiales, sonidos, encantamientos y partículas pueden ser
modernos (`SHARPNESS`, `HAPPY_VILLAGER`) o de 1.8 (`DAMAGE_ALL`,
`VILLAGER_HAPPY`): el plugin traduce los modernos automáticamente.

## 6. Hitos del mes y bonus de racha

**Hitos** (`recompensas/mensuales.yml`): se desbloquean acumulando días con la
diaria reclamada en el mes actual.

```yaml
hitos:
  hito-15:
    dias-requeridos: 15
    icono: { textura: '...' }
    lore: [ '&7▪ &f1000 &7monedas' ]
    comandos: [ 'eco give {jugador} 1000' ]
```

Mantén los hitos alcanzables en cualquier mes (28 días o menos).

**Bonus de racha** (`recompensas/rachas.yml`): se desbloquean al alcanzar N
días (o semanas) **seguidos** y se reclaman una vez por racha.

```yaml
bonus:
  racha-7:
    tipo: diaria          # o semanal
    racha-minima: 7
    icono: { textura: '...' }
    lore: [ '&7▪ &f1000 &7monedas' ]
    comandos: [ 'eco give {jugador} 1000' ]
```

Si el jugador pierde la racha, los bonus vuelven a estar disponibles.

## 7. Personalizar los menús

Cada menú tiene su sección en `menus.yml`:

```yaml
menu-diario:
  titulo: '&d&l✦ Ciclo diario ✦'
  filas: 5
  slot-inicial: 10       # los días se colocan solos desde aquí (7 por fila)
  navegacion:
    volver-slot: 37
    cerrar-slot: 43
  paginacion:            # se usa solo si no caben en una página
    anterior-slot: 39
    info-slot: 40
    siguiente-slot: 41
  botones:
    dia:
      textura-hoy: '...'
      textura-reclamado: '...'
      nombre-hoy: '&a&lDía {dia} &8[&aHOY&8]'
      lore-hoy: [ '&7Tu recompensa de hoy.', '', '{preview}', '', '&a» Clic para reclamar' ]
      brillo-hoy: true
      accion-hoy: 'claim:daily'
      textura-reclamado: '...'
      nombre-reclamado: '&7&lDía {dia} &8[&7OK&8]'
      lore-reclamado: [ '&7Ya reclamada.' ]
      icono-recompensa: true      # usa la cabeza del premio (tu variante de rango)
```

Cosas que puedes hacer sin miedo:

- **Cambiar texturas**: sustituye el value base64 (los sacas de
  minecraft-heads.com). Si el valor es inválido, `/rewards validate` te lo
  dice.
- **Mover botones**: cambia `slot`. También admite `slots: [10, 11]` o
  `slots: '10-16'`.
- **Añadir botones propios**: agrega una clave nueva dentro de `botones:` con
  su `slot`, `nombre`, `lore` y `accion`. El plugin lo dibuja igual.
- **Crear menús propios**: copia una sección `menu-<algo>` con sus botones y
  ábrela con `open:menu-<algo>` desde cualquier botón.
- **Ocultar un botón según el permiso**: `permiso: 'tu.permiso'` +
  `ocultar-sin-permiso: true`.

## 8. Textos e idioma

**El plugin viene en inglés por defecto** (chat, menús y premios de ejemplo).
Para pasarlo TODO a español, cambia estas seis líneas de `config.yml` y ejecuta
`/rewards reload todo`:

```yaml
idioma: es
menus:
  archivo: 'menus_es.yml'
diaria:
  archivo: 'recompensas/diarias_es.yml'
semanal:
  archivo: 'recompensas/semanales_es.yml'
mensual:
  archivo: 'recompensas/mensuales_es.yml'
rachas:
  archivo: 'recompensas/rachas_es.yml'
```

Los archivos `_es.yml` (menús y premios en español) vienen dentro del .jar y se
crean solos en la carpeta del plugin al arrancar. Puedes editarlos igual que los
ingleses: son tus archivos.

- **Chat**: `messages_<idioma>.yml` (se elige con `config.yml -> idioma`; vienen
  `en` y `es`).
- **Menús**: `menus.yml` (inglés) o `menus_es.yml` (español), según
  `menus.archivo`.
- **Premios**: `recompensas/diarias.yml`… o sus versiones `_es`, según
  `diaria.archivo`, `semanal.archivo`, `mensual.archivo` y `rachas.archivo`.
- **Mensajes vacíos**: si dejas una clave en `''`, ese mensaje no se envía
  (útil para silenciar algo sin borrar la clave).
- **Prefijo**: se define una vez en `prefijo` y se añade solo; si escribes
  `{prefijo}` dentro de un mensaje, se sustituye en su sitio.
- **Tu propio idioma**: copia `messages_es.yml` como `messages_pt.yml`,
  traduce y pon `idioma: pt`. Si te falta alguna clave, el plugin usa la
  versión inglesa incluida en el .jar (nunca muestra un hueco).
- **PlaceholderAPI**: puedes usar `%placeholders% de otros plugins dentro de
  cualquier texto del plugin.

## 9. Avisos, sonidos y efectos globales

```yaml
avisos:
  al-entrar:
    diaria: true
    semanal: true
    hitos: true
    rachas: true
    retraso-segundos: 3
    tipo: 'actionbar'      # o 'chat'
  auto-reclamar:
    activado: false        # ojo: revisa el permiso jprewards.autoclaim
    retraso-segundos: 5

sonidos:
  activado: true
  volumen: 0.7
  tono: 1.4

efectos:
  titulos: true
  particulas: true
  fuegos-artificiales: false

barra-progreso:
  largo: 20
  lleno: '|'
  vacio: '|'
  color-lleno: '&a'
  color-vacio: '&7'
```

## 10. Guardar los datos en MySQL

1. Crea una base de datos vacía (por ejemplo `jprewards`).
2. En `config.yml`:

```yaml
almacenamiento:
  tipo: mysql
  mysql:
    host: '127.0.0.1'
    puerto: 3306
    base-de-datos: 'jprewards'
    usuario: 'usuario'
    contrasena: 'contrasena'
    prefijo-tablas: 'jprewards_'
```

3. Reinicia el servidor. La tabla `jprewards_jugadores` se crea sola.
4. Copia los datos actuales: entra como administrador y ejecuta
   `/rewards migrate confirmar`.
5. Si algo falla, la consola lo explica y el plugin sigue con YAML: **nunca**
   se queda sin datos.

## 11. Probar sin esperar días

- `/rewards test diaria 7` — muestra los comandos e items del día 7 sin
  entregar nada.
- `/rewards give <jugador> diaria 7` — entrega ese premio de verdad (sin
  tocar contadores ni esperas).
- `/rewards set <jugador> racha 6` — deja al jugador a un día del bonus.
- `/rewards set <jugador> dias-mes 24` — desbloquea el hito de 25 días.
- `/rewards reset <jugador> diaria` — vuelve a empezar.
- `/rewards reset * todo` — reinicia a todo el servidor (pide cuidado).
- `/rewards reload recompensas` — aplica los cambios de los YAML sin
  reiniciar.

## 12. Problemas comunes

| Síntoma | Causa habitual | Solución |
| --- | --- | --- |
| Un premio no entrega nada | El comando es de un plugin que no está, o el material no existe | `/rewards test <tipo> <clave>` y mira la consola |
| El menú no muestra un botón | `slot` fuera del cofre o el botón tiene `permiso` tuyo | `/rewards validate` |
| Los acentos salen raros | Archivo guardado en otra codificación | Guarda el YAML en **UTF-8** |
| "No se pudo leer config.yml" | YAML mal indentado o con tabuladores | Usa espacios (2 por nivel) y `/rewards validate` |
| No suena nada | El nombre del sonido no existe en tu versión | Prueba los nombres modernos de la lista del README |
| La racha se reinicia sola | `reinicio.hora` corta antes de que reclamen | Baja la hora de corte (por ejemplo 4) |
| Un jugador reclama dos veces | Otro plugin de recompensas dando el mismo premio | Revisa tus otros plugins |
| MySQL no conecta | Datos mal escritos o driver ausente | Consola al arrancar; el plugin sigue en YAML |

## 13. Antes de publicar en tu servidor

1. `/rewards validate` sin errores.
2. Abrir cada menú (`/rewards`, `diaria`, `semanal`, `mensual`, `rachas`,
   `info`) y comprobar que se ve bien.
3. Reclamar una recompensa de cada tipo y mirar el resultado real (chat, título,
   sonido e items).
4. `/rewards reload todo` y volver a abrir un menú (los menús abiertos se
   cierran al recargar).
5. Reiniciar el servidor y comprobar que el progreso sigue guardado.
6. Revisar los permisos de tus rangos (LuckPerms y similares).
