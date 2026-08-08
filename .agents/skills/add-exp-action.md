---
name: add-exp-action
description: Skill for adding a new profession EXP trigger (ActionType) that grants experience.
---

# Skill: Add an EXP Action

Add a new profession EXP trigger (an `ActionType`) that grants experience.

## Steps

1. **Declare the action.** Add the new value to `api/model/ActionType` so the
   contract knows the trigger exists.
2. **Parse the config.** Update the YAML configuration parser so the new action
   key is read from the profession files under `professions/`. Validate keys
   with the `yaml_profession_linter` tool.
3. **Listen for the event.** Add or extend a Bukkit event listener under
   `bukkit/listeners/` (grouped by category). Keep listeners modular — one
   responsibility per class.
4. **Apply EXP math.** Call the experience math utilities in `bukkit/util`
   rather than recomputing curves inline. Route passive gains through the
   buffered passive base-profession EXP service.
5. **Folia safety.** World/inventory mutations must hop back to the
   `PlatformScheduler`. Run the listener through
   `prompts/folia-compatibility-check.md`.
6. **RAM first.** Update the in-memory `MCIdentityManager` state for instant
   feedback, then flush to the database asynchronously and in bulk.
7. **Docs.** Update `wiki/custom-exp.md` and, if commands change,
   `wiki/commands.md`.
