package git.jbredwards.fluidlogged_api.asm.plugins.forge;

import git.jbredwards.fluidlogged_api.asm.plugins.IASMPlugin;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;

/**
 *
 * @author jbred
 *
 */
public final class BlockFluidBasePlugin implements IASMPlugin
{
    @Override
    public int isMethodValid(@Nonnull MethodNode method, boolean obfuscated) {
        //constructor
        if(checkMethod(method, "<init>", "(Lnet/minecraftforge/fluids/Fluid;Lnet/minecraft/block/material/Material;Lnet/minecraft/block/material/MapColor;)V"))
            return 1;
        //canDisplace
        if(checkMethod(method, "canDisplace", null))
            return 2;
        //shouldSideBeRendered
        if(checkMethod(method, obfuscated ? "func_176225_a" : "shouldSideBeRendered", null))
            return 2;


        //default
        return 0;
    }

    @Override
    public boolean transform(@Nonnull InsnList instructions, @Nonnull MethodNode method, @Nonnull AbstractInsnNode insn, boolean obfuscated, int index) {
        //constructor, line 181
        if(index == 1 && checkField(insn, "defaultDisplacements", "Ljava/util/Map;")) {
            instructions.insert(insn, genMethodNode("defaultDisplacements", "(Ljava/util/Map;)Ljava/util/Map;"));
            return true;
        }
        //canDisplace, line 276
        else if(index == 2 && checkMethod(insn, obfuscated ? "func_180495_p" : "getBlockState", null)) {
            instructions.insert(genMethodNode("git/jbredwards/fluidlogged_api/common/util/FluidloggedUtils", "getFluidOrReal", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"));
            instructions.remove(insn);
            return true;
        }
        //(UNUSED) shouldSideBeRendered, line 474
        else if(index == 3 && checkMethod(insn, obfuscated ? "func_185904_a" : "getMaterial", "()Lnet/minecraft/block/material/Material;")) {
            final InsnList list = new InsnList();
            //add params
            list.add(new VarInsnNode(ALOAD, 2));
            list.add(new VarInsnNode(ALOAD, 3));
            list.add(new VarInsnNode(ALOAD, 4));

            list.add(genMethodNode("shouldSideBeRendered", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/block/material/Material;"));
            instructions.insert(insn, list);
            instructions.remove(insn);
            return true;
        }

        return false;
    }
}
