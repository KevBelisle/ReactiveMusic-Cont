package circuitlord.reactivemusic.platform;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ServiceLoader;

public interface BiomeTagHelper {
    BiomeTagHelper INSTANCE = ServiceLoader.load(BiomeTagHelper.class).findFirst()
        .orElseThrow(() -> new RuntimeException("No BiomeTagHelper implementation found! Is a loader module missing?"));

    String getTagNamespace();

    default String remapTagPath(String path) {
        return path;
    }

    default TagKey<Biome> createBiomeTagKey(String canonicalPath) {
        String remapped = remapTagPath(canonicalPath);
        if (remapped == null) return null;
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(getTagNamespace(), remapped));
    }
}
