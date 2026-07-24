# Prompt: Folia Compatibility Check

Run this template against any new Bukkit listener or scheduler task to catch
Folia region-threading violations before merge.

## Prompt

> Review the following listener/task for Folia region-threading safety. For each
> point, answer YES/NO with the offending line if NO:
>
> 1. Does it avoid Bukkit `getScheduler()` calls and use the `PlatformScheduler`
>    abstraction instead?
> 2. Are all world/block mutations run on the `RegionScheduler` for the affected
>    location?
> 3. Are all entity/player/inventory mutations run on the `EntityScheduler` for
>    that entity?
> 4. Does it avoid touching entities or blocks outside the region it currently
>    owns (no cross-region access on the calling thread)?
> 5. Are all database reads/writes async (off the scheduler) and reconciled back
>    via the `PlatformScheduler` only for the world-state step?
> 6. Does it avoid static mutable shared state without synchronization?
>
> List every violation with a concrete fix.

Paste the listener/task source after this prompt.
