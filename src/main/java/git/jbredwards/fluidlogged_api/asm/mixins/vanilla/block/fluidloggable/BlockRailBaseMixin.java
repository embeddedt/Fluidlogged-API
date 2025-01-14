package git.jbredwards.fluidlogged_api.asm.mixins.vanilla.block.fluidloggable;

import git.jbredwards.fluidlogged_api.common.block.IFluidloggable;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

/**
 * makes rails fluidloggable by default
 * @author jbred
 *
 */
@Mixin(BlockRailBase.class)
public abstract class BlockRailBaseMixin implements IFluidloggable
{
    @Nonnull
    private static final Material RAIL = new Material(MapColor.AIR) {
        @Nonnull public Material setNoPushMobility() { return super.setNoPushMobility(); }
        @Nonnull public MapColor getMaterialMapColor() { return Material.CIRCUITS.getMaterialMapColor(); }
        public boolean isSolid()     { return false; }
        public boolean blocksLight() { return false; }
    }.setNoPushMobility();

    @Nonnull
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/block/material/Material;CIRCUITS:Lnet/minecraft/block/material/Material;"))
    private static Material material() { return RAIL; }
}
