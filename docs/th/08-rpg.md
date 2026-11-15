# 8. RPG: mana, เลเวล, class, ของดรอป

[← สารบัญ](index.md) · [English](../en/08-rpg.md)

## Mana

skill ใช้ mana ตาม `mana:` mana ฟื้นทุกวินาที และแสดงบน action bar หลังใช้ skill

```yaml
# config.yml
mana:
  enabled: true
  max: 100.0                # ตอนไม่มี class
  regen-per-second: 5.0
  show-actionbar: true
```

ถ้ามี class จะใช้ค่า mana สูงสุดและอัตราฟื้นของ class แทน

## เลเวลและ XP

ผู้เล่นได้ XP จากการใช้ skill และการฆ่ามอน (ดูหัวข้อของดรอปด้านล่าง) XP ที่ต้องใช้ไปเลเวลถัดไปคือ
`base × level^exponent` ตอนเลเวลอัปจะแสดงหนึ่งบรรทัดบน action bar พร้อมเสียง ไม่มี title

```yaml
leveling:
  enabled: true
  max-level: 50
  xp-curve: { base: 100, exponent: 1.5 }   # L1→2: 100, L10→11: 3162
  xp: { skill-cast: 1 }
  level-up: { restore-mana: true }
classes:
  allow-change: false        # true = ใช้ /class choose ซ้ำได้
```

เลเวล, XP, class และ mana เก็บไว้ในตัวผู้เล่น ไม่ต้องตั้ง database

## Class (`classes.yml`)

```yaml
Warrior:
  display: "<gradient:#ef4444:#f97316><bold>Warrior</bold></gradient>"
  description: "<gray>Less mana, but one extra socket on every weapon."
  icon: IRON_SWORD
  mana: { base: 80, per-level: 2 }
  mana-regen: { base: 4, per-level: 0.1 }
  socket-bonus: 1            # ช่อง socket เพิ่มจาก gems.max-slots
  starting-gems: [SmiteGem]  # ได้ครั้งเดียวตอนเลือก class
  # permission: celestirift.class.warrior
  passives:
  - { trigger: crit, skill: Cleave }
  - { trigger: kill, skill: Bloodlust }
  - { trigger: low_health, skill: LastStand, min-level: 10 }
```

**Passive** ทำงานเองตาม trigger ([trigger](06-gems-sockets.md)) ใส่ได้: `chance`,
`every` (วินาที ใช้กับ `timer`), `min-level` ยังติด cooldown และใช้ mana ตาม skill

ผู้เล่น: `/class`, `/class info <id>`, `/class choose <id>`, `/profile`
skill เช็ค `{ class: Mage }` และ `{ level: 10 }` ได้ และแสดง `<caster.class>` / `<caster.level>` ได้

## ชุด skill ประจำ class (`skills/classes/`)

| class | Lv 1 | Lv 5 | Lv 10 | Lv 20 | passive |
|---|---|---|---|---|---|
| **Mage** | `Frostbolt`: ยิงน้ำแข็ง ติด Chill ซ้อนได้ | `Shatter`: น้ำแข็งงอกใต้ศัตรูที่ติด Chill ดาเมจ 6 + 5 ต่อ stack | `Stormcall`: ร่าย 0.75 วินาที สายฟ้ากระโดด | `Cataclysm`: ร่าย 2 วินาที อุกกาบาตพร้อมวงเตือน | เลือดต่ำ → `ManaBarrier` |
| **Warrior** | `Charge`: กระโจนแล้วฟาดเป็นกรวย | `Whirlwind`: หมุน 3 รอบ | `Rend`: ฟันกวาด + เลือดไหลซ้อนได้ | `Warcry`: พวกเดียวกันดาเมจ +25% และโล่ ศัตรูโดนแรงขึ้น | critical → `Cleave`, ฆ่าได้ → `Bloodlust`, เลือดต่ำ → `LastStand` |
| **Cleric** | `HolyBolt`: ดาเมจ + ฮีลตัวเอง | `Renew`: ฮีลต่อเนื่องให้พวกที่เลือดน้อยสุด 2 คน | `Sanctuary`: ร่าย 1 วินาที วงฮีล/ล้าง debuff/โล่ | `Judgement`: + 15% ของเลือดที่เป้าเสียไป | ทุก 5 วินาที → `Blessing` |

skill ในชุดล็อกด้วย `conditions: [ { class: Mage }, { level: 10 } ]` แจกด้วย
`/cr give <skill>` หรือทำเป็น gem ดาเมจและการฮีลแรงขึ้นตามเลเวล

## ของดรอป

`drops.yml` (หรือ `drops/*.yml`) ให้ gem, Shardvoid และ XP เมื่อมอนตาย ทุกตารางที่ตรงเงื่อนไขจะทำงานหมด

```yaml
Blazes:
  mobs: [BLAZE]
  xp: 12
  looting-bonus: 0.01        # โอกาสเพิ่มต่อเลเวล Looting
  drops:
  - { gem: EmberGem, chance: 0.04 }
  - { shardvoid: 1, chance: 0.01 }
```

เลือกมอนด้วย `mobs`, `name` (ชื่อมีคำนี้), `tag` (scoreboard tag), `boss`, `worlds`
และ `player-kill-only` (ค่าเริ่มต้น true) boss ใส่ `loot:` ไว้ในไฟล์ของตัวเองได้ด้วย
ถ้ามี OrionRP ของดรอปเป็นไอเทม custom ได้: `- { item: demo:ruby, chance: 0.1 }` ([OrionRP](14-orionrp.md))
