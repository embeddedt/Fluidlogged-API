package git.jbredwards.fluidlogged_api.asm.plugins.forge;

import git.jbredwards.fluidlogged_api.asm.plugins.IASMPlugin;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;

/**
 * changes some of this class's util functions to be FluidState sensitive
 * @author jbred
 *
 */
public final class FluidUtilPlugin implements IASMPlugin
{
    @Override
    public int isMethodValid(@Nonnull MethodNode method, boolean obfuscated) {
        //getFluidHandler, line 543
        if(checkMethod(method, "getFluidHandler", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lnet/minecraftforge/fluids/capability/IFluidHandler;"))
            return 1;

        //tryPickUpFluid, line 565
        else if(checkMethod(method, "tryPickUpFluid", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lnet/minecraftforge/fluids/FluidActionResult;"))
            return 2;

        //tryPlaceFluid, line 640
        else if(checkMethod(method, "tryPlaceFluid", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/FluidStack;)Z")) {
            //setMaxLocals(method, 6);
            return 3;
        }

        return 0;
    }

    @Override
    public boolean transform(@Nonnull InsnList instructions, @Nonnull MethodNode method, @Nonnull AbstractInsnNode insn, boolean obfuscated, int index) {
        //getFluidHandler, line 543
        if(index == 1 && insn.getOpcode() == ACONST_NULL) {
            final InsnList list = new InsnList();
            //params
            list.add(new VarInsnNode(ALOAD, 0));
            list.add(new VarInsnNode(ALOAD, 1));
            //add new code
            list.add(genMethodNode("getFluidHandler", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraftforge/fluids/capability/IFluidHandler;"));
            instructions.insert(insn, list);
            instructions.remove(insn);
            return true;
        }
        //tryPickUpFluid, line 565
        else if(index == 2 && checkMethod(insn, obfuscated ? "func_180495_p" : "getBlockState", null)) {
            instructions.insert(insn, genMethodNode("git/jbredwards/fluidlogged_api/common/util/FluidloggedUtils", "getFluidOrReal", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"));
            instructions.remove(insn);
            return true;
        }
        //tryPlaceFluid, line 640
        else if(index == 3) {
            if(checkMethod(insn, obfuscated ? "func_175623_d" : "isAirBlock", null)) {
                final InsnList list = new InsnList();
                //Fluid local var
                list.add(new VarInsnNode(ALOAD, 5));
                //IBlockState local var
                list.add(new VarInsnNode(ALOAD, 6));
                //add new code
                list.add(genMethodNode("tryPlaceFluid", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraftforge/fluids/Fluid;Lnet/minecraft/block/state/IBlockState;)Z"));
                instructions.insertBefore(insn, list);
                instructions.remove(insn);
                return true;
            }
        }

        return false;
    }
}
