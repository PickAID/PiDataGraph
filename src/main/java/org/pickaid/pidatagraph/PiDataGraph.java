package org.pickaid.pidatagraph;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(PiDataGraph.MOD_ID)
public class PiDataGraph {
    public static final String MOD_ID = "pidatagraph";

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
