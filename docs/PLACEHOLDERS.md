# Placeholders

There are two families: the ones you write **inside the plugin's own texts** (messages, menus,
rewards) and the ones **PlaceholderAPI** exposes to other plugins.

## Inside the plugin

Write them between braces in any text of `messages_*.yml`, `menus.yml` or
`recompensas/*.yml`. `/rewards validate` reports any placeholder the plugin does not fill, so a
typo never goes unnoticed.

### Player and reward

| Placeholder | Value |
| --- | --- |
| `{jugador}` `{uuid}` `{mundo}` | Name, UUID and world of the player. |
| `{premio}` `{recompensa}` | Name of the reward being shown or delivered. |
| `{cantidad}` | How many (rewards, items, players), depends on the text. |
| `{item}` | Name of the item. |
| `{tipo}` `{modulo}` `{clave}` `{valor}` `{actual}` `{entrada}` | Type, module, key, value and current entry. |

### Daily cycle

| Placeholder | Value |
| --- | --- |
| `{dia}` | Next day of the cycle the player will get. |
| `{dias}` | Days of the cycle. |
| `{faltan}` | Days missing for something (a milestone, for example). |
| `{estado}` | State of the entry (available, claimed, locked...). |
| `{racha}` `{mejor}` `{total}` | Current streak, best streak and total claimed. |
| `{proximo}` | What comes next. |

### Weekly cycle

| Placeholder | Value |
| --- | --- |
| `{semana}` `{semanas}` | Next week and weeks of the cycle. |
| `{racha_semanal}` `{mejor_semanal}` `{total_semanal}` | Weekly streak, best weekly streak and weekly total. |
| `{tiempo_semanal}` | Time left for the next weekly reward. |

### Monthly milestones and streak bonuses

| Placeholder | Value |
| --- | --- |
| `{dias_mes}` `{dias_mes_total}` | Days claimed this month and days of the month. |
| `{hitos}` `{hitos_total}` `{hitos_disponibles}` | Claimed, total and ready milestones. |
| `{hito}` `{racha_necesaria}` | Milestone name and the streak it needs. |
| `{bonus}` `{bonus_total}` `{bonus_disponibles}` | Claimed, total and ready streak bonuses. |
| `{racha_minima}` | Streak a bonus needs. |

### Time and progress bars

| Placeholder | Value |
| --- | --- |
| `{tiempo}` `{tiempo_largo}` `{hora}` | Time left (short and long form) and the reset hour. |
| `{barra_mes}` `{barra_ciclo}` `{barra_racha}` `{barra_racha_semanal}` | Text progress bars. |

### Leaderboard and plugin information

| Placeholder | Value |
| --- | --- |
| `{top}` `{puesto}` `{primero}` `{ultimo}` | Leaderboard lines, position, first and last entry. |
| `{pagina}` `{paginas}` | Current page and page count. |
| `{preview}` | The reward preview block (items, commands and experience of an entry). |
| `{version}` `{servidor}` `{idioma}` | Plugin version, server version and active language. |
| `{almacenamiento}` `{tipo_almacenamiento}` `{ruta}` `{cargados}` `{autoguardado}` | Storage target, type, folder, players in memory and autosave interval. |
| `{modulos}` `{archivos}` `{pendientes}` `{moneda}` | Enabled modules, managed files, pending rewards and currency name. |
| `{prefijo}` `{texto}` | Message prefix and a plain text. |
| `{tipos}` `{campos}` `{destinos}` `{campo}` | Valid values of a command argument. |
| `{errores}` `{avisos}` `{copiados}` `{omitidos}` `{origen}` `{destino}` `{error}` | Results of `validate` and `migrate`. |

## PlaceholderAPI

The expansion identifier is `jprewards`. It is registered only if PlaceholderAPI is installed; the
plugin works exactly the same without it.

| Placeholder | Value |
| --- | --- |
| `%jprewards_dia%` | Next day of the daily cycle. |
| `%jprewards_racha%` `%jprewards_mejor_racha%` `%jprewards_total%` | Streak, best streak and total. |
| `%jprewards_disponible_diaria%` `%jprewards_tiempo_diaria%` | Whether the daily reward is ready, and the time left. |
| `%jprewards_semana%` `%jprewards_semanas%` | Next week and weeks of the cycle. |
| `%jprewards_racha_semanal%` `%jprewards_mejor_racha_semanal%` `%jprewards_total_semanal%` | Weekly streak, best and total. |
| `%jprewards_disponible_semanal%` `%jprewards_tiempo_semanal%` | Weekly availability and time left. |
| `%jprewards_dias_mes%` `%jprewards_dias_mes_total%` | Days claimed this month and days of the month. |
| `%jprewards_hitos%` `%jprewards_hitos_total%` `%jprewards_hitos_disponibles%` | Monthly milestones. |
| `%jprewards_bonus%` `%jprewards_bonus_total%` `%jprewards_bonus_disponibles%` | Streak bonuses. |
| `%jprewards_disponibles%` | Everything the player can claim right now. |
| `%jprewards_reclamadas%` | `si` / `no`: whether the daily reward was claimed. |
| `%jprewards_moneda%` | Currency name from `config.yml`. |
| `%jprewards_premio_diaria%` `%jprewards_premio_semanal%` | Name of the next daily and weekly reward. |

Any of them accepts another player at the end, as long as that player is online:
`%jprewards_racha_Notch%`.

> Placeholders of **other** plugins can be used inside the plugin's texts (menus, messages,
> rewards): they are resolved when the text is shown, if PlaceholderAPI is installed.
