# Firework displays

A firework display is a script in a folder and a sign that plays it. The script says what goes up,
where, in what colour and in what order; the sign says when. Nothing about the display is built in
the world, so one script can be played by any number of signs in any number of places.

The chip is **`MC1253`**, shorthand `FIREWORK`.

> Building one needs `craftbook.ic.restricted.mc1253`, which is granted to operators by default.

---

## Your first display

Four shows ship with the plugin, so there is one to play before you have written anything.

1. Find a wall with clear sky above it — about twenty-five blocks up and twelve to either side.
2. Put a **wall sign** on it, at about head height.
3. Write:

```
Line 1
Line 2   [MC1253]
Line 3   finale
Line 4   true
```

4. Put a **lever on the wall directly beside the sign**, either side.
5. Throw it.

Line 1 fills itself in with `FIREWORK` as you place the sign, so leave it blank.

If nothing happens, jump to [when it does not work](#when-it-does-not-work).

### The shows that come with it

| Name | | |
| --- | --- | --- |
| `finale` | 66 rockets, about half a minute | Seven movements, ending on a single gold burst. The big one. |
| `aurora` | 19 rockets, about forty seconds | Cold colours, long gaps, no bang at the end. Scenery rather than an event. |
| `victory` | 7 rockets, about six seconds | For something that happens often — a station, an arena, a puzzle solved. |
| `heartbeat` | 7 rockets, about eleven seconds | Written in the plain spelling, as an example of it. |

They are written into `plugins/CraftBookUltimate/fireworks/` the first time the plugin starts. Edit
them freely: they are only ever written out when that folder is created, so your changes survive a
restart and a show you delete stays deleted. Deleting the whole folder brings all four back.

---

## The sign

```
Line 1   FIREWORK        filled in for you
Line 2   [MC1253]
Line 3   finale          which show — required
Line 4   true            whether dropping the input cuts it short
```

**Line 3** is the name of a file in the fireworks folder, without its extension. Leave it blank and
the sign is refused as you write it, because a display chip with no show named does nothing at all
and there would be no way to tell that from a wiring fault.

**Line 4** is `true` or blank. With `true`, taking the power away stops the display where it is.
Blank, or anything that is not `true`, and the display runs to its end whatever the redstone does
afterwards — which is usually what you want for a button.

Powering a display that is already running does nothing. Holding the lever on will not stack a
dozen copies of the same show on top of each other, and neither will a train of players tripping
the same plate.

### Where the wires go

`MC1253` is wired `AISO`: four inputs, **any one of which sets it off**.

Standing in front of the sign and reading it:

| Input | Where |
| --- | --- |
| 1 | The block **in front** of the sign |
| 2 | The block to your **left** |
| 3 | The block **below** the sign |
| 4 | The block to your **right** |

A lever or a button on the wall immediately left of, right of, or below the sign lands in one of
those. That is the easy way to wire it.

The pin block has to *be* a redstone thing — dust, a lever, a button, a pressure plate, a repeater,
a comparator, an observer, a redstone block or torch, a target, a sculk sensor, a tripwire hook, a
daylight detector or a lightning rod. A powered *stone* block beside the sign is not wiring; the
chip reads what is on the pin, not what the world thinks about it.

The chip has an output pin, and this chip never drives it. There is nothing to wire it to.

---

## Writing your own

Put a file in `plugins/CraftBookUltimate/fireworks/` and run `/craftbook reload`. The name of the
file, without its extension, is what goes on line 3, and it may hold letters, digits and
underscores only.

**The extension decides the grammar**, and the two are genuinely different:

| | `.txt` — the plain spelling | `.fwk` — the named spelling |
| --- | --- | --- |
| One firework | One line | A block of lines, then fired by name |
| Colours per firework | One, and one fade | As many as you like |
| Several at once | No | Yes — file them under one name |
| Sounds | No | Yes |
| Good for | Something short | Anything else |

Everything in both is placed **relative to the sign**, so the same script works wherever it is
played. `#` starts a comment, to the end of the line.

A line the plugin cannot make sense of is **skipped, not reported**. A misspelling costs you one
firework quietly, so if a display comes out thinner than you wrote it, suspect a typo before you
suspect the plugin.

### The plain spelling

Two commands, and that is all there is.

```
launch:x,y,z ; duration ; shape ; r,g,b ; r,g,b [; twinkle | trail]
wait:ticks
```

| Field | What it means |
| --- | --- |
| `x,y,z` | where it goes up from, relative to the sign |
| `duration` | how long the fuse burns — see [fuses](#fuses) |
| `shape` | `BALL`, `BALL_LARGE`, `BURST`, `CREEPER` or `STAR` |
| first `r,g,b` | the colour of the sparks, each 0 to 255 |
| second `r,g,b` | what they fade to before dying |
| last | the word `twinkle` **or** the word `trail` — one or the other, not both |
| `ticks` | 20 to the second |

```
# a red ball two blocks up, a pause, then a white star higher
launch:0,2,0;0;BALL;220,20,40;90,0,20;trail
wait:20
launch:0,2,0;2;STAR;255,255,255;180,200,255;twinkle
```

### The named spelling

Here a firework is described once, given a name, and then fired as many times as you like.

**Describing one:**

```
start bigred            # begin describing an effect called bigred
set.shape ball_large    # BALL, BALL_LARGE, BURST, CREEPER or STAR
set.color 255,0,0       # may be repeated for several colours
set.fade 120,0,0        # may be repeated too
set.trail               # leaves a tail
set.flicker             # sparks twinkle — set.twinkle means the same
build                   # file it under that name
```

`set.colour` is accepted as well as `set.color`.

**Firing it:**

```
location 0,2,0          # where the next launches go up from
duration 10 precise     # how long their fuses burn
launch bigred           # send one up
wait 20                 # pause, in ticks
sound entity.firework_rocket.twinkle 0,8,0 1.5 1.0
```

`location` and `duration` stay set until you change them, so a run of launches at one spot needs
one `location` between them.

**`sound`** takes a name and then, optionally, a position, a volume and a pitch:

```
sound <name> [x,y,z [volume [pitch]]]
```

The position has no spaces in it. Pitch runs 0.5 to 2.0. Names are the game's own —
`entity.firework_rocket.launch`, `entity.firework_rocket.blast`,
`entity.firework_rocket.large_blast`, `entity.firework_rocket.twinkle`, `block.note_block.bell`
and so on.

### Several at once

Describing two effects under the **same name** makes one `launch` that sends up both together:

```
start salvo
set.shape ball_large
set.color 230,30,40
set.trail
build

start salvo
set.shape star
set.color 255,255,255
set.trail
build

launch salvo            # both of them, at once
```

That is how the salvos and the closing wall in `finale.fwk` are built — the wall is five effects
under one name, so six lines of show produce thirty rockets.

The name is matched **exactly**, capitals and all. `start Gold` followed by `launch gold` fires
nothing, and says nothing about it.

### Fuses

The fuse is how long a rocket climbs before it goes off, and so how high it gets. It is set by
`duration`, which comes in two forms:

| Written | Fuse | Roughly |
| --- | --- | --- |
| `duration 0` | 10 ticks | just overhead |
| `duration 1` | 30 ticks | a house's height |
| `duration 2` | 50 ticks | high |
| `duration 8 precise` | 16 ticks | low |
| `duration 14 precise` | 28 ticks | mid |
| `duration 22 precise` | 44 ticks | very high |

Without `precise` you are choosing from a handful of heights, the same ones a firework star gives
you. With `precise` you get every step in between, which is what you want when a burst has to land
on a beat. Whole numbers only either way — `duration 1.5` is read as `duration 1`.

Remember that a burst happens *after* its fuse. To make a sound land with the bang rather than with
the launch, wait the length of the fuse first:

```
duration 10 precise     # 20 ticks
launch creeper
wait 20                 # let it get up there
sound entity.creeper.primed 0,10,0 1.4 0.8
```

---

## When it does not work

**The sign's first line is red.** Line 3 is blank, or names nothing. Fill it in.

**`/craftbook check`** lists every loaded chip in that state across the server, and says which line
each is short of.

**The sign was refused as you wrote it.** Same cause, caught earlier — you left line 3 blank.

**Nothing happens at all, and the sign looks fine.**

- Check the console at start-up for `Read 4 firework displays`. If the count is lower than the
  number of files you have, one of them parsed to nothing.
- Check the file's extension is `.txt` or `.fwk`. Anything else is not read at all.
- Check the file's name is letters, digits and underscores only. `my show.fwk` is ignored.
- Check line 3 matches the file name without its extension, spelled the same way.
- Check the block you are powering is one of the redstone blocks listed above, and is in one of the
  four pin positions.
- Check you ran `/craftbook reload` after adding the file.

**The display is thinner than the script.** A line that will not parse is skipped in silence. In the
named spelling the usual cause is a `launch` naming an effect that no `build` filed, often through a
capital letter.

**Fireworks go off inside the ceiling.** Every position is relative to the sign, and the fuse
decides the height. Lower the `duration`, or put the sign somewhere with more sky.

---

## For operators

Scripts live in `plugins/CraftBookUltimate/fireworks/`. They are read at start-up and again on
`/craftbook reload`. Reload takes every chip down and starts it again, so a display that happens to
be in the air is cut off along with everything else — worth knowing before reloading during an
event, and of no consequence at any other time.

The four bundled shows are written out **only when that folder is created**. An operator's edits
are never overwritten, and a show they delete stays deleted — the cost being that a show added in a
later version does not arrive on a server that already has the folder.

A file over 8192 lines is truncated, and a script over 4096 steps stops there, so one runaway file
cannot hang a region.

Every chip belongs to whichever region owns its sign, and a display hands the rest of itself to
that region's scheduler at every `wait`. A long show therefore costs nothing while it is waiting
and never reaches across a region boundary.

`MC1253` is restricted: `craftbook.ic.restricted.mc1253`, operator by default. It can be switched
off entirely by adding `MC1253` to `ics.disabled` in `config.yml`.
