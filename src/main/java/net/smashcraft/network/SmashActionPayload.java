package net.smashcraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SmashActionPayload(String action) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SmashActionPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("smashcraft", "action"));

    public static final StreamCodec<FriendlyByteBuf, SmashActionPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.action()),
            buf -> new SmashActionPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
