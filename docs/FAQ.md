# Frequently asked questions

### Can a player claim the same reward twice?

No. Every claim is decided on the server from the stored data: the first one is delivered, the second
one shows "already claimed" and delivers nothing. There is **no permission** that skips that check —
if you want to hand out a reward on purpose, use `/rewards give <player> <type> [key]`, which is
permission-gated and logged.

### How do I change the language?

In `config.yml`:

```yaml
idioma: en          # en or es
menus:
  archivo: menus.yml        # menus_es.yml for the Spanish menus
diaria:
  archivo: recompensas/diarias.yml     # recompensas/diarias_es.yml for the Spanish presets
```

English is the default and Spanish ships complete. To add a third language, copy
`messages_en.yml` to `messages_xx.yml`, translate it and set `idioma: xx`; missing keys fall back to
the bundled English file, so a partial translation never breaks anything.

### I updated the plugin and my configuration still has the old options.

The auto-merge runs only when your file has an **older `config-version`** than
the plugin: on start (or `/rewards reload`) the new options are inserted
**inside their section**, with their comments, marked `# --- NEW in this
version`, after a `.bak` copy. A file that is already up to date is never
rewritten, so options you deleted on purpose do not come back; run
`/rewards validate` to see what is missing — it names the file and the key.
Lists (`lore:`, `comandos:`, `avisos:`…) are never modified by the merge.

### How do I move from YAML files to MySQL?

1. `config.yml` → `almacenamiento.tipo: mysql` and fill in the connection data.
2. Restart the server.
3. `/rewards migrate confirm` — it copies everything, in both directions, without losing data.

### I come from 1.x. What do I have to do?

Nothing: put the 2.0.0 jar in `plugins/` and start the server. The plugin renames `messages.yml` to
`messages_es.yml`, adds `idioma: es` so a Spanish server stays in Spanish, updates the files with
the new options (keeping your values and writing `.bak` copies) and keeps reading your player data.
The old `/recompensas` command still answers, and old menu actions (`abrir:`, `reclamar:diaria`…)
are still accepted.

### Why does an item or a sound not appear?

Run `/rewards validate`. It checks materials, sounds, particles, enchantments, slots, actions and
placeholders **for the server version you are running** and tells you the exact file and key. A sound
or a particle that only exists on newer versions is reported as a warning, not an error.

### Can I preview a reward without giving it?

Yes: `/rewards test daily 7` prints the items, commands and messages of that entry and delivers
nothing. It is the fastest way to check a configuration.

### What happens if the player's inventory is full?

The reward is not lost: the items that do not fit are stored with the player profile and delivered on
the next join, and the player gets a single message saying how many were stored. If you prefer them
to drop on the floor, turn `entrega.guardar-pendientes` off.

### How do I give a different reward per rank?

Use `rangos:` inside an entry of `recompensas/*.yml`: each rank has its own items, commands, lore
and head, and the plugin picks the best permission the player has. See the Spanish guide
[CONFIGURACION.es.md](CONFIGURACION.es.md), section "Variantes por rango".

### Can I use custom heads?

Yes. `textura:` accepts the full base64 value from minecraft-heads.com, and `textura-jugador:` shows
the head of a player (the owner, the player opening the menu or a fixed name). If a texture cannot be
applied on the running version, the plugin uses a normal head and warns in the console.

### Does it work with PlaceholderAPI?

Yes, and it is optional: without PlaceholderAPI everything works, only the `%jprewards_...%`
placeholders and the placeholders of other plugins inside the texts stop resolving. See
[PLACEHOLDERS.md](PLACEHOLDERS.md).

### Does it work on Folia?

No. The plugin uses the classic Bukkit scheduler, which Folia replaced. It works on Spigot, Paper,
Purpur and their forks from 1.8 to the latest version.

### Where is the player data?

In `plugins/JPRewards/datos/`, one YAML file per player (or a single file, or the database, depending
on `almacenamiento.tipo`). The files are written atomically and a file that cannot be read is moved
aside as `.bak` instead of being overwritten.

### How do I report a bug or ask for help?

The issue templates ask for the version, the server software and the output of `/rewards validate`,
which is what makes a report actionable. Security problems go through
[SECURITY.md](../SECURITY.md), never a public issue.
