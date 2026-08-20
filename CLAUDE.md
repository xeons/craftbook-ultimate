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
folder. The extra fork's whole IC catalogue is done. Beyond the chips, the minecart mechanics all
run, and the sign mechanics with them: a bridge runs out across a gap, a door fills a doorway, a
gate drops from its lintel, a lift carries people between floors, and a whole saved region swaps
itself in and out — each answering to a hand and to redstone.**

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
| IC catalogue wiring (114 chips) | Done |
| IC instance lifecycle (`ICInstance`, `ICManager`) | Done |
| Listeners: sign creation, break, redstone, chunk load | Done |
| Self-triggering chips (per-region tick tasks) | Done |
| Time-based chips (clock, sensors, pulse, delays) | Done |
| Commands (Brigadier, `/craftbook` and the switch commands) | Done |
| Configuration (`config.yml`, `/craftbook reload`) | Done |
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
| Minecart mechanics (13 on the rails, plus the dispenser) | Done |
| Sign mechanic seam (`SignMechanic`, `MechanicWorld`, `MechanicVisit`) | Done |
| Bridge, door and gate, with the gate's six materials | Done |
| Lifts, including the two-way sign, buttons and jump pads | Done |
| Toggled areas, saved in the game's own structure format | Done |
| Mechanics other than those and the minecart ones | Not started |

Verified working: Gradle 9.7, JDK 25, `paper-api:26.2.build.112-stable`, Adventure 5.2.0,
**1683 tests passing**.

Remaining work is inventoried in `TODO.md`.

Run `./gradlew build` to compile, test, and produce
`paper/build/libs/CraftBookUltimate-<version>.jar`. That jar carries the core module's classes;
Adventure and JSpecify come from the server.

## Layout

```
core/    platform-independent domain model; no server API on its classpath
paper/   Paper 26.x bindings: plugin, schedulers, adapters, catalogue
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

`config.yml` in the plugin's folder, read into an immutable `Settings` value in `core`. Chips reach
it through `ChipState#settings()`; nothing reads a file or a server from inside a chip, so a limit
can be exercised in a plain unit test.

`Configuration` is the one mutable holder saying which `Settings` is current. `/craftbook reload`
replaces it and starts every chip again, so a change takes effect without a restart.

Two rules shape what belongs there. A setting is either a limit on how far a chip may reach or a
statement about what may run at all; nothing in the file changes what a sign *means*. And a sign
asking for more than it is allowed gets as much as it is allowed rather than being refused, so
narrowing a limit shortens an existing build instead of breaking it.

The settings that exist are the ones the legacy fork had, at the same defaults, so a world of
existing signs behaves the same on a server that has never been configured. Do not add a setting
for a limit a chip has always had on its own: those numbers are part of the frozen sign grammar.

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

**Not ported:** CartWarp, whose whole purpose was teleporting a cart to a CBWarp. CBWarps is on
the dropped list, so there is nothing for it to warp to.

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
are the same limits: `ics.max-width`, `ics.max-length` and `ics.placeable-blocks`. The `mechanics`
section carries only what is peculiar to these — what a gate may be made of, how far it looks, how
the lifts are worked, and how large and how many the saved areas may be.

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
