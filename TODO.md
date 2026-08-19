# Remaining work

An inventory of what the rewrite has not covered yet. Current status and the standing rules
are in `CLAUDE.md`; bugs found in the legacy code are in `FINDINGS.md`.

## Integrated circuits

**58 model numbers are registered.** 61 of the extra fork's remain, plus 4 dropped by decision.

The legacy class named in each row is the one to read for behaviour, under
`src/main/java/com/minecraftonline/` or `src/main/java/com/sk89q/craftbook/sponge/mechanics/ics/chips/`.
Look every model number and shorthand up in `ICManager` before registering it; do not invent them.

### Entities and projectiles

Needs an entity seam in `ChipWorld` or alongside it: spawning, finding
nearby entities, damaging them, and launching projectiles.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MC1200` | SPAWNER | Entity Spawner | `EntitySpawner` | AISO |
| `MCX200` | ENTITY SPAWNER | Entity Spawner | `?` | 3ISO |
| `MC1201` | DISPENSER | Item Dispenser | `ItemDispenser` | AISO |
| `MCX201` | ITEM SPAWNER | Item Spawner | `ItemSpawner` | AISO |
| `MC1202` | CONTAINER DISPENSER | Container Dispenser | `ContainerDispenser` | 3ISO |
| `MCX202` | CHEST DISPENSER | Chest Dispenser | `ChestDispenser` | AISO |
| `MCX203` | CHEST COLLECTOR | Chest Collector | `ChestCollector` | AISO |
| `MC1240` | ARROW SHOOTER | Arrow Shooter | `ArrowShooter` | AISO |
| `MC1241` | ARROW BARRAGE | Arrow Barrage | `ArrowBarrage` | AISO |
| `MCX242` | SNOW SHOOTER | Snow Shooter | `SnowShooter` | 3ISO |
| `MCX243` | SNOW BARRAGE | Snow Barrage | `SnowBarrage` | 3ISO |
| `MCX244` | EGG SHOOTER | Egg Shooter | `EggShooter` | 3ISO |
| `MCX245` | EGG BARRAGE | Egg Barrage | `EggBarrage` | 3ISO |
| `MCX246` | FIREBALL | Fireball | `FireballShooter` | 3ISO |
| `MC1203` | ZEUS BOLT | Zeus Bolt | `ZeusBolt` | AISO |
| `MCX255` | LIGHTNING | Lightning | `Lightning` | 3ISO |
| `MCX256` | HOLY SMITE | Holy Smite | `HolySmite` | 3ISO |
| `MCX130` | MOB ZAPPER | Mob Zapper | `MobZapper` | SISO |
| `MCX131` | HIT PLAYER ABV | Hit Player Above | `HitPlayerAbove` | UISO |
| `MCX132` | HIT MOB ABOVE | Hit Mob Above | `HitMobAbove` | UISO |
| `MC1250` | FIREWORKS | Fireworks | `Fireworks` | AISO |
| `MC1253` | FIREWORK | Programmable Firework Display | `ProgrammableFireworksDisplay` | AISO |
| `MCX146` | POTION AREA | Potion Area | `PotionArea` | AISO |
| `MCX250` | PARTICLE | Particle | `ParticleEmitter` | 3ISO |

### Sensing players, mobs and items

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCM116` | MOB ABOVE? | Mob Above | `MobAbove` | 3ISO |
| `MCX116` | PLAYER ABOVE? | Player Above | `PlayerAbove` | 3ISO |
| `MCX117` | PLAYER BELOW? | Player Below | `PlayerBelow` | 3ISO |
| `MCX118` | PLAYER NEAR? | Player Near | `PlayerNear` | 3ISO |
| `MCX119` | MOB NEAR? | Mob Near | `MobNear` | 3ISO |
| `MCX133` | HELD ITEM NEAR? | Held Item Near | `HeldItemNear` | MCX133 |
| `MCX138` | ITEM NEAR? | Item Near | `ItemNear` | 3ISO |
| `MCX139` | HELD ITEM NEAR? | Held Item Near | `HeldItemNear` | MCX133 |
| `MCX140` | IN AREA | In Area | `InArea` | UISO |
| `MC1500` | PLAYER ONLINE? | Player Online? | `PlayerOnline` | 3ISO |

### Messaging and logging

Needs an audience seam in core so messages can be composed as Adventure
components and asserted in tests without a server.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MC1510` | MESSAGE PLAYER | Player Messenger | `SendMessage` | 3ISO |
| `MC1511` | MESSAGE ALL | Message All | `MessageAll` | 3ISO |
| `MCX512` | MESSAGENEARBY | Message Nearby | `MessageNearby` | 3ISO |
| `MCX513` | NAMED NEARBY | Message Named Nearby | `NamedNearby` | AISO |
| `MCX515` | SERVER LOG | Server Log | `ServerLog` | 3ISO |
| `MCX516` | S-LOG NEARBY | Server Log Nearby | `ServerLogNearby` | 3ISO |
| `MCX517` | S-LOG NEARBY+ | Server Log Nearby+ | `ServerLogNearbyPlus` | 3ISO |
| `MC2999` | MARQUEE | Marquee | `Marquee` | SI3O |
| `MC3456` | MARQUEETRANSMIT | Marquee Transmitter | `MarqueeTransmitter` | 3ISO |

### Blocks and farming

`MCX211` needs a stockpile, which now exists. The farming chips need crop
state, which the block seam does not yet expose.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCX211` | TOGGLE BLOCK | ToggleBlock | `ToggleBlock` | AISO |
| `MCX213` | HARVESTER | Harvester | `Harvester` | AISO |
| `MCX215` | AREA PLANTER | Area Planter | `AreaPlanter` | AISO |
| `MCX216` | PLANTER | Planter | `Planter` | AISO |
| `MC1249` | BLOCK REPLACER | Block Replacer | `BlockReplacer` | 3ISO |

### Weather illusions

These show one player different weather from another, which the legacy code did
by sending packets directly. Paper has `Player#setPlayerWeather`, so no packet work is needed.

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCX235` | FALSE WEATHER | False Weather | `FalseWeather` | 3ISO |
| `MCX236` | DIST FALSE RAIN | Distance False Weather | `DistanceFalseWeather` | 3ISO |
| `MCX237` | HIDE WEATHER | Hide Weather | `HideWeather` | 3ISO |
| `MCX238` | DIST HIDE RAIN | Distance Hide Weather | `DistanceHideWeather` | 3ISO |

### Sound and music

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MCU700` | MELODY | Melody | `Melody` | UISO |
| `MCU705` | TUNE | Tune | `Tune` | AISO |
| `MCU706` | JUKEBOX | Jukebox | `Jukebox` | AISO |
| `MCX251` | SOUND EFFECT | Sound Effect | `SoundEffect` | 3ISO |

### Logic and control

| Model | Shorthand | Name | Legacy class | Layout |
| --- | --- | --- | --- | --- |
| `MC2022` | BITSHIFT | Bit Shift | `BitShift` | 3ISO |
| `MCU440` | ^MONOFLOP | Monoflop | `Monoflop` | AISO |
| `MCX120` | COMMAND CTRL | Command Controlled IC | `CommandControlled` | 3ISO |
| `MCX121` | PASSWORD CTRL | Command Controlled IC | `PasswordControlled` | 3ISO |
| `MCX295` | TRIGGER READER | Trigger Reader | `TriggerReader` | 3ISO |

### Dropped by decision

Not to be implemented.

| Model | Name |
| --- | --- |
| `MC2300` | ROM Get |
| `MC3300` | ROM Set |
| `MC5000` | Perlstone 3ISO Programmable Logic Chip |
| `MCX144` | CBWarp Area |

### Upstream-only ICs

Around 70 further model numbers exist in `E:\Code\CraftBook` that never existed in this fork,
covering the automatic crafter, container sorters and stockers, ranged collectors, the sentry
gun and others. They are optional additions rather than a compatibility obligation, so they
come after everything above.
## Mechanics

None are started. All ~74 live under `src/main/java/`, each marked with `@Module`. Those below are
unique to this fork and have no upstream equivalent to compare against, so the legacy source is the
only specification.

### Unique to this fork

| Mechanic | What it does |
| --- | --- |
| `MegaPipes` | Item routing with filters, sources and destinations. Larger than the plain `Pipes`. |
| `Footprints` | Cosmetic particles where players walk. |
| `BannerCopier`, `BookCopier`, `MapCopier` | Duplicate a held item onto blanks. Kept despite vanilla equivalents. |
| `PageReader` | Reads a book's pages aloud. |
| `LightNetherrack` | Netherrack lit by redstone. |
| `CartCraft`, `CartDelay`, `CartDirection`, `CartLaunch`, `CartLoad`, `CartPrint`, `CartWarp`, `CartCollect`, `StationClear` | Minecart mechanics with no upstream counterpart. |

### Shared with upstream

Compare both sources for these. Where they differ, this fork's behaviour is the one to keep.

`Ammeter`, `BetterPhysics`, `BetterPlants`, `Bookshelf`, `BounceBlocks`, `Chairs`, `CommandSigns`,
`CookingPot`, `Elevator`, `GlowStone`, `HeadDrops`, `HiddenSwitch`, `JackOLantern`, `LightStone`,
`LightSwitch`, `Marquee`, `Netherrack`, `PaintingSwitcher`, `Pipes`, `RedstoneJukebox`,
`SignCopier`, `Snow`, `Teleporter`, `TreeLopper`, `Variables`, `XPStorer`, the `area` mechanics
(`Bridge`, `Door`, `Gate`, `ComplexArea`), the `boat` and `minecart` sets, `DispenserRecipes`.

### Dropped by decision

`CBWarps`, `ChunkAnchor`, the Pastebin report upload, and the vendored bStats metrics.

## Infrastructure not yet built

| Piece | Why it is needed |
| --- | --- |
| Entity seam | Spawning, damaging entities and launching projectiles. Blocks ~24 ICs. Finding and moving people exists already, as `Traveller` and `ChipWorld.travellersIn`. |
| Audience seam | Sending Adventure components to players near a chip. Blocks ~9 ICs, and the warning a destination should give when its name is already in use. |
| Persistence | Chips that keep state across a restart currently keep it only on the sign. |
| Configuration | Nothing is configurable. The legacy code had per-mechanic config with an enable flag. |
| Commands | No commands at all. The legacy code had `/cb`, plus per-mechanic commands. |
| Permissions | `ICDefinition.permission()` exists and is checked on creation. Nothing else checks anything. |
| Documentation generator | The legacy code generated its own IC documentation. |

## Verification

Nothing has ever been run on a server. The unit tests cover the platform-independent half only;
the listeners, the schedulers, the container lookups and the legacy block mapping have no coverage
at all. Worth checking early, in roughly this order:

1. The plugin loads and reports its catalogue.
2. Writing `[MC1000]` on a wall sign creates a chip that follows redstone.
3. A pre-flattening block name such as `35:14` resolves to red wool.
4. A bridge builds and retracts, paying into and out of a nearby chest.
5. A transmitter drives a receiver in another world, and a transporter delivers to a destination.
6. Region behaviour on an actual Folia server.
