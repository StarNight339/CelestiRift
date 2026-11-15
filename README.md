# CelestiRift

A skill, gem and RPG framework for **Paper 26.3** (build 252+, Java 25). Write skills in YAML,
put them in gems, socket the gems into weapons, and cast them with clicks and keys. Classes,
levels, bosses, drops and 3D effects are built on the same engine, and other plugins can
extend it through a public API.

**📖 Documentation:** [English](docs/en/index.md) · [ภาษาไทย](docs/th/index.md) · [all pages](docs/README.md)

## Features

- **Skills in YAML**: 30+ mechanics (damage, heal, auras, chain lightning, summons, meteors,
  display-entity effects, particle shapes...), area targeters (nearby, cone, line, ring) with
  ally/enemy filters, formulas that scale with level, cast times and telegraphs.
- **Gems and sockets**: themed gems with generated tooltips, runes for weapons and armour,
  a socket workbench with tabs, and fusions.
- **RPG**: mana, levels and XP, classes with passives and level-gated kits, drop tables,
  and bosses with phases, telegraphed moves and loot.
- **Safe for live servers**: AoE never hits pets/NPCs/your team, PvP and WorldGuard
  respected, safe reload that keeps old content when a file is broken, validation with
  file:line, training dummies, and an automated in-server test.
- **Resource pack**: bundled models, OrionRP integration, per-gem looks.
- **With OrionRP** (optional): `<glyph:x>` icons in all text, OrionRP custom items in drops and
  skills (`giveitem`), OrionRP items/furniture/blocks that cast skills (`skill <name>` action),
  textured sprite effects (`display` `type: sprite`), menu backgrounds and a HUD mana bar -
  see `orionrp:` in config.yml.

## Quick start

1. Drop `CelestiRift-<version>.jar` into `plugins/` and start the server.
2. `/cr gem EmberGem`, hold a sword, `/cr socket`, click the gem, then right-click.
3. `/cr dummy spawn` and try your skills. Edit `plugins/CelestiRift/skills/`, then `/cr reload`.

More: [Getting started](docs/en/01-getting-started.md) · [เริ่มต้นใช้งาน](docs/th/01-getting-started.md)

---

## Building from source

Only **JDK 25** is needed; the Gradle wrapper downloads Gradle.

```bash
./gradlew build                              # jar: celestirift-core/build/libs/
./gradlew :celestirift-core:runServer        # a Paper test server with the plugin (port 25570)
./gradlew :celestirift-core:smokeTest        # automated in-server test on a fresh server
./gradlew :celestirift-core:resourcePack     # the resource pack zip + SHA-1
./gradlew deploy                             # copy the jar to deployDir (gradle.properties)
```

The build fails on deprecated Paper API (`strictDeprecation=true`), so the next Minecraft
release shows problems at compile time. Try another version with
`-PpaperVersion=26.4.build.+ -PminecraftVersion=26.4`. CI (`.github/workflows/build.yml`)
builds and runs the smoke test on every push and weekly.

| module | purpose |
|---|---|
| `celestirift-api` | public API for other plugins (bundled into the plugin jar, published separately) |
| `celestirift-core` | the Paper plugin |

### Releasing

1. Add a `## <version>` section at the top of `CHANGELOG.md`.
2. Push a tag: `git tag v2.0.0 && git push --tags`.

`.github/workflows/release.yml` builds, runs the smoke test and creates a GitHub release
(jar + resource pack). It publishes `celestirift-api` to GitHub Packages, and uploads to
**Hangar** and **Modrinth** when the secrets `HANGAR_API_TOKEN` / `MODRINTH_TOKEN` are set
(slugs and versions in `gradle.properties`).

### Project layout

```
celestirift-api/            public API: CelestiRiftApi, events, ConfigMap, Expression, NumberValue
celestirift-core/
  src/main/resources/       plugin.yml, config.yml, messages.yml, skills/, gems/, fusions/, bosses/, classes.yml, drops.yml
  src/resourcepack/         the bundled resource pack
  src/main/java/com/celestirift/
    CelestiRift.java        wiring and default-content install
    cast/                   cast pipeline (cooldown, mana, cast time), trigger dispatch
    skill/                  skill engine: loader, executor, mechanics, targeters, conditions, combat rules
    aura/                   auras (buffs, debuffs, stacks, shields)
    gem/ gui/               gems, sockets, fusions, the socket workbench and menus
    rpg/                    levels, classes, drops, bosses
    vfx/                    particle specs, display effects, damage numbers
    pack/                   resource pack + OrionRP hook
    validate/               diagnostics (file:line), generated reference and schema
    devtools/               smoke test, training dummies
docs/                       documentation (en, th)
tools/                      helper scripts (placeholder textures, docs page)
```

## License

[MIT](LICENSE) © 2026 StarNight339. `libs/orionrp-api-*.jar` is the compile-only API of the
optional OrionRP plugin and is not included in the CelestiRift jar.
