package circuitlord.reactivemusic.platform;

import java.lang.reflect.Field;
import java.util.ServiceLoader;

public interface BiomeTagHelper {
    BiomeTagHelper INSTANCE = ServiceLoader.load(BiomeTagHelper.class).findFirst()
        .orElseThrow(() -> new RuntimeException("No BiomeTagHelper implementation found! Is a loader module missing?"));

    /**
     * Returns the declared fields of the platform's conventional biome tag class.
     * On Fabric: ConventionalBiomeTags.class.getDeclaredFields()
     * On Forge: Tags.Biomes.class.getDeclaredFields()
     * On NeoForge: Tags.Biomes.class.getDeclaredFields()
     */
    Field[] getBiomeTagFields();
}
