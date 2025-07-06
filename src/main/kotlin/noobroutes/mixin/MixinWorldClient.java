package noobroutes.mixin;


import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import noobroutes.features.dungeon.brush.BrushModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World {
    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    @Shadow public abstract void invalidateBlockReceiveRegion(int x1, int y1, int z1, int x2, int y2, int z2);

    @Inject(method = "invalidateRegionAndSetBlock", at = @At("HEAD"), cancellable = true)
    private void invalidateRegionAndSetBlock(BlockPos pos, IBlockState state, CallbackInfoReturnable<Boolean> cir) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        this.invalidateBlockReceiveRegion(i, j, k, i, j, k);
        IBlockState edited = BrushModule.INSTANCE.getEditedBlock(pos);
        IBlockState finalState = (edited != null && BrushModule.INSTANCE.getEnabled()) ? edited : state;
        cir.setReturnValue(super.setBlockState(pos, finalState, 3));
    }
}
