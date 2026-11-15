# 9. Bosses

[← Index](index.md) · [ภาษาไทย](../th/09-bosses.md)

A boss is a mob with more health, a boss bar, skills on spawn / on a timer / when hit / at
health phases / on death, and its own loot. Bosses survive chunk unloads and restarts.

## Files: one boss, two files

```
bosses/frost_lord.yml          the boss and its loot
skills/bosses/frost_lord.yml   its moves (FrostLord_*)
```

Sub-folders and `.yaml` work. `bosses.yml` is the field reference.

```yaml
# bosses/frost_lord.yml
FrostLord:
  type: STRAY
  name: "<gradient:#7dd3fc:#3b82f6><bold>Frost Lord</bold></gradient>"
  health: 400
  damage: 9
  bossbar: { color: BLUE, style: NOTCHED_10, radius: 48 }
  skills:
    on-spawn: [FrostLord_Intro]
    timer:
    - { every: 160, skill: FrostLord_Nova }      # ticks: every 8 s
    - { every: 100, skill: FrostLord_Lance }
    on-damaged:
    - { skill: FrostLord_Blink, chance: 0.08 }
    phases:
    - { below: 60%, skill: FrostLord_Minions }   # once
    - { below: 30%, skill: FrostLord_Enrage }
    on-death: [FrostLord_Shatter]
  loot:                                          # same fields as drops.yml
    xp: 500
    drops:
    - { gem: FrostGem, chance: 1.0 }
    - { shardvoid: 2, chance: 0.5 }
```

| key | meaning |
|---|---|
| `type`, `name`, `health`, `damage`, `glowing` | the mob |
| `bossbar` | `color`, `style`, `radius` (shown to players within it) |
| `skills.on-spawn` / `on-death` | lists of skills |
| `skills.timer` | `{ every: <ticks>, skill }` |
| `skills.on-damaged` | `{ skill, chance }` |
| `skills.phases` | `{ below: 50%, skill }`, each fires once |
| `loot` | `xp`, `looting-bonus`, `drops` |

## Writing boss moves

Boss skills run **as the boss**: `@self` is the boss, `@target` what it fights, `@trigger`
the attacker (`on-damaged`, phases) or the killer (`on-death`). `nearby` hits the boss's
enemies (players and their pets), never its own minions.

**Telegraph big hits** so players can dodge:

```yaml
FrostLord_Nova:
  skills:
  - telegraph: { skill: FrostLord_NovaBlast, radius: 6, duration: 30, color: "#4da6ff" }
    target: casterLocation

FrostLord_NovaBlast:
  skills:
  - damage: 6
    target: { nearby: 6 }          # measured from the circle
```

Ideas: a `display` text shout on enrage, `summon` minions at a phase, a `ring` attack that
punishes standing far away, a `meteor` with a telegraph of the same duration.

## Commands

`/cr boss spawn <id> [player]`, `/cr boss list`, `/cr boss killall` (loaded chunks only).
