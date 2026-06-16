package net.smashcraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncSmashPercentPayload(int entityId, float percent) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncSmashPercentPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("smashcraft", "sync_percent"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSmashPercentPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncSmashPercentPayload::entityId,
            ByteBufCodecs.FLOAT, SyncSmashPercentPayload::percent,
            SyncSmashPercentPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
