# CraftBook Ultimate

A ground-up rewrite of CraftBook for **Paper 26.x** on **Java 25**, in the namespace
`com.xeonproductions.craftbookultimate`.

## Project status

**Phase: chips run in game. Writing a recognised model reference on a wall sign creates a
working chip that responds to redstone and to the passage of time, places and swaps blocks, farms,
drives wireless bands, moves people between named pads, follows switches thrown by command,
spawns, shoots at, hurts, doses and senses what stands near it, speaks to whoever is near, to one
player anywhere, to the whole server or to the log, shows people weather the world is not having,
and plays a sound, a record, a tune written on its own sign or a MIDI file out of the plugin's
folder. The extra fork's whole IC catalogue is done. Beyond the chips, a railway runs — a cart is
stopped, sorted, lifted, launched, held, turned back, emptied of goods and of people, filled and
sent on — and the sign mechanics with it: a bridge runs out across a gap, a door fills a doorway, a gate drops from its lintel, a lift
carries people between floors, and a whole saved region swaps itself in and out — each answering
to a hand and to redstone.**

| Area | State |
| --- | --- |
| Gradle build, two modules, toolchain | Done |
| Core value types (`Vec3i`, `BlockFace`, `SignLines`) | Done |
| IC sign grammar (`ICLine`, `ICMode`) | Done |
| Pin geometry (`PinOffset`, `PinLayout`) | Done |
| IC contracts (`ChipState`, `ICLogic`, `SelfTriggeringICLogic`) | Done |
| IC catalogue (`ICDefinition`, `ICRegistry`) | Done |
| Combinational gates, latches, arithmetic, routing | Done |
| Plugin bootstrap, descriptor, loadable jar | Done |
| Folia region schedulers (`RegionSchedulers`) | Done |
| World adapters (directions, positions, signs, redstone) | Done |
| World-backed `ChipState` (`BlockChipState`) | Done |
| IC catalogue wiring (128 chips) | Done |
| IC instance lifecycle (`ICInstance`, `ICManager`) | Done |
| Listeners: sign creation, break, redstone, chunk load | Done |
| Self-triggering chips (per-region tick tasks) | Done |
| Time-based chips (clock, sensors, pulse, delays) | Done |
| Commands (Brigadier, `/craftbook` and the switch commands) | Done |
| Configuration (`config.yml`, `mechanics.yml`, `/craftbook reload`) | Done |
| Persistence | Switch passwords, switch positions, wireless bands |
| World seam (`ChipWorld`, `SimpleChipWorld`) | Done |
| World sensors (liquid, light, weather, block detector) | Done |
| Weather and time control chips | Done |
| Stockpiles, formerly BlockBags | Done |
| Shared chip registries (`ChipServices`, `Radio`, `Destinations`) | Done |
| Entity seam: finding and moving people (`Traveller`) | Done |
| Entity seam: seeing, spawning and hurting (`Bystander`, `EntitySpec`) | Done |
| Block placing chips (bridge, door, flex set, set above/below) | Done |
| Block swapping chips (toggle block, block replacer) | Done |
| Harvesting and planting (harvester, planter, area planter) | Done |
| Command driven switches, shift register, monoflop, trigger reader | Done |
| Wireless bands (transmitter, receiver, analog transmitter) | Done |
| Transporters and destinations | Done |
| Spawners, dispensers and the chest collector | Done |
| Shooters, barrages and the fireball | Done |
| Lightning, Zeus bolt and holy smite | Done |
| Mob zapper and the two trap chips | Done |
| Potion areas, particles and fireworks | Done |
| Sensing people, creatures and items | Done |
| Messaging, logging and the two marquees | Done |
| Weather illusions (false and hidden rain) | Done |
| Sound effect, jukebox and written tunes | Done |
| Music from a MIDI file (`MCU700`) | Done |
| Minecart mechanics (15 on the rails, plus the dispenser) | Done |
| Minecart mechanics: the seven habits every cart has | Done |
| Pipes, both grammars on one engine | Done |
| Sign mechanic seam (`SignMechanic`, `MechanicWorld`, `MechanicVisit`) | Done |
| Bridge, door and gate, with the gate's six materials | Done |
| Lifts, including the two-way sign, buttons and jump pads | Done |
| Toggled areas, saved in the game's own structure format | Done |
| Variables, and the three upstream chips that read them | Done |
| Test bed: a rig per chip, built from the catalogue | Done |
| Debugging: the IC stick, the commands, the report | Done |
| The copiers: banner, book, map and sign | Done |
| The powerables and the light switch | Done |
| The two meters, light and redstone, on one dial | Done |
| Bounce blocks, teleporters, the experience store | Done |
| Snow that piles, slumps and melts | Done |
| Chairs, with the healing sign and the three commands | Done |
| Head drops, the game's seven and a face for the rest | Done |
| The boat habits, beside the cart ones under `vehicles` | Done |
| Ladders that fall, and the six dispenser machines | Done |
| The hidden switch, on the sign mechanic seam | Done |
| The marquee, which reads a variable onto a sign | Done |
| The tree lopper and the vein miner, on one engine | Done |
| Mechanics other than those, the copiers and the minecart ones | 9 left |
| Sponge build: module, adapters, world seam, entity bindings | Done |
| Sponge build: the native layer over Minecraft's own code | Done |
| Sponge build: entry point, config, chips running | Done |
| Sponge build: commands, on `Command.Parameterized` | Done |
| Sponge build: mechanics, carts, pipes, areas, test bed | Not started |

Verified working: Gradle 9.7, JDK 25, `paper-api:26.2.build.112-stable`, Adventure 5.2.0,
**2376 tests passing**.

Remaining work is inventoried in `TODO.md`.

Run `./gradlew build` to compile, test, and produce
`paper/build/libs/CraftBookUltimate-<version>.jar`. That jar carries the core module's classes;
Adventure and JSpecify come from the server.

## Layout

```
core/    platform-independent domain model; no server API on its classpath
paper/   Paper 26.x bindings: plugin, schedulers, adapters, catalogue
sponge/  SpongeVanilla 26.2 bindings (SpongeAPI 20); see docs/sponge.md
src/     legacy Sponge sources, kept only as a behavioural reference
```

`core` depends on Adventure and JSpecify and nothing else. That is deliberate: chip logic, sign
parsing and pin geometry are pure functions there, so they are exercised in plain JUnit with no
server running. Anything needing a `World`, a scheduler or an event belongs in `paper`.

The root project does not apply the `java` plugin, which is what keeps the legacy `src/` tree out
of the build.

## Source material

Two existing codebases serve as **behavioural specification only**. No code is copied from
either; each is read to learn what a mechanic does, then written fresh.

- `src/` in this repository — Sponge 7.3 / MC 1.12.2 fork, ~20.6k LOC, 74 mechanics, 122 ICs.
  Holds the extra-fork-only features.
- `E:\Code\CraftBook` — EngineHub CraftBook 3.10.14 on Bukkit / MC 26.1.2, ~54.9k LOC.
  The larger catalogue.

Between them: 58 ICs in both, 73 upstream only, **64 only in this fork**, for a target of ~195.

`E:\Code\Paper` holds a Paper source checkout. Read it to settle a question about server API
behaviour rather than inferring one from a javadoc summary.

## Rules for this rewrite

1. **The sign text format is frozen.** Existing worlds are full of signs. `ICLine` is the
   contract; changing what it accepts breaks live builds. Mechanic behaviour is likewise
   expected to match. That includes pre-flattening block spellings: the fork being ported from
   ran on 1.12, so signs naming blocks as `35:14` are the common case, not an edge case.
2. **Everything else may change.** No backwards compatibility is owed to the old APIs, config
   format, or storage.
3. **No MinecraftOnline references** anywhere in the new code.
4. **Prefer Paper API over Bukkit API**, including experimental Paper API.
5. **Adventure for all text.** No legacy colour codes, no `ChatColor`. The one place `&` codes
   are read at all is where a builder writes them on a sign, which is part of the frozen format;
   they become a component the moment they are read and are never written back out.
6. **Folia compatible** where possible: region schedulers, no cross-region reach, no assumption
   of a single main thread.
7. **Fix bugs rather than reproduce them.** Record each one in `FINDINGS.md` with what the old
   code did and why it was wrong. Do not leave that history as comments in the new source.
8. **Comments describe behaviour**, not the rewrite's own history.
9. **Merge duplicated behaviour.** Where two ICs do the same thing, write one and register it
   under the **craftbook-extra number**, keeping the retired number as an alias.
10. **Never invent a model number or shorthand.** Look every one up in the legacy registry before
   registering it. A wrong number silently breaks existing signs and can collide with a chip that
   has not been ported yet.

## Naming

The legacy code called a place to take materials from a "block bag". The rewrite calls it a
**stockpile**, which says what it is, and its operations are `take` and `give` rather than
`remove` and `add`. A mechanic asks a stockpile for materials before it builds and gives them
back when it takes the structure down.

## Configuration

**Two files** in the plugin's folder, read into one immutable `Settings` value in `core`.
`config.yml` carries the chips, the carts, the vehicle habits and the pipes; `mechanics.yml`
carries the mechanics. Chips reach the result through `ChipState#settings()`; nothing reads a file
or a server from inside a chip, so a limit can be exercised in a plain unit test.

`ConfigDocument` and `MechanicsDocument` hold every setting's name, default and explanation. A
platform's `ConfigFile` supplies a `ConfigTree` per file and nothing else, which is what keeps the
two bindings from drifting into disagreeing about what an operator's file means.

`Configuration` is the one mutable holder saying which `Settings` is current. `/craftbook reload`
replaces it and starts every chip again, so a change takes effect without a restart.

Two rules shape what belongs there. A setting is either a limit on how far a chip may reach or a
statement about what may run at all; nothing in either file changes what a sign *means*. And a sign
asking for more than it is allowed gets as much as it is allowed rather than being refused, so
narrowing a limit shortens an existing build instead of breaking it.

The settings that exist are the ones the legacy fork had, at the same defaults, so a world of
existing signs behaves the same on a server that has never been configured. Do not add a setting
for a limit a chip has always had on its own: those numbers are part of the frozen sign grammar.

### The mechanics file

`mechanics.yml` is **a section per mechanic, named after the mechanic**, which is how both source
codebases arranged the same thing: upstream keeps one `mechanisms.yml` keyed by name, and the fork
wrote a file per mechanic into a `mechanics/` folder. One file rather than twenty, because a folder
of twenty is harder to grep, back up or paste into a bug report and the per-mechanic grouping — the
part that actually helps — is the same either way.

`Mechanics.ALL` in `core/mechanic/` is the one list of names, and every mechanic takes its own name
from it. That name is asked for in three places which have to agree — the file writes a section
with it, the settings record which are switched off by it, and each mechanic asks whether it may
run by it — and a name spelt differently in any of those reads exactly like a mechanic somebody
turned off.

**Every mechanic gets a section**, including the ones with nothing to configure, so the file answers
what there is as well as what may be changed. **Switching one on is that section saying
`enabled: true`**, rather than a list of names somewhere else: a list sits in a different place
from the settings it governs, so an operator whose mechanic does nothing has to know to look in two
places.

**Every mechanic starts switched off**, which is the opposite of the chips. A chip does nothing
until a builder writes its sign, so shipping the whole catalogue enabled costs a server nothing. A
mechanic has no such sign to wait for: the blocks it answers to are ordinary blocks already in the
world, so switching `Chairs` on makes every stair a seat and switching `HeadDrops` on changes what
every death leaves behind. Neither is something a builder opted into, so neither happens until an
operator says so. It is also what the legacy fork did — its `enabled-mechanics` began as a list of
one — and the same reasoning that keeps the cart habits off.

`MechanicSettings#enabled` is therefore the set that **runs**, not the set that does not, and it is
empty in `DEFAULTS`. That direction matters beyond taste: a mechanic added in a later version is
off without anybody remembering to add it anywhere, and a file an operator has trimmed by hand
cannot turn something on by omission. `CraftBookPlugin#sayWhichMechanicsRun` logs the count and the
names once at start-up, because "nothing is switched on" and "the plugin failed to load" look
identical from inside the game.

`MechanicSettings` mirrors it — one small record per mechanic (`GateSettings`, `ElevatorSettings`,
`AreaSettings` and the rest), assembled through a builder. Two rules belong to no single mechanic
and stay at the top of the file: whether redstone works a mechanic's sign, and whether a powered
block goes out when its source is mined away.

Nothing migrates an existing `config.yml`. A `mechanics.*` key left in the old file is ignored and
the new file is written from the defaults, which is the "no backwards compatibility is owed to the
config format" rule taken at its word.

## Scope decisions

Dropped: CBWarps, ChunkAnchor, Pastebin report upload, vendored bStats, PLC/Perlstone (MC5000),
ROM Get/Set (MC2300/MC3300).

Kept despite vanilla equivalents: MapCopier, BannerCopier, BookCopier, the powerable blocks,
BounceBlocks, XPStorer, Ammeter, LightStone.

ComplexArea is reimplemented on the game's own structure format through `org.bukkit.structure`;
**no WorldEdit dependency** anywhere in the plugin.

## Conventions

- Records for value types, sealed hierarchies where a closed set is meant.
- `@NullMarked` at type level; JSpecify annotations, not JetBrains or JSR-305.
- Tests use JUnit 5 with `@Nested` groupings and AssertJ. Test names read as sentences.
- One behaviour per test; assert on outcomes, not on how they were reached.

## The minecart mechanics

A cart mechanic is a piece of rail, the block under it, and usually a sign. The block says which
mechanic it is and the sign says what to do. `core/cart/` holds the seams (`Cart`, `CartWorld`,
`CartVisit`), the frozen filter grammar (`CartFilter`), the shared destination registry
(`Stations`) and the mechanics themselves in `core/cart/mechanic/`; `paper/cart/` binds them.

`CartDispatcher` is the single entry point. A cart crossing one block sends a dozen move events, so
the mechanism is resolved and its wiring read once per event and every applicable mechanic is then
run against the same `CartVisit`. Do not give a mechanic its own listener.

Three things are frozen and must not be quietly improved:

1. **The filter grammar** in `CartFilter` — `storage`, `#north*`, `ply:!Steve`, `sci+:stone:4` and
   the rest. Junction signs all over existing railways are written in it.
2. **The sign names** in `CartSignRules` — `[Station]`, `[CartLift]`, `[Sort]` and so on.
3. **Which block builds which mechanic**, though an operator may now change it in `config.yml`.

`Bukkit`'s `VehicleMoveEvent` is not cancellable, unlike the Sponge event the fork was written
against. A mechanic that wants to hold a cart stops it dead instead, which is what holding it
amounted to anyway.

**Not ported:** CartWarp, and it will not be. Its whole purpose was teleporting a cart to a
CBWarp, and CBWarps is on the dropped list, so there is nothing for it to warp to.

## The habits every cart and boat has

Seven of the minecart mechanics are not mechanisms at all. Nothing is built, nothing carries a
sign, and each one changes what every cart in the world does: empty carts decay, a rider takes the
cart with them, a storage cart gathers up what it runs over, creatures are kept out, ladders and
vines and pressure plates carry carts the game would not, carts pass through one another, and an
occupied cart hurts what it hits.

They live together in `core/cart/CartBehaviour.java` as decisions and in
`paper/listener/CartHabitListener.java` as the one listener that asks, because they overlap: three
answer the same collision, two the same dismount and two the same move. `CartDispatcher` resolves a
mechanic from a block and a sign, so none of these goes near it.

**All of it is off out of the box**, as the mechanics are and for the same reason. A server that
has never been configured runs carts exactly as the game does. A habit changes every cart on the
server the moment it is switched on, and nobody built anything to ask for it.

The settings are `vehicles.carts` in `config.yml`, held in `CartHabits`. Two of them are numbers
rather than switches, and each turns its own habit off at zero: waiting no time before taking an
empty cart away, and climbing at no speed, both mean not doing it.

## Documentation

`docs/` holds what a builder reads. Two kinds live there and they are kept in different ways.

`docs/ics.md` is **generated**: `./gradlew generateIcDocs` writes it from `ICCatalogue` through
`ICDocs`, and `IcDocsAreCurrentTest` fails the build when the committed page and the catalogue
disagree. A hundred and more chips, each with a number, a shorthand, a wiring and a permission, is
more than anybody keeps in step by remembering to, and a wrong model number reads exactly like a
right one — so the build notices rather than a builder finding out by writing a sign that does
nothing. Do not edit that page by hand.

Everything else there is **written**, because what a mechanic does is not held as data anywhere to
generate from. `docs/pipes.md` is the pattern: what the thing is, how to build one, the frozen
grammar in full, worked examples, what to check when it does not work, and a section for operators
at the end. `docs/mechanics.md`, `docs/variables.md`, `docs/testbed.md`, `docs/fireworks.md` and
`docs/debugging.md` follow it.

## What a chip's lines mean

Every chip says what its third and fourth sign lines are for, as a `LineSpec` on its
`ICDefinition`. That one piece of data serves two purposes, which is why it lives in the catalogue
rather than as prose in a document: `docs/ics.md` is generated from it, and `ICSignListener` checks
a sign against it as the sign is written.

Two questions, deliberately separate.

**Required against optional.** A required line is one the chip does nothing at all without — a
melody with no file named returns before it plays a note, and says so to nobody — and a sign
leaving one blank is refused with a reason. An optional line has a sensible default, the sign is
created, and the builder is told what they defaulted to. Refusing a chip that would have worked is
the failure mode to avoid, so `required` was set only where the chip's own code bails when the line
will not resolve.

**What the line will take**, which is a `LineForm`. A form is not a description of a grammar — it
**is** the chip's own reader, asked whether it would succeed. `LineForms.itemFilter()` calls
`ItemCriteria.parse`; `LineForms.entity()` calls `EntitySpec.parse`; the spellings each one prints
come from the parser's own declared vocabulary. Nothing there can promise something the chip would
refuse, and a test asks every form to read its own example so a form whose promise and whose reader
come apart fails the build.

The two combine the obvious way. A required line the chip cannot read refuses the sign and quotes
what was written and what the line takes; an optional one warns and the chip falls back to its
default, which is what it would have done silently anyway. `LineForms.free()` is a line that takes
any text at all, which is most of them and is not the same as a line nobody has described.

That gap mattered. Before forms existed a line that would not parse was completely silent: the
sensor set its output low and returned, so `item:stone` on an item sensor built a chip that read to
a builder as a wiring fault. There are 38 of those early returns; the lines that reach them are the
ones converted.

Refusing is safe because review only happens on `SignChangeEvent`. A sign already in the world is
read through `ICManager#describe` on chunk load and never comes past the listener, so a rule added
later cannot invalidate anything already built.

That safety is also the gap. A chip built before its lines were written down is never refused and
so is never told about, and a builder has no way to tell one from a wiring fault. `ChipTitle` is
the answer: a loaded chip missing a required line has **its first line written red**, and the mark
comes off again once the line is filled in, so a red title always means broken now rather than
broken once. Line 1 is the plugin's own — it is overwritten with the chip's shorthand as the sign
is created — so nothing a builder wrote is at stake, and only its colour changes.

Three things keep that off the chunk-load hot path. The reading is the same `LineReview` the
listener uses, made from `SignLines` that `ICManager#load` had already read for the chip itself, so
the check itself costs a comparison. Nothing is written unless the colour actually differs, which
means a world of working chips writes nothing at all and a marked one is not marked twice. And the
write that does happen is deferred a tick and dropped if the chip has gone, so no block is written
while its chunk is still arriving.

`/craftbook check` is the same question asked of everything loaded at once, for an operator who
would rather not walk the map. It writes nothing whatever — every answer comes from sign text
already in memory — which is what makes it, rather than a repainting sweep, the right tool for
"what is broken across my server".

Every chip declares this, including `noLines()` for the gates that read neither. Said outright
rather than left to silence, so that a chip nobody has documented is distinguishable from one with
nothing to document — `ICCatalogueTest` fails the build on a chip that says nothing, which is what
stops the page going quietly half-written as chips are added.

Upstream has a `getLineHelp()` of its own and it was **not** ported. It describes upstream's
grammar, which often differs: its bridge reads `onID{:onData-offID:offData}` where this one takes a
block and then `width:length`. Documenting grammar the code does not accept is worse than
documenting none — which is exactly why a form is the reader rather than a sentence about it.

## What a chip's pins do

A chip's wiring says how many pins it has and where they sit. What each one **means** is
`inputs(...)` and `outputs(...)` on the definition, and only the chips with something to say say
it: the other eighty-odd are set off by input 1 and answer on output 1, which the layout already
implies and which would be the same sentence on eighty pages. What the page says for those instead
is the one thing a builder cannot see from the layout — that inputs 2 and 3 are wired to nothing.

A chip that names its pins must name **all** of them, padding with `"not read"` where the layout is
wider than the chip. A half-named chip is worse than an unnamed one: the page then says what two of
three levers do and leaves a builder guessing whether the third is unread or merely unwritten.
`ICCatalogueTest` fails the build on one.

`ICDefinition#readsEveryInput` is derived from that rather than kept as a list. The test bed used
to hold its own list of which chips read past input 1 and had missed the bit shift register, which
was built with a single lever and so could never be written into. One list, in the place that
already knows.

## What a command does, and where it says it

A command is two things: a grammar, and what it does. The grammar differs by platform and nothing
can be done about that — Paper's commands are Brigadier, and SpongeAPI 20 exposes no Brigadier at
all, so the Sponge build is `Command.Parameterized`. What a command *does* is the same on both, so
it lives once in `core/command/` as a set of `...Actions` classes, named after `DebugActions`, which
was already the pattern for "what each mode does, said once".

`Caller` is the whole seam between them, and it is four questions, none of which needs a server:
what to say back, what the caller may do, what they are called — which is what decides whose
variables are whose — and where they are standing, which only `/craftbook check` uses and only to
put the nearest broken chip first. The console is a caller like any other: it has a name, every
permission, and is standing nowhere.

This is the same split as `ConfigDocument`, and for the same reason. Every one of these commands is
mostly wording, and wording that exists twice drifts — two platforms telling a builder different
things about the same variable is worse than either wording alone. It also means the behaviour is
testable in plain JUnit with a recording `Caller`, which is where the rules that were previously
only exercised by hand now get pinned.

A platform still owns anything that needs a world. `/craftbook check` is the example: reading the
loaded chips' signs is done by the binding, which hands `core` a list of `BrokenChip` values and
lets it decide what the answer reads like.

## The variables

A variable is a named number the whole server shares, kept in `Variables` alongside the wireless
bands and the commanded switches and persisted the same way. `VAR100`, `VAR170` and `VAR200` are the
three chips that read and change one, and `/var` is where variables are made.

These three come from **upstream** rather than from the fork being ported, so their model numbers,
their shorthands and their sign grammar are upstream's. The namespace grammar is `namespace|name`,
defaulting to `global`, which is the same shape as a wireless `Band` and is written the same way on
a sign and in a command.

Two decisions are worth keeping:

**A variable must exist before a sign may name one.** The chips refuse such a sign as it is
written, through `ICLogic#reviewSign`. That hook is new, and it is the IC counterpart of
`SignMechanic#review` for exactly the reason that one exists: what a variable is called lives in the
store rather than in the blocks beside the sign, so a sign naming one nobody has made would be
silently dead and its builder would have nothing to tell that from a wiring fault. Almost no chip
needs it; these three do.

`[Marquee]` is a fourth reader and the only one that is not a chip: a sign that tells whoever
clicks it what a variable says. It is on the `SignMechanic` seam and makes the same check through
`SignMechanic#review`, which is what those two hooks exist for. The variables reach it through
`MechanicWorld#variables()`, added the way `vault()` already was — a mechanic reaches everything
through that seam, and a world with no store behind it holds no variables rather than refusing to
answer.

Two chips and a mechanic are called marquees and they are unrelated. `MC2999` and `MC3456` are
chasing lights; this is a board with writing on it. The word means both.

**A variable that goes away afterwards is not an error.** It cannot be — a chip cannot be refused
retrospectively, and throwing from inside one that ticks would take the region with it. So
`Variables#number` answers `OptionalDouble`, empty both for a variable that is gone and for one
holding something that is not a number, and each chip decides what to do with nothing. Upstream
instead threw a `NullPointerException` once per tick; see findings 125 to 127.

## The test bed

`/craftbook testbed build` lays out a flat plane carrying a working rig for every chip: sign,
levers, lamps, label. `core/testbed/` holds the geometry — `Rig` wires one chip from its
`PinLayout`, `Testbed` lays them on a grid, `ChipSetup` says what each sign needs told —
and `paper/testbed/TestbedBuilder` decides which block plays which part. `docs/testbed.md` is the
guide.

Built from `ICCatalogue` rather than shipped as a schematic, for the reason `docs/ics.md` is
generated: a chip added later would be missing from a saved plane and nobody would notice, and a
rig wired from a remembered layout reads as a broken chip when it is the bed that is wrong.

**Levers on both sides**, because that is what the plugin reads and writes. `BlockChipState`
decides an input is wired by asking whether the pin block is a power source, and drives an output
only when the pin already holds a lever, leaving anything else alone. A rig of redstone blocks
would read as permanently on and one of lamps would never be driven. Each output lever clings to
its lamp, so the lamp is strongly powered rather than lit by proximity, and a lever whose pin sits
directly above another — `UISO` stacks two — takes a wall or ceiling instead of a floor.

**A ticking rig is written as the chip's own ticking number** where the catalogue has one —
`[MC0111]` rather than `[MC1111]S`. Twenty-six chips were catalogued twice, and writing the second
number is both what a builder does and the only way those numbers appear on the bed at all.

**Ticking is opt-in, per chip, and the list is short.** `ChipSetup#ticks` names the chips whose
tick only reads — sensors, the wireless receiver, the variable comparison — and those are built
with `S`. Everything else carries the plain model number. Both halves of that matter: forcing the
flag on everything killed a server, because `HolySmite.tick` strikes everything in range with no
input check; leaving it off everything made the receiver read its band only while its own lever
was held, so a working transmitter and receiver looked broken.

**Only the inputs a chip reads get a lever.** `ChipSetup#usesEveryInput` names the 24 that read
past input 0, found by grepping the gates for `input(1)`, `input(2)`, `connectedPoweredCount` and
`inputPower` rather than by reasoning about what each ought to need. An `AISO` chip is set off by
any of its four inputs, so three of its levers were noise.

`ChipSetup` fills in lines 3 and 4 only where the value was read off the chip's own source. A wrong
line there is worse than a blank one: a chip configured with grammar it does not accept reads
exactly like a chip that is broken. Most of the catalogue is deliberately left blank, and only the
chips wanting a file an operator supplies are reported as unfinished.

## The pipes

The fork carried two ways of moving items along a run of blocks. `Pipes` was its port of upstream's:
glass carries, panes let a run cross itself, stained glass keeps to its own colour, and a piston at
the end fills whatever it points at. `MegaPipes` was a fresh attempt at the same thing built for
scale, where panes are the pipe, a piston with an `[Extractor]` sign is the head, and any container
the run touches is somewhere items may go.

Both are built in the world, so both grammars are accepted, and there is one implementation under
them. What differs is in `PipeStyle`, which is chosen by the block a run **starts** from and then
decides how every block along that run is read. That matters because the two disagree about two
blocks: a pane carries items in one and merely keeps them going straight in the other, and a piston
starts a run in one and ends it in the other. Reading the meaning off the input rather than off the
block means a single pane can be part of a pane pipe and a crossing in a glass pipe without the two
ever contradicting each other.

`core/pipe/` holds the seam (`PipeWorld`), the two grammars (`PipeStyle`), the filter (`PipeFilter`)
and the tracer (`Pipes`); `paper/pipe/` binds them and `PipeDispatcher` is the single entry point.
`docs/pipes.md` is the builder's guide to all of it.

The `[Extractor]` sign is what makes a piston the head of a pane pipe, and it is required, because a
glass pipe's own ways out are plain pistons and they are powered every time that pipe runs. Filters
are `[Pipe]` whichever way the pipe was built, since a filter is a filter.

**Nearest first.** A pipe is followed outward a step at a time, so the closest way out is tried
first. Neither of the mechanics this replaces chose that way — both followed one branch to its end
before looking at the next, so which chest received a stack depended on the order the glass had been
placed in.

**What is remembered is an answer, not a picture.** Following a pipe reads every block of it, and a
pipe is asked to carry something every time it is powered, so `PipeNetworks` keeps what each one was
found to reach. Nothing is kept up to date as a pipe is built or broken: a block changing throws
away every answer that mentioned it, and the next pulse works it out again. A cache that can always
be discarded cannot drift, which is what made the legacy version's live graph go wrong, and it is
also what makes it safe on a regionised server — a pipe is a line of touching blocks and so belongs
to one region, and another region interfering can only ever cost a walk.

Two indexes point back at the answers so that letting one go is a lookup rather than a search: one
from each block to the pipes that mention it, one from each chunk to the same. A block changing
anywhere costs two lookups and, where nothing is indexed there, nothing else. The first index
carries a block's neighbours as well as the block itself, because a pane placed against the end of a
run lengthens it and nothing already inside the run has changed to say so.

The second index is what keeps the memory bounded: an answer is let go as its chunk unloads, and a
world's answers go with the world, so what is held is what is loaded rather than everything ever
powered.

Whole stacks travel. The domain decides where a stack goes and never looks at more of it than what
sort of thing it is, so the server moves the stack itself and everything done to it — its name, its
enchantments, its damage — survives the journey.

## The sign mechanics

A sign mechanic is a sign and the blocks around it. Unlike a chip it holds nothing between one use
and the next — the state of a bridge is whether its blocks are there — and unlike a cart mechanic
it has no block underneath saying what it is. The name in brackets on the second line is the whole
of the declaration.

`core/mechanic/` holds the seam: `SignMechanic` is the contract, `MechanicWorld` is the world as
one of them sees it, `MechanicVisit` carries everything one is given, and `Actor` is whoever set it
off — absent when redstone did. `paper/mechanic/` binds them and `MechanicDispatcher` is the single
entry point, in the same way `CartDispatcher` is for the rails. Do not give a mechanic its own
listener.

Five are built. Bridge, Door and Gate all put blocks up and take them down, paying into and out of
the chests near their sign, or out of nothing at all where the first line reads `ADMIN` and the
builder had the permission for it. Elevator builds nothing and carries people instead. ToggleArea
has no shape of its own at all: it swaps a whole saved region in and out.

Three things set one off, and all three come through the dispatcher: a hand on its own sign, a hand
on something standing in for that sign — a button in front of a lift, a fence a clickable gate is
made of — and redstone arriving beside it. Redstone drives rather than toggles: power arriving
shuts a mechanic and power leaving opens it, so a lever and the thing it drives always agree.

The sign names are frozen along with everything else on a sign. They are `[Bridge]` and
`[Bridge End]`; `[Door Up]`, `[Door Down]` and `[Door]`; `[Lift Up]`, `[Lift Down]`, `[Lift]` and
`[Lift UpDown]`; `[Area]` and `[SaveArea]`; and the gate's eight — `[Gate]`, `[DGate]`, and the
glass, iron and nether forms of each — every one of which may also carry a trailing `C` for a gate
that answers to a hand on its own fence.

A mechanic may check a sign as it is written, through `SignMechanic#review`, and either keep it in
whatever spelling it wants or refuse it with a reason. Only the toggled area needs to: what it
names lives on disk rather than in the blocks beside the sign, so a sign naming an area nobody has
saved would otherwise be silently dead.

Bridges and doors take their limits from the settings the building chips already use, because they
are the same limits: `ics.max-width`, `ics.max-length` and `ics.placeable-blocks` in `config.yml`.
`mechanics.yml` carries only what is peculiar to each — what a gate may be made of, how far it
looks, how the lifts are worked, and how large and how many the saved areas may be — under a
section named after the mechanic.

Whether a mechanic runs at all is one question with two halves, and `Settings#runsMechanicIn` is
where it is asked: the world has to allow it and the mechanic has to be switched on. Every mechanic
goes through it, sign or no sign. Half of them used to ask only the second half, so `disabled-worlds`
reached the bridges and the gates but not the bounce blocks or the copiers, which is exactly the
kind of half-working setting an operator has no way to notice.

## The loppers

Felling a tree and mining a seam are one mechanic twice. `core/lopper/` holds it: `LopperRules` is
what a run is allowed to take, `Loppers` is the run, and `LopperSight` is as much of the world as a
run needs to see — two questions, neither of which changes anything. `TreeLopper` is those rules
pointed at logs and held in an axe; `VeinMiner` is the same pointed at ores and held in a pickaxe.
`paper/lopper/LopperListener` is the one listener that asks both.

`VeinMiner` is **new** rather than ported, so nothing about it is frozen. It was written to the
tree lopper's shape on purpose: a builder who has learnt one has learnt both, and an operator
reading one section of `mechanics.yml` can read the other.

Three decisions are worth keeping.

**A run follows the block that was broken, not the list.** The list decides whether the mechanic
engages at all; what is then followed is the exact block struck, so felling an oak leaves the spruce
against it. `any-listed-block` asks for the other behaviour and exists for one case: an ore seam
crossing from stone into deepslate, which the game names as two different blocks.

**Nearest first**, as the pipes are and for the same reason. A limit reached partway takes the
blocks closest to the hand, so what is left standing is the far end rather than whichever branch
happened to be walked first. That only shows at the limit, which is exactly when somebody is
watching.

**The decision is made in full before a block is broken.** `Loppers.reach` reads block names and
returns a list; nothing it touches can change under it, and the whole thing is exercised against a
world written in a test. What the listener then does — drops, tool wear, replanting — is the
server's own `breakNaturally` plus `damageItemStack`, so none of it is this plugin's idea of what
the game does.

The fork destroyed the tree rather than felling it, and said in its own source that this was a
placeholder; see finding 137.

## The saved areas

A toggled area is a piece of the world put away and brought back, so unlike every other mechanic
it keeps its blocks somewhere other than the world. That somewhere is the game's own structure
format, reached through `org.bukkit.structure` — the same files a structure block writes, and no
world editor anywhere in the plugin.

Each area is two files in `areas/<namespace>/`: an `.nbt` holding the blocks and an `.anchor`
saying which world they came out of and where in it. The place is kept outside the structure
because the structure format does not record one: a structure is meant to be placed anywhere and a
toggled area belongs in exactly one spot. Keeping them apart also means an area small enough for
one can be opened in a structure block, and a structure built elsewhere can be dropped in with an
anchor written by hand.

Blocks only, deliberately. Putting an area up spawns whatever it holds and taking it down clears
blocks, so an area carrying entities would leave a fresh set of item frames behind on every toggle.

A namespace is a player's name or `GLOBAL`, and it is what is written on the sign rather than
anything kept beside it. The legacy fork showed the name and stored the owner's UUID in block data
the player could not see; one readable thing that a builder and an operator can both act on is
worth more than surviving a rename.

`/area pos1` and `/area pos2` pick out the block being looked at, which is what stands in for a
world editor's selection. `/area save`, `delete` and `list` do the rest.

## The firework shows

A display is a script in `fireworks/`, named on line 3 of an `MC1253` sign. Two spellings are
accepted and the extension picks between them: `.txt` is one launch to a line, `.fwk` builds
effects by name and then fires them, and only the second can play a sound or send up several
rockets at once. Both are parsed by `FireworkShow` into a flat list of launches, sounds and waits,
which is what lets `FireworkDisplay` hand the tail of a show to the region's scheduler at every
wait rather than blocking on it.

**Four shows ship in the jar** and are written out when the folder is first made: `finale`,
`aurora`, `victory` and `heartbeat`. A new server has something to wire a chip to before anybody
has written one, and — because the parser skips what it cannot read rather than refusing it — a
worked example is the only practical documentation of the grammar.

They are written **only as the folder is created**, never per file. An operator's edit therefore
survives, and a show they deleted stays deleted; the cost is that a show added in a later version
does not reach a server that already has the folder. That is the right way round, because a file
reappearing after somebody removed it is worse than an example nobody asked for going missing.

`BundledFireworkShowsTest` reads each of them the way the unpacker does and checks it survived
parsing. That test exists because the failure mode here is silent: a typo in a shipped script makes
a quieter display, not an error, and nothing else would ever say so.

`/craftbook reload` rereads the scripts, because writing a display is an edit-and-look-at-it
business and a restart between attempts makes it a different and much worse one. The songs are
deliberately not reread with them: a folder of MIDI files is far too slow to convert on the thread
the command runs on, which is the same reason they are read once at start-up in the first place.

`docs/fireworks.md` is the builder's guide to all of it.

## Debugging a chip

A chip that does nothing looks exactly like a chip that is wired wrong, and a builder cannot tell
which from the blocks. The tools answer that by printing what the **plugin** believes: which pins it
reads as wired, what each is carrying, which model the sign actually resolved to, and whether
anything will ever set the chip off. `ChipReport` in core is that answer as a value, so the stick,
the commands and the tests all say the same things about the same chip.

Two ways in, one implementation under them. `DebugActions` holds what each mode does; the stick
picks a mode from its own item data, a command names one, and the clickable menu offers them all.
None of the three knows which way it was reached. The commands act on the block you are looking at,
which is how `/area pos1` already works.

The stick keeps its mode **in the item**, not against the player: a stick can be handed over, left
in a chest, or carried in each hand set differently, and one found years later still works. It is
recognised by its persistent data and never by its name, so renaming it in an anvil does not break
it and naming an ordinary stick does not make one. There is **no crafting recipe**, unlike the fork's
three: permission is checked on use, so a recipe would mostly produce sticks that do nothing.

Two narrow seams exist purely for these tools, and no chip's behaviour depends on either.
`AreaAwareICLogic` says which box a chip works on, which the outline draws — `MCX116`, `MCX117`,
`MCX140`, `MCX130` and `MCX133` implement it. `BandAwareICLogic` says which wireless channel a chip
is on, implemented by the transmitter and the receiver, because the two ends of a pair cannot see
each other and a disagreement about the channel looks exactly like agreement.

The fork drew its area through **WorldEdit CUI**; `AreaOutline` draws particles instead, which keeps
the no-WorldEdit rule and works for a builder with no client mod. The fork's **debug message
subscription** was not ported: no chip here emits a commentary, so it would mean threading a
reporting seam through every one of them, and the report plus live pin state covers most of what it
was for.

`docs/debugging.md` is the builder's guide.

## Testing the part that touches the world

Most of this plugin is pure and is tested in `core` with no server at all — that is what the two
modules are for. What is left over is the half that reads and writes blocks: `ICManager` and the
listeners. None of it was covered, and a fault there was found by somebody standing in the game.

`paper` therefore carries **MockBukkit** (`org.mockbukkit.mockbukkit:mockbukkit-v26.2`, matching the
`paper-api` line exactly), and `ChipWorld` in the test sources is the whole harness: a mock server,
one world, and a method that hangs a wall sign on a block. Findings 130 and 132 were both written
before it existed and neither could be tested; both now have tests that were checked by reverting
the fix and watching them fail.

The test harness carries one workaround worth knowing about. Forcing a sign state through the mock
server resets the block to its default data, so a wall sign stops facing wherever it was put and
faces north instead. A chip reads its entire pin geometry off that facing, so `ChipWorld#write` puts
the block data back afterwards. Without it every sign in every test would quietly point the same way
and the geometry would never be exercised.

**Prefer `core` still.** Reach for the harness only where the thing under test genuinely needs a
block — and where a rule can be lifted out into `core` instead, lift it: `SignSupport` exists
because "which block holds a wall sign up" was worth stating once and testing directly rather than
reaching through a mock server to observe it.

Adding MockBukkit moved the whole project to **JUnit 6**, which is what it is built against.

## Folia and regions

Folia splits a world into regions that tick on separate threads, and a thread may only touch
blocks its own region owns. Three consequences shape the design.

**A single chip never spans regions.** Its pins sit within a few blocks of its sign, and Folia's
regionizer guarantees that everything within the merge radius of a loaded chunk belongs to the
same region as that chunk. Adjacent loaded chunks merge; regions only separate across a gap of
unloaded chunks, which contiguous redstone cannot have.

**A large build is one region, not many.** Every chunk of a connected redstone machine is loaded
and adjacent, so the whole thing merges into a single region on a single thread. Folia does not
make one big build faster; it stops that build from holding up the rest of the server.

**Action at a distance is the real cross-region case.** Wireless transmitters and receivers,
transporters and destinations, marquee and analog transmitters, teleporters and cart warps all
act on somewhere arbitrarily far away, which is very likely another region. None of them may
touch the far end directly.

Two ways of not touching it are in use, and the first is preferred where it fits.

*Publish and read.* Each end works out whatever the other needs from its own blocks, on its own
thread, and puts the answer somewhere shared as an immutable value. The far end reads that value
and nothing else. `Radio` and `Destinations` in `ChipServices` are both this: a transmitter
writes its band's state, a destination publishes a `Landing`, and neither ever looks at the
other's blocks. Where the work itself must happen elsewhere, the server's own region-crossing
API does the carrying — a transporter calls `Entity#teleportAsync` rather than moving anybody
itself.

*Say it rather than reach for it.* `Announcer` in `ChipServices` is how a chip addresses the
server rather than a place: everybody online, one player wherever they are, or the log. Only a
name and a piece of text cross, and both are immutable, so it is safe from any region's thread.
The chips that speak to whoever is standing near them do not go through it — those ask the world
who is there and tell each in turn, which is work in their own region.

*Hand the work over.* Where a value cannot stand in for the work,
`RegionSchedulers.executeAt(world, position, task)` runs it immediately when the caller already
owns that place and otherwise hands it to the region that does.

Where a chip needs an answer now and cannot wait for either, it asks and accepts that there may be
none: `ChipWorld.poweredAt` reports nothing at all for a place this thread may not read, and the
trigger reader leaves its output where it is rather than guessing. That is only safe because the
answer was advisory to begin with.

`ICManager.triggerAt` already routes through `executeAt`, so a chip only ever runs on the thread
owning its sign even if the invariant above is ever violated, or if a region splits mid-update.
