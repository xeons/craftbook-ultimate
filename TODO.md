# Remaining work

An inventory of what the rewrite has not covered yet. Current status and the standing rules
are in `CLAUDE.md`; bugs found in the legacy code are in `FINDINGS.md`.

## Integrated circuits

**87 chips are registered, under 104 model numbers.** 27 of the extra fork's remain, plus 4
dropped by decision.

The legacy class named in each row is the one to read for behaviour, under
`src/main/java/com/minecraftonline/` or `src/main/java/com/sk89q/craftbook/sponge/mechanics/ics/chips/`.
Look every model number and shorthand up in `ICManager` before registering it; do not invent them.

### Sensing players, mobs and items

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCM116` | MOB ABOVE? | Mob Above | `MobAbove` | 3ISO |
| `MCX116` | PLAYER ABOVE? | Player Above | `PlayerAbove` | 3ISO |
| `MCX117` | PLAYER BELOW? | Player Below | `PlayerBelow` | 3ISO |
| `MCX118` | PLAYER NEAR? | Player Near | `PlayerNear` | 3ISO |
| `MCX119` | MOB NEAR? | Mob Near | `MobNear` | 3ISO |
| `MCX133` | HELD ITEM NEAR? | Held Item Near | `HeldItemNear` | MCX133 |
| `MCX138` | ITEM NEAR? | Item Near | `ItemNear` | 3ISO |
| `MCX139` | HELD ITEM NEAR? | Held Item Near | `HeldItemNear` | MCX133 |
| `MCX140` | IN AREA | In Area | `InArea` | UISO |
| `MC1500` | PLAYER ONLINE? | Player Online? | `PlayerOnline` | 3ISO |

### Messaging and logging

Needs an audience seam in core so messages can be composed as Adventure
components and asserted in tests without a server.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MC1510` | MESSAGE PLAYER | Player Messenger | `SendMessage` | 3ISO |
| `MC1511` | MESSAGE ALL | Message All | `MessageAll` | 3ISO |
| `MCX512` | MESSAGENEARBY | Message Nearby | `MessageNearby` | 3ISO |
| `MCX513` | NAMED NEARBY | Message Named Nearby | `NamedNearby` | AISO |
| `MCX515` | SERVER LOG | Server Log | `ServerLog` | 3ISO |
| `MCX516` | S-LOG NEARBY | Server Log Nearby | `ServerLogNearby` | 3ISO |
| `MCX517` | S-LOG NEARBY+ | Server Log Nearby+ | `ServerLogNearbyPlus` | 3ISO |
| `MC2999` | MARQUEE | Marquee | `Marquee` | SI3O |
| `MC3456` | MARQUEETRANSMIT | Marquee Transmitter | `MarqueeTransmitter` | 3ISO |

### Weather illusions

These show one player different weather from another, which the legacy code did
by sending packets directly. Paper has `Player#setPlayerWeather`, so no packet work is needed.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCX235` | FALSE WEATHER | False Weather | `FalseWeather` | 3ISO |
| `MCX236` | DIST FALSE RAIN | Distance False Weather | `DistanceFalseWeather` | 3ISO |
| `MCX237` | HIDE WEATHER | Hide Weather | `HideWeather` | 3ISO |
| `MCX238` | DIST HIDE RAIN | Distance Hide Weather | `DistanceHideWeather` | 3ISO |

### Sound and music

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCU700` | MELODY | Melody | `Melody` | UISO |
| `MCU705` | TUNE | Tune | `Tune` | AISO |
| `MCU706` | JUKEBOX | Jukebox | `Jukebox` | AISO |
| `MCX251` | SOUND EFFECT | Sound Effect | `SoundEffect` | 3ISO |

### Dropped by decision

Not to be implemented.

| Model | Name |
| --- | --- |
| `MC2300` | ROM Get |
| `MC3300` | ROM Set |
| `MC5000` | Perlstone 3ISO Programmable Logic Chip |
| `MCX144` | CBWarp Area |

### Upstream-only ICs

Around 70 further model numbers exist in `E:\Code\CraftBook` that never existed in this fork,
covering the automatic crafter, container sorters and stockers, ranged collectors, the sentry
gun and others. They are optional additions rather than a compatibility obligation, so they
come after everything above.
## Mechanics

None are started. All ~74 live under `src/main/java/`, each marked with `@Module`. Those below are
unique to this fork and have no upstream equivalent to compare against, so the legacy source is the
only specification.

### Unique to this fork

| Mechanic | What it does |
| --- | --- |
| `MegaPipes` | Item routing with filters, sources and destinations. Larger than the plain `Pipes`. |
| `Footprints` | Cosmetic particles where players walk. |
| `BannerCopier`, `BookCopier`, `MapCopier` | Duplicate a held item onto blanks. Kept despite vanilla equivalents. |
| `PageReader` | Reads a book's pages aloud. |
| `LightNetherrack` | Netherrack lit by redstone. |
| `CartCraft`, `CartDelay`, `CartDirection`, `CartLaunch`, `CartLoad`, `CartPrint`, `CartWarp`, `CartCollect`, `StationClear` | Minecart mechanics with no upstream counterpart. |

### Shared with upstream

Compare both sources for these. Where they differ, this fork's behaviour is the one to keep.

`Ammeter`, `BetterPhysics`, `BetterPlants`, `Bookshelf`, `BounceBlocks`, `Chairs`, `CommandSigns`,
`CookingPot`, `Elevator`, `GlowStone`, `HeadDrops`, `HiddenSwitch`, `JackOLantern`, `LightStone`,
`LightSwitch`, `Marquee`, `Netherrack`, `PaintingSwitcher`, `Pipes`, `RedstoneJukebox`,
`SignCopier`, `Snow`, `Teleporter`, `TreeLopper`, `Variables`, `XPStorer`, the `area` mechanics
(`Bridge`, `Door`, `Gate`, `ComplexArea`), the `boat` and `minecart` sets, `DispenserRecipes`.

### Dropped by decision

`CBWarps`, `ChunkAnchor`, the Pastebin report upload, and the vendored bStats metrics.

## Infrastructure not yet built

| Piece | Why it is needed |
| --- | --- |
| Audience seam | Sending Adventure components to players near a chip. Blocks ~9 ICs, and the warning a destination should give when its name is already in use. |
| Persistence | Only the switch passwords are saved, in `switch-passwords.txt`. Everything else a chip remembers is kept on its own sign or lost when its chunk unloads. The firework display scripts in `fireworks/` are read, never written. |
| Configuration | Nothing is configurable. The legacy code had per-mechanic config with an enable flag. |
| Commands | `/craftbook` reads the catalogue and the switch commands drive `MCX120` and `MCX121`, all through Brigadier. The per-mechanic commands come with their mechanics. |
| Permissions | Every chip's permission is registered under `craftbook.ic.safe.*` or `craftbook.ic.restricted.*` and checked on creation. Nothing yet checks anything at run time. |
| Documentation generator | The legacy code generated its own IC documentation. |

## Verification

Nothing has ever been run on a server. The unit tests cover the platform-independent half only;
the listeners, the schedulers, the container lookups and the legacy block mapping have no coverage
at all. Worth checking early, in roughly this order:

1. The plugin loads and reports its catalogue.
2. Writing `[MC1000]` on a wall sign creates a chip that follows redstone.
3. A pre-flattening block name such as `35:14` resolves to red wool.
4. A bridge builds and retracts, paying into and out of a nearby chest.
5. A transmitter drives a receiver in another world, and a transporter delivers to a destination.
6. A planter sows seeds thrown at it, and a harvester gathers the crop into a chest.
7. `/craftbook ic list` reports the catalogue, and `/mcx120` throws a switch a chip follows.
8. A password set with `/mcx121pass add` still works after a restart.
9. A spawner makes what its sign describes, riders and all, and a shooter fires out of its back.
10. A potion area doses whoever walks through it without stripping what they had drunk.
11. A firework script in `fireworks/` plays, pauses and stops when it should.
12. Region behaviour on an actual Folia server.
