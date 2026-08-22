# The mechanics file

A **mechanic** is something built in the world rather than written on an IC sign: a bridge that runs
out across a gap, a gate that drops from its lintel, a lift, a block that answers redstone, a sign
that hands out copies of a book. What an operator may say about them lives in `mechanics.yml`, in
the plugin's own folder, beside `config.yml`.

It is written for you the first time the plugin starts, complete and explained. Nothing you have
already put in it is ever overwritten — a version that adds a setting adds it at its default and
leaves the rest alone — and the explanations are rewritten every time, so a wording improved later
reaches a server that has been running since before it.

Changes take effect on the next `/craftbook reload`. No restart.

## Everything starts switched off

**A brand new server runs no mechanics at all.** Drop the plugin in and no bridge extends, no gate
drops, no stair seats anybody and no head is left behind. You turn on the ones you want, one at a
time, and nothing else changes.

That is on purpose, and it is the opposite of how the chips work. A chip does nothing until a
builder writes its sign, so having the whole catalogue available costs a server nothing. A mechanic
has no such sign to wait for — the blocks it answers to are ordinary blocks that are already in
your world. Turning `Chairs` on makes every stair on the server a seat; turning `HeadDrops` on
changes what every death leaves behind; turning `GlowStone` on changes what a piece of glowstone
does when redstone reaches it. None of those is something a builder asked for, so none of them
happens until you say so.

The console tells you where you stand every time the server starts:

```
[CraftBookUltimate] No mechanics are switched on. Every one of them starts off, since a mechanic
changes what blocks already in the world do. Set enabled to true in mechanics.yml for the ones you
want, then run /craftbook reload.
```

and, once you have turned some on:

```
[CraftBookUltimate] 4 of 21 mechanics are switched on: Bridge, Door, Elevator, Gate
```

## The shape of it

One section per mechanic, named after the mechanic:

```yaml
Gate:
  enabled: false
  blocks:
  - minecraft:oak_fence
  - minecraft:iron_bars
  radius: 5
  clicking: true
```

**Every mechanic has a section**, including the ones with nothing to configure, so the file also
answers what there is:

```yaml
Bridge:
  enabled: false
```

**Switching a mechanic on is its own section saying so.** There is no list of enabled names
anywhere else — the switch sits at the top of the settings it governs, so a mechanic that is doing
nothing and a mechanic whose settings are being ignored are the same line to look at:

```yaml
Gate:
  enabled: true
```

Switching one back off leaves everything already built exactly where it is. A gate switched off is
a gate that no longer opens, not a gate that has been taken down, and switching it on again picks
up where it left off.

A mechanic the file does not mention at all stays off. That covers a file you have trimmed by hand
and a mechanic added by a version newer than your file: neither starts working on its own.

The name in the file is **not** the name on the sign. A builder writes `[BannerCopier]` on a sign;
you write `BannerCopier` in this file. The brackets belong only to the sign. Case does not matter —
`gate`, `Gate` and `GATE` all mean the same section.

## The two rules that belong to no mechanic

They sit at the top of the file, outside every section.

```yaml
redstone: true
depower-on-source-removal: false
```

`redstone` is whether power arriving beside a mechanic's sign works it. It **drives** rather than
toggles: power arriving shuts a mechanic and power leaving opens it, so a lever and the thing it
drives always agree.

`depower-on-source-removal` is whether a powered block goes out when the redstone feeding it is
mined away. Off, and deliberately: powering a light and then mining the redstone is how a builder
makes one that stays on. Switching a lever off still turns it off either way — this is only about
the source being taken out of the world. `GlowStone`, `JackOLantern` and `Netherrack` all read it.

## What each mechanic has

| Mechanic | Settings of its own |
| --- | --- |
| `Ammeter` | `item` — what is held up to a block to read its redstone power |
| `Area` | `max-blocks`, `max-per-name` — how large and how many a saved area may be. Zero is no limit |
| `BannerCopier` | — |
| `BookCopier` | — |
| `BounceBlocks` | `blocks`, `automatic`, `sensitivity` |
| `Bridge` | — |
| `Chairs` | `blocks`, `require-sign`, `max-sign-distance`, `face-correct-direction`, `exit-at-last-position`, `heal-amount`, `heal-rate` |
| `Door` | — |
| `Elevator` | `jumping`, `buttons`, `tolerance` |
| `Gate` | `blocks`, `radius`, `clicking` |
| `GlowStone` | `off-block` — what a glowstone looks like while it is dark |
| `HeadDrops` | `player-heads`, `mob-heads`, `player-kills-only`, `drop-rate`, `looting-rate-modifier`, `show-name-on-click`, `ignored-names` |
| `JackOLantern` | — |
| `LightStone` | `item` — what is held up to a block to read its light level |
| `LightSwitch` | `range`, `max-lights` |
| `MapCopier` | — |
| `BetterPhysics` | `falling-ladders` — whether a ladder falls when what it stood on goes away |
| `DispenserRecipes` | `cannon`, `fan`, `vacuum`, `fire-arrows`, `snow-shooter`, `xp-shooter` |
| `HiddenSwitch` | `any-side` |
| `Marquee` | — |
| `Netherrack` | `fire-blocks` — what catches light on top of itself while powered |
| `SignCopier` | — |
| `Snow` | `piling`, `dispersion`, `freezes-water`, `melts-in-sunlight`, `partial-melt-only`, `snowballs-pile` |
| `Teleporter` | `buttons`, `require-sign`, `range` |
| `TreeLopper` | `blocks`, `tools`, `max-size`, `diagonals`, `any-listed-block`, `single-use`, `leaves`, `break-leaves`, `place-saplings` |
| `VeinMiner` | `blocks`, `tools`, `max-size`, `diagonals`, `any-listed-block`, `single-use` |
| `XPStorer` | `block`, `per-bottle`, `requires-bottle`, `sneaking` |

A mechanic with nothing in the second column has only `enabled`. That is not an oversight: a bridge
takes its limits from the chips (below), and a book copier has nothing to decide.

### Naming blocks

Anywhere a block is named — `Gate.blocks`, `Chairs.blocks`, `Netherrack.fire-blocks`,
`BounceBlocks.blocks`, `GlowStone.off-block`, `XPStorer.block` — three spellings are accepted:

- a modern name, `minecraft:oak_fence` or just `oak_fence`
- a tag, with a leading `#`, such as `#minecraft:fences`, expanded to whatever the server currently
  has in it
- a name from before the flattening, such as `35:14`

A name the server does not know is complained about in the console and skipped; the rest of the list
survives. A list where **nothing** could be read falls back to the default rather than leaving the
mechanic with nothing to work with.

### Chairs

Right-click a stair with an empty hand and you sit on it. Nothing is built, no sign is needed, and
the stair goes on being an ordinary stair for every other purpose — a chair is only a way of
looking at a block somebody put there anyway.

`blocks` ships as the tag `#minecraft:stairs` rather than the sixty-odd names in it, so a stair
added by a later version of the game becomes a chair without the file being touched. A stair laid
upside down is never a chair whatever the list says, and neither is one with a block on top of it:
that is a step in a staircase, and there is no room for a head.

`/sit` seats you where you are standing, on any block at all, which is what makes it useful on a
carpet or a slab the list does not mention. `/stand` gets you up again, as does the dismount key.
`/sittoggle` turns clicking-to-sit off **for yourself** — it is a preference, kept in your own
player data, not a permission an operator has to manage.

Writing `[Sit Heal]` on a sign hung on a chair makes it heal whoever sits in it, `heal-amount` at a
time, `heal-rate` ticks apart. That needs `craftbook.chairs.heal`, which is an operator permission
by default: a healing chair is a bed nobody can sleep through, and worth deciding about.

### Head drops

Kill something and it may leave its head. Like snow, this changes what every death in the world
does, so it is worth knowing what it costs before turning the rate up.

The game has a head of its own for exactly seven things — a player, a zombie, a creeper, a
skeleton, a wither skeleton, a dragon and a piglin — and those drop the real item. Every other
creature drops a **player head wearing its face**, because that is the only way the game has ever
had of showing a cow on a block. Those faces are not stored in the item: the server fetches each
one from Mojang the first time it hands one out and remembers it afterwards.

That last part is the one caveat. A server with no way out to the internet gets a blank head for a
cow, and there is nothing this can do about it. The seven the game knows about are unaffected.

`ignored-names` is for accounts whose head means something to another plugin. The one that ships
belongs to a library that uses a head as a marker in the world; handing it out would put a piece of
somebody else's furniture into a player's inventory.

The fork also re-dropped a head when its **block** was mined, because the game of the day dropped a
blank one and lost whose it was. The game has kept the face on a mined head since the flattening,
so that is not here and does not need to be.

### The tree lopper and the vein miner are one mechanic

Break a log with an axe and the whole trunk comes away. Break an ore with a pickaxe and the whole
seam does. They are the same code with a different list of blocks and a different list of tools, so
every setting means the same thing in both sections and a change to how one follows a run reaches
the other.

`VeinMiner` is **new here** rather than ported from either CraftBook, so nothing about it is fixed
by what an old world already contains. `TreeLopper` comes from both, and where they disagreed the
fork's behaviour was kept — except where the fork's was plainly a limitation of the API it was
written against. It dropped the felled logs entirely, which its own source notes as something to
fix once the platform allowed it; that is finding 134.

Three things are worth knowing before switching either on.

**A run follows the block that was broken, not the list.** The list says whether the mechanic
engages at all. Felling an oak leaves the spruce growing against it, and mining iron leaves the gold
beside it. `any-listed-block: true` is how you ask for the other behaviour, and its one good use is
an ore seam that crosses from stone into deepslate — the game gives those two halves different
names, so a seam on the boundary otherwise comes away in two swings.

**The tool wears out and the run stops with it.** Twenty logs cost an axe twenty points. An axe that
breaks partway leaves the rest of the tree standing. `single-use: true` makes the whole run cost one
point instead, which is a decision rather than something that falls out of how it is written.

**`max-size` is the safety valve, and it counts the block you struck.** A tree bigger than the limit
is felled as far as the limit, nearest blocks first, and the rest is still there to swing at again.
Setting it to zero switches the mechanic off without a second setting saying so. Turning
`break-leaves` on is what will actually spend that limit — a canopy is far more blocks than a
trunk.

Players can turn either off for themselves with `/treelopper toggle` and `/veinminer toggle`, which
matters because both change what an ordinary swing of an ordinary tool does. `/timber` is an alias
for the first, kept from the fork. The preference lives in the player's own data rather than in your
settings file.

### The hidden switch is the only sign you never click

Put an `[X]` sign on the **back** of a block and the levers or buttons touching that sign are what
get thrown. From the front there is a plain wall. Clicking the wall works whatever is behind it.

A key may be named on the sign's **first** line, and then the switch only answers to somebody
holding one, in either hand. That is upstream's spelling. The fork asked for the key through a chat
prompt and kept it in block data the builder could never see again, which is the arrangement the
toggled areas already rejected: a line anybody can read and anybody can change is worth more.

`any-side: false` means only the face opposite the sign works it, which is what you want for a
switch built into a wall. `true` reaches round the block, for a switch behind something a player can
walk around.

### The dispenser machines

Load a dispenser in one of six patterns and it does something other than dispense. Nothing is
crafted — the pattern stays where it is and one of every stack is taken, so a machine works until
one of its stacks runs out.

| Pattern | Middle | Sides | Corners |
| --- | --- | --- | --- |
| Cannon — throws lit TNT | TNT | gunpowder | fire charges |
| Fan — blows things away | piston | leaves | cobwebs |
| Vacuum — drags things closer | sticky piston | leaves | cobwebs |
| Fire arrows | arrow | fire charges | *empty* |
| Snow shooter | potion | snow blocks | *empty* |
| XP shooter | glass bottle | redstone | *empty* |

The empty corners have to actually be empty — a machine is nine slots and all nine are read.

The fan and the vacuum reach five blocks along the open air in front of the dispenser and weaken
with every block, so a wall stops the draught. That is the fork's behaviour; upstream's fan reaches
one block and pushes at a fixed strength, and upstream has no vacuum at all.

### The marquee is a readout, not a chaser

`[Marquee]` on a sign, a variable's name on line 3, and a namespace on line 4 if it is not the
shared one. Right-click it and it tells you what that variable says. A score beside a scoreboard, a
stock count on a shop wall, a countdown at a gate.

`/var get` answers the same question, but only for somebody who already knows the variable is there
and how it is spelt. The sign is a builder putting the answer where other people will find it.

The variable has to exist before the sign may name one, and a sign naming one nobody has made is
refused as it is written. That is the same rule the `VAR100`, `VAR170` and `VAR200` chips follow and
for the same reason: what a variable is called lives in the store rather than in the blocks beside
the sign, so a sign naming a missing one would be silently dead. A variable deleted *afterwards* is
not an error — a sign cannot be refused retrospectively — so the sign says so when it is clicked.

**Two different things are called a marquee**, and it is worth knowing which is which. `MC2999` and
`MC3456` are chasing lights: one output, or one wireless band, stepping along a row. This is the
other sense of the word, a board with something written on it, and it shares no code with either.

### Snow is not like the others

Snow is the one mechanic where `enabled: true` by itself still does nothing. Its six parts each
start off as well, so turning snow on is two decisions rather than one: `enabled` says the mechanic
runs at all, and the lines underneath say which of its behaviours it does.

The reason is that they are independent of one another — piling, slumping, freezing and melting are
four different changes to how snow behaves, and a server usually wants some and not others. Every
other mechanic is one thing you either want or do not.

## What is not here

**How wide a bridge or a door may be, how far it may run, and what it may be made of.** Those are
the same limits the building chips use, so they live under `ics` in `config.yml`:

```yaml
ics:
  max-width: 5
  max-length: 16
  placeable-blocks: []
```

Narrowing one shortens an existing bridge rather than breaking it. Taking a block off
`placeable-blocks` is never applied to *removing* a block, so a structure already made of it can
still retract instead of being stuck out.

**The minecart mechanics** are under `carts` in `config.yml`, and **the pipes** under `pipes`.
Neither is a sign mechanic: a cart mechanic is a block under a piece of rail, and a pipe is a run of
glass.

## Upgrading from an earlier version

The mechanic settings used to be under `mechanics` in `config.yml`, flat: `mechanics.gate-radius`,
`mechanics.lift-tolerance` and so on. They are not read from there any more.

`mechanics.yml` is written from the defaults on the first start after upgrading, and **anything you
had configured under `mechanics` in `config.yml` reverts.** If you had changed any of it, the old
keys are still sitting in `config.yml` where you left them — read them across into the new file, and
then delete the `mechanics` block, which does nothing now.

The names changed shape along with the file, so the old key is not always the obvious one:

| Was, in `config.yml` | Is, in `mechanics.yml` |
| --- | --- |
| `mechanics.disabled: [Gate]` | `Gate.enabled: false` — but see below |
| `mechanics.redstone` | `redstone` |
| `mechanics.depower-on-source-removal` | `depower-on-source-removal` |
| `mechanics.gate-blocks` | `Gate.blocks` |
| `mechanics.gate-radius` | `Gate.radius` |
| `mechanics.gate-clicking` | `Gate.clicking` |
| `mechanics.lift-jumping` | `Elevator.jumping` |
| `mechanics.lift-buttons` | `Elevator.buttons` |
| `mechanics.lift-tolerance` | `Elevator.tolerance` |
| `mechanics.max-area-blocks` | `Area.max-blocks` |
| `mechanics.max-areas-per-name` | `Area.max-per-name` |
| `mechanics.glowstone-off-block` | `GlowStone.off-block` |
| `mechanics.fire-blocks` | `Netherrack.fire-blocks` |
| `mechanics.light-switch-range` | `LightSwitch.range` |
| `mechanics.light-switch-max-lights` | `LightSwitch.max-lights` |
| `mechanics.light-stone-item` | `LightStone.item` |
| `mechanics.ammeter-item` | `Ammeter.item` |
| `mechanics.bounce-blocks` | `BounceBlocks.blocks` |
| `mechanics.auto-bounce-blocks` | `BounceBlocks.automatic` |
| `mechanics.bounce-sensitivity` | `BounceBlocks.sensitivity` |
| `mechanics.teleporter-buttons` | `Teleporter.buttons` |
| `mechanics.teleporter-require-sign` | `Teleporter.require-sign` |
| `mechanics.teleporter-range` | `Teleporter.range` |
| `mechanics.xp-storer-block` | `XPStorer.block` |
| `mechanics.xp-per-bottle` | `XPStorer.per-bottle` |
| `mechanics.xp-requires-bottle` | `XPStorer.requires-bottle` |
| `mechanics.xp-sneak-state` | `XPStorer.sneaking` |
| `mechanics.snow.*` | `Snow.*`, unchanged below the section |

Two further changes worth knowing about.

**The default flipped.** `mechanics.disabled` was a list of exceptions to everything running; the
`enabled` lines are the whole of what runs. Reading a `disabled` list across is therefore not a
translation of that row alone — set `enabled: true` on every mechanic you were **not** disabling,
or you will find the ones you never mentioned have gone quiet too.

**The copiers were switched off by their sign name before** — `[BannerCopier]`, brackets and all —
which almost nobody would have guessed. They are `BannerCopier`, `BookCopier` and `MapCopier` now,
like everything else.

## When it does not work

**A mechanic does nothing at all.** Check its section says `enabled: true` — that is the answer far
more often than not, because it is what every section says until you change it. Then check the two
switches in `config.yml` that stop everything at once: `enabled: false` at the top of it takes the
whole plugin out of service, and a world named under `disabled-worlds` runs no mechanic and no chip.
Both reach every mechanic here, not only the ones with signs.

**A tree lopper or a vein miner does nothing.** Four things, in order: the section says
`enabled: true`; you are holding something on its `tools` list; the block is on its `blocks` list;
and you have not turned it off for yourself with `/treelopper toggle`. Creative mode never fells or
mines, deliberately — a creative swing already takes the block it hit.

**A marquee sign was refused.** It names a variable nobody has made. `/var define <name> 0` first,
or `/var list` to see what there is.

**A change did nothing.** `/craftbook reload`. If it still did nothing, look in the console: an
entry that could not be understood is complained about by name as the file is read.

**A block you named is ignored.** The console says which one and why. The usual cause is a name the
server does not have — a mod block on a server without the mod, or a spelling that was right in an
older version.

**The whole file went back to defaults.** That means it could not be parsed as YAML at all. The
console says so, naming the file. YAML cares about indentation: two spaces, never tabs.

**Something in `mechanics.yml` had no effect, and you are on Sponge.** The Sponge build reads both
files exactly as the Paper build does, but the mechanics themselves are not bound there yet — see
[sponge.md](sponge.md). The settings are read and are simply not acted on.
