# คู่มือ CelestiRift

CelestiRift คือระบบ skill, gem และ RPG สำหรับ Paper เขียน skill ด้วย YAML ใส่ไว้ใน gem
แล้ว socket gem เข้าอาวุธ ผู้เล่นใช้ skill ด้วยการคลิกหรือกดปุ่ม ระบบ class, เลเวล, boss,
ของดรอป และเอฟเฟกต์ 3D ทั้งหมดใช้ skill engine ตัวเดียวกัน

**ต้องใช้:** Paper 26.3 (build 252 ขึ้นไป), Java 25
**ภาษา:** [English](../en/index.md) · [ภาษาไทย](index.md)

## ทำอะไรได้บ้าง

- **Skill** ประกอบจาก mechanic (ดาเมจ, ฮีล, aura, กระสุน, เรียกมอน, วงเตือน...), targeter
  (รอบตัว, กรวย, เส้น, วงแหวน...) และ condition ใส่สูตรให้แรงขึ้นตามเลเวลได้
- **Gem** ที่เก็บ skill ไว้ แล้ว socket เข้าอาวุธ **Rune** ที่เพิ่มพลังให้อาวุธหรือเกราะ
  **Fusion** ที่รวม gem เป็นตัวที่แรงกว่า
- **Class** ที่มี mana, passive และชุด skill ปลดตามเลเวล **เลเวลและ XP** และ **ของดรอป**
- **Boss** ที่มีแถบเลือด, ช่วงเลือด (phase), ท่าตามเวลาพร้อมวงเตือน และของดรอปของตัวเอง
- **เอฟเฟกต์**: particle เป็นรูปทรง, เอฟเฟกต์ 3D (ดาบตก, อุกกาบาต, หนามน้ำแข็ง),
  ตัวเลขดาเมจลอย และ model จาก resource pack

## หน้าทั้งหมด

| # | หน้า | เรื่อง |
|---|---|---|
| 1 | [เริ่มต้นใช้งาน](01-getting-started.md) | ติดตั้ง, เปิดครั้งแรก, โฟลเดอร์ของ plugin |
| 2 | [เขียน skill](02-writing-skills.md) | รูปแบบ YAML, โฟลเดอร์, การตั้งชื่อ, สูตร, การตรวจไฟล์ |
| 3 | [Mechanic](03-mechanics.md) | ทุกอย่างที่ skill หนึ่งบรรทัดทำได้ |
| 4 | [การเลือกเป้าหมาย](04-targeting.md) | ใครโดน, รูปทรงพื้นที่, ความปลอดภัย, PvP |
| 5 | [Aura และการร่าย](05-auras-casting.md) | buff/debuff/stack, ร่าย skill, วงเตือน |
| 6 | [Gem และ socket](06-gems-sockets.md) | gem, rune, หน้า socket, trigger |
| 7 | [Fusion](07-fusions.md) | การรวม gem |
| 8 | [RPG](08-rpg.md) | mana, เลเวล, class, passive, ชุด skill, ของดรอป |
| 9 | [Boss](09-bosses.md) | ไฟล์ boss, phase, ของดรอป |
| 10 | [Resource pack](10-resource-pack.md) | model, OrionRP, ภาพ gem |
| 11 | [คำสั่งและ permission](11-commands-permissions.md) | คำสั่งทั้งหมด |
| 12 | [ดูแล server](12-admin-production.md) | reload, debug, หุ่นทดสอบ, การทดสอบ, อัปเดต |
| 13 | [API](13-api.md) | ต่อยอดจาก plugin อื่น |
| 14 | [OrionRP](14-orionrp.md) | glyph, ไอเทม custom, action skill, sprite, พื้นหลังเมนู, HUD |
| — | [ตาราง attribute](../en/reference.md) | attribute ของทุก mechanic/targeter/condition (สร้างอัตโนมัติ, ภาษาอังกฤษ) |

## ลองใน 5 นาที

1. ใส่ไฟล์ jar ใน `plugins/` แล้วเปิด server CelestiRift จะสร้างตัวอย่าง skill, gem, class และ boss ให้
2. `/cr gem EmberGem` เพื่อรับ gem ถือดาบแล้วพิมพ์ `/cr socket` คลิกที่ gem
3. คลิกขวาด้วยดาบ จะยิง Fireball ออกไป tooltip ของ gem บอก trigger, mana และ cooldown
4. `/cr dummy spawn` จะได้หุ่นทดสอบตรงหน้า ใช้ skill ใส่เพื่อดูดาเมจและ DPS
5. เปิด `plugins/CelestiRift/skills/gems/fireball.yml` แก้ดาเมจ แล้ว `/cr reload`
