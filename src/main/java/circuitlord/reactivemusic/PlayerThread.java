package circuitlord.reactivemusic;


import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundSource;
import rm_javazoom.jl.player.AudioDevice;
import rm_javazoom.jl.player.JavaSoundAudioDevice;
import rm_javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.IOException;
import java.io.InputStream;

public class PlayerThread extends Thread {

	public static final float MIN_POSSIBLE_GAIN = -80F;
	public static final float MIN_GAIN = -50F;
	public static final float MAX_GAIN = 0F;

	static {
	}

	public volatile static float gainPercentage = 1.0f;
	public volatile static float musicDiscDuckPercentage = 1.0f;

	public static final float QUIET_VOLUME_PERCENTAGE = 0.7f;
	public static final float QUIET_VOLUME_LERP_RATE = 0.02f;
	public static float quietPercentage = 1.0f;

	public volatile static float realGain = 0;

	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;

	public volatile MusicPackResource currentSongResource = null;

	AdvancedPlayer player;

	private volatile boolean queued = false;

	private volatile boolean kill = false;
	private volatile boolean playing = false;


	boolean notQueuedOrPlaying() {
		return !(queued || isPlaying());
	}

	boolean isPlaying() {
		return playing && !player.getComplete();
	}

	public PlayerThread() {
		setDaemon(true);
		setName("ReactiveMusic Player Thread");
		start();
	}

	@Override
	public void run() {
		try {
			while(!kill) {

				if(queued && currentSong != null) {

					currentSongResource = RMSongpackLoader.getInputStream(ReactiveMusic.currentSongpack.path, "music/" + currentSong + ".mp3", ReactiveMusic.currentSongpack.embedded);
					if(currentSongResource == null || currentSongResource.inputStream == null)
						continue;

					player = new AdvancedPlayer(currentSongResource.inputStream);
					queued = false;

				}


				if(player != null && player.getAudioDevice() != null) {

					// go to full volume
					setGainPercentage(1.0f);
					processRealGain();

					ReactiveMusic.LOGGER.info("Playing " + currentSong);
					playing = true;
					player.play();

				}

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}



	public void resetPlayer() {
		playing = false;

		if(player != null)
			player.queuedToStop = true;

		queued = false;
		currentSong = null;

		if (currentSongResource != null && currentSongResource.fileSystem != null) {
            try {
				currentSongResource.close();
            } catch (Exception e) {
                ReactiveMusic.LOGGER.error("Failed to close file system/input stream " + e.getMessage());
            }
        }

		currentSongResource = null;
	}

	public void play(String song) {
		resetPlayer();

		currentSong = song;
		queued = true;
	}

	public void setGainPercentage(float newGain) {
		gainPercentage = Math.min(1.0f, Math.max(0.0f, newGain));
	}

	public void setMusicDiscDuckPercentage(float newGain) {
		musicDiscDuckPercentage = newGain;
	}

	public void processRealGain() {

		var client = Minecraft.getInstance();

		Options options = Minecraft.getInstance().options;

		boolean musicOptionsOpen = false;

		// Try to find the music options menu
		TranslatableContents ScreenTitleContent = null;
		if (client.screen != null && client.screen.getTitle() != null && client.screen.getTitle().getContents() != null
			&& client.screen.getTitle().getContents() instanceof TranslatableContents) {

			ScreenTitleContent = (TranslatableContents) client.screen.getTitle().getContents();

			if (ScreenTitleContent != null) {
				musicOptionsOpen = ScreenTitleContent.getKey().equals("options.sounds.title");
			}
		}


		boolean doQuietMusic =  client.isPaused()
				&& client.level != null
				&& !musicOptionsOpen;


		float targetQuietMusicPercentage = doQuietMusic ? QUIET_VOLUME_PERCENTAGE : 1.0f;
        quietPercentage = MyMath.lerpConstant(quietPercentage, targetQuietMusicPercentage, QUIET_VOLUME_LERP_RATE);


		float minecraftGain = options.getSoundSourceVolume(SoundSource.MUSIC) * options.getSoundSourceVolume(SoundSource.MASTER);

		// my jank way of changing the volume curve to be less drastic
		float minecraftDistFromMax = 1.0f - minecraftGain;
		float minecraftGainAddScalar = (minecraftDistFromMax * 1.0f) * minecraftGain;
		// cap to 1.0
		minecraftGain = Math.min(minecraftGain + minecraftGainAddScalar, 1.0f);


		float newRealGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * minecraftGain * gainPercentage * quietPercentage * musicDiscDuckPercentage;

		// Force to basically off if the user sets their volume off
		if (minecraftGain <= 0) {
			newRealGain = MIN_POSSIBLE_GAIN;
		}

		realGain = newRealGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if(device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(newRealGain);
				} catch(IllegalArgumentException e) {
					ReactiveMusic.LOGGER.error(e.toString());
				}
			}
		}

	}


	public void forceKill() {
		try {
			resetPlayer();
			interrupt();

			finalize();
			kill = true;
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
