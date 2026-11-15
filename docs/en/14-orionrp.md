# 14. OrionRP integration

[← Index](index.md) · [ภาษาไทย](../th/14-orionrp.md)

OrionRP is an optional resource-pack plugin (ItemsAdder-style: custom
items, glyphs, GUIs, a HUD, and pack hosting). With **OrionRP 0.1.0 or newer** installed,
CelestiRift uses it automatically. Without it, or with an older OrionRP, nothing changes:
the console says why and CelestiRift runs on its own.

On start you see:

```
[CelestiRift] OrionRP found: glyphs, OrionRP items, the 'skill' action, GUI backgrounds and the HUD are available.
```

## Resource pack

CelestiRift's pack is merged into OrionRP's pack and sent by OrionRP. No URL to set. See
[Resource pack](10-resource-pack.md).

## Glyphs in text

Any image in OrionRP's `textures/glyph/<name>.png` can be used as `<glyph:name>` in:
`messages.yml`, gem `name`/`lore`, skill `description`, `message` lines, summon names and
`display` text.

```yaml
EmberGem:
  name: "<glyph:fire> <gradient:#ff3d00:#ffb300><bold>Ember Gem</bold></gradient>"
```

## OrionRP items in skills and drops

```yaml
# drops.yml or a boss's loot:
drops:
- { item: demo:ruby, amount: 2, chance: 0.1 }

# a skill line: give each targeted player an item
- giveitem: { item: demo:ruby, amount: 1 }

# a 3D effect with an OrionRP item's model
- display: { type: item, material: demo:fire_orb, spin: 20, duration: 30 }
```

A wrong item id is logged once (`see /orp items`). `/cr validate` warns when `item:` drops
are used without OrionRP.

## OrionRP items, furniture, blocks and GUIs that cast skills

CelestiRift adds the OrionRP action `skill <name>`. The cast is a normal player cast: mana,
cooldown and conditions apply.

```yaml
# plugins/OrionRP/contents/demo/items/celestirift.yml
fire_wand:
  name: "<gold>Fire Wand"
  actions:
    right-click: ["skill Fireball"]
fire_table:
  furniture:
    interact: ["skill Fireball", "message <red>Hot!"]
```

The same action works on GUI buttons (`gui/*.yml`).

## Textured effects (sprites)

`display` with `type: sprite` shows glyph images as a flat, full-bright picture that faces the
player. Several `frames` play as an animation:

```yaml
- display: { type: sprite, frames: "boom_1,boom_2,boom_3,boom_4", frametime: 2, scale: 4, duration: 8 }
  target: origin
- display: { type: sprite, glyph: magic_circle, tilt: -90, scale: 6, spin: 6, duration: 40 }   # flat on the ground
  target: casterLocation
```

Put the images in `textures/glyph/` of any OrionRP content folder and run `/orp rebuild`.

## Menus with image backgrounds

```yaml
# config.yml
orionrp:
  gui-backgrounds:
    gem-library: "celestirift:gem_library"   # textures/gui/gem_library.png
    socket: ""                               # empty = plain menu
    trigger: ""
```

176×222 pixels covers the 6-row menus.

## HUD: action bar and mana bar

OrionRP's HUD shows several action-bar layers at once. With it, CelestiRift's cast messages
(cooldown, no mana, casting bar), training-dummy reports and the mana bar no longer overwrite
each other or other plugins' HUD layers.

```yaml
orionrp:
  hud:
    enabled: true
    message-ticks: 40       # how long a message stays
    mana-glyphs:            # a bar of icons when these glyphs exist
      full: "mana_full"
      half: "mana_half"
      empty: "mana_empty"
    mana-icons: 10
    # mana-x: -91           # pixels from the screen centre to the bar's left edge
```

Without the three mana glyphs the mana bar stays text (`mana.actionbar` in `messages.yml`).

## Updating

Update **both** jars together and restart. A new CelestiRift with an old OrionRP logs
"OrionRP is installed but its API could not be used - update OrionRP" and runs without it.
