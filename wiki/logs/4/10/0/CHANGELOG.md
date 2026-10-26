# Changelog — 4.10.0

**Released:** 2026-10-26

*MCIdentity reads each of the nine modalities in its `agents` block on its own, and resolves a
`global` modality from MCAgents 6.3.0's per-modality settings.*

## Changed

- **`agents` block** (#136) — `text`, `image`, `video`, `speech`, `decisions`, `embeddings`,
  `rerank`, `transcription` and `audio` are each read on their own. A value other than
  `global` is used as written; an `agents.model` left over from an older `config.yml` is read
  as the text model.
- **`MCAgentsGlobal`** (#136) — a `global` modality resolves from MCAgents'
  `getAgents{Modality}()` getters (6.3.0), then from `agents.{modality}` in MCAgents' own
  `config.yml`, then, for the text model only, from `getAgentsModel()` (6.2.0) or the provider's
  default on older MCAgents.
- The plugin's agent work runs on the text model, as before (#136).
- The `config.yml` comment and the configuration docs no longer say the modalities are unread
  (#136).
