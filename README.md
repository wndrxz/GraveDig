# GraveDig

You die, your stuff doesn't scatter into a lava lake. Instead a block of
suspicious sand (or gravel, depending on the ground) appears where you
fell. Come back with a brush and dig it out — in portions: armor first,
then your hotbar, then the rest, and finally your XP.

No GUIs, no holograms, no armor stands pretending to be corpses. One
block, one brush, clickable coordinates in chat. That's it.

This is **0.1.0** — the first playable cut. The core loop works: death →
grave block → brush it out → grave disappears. Owner protection, expiry,
persistence across restarts and the en/ru locales are all in. The
roadmap below is honest about what isn't.

## What you need

- **Paper 1.20.4+.** Brushes and suspicious blocks came in 1.20, we
  target the 1.20.4 API. Public Paper API only, no NMS.
- **Folia-supported from the first commit.** Block edits go through the
  region scheduler, file I/O through the async scheduler. (Tested on
  Paper; Folia testing is on the roadmap, the wiring is there.)
- **Java 21.**
- **Spigot is not supported.** We lean on Adventure and MiniMessage.

## Building

```
./gradlew build
```

You'll find `GraveDig-0.1.0.jar` in `build/libs/`. Drop it into
`plugins/`, restart, done. First boot creates `plugins/GraveDig/` with
the config and language files. No JDK 21 on the machine? The build
fetches one itself (foojay resolver), just let it download.

## Tests

```
./gradlew test
```

Three small suites. `PortionSplitterTest` covers the death-inventory
split (armor + offhand / hotbar / rest, air and empty slots skipped).
`XpMathTest` pins the vanilla XP curve against reference points from
the wiki. `LocaleParityTest` keeps `en.yml` and `ru.yml` key-for-key in
sync and pins every key the code references by string literal.

## How it actually plays

1. Die somewhere inconvenient, as usual.
2. A suspicious sand or gravel block appears at your death spot — sand
   over sandy ground, gravel everywhere else. Coordinates land in your
   chat, yellow and clickable (click copies them).
3. For the first N minutes (default 5) only you can dig or break the
   block. After that it goes public — you get a heads-up in chat.
4. Come back with a **brush** and right-click the block. Every couple
   of clicks a portion pops out: **armor → hotbar → the rest → XP**.
5. Last portion out — the block vanishes. Dawdle past M minutes
   (default 30) and the grave expires instead.

Breaking the block instead of brushing it dumps everything out at once
(toggleable). Explosions refuse to touch graves. Vanilla brushing with
its loot tables is cancelled outright — the dig progress is ours, so the
block can't get eaten by a bad loot roll.

## Config tour

Everything lives in `plugins/GraveDig/config.yml` and every mechanic is
toggleable. The knobs:

- **`locale`** — `auto`, `en` or `ru`. `auto` follows the JVM language
  and falls back to English.
- **`grave.enabled`** — the master switch. Off = vanilla deaths.
- **`grave.block`** — `auto` picks sand/gravel by the ground under the
  death spot; or pin `SUSPICIOUS_SAND` / `SUSPICIOUS_GRAVEL`.
- **`grave.protect-minutes`** — owner-only window. `0` = public at once.
- **`grave.expire-minutes`** — grave lifetime. `0` = graves never expire.
- **`grave.drop-on-expire`** — spill contents on expiry, or eat them.
- **`grave.break-drops-all`** — whether breaking the block is allowed
  as a "give me everything now" shortcut.
- **`dig.clicks-per-portion`** — brush clicks per portion, default 2.
- **`dig.effects`** — brushing sounds + particles.
- **`xp.enabled`** / **`xp.percent`** — how much XP the grave stores,
  as a share of what you carried (default all of it).
- **`messages.death-coords`** — the clickable coordinates line.

`lang/en.yml` and `lang/ru.yml` are MiniMessage, copied to the data
folder on first boot; your edits win, missing keys fall back to English.
`state.yml` is machine-written, don't edit it by hand.

## Commands & permissions

- **`/gravedig list`** (alias `/gd list`) — your graves, with clickable
  coordinates and portions left. Permission `gravedig.use`, default on.
- **`/gravedig reload`** — reloads config and locales on the fly.
  Existing graves are left alone. Permission `gravedig.admin`, default op.

## Things to know before going live

Honest list of sharp edges, because surprises in production are no fun.

- **This is 0.1.0 and it hasn't met a real server yet.** The build is
  green and the logic is tested, but the first live death is still ahead.
  Treat it as a prototype.
- **Folia wiring is in, Folia testing isn't.** Region scheduler for
  block edits, async scheduler for saves, global tick for the expiry
  sweep — all per the book, none of it battle-tested yet.
- **Pistons can shove the grave block.** Explosions are handled, pistons
  are a known TODO.
- **Items other plugins inject into death drops get eaten** when we
  capture the inventory ourselves. Matching them up is on the list.
- **One grave per block position.** Die twice on the exact same block
  and the newer grave wins the map slot.
- **state.yml is written on every grave change** (async, serialized on
  the owning thread). A hard crash can lose the newest change, a clean
  stop never does.

## Roadmap

Each step ships as a self-contained, toggleable piece. Fixes first,
features when the previous cut is solid.

- **0.2** — first live-server pass: verify sounds/particles on a real
  1.20.4, a proper Folia run, piston protection.
- **0.3** — configurable portion order, protection bypass permission,
  play-nice mode for items injected by other plugins.
- **later** — whatever actually hurts after people play with it.

## License

MIT. Use it, fork it, ship it on your server, don't take credit for
writing it. Standard internet manners.

---

Built because every grave plugin I've tried was a hologram circus over
an armor stand. This one tries to feel like vanilla archaeology —
because that's literally what it is.
