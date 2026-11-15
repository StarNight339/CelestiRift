# Changelog

The newest `## ` section is used as the release notes on Hangar and Modrinth.

## 2.0.0

Full rework for **Paper 26.3** (build 252 or newer, Java 25). Back up `plugins/CelestiRift`
before updating. Existing skills, gems and fusions keep working.

### Platform
- Built with Gradle; the build fails on deprecated Paper API, so the next Minecraft release
  (26.4) shows problems at compile time. CI checks the pinned and the latest Paper build.
- Items use Paper data components (`ItemStack.of`, `ITEM_MODEL`, `TOOLTIP_STYLE`).
- Commands use Brigadier: `/cr` (aliases `/skill`, `/skills`) with tab-completion for skills,
  gems, bosses and classes.
- Logs a clear error at startup on Paper builds older than 252.

### Content (YAML v2)
- Skills can use a structured block form next to the classic one-line form, plus `extends:`
  and `${variables}`.
- Every content type loads from a main file and a folder (`skills/**`, `gems/**`, `bosses/**`
  and so on); sub-folders and `.yaml` files work.
- `/cr validate` and every reload list problems with `file:line` and "did you mean" hints.
- `docs/` and `schema/` are generated in the plugin folder for VS Code autocomplete.

### Combat safety and formulas
- `nearby` hits only enemies by default. It never hits pets, summons, villagers, armor
  stands, NPCs or creative players. Use `filter` (enemies/allies/players/mobs/all), `limit`
  and `sort` (nearest/furthest/random/lowesthp).
- `combat.pvp` and `combat.pvp-multiplier` in `config.yml`. Damage still respects
  WorldGuard and claim plugins.
- `/cr dummy spawn [enemy|ally|player]`: Mannequin training dummies that never die and show
  damage, healing and DPS. Decorative mannequins are never hit by skills.
- Auras: `aura` / `removeaura` mechanics and the `hasaura` condition. They add buffs and
  debuffs with stacks, damage-dealt/taken and speed modifiers, shields, damage over time
  (`ontick`), and `onhit`/`ondamaged`/`onend` skills. See `skills/auras.yml`.
- New triggers `KILL`, `CRIT`, `PROJECTILE_HIT`, `LOW_HEALTH` and `TIMER`, for gems, bindings
  and the new class `passives:` (classes.yml, with `chance`, `every` and `min-level`).
- New targeters `cone`, `line` and `ring`.
- `cast-time` / `cast-interrupt` on skills: casting with a progress bar, interrupted by
  moving or damage. API `CastResult.CASTING` and `accepted()`.
- `telegraph` mechanic: a warning circle fills up, then the attack hits from it. The Frost Lord
  nova is now telegraphed.
- Hotbar cooldown sweep on skill items. Key-press triggers say why they didn't cast.
- Training dummies ignore environmental damage (suffocation, fall...).
- 3D effects: `display` (block/item/text, spin, grow, rise, follow, glow, custom models),
  `meteor` (falling block + onland) and `chain` (chain lightning with decay). Floating
  damage and heal numbers (`vfx.damage-numbers`). See `skills/display_vfx.yml`.
- Class kits: 4 level-gated skills each for Mage, Warrior and Cleric (`skills/classes/`).
- Default skills reorganised into `shared/`, `gems/`, `classes/`, `bosses/` and `showcase/` with
  naming rules (`skills/_guide.yml`) and reworked visuals: colour palettes, a shared `Fx_*`
  effects library, and telegraphed boss moves. Defaults install only on a fresh server.
- Gems: one theme per file in `gems/`, generated tooltips (skill description, trigger, mana,
  cooldown, tier stars, flavour), new Storm, Blade and Starfall gems, and Dark Gem now casts
  ShadowBolt.
- Fusions: one recipe per file in `fusions/`, named result gems (Blackfire, Blizzard, Radiant)
  and two new recipes (Frost + Storm, Judgement + Blade).
- New socket workbench (/cr socket): shows the weapon's sockets (right-click to take a gem
  out, `gems.allow-unsocket`), separate Skill Gems / Runes / Fusion tabs listing what you
  own, click-to-socket from your inventory, a Shardvoid button and paging.
- Formulas in damage, heal, potion, ignite and projectile damage, e.g.
  `"6 + <caster.level> * 0.5"`, evaluated on every cast. API: `ConfigMap#getNumber`,
  `ConfigMap#getChoice`.

### Resource pack
- Gem tiers, runes, Shardvoid and skill scrolls have custom models and tooltip styles.
- Per-gem and per-tier looks (`resource-pack.gem-models`, `tier-models`), e.g. models from
  OrionRP's assets folder. Only applied while the pack is on, so no missing textures.
- The plugin ships the pack, can host it itself, and sends it to players on join.
- With **OrionRP** installed, the pack is merged into OrionRP's server pack and sent by it:
  no `send.url` needed and models turn on automatically (`resource-pack.use-orionrp`).

### OrionRP integration (optional, `orionrp:` in config.yml)
- `<glyph:name>` (OrionRP emoji/icons) in messages.yml, gem names and lore, skill messages and
  descriptions, summon names and display text.
- OrionRP custom items: `{ item: demo:ruby, amount: 2, chance: 0.1 }` in drops.yml, the
  `giveitem{item=demo:ruby;amount=1}` mechanic, and `display` with `type: item, item: demo:ruby`.
- OrionRP items, furniture, blocks and GUI buttons can cast skills with the action
  `skill <name>` (mana, cooldown and conditions apply).
- Textured effects: `display` with `type: sprite` shows OrionRP glyph images facing the player,
  animated with `frames: "a,b,c"` / `frametime`, or flat on the ground with `tilt: -90`.
- Image backgrounds for the Gem Library, Socket and Trigger menus (`orionrp.gui-backgrounds`).
- Action bar through OrionRP's HUD (`orionrp.hud`): cast messages and the mana bar don't
  overwrite each other; mana can be a bar of icons (`mana_full` / `mana_half` / `mana_empty` glyphs).
- Without OrionRP nothing changes.

### RPG
- Levels and XP (configurable curve) and classes (`classes.yml`) with mana, regen, socket
  bonuses and starting gems.
- Drop tables (`drops.yml`) for gems, Shardvoid and XP.
- Bosses: one file per boss in `bosses/`, with boss bar, timer, damage, phase and death skills,
  and their own `loot:`. Bosses survive restarts and chunk unloads.
- Level-ups and XP show on the action bar (no titles).

### Production
- Safe reload: if a YAML file has a syntax error, `/cr reload` keeps the previous content and
  lists the broken files. `/cr reload force` loads everything else anyway.
- A skill line that throws (for example an add-on mechanic) no longer stops the rest of the
  skill. The error is logged at most every 30 s per line.
- `settings.debug: true` logs every cast result and full stack traces.
- Player mana, level, XP and class are stored on the player and survive restarts.

### Documentation
- New docs in `docs/en` and `docs/th` (13 chapters each, plus the generated attribute
  reference). The README is now an overview. `node tools/build-docs-page.mjs` builds a
  single-page site from the same Markdown.

### API (`celestirift-api` 2.2)
- Register mechanics, targeters and conditions; cast skills; read and change mana, cooldowns,
  level, XP and class; spawn bosses.
- Events: `SkillPreCastEvent`, `SkillCastEvent`, `ManaChangeEvent`, `GemSocketEvent`,
  `GemFuseEvent`, `PlayerXpGainEvent`, `PlayerLevelUpEvent`, `PlayerClassChangeEvent`.
