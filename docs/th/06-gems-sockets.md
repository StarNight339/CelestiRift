# 6. Gem และ socket

[← สารบัญ](index.md) · [English](../en/06-gems-sockets.md)

ผู้เล่นได้ skill มาในรูปของ **gem** แล้ว **socket** เข้าอาวุธ เมื่อถืออาวุธ skill ของ gem
จะทำงานตาม trigger ของแต่ละตัว **Rune** (gem แบบ passive) ช่วยเพิ่มพลัง skill ของอาวุธ
หรือใส่ในเกราะได้

## ไฟล์ gem

```
gems.yml            คู่มือ field (เขียน gem ในนี้ก็ได้)
gems/fire.yml       Ember Gem, Starfall Gem
gems/frost.yml      Frost Gem
gems/holy.yml       Mending Gem, Gem of Judgement
gems/shadow.yml     Dark Gem
gems/storm.yml      Storm Gem
gems/steel.yml      Blade Gem
gems/runes.yml      Rune of Power, Farsight, Warding, Ironskin
gems/fusion.yml     Blackfire, Blizzard, Radiant (ผลจาก fusion)
```

skill ของแต่ละ gem อยู่ที่ `skills/gems/<skill>.yml`

```yaml
EmberGem:
  skill: Fireball
  tier: 3                  # 1 Common · 2 Uncommon · 3 Rare · 4 Epic · 5 Legendary
  trigger: RIGHT_CLICK     # ค่าเริ่มต้น ผู้เล่นเปลี่ยนเองได้
  material: BLAZE_POWDER   # หน้าตาเมื่อไม่มี resource pack
  name: "<gradient:#ff3d00:#ffb300><bold>Ember Gem</bold></gradient>"
  lore:
  - "Forged in dragonfire, still warm to the touch."
```

| key | ความหมาย |
|---|---|
| `skill` | skill ที่ gem ร่าย (gem แบบ active) |
| `type` | `ACTIVE` (ค่าเริ่มต้น) หรือ `PASSIVE` (rune) |
| `tier` | 1-5: สี, ดาว และกรอบ tooltip legendary เมื่อเป็น 5 |
| `trigger` | `RIGHT_CLICK`, `LEFT_CLICK`, `SWAP_HANDS`, `DROP_ITEM`, `SNEAK`, `UNSNEAK`, `ATTACK`, `CRIT`, `PROJECTILE_HIT`, `KILL`, `DAMAGED`, `LOW_HEALTH`, `TIMER`, `JOIN` |
| `name`, `lore` | ชื่อแบบ MiniMessage; ข้อความบรรยาย (แสดงเป็นตัวเอียงสีเทา) |
| `material` | ไอเทมที่ใช้เมื่อไม่มี resource pack |
| `model` | model ที่ใช้เสมอ แนะนำให้ใช้ `resource-pack.gem-models` แทน ([Resource pack](10-resource-pack.md)) |

### Tooltip

สร้างให้อัตโนมัติจาก gem และ skill ของมัน gem ที่ผู้เล่นมีอยู่แล้วก็อัปเดตตาม:

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

## Rune (gem แบบ passive)

```yaml
PowerRune:
  type: PASSIVE
  tier: 4
  power: 1.5                 # ดาเมจ/ฮีลของ skill ในอาวุธ +50%
FarsightRune:
  type: PASSIVE
  range: 1.4                 # ระยะ/รัศมี +40%
  cooldown: 0.85             # cooldown -15%
WardingRune:
  type: PASSIVE
  buff: { type: RESISTANCE, duration: 100, level: 1 }   # ทุกครั้งที่ skill ทำงาน
```

**ใส่ในเกราะ** (หมวก, เสื้อ/elytra, กางเกง, รองเท้า) rune จะทำงานตอนสวม: `buff` ติดตลอด
และ `power`/`range`/`cooldown` มีผลกับ**ทุก** skill ที่ใช้ เกราะใส่ได้แค่ rune

## หน้า socket (`/cr socket`)

```
 [Skill Gems]  [Runes]  [Fusion]                    [วิธีใช้]
 [อาวุธ] →  ช่อง 1 … ช่อง 7                         ← gem ที่ใส่อยู่ตอนนี้
 ▼ gem ของคุณในหมวดที่เลือก
 (gem จากกระเป๋า 2 แถว มีหลายหน้าถ้าเยอะ)
 [◀]  [Shardvoid +ช่อง]  [สถานะ]  [ปิด]  [▶]
```

1. ถืออาวุธแล้วพิมพ์ `/cr socket` อาวุธจะเข้าไปอยู่ในหน้านี้ ถ้าจะเปลี่ยน ให้คลิกไอเทมอื่นในกระเป๋า
2. เลือกหมวด: **Skill Gems** แสดงเฉพาะ gem แบบ active ที่มี **Runes** แสดงเฉพาะ rune รวมกองพร้อมจำนวน
3. คลิก gem เพื่อใส่ (หรือคลิก gem ในกระเป๋าตัวเองก็ได้)
4. **แถวช่อง socket** แสดงทุกช่องพร้อม tooltip และ trigger ของ gem ช่องว่างเป็นสีเทาอ่อน
   ช่องที่ล็อกปลดได้ด้วย Shardvoid
5. **คลิกขวา**ที่ช่องเพื่อถอด gem ออก (ปิดได้ด้วย `gems.allow-unsocket: false`)
6. ปิดหน้าแล้วอาวุธกลับเข้ามือ

ข้อความแจ้งผลขึ้นที่ action bar ("ไม่มีช่องว่าง", "เกราะใส่ได้แค่ rune"...)

## ช่อง socket และ Shardvoid

อาวุธเริ่มต้นมี `gems.default-slots` (1) ช่อง และเพิ่มได้ถึง `gems.max-slots` (6) บวก
`socket-bonus` ของ class **Shardvoid** หนึ่งชิ้นเพิ่มหนึ่งช่อง (กดปุ่มในหน้า socket)
ได้จาก `/cr shardvoid [amount] [player]` หรือจากของดรอป

## เปลี่ยน trigger

`/cr trigger` ตอนถือ gem หรืออาวุธที่ใส่ gem แล้ว: คลิก skill ในแถวบน แล้วคลิก trigger ด้านล่าง
แบบคำสั่ง: `/cr item settrigger <skill> <trigger>` ส่วน `/cr item` แสดงช่อง socket ของอาวุธ
และ `/cr item clear` ล้างทุกช่อง

## Trigger

| trigger | ทำงานเมื่อ | `@trigger` |
|---|---|---|
| `RIGHT_CLICK`, `LEFT_CLICK` | คลิก (อากาศหรือบล็อก) | — |
| `SWAP_HANDS` (F), `DROP_ITEM` (Q) | กดปุ่ม (ปุ่มจะไม่ทำงานตามปกติ) | — |
| `SNEAK`, `UNSNEAK` | เริ่ม/เลิกย่อ | — |
| `ATTACK` | ตีโดนอะไรสักอย่าง | ตัวที่โดน |
| `CRIT` | ตีติด critical (กระโดดตี) | ตัวที่โดน |
| `PROJECTILE_HIT` | ธนู/ตรีศูลที่ยิงไปโดน | ตัวที่โดน |
| `KILL` | ฆ่าได้ | ตัวที่ตาย |
| `DAMAGED` | โดนตี | คนที่ตี |
| `LOW_HEALTH` | เลือดต่ำกว่า `combat.low-health` (ครั้งเดียวต่อการลดผ่านเส้น) | คนที่ตี |
| `TIMER` | ทุก 1 วินาที (ใช้ cooldown ของ skill คุมความถี่) | — |
| `JOIN` | เข้าเกม | — |

trigger เรียกตัวเองซ้ำจาก skill ของตัวเองไม่ได้ แต่ trigger อื่นทำงานต่อได้ (skill ของ ATTACK
ที่ฆ่ามอนได้จะทำให้ KILL ทำงาน) trigger ชุดเดียวกันนี้ใช้กับ passive ของ class และ
`triggers:` แบบเดิมใน `config.yml` ด้วย
