package circuitlord.reactivemusic.forge;

import circuitlord.reactivemusic.platform.BiomeTagHelper;

import java.util.Map;
import java.util.Set;

public class ForgeBiomeTagHelper implements BiomeTagHelper {

    private static final Map<String, String> FORGE_REMAP = Map.ofEntries(
        Map.entry("is_aquatic", "is_water"),
        Map.entry("is_sparse_vegetation", "is_sparse"),
        Map.entry("is_dense_vegetation", "is_dense"),
        Map.entry("is_mountain/peak", "is_peak"),
        Map.entry("is_mountain/slope", "is_slope"),
        Map.entry("is_tree/coniferous", "is_coniferous"),
        Map.entry("is_coniferous_tree", "is_coniferous")
    );

    // Tags that don't exist on Forge at all
    private static final Set<String> FORGE_MISSING = Set.of(
        "is_flower_forest",
        "is_old_growth",
        "is_aquatic_icy",
        "is_stony_shores",
        "is_windswept",
        "no_default_monsters"
    );

    @Override
    public String getTagNamespace() {
        return "forge";
    }

    @Override
    public String remapTagPath(String path) {
        if (FORGE_MISSING.contains(path)) return null;
        return FORGE_REMAP.getOrDefault(path, path);
    }
}
