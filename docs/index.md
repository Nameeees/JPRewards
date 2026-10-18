---
title: Documentation
---

<div class="hero">
  <img src="{{ '/assets/banner.png' | relative_url }}" alt="JPRewards">
  <div class="texto">
    <h1>Documentation</h1>
    <p>Daily, weekly, monthly and streak rewards with fully configurable head menus for Minecraft 1.8 to the latest version. One jar, no dependencies, English by default with Spanish presets.</p>
    <div class="acciones">
      <a href="https://github.com/Nameeees/JPRewards/releases">Download the jar</a>
      <a class="secundario" href="{{ '/COMMANDS.html' | relative_url }}">Commands</a>
      <a class="secundario" href="{{ '/CONFIGURACION.es.html' | relative_url }}">Guía en español</a>
    </div>
    <ul class="pastillas">
      <li>1.8 → latest</li>
      <li>No dependencies</li>
      <li>YAML or MySQL</li>
      <li>PlaceholderAPI</li>
      <li>English + Spanish</li>
      <li>29 automated checks</li>
    </ul>
  </div>
</div>

## Start here

<div class="rejilla">
  <div class="tarjeta">
    <h3><a href="{{ '/COMMANDS.html' | relative_url }}">Commands and permissions</a></h3>
    <p>Every subcommand, alias, argument, permission and example. The root command is <code>/rewards</code>.</p>
  </div>
  <div class="tarjeta">
    <h3><a href="{{ '/PLACEHOLDERS.html' | relative_url }}">Placeholders</a></h3>
    <p>Menu, message and reward placeholders, plus the <code>%jprewards_...%</code> expansion for other plugins.</p>
  </div>
  <div class="tarjeta">
    <h3><a href="{{ '/FAQ.html' | relative_url }}">FAQ</a></h3>
    <p>Languages, MySQL, upgrading from 1.x, duplicate claims, full inventories and custom heads.</p>
  </div>
  <div class="tarjeta">
    <h3><a href="{{ '/CONFIGURACION.es.html' | relative_url }}">Guía de configuración</a></h3>
    <p>Paso a paso de <code>config.yml</code>, <code>menus.yml</code> y <code>recompensas/*.yml</code> (en español).</p>
  </div>
  <div class="tarjeta">
    <h3><a href="{{ '/VERIFICATION.html' | relative_url }}">Verification</a></h3>
    <p>What was tested, on which servers, with the hash of the released artifact.</p>
  </div>
  <div class="tarjeta">
    <h3><a href="https://github.com/Nameeees/JPRewards">Source code</a></h3>
    <p>Java sources, the integration harness, the artwork generators and the issue templates.</p>
  </div>
</div>

## Install in one minute

```bash
# 1. Drop the jar in plugins/ and start the server
# 2. In game, check that everything is fine:
/rewards validate      # names the exact file and key that is wrong
/rewards               # opens the main menu
/rewards help          # the commands your permissions allow
```

## What it looks like

| | |
| --- | --- |
| ![What you get]({{ '/assets/features.png' | relative_url }}) | ![Commands]({{ '/assets/commands.png' | relative_url }}) |

Every image on this site is generated from code by the scripts in
[`docs/assets/`](https://github.com/Nameeees/JPRewards/tree/main/docs/assets), so the artwork and
the documentation never drift apart.

## All the pages

| Page | Language | What it covers |
| --- | --- | --- |
| [Commands and permissions]({{ '/COMMANDS.html' | relative_url }}) | English | Subcommands, aliases, arguments, permissions, examples. |
| [Placeholders]({{ '/PLACEHOLDERS.html' | relative_url }}) | English | Texts, menus, rewards and the PlaceholderAPI expansion. |
| [FAQ]({{ '/FAQ.html' | relative_url }}) | English | The questions that come up most. |
| [Configuration guide]({{ '/CONFIGURACION.es.html' | relative_url }}) | Spanish | Step-by-step guide to the YAML files. |
| [Verification]({{ '/VERIFICATION.html' | relative_url }}) | English | Tests, servers and artifact hash. |
| [QA checklist]({{ '/QA_CHECKLIST.html' | relative_url }}) | English | The list used before every release. |
| [SpigotMC pack]({{ '/SPIGOTMC_PAGE.html' | relative_url }}) | English | Resource description and image order. |
| [README]({{ '/README.html' | relative_url }}) | English | The full feature list and configuration reference. |
| [README (español)](https://github.com/Nameeees/JPRewards/blob/main/README.es.md) | Spanish | La misma información en español. |
