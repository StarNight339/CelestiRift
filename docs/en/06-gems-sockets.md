# 6. Gems and sockets

[← Index](index.md) · [ภาษาไทย](../th/06-gems-sockets.md)

Players get skills as **gems** and **socket** them into weapons. A held weapon casts its
gems' skills on their triggers. **Runes** (passive gems) boost the weapon's skills, or sit in
armour.

## Gem files

```
gems.yml            field reference (gems can be written here too)
gems/fire.yml       Ember Gem, Starfall Gem
gems/frost.yml      Frost Gem
gems/holy.yml       Mending Gem, Gem of Judgement
gems/shadow.yml     Dark Gem
gems/storm.yml      Storm Gem
gems/steel.yml      Blade Gem
gems/runes.yml      Rune of Power, Farsight, Warding, Ironskin
gems/fusion.yml     Blackfire, Blizzard, Radiant (fusion results)
```

Each gem's skill lives in `skills/gems/<skill>.yml`.

```yaml
EmberGem:
  skill: Fireball
  tier: 3                  # 1 Common · 2 Uncommon · 3 Rare · 4 Epic · 5 Legendary
  trigger: RIGHT_CLICK     # default; players can change it
  material: BLAZE_POWDER   # look without the resource pack
  name: "<gradient:#ff3d00:#ffb300><bold>Ember Gem</bold></gradient>"
  lore:
  - "Forged in dragonfire, still warm to the touch."
```

| key | meaning |
|---|---|
| `skill` | the skill it casts (active gems) |
| `type` | `ACTIVE` (default) or `PASSIVE` (a rune) |
| `tier` | 1-5: colours, stars, and the legendary tooltip frame at 5 |
| `trigger` | `RIGHT_CLICK`, `LEFT_CLICK`, `SWAP_HANDS`, `DROP_ITEM`, `SNEAK`, `UNSNEAK`, `ATTACK`, `CRIT`, `PROJECTILE_HIT`, `KILL`, `DAMAGED`, `LOW_HEALTH`, `TIMER`, `JOIN` |
| `name`, `lore` | MiniMessage name; flavour lines (shown in grey italics) |
| `material` | vanilla item used without the resource pack |
| `model` | an item model used always. Prefer `resource-pack.gem-models` ([Resource pack](10-resource-pack.md)) |

### The tooltip

Built automatically from the gem and its skill, and refreshed on gems players already own:

```
✦ Fireball
Hurl a blazing orb that explodes on
impact, burning everything nearby.

▸ Trigger Right-Click
▸ Mana 20 · Cooldown 3s
▸ Rare ★★★☆☆
Forged in dragonfire, still warm to the touch.

Socket into a weapon · /cr socket
```

## Runes (passive gems)

```yaml
PowerRune:
  type: PASSIVE
  tier: 4
  power: 1.5                 # +50% damage/heal of the weapon's skills
FarsightRune:
  type: PASSIVE
  range: 1.4                 # +40% range/radius
  cooldown: 0.85             # -15% cooldown
WardingRune:
  type: PASSIVE
  buff: { type: RESISTANCE, duration: 100, level: 1 }   # each time a skill fires
```

**In armour** (helmet, chestplate/elytra, leggings, boots) a rune works while worn: its
`buff` stays on permanently and its `power`/`range`/`cooldown` apply to **every** skill you
cast. Armour accepts runes only.

## The socket workbench (`/cr socket`)

```
 [Skill Gems]  [Runes]  [Fusion]                    [help]
 [weapon] →  socket 1 … socket 7                    ← what is in the weapon now
 ▼ your gems of the chosen tab
 (two rows of gems from your inventory; pages if you own many)
 [◀]  [Shardvoid +slot]  [status]  [close]  [▶]
```

1. Hold the weapon and run `/cr socket`. The weapon goes into the menu. To switch, click
   another item in your inventory.
2. Pick a tab: **Skill Gems** shows only active gems you own, **Runes** only runes, grouped
   with counts.
3. Click a gem to socket it (or click a gem in your own inventory).
4. The **sockets row** shows every socket with the gem's tooltip and trigger. Empty sockets
   are light grey. Locked ones can be opened with a Shardvoid.
5. **Right-click** a socket to take its gem back (`gems.allow-unsocket: false` turns this off).
6. The weapon returns to your hand when you close the menu.

Messages appear on the action bar ("No free socket", "Armour only accepts runes"...).

## Slots and Shardvoid

A weapon starts with `gems.default-slots` (1) and can reach `gems.max-slots` (6), plus the
class `socket-bonus`. Each **Shardvoid** used from the workbench button adds one slot.
Get them with `/cr shardvoid [amount] [player]` or from drops.

## Changing triggers

`/cr trigger` while holding a gem or a socketed weapon: click a skill in the top row, then a
trigger below. Command form: `/cr item settrigger <skill> <trigger>`. `/cr item` lists a
weapon's sockets and `/cr item clear` empties them.

## Triggers

| trigger | fires when | `@trigger` |
|---|---|---|
| `RIGHT_CLICK`, `LEFT_CLICK` | clicking (air or block) | — |
| `SWAP_HANDS` (F), `DROP_ITEM` (Q) | pressing the key (the event is cancelled) | — |
| `SNEAK`, `UNSNEAK` | sneaking starts/stops | — |
| `ATTACK` | you hit something | the victim |
| `CRIT` | a critical (falling) melee hit | the victim |
| `PROJECTILE_HIT` | your arrow/trident hits | the victim |
| `KILL` | you kill something | the victim |
| `DAMAGED` | something hits you | the attacker |
| `LOW_HEALTH` | your health drops below `combat.low-health` (once per drop) | the attacker |
| `TIMER` | every second (pace it with the skill's cooldown) | — |
| `JOIN` | you join | — |

A trigger can't re-fire itself from its own skill, but a different one can (an ATTACK skill
that kills fires KILL). The same triggers drive class passives and the legacy `triggers:`
bindings in `config.yml`.
