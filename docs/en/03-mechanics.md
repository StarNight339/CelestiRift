# 3. Mechanics

[← Index](index.md) · [ภาษาไทย](../th/03-mechanics.md)

A mechanic is what one skill line does. The **first attribute listed** is the shorthand one
(`- heal: 8`). Exact attributes, aliases and defaults: [reference](reference.md).

**Target kinds.** *entity* mechanics run once per targeted entity. *location* mechanics run
once per location (an entity target counts as its location). *none* runs once.

## Combat

| mechanic | kind | attributes | notes |
|---|---|---|---|
| `damage` | entity | `amount`, `ignorearmor` | formulas; respects combat safety; `ignorearmor` deals magic damage |
| `heal` | entity | `amount` | formulas; shows a green number |
| `potion` | entity | `type`, `duration`, `level` | duration/level take formulas |
| `ignite` | entity | `ticks` | sets on fire |
| `push` | entity | `velocity`, `mode` (`away`/`toward`), `y` | knockback |
| `lightning` | location | `damage` | `damage: false` = visual only; real lightning can't be filtered |
| `chain` | entity | `damage`, `jumps`, `radius`, `decay`, `delay`, `color`, `onhit` | lightning that jumps to the nearest enemy each time |
| `stealitem` | entity | `slot`, `amount`, `material`, `drop` | takes an item from a player or mob |

```yaml
- chain: { damage: "7 + <caster.level> * 0.25", jumps: 5, radius: 6, decay: 0.8 }
  target: { target: 25 }
```

## Movement and utility

| mechanic | kind | attributes |
|---|---|---|
| `leap` | entity | `forward`, `up` |
| `teleport` | location | (to the target location) |
| `fly` | entity | `duration`, `speed`, `launch`, `safeland` |
| `summon` | location | `type`, `amount`, `radius`, `duration`, `health`, `name`, `baby`, `glowing`, `silent`, `target`, `onsummon` |
| `projectile` | none | `type` (`FIREBALL`, `SNOWBALL`...), `velocity` (vanilla projectile) |
| `message` | entity | `msg` (MiniMessage, placeholders) |
| `giveitem` | entity | `item`, `amount`: an OrionRP custom item ([OrionRP](14-orionrp.md)) |
| `sound` | location | `sound`, `volume`, `pitch` |
| `delay` | none | `ticks`, pauses the remaining lines |
| `skill` | none | `skill`, casts another skill with the same caster/origin |

Summons are tagged with their owner and never attack their owner's team.

## Projectiles

`projectiletrail` flies a particle shot from your eyes, draws a shape along the way, and on
impact deals `damage` and/or casts `onhit` (with the **impact point as origin** and the hit
entity as `@trigger`). It only hits enemies.

```yaml
- projectiletrail: { particle: DUST_COLOR_TRANSITION, color: "#38bdf8", tocolor: "#e0f2fe", size: 1.3,
                     speed: 1.25, maxdistance: 30, shape: helix, shaperadius: 0.28, shapepoints: 2,
                     hitradius: 1.1, damage: "4 + <caster.level> * 0.2", onhit: Frostbolt_Hit }
```

`shape`: `none`, `ring`, `helix`. `stoponblock: true` (default) explodes on walls.

## Particle shapes

All shapes are *location* mechanics and share the particle settings:
`particle`, `count`, `offset`, `speed`, and for `DUST`: `color`, `size`;
`DUST_COLOR_TRANSITION` adds `tocolor`.

| shape | own attributes |
|---|---|
| `circle` | `radius`, `points`, `plane` (`xz`/`xy`/`yz`) |
| `sphere` | `radius`, `points` |
| `helix` | `radius`, `height`, `coils`, `points`, `strands` |
| `line` | `step`, a beam from your eyes to the location |
| `star` | `spikes`, `radius`, `innerratio`, `peredge`, `plane`, `rotation` |
| `shockwave` | `startradius`, `maxradius`, `speed`, `density`, an expanding ring |
| `slash` | `radius`, `arc`, `forward`, `points`, `animate`, `duration`, `curve` (*none* kind, in front of you) |
| `blackhole` | `radius`, `arms`, `points`, `duration`, `rotationspeed`, `tightness`, `pull`, `pullstrength` |
| `tornado` | `radiusbottom`, `radiustop`, `height`, `rings`, `points`, `duration`, `spin`, `swirl`, `pull`, `pullstrength`, `lift` |
| `particle` | `type`, `amount`, `spread`, `speed`, a plain burst (no colours) |

`blackhole` and `tornado` pull only enemies.

## 3D effects (display entities)

```yaml
- display: { type: item, material: IRON_SWORD, scale: 1.6, tilt: 135, y: 4, rise: -3.6, duration: 14, onend: SwordRain_Hit }
- display: { type: block, material: PACKED_ICE, scale: 0.1, endscale: 1.5, duration: 6 }
- display: { type: text, text: "<red><bold>ENRAGED", y: 3, rise: 0.8, scale: 1.5, duration: 40 }
- meteor: { block: MAGMA_BLOCK, scale: 2.2, height: 22, duration: 24, trail: FLAME, onland: Cataclysm_Impact }
```

| mechanic | attributes |
|---|---|
| `display` | `type` (`block`/`item`/`text`/`sprite`), `material` (or an OrionRP item `ns:id`), `frames`/`glyph` + `frametime` (sprite), `model` (resource-pack model), `text`, `scale`, `endscale`, `spin` (°/tick), `tilt` (°), `rise` (blocks, negative falls), `y`, `duration`, `follow`, `glow`, `glowcolor`, `billboard`, `onend` |
| `meteor` | `block`, `height`, `drift`, `duration`, `scale`, `trail`, `onland` |

`type: sprite` (OrionRP glyph images, animated with `frames`) is described in [OrionRP](14-orionrp.md).
`onend`/`onland` cast a skill where the effect ended. Effects are never saved to the world and
are removed on reload and shutdown.

## Auras, casting and telegraphs

`aura`, `removeaura` and `telegraph` have their own page: [Auras and casting](05-auras-casting.md).
