# Findings

Bugs and inconsistencies found in the legacy codebases while establishing behaviour for the
rewrite. Each entry records what the old code did, why it is wrong, and how the rewrite handles
it. Nothing here is a description of the new code — the new code documents itself.

Sources referred to below:

- **extra** — the Sponge 7.3 / MC 1.12.2 fork in this repository, under `src/`.
- **upstream** — EngineHub CraftBook 3.10.14 at `E:\Code\CraftBook`.

---

## IC identifier line

### 1. A model reference was matched anywhere in the line

`ICManager.getICType` tested `id.contains('[' + model + ']')`. Any sign whose second line merely
contained a model reference became a working IC, so ordinary text such as `look at [MC1000]`
silently built a repeater.

**Rewrite:** the model pattern is anchored to the whole line.

### 2. Model and shorthand references were matched by different rules

The same method used `contains` for model references but `equalsIgnoreCase` for shorthands. A
shorthand with a trailing space failed to resolve while an equivalent model reference resolved
fine.

**Rewrite:** both spellings are trimmed and normalised through one parser.

### 3. The restricted marker disabled self-triggering

Self-triggering was detected with `ic.getLine(1).endsWith("S")`, but the restricted marker `*`
is appended *after* the `S` when a restricted IC is created. A restricted self-triggering chip
written `[MCX131]S*` therefore stopped ticking.

**Rewrite:** flags are collected from anywhere in the suffix, so order does not matter.

### 4. The shorthand `ST` marker never actually enabled self-triggering

`ICManager.getICType` accepted `=REPEATER ST` when resolving the chip, but the self-triggering
check tested `endsWith("S")`. `=REPEATER ST` ends in `T`, so the chip resolved but never ticked.

**Rewrite:** the ` ST` marker sets the self-triggering flag during parsing.

---

## Pin layouts

### 5. The 3I5O layout reported a name that could never be looked up

`Pins3I5O.getName()` returned `"3I50"` with a digit zero, while the registry key in
`ICSocket.PINSETS` and the IC registration for `MC4040` both use `"3I5O"` with a letter O. Any
round trip through the layout's own name failed to resolve.

**Rewrite:** one spelling, `3I5O`, with a test asserting the digit-zero form does not resolve.

### 6. `Pins3I5O` ignored the pin permutation mode

Every other layout resolved pin positions through
`ic.getMode().getPinConfiguration()[id]`, but `Pins3I5O` switched on the raw `id`. Pin remapping
silently did nothing on that one layout.

**Rewrite:** permutation is applied uniformly by the framework rather than by each layout.

---

## Mode string

### 7. Valid pin permutations were rejected

`Mode.getPinConfiguration` required `mode.substring(1,3)` to contain a letter in `a`–`c` and
`mode.substring(4,6)` to contain one in `d`–`f`. That accepts `abcdef` and `badcfe` but rejects
`fedcba`, which is a perfectly ordinary permutation, and it indexes fixed offsets rather than
validating the string as a whole.

**Rewrite:** a run of letters is a permutation when every letter is in range and appears at most
once.

### 8. The mode switch trimmed but the permutation check did not

`Mode` selected its behaviour from `mode.trim()` while testing for a permutation with
`mode.matches(".*[a-f].*")` on the untrimmed string. The two disagreed about leading whitespace.

**Rewrite:** the mode string is stripped once, up front.

---

## Registry

### 9. IC lookup depended on iteration order of a sorted set

`getICType` walked `registeredICTypes`, a `TreeSet` ordered by model id, returning the first
entry whose id was *contained* in the line. Combined with finding 1, resolution depended on
catalogue ordering rather than on an exact match.

**Rewrite:** lookup is an exact map read.

### 10. Duplicate shorthands were registered without complaint

`MC1111` and `MC0111` both register `WirelessReceiver.Factory()` with the same name, the same
description and the same shorthand `RECEIVER`. Registration silently kept both, so which chip a
`=RECEIVER` sign resolved to depended on catalogue iteration order.

**Rewrite:** one definition holds the surviving number, with the retired number kept as an alias
so existing signs still resolve. The registry rejects a duplicate model number or shorthand at
registration time rather than resolving one arbitrarily.

### 11. Registration formatting hides entries

`ICManager` line 236 declares both `MCX139` and `MCX133` on a single source line, which makes
the catalogue easy to misread when auditing it. Both registrations are themselves correct.

**Rewrite:** one definition per declaration.

---

## Platform workarounds that no longer apply

The extra fork carries four Mixins against Minecraft 1.12 internals. All four exist to work
around limitations that modern Paper addresses directly, so none is carried forward:

| Mixin | Purpose | Replacement |
| --- | --- | --- |
| `MapStorageMixin` | read the highest allocated map id | Paper's map API |
| `ChunkUseThreadSaferEntityLists` | make chunk entity lists safe to read off-thread | not needed; Folia's region threading replaces the hack |
| `CraftBookChairArmorEquipMixin` | stop dispensers equipping armour onto chair entities | `BlockDispenseArmorEvent` |
| `CraftBookChairWaterDismountMixin` | stop chairs ejecting their rider underwater | `EntityDismountEvent` |

---

## Logic chips

### 12. `MC3031` and `MC3033` are the same chip

`RSNandLatch` (MC3033, RS-Nand Latch) and `InvertedRSNandLatch` (MC3031, Inverse RS-Nand Latch)
read the same two pins and produce identical outputs for all four input combinations:

| in 0 | in 1 | MC3033 | MC3031 |
| --- | --- | --- | --- |
| high | high | high | high |
| high | low | high | high |
| low | high | low | low |
| low | low | hold | hold |

Both reduce to "input 0 sets, input 1 resets, neither holds". Whatever the inverted variant was
meant to do, it does not differ from the plain one.

**Rewrite:** one implementation, `Latches.rsNandLatch()`, registered under both numbers so
existing signs of either kind keep behaving exactly as they do now. Worth a decision later on
whether MC3031 should be given genuinely inverted behaviour — that would be a behaviour change,
so it is not being made unilaterally.

### 13. `MC4010` is registered twice, and one half adder is dead code

`ICManager` registers `MC4010` at line 199 and again at line 313, both times with
`chips.logic.HalfAdder`, differing only in their description text. Because the catalogue is a
`TreeSet` ordered by model number the second entry is silently dropped from listings, while the
factory lookup map keeps the later one — so documentation and resolution disagree about which
description belongs to the chip.

Separately, `legacy.ics.chips.logic.HalfAdder` is byte-for-byte equivalent in behaviour to
`chips.logic.HalfAdder` and is never imported or registered anywhere. It is dead code.

**Rewrite:** one half adder, registered once.

### 14. The full subtractor's borrow expression relies on operator precedence

`FullSubtractor` computes the borrow as `C & A == B | !A & B`. Java binds `==` more tightly than
`&`, so this parses as `(C & (A == B)) | ((!A) & B)`. That happens to be correct for all eight
input combinations, but reads as though it were comparing `C & A` against `B`.

**Rewrite:** the borrow is written in its conventional sum-of-products form, with an exhaustive
test against integer subtraction.

---

## Chip triggering

### 15. A chain of chips could recurse until the stack ran out

`AbstractIC.trigger` pushes a cause frame and calls `onTrigger` with no re-entrancy guard and no
depth limit. Driving an output goes through `LocationWorldUtil.toggleLever`, which calls
`setBlockState` followed by `notifyNeighborsOfStateChange` on the same call stack, so one chip
triggering the next happens synchronously inside a single redstone update.

In practice most circuits are safe, because `AbstractPinSet.setOutput` writes a lever only when
its value actually changes:

```java
if (blockState.get(Keys.POWERED).orElse(false) != powered) {
    LocationWorldUtil.toggleLever(block, false);
}
```

A settling circuit stops changing and the chain ends. A circuit that oscillates never settles,
and recurses until it throws `StackOverflowError` part-way through a redstone update, leaving
whatever it had already written in place.

**Rewrite:** the "only write when the value changes" behaviour is kept, since that is what makes
ordinary circuits terminate. A chip additionally cannot re-enter itself, and a chain unwinds at a
fixed depth rather than exhausting the stack, leaving the circuit consistent so the next redstone
change carries it forward.

### 16. The daylight sensor's wrapping window was always true

`DaySensor.isDay` handles a window whose start is after its end, which describes a window running
through midnight:

```java
if (day < night) return time >= day && time <= night;
else if (day > night) return time <= day || time >= night;
```

The second branch is satisfied by every possible time. With a start of 18000 and an end of 6000,
a time fails `time <= 18000` only by being above 18000, and fails `time >= 6000` only by being
below 6000; no time does both. Any sensor configured to span midnight therefore reported daytime
permanently.

**Rewrite:** the wrapping branch reads `time >= dawn || time <= dusk`, which is the window the
configuration describes.

### 17. Two weather chips shared one shorthand

`MCX233` (Simple Weather Control) and `MCT233` (Weather Control) are both registered with the
shorthand `WEATHER CONTROL`. As with finding 10, which of them a `=WEATHER CONTROL` sign resolved
to depended on catalogue iteration order rather than on anything a player could see.

**Rewrite:** `MCX233` keeps the shorthand, since it is the one listed first and the simpler of
the two. `MCT233` is registered as `WEATHER CTRL ADV`. Both model numbers resolve as before, so
only signs written with the ambiguous shorthand are affected, and those now resolve predictably.

---

## Block bags, rewritten as stockpiles

### 18. The single-chest search box was off by one on three sides

`NearbyChestBlockBag.fromSourcePositionNearby` searched with

```java
for (int dx = -5; dx < 5; dx++)
```

on each axis, which covers -5 to 4 rather than -5 to 5. The search box was therefore 10 blocks
wide instead of 11 and sat off-centre, so a chest exactly five blocks to the north, east or above
was found while the same chest to the south, west or below was not.

**Rewrite:** the radius is inclusive on both sides, so the box is centred on the mechanic.

### 19. Taking across several containers could half-empty them

`MultiNearbyChestBlockBag` drew from each container in turn without first checking that the
containers held enough between them. A withdrawal that ran out part-way left the earlier
containers already emptied, so a mechanic that then declined to build had still taken the
materials.

**Rewrite:** `Stockpile.takeAll` puts back everything it took if it cannot take the whole amount,
so a mechanic either gets all its materials or leaves the containers as they were.

### 20. Double chests risked being counted twice

Halves of a double chest were skipped by remembering the inventory objects already seen. The
inventory a chest hands back for a double chest is not guaranteed to be the same object for both
halves, so identity comparison is not a reliable way to tell that two blocks share one container.

**Rewrite:** finding either half claims both positions, worked out from the chest's own block
data rather than from the inventory it returns.

---

## Block placing

### 21. A partly-affordable structure built from the wrong end

`BlockPlacingIC` walked its area from the region's minimum corner to its maximum. Which end of a
bridge that corresponds to depends on which way the sign faces: for a sign facing one way the
minimum corner is nearest the sign, and for the opposite facing it is furthest from it.

A chip that ran out of materials part-way therefore left a structure that reached out from the
sign in one direction and floated in mid-air, detached from the sign, in the other.

**Rewrite:** the area is walked outward from the sign regardless of facing, so a partial
structure always reaches part of the way rather than starting at the far end.

### 22. The `*` marker is an arming marker, not a permission marker

Worth recording because the name invites the wrong reading. A `*` appended to the model reference
does not mean the chip was permission checked. It means the chip has not yet been authorised: a
block-placing chip is created with the marker, refuses to act while its area still contains the
block it would place, and clears the marker once the area is free. The purpose is to stop a chip
being built over an existing structure and used to mine it.

Permission restriction is a separate and independent thing: `Bridge.ForcingFactory` is
permission restricted and skips the authorisation check, while `Bridge.Factory` is neither
restricted nor pre-authorised.

**Rewrite:** the two are modelled separately. `ICLine.awaitingAuthorisation` carries the marker,
and `ICDefinition.requiresAuthorisation()` says which chips are created with it, independently of
`restricted()`.

### 23. Legacy numeric block ids and damage values

Line 2 of a block-placing sign was parsed as `id[:damage]`, where the id could be numeric and the
damage value selected a variant, so red wool was `35:14`. Both concepts went away when block
states were flattened in 1.13.

This matters more here than it would elsewhere: the fork this is being ported from ran on 1.12,
so essentially every block-placing sign already in a world uses the old spelling.

**Rewrite:** both spellings are read. `BlockReference` decides which one a sign is using, telling
a damage value apart from a namespace by whether what follows the colon is a number, and the
platform layer resolves legacy pairs through the server's own flattening tables rather than
through a guessed mapping. `35:14`, `wool:14`, `red_wool` and `minecraft:red_wool` all name the
same block.

## Wireless

### 24. Band updates ran on a thread pool and touched blocks from it

`Bands.setActivatedBand` handed every change to a single-threaded `ThreadPoolExecutor`, which
then called `BandInfo.updateReceivers`. That method read each receiver's world, its loaded state
and its output lever, all from the pool thread rather than from the server thread. Reading block
state off the server thread is unsafe on any Minecraft server, and on a regionised one it is
unsafe even from another region's thread.

It also meant a transmitter's effect on its receivers was ordered by the pool rather than by the
redstone update that caused it, so two transmitters changing in the same tick could land in
either order.

**Rewrite:** nothing is pushed. A transmitter writes its band's state into `Radio` and a
receiver reads it on its own tick, on the thread that owns its own blocks. The two ends share
one concurrent map and nothing else, so no work and no block access crosses a region boundary.

### 25. A sign with no channel name joined a shared blank band

The channel name was parsed with the pattern check disabled, so a blank line 3 produced a band
of `("", "")` rather than being rejected. Every transmitter and receiver in the world whose
channel line was empty was therefore on one band together, and any of them switched all of them.

**Rewrite:** `Band` cannot be built without a channel name, and `Wireless.bandOn` returns nothing
for a sign that has none. A chip with a blank channel line is inert.

### 26. The analog transmitter parsed its bounds while loading a chunk

`Redcoder.load` called `Integer.parseInt` on the settings line with nothing catching the failure.
A sign reading `lift:x:9` — a typo, or a line a player edited afterwards — threw
`NumberFormatException` out of chunk load rather than producing a chip that did nothing.

**Rewrite:** `Wireless.AnalogSettings.parse` returns nothing for a line it cannot use, and the
chip stays quiet.

## Transporters and destinations

### 27. A destination that lost the race still evicted the winner

`Destination.registerIfNotRegistered` refused to register when the name was already taken, but
`onTrigger` had already set `isActive` before calling it. `unload` then removed the name from the
map on the strength of that flag, without checking whose entry it was. A second destination built
on a name already in use therefore did nothing while it was loaded and broke the working one the
moment its chunk unloaded.

**Rewrite:** `Destinations` records who holds each name and only that holder can release or move
it, so a destination that never took a name cannot take one away.

### 28. The forced pressure plate mode released the plate at the wrong end

`Transporter.onTrigger` teleported the traveller and then read `humanoid.getLocation()` to find
the pressure plate to release. By that point the traveller was standing at the destination, so it
was the plate at the arrival point that was released, not the pad they had just left. The pad
stayed pressed, which is the exact thing the mode exists to prevent.

**Rewrite:** where a traveller was standing is read before they are moved, and that is the plate
that is released.

### 29. Finding the arrival spot read the far end's blocks from the near end

`Destination.teleportTo` ran `findEmptySpot` against its own blocks, but it was called by the
transporter, so the scan happened on whichever thread the transporter was running on. On a single
threaded server that is merely surprising; on a regionised one the transporter would be reading
blocks belonging to a region it does not own, arbitrarily far away.

**Rewrite:** a destination works out its own arrival point on its own thread and publishes it as
a `Landing`. A transporter reads those three values and hands the traveller to the server to
move, so it never reads a block that is not its own.

## Blocks and farming

### 30. Two spellings for a block with a damage value

The chips that build an area read their block as `id[:damage]`, so red wool is `35:14`. The chips
that set a single block read theirs as `type[@damage]` through a different parser, so on those
signs the same block is `35@14`. Nothing marks the difference; it is simply two families of chip
that grew separate parsers.

**Rewrite:** `BlockReference` reads both. An `@` always separates a damage value, since no block
name contains one, and the existing rule for `:` is unchanged. Either spelling therefore works on
either family, and no sign already in the world has to be edited.

### 31. The block replacer's output was always high

`replaceBlocks` returned `traversedBlocks.size() > 0`, and the set was seeded with the block
behind the sign before the walk began, so it could never be empty. The chip drove its output high
on every trigger regardless of whether it had found anything to change, which makes the output
useless for chaining.

**Rewrite:** the output reports whether the block behind the sign was one of the chip's two, which
is the question a builder is actually asking.

### 32. The block replacer's configuration was not on its sign

The two block states came from a pair of chat prompts when the sign was made and were kept in the
IC's serialised data. Nothing about the chip could be read off the sign, so a player could not see
what it was set to, and the setting was lost if the stored data was.

The sign's third line was unused, and its fourth already carried `delay:mode:physics`.

**Rewrite:** the pair goes on the third line as `driven|idle`, the same spelling the toggle block
already used for its pair. Signs made under the old scheme carry no pair and need that line
filled in; there was no way to carry the stored data across in any case.

### 33. The block replacer scheduled one task per block

Every block the change reached scheduled its own delayed continuation, and each of those looked at
six neighbours and scheduled again. A wave over a few thousand blocks therefore created tens of
thousands of scheduled tasks, all doing almost nothing.

**Rewrite:** the wave is walked a ring at a time. One task per step of the delay covers the whole
front, whatever its size, and blocks already reached are never queued twice.

### 34. A crop's growth stage cannot be written as a block key

The harvester matched an exact block state, so a sign reading `59:7` gathered fully grown wheat
and left the rest. The flattening turned that pair into a single block, and a growth stage is a
block state rather than part of the block's name, so the damage value has nowhere to go.

**Rewrite:** the harvester matches the block and requires it to have finished growing. That is
what `59:7` meant in practice, and it keeps working when a plant's number of growth stages
changes.

### 35. The planters read entities off the server thread

`Planter.onTrigger` handed its work to an async batch and called `getNearbyItemsAsync`, walking
the chunk provider's entity lists from that thread before hopping back to place the block. Reading
entities off the server thread is unsafe on any Minecraft server, and on a regionised one it is
unsafe even from another region's thread.

**Rewrite:** a planter reads the items around it on the thread that owns them, which is its own,
and the search goes through the world seam rather than through the chunk provider.
