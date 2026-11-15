# CelestiRift documentation

CelestiRift is a skill, gem and RPG framework for Paper. You write skills in YAML, put
them in gems, socket the gems into weapons, and players cast them with clicks and keys.
Classes, levels, bosses, drops and 3D effects all build on the same skill engine.

**Requires:** Paper 26.3 (build 252 or newer), Java 25.
**Language:** [English](index.md) · [ภาษาไทย](../th/index.md)

## What you can build

- **Skills** from mechanics (damage, heal, auras, projectiles, summons, telegraphs...),
  targeters (nearby, cone, line, ring...) and conditions, with formulas that scale with level.
- **Gems** that carry skills, socketed into weapons. **Runes** that boost a weapon or armour.
  **Fusions** that combine gems into stronger ones.
- **Classes** with mana, passives and level-gated skill kits. **Levels and XP**, and **drops**.
- **Bosses** with boss bars, phases, timed and telegraphed moves, and their own loot.
- **Effects**: particle shapes, display-entity 3D effects (falling swords, meteors, ice
  spikes), floating damage numbers, and resource-pack models.

## Pages

| # | Page | For |
|---|---|---|
| 1 | [Getting started](01-getting-started.md) | install, first run, the plugin folder |
| 2 | [Writing skills](02-writing-skills.md) | the YAML format, folders, naming, formulas, validation |
| 3 | [Mechanics](03-mechanics.md) | everything a skill line can do |
| 4 | [Targeting](04-targeting.md) | who a skill hits, shapes, combat safety, PvP |
| 5 | [Auras and casting](05-auras-casting.md) | buffs/debuffs/stacks, cast time, telegraphs |
| 6 | [Gems and sockets](06-gems-sockets.md) | gems, runes, the socket workbench, triggers |
| 7 | [Fusions](07-fusions.md) | combining gems |
| 8 | [RPG](08-rpg.md) | mana, levels, classes, passives, kits, drops |
| 9 | [Bosses](09-bosses.md) | boss files, phases, loot |
| 10 | [Resource pack](10-resource-pack.md) | models, OrionRP, gem looks |
| 11 | [Commands and permissions](11-commands-permissions.md) | every command |
| 12 | [Running a server](12-admin-production.md) | reload, debug, dummies, testing, upgrades |
| 13 | [API](13-api.md) | extending CelestiRift from another plugin |
| 14 | [OrionRP](14-orionrp.md) | glyphs, custom items, skill actions, sprites, menu backgrounds, HUD |
| — | [Attribute reference](reference.md) | every mechanic/targeter/condition attribute (generated) |

## Five-minute tour

1. Drop the jar in `plugins/` and start the server. CelestiRift writes example skills, gems,
   classes and a boss.
2. `/cr gem EmberGem` gives you a gem. Hold a sword and run `/cr socket`. Click the gem.
3. Right-click with the sword: a Fireball. The gem's tooltip shows its trigger, mana and cooldown.
4. `/cr dummy spawn` puts a training dummy in front of you. Hit it with skills to see damage and DPS.
5. Open `plugins/CelestiRift/skills/gems/fireball.yml`, change the damage, run `/cr reload`.
