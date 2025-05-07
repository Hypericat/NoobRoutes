package noobroutes.mixin;


import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import noobroutes.utils.SilentRotator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    //@Inject(method = "rotateArroundXAndY", cancellable = true, at = @At("HEAD"))
    private void silent(float angle, float angleY, CallbackInfo ci){
        if (SilentRotator.INSTANCE.getPitchRenderer3Registered()) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(SilentRotator.INSTANCE.getRenderPitch().getNow(), 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(Minecraft.getMinecraft().thePlayer.rotationYaw, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.popMatrix();
            ci.cancel();
        }
    }

}
