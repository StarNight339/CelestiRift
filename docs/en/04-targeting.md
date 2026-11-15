# 4. Targeting

[← Index](index.md) · [ภาษาไทย](../th/04-targeting.md)

`target:` decides who or where a line acts. Without it, the line acts on the caster.

## Targeters

| targeter | gives | example |
|---|---|---|
| `self` | the caster | `target: self` |
| `target` | the entity you look at (a mob's current target) | `target: { target: 25 }` |
| `trigger` | the entity that triggered the skill (victim on attack, attacker when hit...) | `target: trigger` |
| `nearby` | living entities in a radius around the origin | `target: { nearby: 6 }` |
| `cone` | in front of you | `target: { cone: { angle: 90, range: 4 } }` |
| `line` | a straight strip from your eyes, pierces | `target: { line: { length: 12, width: 1.5 } }` |
| `ring` | between two radii (standing close is safe) | `target: { ring: { inner: 3, outer: 7 } }` |
| `casterLocation` | where the caster stands | `target: casterLocation` |
| `targetLocation` | the block you look at | `target: { targetLocation: { range: 30 } }` |
| `origin` | the skill's origin: the impact point in `onhit`/`onland`/`onend` helpers | `target: origin` |

Shapes count hitboxes, so big mobs are easy to hit. Radii scale with rune `range`.

## Who gets hit

`nearby`, `target`, `cone`, `line` and `ring` accept:

```yaml
target: { nearby: { radius: 8, filter: allies, sort: lowesthp, limit: 2 } }
```

| `filter` | meaning |
|---|---|
| `enemies` | **default for areas.** For a player: hostile mobs, mobs fighting them and, when PvP is on, other players and their pets. For a mob/boss: players, their pets and anything fighting it |
| `allies` | your pets and summons, and other players when PvP is off |
| `players` / `mobs` | by type |
| `all` | everything not protected (default for `target`) |

`sort`: `nearest`, `furthest`, `random`, `lowesthp`. `limit`: the maximum number of targets.

## Combat safety

- **Never targeted:** armour stands, NPCs (Citizens), creative/spectator players,
  invulnerable entities, decorative mannequins.
- **Safety net:** harmful mechanics (`damage`, `ignite`, `stealitem`, `chain`, projectile
  hits, `blackhole`/`tornado` pulls, debuff auras) never touch your own team, whatever the
  target line says.
- Damage uses normal damage events, so **WorldGuard** `pvp deny` regions and claim plugins
  still block it.

```yaml
# config.yml
combat:
  pvp: true             # false = skills never hurt players or their pets
  pvp-multiplier: 1.0   # skill damage between players × this
  low-health: 0.3       # LOW_HEALTH trigger threshold
```

## Testing targets

`/cr dummy spawn enemy|ally|player [health]` spawns a training mannequin that never dies and
reports damage, healing and DPS on your action bar:

| dummy | behaves like |
|---|---|
| `enemy` | a monster: hit by area skills; heals back after 3 s idle |
| `ally` | a friend: hit by `filter: allies`; starts at 50% health |
| `player` | another player, with your skin: an enemy with PvP on, an ally with PvP off |

`/cr dummy clear` removes them. Examples: `skills/showcase/targeting.yml`.
