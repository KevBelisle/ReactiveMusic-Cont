package circuitlord.reactivemusic.fabric;

import circuitlord.reactivemusic.platform.BiomeTagHelper;
//? if >=1.21 {
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
//?} else {
/*import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
*///?}

import java.lang.reflect.Field;

public class FabricBiomeTagHelper implements BiomeTagHelper {
    @Override
    public Field[] getBiomeTagFields() {
        return ConventionalBiomeTags.class.getDeclaredFields();
    }
}
