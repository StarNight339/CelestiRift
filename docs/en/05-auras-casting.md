# 5. Auras and casting

[← Index](index.md) · [ภาษาไทย](../th/05-auras-casting.md)

## Auras: buffs, debuffs, stacks, shields

`aura` puts a named status on each target. Applying it again adds stacks.

```yaml
Frostbolt_Hit:
  skills:
  - aura: { id: Chill, type: debuff, duration: 120, maxstacks: 3, speed: -0.15, damage-taken: 0.05, particle: SNOWFLAKE }
    target: trigger

Shatter:
  skills:
  - damage: "6 + 5 * <target.aura.chill>"        # consume the stacks
    target: { nearby: 10 }
    if: [ { hasaura: Chill } ]
  - removeaura: Chill
    target: { nearby: 10 }
```

| attribute | meaning |
|---|---|
| `id` | the aura's name (case-insensitive) |
| `type` | `buff` or `debuff`; debuffs never land on your own team |
| `duration` | ticks (formula allowed) |
| `stacks`, `maxstacks` | stacks added per application; the cap |
| `refresh` | reapplying resets the timer (default true) |
| `damage-dealt`, `damage-taken`, `speed` | per stack: `0.1` = +10%, `-0.15` = -15% |
| `shield` | absorbs damage (formula allowed); the aura ends when the shield breaks |
| `interval` + `ontick` | a skill every N ticks, cast by whoever applied the aura, `@trigger` = the holder (damage over time) |
| `onend` | when it expires or its shield breaks |
| `onhit` + `onhit-consume` | cast by the holder on each hit, `@trigger` = the victim; remove n stacks per hit ("next 3 hits") |
| `ondamaged` | cast by the holder when hit, `@trigger` = the attacker (thorns) |
| `particle` | shown around the holder |

- `removeaura: Chill`, or `{ id: Chill, stacks: 1 }` for some stacks, or `{ type: debuff }` to cleanse.
- Condition `hasaura: { id: Chill, stacks: ">=3" }`. Formula `<target.aura.chill>`, `.time`, `.shield`.
- Auras end on death, logout and restart.

Examples in `skills/showcase/auras.yml`: mark and detonate, burn, battle cry, shield,
empower, thorns, cleanse.

## Cast time

```yaml
Cataclysm:
  cooldown: 30
  mana: 60
  cast-time: 40                    # 2 s with a progress bar on the action bar
  cast-interrupt: [move, damage]   # default. [damage] lets you walk; none = never
  skills:
  - meteor: { ... }
```

- Mana and cooldown are spent **only when the cast completes**. One cast at a time.
- Mobs and bosses ignore `cast-time`. Use a telegraph for them.

## Telegraphs

`telegraph` draws a warning circle on the ground that fills up, then casts `skill` with the
circle as its **origin**. `nearby`/`ring` inside that skill are measured from the circle,
so players can step out.

```yaml
FrostLord_Nova:
  skills:
  - telegraph: { skill: FrostLord_NovaBlast, radius: 6, duration: 30, color: "#4da6ff" }
    target: casterLocation
```

Attributes: `skill`, `radius`, `inner` (a ring telegraph), `duration`, `color`. The spot is
fixed when the telegraph starts. If the caster dies, the attack doesn't happen. Without
`skill` it is a pure warning (paired with a `meteor` of the same duration, for example).

## Cooldown feedback

- Skill items (`/cr give`) show the vanilla cooldown sweep on the hotbar.
- When a key press (click, F, Q, sneak) can't cast, the action bar says why: cooldown left
  or not enough mana. Passive triggers stay silent.
