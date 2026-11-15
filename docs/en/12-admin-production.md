# 12. Running a server

[← Index](index.md) · [ภาษาไทย](../th/12-admin-production.md)

## Safe reload

`/cr reload` checks `config.yml` and `messages.yml` first, then loads all content. If any
file has a YAML syntax error, **nothing changes**: the old skills, gems, classes, bosses and
drops stay active and the broken files are listed. Fix them and reload again, or use
`/cr reload force` to load everything else without them. 3D effects are cleared on reload.

## When something goes wrong

- `/cr validate`: every problem with file:line.
- `settings.debug: true` in `config.yml` logs every cast with its result (`SUCCESS`,
  `ON_COOLDOWN`, `NOT_ENOUGH_MANA`...), every hit on a training dummy, and full stack traces.
- If one skill line throws (for example an add-on mechanic), the rest of the skill still
  runs. The error is logged at most once every 30 s per line.

## Testing skills

- `/cr dummy spawn enemy|ally|player [health]`: mannequins that never die and show damage,
  healing and DPS. Environment damage (suffocation, falls) is ignored. See
  [Targeting](04-targeting.md#testing-targets).
- `vfx.damage-numbers: true` shows floating damage/heal numbers on players' hits, bosses and dummies.
- Test with a **survival** player: creative players are never targeted.

## Player data

Mana, level, XP and class are stored on the player (persistent data). There is no database.
They survive restarts and move with world backups. Auras are not saved (they end on logout).

## Updating

- **CelestiRift:** replace the jar and restart. Your files are never overwritten. New
  example content is only written on a fresh install, so copy new examples by hand if you
  want them.
- **Back up** `plugins/CelestiRift/` before big changes. The test server keeps one in
  `backups/` (that folder is not loaded).
- **Paper:** CelestiRift needs Paper 26.3 build 252+ and logs an error on older builds.
  Before a Minecraft update, developers can run the in-server test against the new version
  (below).

## For developers

| task | command |
|---|---|
| build | `./gradlew build` (fails on deprecated Paper API) |
| test server with the plugin | `./gradlew :celestirift-core:runServer` (port 25570) |
| automated in-server test | `./gradlew :celestirift-core:smokeTest` (fresh server, casts every skill, spawns every boss, tests safe reload; fails the build on any problem) |
| another Minecraft version | add `-PminecraftVersion=26.4` / `-PpaperVersion=26.4.build.+` |
| copy into your server | `./gradlew deploy` (`deployDir` in `gradle.properties`) |
| release | add a `## <version>` section to `CHANGELOG.md`, push a tag `v<version>` |
