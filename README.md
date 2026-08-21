# CraftBook Ultimate

Redstone integrated circuits, minecart mechanics, pipes and sign mechanics for **Paper 26.x** on
**Java 25**.

Write `[MC1000]` on a wall sign and the block behind it becomes a repeater you can wire into. Write
`[Bridge]` and a walkway runs out across a gap when somebody throws a lever. Put a sign under a
piece of rail and carts stop there, sort themselves, get lifted, launched, emptied and sent on.

This is a ground-up rewrite. It is not a fork, and it shares no code with the projects it descends
from — see [Where this comes from](#where-this-comes-from), which is the part of this file worth
reading first.

## Status

In development, and not yet released.

117 chips are wired and working, along with the minecart mechanics, both pipe grammars, and the
bridge, door, gate, lift and toggled-area sign mechanics. **2010 tests pass.** What is left is
inventoried in [TODO.md](TODO.md).

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
`core/` or `paper/` is copied, adapted or transcribed from either.

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

Produces `paper/build/libs/CraftBookUltimate-<version>.jar`. Needs **JDK 25**; the Gradle wrapper
handles the rest. Adventure and JSpecify come from the server rather than being shaded in.

## Layout

```
core/    platform-independent domain model; no server API on its classpath
paper/   Paper 26.x bindings: plugin, schedulers, adapters, catalogue
docs/    what a builder reads
```

`core` depends on Adventure and JSpecify and nothing else. That is deliberate: chip logic, sign
parsing, pin geometry and the cart filter grammar are pure functions there, so they are exercised in
plain JUnit with no server running. Anything needing a `World`, a scheduler or an event lives in
`paper`.

The plugin is Folia-compatible — region schedulers throughout, and no mechanic reaches across a
region boundary.

## Documentation

| Page | What it covers |
| --- | --- |
| [docs/ics.md](docs/ics.md) | Every chip, its model number, wiring and what its sign lines mean. Generated from the catalogue; do not edit by hand. |
| [docs/pipes.md](docs/pipes.md) | Moving items along a run of blocks, in both grammars. |
| [docs/variables.md](docs/variables.md) | Named numbers the server shares, and the three chips that read them. |
| [docs/fireworks.md](docs/fireworks.md) | Writing a firework display script. |
| [docs/testbed.md](docs/testbed.md) | The generated plane carrying a working rig for every chip. |
| [docs/debugging.md](docs/debugging.md) | The debug stick and commands, for when a chip does nothing. |

## Contributing

Not yet — the architecture is still moving. [CLAUDE.md](CLAUDE.md) documents the rules the rewrite
follows and is the place to start if you want to know why something is built the way it is.
