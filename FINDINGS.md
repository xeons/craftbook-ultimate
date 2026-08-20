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

## Logic and control

### 36. The password chip's "encryption" was `String.hashCode`

`PasswordControlled.encrypt` was `"" + password.hashCode()`, and that value was written into
`mcx121.txt` next to the switch name. A Java string hash is a 32-bit non-cryptographic checksum:
it is trivial to find a second string that produces the same number, so a door could be opened
without ever learning the password that was set on it, and equally trivial to work backwards
through a list of likely passwords. Anyone who could read the file had the door.

The comparison was also an ordinary string equality, which takes longer the more of the value
matches. That leaks how close a guess was, though it hardly matters next to the hash itself.

**Rewrite:** `PasswordStore` derives a key with PBKDF2-HMAC-SHA256 over a random salt per switch,
and compares in constant time. Nothing that can be typed back in is written anywhere. Deriving a
key is slow by design, so it is done away from the thread that ticks the world; that also means
somebody typing the command over and over cannot hold a region up.

### 37. The command controlled chips never released a switch

`CommandControlled.unload` was empty and `PasswordControlled` had no unload at all, so every
switch name that had ever loaded stayed in the map for as long as the server ran. A command could
therefore still throw a switch that no chip was following, and the listing filled up with names of
chips that had long since been broken.

**Rewrite:** a chip claims its switch as it loads and gives it up as it unloads, so the listing is
what is actually out there and a name means something.

### 38. Several chips parsed their signs during load with nothing catching failure

Finding 26 records this for the analog transmitter, but it is a pattern rather than one mistake.
`BitShift.load` called `Integer.parseInt(getLine(2))` outright. `Monoflop.load` indexed into the
result of a split without checking its length first. Both threw out of chunk load if a player had
edited the sign since the chip was made.

**Rewrite:** every one of these reads its sign afresh each time it runs, and a line it cannot use
leaves the chip idle rather than throwing.

### 39. The trigger reader swallowed a bad target and then used it

`TriggerReader.load` caught `NumberFormatException` around parsing its target and commented the
catch block `//eat it`, leaving the offsets at whatever they already were. On a fresh load that is
zero, so a sign with a damaged target line silently made the chip watch its own sign block rather
than doing nothing.

**Rewrite:** a target that cannot be read means no target, and the chip leaves its output where it
is.

### 40. The monoflop kept its clock progress as spaces on its fourth line

`Monoflop.unload` wrote its part-completed count out as that many space characters on line 4, and
`load` read the count back as the length of that line. The sign's fourth line was therefore both
required to be empty when the chip was made and quietly filled with whitespace afterwards.

**Rewrite:** the countdown is kept in memory. A timer caught mid-count by a chunk unloading comes
back ready rather than part way through, which is only visible to somebody who was not there to
see it.

## Entities and projectiles

### 41. The shooters checked their numbers only when the sign was made

`ArrowShooter.create` validated the speed, spread and vertical velocity and threw for anything out
of range. `ArrowShooter.load` re-read the same three numbers with no checks at all, so editing the
sign afterwards set whatever it liked: a speed of five hundred, a spread of nine hundred degrees.
The same code is repeated verbatim in `ArrowBarrage`, `SnowShooter`, `SnowBarrage`, `EggShooter`
and `EggBarrage`.

**Rewrite:** the limits are applied every time the sign is read, so an edited sign is held to the
same range as a new one. The six chips are one implementation with the projectile and the count as
parameters.

### 42. The fireball's speed and power were validated and then discarded

`FireballShooter.create` insisted the speed was between zero and five and the power between a tenth
and ten. `load` parsed both into fields and `onTrigger` used neither: it called `launch` with the
bare unit vector pointing out of the back of the sign. The rotation and pitch fared no better —
`pitchYawRoll` was assembled as `rotation + SignUtil.getFacing(getBlock()).asOffset().getX()`,
adding a block offset to an angle, and applied as an entity rotation, which a fireball recomputes
from its own motion on the next tick.

**Rewrite:** the rotation and the pitch aim the fireball, which is what a builder writing them
meant. The speed and the power stay inert: a fireball steers itself once it is away, so speed would
change nothing, and making the explosion bigger than a ghast's on signs that already exist is not a
change to make quietly.

### 43. The Zeus bolt's default radius struck nothing

`ZeusBolt.load` set the radius to `(1, 1, 1)` when line 3 was empty, and `onTrigger` looped
`for (int x = -radius + 1; x < radius; x++)`, which for a radius of one runs from zero to below
zero and so never executes. A chip left on its defaults did nothing at all. Where the loop did run
it was also asymmetric, covering one more block on one side of the middle than the other, and it
subtracted the loop variable from the centre rather than adding it.

The chance was compared as `nextInt(100) <= chance`, so a chance of zero still struck one block in
a hundred.

**Rewrite:** a reach of one means the cube of twenty-seven blocks around the middle, and a chance
of zero means never.

### 44. The fireworks chip was a disabled experiment

`Fireworks.onTrigger` shot a tipped arrow upward and started a bare `new Thread` per trigger, which
polled the arrow's position every hundred milliseconds off the server thread until it began to fall
and then removed it. Everything that would have made it a firework — `explodeTNT` and its callers —
is commented out in the source. So the chip fired an invisible arrow that deleted itself at the top
of its arc, from an unbounded number of threads reading entity state they had no right to touch.

**Rewrite:** the chip sets off a firework rocket, which is what its name, its shorthand and its
description all say it does. Fireworks have been an item in the game since long after this code was
written, so the arrow-and-TNT approach it never finished is not worth reviving.

### 45. The firework display's stop flag read a system property

`ProgrammableFireworksDisplay.load` set its stop-on-low flag with `Boolean.getBoolean(bits[0])`.
That method takes the name of a **system property** and reports whether it is set to `"true"`; it
does not parse the string it is given. The flag was therefore always false and the fourth line of
every one of these signs did nothing.

**Rewrite:** the line is parsed as what it says.

### 46. The plain firework script could not wait

`BasicShowInterpreter` split each line on a colon, so `wait:20` gave `bits[0] = "wait"`. It then
computed the delay as `Long.parseLong(line.replace("wait ", ""))` — replacing `"wait "`, with a
trailing space, which does not occur in `wait:20`. The parse therefore ran on the whole line and
threw, out of a scheduled task with nothing catching it. A show written in the documented format
ran every launch in the same tick and then died at its first pause.

The same branch built a fresh interpreter for the continuation but never assigned it to `show`, so
`isShowRunning` went on reporting the state of the old one.

**Rewrite:** a wait is the number after the separator, in both spellings, and the show that is
running is the one being tracked.

### 47. The potion area measured its duration in the wrong unit

`PotionArea.parsePotionEffect` multiplied the seconds on the sign by twenty to get ticks and then
rejected the result if it was above 999, while telling the player the limit was "999 seconds". The
real ceiling was therefore forty-nine seconds. Writing `INF` produced a hundred thousand ticks,
which failed the same check, so the documented way of asking for an effect that never wears off
could never be used.

The effect was also added to the list before those checks ran, and `load` swallowed the exception,
so a sign that was rejected when it was made still applied its effect after a chunk reload.

**Rewrite:** the limit is nine hundred and ninety-nine seconds, as the message always said, and
`INF` means an effect that does not wear off. A dose that cannot be read is not applied.

### 48. The potion area replaced a player's effects rather than adding to them

`onTrigger` called `entity.offer(Keys.POTION_EFFECTS, potionEffects)`, which sets the whole list.
Walking through an area that gives speed stripped whatever the player had drunk.

It also never checked whether the chip had actually been triggered, so it fired on the falling edge
as well as the rising one, and its self-triggering variant ignored the think flag.

**Rewrite:** effects are added to what is already there, and the chip acts only while something
drives it.

### 49. Two of the short names for potion effects collided

The abbreviations were generated at class load by walking `PotionEffectTypes` with reflection and
taking the first two letters of each underscore-separated word. `REGENERATION` and `RESISTANCE`
both give `RE`, and whichever field came later in the class won, which made regeneration
unreachable. The scheme was also unstable by design: adding an effect to the game could take a
short name away from an existing one.

**Rewrite:** the abbreviations are a fixed table, so what a sign means cannot change under it, and
`RE` stays on resistance because that is what signs have always got. Every effect can also be named
outright, which is how regeneration is reached and how the effects added since are reached.

### 50. Hit Mob Above matched nothing when its third line was blank

`HitMobAbove.load` fell back to an anonymous matcher whose test was `entity instanceof Creature`.
`Creature` there resolves to `DetailedEntityType.Creature`, a nested class of the matcher's own
superclass, not to Sponge's `Creature` interface — and no entity is ever an instance of it.
`DetailedEntityType.Creature.matches` has the same mistake, and `MobAbove.load` falls back to that
very class, so that sensor reported nothing when its third line was blank. A chip left on its
defaults hurt or saw nothing.

`load` also called `Integer.parseInt(getLine(3))` unguarded, and left `detailedEntityType` null when
parsing failed, so the next trigger threw.

**Rewrite:** a blank line means hostile mobs, which is what the chip is named for and what the mob
zapper already defaulted to.

### 51. The zapper and the collector searched for entities off the server thread

`MobZapper.onTrigger` and `ChestCollector.getAsyncAction` both ran their entity search through
`scheduleAsyncBatch` and then hopped back to the main thread to act on the results. `PlayerNear`
and `MobNear` do the same, and `ItemNear` and `HeldItemNear` call the `Async` search variants from
the ticking thread without leaving it. Finding 35 records the same pattern in the planters. The list could be stale by the time it was used, and the
search itself read entity state from another thread.

**Rewrite:** both search through the world seam, on the thread that owns the place they are
searching.

### 52. The spawners promised an output they never drove

The pin help for `MCX200` says "Outputs when spawning entities" and for `MCX201` "Outputs when
spawning items". Neither `onTrigger` writes to a pin.

**Rewrite:** the behaviour is kept as the code had it rather than as the help described it. Turning
a pin that has always read low into one that pulses would change what every existing build wired to
it does, which is not a fix worth making silently.

### 53. The stacked-entity grammar was reachable from chips that could not use it

`MobZapper.load` parsed its third line with `parseStackedAndNbt`, which understands riders and NBT.
`MobZapper.create` accepts only `mob`, `animal` or a bare type name, so no sign that the plugin
made could ever carry either. The NBT half of the grammar was likewise only ever reachable from the
entity spawner, and matching against it needed a full entity write-out per candidate.

**Rewrite:** one parser covers the whole grammar, and the extra data it can carry is used when
spawning and ignored when matching. Only the spawner's sign can carry any, so nothing is lost.

## Sensing players, mobs and items

### 54. The item sensor took its range only when the range was impossible

`ItemNear.load` read line 4 as:

```java
if (Integer.parseInt(line4) < 1 || Integer.parseInt(line4) > 30) {
    range = Integer.parseInt(line4);
}
```

The condition is inverted. A range inside the allowed one to thirty was parsed, found to be
allowed, and thrown away, leaving the default of five; a range outside it was accepted and used.
`create` checked the same line the right way round, so a sign could only ever be made with a range
the chip would then ignore. `HeldItemNear.load`, which is otherwise the same class, has the check
the right way round, which is how the mistake shows itself.

**Rewrite:** the range on the sign is the range used, held between one and thirty.

### 55. The area sensor searched a ball too small for the box it then filtered

`InArea.onTrigger` narrowed the candidates with
`getNearbyEntitiesInRadius(middle, max(range) / 2)` and then filtered them against a box whose
half-extent on each axis was the full `range`. A ball of radius `max/2` does not reach the faces of
that box, let alone its corners, so most of the area the sign asked for was never looked at. With
the default reach of three the sign describes a seven-block-wide box and the chip searched a ball
one and a half blocks across.

The box was also hung off the corner of the sign's block rather than its middle, so it sat half a
block out of true on two axes.

**Rewrite:** the world is asked for a box directly, which is what the chip wanted, and the box is
centred on the middle of the sign's block.

### 56. The sparing mode of Humans Only removed the humans

`HumansOnly.ExtraFilter` decides what to spare by testing `entity.getType()`:

```java
EntityType type = entity.getType();
return !(type instanceof Humanoid) && !(type instanceof Minecart) && ...
```

An `EntityType` is a registry entry, not an entity, and is never an instance of either interface.
Both tests are therefore constantly true, so the mode meant to spare *more* than the ordinary one
spared *less*: it kept nothing back, players included. The ordinary mode filters on the entity
itself and is correct.

**Rewrite:** the sparing mode spares what somebody is using — minecarts and boats, thrown ender
pearls, eyes of ender, fishing lines and tamed animals — and players are never removed in either
mode.

### 57. The item sensors accepted numeric item ids only

`parseDetectionParameters` reads an `ID:` value as
`String.valueOf(Integer.parseInt(value))`, so anything but a number throws and is reported as "ID
and damage value must be integers". `ID:wool` was refused while `ID:35` was accepted. The resolved
type was then taken with `.get()` on an `Optional`, so an id that named nothing threw
`NoSuchElementException` out of a method whose contract is to throw `InvalidICException`.

**Rewrite:** an item is named the same way as everywhere else in the plugin, by modern name or by
the old number and damage, and a name that resolves to nothing leaves the sensor reporting nothing.

### 58. Two sensors classified mobs from a list frozen at 1.12

`MobNear` and `MobZapper` decide what counts as a mob or an animal by looking the entity's type up
in `EntityParsingUtil.getMobs()` and `getAnimals()`, two hand-written sets. Anything added to the
game after they were written is in neither, so a sensor set to watch for mobs ignores it entirely
and a zapper set to clear them leaves it standing.

**Rewrite:** the question goes to the game, which knows what it counts as hostile and what it
counts as an animal, so a mob added by a later version is classified without anything here
changing.

## Configuration

### 59. The harvester's height limit set its width

`RangeLimitations.maxHeight` is written as

```java
if (Math.abs(target.getHeight()) > max) {
    return RelativeRange.builder().from(target).width(max).build();
}
```

An area too tall therefore had its *width* replaced, and kept every bit of the height that
tripped the check. The one chip that used it is the harvester, which asks for
`maxHeight(maximumLength)`, so a sign asking to gather a hundred blocks upward gathered a hundred
blocks upward and lost its width instead.

**Rewrite:** each of a harvested area's three sides is held against the limit that belongs to it,
and holding one never alters another.

### 60. The offset limiter returned the larger of the two numbers

`RangeLimitations.limit` reads

```java
final double local_limit = Math.max(Math.abs(i), max);
return sign * local_limit;
```

`Math.max` of the value and the limit is the value whenever the value is the bigger one, which is
the only case the method is called in. So `maxOffsetInSingleDirection` detected an offset beyond
its limit, built a new range, and put the offending offset straight back. The chest collector asks
for a limit of eight and the light sensor for twenty; neither ever got one.

The comment above it says the code exists to stop a bridge being built with a width of minus
sixteen, which the plain `Math.min` it replaced would have done correctly for the positive case
and which the surrounding builder handles for the negative one.

**Rewrite:** an offset beyond what a chip may reach is refused, and the chips that take one check
each axis directly rather than through a shared helper that has to guess at the sign.

## The minecart mechanics

### 61. The station matcher matched everything after a wildcard

`CartSorting.matchGlobStation` walks the destination and the pattern together, and where the
pattern holds a `*` it looks at the character after it:

```java
final char nextChar = globChars[globCharPos + 1];
if (nextChar == stationChars[i + 1]) {
    globCharPos++;
}
else {
    return true;
}
```

The `else` gives up and reports a match. A pattern only matches after a wildcard if what follows
the wildcard happens to sit immediately after it in the destination, and if it does not, everything
matches. `#north*gate` therefore claimed `northeastpier` and every other destination beginning
`north`, so a junction meant to pick out one branch took every cart that reached it.

The tail has the same shape: after the destination runs out, `globCharPos == globChars.length - 1`
reports a match whatever that last character is, so `#north` also claimed `nort`.

**Rewrite:** an ordinary wildcard match, where `*` stands for any run of characters and everything
else has to be there.

### 62. The held-item filter read an item out of an empty hand

`CartSorting.isHeld` fetches the rider's held item as an `Optional` and then, for any filter but
`none`, reads it without checking:

```java
Optional<ItemStack> helditem = rider.getItemInHand(HandTypes.MAIN_HAND);
...
if (helditem.get().getType() == itemStack.getType()
```

A rider with empty hands rolling over a junction filtered on `held:` threw
`NoSuchElementException` out of the movement listener, taking the rest of that event's mechanics
with it.

**Rewrite:** empty hands are an answer rather than an absence, so `held:stone` is false for
somebody holding nothing and `held:none` is true.

### 63. The launcher was gated by the loader's permission

`CartLaunch` declares its creation permission as

```java
new SpongePermissionNode("craftbook.cartload", ...)
```

which is the node `CartLoad` declares as well. Anybody granted the right to build a loader could
build a launcher, and anybody granted the right to build a launcher by its own name could not.

**Rewrite:** every cart mechanic's permission is derived from its own name, so the two cannot drift
apart again.

### 64. A delay would hold a cart for as long as its sign said

`CartDelay` parses its wait and schedules the release with no upper bound, so a mistyped `99999`
took the cart out of service for a day and a bit, with no way to get it back but breaking it.

**Rewrite:** a wait is between one second and an hour, and a sign asking for more is refused as it
is written.

## Messaging and logging

### 65. A message nearby forgot its range every time its chunk came back

`MessageNearby.load` reads the distance back off the first line under this guard:

```java
Matcher matcher = pattern.matcher(line1);
if (matcher.find() && distance != 64) {
    distance = Integer.parseInt(matcher.group());
} else {
    distance = 64;
}
```

`distance` is still its initial `64` at that point and nothing sets it beforehand, so the second
half of the condition is never true and the else branch always runs. A sign built to reach ten
blocks reached the full sixty-four as soon as its chunk unloaded and came back, which is to say
almost always.

**Rewrite:** the range is read from the sign every time the chip runs, so there is nothing to
forget.

### 66. A server log nearby never used the range on its sign

`ServerLogNearby.onTrigger` looks for the closest player starting from

```java
double closestplayerdistance = 64;
```

rather than from the range the sign asked for. The range was parsed on creation, validated,
stamped onto the first line and then never read by anything that mattered: every one of these
chips reached sixty-four blocks whatever it said.

**Rewrite:** the range bounds the search, so a chip asking for ten blocks sees ten blocks.

### 67. A server log nearby refused to load from a sign it had not stamped itself

`load()` ends with

```java
range = Integer.parseInt(line1.substring(length));
```

where `length` is the title's. A first line shorter than the title threw
`StringIndexOutOfBoundsException` and one of the right length carrying anything else threw
`NumberFormatException`, either of which came out of chip loading. Only a sign the plugin had
written itself was safe to read back.

**Rewrite:** the range is the run of digits at the end of the line, and a line with none means the
default. A hand-written sign and a stamped one both work.

### 68. The fuller log chip left one of its placeholders showing

`ServerLogNearbyPlus.onTrigger` replaces both `%a` and `%p` when it has somebody to report, but
its other branch replaces only `%p`:

```java
String msg = "[CB!] " + logmessage.replaceAll("%p", "[NONE_FOUND]");
```

A sign written with `%a` on it therefore logged a literal `%a` whenever nobody was in range.

**Rewrite:** both placeholders read `[NONE_FOUND]` when there is nobody to name.

### 69. A vanished player was logged along with how far away they were

Both nearby log chips walk every player in the world with no visibility check, and the fuller one
writes each name out with a distance. Anybody able to press a button could find somebody who had
taken trouble not to be found, to within a tenth of a block.

**Rewrite:** these chips see the same people a sensor sees, which does not include anybody
spectating or vanished.

### 70. A marquee refused to load from a sign somebody had written on

`Marquee.load` reads its running position with a bare

```java
this.currentPin = Integer.parseInt(getLine(2));
```

so any text on the third line — a builder's note, a leftover from a previous chip — threw out of
chip loading rather than being ignored.

**Rewrite:** a line that is not a position means the beginning of the cycle.

### 71. A marquee transmitter's reset left a band transmitting for ever

The step branch of `MarqueeTransmitter.onTrigger` turns the current band off before moving on. The
reset branch does not:

```java
if (getTriggeredPin() == 1) {
    current = getMode().getType() != Modes.REVERSE ? start : end;
}
```

The band it had reached stayed on with nothing to turn it off again, so a reset left two lamps lit
and every reset after that left another.

**Rewrite:** reset and step both turn the old band off and the new band on, so exactly one band in
a run is ever carrying a signal.

### 72. Message All declared an output and never drove it

`MessageAll.Factory.getPinHelp` documents an output, and `onTrigger` never writes one. A builder
chaining anything off it got a pin that stayed wherever it happened to be.

**Rewrite:** the output is high while the chip is being driven and has something to say.

### 73. A book could hold a message back for as long as it liked

`MessageNearby.parseDelay` accumulates whatever a `[DELAY:]` line asks for with no upper bound, so
a mistyped page scheduled a message for some time next week and held a task open until then.

**Rewrite:** a book cannot hold a message back by more than an hour, which is the same bound the
cart delay takes.

## Weather illusions

### 74. A one-character audience line threw out of the redstone handler

Both `FalseWeather` and `HideWeather` read the audience with

```java
if(getLine(2).length() > 0 && getLine(2).charAt(1) == ':')
```

A line of exactly one character passes the length check and is then read at index 1.
`StringIndexOutOfBoundsException`, thrown out of the chip's trigger and taking the rest of that
redstone update with it. A single stray letter on line 3 was enough.

**Rewrite:** a line shorter than a prefix and a colon names no audience, which means the whole
world, the same as a blank one.

### 75. A distance weather chip refused to load unless its sign named a distance

`create` treats line 3 as optional and leaves the radius at its default of ten. `load` does not:

```java
radius = Integer.parseInt(getLine(2));
```

So a chip built without a distance — which creation explicitly allowed — threw on every load from
then on. The same shape as finding 67, in a different family.

**Rewrite:** a line that is not a number means the default reach, and the reach is read afresh
each time the chip runs.

### 76. The message on a distance weather chip could not contain a space

`create` rejects line 4 outright if it contains one:

```java
if (line4.contains(" ")) {
    throw new InvalidICException("Fourth line contains an invalid message.");
}
```

The field is described as a message and shown to players as one, so this left it able to say
exactly one word.

**Rewrite:** the greeting is whatever is written on line 4. Nothing existing breaks, because
nothing existing could have had a space in it.

### 77. The output reported which pin had fired, not whether anything happened

Both of the named-audience chips end with

```java
boolean out = getPinSet().isTriggered(0,this);
...
getPinSet().setOutput(0,out,this);
```

which is true when input 1 happened to be the pin that caused the run and false otherwise —
unrelated to whether an illusion went up, and false for a chip driven on any other pin. The two
distance chips set no output at all.

**Rewrite:** the output is high while the chip has an illusion up, on all four.

### 78. The named player was whichever one came last

```java
for(Player anyPerson : Sponge.getServer().getOnlinePlayers()){
    if(anyPerson.getName().contains(id)) player = anyPerson;
}
```

No break, so a sign reading `p:Ste` fooled the last matching player the server happened to list
rather than the first, and which one that was could change between runs.

**Rewrite:** the first match wins, so the same sign fools the same person.

### 79. An illusion outlived the chip that put it up

None of the four chips restores anybody's weather when it unloads. A chunk going out of view while
the illusion was up left the player seeing weather that was not there, with the only thing that
could have taken it away now gone. Relogging was the fix.

**Rewrite:** each chip remembers exactly who it has fooled and puts those people back when it
stops being driven, when the real weather catches up with the illusion, and when it unloads.
