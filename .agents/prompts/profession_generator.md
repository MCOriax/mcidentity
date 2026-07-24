---
name: profession-generator
description: Few-shot prompt for generating balanced, themed custom profession YAML files.
---

# Prompt: Profession Generator

A few-shot prompt for generating balanced, themed custom profession YAML files
using valid EXP action keys.

## Rules Baked Into the Prompt

* Only use EXP action keys present in the `ActionType` allow-list.
* Every file defines `stats`, `stats_random`, and `skins`.
* Keep totals balanced against existing default professions — no single stat
  dominates.

## Prompt

> Generate a custom profession YAML file for the theme: `{THEME}`.
> Requirements:
> - Include `stats`, `stats_random`, and `skins` keys.
> - Assign EXP rewards only to valid `ActionType` keys.
> - Keep the total stat budget comparable to the example below.
> - Output valid YAML only, no prose.
>
> Example (theme: miner):
> ```yaml
> stats:
>   max_health: 22
>   attack_damage: 5
> stats_random:
>   luck: [0, 3]
> skins:
>   default: your_skin_texture
> exp:
>   BLOCK_BREAK: 3
>   ORE_MINE: 8
> ```

Validate the output with the `yaml_profession_linter` tool before committing.
