# CelestiRift reference

_Generated from the installed mechanics on every `/cr reload`. Do not edit._

The **first attribute** of each component is its *shorthand*: `- delay: 20` means `delay{ticks=20}`.

## Mechanics

### `aura` — aliases: `buff`, `status`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `id` *(shorthand)* | name | string | `aura` |
| `type` |  | string | `buff` |
| `duration` | d, ticks | number | `100.0` |
| `stacks` | add | int | `1` |
| `maxstacks` | max | int | `1` |
| `refresh` |  | boolean | `true` |
| `interval` | every | int | `20` |
| `ontick` |  | string | `` |
| `onhit` |  | string | `` |
| `ondamaged` |  | string | `` |
| `onend` | onexpire | string | `` |
| `onhit-consume` | consume | int | `0` |
| `damage-dealt` | dealt | double | `0.0` |
| `damage-taken` | taken | double | `0.0` |
| `speed` |  | double | `0.0` |
| `shield` |  | number | `0.0` |
| `particle` |  | string | `` |

### `blackhole` — aliases: `vortex`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radius` | r | double | `4.0` |
| `arms` |  | int | `3` |
| `points` | n | int | `24` |
| `duration` | ticks | int | `40` |
| `rotationspeed` | spin | double | `0.35` |
| `tightness` |  | double | `6.0` |
| `pull` |  | boolean | `true` |
| `pullstrength` | strength | double | `0.5` |

### `chain` — aliases: `chainlightning`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `damage` *(shorthand)* | dmg, amount | number | `4.0` |
| `jumps` | targets, bounces | int | `4` |
| `radius` | r, range | double | `6.0` |
| `decay` |  | double | `0.8` |
| `delay` |  | int | `3` |
| `color` | c | string | `#9be7ff` |
| `onhit` |  | string | `` |

### `circle` — aliases: `ring`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radius` | r | double | `2.0` |
| `points` | n | int | `40` |
| `plane` |  | string | `xz` |

### `damage` — aliases: `dmg`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `amount` *(shorthand)* | a, damage, d | number | `1.0` |
| `ignorearmor` | ia | boolean | `false` |

### `delay` — aliases: `wait`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `ticks` *(shorthand)* | t, duration, d | int | `20` |

### `display` — aliases: `model`, `hologram`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `type` *(shorthand)* | t | string | `block` |
| `frames` | glyph, glyphs, sprite | string | `` |
| `frametime` | frame | int | `2` |
| `material` | m, block, item | string | `` |
| `model` |  | string | `` |
| `text` |  | string | `` |
| `scale` | size | double | `1.0` |
| `endscale` | grow | double | `NaN` |
| `rise` |  | double | `0.0` |
| `spin` |  | double | `0.0` |
| `tilt` | pitch | double | `0.0` |
| `y` | offsety | double | `0.0` |
| `duration` | d, ticks | int | `40` |
| `follow` |  | boolean | `false` |
| `glow` |  | boolean | `false` |
| `glowcolor` |  | string | `` |
| `billboard` | face | string | `fixed` |
| `onend` |  | string | `` |

### `fly` — aliases: `flight`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `duration` *(shorthand)* | d, ticks, t | int | `200` |
| `speed` | s, flyspeed | double | `0.1` |
| `launch` | up, boost | double | `0.0` |
| `safeland` | slowfall | boolean | `true` |

### `giveitem` — aliases: `orionitem`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `item` *(shorthand)* | i, id | string | `` |
| `amount` | a | int | `1` |

### `heal`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `amount` *(shorthand)* | a, heal, h | number | `1.0` |

### `helix`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radius` | r | double | `1.0` |
| `height` | h | double | `3.0` |
| `coils` | turns | double | `3.0` |
| `points` | n | int | `60` |
| `strands` |  | int | `1` |

### `ignite` — aliases: `burn`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `ticks` *(shorthand)* | duration, d | number | `40.0` |

### `leap` — aliases: `jump`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `forward` *(shorthand)* | f, velocity, v | double | `1.2` |
| `up` | u, y | double | `0.7` |

### `lightning` — aliases: `strike`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `damage` *(shorthand)* | d | boolean | `true` |

### `line` — aliases: `beam`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `step` | gap | double | `0.4` |

### `message` — aliases: `msg`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `msg` *(shorthand)* | m, message | string | `` |

### `meteor`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `onland` *(shorthand)* | skill, s | string | `` |
| `block` | material, m | string | `MAGMA_BLOCK` |
| `height` | h | double | `18.0` |
| `drift` | angle | double | `6.0` |
| `duration` | d, ticks | int | `20` |
| `scale` | size | double | `1.6` |
| `trail` | particle | string | `FLAME` |

### `particle` — aliases: `particles`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `type` *(shorthand)* | particle, p | string | `FLAME` |
| `amount` | count, a | int | `15` |
| `spread` | offset, o | double | `0.4` |
| `speed` | s | double | `0.02` |

### `potion` — aliases: `effect`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `type` *(shorthand)* | t | string | `SPEED` |
| `duration` | d, ticks | number | `100.0` |
| `level` | lvl, l | number | `1.0` |

### `projectile` — aliases: `shoot`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `type` *(shorthand)* | t | string | `SNOWBALL` |
| `velocity` | v, speed | double | `1.5` |

### `projectiletrail` — aliases: `trail`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `maxdistance` | range, distance | double | `30.0` |
| `substeps` |  | int | `4` |
| `shape` |  | string | `none` |
| `shaperadius` | sr | double | `0.5` |
| `shapepoints` | sp | int | `8` |
| `coilspeed` |  | double | `1.2` |
| `hitradius` | hr | double | `1.0` |
| `damage` | dmg | number | `0.0` |
| `onhit` | hitskill | string | `` |
| `stoponblock` |  | boolean | `true` |

### `push` — aliases: `knockback`, `velocity`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `velocity` *(shorthand)* | v, power | double | `1.0` |
| `mode` | m | string | `away` |
| `y` | lift | double | `0.3` |

### `removeaura` — aliases: `cleanse`, `dispel`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `id` *(shorthand)* | name | string | `all` |
| `type` |  | string | `any` |
| `stacks` |  | int | `0` |
| `runend` |  | boolean | `false` |

### `shockwave` — aliases: `nova`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `startradius` | start | double | `0.5` |
| `maxradius` | max, radius, r | double | `8.0` |
| `density` |  | double | `1.5` |
| `minpoints` |  | int | `12` |
| `plane` |  | string | `xz` |

### `skill` — aliases: `cast`, `meta`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `skill` *(shorthand)* | s, name | string | `` |

### `slash` — aliases: `airslash`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radius` | r | double | `3.0` |
| `arc` | degrees | double | `160.0` |
| `forward` | f | double | `2.0` |
| `points` | n | int | `28` |
| `animate` |  | boolean | `true` |
| `duration` | ticks | int | `4` |
| `curve` | depth | double | `0.6` |

### `sound`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `sound` *(shorthand)* | s | string | `entity.experience_orb.pickup` |
| `volume` | vol, v | double | `1.0` |
| `pitch` | p | double | `1.0` |

### `sphere`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radius` | r | double | `2.0` |
| `points` | n | int | `120` |

### `star`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `spikes` | points, n | int | `5` |
| `radius` | r | double | `3.0` |
| `innerratio` | inner | double | `0.45` |
| `peredge` | step | int | `8` |
| `plane` |  | string | `xz` |
| `rotation` | rot | double | `0.0` |

### `stealitem` — aliases: `steal`, `disarm`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `slot` *(shorthand)* | s, from | string | `random` |
| `amount` | a, count | int | `1` |
| `material` | item, type | string | `` |
| `drop` |  | boolean | `false` |

### `summon` — aliases: `spawn`, `minion`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `type` *(shorthand)* | mob, m, t | string | `ZOMBIE` |
| `amount` | a, count | int | `1` |
| `radius` | r, spread | double | `1.5` |
| `duration` | d, ticks | int | `0` |
| `health` | hp | double | `0.0` |
| `name` | n | string | `` |
| `baby` |  | boolean | `false` |
| `glowing` | glow | boolean | `false` |
| `silent` |  | boolean | `false` |
| `target` |  | boolean | `true` |
| `onsummon` | summonskill | string | `` |

### `telegraph` — aliases: `warning`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `skill` *(shorthand)* | s, then | string | `` |
| `radius` | r | double | `4.0` |
| `inner` |  | double | `0.0` |
| `duration` | d, ticks | int | `30` |
| `color` | c | string | `#ff3030` |

### `teleport` — aliases: `tp`

_No attributes._

### `tornado` — aliases: `twister`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `particle` *(shorthand)* | type, p | string | `FLAME` |
| `count` | amount, a | int | `1` |
| `offset` | spread, o | double | `0.0` |
| `speed` | s | double | `0.0` |
| `size` |  | double | `1.0` |
| `color` | c, fromcolor | string | `#ffffff` |
| `tocolor` | to | string | `#ffffff` |
| `radiusbottom` | rbottom, rb | double | `0.5` |
| `radiustop` | rtop, rt, radius, r | double | `3.0` |
| `height` | h | double | `5.0` |
| `rings` |  | int | `12` |
| `points` | n | int | `16` |
| `duration` | ticks | int | `60` |
| `spin` | rotationspeed | double | `0.4` |
| `swirl` | twist | double | `4.0` |
| `pull` |  | boolean | `true` |
| `pullstrength` | strength | double | `0.3` |
| `lift` |  | double | `0.15` |

## Targeters

### `casterlocation` — aliases: `cl`

_No attributes._

### `cone`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `range` *(shorthand)* | r, length | double | `5.0` |
| `angle` | a | double | `90.0` |
| `filter` | f | string | `enemies` |
| `limit` | max | int | `0` |
| `sort` |  | string | `none` |
| `ignorecaster` | ic | boolean | `true` |
| `players` | playersonly | boolean | `false` |

### `line` — aliases: `ray`, `strip`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `length` *(shorthand)* | l, range | double | `12.0` |
| `width` | w | double | `1.0` |
| `filter` | f | string | `enemies` |
| `limit` | max | int | `0` |
| `sort` |  | string | `none` |
| `ignorecaster` | ic | boolean | `true` |
| `players` | playersonly | boolean | `false` |

### `nearbyentities` — aliases: `nearby`, `eir`, `entitiesinradius`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `radius` *(shorthand)* | r | double | `5.0` |
| `filter` | f | string | `enemies` |
| `limit` | max | int | `0` |
| `sort` |  | string | `none` |
| `ignorecaster` | ic | boolean | `true` |
| `players` | playersonly | boolean | `false` |

### `origin` — aliases: `o`

_No attributes._

### `ring` — aliases: `donut`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `inner` *(shorthand)* | min | double | `3.0` |
| `outer` | max, radius, r | double | `6.0` |
| `filter` | f | string | `enemies` |
| `limit` | max | int | `0` |
| `sort` |  | string | `none` |
| `ignorecaster` | ic | boolean | `true` |
| `players` | playersonly | boolean | `false` |

### `self` — aliases: `caster`

_No attributes._

### `target` — aliases: `t`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `range` *(shorthand)* | r | double | `30.0` |
| `raysize` | rs | double | `0.5` |
| `filter` | f | string | `all` |
| `limit` | max | int | `0` |
| `sort` |  | string | `none` |
| `ignorecaster` | ic | boolean | `true` |
| `players` | playersonly | boolean | `false` |

### `targetlocation` — aliases: `tl`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `range` *(shorthand)* | r | int | `30` |

### `trigger`

_No attributes._

## Conditions

### `class` — aliases: `playerclass`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `is` *(shorthand)* | class, classes | string | `` |

### `distance` — aliases: `dist`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `max` *(shorthand)* | distance, d | double | `10.0` |
| `min` |  | double | `0.0` |

### `hasaura` — aliases: `aura`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `id` *(shorthand)* | name | string | `` |
| `stacks` | s | string | `>=1` |

### `health` — aliases: `hp`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `value` *(shorthand)* | v, amount, a | string | `>0` |

### `level` — aliases: `lvl`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `min` *(shorthand)* | level, l | int | `1` |
| `max` |  | int | `2147483647` |

### `permission` — aliases: `perm`

| Attribute | Aliases | Type | Default |
|---|---|---|---|
| `perm` *(shorthand)* | permission, p | string | `` |

