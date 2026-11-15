# 11. Commands and permissions

[← Index](index.md) · [ภาษาไทย](../th/11-commands-permissions.md)

## Admin: `/celestirift` (aliases `/cr`, `/skill`, `/skills`), permission `celestirift.admin`

Every argument tab-completes (skills, gems, bosses, classes, triggers, players).

### Content

| command | does |
|---|---|
| `/cr reload` | reload everything safely (a broken file keeps the old content) |
| `/cr reload force` | reload and skip broken files |
| `/cr validate` | every problem with file:line and hints |
| `/cr list` | loaded skills |
| `/cr info <skill>` | description, cooldown, mana, lines |

### Skills and items

| command | does |
|---|---|
| `/cr cast <skill> [player]` | cast a skill (no cooldown/mana) |
| `/cr give <skill> [player] [material]` | a skill item: right-click to cast |
| `/cr gem` | browse every gem (gem library menu) |
| `/cr gem <GemId> [player]` | give a defined gem |
| `/cr gem <skill> [tier] [trigger] [player]` | give an ad-hoc gem |
| `/cr shardvoid [amount] [player]` | give Shardvoid |
| `/cr socket` | the socket workbench |
| `/cr trigger` | the trigger menu for the held gem/weapon |
| `/cr fuse` | fuse gems in your inventory into a gem item |
| `/cr item` · `/cr item settrigger <skill> <trigger>` · `/cr item clear` | the held weapon's sockets |

### Players, RPG and bosses

| command | does |
|---|---|
| `/cr mana [player]` · `/cr mana set <player> <amount>` | mana |
| `/cr level set <player> <level>` | set a level |
| `/cr xp add <player> <amount>` | give XP |
| `/cr class set <player> <class\|none>` | set or remove a class |
| `/cr boss spawn <id> [player]` · `list` · `killall` | bosses |
| `/cr dummy spawn [enemy\|ally\|player] [health]` · `/cr dummy clear` | training dummies |
| `/cr pack` · `/cr pack send [player]` | resource pack status / resend |

## Player commands

| command | permission | default |
|---|---|---|
| `/class`, `/class info <id>`, `/class choose <id>` | `celestirift.class` | everyone |
| `/profile` | `celestirift.profile` | everyone |
| `/profile <player>` | `celestirift.profile.others` | op |
| casting skill items | `celestirift.use` | everyone |

Classes can require their own permission (`permission:` in `classes.yml`).
