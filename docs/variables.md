# Variables

A variable is a number the whole server shares. A command sets it, a chip reads it, and another
chip on the far side of the world adds to it — none of them anywhere near each other, and all of
it still there after a restart.

That is what makes a scoreboard, a shop till, a quest counter or a stock level possible without
any of the redstone involved being in the same chunk.

Five chips work with them, and one sign that is not a chip at all:

| Sign | What it does |
| --- | --- |
| `[VAR100]` | Does a sum to a variable — add one, double it, take ten away. |
| `[VAR170]` | Drives its output high once a variable has reached a number. |
| `[VAR200]` | Counts what is in the chest above it and adds the total to a variable. |
| `[MCN100]` | Shows a variable across four pins as a number from 0 to 15. |
| `[MCN101]` | Writes the redstone level arriving at it into a variable. |
| `[Marquee]` | Tells whoever right-clicks it what a variable says. |

---

## Making your first variable

A variable has to exist before a sign can name one. Make one with a command:

```
/var define score 0
```

Then write a sign:

```
Line 1  (filled in for you)
Line 2  [VAR100]
Line 3  score
Line 4  +:1
```

Power it and `score` goes up by one. Power it again and it goes up again.

Read it back at any time:

```
/var get score
```

### Why the command comes first

A sign naming a variable nobody has made is **refused as you write it**, and tells you so.

That is deliberate. A variable lives in the plugin's own store rather than in the blocks beside
the sign, so a sign naming one that does not exist would look exactly like a sign that does — it
would simply never do anything, and you would have no way to tell that from a wiring fault. Being
told at the moment you write it means you are standing at the sign with the means to fix it.

---

## Namespaces

Two builders both wanting a variable called `score` do not have to argue about it. A name may
carry a namespace in front of it, separated by a bar:

```
score          the shared variable, which anybody may use
alice|score    Alice's, which is a different variable entirely
```

A bare name always means the shared one. `global` is the shared namespace's real name, so
`global|score` and `score` are the same variable written two ways.

You may use the shared variables and your own freely. Somebody else's needs
`craftbook.variables.use.other`, and that is checked both when you write a sign and when you type
a command.

---

## `[VAR100]` — doing a sum

```
Line 3  the variable
Line 4  the sum, as function:amount
```

The sum runs each time the input goes high. The functions:

| Write | Or | What it does |
| --- | --- | --- |
| `+` | `add` | Adds the amount. |
| `-` | `subtract` | Takes the amount away. |
| `*` | `x` or `multiply` | Multiplies by the amount. |
| `/` | `divide` | Divides by the amount. |
| `%` | `mod` | Keeps the remainder after dividing by the amount. |

```
+:1        count up by one
-:1        count down by one
*:2        double it
%:10       keep only the last digit
+:0.5      amounts need not be whole
```

The output goes high when the sum was done and low when it was not — which covers a variable that
has been deleted since the sign was written, one holding something that is not a number, and a
fourth line that does not read as a function and an amount.

**Dividing by zero, and taking a remainder by zero, both leave the number where it was.** Neither
has an answer, and a counter that stayed put is something a build can carry on from.

---

## `[VAR170]` — waiting for a number

```
Line 3  the variable
Line 4  the number to reach
```

The output is high while the variable is that number **or greater**.

```
Line 2  [VAR170]
Line 3  score
Line 4  100
```

That is high once `score` reaches 100. Wire it to a door and the door opens when somebody gets
there.

Written plainly it reads the variable each time its input goes high, so a clock drives it as often
as you need. Written `[VAR170]S` it follows the variable on its own, every tick, with no input at
all — which is usually what you want for something watching a score.

A variable that has been deleted, or that holds something that is not a number, reads as **not
having reached** the number, so the output goes low.

---

## `[VAR200]` — counting a chest

```
Line 3  the variable
Line 4  what to count, or blank for everything
```

The chest is the block **above the block the sign hangs on** — not above the sign itself.

```
        [chest]
        [block] [sign]
```

Each time the input goes high, the chest is counted and the total is **added** to the variable.
The output goes high if anything was found.

```
Line 3  coal_stock
Line 4  coal
```

Old-style item names work here as they do everywhere else: `35:14` is red wool, and signs written
before the flattening go on working.

---

## `[Marquee]` — showing a variable to somebody

```
Line 3  the variable
Line 4  the namespace, or blank for the shared one
```

No redstone, no inputs and no outputs. Right-click the sign and it tells you what the variable
says — that is the whole of it.

```
Line 2  [Marquee]
Line 3  coal_stock
```

It is the readout to go with the other three. `[VAR200]` counts a chest into `coal_stock` and a
`[Marquee]` beside the shop door tells a customer what is in it. `/var get` answers the same
question, but only for somebody who already knows the variable is there and how it is spelt.

Reading somebody else's works the same way it does everywhere: `alice|score` on line 3, or `score`
on line 3 with `alice` on line 4. You need `craftbook.variables.use.other` to build one that reads a
namespace that is not yours.

The variable must exist when the sign is written, and a sign naming one nobody has made is refused
on the spot. If the variable is deleted **afterwards** the sign stays where it is and says so when
somebody clicks it — a sign cannot be un-built retrospectively.

**A note on the name.** `MC2999` and `MC3456` are also called marquees, and they are nothing to do
with this: those are chasing lights, one lamp or one wireless band stepping along a row. The word
means both a row of chasing bulbs and a board with writing on it, and CraftBook has one of each.

Leave line 4 blank and it counts everything in the chest, whatever it is.

> It adds rather than sets. Counting the same chest twice gives you twice the total. To take a
> fresh reading, set the variable back to zero first — a `[VAR100]` reading `*:0` on the same pulse
> does it.

---

## `[MCN100]` and `[MCN101]` — a variable as a redstone level

These two are each other's opposite, and between them a redstone level and a variable become the
same thing. A lever on the far side of the map, a comparator on a chest, a daylight detector — any
of them can set a number the whole server reads, and any of them can be driven by one.

```
Line 2  [MCN101]        Line 2  [MCN100]
Line 3  lift_floor      Line 3  lift_floor
```

`[MCN101]` reads the **strength** of the signal reaching its input, not merely whether there is
one, and writes that number — 0 to 15 — into the variable. `[MCN100]` does the reverse: it drives
its pins with the variable's value as a binary number.

### Reading the pins

`[MCN100]` has five outputs and they are not interchangeable:

| Pin | Carries |
| --- | --- |
| Output 1 | the ones |
| Output 2 | the twos |
| Output 3 | the fours |
| Output 4 | the eights |
| Output 5 | whether the variable could be read at all |

So a variable holding 11 lights outputs 1, 2 and 4 — eight plus two plus one. Least significant
first, which is the order the adders and subtractors in this catalogue already work in, so the
four pins feed straight into an `[MC4000]` without rearranging anything.

**Output 5 is not decoration.** A variable holding zero and a variable that has been deleted look
identical on the first four pins, and so does one holding a word rather than a number. Without
that pin there is no way to tell an empty counter from a broken sign. Wire it to a lamp if you
build nothing else.

### Why four pins and not one

Nothing in the game will hold a redstone level a plugin puts on it. Every block with a `power`
property has it worked out again by the game — wire from its neighbours, a daylight detector and a
sculk sensor from their own ticking, a weighted plate by decay — so a level written to any of them
is gone within a tick. A lever holds what it is set to, so a number is carried by several of them.

A value too big for four pins comes out as **15** rather than wrapping round to a small number, and
anything below zero comes out as **0**. A readout stuck at fifteen is telling you it has run out of
room; one that had wrapped to zero would read as a variable nobody had set. Fractions are rounded
to the nearest whole number, since a pin cannot be half on.

### Both follow the variable on their own

Written plainly, each does its work when its input is pulsed. Written `[MCN100]S` the readout
follows the variable every tick with no input at all, which is what you want for a display that
somebody else's chip is updating.

`[MCN101]` writes nothing when the level has not changed, so one left ticking against a steady
lever does not rewrite the same number every tick.

> Both refuse a sign naming a variable that does not exist, as the other three do. If the variable
> is deleted afterwards, `[MCN100]` reads zero with output 5 low, and `[MCN101]` drives its output
> low rather than silently doing nothing.

---

## The commands

| Command | What it does |
| --- | --- |
| `/var define <name> [value]` | Makes a variable. Starts at `0` if you do not say. |
| `/var set <name> <value>` | Changes one that already exists. |
| `/var get <name>` | Says what one holds. |
| `/var list [namespace]` | Lists them. |
| `/var delete <name>` | Removes one. |
| `/var add <name> <amount>` | Adds to one. |
| `/var subtract <name> <amount>` | Takes from one. |
| `/var multiply <name> <amount>` | Multiplies one. |
| `/var divide <name> <amount>` | Divides one. |

`/variable` works as well as `/var`.

**Making and changing are separate on purpose.** `define` will not touch a variable that already
exists, and `set` will not create one. A command meant to change a running score cannot quietly
make a second one under a misspelling and leave the original sitting where it was.

Values are letters, digits and `. , : ; _ + -`, with no spaces. A variable may hold something that
is not a number — the chips just will not do arithmetic with it.

---

## When it does not work

**The sign was refused when I wrote it.**
Read what it said. Either the variable does not exist yet — make it with `/var define` — or line 4
is not what that chip expects, or the namespace is somebody else's.

**The sum never runs.**
The output going low means the chip tried and could not. Check with `/var get` that the variable is
still there and still holds a number; deleting a variable does not remove the signs that named it.

**`[VAR200]` counts nothing.**
Check the chest is above the block the sign is *on*, not above the sign.

**The number is stuck.**
`/var get` it. If it holds something that is not a number, no function will move it — set it back
to a number with `/var set`.

**Two chips fight over the same variable.**
Nothing serialises them. If two `[VAR100]` chips add to one variable on the same tick, both read
before either writes and one of the two increments is lost. Drive them off the same clock in
sequence rather than in parallel.

---

## For operators

Variables are kept in `variables.txt` in the plugin's folder, one to a line: namespace, name, then
value. It is a plain file you can read and edit while the server is down. A line you break is
skipped rather than costing you the rest of the file.

There is no setting to turn variables off. Like the wireless bands and the commanded switches, they
are a registry rather than a mechanic — nothing happens until somebody builds a chip or types a
command.

Permissions:

| Node | Default | What it allows |
| --- | --- | --- |
| `craftbook.variables.get` | everybody | Reading a variable. |
| `craftbook.variables.list` | everybody | Listing them. |
| `craftbook.variables.define` | operators | Making one. |
| `craftbook.variables.set` | operators | Changing one, including the arithmetic commands. |
| `craftbook.variables.delete` | operators | Removing one. |
| `craftbook.variables.use.other` | operators | Using a namespace that is not your own. |
| `craftbook.ic.safe.var100` | everybody | Building the modifier chip. |
| `craftbook.ic.safe.var170` | everybody | Building the comparison chip. |
| `craftbook.ic.safe.var200` | everybody | Building the item counter. |

Making and changing default to operators because a variable is shared: a chip somebody else built
may be reading it, and there is nothing in the world to show who depends on one. Building a chip
that *uses* a variable is ordinary, since the variable had to be made by somebody who was trusted
to make it.
