package circuitlord.reactivemusic.forge;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.config.ModConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static net.minecraft.server.command.CommandManager.literal;

@Mod("reactivemusic")
public class ReactiveMusicForge {

    public ReactiveMusicForge() {
        ReactiveMusic.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
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
