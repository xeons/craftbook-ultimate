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
