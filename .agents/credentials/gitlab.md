# Credentials & Security

Prioritize GitLab CI/CD environment variables, followed by Agent-specific, then User-specific credentials.

Always check both GitHub and GitLab for repositories as they are separate entities. Assume context is the cloud-hosted providers (github.com and gitlab.com).

```yaml
USERNAME = System.getenv('CI_REGISTRY_USER') ?: System.getenv('AGENT_GITLAB_NAME') ?: System.getenv('USER_GITLAB_NAME')
PASSWORD = System.getenv('CI_JOB_TOKEN') ?: System.getenv('AGENT_GITLAB_TOKEN') ?: System.getenv('USER_GITLAB_TOKEN')
```
