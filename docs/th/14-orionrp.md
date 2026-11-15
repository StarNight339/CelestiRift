# 14. ใช้ร่วมกับ OrionRP

[← สารบัญ](index.md) · [English](../en/14-orionrp.md)

OrionRP คือ plugin resource pack แบบ ItemsAdder (ไอเทม custom, glyph, GUI, HUD และ host pack)
ไม่บังคับติดตั้ง ถ้าติดตั้ง **OrionRP 0.1.0 ขึ้นไป** CelestiRift จะใช้เองอัตโนมัติ ถ้าไม่มี หรือเป็น
OrionRP เวอร์ชันเก่า จะไม่มีอะไรเปลี่ยน console จะบอกเหตุผล และ CelestiRift ทำงานได้ตามปกติ

ตอนเปิด server จะเห็น:

```
[CelestiRift] OrionRP found: glyphs, OrionRP items, the 'skill' action, GUI backgrounds and the HUD are available.
```

## Resource pack

pack ของ CelestiRift ถูกรวมเข้า pack ของ OrionRP และ OrionRP เป็นคนส่ง ไม่ต้องตั้ง URL
ดู [Resource pack](10-resource-pack.md)

## Glyph ในข้อความ

รูปใน `textures/glyph/<name>.png` ของ OrionRP ใช้เป็น `<glyph:name>` ได้ใน `messages.yml`,
`name`/`lore` ของ gem, `description` ของ skill, บรรทัด `message`, ชื่อมอนที่ summon และข้อความของ `display`

```yaml
EmberGem:
  name: "<glyph:fire> <gradient:#ff3d00:#ffb300><bold>Ember Gem</bold></gradient>"
```

## ไอเทมของ OrionRP ใน skill และของดรอป

```yaml
# drops.yml หรือ loot ของ boss:
drops:
- { item: demo:ruby, amount: 2, chance: 0.1 }

# บรรทัด skill: ให้ไอเทมกับผู้เล่นที่เป็นเป้า
- giveitem: { item: demo:ruby, amount: 1 }

# เอฟเฟกต์ 3D ที่ใช้ model ของไอเทม OrionRP
- display: { type: item, material: demo:fire_orb, spin: 20, duration: 30 }
```

ถ้า id ไอเทมผิด จะแจ้งใน console ครั้งเดียว (`see /orp items`) และ `/cr validate` จะเตือนถ้าใช้
ของดรอปแบบ `item:` โดยไม่มี OrionRP

## ไอเทม เฟอร์นิเจอร์ บล็อก และ GUI ของ OrionRP ที่ใช้ skill ได้

CelestiRift เพิ่ม action `skill <name>` ให้ OrionRP การใช้ skill แบบนี้นับเป็นผู้เล่นใช้เอง
จึงเช็ค mana, cooldown และ condition ตามปกติ

```yaml
# plugins/OrionRP/contents/demo/items/celestirift.yml
fire_wand:
  name: "<gold>Fire Wand"
  actions:
    right-click: ["skill Fireball"]
fire_table:
  furniture:
    interact: ["skill Fireball", "message <red>Hot!"]
```

ใช้กับปุ่มใน GUI (`gui/*.yml`) ได้เหมือนกัน

## เอฟเฟกต์จากรูปภาพ (sprite)

`display` แบบ `type: sprite` แสดงรูป glyph เป็นภาพแบน สว่างเต็มที่ และหันหน้าเข้าหาผู้เล่นเสมอ
ใส่หลาย `frames` จะเล่นเป็นภาพเคลื่อนไหว:

```yaml
- display: { type: sprite, frames: "boom_1,boom_2,boom_3,boom_4", frametime: 2, scale: 4, duration: 8 }
  target: origin
- display: { type: sprite, glyph: magic_circle, tilt: -90, scale: 6, spin: 6, duration: 40 }   # วางราบบนพื้น
  target: casterLocation
```

ใส่รูปไว้ใน `textures/glyph/` ของโฟลเดอร์ content ของ OrionRP แล้ว `/orp rebuild`

## เมนูที่มีพื้นหลังเป็นรูป

```yaml
# config.yml
orionrp:
  gui-backgrounds:
    gem-library: "celestirift:gem_library"   # textures/gui/gem_library.png
    socket: ""                               # ว่าง = เมนูธรรมดา
    trigger: ""
```

รูปขนาด 176×222 พิกเซลพอดีกับเมนู 6 แถว

## HUD: action bar และแถบ mana

HUD ของ OrionRP แสดง action bar ได้หลาย layer พร้อมกัน เมื่อเปิดใช้ ข้อความตอนใช้ skill
(cooldown, mana ไม่พอ, แถบร่าย), ตัวเลขของหุ่นทดสอบ และแถบ mana จะไม่เขียนทับกัน
และไม่ทับ layer ของ plugin อื่น

```yaml
orionrp:
  hud:
    enabled: true
    message-ticks: 40       # ข้อความค้างอยู่นานเท่าไร
    mana-glyphs:            # ถ้ามี glyph เหล่านี้ แถบ mana จะเป็นไอคอน
      full: "mana_full"
      half: "mana_half"
      empty: "mana_empty"
    mana-icons: 10
    # mana-x: -91           # ระยะพิกเซลจากกลางจอถึงขอบซ้ายของแถบ
```

ถ้าไม่มี glyph mana ทั้งสามตัว แถบ mana จะเป็นตัวหนังสือเหมือนเดิม (`mana.actionbar` ใน `messages.yml`)

## การอัปเดต

อัปเดต jar **ทั้งสองตัว**พร้อมกันแล้ว restart ถ้า CelestiRift ใหม่แต่ OrionRP เก่า จะขึ้นว่า
"OrionRP is installed but its API could not be used - update OrionRP" และทำงานแบบไม่มี OrionRP
