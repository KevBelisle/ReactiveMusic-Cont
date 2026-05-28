package circuitlord.reactivemusic.config;

import circuitlord.reactivemusic.RMSongpackLoader;
import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongpackZip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
//? if >=1.20 {
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
//?}
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ModConfig {

    public static final ConfigStore GSON = new ConfigStore();

    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static void saveConfig() {
        GSON.save();
    }

    public MusicDelayLength musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;

    public MusicSwitchSpeed musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;

    public boolean debugModeEnabled = false;

    public String loadedUserSongpack = "";

    public List<String> blacklistedDimensions = new ArrayList<>();

    public HashMap<String, Vec3d> savedHomePositions = new HashMap<>();

    public List<String> soundsMuteMusic = new ArrayList<>();

    public boolean hasForcedInitialVolume = false;

    public static Screen createScreen(Screen parent) {
        //? if >=1.20 {
        RMSongpackLoader.fetchAvailableSongpacks();

        ModConfig defaults = new ModConfig();
        ModConfig config = getConfig();

        var songpacksBuilder = ConfigCategory.createBuilder();
        songpacksBuilder.name(Text.literal("Songpacks"));

        for (var songpackZip : RMSongpackLoader.availableSongpacks) {
            boolean isLoaded = false;

            if (ReactiveMusic.currentSongpack != null) {
                isLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, songpackZip.config.name);
            }

            if (songpackZip.blockLoading) {
                songpacksBuilder.option(ButtonOption.createBuilder()
                        .name(Text.literal("FAILED LOADING: " + songpackZip.config.name))
                        .description(
                                OptionDescription.createBuilder()
                                        .text(Text.literal("Failed to load songpack:\n\n" + songpackZip.errorString))
                                        .build()
                        )
                        .available(false)
                        .text(Text.literal(""))
                        .action((yaclScreen, buttonOption) -> {
                        })
                        .build());
            } else {
                String name = songpackZip.config.name;
                String description = songpackZip.config.description + "\n\nCredits:\n" + songpackZip.config.credits;

                boolean allowedToShowErrors =
                        getConfig().debugModeEnabled ||
                                songpackZip.isv05OldSongpack ||
                                (songpackZip.path != null && !songpackZip.path.toString().endsWith(".zip"));

                if (allowedToShowErrors && !songpackZip.errorString.isEmpty()) {
                    name = "WARNING: " + name;
                    description = "Encountered warnings while loading:\n\n" + songpackZip.errorString + "----------\n\n" + description;
                }

                songpacksBuilder.option(ButtonOption.createBuilder()
                        .name(Text.literal(name))
                        .description(
                                OptionDescription.createBuilder()
                                        .text(Text.literal(description))
                                        .build()
                        )
                        .available(!isLoaded)
                        .text(Text.literal(isLoaded ? "Loaded" : "Load"))
                        .action((yaclScreen, buttonOption) -> {
                            setActiveSongpack(songpackZip);
                            MinecraftClient.getInstance().setScreen(ModConfig.createScreen(parent));
                        })
                        .build());
            }
        }

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Reactive Music"))
                .save(ModConfig::saveConfig)
                .category(songpacksBuilder.build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("General"))
                        .option(Option.<MusicDelayLength>createBuilder()
                                .name(Text.literal("Music Delay Length"))
                                .binding(defaults.musicDelayLength2, () -> config.musicDelayLength2, newVal -> config.musicDelayLength2 = newVal)
                                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicDelayLength.class))
                                .description(
                                        OptionDescription.createBuilder()
                                                .text(Text.literal("Defines how much silence there should be between songs playing.\n\n" +
                                                        "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                .build()
                                )
                                .build())
                        .option(Option.<MusicSwitchSpeed>createBuilder()
                                .name(Text.literal("Music Switch Speed"))
                                .binding(defaults.musicSwitchSpeed2, () -> config.musicSwitchSpeed2, newVal -> config.musicSwitchSpeed2 = newVal)
                                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicSwitchSpeed.class))
                                .description(
                                        OptionDescription.createBuilder()
                                                .text(Text.literal("Defines how long before a song fades out when it's event becomes invalid.\n\n" +
                                                        "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                .build()
                                )
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Debug"))
                        .tooltip(Text.literal("Any debug tools useful for songpack creators or developers"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Debug Mode Enabled"))
                                .description(OptionDescription.createBuilder()
                                        .text(Text.literal("Enables songpack developer functionality.\n" +
                                                "- Always immediately switch between songs when events change.\n" +
                                                "- Always display all songpack loading errors in the menu.\n"))
                                        .build())
                                .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
        //?} else {
        /*return parent;
        *///?}
    }

    public static void setActiveSongpack(SongpackZip songpack) {
        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        } else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();
        ReactiveMusic.setActiveSongpack(songpack);
    }

    public static class ConfigStore {
        private static final Path PATH = Paths.get("config", "ReactiveMusic.json5");
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        private ModConfig instance = new ModConfig();

        public ModConfig instance() {
            return instance;
        }

        public void load() {
            if (!Files.exists(PATH)) {
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(PATH)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                ModConfig loaded = gson.fromJson(jsonReader, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    ensureCollections();
                }
            } catch (Exception e) {
                System.err.println("[ReactiveMusic] Failed to load config, using defaults: " + e.getMessage());
                instance = new ModConfig();
            }
        }

        public void save() {
            ensureCollections();
            try {
                Path parent = PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (Writer writer = Files.newBufferedWriter(PATH)) {
                    gson.toJson(instance, writer);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save Reactive Music config", e);
            }
        }

        private void ensureCollections() {
            if (instance.blacklistedDimensions == null) instance.blacklistedDimensions = new ArrayList<>();
            if (instance.savedHomePositions == null) instance.savedHomePositions = new HashMap<>();
            if (instance.soundsMuteMusic == null) instance.soundsMuteMusic = new ArrayList<>();
            if (instance.loadedUserSongpack == null) instance.loadedUserSongpack = "";
            if (instance.musicDelayLength2 == null) instance.musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;
            if (instance.musicSwitchSpeed2 == null) instance.musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;
        }
    }
}
