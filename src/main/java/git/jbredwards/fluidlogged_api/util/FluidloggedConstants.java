package git.jbredwards.fluidlogged_api.util;

import git.jbredwards.fluidlogged_api.common.block.BlockFluidloggedTE;
import net.minecraft.block.material.Material;
import net.minecraftforge.fluids.Fluid;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbred
 *
 */
public enum FluidloggedConstants
{
    ;

    //mod id constants
    @Nonnull public static final String MODID = "fluidlogged_api";
    @Nonnull public static final String NAME = "Fluidlogged API";
    @Nonnull public static final String VERSION = "1.6";

    //used to get the fluidlogged te's from the fluid
    @Nonnull public static Map<Fluid, BlockFluidloggedTE> FLUIDLOGGED_TE_LOOKUP = new HashMap<>();

    //replaces the all rail block's materials
    @SuppressWarnings("unused")
    @Nonnull public static Material RAIL = new Material(Material.AIR.getMaterialMapColor()) {
        @Nonnull public Material setNoPushMobility() { return super.setNoPushMobility(); }
        public boolean isSolid()     { return false; }
        public boolean blocksLight() { return false; }
    }.setNoPushMobility();
}
