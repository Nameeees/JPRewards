# SpigotMC submission pack

Everything needed to publish **JPRewards 2.1.0** as a **free** resource on SpigotMC. Copy the
BBCode block into the resource description field and fill the form with the values below.

---

## 1. Form values

| Field | Value |
| --- | --- |
| Resource name | `JPRewards` |
| Tagline | `Daily, weekly, monthly and streak rewards with head menus for 1.8 → latest. One jar, no dependencies.` |
| Category | `Gameplay` → `Mechanics` (alternative: `Tools and Utilities`) |
| Version | `2.1.0` |
| Native Minecraft version | The latest stable release at the moment you publish (update it on every Minecraft release) |
| Supported Minecraft versions | `1.8` through the latest (select the full range) |
| Price | Free |
| External download | No — upload `JPRewards-2.1.0.jar` directly |
| License | MIT (source included; the jar is not obfuscated) |
| Source / issues | Fill in with your repository or discussion thread. There is no public repository configured in the project yet |
| Support | The resource discussion thread (and your Discord, if you have one) |
| Tags (pick 4–5) | `rewards`, `daily rewards`, `weekly rewards`, `streak`, `menus` |

> The plugin ships **English by default**: chat messages, menu texts and the example rewards.
> Complete Spanish presets are bundled too (`menus_es.yml` and `recompensas/*_es.yml`): a server
> switches to Spanish with six lines in `config.yml`, without translating anything by hand.

Two values in the project must be updated **before** uploading the jar:

- `src/main/resources/plugin.yml` → `website:` is still the generic `https://www.spigotmc.org/resources/`.
  Point it at the resource URL once the resource exists.
- `config.yml` → `actualizaciones.id-recurso` is `0` (disabled). After publishing, tell your users to
  put the resource id there (or ship it in a later update) so the optional update notice works.

## 2. Images

Every image is **generated from code** in [docs/assets/](assets/) (see [its README](assets/README.md)):
no external editor and no stock art, so the whole set can be re-rendered after a version bump with
one command per script. **The six `real-*.png` are genuine captures** taken from a real Minecraft 1.8.9 client connected to a test server (see §7); the rest is generated artwork. Everything is ready to upload as it is:

| File | Size | Where to use it |
| --- | --- | --- |
| [assets/banner.png](assets/banner.png) | 1280×420 | Header image of the resource |
| [assets/icon.png](assets/icon.png) | 512×512 | Resource icon (also works as a server icon) |
| [assets/features.png](assets/features.png) | 1280×720 | "What you get" section |
| [assets/commands.png](assets/commands.png) | 1280×720 | "Commands" section |
| [assets/permissions.png](assets/permissions.png) | 1280×720 | "Permissions" section |
| [assets/setup.png](assets/setup.png) | 1280×720 | "Setup" section |
| [assets/configuration.png](assets/configuration.png) | 1280×720 | "Configuration" section |
| [assets/diagnostics.png](assets/diagnostics.png) | 1280×720 | "Diagnostics" section |
| [assets/compatibility.png](assets/compatibility.png) | 1280×720 | "Compatibility" section |
| [assets/integrations.png](assets/integrations.png) | 1280×720 | "Integrations" section (API, events, extra rewards) |
| [assets/documentation.png](assets/documentation.png) | 1280×260 | Footer CTA (optional) |
| [assets/discord.png](assets/discord.png) | 1280×260 | Footer CTA (optional) |
| [assets/real-hub.png](assets/real-hub.png) | 1280×720 | **Real in-game capture** of the main menu |
| [assets/real-diario.png](assets/real-diario.png) | 1280×720 | **Real in-game capture** of the daily cycle |
| [assets/real-mensual.png](assets/real-mensual.png) | 1280×720 | **Real in-game capture** of the milestones |
| [assets/real-rachas.png](assets/real-rachas.png) | 1280×720 | **Real in-game capture** of the streak bonuses |
| [assets/real-semanal.png](assets/real-semanal.png) | 1280×720 | **Real in-game capture** of the weekly cycle |
| [assets/real-info.png](assets/real-info.png) | 1280×720 | **Real in-game capture** of the info menu |

Regenerate everything (the footer prints the version, so do it after a release):

```bash
python docs/assets/generate_banner_icon.py
python docs/assets/generate_infographics.py
python docs/assets/generate_cta.py --discord-url "discord.gg/your-invite" --docs-url "github.com/you/JPRewards#readme"
```

**In-game screenshots are still the best complement** and they are the only thing code cannot draw.
Shots worth taking on a test server, in this order:

| Screenshot | Where to use it |
| --- | --- |
| Main menu (`/rewards`) with the daily, weekly, monthly, streak and claim-all buttons | Second image, right below the banner |
| Daily cycle with a couple of days already claimed (the three states visible) | Next to the features infographic |
| A rank variant: the same day previewed with a normal account and with the VIP permission | "Rank variants" |
| `/rewards validate` and `/rewards test diaria 7` output in chat | "Diagnostics" section |
| The same jar running on a 1.8 server (proves the compatibility claim) | "Compatibility" section |

Do not put important text inside the images: SpigotMC guidelines ask for text in the description,
and a caption is enough. 1280 px wide images look best on the resource page.

## 3. Description (BBCode — paste as-is)

```bbcode
[SIZE=6][B]JPRewards[/B][/SIZE]
[SIZE=4][COLOR=#c026d3]Daily, weekly, monthly and streak rewards with fully configurable head menus. One jar for Minecraft 1.8 to the latest version.[/COLOR][/SIZE]

[B]JPRewards[/B] gives your server four reward systems that work together: a [B]daily[/B] cycle, a [B]weekly[/B] cycle, [B]monthly milestones[/B] and [B]streak bonuses[/B]. Players open a chest menu made of textured player heads, see what they are about to receive and claim it with one click.

The same jar runs from Minecraft [B]1.8[/B] to the [B]latest version[/B] on Spigot, Paper, Purpur and forks. It is built against the Spigot 1.8.8 API, has no NMS dependency, needs no other plugin (not even Vault), and is not obfuscated.

[HR][/HR]

[SIZE=5][B]Why JPRewards?[/B][/SIZE]
[LIST]
[*][B]One jar, every version.[/B] 1.8 servers on Java 8 and modern servers on Java 21+ load the same file. Renamed materials, sounds, enchantments, particles and titles are resolved at runtime, and anything that fails is reported once in the console instead of breaking the plugin.
[*][B]No dependencies.[/B] No Vault, no economy plugin, no database, no library. Rewards are delivered with console commands you write, so they work with EssentialsX, CMI, crates, LuckPerms or anything else your server already has.
[*][B]Items and XP directly.[/B] Besides commands, an entry can deliver items (material, amount, name, lore, enchantments, item flags, custom model data, textured heads) and experience or levels.
[*][B]Rank variants.[/B] The same day can give a different reward to VIP, MVP or a player with no rank, each with its own head, lore and commands. Exclusive entries (only one rank can claim them) are one line: [B]permiso: 'group.vip'[/B].
[*][B]Real streaks.[/B] The daily and weekly cycles have their own streak counters, an option to lose the streak when a day is missed, and bonuses for reaching 7, 30 or any number of days in a row.
[*][B]Monthly milestones.[/B] Unlocked by claiming the daily reward on enough days of the month (5, 10, 15, 25 days…), with a text progress bar. They reset by themselves when the month changes.
[*][B]Menus you can rebuild without touching code.[/B] Titles, textures, slots, states (available / claimed / locked / completed), previews and pagination all live in [B]menus.yml[/B], and you can add your own menus.
[*][B]Updating is safe.[/B] When a new version adds options, they are inserted into your files with their comments and marked as [B]# --- NUEVO en esta version[/B], after saving a [B].bak[/B] copy. Your values are never overwritten.
[*][B]/rewards validate[/B] checks every file (materials, sounds, particles, enchantments, slots, actions, textures, placeholders) and tells you the exact file and key that is wrong.
[*][B]YAML or MySQL.[/B] Readable per-player YAML files by default, or one table in MySQL/MariaDB over JDBC using the driver your server already ships. Data can be copied between both backends.
[*][B]No bStats, no metrics.[/B] The only outbound connection the plugin can make is the optional update check (off by default), which only fetches the resource version number from the public SpigotMC API.
[/LIST]

[SIZE=5][B]Features[/B][/SIZE]
[LIST]
[*][B]Daily reward[/B] with a configurable cycle of days ([B]recompensas/diarias.yml[/B]).
[*][B]Weekly reward[/B] with a configurable first day of the week and its own streak.
[*][B]Monthly milestones[/B] based on days claimed inside the month.
[*][B]Streak bonuses[/B] for N days or weeks in a row, claimable once per streak.
[*][B]Head menus[/B] with custom textures, per-player states, reward previews, automatic pagination and configurable navigation.
[*][B]Claim everything[/B] with one button: [B]/rewards claim todo[/B].
[*][B]Items that do not fit[/B] are stored and given on the next login (or dropped on the ground, your choice).
[*][B]Optional auto-claim[/B] of the daily reward on join, with its own permission ([B]jprewards.autoclaim[/B]).
[*][B]Effects[/B]: sounds, particles, fireworks, screen titles, action bars, extra messages and server-wide broadcasts, per reward.
[*][B]PlaceholderAPI[/B] (optional): %jprewards_racha%, %jprewards_dia%, %jprewards_disponibles% and many more, plus other plugins' placeholders inside its texts.
[*][B]English and Spanish[/B] messages included; add your own language with one file.
[*][B]Administration tools[/B]: validar, estado, datos, dar, test, reset, set, top and reload.
[*][B]Tab completion[/B] for subcommands, types, keys, players, fields and reload targets.
[/LIST]

[SIZE=5][B]Commands[/B][/SIZE]
[TABLE]
[TR][TH]Command[/TH][TH]What it does[/TH][TH]Permission[/TH][/TR]
[TR][TD]/rewards[/TD][TD]Opens the main menu.[/TD][TD]jprewards.use[/TD][/TR]
[TR][TD]/rewards daily|semanal|mensual|rachas|info[/TD][TD]Opens each menu directly.[/TD][TD]jprewards.use[/TD][/TR]
[TR][TD]/rewards claim <tipo|todo>[/TD][TD]Claims without opening a menu.[/TD][TD]jprewards.use[/TD][/TR]
[TR][TD]/rewards top [racha|total|semanas|hitos|bonus][/TD][TD]Server leaderboard.[/TD][TD]jprewards.top[/TD][/TR]
[TR][TD]/rewards status[/TD][TD]Plugin, storage and configuration status.[/TD][TD]jprewards.admin[/TD][/TR]
[TR][TD]/rewards validate[/TD][TD]Checks every file and explains each problem.[/TD][TD]jprewards.admin[/TD][/TR]
[TR][TD]/rewards reload [config|mensajes|menus|recompensas|todo][/TD][TD]Hot reload.[/TD][TD]jprewards.admin[/TD][/TR]
[TR][TD]/rewards data <player>[/TD][TD]Full report for one player.[/TD][TD]jprewards.admin[/TD][/TR]
[TR][TD]/rewards give <player> <tipo> [clave][/TD][TD]Delivers a reward for testing.[/TD][TD]jprewards.admin.manage[/TD][/TR]
[TR][TD]/rewards test <tipo> [clave][/TD][TD]Dry run: shows what would be delivered.[/TD][TD]jprewards.admin.manage[/TD][/TR]
[TR][TD]/rewards reset <player|*> [tipo][/TD][TD]Resets counters.[/TD][TD]jprewards.admin.manage[/TD][/TR]
[TR][TD]/rewards set <player> <campo> <valor>[/TD][TD]Sets a counter.[/TD][TD]jprewards.admin.manage[/TD][/TR]
[TR][TD]/rewards migrate confirmar[/TD][TD]Copies all data between YAML and MySQL.[/TD][TD]jprewards.admin.migrate[/TD][/TR]
[/TABLE]

Aliases: [B]/rewards[/B], [B]/jprewards[/B], [B]/rec[/B], [B]/recompensa[/B].

[SIZE=5][B]Permissions[/B][/SIZE]
[TABLE]
[TR][TH]Permission[/TH][TH]Description[/TH][TH]Default[/TH][/TR]
[TR][TD]jprewards.use[/TD][TD]Open the menus and use /rewards.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.claim.daily[/TD][TD]Claim the daily reward.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.claim.weekly[/TD][TD]Claim the weekly reward.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.claim.monthly[/TD][TD]Claim the monthly milestones.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.claim.streak[/TD][TD]Claim the streak bonuses.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.notify[/TD][TD]Join notice when something can be claimed.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.autoclaim[/TD][TD]Claim the daily reward automatically on join.[/TD][TD]false[/TD][/TR]
[TR][TD]jprewards.top[/TD][TD]View the leaderboard with /rewards top.[/TD][TD]true[/TD][/TR]
[TR][TD]jprewards.update[/TD][TD]Receive the new-version notice.[/TD][TD]OP[/TD][/TR]
[TR][TD]jprewards.admin[/TD][TD]estado, validar, reload, datos.[/TD][TD]OP[/TD][/TR]
[TR][TD]jprewards.admin.manage[/TD][TD]dar, test, reset, set.[/TD][TD]OP[/TD][/TR]
[TR][TD]jprewards.admin.migrate[/TD][TD]migrar.[/TD][TD]OP[/TD][/TR]
[TR][TD]jprewards.*[/TD][TD]Everything above.[/TD][TD]OP[/TD][/TR]
[/TABLE]

The permissions of the rank variants are yours: put the permission you already use ([B]group.vip[/B], [B]jprewards.rewards.vip[/B]…) in the reward file.

[SIZE=5][B]Configuration[/B][/SIZE]
[CODE]
# plugins/JPRewards/config.yml (excerpt)
idioma: en                 # en or es; messages_<idioma>.yml

modulos:
  diaria: true
  semanal: true
  mensual: true
  rachas: true

reinicio:
  hora: 0                  # hour at which a new day starts (0 = midnight)

diaria:
  dias-del-ciclo: 0        # 0 = as many days as diarias.yml defines
  perder-racha-al-faltar: true

semanal:
  semanas-del-ciclo: 0
  dia-inicio: lunes        # spanish or english day names
  perder-racha-al-faltar: true

entrega:
  guardar-pendientes: true # items that do not fit are given on the next login

avisos:
  al-entrar:
    tipo: 'actionbar'      # actionbar (falls back to chat) or chat
  auto-reclamar:
    activado: false

almacenamiento:
  tipo: yaml               # yaml or mysql (JDBC)
  modo: por-jugador        # por-jugador (one file per player) or unico
  autoguardado-segundos: 300
  mysql:
    host: '127.0.0.1'
    puerto: 3306
    base-de-datos: 'jprewards'
    usuario: 'root'
    contrasena: ''
    prefijo-tablas: 'jprewards_'

actualizaciones:
  activado: false          # optional SpigotMC version check
  id-recurso: 0            # the number in the resource URL
[/CODE]

Every option is commented in Spanish and English inside the file, and [B]/rewards validate[/B] tells you when a value is out of range or a file does not match the shipped one.

[SIZE=5][B]Rewards[/B][/SIZE]
[CODE]
# plugins/JPRewards/rewards/diarias.yml (excerpt)
dias:

  '1':
    icono:
      textura: 'eyJ0ZXh0dXJlcyI6...'     # head texture (minecraft-heads.com)
    lore:
      - '&7▪ &f50 &7monedas'
      - '&7▪ &f10 &7experiencia'
    comandos:
      - 'eco give {jugador} 50'
    experiencia: 10

  '4':
    lore:
      - '&7▪ &f150 &7monedas'
    comandos:
      - 'eco give {jugador} 150'
    items:
      - material: GOLDEN_APPLE
        cantidad: 2
        nombre: '&6Manzana dorada'
    mensajes:
      - '&7Consejo: &fusa /rewards&7 para ver todo tu progreso.'
    efectos:
      sonido: ENTITY_EXPERIENCE_ORB_PICKUP

  '7':
    lore: [ '&7▪ &f500 &7monedas' ]          # what a player with no rank sees
    comandos: [ 'eco give {jugador} 500' ]
    rangos:
      mvip:
        permiso: 'group.mvip'                 # highest ranks first
        lore: [ '&8Rango MVIP', '&7▪ &f2500 &7monedas' ]
        comandos: [ 'eco give {jugador} 2500' ]
      vip:
        permiso: 'group.vip'
        lore: [ '&8Rango VIP', '&7▪ &f1200 &7monedas' ]
        comandos: [ 'eco give {jugador} 1200' ]
[/CODE]

Monthly milestones use [B]dias-requeridos[/B] and streak bonuses use [B]tipo[/B] (diaria or semanal) plus [B]racha-minima[/B]. Every entry accepts the same fields: [B]icono[/B], [B]nombre[/B], [B]permiso[/B], [B]lore[/B], [B]comandos[/B], [B]items[/B], [B]experiencia[/B], [B]niveles[/B], [B]mensajes[/B], [B]efectos[/B] and [B]rangos[/B]. The [B]lore[/B] lines accept the same placeholders as the menus (the shipped files use [B]{moneda}[/B], the currency name configured in config.yml).

[SIZE=5][B]Menus[/B][/SIZE]
Everything about the menus lives in [B]menus.yml[/B]: titles, textures, slots, states, pagination and navigation. Buttons accept [B]material[/B], [B]textura[/B], [B]textura-jugador[/B], [B]cantidad[/B], [B]nombre[/B], [B]lore[/B], [B]brillo[/B], [B]encantamientos[/B], [B]item-flags[/B], [B]custom-model-data[/B] (1.14+), [B]irrompible[/B], [B]icono-recompensa[/B], [B]permiso[/B], [B]ocultar-sin-permiso[/B] and [B]accion[/B]. States (available, claimed, locked, completed…) have their own name, lore, texture and action. Supported actions: open:<menu>, close, none, reclamar:<tipo>, claim:milestone:<id>, claim:streak:<id>, command:<cmd>, jugador-command:<cmd>, mensaje:<key> and sonido:<name>. Menus are read-only: every click is cancelled.

[B]The bundled menus are in English[/B] and a complete Spanish copy ships as [B]menus_es.yml[/B]. Every visible menu text is read from that file, so translating it (or keeping several files and choosing one with [B]menus.archivo[/B]) needs no code changes.

[SIZE=5][B]Storage and migration[/B][/SIZE]
[LIST]
[*][B]YAML (default)[/B]: one readable [B]datos/<uuid>.yml[/B] per player, or everything in [B]datos/datos.yml[/B] with [B]modo: unico[/B]. Every write is atomic (.tmp + rename) and a file that cannot be parsed is backed up as .bak and reported instead of being overwritten with empty data.
[*][B]MySQL / MariaDB (JDBC)[/B]: one table ([B]<prefijo>jugadores[/B]) created automatically. The driver is the one the server ships (Spigot and Paper include MySQL Connector/J); nothing is bundled in the jar.
[*][B]If the database does not respond at startup[/B], the plugin retries the connection (`almacenamiento.mysql.reintentos`, with a growing wait between attempts), logs the error and keeps working on YAML, so players never lose their rewards.
[*][B]Migrating[/B]: set [B]almacenamiento.tipo[/B] to the destination and restart the server (the plugin starts on the new, still empty backend), then run [B]/rewards migrate confirmar[/B]. It copies every player from the backend you were using before and overwrites players that already exist in the destination. Connected players see the copied data after reconnecting, and the old data is only read, never deleted.
[/LIST]

[SIZE=5][B]Placeholders[/B][/SIZE]
Inside menus and messages: {jugador} {dia} {racha} {mejor} {total} {semana} {semanas} {racha_semanal} {dias_mes} {dias_mes_total} {hitos} {hitos_disponibles} {bonus} {bonus_disponibles} {faltan} {tiempo} {tiempo_semanal} {moneda} {barra_mes} {barra_ciclo} {barra_racha} {version} {servidor} {almacenamiento} {preview} and more.

With [B]PlaceholderAPI[/B] installed (optional): [B]%jprewards_dia%[/B], [B]%jprewards_racha%[/B], [B]%jprewards_mejor_racha%[/B], [B]%jprewards_total%[/B], [B]%jprewards_disponible_diaria%[/B], [B]%jprewards_tiempo_diaria%[/B], [B]%jprewards_racha_semanal%[/B], [B]%jprewards_dias_mes%[/B], [B]%jprewards_hitos_disponibles%[/B], [B]%jprewards_bonus_disponibles%[/B], [B]%jprewards_disponibles%[/B], [B]%jprewards_premio_diaria%[/B] and more.

[SIZE=5][B]Installation[/B][/SIZE]
[LIST=1]
[*]Drop [B]JPRewards-2.1.0.jar[/B] into the server's [B]plugins/[/B] folder.
[*]Start the server. It creates config.yml, messages_es.yml, messages_en.yml, menus.yml and the recompensas/ folder (existing files are never overwritten).
[*]Run [B]/rewards validate[/B] to check that everything is fine.
[*]Adjust the YAML files and run [B]/rewards reload[/B].
[*]Optional: set [B]idioma: en[/B] for English chat messages, and grant the rank variant permissions you use.
[/LIST]

[SIZE=5][B]Compatibility[/B][/SIZE]
[TABLE]
[TR][TH]Item[/TH][TH]Value[/TH][/TR]
[TR][TD]Minecraft[/TD][TD]1.8 through the latest version[/TD][/TR]
[TR][TD]Server software[/TD][TD]Spigot, Paper, Purpur and forks that implement the Bukkit API[/TD][/TR]
[TR][TD]Java[/TD][TD]Java 8+ for the plugin; the server itself needs the Java version its Minecraft release requires (1.21+ → Java 21)[/TD][/TR]
[TR][TD]Dependencies[/TD][TD]None. PlaceholderAPI is optional[/TD][/TR]
[TR][TD]Built against[/TD][TD]Spigot API 1.8.8, Java 8 bytecode (class file version 52)[/TD][/TR]
[TR][TD]Tested with[/TD][TD]The bundled integration harness on Paper 1.8.8 (JDK 8) and Spigot 1.21.1 (Java 21): 29 checks, 0 failures on both[/TD][/TR]
[/TABLE]

[SIZE=5][B]Notes and limits[/B][/SIZE]
[LIST]
[*]English is the default; [B]messages_es.yml[/B], [B]menus_es.yml[/B] and the four [B]recompensas/*_es.yml[/B] presets turn everything into Spanish with six options in [B]config.yml[/B].
[*][B]custom-model-data[/B] needs Minecraft 1.14 or newer; it is ignored on older versions.
[*]Particles, sounds and materials that do not exist on your Minecraft version are skipped, and [B]/rewards validate[/B] warns about them. On 1.8 the plugin translates the modern names it knows to their legacy equivalent.
[*][B]irrompible[/B] uses the real API on 1.11+ and is approximated on older versions.
[*]No Folia support: the plugin uses the standard Bukkit scheduler.
[*]Rewards are delivered with commands you configure, so the money, items or keys come from your other plugins; if one of those commands fails, the plugin logs it and the rest of the reward is still given.
[/LIST]

[SIZE=5][B]FAQ[/B][/SIZE]
[B]Do I need Vault or an economy plugin?[/B]
No. Money is given with whatever command your server already uses (eco give, cmi money add…). Vault is not required.

[B]Can I have different rewards for different ranks?[/B]
Yes, in two ways: an exclusive entry with [B]permiso: 'group.vip'[/B], or [B]rangos:[/B] variants where the first variant whose permission the player has wins and everything you leave out is inherited from the base entry.

[B]What happens when a player's inventory is full?[/B]
The item is stored and given the next time they join (or dropped on the ground, depending on [B]entrega.guardar-pendientes[/B]). It is never lost silently.

[B]Can I test rewards without waiting for days to pass?[/B]
Yes. [B]/rewards test diaria 7[/B] shows what would be delivered without delivering anything, [B]/rewards give <player> diaria 7[/B] delivers it for real, and [B]/rewards set <player> racha 6[/B] leaves a player one day away from a streak bonus.

[B]Does it work with MySQL?[/B]
Yes, optionally: [B]almacenamiento.tipo: mysql[/B] stores the data in a database over JDBC using the driver your server ships. If the database is down the plugin keeps working on YAML, and [B]/rewards migrate confirmar[/B] copies the data between the two backends.

[B]Does it send anything to the internet?[/B]
No bStats, no metrics. The only optional connection is the update check ([B]actualizaciones.activado[/B], off by default), which only reads the resource version number from the public SpigotMC API.

[B]Can I translate it?[/B]
Yes. Chat: copy [B]messages_en.yml[/B] to [B]messages_<code>.yml[/B], translate it and set [B]idioma: <code>[/B]; missing keys fall back to the bundled English file. Menus: edit the texts inside [B]menus.yml[/B] (or keep your own copy and point [B]menus.archivo[/B] at it). Spanish presets for the menus and the example rewards are included as [B]_es.yml[/B] files.

[SIZE=5][B]Support[/B][/SIZE]
Questions and bug reports: the discussion thread below (or your Discord). Please include the server version, the plugin version, the output of [B]/rewards validate[/B] and the relevant console log.

[SIZE=5][B]License[/B][/SIZE]
MIT. You may use, modify and redistribute the plugin, including on commercial servers, as long as the copyright notice is kept.

[HR][/HR]
[SIZE=3][COLOR=#808080]JPRewards is not affiliated with Mojang or Microsoft. "Minecraft" is a trademark of Mojang AB.[/COLOR][/SIZE]
```

## 4. Before pressing "Submit"

- [ ] `mvn clean package` produces `target/JPRewards-2.0.0.jar` and its `plugin.yml` says `version: 2.0.0`.
- [ ] The jar was tested on a **fresh** server folder (no leftover configuration) on the latest Minecraft version.
- [ ] `python tools/validar-recursos.py` ends with `RESULTADO: 0 error(es)`.
- [x] `powershell -ExecutionPolicy Bypass -File .\testing\run-tests.ps1` passes on both test servers: 29 checks, 0 failures on Paper 1.8.8 and on Spigot 1.21.1 (logs in [VERIFICATION.md](VERIFICATION.md)). Re-run it if any source file changes after that run.
- [x] **Default language is English** (`idioma: en`, plus `menus.yml` and the example rewards in English). Spanish presets ship as `menus_es.yml` and `recompensas/*_es.yml`.
- [ ] `plugin.yml → website:` points at the real resource URL.
- [ ] At least one image is uploaded (SpigotMC requires it); screenshots and captions match the section 2 shot list.
- [ ] The description above is pasted and renders correctly in the preview (tables, lists and code blocks).
- [ ] The support URL (discussion thread / Discord / repository) is filled in the form.
- [ ] The resource is marked **free** and the jar is uploaded directly (no external download link).
- [ ] The latest Minecraft version is selected as native and included in the supported range.
- [ ] No server IP, advertisement or unrelated link is in the description.
- [ ] The jar is not obfuscated (it is not), contains no DRM and bundles no third-party library.
- [ ] After publishing, note the resource id (`actualizaciones.id-recurso`) in the description or in the next update.

## 4b. Real in-game screenshots

The `real-*.png` banners are **not mockups**: they are captured from a real client.

```powershell
powershell -ExecutionPolicy Bypass -File tools\capturar-juego.ps1   # captura los menus
python docs/assets/componer-capturas.py                            # los monta en 1280x720
```

The first script starts the 1.8.8 test server with JPRewards, launches the 1.8.9 client installed on the
machine (it connects by itself), asks a tiny helper plugin to open each menu as the player and captures
the window. The second one crops the game window and puts it on the brand background. It needs a
Minecraft client installed and a desktop session, which is why it is not part of the CI.

## 5. After publishing

- Reply to reviews and bug reports in the discussion thread.
- Keep the native Minecraft version updated when a new release comes out (SpigotMC requires support for the latest stable version).
- Publish the same text on the release page of your repository and keep `CHANGELOG.md` in sync.

## 6. Publishing an update

Two scripts automate most of this, see [tools/spigot-auto/README.md](../tools/spigot-auto/README.md):

| Script | What it does |
| --- | --- |
| `tools/publicar-spigot.ps1 -ResourceId <id>` | Builds, validates, writes `dist/JPRewards-<version>.jar` + `TITULO.txt` + `DESCRIPCION.bbcode`, copies the description to the clipboard and opens the resource page. Two clicks left. |
| `tools/spigot-auto/subir.js` | Fills the update form in a real browser (Edge through Playwright) and submits it. SpigotMC has no API, so this is the only full automation; it can break when their HTML changes. |

Every release after the first one is published from **Resource → Post an update**, which asks for a
version, an update title and a message. The message accepts BBCode and is what users see in their
alerts, so it should stand on its own:

1. **Version heading** and one sentence about what the update is.
2. **New features**, most important first.
3. **Behaviour changes** and anything that could surprise an existing installation.
4. **Updating**, numbered: back up, replace the jar, restart, optional steps, new permissions.
5. **Verified**, the versions actually tested.
6. **Footer** with the changelog/source links, the SHA-256 of the jar and the note that no
   configuration change is necessary when that is the case.
