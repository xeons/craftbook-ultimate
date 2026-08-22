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

## What it cannot do

Eight things. Six are limits of SpongeAPI, one is a limit of the platform, and one is a gap in the
testing story.

### Weather illusions do not work

`MCX237` and the hidden-rain chip show one player weather the world is not having. Bukkit has
`Player#setPlayerWeather`. Sponge has no per-player weather, and — unlike Bukkit — no general
packet-sending API to build one out of: `Viewer` will send a player a block change, a particle, a
sound or a world type, and nothing else.

`Illusions` is already a seam in `core`, so the Sponge binding implements it as a chip that does
nothing rather than as a chip that is missing. A sign carrying one is still created and still
reads correctly; it just shows nobody anything.

### Legacy block spellings need a table generating first

Signs in the worlds this plugin is for name blocks as `35:14` or `WOOL:14`. That mapping is not
static data anywhere: Bukkit answers it by running the game's own **data fixers** over a 1.12
block tag. Sponge exposes no data fixer, and the ones its implementation uses internally are
server internals rather than API.

So the mapping is carried as data. On a Paper server, `/craftbook legacytable` writes
`legacy-blocks.properties` into the plugin's folder; that file goes into the Sponge jar's
resources, and `LegacyBlocks` reads it. It is derived from the game's own answer rather than from
anybody's recollection of what `35:14` used to be, which is the one thing here that must not be
guessed — a wrong entry does not fail, it quietly builds the wrong block.

**Without the table, modern block names resolve and legacy spellings do not.** That is deliberate:
a sign that cannot be read reports as unreadable rather than as something else.

### Redstone is watched differently

Bukkit has `BlockRedstoneEvent`, which fires when a block's power level changes. Sponge has
nothing equivalent, and this was the question that decided whether the port was viable at all.

It is: `ChangeBlockEvent.Post` carries every block transaction that actually happened, and a chip's
pin positions are known, so the pins are held in a position-indexed map and each `Post` is a lookup
rather than a search. Comparing the original and final states of the transaction gives the same
"was it powered, is it powered now" answer `BlockRedstoneEvent` gave directly.

This is the shape the Sponge fork this codebase was ported from already used, which is some
evidence it holds up under a real redstone load.

### Toggled areas are stored in a different format

`ToggleArea` puts a piece of the world away and brings it back. On Paper that is the game's own
structure format through `org.bukkit.structure` — the same files a structure block writes. Sponge
has no structure API; it has its own **schematic** API instead.

The mechanic works either way, but the files do not travel: an area saved on Paper will not load
on Sponge or the reverse, and the Sponge form cannot be opened in a structure block. The `.anchor`
file beside it is the plugin's own and is the same on both.

### The harvester pays out differently

`ChipWorld#dropsAt` asks what breaking a block would give. Bukkit answers it; Sponge has no such
query at all. What the Sponge binding answers instead is the block's own item form, which is right
for everything the harvester deals in except where a plant yields something other than itself — a
wheat crop being the case that differs.

The alternative was a table of remembered yields, and that was rejected on the same grounds as the
legacy block spellings: a wrong number there does not fail, it quietly pays a builder the wrong
amount. An approximation that is visibly an approximation is better than a table nobody can check.

### The planter looks before it leaps rather less

`ChipWorld#canPlace` asks whether a block would survive where it is going. Bukkit asks the game;
Sponge has nothing equivalent, so what the binding answers is the part that can be known — whether
the place is clear.

Only the planter and the area planter ask. The difference shows as a crop planted on the wrong
block popping off immediately rather than never being planted, which is what a player doing it by
hand would see too.

### Regions are not a thing

The Paper build is written for Folia: region schedulers, no cross-region reach, no assumption of a
single main thread. SpongeVanilla ticks every world on one server thread, so `ServerSchedulers`
collapses onto the server scheduler and `ownsCurrentThread` is "are we on the server thread".

Nothing breaks. The care taken over action at a distance — `Radio`, `Destinations`, `Announcer`
publishing values rather than reaching across — costs nothing here and is kept, because it is
also just a cleaner way to write those chips.

### The world-touching tests do not run there

`paper` carries MockBukkit, and `ChipWorld` in its test sources is what lets `ICManager` and the
listeners be tested against real blocks. There is no Sponge equivalent of MockBukkit.

Core's tests are unaffected — they are the large majority, and they are where the behaviour lives.
What is not covered on the Sponge side is the binding layer, which is exactly the layer that is
new. Prefer `core` even harder here than the main guidance already says: a rule that can be lifted
out into `core` gets tested on both platforms for free.

## What is not a problem

Worth saying, because they would be the obvious worries:

- **Adventure** is native to Sponge. No conversion layer, no legacy colour codes.
- **Commands** are Sponge's own `Command.Parameterized` rather than Brigadier — SpongeAPI 20 does
  not expose Brigadier to plugins — but everything the Paper commands do has a counterpart.
- **Custom item data** for the debug stick works through `RegisterDataEvent` and a `DataStore`.
- **Holding a cart** is cleaner: Sponge's `MoveEntityEvent` is cancellable, unlike Bukkit's
  `VehicleMoveEvent`, so the "stop it dead instead" workaround is not forced.
