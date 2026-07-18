# GraveDig

Paper plugin. When you die, a suspicious sand/gravel block appears where
you fell. Dig it out with a brush and you get your stuff back in
portions: armor first, then hotbar, then the rest, then XP.

No GUIs and no holograms on purpose. One block, a brush, clickable
coords in chat.

Status: 0.2.1. Live-tested on Paper and Folia 1.20.4: death, digging,
protection, expiry, restarts. Rough edges left are in Known issues.

## Requirements

- Paper 1.20.4+ (brushes and suspicious blocks appeared in 1.20)
- Java 21
- Folia: supported and tested on 1.20.4, scheduling goes through the
  region/async schedulers
- no Spigot (Adventure + MiniMessage)

## Building

```
./gradlew build
```

Jar lands in `build/libs/`. If there's no JDK 21 around, gradle
downloads one itself. First boot creates `plugins/GraveDig/` with the
config and lang files.

## How it works

- death → grave block at the death spot. Sand on sandy ground, gravel
  otherwise, or pin one in config. Coords arrive in chat, click to copy.
- first 15 minutes (configurable) the grave is owner-only, then it goes
  public and the owner gets a warning message
- right-click with a brush. Every 5 clicks a portion drops out:
  armor+offhand → hotbar → everything else → xp
- after the last portion the block disappears. After a day
  (configurable) the grave expires
- breaking the block dumps everything at once (can be disabled),
  explosions skip grave blocks
- vanilla brushing is cancelled — it runs loot tables and would eat the
  block, so the plugin tracks dig progress itself

## Config

Everything sits in `config.yml`, every mechanic can be switched off.
`grave.protect-minutes` / `grave.expire-minutes` (0 disables either),
`grave.block` (auto or pinned), `dig.clicks-per-portion`, `dig.effects`,
`xp.percent`, `grave.break-drops-all`, `grave.drop-on-expire`,
`messages.death-coords`, `locale` (auto/en/ru).

Locales are MiniMessage, `lang/en.yml` + `lang/ru.yml`, copied to the
data folder on first run. Missing keys fall back to english.
`state.yml` stores graves between restarts, don't edit it.

## Commands

- `/gravedig list` (or `/gd list`) — your graves and what's left in them
- `/gravedig reload` — reload config and locales, needs `gravedig.admin`

## Tests

```
./gradlew test
```

Portion splitting, the xp curve (checked against wiki values) and
en/ru locale parity.

## Known issues

- items that other plugins add to death drops are lost

## License

MIT.
