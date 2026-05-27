# Porting Reactive Music to Minecraft 26.1 (Fabric)

> Status: PLAN ONLY — no mod code changed. Written in a network-restricted
> environment that can reach github.com / Maven Central / Gradle but **not**
> the Fabric/Mojang/Modrinth maven hosts (all 403), and only has **JDK 21**
> (26.1 needs JDK 25). So nothing here was compile-verified. Execute in an
> environment with full network access **and a JDK 25 toolchain**, and iterate
> against a real Gradle build.
>
> Current state: targets **1.21.9** on **Yarn**, Loom `1.11-SNAPSHOT`, Loader
> `0.17.3`, Fabric API `0.134.0+1.21.9`, Gradle `8.14`, Java 21.

This plan was cross-checked against the official Fabric sources (fetched from
GitHub raw, since the rendered docs host was blocked here):

- Fabric 26.1 release post — https://fabricmc.net/2026/03/14/261.html
- Porting to 26.1 — https://docs.fabricmc.net/develop/porting/  (repo: `FabricMC/fabric-docs:develop/porting/index.md`)
- Migrating Mappings — https://docs.fabricmc.net/develop/porting/mappings/
- Porting to Fabric API 26.1 — https://docs.fabricmc.net/develop/porting/fabric-api  (full rename list)
- Fabric example mod (26.1 branch) — https://github.com/FabricMC/fabric-example-mod/tree/26.1
- Fabric Develop (recommended versions) — https://fabricmc.net/develop/
- NeoForge 1.21.11→26.1 vanilla primer — https://github.com/neoforged/.github/blob/main/primers/26.1/index.md
- **Fabric API class migration map (saved in this repo):** `docs/fabric-api-26-1-migration-map.xml` (139 entries; apply via IntelliJ "Migrate")

---

## 1. Why this is a hard, breaking change

26.1 is **the first unobfuscated Minecraft release**. Fabric stopped
maintaining Yarn/Intermediary after 1.21.11, so there is no Yarn for 26.1 and
mods must use **Mojang's official mappings**. No 1.21.11-or-older mod works on
26.1, even as a compile-only dependency — everything must be recompiled against
the new toolchain. For this mod that means:

1. A **mappings migration** (Yarn → official) renaming almost every
   `net.minecraft.*` class and many methods it uses.
2. A **build-system migration**: new non-remapping Loom plugin, Gradle 9.4.x,
   Loader 0.18.4+, **Java 25**, and 26.1 builds of Fabric API / ModMenu / YACL.
3. A **Fabric API rename** (`ClientCommandManager` → `ClientCommands`).
4. A handful of **vanilla 26.1 changes** beyond the pure rename (see §6.4).

Do it in **two phases** (Fabric's recommended order):

- **Phase A — migrate mappings while still on 1.21.x.** Easier to keep a green
  build as you only change names, not versions.
- **Phase B — bump to 26.1.** Build script + Fabric API renames + vanilla API
  changes, compiled with JDK 25.

---

## 2. Version coordinates to look up FIRST (blocked here — all 403)

Fill these from https://fabricmc.net/develop/ and the maven metadata, and
sanity-check against the 26.1 example mod's `build.gradle`/`gradle.properties`.

| Coordinate | Current | 26.1 value |
|---|---|---|
| `minecraft_version` | 1.21.9 | `26.1` (confirm exact string; patch builds 26.1.x exist) |
| Loom plugin id | `fabric-loom` | **`net.fabricmc.fabric-loom`** (confirmed) |
| `loom_version` | 1.11-SNAPSHOT | `1.15.x` (Loom 1.15) — confirm exact |
| `loader_version` | 0.17.3 | `0.18.4` (or newer stable) |
| `fabric_version` (Fabric API) | 0.134.0+1.21.9 | the published `…+26.1` build — **LOOK UP** |
| Gradle | 8.14 | `9.4.x` — set via `./gradlew wrapper --gradle-version latest` |
| Java | 21 | **25** (confirmed) |
| `modmenu_version` | 16.0.0-rc.1 | 26.1 build — **LOOK UP** |
| `yacl_version` | 3.8.0+1.21.9-fabric | 26.1 build — **LOOK UP**; confirm artifact + mod id `yet_another_config_lib_v3` unchanged |

Lookups once network is available:
```
curl -s https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml
curl -s https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml
curl -s https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/maven-metadata.xml
curl -s https://maven.isxander.dev/releases/dev/isxander/yet-another-config-lib-fabric/maven-metadata.xml
```

You also need **JDK 25** installed and **IntelliJ IDEA 2025.3+** (for Java 25
support and the Migrate refactoring).

---

## 3. PHASE A — migrate Yarn → Mojang mappings (stay on 1.21.x)

Use one of the two official tools. This mod is **pure Java** (the `org.rm_yaml`
and `rm_javazoom` packages are vendored libraries, not Minecraft code), so
either works:

- **Loom `migrateMappings`** (recommended here; Java-only, which is fine):
  1. In `build.gradle`, set `mappings loom.officialMojangMappings()` (replacing
     the `net.fabricmc:yarn` line) — **this `officialMojangMappings()` call is
     only for this phase; it gets removed in Phase B.**
  2. Run the migrate task against the current MC version, e.g.
     `./gradlew migrateMappings` (see
     https://docs.fabricmc.net/develop/migrating-mappings/loom for exact args),
     which rewrites `src/main/java` from Yarn to official names.
  3. Review the diff and fix what the tool missed — **mixins always need manual
     review** (target method names/descriptors, `@Shadow` field names/types,
     `@Accessor` field names). See §7.
- **Ravel IntelliJ plugin** (alternative; also handles Kotlin and may resolve
  complex projects better): adds a GUI migration dialog.

Neither tool is perfect — expect manual fixups. §6 below is a **cross-check
reference** for the renames this specific mod needs; §6.4 lists changes the
mapping tools will NOT catch (real 26.1 API changes).

Goal of Phase A: the mod still targets 1.21.x but compiles & runs on official
mappings. Commit that as a checkpoint before Phase B.

---

## 4. PHASE B — build-script changes for 26.1

Follow the official `Porting to 26.1` steps:

### 4.1 `gradle/wrapper/gradle-wrapper.properties`
`./gradlew wrapper --gradle-version latest` (targets Gradle 9.4.x). Equivalent
manual edit:
```
distributionUrl=https\://services.gradle.org/distributions/gradle-9.4.0-bin.zip
```

### 4.2 `gradle.properties` (fill bracketed values from §2)
```properties
org.gradle.jvmargs=-Xmx8G
org.gradle.parallel=true

# https://fabricmc.net/develop
minecraft_version=26.1
loader_version=0.18.4
loom_version=1.15-SNAPSHOT        # confirm exact

fabric_version=<LOOK UP +26.1>

mod_version=1.3.0                  # bump from 1.2.2
maven_group=circuitlord.reactivemusic
archives_base_name=reactivemusic

modmenu_version=<LOOK UP 26.1>
yacl_version=<LOOK UP 26.1>
```
> The `yarn_mappings` property is removed.

### 4.3 `build.gradle`
1. Plugin id: `id 'fabric-loom'` → **`id 'net.fabricmc.fabric-loom'`** (keep
   `version "${loom_version}"`).
2. **Delete the `mappings …` line entirely** (both the old yarn line and the
   Phase-A `loom.officialMojangMappings()` line). The new plugin does not remap;
   26.1 is already official names.
3. Change remapping configurations to plain ones:
   - `modImplementation` → `implementation`
   - `modApi` → `api`
   - `modCompileOnly` → `compileOnly`
   So fabric-loader, fabric-api, YACL → `implementation`, modmenu → `api`. The
   YACL `exclude(group: "net.fabricmc.fabric-api")` closure stays.
4. Any `remapJar` references → `jar` (none currently in this build, but check).
5. **Java 25**: `it.options.release = 25`; `sourceCompatibility =
   JavaVersion.VERSION_25`; `targetCompatibility = JavaVersion.VERSION_25`.
6. Keep `processResources` `fabric.mod.json` `expand version`, the `jar`
   LICENSE rename, `withSourcesJar()`, and publishing as-is.
7. Repos: keep TerraformersMC + Xander maven; confirm YACL group/artifact.
> Compare the final result with the 26.1 example mod's `build.gradle`.

### 4.4 `settings.gradle`
`pluginManagement` already lists Fabric maven + mavenCentral + plugin portal —
sufficient. If you ever pin the Loom plugin id here, use
`net.fabricmc.fabric-loom`.

### 4.5 `fabric.mod.json`
```json
"depends": {
  "fabricloader": ">=0.18.4",
  "minecraft": ">=26.1 <26.2",       // confirm predicate syntax; include 26.1.x patches you support
  "java": ">=25",
  "yet_another_config_lib_v3": "*"   // confirm mod id unchanged for 26.1 YACL
}
```

### 4.6 `reactivemusic.mixins.json`
`"compatibilityLevel": "JAVA_21"` → **`"JAVA_25"`**.

### 4.7 Access widener / class tweaker
None in this mod (no `.accesswidener`, none referenced in `fabric.mod.json`), so
the docs' "change `named` → `official`" step is N/A.

---

## 5. Fabric API renames affecting THIS mod

The full list is in `docs/fabric-api-26-1-migration-map.xml` (139 class
entries) — apply it with IntelliJ's Migrate refactoring (Refactor → Migrate
Packages and Classes) for the class-level renames, then fix member renames by
hand. Of those, the mod only uses **one** renamed symbol:

- **`net.fabricmc.fabric.api.client.command.v2.ClientCommandManager`
  → `…client.command.v2.ClientCommands`** (used in `ReactiveMusic.onInitialize`:
  `ClientCommandManager.literal(...)` → `ClientCommands.literal(...)`, and the
  import).

Everything else the mod touches is **unchanged** in Fabric API 26.1 (verified
against the rename list):
- `ClientCommandRegistrationCallback` — not renamed.
- `ServerLifecycleEvents.SERVER_STARTED` — not renamed.
- `PlayerLookup.all(server)` — `PlayerLookup` only renamed its `world`→`level`
  member, which the mod doesn't use.
- `FabricClientCommandSource.getClient()` / `.sendFeedback(...)` — only
  `getWorld`→`getLevel` was renamed on that interface; mod doesn't use those.
- `ConventionalBiomeTags` (v2) — not renamed (and it's used via reflection).
- `net.fabricmc.api.*`, `net.fabricmc.loader.api.FabricLoader` — unchanged.
- ModMenu `ConfigScreenFactory` / `ModMenuApi` — TerraformersMC, not Fabric API;
  confirm against the 26.1 ModMenu but interface shape is stable.

---

## 6. Source migration reference (Yarn → official)

The Phase-A tool should produce most of this; use these tables to verify it and
to fix mixins by hand.

### 6.1 Class import map

| Yarn import | Official import |
|---|---|
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `net.minecraft.client.network.ClientPlayerEntity` | `net.minecraft.client.player.LocalPlayer` |
| `net.minecraft.client.world.ClientWorld` | `net.minecraft.client.multiplayer.ClientLevel` |
| `net.minecraft.client.option.GameOptions` | `net.minecraft.client.Options` |
| `net.minecraft.client.gui.screen.Screen` | `net.minecraft.client.gui.screens.Screen` |
| `net.minecraft.client.gui.screen.CreditsScreen` | `net.minecraft.client.gui.screens.WinScreen` |
| `net.minecraft.client.gui.hud.BossBarHud` | `net.minecraft.client.gui.components.BossHealthOverlay` |
| `net.minecraft.client.gui.hud.ClientBossBar` | `net.minecraft.client.gui.components.LerpingBossEvent` |
| `net.minecraft.client.sound.MusicTracker` | `net.minecraft.client.sounds.MusicManager` |
| `net.minecraft.client.sound.SoundManager` | `net.minecraft.client.sounds.SoundManager` |
| `net.minecraft.client.sound.SoundInstance` | `net.minecraft.client.resources.sounds.SoundInstance` |
| `net.minecraft.client.sound.SoundSystem` | `net.minecraft.client.sounds.SoundEngine` |
| `net.minecraft.entity.Entity` | `net.minecraft.world.entity.Entity` |
| `net.minecraft.entity.mob.HostileEntity` | `net.minecraft.world.entity.monster.Monster` |
| `net.minecraft.entity.passive.HorseEntity` | `net.minecraft.world.entity.animal.horse.Horse` |
| `net.minecraft.entity.passive.PigEntity` | `net.minecraft.world.entity.animal.Pig` |
| `net.minecraft.entity.passive.VillagerEntity` | `net.minecraft.world.entity.npc.Villager` |
| `net.minecraft.entity.vehicle.BoatEntity` | `net.minecraft.world.entity.vehicle.AbstractBoat` ⚠️ §6.4 |
| `net.minecraft.entity.vehicle.MinecartEntity` | `net.minecraft.world.entity.vehicle.Minecart` ⚠️ §6.4 (possible `.vehicle.minecart` subpackage) |
| `net.minecraft.registry.Registries` | `net.minecraft.core.registries.BuiltInRegistries` |
| `net.minecraft.registry.tag.TagKey` | `net.minecraft.tags.TagKey` |
| `net.minecraft.server.MinecraftServer` | `net.minecraft.server.MinecraftServer` (unchanged) |
| `net.minecraft.server.network.ServerPlayerEntity` | `net.minecraft.server.level.ServerPlayer` |
| `net.minecraft.sound.SoundCategory` | `net.minecraft.sounds.SoundSource` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.text.TranslatableTextContent` | `net.minecraft.network.chat.contents.TranslatableContents` |
| `net.minecraft.util.Formatting` | `net.minecraft.ChatFormatting` |
| `net.minecraft.util.math.BlockPos` | `net.minecraft.core.BlockPos` |
| `net.minecraft.util.math.Box` | `net.minecraft.world.phys.AABB` |
| `net.minecraft.util.math.Vec3d` | `net.minecraft.world.phys.Vec3` |
| `net.minecraft.world.World` | `net.minecraft.world.level.Level` |
| `net.minecraft.world.biome.Biome` | `net.minecraft.world.level.biome.Biome` |

### 6.2 Method / field renames (handled by the mapping tool, listed to verify)

| Yarn | Official |
|---|---|
| `MinecraftClient.getInstance()` | `Minecraft.getInstance()` |
| `mc.world` | `mc.level` |
| `mc.currentScreen` | `mc.screen` |
| `mc.send(Runnable)` | `mc.execute(Runnable)` |
| `mc.inGameHud` | `mc.gui` |
| `gui.getBossBarHud()` | `gui.getBossOverlay()` |
| `options.getSoundVolume(cat)` | `options.getSoundSourceVolume(SoundSource)` |
| `options.getSoundVolumeOption(cat).setValue(v)` | `options.getSoundSourceOptionInstance(SoundSource).set(v)` ⚠️ verify |
| `options.write()` | `options.save()` |
| `player.getBlockPos()` | `player.blockPosition()` |
| `player.getEntityPos()` | `player.position()` |
| `player.isSubmergedInWater()` | `player.isUnderWater()` |
| `player.fishHook` (field) | `player.fishing` (field) ⚠️ verify |
| `player.getRecentDamageSource()` | `player.getLastDamageSource()` ⚠️ verify |
| `damageSource.getSource()` | `damageSource.getDirectEntity()` ⚠️ (or `getEntity()` if attacker intended) |
| `world.isSkyVisible(pos)` | `level.canSeeSky(pos)` |
| `world.getRegistryKey()` | `level.dimension()` |
| `world.getTime()` | `level.getGameTime()` (unchanged in 26.1) |
| `world.getEntitiesByClass(C, Box, p)` | `level.getEntitiesOfClass(C, AABB, p)` |
| `world.getBiome(pos)` | `level.getBiome(pos)` (→ `Holder<Biome>`) |
| `World.OVERWORLD/NETHER/END` | `Level.OVERWORLD/NETHER/END` |
| `registryKey.getValue()` | `resourceKey.location()` |
| `tagKey.id()` | `tagKey.location()` |
| `Registries.BLOCK.getId(b)` | `BuiltInRegistries.BLOCK.getKey(b)` |
| `biome.getKey()` (Holder) | `biome.unwrapKey()` |
| `biome.streamTags()` | `biome.tags()` |
| `biome.value().getPrecipitation(pos, sea)` | `biome.value().getPrecipitationAt(pos, sea)` ⚠️ verify name/arity |
| `BlockPos.Mutable` | `BlockPos.MutableBlockPos` |
| `new Box(...)` | `new AABB(...)` |
| `new Vec3d(x,y,z)` | `new Vec3(x,y,z)` |
| `soundInstance.getId()` | `soundInstance.getLocation()` |
| `soundInstance.getCategory()` | `soundInstance.getSource()` |
| `soundInstance.getAttenuationType()` | `soundInstance.getAttenuation()` |
| `soundManager.isPlaying(si)` | `soundManager.isActive(si)` |
| `screen.getTitle().getContent()` | `screen.getTitle().getContents()` |
| `Text.of(s)` / `Text.literal(s)` | `Component.literal(s)` |

### 6.3 Things that do NOT change
`mc.player`, `mc.options`, `mc.setScreen`, `mc.getSoundManager`, `mc.isPaused`,
`getX/getY/getZ`, `getHealth/getMaxHealth`, `isSleeping`, `getVehicle`,
`isRaining/isThundering/getSeaLevel/getBlockState`, `Vec3.subtract/length`,
`MutableBlockPos.set`, `Biome.Precipitation.RAIN/SNOW/NONE`,
`SoundInstance.AttenuationType.NONE`, `TranslatableContents.getKey()`,
`ChatFormatting.getName()`.

### 6.4 ⚠️ Real 26.1 vanilla changes the mapping tools will NOT catch
(from the NeoForge primer — verify each against decompiled 26.1 sources)

1. **Chat message API split.** `Player#displayClientMessage(Component, boolean)`
   is split into `sendSystemMessage(Component)` (overlay=false) and
   `sendOverlayMessage(Component)` (overlay=true). **Every** `sendMessage(..,
   false)` call in this mod passes `false`, so they all become
   **`player.sendSystemMessage(Component)`** — NOT `displayClientMessage`.
   Call sites: `ReactiveMusic.doDebugLog`, `SoundManagerMixin.play`,
   `SongPicker.tickBlockCounterMap` (×2). Also `FabricClientCommandSource
   .sendFeedback(Component)` stays as-is (that's Fabric API, not Player).
2. **World clock rework.** `Level#getDayTime()` → **`getOverworldClockTime()`**,
   flagged *"not one-to-one"*. The mod uses `world.getTimeOfDay() % 24000` to
   derive DAY/NIGHT/SUNSET/SUNRISE. Map `getTimeOfDay()` →
   `getOverworldClockTime()` and **verify the returned value still represents
   ticks-within-the-day** (there's also `getDefaultClockTime()` /
   `clockManager`). This is the highest-risk behavioral change. Test in-game
   across a day cycle.
3. **Boat refactor.** Rideable boats are under `AbstractBoat` (rafts included);
   plain `Boat` excludes rafts. Original used the single `BoatEntity`.
   Recommended: `instanceof AbstractBoat` for "is riding a boat".
4. **Minecart package.** A `net.minecraft.world.entity.vehicle.minecart`
   subpackage now exists (e.g. `NewMinecartBehavior`). Confirm whether the
   rideable `Minecart` class lives in `…vehicle.Minecart` or
   `…vehicle.minecart.Minecart` in 26.1 and import accordingly.
5. **`getPrecipitationAt`** — confirm the method name and that the 2-arg
   `(BlockPos, int seaLevel)` overload exists in 26.1.
6. **`Player.fishing` field** and **`Player.getLastDamageSource()`** — least
   certain names; verify in the decompiled source.
7. **YACL 26.1 API** — builder methods take `Component`; confirm package is
   still `dev.isxander.yacl3.*` and `YACLPlatform.getConfigDir()` exists.

---

## 7. Mixin changes (manual — tools won't fully handle these)

`src/main/java/circuitlord/reactivemusic/mixin/`

### `MinecraftClientMixin.java`
- `@Mixin(MinecraftClient.class)` → `@Mixin(Minecraft.class)`
- `@Shadow public ClientPlayerEntity player;` → `@Shadow public LocalPlayer player;`
- `@Shadow public ClientWorld world;` → `@Shadow public ClientLevel level;`
  (field renamed `world`→`level`; rename the field and any uses)
- `@Inject(method = "tick", at = @At("RETURN"))` — `Minecraft#tick()` exists,
  no args; keep. ⚠️ confirm there's no `tick` vs `runTick` ambiguity.

### `MusicTrackerMixin.java`
- `@Mixin(MusicTracker.class)` → `@Mixin(MusicManager.class)`
- Keep the `tick` HEAD-cancel to suppress vanilla music. ⚠️ confirm
  `MusicManager#tick` signature in 26.1; if name-only matching is ambiguous,
  add the full descriptor.

### `BossBarHudAccessor.java`
- `@Mixin(BossBarHud.class)` → `@Mixin(BossHealthOverlay.class)`
- The map field is **`events`** in `BossHealthOverlay`, so name it explicitly:
  ```java
  @Accessor("events")
  Map<UUID, LerpingBossEvent> getBossBars();
  ```
- Call site (`SongPicker`): `((BossBarHudAccessor) mc.gui.getBossOverlay()).getBossBars()`.

### `SoundManagerMixin.java`
- `@Mixin(SoundManager.class)` unchanged (now `net.minecraft.client.sounds.SoundManager`).
- Descriptor rewrite:
  ```java
  @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
          at = @At("HEAD"), cancellable = true)
  private void play(SoundInstance soundInstance,
                    CallbackInfoReturnable<SoundEngine.PlayResult> cir) { ... }
  ```
- Body: `getId().getPath()` → `getLocation().getPath()`;
  `getAttenuationType()` → `getAttenuation()`;
  `mc.player.sendMessage(Text.of(...), false)` →
  `mc.player.sendSystemMessage(Component.literal(...))` (see §6.4 #1).
  ⚠️ confirm `SoundManager#play` returns `SoundEngine.PlayResult` in 26.1.

---

## 8. Cleanups while migrating
- `SongpackEntry.java`: remove all three imports (`TagEntry`, `TagKey`,
  `Biome`) — all unused.
- `SongPicker.java` line 21: drop the dead `//import …BiomeTags;` comment.
- `YAConfig.java`: unused demo code (ModMenuIntegration calls
  `ModConfig.createScreen`). Imports `dev.isxander.yacl3.config.GsonConfigInstance`
  which may not exist in YACL 26.1 — **recommend deleting `YAConfig.java`**.
- `ModConfig.savedHomePositions` is `HashMap<String, Vec3d>` → `Vec3` keeps
  `x/y/z` field names, so existing `ReactiveMusic.json5` configs stay
  Gson-compatible; smoke-test loading an old config.
- `VersionHelper.GetRidingEntity` uses `getVehicle()` — unchanged; keep.

---

## 9. Build / verify / test loop (permissive env + JDK 25)
1. Phase A: migrate mappings on 1.21.x, fix up (incl. mixins §7), confirm
   `./gradlew build` and `runClient` still work. Commit checkpoint.
2. Phase B: apply §4 build changes + fill §2 versions; apply the Fabric API
   migration map (§5) and the §6.4 vanilla fixes.
3. `./gradlew --refresh-dependencies build`; fix compile errors iteratively —
   the JDK-25 compiler is the source of truth for every ⚠️ item.
4. `./gradlew genSources` to read the real 26.1 official names when a mapping
   doesn't resolve.
5. `./gradlew runClient` and smoke test:
   - mod loads; no mixin-apply errors in the log;
   - vanilla music suppressed and Reactive Music plays;
   - `/reactivemusic` opens the YACL screen (also via ModMenu);
   - DAY/NIGHT/SUNSET/SUNRISE still correct across a full day cycle (validates
     the `getOverworldClockTime` change, §6.4 #2);
   - biome / dimension / underground / boss / village / mob / boat / minecart /
     horse / pig / fishing / underwater / weather events still trigger;
   - `/reactivemusic blacklistDimension`, `toggleSoundEventLogging`,
     `toggleLogging`, `logBlockCounter` work;
   - jukebox / music-disc ducking works (SoundManagerMixin path);
   - chat debug messages appear (validates `sendSystemMessage`, §6.4 #1);
   - load an existing `ReactiveMusic.json5` for config compatibility.
6. Set `fabric.mod.json` `depends.minecraft` to cover the 26.1.x patches you
   want to support.

---

## 10. Out of scope / leave alone
- `org.rm_yaml.snakeyaml.*` and `rm_javazoom.jl.*` — vendored pure-Java libs,
  no Minecraft API; do not touch.
- `gitPatches/BUILD-1.20.1.patch`, `BUILD-1.21.1.patch` — legacy back-port
  patches. 26.1 cannot share a build config with Yarn versions; if you want to
  keep shipping older MC versions, branch/patch them separately.
