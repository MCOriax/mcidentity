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

## EXP Gain Syntax
You can define EXP gain for specific actions using several formats:

<details>
<summary>Simple Value</summary>

Award a specific amount or random range of EXP.
```yaml
receive_damage: 10 # Fixed
deal_damage: 10~20 # Random Range
```
</details>

<details>
<summary>Condition-Based (Numerical)</summary>

Award EXP based on numerical thresholds (e.g., damage amount). Supports range keys (e.g., `min-max` or `min~max`).
```yaml
deal_damage:
  "1-10": 5       # 1 to 10 damage awards 5 EXP
  "11-50": 10~20  # 11 to 50 damage awards random 10-20 EXP
  "51-100": 50    # 51 to 100 damage awards 50 EXP
```
</details>

<details>
<summary>Condition-Based (String)</summary>

Award EXP based on specific targets (e.g., mob types).
```yaml
kill_mob:
  zombie: 20      # Killing a zombie awards 20 EXP
  skeleton: 25    # Killing a skeleton awards 25 EXP
  ender_dragon: 1000~2000 # Boss kill awards random large EXP
```
</details>

<details>
<summary>Example Configuration (<code>professions/tanker.yml</code>)</summary>

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
- **`stats:`**: Defines the starting attributes for the profession.
  - `energy`: Maximum energy.
  - `health`: Maximum health.
  - `food`: Maximum food capacity. Health regeneration consumes **10 food points per 1% HP regenerated**. Regeneration halts when the identity's current food drops below 10% of this capacity.
  - `strength`, `defense`, `wisdom`, `luck`, `agility`, `resilience`: Primary attributes.
- **`stats_random:`**: Defines the total amount of bonus points that will be randomly distributed across the 6 primary attributes (strength, defense, wisdom, luck, agility, resilience) when a new identity is created.
- **`skins:`**: Defines available Base64 skin textures for this profession. When a new identity of this profession is created, a skin will be chosen randomly from this list.

Supported action keys include:

**Combat**
- `deal_damage`: Triggered when the player deals melee damage to another entity.
- `kill_mob`: Triggered when the player kills an entity.
- `deal_ranged_damage`: Triggered when the player deals damage with a projectile (e.g. a bow).
- `critical_hit`: Triggered when the player lands a critical melee hit.

**Defense**
- `receive_damage`: Triggered when the player takes damage.
- `shield_block`: Triggered when the player blocks incoming damage with a shield.
- `dodge_action`: Triggered when the player evades incoming damage. The dodge chance scales with the identity's `agility` stat, and a successful dodge fully negates the damage.

**Gathering**
- `break_ore`: Triggered when the player breaks an ore block (keyed by ore material, e.g. `diamond_ore`).
- `chop_tree`: Triggered when the player breaks a log block.
- `harvest_crop`: Triggered when the player breaks a fully grown crop.
- `fish_caught`: Triggered when the player reels in a fish.

**Crafting & Processing**
- `craft_item`: Triggered when the player crafts an item (keyed by result material).
- `smelt_furnace`: Triggered when the player extracts a smelted item from a furnace (keyed by result material).
- `brew_potion`: Triggered when the player collects a brewed potion from a brewing stand.

**Interaction & Exploration**
- `tame_animal`: Triggered when the player tames an animal (keyed by entity type).
- `breed_animal`: Triggered when the player breeds two animals (keyed by entity type).
- `explore_biome`: Triggered the first time the player enters a given biome during the current server session (keyed by biome name).
- `heal_target`: Triggered when the player heals another entity with a splash potion.

**Magic & Economy**
- `enchant_item`: Triggered when the player enchants an item at an enchanting table (keyed by item material).
- `villager_trade`: Triggered when the player collects the result of a villager trade (keyed by result material).
- `repair_item`: Triggered when the player takes a repaired or combined item out of an anvil. Only durability-bearing items qualify (keyed by result material).

**Building & Survival**
- `place_block`: Triggered when the player places a block (keyed by block material). A location is rewarded only once, preventing the place / break / replace EXP farm.
- `consume_food`: Triggered when the player eats a food item (keyed by food material). Non-food consumables such as potions and milk are ignored.
- `shear_entity`: Triggered when the player shears an entity such as a sheep (keyed by entity type).

**Advanced Exploration & Movement**
- `open_loot_chest`: Triggered when the player opens a naturally generated loot container. A vanilla loot table only generates once, so the reward cannot be farmed.
- `glide_elytra`: Triggered when the player starts gliding with an elytra. Rate-limited per player to prevent rapid toggle farming.
- `survive_fall`: Triggered when the player takes fall damage but survives it (numerical, keyed by the survived damage amount).

**Team & Support**
- `apply_buff`: Triggered when the player lands a beneficial (non-healing) potion on another player. Kept distinct from `heal_target`, which covers instant healing.

**Farming & Ranching**
- `plant_crop`: Triggered when the player plants a crop seed (keyed by crop material).
- `strip_log`: Triggered when the player strips a log with an axe (keyed by log material). A location is rewarded only once, preventing the place / strip / break / replace farm.
- `milk_entity`: Triggered when the player milks a cow, mushroom cow or goat with a bucket (keyed by entity type).
- `catch_entity_bucket`: Triggered when the player captures an entity in a bucket, such as a fish or axolotl (keyed by entity type).

**Exploration & Travel**
- `discover_structure`: Triggered when the player discovers a major structure, detected through the vanilla advancement granted on entry (stronghold, nether fortress, bastion remnant).
- `ride_vehicle`: Triggered when the player starts riding a vehicle such as a boat or minecart (keyed by vehicle type). Rate-limited per player.
- `leash_entity`: Triggered when the player leashes an entity (keyed by entity type). Rate-limited per player.

**Interaction & Art**
- `play_music_disc`: Triggered when the player inserts a music disc into an empty jukebox (ejecting is not rewarded).
- `dye_item`: Triggered when the player dyes a sheep (keyed by dye material). Rate-limited per player.
- `carve_pumpkin`: Triggered when the player carves a pumpkin with shears. A location is rewarded only once.
- `ring_bell`: Triggered when the player rings a bell. Rate-limited per player.

**Magic, Utility & Combat**
- `ignite_fire`: Triggered when the player ignites a fire with flint and steel. Rate-limited per player.
- `consume_potion`: Triggered when the player drinks a potion.
- `apply_debuff`: Triggered when the player lands a harmful potion on another entity.
- `cure_zombie_villager`: Triggered when the player feeds a golden apple to a weakened zombie villager, starting the cure.

**Engineering & Mechanics**
- `place_redstone`: Triggered when the player places a redstone component such as dust, a repeater, a piston, an observer, a button or a pressure plate (keyed by block material). A location is rewarded only once, preventing the place / break / replace farm.
- `create_golem`: Triggered when a player builds a utility golem (keyed by entity type, e.g. `iron_golem` or `snow_golem`). The Bukkit API does not attribute the build to a player, so the reward is credited to the nearest player around the spawn.
- `activate_conduit`: Triggered when the player places (activates) a conduit. A location is rewarded only once.

**Dimensions & Aquatic**
- `swim_distance`: Triggered for every 10 blocks the player swims (sprint-swimming in water). Distance is accumulated per player so the reward scales with genuine travel rather than firing on every move.
- `enter_portal`: Triggered when the player travels through a nether or end portal (keyed by the destination environment, e.g. `nether` or `the_end`). Rate-limited per player to stop back-and-forth farming.
- `use_totem`: Triggered when a totem of undying saves the player from death.

**Survival & Resilience**
- `sleep_in_bed`: Triggered when the player successfully lies down to sleep in a bed. Rate-limited per player.
- `extinguish_fire`: Triggered when the player extinguishes a fire block (keyed by fire material). Rate-limited per player to defeat the ignite / extinguish farm.
- `survive_explosion`: Triggered when the player takes block or entity explosion damage but survives it (numerical, keyed by the survived damage amount).

**Special Events & Bosses**
- `trigger_raid`: Triggered when the player triggers a village raid, for example by entering a village while carrying Bad Omen.
- `win_raid`: Triggered for every player credited with winning a village raid.
- `summon_boss`: Triggered when a player summons a boss such as the Wither (keyed by entity type). The reward is credited to the nearest player around the spawn.

**Specialty Farming & Pets**
- `feed_pet`: Triggered when the player feeds one of their own tamed pets an actual food item (keyed by entity type). Rate-limited per player.
- `hatch_egg`: Triggered when a thrown egg hatches into a chick.
- `collect_honey`: Triggered when the player bottles honey from a full beehive or bee nest with a glass bottle. Only a full hive yields honey, which the bees take real time to refill.

**Ranching & Wildlife**
- `shear_sheep_color`: Triggered when the player shears a sheep (keyed by the sheep's wool color, e.g. `pink`). Fires alongside `shear_entity`, so configs can reward rare colors specifically. Re-shearing is gated by vanilla wool regrowth.
- `shear_mushroom_cow`: Triggered when the player shears a mooshroom. A mooshroom is sheared only once before it reverts to a cow, so it cannot be farmed.
- `spawn_axolotl_bucket`: Triggered when the player releases an axolotl from a bucket into the world. The companion action to `catch_entity_bucket`.
- `sniff_sniffer_egg`: Triggered when a sniffer unearths an ancient seed (keyed by the seed item, `torchflower_seeds` or `pitcher_pod`). The dig is not attributed by the Bukkit API, so the reward is credited to the nearest player around the sniffer.

**Workstations & Economy**
- `sculpt_chiseled_block`: Triggered when the player cuts a chiseled block variant on a stonecutter (keyed by result material, e.g. `chiseled_stone_bricks`).
- `use_smithing_template`: Triggered when the player takes a result out of a smithing table, which always consumes a smithing template (keyed by result material).
- `repair_trident`: Triggered when the player takes a repaired trident out of an anvil. Kept separate from the general `repair_item` so tridents can be rewarded specifically.
- `complete_map_fill`: Triggered when the player finalizes a map by locking it at a cartography table. Locking captures a permanent snapshot of the explored map, and the glass-pane cost is the natural gate.
- `trade_special_wandering`: Triggered when the player completes a trade with a wandering trader specifically (keyed by result material). Fires alongside `villager_trade`.

**World Interaction**
- `ignite_campfire`: Triggered when the player lights an unlit campfire with flint and steel or a fire charge (keyed by campfire material). Rate-limited per player to defeat the light / extinguish farm.
- `harvest_sweet_berries`: Triggered when the player gathers berries from a ripe sweet berry bush. Gated by the vanilla bush growth stage, so it cannot be farmed by re-clicking an empty bush.
- `brush_archaeology`: Triggered when the player brushes a suspicious block (keyed by block material, `suspicious_sand` or `suspicious_gravel`). A suspicious block holds a single loot and is rewarded only once.
- `use_ender_eye_portal`: Triggered when the player places an eye of ender into an empty end portal frame. A frame is rewarded only once.
- `plant_flower`: Triggered when the player plants a flower on the ground or into a flower pot (keyed by flower material). A location is rewarded only once, preventing the place / break / replace farm.
- `clean_armor_trim`: Triggered when the player washes a trimmed armor piece in a cauldron (keyed by trim pattern). Rate-limited per player.

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
