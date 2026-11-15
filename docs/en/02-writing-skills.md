# 2. Writing skills

[← Index](index.md) · [ภาษาไทย](../th/02-writing-skills.md)

## A skill

Every `.yml`/`.yaml` file under `skills/` loads, in any sub-folder.

```yaml
Fireball:
  description: "<gray>Hurl a blazing orb that explodes on impact."   # tooltips, /cr info
  cooldown: 3              # seconds
  mana: 20
  skills:
  - projectiletrail: { particle: FLAME, speed: 1.1, hitradius: 1.2, onhit: Fireball_Impact }
  - sound: { sound: entity.blaze.shoot, pitch: 0.9 }

Fireball_Impact:
  skills:
  - damage: "6 + <caster.level> * 0.3"
    target: { nearby: 3 }
  - ignite: 60
    target: { nearby: 3 }
  - skill: Fx_FireBurst
```

Each item under `skills:` is **one mechanic** plus optional keys:

| key | meaning |
|---|---|
| `target` | who or where it acts (default: the caster). See [Targeting](04-targeting.md) |
| `if` | a list of conditions; all must pass, checked per target |
| `chance` | 0-1, chance this line runs |

A single value fills the mechanic's main attribute: `- delay: 20` is the same as
`- delay: { ticks: 20 }`, `- heal: 8` as `- heal: { amount: 8 }`. Keys are case-insensitive.
Quote text that starts with `<`, `>`, `!`, `@`, `*` or `&`, or contains `": "`.

### Skill settings

Keep this order in every skill so files are easy to scan:

| key | meaning |
|---|---|
| `description` | MiniMessage text, shown in gem tooltips and `/cr info` |
| `cooldown` | seconds before the player can cast it again |
| `mana` | mana cost |
| `cast-time`, `cast-interrupt` | casting before it fires. See [Auras and casting](05-auras-casting.md) |
| `conditions` | the whole skill casts only if these pass (no mana or cooldown is spent otherwise) |
| `item` | colours/model of the skill item from `/cr give` |
| `extends`, `variables` | templates, below |
| `skills` | the lines |

### Timelines

`delay` pauses the rest of the list:

```yaml
BladeCombo:
  skills:
  - damage: 3
    target: target
  - delay: 10              # 0.5 s
  - damage: 3
    target: target
  - delay: 10
  - damage: 6
    target: target
```

### The classic one-line format

The MythicMobs-style format still works and can be mixed with the structured one:

```yaml
  skills:
  - damage{amount=6} @NearbyEntities{radius=5} ?health{value=>10}
  - heal: 4
```

`@` is the targeter, `?` a condition (`?!` negates).

## Folder layout and naming

The bundled content follows one pattern. Use it for your own files too:

```
skills/
  shared/effects.yml    Fx_* reusable impact effects
  gems/<skill>.yml      skills granted by gems
  classes/<class>.yml   a class kit and its passives
  bosses/<boss>.yml     a boss's moves
  showcase/<topic>.yml  examples to learn from
```

| what | name | example |
|---|---|---|
| castable skill | `PascalCase` | `Fireball`, `Warcry` |
| helper used by `onhit`/`onland`/`onend`/`ontick` | `<Skill>_<Part>`, same file | `Fireball_Impact`, `Rend_Tick` |
| boss move | `<Boss>_<Move>` | `FrostLord_Nova` |
| shared effect | `Fx_<Name>` | `- skill: Fx_FrostBurst` |

Colour palettes for `DUST`/`DUST_COLOR_TRANSITION` (`color` → `tocolor`):

| theme | colours | theme | colours |
|---|---|---|---|
| fire | `#ff3d00` → `#ffb300` | frost | `#38bdf8` → `#e0f2fe` |
| arcane | `#a855f7` → `#f0abfc` | holy | `#fde047` → `#fffbeb` |
| blood | `#b91c1c` → `#f97316` | shadow | `#4c1d95` → `#0f0a1a` |
| nature | `#22c55e` → `#bbf7d0` | storm | `#60a5fa` → `#e0e7ff` |

Shared effects: `Fx_FireBurst`, `Fx_FrostBurst`, `Fx_ArcaneBurst`, `Fx_HolyBurst`,
`Fx_BloodBurst`, `Fx_ShadowBurst`, `Fx_StormBurst`, `Fx_NatureBloom`. They draw at the
skill's **origin**: the impact point inside `onhit`/`onland`/`onend` helpers, the caster
otherwise.

## Templates and variables

```yaml
ArcaneBolt:
  cooldown: 2
  variables: { color: "#a855f7", damage: 4, range: 20 }
  skills:
  - line: { particle: DUST, color: "${color}", step: 0.3 }
    target: { targetLocation: "${range}" }
  - damage: "${damage}"
    target: { target: "${range}" }

FireBolt:
  extends: ArcaneBolt          # inherit every key you don't set
  variables: { color: "#ff5f1f", damage: 6 }
```

`skills` is replaced, not merged, when you set it in a child. Full example:
`skills/showcase/templates.yml`.

## Formulas

Numbers in `damage`, `heal`, `potion` (duration, level), `ignite`, `aura` (duration, shield),
`chain` and `projectiletrail` (damage) can be formulas, worked out on every cast:

```yaml
- damage: "6 + <caster.level> * 0.5"
- damage: "max(4, <target.mhp> * 0.1)"           # 10% of max health, at least 4
- damage: "8 + (<target.mhp> - <target.hp>) * 0.15"   # stronger on wounded targets
- damage: "5 + 4 * <target.aura.chill>"            # per stack of an aura
```

| variables | |
|---|---|
| `<caster.X>`, `<target.X>`, `<trigger.X>` | X = `hp`, `mhp`, `hppct`, `level`, `mana`, `maxmana`, `armor`, `distance`, `l.x`, `l.y`, `l.z` |
| `<target.aura.ID>` | stacks of an aura; also `.time` (seconds left) and `.shield` |
| `<power>` | the cast's power (runes raise it) |

Operators `+ - * / % ^` and functions `min max abs floor ceil round sqrt clamp random`.

## Conditions

As a line `if:` (per target) or skill `conditions:` (whole skill):

| condition | example |
|---|---|
| `health` | `{ health: "<10" }` |
| `level` | `{ level: 10 }` or `{ level: { min: 10, max: 20 } }` |
| `class` | `{ class: Mage }` or `{ class: "Mage,Cleric" }` |
| `hasaura` | `{ hasaura: Chill }`, `{ hasaura: { id: Chill, stacks: ">=3" } }` |
| `distance` | `{ distance: { max: 5 } }` |
| `permission` | `{ permission: my.perm }` |

## Text placeholders

Inside messages and text: `<caster.name>`, `<caster.hp>`, `<caster.level>`, `<caster.class>`,
`<caster.l.x>`... and the same for `<target...>` and `<trigger...>`.

## Validation

- `/cr validate` lists every problem with **file:line** and a "did you mean" hint: unknown
  mechanics/attributes, wrong types, bad formulas, unknown variables, broken `extends`, and
  references to missing skills, gems or recipes.
- `/cr reload` prints a summary. A file with a YAML syntax error stops the reload and the old
  content stays active. See [Running a server](12-admin-production.md).
- `plugins/CelestiRift/docs/reference.md` lists every attribute with aliases and defaults,
  including add-on mechanics.
