# Docs: Wiki Sync Rules

## Mandate

Any change to a user-facing surface MUST update the matching `wiki/` page in the
same pull request:

| Change | Update |
|---|---|
| CLI / commands | `wiki/commands.md` |
| Database schema | `wiki/database.md` |
| Public API | `wiki/api.md` |
| EXP actions / professions | `wiki/custom-exp.md` |
| Environment variables / config | `wiki/environment.md` |
| GUI | `wiki/gui.md` |
| Install / platforms | `wiki/installation.md` |
| Architecture | `wiki/system.md`, `wiki/bukkit.md` |

## Rules

* The root `README.md` holds only an overview — detailed docs live in `wiki/`.
* Use lowercase filenames with hyphens for multi-word pages (e.g.
  `getting-started.md`).
* Environment variables and infrastructure examples belong exclusively in
  `wiki/environment.md`; do not create a `.env.example`.
