# Changelog

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
