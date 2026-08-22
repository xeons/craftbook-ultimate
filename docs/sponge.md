# The Sponge build

CraftBook Ultimate is written as a domain model that knows nothing about a server (`core`) and a
set of bindings that do (`paper`). `sponge` is a second set of bindings, against **SpongeAPI 20**
on **SpongeVanilla for Minecraft 26.2**.

This page is what a developer or an operator needs to know about that build: which version it is
for, how it is put together, and — the part worth reading before anything else — what it cannot do
that the Paper build can.

## What runs

The chips. Writing a model reference on a wall sign makes a working chip, redstone drives it, and
the chips in a chunk come and go with the chunk. It reads the same `config.yml` as the Paper build,
through Configurate rather than Bukkit's YAML but against the same shared document, so an operator
moving a server between the two keeps their settings.

The commands that do not need a mechanic run too: `/craftbook` and its `ic`, `reload`, `check` and
`music` branches, `/var`, and the five switch commands.

**Watched working in game** on SpongeVanilla for Minecraft 26.2: a chip loading from a sign already
in the world, and redstone driving it. The second was the assumption everything else rested on,
because SpongeAPI has no redstone event and the listener stands `ChangeBlockEvent.Post` in for one.

Not yet bound here: the cart mechanics, the pipes, the sign mechanics, the toggled areas, the test
bed and the debugging tools, along with the commands that belong to them — those are absent rather
than registered and dead.

Before any of it will run, see [what a stock RC does](#the-entity-type-bug-in-spongevanilla-262):
an unpatched build cannot host a vanilla client at all, and it fails in a way that looks like this
plugin's fault.

## Where the jar goes

`mods/plugins/` — SpongeVanilla creates it at start-up and it is what the launch config calls the
additional plugins directory. `mods/` works just as well: both are scanned, identically, and which
one an operator uses is a matter of tidiness.

It genuinely does not matter for this plugin, which is worth saying because it could have. Reaching
into the game needs the jar on the **game module layer**, and what decides that is
`Candidate#gameResource()` — true as soon as a jar carries `META-INF/sponge_plugins.json`, with no
regard for where it was found. A plugin that quietly lost its old block spellings depending on which
folder it was dropped in would be a miserable thing to work out.

The settings file is written to `config/craftbookultimate/config.yml`, which is Sponge's own layout
rather than the plugin choosing one.

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

That is what the next section is about.

The module also applies **SpongeGradle**, which generates `META-INF/sponge_plugins.json` from the
`sponge { }` block — the plugin's id, its entrypoint and the API version it asks for, taken from
what was actually built against rather than typed out twice. On SpongeAPI 7 an annotation processor
did that job from `@Plugin`; it does not any more. The API's only processor now is the one that
checks an `@Listener` method is one the server could actually call. SpongeGradle also contributes a
`runServer` task, which is how this gets tried on a real server.

One consequence worth knowing, because it is not obvious and it bites twice: **SpongeGradle declares
repositories on the project**, and a Gradle project declaring any repository of its own ignores the
ones settled in `dependencyResolutionManagement`. So `sponge/build.gradle.kts` carries the whole
repository list rather than half of it. The other two modules still take theirs from the settings.

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

## The entity type bug in SpongeVanilla 26.2

**A stock SpongeVanilla `26.2-20.0.0-RC####` cannot host a vanilla client.** This is upstream's bug,
not this plugin's, but it presents as this plugin's: you join, and the client disconnects with

```
Invalid entity data item type for field 9 on entity Boat['Dark Oak Boat']:
  old=1(class java.lang.Integer), new=20.0(class java.lang.Float)
```

There is no boat. In a fresh world at cave depth there are mobs, and the client is building a boat
out of a packet the server sent for one of them. `20.0` is a creeper's health and `14.0` is an
axolotl's; on `LivingEntity` the float at id 9 is `DATA_HEALTH_ID`, while on a boat id 9 is
`VehicleEntity.DATA_ID_HURTDIR`, an integer defaulting to 1. Every entity type id is off by one.

The cause is where `sponge:human` gets registered. `EntityTypeMixin_Vanilla` injects
`SpongeEntityTypes.register(BuiltInRegistries.ENTITY_TYPE)` at `@At("TAIL")` of
`EntityType.<clinit>`, which was correct while `EntityType` held the type constants. Minecraft 26.2
moved all 158 of them into a new `net.minecraft.world.entity.EntityTypes` class, leaving
`EntityType.<clinit>` nine instructions long and registering nothing. So the injection now fires
*before* any vanilla type exists, `sponge:human` takes id 0, and everything after it shifts.

It fails silently because `EntityType` still has a `<clinit>` for the mixin to land in — no error,
no warning. Upstream had already guarded against exactly this failure mode in commit `1a07c8baa7`,
*"Ensure sponge:human is always registered last, to avoid holes in numeric ids from client pov"*;
the refactor defeated the guard without touching the code that implements it.

**The fix is to retarget the mixin** to `EntityTypes.class`, whose `<clinit>` ends with all 158
registrations done — three lines, counting the class rename and the entry in
`mixins.spongevanilla.core.json`. A sturdier anchor is to inject into `BuiltInRegistries.bootStrap()`
after `createContents()` and before `freeze()`, which is the semantic point rather than a class's
static initialiser and so survives Mojang moving constants around again.

Forge and NeoForge are unaffected. They hook `GameData.postRegisterEvents` after
`ModLoader.postEventWrapContainerInModOrder(...)` and filter on the registry key, so they anchor to
a lifecycle phase rather than to a class — and they sync registry ids to the client at login anyway.

Until an RC ships with this fixed, build SpongeVanilla from source with the change applied. Nothing
in this plugin can work around it: it happens before a plugin is consulted about anything.

## What is not a problem

Worth saying, because they would be the obvious worries:

- **Adventure** is native to Sponge. No conversion layer, no legacy colour codes.
- **Commands** are Sponge's own `Command.Parameterized` rather than Brigadier — SpongeAPI 20 does
  not expose Brigadier to plugins. What each command *does* was lifted into `core/command/` when
  this build gained them, so the two platforms say the same things in the same words and only the
  grammar differs. `Caller` is the whole seam: what to say back, what the caller may do, what they
  are called, and where they are standing.
- **Custom item data** for the debug stick works through `RegisterDataEvent` and a `DataStore`.
- **Holding a cart** is cleaner: Sponge's `MoveEntityEvent` is cancellable, unlike Bukkit's
  `VehicleMoveEvent`, so the "stop it dead instead" workaround is not forced.
