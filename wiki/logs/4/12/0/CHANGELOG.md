# Changelog — 4.12.0

**Released:** 2026-11-12

A minor release. Every MCIdentity AI tool names a permission node, so MCAgents 6.9.0 and
later only offer a player the tools they may use and no tokens go on the rest.

## Added

- `PluginTool#getPermission()`, answered to MCAgents through the tool adapter (#145).
- `mcoriax.mcidentity.tool.mcidentity_get_my_identity` (`default: true`) for `mcidentity_get_my_identity` (#145).
- `mcoriax.mcidentity.tool.mcidentity_get_my_stats` (`default: true`) for `mcidentity_get_my_stats` (#145).

## Upgrading

Drop in the new jar. Players keep the tools they have today; deny a node to hide one.
Older MCAgents versions ignore the nodes.
