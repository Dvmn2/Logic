package net.dvmn2.logic.client.network;

import net.minecraft.network.PacketByteBuf;

/** Constants shared by the payloads of the editor protocol. Mirrors {@code EditorNetwork} of the plugin. */
public final class LogicProtocol {

    /** Wire protocol version; MUST equal {@code EditorNetwork.PROTOCOL} of the plugin. */
    public static final int PROTOCOL = 1;
    /** Longest circuit name accepted from the server, characters. */
    public static final int MAX_NAME_LENGTH = 64;
    /** Largest circuit side, cells. */
    public static final int MAX_DIMENSION = 128;
    /** Largest cell count of one circuit. */
    public static final int MAX_CELLS = MAX_DIMENSION * MAX_DIMENSION;
    /** Largest size of a lit bitmask, bytes. */
    public static final int MAX_BITS_BYTES = (MAX_CELLS + 7) / 8;

    /** Action of the save packet: store the edited cells. */
    public static final int ACTION_SAVE = 0;
    /** Action of the save packet: close without storing. */
    public static final int ACTION_CANCEL = 1;

    private LogicProtocol() {
    }

    /** Discards the rest of a packet whose protocol version is not understood. */
    static void skipRest(PacketByteBuf buf) {
        buf.skipBytes(buf.readableBytes());
    }
}
