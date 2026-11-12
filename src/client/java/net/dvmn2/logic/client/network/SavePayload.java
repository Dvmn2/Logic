package net.dvmn2.logic.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * C2S {@code dvmn2:lg_save}: the result of an editing session. The server validates everything.
 * <pre>
 * VarInt  protocol
 * UUID    circuit
 * VarInt  action   (0 = save, 1 = cancel)
 * VarInt  width    (0 for cancel)
 * VarInt  height   (0 for cancel)
 * byte[]  cells    (VarInt length, then width * height packed cells; empty for cancel)
 * </pre>
 */
public record SavePayload(int protocol, UUID circuit, int action, int width, int height,
        byte[] cells) implements CustomPayload {

    public static final CustomPayload.Id<SavePayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "lg_save"));

    public static final PacketCodec<PacketByteBuf, SavePayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.protocol());
                buf.writeUuid(p.circuit());
                buf.writeVarInt(p.action());
                buf.writeVarInt(p.width());
                buf.writeVarInt(p.height());
                buf.writeByteArray(p.cells());
            },
            buf -> {
                int protocol = buf.readVarInt();
                if (protocol != LogicProtocol.PROTOCOL) {
                    LogicProtocol.skipRest(buf);
                    return new SavePayload(protocol, new UUID(0L, 0L), LogicProtocol.ACTION_CANCEL, 0, 0,
                            new byte[0]);
                }
                return new SavePayload(protocol, buf.readUuid(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readByteArray(LogicProtocol.MAX_CELLS));
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
