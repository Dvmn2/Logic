package net.dvmn2.logic.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * S2C {@code dvmn2:lg_signals}: which cells carry a true signal; replaces the previous state.
 * <pre>
 * VarInt  protocol
 * UUID    circuit
 * byte[]  bits   (VarInt length, bit i of the array = cell i is lit)
 * </pre>
 */
public record SignalsPayload(int protocol, UUID circuit, byte[] bits) implements CustomPayload {

    public static final CustomPayload.Id<SignalsPayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "lg_signals"));

    public static final PacketCodec<PacketByteBuf, SignalsPayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.protocol());
                buf.writeUuid(p.circuit());
                buf.writeByteArray(p.bits());
            },
            buf -> {
                int protocol = buf.readVarInt();
                if (protocol != LogicProtocol.PROTOCOL) {
                    LogicProtocol.skipRest(buf);
                    return new SignalsPayload(protocol, new UUID(0L, 0L), new byte[0]);
                }
                return new SignalsPayload(protocol, buf.readUuid(), buf.readByteArray(LogicProtocol.MAX_BITS_BYTES));
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
