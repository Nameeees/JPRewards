# Changelog - JPRewards

Todas las versiones publicadas, de la mas nueva a la mas vieja.
Formato: version, fecha y que cambio (anadido / cambiado / arreglado).

## 2.1.0 - en desarrollo

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
- **Premios sueltos** en `recompensas/extra.yml` + comando
  `/rewards trigger <jugador> <id>`: la forma de dar recompensas desde eventos
  de otros plugins (jefes, votos, misiones) sin tocar los cuatro ciclos.
- **Historial de entregas**: una linea por entrega en `historial.log` y las
  ultimas cinco en `/rewards data <jugador>`.
- **Copia de seguridad**: `/rewards backup` comprime la carpeta de datos en
  `backups/`.
- **MySQL en red**: la cache comprueba la columna `actualizado` cada 2,5 s y
  recarga los datos si otro servidor los ha cambiado, para que dos servidores no
  se pisen al reclamar el mismo jugador.
- Comprobaciones del harness: **32** (antes 29).

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
