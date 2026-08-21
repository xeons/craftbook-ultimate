# Debugging a chip

A chip that does nothing gives you almost nothing to go on. The sign looks right, the levers look
wired, and which of those two impressions is wrong is exactly the question you cannot answer by
looking. These tools answer it by saying what the **plugin** thinks — which pins it believes are
wired, what it reads on each, which chip your sign actually resolved to, and whether anything is
ever going to set it off.

There are two ways in and they do the same things through the same code.

| | |
| --- | --- |
| **The stick** | Point it at a chip's sign and right-click. Best at a workbench. |
| **The commands** | `/craftbook debug …` acts on the chip you are looking at. Best bound to a key, and the only one that works from a command block. |

> Everything here needs `craftbook.debug`, and each mode needs its own permission underneath.
> All of them are operator-only by default.

---

## Getting a stick

```
/craftbook debug stick
```

You get a stick. **Right-click a chip's sign** to use it, and **crouch and right-click the air** to
change what it does. The stick never places a block, never opens what you point it at, and never
puts a sign into edit mode.

The mode is written into the item, not against you, so a stick can be handed to somebody else, left
in a chest, or carried in each hand set differently. Renaming one in an anvil does not break it, and
naming an ordinary stick does not make one.

There is no crafting recipe, deliberately. The fork had three, because its command was admin-only
and builders needed to make their own; here the permission is checked when the stick is *used*, so a
recipe would mostly produce sticks that do nothing.

---

## What it tells you

The default mode is **Menu**, and it is the one to reach for first. It prints the whole report and
then offers the other modes as buttons you can click.

```
MC1200  Sound Effect  =SOUND
  at 118,71,-204 in world
  Line 3 is the sound to play. It is blank, so this chip does nothing.
Pins  AISO  4 in, 1 out
  in  0  118,71,-203  nothing wired here
  in  1  117,71,-204  on, power 15
  in  2  118,70,-204  nothing wired here
  in  3  119,71,-204  nothing wired here
  out 0  118,71,-206  off
Runs  waiting on redstone; add S to the model to make it tick
```

Read it in that order, because that is the order things go wrong in:

**The name.** A sign resolves to whatever model number is on it, which is not always the one you
meant to write. If the first line does not name the chip you thought you built, stop there.

**Anything red.** A line the chip cannot work without, left blank, is called out before anything
else — as is a chip with nothing wired to any input that does not tick, which nothing will ever set
off.

**The pins.** Each says where it is and what the plugin reads there. `nothing wired here` means the
block at that position is not a redstone thing at all — a powered stone block is not wiring, because
the chip reads what is *on* the pin rather than what the world thinks about it. This is the single
most common fault and the report is the fastest way to see it.

**How it runs.** Whether it is ticking, and if not, whether it could.

---

## The modes

| Mode | What it does |
| --- | --- |
| **Menu** | The report above, and the rest as buttons. |
| **Trigger** | Sets the chip off without touching an input, so it reads them exactly as they stand. The way to tell a dead chip from an unwired one. |
| **Area** | Outlines the stretch of world the chip works on, in particles, for eight seconds and only for you. |
| **Fields** | Shows what the chip is holding internally — a counter's total, a display's position in its script. |
| **Reload** | Stops the chip and starts it again, which is exactly what a chunk load does. |
| **Ticking** | Lists every chip on the server currently ticking on its own, with the one you are pointing at marked. |
| **Band** | For a wireless transmitter or receiver: what channel it is on and what that channel is carrying. |

Each is also a command:

```
/craftbook debug              the report, same as Menu
/craftbook debug trigger
/craftbook debug area
/craftbook debug fields
/craftbook debug reload
/craftbook debug ticking
/craftbook debug band
```

### Area

Only chips that can say what area they work on have anything to show here. Those are:

| | |
| --- | --- |
| `MCX116`, `MCX117` | Player Above and Player Below — the column each watches |
| `MCX140` | In Area — the box, offset and all |
| `MCX130` | Mob Zapper — how far it reaches |
| `MCX133` | Humans Only — the same |

The outline is drawn on block boundaries, so it encloses the blocks it describes rather than running
through the middle of the outermost ones. It is edges only: a filled box is unreadable from inside
it, and standing inside a sensor's area is exactly where you are when you ask.

Other chips answer *"this chip does not say what area it works on"*. That is a gap in the chips, not
in the tool — the seam is `AreaAwareICLogic` and any chip may implement it.

### Band

The two ends of a wireless pair cannot see each other, so a transmitter and a receiver that disagree
about their channel look exactly like a pair that agree. Point this at each end in turn and compare
what they say. A band nothing has ever transmitted on is called out separately, because a receiver
on one of those holds whatever it is already showing rather than going low — which reads as a stuck
output.

### Trigger

Worth being clear about what this does not do: it does **not** change any input. It runs the chip as
though something had. So if triggering makes a chip work, the chip is fine and the wiring is not; if
it does not, the chip's own conditions are not being met and the pins are a red herring.

---

## The other thing to try first

```
/craftbook check
```

Lists every loaded chip on the server whose sign leaves out a line it cannot work without, and says
which line each is short of. Those chips also have **their first sign line written in red**, so one
of them is recognisable across a room without any tool at all.

That is the right place to start when the question is "what is broken", and the stick is the right
place to start when the question is "why is *this* broken".

---

## For operators

Nothing here writes to the world except **Reload**, which does exactly what a chunk load does, and
**Trigger**, which runs a chip that was going to run anyway. The report and the outline are reads.

Each mode has its own permission, so a server can hand builders the report and the area outline
without handing them the ability to set every chip on the map off from a distance:

```
craftbook.debug            hold a stick, and use the commands at all
craftbook.debug.menu       the report
craftbook.debug.trigger
craftbook.debug.area
craftbook.debug.fields
craftbook.debug.reload
craftbook.debug.ticking
craftbook.debug.band
```

Cycling skips any mode its holder may not use, so a builder given only `menu` and `area` cycles
between those two and never lands on one that refuses them.

**Fields** reads a chip's private state by reflection. It is a debugging tool and reads nothing but
the chip's own logic object, but it will show you internals that are not part of any contract, and
what it prints will change as chips are rewritten. Do not build anything on top of what it says.

The area outline is capped at four thousand particles and spaces them further apart for a large box
rather than drawing more, so pointing it at something enormous costs a sparse outline rather than a
frozen client.
