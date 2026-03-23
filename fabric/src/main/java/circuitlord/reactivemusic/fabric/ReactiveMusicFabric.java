package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ReactiveMusicFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ReactiveMusic.init();
        ReactiveMusic.initClient();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("reactivemusic")
                .executes(context -> {
                    MinecraftClient mc = context.getSource().getClient();
                    Screen screen = ModConfig.createScreen(mc.currentScreen);
                    mc.send(() -> mc.setScreen(screen));
                    return 1;
                })

                .then(ClientCommandManager.literal("logBlockCounter")
                        .executes(context -> {
                            SongPicker.queuedToPrintBlockCounter = true;
                            return 1;
                        })
                )

                .then(ClientCommandManager.literal("toggleSoundEventLogging")
                        .executes(context -> {
                            ReactiveMusic.printSoundEvents = !ReactiveMusic.printSoundEvents;
                            return 1;
                        })
                )

                .then(ClientCommandManager.literal("blacklistDimension")
                        .executes(context -> {
                            String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

                            if (ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                context.getSource().sendFeedback(Text.literal("[ReactiveMusic]: " + key + " was already in blacklist."));
                                return 1;
                            }

                            context.getSource().sendFeedback(Text.literal("[ReactiveMusic]: Added " + key + " to blacklist."));
                            ReactiveMusic.config.blacklistedDimensions.add(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(ClientCommandManager.literal("unblacklistDimension")
                        .executes(context -> {
                            String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

                            if (!ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                context.getSource().sendFeedback(Text.literal("[ReactiveMusic]: " + key + " was not in blacklist."));
                                return 1;
                            }

                            context.getSource().sendFeedback(Text.literal("[ReactiveMusic]: Removed " + key + " from blacklist."));
                            ReactiveMusic.config.blacklistedDimensions.remove(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(ClientCommandManager.literal("toggleLogging")
                        .executes(context -> {
                            ReactiveMusic.chatLoggingEnabled = !ReactiveMusic.chatLoggingEnabled;
                            context.getSource().sendFeedback(Text.literal("[ReactiveMusic]: Logging enabled: " + ReactiveMusic.chatLoggingEnabled));
                            return 1;
                        })
                )
            )
        );
    }
}
