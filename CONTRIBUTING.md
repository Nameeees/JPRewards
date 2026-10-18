# Contributing to JPRewards

Thanks for wanting to help. This page explains how the project is built, what a good change looks
like and how to send it.

## Before you start

- **Questions and setup help** belong in the SpigotMC discussion thread, not in an issue.
- **Bug reports** and **feature requests** use the issue templates: they ask for the version, the
  server software and the output of `/rewards validate`, which is what makes a report actionable.
- **Security problems** are handled privately, see [SECURITY.md](SECURITY.md).

## Requirements

| Tool | Version | Why |
| --- | --- | --- |
| JDK | 8 or newer | The jar targets Java 8 bytecode so it loads on 1.8 servers. |
| Maven | 3.6+ | `mvn -B clean package` is the reference build. |
| Python | 3.8+ with PyYAML and Pillow | Resource validator and artwork generators. |

Gradle works too (`gradle build`) but Maven is the reference: the CI uses it.

## Build and check

```bash
mvn -B clean package                 # target/JPRewards-2.0.0.jar
python tools/validar-recursos.py     # 0 errors, 0 warnings
```

The integration harness needs a running test server; it is described in
[testing/README.md](testing/README.md) and is what produces the 29 checks of
[docs/VERIFICATION.md](docs/VERIFICATION.md).

## Ground rules

1. **No dependencies.** The jar must stay self-contained: no Vault, no NMS, no shaded libraries.
   PlaceholderAPI is optional and only ever `provided`.
2. **Java 8 bytecode.** Do not use APIs newer than Java 8 in the main sources, and remember the
   server API is 1.8: anything modern goes through `util/Compat.java`.
3. **Everything is configurable.** A hard-coded reward, text, slot, sound or material is a bug.
   If a value can change per server, it belongs in a YAML file with a comment.
4. **Both languages.** Every player-facing text needs a key in `messages_en.yml` **and**
   `messages_es.yml`. Missing keys fall back to the bundled English file, so a partial translation
   never breaks the plugin, but an untranslated key is still a gap.
5. **No new permissions without a reason.** They are documented in `plugin.yml`, the README and the
   SpigotMC page; adding one means updating all three.
6. **Spanish comments, English product.** The code comments and the changelog are written in
   Spanish; everything the user sees (messages, menus, commands, docs in `README.md`, artwork) is
   in English.

## What a good pull request looks like

- One topic per pull request. A bug fix and a refactor in the same branch are hard to review.
- The checklist of the pull request template is filled in; it mirrors what the CI checks.
- If the change is visible, say how to see it: the command to run, the file to open or a screenshot.
- If the change affects the artwork, re-run the three generators in `docs/assets/` and commit the
  PNGs: they are versioned on purpose, so the documentation works straight from the repository.

## Repository layout

```
src/main/java/me/jp/jprewards/   plugin sources (see the package list in the README)
src/main/resources/              config.yml, messages_en/es.yml, menus.yml, recompensas/*.yml
docs/                            guides, verification log, SpigotMC pack and the generated artwork
tools/                           resource validator and one-off maintenance scripts
testing/                         integration harness and the scripts that drive the test servers
```

## Licence

By contributing you agree that your work is released under the [MIT licence](LICENSE) of the
project.
