# JSON export format and implementation notes

This document records the rules used by COC-LIO. The in-game JSON is treated as a point-in-time snapshot, not a live connection to the village.

## Verified top-level fields

- `tag`: Player Tag, expected to start with `#`.
- `timestamp`: Unix timestamp for the export. COC-LIO accepts both seconds and millisecond timestamps and normalizes milliseconds to seconds.
- `buildings`, `traps`, `units`, `siege_machines`, `heroes`, `spells`, `pets`, `equipment`, `helpers`: Home Village groups.
- Groups with a `2` suffix, such as `buildings2`, `traps2`, `units2`, and `heroes2`: Builder Base groups.

## Entry fields

- `data`: game data identifier.
- `lvl`: current level in the exported snapshot.
- `cnt`: number of idle copies at that level.
- `timer`: seconds remaining for one instance currently upgrading. Its expected finish moment is `timestamp + timer`. COC-LIO keeps this field in seconds exactly as exported by the game.
- `helper_cooldown`: helper cooldown information; it is not a building upgrade timer.
- `gear_up`: flag used for a defence gear-up. A row with both `gear_up` and `timer` is a Master Builder gear-up and must not be reported as a normal level upgrade.
- `types` and nested `modules`: crafted-defence/module details; each module carries its own ID and level.

A row with `timer` and no `cnt` is interpreted as one running instance. A row with `cnt` is interpreted as idle instances. The application preserves the raw export and keeps unknown IDs instead of silently dropping them.

## Comparison rules

1. The first import for a tag creates a baseline; there is no earlier state to compare.
2. Later imports for the same tag are compared with the newest earlier snapshot.
3. A timer appearing where no matching timer existed is a possible upgrade start.
4. A timer disappearing alone is not enough to claim completion. COC-LIO looks for the corresponding next-level idle entry before marking completion.
5. Gear-ups are displayed separately and excluded from normal upgrade detection.
6. The countdown uses the export's timestamp plus the timer. When it reaches zero, a fresh game export is still required to confirm the new state.

## Known limits

- The export does not contain current Gold, Elixir, Dark Elixir or Gem balances.
- It does not provide a live online/offline status.
- It does not update itself after the player returns to the game; a new export must be imported.
- Game updates may add identifiers, levels or fields. Unknown identifiers remain visible as numeric IDs until the mapping is verified.
- Supercharge has special tier semantics beyond ordinary level upgrades. COC-LIO currently retains the source JSON but does not yet claim to interpret every Supercharge field. This must be validated against current real exports before it is used in completion detection.
- The export is a snapshot. A book, potion, snack, helper, Clock Tower boost, Gold Pass boost, or other game action after export can make the estimate differ from the current in-game timer. Import a fresh JSON after those actions. The game timer is the reference.

## References

- ClashClock, How ClashClock Works: https://clashclock.com/guides/how-it-works
- ClashClock, Import Your Village: https://clashclock.com/guides/user-guide/import-your-village
- ClashClock, When a Timer Looks Wrong: https://clashclock.com/guides/user-guide/fixing-a-wrong-timer
- Community mapping of JSON export data IDs: https://gist.github.com/rahulkhatri137/a8449943df45100c5f1e1359cd9ec67a
- Game data and images dataset (MIT package): https://github.com/chiefpansancolt/clash-of-clans-data

These are independent community references, not official Supercell API documentation. Their mappings can lag a game release or contain uncertain labels. Where the current JSON differs, the exported data is retained and should be checked against a fresh in-game export.
