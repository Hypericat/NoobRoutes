package noobroutes.mixin.accessors;


import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChunkProviderClient.class)
public interface ChunkListingFieldAccessor {
    @Accessor("chunkListing")
    List<Chunk> getChunkList();


}
