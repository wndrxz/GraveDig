# Changelog

## 0.2.1 — 2026-07-18

Nobody should get my test bench as their server settings. The shipped
defaults were still the quick-testing ones: graves gone in half an
hour, two brush clicks per portion.

- expire-minutes 30 → 1440: a grave now survives a day, not a coffee
  break. it still ticks while the owner is offline, the config warns
  about that now
- protect-minutes 5 → 15
- clicks-per-portion 2 → 5, close to how vanilla brushing feels
- code fallbacks match the shipped file, and the readme no longer
  claims 0.1.2

Existing servers keep their config.yml, this only changes what new
installs get.

## 0.2.0 — 2026-07-17

Cleared the known-issues list down to plugin-added drops.

- pistons can no longer push or pull grave blocks — the whole move is
  vetoed, easier than chasing the block around and rekeying the grave
- dying twice on the same block no longer eats the first grave: the old
  loot pops out on the ground when the new grave takes the spot

## 0.1.2 — 2026-07-16

Folia pass came back clean, just two rough edges from the field notes.

- dying on grass/flowers/snow no longer perches the grave on top of the
  plant — replaceable blocks get swapped for the grave like air would
  (vanilla `replaceable` tag, so the list stays in sync with the game)
- breaking a grave now says so in chat instead of silently spilling

## 0.1.1 — 2026-07-16

First live-server pass found the obvious thing: suspicious blocks have
gravity. Falling graves left the plugin pointing at empty air.

- grave blocks are placed without physics and are stopped from falling
  (support gone, whatever) — no more graves wandering off their key
- mid-air deaths: the spot scan now sinks to the ground instead of
  planting the block in the sky, where it fell apart instantly
- orphan sweep: if a tracked grave's block is gone (fell on an older
  version, /setblock over it...), the loot is spilled at the old spot
  and the entry cleaned up, instead of haunting /gravedig list forever

## 0.1.0 — 2026-07-15

First playable cut. Everything below is new because everything is new.

- death leaves a suspicious sand/gravel block at the death spot
  (auto-picked by the ground, or pinned in config)
- brush + right-click digs it out in portions: armor (+offhand) →
  hotbar → the rest → xp, N clicks per portion
- vanilla brushing is cancelled — our own progress on top of interact
  events, no loot tables involved
- owner-only protection window, then the grave goes public (owner gets
  a chat heads-up); expiry after a configurable lifetime
- breaking the block dumps everything at once (toggleable); explosions
  can't touch graves
- clickable death coordinates in chat (click = copy), MiniMessage
- `/gravedig list` and `/gravedig reload` (`/gd`)
- graves survive restarts via state.yml, bad entries are skipped
  one-by-one instead of nuking the file
- folia-ready scheduling: region scheduler for block edits, async
  scheduler for saves, global tick for the expiry sweep
- en + ru locales with english fallback and a parity test
- tests: portion splitter, xp math, locale parity

Known gaps (honest list): no live-server pass yet, folia untested in
practice, pistons can move the block, items injected into drops by
other plugins get eaten.
