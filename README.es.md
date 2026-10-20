# JPRewards

**Recompensas diarias, semanales, mensuales y por racha con menús de cabezas
totalmente configurables.** Un solo `.jar` para Minecraft **1.8 hasta la última
versión**, sin NMS, sin dependencias y sin código ofuscado.

![JPRewards](docs/assets/banner.png)

> Los textos de este README también existen en inglés: [README.md](README.md).
> Toda la documentación: [docs/](docs/README.es.md) · Guía paso a paso:
> [docs/CONFIGURACION.es.md](docs/CONFIGURACION.es.md) · Arte generado por código:
> [docs/assets/README.md](docs/assets/README.md).

- [Qué trae](#qué-trae)
- [Documentación](#documentación)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Comandos](#comandos)
- [Permisos](#permisos)
- [Archivos de configuración](#archivos-de-configuración)
- [Recetas rápidas](#recetas-rápidas)
- [Campos de una recompensa](#campos-de-una-recompensa)
- [Menús](#menús)
- [Placeholders](#placeholders)
- [Almacenamiento y migración](#almacenamiento-y-migración)
- [Diagnóstico: validar, estado y test](#diagnóstico-validar-estado-y-test)
- [Actualización de archivos](#actualización-de-archivos)
- [Compilar desde el código](#compilar-desde-el-código)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Licencia](#licencia)

## Documentación

| Página | Qué cubre |
| --- | --- |
| [docs/](docs/README.es.md) | Índice de todas las guías y referencias. |
| [Comandos y permisos](docs/COMMANDS.md) | Cada subcomando, alias, argumento, permiso y ejemplo. |
| [Placeholders](docs/PLACEHOLDERS.md) | Placeholders de menús, mensajes y recompensas, y la expansión de PlaceholderAPI. |
| [Preguntas frecuentes](docs/FAQ.md) | Idiomas, MySQL, actualizar desde la 1.x, reclamar dos veces, inventario lleno. |
| [Guía de configuración](docs/CONFIGURACION.es.md) | Guía paso a paso de los archivos YAML. |
| [Registro de verificación](docs/VERIFICATION.md) | Qué se probó, en qué servidores y el hash del artefacto. |
| [Contribuir](CONTRIBUTING.md) · [Seguridad](SECURITY.md) | Cómo enviar un cambio y cómo reportar una vulnerabilidad. |

## Qué trae

- **Cuatro sistemas de recompensas** que se pueden activar por separado:
  - **Diaria**: ciclo de N días (`recompensas/diarias.yml`), con racha y
    opción de perderla al faltar.
  - **Semanal**: ciclo de N semanas, con día de inicio configurable y racha
    semanal propia.
  - **Mensual**: hitos que se desbloquean acumulando días reclamados dentro
    del mes; se reinician solos al cambiar de mes.
  - **Rachas**: bonus al alcanzar N días (o semanas) seguidos, reclamables una
    vez por racha.
- **Entrega flexible**: comandos de consola, **items directos** (con nombre,
  lore, encantamientos, flags, model data, cabezas con textura…) y
  **experiencia**.
- **Menús de cabezas** con texturas personalizadas, estados por jugador
  (disponible / reclamado / bloqueado / completada), vista previa de premios,
  paginación automática y navegación configurables.
- **Variantes por rango**: la misma recompensa puede dar y mostrar cosas
  distintas según el permiso que tenga el jugador.
- **Todo configurable por YAML**, con comentarios en español e inglés.
- **Auto-actualización de los archivos**: al actualizar el plugin, las opciones
  nuevas se agregan solas con sus comentarios y sin pisar nada tuyo; los
  archivos que ya están al día no se reescriben nunca (ni al recargar).
- **/rewards validate**: revisa todos los archivos y te dice exactamente qué
  está mal y dónde.
- **Almacenamiento en YAML o MySQL** (JDBC), con migración en los dos sentidos.
- **PlaceholderAPI opcional** y **aviso de versión nueva opcional**.
- **Inglés por defecto y español incluido**: el chat, los menús y los premios de ejemplo vienen en inglés, y hay presets completos en español (`menus_es.yml` y `recompensas/*_es.yml`) que se activan con unas pocas líneas de `config.yml`. Además puedes añadir cualquier otro idioma.
- Compatible con **Spigot, Paper, Purpur** y derivados; funciona igual en
  servidores con Java 8 y con Java 21.

## Galería

Todas las imágenes se generan con código (ver [docs/assets/README.md](docs/assets/README.md)); en
[docs/SPIGOTMC_PAGE.md](docs/SPIGOTMC_PAGE.md) está dónde va cada una en la página del recurso.

| | |
| --- | --- |
| ![Qué trae](docs/assets/features.png) | ![Comandos](docs/assets/commands.png) |
| ![Permisos](docs/assets/permissions.png) | ![Instalación](docs/assets/setup.png) |
| ![Configuración](docs/assets/configuration.png) | ![Diagnóstico](docs/assets/diagnostics.png) |
| ![Compatibilidad](docs/assets/compatibility.png) | |

## Requisitos

| | |
| --- | --- |
| Servidor | Spigot / Paper / Purpur 1.8 - última versión |
| Java | 8 o superior (el plugin es bytecode Java 8) |
| Dependencias | Ninguna. PlaceholderAPI es opcional |
| Base de datos | Opcional (MySQL/MariaDB). El driver lo trae el servidor |

## Instalación

1. Copia `JPRewards-2.0.0.jar` en la carpeta `plugins/` del servidor.
2. Arranca el servidor: se crean `config.yml`, `messages_es.yml`,
   `messages_en.yml`, `messages_es.yml`, `menus.yml`, `menus_es.yml` y la carpeta `recompensas/` (con los presets `_es.yml`).
   Los archivos existentes **nunca** se sobrescriben.
3. Ejecuta `/rewards validate` para comprobar que todo está bien.
4. Abre el menú con `/rewards` y ajusta lo que quieras.

> Si vienes de la versión 1.x: tu `messages.yml` se renombra solo a
> `messages_es.yml`, se añade `idioma: es` a tu `config.yml` (para que el chat
> siga en español, porque el inglés por defecto es solo para instalaciones
> nuevas) y **no se pierde nada**, ni los datos de los jugadores. Las opciones
> nuevas se añaden a tus archivos con una copia `.bak` de seguridad.

## Comandos

Alias del comando: `rewards`, `jprewards`, `rec`, `recompensa`.

### Jugadores

| Comando | Qué hace |
| --- | --- |
| `/rewards` | Abre el menú principal. |
| `/rewards daily` | Abre el ciclo diario. |
| `/rewards weekly` | Abre el ciclo de semanas. |
| `/rewards monthly` | Abre los hitos del mes. |
| `/rewards streaks` | Abre los bonus de racha. |
| `/rewards info` | Tu progreso y cómo funciona. |
| `/rewards claim <diaria\|semanal\|mensual\|rachas\|todo>` | Reclama sin abrir el menú. |
| `/rewards top [racha\|total\|semanas\|hitos\|bonus]` | Ranking del servidor. |
| `/rewards help` | Ayuda en el chat. |

### Administración

| Comando | Qué hace |
| --- | --- |
| `/rewards status` | Estado del plugin, del almacenamiento y de la configuración. |
| `/rewards validate` | Revisa TODOS los archivos y explica cada error con su ruta. |
| `/rewards reload [config\|mensajes\|menus\|recompensas\|todo]` | Recarga en caliente. |
| `/rewards data <jugador>` | Informe completo de un jugador. |
| `/rewards give <jugador> <tipo> [clave]` | Entrega un premio sin tocar contadores (pruebas). |
| `/rewards test <tipo> [clave]` | **Simulacro**: muestra qué se entregaría, sin entregarlo. |
| `/rewards reset <jugador\|*> [tipo]` | Reinicia datos de uno o de todos. |
| `/rewards set <jugador> <campo> <valor>` | Fija un contador (racha, total, días del mes…). |
| `/rewards migrate confirmar` | Copia los datos entre YAML y MySQL. |

## Permisos

| Permiso | Por defecto | Para qué |
| --- | --- | --- |
| `jprewards.use` | todos | Abrir los menús y usar el comando. |
| `jprewards.claim.daily` | todos | Reclamar la recompensa diaria. |
| `jprewards.claim.weekly` | todos | Reclamar la semanal. |
| `jprewards.claim.monthly` | todos | Reclamar los hitos del mes. |
| `jprewards.claim.streak` | todos | Reclamar los bonus de racha. |
| `jprewards.notify` | todos | Recibir el aviso al entrar. |
| `jprewards.autoclaim` | nadie | Reclamo automático de la diaria (si está activado). |
| `jprewards.top` | todos | Ver el ranking con `/rewards top`. |
| `jprewards.update` | operadores | Aviso de versión nueva en el chat. |
| `jprewards.admin` | operadores | `status`, `validate`, `reload`, `data`, `top`. |
| `jprewards.admin.manage` | operadores | `give`, `test`, `reset`, `set`. |
| `jprewards.admin.migrate` | operadores | `migrate`. |
| `jprewards.*` | operadores | Todo lo anterior. |

## Archivos de configuración

Todos viven en `plugins/JPRewards/`. Las opciones llevan comentarios en español e inglés; los archivos `_es` son los presets en español que se activan desde `config.yml`.

| Archivo | Qué se configura |
| --- | --- |
| `config.yml` | Módulos activos, hora de reinicio, avisos, sonidos, efectos, barras, almacenamiento, actualizaciones. |
| `messages_es.yml` / `messages_en.yml` | Todos los textos del chat. Se elige con `config.yml -> idioma`. |
| `menus.yml` / `menus_es.yml` | Títulos, texturas, botones, estados, paginación y acciones de los menús (inglés / preset en español). |
| `recompensas/diarias.yml` | Los días del ciclo diario. |
| `recompensas/semanales.yml` | Los bloques del ciclo semanal. |
| `recompensas/mensuales.yml` | Los hitos del mes. |
| `recompensas/rachas.yml` | Los bonus por racha. |

## Recetas rápidas

**Cambiar la hora a la que empieza el día** (`config.yml`):

```yaml
reinicio:
  hora: 4      # a las 4 de la mañana
```

**Desactivar la recompensa semanal**:

```yaml
modulos:
  semanal: false
```

**Que los premios diarios sean solo para un rango** (`recompensas/diarias.yml`):

```yaml
'3':
  permiso: 'jprewards.rewards.vip'   # solo quien tenga este permiso
  lore: [ '&7▪ &f500 &7monedas' ]
  comandos: [ 'eco give {jugador} 500' ]
```

**Premios distintos según el rango** (variantes):

```yaml
'7':
  lore: [ '&7▪ &f500 &7monedas' ]
  comandos: [ 'eco give {jugador} 500' ]
  rangos:
    mvip:
      permiso: 'group.mvip'          # pon los rangos altos PRIMERO
      lore: [ '&8Rango MVIP', '&7▪ &f2500 &7monedas' ]
      comandos: [ 'eco give {jugador} 2500' ]
    vip:
      permiso: 'group.vip'
      lore: [ '&8Rango VIP', '&7▪ &f1200 &7monedas' ]
      comandos: [ 'eco give {jugador} 1200' ]
```

**Dar items sin comandos**:

```yaml
items:
  - 'DIAMOND 3'                     # forma corta: material y cantidad
  - material: DIAMOND_SWORD         # forma completa
    cantidad: 1
    nombre: '&bFilo del día 6'
    encantamientos: { SHARPNESS: 2, UNBREAKING: 1 }
    item-flags: [ HIDE_ATTRIBUTES ]
    irrompible: true
```

**Añadir un anuncio y efectos a un premio**:

```yaml
efectos:
  sonido: ENTITY_PLAYER_LEVELUP
  particula: HAPPY_VILLAGER
  fuegos: true
  accion-bar: '&a¡Ciclo completado!'
  broadcast: '&d{jugador} &7ha completado el ciclo semanal!'
mensajes:
  - '&7Vuelve mañana para empezar otro ciclo.'
```

**Activar MySQL** (`config.yml`):

```yaml
almacenamiento:
  tipo: mysql
  mysql:
    host: '127.0.0.1'
    base-de-datos: 'jprewards'
    usuario: 'root'
    contrasena: 'tu-contrasena'
```

Después, reinicia y ejecuta `/rewards migrate confirmar` para copiar tus
datos actuales a la base de datos.

**Pasar TODO el plugin a español** (chat, menús y premios de ejemplo): el idioma
por defecto es inglés, así que cambia estas seis líneas de `config.yml` y ejecuta
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

Los presets `_es.yml` se crean solos en la carpeta del plugin al arrancar (vienen
dentro del .jar). Si prefieres otro idioma, copia `messages_en.yml` a
`messages_<código>.yml`, tradúcelo y ajusta `idioma`.

## Campos de una recompensa

Cada entrada de `recompensas/*.yml` admite:

| Campo | Descripción |
| --- | --- |
| `icono.textura` | Cabeza del menú (value base64 de minecraft-heads.com). |
| `icono.material` | Material en vez de cabeza (`CHEST`, `PAPER`…). |
| `icono.textura-jugador` | Cabeza de un jugador por nombre. |
| `nombre` | Nombre visible en mensajes, títulos y anuncios (`{premio}`). |
| `permiso` | Si se define, la entrada es solo para quien tenga ese permiso. |
| `lore` | Cómo se ve el premio donde pongas `{preview}`. Admite los mismos placeholders que los menús (por ejemplo `{moneda}`), así el mismo archivo sirve para cualquier idioma. |
| `comandos` | Comandos de consola: `{jugador}`, `{uuid}`, `{mundo}`, `{premio}`. |
| `items` | Items que se entregan directamente. |
| `experiencia` / `niveles` | XP directa. |
| `mensajes` | Líneas extra de chat al reclamar. |
| `efectos` | `sonido`, `volumen`, `tono`, `particula`, `particulas`, `fuegos`, `fuegos-colores`, `fuegos-potencia`, `titulo`, `accion-bar`, `broadcast`. |
| `rangos` | Variantes por permiso: gana la primera cuyo permiso tenga el jugador; lo que no definas se hereda de la base. |

## Menús

Todo el aspecto se define en `menus.yml`. Cada botón admite: `slot`/`slots`
(`10`, `[10, 11]` o `'10-16'`), `material`, `textura`, `textura-jugador`,
`cantidad`, `nombre`, `lore`, `brillo`, `encantamientos`, `item-flags`,
`custom-model-data`, `irrompible`, `icono-recompensa`, `permiso`,
`ocultar-sin-permiso` y `accion`.

**Estados** (se eligen solos según los datos del jugador): `hoy`,
`disponible`, `reclamado`/`reclamada`, `bloqueado`/`bloqueada`,
`completada`, `none`. Para cada estado puedes definir
`nombre-<estado>`, `lore-<estado>`, `textura-<estado>`, `brillo-<estado>` y
`accion-<estado>`.

**Acciones**: `open:<menu>`, `close`, `none`, `claim:daily`,
`claim:weekly`, `claim:monthly`, `claim:streaks`, `claim:all`,
`claim:milestone:<id>`, `claim:streak:<id>`, `command:<cmd>`,
`jugador-command:<cmd>`, `message:<key>`, `sound:<name>`.
Varias acciones se separan con `;` y se ejecutan en orden.

También puedes **crear menús propios**: cualquier sección `menu-<algo>` con
`botones:` se abre con `open:menu-<algo>` (o desde un botón de otro menú).

## Placeholders

En `menus.yml`, en los mensajes y también en los `lore` de `recompensas/*.yml`:

`{jugador}` `{uuid}` `{mundo}` `{premio}` `{recompensa}` `{dia}`
`{dias}` `{racha}` `{mejor}` `{total}` `{semana}` `{semanas}`
`{racha_semanal}` `{mejor_semanal}` `{total_semanal}` `{dias_mes}`
`{dias_mes_total}` `{hitos}` `{hitos_total}` `{hitos_disponibles}`
`{bonus}` `{bonus_total}` `{bonus_disponibles}` `{pendientes}`
`{faltan}` `{racha_minima}` `{tiempo}` `{tiempo_largo}`
`{tiempo_semanal}` `{hora}` `{moneda}` `{barra_mes}` `{barra_ciclo}`
`{barra_racha}` `{barra_racha_semanal}` `{version}` `{servidor}`
`{almacenamiento}` `{modulos}`.

Y con **PlaceholderAPI** instalado: `%jprewards_racha%`, `%jprewards_dia%`,
`%jprewards_total%`, `%jprewards_racha_semanal%`, `%jprewards_dias_mes%`,
`%jprewards_hitos_disponibles%`, `%jprewards_bonus_disponibles%`,
`%jprewards_disponibles%`, `%jprewards_disponible_diaria%`,
`%jprewards_tiempo_diaria%`, `%jprewards_premio_diaria%`… (lista completa en
`docs/SPIGOTMC_PAGE.md`). Los placeholders de otros plugins también funcionan
dentro de los textos del plugin.

## Almacenamiento y migración

- `almacenamiento.tipo: yaml` (por defecto): un archivo `<uuid>.yml` por
  jugador, o todos en `datos/datos.yml` con `modo: unico`.
- `almacenamiento.tipo: mysql`: una sola tabla (`<prefijo>jugadores`) con los
  datos serializados, para redes de servidores. **No hace falta descargar nada**:
  se usa el driver JDBC que ya trae Spigot/Paper.
- **Migrar** en cualquier sentido: `/rewards migrate confirmar`.
- Si MySQL no responde al arrancar, el plugin **sigue funcionando con YAML** y
  lo avisa por consola (nunca deja a los jugadores sin recompensas).

## Diagnóstico: validar, estado y test

- `/rewards validate` revisa YAML, materiales, texturas, sonidos,
  partículas, encantamientos, slots, acciones, permisos, placeholders y claves
  que falten, indicando **archivo y clave** en cada aviso.
- `/rewards test diaria 7` (por ejemplo) muestra exactamente qué comandos
  e items se entregarían con esa entrada, sin entregar nada.
- `/rewards status` resume versión, idioma, almacenamiento, cuántos días,
  semanas, hitos y bonus hay cargados.

## Actualización de archivos

Al actualizar el plugin, los archivos se actualizan solos:

1. Solo se ejecuta el auto-merge si el `config-version` de tu archivo es
   **menor** que el del plugin. Un archivo que ya está al día no se reescribe
   nunca: `/rewards reload` solo lo vuelve a leer, así que las opciones (o los
   textos y premios de ejemplo) que borraste a propósito no reaparecen.
2. Se guarda una copia `<archivo>.bak`.
3. Se agregan las opciones nuevas **en su sección**, con sus comentarios y
   marcadas como `# --- NEW in this version: <ruta> (revisa esta opcion)`.
4. Tus valores nunca se pisan, y **las listas no se tocan jamás**: el lore, los
   comandos, las líneas de estado y similares son tuyos y no se tratan como
   opciones.
5. El resultado se valida antes de guardarlo; si no fuera YAML válido, el
   archivo queda intacto y la consola dice qué opción no se pudo agregar.
6. `config-version` sube solo (no lo bajes a mano). Si un archivo llega roto,
   se aparta como `<archivo>.invalido` y se restaura sola la última copia `.bak`
   válida.

## Compilar desde el código

```bash
mvn clean package        # -> target/JPRewards-2.0.0.jar
gradle build             # -> build/libs/JPRewards-2.0.0.jar
python tools/validar-recursos.py   # comprueba los YAML (necesita PyYAML)
```

## Estructura del proyecto

```
src/main/java/me/jp/jprewards/
├── JPRewards.java        clase principal (arranque, recarga, accesores)
├── command/              /rewards y todos sus subcomandos
├── config/               config, mensajes, menus, items, archivos y validador
├── hook/                 PlaceholderAPI y aviso de versión nueva
├── listener/             entrada y salida de jugadores
├── manager/              datos, recompensas, entrega, avisos, ranking, migración
├── menu/                 base de menús, gestor y los 7 menús
├── model/                recompensa, variante, item, efectos, hito, bonus y datos
├── storage/              almacén YAML y MySQL + códec de datos
└── util/                 compatibilidad 1.8-1.21+, textos, items, cabezas, tiempo
src/main/resources/       config.yml, messages_es/en.yml, menus.yml, recompensas/
tools/                    validador de recursos (Python)
testing/                  harness de pruebas (no se publica)
```

## Licencia

[MIT](LICENSE) © 2026 JP.

![Documentación, guías y FAQ](docs/assets/documentation.png)
![Únete a la comunidad](docs/assets/discord.png)
