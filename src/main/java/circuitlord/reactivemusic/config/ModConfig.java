package circuitlord.reactivemusic.config;


import circuitlord.reactivemusic.RMSongpackLoader;
import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongpackZip;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

import static dev.isxander.yacl3.platform.YACLPlatform.getConfigDir;


public class ModConfig {

    public static final ValueFormatter<ChatFormatting> FORMATTING_FORMATTER = formatting -> Component.literal(StringUtils.capitalize(formatting.getName().replaceAll("_", " ")));


    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static void saveConfig() {
        GSON.save();
    }

    public static final ConfigClassHandler<ModConfig> GSON = ConfigClassHandler.createBuilder(ModConfig.class)
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(getConfigDir().resolve("ReactiveMusic.json5"))
                    .setJson5(true)
                    .build())
            .build();



    @SerialEntry
    public MusicDelayLength musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;

    @SerialEntry
    public MusicSwitchSpeed musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;

    @SerialEntry
    public boolean debugModeEnabled = false;

    @SerialEntry
    public String loadedUserSongpack = "";

    @SerialEntry
    public List<String> blacklistedDimensions = new ArrayList<>();

    @SerialEntry
    public HashMap<String, Vec3> savedHomePositions = new HashMap<>();

    @SerialEntry
    public List<String> soundsMuteMusic = new ArrayList<>();

    @SerialEntry
    public boolean hasForcedInitialVolume = false;


    public static Screen createScreen(Screen parent) {

        RMSongpackLoader.fetchAvailableSongpacks();

        return YetAnotherConfigLib.create(ModConfig.GSON, ((defaults, config, builder) -> {



            var songpacksBuilder = ConfigCategory.createBuilder();
            songpacksBuilder.name(Component.literal("Songpacks"));


            for (var songpackZip : RMSongpackLoader.availableSongpacks) {

                boolean isLoaded = false;

                if (ReactiveMusic.currentSongpack != null) {

                    isLoaded = Objects.equals(ReactiveMusic.currentSongpack.config.name, songpackZip.config.name);
                }

                if (songpackZip.blockLoading) {
                    songpacksBuilder.option(ButtonOption.createBuilder()
                            .name(Component.literal("FAILED LOADING: " + songpackZip.config.name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Component.literal("Failed to load songpack:\n\n" + songpackZip.errorString))
                                            .build()
                            )

                            .available(false)
                            .text(Component.literal(""))
                            .action((yaclScreen, buttonOption) -> {

                            })
                            .build());

                }
                else {

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
                            .name(Component.literal(name))
                            .description(
                                    OptionDescription.createBuilder()
                                            .text(Component.literal(description))
                                            .build()
                            )

                            .available(!isLoaded)

                            .text(Component.literal(isLoaded ? "Loaded" : "Load"))


                            .action((yaclScreen, buttonOption) -> {
                                setActiveSongpack(songpackZip);

                                Minecraft.getInstance().setScreen(ModConfig.createScreen(parent));
                            })



                            .build());
                }

            }


            builder.category(songpacksBuilder.build());


            builder
                    .title(Component.literal("Reactive Music"))


                    .category(ConfigCategory.createBuilder()
                            .name(Component.literal("General"))

                            .option(Option.<MusicDelayLength>createBuilder()
                                    .name(Component.literal("Music Delay Length"))
                                    .binding(defaults.musicDelayLength2, () -> config.musicDelayLength2, newVal -> config.musicDelayLength2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicDelayLength.class))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Component.literal("Defines how much silence there should be between songs playing.\n\n" +
                                                            "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                    .build()
                                    )

                                    .build())

                            .option(Option.<MusicSwitchSpeed>createBuilder()
                                    .name(Component.literal("Music Switch Speed"))
                                    .binding(defaults.musicSwitchSpeed2, () -> config.musicSwitchSpeed2, newVal -> config.musicSwitchSpeed2 = newVal )
                                    .controller(opt -> EnumControllerBuilder.create(opt).enumClass(MusicSwitchSpeed.class))
                                    .description(
                                            OptionDescription.createBuilder()
                                                    .text(Component.literal("Defines how long before a song fades out when it's event becomes invalid.\n\n" +
                                                            "SONGPACK_DEFAULT will use values recommended by the songpack creator."))
                                                    .build()
                                    )

                                    .build())


                            .build())


                    .category(ConfigCategory.createBuilder()
                            .name(Component.literal("Debug"))
                            .tooltip(Component.literal("Any debug tools useful for songpack creators or developers"))


                            .option(Option.<Boolean>createBuilder()
                                    .name(Component.literal("Debug Mode Enabled"))
                                    .description(OptionDescription.createBuilder()
                                            .text(Component.literal("Enables songpack developer functionality.\n" +
                                                    "- Always immediately switch between songs when events change.\n" +
                                                    "- Always display all songpack loading errors in the menu.\n"))
                                            .build())
                                    .binding(defaults.debugModeEnabled, () -> config.debugModeEnabled, newVal -> config.debugModeEnabled = newVal )
                                    .controller(TickBoxControllerBuilder::create)
                                    .build())




                            .build())




            .build();


            return builder;

        })).generateScreen(parent);
    }


    public static void setActiveSongpack(SongpackZip songpack) {

        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        }
        else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();

        ReactiveMusic.setActiveSongpack(songpack);

    }


    public static <E extends Enum<E>> Function<Option<E>, ControllerBuilder<E>> getEnumDropdownControllerFactory(ValueFormatter<E> formatter) {
        return opt -> EnumDropdownControllerBuilder.create(opt).formatValue(formatter);
    }

}
