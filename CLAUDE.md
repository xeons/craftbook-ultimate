# CraftBook Ultimate

A ground-up rewrite of CraftBook for **Paper 26.x** on **Java 25**, in the namespace
`com.xeonproductions.craftbookultimate`.

## Project status

**Phase: chips run in game. Writing a recognised model reference on a wall sign creates a
working chip that responds to redstone. Only the chips that need nothing from the world beyond
redstone exist so far; the mechanics have not been started.**

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
| IC catalogue wiring (27 chips) | Done |
| IC instance lifecycle (`ICInstance`, `ICManager`) | Done |
| Listeners: sign creation, break, redstone, chunk load | Done |
| Self-triggering chips (per-region tick tasks) | Done |
| Time-based chips (clock, pulse, delays) | Not started |
| Commands, configuration, persistence | Not started |
| World-affecting ICs | Not started |
| Mechanics | Not started |

Verified working: Gradle 9.7, JDK 25, `paper-api:26.2.build.112-stable`, Adventure 5.2.0,
**378 tests passing**.

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

## Rules for this rewrite

1. **The sign text format is frozen.** Existing worlds are full of signs. `ICLine` is the
   contract; changing what it accepts breaks live builds. Mechanic behaviour is likewise
   expected to match.
2. **Everything else may change.** No backwards compatibility is owed to the old APIs, config
   format, or storage.
3. **No MinecraftOnline references** anywhere in the new code.
4. **Prefer Paper API over Bukkit API**, including experimental Paper API.
5. **Adventure for all text.** No legacy colour codes, no `ChatColor`.
6. **Folia compatible** where possible: region schedulers, no cross-region reach, no assumption
   of a single main thread.
7. **Fix bugs rather than reproduce them.** Record each one in `FINDINGS.md` with what the old
   code did and why it was wrong. Do not leave that history as comments in the new source.
8. **Comments describe behaviour**, not the rewrite's own history.
9. **Merge duplicated behaviour.** Where two ICs do the same thing, write one and register it
   under the **craftbook-extra number**, keeping the retired number as an alias.

## Scope decisions

Dropped: CBWarps, ChunkAnchor, Pastebin report upload, vendored bStats, PLC/Perlstone (MC5000),
ROM Get/Set (MC2300/MC3300).

Kept despite vanilla equivalents: MapCopier, BannerCopier, BookCopier, the powerable blocks,
BounceBlocks, XPStorer, Ammeter, LightStone.

ComplexArea is reimplemented with a self-contained storage format; **no WorldEdit dependency**
anywhere in the plugin.

## Conventions

- Records for value types, sealed hierarchies where a closed set is meant.
- `@NullMarked` at type level; JSpecify annotations, not JetBrains or JSR-305.
- Tests use JUnit 5 with `@Nested` groupings and AssertJ. Test names read as sentences.
- One behaviour per test; assert on outcomes, not on how they were reached.
