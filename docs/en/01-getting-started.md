# 1. Getting started

[← Index](index.md) · [ภาษาไทย](../th/01-getting-started.md)

## Install

1. Use **Paper 26.3, build 252 or newer**, on **Java 25**. On an older build CelestiRift logs
   an error at startup. Update `paper.jar`.
2. Put `CelestiRift-<version>.jar` in `plugins/` and start the server.
3. (Optional) Install **OrionRP** to host and send the resource pack automatically. See
   [Resource pack](10-resource-pack.md).

## The plugin folder

On the first start CelestiRift writes this layout to `plugins/CelestiRift/`:

```
config.yml          settings: mana, combat/PvP, gems, resource pack, levels, effects
messages.yml        every player-facing message (MiniMessage)
skills/             all skills, any sub-folder (see "Writing skills")
  _guide.yml          layout, naming rules, colour palettes (no skills inside)
  shared/effects.yml  Fx_* reusable impact effects
  gems/               the skill of each gem
  classes/            class kits and passives (mage, warrior, cleric)
  bosses/             boss moves
  showcase/           examples to learn from
gems.yml            gem field reference          gems/      gems, one theme per file
fusions.yml         fusion field reference       fusions/   recipes, one per file
bosses.yml          boss field reference         bosses/    bosses, one per file
classes.yml         classes                      drops.yml  mob drops and XP
docs/reference.md   generated attribute reference (rewritten on every reload)
schema/             JSON schema for editor autocomplete
pack/               the bundled resource pack zip
```

The examples are written **only on a fresh install**, when the folders are still empty, so
an update never duplicates or overwrites your files. To get a new example, copy it from the
jar or from this repository.

## First steps in game

```
/cr gem EmberGem                 give a gem (or /cr gem to browse all gems)
/cr socket                       open the socket workbench with the item in your hand
/cr dummy spawn                  a training dummy that shows damage and DPS
/class                           list classes; /class choose Mage
/cr boss spawn FrostLord         spawn the example boss
/cr reload                       apply your edits
/cr validate                     list every problem in your files, with file:line
```

## Editing comfortably

Use VS Code with the **YAML** extension (Red Hat). The bundled files start with a
`# yaml-language-server: $schema=...` line, which gives you autocomplete and hover help for
every mechanic and attribute. Keep that line in your own files. In a sub-folder, the path
needs one more `../`.
