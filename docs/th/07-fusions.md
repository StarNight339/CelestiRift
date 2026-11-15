# 7. Fusion

[← สารบัญ](index.md) · [English](../en/07-fusions.md)

fusion ใช้ gem ตั้งแต่สองชิ้นขึ้นไป แล้วใส่ gem ที่แรงกว่าลงในอาวุธ

## ในเกม

`/cr socket` → หมวด **Fusion** แสดงทุกสูตรพร้อม ✔ (มีแล้ว) หรือ ✘ (ยังขาด) ของวัตถุดิบ
แต่ละตัว คลิกสูตรที่ทำได้ ระบบจะหยิบวัตถุดิบอย่างละชิ้นออกจากกระเป๋า แล้วใส่ผลลัพธ์ลงอาวุธ

`/cr fuse` เป็นแบบคำสั่ง จะรวม gem ในกระเป๋าออกมาเป็น **ไอเทม gem** แทนการใส่ลงอาวุธ

## สูตรที่มากับ plugin

| สูตร | วัตถุดิบ | ได้ | skill |
|---|---|---|---|
| Blackfire | Ember Gem + Dark Gem | Blackfire Gem | ลูกไฟวิญญาณที่กลายเป็นหลุมดำ แล้วระเบิด |
| Blizzard | Frost Gem + Storm Gem | Blizzard Gem | พายุ: น้ำแข็งและสายฟ้า 4 จังหวะในวงเตือน |
| Radiant | Gem of Judgement + Blade Gem | Radiant Gem | ดาบทองยักษ์ตกใส่เป้าหมาย |

## ไฟล์สูตร

```
fusions.yml            คู่มือ field (เขียนสูตรในนี้ก็ได้)
fusions/blackfire.yml  ไฟล์ละหนึ่งสูตร
fusions/blizzard.yml
fusions/radiant.yml
```

```yaml
# fusions/blizzard.yml
Blizzard:
  ingredients: [FrostGem, StormGem]
  result-gem: BlizzardGem
```

| key | ความหมาย |
|---|---|
| `ingredients` | id ของ gem 2 ตัวขึ้นไป ใช้อย่างละหนึ่งชิ้น (ใส่ชื่อซ้ำถ้าต้องการสองชิ้น) |
| `result-gem` | id ของ gem ที่จะใส่ (มีชื่อ หน้าตา และคำบรรยายของตัวเอง) |
| `skill` | ใช้แทน `result-gem`: gem อัตโนมัติของ skill นี้ |
| `trigger`, `tier` | ใช้กับผลแบบ `skill` เท่านั้น (ค่าเริ่มต้น `RIGHT_CLICK`, 5) |

gem ที่เป็นผลลัพธ์เป็น gem ปกติใน `gems/fusion.yml` จึงใช้ `/cr gem BlizzardGem` ได้ด้วย
ตั้ง `tier: 5` จะได้ tooltip แบบ legendary plugin อื่นยกเลิก fusion ได้ผ่าน `GemFuseEvent`
([API](13-api.md))
