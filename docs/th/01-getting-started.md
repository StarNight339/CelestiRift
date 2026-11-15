# 1. เริ่มต้นใช้งาน

[← สารบัญ](index.md) · [English](../en/01-getting-started.md)

## ติดตั้ง

1. ใช้ **Paper 26.3 build 252 ขึ้นไป** กับ **Java 25** ถ้า build เก่ากว่านี้ CelestiRift จะแจ้ง
   error ตอนเปิด ให้อัปเดต `paper.jar`
2. ใส่ `CelestiRift-<version>.jar` ใน `plugins/` แล้วเปิด server
3. (ไม่บังคับ) ติดตั้ง **OrionRP** เพื่อให้ host และส่ง resource pack ให้อัตโนมัติ ดู
   [Resource pack](10-resource-pack.md)

## โฟลเดอร์ของ plugin

เปิดครั้งแรก CelestiRift จะสร้างโครงสร้างนี้ใน `plugins/CelestiRift/`:

```
config.yml          ตั้งค่า: mana, combat/PvP, gem, resource pack, เลเวล, เอฟเฟกต์
messages.yml        ข้อความทั้งหมดที่ผู้เล่นเห็น (MiniMessage)
skills/             skill ทั้งหมด จะมีโฟลเดอร์ย่อยกี่ชั้นก็ได้ (ดู "เขียน skill")
  _guide.yml          โครงสร้าง, กฎการตั้งชื่อ, ชุดสี (ไม่มี skill ในไฟล์นี้)
  shared/effects.yml  เอฟเฟกต์ Fx_* ที่ใช้ซ้ำได้
  gems/               skill ของแต่ละ gem
  classes/            ชุด skill และ passive ของ class (mage, warrior, cleric)
  bosses/             ท่าของ boss
  showcase/           ตัวอย่างไว้ศึกษา
gems.yml            คู่มือ field ของ gem          gems/      gem แยกไฟล์ตามธาตุ
fusions.yml         คู่มือ field ของ fusion       fusions/   สูตร fusion ไฟล์ละสูตร
bosses.yml          คู่มือ field ของ boss         bosses/    boss ไฟล์ละตัว
classes.yml         class                        drops.yml  ของดรอปและ XP จากมอน
docs/reference.md   ตาราง attribute ที่สร้างอัตโนมัติ (สร้างใหม่ทุกครั้งที่ reload)
schema/             JSON schema สำหรับ autocomplete ใน editor
pack/               ไฟล์ zip ของ resource pack
```

ไฟล์ตัวอย่างจะถูกสร้าง**เฉพาะตอนติดตั้งใหม่** (โฟลเดอร์ยังว่าง) การอัปเดต plugin จึงไม่ทับ
หรือสร้างไฟล์ซ้ำกับของคุณ ถ้าอยากได้ตัวอย่างใหม่ ให้ copy จาก jar หรือจาก repository นี้

## ลองในเกม

```
/cr gem EmberGem                 รับ gem (หรือ /cr gem เพื่อดู gem ทั้งหมด)
/cr socket                       เปิดหน้า socket โดยใช้ไอเทมในมือเป็นอาวุธ
/cr dummy spawn                  หุ่นทดสอบที่แสดงดาเมจและ DPS
/class                           ดูรายชื่อ class; /class choose Mage
/cr boss spawn FrostLord         เรียก boss ตัวอย่าง
/cr reload                       โหลดไฟล์ที่แก้ใหม่
/cr validate                     แสดงปัญหาทั้งหมดในไฟล์ พร้อมชื่อไฟล์และเลขบรรทัด
```

## แก้ไฟล์ให้สะดวก

ใช้ VS Code ติดตั้ง extension **YAML** (Red Hat) ไฟล์ตัวอย่างจะขึ้นต้นด้วยบรรทัด
`# yaml-language-server: $schema=...` ซึ่งทำให้มี autocomplete และคำอธิบายเวลาชี้เมาส์
ของทุก mechanic และ attribute ใส่บรรทัดนี้ในไฟล์ของคุณด้วย ถ้าไฟล์อยู่ในโฟลเดอร์ย่อย
ต้องเพิ่ม `../` อีกหนึ่งชั้น
