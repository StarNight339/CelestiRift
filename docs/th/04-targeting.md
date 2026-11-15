# 4. การเลือกเป้าหมาย

[← สารบัญ](index.md) · [English](../en/04-targeting.md)

`target:` กำหนดว่าบรรทัดนั้นทำกับใครหรือที่ไหน ถ้าไม่ใส่ จะทำกับตัวผู้ใช้

## Targeter

| targeter | ได้อะไร | ตัวอย่าง |
|---|---|---|
| `self` | ตัวผู้ใช้ | `target: self` |
| `target` | ตัวที่มองอยู่ (ถ้าเป็นมอน คือตัวที่มันกำลังสู้) | `target: { target: 25 }` |
| `trigger` | ตัวที่ทำให้ skill ทำงาน (ตัวที่เราตี, ตัวที่ตีเรา...) | `target: trigger` |
| `nearby` | สิ่งมีชีวิตในรัศมีรอบ origin | `target: { nearby: 6 }` |
| `cone` | รูปกรวยด้านหน้า | `target: { cone: { angle: 90, range: 4 } }` |
| `line` | แถบตรงจากตา ทะลุทุกตัว | `target: { line: { length: 12, width: 1.5 } }` |
| `ring` | ระหว่างรัศมีสองค่า (ยืนใกล้จะไม่โดน) | `target: { ring: { inner: 3, outer: 7 } }` |
| `casterLocation` | ตำแหน่งที่ผู้ใช้ยืน | `target: casterLocation` |
| `targetLocation` | บล็อกที่มองอยู่ | `target: { targetLocation: { range: 30 } }` |
| `origin` | origin ของ skill: จุดที่ชนใน skill ย่อยของ `onhit`/`onland`/`onend` | `target: origin` |

รูปทรงคิดจากขนาดตัวมอน มอนตัวใหญ่จึงโดนง่ายกว่า รัศมีขยายตาม `range` ของ rune

## ใครโดน

`nearby`, `target`, `cone`, `line` และ `ring` ใส่ค่าเพิ่มได้:

```yaml
target: { nearby: { radius: 8, filter: allies, sort: lowesthp, limit: 2 } }
```

| `filter` | ความหมาย |
|---|---|
| `enemies` | **ค่าเริ่มต้นของแบบพื้นที่** ถ้าผู้เล่นใช้: มอนสเตอร์, มอนที่กำลังตีเรา และเมื่อเปิด PvP คือผู้เล่นอื่นกับสัตว์เลี้ยงของเขา ถ้ามอน/boss ใช้: ผู้เล่น, สัตว์เลี้ยงของผู้เล่น และตัวที่กำลังตีมัน |
| `allies` | สัตว์เลี้ยงและมอนที่เราเรียก และผู้เล่นอื่นเมื่อปิด PvP |
| `players` / `mobs` | ตามชนิด |
| `all` | ทุกตัวที่ไม่ได้ถูกกันไว้ (ค่าเริ่มต้นของ `target`) |

`sort`: `nearest` (ใกล้สุด), `furthest` (ไกลสุด), `random` (สุ่ม), `lowesthp` (เลือดน้อยสุด)
`limit`: จำนวนเป้าหมายสูงสุด

## ความปลอดภัยในการต่อสู้

- **ไม่โดนเลย:** armor stand, NPC (Citizens), ผู้เล่นโหมด creative/spectator, ตัวที่อมตะ
  และ mannequin ที่วางไว้ประดับ
- **ด่านสุดท้าย:** mechanic ที่สร้างความเสียหาย (`damage`, `ignite`, `stealitem`, `chain`,
  กระสุน, แรงดูดของ `blackhole`/`tornado`, aura แบบ debuff) ไม่มีวันโดนฝ่ายเดียวกัน
  ไม่ว่าบรรทัดนั้นจะตั้งเป้าไว้แบบไหน
- ดาเมจผ่าน damage event ปกติ เขต **WorldGuard** ที่ตั้ง `pvp deny` และ plugin กันพื้นที่ยังกันได้

```yaml
# config.yml
combat:
  pvp: true             # false = skill ไม่โดนผู้เล่นและสัตว์เลี้ยงของผู้เล่นเลย
  pvp-multiplier: 1.0   # ดาเมจ skill ระหว่างผู้เล่น × ค่านี้
  low-health: 0.3       # เกณฑ์ของ trigger LOW_HEALTH
```

## ทดสอบเป้าหมาย

`/cr dummy spawn enemy|ally|player [health]` เรียกหุ่น mannequin ที่ไม่ตาย และแสดงดาเมจ,
การฮีล และ DPS บน action bar:

| หุ่น | ทำตัวเหมือน |
|---|---|
| `enemy` | มอนสเตอร์: โดน skill พื้นที่, ไม่โดนอะไร 3 วินาทีเลือดเต็มเอง |
| `ally` | เพื่อน: โดน `filter: allies` เริ่มที่เลือด 50% |
| `player` | ผู้เล่นอีกคน ใช้สกินของคุณ: เป็นศัตรูเมื่อเปิด PvP เป็นพวกเดียวกันเมื่อปิด |

`/cr dummy clear` ลบหุ่นทั้งหมด ตัวอย่าง: `skills/showcase/targeting.yml`
