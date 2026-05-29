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

## Vendored libraries — do not touch

`org.rm_yaml.snakeyaml.*` and `rm_javazoom.jl.*` are vendored pure-Java libraries with no
Minecraft API. They do not need mapping migration or 26.1 changes.

---

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

3. **Translate Yarn → Mojang imports** (see tables below).

4. **Check for new Fabric API renames** against `./fabric-api-26-1-migration-map.xml`
   (139 entries; apply via IntelliJ Refactor → Migrate Packages and Classes, or manually).
   The one rename this mod already uses: `ClientCommandManager` → `ClientCommands`.

5. **Upstream adds forge/neoforge modules** — we don't build those. Any new files under
   `forge/` or `neoforge/` can be ignored or deleted.

---

## Yarn → Mojang class map

| Yarn | Mojang |
|---|---|
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `net.minecraft.client.network.ClientPlayerEntity` | `net.minecraft.client.player.LocalPlayer` |
| `net.minecraft.client.world.ClientWorld` | `net.minecraft.client.multiplayer.ClientLevel` |
| `net.minecraft.client.option.GameOptions` | `net.minecraft.client.Options` |
| `net.minecraft.client.gui.screen.Screen` | `net.minecraft.client.gui.screens.Screen` |
| `net.minecraft.client.gui.screen.CreditsScreen` | `net.minecraft.client.gui.screens.WinScreen` |
| `net.minecraft.client.gui.hud.BossBarHud` | `net.minecraft.client.gui.components.BossHealthOverlay` |
| `net.minecraft.client.gui.hud.ClientBossBar` | `net.minecraft.client.gui.components.LerpingBossEvent` |
| `net.minecraft.client.sound.SoundManager` | `net.minecraft.client.sounds.SoundManager` |
| `net.minecraft.client.sound.SoundInstance` | `net.minecraft.client.resources.sounds.SoundInstance` |
| `net.minecraft.client.sound.SoundSystem` | `net.minecraft.client.sounds.SoundEngine` |
| `net.minecraft.entity.Entity` | `net.minecraft.world.entity.Entity` |
| `net.minecraft.entity.mob.HostileEntity` | `net.minecraft.world.entity.monster.Monster` |
| `net.minecraft.entity.passive.HorseEntity` | `net.minecraft.world.entity.animal.equine.Horse` |
| `net.minecraft.entity.passive.PigEntity` | `net.minecraft.world.entity.animal.pig.Pig` |
| `net.minecraft.entity.passive.VillagerEntity` | `net.minecraft.world.entity.npc.villager.Villager` |
| `net.minecraft.entity.vehicle.BoatEntity` | `net.minecraft.world.entity.vehicle.boat.AbstractBoat` |
| `net.minecraft.entity.vehicle.MinecartEntity` | `net.minecraft.world.entity.vehicle.minecart.Minecart` |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` |
| `net.minecraft.registry.RegistryKeys` | `net.minecraft.core.registries.Registries` |
| `net.minecraft.registry.tag.TagKey` | `net.minecraft.tags.TagKey` |
| `net.minecraft.sound.SoundCategory` | `net.minecraft.sounds.SoundSource` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `net.minecraft.util.math.BlockPos` | `net.minecraft.core.BlockPos` |
| `net.minecraft.util.math.Box` | `net.minecraft.world.phys.AABB` |
| `net.minecraft.util.math.Vec3d` | `net.minecraft.world.phys.Vec3` |
| `net.minecraft.world.World` | `net.minecraft.world.level.Level` |
| `net.minecraft.world.biome.Biome` | `net.minecraft.world.level.biome.Biome` |

## Yarn → Mojang method/field map

| Yarn | Mojang |
|---|---|
| `mc.world` | `mc.level` |
| `mc.currentScreen` | `mc.screen` |
| `mc.send(Runnable)` | `mc.execute(Runnable)` |
| `mc.inGameHud` | `mc.gui` |
| `gui.getBossBarHud()` | `gui.getBossOverlay()` |
| `options.getSoundVolume(cat)` | `options.getSoundSourceVolume(SoundSource)` |
| `options.write()` | `options.save()` |
| `player.getBlockPos()` | `player.blockPosition()` |
| `player.getEntityPos()` | `player.position()` |
| `player.isSubmergedInWater()` | `player.isUnderWater()` |
| `player.fishHook` (field) | `player.fishing` (field) |
| `player.getRecentDamageSource()` | `player.getLastDamageSource()` |
| `damageSource.getSource()` | `damageSource.getDirectEntity()` |
| `world.isSkyVisible(pos)` | `level.canSeeSky(pos)` |
| `world.getRegistryKey()` | `level.dimension()` |
| `world.getTime()` | `level.getGameTime()` |
| `world.getTimeOfDay()` | `level.getOverworldClockTime()` |
| `world.getEntitiesByClass(C, Box, p)` | `level.getEntitiesOfClass(C, AABB, p)` |
| `world.getBiome(pos)` | `level.getBiome(pos)` (returns `Holder<Biome>`) |
| `World.OVERWORLD/NETHER/END` | `Level.OVERWORLD/NETHER/END` |
| `registryKey.getValue()` | `resourceKey.location()` |
| `tagKey.id()` | `tagKey.location()` |
| `Registries.BLOCK.getId(b)` | `BuiltInRegistries.BLOCK.getKey(b)` |
| `biome.getKey()` (Holder) | `biome.unwrapKey()` |
| `biome.streamTags()` | `biome.tags()` |
| `biome.value().getPrecipitation(pos, sea)` | `biome.value().getPrecipitationAt(pos, sea)` |
| `BlockPos.Mutable` | `BlockPos.MutableBlockPos` |
| `soundInstance.getId()` | `soundInstance.getLocation()` |
| `soundInstance.getCategory()` | `soundInstance.getSource()` |
| `soundInstance.getAttenuationType()` | `soundInstance.getAttenuation()` |
| `soundManager.isPlaying(si)` | `soundManager.isActive(si)` |
| `Text.literal(s)` / `Text.of(s)` | `Component.literal(s)` |
| `TagKey.of(registry, id)` | `TagKey.create(registry, id)` |
| `Identifier.of(namespace, path)` | `Identifier.fromNamespaceAndPath(namespace, path)` |

**Things that do NOT change:** `mc.player`, `mc.options`, `mc.setScreen`, `mc.getSoundManager`,
`getX/getY/getZ`, `getHealth/getMaxHealth`, `isSleeping`, `getVehicle`,
`isRaining/isThundering/getSeaLevel/getBlockState`, `Vec3.subtract/length`,
`MutableBlockPos.set`, `Biome.Precipitation.RAIN/SNOW/NONE`, `SoundInstance.Attenuation.NONE`.

---

## MC 26.1 API gotchas

Changes beyond the Yarn→Mojang rename that tools won't catch:

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

- **Chat messages.** `player.sendMessage(text, false)` → `player.sendSystemMessage(text)`.

- **Boat refactor.** Use `instanceof AbstractBoat` for "is riding a boat" (covers rafts too).
  Plain `Boat` excludes rafts.

- **`ClientCommandManager` renamed to `ClientCommands`** in Fabric API 26.1.

---

## Mixin reference

| Mixin | Target | Notes |
|---|---|---|
| `SoundManagerMixin` | `net.minecraft.client.sounds.SoundManager` | Injects into `play(SoundInstance)` returning `SoundEngine.PlayResult`; cancels vanilla music sounds |
| `BossBarHudAccessor` | `net.minecraft.client.gui.components.BossHealthOverlay` | `@Accessor("bossBars")` returns `Map<UUID, LerpingBossEvent>` |
| `MusicManagerMixin` | `net.minecraft.client.sounds.MusicManager` | Cancels `tick()` to suppress vanilla music |

---

## Smoke test checklist

After building and installing, verify in-game:

- Mod loads with no mixin errors in the log
- Vanilla music is suppressed; Reactive Music plays
- `/reactivemusic` opens the config screen (also via ModMenu)
- Config screen renders correctly (all labels visible, buttons functional)
- DAY / NIGHT / SUNSET / SUNRISE events trigger correctly across a full day cycle
- Biome, dimension, underground, boss bar, village, nearby mobs events trigger
- Vehicle events work: boat, minecart, horse, pig
- Fishing, underwater, weather (rain/snow/storm) events trigger
- `/reactivemusic blacklistDimension`, `toggleSoundEventLogging`, `toggleLogging`, `logBlockCounter` work
- Jukebox / music disc ducking works
- Existing `ReactiveMusic.json5` config loads without error
