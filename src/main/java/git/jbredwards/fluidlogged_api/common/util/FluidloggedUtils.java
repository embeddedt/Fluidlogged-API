package git.jbredwards.fluidlogged_api.common.util;

import git.jbredwards.fluidlogged_api.Main;
import git.jbredwards.fluidlogged_api.common.block.ICompatibleFluid;
import git.jbredwards.fluidlogged_api.common.block.IFluidloggable;
import git.jbredwards.fluidlogged_api.common.block.IFluidloggableFluid;
import git.jbredwards.fluidlogged_api.common.config.FluidloggedConfig;
import git.jbredwards.fluidlogged_api.common.event.FluidloggedEvent;
import git.jbredwards.fluidlogged_api.common.network.FluidStateMessage;
import git.jbredwards.fluidlogged_api.common.storage.IFluidStateCapability;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * helpful functions
 * @author jbred
 *
 */
public enum FluidloggedUtils
{
    ;

    //convenience method
    @Nonnull
    public static FluidState getFluidState(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return getFluidState(world, pos, world.getBlockState(pos));
    }

    //forms the FluidState from the IBlockState here if it's a fluid block,
    //if not a fluid block, return FluidState stored via the capability
    @Nonnull
    public static FluidState getFluidState(@Nullable IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState here) {
        final @Nullable Fluid fluidHere = getFluidFromState(here);
        return (fluidHere != null) ? new FluidState(fluidHere, here) : FluidState.get(world, pos);
    }

    //convenience method
    @Nonnull
    public static IBlockState getFluidOrReal(@Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return getFluidOrReal(world, pos, world.getBlockState(pos));
    }

    //tries to get the fluid at the pos (prioritizing ones physically in the world, then the fluid capability),
    //if none return input state
    @Nonnull
    public static IBlockState getFluidOrReal(@Nullable IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState here) {
        return getFluidFromState(here) != null ? here : Optional.ofNullable(FluidState.get(world, pos).getState()).orElse(here);
    }

    //convenience method that uses default block flags
    public static boolean setFluidState(@Nonnull World world, @Nonnull BlockPos pos, @Nullable IBlockState here, @Nonnull FluidState fluidState, boolean checkVaporize) {
        return setFluidState(world, pos, here, fluidState, checkVaporize, Constants.BlockFlags.DEFAULT_AND_RERENDER);
    }

    public static boolean setFluidState(@Nonnull World world, @Nonnull BlockPos pos, @Nullable IBlockState here, @Nonnull FluidState fluidState, boolean checkVaporize, int blockFlags) {
        if(world.isOutsideBuildHeight(pos) || world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES) return false;

        final Chunk chunk = world.getChunkFromBlockCoords(pos);
        if(here == null) here = chunk.getBlockState(pos);

        final FluidloggedEvent event = new FluidloggedEvent(world, chunk, pos, here, fluidState, checkVaporize, blockFlags);
        //event did stuff
        if(MinecraftForge.EVENT_BUS.post(event) && event.getResult() != Event.Result.DEFAULT) return event.getResult() == Event.Result.ALLOW;
        //default
        else {
            //if the world is to warm for the fluid, vaporize it
            final @Nullable Fluid fluid = event.fluidState.getFluid();
            if(event.checkVaporize && fluid != null && world.provider.doesWaterVaporize() && fluid.doesVaporize(new FluidStack(fluid, Fluid.BUCKET_VOLUME))) {
                fluid.vaporize(null, world, pos, new FluidStack(fluid, Fluid.BUCKET_VOLUME));
                return true;
            }

            //check for IFluidloggable
            if(here.getBlock() instanceof IFluidloggable) {
                final EnumActionResult result = ((IFluidloggable)here.getBlock()).onFluidChange(world, pos, here, event.fluidState, event.blockFlags);
                if(result != EnumActionResult.PASS) return result == EnumActionResult.SUCCESS;
            }

            //moved to separate function, as to allow easy calling by IFluidloggable instances that use IFluidloggable#onFluidChange
            FluidloggedUtils.setFluidState_Internal(world, chunk, here, pos, event.fluidState, event.blockFlags);

            //default
            return event.getResult() != Event.Result.DENY;
        }
    }

    //if you're not an event instance or an IFluidloggable instance, use setFluidState instead!
    //moved to separate function, as to allow easy calling by IFluidloggable instances that use IFluidloggable#onFluidChange
    public static void setFluidState_Internal(@Nonnull World world, @Nonnull Chunk chunk, @Nonnull IBlockState here, @Nonnull BlockPos pos, @Nonnull FluidState fluidState, int blockFlags) {
        final @Nullable IFluidStateCapability cap = IFluidStateCapability.get(chunk);
        if(cap == null) throw new NullPointerException("There was a critical internal error involving the Fluidlogged API mod, notify the mod author!");

        //fix small graphical flicker with blocks placed inside fluids
        if(world.isRemote && !fluidState.isEmpty()) cap.setFluidState(pos, fluidState);

        //only do these on server
        if(!world.isRemote) {
            //send changes to server
            cap.setFluidState(pos, fluidState);

            //send changes to client
            if((blockFlags & Constants.BlockFlags.SEND_TO_CLIENTS) != 0) {
                Main.wrapper.sendToAllAround(
                        new FluidStateMessage(pos, fluidState),
                        new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 64)
                );
            }

            //update light levels
            relightFluidBlock(world, pos, fluidState);

            //post fluid added
            if(!fluidState.isEmpty()) fluidState.getBlock().onBlockAdded(world, pos, fluidState.getState());
        }

        //update world
        if((blockFlags & Constants.BlockFlags.NOTIFY_NEIGHBORS) != 0)
            world.markAndNotifyBlock(pos, chunk, here, here, blockFlags);
    }

    //forces a fluid light level & light opacity update
    public static void relightFluidBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull FluidState fluidState) {
        final @Nullable Chunk chunk = getChunk(world, pos);
        if(chunk == null) return;

        final int x = pos.getX() & 15;
        final int z = pos.getZ() & 15;
        final int height = chunk.getHeightValue(x, z);

        if(!fluidState.isEmpty() && fluidState.getState().getLightOpacity(world, pos) > 0) {
            if(pos.getY() >= height) chunk.relightBlock(x, pos.getY() + 1, z);
        }
        else if(pos.getY() == height - 1) chunk.relightBlock(x, pos.getY(), z);

        if(chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0 || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0)
            chunk.propagateSkylightOcclusion(x, z);

        world.profiler.startSection("checkLight");
        world.checkLight(pos);
        world.profiler.endSection();
    }

    //fork of Main.CommonProxy#getChunk
    @Nullable
    public static Chunk getChunk(@Nullable IBlockAccess world, @Nonnull BlockPos pos) { return Main.proxy.getChunk(world, pos); }

    //has two purposes:
    //1: returns true if the contained fluid can flow from the specified side
    //2: returns true if a fluid can flow into this block from the specified side
    public static boolean canFluidFlow(@Nonnull IBlockAccess world, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EnumFacing side) {
        return (state.getBlock() instanceof IFluidloggable)
                ? ((IFluidloggable)state.getBlock()).canFluidFlow(world, pos, state, side)
                : state.getBlockFaceShape(world, pos, side) != BlockFaceShape.SOLID;
    }

    //checks if two fluids are compatible
    public static boolean isCompatibleFluid(@Nullable Fluid fluid1, @Nullable Fluid fluid2) {
        if(fluid1 == null || fluid2 == null) return fluid1 == fluid2;
        else return fluid1 == fluid2
                || fluid1.getBlock() instanceof ICompatibleFluid && ((ICompatibleFluid)fluid1.getBlock()).isCompatibleFluid(fluid2)
                || fluid2.getBlock() instanceof ICompatibleFluid && ((ICompatibleFluid)fluid2.getBlock()).isCompatibleFluid(fluid1);
    }

    //convenience method that takes in an IBlockState rather than a Block
    @Nullable
    public static Fluid getFluidFromState(@Nullable IBlockState fluid) { return (fluid != null) ? getFluidFromBlock(fluid.getBlock()) : null; }

    //fork of IFluidBlock#getFluid
    //(BlockLiquid extends IFluidBlock during runtime through asm)
    @Nullable
    public static Fluid getFluidFromBlock(@Nullable Block fluid) { return (fluid instanceof IFluidBlock) ? ((IFluidBlock)fluid).getFluid() : null; }

    //return true if the IBlockState can be fluidlogged
    public static boolean isFluidloggableFluid(@Nonnull IBlockState fluid, boolean checkLevel) {
        //(BlockLiquid & BlockFluidClassic extend this through asm)
        if(!(fluid.getBlock() instanceof IFluidloggableFluid)
                || !((IFluidloggableFluid)fluid.getBlock()).isFluidloggableFluid(fluid)) return false;

        //check for fluid level
        if(checkLevel) {
            final int level = fluid.getValue(BlockLiquid.LEVEL);
            return level == 0 || (level >= 8 && AccessorUtils.canCreateSources(fluid.getBlock()));
        }

        //skip level check
        return true;
    }

    public static boolean isStateFluidloggable(@Nonnull IBlockState state, @Nullable Fluid fluid) {
        //config
        final EnumActionResult result = FluidloggedConfig.isStateFluidloggable(state, fluid);
        if(result != EnumActionResult.PASS) return result == EnumActionResult.SUCCESS;
        //defaults
        return (state.getBlock() instanceof IFluidloggable) && (fluid != null
                ? ((IFluidloggable)state.getBlock()).isFluidValid(state, fluid)
                : ((IFluidloggable)state.getBlock()).isFluidloggable(state));
    }
}
