# Changelog - JPRewards

Todas las versiones publicadas, de la mas nueva a la mas vieja.
Formato: version, fecha y que cambio (anadido / cambiado / arreglado).

## 2.1.0 - en desarrollo

### Arreglado

- **Los menus con paginacion no se abrian**: el constructor de `Menu` calculaba
  las paginas antes de crear el inventario, asi que `/rewards daily`, `weekly`,
  `monthly` y `streaks` lanzaban `NullPointerException` (el menu principal, que no
  pagina, si funcionaba). Detectado probando con un **cliente real de Minecraft**
  1.8.9 contra el servidor; el harness no lo veia porque nunca abria menus.
  Ahora el tamaño del cofre se calcula sin depender del inventario ya creado.
- **Traza completa en la consola** cuando un comando falla: antes solo salia el
  nombre de la excepcion, sin la linea que lo causaba.

### Anadido

- **Recompensas en una linea.** Cada entrada de `recompensas/*.yml` puede ser un
  comando suelto o una lista de comandos y materiales:
  `'1': 'eco give {jugador} 100'` o
  `'2': ['eco give {jugador} 200', 'BREAD 4']`. En las listas, lo que es un
  material (con cantidad opcional) se entrega como item y el resto se ejecuta
  como comando.
- **Packs reutilizables**: `definir:` crea premios con nombre y `usar:` los
  reutiliza, con `extra:` para anadir comandos encima. Funciona en diarias,
  semanales, hitos y rachas.
- **`dar` y `give` como alias de `comandos`**, e **`icono: MATERIAL`** en una
  linea. Lore e icono pasan a ser opcionales: el menu genera la vista previa.
- **Condiciones por recompensa**: `probabilidad` (0.25, 25 o '25%'),
  `mundo` (solo se puede reclamar ahi) y `una-vez` (solo la primera vez en la
  vida del jugador, guardado en sus datos). Si el mundo no coincide el reclamo
  NO se consume; si falla la probabilidad o ya se recibio, si se consume.
- **API publica para otros plugins** (`me.jp.jprewards.api.JPRewardsAPI`):
  `claim`, `give`, `giveExtra`, `hasPending`, `pending`, `hasClaimedToday`,
  `data`, `rewardKeys`, `extraKeys`.
- **Eventos**: `RewardClaimEvent` (cancelable, antes de entregar) y
  `RewardDeliveredEvent` (despues, con lo entregado). Documentados en
  `docs/API.md`.
