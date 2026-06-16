package net.smashcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SmashImpactPayload(float intensity, boolean doImpactFrame) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SmashImpactPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("smashcraft", "impact"));

    public static final StreamCodec<FriendlyByteBuf, SmashImpactPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeFloat(payload.intensity());
                buf.writeBoolean(payload.doImpactFrame());
            },
            buf -> new SmashImpactPayload(buf.readFloat(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
