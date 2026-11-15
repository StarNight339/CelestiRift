# 12. ดูแล server

[← สารบัญ](index.md) · [English](../en/12-admin-production.md)

## Reload แบบปลอดภัย

`/cr reload` เช็ค `config.yml` และ `messages.yml` ก่อน แล้วค่อยโหลดทุกอย่าง ถ้ามีไฟล์ไหนเขียน
YAML ผิด**จะไม่เปลี่ยนอะไรเลย**: skill, gem, class, boss และของดรอปชุดเดิมยังใช้ได้ และจะบอกว่า
ไฟล์ไหนเสีย แก้แล้ว reload อีกครั้ง หรือใช้ `/cr reload force` เพื่อโหลดส่วนอื่นโดยข้ามไฟล์ที่เสีย
เอฟเฟกต์ 3D จะถูกลบเมื่อ reload

## เมื่อมีปัญหา

- `/cr validate`: ทุกปัญหาพร้อมไฟล์:บรรทัด
- `settings.debug: true` ใน `config.yml` บันทึกผลการใช้ skill ทุกครั้ง (`SUCCESS`, `ON_COOLDOWN`,
  `NOT_ENOUGH_MANA`...) ทุกครั้งที่หุ่นทดสอบโดนดาเมจ และ stack trace เต็ม
- ถ้าบรรทัดหนึ่งของ skill error (เช่น mechanic จาก plugin เสริม) บรรทัดที่เหลือยังทำงานต่อ
  error ของบรรทัดเดิมถูกบันทึกไม่เกินหนึ่งครั้งทุก 30 วินาที

## ทดสอบ skill

- `/cr dummy spawn enemy|ally|player [health]`: หุ่นที่ไม่ตาย แสดงดาเมจ, การฮีล และ DPS
  ไม่โดนดาเมจจากสภาพแวดล้อม (ติดบล็อก, ตกที่สูง) ดู [การเลือกเป้าหมาย](04-targeting.md)
- `vfx.damage-numbers: true` แสดงตัวเลขดาเมจ/ฮีลลอยเมื่อผู้เล่นตี, boss และหุ่นโดนตี
- ทดสอบด้วยโหมด **survival** ผู้เล่น creative ไม่มีวันโดน skill

## ข้อมูลผู้เล่น

mana, เลเวล, XP และ class เก็บไว้ในตัวผู้เล่น (persistent data) ไม่มี database ข้อมูลอยู่รอด
หลัง restart และย้ายไปกับการ backup โลก aura ไม่ถูกบันทึก (หายเมื่อออกเกม)

## การอัปเดต

- **CelestiRift:** เปลี่ยนไฟล์ jar แล้ว restart ไฟล์ของคุณจะไม่ถูกทับ ตัวอย่างใหม่ถูกสร้าง
  เฉพาะตอนติดตั้งใหม่ ถ้าอยากได้ให้ copy เอง
- **สำรอง** `plugins/CelestiRift/` ก่อนเปลี่ยนแปลงใหญ่ (เก็บไว้ในโฟลเดอร์ `backups/` ได้
  โฟลเดอร์นี้ไม่ถูกโหลด)
- **Paper:** CelestiRift ต้องใช้ Paper 26.3 build 252 ขึ้นไป และจะแจ้ง error ถ้าเก่ากว่า ก่อนอัปเดต
  Minecraft นักพัฒนาลองรันการทดสอบใน server กับเวอร์ชันใหม่ได้ (ด้านล่าง)

## สำหรับนักพัฒนา

| งาน | คำสั่ง |
|---|---|
| build | `./gradlew build` (fail ถ้าใช้ Paper API ที่ deprecated) |
| เปิด server ทดสอบพร้อม plugin | `./gradlew :celestirift-core:runServer` (port 25570) |
| ทดสอบใน server อัตโนมัติ | `./gradlew :celestirift-core:smokeTest` (server ใหม่, ใช้ทุก skill, เรียกทุก boss, ทดสอบ reload; fail ถ้ามีปัญหา) |
| ลอง Minecraft เวอร์ชันอื่น | เพิ่ม `-PminecraftVersion=26.4` / `-PpaperVersion=26.4.build.+` |
| copy เข้า server ของคุณ | `./gradlew deploy` (`deployDir` ใน `gradle.properties`) |
| ปล่อยเวอร์ชัน | เพิ่มหัวข้อ `## <version>` ใน `CHANGELOG.md` แล้ว push tag `v<version>` |
