# 5. Aura และการร่าย

[← สารบัญ](index.md) · [English](../en/05-auras-casting.md)

## Aura: buff, debuff, stack, โล่

`aura` ติดสถานะที่มีชื่อให้เป้าหมาย ติดซ้ำจะเพิ่ม stack

```yaml
Frostbolt_Hit:
  skills:
  - aura: { id: Chill, type: debuff, duration: 120, maxstacks: 3, speed: -0.15, damage-taken: 0.05, particle: SNOWFLAKE }
    target: trigger

Shatter:
  skills:
  - damage: "6 + 5 * <target.aura.chill>"        # ใช้ stack ทั้งหมด
    target: { nearby: 10 }
    if: [ { hasaura: Chill } ]
  - removeaura: Chill
    target: { nearby: 10 }
```

| attribute | ความหมาย |
|---|---|
| `id` | ชื่อ aura (ไม่สนตัวพิมพ์เล็กใหญ่) |
| `type` | `buff` หรือ `debuff` โดย debuff ไม่ติดฝ่ายเดียวกัน |
| `duration` | tick (ใส่สูตรได้) |
| `stacks`, `maxstacks` | stack ที่เพิ่มต่อครั้ง; จำนวนสูงสุด |
| `refresh` | ติดซ้ำแล้วรีเวลาใหม่ (ค่าเริ่มต้น true) |
| `damage-dealt`, `damage-taken`, `speed` | ต่อ stack: `0.1` = +10%, `-0.15` = -15% |
| `shield` | โล่รับดาเมจ (ใส่สูตรได้) โล่แตกแล้ว aura หาย |
| `interval` + `ontick` | skill ทุก N tick ร่ายโดยคนที่ติด aura ให้ `@trigger` = คนที่ติด (ดาเมจต่อเนื่อง) |
| `onend` | ตอนหมดเวลาหรือโล่แตก |
| `onhit` + `onhit-consume` | คนที่ติดร่ายทุกครั้งที่ตีโดน `@trigger` = ตัวที่โดน; ใช้ n stack ต่อครั้ง ("3 ครั้งถัดไป") |
| `ondamaged` | คนที่ติดร่ายตอนโดนตี `@trigger` = คนที่ตี (สะท้อนดาเมจ) |
| `particle` | แสดงรอบตัวคนที่ติด |

- `removeaura: Chill` หรือ `{ id: Chill, stacks: 1 }` เพื่อลบบาง stack หรือ `{ type: debuff }` เพื่อล้าง debuff ทั้งหมด
- condition `hasaura: { id: Chill, stacks: ">=3" }` และสูตร `<target.aura.chill>`, `.time`, `.shield`
- aura หายเมื่อตาย ออกจากเกม หรือ restart

ตัวอย่างใน `skills/showcase/auras.yml`: ติด mark แล้วระเบิด, ไฟเผา, battle cry, โล่, empower,
หนามสะท้อน, ล้าง debuff

## Cast time (ร่ายก่อนใช้)

```yaml
Cataclysm:
  cooldown: 30
  mana: 60
  cast-time: 40                    # 2 วินาที มีแถบความคืบหน้าบน action bar
  cast-interrupt: [move, damage]   # ค่าเริ่มต้น. [damage] = เดินได้; none = ไม่มีอะไรขัด
  skills:
  - meteor: { ... }
```

- เสีย mana และติด cooldown **ก็ต่อเมื่อร่ายจบ** ร่ายได้ทีละ skill
- มอนและ boss ไม่มี cast time ให้ใช้วงเตือนแทน

## Telegraph (วงเตือน)

`telegraph` วาดวงเตือนบนพื้นที่ค่อยๆ เติมจนเต็ม แล้วร่าย `skill` โดยใช้วงเป็น **origin**
`nearby`/`ring` ใน skill นั้นวัดจากวง ผู้เล่นจึงเดินหลบออกได้

```yaml
FrostLord_Nova:
  skills:
  - telegraph: { skill: FrostLord_NovaBlast, radius: 6, duration: 30, color: "#4da6ff" }
    target: casterLocation
```

attribute: `skill`, `radius`, `inner` (วงแบบวงแหวน), `duration`, `color` ตำแหน่งวงถูกล็อกตอนเริ่ม
ถ้าคนร่ายตายก่อน การโจมตีจะไม่เกิด ถ้าไม่ใส่ `skill` จะเป็นแค่วงเตือน
(เช่น ใช้คู่กับ `meteor` ที่ใช้เวลาเท่ากัน)

## การแสดง cooldown

- item ของ skill (`/cr give`) แสดงแถบนับถอยหลังบน hotbar แบบเดียวกับของในเกม
- ถ้ากดปุ่ม (คลิก, F, Q, ย่อ) แล้วใช้ไม่ได้ action bar จะบอกเหตุผล: cooldown ที่เหลือ หรือ
  mana ไม่พอ ส่วน trigger แบบ passive จะไม่แสดงข้อความ
