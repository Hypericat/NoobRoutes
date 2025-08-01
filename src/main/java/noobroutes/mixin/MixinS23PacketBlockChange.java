/*package noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import noobroutes.events.impl.PacketEvent;
import noobroutes.utils.ClientUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;


}*/

package noobroutes.mixin;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import noobroutes.IS23;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(S23PacketBlockChange.class)
public class MixinS23PacketBlockChange implements IS23 {
    @Shadow public IBlockState blockState;

    @Shadow private BlockPos blockPosition;

    @Unique
    public void setBlock(BlockPos pos, IBlockState state) {
        this.blockState = state;
        this.blockPosition = pos;
    }
}

