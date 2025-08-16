package noobroutes;

import net.minecraft.client.Minecraft;
import noobroutes.utils.MutableVec3;
import noobroutes.utils.skyblock.PlayerUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CenterType {
    //All, Angles, Pos, X, Y, Z, Yaw, Pitch, Edge, Center
    ALL("ALL",
            () -> centerX(false),
            () -> centerZ(false),
            CenterType::centerYaw,
            CenterType::centerPitch
    ),
    ANGLES("ANGLES",
            CenterType::centerYaw,
            CenterType::centerPitch
    ),
    POS("POS",
            () -> centerX(false),
            CenterType::centerY,
            () -> centerZ(false)
    ),
    X("X", () -> centerX(false)),
    Y("Y", CenterType::centerY),
    Z("Z", () -> centerZ(false)),
    YAW("YAW", CenterType::centerYaw),
    PITCH("PITCH", CenterType::centerPitch),
    EDGE("EDGE",
            () -> centerX(true),
            () -> centerZ(true)
    ),
    CENTER("CENTER",
            () -> centerX(false),
            () -> centerZ(false)
    );

    private static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final List<Runnable> executes;

    private static MutableVec3 pos;
    private static float yaw;
    private static float pitch;

    CenterType(String name, Runnable... executes) {
        this.name = name;
        this.executes = Arrays.asList(executes);
    }

    public String getName() {
        return name;
    }

    public void run() {
        pos = new MutableVec3(mc.thePlayer.getPositionVector());
        yaw = mc.thePlayer.rotationYaw;
        pitch = mc.thePlayer.rotationPitch;
        executes.forEach(Runnable::run);

        PlayerUtils.INSTANCE.stopVelocity();
        mc.thePlayer.setPosition(pos.getX(), pos.getY(), pos.getZ());
        mc.thePlayer.rotationYaw = yaw;
        mc.thePlayer.rotationPitch = pitch;
    }

    public static CenterType fromString(String string) {
        return Arrays.stream(CenterType.values()).filter(centerType -> centerType.name.equalsIgnoreCase(string)).findAny().orElse(null);
    }

    private static void centerX(boolean edge) {
        if (edge) {
            pos.setX((Math.floor(mc.thePlayer.posX)));
            return;
        }
        pos.setX((Math.floor(mc.thePlayer.posX)) + 0.5);
    }

    private static void centerY() {
        if (!mc.isSingleplayer()) return; // Only runs in singleplayer so you don't get banned!
        pos.setY((int) mc.thePlayer.posY);
    }

    private static void centerZ(boolean edge) {
        if (edge) {
            pos.setZ((Math.floor(mc.thePlayer.posZ)));
            return;
        }
        pos.setZ((Math.floor(mc.thePlayer.posZ) + 0.5));
    }

    private static void centerYaw() {
        yaw = (Math.round((mc.thePlayer.rotationYaw) / 45f)) * 45f;
    }

    private static void centerPitch() {
        pitch = 0;
    }
}
