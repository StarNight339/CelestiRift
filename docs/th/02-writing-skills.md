# 2. เขียน skill

[← สารบัญ](index.md) · [English](../en/02-writing-skills.md)

## หน้าตาของ skill

ทุกไฟล์ `.yml`/`.yaml` ใน `skills/` ถูกโหลด จะอยู่ในโฟลเดอร์ย่อยไหนก็ได้

```yaml
Fireball:
  description: "<gray>Hurl a blazing orb that explodes on impact."   # แสดงใน tooltip และ /cr info
  cooldown: 3              # วินาที
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

แต่ละบรรทัดใต้ `skills:` คือ **mechanic หนึ่งตัว** และใส่ key เพิ่มได้:

| key | ความหมาย |
|---|---|
| `target` | ทำกับใครหรือที่ไหน (ไม่ใส่ = ตัวผู้ใช้) ดู [การเลือกเป้าหมาย](04-targeting.md) |
| `if` | รายการ condition ต้องผ่านทุกข้อ เช็คแยกทีละเป้าหมาย |
| `chance` | 0-1 โอกาสที่บรรทัดนี้จะทำงาน |

ใส่ค่าเดียวได้ จะไปเป็น attribute หลักของ mechanic: `- delay: 20` เท่ากับ
`- delay: { ticks: 20 }` และ `- heal: 8` เท่ากับ `- heal: { amount: 8 }` ชื่อ key ไม่สนตัวพิมพ์เล็กใหญ่
ข้อความที่ขึ้นต้นด้วย `<`, `>`, `!`, `@`, `*` หรือ `&` หรือมี `": "` อยู่ ต้องใส่เครื่องหมายคำพูด

### ค่าตั้งของ skill

เรียงตามลำดับนี้ทุก skill จะได้อ่านง่าย:

| key | ความหมาย |
|---|---|
| `description` | ข้อความ MiniMessage แสดงใน tooltip ของ gem และ `/cr info` |
| `cooldown` | วินาทีก่อนใช้ซ้ำได้ |
| `mana` | mana ที่ใช้ |
| `cast-time`, `cast-interrupt` | เวลาร่ายก่อน skill ทำงาน ดู [Aura และการร่าย](05-auras-casting.md) |
| `conditions` | ทั้ง skill ใช้ได้ก็ต่อเมื่อผ่านเงื่อนไข (ถ้าไม่ผ่านจะไม่เสีย mana และไม่ติด cooldown) |
| `item` | สี/model ของ item skill จาก `/cr give` |
| `extends`, `variables` | template ดูด้านล่าง |
| `skills` | รายการบรรทัด |

### ทำเป็นจังหวะ (timeline)

`delay` หยุดรอก่อนทำบรรทัดถัดไป:

```yaml
BladeCombo:
  skills:
  - damage: 3
    target: target
  - delay: 10              # 0.5 วินาที
  - damage: 3
    target: target
  - delay: 10
  - damage: 6
    target: target
```

### แบบเขียนบรรทัดเดียว (classic)

รูปแบบแบบ MythicMobs ยังใช้ได้ และเขียนปนกับแบบใหม่ได้:

```yaml
  skills:
  - damage{amount=6} @NearbyEntities{radius=5} ?health{value=>10}
  - heal: 4
```

`@` คือ targeter และ `?` คือ condition (`?!` คือกลับเงื่อนไข)

## โครงสร้างโฟลเดอร์และการตั้งชื่อ

ไฟล์ที่มากับ plugin ใช้รูปแบบเดียวกันทั้งหมด ไฟล์ของคุณก็ใช้แบบนี้ได้:

```
skills/
  shared/effects.yml    เอฟเฟกต์ Fx_* ที่ใช้ซ้ำ
  gems/<skill>.yml      skill ที่ gem ให้
  classes/<class>.yml   ชุด skill และ passive ของ class
  bosses/<boss>.yml     ท่าของ boss
  showcase/<topic>.yml  ตัวอย่างไว้ศึกษา
```

| อะไร | ตั้งชื่อ | ตัวอย่าง |
|---|---|---|
| skill ที่ใช้ได้ | `PascalCase` | `Fireball`, `Warcry` |
| skill ย่อยที่ถูกเรียกจาก `onhit`/`onland`/`onend`/`ontick` | `<Skill>_<Part>` ไว้ไฟล์เดียวกัน | `Fireball_Impact`, `Rend_Tick` |
| ท่า boss | `<Boss>_<Move>` | `FrostLord_Nova` |
| เอฟเฟกต์ส่วนกลาง | `Fx_<Name>` | `- skill: Fx_FrostBurst` |

ชุดสีสำหรับ `DUST`/`DUST_COLOR_TRANSITION` (`color` → `tocolor`):

| ธีม | สี | ธีม | สี |
|---|---|---|---|
| ไฟ | `#ff3d00` → `#ffb300` | น้ำแข็ง | `#38bdf8` → `#e0f2fe` |
| เวท | `#a855f7` → `#f0abfc` | ศักดิ์สิทธิ์ | `#fde047` → `#fffbeb` |
| เลือด | `#b91c1c` → `#f97316` | มืด | `#4c1d95` → `#0f0a1a` |
| ธรรมชาติ | `#22c55e` → `#bbf7d0` | พายุ | `#60a5fa` → `#e0e7ff` |

เอฟเฟกต์ส่วนกลาง: `Fx_FireBurst`, `Fx_FrostBurst`, `Fx_ArcaneBurst`, `Fx_HolyBurst`,
`Fx_BloodBurst`, `Fx_ShadowBurst`, `Fx_StormBurst`, `Fx_NatureBloom` จะวาดที่ **origin**
ของ skill: ถ้าอยู่ใน skill ย่อยของ `onhit`/`onland`/`onend` คือจุดที่ชน ถ้าไม่ใช่ คือตัวผู้ใช้

## Template และตัวแปร

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
  extends: ArcaneBolt          # ใช้ทุก key ที่ไม่ได้เขียนเองจากตัวแม่
  variables: { color: "#ff5f1f", damage: 6 }
```

ถ้าเขียน `skills` ในตัวลูก จะแทนที่ทั้งหมด ไม่รวมกับของตัวแม่ ตัวอย่างเต็ม:
`skills/showcase/templates.yml`

## สูตรคำนวณ

ตัวเลขใน `damage`, `heal`, `potion` (ระยะเวลา, เลเวล), `ignite`, `aura` (ระยะเวลา, โล่),
`chain` และ `projectiletrail` (ดาเมจ) ใส่เป็นสูตรได้ คำนวณใหม่ทุกครั้งที่ใช้:

```yaml
- damage: "6 + <caster.level> * 0.5"
- damage: "max(4, <target.mhp> * 0.1)"           # 10% ของเลือดสูงสุด อย่างน้อย 4
- damage: "8 + (<target.mhp> - <target.hp>) * 0.15"   # ยิ่งเป้าเลือดน้อยยิ่งแรง
- damage: "5 + 4 * <target.aura.chill>"            # ตามจำนวน stack ของ aura
```

| ตัวแปร | |
|---|---|
| `<caster.X>`, `<target.X>`, `<trigger.X>` | X = `hp`, `mhp`, `hppct`, `level`, `mana`, `maxmana`, `armor`, `distance`, `l.x`, `l.y`, `l.z` |
| `<target.aura.ID>` | จำนวน stack ของ aura และ `.time` (วินาทีที่เหลือ), `.shield` |
| `<power>` | พลังของการร่ายครั้งนั้น (rune เพิ่มได้) |

เครื่องหมาย `+ - * / % ^` และฟังก์ชัน `min max abs floor ceil round sqrt clamp random`

## Condition

ใช้เป็น `if:` ของบรรทัด (เช็คทีละเป้าหมาย) หรือ `conditions:` ของ skill (ทั้ง skill):

| condition | ตัวอย่าง |
|---|---|
| `health` | `{ health: "<10" }` |
| `level` | `{ level: 10 }` หรือ `{ level: { min: 10, max: 20 } }` |
| `class` | `{ class: Mage }` หรือ `{ class: "Mage,Cleric" }` |
| `hasaura` | `{ hasaura: Chill }`, `{ hasaura: { id: Chill, stacks: ">=3" } }` |
| `distance` | `{ distance: { max: 5 } }` |
| `permission` | `{ permission: my.perm }` |

## ตัวแทนข้อความ (placeholder)

ใช้ในข้อความได้: `<caster.name>`, `<caster.hp>`, `<caster.level>`, `<caster.class>`,
`<caster.l.x>`... และแบบเดียวกันกับ `<target...>` และ `<trigger...>`

## การตรวจไฟล์

- `/cr validate` แสดงทุกปัญหาพร้อม**ชื่อไฟล์:เลขบรรทัด** และคำแนะนำ "did you mean":
  mechanic/attribute ที่ไม่รู้จัก, ชนิดค่าผิด, สูตรผิด, ตัวแปรที่ไม่มี, `extends` ที่เสีย
  และการอ้างถึง skill, gem หรือสูตรที่ไม่มีอยู่
- `/cr reload` สรุปผลให้ ถ้าไฟล์ไหนเขียน YAML ผิดจนอ่านไม่ได้ จะไม่โหลดอะไรใหม่เลย
  และ skill ชุดเดิมยังใช้ได้ต่อ ดู [ดูแล server](12-admin-production.md)
- `plugins/CelestiRift/docs/reference.md` มี attribute ทุกตัวพร้อมชื่อย่อและค่าเริ่มต้น
  รวมถึง mechanic จาก plugin เสริมด้วย
