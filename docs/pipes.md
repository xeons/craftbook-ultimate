# Pipes

A pipe carries items from one container to another along a run of glass. Power the block at its
head and it moves what it finds; the rest is the shape you build.

There are two ways to build one. They are different mechanics with different rules, and both work.
Which one you get is decided by the block at the head of the run, and that decision then governs
how every block along it behaves.

| | Glass pipe | Pane pipe |
| --- | --- | --- |
| Head | Sticky piston | Piston with an `[Extractor]` sign |
| The run is made of | Glass | Glass panes |
| Takes items from | The block the piston faces | The block **behind** the piston |
| Delivers to | A container a plain piston points at | Any container the run touches |
| Branches | Wherever glass meets glass | At every pane |
| Understands colour | Yes | No |

Neither has a tick of its own. A pipe does nothing at all until it is powered.

---

## Your first glass pipe

Five blocks, in a line:

```
  [chest]  [sticky piston]  [glass]  [piston]  [chest]
     ^            ^                      ^        ^
   source    faces the source       faces this   here they land
```

1. Place a **chest** and put something in it.
2. Place a **sticky piston** against it, **facing the chest**.
3. Run **glass** away from the piston — any length, any shape.
4. At the far end place a **plain piston** facing a second **chest**.
5. Give the sticky piston redstone.

Items move from the first chest to the second.

The direction the sticky piston faces matters: that is the container it empties. Everything else —
which way the glass runs, where it bends — is up to you.

### Sending things two ways

Glass carries an item to every side except the one it came in by, so the run splits wherever glass
touches glass. Put a piston at the end of each branch and the pipe fills both.

```
                       [glass] [piston] [chest]     <- second way out
                          |
 [chest] [sticky] [glass][glass][glass] [piston] [chest]
```

A stack goes to the **nearest** way out that will take it. If that one is full, what it would not
take carries on to the next. If nothing anywhere will take it, the stack stays in the chest it
started in — a pipe never spills items on the floor.

### Crossing without mixing

A **glass pane** in a glass pipe is a crossing, not a junction. Items go straight over it and never
turn. That is how two pipes share the same block:

```
              [glass]
                 |
 [glass]------[pane]------[glass]        one pipe runs across, the other runs down
                 |
              [glass]
```

Each pipe passes straight through and neither picks up the other's items.

### Colours

Use **stained glass** and a pipe keeps to its own colour. The rules are worth learning because they
are what makes bundling work:

- Coloured → the **same** colour: passes.
- Coloured → a **different** colour: stops.
- Coloured → **plain** glass: passes.
- Plain → **any** colour: passes.

So plain glass is a joiner and coloured glass is a divider. A trunk of plain glass with red and blue
branches off it will send items down both; a red run that turns blue halfway along goes nowhere.

Stained **panes** follow the same colour rule and still go straight on.

```
 [chest] [sticky] [red glass][red glass][red glass] [piston] [chest]
                        |
                  [blue glass]      <- items will not turn down here
```

---

## Your first pane pipe

A pane pipe is for feeding **many** containers from one place without building a piston for each.

1. Place a **chest** and fill it.
2. Place a **piston** against it, **facing away from the chest** — it points at where the pipe will
   run, and pulls from behind itself.
3. Put a **sign** on that piston reading `[Extractor]` on its second line.
4. Run **glass panes** from the block the piston points at.
5. Put containers anywhere touching the panes.
6. Power the piston.

```
                          [chest]
                             |
 [chest] [piston] [pane][pane][pane][pane]
   ^      ^ [Extractor]         |
 source   faces the panes    [furnace]
```

Every pane spreads in all six directions, so the run is a web rather than a line, and any container
it touches is somewhere items can go. There is no output block to place.

> The `[Extractor]` sign is required. Without it a piston is just a way out of a glass pipe — which
> is exactly what stops a glass pipe's own outputs from being mistaken for the head of a second
> pipe when they are powered.

Colour is ignored in a pane pipe. A pane is the pipe there, not a junction in one, so stained panes
carry items the same as plain ones.

---

## Filters

Any way out can be told what it will and will not take. Put a sign on it reading `[Pipe]`:

```
Line 1  (anything)
Line 2  [Pipe]
Line 3  what it takes      coal, iron_ingot
Line 4  what it refuses    coal
```

- A **blank third line** takes everything.
- A **blank fourth line** refuses nothing.
- Names are separated by **commas**, and spaces around them do not matter.
- **Refusing wins.** An item on both lines does not pass, which is how you carve an exception out of
  a wide list.

Where the sign goes:

- **Glass pipe** — on the plain piston that hands the items over.
- **Pane pipe** — on the container itself.

Either on the side of the block, or standing on top of it.

Old-style item names work. `35:14` is red wool, and signs written before the flattening go on
working exactly as they did.

### Sorting

Give each way out a different filter and the pipe sorts. Because a stack goes to the nearest way out
that will take it, put the fussy ones nearest the head and the catch-all furthest away:

```
 [chest] [sticky] [glass][glass][glass][glass][glass] [piston] [chest]  <- no sign: everything else
                     |               |
                  [piston]        [piston]
                  [Pipe]          [Pipe]
                  coal            iron_ingot
                     |               |
                  [chest]         [chest]
```

A name that means nothing is refused as you write the sign, so a typo tells you at once rather than
leaving a sorter that quietly passes everything.

---

## Furnaces

A furnace takes items into a different slot depending on which side they arrive at, exactly as a
hopper does:

- Arriving **through the top** → the smelting slot.
- Arriving **from any side or below** → the fuel slot.

So one pipe over the top of a row of furnaces and another along their side will keep them fed and
lit, and neither pipe has to say which is which.

Any other container takes items into the first slot with room: chests, barrels, hoppers, droppers,
dispensers, and anything a future version of the game adds that holds items.

---

## How much moves, and when

**A pipe runs when power arrives**, not while it stays on. One button press, one delivery. To move
items steadily, pulse it — a clock, a repeater loop, an observer.

**One stack per pulse** by default. If an operator has turned `stack-per-pull` off, a single pulse
moves as much of the source container as the pipe can find room for.

**Items keep everything about them.** A named, enchanted, part-damaged tool comes out of the far end
exactly as it went in. A pipe moves the stack itself rather than a count of a kind of item.

---

## Limits

A pipe is followed for **150 blocks** by default before the search gives up. A pipe longer than the
limit is not refused — it carries items as far as the limit reaches. If a distant chest has stopped
being filled and nothing else has changed, the run has probably grown past the limit.

Your operator can change it. See **For operators** below.

---

## When it does not work

**Nothing moves at all.**
Check the piston is getting redstone, and that power is *arriving* rather than already on. Check
which way it faces: a sticky piston must face the chest it empties, an extractor must face away from
it.

**A glass pipe stops partway.**
Look for a pane where you meant to branch — panes go straight on. Look for a colour change; coloured
glass will not pass into a different colour.

**A pane pipe does nothing.**
Check the sign says `[Extractor]` on its **second** line. Check the run is panes and not glass — a
pane pipe will not travel through glass.

**Items go to the wrong chest.**
A stack goes to the nearest way out that will take it. Add a filter, or move the way out you wanted
closer to the head.

**A furnace is getting fuel in its smelting slot.**
Items arriving from the side are fuel. Bring that pipe over the top instead.

**It worked, then stopped after I built something.**
The pipe is followed again the next time it is powered, so a change is picked up straight away. If
it stopped, the change broke the run — most often a block placed where the run used to pass, or a
colour that no longer matches.

---

## For operators

Everything lives under `pipes` in `config.yml`:

```yaml
pipes:
  # Whether pipes carry anything. The blocks stay where they are either way.
  enabled: true
  # How many blocks of pipe are followed before the search gives up.
  max-length: 150
  # Whether one pulse moves a single stack.
  stack-per-pull: true
```

`craftbook.pipes` is the permission to write a `[Pipe]` or `[Extractor]` sign. It is granted by
default. Nothing is needed to *use* a pipe somebody else built.

Narrowing `max-length` shortens existing pipes rather than breaking them, in the same way every
other limit in the plugin works. `/craftbook reload` picks up a change without a restart.

### What it costs

A pipe is followed once and the answer is kept until one of its blocks changes, so a pipe that runs
every tick is not re-read every tick. A block changing anywhere on the server costs two lookups and,
where nothing is indexed there, nothing more. What is remembered is dropped as chunks unload, so a
pipe nobody visits costs nothing at all.
