# API for other plugins

JPRewards exposes a small API so another plugin can **read**, **grant** and **react to** rewards
without touching the configuration from code.

```java
JPRewardsAPI api = JPRewardsAPI.get();
if (api != null) {                        // null when JPRewards is not enabled
    api.claim(player, "daily");           // same as /rewards claim daily
    api.give(player, "daily", "3");       // same as /rewards give
    api.giveExtra(player, "boss-kill");   // a named reward from recompensas/extra.yml
}
```

The API lives in `me.jp.jprewards.api` and is optional at runtime: check `JPRewardsAPI.get()`
before using it, or declare JPRewards as a `softdepend` in your `plugin.yml`.

## Methods

| Method | What it does |
| --- | --- |
| `JPRewardsAPI.get()` | The API, or `null` if the plugin is not enabled. |
| `enabled()` | `true` if JPRewards is loaded and enabled. |
| `claim(player, type)` | Claims like the command. Types: `daily`, `weekly`, `monthly`, `streaks`, `all`. |
| `give(player, type, key)` | Delivers a specific reward of a cycle (no counters, no waits). |
| `giveExtra(player, id)` | Delivers a named reward from `recompensas/extra.yml`. |
| `giveExtra(playerName, id)` | The same, looking the player up by name. |
| `hasPending(player)` / `pending(player)` | Whether the player has something to claim, and how many. |
| `hasClaimedToday(player)` | `true` if the daily reward was already claimed. |
| `data(uuid)` | The player data object (streaks, totals, month, pending items). |
| `rewardKeys(type)` | The keys defined in a cycle: days `1..N`, weeks, milestone ids, bonus ids. |
| `extraKeys()` | The ids defined in `recompensas/extra.yml`. |

## Events

Both events are fired on the server thread, from the single delivery point, so they cover claims,
`/rewards give`, `/rewards trigger` and API calls.

### `RewardClaimEvent` (cancelable)

Fired **before** anything is delivered.

```java
@EventHandler(priority = EventPriority.HIGH)
public void alEntregar(RewardClaimEvent evento) {
    Player jugador = evento.getPlayer();
    // getId() = 'dia-3', 'hito-hito-15', 'racha-racha-7' or the extra id
    // getTipo() = daily | weekly | monthly | streaks | extra
    if (evento.getTipo().equals("daily") && jugador.isFlying()) {
        evento.setCancelled(true);   // nothing is delivered
    }
}
```

| Getter | Value |
| --- | --- |
| `getPlayer()` | The player who is going to receive the reward. |
| `getId()` | Internal id of the reward (`dia-3`, `hito-hito-15`, `racha-racha-7`, extra id). |
| `getTipo()` | `daily`, `weekly`, `monthly`, `streaks` or `extra`. |
| `getClave()` | The day, the week, the milestone id or the extra id. |
| `getNombre()` | Visible name of the reward. |

### `RewardDeliveredEvent`

Fired **after** the delivery, with what was delivered.

```java
@EventHandler
public void alEntregar(RewardDeliveredEvent evento) {
    getLogger().info(evento.getPlayer().getName() + " recibio " + evento.getNombre()
            + " (" + evento.getComandos() + " comandos, " + evento.getItems() + " items)");
}
```

## Extra rewards: rewards from your own events

`recompensas/extra.yml` defines **named rewards that do not belong to any cycle**. They are the
bridge for bosses, votes, quests, crates or anything your plugin knows about.

```yaml
# recompensas/extra.yml
definir:
  vip: 'crates give {jugador} vip 1'

premios:
  boss-kill: 'eco give {jugador} 5000'
  vip-gift: {usar: vip, extra: 'give {jugador} diamond 5'}
  solo-finde:
    dar: ['crate key event {jugador}']
    probabilidad: 0.25        # 25 % de las veces
    una-vez: true             # o solo la primera vez por jugador
    mundo: world_event        # o solo en ese mundo
```

From your plugin:

```java
JPRewardsAPI api = JPRewardsAPI.get();
if (api != null) {
    api.giveExtra(player, "boss-kill");
}
```

From the console (command blocks, other plugins, scripts):

```
/rewards trigger <player> boss-kill
```

The entry uses the same format as any reward: short form, packs and the full form all work, and the
conditions (`probabilidad`, `mundo`, `una-vez`) apply exactly as in the cycles.

## Example plugin skeleton

```java
public final class MiPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        if (JPRewardsAPI.get() == null) {
            getLogger().info("JPRewards is not installed: runner rewards are disabled.");
            return;
        }
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void alMorir(EntityDeathEvent evento) {
                if (!(evento.getEntity() instanceof Player)) {
                    return;
                }
                Player jugador = (Player) evento.getEntity();
                Player asesino = jugador.getKiller();
                if (asesino != null) {
                    JPRewardsAPI.get().giveExtra(asesino, "boss-kill");
                }
            }
        }, this);
    }
}
```

> The API never throws into your code: an invalid type or an unknown id simply returns `false`.
