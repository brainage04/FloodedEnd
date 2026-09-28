# FloodedEnd todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [ ] Low: the preset datapack's `pack.mcmeta` has only `pack_format` 107 (`EndOceanPreset.java:117-121`), so every pack scan logs a WARN with a stack trace about missing `min_format`/`max_format`.
- [ ] Low: `/floodedend preset clear` reports success when no preset is installed.
