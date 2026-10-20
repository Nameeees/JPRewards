# SpigotMC update — 2.1.0

Everything needed to publish the update. The file to upload is
`dist/JPRewards-2.1.0.jar` (run `tools/publicar-spigot.ps1` to build it and prepare everything).

## Title

```
2.1.0 — One-line rewards, conditions and a public API
```

## Description (BBCode, paste as-is)

```bbcode
[SIZE=5][B]2.1.0 — rewards in one line, conditions and an API for other plugins[/B][/SIZE]

This update makes the reward files much shorter and adds the two things you asked for the most:
conditions on a reward and a way for other plugins to give rewards.

[SIZE=4][B]Rewards in one line[/B][/SIZE]
The minimum to add a reward is now a single line:

[CODE]
dias:
  '1': 'eco give {jugador} 100'
  '2': ['eco give {jugador} 200', 'BREAD 4']
  '3': {usar: vip, extra: 'give {jugador} diamond 1'}
[/CODE]

In a list, anything that is a material is delivered as an item and the rest runs as a console
command. The icon and the lore are optional now: the menu builds the preview for you.

[SIZE=4][B]Reusable packs[/B][/SIZE]
Define a reward once and use it in as many days as you want:

[CODE]
definir:
  vip: 'crates give {jugador} vip 1'

dias:
  '5': {usar: vip}
  '6': {usar: vip, extra: 'eco give {jugador} 1000'}
[/CODE]

[SIZE=4][B]Conditions on any reward[/B][/SIZE]
[CODE]
dias:
  '7':
    dar: ['crates give {jugador} vip 1']
    probabilidad: 50      # 0.5, 50 or '50%'
    mundo: world_nether   # only in that world
    una-vez: true         # only the first time for each player
[/CODE]

[SIZE=4][B]API, events and extra rewards[/B][/SIZE]
[LIST]
[*][B]Java API[/B] ([COLOR=#a020f0]JPRewardsAPI[/COLOR]): claim, give, giveExtra, pending, data...
[*][B]Events[/B]: [COLOR=#a020f0]RewardClaimEvent[/COLOR] (cancelable, before delivery) and [COLOR=#a020f0]RewardDeliveredEvent[/COLOR].
[*][B]Extra rewards[/B]: define [COLOR=#a020f0]recompensas/extra.yml[/COLOR] and deliver them with [COLOR=#a020f0]/rewards trigger <player> <id>[/COLOR] from any plugin, boss, vote or quest.
[/LIST]
Documentation: [URL]https://nameeees.github.io/JPRewards/API.html[/URL]

[SIZE=4][B]More[/B][/SIZE]
[LIST]
[*]New [COLOR=#a020f0]/rewards backup[/COLOR]: zips the player data into [COLOR=#a020f0]backups/[/COLOR].
[*]Delivery history: every reward is logged in [COLOR=#a020f0]historial.log[/COLOR] and the last five are shown in [COLOR=#a020f0]/rewards data <player>[/COLOR].
[*]Networks with shared MySQL no longer overwrite each other: the cache checks the [COLOR=#a020f0]actualizado[/COLOR] column and reloads the data if another server changed it.
[/LIST]

[SIZE=4][B]Updating[/B][/SIZE]
[LIST]
[*]Replace the jar and restart. Your configuration, your menus and your player data are kept.
[*]New options are added automatically, inside their section, with a [COLOR=#a020f0].bak[/COLOR] copy.
[*]The auto-merge only runs when your file has an older [COLOR=#a020f0]config-version[/COLOR]; a file that is already up to date is never rewritten, so [COLOR=#a020f0]/rewards reload[/COLOR] cannot re-inject options or example rewards you deleted on purpose.
[*]Lists ([COLOR=#a020f0]lore[/COLOR], [COLOR=#a020f0]comandos[/COLOR], status lines...) are never modified by the merge, and the merged file is validated before it is saved.
[*]Everything that worked before keeps working: nothing was renamed or removed.
[/LIST]

Tested on Paper 1.8.8 (Java 8) and Spigot 1.21.1 (Java 21): 34 automated checks, 0 failures.
```

## Files

| File | What to do |
| --- | --- |
| `dist/JPRewards-2.1.0.jar` | The jar to upload as the new version. |
| `docs/SPIGOTMC_UPDATE_2.1.0.md` | This page (title + description). |
| `docs/assets/integrations.png` | New screenshot for the "Integrations" section (optional but recommended). |
| `docs/assets/features.png`, `commands.png` | Regenerated with the new content (upload again to replace them). |

## After publishing

1. Copy the resource id (the number in the URL) into `config.yml -> actualizaciones.id-recurso`
   and into `plugin.yml -> website` if it changes.
2. `git tag v2.1.0 && git push origin v2.1.0` publishes the GitHub release with the jar attached.
3. Run `tools/publicar-spigot.ps1 -Version 2.1.0` (or the automation) for the next update.
