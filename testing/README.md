# Infraestructura de pruebas de JPRewards

Todo lo de esta carpeta y lo de `.test-servers/` es **solo para pruebas**: el harness
nunca se publica ni se empaqueta en el jar de JPRewards.

## Resumen rapido

~~~powershell
cd C:\Users\Usuario\Downloads\Plugins\JPRewards
powershell -ExecutionPolicy Bypass -File .\testing\run-tests.ps1
# (o simplemente .\testing\run-tests.ps1 si la politica de ejecucion lo permite)
~~~

Eso compila el plugin, compila el harness, copia los dos jars a los dos servidores,
arranca cada servidor, recoge las lineas `CHECK ...` / `HARNESS ...` y deja los logs en
`testing/logs/`. Los servidores se apagan solos (`Bukkit.shutdown()`); si algo se cuelga, el
script mata el proceso al vencer el timeout (180 s por defecto).

## 1. Donde esta cada cosa

| Ruta | Que es |
| --- | --- |
| `.test-servers/paper-1.8.8/` | Servidor Paper 1.8.8 listo para arrancar (jar, eula, server.properties, plugins/) |
| `.test-servers/spigot-1.21.1/` | Servidor Spigot 1.21.1 listo para arrancar |
| `.test-servers/tools/jdk8/jdk8u504-b01/` | Copia del JDK 8 que necesita el servidor 1.8.8 |
| `testing/harness/` | Codigo del plugin de pruebas (Maven, Java 8, Spigot 1.8.8 provided) |
| `testing/run-tests.ps1` | Ciclo completo |
| `testing/logs/` | Un log por servidor y ejecucion, mas los logs de Maven y un resumen |

## 2. Los dos servidores

### Paper 1.8.8 (`.test-servers/paper-1.8.8/`)

* **Jar**: `paper-1.8.8.jar`, copiado de `C:\Users\Usuario\Downloads\mcserver\paper-1.8.8.jar`.
  Es un **paperclip** (`Main-Class: io.papermc.paperclip.Paperclip`): la primera ejecucion
  descargo el vanilla jar de Mojang y lo parcheo; el resultado quedo cacheado en
  `cache/mojang_1.8.8.jar` y `cache/patched_1.8.8.jar`. **A partir de ahi arranca sin red**
  (verificado: la segunda ejecucion no imprime ninguna linea de descarga).
  Si se borra `cache/`, la siguiente ejecucion vuelve a necesitar internet.
* **Java**: JDK 8 en `.test-servers/tools/jdk8/jdk8u504-b01/bin/java.exe`
  (copia de `C:\Users\Usuario\Downloads\Plugins\JPPlayerVaults\.test-servers\tools\jdk8\jdk8u504-b01`,
  comprobado que el ejecutable existe). Se puede cambiar con la variable de entorno
  `JPR_JDK8` (ruta al java.exe o al JDK).
* **Puerto**: 25580.

### Spigot 1.21.1 (`.test-servers/spigot-1.21.1/`)

* **Jar**: `spigot-1.21.1.jar`, copiado de
  `C:\Users\Usuario\Downloads\mcserver 1.21\buildtools\spigot-1.21.1.jar` (generado por BuildTools;
  `Main-Class: org.bukkit.craftbukkit.bootstrap.Main`, 76 MB, con sus 70 librerias dentro).
  Es un jar "bundler": en el primer arranque descomprime sus librerias en `bundler/`
  (local, sin red). **No descarga nada.**
* **Java**: el `java` del PATH (Java 21). Se puede cambiar con `JPR_JAVA21`.
* **Puerto**: 25581.

### Configuracion de los dos

`eula=true`, `online-mode=false`, mundo plano, `spawn-protection=0`, `max-players=5`,
`view-distance=4`, `enable-command-block=false`, dificultad pacifica, `motd` identificable
(`JPRewards test 1.8.8` / `JPRewards test 1.21.1`), `allow-nether=false`, sin animales ni
monstruos, `generate-structures=false`. Los mundos se generan solos al arrancar.

Dos detalles de version:

* En 1.8.8 (antes de 1.13) `difficulty` y `gamemode` son **numeros**: `difficulty=0` es
  pacifico y `gamemode=1` es creativo. En 1.21.1 son textos (`peaceful`, `creative`).
* El tipo plano es `level-type=FLAT` en 1.8.8 y `level-type=minecraft\:flat` en 1.21.1.
  El harness imprime una linea `HARNESS INFO: mundo=... tipo=FLAT ...` en cada arranque: en
  1.8.8 el bloque mas alto en 0,0 esta en y=4 y en 1.21.1 en y=-61 (mundos planos de cada
  version).

`usercache.json` incluye a `HarnessPlayer` (el jugador que usan las comprobaciones de comandos)
para que `Bukkit.getOfflinePlayer(nombre)` no tenga que consultar nada fuera.

En `plugins/` solo hay dos jars: `JPRewards-<version>.jar` y `Harness.jar`.
(`plugins/PluginMetrics/` lo crea el propio servidor Paper 1.8.8, no JPRewards: el plugin no
incluye ni usa bStats, y su unica llamada de red opcional es el aviso de version nueva, que viene
desactivado por defecto.)

## 3. El harness (`testing/harness/`)

Plugin de pruebas que compila aparte, con Java 8 y la API de Spigot 1.8.8 como
`provided`. **No depende de las clases de JPRewards**: las busca por reflexion con el
classloader del plugin, asi que compila y arranca aunque JPRewards este a medio escribir.

Al habilitarse espera 3 s, ejecuta las comprobaciones, espera otros 3 s con una tarea
repetida (estabilidad del scheduler), imprime el resumen y llama a `Bukkit.shutdown()`.

Formato de salida (una linea por comprobacion):

~~~
CHECK OK: <texto>
CHECK FAILED: <texto>
CHECK SKIPPED: <texto>
HARNESS RESULT checks=<n> failures=<n> skipped=<n>
HARNESS PASS | HARNESS FAIL
~~~

Reglas:

* Si una clase o un metodo de JPRewards todavia no existe, la comprobacion se marca
  **SKIPPED** con el motivo (no rompe el arranque).
* Si JPRewards no esta en `plugins/` o no se habilita, eso si es **FAILED** (es la
  comprobacion 1) y el resto se salta.
* `HARNESS PASS` solo si `failures=0`.
* Los comandos se ejecutan con `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)`. Si el
  servidor se come la excepcion y la escribe en el log, un `Handler` propio enganchado a los
  loggers de Bukkit la detecta y la comprobacion falla igual.

Comprobaciones:

| # | Que mira |
| --- | --- |
| 1 | JPRewards activado; `/rewards` registrado, alias `/rec`, `setExecutor` y `setTabCompleter` |
| 2 | Los 8 archivos por defecto creados en `plugins/JPRewards/` |
| 3 | Esos YAML se cargan con `YamlConfiguration.loadConfiguration` sin excepciones y sin claves duplicadas |
| 4 | `config-version` presente y `>= 2` en cada archivo gestionado |
| 5 | 8 comandos de consola sin excepciones (`info`, `validate`, `ayuda`, `datos <jugador>`, `dar <jugador> diaria`, `reload`, `top`, `test diaria`) y el plugin sigue activo |
| 6 | `DatosCodec`: ida y vuelta de un `DatosJugador` conocido (racha 12, total 40, diasMes 9, 1 hito, 1 bonus, 2 items pendientes) |
| 7 | `AlmacenYaml`: guardar y recargar ese jugador en una carpeta temporal |
| 8 | Fusion de config: se respeta el valor del usuario, aparecen las secciones nuevas y el YAML sigue valido |
| 9 | Tarea repetida 3 s: el scheduler responde, el plugin sigue activo y no hay errores en el log |
| 10 | `messages_es.yml` define `prefijo` y las secciones `general`, `comando`, `diaria`, `semanal`, `mensual`, `rachas`, `recompensa`, `titulos` |

Compilar solo el harness:

~~~powershell
mvn -q -B -f testing\harness\pom.xml clean package      # -> testing\harness\target\Harness.jar
~~~

## 4. run-tests.ps1

~~~powershell
.\testing\run-tests.ps1                                  # ciclo completo
.\testing\run-tests.ps1 -TimeoutSeconds 240              # mas paciencia por servidor
.\testing\run-tests.ps1 -Servers paper-1.8.8             # solo un servidor
.\testing\run-tests.ps1 -SkipBuild                       # no compila: usa los jars que ya hay
.\testing\run-tests.ps1 -SkipJPRewards                   # solo el harness (sin JPRewards)
.\testing\run-tests.ps1 -KeepPluginData                  # no borra plugins\JPRewards antes de arrancar
~~~

* Antes de cada servidor borra `plugins/JPRewards` (para probar de verdad que los archivos
  por defecto se crean solos) y las copias viejas de los jars.
* Si JPRewards no compila, se para y muestra el final del log de Maven (no arranca nada).
* Guarda `testing/logs/<servidor>-<fecha>.log`, `build-*.log` y `resumen-<fecha>.txt`.
* Devuelve 0 si los dos servidores imprimen `HARNESS PASS`; si no, 1.
* Mata el proceso del servidor (y su arbol) al terminar o al vencer el timeout, asi que se
  puede ejecutar las veces que haga falta sin dejar nada colgado.

Si PowerShell bloquea el script por la politica de ejecucion:

~~~powershell
powershell -ExecutionPolicy Bypass -File .\testing\run-tests.ps1
~~~

## 5. Notas

* `.test-servers/` ocupa unos 400 MB (JDK 8 + jars + cache del paperclip + mundos) y ya
  esta excluido en el `.gitignore` del proyecto.
* `plugins/PluginMetrics/` lo crea el propio servidor Paper 1.8.8, no JPRewards: el plugin no
  incluye ni usa bStats (no hay ninguna referencia en el codigo). No afecta a las pruebas.
* Los mundos y `plugins/JPRewards` de cada servidor quedan con lo de la ultima ejecucion:
  no hay nada que limpiar a mano.

## 6. Ultima ejecucion real (2026-10-18, JPRewards-2.0.0.jar congelado)

La suite se ejecuto muchas veces durante el desarrollo. Encontro tres defectos reales, todos
arreglados y vueltos a verificar: faltaba `config-version` en los cuatro `recompensas/*.yml`,
la particula del premio semanal no existia en 1.8, y la fusion de archivos insertaba las
opciones nuevas fuera de su seccion. Esta es la ultima corrida, sobre el artefacto final:

| Servidor | Veredicto | checks | ok | failed | skipped | exit | timeout |
| --- | --- | --- | --- | --- | --- | --- | --- |
| paper-1.8.8 (Paper 1.8.8 + JDK 8) | PASA | 29 | 29 | 0 | 0 | 0 | no |
| spigot-1.21.1 (Spigot 1.21.1 + Java 21) | PASA | 29 | 29 | 0 | 0 | 0 | no |

Lineas finales del harness (iguales en los dos servidores):

~~~
HARNESS RESULT checks=29 failures=0 skipped=0
HARNESS PASS
~~~

Logs de esa corrida: `testing/logs/paper-1.8.8-20261018-182723.log`,
`testing/logs/spigot-1.21.1-20261018-182723.log` y
`testing/logs/resumen-20261018-182723.txt`. El detalle de lo que cubre cada comprobacion
esta en `docs/VERIFICATION.md`.

Lo que pasa en las dos versiones: el plugin carga, los 8 archivos por defecto se crean, los
8 archivos YAML se leen sin claves duplicadas y con `config-version >= 2`, los 8 comandos
de consola responden sin excepciones, `DatosCodec` y `AlmacenYaml` devuelven los mismos
datos, la fusion de config respeta lo del usuario y agrega las 16 secciones nuevas con su
copia `.bak`, el scheduler aguanta 3 segundos de tarea repetida y `messages_es.yml` tiene
todas las claves esperadas.

Los logs completos estan en `testing/logs/paper-1.8.8-<fecha>.log` y
`testing/logs/spigot-1.21.1-<fecha>.log`.
