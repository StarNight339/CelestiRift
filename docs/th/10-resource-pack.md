# 10. Resource pack

[← สารบัญ](index.md) · [English](../en/10-resource-pack.md)

CelestiRift มี pack เล็กๆ มาให้: model ของ gem แต่ละ tier, rune, Shardvoid, ม้วนคัมภีร์สำหรับ
item skill และกรอบ tooltip สีทองสำหรับ gem tier 5 ภาพในนั้นเป็นภาพชั่วคราว
เปลี่ยนเป็นภาพของคุณเองได้ (ดูด้านล่าง)

ทุกครั้งที่เปิด server pack จะถูกเขียนไว้ที่ `plugins/CelestiRift/pack/CelestiRift-pack.zip`
`/cr pack` แสดง SHA-1 และค่าที่ตั้งไว้

## ใช้กับ OrionRP (แนะนำ)

ถ้าติดตั้ง **OrionRP** ไว้ CelestiRift จะรวม pack ของตัวเองเข้าไปใน pack ของ OrionRP และ
OrionRP จะ host และส่งให้ผู้เล่นเอง ไม่ต้องตั้งอะไรเพิ่ม:

```yaml
resource-pack:
  enabled: true
  use-orionrp: true
  send: { url: "", hash: "" }   # ไม่ได้ใช้เมื่อมี OrionRP
```

ใน console จะขึ้นว่า `Resource pack is merged and sent by OrionRP.` สิ่งอื่นที่ OrionRP เพิ่มให้
(glyph, ไอเทม, action skill, sprite, พื้นหลังเมนู, HUD): [OrionRP](14-orionrp.md)

## ไม่ใช้ OrionRP

1. อัปโหลดไฟล์ zip ไว้ที่ที่ผู้เล่นดาวน์โหลดได้โดยตรง หรือรวมโฟลเดอร์ `assets/` เข้ากับ pack ของ server
2. ตั้ง `resource-pack.send.url` (และ `hash` ถ้าแก้ zip) แล้ว `enabled: true`
3. `/cr reload` ผู้เล่นจะได้ pack ตอนเข้าเกม ส่งซ้ำได้ด้วย `/cr pack send [player]`

อย่าตั้ง `enabled: true` จนกว่าผู้เล่นจะได้ pack จริง ไม่อย่างนั้นไอเทมจะเป็นสีม่วงดำ

## หน้าตาของ gem

| ค่าตั้ง | ใช้กับ |
|---|---|
| `resource-pack.models.gem` | model เริ่มต้นของ gem ตาม tier (`{tier}` → 1-5) |
| `resource-pack.tier-models` | กำหนดราย tier: `{ 1: "ns:model", 5: "ns:model" }` |
| `resource-pack.gem-models` | กำหนดราย gem: `{ EmberGem: "ns:model" }` |
| `resource-pack.models.rune` / `shardvoid` / `skill-item` | ไอเทมเหล่านั้น |
| `resource-pack.tooltip-styles` | กรอบ tooltip ตาม tier (5 = `celestirift:legendary`) |

`tier-models` และ `gem-models` ใช้**เฉพาะตอนเปิด pack** ผู้เล่นที่ไม่มี pack จะไม่เห็นภาพสีม่วงดำ
ส่วน `model:` ที่เขียนใน gem เองจะใช้เสมอ

### ตัวอย่าง: ใช้ภาพจาก assets ของ OrionRP

ใส่ชุดภาพไว้ใน `plugins/OrionRP/assets/<namespace>/` (เช่น `items:item559`) แล้วชี้ gem ไปที่ภาพนั้น:

```yaml
resource-pack:
  tier-models: { 1: "items:item547", 2: "items:item542", 3: "items:item536", 4: "items:item551", 5: "items:item554" }
  models: { rune: "items:item558", shardvoid: "items:item561" }
  gem-models:
    EmberGem: "items:item559"
    FrostGem: "items:item557"
```

ตรวจ license ของภาพก่อนจะรวมไว้ใน plugin ที่ปล่อยสาธารณะ การเก็บภาพไว้ในโฟลเดอร์ OrionRP
ของ server จะทำให้ภาพไม่ติดไปกับไฟล์ jar

## Build pack เอง (สำหรับนักพัฒนา)

`./gradlew :celestirift-core:resourcePack` → `celestirift-core/build/resourcepack/CelestiRift-pack.zip`
(+ `.sha1`) pack รองรับ format 84-97 (Minecraft 26.1-26.3) ถ้า client ใหม่กว่านี้ให้เพิ่ม
`max_format` ใน `pack.mcmeta`
