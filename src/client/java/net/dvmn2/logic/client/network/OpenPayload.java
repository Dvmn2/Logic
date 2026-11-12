package net.dvmn2.logic.client.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * S2C {@code dvmn2:lg_open}: the whole circuit for the editor; replaces any previous state.
 * <pre>
 * VarInt  protocol
 * UUID    circuit
 * String  name         (max 64 characters)
 * VarInt  mode         (0 = edit, 1 = setup)
 * VarInt  width        (1..128)
 * VarInt  height       (1..128)
 * VarInt  allowedMask  (bit n set = cell type with code n may be placed)
 * byte[]  cells        (VarInt length, then width * height packed cells)
 * </pre>
 */
public record OpenPayload(int protocol, UUID circuit, String name, int mode, int width, int height,
        int allowedMask, byte[] cells) implements CustomPayload {

    public static final CustomPayload.Id<OpenPayload> ID =
            new CustomPayload.Id<>(Identifier.of("dvmn2", "lg_open"));

    public static final PacketCodec<PacketByteBuf, OpenPayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeVarInt(p.protocol());
                buf.writeUuid(p.circuit());
                buf.writeString(p.name());
                buf.writeVarInt(p.mode());
                buf.writeVarInt(p.width());
                buf.writeVarInt(p.height());
                buf.writeVarInt(p.allowedMask());
                buf.writeByteArray(p.cells());
            },
            buf -> {
                int protocol = buf.readVarInt();
                if (protocol != LogicProtocol.PROTOCOL) {
                    LogicProtocol.skipRest(buf);
                    return new OpenPayload(protocol, new UUID(0L, 0L), "", 0, 0, 0, 0, new byte[0]);
                }
                return new OpenPayload(protocol, buf.readUuid(), buf.readString(LogicProtocol.MAX_NAME_LENGTH),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readByteArray(LogicProtocol.MAX_CELLS));
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
