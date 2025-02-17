package git.jbredwards.fluidlogged_api.asm.plugins.vanilla.client;

import git.jbredwards.fluidlogged_api.asm.plugins.IASMPlugin;
import org.objectweb.asm.tree.*;

import javax.annotation.Nonnull;

/**
 * allows the game to render FluidStates
 * @author jbred
 *
 */
public final class RenderChunkPlugin implements IASMPlugin
{
    @Override
    public int isMethodValid(@Nonnull MethodNode method, boolean obfuscated) {
        if(checkMethod(method, obfuscated ? "func_178581_b" : "rebuildChunk", null)) {
            setMaxLocals(method, 9);
            return 1;
        }

        return 0;
    }

    @Override
    public boolean transform(@Nonnull InsnList instructions, @Nonnull MethodNode method, @Nonnull AbstractInsnNode insn, boolean obfuscated, int index) {
        //VANILLA, line 187
        if(checkMethod(insn, "canRenderInLayer", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockRenderLayer;)Z")) {
            final InsnList list = new InsnList();
            //boolean array variable
            list.add(new VarInsnNode(ALOAD, 11));
            //chunk compiler variable
            list.add(new VarInsnNode(ALOAD, 4));
            //compiled chunk variable
            list.add(new VarInsnNode(ALOAD, 5));
            //world variable
            list.add(new VarInsnNode(ALOAD, 0));
            list.add(new FieldInsnNode(GETFIELD, "net/minecraft/client/renderer/chunk/RenderChunk", (obfuscated ? "field_189564_r" : "worldView"), "Lnet/minecraft/world/ChunkCache;"));
            //block position variable
            list.add(new VarInsnNode(ALOAD, 14));
            //chunk position variable
            list.add(new VarInsnNode(ALOAD, 7));
            //adds the new code
            list.add(genMethodNode("renderChunk", "(Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockRenderLayer;[ZLnet/minecraft/client/renderer/chunk/ChunkCompileTaskGenerator;Lnet/minecraft/client/renderer/chunk/CompiledChunk;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Z"));
            instructions.insert(insn, list);
            instructions.remove(insn);
            return false;
        }

        return false;
    }
}
