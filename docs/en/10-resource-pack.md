# 10. Resource pack

[← Index](index.md) · [ภาษาไทย](../th/10-resource-pack.md)

CelestiRift ships a small pack: a model per gem tier, runes, Shardvoid, a skill scroll for
skill items, and a gold legendary tooltip frame for tier-5 gems. The art is placeholder art.
Replace it with your own (below).

On every start the pack is written to `plugins/CelestiRift/pack/CelestiRift-pack.zip`.
`/cr pack` shows its SHA-1 and the current settings.

## With OrionRP (recommended)

When **OrionRP** is installed, CelestiRift adds its pack to OrionRP's merged pack, and
OrionRP hosts and sends it. Nothing else to set up:

```yaml
resource-pack:
  enabled: true
  use-orionrp: true
  send: { url: "", hash: "" }   # unused with OrionRP
```

The console says `Resource pack is merged and sent by OrionRP.` Everything else OrionRP adds
(glyphs, items, skill actions, sprites, menu backgrounds, HUD): [OrionRP](14-orionrp.md).

## Without OrionRP

1. Upload the zip somewhere players can download it directly, or merge its `assets/` into
   your own server pack.
2. Set `resource-pack.send.url` (and `hash` if you changed the zip), then `enabled: true`.
3. `/cr reload`. Players get it on join; `/cr pack send [player]` resends it.

Keep `enabled: false` until players actually receive the pack, or items show the missing
(purple/black) texture.

## Gem looks

| setting | used for |
|---|---|
| `resource-pack.models.gem` | the default gem model per tier (`{tier}` → 1-5) |
| `resource-pack.tier-models` | override per tier: `{ 1: "ns:model", 5: "ns:model" }` |
| `resource-pack.gem-models` | per gem id: `{ EmberGem: "ns:model" }` |
| `resource-pack.models.rune` / `shardvoid` / `skill-item` | those items |
| `resource-pack.tooltip-styles` | tooltip sprite per tier (5 = `celestirift:legendary`) |

`tier-models` and `gem-models` apply **only while the pack is on**, so players without it
never see missing textures. A `model:` written in a gem's own definition applies always.

### Example: art from OrionRP's assets

Put a texture set in `plugins/OrionRP/assets/<namespace>/` (for example `items:item559`)
and point gems at it:

```yaml
resource-pack:
  tier-models: { 1: "items:item547", 2: "items:item542", 3: "items:item536", 4: "items:item551", 5: "items:item554" }
  models: { rune: "items:item558", shardvoid: "items:item561" }
  gem-models:
    EmberGem: "items:item559"
    FrostGem: "items:item557"
```

Check the licence of any art before you publish it inside a plugin. Keeping it in your
server's OrionRP folder keeps it out of the plugin jar.

## Building the pack (developers)

`./gradlew :celestirift-core:resourcePack` → `celestirift-core/build/resourcepack/CelestiRift-pack.zip`
(+ `.sha1`). The pack declares formats 84-97 (Minecraft 26.1-26.3). Raise `max_format` in
`pack.mcmeta` for newer clients.
