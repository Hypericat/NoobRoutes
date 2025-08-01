package noobroutes;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;

public interface INetwork {
    public void receive(Packet<INetHandler> packet);
}
