# 11. คำสั่งและ permission

[← สารบัญ](index.md) · [English](../en/11-commands-permissions.md)

## แอดมิน: `/celestirift` (ชื่อย่อ `/cr`, `/skill`, `/skills`) permission `celestirift.admin`

ทุก argument กด tab ได้ (skill, gem, boss, class, trigger, ผู้เล่น)

### ไฟล์และเนื้อหา

| คำสั่ง | ทำอะไร |
|---|---|
| `/cr reload` | โหลดใหม่แบบปลอดภัย (ไฟล์เสียจะใช้ข้อมูลเดิมต่อ) |
| `/cr reload force` | โหลดใหม่และข้ามไฟล์ที่เสีย |
| `/cr validate` | ทุกปัญหาพร้อมไฟล์:บรรทัดและคำแนะนำ |
| `/cr list` | skill ที่โหลดอยู่ |
| `/cr info <skill>` | คำอธิบาย, cooldown, mana, จำนวนบรรทัด |

### Skill และไอเทม

| คำสั่ง | ทำอะไร |
|---|---|
| `/cr cast <skill> [player]` | ใช้ skill (ไม่ติด cooldown/ไม่ใช้ mana) |
| `/cr give <skill> [player] [material]` | item skill: คลิกขวาเพื่อใช้ |
| `/cr gem` | ดู gem ทั้งหมด (หน้าคลัง gem) |
| `/cr gem <GemId> [player]` | ให้ gem ที่กำหนดไว้ |
| `/cr gem <skill> [tier] [trigger] [player]` | ให้ gem แบบสร้างเฉพาะกิจ |
| `/cr shardvoid [amount] [player]` | ให้ Shardvoid |
| `/cr socket` | หน้า socket |
| `/cr trigger` | หน้าเปลี่ยน trigger ของ gem/อาวุธในมือ |
| `/cr fuse` | รวม gem ในกระเป๋าเป็นไอเทม gem |
| `/cr item` · `/cr item settrigger <skill> <trigger>` · `/cr item clear` | ช่อง socket ของอาวุธในมือ |

### ผู้เล่น, RPG และ boss

| คำสั่ง | ทำอะไร |
|---|---|
| `/cr mana [player]` · `/cr mana set <player> <amount>` | mana |
| `/cr level set <player> <level>` | ตั้งเลเวล |
| `/cr xp add <player> <amount>` | ให้ XP |
| `/cr class set <player> <class\|none>` | ตั้งหรือลบ class |
| `/cr boss spawn <id> [player]` · `list` · `killall` | boss |
| `/cr dummy spawn [enemy\|ally\|player] [health]` · `/cr dummy clear` | หุ่นทดสอบ |
| `/cr pack` · `/cr pack send [player]` | สถานะ resource pack / ส่งซ้ำ |

## คำสั่งของผู้เล่น

| คำสั่ง | permission | ค่าเริ่มต้น |
|---|---|---|
| `/class`, `/class info <id>`, `/class choose <id>` | `celestirift.class` | ทุกคน |
| `/profile` | `celestirift.profile` | ทุกคน |
| `/profile <player>` | `celestirift.profile.others` | op |
| ใช้ item skill | `celestirift.use` | ทุกคน |

class กำหนด permission ของตัวเองได้ (`permission:` ใน `classes.yml`)
