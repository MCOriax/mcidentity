# Ethics: Safe Degradation

Safety bounds ensuring failures never corrupt player state or crash the game
thread.

## Rules

* **Read failures resolve to neutral fallbacks.** A failed database read returns
  an empty `Optional`, a zero value, or a documented default — never a thrown
  exception that reaches the main Bukkit/Folia thread.
* **Never corrupt on write failure.** If a write cannot be confirmed, keep the
  last known-good in-memory state; do not overwrite RAM with partial results.
* **Cold-boot login protection.** When the central server (or backend) is not
  yet reachable, `PlayerLoginListener` politely refuses the connection with a
  clear message rather than admitting the player with empty/incorrect state that
  could be persisted over their real data.
* **Fail closed, explain clearly.** Prefer refusing an action to performing a
  destructive or ambiguous one. Log enough context to diagnose without leaking
  secrets.
