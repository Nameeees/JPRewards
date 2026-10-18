# Security policy

## Supported versions

| Version | Supported |
| --- | --- |
| 2.0.x | Yes |
| 1.x | No (please update: the 2.0 files are read automatically and a `.bak` copy is written) |

## Reporting a problem

Please **do not open a public issue** for anything that can be exploited on a live server. Report it
privately through GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
("Security" tab → "Report a vulnerability"), or by private message to the author on SpigotMC.

Include, as far as you can:

- What the problem is and what an attacker gains (duplicate items, run commands as console, read
  another player's data, crash the server...).
- The version of JPRewards, the server software and the Java version.
- The steps to reproduce it, and any configuration involved (`config.yml`, `menus.yml`,
  `recompensas/*.yml`).
- A proof of concept if you have one.

You will get an answer as soon as possible, and the fix is published as a patch release with a note
in `CHANGELOG.md`. Credit is given in the release notes unless you prefer to stay anonymous.

## What is in scope

- Reward duplication or lost rewards.
- Escalation of permissions: a player reaching a command or a value they should not.
- Injection through configuration (a value in a YAML file that ends up running something).
- Storage problems: reading or overwriting another player's data.
- Anything that lets a player crash or freeze the server.

## What is not in scope

- Someone with file access to the server editing the configuration (that is already game over).
- Plugins that grant `jprewards.*` to players: that is a permission setup decision.
- Problems caused by running an unsupported combination (a modified server jar, an old 1.x config
  that was never validated).

## How the plugin protects data

- Player data is written atomically, and a file that cannot be read is moved aside as `.bak`
  instead of being overwritten.
- Pending rewards are stored with the player profile, so a full inventory never loses a reward.
- The plugin never trusts a client: every claim is decided on the server from the stored data.
- There is no permission that skips the claim checks: claiming twice in a row delivers the reward
  once.
