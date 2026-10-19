# JPRewards documentation

Guides, references and the verification log of the plugin. The main page is
[README.md](../README.md); this folder goes deeper.

| Page | Language | What it covers |
| --- | --- | --- |
| [Commands and permissions](COMMANDS.md) | English | Every subcommand, alias, argument, permission and example. |
| [Placeholders](PLACEHOLDERS.md) | English | Placeholders for menus, messages and rewards, plus the PlaceholderAPI expansion. |
| [API for plugins](API.md) | English | Java API, events and named rewards for bosses, votes or quests. |
| [FAQ](FAQ.md) | English | The questions that come up most: languages, MySQL, 1.x upgrades, duplicates, full inventories. |
| [Configuration guide](CONFIGURACION.es.md) | Spanish | Step-by-step guide to `config.yml`, `menus.yml` and `recompensas/*.yml`. |
| [Verification log](VERIFICATION.md) | English | What was tested, on which servers, and the hash of the release artifact. |
| [QA checklist](QA_CHECKLIST.md) | English | The list used before publishing a release. |
| [SpigotMC pack](SPIGOTMC_PAGE.md) | English | The resource description, the image order and the update notes. |
| [Artwork pipeline](assets/README.md) | English | How every image is generated from code. |
| [Page template](TEMPLATE.md) | English | How to write a new page in this folder. |

## For developers

| Page | What it covers |
| --- | --- |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Build, checks, ground rules and what a good pull request looks like. |
| [SECURITY.md](../SECURITY.md) | How to report a vulnerability and what the plugin does to protect data. |
| [testing/README.md](../testing/README.md) | The integration harness and the test servers it drives. |
| [CHANGELOG.md](../CHANGELOG.md) | What changed in every version (Spanish). |

## The short version

```bash
# Install: drop the jar in plugins/ and start the server, then in game:
/rewards validate      # checks every file and names what is wrong
/rewards               # opens the menu
/rewards help          # commands that fit your permissions
```
