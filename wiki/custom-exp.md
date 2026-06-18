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

**Core**
- `receive_damage`: Triggered when the player takes damage.
- `deal_damage`: Triggered when the player deals melee damage to another entity.
- `kill_mob`: Triggered when the player kills an entity.

**Combat & Defense**
- `deal_ranged_damage`: Triggered when the player deals damage with a projectile (e.g. a bow).
- `shield_block`: Triggered when the player blocks incoming damage with a shield.
- `critical_hit`: Triggered when the player lands a critical melee hit.
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
