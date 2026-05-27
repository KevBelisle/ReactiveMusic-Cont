package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {

        String path = soundInstance.getLocation().getPath();


        Minecraft mc = Minecraft.getInstance();


        if (mc.player != null && ReactiveMusic.printSoundEvents) {
            mc.player.sendSystemMessage(Component.literal("[ReactiveMusic]: Sound: " + path + " Attenuation: " + soundInstance.getAttenuation()));
        }

        if (path.contains("music_disc")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
        }

        // cobblemon resource pack uses:
        //"battle.pvn.default"
        //"battle.pvp.default"
        //"battle.pvw.default"
        else if (path.contains("battle.pv")) {
            ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);

            ReactiveMusic.LOGGER.info("Detected cobblemon battle event, adding to list!");
        }


        for (String muteSound : ReactiveMusic.config.soundsMuteMusic) {
            if (path.contains(muteSound)) {
                ReactiveMusic.trackedSoundsMuteMusic.add(soundInstance);
                break;
            }
        }


    }

}
