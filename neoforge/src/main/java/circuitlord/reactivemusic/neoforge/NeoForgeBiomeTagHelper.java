package circuitlord.reactivemusic.neoforge;

import circuitlord.reactivemusic.platform.BiomeTagHelper;
import net.neoforged.neoforge.common.Tags;

import java.lang.reflect.Field;

public class NeoForgeBiomeTagHelper implements BiomeTagHelper {
    @Override
    public Field[] getBiomeTagFields() {
        return Tags.Biomes.class.getDeclaredFields();
    }
}
