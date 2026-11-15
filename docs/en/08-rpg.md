# 8. RPG: mana, levels, classes, drops

[← Index](index.md) · [ภาษาไทย](../th/08-rpg.md)

## Mana

Skills cost `mana:`. Mana regenerates every second and shows on the action bar after casts.

```yaml
# config.yml
mana:
  enabled: true
  max: 100.0                # without a class
  regen-per-second: 5.0
  show-actionbar: true
```

Classes replace the max and regen with their own values.

## Levels and XP

Players earn XP from skill casts and kills ([drops](#drops)). XP to the next level is
`base × level^exponent`. A level-up shows one action-bar line and plays a sound, with no
titles.

```yaml
leveling:
  enabled: true
  max-level: 50
  xp-curve: { base: 100, exponent: 1.5 }   # L1→2: 100, L10→11: 3162
  xp: { skill-cast: 1 }
  level-up: { restore-mana: true }
classes:
  allow-change: false        # true = /class choose can be used again
```

Level, XP, class and mana are saved on the player, so there is no database to set up.

## Classes (`classes.yml`)

```yaml
Warrior:
  display: "<gradient:#ef4444:#f97316><bold>Warrior</bold></gradient>"
  description: "<gray>Less mana, but one extra socket on every weapon."
  icon: IRON_SWORD
  mana: { base: 80, per-level: 2 }
  mana-regen: { base: 4, per-level: 0.1 }
  socket-bonus: 1            # extra socket slots above gems.max-slots
  starting-gems: [SmiteGem]  # given once when chosen
  # permission: celestirift.class.warrior
  passives:
  - { trigger: crit, skill: Cleave }
  - { trigger: kill, skill: Bloodlust }
  - { trigger: low_health, skill: LastStand, min-level: 10 }
```

**Passives** cast by themselves on a trigger ([triggers](06-gems-sockets.md#triggers)).
Options: `chance`, `every` (seconds, for `timer`), `min-level`. They still use the skill's
cooldown and mana.

Players: `/class`, `/class info <id>`, `/class choose <id>`, `/profile`.
Skills can check `{ class: Mage }` and `{ level: 10 }`, and show `<caster.class>` / `<caster.level>`.

## Class kits (`skills/classes/`)

| class | Lv 1 | Lv 5 | Lv 10 | Lv 20 | passives |
|---|---|---|---|---|---|
| **Mage** | `Frostbolt`: skill-shot, stacks Chill | `Shatter`: ice under chilled enemies, 6 + 5/stack | `Stormcall`: 0.75 s cast, chain lightning | `Cataclysm`: 2 s channel, warned meteor | low health → `ManaBarrier` |
| **Warrior** | `Charge`: leap + cone slam | `Whirlwind`: three spins | `Rend`: cleave + stacking Bleed | `Warcry`: party +25% damage and shield, enemies Sundered | crit → `Cleave`, kill → `Bloodlust`, low health → `LastStand` |
| **Cleric** | `HolyBolt`: damage + self-heal | `Renew`: heal over time on the 2 most hurt allies | `Sanctuary`: 1 s cast, heal/cleanse/shield circle | `Judgement`: + 15% of missing health | every 5 s → `Blessing` |

Kit skills are gated with `conditions: [ { class: Mage }, { level: 10 } ]`. Hand them out
with `/cr give <skill>` or as gems. Damage and heals scale with level.

## Drops

`drops.yml` (or `drops/*.yml`) gives gems, Shardvoid and XP when mobs die. Every matching
table applies.

```yaml
Blazes:
  mobs: [BLAZE]
  xp: 12
  looting-bonus: 0.01        # +chance per Looting level
  drops:
  - { gem: EmberGem, chance: 0.04 }
  - { shardvoid: 1, chance: 0.01 }
```

Match on `mobs`, `name` (custom name contains), `tag` (scoreboard tag), `boss`, `worlds`.
`player-kill-only` (default true). Bosses can also carry `loot:` in their own file.
With OrionRP, drops can also be custom items: `- { item: demo:ruby, chance: 0.1 }` ([OrionRP](14-orionrp.md)).
