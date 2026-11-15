# 3. Mechanic

[← สารบัญ](index.md) · [English](../en/03-mechanics.md)

mechanic คือสิ่งที่ skill หนึ่งบรรทัดทำ **attribute ตัวแรกในตาราง** คือตัวที่ใช้แบบใส่ค่าเดียว
(`- heal: 8`) attribute, ชื่อย่อ และค่าเริ่มต้นแบบละเอียด: [ตาราง attribute](../en/reference.md)

**ชนิดเป้าหมาย** mechanic แบบ *entity* ทำงานหนึ่งครั้งต่อ entity ที่เป็นเป้า แบบ *location*
ทำงานหนึ่งครั้งต่อตำแหน่ง (ถ้าเป้าเป็น entity จะใช้ตำแหน่งของมัน) แบบ *none* ทำงานครั้งเดียว

## ต่อสู้

| mechanic | ชนิด | attribute | หมายเหตุ |
|---|---|---|---|
| `damage` | entity | `amount`, `ignorearmor` | ใส่สูตรได้; ทำตามกฎความปลอดภัย; `ignorearmor` เป็นดาเมจเวท |
| `heal` | entity | `amount` | ใส่สูตรได้; แสดงตัวเลขสีเขียว |
| `potion` | entity | `type`, `duration`, `level` | ระยะเวลา/เลเวลใส่สูตรได้ |
| `ignite` | entity | `ticks` | ติดไฟ |
| `push` | entity | `velocity`, `mode` (`away`/`toward`), `y` | ผลักหรือดึง |
| `lightning` | location | `damage` | `damage: false` = แค่ภาพ; ฟ้าผ่าจริงกรองเป้าหมายไม่ได้ |
| `chain` | entity | `damage`, `jumps`, `radius`, `decay`, `delay`, `color`, `onhit` | สายฟ้ากระโดดไปหาศัตรูที่ใกล้ที่สุดทีละตัว |
| `stealitem` | entity | `slot`, `amount`, `material`, `drop` | ขโมยไอเทมจากผู้เล่นหรือมอน |

```yaml
- chain: { damage: "7 + <caster.level> * 0.25", jumps: 5, radius: 6, decay: 0.8 }
  target: { target: 25 }
```

## เคลื่อนที่และอื่นๆ

| mechanic | ชนิด | attribute |
|---|---|---|
| `leap` | entity | `forward`, `up` |
| `teleport` | location | (ไปยังตำแหน่งเป้าหมาย) |
| `fly` | entity | `duration`, `speed`, `launch`, `safeland` |
| `summon` | location | `type`, `amount`, `radius`, `duration`, `health`, `name`, `baby`, `glowing`, `silent`, `target`, `onsummon` |
| `projectile` | none | `type` (`FIREBALL`, `SNOWBALL`...), `velocity` (กระสุนของเกม) |
| `message` | entity | `msg` (MiniMessage, ใช้ placeholder ได้) |
| `giveitem` | entity | `item`, `amount`: ไอเทม custom ของ OrionRP ([OrionRP](14-orionrp.md)) |
| `sound` | location | `sound`, `volume`, `pitch` |
| `delay` | none | `ticks` หยุดรอก่อนบรรทัดถัดไป |
| `skill` | none | `skill` เรียก skill อื่นโดยใช้ผู้ใช้/origin เดิม |

มอนที่เรียกออกมาจะจำเจ้าของไว้ และไม่โจมตีฝ่ายของเจ้าของ

## กระสุน

`projectiletrail` ยิง particle ออกจากตา วาดรูปทรงไปตามทาง เมื่อชนจะทำ `damage` และ/หรือ
เรียก `onhit` (โดยใช้**จุดที่ชนเป็น origin** และตัวที่โดนเป็น `@trigger`) โดนเฉพาะศัตรู

```yaml
- projectiletrail: { particle: DUST_COLOR_TRANSITION, color: "#38bdf8", tocolor: "#e0f2fe", size: 1.3,
                     speed: 1.25, maxdistance: 30, shape: helix, shaperadius: 0.28, shapepoints: 2,
                     hitradius: 1.1, damage: "4 + <caster.level> * 0.2", onhit: Frostbolt_Hit }
```

`shape`: `none`, `ring`, `helix` และ `stoponblock: true` (ค่าเริ่มต้น) ชนกำแพงแล้วระเบิด

## Particle รูปทรง

รูปทรงทั้งหมดเป็นแบบ *location* และใช้ค่า particle ร่วมกัน: `particle`, `count`, `offset`,
`speed` ถ้าเป็น `DUST` มี `color`, `size` และ `DUST_COLOR_TRANSITION` มี `tocolor` เพิ่ม

| รูปทรง | attribute ของตัวเอง |
|---|---|
| `circle` | `radius`, `points`, `plane` (`xz`/`xy`/`yz`) |
| `sphere` | `radius`, `points` |
| `helix` | `radius`, `height`, `coils`, `points`, `strands` |
| `line` | `step` ลำแสงจากตาไปยังตำแหน่ง |
| `star` | `spikes`, `radius`, `innerratio`, `peredge`, `plane`, `rotation` |
| `shockwave` | `startradius`, `maxradius`, `speed`, `density` วงที่ขยายออก |
| `slash` | `radius`, `arc`, `forward`, `points`, `animate`, `duration`, `curve` (ชนิด *none* ด้านหน้าผู้ใช้) |
| `blackhole` | `radius`, `arms`, `points`, `duration`, `rotationspeed`, `tightness`, `pull`, `pullstrength` |
| `tornado` | `radiusbottom`, `radiustop`, `height`, `rings`, `points`, `duration`, `spin`, `swirl`, `pull`, `pullstrength`, `lift` |
| `particle` | `type`, `amount`, `spread`, `speed` พ่น particle ธรรมดา (ใส่สีไม่ได้) |

`blackhole` และ `tornado` ดูดเฉพาะศัตรู

## เอฟเฟกต์ 3D (display entity)

```yaml
- display: { type: item, material: IRON_SWORD, scale: 1.6, tilt: 135, y: 4, rise: -3.6, duration: 14, onend: SwordRain_Hit }
- display: { type: block, material: PACKED_ICE, scale: 0.1, endscale: 1.5, duration: 6 }
- display: { type: text, text: "<red><bold>ENRAGED", y: 3, rise: 0.8, scale: 1.5, duration: 40 }
- meteor: { block: MAGMA_BLOCK, scale: 2.2, height: 22, duration: 24, trail: FLAME, onland: Cataclysm_Impact }
```

| mechanic | attribute |
|---|---|
| `display` | `type` (`block`/`item`/`text`/`sprite`), `material` (หรือไอเทม OrionRP `ns:id`), `frames`/`glyph` + `frametime` (sprite), `model` (model จาก resource pack), `text`, `scale`, `endscale`, `spin` (องศา/tick), `tilt` (องศา), `rise` (บล็อก, ติดลบ = ตกลง), `y`, `duration`, `follow`, `glow`, `glowcolor`, `billboard`, `onend` |
| `meteor` | `block`, `height`, `drift`, `duration`, `scale`, `trail`, `onland` |

`type: sprite` (รูป glyph ของ OrionRP ทำเป็นภาพเคลื่อนไหวด้วย `frames`) ดูที่ [OrionRP](14-orionrp.md)
`onend`/`onland` เรียก skill ตรงจุดที่เอฟเฟกต์จบ เอฟเฟกต์ไม่ถูกบันทึกลงโลก และถูกลบเมื่อ
reload หรือปิด server

## Aura, การร่าย และวงเตือน

`aura`, `removeaura` และ `telegraph` อยู่ในหน้า [Aura และการร่าย](05-auras-casting.md)
