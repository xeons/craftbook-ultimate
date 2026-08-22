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

## Sound and music

### 80. A tune's rests did nothing at all

`Tune.playNotes` handles a rest with

```java
if (pitchStr.charAt(0) == '-' && delay < 0) {
    delay = delay + Integer.parseInt(pitchStr.substring(1));
    return;
}
```

`delay` starts at zero and the only other thing done to it is setting it back to zero, so
`delay < 0` is never true. Every rest fell through to being read as a note, failed to match the
note table, and did nothing. A tune written with gaps in it played as an unbroken run.

**Rewrite:** a rest holds back everything after it by as many ticks as it says, so a tune plays at
the spacing it was written with.

### 81. A tune's notes before its first instrument were silent

`currentInstrument` starts null and `playNotes` only sounds a note when it is not. Nothing says an
instrument number has to come first, so a tune that opened with a note simply lost every note up
to the first number.

**Rewrite:** a tune with no instrument named yet plays on a harp, which is what a note block does
standing on nothing in particular.

### 82. A tune stopped dead at an instrument number nothing answered to

```java
currentInstrument = INSTRUMENT_MAP.get(notes[i++]);
if (currentInstrument == null) {
    return;
}
```

The map held 0 to 10, and the notation matches two digits, so `11` and up abandoned the rest of
the tune rather than being ignored.

**Rewrite:** an unknown number leaves the voice as it was and the tune plays on — and 11 upwards
are no longer unknown, since they now name the voices Minecraft has gained since.

### 83. A tune's output went high and low within the same run

`playTune` raises the output, schedules every note, and lowers the output again before returning,
all synchronously. Nothing could ever observe it raised.

**Rewrite:** the output stays high until the last note has sounded.

### 84. Only the last note of a tune could be cancelled

`delayTask` is overwritten by each scheduled note, so `unload` cancelled one note out of however
many were still to come. A tune whose chunk unloaded went on playing.

**Rewrite:** the notes are scheduled on the chip's own scheduler, which stops when the chip does.

### 85. A jukebox played the record somewhere other than the jukebox

`Jukebox.onTrigger` insists on a jukebox touching the sign's support and then plays the record at

```java
Location<World> location = getBlock().add(0, 1, 0);
```

which is the block above the sign. The jukebox was a requirement that had no bearing on where the
music came from.

**Rewrite:** the record plays at the jukebox, which is the thing the builder put there for it.

### 86. The sound effect shorthand was built from whatever order reflection returned

`SoundEffect` builds its abbreviations by reflecting over `SoundTypes` and calling
`HashMap.put`, so where two sounds shortened to the same six letters the winner was whichever
field reflection happened to hand over last — not stable between runs, let alone between versions.

**Rewrite:** the shorthand is worked out from the sounds the server actually has, and where two
share one the first in the server's own order answers to it. A sound can also be named in full,
which is unambiguous and is the better way to write a new sign.

## Music from a file

### 87. The last song on a playlist was never played

`Melody.playPlaylist` picks a song, advances past it, and then decides whether to carry on:

```java
selected = playlistEntries.get(playlistIndex);
playlistIndex++;

if (playlistIndex >= playlistEntries.size()) {
    if (loop) {
        playlistIndex = 0;
    } else {
        playlistIndex = 0;
        stopSong();
        return;
    }
}
playMidiFile(noteBlockLocation, selected);
```

On the run that selects the last entry, the index goes past the end and, without `loop`, the
method returns before reaching `playMidiFile`. The song it had just chosen was dropped, so a
playlist of five songs played four and a playlist of one played nothing at all.

**Rewrite:** the song is played first and the place in the list moves on afterwards, so every
entry on a playlist is heard once before it stops.

### 88. The playback speed on the sign did nothing

`parseLine3` reads a number after the colon, checks it is between zero and ten, refuses the sign if
it is not, and stores it in a field. Nothing else in the class ever reads that field. A builder
could be told their sign was invalid over a number that had no effect either way.

**Rewrite:** the number is still read off the line, so existing signs go on being accepted, and it
still does nothing. Giving it a meaning now would change how every sign carrying it sounds, and
there is nothing in the old code to say what it was supposed to mean.

### 89. The playlist file was read from disk every time a song ended

`playPlaylist` opens with `loadPlaylist(playlistFile)`, which does `Files.readAllLines`. That runs
on the server thread from inside the playback monitor, so every song change on every melody chip
in the world stopped the server to read a file that had almost certainly not changed.

**Rewrite:** playlists are read once at startup, alongside the songs, and held in the same registry
the firework displays use.

### 90. There was up to a second of silence between songs

The monitor that notices a song has finished runs on `intervalTicks(20)`, so the gap before the
next one started was whatever was left of that second. Nothing was wrong with the music; it simply
waited.

**Rewrite:** the next song starts on the tick the last note of the previous one sounds.

### 91. The commands listed a different folder from the one the chip read

`Melody` resolves its files against `CraftBookAPI.inst().getWorkingDirectory()`, while the two
commands beside it hard-code

```java
Paths.get("config", "craftbook", "midi")
```

relative to whatever the process was started in. On any server where those two were not the same
place, the commands listed music no chip could play and hid music every chip could.

**Rewrite:** both the chip and the commands read the one registry, which is filled from the plugin's
own folder, so what the commands list is exactly what a sign can name.

### 92. Nothing bounded what a MIDI file could ask the server to do

A file was handed to the sequencer whatever its size, and every note in it became a scheduled
event. A long file with dense chords is an ordinary thing to find on the internet and was enough
to bury a server, from a sign anybody with the permission could build.

**Rewrite:** a file over a megabyte is not read, a song holds at most thirty thousand notes, and at
most eight sound on any one tick. The whole song is walked by a single repeating task rather than
by one task per note.

## The area mechanics and the lift

### 93. A bridge's width was held to the length limit, not the width limit

`Bridge.countWidth` counts sideways but stops on

```java
for (; i < maximumLength.getValue(); i++)
```

which is the sixteen-block length limit rather than the five-block width one. `maximum-width` was
in the configuration file, was documented, and did nothing at all for a bridge: a landing twelve
blocks across made a bridge twelve blocks across.

**Rewrite:** both mechanics count width against the width limit and length against the length
limit, and both read the limits the building chips already use rather than carrying their own.

### 94. A door could be one block wider than it was allowed to be

`Door.countLength` runs `while (dist <= max)` and increments inside the loop, so a frame wide
enough returned `maximumWidth + 1` rather than `maximumWidth`. The panel was a block wider to each
side than the setting said.

**Rewrite:** the count is bounded by the limit itself, so the widest allowed is the widest built.

### 95. Redstone toggled a bridge and a door rather than driving them

`SimpleArea.RedstoneListener` works out whether power arrived or left and passes that in as
`forceState`, but neither `Bridge` nor `Door` uses it to decide anything. Both look at what is
already in the span:

```java
filling = alreadyFilledBlocks == 0;   // Bridge
filling = emptyBlocks != 0;           // Door
```

so each edge of the signal toggled whatever state the structure happened to be in. Work one by hand
while the lever is on and it is inside out from then on: lever on retracts it, lever off puts it
out. `Gate` in the same fork does use `forceState`, which is what the other two were meant to do.

**Rewrite:** power arriving shuts a mechanic and power leaving opens it, for all three. A lever and
the thing it drives always agree, whoever last touched it by hand.

### 96. A bridge and a door disagreed about a half-built structure

The two lines quoted above are opposite rules. Given a span with one block in it and the rest
empty, a bridge empties the one and a door fills the rest. They are the same mechanic stood on end,
built from the same base class, and a builder learning one learned the wrong thing about the other.

**Rewrite:** one rule for both. Any gap at all means the next use fills it; only a structure that is
already whole comes down.

### 97. A door built as much of itself as it could afford

`Door` asks for what it needs, takes what there is, and lays that many blocks:

```java
int failedToTake = blockBag.remove(BlockPlacingIC.getPredicate(blockBagItem), requiredItems);
requiredItems -= failedToTake;
...
fillEmpty(world, min, max, baseBlock.getBlock(), requiredItems / blockBagItem.getQuantity());
```

A chest one block short left a doorway with a hole in it and a chest with nothing in it. `Bridge`
beside it checks `bag.has(...)` first and refuses outright, which is the right way round: a bridge
with a hole in it is worse than a bridge that did not move.

**Rewrite:** both take everything they need before they lay anything, and neither takes anything at
all if it cannot take all of it.

### 98. A door with nowhere to store its blocks threw instead of saying so

Every other complaint in the file is guarded by `human instanceof CommandSource`. This one is not:

```java
if (blockBag == null) {
    ((CommandSource) human).sendMessage(Text.of(TextColors.RED, "You have no where to put the blocks!"));
```

The redstone path passes `cause.first(Player.class).orElse(null)`, so a door with no chest near it
and a lever wired to it threw a `NullPointerException` out of the redstone listener on every change
of the signal.

**Rewrite:** whoever set a mechanic off is optional throughout, and a mechanic driven by redstone
simply has nobody to tell.

### 99. Button lifts worked even when they had been switched off

```java
if (allowButtonLifts.getValue() &&
        signLocation.getBlockType() == BlockTypes.STONE_BUTTON || signLocation.getBlockType() == BlockTypes.WOODEN_BUTTON) {
```

`&&` binds tighter than `||`, so this reads as *(button lifts allowed and it is a stone button) or
it is a wooden button*. Turning `allow-button-lifts` off left every wooden button working. The same
expression appears again in `findDestination`, with the same effect on which floors could be
reached.

**Rewrite:** the setting is checked once, on its own, and every kind of button obeys it.

### 100. A lift looked for headroom above the sign rather than above the rider

The lift measures the landing in the rider's own column, then makes an exception for a sign hung at
head height:

```java
if (foundFree == 1 && checkBlock(orginalDestination.getRelative(Direction.UP))) {
```

`orginalDestination` is the far sign's own block, so the exception asks about the block above the
sign while everything around it asks about the block above the rider. Standing anywhere but
directly in the sign's column, a rider was let out into a solid block, or refused a landing that
had room.

**Rewrite:** every part of the measurement is taken in the column the rider is actually standing in.

### 101. A lift would not go below y=0

```java
if (destination.getY() == 0) {
    break;
}
```

Written when zero was the bottom of the world. Since 1.18 it is the middle of one, so a lift in a
deep build reported no floor rather than finding the floor forty blocks further down.

**Rewrite:** the world says where its floor is.

### 102. A button lift could only reach floors that also had a button

`findDestination` scans the column of whatever was clicked, and where that is a button it hops two
blocks behind each button it finds to reach the sign. A floor with a sign but no button is
invisible to it, so a shaft called by buttons had to have a button at every floor even where nobody
could arrive at one.

**Rewrite:** the floors are looked for in the sign's own column, whatever was touched to start the
journey. A button is a way of reaching a sign, not a floor in its own right.

### 103. Holding crouch dropped a rider through the whole building

Crouching on a `[Lift UpDown]` pad is checked on the move event, which fires many times a second,
and the rider lands on the next floor's pad still crouching. Nothing stopped it repeating, so a
single press went down as many floors as the shaft had.

**Rewrite:** a rider carried by jumping or crouching is not carried again for half a second, which
is long enough to let go and short enough not to be noticed by anybody using the lift normally.

### 104. A gate's search followed fences out of the area its sign could see

`Gate.searchColumn` recurses into its neighbours, and their neighbours, with nothing bounding it to
the box the sign searched:

```java
for (Direction dir : BlockUtil.getDirectHorizontalFaces()) {
    Location<World> sideBlock = temp.getRelative(dir);
    Location<World> aboveBlock = sideBlock.getRelative(Direction.UP);
    ...
    state = searchColumn(aboveBlock, columns, state, exclude);
```

A gate built into a long fence line took the whole fence line with it, at whatever distance, and
paid for it out of the nearest chest. The `search-radius` setting bounded where a gate could be
found and not how far it then went.

**Rewrite:** the run of gate is followed only within the box the sign reaches. What the setting
limits is the whole of the gate, not merely where it starts.

### 105. Two gates one above the other were treated as one gate

```java
public boolean equals(Object o) {
    return o instanceof GateColumn && ((GateColumn) o).topBlock.getX() == topBlock.getX()
            && ((GateColumn) o).topBlock.getZ() == topBlock.getZ();
}
```

A column is identified by where it stands and not by what it hangs from, so a portcullis over a
doorway and another over the balcony above it were the same column. Whichever was found first was
worked and the other was quietly skipped.

**Rewrite:** a column is where it stands and the block it hangs by, so two gates stacked in one
place are two gates.

### 106. Only the plain gate could be made clickable

`Gate` reads a trailing `C` off any gate name:

```java
boolean clickable = line2.endsWith("c");
```

but `getValidSigns` lists `[Gate]C` and no other, and a sign not on that list is refused as it is
written. `[GlassGate]C` and the rest were parsed for by code that could never be reached.

**Rewrite:** every gate sign has a clickable form. Accepting more than the old list did cannot break
a sign somebody has already built.

### 107. Clicking a gate's fence with no chest nearby threw

```java
BlockBag bag = getBlockBag(searchLocation);

if (bag == null) {

}

for (Map.Entry<Set<GateColumn>, BlockState> gateEntry : ...) {
    toggleColumns(gateEntry.getValue(), searchLocation, human, gateEntry.getKey(), null, bag);
```

The check is there, the body of it is empty, and the null goes straight on into `toggleColumn`,
which calls `bag.has(...)`. The path through the sign has a real check and a real message; the path
through the fence has the shape of one.

**Rewrite:** there is always a stockpile. Where no chest is near, it is one that holds nothing, and
a gate asking it for a block is told no in the ordinary way.

## The toggled area

### 108. A limit of zero areas meant none at all rather than no limit

The setting says what it is for in its own description — *"The maximum amount of areas that a
namespace can own. 0 to disable limit."* — and then:

```java
if (files == null) return quota > 0 ? -1 : 0;
...
return count < quota ? -1 : count;
```

with the caller refusing on anything but `-1`. At a quota of zero, `count < 0` is never true and
the no-folder case answers zero rather than `-1`, so an operator who followed the description
stopped every player on the server from saving an area, including players who had none.

**Rewrite:** zero is no limit, in both of the area settings, which is what both have always said
they meant.

### 109. Redstone drove a toggled area backwards from every other area mechanic

`ComplexArea` passes the signal straight in as the state:

```java
boolean state = checkToggleState(cachedData);
if (forceState != null) {
    state = forceState;
}

if (state) {
    // ... take the area away
```

but `state` means *the area is already standing*, so power arriving says it is up and the mechanic
takes it down. A lever wired to a toggled area removed the building when thrown and put it back
when released. `Gate`, reading the same signal from the same listener, does the opposite.

**Rewrite:** power arriving puts the area up and power leaving takes it away, the same way it shuts
a door and closes a gate.

### 110. The permission for the more dangerous area sign was the lesser grant

```java
if (!createPermissions.hasPermission(player)
        && !("[SaveArea]".equals(line) && createSavePermissions.hasPermission(player))) {
```

`craftbook.area.create.save` is an alternative to `craftbook.area`, not an addition to it. So
somebody granted only the narrower-sounding node could not make an ordinary `[Area]` sign but
could make a `[SaveArea]` one — the sign that overwrites the stored copy of an area every time it
is used, and the only one that can destroy somebody's saved work.

**Rewrite:** the base permission builds either sign, and the save variant takes that plus its own.

### 111. Listing the areas of somebody who had none threw

```java
String[] files = new File(new File(..., "areas"), namespace).list();
List<Text> contents = new ArrayList<>();
for (String file : files)
```

`File#list` answers null for a directory that is not there, which is the state every player is in
before they save their first area. The first thing a new player could do with the command was
produce a stack trace.

**Rewrite:** a namespace nobody has saved anything under holds no areas and says so.

### 112. Every area command threw when run from the console

All three open by casting:

```java
String namespace = args.<String>getOne("namespace").orElse(((Player) src).getUniqueId().toString());
```

The cast is unchecked and there is nothing above it that keeps a console out. Listing and deleting
are exactly the things an operator wants to do from the console, and neither could be.

**Rewrite:** the commands say who they need. Listing and deleting work from the console with a
namespace named; picking out corners and saving want somebody standing in the world, and say so
rather than throwing.

### 113. An area saved as global and a sign saying global were two different folders

The sign path folds the namespace up:

```java
namespace = "GLOBAL";
```

while the save command compares it down, and only for deciding whether the quota applies:

```java
if (area.maxPerUser.getValue() >= 0 && !"global".equals(namespace) && ...)
```

so `/area save door global` writes into a folder called `global` and skips the quota, and a sign
reading `global` looks in `GLOBAL` and finds nothing. On a filesystem that tells case apart — which
is to say on the servers this ran on — the shared areas could be saved somewhere no sign could
read them.

**Rewrite:** the shared namespace has one spelling everywhere, and a namespace is compared without
regard to case wherever it is compared at all.

### 114. Every failure was a stack trace on the console and silence to the player

```java
} catch (Exception e) {
    e.printStackTrace();
}

return false;
```

One catch around the whole of the toggle. A missing area, an unreadable file and a full disk were
the same nothing to whoever clicked the sign, and the same untraceable stack trace on standard
output rather than in the plugin's own log.

**Rewrite:** each thing that can fail says which thing it was, to the person who set it off. What
goes to the log is what nobody standing at the sign could have fixed, such as a file that will not
open.

## The ejector and the reverser

### 115. A one-way reverser worked in two directions out of four

`CartReverser` decides whether a cart is already going the way its sign faces by taking the floor
of a normalised velocity:

```java
Vector3d normalVelocity = minecart.getVelocity().normalize();
case SOUTH:
    if (normalVelocity.getFloorZ() != 1) {
        minecart.setVelocity(minecart.getVelocity().mul(-1));
    }
```

A cart travelling due south on flat straight rail normalises to exactly `(0, 0, 1)` and passes. Give
it any velocity on another axis — a curve, a slope, the moment after a booster — and the normalised
z falls short of 1, floors to 0, and the cart is turned back. The same cart heading north normalises
to about `-0.95`, which floors to `-1` and passes, because flooring rounds away from zero on one
side and towards it on the other. So a northbound or westbound one-way tolerated a cart that was
not perfectly aligned and a southbound or eastbound one did not, and a reverser at the top of a
slope bounced everything that climbed it.

**Rewrite:** the cart's heading is the axis it is moving along fastest, and a cart is let through
when that heading is the way the sign looks. Which way round the sign faces makes no difference to
how forgiving it is.

### 116. An ejector read a filter off a sign belonging to another mechanic

```java
if (blocks.hasSign()) {
    String line3 = blocks.getSign().get(Keys.SIGN_LINES)...
    if (!line3.isEmpty() && !CartSorting.match(minecart, line3)) {
        return false; // Shouldn't eject
    }
}
```

Any sign at all, not merely an `[Eject]` one. The message sign is the mechanic with no block of its
own, so hanging one under an ejector is a reasonable thing to build — say what the stop is and then
turn everybody out. Under that arrangement the ejector read the message as a cart filter, failed to
match it against anything, and quietly stopped ejecting. The builder saw a working sign and a
mechanism that had stopped working, with nothing connecting the two.

**Rewrite:** the filter is read only off a sign that names the ejector. A sign naming something else
leaves the ejector to empty every cart, which is what an ejector with no sign of its own does.

## The habits every cart has

### 117. A crossroads normalised a velocity without asking whether there was one

`MoreRails` sends a cart straight across a pressure plate by pointing its existing velocity
somewhere faster:

```java
minecart.setVelocity(minecart.getVelocity().normalize().mul(4d));
```

flow-math refuses the zero vector outright — `throw new ArithmeticException("Cannot normalize the
zero vector")` — and nothing above this asks whether the cart is moving. A cart whose velocity has
already been zeroed on the same event, by a station or a delay or a direction block built on the
plate, takes the mechanic down with an exception instead of being left where it is.

**Rewrite:** a cart with no direction has no direction to be sent in, so it is left alone. The
domain's own `Vec3d.normalise` answers zero for the zero vector rather than throwing, so this is
the only place that has to think about it.

### 118. A storage cart that could not hold a whole stack duplicated it

```java
Collection<ItemStackSnapshot> rejects = chestMinecart.getInventory()
        .offer(item.get(Keys.REPRESENTED_ITEM).get().createStack()).getRejectedItems();
if (rejects.isEmpty()) {
    item.remove();
}
```

`offer` puts in as much as fits and hands back the remainder. So a cart with room for four of a
stack of twelve takes four, is handed eight back, finds the rejects not empty, and leaves the
stack of twelve lying on the ground. Sixteen items where there were twelve, and the cart can be
run over the same stack until it is full.

**Rewrite:** a stack is taken whole or not at all. A cart with no room for the lot leaves it where
it lies, which is also what a player watching would expect to see.

### 119. Damage-only still destroyed the carts it hit

```java
if (damageOnly.getValue() && (!(entity instanceof Living) && !(entity instanceof Minecart))) {
    return;
}
...
} else {
    entity.remove();
}
```

The guard lets a minecart through as though it were something that could be damaged, and the
branch below then removes it, because a minecart is not `Living`. So the setting whose description
is *"Only damage entities, don't remove them"* removed every cart a laden cart caught up with,
which is the one thing on a railway that is expensive to lose.

**Rewrite:** hurting only means nothing is removed. What cannot be hurt is left alone, carts
included.

### 120. A cart was taken out from under whoever climbed in next

`ExitRemover` waits two ticks after somebody gets out and then removes the cart, and what it checks
in between is only whether the cart is still there:

```java
public void run() {
    if(minecart.isRemoved()) return;
    ...
    minecart.remove();
}
```

Two ticks is long enough for the next person in the queue to get in, and they were taken out of a
cart that then vanished. The delay exists because the rider is not clear of the cart when the event
fires, so it cannot simply be dropped.

**Rewrite:** the cart is taken away only if it is still empty when the wait is over. A cart
somebody is sitting in is left alone, which leaves one cart behind rather than dropping a player on
the platform.

## The pipes

### 121. A pipe that could not deliver everything lost what it was carrying

`Pipes` hands a stack to a container and keeps whatever comes back:

```java
InventoryTransactionResult result = inventory.get().offer(itemStack);
if (!result.getRejectedItems().isEmpty()) {
    for (ItemStackSnapshot snapshot : result.getRejectedItems()) {
        itemStack = snapshot.createStack();
    }
}
```

The loop assigns each rejected stack over the one before it, so where more than one came back only
the last survives and the rest are gone. It happens exactly when a pipe is under load — a chest
with two part-filled slots of different things is the ordinary case — and it destroys items rather
than failing to move them.

**Rewrite:** what a container will not take is carried on to the next way out, and what nothing
will take stays where it was. Nothing is offered anywhere until there is somewhere to offer it to,
so there is never a leftover with nowhere to go.

### 122. The only limit on how far a pipe ran was catching a stack overflow

```java
try {
    itemStack = doPipeIteration(location, itemStack, direction, traversed);
} catch (StackOverflowError e) {
    CraftBookPlugin.spongeInst().getLogger()
            .error("Pipe overflow. Please report this issue to the developers with images of setup.");
}
```

Following a pipe recursed once per block with no bound, and the bound that existed in practice was
the thread's stack running out. An `Error` caught and carried on from leaves the run half done: some
of the stack delivered, the rest lost with the frame it was held in. The message asks the builder to
report it, which says plainly that nobody meant this to be the limit.

**Rewrite:** following a pipe is a queue rather than a recursion, and how far it goes is a setting.
A pipe past its limit carries as far as the limit reaches and says it was cut short.

### 123. A pipe read a property it had not checked was there

```java
PoweredProperty poweredProperty = source.getLocation().getProperty(PoweredProperty.class).orElse(null);
if (poweredProperty.getValue() != null && poweredProperty.getValue())
```

The `orElse(null)` is written as though the absence had been thought about, and then the next line
dereferences it. Every block update beside a sticky piston went through here, so anything without
that property threw.

**Rewrite:** the pipe runs on the edge where power arrives, which the server reports directly, and
nothing has to ask a block what it is carrying.

### 124. A pipe network was remembered until the server stopped, and crashed when it was wrong

`MegaPipes` keeps every network it has ever built in a set, loads more whenever a chunk loads, and
then:

```java
@Listener
public void onChunkUnload(UnloadChunkEvent e) {
}
```

Nothing is ever released. A world explored is a set that only grows, and every pipe run walks it
looking for the one that contains a position.

Where the remembered shape and the world disagreed, it did not recover:

```java
if (destination == null) {
    throw new IllegalStateException("No destination at " + dest + " when there should have been");
}
```

which is thrown from inside the extractor's own tick.

**Rewrite:** what a pipe reaches is remembered as an answer that may be thrown away rather than as a
picture of the world that must be kept true. A block changing forgets every answer that mentioned
it and the next pulse works it out again, so there is nothing to drift, nothing to reconcile, and
nothing that has to be right for the server to keep running.


### 125. Deleting a variable broke every chip that mentioned it, once a tick

All three of upstream's variable chips read their variable like this:

```java
double existing = Double.parseDouble(VariableManager.instance.getVariable(var, key));
```

`getVariable` is a map lookup and answers `null` for a variable that is not there, so this is
`Double.parseDouble(null)` — a `NullPointerException`, not a `NumberFormatException`.

Each of the three then fails to catch it. `ItemCounter.trigger` has no `try` at all.
`NumericModifier.trigger` has one, but it catches `NumberFormatException`, which is the exception
this cannot throw. `IsAtLeast` is the worst of the three, because it is self-triggering: the read
is in `think`, so once the variable is gone the chip throws on **every tick** rather than once.

Getting there takes one command. A sign naming an unknown variable is refused when it is written,
which is what makes this look unreachable — but nothing stops the variable being erased
afterwards, and `/var erase` does not go looking for the signs that named it. It clears the IC
cache and leaves the chips in the world.

**Rewrite:** a variable is read through `Variables#number`, which answers `OptionalDouble` and is
empty both for a variable that does not exist and for one holding something that is not a number.
A chip cannot usefully tell those apart, and each decides what to do with nothing: the modifier
reports failure on its output, the comparison goes low, and the item counter still reports what it
found in the chest and simply does not record it.

### 126. Asking for a remainder by zero poisoned a variable permanently

`MathFunction.parseNumber` guards division:

```java
case DIVIDE :
    if (amount == 0) {
        return initial;
    }
    initial /= amount;
    break;
```

and then does not guard the modulo directly beneath it:

```java
case MOD :
    initial %= amount;
    break;
```

In Java `x % 0` is `NaN`. What happens next is what makes this worth recording rather than merely
noting. `String.valueOf(NaN)` is `"NaN"`, which is stored; `Double.parseDouble("NaN")` succeeds and
returns `NaN`, so nothing downstream ever reports a problem; and every arithmetic operation on
`NaN` is `NaN`, so no function a builder can write will move the variable off it again. The value
also passes `VARIABLE_VALUE_PATTERN`, so it is written to `variables.yml` and survives a restart.

A single `[VAR100]` sign reading `%:0` therefore destroys a variable for good, silently, and the
only repair is a command.

**Rewrite:** both zero cases leave the number alone, for the same reason: neither has an answer,
and a counter that stayed where it was is something a build can carry on from. Beyond that, the
store refuses to hold a value that is not finite at all, so nothing can put a variable into a state
no arithmetic will move it out of.

### 127. A whole number came back with a decimal point on it, and a large one in scientific notation

Both chips that write a number back do this:

```java
String val = String.valueOf(currentValue);
if (val.endsWith(".0"))
    val = StringUtils.replace(val, ".0", "");
```

`StringUtils.replace` replaces **every** occurrence, not the trailing one, so `10.05` — which does
not end in `.0` and so is not meant to be touched at all — is safe only because of the guard above
it. The guard is what makes the line correct, and it is doing work the call it guards should be
doing itself.

The larger problem is what it does not cover. `String.valueOf(1.0E20)` is `"1.0E20"`, which does
not end in `.0`, so a variable that grows past the point where `Double.toString` switches notation
starts reading back as something no player would recognise as a number, on a sign or in a chat
message.

**Rewrite:** `Variables#format` writes through `BigDecimal.stripTrailingZeros().toPlainString()`, so
a whole number is written without a decimal part and a large one is written as its digits. A value
is always something somebody could have typed, which matters because these are the values a
scoreboard puts in front of people.

### 128. A command controlled switch could not be given back once its sign had gone

`Control.SwitchFollower` claimed its switch name by reading line 3 as the chip loaded, and gave it
back by reading line 3 again as the chip unloaded:

```java
public void unload(ChipState state) {
    String name = nameOn(state);       // state.sign().trimmedText(2)
    if (!name.isEmpty()) {
        board.apply(state).forget(name);
    }
}
```

Reading the sign at unload assumes the sign is still there to read. It very often is not: a chip
unloads because its sign was destroyed as readily as for any other reason, and `BlockChipState#sign`
answers `SignLines.EMPTY` for a block that is no longer a sign. The name then comes back blank,
`forget` is never reached, and the switchboard goes on believing something is following that name.

The consequence is not just a leaked map entry. `Switchboard#set` refuses a name nothing is
following, which is what stops a command inventing a switch — so the leak is precisely what makes a
dead switch look alive. `/mcx120 door on` keeps reporting success, `/mcx120list` keeps listing
`door`, and nothing anywhere is driven by any of it, for as long as the server runs.

Sign breaks are safe by luck rather than by design: `BlockBreakEvent` fires before the block is
removed, so the sign is still readable there. Everything else that can take a sign away without
that event — a piston, water, an explosion, another plugin — leaks.

**Rewrite:** the chip remembers the name it claimed and gives back exactly that. What was claimed is
a fact about the chip, not about the blocks around it, so it is not something to go and re-read.

### 129. A lower case S on a sign asked for a chip that would never tick

`ICLine#fromSuffix` recognised `S` as the self-triggering flag and passed everything else through as
the mode string. A sign written `[MCX120]s` therefore resolved to the chip with self-triggering off
and a mode of `"s"` — and since no behaviour is selected by an `s`, `ICMode#parse` discarded it.

Both halves of that are silent. The chip is created, the sign is accepted, the mode is dropped
without comment, and what the builder gets is a chip that only ever acts when its inputs change. For
a chip whose whole purpose is to be driven from somewhere other than its inputs, that is a chip that
does nothing at all.

Nothing was gained by the strictness. No mode character is an `s`, and a pin permutation is six
letters from `a` to `f`, so an `s` in a suffix could never have meant anything else.

**Rewrite:** either case sets the flag. This widens what a sign may say rather than narrowing it, so
no sign that worked before reads differently now.

### 130. Editing a chip's sign left the old chip running

`ICManager#load` refuses to build a chip on a block that already has one:

```java
public Optional<ICInstance> load(Block block) {
    BlockKey key = BlockKey.of(block);
    if (bySign.containsKey(key)) {
        return Optional.empty();
    }
```

That is right for a chunk arriving twice and wrong for the other caller. `ICSignListener` ran the
same `load` after a `SignChangeEvent`, so editing the sign of a chip that was already there did
nothing at all: the sign was rewritten, the builder was told the chip had been created, and the
chip that went on running was the one built from the old text.

What survives an edit is everything the chip read once and then carried: its pin layout, its mode
characters, whether it ticks, and whatever state its logic had accumulated. So turning `[MCX120]S`
back into `[MCX120]` left a chip still ticking every tick, and the sign in front of the builder said
otherwise. Changing the model number outright left the previous chip in place under a sign naming a
different one.

The same hole has a second side. A sign edited until it no longer names a chip at all — the model
number rubbed out, the line replaced with a word — took the early return in the listener, so nothing
stopped the chip either. It went on running against a sign that no longer described it.

**Rewrite:** `ICManager#reload` unloads whatever is on the block and builds what the sign now
describes, and the listener calls that on every accepted edit, including the one that leaves no chip
behind. A refused edit does not reconcile, because a cancelled `SignChangeEvent` leaves the old text
and so the old chip is still the right one.

### 131. Rebuilding the test bed left the previous bed's chips loaded

The same defect, reached another way. `TestbedBuilder#build` clears its plane and lays a floor over
whatever was there, then calls `loadAll` on the signs it has just written.

Nothing unloads what it destroyed. Replacing a block wholesale raises no `BlockBreakEvent`, so the
chips from the previous bed stayed in `bySign` with their tick tasks still scheduled, their pins
still indexed, and their signs gone. A command-controlled chip among them kept its switch registered,
which is enough on its own to make `/mcx120 <name> on` report success while nothing anywhere is
driven — see finding 128.

Building the bed twice therefore left two generations of chips ticking, and only the newer generation
was reachable from any sign.

**Rewrite:** `ICManager#unloadWithin` stops every chip whose sign stands inside a box, and the
builder calls it over the stretch it is about to overwrite before laying a block. That stretch comes
from `Testbed#overwritten`, which measures the rigs rather than assuming a height, and
`TestbedTest` asserts every block of every rig falls inside it — a rig growing taller would
otherwise start leaving chips behind with nothing to say so.

### 132. A chip destroyed through its supporting block was never stopped, and nobody was told

`ICSignListener` unloaded the chip on whatever block a `BlockBreakEvent` named:

```java
public void onBlockBreak(BlockBreakEvent event) {
    manager.unload(event.getBlock());
}
```

A wall sign is destroyed by breaking the block it hangs on as surely as by breaking the sign, and
that raises the event for the **supporting** block. No chip is loaded there, so nothing was
unloaded; the sign popped off a moment later and the chip stayed in `bySign` with its tick task
still scheduled, its pins still indexed, and its sign gone.

That is the same leak as findings 130 and 131 reached a third way, and it carries the same
consequence for anything a chip had claimed elsewhere: a command controlled chip destroyed like this
kept its switch registered, which is enough on its own to make the command report success while
nothing is driven — finding 128.

Neither break said anything either. A chip is a build somebody made, often somebody else, and
knocking out a stone block two blocks from a sign you never looked at is an easy way to destroy one
without ever knowing you had.

**Rewrite:** the handler unloads the chip on the broken block and on every wall sign hanging from
it — up to four — and tells whoever broke it what they broke, saying so differently when it went
through the supporting block, since that is the case where they may not realise a chip was involved
at all. It runs at `HIGHEST` with `ignoreCancelled`, so a break a protection plugin refuses does not
report a chip destroyed that is still standing.

### 133. Two boat mechanics wrote values that nothing has read for years

`LandBoats` set a boat to work on dry land. `SpeedModifiers` set its top speed and its two
decelerations. Both were `@Module` mechanics an operator could switch on, and both did nothing.

The fork called Sponge 7's `Boat.setMoveOnLand`, `setMaxSpeed`, `setOccupiedDeceleration` and
`setUnoccupiedDeceleration`. Those are not vanilla concepts. They are fields CraftBukkit added years
ago and Sponge copied, and CraftBukkit's own comment beside them says what became of them:

```java
// PAIL: Some of these haven't worked since a few updates, and since 1.9 they are less and less applicable.
public double maxSpeed = 0.4D;
public double occupiedDeceleration = 0.2D;
public double unoccupiedDeceleration = -1;
public boolean landBoats = false;
```

On Paper 26.2 those four fields are declared in `AbstractBoat` and **read nowhere at all** —
`CraftBoat` writes them and the boat's movement code never looks. On SpongeVanilla only `maxSpeed`
survives, through one `@ModifyConstant` on `floatBoat`; `moveOnLand` and both decelerations are
stored on the mixin and never consulted.

So an operator switching either on got a setting that looked like it worked, changed nothing, and
gave them no way to tell which. That is the worst shape a setting can have.

**Rewrite:** neither is ported, and `BoatHabits` says so where somebody looking for them will read
it. Making boats work on land is possible — it means moving the boat this plugin's own way, every
tick, rather than asking the game to — but that is a new mechanic with new behaviour to agree on
rather than a port, so it is not smuggled in under an old name.

### 134. A light switch reached further one way than the other, and turned one light too many

`LightSwitch` swept a box for torches to turn:

```java
for (int x = -maxRange; x < maxRange; x++)
```

Strictly less than, on all three axes. So a switch set to reach ten reached ten blocks west and
nine east, ten down and nine up — a room lit from a switch on its east wall left a column dark at
the far end, and nothing about the sign said why.

The limit on how many it turned was checked in the wrong place too:

```java
toggledLights++;
// ... turn the torch ...
if (toggledLights > maxLights) return;
```

Counted, turned, then tested — and tested for *greater than*. A switch allowed twenty turned
twenty-one before stopping.

Which twenty-one was also arbitrary. The sweep ran in `x`, then `y`, then `z` order from the
corner, so the torches that got turned were the ones furthest west and down, not the ones near the
switch. A builder clicking a switch in a large room saw a patch light up somewhere behind them.

**Rewrite:** `LightSwitches.reach` returns a ball, symmetric on every axis, sorted nearest first,
and the binding stops at the limit rather than one past it. Nearest first is the part worth having:
the limit exists so one switch cannot rewrite half a world, and when it bites it should bite on the
lights furthest from the person who threw it.

### 135. Netherrack and LightNetherrack were the same mechanic, written twice

Both put a fire above a block while it was powered and took it away when it was not. Side by side:

```java
// Netherrack
if (loc.getBlockType() == BlockTypes.FIRE || loc.getBlockType() == BlockTypes.AIR)
    loc.setBlock(powered ? FIRE : AIR);

// LightNetherrack
if (powered)  { if (fireLoc.getBlockType() == AIR)  fireLoc.setBlock(FIRE); }
else          { if (fireLoc.getBlockType() == FIRE) fireLoc.setBlock(AIR);  }
```

The same outcome for every input, written two ways. The only real difference was that
`LightNetherrack` let an operator name the block it worked on and `Netherrack` had netherrack
hardcoded, and both were separately switchable — so a server could switch one off and still have
the other lighting fires on netherrack, with no way to tell which one was doing it.

**Rewrite:** one `Powerable.Fire`, registered under `Netherrack`, with the blocks it works on as
`mechanics.fire-blocks`. That is the more general of the two behaviours and the more familiar of
the two names.

### 136. Reading a block's power during the redstone event answers the wrong question

Not a bug in the fork — a bug in this rewrite, found by somebody standing in front of a carved
pumpkin wondering why powering it did nothing and unpowering it lit it.

`PowerableListener` hung off `BlockRedstoneEvent` and, for each block beside the change, asked the
world whether that block was powered:

```java
boolean powered = block.isBlockIndirectlyPowered() || block.isBlockPowered();
```

`BlockRedstoneEvent` is raised **by a source while it is changing**, before the world around it
reflects the change. Paper fires it from the block that is turning over — `LeverBlock`,
`ButtonBlock`, `DiodeBlock`, the wire evaluators and the rest — and the neighbours have not been
told yet. So the answer is the power that is going away, not the power that is arriving.

Every powerable therefore did the opposite of what it was asked. Switch a lever on and the light
read "still off" and stayed dark; switch it off and the light read "still on" and lit up.

It also explains a second symptom that looked unrelated: mining an unpowered source made a pumpkin
come **on**, because the reading it took was of the power the source had before it was removed.

**Rewrite:** the blocks that care are noted during the event and read one tick later, once the
change has landed. Nothing is scheduled unless a powerable is actually beside the change, so an
ordinary redstone machine costs six map lookups and no task.

The general lesson is worth keeping: an event named for a change is not a promise that the change
has happened. Anything that answers a redstone question by asking the world has to ask after the
world has settled, and the failure is silent and inverted rather than loud.


### 137. The tree lopper destroyed the tree instead of felling it

`TreeLopper.checkBlocks` in the fork took each log away with `block.removeBlock()`, which empties
the block and drops nothing at all. Felling a tree therefore cost the player the whole tree: one log
from the swing that started it, and nothing from the twenty the mechanic took.

The source says why, in a comment above the line:

```java
// TODO Switch to FakePlayer when implemented.
//block.getExtent().digBlockWith(block.getBlockPosition(), player.getItemInHand(...), ...);
block.removeBlock();
```

So it is a placeholder for an API that never arrived, not a decision. Upstream broke the same block
with `block.breakNaturally(tool)` and got the drops, the fortune on the axe and the experience right
for free.

The rule for this rewrite is the fork's behaviour where the two disagree, and this is the exception
that rule needs: the fork's behaviour here is what its own author wrote down as wrong. The rewrite
breaks every block through the server's own `breakNaturally`, so what a felled tree yields is
whatever the game says it should.

Two further pieces of the same mechanic were the fork's placeholder rather than its intent, and are
not reproduced either. Nothing wore the axe out, so a tree was free; and the run ignored the block
that was actually struck when deciding what to follow, matching instead on Sponge's `TREE_TYPE`
data, which no longer exists after the flattening and answers nothing for any wood added since 1.13
— mangrove, cherry and pale oak among them. The rewrite follows the block's own name and so gains
every wood a later version adds without being touched.

### 138. An operator could not change what a meter is held up to

`MechanicsDocument` read the ammeter's and the light meter's items through `BlockNames#block`,
which resolves a name and then filters it to things that are blocks:

```java
names.block(tree.text(AMMETER_ITEM, ...)).orElseGet(defaults::ammeterItem)
```

Both defaults — charcoal and glowstone dust — are items and not blocks, so both lookups answered
nothing and both fell back to the default. That made the setting look like it worked, because the
default is what an unconfigured server wanted anyway. Writing anything else in the file, including
the default spelt out by hand, silently reverted.

Nothing complained, since falling back to a default is what that line is for.

**Rewrite:** `BlockNames` now answers blocks and items separately, and a platform resolves each
through its own lookup — `LegacyBlocks.resolve` for blocks and `resolveItem` for items, which the
plugin already had for exactly this reason on the sign side. The meters ask for items. The tree
lopper's and the vein miner's tool lists are the same question and would have hit the same wall.

The general shape is worth noting: a setting that falls back to its default on a bad value cannot
be tested by checking the default comes out right.

### 139. Upstream's movement sensor was registered as a comment

`MC1267 sense move` is not in upstream's catalogue. Its registration is there, and it is commented
out:

```java
// FIXME registerIC("MC1267", "sense move", new MovementSensor.Factory(server), familySISO, familyAISO);
```

The class it names is complete and 136 lines long, so this is a chip that was written, wired up and
then quietly withdrawn. The reason is visible in the class:

```java
@Override
public void think(ChipState chip) {
    check();
}
```

`check()` answers whether anything nearby is moving, and `think` throws that answer away. A
self-triggering movement sensor therefore never drives its output, which is the whole point of a
sensor being self-triggering — nothing else can tell it to look, because movement is not an event
anything raises. The pulsed form works, but a builder has to pulse it faster than things move.

Ported here with the answer used, which is the one-word fix, and with the two configured lines the
other way round: what to look for on line 3 and how far on line 4, matching every other creature
sensor in this catalogue rather than upstream's reversed order for this one chip. That is normally
forbidden — the sign format is frozen — and it is safe here for exactly one reason: a chip that was
never registered has no signs anywhere in any world, so there is nothing to break. It is the only
one of the twenty-four ported alongside it that could be changed at all.

### 140. The liquid flooder's area line was documented but never read

`LiquidFlood` tells a builder its fourth line is the area:

```java
public String[] getLineHelp() {
    return new String[]{"+owater/lava", "+oradius=x:y:z offset"};
}
```

but reads both the liquid and the radius from the same line, the third:

```java
liquid = getSign().getLine(2).equalsIgnoreCase("lava") ? "lava" : "water";
radius = ICUtil.parseRadius(getSign()).toBlockPoint();
```

`parseRadius(ChangedSign)` defaults to line index 2, so it is handed the word `water` or `lava`.
`parseUnsafeRadius` calls `Double.parseDouble` on it, throws `NumberFormatException`, and the
catch in `parseRadius` swallows it and returns the default of ten on every axis.

So every liquid flooder ever built floods the same ten blocks in each direction, whatever its
fourth line says, and nothing anywhere reports that the line was ignored. The same call is made
correctly by `Spigot`, which really does keep its area on line 3, which is probably where the
confusion came from.

Fixed by reading the area from the line the sign says it is on. A world of existing flooders keeps
working: they were all flooding ten blocks, and a blank fourth line still means ten.

### 141. The block breaker reported success for blocks it had not broken

`BlockBreaker.breakBlock` returns `true` for anything that is not air, not a moving piston and not
blacklisted — and only then checks whether the block matches the filter on line 3:

```java
if (item == null || item.equalsFuzzy(BukkitAdapter.adapt(brokenData))) {
    ICUtil.collectItem(...);
    broken.setType(Material.AIR);
}

return true;
```

A breaker set to take only diamond ore therefore drives its output high while sitting over stone,
which is exactly backwards for the usual build — a breaker wired to a counter, or to a piston that
advances the seam, runs on as though it were working.

It also caches the block it works on in a field:

```java
private Block broken;
if (broken == null) { ... }
```

so a chip whose sign is rebuilt somewhere else keeps breaking the old position until the server
restarts.

Both fixed: the output says whether a block was actually taken, and the position is worked out
from the sign every time.

### 142. The driller took a block before checking there was room for it

`Driller.drillLine` collects the drops and then clears the block:

```java
ICUtil.collectItem(this, BlockVector3.at(0, 1, 0), BlockUtil.getBlockDrops(blockToBreak, tool));
brokenType = blockToBreak.getType();
blockToBreak.setType(Material.AIR);
```

`ICUtil.collectItem` drops on the floor whatever the container will not take, so a driller with a
full chest above it strips the shaft and scatters the spoil. The harvester in the same codebase
gets this right and leaves the crop standing.

Fixed by asking for room first and leaving the block alone when there is none, which is what the
rest of this catalogue does. The driller also refused nothing but bedrock; here it refuses
everything the game itself will not let a player mine, so a drill cannot quietly eat a spawner or
an end portal frame.

### 143. The terraformer paid for growth it did not cause

`BonemealTerraformer` consumes bonemeal before it knows whether anything will come of it, and
refunds it in only two of its ten cases — saplings and mushrooms:

```java
if (Tag.SAPLINGS.isTagged(b.getType())) {
    if (consumeBonemeal()) {
        if (!growTree(b, ...)) refundBonemeal();
```

The other eight paths spend a bonemeal and return whether or not the block changed, and several of
them are behind a one-in-fifteen or one-in-thirty roll that usually fails. A terraformer over an
area of plain grass therefore burns through a chest of bonemeal to produce the occasional flower.

It also carries its own table of what bonemeal does — ten hand-written cases, one of which tests
`b.getData() < 0x15` on sugar cane, a pre-flattening check that cannot be true of a modern block.
Anything the game learned to fertilise after that table was written is not in it.

Fixed by asking the game to apply the bonemeal and spending one only where it took. The table is
gone: `applyBoneMeal` on Paper and `BonemealableBlock` on Sponge both answer what the game itself
would do, so a plant added in a later version works with nothing here being changed.

### 144. The cultivator could not use a netherite hoe

`Cultivator` keeps its own list of what counts as a hoe:

```java
private static final Set<Material> hoes = Set.of(Material.WOODEN_HOE, Material.STONE_HOE,
        Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE);
```

Netherite is missing. It was added to the game in 1.16 and the list was never widened, so the best
hoe there is sits in the chest doing nothing while the chip reports failure. A builder who has just
upgraded their tool sees a working farm stop, with no message and nothing changed but the tool.

It also wears the hoe through `setDurability`, which counts up from zero on an item whose damage
the modern game keeps separately, and destroys the tool by assigning `null` into the slot rather
than letting the game do it — so an unbreaking enchantment does nothing.

Fixed by listing every hoe, netherite included, and by wearing the tool through `Damageable`, which
is what the game itself uses. An enchanted hoe now lasts as long in a chip as it would in a hand.

### 145. The animal harvester never wore out its shears

`AnimalHarvester` spends a bucket to milk a cow, and takes the wool off a sheep for nothing:

```java
if (doesChestContain(Material.SHEARS)) {
    Sheep sh = (Sheep) entity;
    if (sh.isSheared()) return false;
    if (addToChest(new ItemStack(ItemUtil.getWoolFromColour(sh.getColor()), ...))) {
        sh.setSheared(true);
        return true;
    }
}
```

`doesChestContain` only looks. Nothing is ever removed and nothing is ever damaged, so one pair of
shears in the chest shears a flock for ever — the chip checks they are there and then works for
free. The bucket beside it in the same class is taken properly, which is what makes this look like
an omission rather than a decision.

Fixed by wearing the shears a point per sheep, through the same `Damageable` path the cultivator's
hoe uses. A pair now lasts its 238 sheep and is gone.

### 146. The animal breeder spawned the babies itself

`AnimalBreeder` does not breed anything. It finds two animals of a kind, takes the food, and then
makes a third:

```java
Ageable animal = (Ageable) entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());
animal.setBaby();
((Ageable) entity).setBreed(false);
```

Four things follow from that. No experience is dropped, because nothing bred. Only one of the two
parents has its cooldown set — the other is left able to breed again immediately, so one animal
paired with a queue of others is a baby factory. A sheep's colour is copied by hand and every other
inherited trait is lost, so two brown horses give a plain one. And the baby appears whether or not
the game would have allowed the pairing at all.

It also carries its own table of what may breed and what each species eats — cows and sheep on
wheat, pigs on carrots, chickens on wheat seeds, wolves on meat. That is five species out of the
several dozen the game now breeds, and the foods are wrong even for those: a pig takes potatoes and
beetroot too, a chicken takes any seed. Everything the game has learned since is absent — horses,
llamas, rabbits, turtles, foxes, pandas, bees, goats, camels, sniffers, armadillos.

Fixed by asking the game both questions and doing neither itself. `Animals#isBreedItem` says
whether a creature would take a particular food, and `setLoveModeTicks` puts the pair in love so
that the game breeds them. Cooldowns, experience, inherited traits and every rule about what may
breed with what are the game's, so an animal added in a later version works with nothing here
changed. Same reasoning as the terraformer in finding 143.

### 147. The experience spawner would spawn as many orbs as a sign asked for

`XPSpawner` reads two numbers off its sign and bounds neither:

```java
amount = Integer.parseInt(getLine(2));
orbs = Integer.parseInt(getLine(3));
...
for (int i = 0; i < orbs; i++) { ... }
```

A sign reading `1000000` on line 4 spawns a million entities in one pulse, on the server thread,
every time the chip is triggered. It is a restricted chip, so this needs a permission — but a
restricted chip is one an operator hands to trusted builders, not one that should be able to stop
the server by typo.

Both numbers are now held to what a sign may ask for: a thousand experience an orb, sixty-four orbs
a pulse.

### 148. The documented way of writing a duration was never the way one was read

Every chip in this rewrite that takes a length of time says the same thing on its sign line, and
`docs/ics.md` prints it for each of them:

```
- **Line 3** — how long to delay, such as 20T or 2S; blank repeats at once
```

`durationMillis` split the line on a colon and read the unit out of the *second* field:

```java
String[] parts = text.split(":");
value = Long.parseLong(parts[0].trim());
...
return switch (parts[1].trim().toUpperCase(Locale.ROOT)) {
    case "T", "TICKS" -> value * MILLIS_PER_TICK;
```

So `20T` failed `Long.parseLong` and fell back to the default, silently. The only spelling that
ever worked was `20:T`, which nothing documents anywhere. A builder following the page got the
default delay and no indication that their line had been ignored — and because the default is
usually a sensible number, the chip looked as though it worked.

This is a finding against the rewrite rather than against either fork: it was introduced here. Both
spellings are read now, and telling them apart is what lets a colon also mean "and here is a second
field" — a colon followed by a unit word belongs to the number in front of it, anything else starts
a new field. That is what made widening the pulser possible without changing what any existing sign
means.

The lesson is the one `LineForms` was built for: a form is the chip's own reader asked whether it
would succeed, so a promise and a parser cannot come apart. These duration lines are
`LineForms.free()` — they promise nothing checkable, and so nothing noticed.

### 149. The plugin could not start, and every test passed

`onEnable` registered the commands before it built two of the things they need:

```java
this.lopping = new Lopping(this);

registerPermissions();
...
registerCommands(chipServices);          // line 193
...
this.seats = new Seats(this);            // line 238
this.sitting = new Sitting(this, ...);   // line 239
```

`ChairCommands` asks for both through the guarded accessors, so `sittingTarget()` threw
`IllegalStateException: Sitting is not available until enabled` and the server never got the plugin
enabled. Not conditional on any setting: it failed on every start, on every server, from the commit
that added the chair commands onwards.

Two things made it survive. The accessors are written to fail loudly on a field that has not been
assigned, which is right — but they turn an ordering mistake into a runtime error rather than a
compile error, and nothing reads `onEnable` top to bottom. And the whole of `paper`'s test suite
builds its pieces with mocks around them; **not one test had ever started the plugin**. Sixty-seven
tests passed against a plugin that could not load.

Fixed by building both before the commands, and by `PluginStartupTest`, which loads the real plugin
into a test server and asserts it enabled. It asserts almost nothing else, because the throwing is
the point. Checked by putting the ordering back and watching it fail.

Two things had to give way for that test to be possible, and both are improvements on their own:

`CraftBookPlugin` was `final`. A test server builds a plugin by subclassing it, so a final plugin
class cannot be started in a test at all. Nothing extends it and nothing is meant to.

`LegacyBlocks` built its two indexes of pre-flattening ids in a static initialiser, which walks
every material the game has twice on first touch of the class — and touching the class at all
requires the server's legacy converter, which not every environment has. They are built on first
use now, behind a holder, so a server that never reads an old spelling never builds them. That is
the common case: nearly every name a server reads is a modern one.

What is still not covered is the rest of `onEnable` on a real server. A test server does not run
Paper's command lifecycle, so the commands are built here but never registered, and the listeners
are registered but never fired.
