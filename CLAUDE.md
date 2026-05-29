# ReactiveMusic-Cont — Claude Notes

This is a Fabric-only fork of [CircuitLord/ReactiveMusic](https://github.com/CircuitLord/ReactiveMusic),
targeting **Minecraft 26.1** with **Mojang mappings** (official names). The upstream repo uses
Stonecutter (multi-version) + Architectury (multi-loader) with Yarn mappings — our build is a
simple Fabric Loom setup.

## Building

```
./gradlew build
```

Output: `build/libs/reactivemusic-cont-<version>+26.1.jar`

Java 25 required. See `gradle.properties` for version coordinates.

## Merging upstream changes

The upstream uses **Yarn mappings** and **Stonecutter conditionals**. Every merge requires:

1. **Strip Stonecutter build files** — discard any `settings.gradle.kts`, `build.gradle.kts`,
   `stonecutter.gradle.kts` brought in by the merge. Our `settings.gradle` and `build.gradle`
   must be kept as-is.

2. **Strip Stonecutter source conditionals** — upstream Java files contain blocks like:
   ```java
   //? if >=1.21 {
   import net.minecraft.registry.tag.TagKey;
   //?} else {
   /*import net.minecraft.tag.TagKey;
   *///?}
   ```
   Remove all `//? if` / `//?}` markers and keep only the code appropriate for 26.1.

3. **Translate Yarn → Mojang imports** (see table below).

4. **Check for new Fabric API renames** against `./fabric-api-26-1-migration-map.xml`
   (139 entries; apply via IntelliJ Refactor → Migrate Packages and Classes, or manually).
   The one rename this mod already uses: `ClientCommandManager` → `ClientCommands`.

5. **Upstream adds forge/neoforge modules** — we don't build those. Any new files under
   `forge/` or `neoforge/` can be ignored or deleted.

For a full porting reference (build coords, mixin changes, method renames, test checklist),
see `./PORTING-26.1.md`.

## Yarn → Mojang mapping quick reference

Only the symbols actually used in this mod:

| Yarn | Mojang |
|---|---|
| `net.minecraft.registry.tag.TagKey` | `net.minecraft.tags.TagKey` |
| `net.minecraft.world.biome.Biome` | `net.minecraft.world.level.biome.Biome` |
| `net.minecraft.registry.RegistryKeys` | `net.minecraft.core.registries.Registries` |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` |
| `TagKey.of(registry, id)` | `TagKey.create(registry, id)` |
| `Identifier.of(namespace, path)` | `Identifier.fromNamespaceAndPath(namespace, path)` |
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `net.minecraft.client.network.ClientPlayerEntity` | `net.minecraft.client.player.LocalPlayer` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.world.World` | `net.minecraft.world.level.Level` |

For the complete class rename list, see `./PORTING-26.1.md` §6.

## MC 26.1 API gotchas

These are 26.1-specific changes beyond the Yarn→Mojang rename that tools won't catch:

- **Rendering pipeline replaced.** `GuiGraphics` does not exist. Use `GuiGraphicsExtractor`.
  - `Screen.render(GuiGraphics, int, int, float)` → `Screen.extractRenderState(GuiGraphicsExtractor, int, int, float)`
  - Drawing: `graphics.drawString(font, text, x, y, color)` → `context.text(font, text, x, y, color)`
  - Drawing: `graphics.drawCenteredString(...)` → `context.centeredText(...)`
  - Fills: `graphics.fill(...)` → `context.fill(...)` (same signature)
  - **Do NOT call `this.extractBackground(...)` inside `extractRenderState()`** — the framework
    (`extractRenderStateWithTooltipAndSubtitles`) already calls it before invoking your override.
    Calling it again throws `IllegalStateException: Can only blur once per frame`.

- **Text colors must include alpha.** `GuiGraphicsExtractor.text()` checks `ARGB.alpha(color)`
  and silently skips drawing if alpha is 0. Colors like `0xFFFFFF` (white, no alpha) render
  nothing — use `0xFFFFFFFF`. Colors with explicit alpha like `0x88FFFFFF` are fine as-is.

- **Mouse events changed.** `Screen.mouseClicked(double, double, int)` no longer exists.
  Override `mouseClicked(MouseButtonEvent event, boolean doubled)` and use `event.x()` /
  `event.y()` for coordinates.

- **`ClientCommandManager` renamed to `ClientCommands`** in Fabric API 26.1.
  `ClientCommandManager.literal(...)` → `ClientCommands.literal(...)`

- **World clock.** `world.getTimeOfDay()` → `world.getOverworldClockTime()`.

- **Chat messages.** `player.sendMessage(text, false)` → `player.sendSystemMessage(text)`.
