# The Sponge build

CraftBook Ultimate is written as a domain model that knows nothing about a server (`core`) and a
set of bindings that do (`paper`). `sponge` is a second set of bindings, against **SpongeAPI 20**
on **SpongeVanilla for Minecraft 26.2**.

This page is what a developer or an operator needs to know about that build: which version it is
for, how it is put together, and — the part worth reading before anything else — what it cannot do
that the Paper build can.

## Which version

Minecraft 26.2 is served by **SpongeAPI 20.0.0**. SpongeVanilla publishes it as
`26.2-20.0.0-RC####`. For context, 26.1.2 is API 19 and 26.3 snapshots are API 21.

**There is no stable release.** API 20 exists only as release candidates and as
`20.0.0-SNAPSHOT` on `repo.spongepowered.org`; the newest stable SpongeAPI is 17.0.0, for
Minecraft 1.21.10. The module therefore pins a moving target, which is a different proposition
from `paper-api:26.2.build.112-stable`, and `resolutionStrategy.cacheChangingModulesFor(0, ...)`
is set so that an upstream break shows up as a failed resolve rather than as a jar built against
something no server is running.

Sponge's published documentation is stale — its API-versions table still stops at 14.0.0 for
Minecraft 1.21.4 — so everything here was read off the SpongeAPI source and the download API
rather than off the website.

## How the module is put together

`sponge` does **not** depend on the `core` project. It compiles core's sources itself:

```kotlin
sourceSets {
    main {
        java.srcDir(core.file("src/main/java"))
    }
}
```

That looks redundant and is not. SpongeAPI 20 is built against **Adventure 4.26.1**; Paper 26.2
ships **Adventure 5.2.0**. Core is source-compatible with both — it was compiled clean against
4.26.1 to check — but a class file is compiled against one of them, and core's classes as the
`paper` module builds them carry Adventure 5 method references that no Sponge server can satisfy.
Compiling the same sources again against the Adventure the server actually has is cheaper and
plainer than shading and relocating a text library through every seam in the plugin.

The consequence to remember: **an Adventure 5-only API used anywhere in `core` breaks the Sponge
build.** It breaks it at compile time, which is the right time.

The module also compiles against Minecraft itself, through VanillaGradle:

```kotlin
minecraft {
    version("26.2")
    platform(MinecraftPlatform.SERVER)
}
```

That is what the next section is about. VanillaGradle is applied in `settings.gradle.kts` rather
than in the module, with `injectRepositories(false)`, because a project that declares any repository
of its own ignores the ones settled in `dependencyResolutionManagement` — which would lose Paper's
and Sponge's and break the other two modules.

## Reaching into the game

SpongeVanilla puts a plugin on the **game module layer**, under Mojang's own names, so plugin code
can call `net.minecraft` directly. Sponge builds its API into the game's classes rather than
wrapping them, so a Sponge `ServerWorld` *is* a `ServerLevel` and a Sponge `BlockState` *is* a
Minecraft one — crossing between them is a cast, not a conversion.

Four things SpongeAPI cannot answer are answered that way, in `sponge/game/`:

| Question | What is called |
| --- | --- |
| What would breaking this block give? | `Block.getDrops(state, level, pos, blockEntity)` |
| Would this block stay where it was put? | `BlockState#canSurvive(level, pos)` |
| Show one player weather the world is not having | `ClientboundGameEventPacket` to their connection |
| What is `35:14` called now? | `BlockStateData.getTag`, `ItemIdFix`, `ItemStackTheFlatteningFix` |

**None of these is a mixin.** A mixin changes what the game does; every one of these only asks it
something. Nothing here alters Minecraft's behaviour, and the jar carries no `MixinConfigs`
attribute, so an operator gets no warning about a plugin modifying their server.

`GameInternals` is the seam and the safety. Each method may answer nothing, every caller has
something sensible to do with nothing, and the whole layer stands down permanently the first time
the game refuses it — settled once at start-up by asking a question with a known answer (`35:14` is
red wool, and has been since the flattening). A version that moves these names degrades to the
fallbacks below rather than failing to start.

That is the trade being made deliberately: **the API is stable and the internals are not.** What is
reached for is small, read-only, and individually optional.

### The fallbacks, when the game will not answer

- **Drops** become the block's own item form — right except where a plant yields something other
  than itself.
- **Placement** becomes "is the place clear" — a crop on the wrong block pops off rather than never
  being planted, which is what a player doing it by hand sees too.
- **Weather illusions** show nobody anything, and say so, rather than pretending.
- **Legacy spellings** stop resolving, and there is deliberately nothing behind them. A table would
  have to be generated on a Paper server and baked into the jar, which is not a thing any operator
  would actually do; and a table of remembered values is exactly what must not exist here, because a
  wrong entry does not fail — it quietly builds the wrong block.

Every one of these degradations is visible rather than silent. That is the property being bought:
an approximation that reads as an approximation, or a sign that reads as unreadable.

### Mixins, if they are ever needed

The plumbing is there and deliberately switched off. Mixin is on the compile classpath, and the jar
task adds the `MixinConfigs` manifest attribute the moment a `mixins.*.json` appears in resources —
and not before, because that attribute makes SpongeVanilla warn the operator at every start-up, and
frightening somebody on behalf of an empty config is worse than not having one.

Adding the first mixin means adding the annotation processor alongside it, which writes the refmap
and drags Guava onto the processor path. That is left undone for the same reason: it has nothing to
process yet.

## What it still cannot do

Two things, and neither is about Minecraft.

### Regions are not a thing

The Paper build is written for Folia: region schedulers, no cross-region reach, no assumption of a
single main thread. SpongeVanilla ticks every world on one server thread, so `ServerSchedulers`
collapses onto the server scheduler and `ownsCurrentThread` is "are we on the server thread".

Nothing breaks. The care taken over action at a distance — `Radio`, `Destinations`, `Announcer`
publishing values rather than reaching across — costs nothing here and is kept, because it is also
just a cleaner way to write those chips.

### The world-touching tests do not run there

`paper` carries MockBukkit, and `ChipWorld` in its test sources is what lets `ICManager` and the
listeners be tested against real blocks. There is no Sponge equivalent of MockBukkit.

Core's tests are unaffected — they are the large majority, and they are where the behaviour lives.
What is not covered on the Sponge side is the binding layer, which is exactly the layer that is new.
Prefer `core` even harder here than the main guidance already says: a rule that can be lifted out
into `core` gets tested on both platforms for free.

### Toggled areas are stored in a different format

Not a limitation so much as an incompatibility. `ToggleArea` puts a piece of the world away and
brings it back; on Paper that is `org.bukkit.structure`, the same files a structure block writes.
Sponge has no structure API and its schematics are a different format.

The mechanic works either way, but the files do not travel: an area saved on Paper will not load on
Sponge or the reverse, and the Sponge form cannot be opened in a structure block. The `.anchor` file
beside it is the plugin's own and is the same on both.

### Redstone is watched differently

Also not a limitation — the design differs, and this was the question that decided whether the port
was viable at all.

Bukkit has `BlockRedstoneEvent`, which fires when a block's power level changes. Sponge has nothing
equivalent. But `ChangeBlockEvent.Post` carries every block transaction that actually happened, and
a chip's pin positions are known, so the pins are held in a position-indexed map and each `Post` is
a lookup rather than a search. Comparing the original and final states gives the same "was it
powered, is it powered now" answer `BlockRedstoneEvent` gave directly.

This is the shape the Sponge fork this codebase was ported from already used, which is some evidence
it holds up under a real redstone load.

## What is not a problem

Worth saying, because they would be the obvious worries:

- **Adventure** is native to Sponge. No conversion layer, no legacy colour codes.
- **Commands** are Sponge's own `Command.Parameterized` rather than Brigadier — SpongeAPI 20 does
  not expose Brigadier to plugins — but everything the Paper commands do has a counterpart.
- **Custom item data** for the debug stick works through `RegisterDataEvent` and a `DataStore`.
- **Holding a cart** is cleaner: Sponge's `MoveEntityEvent` is cancellable, unlike Bukkit's
  `VehicleMoveEvent`, so the "stop it dead instead" workaround is not forced.
