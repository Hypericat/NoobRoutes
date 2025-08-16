package noobroutes.mixin.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = {C08PacketPlayerBlockPlacement.class}, priority = 800)
public interface C08Accessor {
    @Accessor("stack")
    void setStack(ItemStack stack);
}
