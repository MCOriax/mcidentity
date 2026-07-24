# Dependencies: Java 25 Toolchain

## Mandate

* The project provisions a JDK 25 toolchain via the Foojay resolver. Build and
  test against Java 25.
* Use Java 25 features and syntax where they improve clarity: records, sealed
  types, pattern matching for `switch`, record patterns, virtual threads where
  appropriate, and modern collection/stream APIs.

## Guidance

* Prefer records for immutable value types in `api/model`.
* Prefer sealed interfaces + pattern matching over instanceof chains.
* Do not lower the language level or add multi-release fallbacks — Java 25 is the
  single supported baseline.
* Keep toolchain configuration in the Gradle build; do not rely on the ambient
  system JDK.
