package circuitlord.reactivemusic.mixin;

import circuitlord.reactivemusic.ReactiveMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void play(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {

        String path = soundInstance.getIdentifier().getPath();

        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && ReactiveMusic.printSoundEvents) {
            mc.player.sendSystemMessage(Component.literal("[ReactiveMusic]: Sound: " + path + " Attenuation: " + soundInstance.getAttenuation()));
        }

        // Suppress vanilla music. Cancelling MusicManager.tick() blocks the normal
        // situational-music path, but some music -- notably in the End dimension --
        // still reaches the SoundManager through other code paths and plays over our
        // custom music. Block any MUSIC-category sound here as a backstop.
        // Our own tracks play through a separate audio device (not the SoundManager)
        // and music discs use the RECORDS category, so neither is affected.
        if (soundInstance.getSource() == SoundSource.MUSIC) {
            cir.setReturnValue(null);
            return;
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
