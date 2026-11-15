# 7. Fusions

[← Index](index.md) · [ภาษาไทย](../th/07-fusions.md)

A fusion consumes two or more gems and sockets a stronger one into the weapon.

## In game

`/cr socket` → **Fusion** tab. Every recipe is listed with ✔ (you have it) or ✘ (missing)
for each ingredient. Click a recipe you can make: one of each ingredient is taken from your
inventory and the result is socketed into the weapon.

`/cr fuse` is the command version. It fuses gems in your inventory into a result **gem item**
instead of socketing it.

## Bundled recipes

| recipe | ingredients | result | skill |
|---|---|---|---|
| Blackfire | Ember Gem + Dark Gem | Blackfire Gem | a soul-fire orb that collapses into a singularity, then bursts |
| Blizzard | Frost Gem + Storm Gem | Blizzard Gem | a storm cell: 4 pulses of ice and chain lightning in a warned circle |
| Radiant | Gem of Judgement + Blade Gem | Radiant Gem | a colossal golden sword falls on the target |

## Recipe files

```
fusions.yml            field reference (recipes can be written here too)
fusions/blackfire.yml  one recipe per file
fusions/blizzard.yml
fusions/radiant.yml
```

```yaml
# fusions/blizzard.yml
Blizzard:
  ingredients: [FrostGem, StormGem]
  result-gem: BlizzardGem
```

| key | meaning |
|---|---|
| `ingredients` | 2+ gem ids, one of each is consumed (list one twice to need two) |
| `result-gem` | a gem id to socket (its own name, look and lore) |
| `skill` | instead of `result-gem`: an automatic gem for this skill |
| `trigger`, `tier` | for `skill` results only (defaults `RIGHT_CLICK`, 5) |

Result gems are normal gems in `gems/fusion.yml`, so `/cr gem BlizzardGem` also gives one.
Give them `tier: 5` for the legendary tooltip. Plugins can cancel a fusion with
`GemFuseEvent` ([API](13-api.md)).
