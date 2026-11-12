package net.dvmn2.logic.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * S2C {@code dvmn2:lg_close}: the server ended the editor session; nothing is sent back.
 * <pre>
 * VarInt  protocol
 * UUID    circuit
 * </pre>
 */
public record ClosePayload(int protocol, UUID circuit) implements CustomPayload {

    public static final CustomPayload.Id<ClosePayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "lg_close"));

    public static final PacketCodec<PacketByteBuf, ClosePayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.protocol());
                buf.writeUuid(p.circuit());
            },
            buf -> {
                int protocol = buf.readVarInt();
                if (protocol != LogicProtocol.PROTOCOL) {
                    LogicProtocol.skipRest(buf);
                    return new ClosePayload(protocol, new UUID(0L, 0L));
                }
                return new ClosePayload(protocol, buf.readUuid());
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
