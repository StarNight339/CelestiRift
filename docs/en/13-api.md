# 13. API

[← Index](index.md) · [ภาษาไทย](../th/13-api.md)

Compile against `celestirift-api` (version 2.2) and add `depend: [CelestiRift]` (or
`softdepend`) to your `plugin.yml`. Everything public is in `com.celestirift.api`.

```java
CelestiRiftApi api = CelestiRiftApi.get();
```

## Add a mechanic

```java
public final class FreezeMechanic implements TargetedEntityMechanic {
    private final NumberValue ticks;

    public FreezeMechanic(ConfigMap cfg) {
        // getNumber accepts formulas: "60 + <caster.level> * 2"
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

- Interfaces: `TargetedEntityMechanic`, `TargetedLocationMechanic`, `NoTargetMechanic`,
  `Targeter`, `Condition`. Register with `registerMechanic`, `registerTargeter`,
  `registerCondition`.
- Read attributes in the constructor with `ConfigMap`: `getString`, `getInt`, `getDouble`,
  `getBoolean`, `getNumber` (formulas), `getChoice` (one of a list). What you read is
  recorded automatically, so your attributes are validated, get "did you mean" hints, and
  appear in `docs/reference.md` and the editor schema.
- Skills compile one tick after startup, so register in `onEnable`. If you register later,
  call `reloadSkills()`.

## Calls

| method | does |
|---|---|
| `cast(player, skill, trigger)` → `CastResult` | a player cast with cooldown/mana; `CASTING` when it has a cast time |
| `castRaw(skill, SkillMetadata)` | cast as any entity, no cooldown/mana |
| `skillNames()`, `hasSkill(name)`, `reloadSkills()` | skills |
| `getMana`, `setMana`, `getMaxMana`, `getCooldown` | mana and cooldowns (by player UUID) |
| `getLevel`, `getXp`, `giveXp`, `getPlayerClass`, `setPlayerClass`, `classIds` | RPG |
| `spawnBoss(id, location)` | bosses |
| `auraStacks(entity, id)`, `removeAura(entity, id)` | auras (2.2) |

## Events

| event | when | can |
|---|---|---|
| `SkillPreCastEvent` | before a player cast, after checks | cancel, change mana cost and cooldown |
| `SkillCastEvent` | after a successful player cast | — |
| `ManaChangeEvent` | any mana change | cancel, change the value |
| `GemSocketEvent` | a gem is socketed in the workbench | cancel |
| `GemFuseEvent` | before a fusion consumes ingredients | cancel |
| `PlayerXpGainEvent` | before XP is gained | cancel, change the amount |
| `PlayerLevelUpEvent` | after each level | — |
| `PlayerClassChangeEvent` | before a class changes | cancel |

Check compatibility at runtime with `ApiVersion.isCompatible(2, 2)`.
