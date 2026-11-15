# 9. Boss

[← สารบัญ](index.md) · [English](../en/09-bosses.md)

boss คือมอนที่เลือดเยอะ มีแถบเลือด มี skill ตอนเกิด / ตามเวลา / ตอนโดนตี / ตามช่วงเลือด /
ตอนตาย และมีของดรอปของตัวเอง boss ไม่หายเมื่อ chunk ถูก unload หรือ restart

## ไฟล์: boss หนึ่งตัว สองไฟล์

```
bosses/frost_lord.yml          ตัว boss และของดรอป
skills/bosses/frost_lord.yml   ท่าของมัน (FrostLord_*)
```

ใช้โฟลเดอร์ย่อยและ `.yaml` ได้ `bosses.yml` คือคู่มือ field

```yaml
# bosses/frost_lord.yml
FrostLord:
  type: STRAY
  name: "<gradient:#7dd3fc:#3b82f6><bold>Frost Lord</bold></gradient>"
  health: 400
  damage: 9
  bossbar: { color: BLUE, style: NOTCHED_10, radius: 48 }
  skills:
    on-spawn: [FrostLord_Intro]
    timer:
    - { every: 160, skill: FrostLord_Nova }      # tick: ทุก 8 วินาที
    - { every: 100, skill: FrostLord_Lance }
    on-damaged:
    - { skill: FrostLord_Blink, chance: 0.08 }
    phases:
    - { below: 60%, skill: FrostLord_Minions }   # ครั้งเดียว
    - { below: 30%, skill: FrostLord_Enrage }
    on-death: [FrostLord_Shatter]
  loot:                                          # field เดียวกับ drops.yml
    xp: 500
    drops:
    - { gem: FrostGem, chance: 1.0 }
    - { shardvoid: 2, chance: 0.5 }
```

| key | ความหมาย |
|---|---|
| `type`, `name`, `health`, `damage`, `glowing` | ตัวมอน |
| `bossbar` | `color`, `style`, `radius` (แสดงให้ผู้เล่นในระยะนี้) |
| `skills.on-spawn` / `on-death` | รายชื่อ skill |
| `skills.timer` | `{ every: <tick>, skill }` |
| `skills.on-damaged` | `{ skill, chance }` |
| `skills.phases` | `{ below: 50%, skill }` แต่ละอันทำงานครั้งเดียว |
| `loot` | `xp`, `looting-bonus`, `drops` |

## เขียนท่าของ boss

skill ของ boss ทำงาน**ในฐานะตัว boss**: `@self` คือ boss, `@target` คือตัวที่มันสู้อยู่,
`@trigger` คือคนที่ตี (`on-damaged`, phase) หรือคนที่ฆ่า (`on-death`) `nearby` โดนศัตรูของ boss
(ผู้เล่นและสัตว์เลี้ยง) ไม่โดนลูกน้องของมันเอง

**ท่าใหญ่ควรมีวงเตือน** ผู้เล่นจะได้หลบทัน:

```yaml
FrostLord_Nova:
  skills:
  - telegraph: { skill: FrostLord_NovaBlast, radius: 6, duration: 30, color: "#4da6ff" }
    target: casterLocation

FrostLord_NovaBlast:
  skills:
  - damage: 6
    target: { nearby: 6 }          # วัดจากวง
```

ไอเดีย: ข้อความลอย (`display` แบบ text) ตอนคลั่ง, `summon` ลูกน้องเมื่อถึง phase, ท่า `ring`
ที่ลงโทษคนยืนไกล, `meteor` คู่กับวงเตือนที่ใช้เวลาเท่ากัน

## คำสั่ง

`/cr boss spawn <id> [player]`, `/cr boss list`, `/cr boss killall` (เฉพาะ chunk ที่โหลดอยู่)
