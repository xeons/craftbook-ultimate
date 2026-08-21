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

Upstream spells three of these differently and carries a fourth: its ROM pair is `MC3300` set and
`MC3301` get, the other way round from here, and its Perlstone is `MC5000` and `MC5001`. The
decision covers all of them.

### Upstream-only ICs

Upstream registers 131 model numbers to this fork's 143, and 55 of those are shared. Of the 76 it
has that this fork never did, four are dropped by decision and **27 are chips already registered
here under the extra fork's number**, so the real gap is 45. Adding any of them is an optional
addition rather than a compatibility obligation, and comes after everything above.

#### Already here under another number

Should one of these ever be wanted as an alias, the extra fork's number stays primary. Two of the
pairings are easy to miss because the two forks named the same chip after different things:
`MCX203` reads as a chest mechanic and is upstream's ranged collector, and `MCX295` is filed under
redstone rather than sensing and is upstream's power sensor.

Where the two differ, this fork's is the richer: `MCX203` also picks which kind of container to
fill, which upstream's cannot. `MCX295` reads only direct power where upstream's reads indirect
power as well, so that one number is still worth having if the indirect reading is ever wanted.

| Upstream | Here | Upstream | Here |
| --- | --- | --- | --- |
| `MC1112` tele-out | `MCX112` Transporter | `MC1231` t control | `MC3231` Time Control Advanced |
| `MC1113` tele-in | `MCU113` Destination | `MC1234` planter | `MCX216` Planter |
| `MC1204` trap | `MCX132` Hit Mob Above | `MC1236` fake weather | `MCX235` False Weather |
| `MC1208` mult set | `MCX206` Flex Set | `MC1239` harvester | `MCX213` Harvester |
| `MC1214` range coll | `MCX203` Chest Collector | `MC1263` sense block | `MCX205` Block Detector |
| `MC1210` emitter | `MCX250` Particle | `MC1264` sense item | `MCX138` Item Near |
| `MC1211` set bridge | `MCX207` / `MCX209` Bridge | `MC1270` melody | `MCU700` Melody |
| `MC1212` set door | `MCX208` / `MCX210` Door | `MC1271` sns entity | `MCX119` Mob Near |
| `MC1213` sound | `MCX251` Sound Effect | `MC1272` sns player | `MCX118` Player Near |
| `MC1215` set a chest | `MC1205` Set Above, on a stockpile | `MC1273` jukebox | `MCU706` Jukebox |
| `MC1216` set b chest | `MC1206` Set Below, on a stockpile | `MC1275` tune | `MCU705` Tune |
| `MC1217` pot induce | `MCX146` Potion Area | `MC1279` player trap | `MCX131` Hit Player Above |
| `MC1227` avd spawner | `MCX200` Entity Spawner | `MC1422` monostable | `MCU440` Monoflop |
| `MC1266` sense power | `MCX295` Trigger Reader | | |

#### Where the two forks disagree about a number

Four numbers name different chips in the two catalogues. The sign format is frozen, so this fork's
reading wins; the consequence is that upstream's chip has no number left to be registered under,
and needs one chosen before it can be ported at all.

| Model | Upstream | Here |
| --- | --- | --- |
| `MC1025` | Server time modulus, off the real clock | World time modulus |
| `MC1250` | Fire shooter | Fireworks |
| `MC1420` | Clock divider | Clock |
| `MC1500` | Ranged output | Player online |

Note that `MC1421` is upstream's clock, so it cannot be taken at its own number either while
`MC1420` here means one.

#### Genuinely absent

| Group | Models |
| --- | --- |
| Containers and logistics | `MC1209` collector — not the ranged one, this picks up only what lies on its own sign and hands it to a `Pipes` network — `MC1219` auto craft, `MC1229` sorter, `MC1233` item fan, `MC1242` stocker, `MC1243` distributer, `MC1245` cont stkr, `MC1268` sns cntns, `MC1269` sns p cntns |
| Terrain and liquids | `MC1220` / `MC1221` block breakers, `MC1222` liq flood, `MC1223` terraform, `MC1225` pump, `MC1226` spigot, `MC1238` irrigate, `MC1248` driller |
| Timing | `MC1421` clock, `MC2100` / `MC2101` / `MC2110` / `MC2111` delayers, `MC2500` / `MC2501` / `MC2510` / `MC2511` pulsers |
| Weapons | `MC1218` block launch, `MC1224` time bomb, `MC1228` ent cannon, `MC1251` shoot fires, `MC1252` flame thrower, `MC1278` sentry gun |
| Farming and animals | `MC1235` cultivator, `MC1244` animal harv, `MC1246` xp spawner, `MC1280` animal brd |
| Sensors | `MC1265` inv sns itm, `MC1267` sense move |
| World control | `MC1232` time set, `MC1237` fake time |
| Radio | `MC1276` radio station, `MC1277` radio player |
| Variables | `VAR100` num mod, `VAR170` at least, `VAR200` item count. These read the `Variables` mechanic, which is itself unported. |

## Mechanics

The legacy fork carries 70 of them, each marked with `@Module` under `src/main/java/`. 33 are
ported, 3 are dropped by decision, and **34 are left**. The bridge, the door, the gate, the lift
and the toggled area are done, and so is the greater part of the rails. Those listed as unique to
this fork have no upstream equivalent to compare against, so the legacy source is the only
specification.

### Minecart mechanics: everything with a sign

`Station`, `StationClear`, `CartSort`, `CartLift`, `CartLaunch`, `CartDelay`, `CartLoad`,
`CartDirection`, `CartBooster`, `CartCollect`, `CartDeposit`, `CartCraft`, `CartPrint`,
`CartDispenser`, `CartEjector` and `CartReverser`, with the `/station`, `/cbgo` and `/cbrecipes`
commands. `CartPrint` covers `CartMessenger` as well: both claim `[Print]` in the legacy fork, so
one implementation answers for both.

### Minecart mechanics: the habits, all off by default

`EmptyDecay`, `ExitRemover`, `ItemPickup`, `MobBlocker`, `MoreRails`, `NoCollide` and
`RemoveEntities` carry no sign and are built from nothing. They are together in `CartBehaviour` and
in the one listener that asks it, and their settings are together under `carts.habits`. Every one
is off until an operator says otherwise, since each changes every cart on the server rather than
one place on the track.

`CartWarp` is **not** ported: it teleports a cart to a CBWarp, and CBWarps is dropped by decision.
Porting it needs a decision about what it should warp to instead.

### Sign mechanics: done

`Bridge`, `Door`, `Gate`, `Elevator` and `ComplexArea`, on the seam in `core/mechanic/` and the
dispatcher in `paper/mechanic/`. Every sign name either fork carries is accepted, the gate's six
materials and its small and clickable forms included.

The toggled area keeps its blocks in the game's own structure format through
`org.bukkit.structure`, with the place they belong in an `.anchor` beside each one, and brings its
own selection commands since there is no world editor to borrow one from. The whole `area` package
is done.

### Pipes: done

`Pipes` and `MegaPipes` are one mechanic. Both grammars are accepted — a sticky piston at the head
of a run of glass, and a piston with an `[Extractor]` sign at the head of a run of panes — and
`PipeStyle`, chosen by the block a run starts from, decides how the blocks along it are read. The
filter `MegaPipes` never built is finished, on the `[Pipe]` sign's third and fourth lines.

`MegaPipes` was left half-written in the fork: debug output through its extraction path, an empty
`MegaPipesFilter`, `MegaPipesSource` and `ChestSource` stubbed out, `registerSource` never called,
and the only source that worked hardcoded into `ExtractorMultiBlock`. What was worth keeping was its
shape rather than its code, and the shape is what was kept.

### Unique to this fork

| Mechanic | What it does |
| --- | --- |
| `Footprints` | Cosmetic particles where players walk. |
| `BannerCopier`, `BookCopier`, `MapCopier` | Duplicate a held item onto blanks. Kept despite vanilla equivalents. |
| `PageReader` | Reads a book's pages aloud. |
| `LightNetherrack` | Netherrack lit by redstone. |

### Shared with upstream

Compare both sources for these. Where they differ, this fork's behaviour is the one to keep.

`Ammeter`, `BetterPhysics`, `BetterPlants`, `Bookshelf`, `BounceBlocks`, `Chairs`, `CommandSigns`,
`CookingPot`, `GlowStone`, `HeadDrops`, `HiddenSwitch`, `JackOLantern`, `LightStone`,
`LightSwitch`, `Marquee`, `Netherrack`, `PaintingSwitcher`, `RedstoneJukebox`,
`SignCopier`, `Snow`, `Teleporter`, `TreeLopper`, `Variables`, `XPStorer`, `DispenserRecipes`,
and the `boat` set — `LandBoats`, `WaterPlaceOnly`, `SpeedModifiers`, and boat-going copies of
`EmptyDecay`, `ExitRemover` and `RemoveEntities`. Those three are separate classes from the
minecart ones now ported, and the same habits applied to boats; whether they join `CartHabits` or
get a section of their own is a decision for when the boats arrive.

### Dropped by decision

`CBWarps`, `ChunkAnchor`, the Pastebin report upload, and the vendored bStats metrics.

## Infrastructure not yet built

| Piece | Why it is needed |
| --- | --- |
| Persistence | The switch passwords, the switch positions and the wireless bands are saved. What a chip keeps on its own sign is saved with the world. What remains unsaved is deliberate: a destination republishes itself when it loads, and a cart's rider says again where they are going. The scripts in `fireworks/`, `midi/` and `playlist/` are read, never written. |
| Configuration | `config.yml` carries the settings the chips read, the minecart mechanics read, and the sign mechanics read. Each further mechanic will want its own entries as it arrives. |
| Storage | The saved areas live in `areas/`, two files each, in the game's own structure format. Nothing else a mechanic uses is written down. |
| Commands | `/craftbook` reads the catalogue and the switch commands drive `MCX120` and `MCX121`, all through Brigadier. The per-mechanic commands come with their mechanics. |
| Permissions | Every chip's permission is registered under `craftbook.ic.safe.*` or `craftbook.ic.restricted.*` and checked on creation. The sign mechanics register a pair each, `craftbook.<name>` to build and `craftbook.<name>.use` to work, and both are checked. Nothing checks a chip at run time. |
| Documentation generator | `./gradlew generateIcDocs` writes `docs/ics.md` from the catalogue, and a test fails the build if the committed page has drifted from it. The mechanics have no generator: what they do is not held as data anywhere, so `docs/pipes.md` and its like are written by hand. |

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
16. A bridge and a door driven by a lever agree with the lever, and a gate drops from its lintel
    and winds back up when clicked on its own fence.
17. A lift carries somebody between floors by sign, by button and by jumping on a pad, and names
    the floor they arrive at.
18. `/area pos1`, `/area pos2` and `/area save` write a structure and an anchor, an `[Area]` sign
    swaps it in and out, and an area holding a chest keeps what was in the chest.
19. An `[Eject]` sign sets a rider down on the platform behind it, and a `[Reverse]` sign turns
    back a cart that comes at it from the wrong side while letting the other way through.
20. Nothing under `carts.habits` does anything until it is switched on, and then: a cart climbs a
    ladder, crosses a pressure plate, gathers a stack it can hold whole and leaves one it cannot,
    passes through an empty cart, and decays once nobody has got back in.
21. A powered sticky piston empties a chest along a run of glass into another; a stained run keeps
    to its own colour; a `[Pipe]` sign sorts by what its third line names; an `[Extractor]` fills
    whatever its panes touch; a named and enchanted tool comes out of the far end unchanged; and
    breaking one block of a pipe is enough for the next pulse to follow the new shape.
22. Region behaviour on an actual Folia server.
