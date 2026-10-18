# Changelog - JPRewards

Todas las versiones publicadas, de la mas nueva a la mas vieja.
Formato: version, fecha y que cambio (anadido / cambiado / arreglado).

## 2.0.0 - 2026-10-18

Reescritura completa. La version 2.0.0 se puede instalar encima de la 1.x:
los datos de los jugadores y los archivos de configuracion se conservan, y las
opciones nuevas se agregan solas con sus comentarios.

### Anadido

- **Bonus por racha** (modulo nuevo): recompensas al alcanzar N dias o
  semanas seguidas, reclamables una vez por racha (`recompensas/rachas.yml`).
- **Entrega por items directos** ademas de por comandos: `items:` con
  material, cantidad, nombre, lore, encantamientos, item-flags,
  custom-model-data (1.14+) e irrompibilidad; acepta cabezas con textura o de
  un jugador. Tambien hay una forma corta: `- 'DIAMOND 3'`.
- **Experiencia directa** (`experiencia:` y `niveles:`) sin pasar por comandos.
- **Efectos por recompensa**: sonido (con volumen y tono), particulas, fuegos
  artificiales, titulo de pantalla, action bar, mensajes extra y anuncio a todo
  el servidor.
- **Almacenamiento en MySQL** por JDBC (ademas de YAML), con
  `/rewards migrate confirmar` para copiar los datos en los dos sentidos.
- **Modo de archivo unico** para los datos (`almacenamiento.modo: unico`),
  pensado para servidores con muchisimos jugadores.
- **Items pendientes**: si un premio no cabe en el inventario se guarda y se
  entrega al volver a entrar (o se suelta, segun configuracion).
- **Reclamo automatico** de la diaria al entrar (desactivado por defecto, con
  permiso propio).
- **Reclamar todo**: boton en el menu principal, accion `reclamar:todo` y
  `/rewards claim todo`.
- **Paginacion** en los ciclos largos y en los hitos, con botones
  configurables (anterior / informacion / siguiente).
- **Menu de bonus de racha** y **menu generico** para crear menus propios
  desde `menus.yml` sin tocar el plugin.
- **Barras de progreso** de texto: `{barra_mes}`, `{barra_ciclo}`,
  `{barra_racha}`, `{barra_racha_semanal}`.
- **PlaceholderAPI** (opcional): `%jprewards_racha%`, `%jprewards_dia%`,
  `%jprewards_tiempo_diaria%`, `%jprewards_disponibles%` y muchos mas, ademas
  de poder usar placeholders de otros plugins dentro de los textos.
- **Aviso de version nueva** opcional con la API publica de SpigotMC.
- **Idiomas**: el plugin viene **en inglés por defecto** (chat, menús y premios de
  ejemplo) y el español va incluido como preset completo: `messages_es.yml`,
  `menus_es.yml` y `recompensas/*_es.yml`, que se activan con seis líneas de
  `config.yml`. Añadir otro idioma es copiar `messages_en.yml` y traducirlo.
- **Auto-actualizacion de archivos**: al actualizar el plugin, las opciones
  nuevas se insertan en la seccion que corresponde con sus comentarios,
  marcadas como `# --- NUEVO en esta version`, y se guarda una copia
  `<archivo>.bak` antes de tocar nada.
- **/rewards validate**: revisa todos los archivos y explica cada error
  (material, sonido, particula, encantamiento, slot, accion, placeholder,
  textura...) indicando el archivo y la clave exacta.
- **/rewards test <tipo> [clave]**: simulacro que muestra exactamente que
  se entregaria, sin entregar nada.
- **/rewards status**, **/rewards data**, **/rewards top**
  (racha, total, semanas, hitos, bonus) y **/rewards give**.
- **Comentarios bilingues** (espanol + ingles) en todos los archivos de
  configuracion.
- Documentacion de lanzamiento: README, guia de configuracion, pagina de
  SpigotMC, checklist de QA y registro de verificacion.
- **Arte generado por codigo** en `docs/assets/`: banner (1280x420), icono
  (512x512), siete infografias (caracteristicas, comandos, permisos, instalacion,
  configuracion, diagnostico y compatibilidad) y dos banners de llamada a la
  accion. Los generadores son scripts de Python con Pillow, la salida es
  determinista y se regenera con un comando por script.
- **Cada imagen tiene su propio tema, siempre en la paleta morada**: cambia el
  tono (fucsia, violeta, lavanda, magenta, orquidea) y sobre todo la
  **composicion**, para que no sean seis bandejas iguales: rejilla de tarjetas,
  ventana de consola, tabla de permisos con chips de audiencia, linea de tiempo
  vertical, arbol de archivos junto a un panel de YAML, informe de validacion y
  linea de versiones. Todas comparten el mismo tratamiento de fondo: degradado de
  tres tramos, aurora de cuatro masas de luz, haces diagonales, campo de bokeh,
  arcos concentricos en las esquinas y viñeta. Los dos banners de
  llamada a la accion usan ademas los **iconos oficiales** de Discord y GitHub
  (`docs/assets/icons`, de simple-icons): Discord en blurple con burbujas de
  chat e insignia cuadrada, documentacion en slate de GitHub con rejilla de
  contribuciones, insignia circular y acento verde.

### Cambiado

- **Todo el sistema de comandos esta en INGLES, incluidos los alias**: el
  comando principal pasa a ser `/rewards` (alias `jprewards`, `rec`, `reward`) y
  los subcomandos son `help`, `daily`, `weekly`, `monthly`, `streaks`, `info`,
  `claim`, `top`, `status`, `validate`, `reload`, `data`, `give`, `test`,
  `reset`, `set` y `migrate`. Tambien los argumentos: tipos
  `daily|weekly|monthly|streaks|all`, rankings
  `streak|total|weeks|milestones|bonuses`, campos de `set`
  `streak|best-streak|total|weekly-streak|weekly-total|month-days|milestones` y
  destinos de `reload` `all|config|messages|menus|rewards`.
  `/recompensas` se mantiene como alias heredado de la 1.x para no romper
  servidores existentes (se puede quitar de `plugin.yml`). La ayuda en el juego,
  el tab-complete, la documentacion y las infografias ya muestran los nombres en
  ingles.

- El paquete Java pasa a `me.jp.jprewards` (antes `com.tuserver.jprewards`).
  La carpeta de datos sigue siendo `plugins/JPRewards`, asi que los datos de la
  1.x se siguen usando sin hacer nada.
- `messages.yml` se renombra a `messages_es.yml` automaticamente al
  actualizar (mismo contenido, formato nuevo). Ademas, si vienes de la 1.x (que
  solo existia en espanol) se agrega `idioma: es` al config.yml para que el
  chat NO cambie de idioma solo: el ingles por defecto es solo para las
  instalaciones nuevas.
- Los avisos al entrar pueden ir por action bar (con degradacion a chat).
- `diaria.dias-del-ciclo` y `semanal.semanas-del-ciclo` aceptan `0` =
  automatico (los que haya en el archivo de recompensas).
- Los menus admiten todos los campos nuevos (cantidad, encantamientos,
  item-flags, custom-model-data, irrompible, permiso, ocultar-sin-permiso,
  varios slots y varias acciones separadas por `;`).

### Arreglado

- **Se podia reclamar la recompensa diaria sin limite.** El permiso
  `jprewards.bypass` (por defecto para operadores) volvia a ENTREGAR el premio
  cada vez que se pulsaba el boton, asi que un operador podia duplicar dinero,
  items o llaves indefinidamente. Ese permiso ya NO existe: reclamar dos veces
  la misma recompensa solo muestra el aviso de "ya reclamado". Para entregar un
  premio a proposito esta `/rewards give <jugador> <tipo> [clave]`
  (`jprewards.admin.manage`), y para previsualizarlo `/rewards test`.
  Ademas se anadio una guardia de re-entrada: un mismo jugador no puede tener
  dos reclamos a la vez ni aunque un premio ejecute un comando que reclame.

- Los archivos YAML se leen y se escriben SIEMPRE en UTF-8, tambien en
  servidores Windows (antes los acentos podian romperse).
- Un archivo de datos corrupto se aparta como `.bak` y se avisa por consola,
  en vez de sobrescribirlo con datos vacios sin decir nada.
- Si la base de datos no responde al arrancar, el plugin sigue funcionando con
  YAML y lo explica, en lugar de dejar a los jugadores sin recompensas.
- Los menus abiertos se cierran al recargar, para no quedar con estados viejos.
- Los guardados se hacen en segundo plano (serializando antes en el hilo
  principal), asi que no bloquean el servidor.

## 1.3.0

- Recompensas diarias, semanales (ciclo) y mensuales (hitos por dias
  reclamados), menus de cabezas con texturas, variantes por rango, entrega por
  comandos de consola y soporte 1.8 - 1.21+.
