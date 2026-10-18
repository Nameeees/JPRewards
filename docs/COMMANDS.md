# Commands and permissions

Everything is under a single command, `/rewards`, with English subcommands and English aliases.
Nothing else is registered, so there are no clashes with other plugins.

| Setting | Value |
| --- | --- |
| Root command | `/rewards` |
| Aliases | `jprewards`, `rec`, `reward` (and `recompensas`, kept only for servers upgrading from 1.x) |
| Tab completion | Every subcommand, argument and online player name |

## Player commands

| Command | Aliases | What it does |
| --- | --- | --- |
| `/rewards` | `/rewards menu` | Opens the main menu. |
| `/rewards help` | `?`, `h` | Shows the command list that fits your permissions. |
| `/rewards daily` | `day` | Opens the daily cycle menu. |
| `/rewards weekly` | `week` | Opens the weekly cycle menu. |
| `/rewards monthly` | `month` | Opens the monthly milestones menu. |
| `/rewards streaks` | `streak`, `bonus`, `bonuses` | Opens the streak bonus menu. |
| `/rewards claim <type>` | | Claims without opening a menu. Types: `daily`, `weekly`, `monthly`, `streaks`, `all`. |
| `/rewards top [type]` | `leaderboard`, `ranking` | Server leaderboard. Types: `streak` (default), `total`, `weeks`, `milestones`, `bonuses`. |
| `/rewards info` | | How the plugin works, in game. |

Examples:

```
/rewards                     opens the main menu
/rewards claim daily         claims today's reward
/rewards claim all           claims everything that is ready
/rewards top weeks           leaderboard of weekly totals
```

## Admin commands

All of them need `jprewards.admin` unless the table says otherwise.

| Command | Aliases | Permission | What it does |
| --- | --- | --- | --- |
| `/rewards status` | | `jprewards.admin` | Version, language, storage, loaded cycles and config warnings. |
| `/rewards validate` | `check` | `jprewards.admin` | Checks every file and names the exact file and key that is wrong. |
| `/rewards reload [target]` | | `jprewards.admin` | Hot reload. Targets: `all` (default), `config`, `messages`, `menus`, `rewards`. |
| `/rewards data <player>` | | `jprewards.admin` | Full report of one player: streak, totals, month, milestones, pending rewards. |
| `/rewards give <player> <type> [key]` | | `jprewards.admin.manage` | Delivers a reward on purpose (it is logged). |
| `/rewards test <type> [key]` | `simulate` | `jprewards.admin.manage` | Dry run: prints what would be delivered, without delivering it. |
| `/rewards reset <player\|*> [type]` | | `jprewards.admin.manage` | Resets data. Types: `daily`, `weekly`, `monthly`, `streaks`, `all` (default). |
| `/rewards set <player> <field> <value>` | | `jprewards.admin.manage` | Sets a counter. Fields: `streak`, `best-streak`, `total`, `weekly-streak`, `weekly-total`, `month-days`, `milestones`. |
| `/rewards migrate [confirm]` | | `jprewards.admin.migrate` | Copies all the data between YAML and the database. |

Examples:

```
/rewards validate
/rewards reload menus
/rewards data Notch
/rewards test daily 7            what day 7 would deliver, without delivering it
/rewards give Notch monthly hito-15
/rewards reset * daily           resets the daily data of every player
/rewards set Notch streak 6
/rewards migrate confirm
```

## Permissions

| Permission | Default | What it grants |
| --- | --- | --- |
| `jprewards.use` | everyone | Open the menus and use `/rewards`. |
| `jprewards.claim.daily` | everyone | Claim the daily reward. |
| `jprewards.claim.weekly` | everyone | Claim the weekly reward. |
| `jprewards.claim.monthly` | everyone | Claim the monthly milestones. |
| `jprewards.claim.streak` | everyone | Claim the streak bonuses. |
| `jprewards.notify` | everyone | Receive the join notice when a reward is ready. |
| `jprewards.top` | everyone | Use `/rewards top`. |
| `jprewards.autoclaim` | nobody | Claim the daily reward automatically on join (only if auto-claim is enabled). |
| `jprewards.update` | operators | Chat notice when a new version is available. |
| `jprewards.admin` | operators | `status`, `validate`, `reload`, `data` and `top`. |
| `jprewards.admin.manage` | operators | `give`, `test`, `reset` and `set` for any player. |
| `jprewards.admin.migrate` | operators | `migrate`. |
| `jprewards.*` | operators | Every permission above. |

> There is **no** permission that skips the claim checks. Claiming the same reward twice delivers it
> once; use `/rewards give` when you really want to hand out a reward.

### Rank variants

Rewards can differ per rank through **permission-based variants**: an entry in
`recompensas/*.yml` can define `rangos:` with its own items, commands, lore and head per
permission, and the plugin picks the best one the player has. See
[CONFIGURACION.es.md](CONFIGURACION.es.md) (Spanish) for the full syntax.
