# FloodedEnd todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] Low: preset datapack metadata now includes `min_format` and `max_format`, checked with Minecraft's server pack metadata reader.
- [x] Low: `/floodedend preset clear` now fails with an explanation when no preset is installed, covered through real command dispatch.
