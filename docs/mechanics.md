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

## The shape of it

One section per mechanic, named after the mechanic:

```yaml
Gate:
  enabled: true
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
  enabled: true
```

**Switching a mechanic off is its own section saying so.** There is no list of disabled names
anywhere else:

```yaml
Gate:
  enabled: false
```

Everything already built stays exactly where it is. A gate switched off is a gate that no longer
opens, not a gate that has been taken down, and switching it back on picks up where it left off.

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
| `Door` | — |
| `Elevator` | `jumping`, `buttons`, `tolerance` |
| `Gate` | `blocks`, `radius`, `clicking` |
| `GlowStone` | `off-block` — what a glowstone looks like while it is dark |
| `JackOLantern` | — |
| `LightStone` | `item` — what is held up to a block to read its light level |
| `LightSwitch` | `range`, `max-lights` |
| `MapCopier` | — |
| `Netherrack` | `fire-blocks` — what catches light on top of itself while powered |
| `SignCopier` | — |
| `Snow` | `piling`, `dispersion`, `freezes-water`, `melts-in-sunlight`, `partial-melt-only`, `snowballs-pile` |
| `Teleporter` | `buttons`, `require-sign`, `range` |
| `XPStorer` | `block`, `per-bottle`, `requires-bottle`, `sneaking` |

A mechanic with nothing in the second column has only `enabled`. That is not an oversight: a bridge
takes its limits from the chips (below), and a book copier has nothing to decide.

### Naming blocks

Anywhere a block is named — `Gate.blocks`, `Netherrack.fire-blocks`, `BounceBlocks.blocks`,
`GlowStone.off-block`, `XPStorer.block` — three spellings are accepted:

- a modern name, `minecraft:oak_fence` or just `oak_fence`
- a tag, with a leading `#`, such as `#minecraft:fences`, expanded to whatever the server currently
  has in it
- a name from before the flattening, such as `35:14`

A name the server does not know is complained about in the console and skipped; the rest of the list
survives. A list where **nothing** could be read falls back to the default rather than leaving the
mechanic with nothing to work with.

### Snow is not like the others

Nothing about snow is built and nothing carries a sign, so switching any part of it on changes every
snowy block in the world. All six parts therefore start **off**, and a server that has never been
configured runs snow exactly as the game does. `enabled: true` on the section by itself does
nothing; the parts underneath are what you turn on.

The vehicle habits in `config.yml` are off for the same reason.

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
| `mechanics.disabled: [Gate]` | `Gate.enabled: false` |
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

One further change worth knowing about: the copiers were switched off by their **sign** name before
— `[BannerCopier]`, brackets and all — which almost nobody would have guessed. They are
`BannerCopier`, `BookCopier` and `MapCopier` now, like everything else.

## When it does not work

**A mechanic does nothing at all.** Check its section says `enabled: true`, then check the two
switches in `config.yml` that stop everything at once: `enabled: false` at the top of it takes the
whole plugin out of service, and a world named under `disabled-worlds` runs no mechanic and no chip.
Both reach every mechanic here, not only the ones with signs.

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
