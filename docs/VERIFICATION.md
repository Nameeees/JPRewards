# Verification record — JPRewards

This file records what was actually executed for each release, on which
software, and with which result.

## 2.1.0 (current)

- Date: 2026-10-19
- Release artifact: `JPRewards-2.1.0.jar`, 276,689 bytes
  SHA-256 `87d85a3f96737dbdbc3fd3f1a7f2e06bd852333d5eaa7e1bd3c90faca278cc57`
- Harness: **32 of 32 checks pass** on Paper 1.8.8 (JDK 8) and Spigot 1.21.1
  (Java 21); logs `testing/logs/paper-1.8.8-20261019-195722.log` and
  `testing/logs/spigot-1.21.1-20261019-195722.log`.
- New since 2.0.0, verified automatically: rewards in one line, reusable packs
  (`definir`/`usar`/`extra`), conditions (chance, world, one-time only),
  `recompensas/extra.yml` with the public API, and the extra-reward command.
- Resource validator: 0 errors, 0 warnings. Documentation links: 0 broken.

## 2.0.0

This section records what was actually executed for the 2.0.0 release, on which
software, and with which result. Every claim made on the SpigotMC page should be
backed by one of the checks below, by an integration check from
[QA_CHECKLIST.md](QA_CHECKLIST.md), or by a run of the harness described here.

- Date: 2026-10-18
- Build host: Windows, Temurin JDK 21.0.11 (Maven 3.9.16; an equivalent Gradle
  build file is included)
- Release artifact: `JPRewards-2.0.0.jar`, 255,422 bytes
  SHA-256 `7259b8f1584c786dfb7ead6ce3a1632f9880d9a88ea572d720e5b94e0a3f3c03`
  (reproduced byte for byte by two consecutive clean builds)
- Compiles against the Spigot 1.8.8 API, emits Java 8 bytecode, no NMS, no
  shaded libraries, no obfuscation.

## 1. Build verification

| Check | Result |
| --- | --- |
| `mvn clean package` | BUILD SUCCESS |
| `plugin.yml` version substituted from the build file | `version: '2.0.0'` |
| Jar contents | 74 classes + `config.yml`, `messages_en.yml` (default) and `messages_es.yml`, `menus.yml` (English) and `menus_es.yml`, plus the four `recompensas/*.yml` files and their four `_es.yml` Spanish presets |
| Bytecode | `major version: 52` (Java 8) on the main class |
| Reproducible build | Two consecutive clean builds produce the **same** SHA-256 |
| Embedded Maven descriptor | Absent (editing the build file cannot change the jar) |
| Shaded dependencies | None (Spigot API plus PlaceholderAPI as compile-only) |
| Metrics / telemetry | No bStats, no metrics class; the only optional network call is the SpigotMC version check, disabled by default |
| Old artifacts | No `messages.yml` and no `com/tuserver` classes in the jar |

## 2. Resource validation

`python tools/validar-recursos.py` (PyYAML; also run in CI):

| Check | Result |
| --- | --- |
| YAML files parsed without errors or duplicated keys | 9 of 9 |
| `config-version` present and >= 2 in every managed file | yes |
| Options read by the code but missing from `config.yml` | none |
| Message keys used by the code but missing from `messages_es.yml` / `messages_en.yml` | none |
| Menu actions pointing to menus or milestones that do not exist | none |
| Buttons with a slot outside the chest | none |
| Unknown placeholders in menus and messages | none |
| **Result** | **0 errors, 0 warnings** |

## 3. Integration harness (real servers)

`testing/harness` is a test-only plugin (never shipped) that loads next to
JPRewards, drives the real classes and the real commands inside a running
server, prints one `CHECK OK` / `CHECK FAILED` per assertion and stops the
server by itself. Run it with:

```powershell
powershell -ExecutionPolicy Bypass -File .\testing\run-tests.ps1
```

| Server | Java | Result |
| --- | --- | --- |
| Paper 1.8.8 (build 445) | Temurin 8u504 | **PASS — 29 checks, 0 failures** |
| Spigot 1.21.1 (BuildTools build) | Temurin 21.0.11 | **PASS — 29 checks, 0 failures** |

Covered automatically (the same 29 checks on both versions):

- JPRewards enables (2.0.0, `me.jp.jprewards.JPRewards`) and `/rewards` is
  registered with the `/rec` alias, an executor and a tab completer.
- The 13 default files are created: `config.yml`, `messages_en.yml` (the default
  language), `messages_es.yml`, `menus.yml` (English), `menus_es.yml` and the four
  `recompensas/*.yml` files with their four Spanish `_es.yml` presets.
- All 13 YAML files load with `YamlConfiguration` without exceptions and without
  duplicated keys, and every managed file has `config-version >= 2`.
- Eleven console subcommands run without throwing (`info`, `validate`, `help`,
  `top`, `status`, `data <player>`, `give <player> daily`, `reload`, `test daily`,
  `set <player> streak 3`, `reset <player> daily`), the legacy `/recompensas`
  alias still answers, and the menu-action normalizer turns the 1.x action names
  (`abrir:`, `reclamar:diaria`, `cerrar`, `nada`…) into the current ones
  (`open:`, `claim:daily`, `close`, `none`) — 10 of 10 cases. The plugin is still
  enabled afterwards.
- `DatosCodec` round-trip with streak 12, total 40, 9 days of the month, one
  claimed milestone, one claimed streak bonus and two pending items.
- `AlmacenYaml` writes and reads back the same data (one file per player).
- File auto-update: a user value is preserved, 16 new sections are inserted with
  their comments, `config-version` is raised to 2, a `.bak` copy is written and
  the result is still valid YAML without duplicated keys.
- `messages_es.yml` defines the prefix and every expected section.
- Scheduler stability: a repeating task for 3 seconds with no exceptions and the
  plugin still enabled.

Selected log lines (Spigot 1.21.1):

```
CHECK OK: JPRewards activado (version 2.0.0, clase me.jp.jprewards.JPRewards)
CHECK OK: los 8 archivos por defecto se crearon en plugins\JPRewards
CHECK OK: los 8 archivos gestionados tienen config-version >= 2
CHECK OK: comando /rewards validate sin excepciones
CHECK OK: DatosCodec: ida y vuelta correcta con racha 12, total 40, diasMes 9, 1 hito, 1 bonus y 2 items pendientes
CHECK OK: fusion de config: se respeto idioma=harness-valor, aparecieron 16 secciones, config-version=2, YAML valido sin claves duplicadas (copia .bak: si)
HARNESS RESULT checks=29 failures=0 skipped=0
HARNESS PASS
```

Logs: [paper-1.8.8](../testing/logs/paper-1.8.8-20261018-182723.log),
[spigot-1.21.1](../testing/logs/spigot-1.21.1-20261018-182723.log),
[summary](../testing/logs/resumen-20261018-182723.txt).
The suite was executed several times over the release cycle; the last run (on the
frozen 2.0.0 artifact whose SHA-256 is recorded above) is the one referenced
here. The very first run found the four `recompensas/*.yml` files were missing
`config-version`; later runs found two more real defects, all fixed and
re-verified: the shipped weekly reward used a particle name that does not exist
on 1.8, and the config auto-merge inserted nested options outside their section
(detected by upgrading a real 1.x installation, see section 3c).

## 3c. Upgrade check from a real 1.x installation

A Paper 1.8.8 server was started with the plugin data folder of a real 1.x
server (its `config.yml`, `messages.yml`, `menus.yml`, `recompensas/` and
player data) and the 2.0.0 jar. Result, straight from the console:

```
messages.yml was renamed to messages_es.yml (same content, new format).
config.yml: added 'idioma: es' because you are upgrading from 1.x (so the chat stays in Spanish).
Files updated with the new options (a .bak copy was saved): config.yml (+20),
messages_es.yml (+65), menus.yml (+46), recompensas/diarias.yml (+16),
recompensas/semanales.yml (+13), recompensas/mensuales.yml (+13).
Config: 1 warning(s) - run '/rewards validate' for details
```

What that proves: user values survive, every new option lands **inside its own
section** with its comments and a `# --- NEW in this version` marker, a `.bak`
copy is written, player data is untouched, the chat stays in Spanish for a
Spanish server, the old `/recompensas` alias and the old menu action names
(`abrir:`, `reclamar:diaria`…) still work, and the only remaining warning is the
harmless list of message keys that 2.0.0 no longer uses.

## 3b. SQL statement check

`java -cp tools/sql-check/h2.jar tools/sql-check/SqlCheck.java` runs the exact
statements of `storage/AlmacenSql.java` against an in-memory database in MySQL
compatibility mode (H2 2.2.224) and prints one `CHECK OK` per assertion:

| Result | Detail |
| --- | --- |
| Checks | **16 of 16 pass** (8 for `MEDIUMTEXT`, 8 for `TEXT`) |
| Covered | `CREATE TABLE IF NOT EXISTS` is valid and repeatable, three rows inserted with the UPDATE-then-INSERT upsert, a second save updates instead of duplicating, the stored payload reads back identical, a 70,000-character payload is accepted, `DELETE` and `SELECT uuid` behave as expected, and `VARCHAR(16)` rejects longer player names (which is why the class truncates them) |

This does **not** replace a run against a real MySQL/MariaDB server, but it
verifies that the schema and every statement the plugin will send are valid SQL
in MySQL syntax, including the `MEDIUMTEXT` column used to avoid the 64 KB
`TEXT` limit when a player has many pending reward items.

## 4. Not covered by the automated run

Being explicit about this is part of the release: these points were **not**
exercised automatically and belong to the manual matrix of
[QA_CHECKLIST.md](QA_CHECKLIST.md).

- **Clicking the menus with a real client.** The harness drives commands and
  internal classes; opening a menu, the per-state items and the claim-by-click
  flow need a real client (or a bot). The menu-building code paths are the same
  ones used by the commands, and the menus were reviewed by hand.
- **MySQL against a live database.** The statements were verified in MySQL
  compatibility mode (section 3b) and the YAML backend, the shared data codec and
  the migration logic were exercised in a real server, but the SQL backend was
  not run against a real MySQL/MariaDB instance in this environment. Test it on a
  staging database before switching a network over.
- **Intermediate Minecraft versions.** Only the two endpoints were tested
  (1.8.8 and 1.21.1). Everything version-specific (materials, sounds, particles,
  titles, action bar) is resolved through `util/Compat` with graceful
  degradation, and `/rewards validate` reports what does not exist in the
  running version.
- **PlaceholderAPI and the update checker.** PlaceholderAPI was not installed in
  the test servers, so the expansion registration was not exercised; the version
  check was left disabled (it is off by default).

## 5. Manual checks performed

- Started the plugin on a clean 1.8.8 server and on a clean 1.21.1 server: no
  exceptions, the 8 files are created and the console summary is correct.
- Ran the admin commands (validar, test, datos, dar, reload, top) on both
  servers through the harness and read their output.
- Reviewed the generated files: comments, config-version lines, UTF-8 content
  and the bilingual headers.
