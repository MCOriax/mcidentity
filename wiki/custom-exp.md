# Custom Profession EXP Gain

[← Back to README](../README.md)

Server owners can customize how professions gain experience by defining action-based EXP configurations in YAML files within the `professions/` folder.

## Creating a Custom Profession
1. Create a new YAML file in the `professions/` directory (e.g., `professions/warrior.yml`).
2. Define the display name and custom EXP gain rules.

## Profession Configuration
New professions are created by adding a `.yml` file to the `professions/` folder. The plugin will automatically register these into the database upon the first execution.

**Key Requirements:**
- `system_code`: Must be unique and provided in **lowercase**.
- `display_name`: Human-readable name for the profession.

<details>
<summary>Complete Profession File (<code>professions/tanker.yml</code>)</summary>

```yaml
system_code: tanker
display_name: "Tanker"

level:
  max: 100
  exp_up: 10000

stats_random: "100~120"
stats:
  energy: 100
  health: 250
  strength: 0
  defense: 0
  wisdom: 0
  luck: 0
  agility: 0
  resilience: 0

receive_damage:
  "1-10": 5~10
  "11-50": 20~40
  "51-100": 60~100
  "101-1000": 150~300

skins:
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEzNWExMzUwMjc4MDNiZGMzMmUyNDFjZmNkOTNhZWJjNzNjZTBhZWViZjEzY2ZlODI2ZDk2NGY2ZTcyZTQ3In19fQ=="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjE3OTJlYWQ4MzE0OWQ3MTI5YjBmNmFhZGQ4OWMxMmUwZTc3OWNmZmY0YWFiMTI2YTZhMzhiY2FjZDQ2YTljNiJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODMxMGZlMjRlNDUyZDI3MTI5ZGQzMjFkYTFjZTk2MWNhODkxMDMwMTlmYzQ2N2JlNzZjMGZiZjg1YzEyMTc2OSJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ4NmUxMjNhMGEzODAzYmQ0ZDYyOTQ1MGZmMDY2YjUwOTAxOGZmMWZlN2IzZGI2MjA0N2VjYTcyOTc3MTk1MCJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjI0NzgxNGJmNzg4Mzg1YmUwOGU2YzRmMTg1MjRiMTVmZGYxNTNmM2MzMzg5ZDcwZDA2ZDc3ODU4MWRkZDIwOCJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNjY2E0ZjAyMjdiYTJkMjcyMDcwMTM4MmQ2MzZmOGZkZDkzOTEzMWQ1YjU2YTVlNzNhNmZkYmIyNGZkOThhYSJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjQyMmMwMmZlMThkMjllZDJlMzU1NGZlYmFiN2VmNGUzZjE3Yzg1YTZjYzM3OWFlYzVkODlhZWMyMzNiNzhjYiJ9fX0="
  - "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljNTg4YThiNjJhZmZkNzQ2NDNlMGQ2NjQxYmRhM2EwNzg0YTMwNGM1NzljMDY3ZmE0OTVlMmM0M2VjOTQ2MiJ9fX0="
```

</details>

## Profession Configuration Keys
- **`level:`**: Defines the progression ceiling for the profession.
  - `max`: Highest reachable level.
  - `exp_up`: EXP required to advance one level.
- **`stats:`**: Defines the starting attributes for the profession.
  - `energy`: Maximum energy.
  - `health`: Maximum health. Health regenerates automatically by **1% of maximum health every 5 seconds**, independent of food.
  - `food`: Maximum food capacity. Food governs the hunger bar only; it is **no longer consumed by health regeneration**.
  - `strength`, `defense`, `wisdom`, `luck`, `agility`, `resilience`: Primary attributes.
- **`stats_random:`**: Defines the total amount of bonus points that will be randomly distributed across the 6 primary attributes (strength, defense, wisdom, luck, agility, resilience) when a new identity is created.
- **`skins:`**: Defines available Base64 skin textures for this profession. When a new identity of this profession is created, a skin will be chosen randomly from this list.

## EXP Gain Actions
Every key below is a top-level entry in the profession file. Each dropdown states what triggers the action, which value shapes it accepts, and a ready-to-paste example, so one action can be configured without reading the rest of the page.

A value is either a fixed amount (`10`) or a random range (`10~20`), which rolls a fresh amount between the two bounds on every gain.

All 73 supported action keys are listed below.

### Combat

<details>
<summary><code>deal_damage</code> — Deal melee damage</summary>

Triggered when the player deals melee damage to another entity.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the final damage dealt. An amount outside every range awards nothing.

**Same EXP for any hit**

```yaml
deal_damage: 5
```

**Bigger hits award more**

```yaml
deal_damage:
  "1-10": 2~5      # a 1 to 10 damage hit awards a random 2 to 5 EXP
  "11-50": 10~20
  "51-100": 30~50
```

</details>

<details>
<summary><code>kill_mob</code> — Kill an entity</summary>

Triggered when the player kills an entity.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for any kill**

```yaml
kill_mob: 15
```

**Per entity type**

```yaml
kill_mob:
  zombie: 20~30
  skeleton: 25~35
  creeper: 40~60
  enderman: 100~150
  ender_dragon: 1000~2000
```

</details>

<details>
<summary><code>deal_ranged_damage</code> — Deal projectile damage</summary>

Triggered when the player deals damage with a projectile, for example a bow.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the final damage dealt. An amount outside every range awards nothing.

**Same EXP for any shot**

```yaml
deal_ranged_damage: 8
```

**Rewarding accurate, fully drawn shots**

```yaml
deal_ranged_damage:
  "1-10": 3~6
  "11-50": 12~22
  "51-100": 35~55
```

</details>

<details>
<summary><code>critical_hit</code> — Land a critical melee hit</summary>

Triggered when the player lands a critical melee hit.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the final damage dealt. An amount outside every range awards nothing.

**Same EXP for any critical hit**

```yaml
critical_hit: 15~25
```

**Scaled by the damage of the critical hit**

```yaml
critical_hit:
  "1-10": 5~10
  "11-50": 15~25
  "51-100": 40~60
```

</details>

### Defense

<details>
<summary><code>receive_damage</code> — Take damage</summary>

Triggered when the player takes damage.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the damage taken. An amount outside every range awards nothing.

**Same EXP for any hit taken**

```yaml
receive_damage: 10
```

**Surviving heavier hits pays more**

```yaml
receive_damage:
  "1-10": 5~10
  "11-50": 20~40
  "51-100": 60~100
  "101-1000": 150~300
```

</details>

<details>
<summary><code>shield_block</code> — Block damage with a shield</summary>

Triggered when the player blocks incoming damage with a shield.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the blocked damage. An amount outside every range awards nothing.

**Same EXP for any block**

```yaml
shield_block: 10~18
```

**Scaled by the damage absorbed**

```yaml
shield_block:
  "1-10": 5
  "11-50": 15~30
  "51-100": 40~70
```

</details>

<details>
<summary><code>dodge_action</code> — Evade incoming damage</summary>

Triggered when the player evades incoming damage. The dodge chance scales with the identity's `agility` stat, and a successful dodge fully negates the damage.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Fixed reward per dodge**

```yaml
dodge_action: 20
```

</details>

### Gathering

<details>
<summary><code>break_ore</code> — Break an ore block</summary>

Triggered when the player breaks an ore block.

Takes a single value that rewards every case equally, or a map keyed by the ore material in lowercase. Keys you leave out award nothing.

**Same EXP for every ore**

```yaml
break_ore: 3
```

**Rarer ores pay more**

```yaml
break_ore:
  coal_ore: 2
  iron_ore: 4
  gold_ore: 6
  diamond_ore: 20
  emerald_ore: 25
  ancient_debris: 50
```

</details>

<details>
<summary><code>chop_tree</code> — Break a log block</summary>

Triggered when the player breaks a log block.

Takes a single value that rewards every case equally, or a map keyed by the log material in lowercase. Keys you leave out award nothing.

**Same EXP for every log**

```yaml
chop_tree: 2~4
```

**Per wood type**

```yaml
chop_tree:
  oak_log: 2
  birch_log: 2
  dark_oak_log: 3
  warped_stem: 5
```

</details>

<details>
<summary><code>harvest_crop</code> — Break a fully grown crop</summary>

Triggered when the player breaks a fully grown crop.

Takes a single value that rewards every case equally, or a map keyed by the crop material in lowercase. Keys you leave out award nothing.

**Same EXP for every crop**

```yaml
harvest_crop: 1~3
```

**Per crop**

```yaml
harvest_crop:
  wheat: 2
  carrots: 2
  potatoes: 2
  beetroots: 3
  nether_wart: 4
```

</details>

<details>
<summary><code>fish_caught</code> — Reel in a fish</summary>

Triggered when the player reels in a fish.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per catch**

```yaml
fish_caught: 4~8
```

</details>

### Crafting & Processing

<details>
<summary><code>craft_item</code> — Craft an item</summary>

Triggered when the player crafts an item.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every recipe**

```yaml
craft_item: 2
```

**Valuable recipes pay more**

```yaml
craft_item:
  stick: 1
  iron_pickaxe: 8
  diamond_sword: 30
  enchanting_table: 50
```

</details>

<details>
<summary><code>smelt_furnace</code> — Take a smelted item from a furnace</summary>

Triggered when the player extracts a smelted item from a furnace.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every smelt**

```yaml
smelt_furnace: 3
```

**Per smelted result**

```yaml
smelt_furnace:
  cooked_beef: 3
  iron_ingot: 5
  gold_ingot: 6
  netherite_scrap: 40
```

</details>

<details>
<summary><code>brew_potion</code> — Collect a brewed potion</summary>

Triggered when the player collects a brewed potion from a brewing stand.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Fixed reward per potion**

```yaml
brew_potion: 12
```

</details>

### Interaction & Exploration

<details>
<summary><code>tame_animal</code> — Tame an animal</summary>

Triggered when the player tames an animal.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every tame**

```yaml
tame_animal: 25~40
```

**Per animal**

```yaml
tame_animal:
  wolf: 25
  cat: 30
  llama: 30
  parrot: 35
  horse: 40
```

</details>

<details>
<summary><code>breed_animal</code> — Breed two animals</summary>

Triggered when the player breeds two animals.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every pairing**

```yaml
breed_animal: 8~12
```

**Harder animals pay more**

```yaml
breed_animal:
  cow: 8
  sheep: 8
  turtle: 30
  panda: 40
```

</details>

<details>
<summary><code>explore_biome</code> — Enter a new biome</summary>

Triggered the first time the player enters a given biome during the current server session.

Takes a single value that rewards every case equally, or a map keyed by the biome name in lowercase. Keys you leave out award nothing.

**Same EXP for every new biome**

```yaml
explore_biome: 30
```

**Remote biomes pay more**

```yaml
explore_biome:
  plains: 10
  desert: 15
  jungle: 30
  deep_dark: 80
  end_highlands: 120
```

</details>

<details>
<summary><code>heal_target</code> — Heal another entity</summary>

Triggered when the player heals another entity with a splash potion.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Fixed reward per heal**

```yaml
heal_target: 20
```

</details>

### Magic & Economy

<details>
<summary><code>enchant_item</code> — Enchant an item</summary>

Triggered when the player enchants an item at an enchanting table.

Takes a single value that rewards every case equally, or a map keyed by the item material in lowercase. Keys you leave out award nothing.

**Same EXP for every enchant**

```yaml
enchant_item: 20~35
```

**Per enchanted item**

```yaml
enchant_item:
  book: 15
  diamond_sword: 35
  netherite_chestplate: 60
```

</details>

<details>
<summary><code>villager_trade</code> — Complete a villager trade</summary>

Triggered when the player collects the result of a villager trade.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every trade**

```yaml
villager_trade: 5
```

**Per traded result**

```yaml
villager_trade:
  emerald: 4
  experience_bottle: 12
  enchanted_book: 30
```

</details>

<details>
<summary><code>repair_item</code> — Repair or combine an item on an anvil</summary>

Triggered when the player takes a repaired or combined item out of an anvil. Only durability-bearing items qualify.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every repair**

```yaml
repair_item: 8~15
```

**Per repaired item**

```yaml
repair_item:
  iron_pickaxe: 8
  diamond_chestplate: 20
  elytra: 40
```

</details>

### Building & Survival

<details>
<summary><code>place_block</code> — Place a block</summary>

Triggered when the player places a block. A location is rewarded only once, preventing the place / break / replace EXP farm.

Takes a single value that rewards every case equally, or a map keyed by the block material in lowercase. Keys you leave out award nothing.

**Same EXP for every block**

```yaml
place_block: 1~2
```

**Decorative blocks pay more**

```yaml
place_block:
  stone: 1
  oak_planks: 1
  quartz_block: 3
  sea_lantern: 5
```

</details>

<details>
<summary><code>consume_food</code> — Eat a food item</summary>

Triggered when the player eats a food item. Non-food consumables such as potions and milk are ignored.

Takes a single value that rewards every case equally, or a map keyed by the food material in lowercase. Keys you leave out award nothing.

**Same EXP for every meal**

```yaml
consume_food: 2~4
```

**Per dish**

```yaml
consume_food:
  bread: 2
  cooked_beef: 4
  golden_carrot: 10
  enchanted_golden_apple: 50
```

</details>

<details>
<summary><code>shear_entity</code> — Shear an entity</summary>

Triggered when the player shears an entity such as a sheep.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every shear**

```yaml
shear_entity: 4
```

**Per entity**

```yaml
shear_entity:
  sheep: 4
  snow_golem: 6
  mooshroom: 8
```

</details>

### Advanced Exploration & Movement

<details>
<summary><code>open_loot_chest</code> — Open a generated loot container</summary>

Triggered when the player opens a naturally generated loot container. A vanilla loot table only generates once, so the reward cannot be farmed.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per container**

```yaml
open_loot_chest: 50~80
```

</details>

<details>
<summary><code>glide_elytra</code> — Start gliding with an elytra</summary>

Triggered when the player starts gliding with an elytra. Rate-limited per player to prevent rapid toggle farming.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per launch**

```yaml
glide_elytra: 10~15
```

</details>

<details>
<summary><code>survive_fall</code> — Survive fall damage</summary>

Triggered when the player takes fall damage but survives it.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the survived damage amount. An amount outside every range awards nothing.

**Same EXP for any fall**

```yaml
survive_fall: 20
```

**Longer falls pay more**

```yaml
survive_fall:
  "1-5": 5
  "6-15": 20
  "16-100": 40~70
```

</details>

### Team & Support

<details>
<summary><code>apply_buff</code> — Buff another player</summary>

Triggered when the player lands a beneficial (non-healing) potion on another player. Kept distinct from `heal_target`, which covers instant healing.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per buff**

```yaml
apply_buff: 15~25
```

</details>

### Farming & Ranching

<details>
<summary><code>plant_crop</code> — Plant a crop seed</summary>

Triggered when the player plants a crop seed.

Takes a single value that rewards every case equally, or a map keyed by the planted crop material in lowercase. Keys you leave out award nothing.

**Same EXP for every seed**

```yaml
plant_crop: 1~2
```

**Per crop**

```yaml
plant_crop:
  wheat: 1
  carrots: 1
  potatoes: 1
  nether_wart: 3
  torchflower_crop: 8
```

</details>

<details>
<summary><code>strip_log</code> — Strip a log with an axe</summary>

Triggered when the player strips a log with an axe. A location is rewarded only once, preventing the place / strip / break / replace farm.

Takes a single value that rewards every case equally, or a map keyed by the log material in lowercase. Keys you leave out award nothing.

**Same EXP for every log**

```yaml
strip_log: 2~3
```

**Per wood type**

```yaml
strip_log:
  oak_log: 2
  spruce_log: 2
  crimson_stem: 4
```

</details>

<details>
<summary><code>milk_entity</code> — Milk an entity with a bucket</summary>

Triggered when the player milks a cow, mushroom cow or goat with a bucket.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every bucket**

```yaml
milk_entity: 3~5
```

**Per animal**

```yaml
milk_entity:
  cow: 3
  mooshroom: 5
  goat: 8
```

</details>

<details>
<summary><code>catch_entity_bucket</code> — Capture an entity in a bucket</summary>

Triggered when the player captures an entity in a bucket, such as a fish or an axolotl.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every capture**

```yaml
catch_entity_bucket: 5
```

**Rarer catches pay more**

```yaml
catch_entity_bucket:
  cod: 5
  salmon: 5
  pufferfish: 8
  tadpole: 8
  tropical_fish: 12
  axolotl: 25
```

</details>

### Exploration & Travel

<details>
<summary><code>discover_structure</code> — Discover a major structure</summary>

Triggered when the player discovers a major structure, detected through the vanilla advancement granted on entry (stronghold, nether fortress, bastion remnant).

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per discovery**

```yaml
discover_structure: 80~150
```

</details>

<details>
<summary><code>ride_vehicle</code> — Start riding a vehicle</summary>

Triggered when the player starts riding a vehicle such as a boat or a minecart. Rate-limited per player.

Takes a single value that rewards every case equally, or a map keyed by the vehicle entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every vehicle**

```yaml
ride_vehicle: 3~5
```

**Per vehicle**

```yaml
ride_vehicle:
  boat: 3
  minecart: 4
  chest_boat: 6
```

</details>

<details>
<summary><code>leash_entity</code> — Leash an entity</summary>

Triggered when the player leashes an entity. Rate-limited per player.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every leash**

```yaml
leash_entity: 5~8
```

**Per animal**

```yaml
leash_entity:
  cow: 4
  sheep: 4
  llama: 8
  allay: 15
```

</details>

### Interaction & Art

<details>
<summary><code>play_music_disc</code> — Play a music disc</summary>

Triggered when the player inserts a music disc into an empty jukebox. Ejecting a disc is not rewarded.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per disc**

```yaml
play_music_disc: 10~15
```

</details>

<details>
<summary><code>dye_item</code> — Dye a sheep</summary>

Triggered when the player dyes a sheep. Rate-limited per player.

Takes a single value that rewards every case equally, or a map keyed by the dye material in lowercase. Keys you leave out award nothing.

**Same EXP for every dye**

```yaml
dye_item: 2~4
```

**Per dye**

```yaml
dye_item:
  black_dye: 2
  cyan_dye: 4
  pink_dye: 4
```

</details>

<details>
<summary><code>carve_pumpkin</code> — Carve a pumpkin</summary>

Triggered when the player carves a pumpkin with shears. A location is rewarded only once.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per pumpkin**

```yaml
carve_pumpkin: 5~8
```

</details>

<details>
<summary><code>ring_bell</code> — Ring a bell</summary>

Triggered when the player rings a bell. Rate-limited per player.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per ring**

```yaml
ring_bell: 3~5
```

</details>

### Magic, Utility & Combat

<details>
<summary><code>ignite_fire</code> — Ignite a fire</summary>

Triggered when the player ignites a fire with flint and steel. Rate-limited per player.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per fire**

```yaml
ignite_fire: 2~4
```

</details>

<details>
<summary><code>consume_potion</code> — Drink a potion</summary>

Triggered when the player drinks a potion.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per potion**

```yaml
consume_potion: 5~10
```

</details>

<details>
<summary><code>apply_debuff</code> — Debuff another entity</summary>

Triggered when the player lands a harmful potion on another entity.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per debuff**

```yaml
apply_debuff: 10~18
```

</details>

<details>
<summary><code>cure_zombie_villager</code> — Cure a zombie villager</summary>

Triggered when the player feeds a golden apple to a weakened zombie villager, starting the cure.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per cure**

```yaml
cure_zombie_villager: 100~150
```

</details>

### Engineering & Mechanics

<details>
<summary><code>place_redstone</code> — Place a redstone component</summary>

Triggered when the player places a redstone component such as dust, a repeater, a piston, an observer, a button or a pressure plate. A location is rewarded only once, preventing the place / break / replace farm.

Takes a single value that rewards every case equally, or a map keyed by the block material in lowercase. Keys you leave out award nothing.

**Same EXP for every component**

```yaml
place_redstone: 1~3
```

**Per component**

```yaml
place_redstone:
  redstone_wire: 1
  repeater: 3
  observer: 5
  piston: 6
```

</details>

<details>
<summary><code>create_golem</code> — Build a utility golem</summary>

Triggered when a player builds a utility golem. The Bukkit API does not attribute the build to a player, so the reward is credited to the nearest player around the spawn.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every golem**

```yaml
create_golem: 30
```

**Per golem**

```yaml
create_golem:
  snow_golem: 15~25
  iron_golem: 40~60
```

</details>

<details>
<summary><code>activate_conduit</code> — Activate a conduit</summary>

Triggered when the player places (activates) a conduit. A location is rewarded only once.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per conduit**

```yaml
activate_conduit: 30~50
```

</details>

### Dimensions & Aquatic

<details>
<summary><code>swim_distance</code> — Swim 10 blocks</summary>

Triggered for every 10 blocks the player swims (sprint-swimming in water). Distance is accumulated per player so the reward scales with genuine travel rather than firing on every move.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per 10 blocks**

```yaml
swim_distance: 2~4
```

</details>

<details>
<summary><code>enter_portal</code> — Travel through a portal</summary>

Triggered when the player travels through a nether or end portal. Rate-limited per player to stop back-and-forth farming.

Takes a single value that rewards every case equally, or a map keyed by the destination environment in lowercase. Keys you leave out award nothing.

**Same EXP for every portal**

```yaml
enter_portal: 20
```

**Per destination**

```yaml
enter_portal:
  nether: 15~25
  the_end: 40~60
```

</details>

<details>
<summary><code>use_totem</code> — Be saved by a totem of undying</summary>

Triggered when a totem of undying saves the player from death.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per totem**

```yaml
use_totem: 50~100
```

</details>

### Survival & Resilience

<details>
<summary><code>sleep_in_bed</code> — Sleep in a bed</summary>

Triggered when the player successfully lies down to sleep in a bed. Rate-limited per player.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per night**

```yaml
sleep_in_bed: 5~10
```

</details>

<details>
<summary><code>extinguish_fire</code> — Extinguish a fire block</summary>

Triggered when the player extinguishes a fire block. Rate-limited per player to defeat the ignite / extinguish farm.

Takes a single value that rewards every case equally, or a map keyed by the fire material in lowercase. Keys you leave out award nothing.

**Same EXP for every fire**

```yaml
extinguish_fire: 3~6
```

**Per fire type**

```yaml
extinguish_fire:
  fire: 3
  soul_fire: 6
```

</details>

<details>
<summary><code>survive_explosion</code> — Survive an explosion</summary>

Triggered when the player takes block or entity explosion damage but survives it.

Takes a single value, or a map of range keys written `"min-max"` or `"min~max"` and matched against the survived damage amount. An amount outside every range awards nothing.

**Same EXP for any explosion**

```yaml
survive_explosion: 30
```

**Bigger blasts pay more**

```yaml
survive_explosion:
  "1-5": 10
  "6-15": 30
  "16-100": 60~100
```

</details>

### Special Events & Bosses

<details>
<summary><code>trigger_raid</code> — Trigger a village raid</summary>

Triggered when the player triggers a village raid, for example by entering a village while carrying Bad Omen.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per raid**

```yaml
trigger_raid: 20~40
```

</details>

<details>
<summary><code>win_raid</code> — Win a village raid</summary>

Triggered for every player credited with winning a village raid.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per victory**

```yaml
win_raid: 150~300
```

</details>

<details>
<summary><code>summon_boss</code> — Summon a boss</summary>

Triggered when a player summons a boss such as the Wither. The reward is credited to the nearest player around the spawn.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every boss**

```yaml
summon_boss: 200~400
```

**Per boss**

```yaml
summon_boss:
  wither: 200~400
```

</details>

### Specialty Farming & Pets

<details>
<summary><code>feed_pet</code> — Feed one of your own pets</summary>

Triggered when the player feeds one of their own tamed pets an actual food item. Rate-limited per player.

Takes a single value that rewards every case equally, or a map keyed by the entity type in lowercase. Keys you leave out award nothing.

**Same EXP for every pet**

```yaml
feed_pet: 2~4
```

**Per pet**

```yaml
feed_pet:
  wolf: 2
  cat: 3
  parrot: 4
```

</details>

<details>
<summary><code>hatch_egg</code> — Hatch a chick from a thrown egg</summary>

Triggered when a thrown egg hatches into a chick.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per chick**

```yaml
hatch_egg: 5~10
```

</details>

<details>
<summary><code>collect_honey</code> — Bottle honey from a full hive</summary>

Triggered when the player bottles honey from a full beehive or bee nest with a glass bottle. Only a full hive yields honey, which the bees take real time to refill.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per bottle**

```yaml
collect_honey: 5~10
```

</details>

### Ranching & Wildlife

<details>
<summary><code>shear_sheep_color</code> — Shear a sheep of a given color</summary>

Triggered when the player shears a sheep. Fires alongside `shear_entity`, so configs can reward rare colors specifically. Re-shearing is gated by vanilla wool regrowth.

Takes a single value that rewards every case equally, or a map keyed by the sheep's wool color in lowercase. Keys you leave out award nothing.

**Same EXP for every color**

```yaml
shear_sheep_color: 10
```

**Rare colors pay more**

```yaml
shear_sheep_color:
  white: 5
  brown: 15
  light_blue: 18
  pink: 25
```

</details>

<details>
<summary><code>shear_mushroom_cow</code> — Shear a mooshroom</summary>

Triggered when the player shears a mooshroom. A mooshroom is sheared only once before it reverts to a cow, so it cannot be farmed.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per mooshroom**

```yaml
shear_mushroom_cow: 20~30
```

</details>

<details>
<summary><code>spawn_axolotl_bucket</code> — Release an axolotl from a bucket</summary>

Triggered when the player releases an axolotl from a bucket into the world. The companion action to `catch_entity_bucket`.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per release**

```yaml
spawn_axolotl_bucket: 15~25
```

</details>

<details>
<summary><code>sniff_sniffer_egg</code> — A sniffer unearths an ancient seed</summary>

Triggered when a sniffer unearths an ancient seed. The dig is not attributed by the Bukkit API, so the reward is credited to the nearest player around the sniffer.

Takes a single value that rewards every case equally, or a map keyed by the unearthed seed item in lowercase. Keys you leave out award nothing.

**Same EXP for every seed**

```yaml
sniff_sniffer_egg: 80
```

**Per seed**

```yaml
sniff_sniffer_egg:
  torchflower_seeds: 80
  pitcher_pod: 90
```

</details>

### Workstations & Economy

<details>
<summary><code>sculpt_chiseled_block</code> — Cut a chiseled block on a stonecutter</summary>

Triggered when the player cuts a chiseled block variant on a stonecutter.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every cut**

```yaml
sculpt_chiseled_block: 4~6
```

**Per variant**

```yaml
sculpt_chiseled_block:
  chiseled_stone_bricks: 4
  chiseled_deepslate: 5
  chiseled_quartz_block: 6
```

</details>

<details>
<summary><code>use_smithing_template</code> — Take a result out of a smithing table</summary>

Triggered when the player takes a result out of a smithing table, which always consumes a smithing template.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every upgrade**

```yaml
use_smithing_template: 10~15
```

**Per result**

```yaml
use_smithing_template:
  diamond_helmet: 10
  netherite_sword: 25
  netherite_chestplate: 30
```

</details>

<details>
<summary><code>repair_trident</code> — Repair a trident on an anvil</summary>

Triggered when the player takes a repaired trident out of an anvil. Kept separate from the general `repair_item` so tridents can be rewarded specifically.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per repair**

```yaml
repair_trident: 12~20
```

</details>

<details>
<summary><code>complete_map_fill</code> — Lock a map at a cartography table</summary>

Triggered when the player finalizes a map by locking it at a cartography table. Locking captures a permanent snapshot of the explored map, and the glass-pane cost is the natural gate.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per locked map**

```yaml
complete_map_fill: 25~40
```

</details>

<details>
<summary><code>trade_special_wandering</code> — Trade with a wandering trader</summary>

Triggered when the player completes a trade with a wandering trader specifically. Fires alongside `villager_trade`.

Takes a single value that rewards every case equally, or a map keyed by the result material in lowercase. Keys you leave out award nothing.

**Same EXP for every trade**

```yaml
trade_special_wandering: 10
```

**Per traded result**

```yaml
trade_special_wandering:
  emerald: 8
  experience_bottle: 20
  enchanted_book: 40
```

</details>

### World Interaction

<details>
<summary><code>ignite_campfire</code> — Light an unlit campfire</summary>

Triggered when the player lights an unlit campfire with flint and steel or a fire charge. Rate-limited per player to defeat the light / extinguish farm.

Takes a single value that rewards every case equally, or a map keyed by the campfire material in lowercase. Keys you leave out award nothing.

**Same EXP for every campfire**

```yaml
ignite_campfire: 5
```

**Per campfire type**

```yaml
ignite_campfire:
  campfire: 5
  soul_campfire: 8
```

</details>

<details>
<summary><code>harvest_sweet_berries</code> — Gather sweet berries</summary>

Triggered when the player gathers berries from a ripe sweet berry bush. Gated by the vanilla bush growth stage, so it cannot be farmed by re-clicking an empty bush.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per bush**

```yaml
harvest_sweet_berries: 3~5
```

</details>

<details>
<summary><code>brush_archaeology</code> — Brush a suspicious block</summary>

Triggered when the player brushes a suspicious block. A suspicious block holds a single loot and is rewarded only once.

Takes a single value that rewards every case equally, or a map keyed by the brushed block material in lowercase. Keys you leave out award nothing.

**Same EXP for every brush**

```yaml
brush_archaeology: 40~60
```

**Per block**

```yaml
brush_archaeology:
  suspicious_sand: 40~60
  suspicious_gravel: 50~70
```

</details>

<details>
<summary><code>use_ender_eye_portal</code> — Place an eye of ender in a portal frame</summary>

Triggered when the player places an eye of ender into an empty end portal frame. A frame is rewarded only once.

Takes a single value. The action reports neither a target nor an amount, so a nested map is ignored.

**Random reward per frame**

```yaml
use_ender_eye_portal: 30~50
```

</details>

<details>
<summary><code>plant_flower</code> — Plant a flower</summary>

Triggered when the player plants a flower on the ground or into a flower pot. A location is rewarded only once, preventing the place / break / replace farm.

Takes a single value that rewards every case equally, or a map keyed by the flower material in lowercase. Keys you leave out award nothing.

**Same EXP for every flower**

```yaml
plant_flower: 2~4
```

**Rare flowers pay more**

```yaml
plant_flower:
  dandelion: 2
  poppy: 2
  lily_of_the_valley: 5
  wither_rose: 12
```

</details>

<details>
<summary><code>clean_armor_trim</code> — Wash a trimmed armor piece</summary>

Triggered when the player washes a trimmed armor piece in a cauldron. Rate-limited per player.

Takes a single value that rewards every case equally, or a map keyed by the trim pattern in lowercase. Keys you leave out award nothing.

**Same EXP for every trim**

```yaml
clean_armor_trim: 8~12
```

**Per pattern**

```yaml
clean_armor_trim:
  dune: 8
  sentry: 8
  eye: 15
  silence: 20
```

</details>

## Bundled Default Professions
The plugin ships with the following default professions, written to the `professions/` folder on first run. They double as ready-made examples of the action keys above:

| Profession | Theme | Highlighted Actions |
| --- | --- | --- |
| `acrobat` | Agility & acrobatics | `survive_fall`, `glide_elytra`, `dodge_action` |
| `archaeologist` | Discovery & excavation | `discover_structure`, `open_loot_chest`, `explore_biome`, `brush_archaeology`, `sniff_sniffer_egg` |
| `archer` | Ranged combat | `deal_ranged_damage` |
| `builder` | Building & art | `place_block`, `dye_item`, `carve_pumpkin`, `play_music_disc`, `ignite_campfire`, `plant_flower` |
| `chef` | Cooking & provisions | `smelt_furnace`, `consume_food`, `milk_entity` |
| `crafter` | Crafting & processing | `craft_item`, `smelt_furnace`, `brew_potion`, `sculpt_chiseled_block`, `use_smithing_template` |
| `enchanter` | Magic, alchemy & repair | `enchant_item`, `repair_item`, `brew_potion`, `consume_potion`, `repair_trident`, `clean_armor_trim` |
| `engineer` | Engineering & mechanics | `place_redstone`, `create_golem`, `activate_conduit` |
| `explorer` | Exploration & travel | `open_loot_chest`, `explore_biome`, `ride_vehicle`, `enter_portal`, `sleep_in_bed`, `complete_map_fill`, `use_ender_eye_portal` |
| `farmer` | Agriculture & ranching | `harvest_crop`, `plant_crop`, `milk_entity`, `hatch_egg`, `shear_sheep_color`, `shear_mushroom_cow` |
| `fighter` | Melee combat & bosses | `deal_damage`, `kill_mob`, `apply_debuff`, `summon_boss` |
| `fisher` | Fishing | `fish_caught`, `catch_entity_bucket`, `spawn_axolotl_bucket` |
| `gatherer` | Foraging | `strip_log`, `collect_honey`, `harvest_sweet_berries` |
| `guard` | Raid defense & protection | `trigger_raid`, `win_raid`, `use_totem`, `extinguish_fire` |
| `lumberjack` | Woodcutting | `chop_tree`, `strip_log` |
| `mariner` | Seafaring & aquatic | `swim_distance`, `ride_vehicle`, `catch_entity_bucket` |
| `merchant` | Trade & economy | `villager_trade`, `cure_zombie_villager`, `trade_special_wandering` |
| `miner` | Mining & cave hazards | `break_ore`, `survive_explosion` |
| `supporter` | Team & community aid | `apply_buff`, `heal_target`, `ring_bell`, `cure_zombie_villager` |
| `tamer` | Animal handling | `tame_animal`, `breed_animal`, `leash_entity`, `feed_pet` |
| `tanker` | Defense & mitigation | `receive_damage`, `shield_block`, `dodge_action` |

> Editing or deleting a default file does not bring it back automatically; defaults are only written when the file is missing.
