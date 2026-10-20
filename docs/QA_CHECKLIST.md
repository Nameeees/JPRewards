# Release QA checklist

Version under test: **2.0.0** · Date: **2026-10-18** · Author: JP

This checklist is the minimum gate before uploading a jar to SpigotMC. Boxes marked `[x]` were
verified while preparing the release and the evidence is in `testing/logs/` or in the build output;
boxes marked `[ ]` must be checked (or deliberately accepted) before pressing "Submit".

Related documents: [SPIGOTMC_PAGE.md](SPIGOTMC_PAGE.md) (resource text and form values),
[CONFIGURACION.es.md](CONFIGURACION.es.md) (configuration guide), `../testing/README.md` (how the
test servers and the harness work).

## 1. Build gate

- [x] `mvn clean package` finishes with `BUILD SUCCESS` and produces `target/JPRewards-2.0.0.jar`
      (final frozen artifact: 255,422 bytes, SHA-256
      `7259b8f1584c786dfb7ead6ce3a1632f9880d9a88ea572d720e5b94e0a3f3c03`).
- [x] The `plugin.yml` inside the jar contains `name: JPRewards`, `version: '2.0.0'`,
      `main: me.jp.jprewards.JPRewards`, `api-version: '1.13'`, `authors: [JP]` and
      `softdepend: [PlaceholderAPI]`.
- [x] The jar bundles the managed files: `config.yml`, `messages_en.yml` (default) and
      `messages_es.yml`, `menus.yml` (English default) and `menus_es.yml`, plus
      `recompensas/{diarias,semanales,mensuales,rachas}.yml` and their four `_es.yml` presets.
- [x] Every class is Java 8 bytecode (`major version: 52`, checked on `me/jp/jprewards/JPRewards.class`),
      so a 1.8 server running Java 8 can load the same jar as a modern server.
- [x] The jar contains 74 plugin classes and **no shaded library** (nothing from `me.clip` or any
      other third-party package is inside).
- [x] No old `messages.yml` is bundled in the jar; the rename to `messages_es.yml` only happens on a
      user's disk when upgrading from 1.x.
- [x] Two consecutive clean builds produce the same SHA-256: verified (the pom fixes the output
      timestamp and drops the Maven descriptor, so editing it cannot change the jar).
- [x] `gradle build` produces the equivalent jar: `build/libs/JPRewards-2.0.0.jar` (its size differs
      from the Maven jar only in jar metadata, not in code).
- [x] The jar is not obfuscated and contains no DRM: class names are readable as shipped and there are
      no licence checks.

## 2. Resource validator (fast, no server)

```bash
python tools/validar-recursos.py     # needs PyYAML
```

- [x] Last run: **0 errors, 0 warnings**. It parses every bundled YAML (duplicate keys are an error),
      checks that every option read by `Ajustes.java`/`AjustesSql.java` exists in `config.yml`,
      that every message key used in the code exists in both `messages_es.yml` and
      `messages_en.yml`, that the menu ids exist and the button slots fit inside the chest, that
      actions point at existing menus and milestones, that milestones and streak bonuses define their
      required fields, and that no unknown `{placeholder}` is used.
- [x] Re-run after the last edit to `src/main/resources/`: still **0 errors, 0 warnings**, and the
      managed files (including the four `recompensas/*.yml`) all carry `config-version >= 2`.
- [ ] Run it again if anything under `src/main/resources/` changes after the release build.

## 3. Integration harness (real servers)

```powershell
powershell -ExecutionPolicy Bypass -File .\testing\run-tests.ps1
```

(`pwsh` is not on the PATH of the build machine; if your execution policy allows scripts, plain
`.\testing\run-tests.ps1` also works.)

It builds the plugin and the harness, copies them into `.test-servers/paper-1.8.8` (Paper 1.8.8 +
JDK 8) and `.test-servers/spigot-1.21.1` (Spigot 1.21.1 + Java 21), starts each server, collects the
`CHECK` lines and shuts the server down. Logs go to `testing/logs/`.

Final run on the frozen 2.0.0 sources (2026-10-18 17:47, `testing/logs/resumen-20261018-182723.txt`):

| Server | Checks | OK | Failed | Verdict |
| --- | --- | --- | --- | --- |
| Paper 1.8.8 (JDK 8 / Temurin 8u504) | 22 | 22 | 0 | **HARNESS PASS** |
| Spigot 1.21.1 (Java 21 / Temurin 21.0.11) | 22 | 22 | 0 | **HARNESS PASS** |

- [x] Both servers print `HARNESS RESULT checks=29 failures=0` / `HARNESS PASS` with the release jar,
      and the script exits 0 (`RESULTADO: TODO PASA`). The gate is green.
- [x] The single defect found by the earlier runs (the four `recompensas/*.yml` files had no
      `config-version`) is fixed and re-verified: the harness now reports "los 8 archivos gestionados
      tienen config-version >= 2".
- [ ] Re-run the suite if any source file changes after the frozen run, and update
      [VERIFICATION.md](VERIFICATION.md) with the new date, server builds and Java versions.

What the harness covers: plugin enabled; `/rewards` registered with the `/rec` alias, executor
and tab completer; creation of the 13 default files from an empty folder; YAML parse with no
duplicate keys; `config-version`; eleven English subcommands without exceptions (`info`,
`validate`, `help`, `top`, `status`, `data <player>`, `give <player> daily`, `reload`,
`test daily`, `set <player> streak 3`, `reset <player> daily`); the legacy `/recompensas` alias
still answering; the menu-action normalizer accepting the 1.x names; `DatosCodec` round trip;
`AlmacenYaml` save and reload; the config merge (user value preserved, new sections inserted,
`.bak` written, valid YAML); the message file structure; and 3 seconds of scheduler stability.

**Not covered by the harness** (do the manual checks below): opening and clicking menus, item
delivery into a real inventory, effect rendering, PlaceholderAPI, a real MySQL/MariaDB server,
migration between backends, and anything that needs a real Minecraft client.

## 4. Manual in-game matrix

Needs a test server and a real client. Tick every row on the release that touches menus, rewards or
storage.

### Menus and states

- [ ] `/rewards` opens the main menu with the frame, the daily/weekly/monthly/streak buttons, the streak summary, "claim all" and "how it works".
- [ ] The daily, weekly, monthly, streak and info menus open, show the configured textures and paginate when the cycle does not fit on one page.
- [ ] A day already claimed shows the claimed head/name/lore; a locked day shows the locked variant; "today" is highlighted.
- [ ] `{preview}` prints the reward `lore` lines inside the button, and the "rank only" hint appears on entries with `permiso`.
- [ ] A reward `lore` line that uses a placeholder (the shipped files use `{moneda}`) is rendered with the configured value inside `{preview}`.
- [ ] `icono-recompensa: true` uses the reward head, including the rank variant the player has.
- [ ] Clicking a decorative slot plays the click sound and does nothing else.
- [ ] No item can be taken out of any menu (clicks are cancelled), and shift-clicking does not move anything.
- [ ] `abrir:<menu>`, `cerrar`, `comando:`, `jugador-comando:`, `mensaje:` and `sonido:` actions work on a custom button.
- [ ] A custom `menu-<name>` section opens with `abrir:menu-<name>`.
- [ ] `permiso` + `ocultar-sin-permiso` hide a button for a player without that permission.

### Rules of each reward system

- [ ] Daily: claim day 1, reconnect, the next day arrives (or use `/rewards set <player> ...` and the reset hour), and the cycle advances.
- [ ] Daily: finishing the last day of the cycle returns to day 1.
- [ ] Daily streak: claiming consecutive days increases the streak; missing a day resets it to 1 with `diaria.perder-racha-al-faltar: true`, and keeps it with `false`.
- [ ] Weekly: the reward can only be claimed once per week; the week starts on `semanal.dia-inicio` at `reinicio.hora`.
- [ ] Monthly: claiming the daily on enough days unlocks the milestone exactly at `dias-requeridos`; claiming it twice is refused; the progress bar matches the days claimed.
- [ ] Monthly: changing month resets the progress and the claimed milestones (the historical counter used by `/top hitos` stays).
- [ ] Streaks: a 7-day streak unlocks `racha-7` once; losing the streak makes it available again; a weekly bonus (`tipo: semanal`) works with weeks.
- [ ] `reclamar:todo` (button and `/rewards claim todo`) claims every pending reward and reports the count.
- [ ] With a module disabled in `config.yml`, its button shows the locked state and claiming it is refused with the "module disabled" message.

### Delivery, items and effects

- [ ] Commands run from the console with `{jugador}`, `{uuid}`, `{mundo}` and `{premio}` replaced.
- [ ] Direct items arrive with their amount, name, lore, enchantments and flags.
- [ ] With a full inventory and `entrega.guardar-pendientes: true`, the item is stored, the player is warned, and it is delivered on the next login.
- [ ] With `guardar-pendientes: false`, the item is dropped next to the player.
- [ ] `experiencia` and `niveles` are delivered (and `entrega.experiencia-directa: false` disables the direct XP).
- [ ] Per-reward sound, volume, pitch, particles, fireworks, screen title, action bar, extra messages and `broadcast` behave as configured.
- [ ] `efectos.titulos: false`, `efectos.particulas: false` and `efectos.fuegos-artificiales: false` disable each effect globally.
- [ ] A reward whose command fails (for example a typo in `eco give`) logs the error and still delivers the rest of the reward.
- [ ] A reward with a `rangos` variant gives the right variant to a VIP account and the base entry to a normal one.

### Notices and auto-claim

- [ ] The join notice appears after `avisos.al-entrar.retraso-segundos`, only for what can be claimed, only with `jprewards.notify`, and in the configured `actionbar`/`chat` form.
- [ ] With `auto-reclamar.activado: true` and `pedir-permiso: true`, only players with `jprewards.autoclaim` get the automatic claim and the confirmation message.
- [ ] Pending items are delivered on login with the "pending rewards" message.

### Commands and permissions

- [ ] `/rewards help` shows the player help, and the admin/management blocks only to those who have the permissions.
- [ ] A player without `jprewards.use` cannot open menus or use `/rewards claim`.
- [ ] A player without `jprewards.claim.daily` (or weekly/monthly/streak) cannot claim that type from a menu or from the command.
- [ ] Claiming twice in a row (daily, weekly, milestone, streak) delivers the reward **only once**: the second attempt shows the "already claimed" message and nothing is delivered. There is no permission that can bypass this.
- [ ] Console cannot open menus ("players only") and `/rewards give` is refused outside the game.
- [ ] Tab completion suggests subcommands, types, keys, online players, `set` fields and `reload` targets, and hides admin subcommands from players.
- [ ] `/rewards top` lists the configured amount (`top.cantidad`), the five rankings work and the message appears when there is no data.
- [ ] `jprewards.top` (default `true`) gates the leaderboard: a player denied that node gets the no-permission message, and `jprewards.*` grants it.
- [ ] `/rewards data <player>` shows UUID, streaks, monthly days, milestones, bonuses, pending items and dates for an online and a known offline player.

### Diagnostics and testing tools

- [ ] `/rewards validate` reports no errors on the default files (both `nada` and `ninguna` are accepted as no-op actions, which is what the shipped menus use).
- [ ] Deliberately break one thing per type (unknown material, sound and particle; a slot outside the chest; an `abrir:` pointing at a nonexistent menu; a reward with no delivery; a bad head texture; an unknown placeholder) and check that each one is reported with its file and key.
- [ ] `/rewards test <tipo> <clave>` prints the commands, items, XP, effects and broadcast of that entry, uses your rank variant when run in game, and delivers nothing.
- [ ] `/rewards give <player> diaria 7` delivers that entry without touching counters or the time gate.
- [ ] `/rewards reset <player> <tipo>` and `/rewards reset * todo` reset what they should (and the server-wide one is handled with care).
- [ ] `/rewards set <player> <campo> <valor>` changes each of the seven fields, rejects a non-number and rejects an unknown field.
- [ ] `/rewards status` shows the real storage (YAML even when `tipo: mysql` is configured and the database was down) and the configuration warnings.

### Reload and persistence

- [ ] `/rewards reload` (and each of `config`, `mensajes`, `menus`, `recompensas`) applies the changes and closes the open menus.
- [ ] Editing a reward and running `/rewards reload recompensas` changes what is delivered without a restart.
- [ ] Restarting the server keeps streaks, totals, monthly days, claimed milestones/bonuses and pending items.
- [ ] Quitting with pending changes and restarting does not lose progress (autosave plus the save on quit and shutdown).

### Configuration options

- [ ] Changing `reinicio.hora` moves the moment at which a new day starts.
- [ ] `diaria.dias-del-ciclo` and `semanal.semanas-del-ciclo` accept a fixed number and `0` (automatic).
- [ ] `idioma: es` switches the chat to Spanish (`messages_es.yml`) and `idioma: en` back (English is the default).
- [ ] Deleting a message key makes the plugin use the bundled English text, and `/rewards validate` lists the missing keys.
- [ ] Emptying a message key silences that message.
- [ ] An out-of-range value (for example `sonidos.volumen: 5`, `top.cantidad: 999`, `reinicio.hora: 30`) is corrected, warned in the console and listed by `validate`.
- [ ] `menus.gradiente-titulos`, `menus.gradiente-colores`, `menus.cerrar-al-reclamar` and `menus.sonido-al-hacer-clic` change the menus as expected.
- [ ] `barra-progreso.*` changes the look of the bar placeholders.
- [ ] `menus.archivo` pointing at a second file (`menus_en.yml`) switches the whole menu preset, and pointing it back restores the original (no restart needed with `/rewards reload menus`).
- [ ] `economia.moneda` changes the `{moneda}` placeholder.

### Storage: YAML

- [ ] `modo: por-jugador` writes `datos/<uuid>.yml` and `modo: unico` writes everything into `datos/datos.yml`.
- [ ] Manually corrupt a data file: the plugin reports it, saves a `.bak` copy and starts that player fresh instead of overwriting silently.
- [ ] `autoguardado-segundos: 0` stops the periodic save but quit, claim and shutdown still save.
- [ ] `guardar-al-reclamar: true` writes the data at claim time (kill the server right after a claim and check that the claim is not lost).

### Storage: MySQL and migration (needs a real MySQL/MariaDB)

- [ ] With valid credentials, the console logs the driver in use and the table `<prefijo>jugadores` is created with `uuid`, `nombre`, `data`, `actualizado`.
- [ ] Claim something, restart the server: the progress is still there (the data really lives in the database).
- [ ] Start the server with the database down: the console shows the retry attempts (`almacenamiento.mysql.reintentos` + 1 tries with a growing wait), then explains the failure and the plugin keeps working on YAML; `/rewards validate` flags the mismatch.
- [ ] Set `almacenamiento.mysql.reintentos: 0`: the plugin tries once and falls back to YAML immediately.
- [ ] YAML → MySQL: set `almacenamiento.tipo: mysql` and restart (the plugin starts on the empty
      database), then run `/rewards migrate confirmar`; every player's data ends up in the table and
      connected players see it after reconnecting.
- [ ] MySQL → YAML: the same with `almacenamiento.tipo: yaml`; the files are written and readable.
- [ ] With the configured type **not** active (database configured but unreachable at startup, so the
      plugin is on YAML), `/rewards migrate confirmar` is refused with `comando.migrar-error` and
      points at the startup console.
- [ ] The source backend is only read, never deleted, after a migration, and a migration over a
      couple of hundred players does not stall the server.

### PlaceholderAPI (optional)

- [ ] Without PlaceholderAPI installed the plugin works exactly the same (only `%...%` in its texts is not resolved).
- [ ] With PlaceholderAPI installed, `%jprewards_racha%`, `%jprewards_dia%`, `%jprewards_total%`, `%jprewards_dias_mes%`, `%jprewards_hitos_disponibles%`, `%jprewards_bonus_disponibles%`, `%jprewards_disponibles%`, `%jprewards_disponible_diaria%`, `%jprewards_tiempo_diaria%` and `%jprewards_premio_diaria%` return the expected values.
- [ ] `%jprewards_racha_<player>%` works for an online player.
- [ ] A placeholder from another plugin written inside a message or a menu lore is resolved.

### Compatibility

- [ ] 1.8 server + 1.8 client: menus open, titles render, sounds play, no client crash and no console exception.
- [ ] Latest server + latest client: same checks.
- [ ] On 1.8, a reward that uses modern names (`PLAYER_HEAD`, `SHARPNESS`, `HAPPY_VILLAGER`, `ENTITY_PLAYER_LEVELUP`) is translated and works.
- [ ] On a version that does not support an option, using it does not break anything: `custom-model-data` is ignored on 1.13 and older, `irrompible` degrades, and a sound/particle/material that does not exist is skipped with a `validate` warning.
- [ ] Server restart with `/reload confirm` (or a plugin reload) leaves the plugin working.

## 5. Performance sanity check

- [ ] With 20+ players online, `/tps` stays at 20 during autosave and during claims.
- [ ] Opening a menu and claiming is instant with 1,000+ player files (data is loaded per player).
- [ ] `/rewards top` on a full data folder does not stall the server (it is calculated off the main thread and cached for a minute).
- [ ] A migration of a large data folder keeps the tick rate stable (it runs asynchronously).

## 6. Open items found while documenting 2.0.0

These are real, current findings — decide each one before the release (fix, accept, or state it in
the resource page). They are listed here so nothing ships by accident:

1. **`menus.yml` uses `accion: 'ninguna'`** on the decorative buttons (streak summary, "how it
   works", monthly/streak progress…). The click handler treats any unknown action as "just play the
   click sound", and the Python validator accepts `ninguna`, but the in-game `/rewards validate`
   only accepts `nada`, `cerrar` and empty as no-ops, so those buttons will be reported as an
   unrecognised action. Change them to `nada` (or teach the validator about `ninguna`) before
   releasing, so a fresh install validates clean.
2. **`comando.migrar-config` is now dead text**: `Migrador` migrates from the previous backend into
   the active one and now sends `comando.migrar-error` when the configured store is not the one in
   use, so `migrar-config` is no longer used by any code. Delete the key from `messages_es.yml` and
   `messages_en.yml` (or reuse it) so the files do not carry unused lines. The migration procedure
   itself matches the documentation: restart on the destination, then `/rewards migrate confirmar`.
3. **Migration and the MySQL backend are not covered by automated tests**: there is no database check
   in the harness, so the fallback, the table layout and both migration directions rest on the manual
   rows above.
4. **PlaceholderAPI is compiled but not executed** by the tests: verify the `%jprewards_...%`
   placeholders on a server that has it installed.
5. **No real-client test in this repository**: menus, clicks, item delivery and effects have not been
   exercised by a Minecraft client here (the harness drives commands and classes, not a player).
6. **`plugin.yml → website:` still points at the generic `https://www.spigotmc.org/resources/`**;
   update it to the resource URL before building the release jar.
7. **`actualizaciones.id-recurso` is `0`** (the notice is disabled). Fill in the real resource id
   after publishing so the optional update check can work.
8. **Language default** (resolved): `config.yml` ships `idioma: en` and the English menus;
    Decide whether the released default should be English (SpigotMC audience) and/or translate the
    menu texts (`menus.archivo` lets you ship a second menu file and switch preset without editing
    the original).
9. ~~**`testing/README.md` result table**~~ — resolved: it now shows the final run (29/29,
    `PASA` on both servers) and the same log files as [VERIFICATION.md](VERIFICATION.md).
10. **Keep the artifact numbers in sync**: the frozen jar is 255,422 bytes with SHA-256
    `7259b8f1584c786dfb7ead6ce3a1632f9880d9a88ea572d720e5b94e0a3f3c03`. If it is rebuilt, update
    [VERIFICATION.md](VERIFICATION.md), this checklist and the release notes with the new hash.
11. ~~**`plugin.yml` wording**~~ — resolved: the permission descriptions are in English, the
    leaderboard keeps its own `jprewards.top` node (default `true`, included in `jprewards.*`)
    and no permission skips the claim checks.
12. **Replace the `USER` placeholder** before pushing the repository: it appears in the CI badge of
    [README.md](../README.md) and in the links of
    [.github/ISSUE_TEMPLATE/config.yml](../.github/ISSUE_TEMPLATE/config.yml). One search and
    replace with the real GitHub account is enough.

## 7. Sign-off

- [x] `CHANGELOG.md` describes 2.0.0 (added / changed / fixed).
- [x] [VERIFICATION.md](VERIFICATION.md) records the final build, the resource validation, the
      harness run on both servers and the list of what was not covered automatically.
- [x] `README.md` (English), `README.es.md` (Spanish) and the SpigotMC description match the
      behaviour, including the English-by-default plus Spanish-presets decision.
- [x] **Artwork generated and committed** in `docs/assets/`: banner (1280×420), icon (512×512),
      seven infographics (1280×720) and two CTA banners (1280×260). Regenerating the three scripts
      produces byte-identical files, and the footer version matches the released 2.0.0.
- [x] **Menus abiertos con un cliente real** (1.8.9 contra Paper 1.8.8): el hub, diario, semanal,
      mensual, rachas e info se abren sin excepciones. Esta prueba encontro el `NullPointerException`
      de `Menu` al calcular las paginas antes de crear el inventario, ya arreglado; las capturas
      resultantes estan en `docs/assets/capturas/` y en los banners `real-*.png`.
- [ ] `docs/SPIGOTMC_PAGE.md` checklist completed (form values, links, free resource) and the
      eleven images uploaded in the order listed in its section 2.
- [x] `testing/README.md` result table updated with the final harness run (29/29 on both servers).
- [ ] SHA-256 of the uploaded jar recorded in the release notes
      (`7259b8f1584c786dfb7ead6ce3a1632f9880d9a88ea572d720e5b94e0a3f3c03` for the frozen build).
- [ ] The release archive contains the jar and the documentation only: `testing/`, `.test-servers/`,
      `target/` and `build/` are development artifacts and stay out of it.
- [x] **GitHub files ready**: `CONTRIBUTING.md`, `SECURITY.md`, `.editorconfig`, `.gitattributes`,
      `.github/workflows/build.yml` (build on JDK 8 and 21, bytecode check, resource validation,
      release job on tags), the issue templates (bug and feature), the pull request template and
      `dependabot.yml`.
- [x] **Documentation set**: `docs/README.md` and `docs/README.es.md` (index), `docs/COMMANDS.md`,
      `docs/PLACEHOLDERS.md`, `docs/FAQ.md`, `docs/TEMPLATE.md`, `docs/CONFIGURACION.es.md`,
      `docs/VERIFICATION.md`, `docs/QA_CHECKLIST.md` and `docs/SPIGOTMC_PAGE.md`.
