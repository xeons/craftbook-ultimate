# The test bed

`/craftbook testbed build` lays out a flat plane in front of you carrying a working rig for every
chip in the catalogue — sign, levers, lamps and a label, one per chip, on a grid.

It is for trying the catalogue: checking a chip does what it should after a change, seeing what a
layout looks like before building one for real, or working out which chip you want.

---

## Building one

```
/craftbook testbed          what it would build, without building it
/craftbook testbed build    build it, ten rigs to a row
/craftbook testbed build 6  build it six rigs to a row
```

It appears a few blocks in front of you, running away from you and to your right. **It overwrites
everything standing there** — a flat, empty area is the place for it.

Ask without `build` first. That tells you how many rigs there are and how many blocks the plane
covers.

---

## Reading a rig

```
        [lamp]  <- lights when the chip drives that output
           |
        (lever) <- the chip flips this one
           |
     [====][SIGN]        <- the chip
           |
        (lever) (lever)  <- you flip these
           |
     [MC1000]            <- label, standing in front
```

**Levers on both sides, and that is not decoration.** A chip decides an input is wired by looking
at what kind of block sits on the pin — only a power source counts at all — and drives an output by
toggling a lever that is already there, leaving anything else alone. A rig built from redstone
blocks would read as permanently on; one built from lamps would never be driven at all.

So: flip the levers around the sign, and watch the lamps behind it.

Each output lever clings to its lamp, so the lamp is strongly powered rather than merely lit by
being nearby. Where a pin sits directly above another — the four inputs of a `UISO` chip do — the
lever takes a wall or a ceiling instead of a floor. Nothing about that changes how it works.

### The label

The sign standing in front of each rig says:

```
Line 1   MC1000            the model number
Line 2   Repeater          what it is
Line 3   AISO              its wiring, and whether it is restricted
Line 4   (usually blank)   what still needs doing, if anything
```

A label never carries brackets or a leading equals sign. Both are how a chip is named, and a label
carrying one on its second line would quietly become a second chip of whatever it named.

---

## What is set up for you, and what is not

Most chips need nothing said to them: a logic gate wired to levers works the moment you flip one.
Those get a blank third and fourth line, which is correct.

Some read their third and fourth lines and are inert without them. The test bed fills those in
where it knows the answer — a variable for the `VAR` chips and a chest of coal above the counter,
a channel name for the wireless pair, a block and a size for the bridge with a chest of stone to
build from, a sound for the sound effect, a record for the jukebox.

The rest are left blank. **A blank sign is not a broken chip** — check the catalogue page,
`docs/ics.md`, for what that chip wants on its lines, and write it yourself.

A handful cannot be set up at all, because they read a file an operator has to supply: `MCU700`
wants a MIDI file and `MC1253` wants a firework show. Their labels say `needs a file`, and the
command lists them when it finishes.

### Which rigs tick

Sensors, the wireless receiver and the other chips that only *read* something are built ticking, so
they follow the world on their own. Everything else carries the plain model number and acts when
you flip its lever.

Where the catalogue has a **second model number** for the ticking form, that is what the rig
writes: the receiver's sign reads `[MC0111]`, not `[MC1111]`. Twenty-six chips were catalogued
twice that way — one number that waits to be clocked and one that follows on its own — and the rig
uses whichever it means. Those numbers are in `docs/ics.md` under **runs on its own**.

That split is a safety rule, not a style one. Several chips act on **every tick, unconditionally**:
a holy smite built ticking strikes every living thing within range every tick, for as long as its
chunk is loaded. That is a lightning bolt per entity per tick, and a server out of memory in about
a minute. Add `S` by hand where you want it, and think about what the chip does before you do.

> The variable the `VAR` chips name is created for you when the bed is built. Signs placed this way
> are not written by a player, so nothing reviews them — a chip naming a variable that does not
> exist would load and then quietly do nothing, which is exactly the failure that review normally
> catches.

---

## When a rig does nothing

**The lamp never lights.**
Check the label's third line for the wiring code and look up that layout in `docs/ics.md` — you may
be flipping a lever the chip does not read. Some chips have four inputs where only one matters.

**The chip has a blank third line.**
It may want something there. `docs/ics.md` says what.

**The wireless pair looks dead.**
The transmitter (`MC1110`) has no output of its own — it is `AIZO`, three inputs and nothing to
show — so the only way to see it working is the receiver standing next to it, whose sign reads
`[MC0111]`. Flip the transmitter's lever and the receiver's lamp should follow within a tick. If it
does not, check both signs carry the same channel on line three.

There is no ticking transmitter and none is needed: a transmitter drives its band the moment its
input changes, so it has nothing to poll for.

**Some other chip only responds while its lever is held.**
That is how it works when it is not ticking: it reads the world on the edge of its input rather
than continuously. Add an `S` to the end of its second line — `[MC1230]S` — to have it follow the
world by itself, as you would on a sign you wrote.

**A restricted chip is missing.**
It is not. Every chip in the catalogue gets a rig whatever its permission, because the bed is built
by the plugin rather than by a player writing signs.

---

## For operators

`craftbook.testbed` is the permission, and it defaults to operators only. The command writes
thousands of blocks over whatever is there.

The plane is built from `ICCatalogue` every time rather than pasted from a saved file. That is
deliberate, and the same reasoning as the generated catalogue page: a chip added later would be
missing from a saved plane and nobody would notice, and a rig wired from a remembered pin layout
rather than the real one reads as a broken chip when it is the bed that is wrong. Rebuild it after
any change and it is right again.

On a regionised server the work is handed to whichever region owns each rig, so a plane far wider
than any one region is still built safely. No single rig ever spans two.
