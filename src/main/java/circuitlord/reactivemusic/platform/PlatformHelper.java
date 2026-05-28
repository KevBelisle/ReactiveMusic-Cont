package circuitlord.reactivemusic.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface PlatformHelper {
    PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class).findFirst()
        .orElseThrow(() -> new RuntimeException("No PlatformHelper implementation found! Is a loader module missing?"));

    boolean isModLoaded(String modId);
    Path getGameDir();
    Path getConfigDir();
    boolean isDedicatedServer();
}
