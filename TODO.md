# Remaining work

An inventory of what the rewrite has not covered yet. Current status and the standing rules
are in `CLAUDE.md`; bugs found in the legacy code are in `FINDINGS.md`.

## Integrated circuits

**140 chips are registered, under 171 model numbers** — 140 of their own, 26 self-triggering
forms and 5 retired numbers kept as aliases. The extra fork's catalogue is complete; 4 model
numbers were dropped by decision.

Nothing from this fork's catalogue is left to port. Anything added from here comes from upstream,
and the rule still holds: look every model number and shorthand up in the legacy `ICManager` before
registering it, and do not invent them.

### Numbering a chip that came from neither codebase

The rule above is about *not colliding with a chip nobody has ported yet*, so a genuinely new chip
— one neither fork ever had — has nothing to look up and must not borrow a number that something
unported might want.

Only eight prefixes are ever claimed across the two source catalogues: `MC`, `MCM`, `MCO`, `MCT`,
`MCU`, `MCX`, `MCZ` and `VAR`. **A new chip takes a prefix none of them uses**, which makes a
collision impossible by construction rather than by remembering to check. `MCN` is the one in use
for this, and `MCN100` and `MCN101` are the first two chips on it.

Note that `MCZ` is the extra fork's self-triggering prefix and always pairs `MCZnnn` with the
`MCXnnn` it ticks for. Upstream never uses it at all; upstream pairs `MC1nnn` with `MC0nnn`. Do not
invent either form — three `MCZ26x` numbers were made up for upstream chips that have no
self-triggering family at all, and had to be taken out again. The `S` suffix ticks any chip
whether or not a second number exists for it.

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

Upstream registers 131 model numbers to this fork's 143, and 55 of those are shared. Counted
against **this rewrite** rather than against the fork it ports, upstream has 50 numbers the
catalogue here does not answer to: four are dropped by decision and **26 are chips already
registered here under the extra fork's number**, so the real gap is **20**. Adding any of them is
an optional addition rather than a compatibility obligation, and comes after everything above.

That gap only shrinks by porting, so it is worth recomputing rather than trusting: it is upstream's
`registerIC` calls, minus every model number, alias and self-triggering form in `ICCatalogue`.

#### Already here under another number

Should one of these ever be wanted as an alias, the extra fork's number stays primary. Two of the
pairings are easy to miss because the two forks named the same chip after different things:
`MCX203` reads as a chest mechanic and is upstream's ranged collector, and `MCX295` is filed under
redstone rather than sensing and is upstream's power sensor.

Where the two differ, this fork's is the richer: `MCX203` also picks which kind of container to
fill, which upstream's cannot.

`MC1266` used to be listed here against `MCX295` and no longer is. The two ask different questions
of the same block — `MCX295` reads what it carries or emits, `MC1266` whether anything is pushing
power *at* it, and a plain block with a lever on its side answers differently — so `MC1266` is
registered as a chip of its own.

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
| Timing | `MC1421` clock |
| Weapons | `MC1218` block launch, `MC1224` time bomb, `MC1228` ent cannon, `MC1251` shoot fires, `MC1252` flame thrower, `MC1278` sentry gun |
| World control | `MC1232` time set, `MC1237` fake time |
| Radio | `MC1276` radio station, `MC1277` radio player |

Twenty, and they reconcile exactly against the fifty upstream numbers this catalogue does not
answer to: four dropped by decision, twenty-six already here under the extra fork's number, and
these twenty.

**Done from upstream so far**, and no longer listed above: the terrain and liquid group — `MC1220`
and `MC1221` block breakers, `MC1222` liq flood, `MC1223` terraform, `MC1225` pump, `MC1226`
spigot, `MC1238` irrigate and `MC1248` driller; the three sensors, `MC1265` inv sns itm,
`MC1266` sense power and `MC1267` sense move; the farming and animal group, `MC1235` cultivator,
`MC1244` animal harv, `MC1280` animal brd and `MC1246` xp spawner; and the delayers and pulsers,
`MC2100`, `MC2101`, `MC2111`, `MC2501`, `MC2510` and `MC2511`.

Three of that last group were **not** written again, because the behaviour was already here.
`MC1214` range coll is `MCX203`. `MC2110` fe delayer is `MCX011` Signal Extender exactly — rise at
once, wait before falling — and `MC2500` pulser is `MCX010` Pulse, whose two lines were widened to
carry upstream's start delay and pause rather than a second chip being registered for them. Both
retired numbers are kept as aliases, which is the merge rule working as intended.

The delayers are worth a word, because it looks like duplication and is not. `MC1000` and `MC1001`
take a delay and hold *both* edges by it, which shifts a signal without changing its shape. A
delayer holds one edge and lets the other through at once, which changes the shape — a flicker
shorter than the delay never reaches the output. Neither can be written as the other, and the
second is a debounce.

The container group is the one that wants a decision before any of it is written. `Stockpile`
aggregates the containers near a chip into one `Key`-to-count map, which loses which slot holds
what; the sorter, the stacker and the contents sensor all care about individual slots. That is a
seam to design once rather than to work around nine times.

## Variables: what is left of them

The store, the `/var` commands, upstream's three chips — `VAR100`, `VAR170`, `VAR200` — the
`[Marquee]` sign and the two new ones, `MCN100` and `MCN101`, are done; see `docs/variables.md`.
Two pieces of both forks' `Variables` mechanic are **not**:

- **`%name%` substitution in text.** Both forks expand `%score%` and `%alice|score%` inside chat
  and commands — the legacy fork against
  `Pattern.compile("%(?:([a-zA-Z0-9_]+)\|)*([a-zA-Z0-9]+)%")`, upstream through
  `ParsingUtil.parseVariables`. Nothing in the rewrite reads a variable outside a chip and a
  command. This is the larger half of the mechanic and is a decision as much as a task: it means
  every chat message on the server going through a substitution pass.
- **Upstream's packet-level `override-all-text`.** Rewrites variables in outgoing packets, and
  needs ProtocolLib. Out of scope on the same grounds as the vendored bStats.

The namespace model was also simplified deliberately. Upstream keys namespaces on a CraftBookID and
resolves player names to UUIDs through `squirrelid`, with per-namespace, per-variable and `.self`
permission nodes underneath. The rewrite keeps plain namespaces, `global` as the shared one, and a
single `craftbook.variables.use.other` — the same trade the toggled areas made, and for the same
reason: one readable thing a builder and an operator can both act on beats surviving a rename.

## Mechanics

The legacy fork carries 70 of them, each marked with `@Module` under `src/main/java/`. 56 are
ported, 5 are dropped or cannot be done, and **9 are left**. (73 files carry the annotation;
`EmptyDecay`, `ExitRemover` and `RemoveEntities` each have a cart class and a boat class, and both
of each are now done.)

The nine are the two unique to this fork and the seven shared with upstream, both listed below.
`Variables` is the one counted as left while being mostly done: everything but the `%name%`
substitution is finished. The five that will not be done are `CBWarps`, `ChunkAnchor`, `CartWarp`,
`LandBoats` and `SpeedModifiers`.

The bridge, the door, the gate, the lift and the toggled area are done, the copiers are done, and
so is the greater part of the rails. Those listed as unique to
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
in the one listener that asks it, and their settings are together under `vehicles.carts`. Every one
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

A pipe can be asked what it believes about itself: the debug stick's **Pipe** mode, or
`/craftbook debug pipe`, reports the style a run was read as, where it takes from, everywhere it
reaches and in what order. It is the only debugging mode that reads an ordinary block rather than a
chip's sign, since a pipe has no sign. See `docs/debugging.md`.

### The copiers: done

`BannerCopier`, `BookCopier`, `MapCopier` and `SignCopier`. The first three are wall signs on a
bookshelf that hand out a copy of the banner above them, the written book in a frame beside them, or
the map their first line numbers. `SignCopier` has no sign at all: black dye copies what one sign
says onto another, and `/sign edit` changes a line before it is pasted.

All four are in one listener, in the spirit of the carts and the sign mechanics, since all of them
answer the same right-click. They are deliberately **not** on the `SignMechanic` seam: every
mechanic there is about blocks, and these are about the item in somebody's hand, which a
`MechanicVisit` does not carry and should not have to.

### The blocks that answer redstone, and the two about light: done

`GlowStone`, `JackOLantern` and `Netherrack` are one seam in `core/powerable/`: a block that becomes
another block when it is powered, or one that carries a fire on top of itself. Nothing is built and
no sign declares them, so they are found by looking at what is beside whatever changed rather than
through an index.

`LightNetherrack` is not a fourth. It and `Netherrack` were the same mechanic written twice, so one
implementation answers for both under the more familiar name, with the block it works on a setting —
see finding 135.

`LightSwitch` has a listener of its own, since nothing powers it and it answers a right-click. Its
reach was made symmetric and its limit made to bite nearest first — finding 134.

`LightStone` and `Ammeter` are one thing in `core/meter/`. Both are an instrument held up to a
block — one reads light, the other reads redstone power — and the fork drew the same fifteen-mark
bar twice to do it. One drawing now, two dials.

Powering a block and then mining the redstone still leaves it lit. That is deliberate and old, and
`depower-on-source-removal` in `mechanics.yml` is how an operator asks for the other behaviour.

Both halves of that are decided in the break handler, because the server decides neither for us:
`BlockRedstoneEvent` is raised by a source when its own power changes, and breaking one goes
through none of those paths. **Not yet verified in game** — findings 136 and the two before it were
all found by standing in front of one of these, so the rest of it wants the same treatment.

### Thrown, sent, bottled and snowed under: done

`BounceBlocks` is a block with a `[Jump]` sign under it, or one an operator has named so it needs no
sign. `Teleporter` is a sign naming a place in the same world, worked by a hand on it or on a button
in front of it. `XPStorer` turns the experience somebody is carrying into bottles and hands back
what would not pay for a whole one — the fork set them to zero and gave back what it had worked out,
so a builder with nineteen points and a sixteen-point bottle lost the other three.

`Snow` is not a mechanic anybody builds. Like the vehicle habits it changes the world everywhere the
moment it is switched on, so it has a settings record of its own under `Snow` in `mechanics.yml`
and every part of it starts off. `core/snow/` turns the game's two snow blocks into one number —
a depth from nought to eight — which is what lets the piling, slumping and melting be arithmetic
and be tested without a server. That paid for itself immediately: a test caught snow piled above
a full block falling back into it and piling again for ever.

**Not ported with it:** the fork's automatic mode for `XPStorer`, an `[XP]` sign and a chest that
gathered loose experience orbs from a distance. It is off by default there too, so a server behaves
the same out of the box; what is missing is an operator's ability to switch it on.

### Sat down and beheaded: done

`Chairs` seats whoever right-clicks a stair with an empty hand. What holds them up is an invisible
marker armour stand the game rides, which means the sitting itself costs nothing per tick and a
server that stops leaves nobody stuck. Nothing is remembered in memory about who is sitting where:
a seat is found by asking the world what is standing in a block, so there is no map to fall out of
step with the world and a seat a crash left behind is still recognised as one when its chunk
returns. `[Sit Heal]` on a sign hung on the chair makes it heal, on the seat's own scheduler rather
than a task walking a list, which is what makes it right on a regionised server.

Two things changed shape from the fork. `/sittoggle` wrote a permission onto the player through
Sponge's permission service, which Bukkit has no equivalent of and which put a player's own
preference into an operator's permission tree; it is kept in the player's own data now, where the
game persists it for us. And the fork's command skipped the occupancy check its click did and told
nobody it had worked, so both go through one `Sitting` now.

`HeadDrops` drops the head of whatever was killed. The game has a head of its own for seven things
and every other creature wears somebody's face, pinned by account identifier rather than by name so
a rename cannot break it. The face itself is never written into the item — the server resolves it
once and remembers it — which is cheaper than a texture apiece but means an offline server with no
way out gets a blank head for a cow.

**Not ported with it:** the fork re-dropped a head when its *block* was mined, because the game of
the day dropped a blank one and lost whose it was. The game has kept the face on a mined head since
the flattening, so there is nothing left for that to fix.

**Neither has been run in game.**

### The loppers and the dispensers: done

`TreeLopper` and the new `VeinMiner` are one engine in `core/lopper/`, bound by one listener. See
the loppers section in `CLAUDE.md` for why they are one thing and what was decided; findings 137 and
138 for what the fork got wrong on the way. `HiddenSwitch` is on the `SignMechanic` seam, resolved
through `MechanicDispatcher` like every other, and takes its key from the sign's first line rather
than from block data nobody can see. `BetterPhysics` is the falling ladders and nothing else, which
is all either codebase ever had under that name. `DispenserRecipes` carries the fork's six machines,
including the two upstream lacks.

**None of it has been run in game.**

### Unique to this fork

| Mechanic | What it does |
| --- | --- |
| `Footprints` | Cosmetic particles where players walk. |
| `PageReader` | A library of books kept in files, with its own commands. 1054 lines in the fork, and its own piece of work rather than a copier. |

### Shared with upstream

Compare both sources for these. Where they differ, this fork's behaviour is the one to keep.

`BetterPlants`, `Bookshelf`, `CommandSigns`, `CookingPot`, `PaintingSwitcher`, `RedstoneJukebox`,
`Variables` (store, commands, the three chips and the marquee sign are done — the `%name%`
substitution in chat and commands is what is left of it; see **Variables: what is left of them**
above).

The `%name%` substitution is what `CommandSigns` actually needs, and is the reason to do it.

**Done from this list:** `Ammeter`, `BetterPhysics`, `BounceBlocks`, `Chairs`, `DispenserRecipes`,
`GlowStone`, `HeadDrops`, `HiddenSwitch`, `JackOLantern`, `LightStone`, `LightSwitch`, `Marquee`,
`Netherrack`, `SignCopier`, `Snow`, `Teleporter`, `TreeLopper`, `XPStorer`.

**New rather than ported:** `VeinMiner`.

The `boat` set is **done, less two that cannot be**. `WaterPlaceOnly` and the boat-going
`EmptyDecay`, `ExitRemover` and `RemoveEntities` are in `BoatHabits`, decided in `BoatBehaviour` and
bound by `BoatHabitListener`, beside the cart habits under a new `vehicles` section rather than
inside `carts`. `LandBoats` and `SpeedModifiers` are **not ported**: the server fields both write
are read nowhere on Paper and only one of four survives on Sponge, so the settings would have looked
as though they worked while doing nothing. See finding 133.

### Dropped by decision

`CBWarps`, `ChunkAnchor`, the Pastebin report upload, and the vendored bStats metrics.

`LandBoats` and `SpeedModifiers` are dropped for a different reason: not a decision about what this
plugin should do, but because neither can be done through any API either platform has. Finding 133
has the evidence. Making boats work on land is possible as a *new* mechanic that moves the boat
itself; that is a thing to agree on rather than a port.

## Infrastructure not yet built

| Piece | Why it is needed |
| --- | --- |
| Persistence | The switch passwords, the switch positions, the wireless bands and the variables are saved, the last three through `SharedStateFiles`. What a chip keeps on its own sign is saved with the world. What remains unsaved is deliberate: a destination republishes itself when it loads, and a cart's rider says again where they are going. The scripts in `fireworks/`, `midi/` and `playlist/` are read, never written. |
| Configuration | `config.yml` carries what the chips, the carts, the vehicle habits and the pipes read; `mechanics.yml` carries the mechanics, a section each, named after the mechanic. A further mechanic wants a name in `Mechanics.ALL` and a section of its own. |
| Storage | The saved areas live in `areas/`, two files each, in the game's own structure format. Nothing else a mechanic uses is written down. |
| Commands | `/craftbook` reads the catalogue and the switch commands drive `MCX120` and `MCX121`, all through Brigadier. The per-mechanic commands come with their mechanics. |
| Permissions | Every chip's permission is registered under `craftbook.ic.safe.*` or `craftbook.ic.restricted.*` and checked on creation. The sign mechanics register a pair each, `craftbook.<name>` to build and `craftbook.<name>.use` to work, and both are checked. Nothing checks a chip at run time. |
| Documentation generator | `./gradlew generateIcDocs` writes `docs/ics.md` from the catalogue, and a test fails the build if the committed page has drifted from it. The mechanics have no generator: what they do is not held as data anywhere, so `docs/pipes.md` and its like are written by hand. |

## Verification

**What has been watched running.** Chips load from signs already in the world and follow redstone,
on Paper and on SpongeVanilla both; see the Sponge section below for what was checked there and
why that one mattered most. Everything else in this document is compiled, unit-tested and unseen.

**What the tests reach.** `core` is covered thoroughly, because it is written to be: chip logic,
sign parsing, pin geometry, the cart and pipe grammars and the mechanic seams are pure functions
with no server behind them. `paper` carries MockBukkit and covers `ICManager`, the sign listener,
the chip title and — since finding 149 — starting the plugin at all. What still has no coverage is
the schedulers, the container lookups, the legacy block mapping and every listener other than
those.

**`PluginStartupTest` is the one to keep working.** It loads the real plugin into a test server and
asserts it enabled, which is the only test here that would have caught the plugin failing to start
— sixty-seven others passed while it could not load. A test server does not run Paper's command
lifecycle, so the commands are built but not registered, and the listeners are registered but never
fired; that is the boundary of what it proves.

The list below is what to check in game, in roughly this order. Items 1 and 2 are done; the rest
are not, and the later groups have been added as the work landed rather than checked off:

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
13. `config.yml` and `mechanics.yml` are written on first run, a narrowed limit shortens a bridge,
    and `/craftbook reload` picks up a change without a restart.
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
20. Nothing under `vehicles.carts` does anything until it is switched on, and then: a cart climbs a
    ladder, crosses a pressure plate, gathers a stack it can hold whole and leaves one it cannot,
    passes through an empty cart, and decays once nobody has got back in.
21. A powered sticky piston empties a chest along a run of glass into another; a stained run keeps
    to its own colour; a `[Pipe]` sign sorts by what its third line names; an `[Extractor]` fills
    whatever its panes touch; a named and enchanted tool comes out of the far end unchanged; and
    breaking one block of a pipe is enough for the next pulse to follow the new shape.
22. Region behaviour on an actual Folia server.

## The Sponge build

`sponge/` binds the same `core` to **SpongeAPI 20** on SpongeVanilla for Minecraft 26.2.
`docs/sponge.md` says what the build is, how it is put together and what it cannot do.

What is there: the module and its build wiring, the scheduler, the direction and position adapters,
the legacy block reader, the world seam, the entity bindings and a stockpile. Core's 38.6k lines
compile against SpongeAPI 20 and Adventure 4.26.1.

`sponge/game/` reaches into Minecraft directly for the four things SpongeAPI cannot answer — drops,
whether a block would survive, one player's own weather, and what `35:14` became. None of them is a
mixin; the plumbing for one is in the build and switched off until something needs it.

The platform-independent files that were sitting in `paper/` have moved to `core`, so both
platforms share one catalogue rather than two that drift.

**The chips run, and they have been watched running.** The plugin has an entry point, reads the
same `config.yml` the Paper build does, starts the chips in every loaded chunk, keeps up with them
as chunks come and go, and answers commands.

**Verified in game** on SpongeVanilla for Minecraft 26.2: a chip loads from a sign already in the
world, and redstone drives it. That last one is the assumption the whole port rested on —
`ChangeBlockEvent.Post` standing in for a redstone event SpongeAPI does not have — and no compiler
could have settled it. See `docs/sponge.md` for what a stock RC build does before it will let a
vanilla client in at all.

What it does not have yet, in the order it needs doing:

| Layer | What it is |
| --- | --- |
| Mechanics, carts, pipes | Three dispatchers and their world bindings |
| Areas | Sponge schematics rather than `org.bukkit.structure` |
| Test bed, debugging | `TestbedBuilder`, the debug stick through `RegisterDataEvent` |

The commands the carts, areas, test bed and debug stick own are absent along with them, rather than
registered and dead.
