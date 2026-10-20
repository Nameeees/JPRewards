# Artwork pipeline

Every image used on the SpigotMC page and in the README is generated from code — no external
editor, no stock art — so the whole set can be rebuilt or rebranded at any time.

| Script | Output |
| --- | --- |
| `generate_banner_icon.py` | `banner.png` (1280×420), `icon.png` (512×512) |
| `generate_infographics.py` | `features`, `commands`, `permissions`, `setup`, `configuration`, `diagnostics`, `integrations`, `compatibility` (1280×720) |
| `generate_cta.py` | `documentation.png`, `discord.png` (1280×260) |
| `componer-capturas.py` | `real-*.png` (1280×720) from the **real in-game captures** in `capturas/` |
| [`tools/capturar-juego.ps1`](../../tools/capturar-juego.ps1) | drives a real Minecraft client and captures each menu (needs a client and a desktop) |

Each script starts with a `BRAND` block holding the plugin name, the tagline and the palette; for a
new plugin, edit that block first.

**Every image keeps the purple palette but changes tone and, above all, composition.** The
`TEMAS` dictionary of `generate_infographics.py` gives each infographic a shade of the brand purple,
a background motif and a layout; the card fill, border and icon badge are derived from that shade,
so the seven images look like one family without looking like the same poster seven times:

| Image | Tone (all purple) | Composition | Motif |
| --- | --- | --- | --- |
| `features` | fuchsia `#D946EF` | 3×2 card grid | sparkles |
| `commands` | violet `#A855F7` | console window with a title bar | prompt lines |
| `permissions` | deep violet `#8B5CF6` | permission table with audience chips | keys and shields |
| `setup` | lavender `#C084FC` | vertical numbered timeline | check marks |
| `configuration` | magenta `#C026D3` | file tree beside a YAML panel | YAML indent guides |
| `diagnostics` | orchid `#E879F9` | validation report (OK / ! / x) | radar rings |
| `compatibility` | indigo violet `#7C3AED` | version timeline over three cards | orbits |
| `integrations` | deep rose `#BE185D` | code panel beside a bullet list | sparkles |

Adding a composition is one function in `DISENOS`, adding a motif is one function in `PATRONES`, and
changing a colour is one value. The gold of the reward star stays as the secondary accent.

**The background is shared by all eleven images** (the three scripts implement the same recipe, so
the set looks like one family): a three-stop gradient, the diagonal texture, an **aurora** of four
heavily blurred light masses in different shades of the palette, faint **light rays** entering from
the top, a **bokeh** field of soft dots at different sizes, concentric **arcs** in two corners and a
vignette that darkens the edges so the content breathes. A 1 px light line at the top separates the
image from the forum background. Everything is drawn from code and seeded, so the output stays
byte-identical between runs.

Requirements: Python 3 with Pillow (`pip install pillow`) and the Segoe UI fonts shipped with
Windows — change the `BOLD`, `SEMI` and `REGULAR` paths at the top of each script for another OS.

The palette comes from the plugin's own menus (`&d` / `&5`): fuchsia **#D946EF** on a dark
purple-navy background, with gold **#FACC15** for the reward star. Output is deterministic: the same
text produces byte-identical PNGs, so a diff in git means the artwork really changed.

The `real-*.png` banners are built from **genuine captures** taken from a real Minecraft 1.8.9 client
connected to a test server (`capturas/` keeps the raw PNGs), not from mockups. The rest of the artwork is
generated from code, so it can be rebuilt or rebranded at any time.

The generated files are committed so the SpigotMC page and the README work straight from the
repository; `docs/SPIGOTMC_PAGE.md` lists which file goes in which part of the resource page.
