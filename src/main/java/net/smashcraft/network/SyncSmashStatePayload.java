package net.smashcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncSmashStatePayload(int entityId, boolean isStarKO, boolean isShielding, float shieldHealth, boolean isSmashAttackReady) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSmashStatePayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("smashcraft", "sync_smash_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncSmashStatePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.entityId());
                buf.writeBoolean(payload.isStarKO());
                buf.writeBoolean(payload.isShielding());
                buf.writeFloat(payload.shieldHealth());
                buf.writeBoolean(payload.isSmashAttackReady());
            },
            buf -> new SyncSmashStatePayload(
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readBoolean()
            )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
