# The legacy fork's IC catalogue

Every integrated circuit registered by `craftbook-extra-paper/src` — the Sponge 7.3 / MC 1.12.2
fork this project rewrites from. This is a requirements document, not a design one: it exists so
the rewrite can be checked off against the whole catalogue rather than against memory of it.
Everything below is transcribed from
[`ICManager.java`](src/main/java/com/sk89q/craftbook/sponge/mechanics/ics/ICManager.java), the
fork's single registration point, plus the `RestrictedIC` marker interface each factory does or
does not implement. Nothing here is inferred from a chip's behaviour; where the source gives a
description, that description is quoted verbatim.

For what this means for the rewrite — which of these are done, which are only in this fork,
which collide with upstream's numbering — see `TODO.md` and `CLAUDE.md`. This document only
answers "what does the old source say," not "what has been ported."

## Reading this table

| Column | Where it comes from |
| --- | --- |
| Model | The first constructor argument to `ICType<>` — what goes in `[brackets]` on a sign. |
| Shorthand | The second argument — what goes after `=` on a sign, upper-cased in source. |
| Name | The third argument — the chip's display name. |
| Wiring | The pin layout code: the sixth constructor argument, or **3ISO** where the fork omits it. `ICType.getDefaultPinSet()` returns `"3ISO"` when no layout is given, and `AbstractIC` actually uses that value to pick pins at runtime — so a chip that reads like a plain one-in-one-out gate (`RandomBit`, `Clock`, the sensors) still gets three inputs and one output unless its own class overrides that. This is a real behaviour of the source, not a transcription default. |
| Restricted | **Yes** where the registered factory (or, for `Bridge`/`Door`, the specific `ForcingFactory`) implements `com.sk89q.craftbook.sponge.mechanics.ics.RestrictedIC`. That marker is what the fork's own permission node keys off: `craftbook.ic.restricted.<model>` versus `craftbook.ic.safe.<model>`. |
| Description | The fourth constructor argument, quoted exactly, including its capitalisation and any typos. |

There are **123 `registerICType` calls** in the source (two of them share a single source line —
`MCX139` and `MCX133` are back-to-back on one statement — so a naive line count under-reports
this by one), covering **122 distinct model numbers**: `MC4010` (Half Adder) is registered twice
with the identical factory, which is inert rather than meaningful — `ICManager` keeps
registrations in a `TreeSet` ordered by model alone, so the second add is silently rejected.
`MC0111` and `MC1111` are a deliberate pair: both resolve to `WirelessReceiver.Factory`, so a
receiver answers to either number.

---

## SISO-shaped chips (registered without a family, or explicitly `AISO`/`AIZO`/`SISO`)

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC1000` | `REPEATER` | Repeater | `AISO` | No | Repeats a redstone signal. |
| `MC1001` | `INVERTER` | Inverter | `AISO` | No | Inverts a redstone signal. |
| `MC1017` | `RE T FLIP` | Toggle Flip Flop RE | `3ISO` | No | Toggles output on high. |
| `MC1018` | `FE T FLIP` | Toggle Flip Flip FE | `3ISO` | No | Toggles output on low. |
| `MC1020` | `RANDOM BIT` | Random Bit | `3ISO` | No | Randomly sets the output on high. |
| `MC1025` | `TIME MODULUS` | World Time Modulus | `3ISO` | No | Outputs high when the world time is odd or when its value mod x is greater than y. |
| `MC1026` | `UNIX TIME` | Unix Time Modulus | `3ISO` | No | Outputs high when unix time is odd or when its value mod x is greater than y. |
| `MC1110` | `TRANSMITTER` | Wireless Transmitter | `AIZO` | No | Transmits a wireless redstone signal. |
| `MC1111` | `RECEIVER` | Wireless Receiver | `3ISO` | No | Receives a wireless redstone signal. |
| `MC0111` | `RECEIVER` | Wireless Receiver | `3ISO` | No | Receives a wireless redstone signal. (Alias of `MC1111`, same factory.) |
| `MC1200` | `SPAWNER` | Entity Spawner | `AISO` | **Yes** | Spawns an entity with specified data. |
| `MC1201` | `DISPENSER` | Item Dispenser | `AISO` | **Yes** | Spawns in items with specified data. |
| `MC1203` | `ZEUS BOLT` | Zeus Bolt | `AISO` | **Yes** | Strikes a location with lightning. |
| `MC1230` | `SENSE DAY` | Daylight Sensor | `3ISO` | No | Outputs high if it is day. |
| `MC1249` | `BLOCK REPLACER` | Block Replacer | `3ISO` | **Yes** | Searches a nearby area and replaces blocks accordingly. |
| `MC1253` | `FIREWORK` | Programmable Firework Display | `AISO` | No | Plays a firework show from a file. |
| `MC1420` | `CLOCK` | Clock | `3ISO` | No | Outputs high every X ticks when input is high. |
| `MC1500` | `PLAYER ONLINE?` | Player Online? | `3ISO` | No | Outputs when a specified player is online. |

## SI3O-shaped chips

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC2020` | `RANDOM 3` | Random 3-Bit | `SI3O` | No | Randomly sets the outputs on high. |
| `MC2300` | `ROM GET` | ROM Get | `SI3O` | No | Gets the memory state from a file for usage in the MemorySetter/Access IC group. |
| `MC6020` | `RANDOM 5` | Random 5-Bit | `SI5O` | No | Randomly sets the outputs on high. *(Registered under the SI3O heading in source but wired `SI5O`.)* |

## 3ISO-shaped chips

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC3002` | `AND` | And Gate | `3ISO` | No | Outputs high if all inputs are high. |
| `MC3003` | `NAND` | Nand Gate | `3ISO` | No | Outputs high if all inputs are low. |
| `MC3020` | `XOR` | Xor Gate | `3ISO` | No | Outputs high if the inputs are different |
| `MC3021` | `XNOR` | Xnor Gate | `3ISO` | No | Outputs high if the inputs are the same |
| `MC3030` | `RS-NOR` | RS-Nor Latch | `3ISO` | No | A compact RS-Nor Latch |
| `MC3031` | `INV RS-NAND` | Inverse RS-Nand Latch | `3ISO` | No | A compact Inverse RS-Nand Latch |
| `MC3032` | `JK FLIP` | JK Flip Flip | `3ISO` | No | A compact JK Flip Flop |
| `MC3033` | `RS-NAND` | RS-Nand Latch | `3ISO` | No | A compact RS-Nand Latch |
| `MC3034` | `EDGE-D` | Edge-Trigger D Flip Flop | `3ISO` | No | A compact Edge-D Flip Flop |
| `MC3036` | `LEVEL-D` | Level-Trigger D Flip Flop | `3ISO` | No | A compact Level-D Flip Flop |
| `MC3050` | `COMBO` | Combination Lock | `3ISO` | No | Outputs high if the correct combination is inputed |
| `MC3101` | `DOWN COUNTER` | Down Counter | `3ISO` | No | Decrements on redstone signal, outputs high when reset. |
| `MC3102` | `COUNTER` | Counter | `3ISO` | No | Increments on redstone signal, outputs high when reset. |
| `MC3231` | `T CONTROL ADV` | Time Control Advanced | `3ISO` | **Yes** | Changes the time of day when the clock input goes from low to high. |
| `MC3300` | `ROM SET` | ROM Set | `3ISO` | No | Sets the memory state for a file for usage in the MemorySetter/Access IC group. |
| `MC2022` | `BITSHIFT` | Bit Shift | `3ISO` | **Yes** | Shift a queue of bits in memory |
| `MC3040` | `MULTIPLEXER` | Multiplexer | `3ISO` | **Yes** | Output Input 1 or 2 depending on state of Input 0 |

## 3I3O-shaped chips

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC4000` | `FULL ADDER` | Full Adder | `3I3O` | No | A compact full-adder |
| `MC4010` | `HALF ADDER` | Half Adder | `3I3O` | **Yes** | A compact half-adder |
| `MC4100` | `FULL SUBTR` | Full Subtractor | `3I3O` | No | A compact full-subtractor |
| `MC4110` | `HALF SUBTR` | Half Subtractor | `3I3O` | No | A compact half-subtractor |
| `MC4200` | `DISPATCH` | Dispatcher | `3I3O` | No | Outputs the centre input on the appropriate outputs when input is high. |

Note: the fork's `MC4010` line quoting `"Adds 2 one bit inputs and outputs the sum"` (further
down, under "Legacy IC's") is a **second, inert registration** of the same model against the same
factory — see the counting note above.

## 3I5O-shaped chips

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC4040` | `DEMULTIPLEXER` | Demultiplexer 2-Bit | `3I5O` | No | Demultiplexes the input |

## Programmable logic

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MC5000` | `PERLSTONE` | Perlstone 3ISO Programmable Logic Chip | `3ISO` | No | 3ISO PLC programmable with Perlstone. *(Dropped from the rewrite — see CLAUDE.md scope decisions.)* |

## The fork's own catalogue — everything registered under "Legacy IC's here"

This is the block CLAUDE.md calls "64 only in this fork." Not every entry below is actually
extra-fork-only in the strict sense (`MC1202`, `MC1205`, `MC1206`, `MC1207`, `MC1240`, `MC1241`,
`MC1250`, `MC1510`, `MC2999`, `MC3040` and a few others share a model number with an upstream
chip that does something different, or the same thing under a different number) but this is
where the source itself groups them, so the table follows source order.

| Model | Shorthand | Name | Wiring | Restricted | Description |
| --- | --- | --- | --- | --- | --- |
| `MCX010` | `PULSE` | Pulse | `3ISO` | No | Sends out a Pulse when toggeled. |
| `MCX011` | `SIGNAL EXTENDER` | Signal Extender | `3ISO` | No | Extends the output time of a signal after it ends. |
| `MCX027` | `BETWEEN TIME` | Between Time | `3ISO` | No | Outputs high if the time is between the specified ticks |
| `MCX112` | `TRANSPORTER` | Transporter | `3ISO` | **Yes** | Transports players to a destination ic |
| `MCU113` | `DESTINATION` | Destination | `3ISO` | **Yes** | A destination for a Transporter |
| `MCM116` | `MOB ABOVE?` | Mob Above | `3ISO` | No | Outputs when specified entity is above |
| `MCX116` | `PLAYER ABOVE?` | Player Above | `3ISO` | No | Outputs when specified playertype is above |
| `MCX117` | `PLAYER BELOW?` | Player Below | `3ISO` | No | Outputs when specified playertype is below |
| `MCX118` | `PLAYER NEAR?` | Player Near | `3ISO` | No | Output when specified player is near. |
| `MCX119` | `MOB NEAR?` | Mob Near | `3ISO` | No | Output when a mob is near |
| `MCX120` | `COMMAND CTRL` | Command Controlled IC | `3ISO` | No | Output when command is turned on |
| `MCX121` | `PASSWORD CTRL` | Command Controlled IC | `3ISO` | No | Output when command is turned on |
| `MCX130` | `MOB ZAPPER` | Mob Zapper | `SISO` | **Yes** | Removes mobs within radius |
| `MCX131` | `HIT PLAYER ABV` | Hit Player Above | `UISO` | **Yes** | Hits players above with specified damage |
| `MCX132` | `HIT MOB ABOVE` | Hit Mob Above | `UISO` | **Yes** | Hits mobs above with specified damage |
| `MCX138` | `ITEM NEAR?` | Item Near | `3ISO` | **Yes** | Output when specified item is near. |
| `MCX139` | `HELD ITEM NEAR?` | Held Item Near | `3ISO` | **Yes** | Output when specified item held by a player is near. |
| `MCX133` | `HUMANS ONLY` | Humans Only | `SISO` | **Yes** | Removes all nonhuman entities |
| `MCX140` | `IN AREA` | In Area | `UISO` | **Yes** | Outputs if specified entity is in area |
| `MCX144` | `AREA CBWARP` | CBWarp Area | `UISO` | **Yes** | Teleports entities to CBWarp within an area *(dropped: CBWarps is on the scope-decisions dropped list)* |
| `MCX146` | `POTION AREA` | Potion Area | `AISO` | **Yes** | Applies a potion effect to players in an area |
| `MCX200` | `ENTITY SPAWNER` | Entity Spawner | `3ISO` | **Yes** | Spawns entites above itself |
| `MCX201` | `ITEM SPAWNER` | Item Spawner | `AISO` | **Yes** | Drops items above itself, with id/damage/amount |
| `MCX202` | `CHEST DISPENSER` | Chest Dispenser | `AISO` | No | Dispenses a specified item and supports color/ damage. |
| `MCX203` | `CHEST COLLECTOR` | Chest Collector | `AISO` | No | Collects dropped items within a specified radius. |
| `MCX205` | `DETECT BLOCK` | Block Detector | `AISO` | No | Detects a block above or below |
| `MCX206` | `FLEX SET` | Flex Set | `AISO` | No | Set a block at a specified location |
| `MCX207` | `BRIDGE` | Bridge | `AISO` | No | Place a set type and amount of blocks |
| `MCX208` | `DOOR` | Door | `AISO` | No | Place a set type and amount of blocks |
| `MCX209` | `BRIDGE+` | Bridge+ | `AISO` | **Yes** | Place a set type and amount of blocks *(`Bridge.ForcingFactory` — forces through obstructions; plain `Bridge` above does not)* |
| `MCX210` | `DOOR+` | Door+ | `AISO` | No | Place a set type and amount of blocks *(`Door.ForcingFactory` — despite the parallel to Bridge+, this factory does **not** implement `RestrictedIC` in source)* |
| `MCX211` | `TOGGLE BLOCK` | ToggleBlock | `AISO` | No | Toggles between two given blocks on signal change |
| `MCX213` | `HARVESTER` | Harvester | `AISO` | No | Harvest a set type and amount of blocks |
| `MCX215` | `AREA PLANTER` | Area Planter | `AISO` | No | Plants crop entities on nearby farmland. |
| `MCX216` | `PLANTER` | Planter | `AISO` | No | Plant a crop entity on nearby farmland. |
| `MCX230` | `IS IT RAIN` | Rain Sensor | `3ISO` | **Yes** | Output when Raining |
| `MCX231` | `IS IT A STORM` | Storm Sensor | `3ISO` | **Yes** | Output when Storming |
| `MCX233` | `WEATHER CONTROL` | Simple Weather Control | `3ISO` | **Yes** | When the input is toggled on, it will turn the rain/snow weather on for the set duration |
| `MCX235` | `FALSE WEATHER` | False Weather | `3ISO` | **Yes** | Send a false weather packet to nearby players |
| `MCX236` | `DIST FALSE RAIN` | Distance False Weather | `3ISO` | **Yes** | Send a false weather packet to nearby players within a given area |
| `MCX237` | `HIDE WEATHER` | Hide Weather | `3ISO` | **Yes** | Send a hide weather packet to nearby players |
| `MCX238` | `DIST HIDE RAIN` | Distance Hide Weather | `3ISO` | **Yes** | Send a hide weather packet to nearby players within a given area |
| `MCX242` | `SNOW SHOOTER` | Snow Shooter | `3ISO` | **Yes** | Shoot snowball |
| `MCX243` | `SNOW BARRAGE` | Snow Barrage | `3ISO` | **Yes** | Shoot snowballs. |
| `MCX244` | `EGG SHOOTER` | Egg Shooter | `3ISO` | **Yes** | Shoot an egg. |
| `MCX245` | `EGG BARRAGE` | Egg Barrage | `3ISO` | **Yes** | Shoot 5 eggs |
| `MCX251` | `SOUND EFFECT` | Sound Effect | `3ISO` | **Yes** | Creates a sound |
| `MCX250` | `PARTICLE` | Particle | `3ISO` | **Yes** | Emits particles |
| `MCX246` | `FIREBALL` | Fireball | `3ISO` | **Yes** | Shoot a ghast fireball |
| `MCX255` | `LIGHTNING` | Lightning | `3ISO` | **Yes** | Summons a lightning bolt when powered |
| `MCX256` | `HOLY SMITE` | Holy Smite | `3ISO` | **Yes** | Summons a lightning bolt on all entities in range |
| `MCX295` | `TRIGGER READER` | Trigger Reader | `3ISO` | No | Output the powered state of a redstone trigger |
| `MCX512` | `MESSAGENEARBY` | Message Nearby | `3ISO` | **Yes** | Message all nearby players. |
| `MCX513` | `NAMED NEARBY` | Message Named Nearby | `AISO` | **Yes** | Message a Named player nearby. |
| `MCX515` | `SERVER LOG` | Server Log | `3ISO` | **Yes** | Send Info Log to server |
| `MCX516` | `S-LOG NEARBY` | Server Log Nearby | `3ISO` | **Yes** | Logs message to the server |
| `MCX517` | `S-LOG NEARBY+` | Server Log Nearby+ | `3ISO` | **Yes** | Logs message to the server |
| `MCT233` | `WEATHER CONTROL` | Weather Control | `3ISO` | **Yes** | Set rain/ storm using 3 inputs. |
| `MCU440` | `^MONOFLOP` | Monoflop | `AISO` | No | Variable downcount timer. |
| `MCU700` | `MELODY` | Melody | `UISO` | **Yes** | Plays midi songs. |
| `MCU705` | `TUNE` | Tune | `AISO` | **Yes** | Plays short tunes written on the IC sign. |
| `MCU706` | `JUKEBOX` | Jukebox | `AISO` | **Yes** | Plays record written on the IC sign. |
| `MC1202` | `CONTAINER DISPENSER` | Container Dispenser | `3ISO` | **Yes** | Dispenses an Item when triggered. |
| `MC1205` | `SET ABOVE` | Set Block Above | `3ISO` | **Yes** | Set a block above IC Block. |
| `MC1206` | `SET BELOW` | Set Block Below | `3ISO` | **Yes** | Set a block below the IC block. |
| `MC1207` | `FLEX SET ADMIN` | Flex Set Admin | `3ISO` | **Yes** | Set a block at a specified location |
| `MC1240` | `ARROW SHOOTER` | Arrow Shooter | `AISO` | **Yes** | Shoot a single arrow when powered. |
| `MC1241` | `ARROW BARRAGE` | Arrow Barrage | `AISO` | **Yes** | Shoot 5 arrows when powered |
| `MC1250` | `FIREWORKS` | Fireworks | `AISO` | **Yes** | Shoots TNT-based fireworks |
| `MC1260` | `SENSE WATER` | Water Sensor | `3ISO` | No | Output high if water is detected. |
| `MC1261` | `SENSE LAVA` | Lava Sensor | `3ISO` | No | Output high if lava is detected. |
| `MC1262` | `SENSE LIGHT` | Light Sensor | `3ISO` | No | Output high if specified light level is detected. |
| `MC1510` | `MESSAGE PLAYER` | Player Messenger | `3ISO` | **Yes** | Send message to player. Output high if message sent successfully |
| `MC1511` | `MESSAGE ALL` | Message All | `3ISO` | **Yes** | Message All online players |
| `MC2022` | `BITSHIFT` | Bit Shift | `3ISO` | **Yes** | Shift a queue of bits in memory *(the 3ISO table above lists this too — one class, registered once, grouped by source under two different comment headings)* |
| `MC2999` | `MARQUEE` | Marquee | `SI3O` | **Yes** | Cycle through all three outputs. *(Source comment: "Was CSI30 but that pinset doesn't exist..")* |
| `MC3040` | `MULTIPLEXER` | Multiplexer | `3ISO` | **Yes** | Output Input 1 or 2 depending on state of Input 0 |
| `MC3456` | `MARQUEETRANSMIT` | Marquee Transmitter | `3ISO` | **Yes** | Transmit to a set number of receivers with the same base bandname |
| `MC4010` | `HALF ADDER` | Half Adder | `3I3O` | **Yes** | Adds 2 one bit inputs and outputs the sum *(inert second registration of the model above — see the counting note)* |
| `MC6543` | `REDCODER` | Analog Transmitter | `AISO` | No | Triggers various bands depending on input signal strength |

---

## What this leaves out

Two things this document does not cover, because the source does not carry them:

- **Per-line help.** Unlike upstream's `getLineHelp()`, none of this fork's `ICType` entries
  carry a description of what lines 3 and 4 mean — that lives, if anywhere, in each chip class's
  own `verify()`/`trigger()` logic, not in the registration table this document is drawn from.
- **A behavioural spec.** "What it does" here is the fork's own one-line summary, not a
  description of the algorithm. Porting a chip still means reading `com/minecraftonline/legacy/`
  or `com/minecraftonline/ic/`, not just this table.
