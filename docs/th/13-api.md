# 13. API

[← สารบัญ](index.md) · [English](../en/13-api.md)

compile กับ `celestirift-api` (เวอร์ชัน 2.2) และใส่ `depend: [CelestiRift]` (หรือ `softdepend`)
ใน `plugin.yml` ทุกอย่างที่เปิดให้ใช้อยู่ใน `com.celestirift.api`

```java
CelestiRiftApi api = CelestiRiftApi.get();
```

## เพิ่ม mechanic

```java
public final class FreezeMechanic implements TargetedEntityMechanic {
    private final NumberValue ticks;

    public FreezeMechanic(ConfigMap cfg) {
        // getNumber รับสูตรได้: "60 + <caster.level> * 2"
        this.ticks = cfg.getNumber(60, "ticks", "t");
    }

    @Override
    public void cast(SkillMetadata meta, Entity target) {
        target.setFreezeTicks((int) ticks.get(meta, target));
    }
}

// onEnable
CelestiRiftApi.get().registerMechanic(FreezeMechanic::new, "freeze");
```

```yaml
- freeze: { ticks: "40 + <caster.level>" }
  target: target
```

- interface: `TargetedEntityMechanic`, `TargetedLocationMechanic`, `NoTargetMechanic`,
  `Targeter`, `Condition` ลงทะเบียนด้วย `registerMechanic`, `registerTargeter`, `registerCondition`
- อ่าน attribute ใน constructor ด้วย `ConfigMap`: `getString`, `getInt`, `getDouble`,
  `getBoolean`, `getNumber` (สูตร), `getChoice` (เลือกจากรายการ) ระบบจดไว้อัตโนมัติว่าอ่าน
  attribute อะไร attribute ของคุณจึงถูกตรวจ, มีคำแนะนำ "did you mean" และขึ้นใน
  `docs/reference.md` และ schema ของ editor
- skill ถูก compile หลังเปิด server หนึ่ง tick ให้ลงทะเบียนใน `onEnable` ถ้าลงทะเบียนทีหลัง
  ให้เรียก `reloadSkills()`

## เมธอด

| เมธอด | ทำอะไร |
|---|---|
| `cast(player, skill, trigger)` → `CastResult` | ผู้เล่นใช้ skill ตามกฎ cooldown/mana; ได้ `CASTING` ถ้ามีเวลาร่าย |
| `castRaw(skill, SkillMetadata)` | ใช้ skill ในฐานะ entity ใดก็ได้ ไม่มี cooldown/mana |
| `skillNames()`, `hasSkill(name)`, `reloadSkills()` | skill |
| `getMana`, `setMana`, `getMaxMana`, `getCooldown` | mana และ cooldown (ตาม UUID ผู้เล่น) |
| `getLevel`, `getXp`, `giveXp`, `getPlayerClass`, `setPlayerClass`, `classIds` | RPG |
| `spawnBoss(id, location)` | boss |
| `auraStacks(entity, id)`, `removeAura(entity, id)` | aura (2.2) |

## Event

| event | เมื่อไหร่ | ทำอะไรได้ |
|---|---|---|
| `SkillPreCastEvent` | ก่อนผู้เล่นใช้ skill หลังผ่านการเช็ค | ยกเลิก, เปลี่ยน mana และ cooldown |
| `SkillCastEvent` | หลังใช้ skill สำเร็จ | — |
| `ManaChangeEvent` | mana เปลี่ยน | ยกเลิก, เปลี่ยนค่า |
| `GemSocketEvent` | ใส่ gem ในหน้า socket | ยกเลิก |
| `GemFuseEvent` | ก่อน fusion ใช้วัตถุดิบ | ยกเลิก |
| `PlayerXpGainEvent` | ก่อนได้ XP | ยกเลิก, เปลี่ยนจำนวน |
| `PlayerLevelUpEvent` | หลังขึ้นแต่ละเลเวล | — |
| `PlayerClassChangeEvent` | ก่อนเปลี่ยน class | ยกเลิก |

เช็คความเข้ากันได้ตอนทำงานด้วย `ApiVersion.isCompatible(2, 2)`
