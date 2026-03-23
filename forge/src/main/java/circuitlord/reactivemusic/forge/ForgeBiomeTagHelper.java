package circuitlord.reactivemusic.forge;

import circuitlord.reactivemusic.platform.BiomeTagHelper;
import net.minecraftforge.common.Tags;

import java.lang.reflect.Field;

public class ForgeBiomeTagHelper implements BiomeTagHelper {
    @Override
    public Field[] getBiomeTagFields() {
        return Tags.Biomes.class.getDeclaredFields();
    }
}
