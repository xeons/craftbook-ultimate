# Integrated circuits

An integrated circuit — a chip — is a sign on a wall that does something when
redstone reaches it. Write a model number on the second line of a sign, put the
sign on a wall, and the block behind it becomes a component you can wire into.

> This page is generated from the catalogue itself. Do not edit it by hand: run
> `./gradlew generateIcDocs` instead.

## Writing the sign

```
Line 1   REPEATER        filled in for you
Line 2   [MC1000]        the model number, in brackets
Line 3   whatever the chip needs told
Line 4   whatever else it needs told
```

Line 2 is the whole declaration. Write the model number in brackets and the rest
fills itself in: line 1 becomes the chip's shorthand, so you can read at a glance
what a wall of signs is doing.

Lines 3 and 4 are what the chip is told, and they mean something different for
every chip. Each entry below says what they are for, and a dash means that chip
reads nothing there.

A line marked **required** is one the chip cannot work without. Leaving it blank is
refused as you write the sign, rather than leaving you a chip that looks built and
does nothing. Everything else has a sensible default and the entry says what it is;
leaving one of those blank is fine and you are told what you have defaulted to.

### Naming it by its shorthand instead

You can name a chip by its shorthand rather than its number, and then it goes after
an **equals sign** rather than in brackets:

```
=REPEATER            the same chip as [MC1000]
=REPEATER ST         the self-triggering form
=RE T FLIP           a shorthand may have spaces in it
```

The sign is rewritten as you place it, so one written `=REPEATER` reads back
`[MC1000]` afterwards. Both spellings reach the same chip; the brackets are not
interchangeable with the equals sign, and `[REPEATER]` names nothing.

### What goes after the brackets

Anything after the closing bracket modifies the chip. `S` asks for the
self-triggering form, and the rest is the mode:

```
[MC1000]S            self-triggering
[MC1000]!            outputs inverted
[MC1000]S!           both
```

| Character | What it does |
| --- | --- |
| `S` | Run on its own rather than waiting for redstone. |
| `!` | Invert every output the chip writes. |
| `+` | Say what the chip did to whoever is nearby. |
| `1` | Go back to off after acting rather than staying on. |
| `=` | Skip the usual step up or down when acting on the world. |
| `r` | Run the chip's effect the other way about. |
| `t` | Read the weather as a thunderstorm rather than as rain. |
| `-` | Leave anything that is not a player alone. |
| `p` | Act as a teleport pad. |
| `P` | Act as a teleport pad that insists on a pressure plate. |

Most chips read only one or two of these and ignore the rest; `!` is the one the
plugin itself applies, to whatever the chip writes. A character that means nothing
is ignored rather than breaking the sign.

An `*` may appear on a sign you did not write it on. It marks a chip whose creation
was already checked, and the plugin puts it there.

### Moving the pins about

Six letters after the brackets rename the chip's pins, so a build can be wired from
a different side: `[MC1000]badcfe` swaps them in pairs. The letters run `a` to `f`
and stand for the pins in their usual order.

### The wiring itself

Every chip has one arrangement of pins and it is **not** chosen on the sign — each
entry below says which its is. The codes mean:

| Code | Inputs | Outputs | Where they sit |
| --- | --- | --- | --- |
| `SISO` | 1 | 1 | One in, one out. The plain arrangement. |
| `3ISO` | 3 | 1 | Three in, one out. What the logic gates use. |
| `AISO` | 4 | 1 | Four in, around the sign. Any of them sets the chip off. |
| `UISO` | 4 | 1 | Four in, clustered in front. For a chip in a floor or a ceiling. |
| `AIZO` | 3 | 0 | Three in, none out. For a chip whose whole effect is on the world. |
| `SI3O` | 1 | 3 | One in, three out. |
| `SI5O` | 1 | 5 | One in, five out. |
| `3I3O` | 3 | 3 | Three in, three out. The adders and subtractors. |
| `3I5O` | 3 | 5 | Three in, five out. The demultiplexer. |

### Chips that run on their own

Some chips act on their own rather than waiting for redstone — a clock, a sensor
watching for somebody to walk past. Those have a second model number for the
self-triggering form, given as **runs on its own** in their entry, and `[MC1420]S`
asks the same of any chip that can do it.

There are **117 chips**, answering to **146 model numbers**. 48 of them are restricted, meaning they are not granted to everybody by default: those can move blocks, hurt people or reach a long way, so an operator decides who may build one.

## Every chip

| Model | Shorthand | Name | What it does |
| --- | --- | --- | --- |
| [`MC1000`](#mc1000--repeater) | `REPEATER` | Repeater | Repeats a redstone signal. |
| [`MC1001`](#mc1001--inverter) | `INVERTER` | Inverter | Inverts a redstone signal. |
| [`MC1017`](#mc1017--toggle-flip-flop-re) | `RE T FLIP` | Toggle Flip Flop RE | Toggles output on high. |
| [`MC1018`](#mc1018--toggle-flip-flop-fe) | `FE T FLIP` | Toggle Flip Flop FE | Toggles output on low. |
| [`MC1020`](#mc1020--random-bit) | `RANDOM BIT` | Random Bit | Randomly sets the output high. |
| [`MC1025`](#mc1025--world-time-modulus) | `TIME MODULUS` | World Time Modulus | Outputs high when the world time mod X is at least Y. |
| [`MC1026`](#mc1026--unix-time-modulus) | `UNIX TIME` | Unix Time Modulus | Outputs high when unix time mod X is at least Y. |
| [`MC1110`](#mc1110--wireless-transmitter) | `TRANSMITTER` | Wireless Transmitter | Transmits a wireless redstone signal. |
| [`MC1111`](#mc1111--wireless-receiver) | `RECEIVER` | Wireless Receiver | Receives a wireless redstone signal. |
| [`MC1203`](#mc1203--zeus-bolt) | `ZEUS BOLT` | Zeus Bolt *(restricted)* | Strikes an area with lightning, at a chance per block. |
| [`MC1205`](#mc1205--set-block-above) | `SET ABOVE` | Set Block Above *(restricted)* | Sets a block above the IC block. |
| [`MC1206`](#mc1206--set-block-below) | `SET BELOW` | Set Block Below *(restricted)* | Sets a block below the IC block. |
| [`MC1207`](#mc1207--flex-set-admin) | `FLEX SET ADMIN` | Flex Set Admin *(restricted)* | Sets a block at a specified location, without paying for it. |
| [`MC1230`](#mc1230--daylight-sensor) | `SENSE DAY` | Daylight Sensor | Outputs high while the world time is within the day. |
| [`MC1240`](#mc1240--arrow-shooter) | `ARROW SHOOTER` | Arrow Shooter *(restricted)* | Shoots a single arrow out of the back of the sign. |
| [`MC1241`](#mc1241--arrow-barrage) | `ARROW BARRAGE` | Arrow Barrage *(restricted)* | Shoots five arrows out of the back of the sign. |
| [`MC1249`](#mc1249--block-replacer) | `BLOCK REPLACER` | Block Replacer *(restricted)* | Swaps a block between two kinds and lets the change spread outward. |
| [`MC1250`](#mc1250--fireworks) | `FIREWORKS` | Fireworks *(restricted)* | Sets off a firework. |
| [`MC1253`](#mc1253--programmable-firework-display) | `FIREWORK` | Programmable Firework Display *(restricted)* | Plays a firework display from a script. |
| [`MC1260`](#mc1260--water-sensor) | `SENSE WATER` | Water Sensor | Outputs high if water is detected. |
| [`MC1261`](#mc1261--lava-sensor) | `SENSE LAVA` | Lava Sensor | Outputs high if lava is detected. |
| [`MC1262`](#mc1262--light-sensor) | `SENSE LIGHT` | Light Sensor | Outputs high if the specified light level is detected. |
| [`MC1420`](#mc1420--clock) | `CLOCK` | Clock | Toggles its output every X ticks. |
| [`MC1500`](#mc1500--player-online) | `PLAYER ONLINE?` | Player Online | Outputs high while a named player is logged in. |
| [`MC1510`](#mc1510--player-messenger) | `MESSAGE PLAYER` | Player Messenger *(restricted)* | Says something to one named player, wherever they are. |
| [`MC1511`](#mc1511--message-all) | `MESSAGE ALL` | Message All *(restricted)* | Says something to everybody online. |
| [`MC2020`](#mc2020--random-3-bit) | `RANDOM 3` | Random 3-Bit | Randomly sets the outputs high. |
| [`MC2022`](#mc2022--bit-shift) | `BITSHIFT` | Bit Shift | Remembers a row of bits and rotates them along. |
| [`MC2999`](#mc2999--marquee) | `MARQUEE` | Marquee | Moves one raised output along its three outputs, a step per pulse. |
| [`MC3002`](#mc3002--and-gate) | `AND` | And Gate | Outputs high if all inputs are high. |
| [`MC3003`](#mc3003--nand-gate) | `NAND` | Nand Gate | Outputs high if any input is low. |
| [`MC3020`](#mc3020--xor-gate) | `XOR` | Xor Gate | Outputs high if the inputs are different. |
| [`MC3021`](#mc3021--xnor-gate) | `XNOR` | Xnor Gate | Outputs high if the inputs are the same. |
| [`MC3030`](#mc3030--rs-nor-latch) | `RS-NOR` | RS-Nor Latch | A compact RS-Nor latch. |
| [`MC3031`](#mc3031--inverse-rs-nand-latch) | `INV RS-NAND` | Inverse RS-Nand Latch | A compact inverse RS-Nand latch. |
| [`MC3032`](#mc3032--jk-flip-flop) | `JK FLIP` | JK Flip Flop | A compact JK flip flop. |
| [`MC3033`](#mc3033--rs-nand-latch) | `RS-NAND` | RS-Nand Latch | A compact RS-Nand latch. |
| [`MC3034`](#mc3034--edge-trigger-d-flip-flop) | `EDGE-D` | Edge-Trigger D Flip Flop | A compact edge-triggered D flip flop. |
| [`MC3036`](#mc3036--level-trigger-d-flip-flop) | `LEVEL-D` | Level-Trigger D Flip Flop | A compact level-triggered D flip flop. |
| [`MC3040`](#mc3040--multiplexer) | `MULTIPLEXER` | Multiplexer | Outputs input 1 or 2 depending on the state of input 0. |
| [`MC3050`](#mc3050--combination-lock) | `COMBO` | Combination Lock | Outputs high if the correct combination is entered. |
| [`MC3101`](#mc3101--down-counter) | `DOWN COUNTER` | Down Counter | Decrements on redstone signal, outputs high on reaching zero. |
| [`MC3102`](#mc3102--counter) | `COUNTER` | Counter | Increments on redstone signal, outputs high on reaching the limit. |
| [`MC3231`](#mc3231--time-control-advanced) | `T CONTROL ADV` | Time Control Advanced *(restricted)* | Moves the world to the next morning or night when clocked. |
| [`MC3456`](#mc3456--marquee-transmitter) | `MARQUEETRANSMIT` | Marquee Transmitter | Steps along a run of numbered bands, one at a time. |
| [`MC4000`](#mc4000--full-adder) | `FULL ADDER` | Full Adder | A compact full adder. |
| [`MC4010`](#mc4010--half-adder) | `HALF ADDER` | Half Adder | A compact half adder. |
| [`MC4040`](#mc4040--demultiplexer-2-bit) | `DEMULTIPLEXER` | Demultiplexer 2-Bit | Raises the output selected by the input. |
| [`MC4100`](#mc4100--full-subtractor) | `FULL SUBTR` | Full Subtractor | A compact full subtractor. |
| [`MC4110`](#mc4110--half-subtractor) | `HALF SUBTR` | Half Subtractor | A compact half subtractor. |
| [`MC4200`](#mc4200--dispatcher) | `DISPATCH` | Dispatcher | Outputs the centre input on the selected outputs. |
| [`MC6020`](#mc6020--random-5-bit) | `RANDOM 5` | Random 5-Bit | Randomly sets the outputs high. |
| [`MC6543`](#mc6543--analog-transmitter) | `REDCODER` | Analog Transmitter | Transmits a band per redstone power level. |
| [`MCM116`](#mcm116--mob-above) | `MOB ABOVE?` | Mob Above | Outputs high while a creature is standing above the sign's support. |
| [`MCT233`](#mct233--weather-control) | `WEATHER CTRL ADV` | Weather Control *(restricted)* | Sets rain and thunder using three inputs. |
| [`MCU113`](#mcu113--destination) | `DESTINATION` | Destination *(restricted)* | Receives whoever a transporter sends to its name. |
| [`MCU440`](#mcu440--monoflop) | `^MONOFLOP` | Monoflop | Waits out a countdown, then turns on. |
| [`MCU700`](#mcu700--melody) | `MELODY` | Melody *(restricted)* | Plays a MIDI file through an adjacent note block. |
| [`MCU705`](#mcu705--tune) | `TUNE` | Tune *(restricted)* | Plays a tune written on the sign, through an adjacent note block. |
| [`MCU706`](#mcu706--jukebox) | `JUKEBOX` | Jukebox *(restricted)* | Plays a record through an adjacent jukebox. |
| [`MCX010`](#mcx010--pulse) | `PULSE` | Pulse | Sends a burst of pulses when triggered. |
| [`MCX011`](#mcx011--signal-extender) | `SIGNAL EXTENDER` | Signal Extender | Holds the output high for a while after the input ends. |
| [`MCX027`](#mcx027--between-time) | `BETWEEN TIME` | Between Time | Outputs high if the time is between the specified ticks. |
| [`MCX112`](#mcx112--transporter) | `TRANSPORTER` | Transporter *(restricted)* | Sends whoever is standing on it to a named destination. |
| [`MCX116`](#mcx116--player-above) | `PLAYER ABOVE?` | Player Above | Outputs high while a player is standing above the sign's support. |
| [`MCX117`](#mcx117--player-below) | `PLAYER BELOW?` | Player Below | Outputs high while a player is standing below the sign's support. |
| [`MCX118`](#mcx118--player-near) | `PLAYER NEAR?` | Player Near | Outputs high while a player is within range. |
| [`MCX119`](#mcx119--mob-near) | `MOB NEAR?` | Mob Near | Outputs high while a creature is within range. |
| [`MCX120`](#mcx120--command-controlled) | `COMMAND CTRL` | Command Controlled | Follows a switch that anyone may throw by command. |
| [`MCX121`](#mcx121--password-controlled) | `PASSWORD CTRL` | Password Controlled | Follows a switch that takes a password to throw. |
| [`MCX130`](#mcx130--mob-zapper) | `MOB ZAPPER` | Mob Zapper *(restricted)* | Removes creatures within range. |
| [`MCX131`](#mcx131--hit-player-above) | `HIT PLAYER ABV` | Hit Player Above *(restricted)* | Hurts players standing above it. |
| [`MCX132`](#mcx132--hit-mob-above) | `HIT MOB ABOVE` | Hit Mob Above *(restricted)* | Hurts creatures standing above it. |
| [`MCX133`](#mcx133--humans-only) | `HUMANS ONLY` | Humans Only *(restricted)* | Removes everything but players from within range. |
| [`MCX138`](#mcx138--item-near) | `ITEM NEAR?` | Item Near *(restricted)* | Outputs high while a matching stack is lying within range. |
| [`MCX139`](#mcx139--held-item-near) | `HELD ITEM NEAR?` | Held Item Near *(restricted)* | Outputs high while a player within range is holding a matching item. |
| [`MCX140`](#mcx140--in-area) | `IN AREA` | In Area *(restricted)* | Outputs high while something is inside a box measured from the sign. |
| [`MCX146`](#mcx146--potion-area) | `POTION AREA` | Potion Area *(restricted)* | Gives potion effects to whatever is in an area. |
| [`MCX200`](#mcx200--entity-spawner) | `ENTITY SPAWNER` | Entity Spawner *(restricted)* | Spawns creatures above itself. |
| [`MCX201`](#mcx201--item-spawner) | `ITEM SPAWNER` | Item Spawner *(restricted)* | Drops items above itself, out of nothing. |
| [`MCX202`](#mcx202--chest-dispenser) | `CHEST DISPENSER` | Chest Dispenser | Drops items taken out of a nearby container. |
| [`MCX203`](#mcx203--chest-collector) | `CHEST COLLECTOR` | Chest Collector | Picks up dropped items and puts them in a nearby container. |
| [`MCX205`](#mcx205--block-detector) | `DETECT BLOCK` | Block Detector | Detects a block above or below. |
| [`MCX206`](#mcx206--flex-set) | `FLEX SET` | Flex Set | Sets a block at a specified location. |
| [`MCX207`](#mcx207--bridge) | `BRIDGE` | Bridge | Places a set type and amount of blocks. |
| [`MCX208`](#mcx208--door) | `DOOR` | Door | Places a set type and amount of blocks. |
| [`MCX209`](#mcx209--bridge) | `BRIDGE+` | Bridge+ *(restricted)* | Places blocks, replacing whatever is already there. |
| [`MCX210`](#mcx210--door) | `DOOR+` | Door+ *(restricted)* | Places blocks, replacing whatever is already there. |
| [`MCX211`](#mcx211--toggle-block) | `TOGGLE BLOCK` | Toggle Block | Swaps one block between two kinds as its input changes. |
| [`MCX213`](#mcx213--harvester) | `HARVESTER` | Harvester | Gathers a grown crop out of an area into nearby containers. |
| [`MCX215`](#mcx215--area-planter) | `AREA PLANTER` | Area Planter | Plants dropped seeds across a field of ground. |
| [`MCX216`](#mcx216--planter) | `PLANTER` | Planter | Plants a dropped seed above the block the sign hangs on. |
| [`MCX230`](#mcx230--rain-sensor) | `IS IT RAIN` | Rain Sensor | Outputs high while it is raining. |
| [`MCX231`](#mcx231--storm-sensor) | `IS IT A STORM` | Storm Sensor | Outputs high while a thunderstorm is running. |
| [`MCX233`](#mcx233--simple-weather-control) | `WEATHER CONTROL` | Simple Weather Control *(restricted)* | Turns the weather on for a set duration while the input is held. |
| [`MCX235`](#mcx235--false-weather) | `FALSE WEATHER` | False Weather *(restricted)* | Shows rain to people it is not raining on. |
| [`MCX236`](#mcx236--distance-false-weather) | `DIST FALSE RAIN` | Distance False Weather *(restricted)* | Shows rain to everybody standing within a distance of the sign. |
| [`MCX237`](#mcx237--hide-weather) | `HIDE WEATHER` | Hide Weather *(restricted)* | Hides the rain from people it is raining on. |
| [`MCX238`](#mcx238--distance-hide-weather) | `DIST HIDE RAIN` | Distance Hide Weather *(restricted)* | Hides the rain from everybody standing within a distance of the sign. |
| [`MCX242`](#mcx242--snow-shooter) | `SNOW SHOOTER` | Snow Shooter *(restricted)* | Throws a single snowball. |
| [`MCX243`](#mcx243--snow-barrage) | `SNOW BARRAGE` | Snow Barrage *(restricted)* | Throws five snowballs. |
| [`MCX244`](#mcx244--egg-shooter) | `EGG SHOOTER` | Egg Shooter *(restricted)* | Throws a single egg. |
| [`MCX245`](#mcx245--egg-barrage) | `EGG BARRAGE` | Egg Barrage *(restricted)* | Throws five eggs. |
| [`MCX246`](#mcx246--fireball) | `FIREBALL` | Fireball *(restricted)* | Launches a ghast fireball, aimed by the sign. |
| [`MCX250`](#mcx250--particle) | `PARTICLE` | Particle | Shows a particle, optionally offset from the sign. |
| [`MCX251`](#mcx251--sound-effect) | `SOUND EFFECT` | Sound Effect *(restricted)* | Plays one sound, named in full or by its shorthand. |
| [`MCX255`](#mcx255--lightning) | `LIGHTNING` | Lightning *(restricted)* | Strikes one place with lightning. |
| [`MCX256`](#mcx256--holy-smite) | `HOLY SMITE` | Holy Smite *(restricted)* | Strikes everything within range with lightning. |
| [`MCX295`](#mcx295--trigger-reader) | `TRIGGER READER` | Trigger Reader | Mirrors the redstone at somewhere else in the world. |
| [`MCX512`](#mcx512--message-nearby) | `MESSAGENEARBY` | Message Nearby *(restricted)* | Says something to everybody standing within range. |
| [`MCX513`](#mcx513--message-named-nearby) | `NAMED NEARBY` | Message Named Nearby *(restricted)* | Says something to everybody within range, naming the nearest. |
| [`MCX515`](#mcx515--server-log) | `SERVER LOG` | Server Log *(restricted)* | Writes a line to the server's log. |
| [`MCX516`](#mcx516--server-log-nearby) | `S-LOG NEARBY` | Server Log Nearby *(restricted)* | Writes a line to the log naming the nearest player. |
| [`MCX517`](#mcx517--server-log-nearby) | `S-LOG NEARBY+` | Server Log Nearby+ *(restricted)* | Writes a line to the log naming everybody in range and how far off. |
| [`VAR100`](#var100--variable-modifier) | `VAR MODIFIER` | Variable Modifier | Does a sum to a variable, such as adding one to it. |
| [`VAR170`](#var170--is-at-least) | `IS AT LEAST` | Is At Least | Outputs high while a variable has reached a number. |
| [`VAR200`](#var200--item-counter) | `ITEM COUNTER` | Item Counter | Counts what is in the container above it into a variable. |

## The chips in detail

### MC1000 — Repeater

Repeats a redstone signal.

| | |
| --- | --- |
| **Write on the sign** | `[MC1000]`, or `=REPEATER` |
| **Line 3** | how long to delay, such as 20T or 2S; blank repeats at once |
| **Line 4** | — |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1000` |

### MC1001 — Inverter

Inverts a redstone signal.

| | |
| --- | --- |
| **Write on the sign** | `[MC1001]`, or `=INVERTER` |
| **Line 3** | how long to delay, such as 20T or 2S; blank inverts at once |
| **Line 4** | — |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1001` |

### MC1017 — Toggle Flip Flop RE

Toggles output on high.

| | |
| --- | --- |
| **Write on the sign** | `[MC1017]`, or `=RE T FLIP` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1017` |

### MC1018 — Toggle Flip Flop FE

Toggles output on low.

| | |
| --- | --- |
| **Write on the sign** | `[MC1018]`, or `=FE T FLIP` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1018` |

### MC1020 — Random Bit

Randomly sets the output high.

| | |
| --- | --- |
| **Write on the sign** | `[MC1020]`, or `=RANDOM BIT` |
| **Line 3** | max on its own, or min:max |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1020` |

### MC1025 — World Time Modulus

Outputs high when the world time mod X is at least Y.

| | |
| --- | --- |
| **Write on the sign** | `[MC1025]`, or `=TIME MODULUS` |
| **Line 3** | the divisor, defaulting to 2 |
| **Line 4** | the threshold, defaulting to 0 |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1025` |

### MC1026 — Unix Time Modulus

Outputs high when unix time mod X is at least Y.

| | |
| --- | --- |
| **Write on the sign** | `[MC1026]`, or `=UNIX TIME` |
| **Line 3** | the divisor, defaulting to 2 |
| **Line 4** | the threshold, defaulting to 0 |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1026` |

### MC1110 — Wireless Transmitter

Transmits a wireless redstone signal.

| | |
| --- | --- |
| **Write on the sign** | `[MC1110]`, or `=TRANSMITTER` |
| **Line 3** | the channel to transmit on *(required)* |
| **Line 4** | a namespace around the channel; uuid means your own |
| **Wiring** | `AIZO` — 3 inputs, no outputs |
| **Permission** | `craftbook.ic.safe.mc1110` |
| **Names you** | Writing `uuid` on line 4 is replaced by your own player id. |

### MC1111 — Wireless Receiver

Receives a wireless redstone signal.

| | |
| --- | --- |
| **Write on the sign** | `[MC1111]`, or `=RECEIVER` |
| **Line 3** | the channel to follow *(required)* |
| **Line 4** | a namespace around the channel; uuid means your own |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MC0111]` |
| **Permission** | `craftbook.ic.safe.mc1111` |
| **Names you** | Writing `uuid` on line 4 is replaced by your own player id. |

### MC1203 — Zeus Bolt

Strikes an area with lightning, at a chance per block.

| | |
| --- | --- |
| **Write on the sign** | `[MC1203]`, or `=ZEUS BOLT` |
| **Line 3** | the reach, one number or x,y,z, optionally =x:y:z to move the middle |
| **Line 4** | the chance out of a hundred that any one block is struck |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1203` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1205 — Set Block Above

Sets a block above the IC block.

| | |
| --- | --- |
| **Write on the sign** | `[MC1205]`, or `=SET ABOVE` |
| **Line 3** | the block to place *(required)* |
| **Line 4** | Force to replace whatever is already there |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1205` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1206 — Set Block Below

Sets a block below the IC block.

| | |
| --- | --- |
| **Write on the sign** | `[MC1206]`, or `=SET BELOW` |
| **Line 3** | the block to place *(required)* |
| **Line 4** | Force to replace whatever is already there |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1206` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1207 — Flex Set Admin

Sets a block at a specified location, without paying for it.

| | |
| --- | --- |
| **Write on the sign** | `[MC1207]`, or `=FLEX SET ADMIN` |
| **Line 3** | offset:block, such as Y+1:stone *(required)* |
| **Line 4** | h to hold the block until the input drops |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1207` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1230 — Daylight Sensor

Outputs high while the world time is within the day.

| | |
| --- | --- |
| **Write on the sign** | `[MC1230]`, or `=SENSE DAY` |
| **Line 3** | the start of the window, in ticks through the day |
| **Line 4** | the end of the window |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MC0230]` |
| **Permission** | `craftbook.ic.safe.mc1230` |

### MC1240 — Arrow Shooter

Shoots a single arrow out of the back of the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MC1240]`, or `=ARROW SHOOTER` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1240` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1241 — Arrow Barrage

Shoots five arrows out of the back of the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MC1241]`, or `=ARROW BARRAGE` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1241` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1249 — Block Replacer

Swaps a block between two kinds and lets the change spread outward.

| | |
| --- | --- |
| **Write on the sign** | `[MC1249]`, or `=BLOCK REPLACER` |
| **Line 3** | the pair, as driven|idle *(required)* |
| **Line 4** | delay:mode:physics |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1249` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1250 — Fireworks

Sets off a firework.

| | |
| --- | --- |
| **Write on the sign** | `[MC1250]`, or `=FIREWORKS` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1250` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1253 — Programmable Firework Display

Plays a firework display from a script.

| | |
| --- | --- |
| **Write on the sign** | `[MC1253]`, or `=FIREWORK` |
| **Line 3** | the show, from the plugin's fireworks folder *(required)* |
| **Line 4** | whether dropping the input cuts the show short |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1253` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1260 — Water Sensor

Outputs high if water is detected.

| | |
| --- | --- |
| **Write on the sign** | `[MC1260]`, or `=SENSE WATER` |
| **Line 3** | a vertical offset from the sign's support, defaulting to one |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1260` |

### MC1261 — Lava Sensor

Outputs high if lava is detected.

| | |
| --- | --- |
| **Write on the sign** | `[MC1261]`, or `=SENSE LAVA` |
| **Line 3** | a vertical offset from the sign's support, defaulting to one |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1261` |

### MC1262 — Light Sensor

Outputs high if the specified light level is detected.

| | |
| --- | --- |
| **Write on the sign** | `[MC1262]`, or `=SENSE LIGHT` |
| **Line 3** | the light level to compare against, defaulting to eight |
| **Line 4** | a vertical offset from the sign's support |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc1262` |

### MC1420 — Clock

Toggles its output every X ticks.

| | |
| --- | --- |
| **Write on the sign** | `[MC1420]`, or `=CLOCK` |
| **Line 3** | the period in ticks, from 3 to 1000, defaulting to 20 |
| **Line 4** | where the chip keeps its count; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MC0420]` |
| **Permission** | `craftbook.ic.safe.mc1420` |

### MC1500 — Player Online

Outputs high while a named player is logged in.

| | |
| --- | --- |
| **Write on the sign** | `[MC1500]`, or `=PLAYER ONLINE?` |
| **Line 3** | the name to look for, matching anybody whose name contains it |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MC0500]` |
| **Permission** | `craftbook.ic.safe.mc1500` |

### MC1510 — Player Messenger

Says something to one named player, wherever they are.

| | |
| --- | --- |
| **Write on the sign** | `[MC1510]`, or `=MESSAGE PLAYER` |
| **Line 3** | the account to message *(required)* |
| **Line 4** | what to say *(required)* |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1510` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC1511 — Message All

Says something to everybody online.

| | |
| --- | --- |
| **Write on the sign** | `[MC1511]`, or `=MESSAGE ALL` |
| **Line 3** | what to say *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc1511` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC2020 — Random 3-Bit

Randomly sets the outputs high.

| | |
| --- | --- |
| **Write on the sign** | `[MC2020]`, or `=RANDOM 3` |
| **Line 3** | max on its own, or min:max |
| **Line 4** | — |
| **Wiring** | `SI3O` — 1 input, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc2020` |

### MC2022 — Bit Shift

Remembers a row of bits and rotates them along.

| | |
| --- | --- |
| **Write on the sign** | `[MC2022]`, or `=BITSHIFT` |
| **Line 3** | how many bits, from 2 to 64, defaulting to eight |
| **Line 4** | the bits themselves; the chip writes these |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc2022` |

### MC2999 — Marquee

Moves one raised output along its three outputs, a step per pulse.

| | |
| --- | --- |
| **Write on the sign** | `[MC2999]`, or `=MARQUEE` |
| **Line 3** | which output to start from |
| **Line 4** | — |
| **Wiring** | `SI3O` — 1 input, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc2999` |

### MC3002 — And Gate

Outputs high if all inputs are high.

| | |
| --- | --- |
| **Write on the sign** | `[MC3002]`, or `=AND` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3002` |

### MC3003 — Nand Gate

Outputs high if any input is low.

| | |
| --- | --- |
| **Write on the sign** | `[MC3003]`, or `=NAND` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3003` |

### MC3020 — Xor Gate

Outputs high if the inputs are different.

| | |
| --- | --- |
| **Write on the sign** | `[MC3020]`, or `=XOR` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3020` |

### MC3021 — Xnor Gate

Outputs high if the inputs are the same.

| | |
| --- | --- |
| **Write on the sign** | `[MC3021]`, or `=XNOR` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3021` |

### MC3030 — RS-Nor Latch

A compact RS-Nor latch.

| | |
| --- | --- |
| **Write on the sign** | `[MC3030]`, or `=RS-NOR` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3030` |

### MC3031 — Inverse RS-Nand Latch

A compact inverse RS-Nand latch.

| | |
| --- | --- |
| **Write on the sign** | `[MC3031]`, or `=INV RS-NAND` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3031` |

### MC3032 — JK Flip Flop

A compact JK flip flop.

| | |
| --- | --- |
| **Write on the sign** | `[MC3032]`, or `=JK FLIP` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3032` |

### MC3033 — RS-Nand Latch

A compact RS-Nand latch.

| | |
| --- | --- |
| **Write on the sign** | `[MC3033]`, or `=RS-NAND` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3033` |

### MC3034 — Edge-Trigger D Flip Flop

A compact edge-triggered D flip flop.

| | |
| --- | --- |
| **Write on the sign** | `[MC3034]`, or `=EDGE-D` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3034` |

### MC3036 — Level-Trigger D Flip Flop

A compact level-triggered D flip flop.

| | |
| --- | --- |
| **Write on the sign** | `[MC3036]`, or `=LEVEL-D` |
| **Line 3** | — |
| **Line 4** | where the chip keeps its state; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3036` |

### MC3040 — Multiplexer

Outputs input 1 or 2 depending on the state of input 0.

| | |
| --- | --- |
| **Write on the sign** | `[MC3040]`, or `=MULTIPLEXER` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3040` |

### MC3050 — Combination Lock

Outputs high if the correct combination is entered.

| | |
| --- | --- |
| **Write on the sign** | `[MC3050]`, or `=COMBO` |
| **Line 3** | the combination, three characters where X means that input must be high *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3050` |

### MC3101 — Down Counter

Decrements on redstone signal, outputs high on reaching zero.

| | |
| --- | --- |
| **Write on the sign** | `[MC3101]`, or `=DOWN COUNTER` |
| **Line 3** | the limit to count to, optionally followed by :INF to keep going |
| **Line 4** | where the chip keeps its total; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3101` |

### MC3102 — Counter

Increments on redstone signal, outputs high on reaching the limit.

| | |
| --- | --- |
| **Write on the sign** | `[MC3102]`, or `=COUNTER` |
| **Line 3** | the limit to count to, optionally followed by :INF to keep going |
| **Line 4** | where the chip keeps its total; it writes this itself |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3102` |

### MC3231 — Time Control Advanced

Moves the world to the next morning or night when clocked.

| | |
| --- | --- |
| **Write on the sign** | `[MC3231]`, or `=T CONTROL ADV` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mc3231` |
| **Restricted** | Yes — not granted to everybody by default. |

### MC3456 — Marquee Transmitter

Steps along a run of numbered bands, one at a time.

| | |
| --- | --- |
| **Write on the sign** | `[MC3456]`, or `=MARQUEETRANSMIT` |
| **Line 3** | channel:first:last *(required)* |
| **Line 4** | a namespace around the channel |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc3456` |
| **Names you** | Writing `uuid` on line 4 is replaced by your own player id. |

### MC4000 — Full Adder

A compact full adder.

| | |
| --- | --- |
| **Write on the sign** | `[MC4000]`, or `=FULL ADDER` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I3O` — 3 inputs, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc4000` |

### MC4010 — Half Adder

A compact half adder.

| | |
| --- | --- |
| **Write on the sign** | `[MC4010]`, or `=HALF ADDER` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I3O` — 3 inputs, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc4010` |

### MC4040 — Demultiplexer 2-Bit

Raises the output selected by the input.

| | |
| --- | --- |
| **Write on the sign** | `[MC4040]`, or `=DEMULTIPLEXER` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I5O` — 3 inputs, 5 outputs |
| **Permission** | `craftbook.ic.safe.mc4040` |

### MC4100 — Full Subtractor

A compact full subtractor.

| | |
| --- | --- |
| **Write on the sign** | `[MC4100]`, or `=FULL SUBTR` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I3O` — 3 inputs, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc4100` |

### MC4110 — Half Subtractor

A compact half subtractor.

| | |
| --- | --- |
| **Write on the sign** | `[MC4110]`, or `=HALF SUBTR` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I3O` — 3 inputs, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc4110` |

### MC4200 — Dispatcher

Outputs the centre input on the selected outputs.

| | |
| --- | --- |
| **Write on the sign** | `[MC4200]`, or `=DISPATCH` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3I3O` — 3 inputs, 3 outputs |
| **Permission** | `craftbook.ic.safe.mc4200` |

### MC6020 — Random 5-Bit

Randomly sets the outputs high.

| | |
| --- | --- |
| **Write on the sign** | `[MC6020]`, or `=RANDOM 5` |
| **Line 3** | max on its own, or min:max |
| **Line 4** | — |
| **Wiring** | `SI5O` — 1 input, 5 outputs |
| **Permission** | `craftbook.ic.safe.mc6020` |

### MC6543 — Analog Transmitter

Transmits a band per redstone power level.

| | |
| --- | --- |
| **Write on the sign** | `[MC6543]`, or `=REDCODER` |
| **Line 3** | channel[:first:last][:T] *(required)* |
| **Line 4** | a namespace around the channel |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mc6543` |
| **Names you** | Writing `uuid` on line 4 is replaced by your own player id. |

### MCM116 — Mob Above

Outputs high while a creature is standing above the sign's support.

| | |
| --- | --- |
| **Write on the sign** | `[MCM116]`, or `=MOB ABOVE?` |
| **Line 3** | what counts as a creature; blank means anything alive |
| **Line 4** | how far to look |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCO116]` |
| **Permission** | `craftbook.ic.safe.mcm116` |

### MCT233 — Weather Control

Sets rain and thunder using three inputs.

| | |
| --- | --- |
| **Write on the sign** | `[MCT233]`, or `=WEATHER CTRL ADV` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mct233` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCU113 — Destination

Receives whoever a transporter sends to its name.

| | |
| --- | --- |
| **Write on the sign** | `[MCU113]`, or `=DESTINATION` |
| **Line 3** | the name this destination answers to *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcu113` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCU440 — Monoflop

Waits out a countdown, then turns on.

| | |
| --- | --- |
| **Write on the sign** | `[MCU440]`, or `=^MONOFLOP` |
| **Line 3** | count:rate, optionally followed by :onCount |
| **Line 4** | — |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcu440` |

### MCU700 — Melody

Plays a MIDI file through an adjacent note block.

| | |
| --- | --- |
| **Write on the sign** | `[MCU700]`, or `=MELODY` |
| **Line 3** | the MIDI file to play, or a name ending .p for a playlist *(required)* |
| **Line 4** | flags separated by colons: loop, random |
| **Wiring** | `UISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcu700` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCU705 — Tune

Plays a tune written on the sign, through an adjacent note block.

| | |
| --- | --- |
| **Write on the sign** | `[MCU705]`, or `=TUNE` |
| **Line 3** | the tune, optionally with ticks between notes in front, as 3:0c2e2g2 *(required)* |
| **Line 4** | more of the same tune, run on from line 3 |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcu705` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCU706 — Jukebox

Plays a record through an adjacent jukebox.

| | |
| --- | --- |
| **Write on the sign** | `[MCU706]`, or `=JUKEBOX` |
| **Line 3** | the record's name as the game calls it, such as 13 or mellohi *(required)* |
| **Line 4** | — |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcu706` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX010 — Pulse

Sends a burst of pulses when triggered.

| | |
| --- | --- |
| **Write on the sign** | `[MCX010]`, or `=PULSE` |
| **Line 3** | how long each pulse lasts in milliseconds, 100 to 1000 |
| **Line 4** | how many pulses, 1 to 10 |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx010` |

### MCX011 — Signal Extender

Holds the output high for a while after the input ends.

| | |
| --- | --- |
| **Write on the sign** | `[MCX011]`, or `=SIGNAL EXTENDER` |
| **Line 3** | how long to hold, such as 500, 20T or 2S |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx011` |

### MCX027 — Between Time

Outputs high if the time is between the specified ticks.

| | |
| --- | --- |
| **Write on the sign** | `[MCX027]`, or `=BETWEEN TIME` |
| **Line 3** | the start, in ticks through the day |
| **Line 4** | the end, defaulting to the whole day |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx027` |

### MCX112 — Transporter

Sends whoever is standing on it to a named destination.

| | |
| --- | --- |
| **Write on the sign** | `[MCX112]`, or `=TRANSPORTER` |
| **Line 3** | the destination to send people to *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx112` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX116 — Player Above

Outputs high while a player is standing above the sign's support.

| | |
| --- | --- |
| **Write on the sign** | `[MCX116]`, or `=PLAYER ABOVE?` |
| **Line 3** | which players, as p:Name, g:group or m:mode; blank means anyone |
| **Line 4** | radius[:height[:up]] |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ116]` |
| **Permission** | `craftbook.ic.safe.mcx116` |

### MCX117 — Player Below

Outputs high while a player is standing below the sign's support.

| | |
| --- | --- |
| **Write on the sign** | `[MCX117]`, or `=PLAYER BELOW?` |
| **Line 3** | which players; blank means anyone |
| **Line 4** | radius[:height[:up]] |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ117]` |
| **Permission** | `craftbook.ic.safe.mcx117` |

### MCX118 — Player Near

Outputs high while a player is within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX118]`, or `=PLAYER NEAR?` |
| **Line 3** | which players; blank means anyone |
| **Line 4** | how far to reach, defaulting to five blocks |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ118]` |
| **Permission** | `craftbook.ic.safe.mcx118` |

### MCX119 — Mob Near

Outputs high while a creature is within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX119]`, or `=MOB NEAR?` |
| **Line 3** | what counts; blank means anything alive |
| **Line 4** | how far to reach |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ119]` |
| **Permission** | `craftbook.ic.safe.mcx119` |

### MCX120 — Command Controlled

Follows a switch that anyone may throw by command.

| | |
| --- | --- |
| **Write on the sign** | `[MCX120]`, or `=COMMAND CTRL` |
| **Line 3** | the switch to follow *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ120]` |
| **Permission** | `craftbook.ic.safe.mcx120` |

### MCX121 — Password Controlled

Follows a switch that takes a password to throw.

| | |
| --- | --- |
| **Write on the sign** | `[MCX121]`, or `=PASSWORD CTRL` |
| **Line 3** | the switch to follow *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ121]` |
| **Permission** | `craftbook.ic.safe.mcx121` |

### MCX130 — Mob Zapper

Removes creatures within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX130]`, or `=MOB ZAPPER` |
| **Line 3** | what to remove; blank means hostile mobs |
| **Line 4** | how far to reach, defaulting to five blocks |
| **Wiring** | `SISO` — 1 input, 1 output |
| **Runs on its own as** | `[MCZ130]` |
| **Permission** | `craftbook.ic.restricted.mcx130` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX131 — Hit Player Above

Hurts players standing above it.

| | |
| --- | --- |
| **Write on the sign** | `[MCX131]`, or `=HIT PLAYER ABV` |
| **Line 3** | which players, as p:Notch, g:admin or m:ott; blank means anyone |
| **Line 4** | how hard to hit |
| **Wiring** | `UISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCU131]` |
| **Permission** | `craftbook.ic.restricted.mcx131` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX132 — Hit Mob Above

Hurts creatures standing above it.

| | |
| --- | --- |
| **Write on the sign** | `[MCX132]`, or `=HIT MOB ABOVE` |
| **Line 3** | what to hit; blank means anything that is not a player |
| **Line 4** | how hard to hit |
| **Wiring** | `UISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCU132]` |
| **Permission** | `craftbook.ic.restricted.mcx132` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX133 — Humans Only

Removes everything but players from within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX133]`, or `=HUMANS ONLY` |
| **Line 3** | — |
| **Line 4** | how far to reach, defaulting to five blocks |
| **Wiring** | `SISO` — 1 input, 1 output |
| **Runs on its own as** | `[MCZ133]` |
| **Permission** | `craftbook.ic.restricted.mcx133` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX138 — Item Near

Outputs high while a matching stack is lying within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX138]`, or `=ITEM NEAR?` |
| **Line 3** | one thing to check, as ID:stone, STACK:64, NAME:Key or LORE:quest *(required)* |
| **Line 4** | how far to reach, up to thirty blocks |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ138]` |
| **Permission** | `craftbook.ic.restricted.mcx138` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX139 — Held Item Near

Outputs high while a player within range is holding a matching item.

| | |
| --- | --- |
| **Write on the sign** | `[MCX139]`, or `=HELD ITEM NEAR?` |
| **Line 3** | one thing to check, as ID:stone, STACK:64, NAME:Key or LORE:quest *(required)* |
| **Line 4** | how far to reach, up to thirty blocks |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ139]` |
| **Permission** | `craftbook.ic.restricted.mcx139` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX140 — In Area

Outputs high while something is inside a box measured from the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MCX140]`, or `=IN AREA` |
| **Line 3** | what to look for, with a rider after a + such as pig+player *(required)* |
| **Line 4** | width:height:length[/x:y:z] |
| **Wiring** | `UISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCU140]` |
| **Permission** | `craftbook.ic.restricted.mcx140` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX146 — Potion Area

Gives potion effects to whatever is in an area.

| | |
| --- | --- |
| **Write on the sign** | `[MCX146]`, or `=POTION AREA` |
| **Line 3** | effect:seconds:strength, such as SP:5:1; INF never wears off *(required)* |
| **Line 4** | range[:x:y:z][@filter] |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCU146]` |
| **Permission** | `craftbook.ic.restricted.mcx146` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX200 — Entity Spawner

Spawns creatures above itself.

| | |
| --- | --- |
| **Write on the sign** | `[MCX200]`, or `=ENTITY SPAWNER` |
| **Line 3** | what to spawn *(required)* |
| **Line 4** | how many, defaulting to one |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Also answers to** | `MC1200` |
| **Permission** | `craftbook.ic.restricted.mcx200` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX201 — Item Spawner

Drops items above itself, out of nothing.

| | |
| --- | --- |
| **Write on the sign** | `[MCX201]`, or `=ITEM SPAWNER` |
| **Line 3** | the item to drop *(required)* |
| **Line 4** | how many, up to a stack, defaulting to one |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Also answers to** | `MC1201` |
| **Permission** | `craftbook.ic.restricted.mcx201` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX202 — Chest Dispenser

Drops items taken out of a nearby container.

| | |
| --- | --- |
| **Write on the sign** | `[MCX202]`, or `=CHEST DISPENSER` |
| **Line 3** | the item; blank or -1 means any item |
| **Line 4** | amount, with an optional @x:y:z naming a container |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Also answers to** | `MC1202` |
| **Permission** | `craftbook.ic.safe.mcx202` |

### MCX203 — Chest Collector

Picks up dropped items and puts them in a nearby container.

| | |
| --- | --- |
| **Write on the sign** | `[MCX203]`, or `=CHEST COLLECTOR` |
| **Line 3** | the item to pick up; blank means any item |
| **Line 4** | range, with an optional :x:y:z naming a container |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCZ203]` |
| **Permission** | `craftbook.ic.safe.mcx203` |

### MCX205 — Block Detector

Detects a block above or below.

| | |
| --- | --- |
| **Write on the sign** | `[MCX205]`, or `=DETECT BLOCK` |
| **Line 3** | the block to look for *(required)* |
| **Line 4** | how far down to search |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx205` |

### MCX206 — Flex Set

Sets a block at a specified location.

| | |
| --- | --- |
| **Write on the sign** | `[MCX206]`, or `=FLEX SET` |
| **Line 3** | offset:block, such as Y+1:stone *(required)* |
| **Line 4** | h to hold the block until the input drops |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx206` |

### MCX207 — Bridge

Places a set type and amount of blocks.

| | |
| --- | --- |
| **Write on the sign** | `[MCX207]`, or `=BRIDGE` |
| **Line 3** | the block to build from *(required)* |
| **Line 4** | width:length, with an optional :verticalOffset *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx207` |
| **Needs arming** | Yes — it is created inert and does nothing until its area is clear. |

### MCX208 — Door

Places a set type and amount of blocks.

| | |
| --- | --- |
| **Write on the sign** | `[MCX208]`, or `=DOOR` |
| **Line 3** | the block to build from *(required)* |
| **Line 4** | width:height, with an optional :verticalOffset *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx208` |
| **Needs arming** | Yes — it is created inert and does nothing until its area is clear. |

### MCX209 — Bridge+

Places blocks, replacing whatever is already there.

| | |
| --- | --- |
| **Write on the sign** | `[MCX209]`, or `=BRIDGE+` |
| **Line 3** | the block to build from *(required)* |
| **Line 4** | width:length, with an optional :verticalOffset *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx209` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX210 — Door+

Places blocks, replacing whatever is already there.

| | |
| --- | --- |
| **Write on the sign** | `[MCX210]`, or `=DOOR+` |
| **Line 3** | the block to build from *(required)* |
| **Line 4** | width:height, with an optional :verticalOffset *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx210` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX211 — Toggle Block

Swaps one block between two kinds as its input changes.

| | |
| --- | --- |
| **Write on the sign** | `[MCX211]`, or `=TOGGLE BLOCK` |
| **Line 3** | the pair, as driven|idle *(required)* |
| **Line 4** | one axis step from the sign's support, such as Y+1 *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx211` |

### MCX213 — Harvester

Gathers a grown crop out of an area into nearby containers.

| | |
| --- | --- |
| **Write on the sign** | `[MCX213]`, or `=HARVESTER` |
| **Line 3** | the block to harvest *(required)* |
| **Line 4** | width:length:height, with an optional /verticalOffset |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx213` |
| **Needs arming** | Yes — it is created inert and does nothing until its area is clear. |

### MCX215 — Area Planter

Plants dropped seeds across a field of ground.

| | |
| --- | --- |
| **Write on the sign** | `[MCX215]`, or `=AREA PLANTER` |
| **Line 3** | the crop to plant *(required)* |
| **Line 4** | width:length, with an optional :height |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCZ215]` |
| **Permission** | `craftbook.ic.safe.mcx215` |

### MCX216 — Planter

Plants a dropped seed above the block the sign hangs on.

| | |
| --- | --- |
| **Write on the sign** | `[MCX216]`, or `=PLANTER` |
| **Line 3** | the item to plant *(required)* |
| **Line 4** | how far above the sign's support, defaulting to one |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Runs on its own as** | `[MCZ216]` |
| **Permission** | `craftbook.ic.safe.mcx216` |

### MCX230 — Rain Sensor

Outputs high while it is raining.

| | |
| --- | --- |
| **Write on the sign** | `[MCX230]`, or `=IS IT RAIN` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx230` |

### MCX231 — Storm Sensor

Outputs high while a thunderstorm is running.

| | |
| --- | --- |
| **Write on the sign** | `[MCX231]`, or `=IS IT A STORM` |
| **Line 3** | — |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx231` |

### MCX233 — Simple Weather Control

Turns the weather on for a set duration while the input is held.

| | |
| --- | --- |
| **Write on the sign** | `[MCX233]`, or `=WEATHER CONTROL` |
| **Line 3** | how long the weather lasts once started, defaulting to a full day |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx233` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX235 — False Weather

Shows rain to people it is not raining on.

| | |
| --- | --- |
| **Write on the sign** | `[MCX235]`, or `=FALSE WEATHER` |
| **Line 3** | who sees it: blank for everybody here, p:Name or g:group |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx235` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX236 — Distance False Weather

Shows rain to everybody standing within a distance of the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MCX236]`, or `=DIST FALSE RAIN` |
| **Line 3** | how far, from one to a hundred and twenty-seven, defaulting to ten |
| **Line 4** | something to say as somebody walks into range |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ236]` |
| **Permission** | `craftbook.ic.restricted.mcx236` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX237 — Hide Weather

Hides the rain from people it is raining on.

| | |
| --- | --- |
| **Write on the sign** | `[MCX237]`, or `=HIDE WEATHER` |
| **Line 3** | who sees it: blank for everybody here, p:Name or g:group |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx237` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX238 — Distance Hide Weather

Hides the rain from everybody standing within a distance of the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MCX238]`, or `=DIST HIDE RAIN` |
| **Line 3** | how far, from one to a hundred and twenty-seven, defaulting to ten |
| **Line 4** | something to say as somebody walks into range |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ238]` |
| **Permission** | `craftbook.ic.restricted.mcx238` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX242 — Snow Shooter

Throws a single snowball.

| | |
| --- | --- |
| **Write on the sign** | `[MCX242]`, or `=SNOW SHOOTER` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx242` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX243 — Snow Barrage

Throws five snowballs.

| | |
| --- | --- |
| **Write on the sign** | `[MCX243]`, or `=SNOW BARRAGE` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx243` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX244 — Egg Shooter

Throws a single egg.

| | |
| --- | --- |
| **Write on the sign** | `[MCX244]`, or `=EGG SHOOTER` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx244` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX245 — Egg Barrage

Throws five eggs.

| | |
| --- | --- |
| **Write on the sign** | `[MCX245]`, or `=EGG BARRAGE` |
| **Line 3** | speed[:spread] |
| **Line 4** | a vertical velocity |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx245` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX246 — Fireball

Launches a ghast fireball, aimed by the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MCX246]`, or `=FIREBALL` |
| **Line 3** | speed[:spread] |
| **Line 4** | rotation[:pitch], from -1 straight down to 1 straight up |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx246` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX250 — Particle

Shows a particle, optionally offset from the sign.

| | |
| --- | --- |
| **Write on the sign** | `[MCX250]`, or `=PARTICLE` |
| **Line 3** | the particle, with a block after a colon where it takes one *(required)* |
| **Line 4** | an axis letter and a distance, such as Y3 |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.safe.mcx250` |

### MCX251 — Sound Effect

Plays one sound, named in full or by its shorthand.

| | |
| --- | --- |
| **Write on the sign** | `[MCX251]`, or `=SOUND EFFECT` |
| **Line 3** | the sound, as entity.creeper.primed or the shorthand ENCRPR *(required)* |
| **Line 4** | an x:y:z offset from the sign |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx251` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX255 — Lightning

Strikes one place with lightning.

| | |
| --- | --- |
| **Write on the sign** | `[MCX255]`, or `=LIGHTNING` |
| **Line 3** | how far above or below the sign's support to strike |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx255` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX256 — Holy Smite

Strikes everything within range with lightning.

| | |
| --- | --- |
| **Write on the sign** | `[MCX256]`, or `=HOLY SMITE` |
| **Line 3** | — |
| **Line 4** | how far to reach, defaulting to five blocks |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ256]` |
| **Permission** | `craftbook.ic.restricted.mcx256` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX295 — Trigger Reader

Mirrors the redstone at somewhere else in the world.

| | |
| --- | --- |
| **Write on the sign** | `[MCX295]`, or `=TRIGGER READER` |
| **Line 3** | x:y:z as a step from the sign, optionally prefixed with ! *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Runs on its own as** | `[MCZ295]` |
| **Permission** | `craftbook.ic.safe.mcx295` |

### MCX512 — Message Nearby

Says something to everybody standing within range.

| | |
| --- | --- |
| **Write on the sign** | `[MCX512]`, or `=MESSAGENEARBY` |
| **Line 3** | what to say *(required)* |
| **Line 4** | the rest of what to say |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx512` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX513 — Message Named Nearby

Says something to everybody within range, naming the nearest.

| | |
| --- | --- |
| **Write on the sign** | `[MCX513]`, or `=NAMED NEARBY` |
| **Line 3** | who to tell; %p means the nearest player *(required)* |
| **Line 4** | what to say *(required)* |
| **Wiring** | `AISO` — 4 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx513` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX515 — Server Log

Writes a line to the server's log.

| | |
| --- | --- |
| **Write on the sign** | `[MCX515]`, or `=SERVER LOG` |
| **Line 3** | the line to write to the log *(required)* |
| **Line 4** | — |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx515` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX516 — Server Log Nearby

Writes a line to the log naming the nearest player.

| | |
| --- | --- |
| **Write on the sign** | `[MCX516]`, or `=S-LOG NEARBY` |
| **Line 3** | the line to write; %p becomes the nearest player *(required)* |
| **Line 4** | the rest of the line |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx516` |
| **Restricted** | Yes — not granted to everybody by default. |

### MCX517 — Server Log Nearby+

Writes a line to the log naming everybody in range and how far off.

| | |
| --- | --- |
| **Write on the sign** | `[MCX517]`, or `=S-LOG NEARBY+` |
| **Line 3** | the line to write; %p and %a become the nearest player and how far *(required)* |
| **Line 4** | the rest of the line |
| **Wiring** | `3ISO` — 3 inputs, 1 output |
| **Permission** | `craftbook.ic.restricted.mcx517` |
| **Restricted** | Yes — not granted to everybody by default. |

### VAR100 — Variable Modifier

Does a sum to a variable, such as adding one to it.

| | |
| --- | --- |
| **Write on the sign** | `[VAR100]`, or `=VAR MODIFIER` |
| **Line 3** | the variable to change *(required)* |
| **Line 4** | the sum to do, as function:amount such as +:1 *(required)* |
| **Wiring** | `SISO` — 1 input, 1 output |
| **Permission** | `craftbook.ic.safe.var100` |

### VAR170 — Is At Least

Outputs high while a variable has reached a number.

| | |
| --- | --- |
| **Write on the sign** | `[VAR170]`, or `=IS AT LEAST` |
| **Line 3** | the variable to watch *(required)* |
| **Line 4** | the number it must reach *(required)* |
| **Wiring** | `SISO` — 1 input, 1 output |
| **Permission** | `craftbook.ic.safe.var170` |

### VAR200 — Item Counter

Counts what is in the container above it into a variable.

| | |
| --- | --- |
| **Write on the sign** | `[VAR200]`, or `=ITEM COUNTER` |
| **Line 3** | the variable to add the count to *(required)* |
| **Line 4** | what to count; blank counts everything |
| **Wiring** | `SISO` — 1 input, 1 output |
| **Permission** | `craftbook.ic.safe.var200` |

