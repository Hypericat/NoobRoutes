package mixin.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(value = {Minecraft.class}, priority = 800)
public interface TimerFieldAccessor {
    @Accessor("timer")
    void setTimer(Timer timer);

    @Accessor("timer")
    Timer getTimer();
}
