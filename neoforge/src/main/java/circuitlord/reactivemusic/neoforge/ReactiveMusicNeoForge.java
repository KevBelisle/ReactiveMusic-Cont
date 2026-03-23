package circuitlord.reactivemusic.neoforge;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

import static net.minecraft.server.command.CommandManager.literal;

@Mod("reactivemusic")
public class ReactiveMusicNeoForge {

    public ReactiveMusicNeoForge(IEventBus modEventBus) {
        ReactiveMusic.init();
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ReactiveMusic::initClient);
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("reactivemusic")
                .executes(context -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    Screen screen = ModConfig.createScreen(mc.currentScreen);
                    mc.send(() -> mc.setScreen(screen));
                    return 1;
                })

                .then(literal("logBlockCounter")
                        .executes(context -> {
                            SongPicker.queuedToPrintBlockCounter = true;
                            return 1;
                        })
                )

                .then(literal("toggleSoundEventLogging")
                        .executes(context -> {
                            ReactiveMusic.printSoundEvents = !ReactiveMusic.printSoundEvents;
                            return 1;
                        })
                )

                .then(literal("blacklistDimension")
                        .executes(context -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            String key = mc.world.getRegistryKey().getValue().toString();

                            if (ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                mc.player.sendMessage(Text.literal("[ReactiveMusic]: " + key + " was already in blacklist."), false);
                                return 1;
                            }

                            mc.player.sendMessage(Text.literal("[ReactiveMusic]: Added " + key + " to blacklist."), false);
                            ReactiveMusic.config.blacklistedDimensions.add(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(literal("unblacklistDimension")
                        .executes(context -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            String key = mc.world.getRegistryKey().getValue().toString();

                            if (!ReactiveMusic.config.blacklistedDimensions.contains(key)) {
                                mc.player.sendMessage(Text.literal("[ReactiveMusic]: " + key + " was not in blacklist."), false);
                                return 1;
                            }

                            mc.player.sendMessage(Text.literal("[ReactiveMusic]: Removed " + key + " from blacklist."), false);
                            ReactiveMusic.config.blacklistedDimensions.remove(key);
                            ModConfig.saveConfig();
                            return 1;
                        })
                )

                .then(literal("toggleLogging")
                        .executes(context -> {
                            ReactiveMusic.chatLoggingEnabled = !ReactiveMusic.chatLoggingEnabled;
                            MinecraftClient.getInstance().player.sendMessage(
                                    Text.literal("[ReactiveMusic]: Logging enabled: " + ReactiveMusic.chatLoggingEnabled), false);
                            return 1;
                        })
                )
        );
    }
}
