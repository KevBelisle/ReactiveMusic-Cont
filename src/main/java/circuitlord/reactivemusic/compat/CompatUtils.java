package circuitlord.reactivemusic.compat;

import circuitlord.reactivemusic.platform.PlatformHelper;

public class CompatUtils {
    public static boolean isModLoaded(String id) {
        return PlatformHelper.INSTANCE.isModLoaded(id);
    }

    public static boolean isClothConfigLoaded() {
        return isModLoaded("cloth-config2");
    }

    public static boolean isYACLLoaded() {
        return isModLoaded("yet_another_config_lib_v3");
    }
}
