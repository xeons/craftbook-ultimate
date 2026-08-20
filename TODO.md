# Remaining work

An inventory of what the rewrite has not covered yet. Current status and the standing rules
are in `CLAUDE.md`; bugs found in the legacy code are in `FINDINGS.md`.

## Integrated circuits

**114 chips are registered, under 143 model numbers.** The extra fork's catalogue is complete;
4 model numbers were dropped by decision.

Nothing from this fork's catalogue is left to port. Anything added from here comes from upstream,
and the rule still holds: look every model number and shorthand up in the legacy `ICManager` before
registering it, and do not invent them.

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

The minecart ones are done. The remaining ~58 live under `src/main/java/`, each marked with
`@Module`. Those below are unique to this fork and have no upstream equivalent to compare against,
so the legacy source is the only specification.

### Minecart mechanics: done

`Station`, `StationClear`, `CartSort`, `CartLift`, `CartLaunch`, `CartDelay`, `CartLoad`,
`CartDirection`, `CartBooster`, `CartCollect`, `CartDeposit`, `CartCraft`, `CartPrint` and
`CartDispenser`, with the `/station`, `/cbgo` and `/cbrecipes` commands.

`CartWarp` is **not** ported: it teleports a cart to a CBWarp, and CBWarps is dropped by decision.
Porting it needs a decision about what it should warp to instead.

### Unique to this fork

| Mechanic | What it does |
| --- | --- |
| `MegaPipes` | Item routing with filters, sources and destinations. Larger than the plain `Pipes`. |
| `Footprints` | Cosmetic particles where players walk. |
| `BannerCopier`, `BookCopier`, `MapCopier` | Duplicate a held item onto blanks. Kept despite vanilla equivalents. |
| `PageReader` | Reads a book's pages aloud. |
| `LightNetherrack` | Netherrack lit by redstone. |

### Shared with upstream

Compare both sources for these. Where they differ, this fork's behaviour is the one to keep.

`Ammeter`, `BetterPhysics`, `BetterPlants`, `Bookshelf`, `BounceBlocks`, `Chairs`, `CommandSigns`,
`CookingPot`, `Elevator`, `GlowStone`, `HeadDrops`, `HiddenSwitch`, `JackOLantern`, `LightStone`,
`LightSwitch`, `Marquee`, `Netherrack`, `PaintingSwitcher`, `Pipes`, `RedstoneJukebox`,
`SignCopier`, `Snow`, `Teleporter`, `TreeLopper`, `Variables`, `XPStorer`, the `area` mechanics
(`Bridge`, `Door`, `Gate`, `ComplexArea`), the `boat` set, `DispenserRecipes`.

### Dropped by decision

`CBWarps`, `ChunkAnchor`, the Pastebin report upload, and the vendored bStats metrics.

## Infrastructure not yet built

| Piece | Why it is needed |
| --- | --- |
| Persistence | The switch passwords, the switch positions and the wireless bands are saved. What a chip keeps on its own sign is saved with the world. What remains unsaved is deliberate: a destination republishes itself when it loads, and a cart's rider says again where they are going. The scripts in `fireworks/`, `midi/` and `playlist/` are read, never written. |
| Configuration | `config.yml` carries the settings the chips read. Each mechanic will want its own section as it arrives, and the mechanics also need the legacy enable flag. |
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
12. A sensor under a floor reports whoever walks over it, and ignores a vanished player.
13. `config.yml` is written on first run, a narrowed limit shortens a bridge, and
    `/craftbook reload` picks up a change without a restart.
14. A message nearby reads a script out of a book, waits where the book says to, and never
    names a vanished player in the log.
15. A melody plays a MIDI file dropped in `midi/` through a note block, works down a playlist,
    and `/craftbook music songs` lists what a sign can name.
16. Region behaviour on an actual Folia server.
