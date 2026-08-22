# CraftBook Ultimate

Redstone integrated circuits, minecart mechanics, pipes and sign mechanics for **Paper 26.x** on
**Java 25**, with a **SpongeVanilla** build alongside it.

Write `[MC1000]` on a wall sign and the block behind it becomes a repeater you can wire into. Write
`[Bridge]` and a walkway runs out across a gap when somebody throws a lever. Put a sign under a
piece of rail and carts stop there, sort themselves, get lifted, launched, emptied and sent on.

This is a ground-up rewrite. It is not a fork, and it shares no code with the projects it descends
from — see [Where this comes from](#where-this-comes-from), which is the part of this file worth
reading first.

## Status

In development, and not yet released.

117 chips are wired and working under 146 model numbers, along with the minecart mechanics, both
pipe grammars, and the bridge, door, gate, lift and toggled-area sign mechanics. **2010 tests
pass.** What is left is inventoried in [TODO.md](TODO.md).

The **Paper build is the complete one.** The Sponge build shares the same domain model and the same
`config.yml`, and the chips run on it — but the commands, the cart mechanics, the pipes, the sign
mechanics and the debugging tools are not bound there yet, and none of it has been run on a server.
[docs/sponge.md](docs/sponge.md) says what is there, what is not, and what it will never do.

## Where this comes from

**CraftBook** was written by **[sk89q](http://www.sk89q.com)**, **[me4502](http://www.me4502.com)**
and its contributors, and is maintained by [EngineHub](https://enginehub.org). It has been going
since 2010 and it is the reason any of this exists — the idea that a sign on a wall is a redstone
component, the model numbers, the pin layouts, the sign grammar and very nearly every mechanic here
are theirs. It is licensed **GPL-3.0-or-later**.

- <https://github.com/EngineHub/CraftBook>

**craftbook-extra** is a fork of it for Sponge 7.3 / Minecraft 1.12.2, likewise
**GPL-3.0-or-later**, which added a substantial catalogue of its own — around sixty chips found
nowhere else, a pipes rewrite built for scale, and a good deal besides.

- <https://gitlab.com/minecraftonline/craftbook-extra>

Its files keep CraftBook's copyright header, and its own authors are credited in `@author` tags:

| Contributor | What they wrote |
| --- | --- |
| **Brendan** (doublehelix457) | The bulk of it. Entity chips, the sensors, block placers, the weather chips. |
| **tyhdefu** | Entity chips, the sensors, and the minecart mechanics. |
| **Fuzbol** | Potion Area, and the item and player sensors. |
| **Chris Hawthorne** (76x) | Lightning, and Message All. |

Those tags credit 78 of its 278 files, so that list is certainly incomplete — MegaPipes, the
tweakers and the command handling carry no attribution at all, and whoever wrote them is owed the
same acknowledgement.

The fork also marks the older CraftBook authors whose work it carried forward, tagged `(Legacy)`.
They predate the codebases linked above and would otherwise be invisible here, so they are worth
naming: **sk89q**, **Drathus**, **Shaun "sturmeh"**, **Lymia**, **Stefan Steinheimer** (nosefish)
and **Tom** (tmhrtly).

### What this project took, and what it did not

**It took the design.** Both codebases were read closely to learn what each mechanic actually does —
what a Cart Sorter does with a filter it cannot parse, which pin an AISO chip reads first, how far a
Mob Zapper reaches. That understanding is the whole substance of the work and it came from them.

**It did not take the code.** Every line here was written fresh, in a new package
(`com.xeonproductions.craftbookultimate`), against a different server API, in a different language
generation. Neither original tree is in this repository or in anything it builds. Nothing in
`core/`, `paper/` or `sponge/` is copied, adapted or transcribed from either. Both are read from
checkouts kept outside it.

That holds for the Sponge build too, which is worth saying plainly given the fork was itself a
Sponge plugin. It was written against SpongeAPI 7 and is a decade of API removed from SpongeAPI 20;
it was read for behaviour, like everything else, and nothing was carried across.

**The sign format is deliberately identical**, and that is the point rather than an oversight.
Existing worlds are full of signs. `[MC1000]`, `#north*`, `sci+:stone:4` and `[Lift UpDown]` all
mean exactly what they have always meant, including 1.12-era block spellings like `35:14`, because a
rewrite that quietly broke a decade of builds would be worth nothing. Model numbers and grammar are
compatibility facts, and they were looked up rather than invented.

**Bugs were fixed rather than reproduced.** Where the original did something wrong, this does not,
and each case is written down in [FINDINGS.md](FINDINGS.md) with what the old code did and why it
was wrong — over a hundred and thirty of them so far. That file is also the clearest record of how
carefully the source material was read.

### License

**GPL-3.0-or-later**, the same as both projects above, and for the obvious reason: this stands
entirely on their design work, and it would be poor form to take a permissive license off the back
of it. See [LICENSE](LICENSE).

## Building

```
./gradlew build
```

Produces `paper/build/libs/CraftBookUltimate-<version>.jar`, which is the one to install. Needs
**JDK 25**; the Gradle wrapper handles the rest. Adventure and JSpecify come from the server rather
than being shaded in.

The same command also builds `sponge/build/libs/CraftBookUltimate-Sponge-<version>.jar`, which is
not yet a working plugin. That module additionally downloads Minecraft through VanillaGradle the
first time it is built, so the first build takes rather longer than the rest.

## Layout

```
core/    platform-independent domain model; no server API on its classpath
paper/   Paper 26.x bindings: plugin, schedulers, adapters, listeners
sponge/  SpongeVanilla bindings, against SpongeAPI 20
docs/    what a builder reads
```

`core` depends on Adventure and JSpecify and nothing else. That is deliberate: chip logic, sign
parsing, pin geometry, the cart filter grammar and the chip catalogue are pure functions and pure
data there, so they are exercised in plain JUnit with no server running. Anything needing a world,
a scheduler or an event lives in one of the two binding modules.

Where the line falls is a decision that gets revisited. A rule that can be stated without a server
is worth stating in `core` even when only one platform currently needs it: it gets tested properly
there, and it gets tested once rather than twice. The chip catalogue, the file stores, the debug
modes and the block key all began life under `paper/` and moved when a second platform made the
duplication obvious.

### Two platforms, one domain

`sponge` does not depend on the `core` project — it compiles the same sources itself. SpongeAPI 20
is built against Adventure 4.26.1 where Paper 26.2 ships Adventure 5.2.0, and while `core` is
source-compatible with both, a class file is compiled against one of them. Compiling twice is
plainer than relocating a text library through every seam in the plugin. The consequence worth
knowing: **an Adventure 5-only API used anywhere in `core` breaks the Sponge build**, at compile
time, which is the right time.

The Paper build is **Folia-compatible** — region schedulers throughout, and no mechanic reaches
across a region boundary. SpongeVanilla ticks every world on one thread, so there the schedulers
collapse to the server's own; the care taken over action at a distance costs nothing either way and
is kept, because it is also just a cleaner way to write those chips.

## Documentation

| Page | What it covers |
| --- | --- |
| [docs/ics.md](docs/ics.md) | Every chip, its model number, wiring and what its sign lines mean. Generated from the catalogue; do not edit by hand. |
| [docs/pipes.md](docs/pipes.md) | Moving items along a run of blocks, in both grammars. |
| [docs/variables.md](docs/variables.md) | Named numbers the server shares, and the three chips that read them. |
| [docs/fireworks.md](docs/fireworks.md) | Writing a firework display script. |
| [docs/testbed.md](docs/testbed.md) | The generated plane carrying a working rig for every chip. |
| [docs/debugging.md](docs/debugging.md) | The debug stick and commands, for when a chip does nothing. |
| [docs/sponge.md](docs/sponge.md) | The SpongeVanilla build: which version, how it is put together, and what it cannot do. |

## Contributing

Not yet — the architecture is still moving. [CLAUDE.md](CLAUDE.md) documents the rules the rewrite
follows and is the place to start if you want to know why something is built the way it is.
