# The upstream IC catalogue

Every integrated circuit registered by `E:\Code\CraftBook` — EngineHub's CraftBook 3.10.14 for
Bukkit / MC 26.1.2, the larger of the two codebases this rewrite reads for behavioural
specification. Like `LEGACY_IC_CATALOGUE.md`, this is a requirements document: a full inventory
to check the rewrite's catalogue against, not a record of what has already been ported. See
`TODO.md` for that.

Everything is transcribed from
[`ICManager.java`](../CraftBook/src/main/java/com/sk89q/craftbook/mechanics/ic/ICManager.java)'s
`registerICs(Server)` method, upstream's single registration point (the direct counterpart of the
legacy fork's `ICManager` static block). Descriptions come from each chip's own
`getShortDescription()` where one exists — quoted verbatim — and are marked **†** where the class
carries no such method and the wording here is a summary of its `trigger()`/`getResult()` logic
instead, written for this document rather than lifted from source.

There are **131 `registerIC` call sites** in source, but one of them —
`MC1267`/`sense move`/`MovementSensor` — is commented out with `// FIXME` and never runs, so only
**130 actually register**: 127 `MC`-numbered and 3 `VAR`-numbered, the latter only firing when a
variables plugin is present, which does not apply to a from-scratch rewrite with no such
dependency. One further model is reserved but unimplemented: a comment reads `// TODO Dyed Armour
Spawner (MC1247) (Sign Title: DYE ARMOUR)` — there is no class for it. Both `MC1247` and `MC1267`
get a row below marked in italics, for completeness, but neither is a live chip in this source.

## Reading this table

| Column | Where it comes from |
| --- | --- |
| Model | The first argument to `registerIC`. |
| Shorthand | The second argument, lower-case in source. |
| Class | The factory's declaring class, under `com.sk89q.craftbook.mechanics.ic.gates.*` unless noted. |
| Families | The `ICFamily` values passed — the pin layouts a builder may choose between for that chip. Most SISO-family chips are registered under **both** `familySISO` and `familyAISO`, meaning either layout is accepted; that pairing is written out in full rather than abbreviated, since it's a real difference from the legacy fork, where a chip has exactly one layout. |
| Restricted | **Yes** where the registered factory implements `com.sk89q.craftbook.mechanics.ic.RestrictedIC`, checked directly against each class rather than trusted from the `// Restricted` comments beside the registration calls — see the note below on why. |
| Description | `getShortDescription()`, quoted, or a **†** summary where that method does not exist. |

### The `// Restricted` comments do not match the code

Source marks 35 registrations with a trailing `// Restricted` comment. The actual gate — the
`RestrictedIC` marker interface, which is what `ICManager` and the in-game IC list both check —
is implemented by 43 factory classes covering **44 models** (`SetBlockAdmin` backs both `MC1205`
and `MC1206`). The two lists disagree in both directions:

- **`MC1202` (`ContainerDispenser`, "c dispense") is commented `// Restricted` but its factory
  does not implement `RestrictedIC`.** Building one needs no special permission despite the
  comment beside its registration saying otherwise.
- **Twelve models are gated by the interface with no comment saying so at all:** `MC1208`
  (mult set), `MC1217` (pot induce), `MC1218` (block launch), `MC1226` (spigot), `MC1279`
  (player trap), `MC1269` (sns p cntns), `MC2500`–`MC2511` (`pulser`/`inv pulser`/`fe
  pulser`/`inv fe pulser`, all four), `MCX233` (weather set) and `MCT233` (weather set ad).

This document reports what the interface says, not what the comment says, and flags the twelve
under-documented ones individually below.

There is a **third state** besides restricted and unrestricted: four chips — `MC1110`
(`WirelessTransmitter`), `MC1111` (`WirelessReceiver`), `MC1270` (`Melody`) and `MC1510`
(`MessageSender`) — call `ICMechanic.hasRestrictedPermissions(...)` from inside their own
`verify()`, gating one particular sign option (an inter-server channel line, in each case) rather
than the chip as a whole. Building the plain chip needs no permission; writing that one line does.
These are marked **Conditional** rather than **Yes**.

---

## SISO-family chips

Registered under `familySISO, familyAISO` unless noted.

| Model | Shorthand | Class | Restricted | Description |
| --- | --- | --- | --- | --- |
| `MC1000` | `repeater` | `Repeater` | No | Repeats a redstone signal. |
| `MC1001` | `inverter` | `Inverter` | No | Invert a redstone signal. |
| `MC1017` | `re t flip` | `ToggleFlipFlop` | No | Toggles output on high. |
| `MC1018` | `fe t flip` | `ToggleFlipFlop` | No | Toggles output on low. |
| `MC1020` | `random bit` | `RandomBit` | No | Randomly sets the output on high. |
| `MC1025` | `server time` | `ServerTimeModulus` | No | Outputs high if time is odd. |
| `MC1110` | `transmitter` | `WirelessTransmitter` | Conditional | Transmits wireless signal to wireless recievers. |
| `MC1111` | `receiver` | `WirelessReceiver` | Conditional | Recieves signal from wireless transmitter. |
| `MC1112` | `tele-out` | `TeleportTransmitter` | No | Transmitter for the teleportation network. |
| `MC1113` | `tele-in` | `TeleportReciever` | No | Reciever for the teleportation network. |
| `MC1200` | `spawner` | `CreatureSpawner` | Yes | Spawns a mob with specified data. |
| `MC1201` | `dispenser` | `ItemDispenser` | Yes | Spawns in items. |
| `MC1202` | `c dispense` | `ContainerDispenser` | No *(commented Restricted; interface says no — see above)* | Dispenses items out of containers. |
| `MC1203` | `strike` | `LightningSummon` | Yes | Strike location with lightning! |
| `MC1204` | `trap` | `EntityTrap` | Yes | Damage nearby entities of type. |
| `MC1205` | `set above` | `SetBlockAdmin` | Yes | Sets block above IC block. † |
| `MC1206` | `set below` | `SetBlockAdmin` | Yes | Sets block below IC block. † |
| `MC1207` | `flex set` | `FlexibleSetBlock` | Yes | Sets a block at a specified distance on a specific axis. Can also hold a block at a place until low power. |
| `MC1208` | `mult set` | `MultipleSetBlock` | **Yes** *(no comment)* | Sets multiple blocks. |
| `MC1209` | `collector` | `ContainerCollector` | No | Collects items into above chest. |
| `MC1210` | `emitter` | `ParticleEffect` | Yes | Creates particle effects. |
| `MC1211` | `set bridge` | `SetBridge` | Yes | Generates a bridge out of the set materials with set size. |
| `MC1212` | `set door` | `SetDoor` | Yes | Generates a door out of the set materials with set size. |
| `MC1213` | `sound` | `SoundEffect` | Yes | Plays a sound effect on high. |
| `MC1214` | `range coll` | `RangedCollector` | No | Collects items at a range into above chest. |
| `MC1215` | `set a chest` | `SetBlockChest` | No | Sets above block from below chest. † |
| `MC1216` | `set b chest` | `SetBlockChest` | No | Sets below block from above chest. † |
| `MC1217` | `pot induce` | `PotionInducer` | **Yes** *(no comment)* | Gives nearby entities a potion effect. |
| `MC1218` | `block launch` | `BlockLauncher` | **Yes** *(no comment)* | Launches set block with set velocity. |
| `MC1219` | `auto craft` | `AutomaticCrafter` | No | Auto-crafts recipes in the above dispenser/dropper. |
| `MC1220` | `a b break` | `BlockBreaker` | No | Breaks blocks above block sign is on. † |
| `MC1221` | `b b break` | `BlockBreaker` | No | Breaks blocks below block sign is on. † |
| `MC1222` | `liq flood` | `LiquidFlood` | Yes | Floods an area with a liquid. |
| `MC1223` | `terraform` | `BonemealTerraformer` | No | Terraforms an area using bonemeal. |
| `MC1224` | `time bomb` | `TimedExplosion` | Yes | Spawn tnt with custom fuse and yield. |
| `MC1225` | `pump` | `Pump` | No | Pumps liquids into buckets in the above chest. |
| `MC1226` | `spigot` | `Spigot` | **Yes** *(no comment)* | Fills areas with liquid from below chest. |
| `MC1227` | `avd spawner` | `AdvancedEntitySpawner` | Yes | Spawns a mob with many customizations. |
| `MC1228` | `ent cannon` | `EntityCannon` | Yes | Shoots nearby entities of type at set velocity. |
| `MC1229` | `sorter` | `Sorter` | No | Sorts items and spits out left/right depending on above chest. |
| `MC1230` | `sense day` | `DaySensor` | No | Outputs high if it is day. |
| `MC1231` | `t control` | `TimeControl` | Yes | Sets time based on input. |
| `MC1232` | `time set` | `TimeSet` | Yes | Set time when triggered. |
| `MC1233` | `item fan` | `ItemFan` | No | Gently pushes items upwards. |
| `MC1234` | `planter` | `Planter` | No | Plants plantable things at set offset. |
| `MC1235` | `cultivator` | `Cultivator` | No | Cultivates an area using a hoe. |
| `MC1236` | `fake weather` | `WeatherFaker` | Yes | Fakes a players weather in radius. |
| `MC1237` | `fake time` | `TimeFaker` | Yes | Radius based fake time. |
| `MC1238` | `irrigate` | `Irrigator` | No | Irrigates nearby farmland using water in above chest. |
| `MC1239` | `harvester` | `CombineHarvester` | No | Harvests nearby crops. |
| `MC1240` | `shoot arrow` | `ArrowShooter` | Yes | Shoots an arrow. |
| `MC1241` | `shoot arrows` | `ArrowBarrage` | Yes | Shoots a barrage of arrows. |
| `MC1242` | `stocker` | `ContainerStocker` | Yes | Adds item into container at specified offset. |
| `MC1243` | `distributer` | `Distributer` | No | Distributes items to right and left based on sign. |
| `MC1244` | `animal harv` | `AnimalHarvester` | No | Harvests nearby cows and sheep. |
| `MC1245` | `cont stkr` | `ContainerStacker` | No | Stacks all items in a container to 64. |
| `MC1246` | `xp spawner` | `XPSpawner` | Yes | Spawns an XP Orb. |
| *`MC1247`* | *(reserved, "DYE ARMOUR")* | — | — | Not implemented in source; a `// TODO` comment only. |
| `MC1248` | `driller` | `Driller` | Yes | Breaks a line of blocks from the IC block. |
| `MC1249` | `replacer` | `BlockReplacer` | Yes | Searches a nearby area and replaces blocks accordingly. |
| `MC1250` | `shoot fire` | `FireShooter` | Yes | Shoots a fireball. |
| `MC1251` | `shoot fires` | `FireBarrage` | Yes | Shoots a barrage of fire. |
| `MC1252` | `flame thower` | `FlameThrower` | Yes | Makes a line of fire. |
| `MC1253` | `firework show` | `ProgrammableFireworkShow` | Yes | Plays a firework show from a file. |
| `MC1260` | `sense water` | `WaterSensor` | No | Outputs high if water is at the given offset. |
| `MC1261` | `sense lava` | `LavaSensor` | No | Outputs high if lava is at given offset. |
| `MC1262` | `sense light` | `LightSensor` | No | Outputs high if specific block is above specified light level. |
| `MC1263` | `sense block` | `BlockSensor` | No | Checks for blocks at location. |
| `MC1264` | `sense item` | `ItemSensor` | No | Detects items within a given radius |
| `MC1265` | `inv sns itm` | `ItemNotSensor` | No | Detects if an item is NOT within a given radius |
| `MC1266` | `sense power` | `PowerSensor` | No | Detects if offset block is powered. |
| *`MC1267`* | *`sense move`* | *`MovementSensor`* | — | Registration is commented out (`// FIXME`) — not active in a build of this source. |
| `MC1268` | `sns cntns` | `ContentsSensor` | No | Detects if the above container has a specific item inside it. |
| `MC1269` | `sns p cntns` | `PlayerInventorySensor` | **Yes** *(no comment)* | Detects if a certain number of players have an item in their inventory. |
| `MC1270` | `melody` | `Melody` | Conditional | Plays the MIDI file entered on the sign. |
| `MC1271` | `sns entity` | `EntitySensor` | No | Detects specific entity types in a given radius. |
| `MC1272` | `sns player` | `PlayerSensor` | Yes | Detects players within a radius. |
| `MC1273` | `jukebox` | `Jukebox` | No | Plays a Playlist. |
| `MC1275` | `tune` | `Tune` | No | Plays a tune. |
| `MC1276` | `radio station` | `RadioStation` | No | Broadcasts a playlist. |
| `MC1277` | `radio player` | `RadioPlayer` | No | Plays a radio station. |
| `MC1278` | `sentry gun` | `SentryGun` | Yes | Shoots nearby mobs with arrows. |
| `MC1279` | `player trap` | `PlayerTrap` | **Yes** *(no comment)* | Damages nearby players that fit criteria. |
| `MC1280` | `animal brd` | `AnimalBreeder` | No | Breeds nearby animals. |
| `MC1420` | `divide clock` | `ClockDivider` | No | Clock that toggles output when reset. |
| `MC1421` | `clock` | `Clock` | No | Outputs high every X ticks when input is high. |
| `MC1422` | `monostable` | `Monostable` | No | Outputs a pulse for a set amount of time on high. |
| `MC1500` | `range output` | `RangedOutput` | No | Sets output high for a random length of time, or a random number of pulses, within a range written on the sign. † |
| `MC1510` | `send message` | `MessageSender` | Conditional | Sends a pre-written message on high. |
| `MC2100` | `delayer` | `Delayer` | No | Delays signal by X seconds (or ticks if set). |
| `MC2101` | `inv delayer` | `NotDelayer` | No | Delays and inverts a signal by X seconds (or ticks if set). † |
| `MC2110` | `fe delayer` | `LowDelayer` | No | Delays a falling edge by X seconds (or ticks if set). † |
| `MC2111` | `inv fe delayer` | `NotLowDelayer` | No | Delays a falling edge and inverts it by X seconds (or ticks if set). † |
| `MC2500` | `pulser` | `Pulser` | **Yes** *(no comment)* | Fires a (choosable) pulse of high-signals with a choosable length of the signal. |
| `MC2501` | `inv pulser` | `NotPulser` | **Yes** *(no comment)* | Fires a (choosable) pulse of low-signals with a choosable length of the signal. |
| `MC2510` | `fe pulser` | `LowPulser` | **Yes** *(no comment)* | Fires a (choosable) pulse of high-signals with a choosable length of the signal. *(Source's description text is identical to `MC2500`'s; the difference is which edge starts the pulse, not stated in the string itself.)* |
| `MC2511` | `inv fe pulser` | `LowNotPulser` | **Yes** *(no comment)* | Fires a (choosable) pulse of low-signals with a choosable length of the signal. |

## SI3O-family chips

| Model | Shorthand | Class | Families | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC2020` | `random 3` | `Random3Bit` | `SI3O` | No | Randomly sets three of the outputs on high. † |
| `MC2999` | `marquee` | `Marquee` | `SI3O` | No | Sequentially sets all pins. |

## 3ISO-family chips

| Model | Shorthand | Class | Restricted | Description |
| --- | --- | --- | --- | --- |
| `MC3002` | `and` | `AndGate` | No | Outputs high if all inputs are high. |
| `MC3003` | `nand` | `NandGate` | No | NAND Gate. Outputs high if all inputs are low. |
| `MC3020` | `xor` | `XorGate` | No | Outputs high if the first two inputs are not equal; the third input is unused. † |
| `MC3021` | `xnor` | `XnorGate` | No | Outputs high if the first two inputs are equal; the third input is unused. † |
| `MC3030` | `nor flip` | `RsNorFlipFlop` | No | An RS latch built from NOR gates. † |
| `MC3031` | `inv nand latch` | `InvertedRsNandLatch` | No | An RS latch built from NAND gates, inverted so it defaults high. † |
| `MC3032` | `jk flip` | `JkFlipFlop` | No | A JK flip-flop: sets, resets or toggles the output on a falling clock edge depending on J and K. † |
| `MC3033` | `nand latch` | `RsNandLatch` | No | An RS latch built from NAND gates. † |
| `MC3034` | `edge df flip` | `EdgeTriggerDFlipFlop` | No | Carries the D input to the output on a rising clock edge; a third input forces the output low. † |
| `MC3036` | `level df flip` | `LevelTriggeredDFlipFlop` | No | Carries the D input to the output while the clock is high; a third input forces the output low. † |
| `MC3040` | `multiplexer` | `Multiplexer` | No | Outputs input 1 or input 2 depending on the state of input 0. † |
| `MC3050` | `combo` | `CombinationLock` | No | Checks combination on sign against inputs. |
| `MC3101` | `down counter` | `DownCounter` | No | Outputs high when counter reaches 0. |
| `MC3102` | `counter` | `Counter` | No | Increments on redstone signal, outputs high when reset. |
| `MC3231` | `t control adva` | `TimeControlAdvanced` | Yes | Changes the time of day when the clock input goes from low to high. † |
| `MC3300` | `ROM set` | `MemorySetter` | Yes | Sets the memory state for a file for usage in the MemorySetter/Access IC group. |

## SI3O-family chip filed among the 3ISOs in source

| Model | Shorthand | Class | Families | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC3301` | `ROM get` | `MemoryAccess` | `SI3O` | Yes | Gets the memory state from a file for usage in the MemorySetter/Access IC group. |

## 3I3O-family chips

| Model | Shorthand | Class | Restricted | Description |
| --- | --- | --- | --- | --- |
| `MC4000` | `full adder` | `FullAdder` | No | Adds three one-bit inputs (two addends and a carry-in) and outputs the sum and the carry-out. † |
| `MC4010` | `half adder` | `HalfAdder` | No | Adds two one-bit inputs and outputs the sum and the carry-out. † |
| `MC4040` | `demultiplexer` | `DeMultiplexer` | No | Demultiplexes the input. **Wired `3I3O` here — the legacy fork's `MC4040` is the same idea but wired `3I5O`; the two disagree about the pin count for this model number.** |
| `MC4100` | `full subtr` | `FullSubtractor` | No | Subtracts one bit from another with a borrow-in, and outputs the difference and the borrow-out. † |
| `MC4110` | `half subtr` | `HalfSubtractor` | No | Subtracts one one-bit input from another and outputs the difference and the borrow-out. † |
| `MC4200` | `dispatcher` | `Dispatcher` | No | Send middle signal out high sides. |

## SI5O-family chips

| Model | Shorthand | Class | Restricted | Description |
| --- | --- | --- | --- | --- |
| `MC6020` | `random 5` | `Random5Bit` | No | Randomly sets five of the outputs on high. † |

## PLCs (`VIVO` / `3I3O`)

| Model | Shorthand | Class | Families | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC5000` | `perlstone` | `PlcFactory.fromLang(Perlstone)` | `VIVO` | No | Programmable Logic Chip. *(Dropped from the rewrite — see CLAUDE.md scope decisions: PLC/Perlstone is out of scope.)* |
| `MC5001` | `perlstone 3i3o` | `PlcFactory.fromLang(Perlstone)` | `3I3O` | No | Programmable Logic Chip. *(Same drop as `MC5000`.)* |

## Xtra ICs

The four models upstream keeps under its own "Xtra ICs" heading — these overlap in concept with
this fork's `MCX230`/`MCX231`/`MCX233`/`MCT233`, though the model numbers below are upstream's,
not the fork's aliasing of them.

| Model | Shorthand | Class | Families | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MCX230` | `rain sense` | `RainSensor` | `SISO, AISO` | No | Outputs high if it is raining. † |
| `MCX231` | `storm sense` | `TStormSensor` | `SISO, AISO` | No | Outputs high if it is storming. † |
| `MCX233` | `weather set` | `WeatherControl` | `SISO, AISO` | **Yes** *(no comment)* | Set rain and thunder duration. |
| `MCT233` | `weather set ad` | `WeatherControlAdvanced` | `3ISO` | **Yes** *(no comment)* | When centre on, set rain if left high and thunder if right high. |

## Variable ICs

Only registered when a `VariableManager` instance exists — a separate variables system upstream
gates them behind.

**All three are now ported**, along with a variables store of the rewrite's own; see
`docs/variables.md` and the **The variables** section of `CLAUDE.md`. What was not carried across is
upstream's `%name%` text substitution in chat and commands, and its packet-level
`override-all-text`, both of which belong to the `Variables` mechanic rather than to these chips.

| Model | Shorthand | Class | Families | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `VAR100` | `num mod` | `NumericModifier` | `SISO, AISO` | No | Modifies a variable using the specified function. |
| `VAR170` | `at least` | `IsAtLeast` | `SISO, AISO` | No | Checks if a variable is at least... |
| `VAR200` | `item count` | `ItemCounter` | `SISO, AISO` | No | Adds to a variable the amount of items of a type counted. |

---

## What this leaves out

- **Per-line help exists in source and is not reproduced here.** Unlike the legacy fork,
  upstream's `AbstractICFactory` carries a real `getLineHelp()` alongside `getPinDescription()`
  for most chips — genuine per-sign-line documentation. It is not transcribed into this table
  because it would roughly triple its length; read the individual class under
  `com/sk89q/craftbook/mechanics/ic/gates/` for it when porting a specific chip.
- **The nineteen `†`-marked descriptions are this document's summaries, not upstream's own
  words** — those classes have no `getShortDescription()` to quote. They are drawn from each
  chip's `trigger()`/`getResult()` body and, for the standard logic gates and arithmetic chips,
  checked for consistency with the equivalent entry in `LEGACY_IC_CATALOGUE.md` where both
  codebases implement the same idea under the same model number.
