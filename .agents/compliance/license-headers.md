---
name: license-headers
description: Rules for retaining Apache-2.0 / SPDX license headers when editing or adding source files.
---

# Compliance: License Headers

## Mandate

* Retain Apache-2.0 / `SPDX-License-Identifier` tags on files that carry them.
* This explicitly includes shell scripts such as `gradlew` and any source file
  that already bears a license header.

## Rules

* Never strip or alter an existing SPDX line or license header when editing a
  file.
* When adding a new source file to a module that headers its files, add the
  matching `SPDX-License-Identifier: Apache-2.0` header.
* Do not add license headers to generated files or third-party vendored code
  that carries its own license.
