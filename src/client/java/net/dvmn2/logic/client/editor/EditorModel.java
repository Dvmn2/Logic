package net.dvmn2.logic.client.editor;

import net.dvmn2.logic.client.network.LogicProtocol;
import net.dvmn2.logic.client.network.OpenPayload;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * State of one open editor: the cells being edited and the signals last shown. Client thread only.
 *
 * <p>The state is visual and advisory; the server validates every save.
 */
public final class EditorModel {

    private static final int BITS_PER_BYTE = 8;

    private final UUID circuit;
    private final String name;
    private final boolean setup;
    private final int width;
    private final int height;
    private final int allowedMask;
    private final byte[] cells;
    private byte[] lit;

    private EditorModel(OpenPayload payload) {
        this.circuit = payload.circuit();
        this.name = payload.name();
        this.setup = payload.mode() == 1;
        this.width = payload.width();
        this.height = payload.height();
        this.allowedMask = payload.allowedMask();
        this.cells = payload.cells().clone();
        this.lit = new byte[(width * height + BITS_PER_BYTE - 1) / BITS_PER_BYTE];
    }

    /** Builds a model from the payload, or returns {@code null} when the payload is invalid. */
    public static @Nullable EditorModel from(OpenPayload payload) {
        if (payload.protocol() != LogicProtocol.PROTOCOL) {
            return null;
        }
        int w = payload.width();
        int h = payload.height();
        if (w < 1 || w > LogicProtocol.MAX_DIMENSION || h < 1 || h > LogicProtocol.MAX_DIMENSION) {
            return null;
        }
        if (payload.cells().length != w * h) {
            return null;
        }
        return new EditorModel(payload);
    }

    public UUID circuit() {
        return circuit;
    }

    public String name() {
        return name;
    }

    public boolean isSetup() {
        return setup;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Returns true when the creator allowed the type for ordinary editing. */
    public boolean isAllowed(CellType type) {
        return (allowedMask & type.bit()) != 0;
    }

    public byte get(int x, int y) {
        return cells[y * width + x];
    }

    public void set(int x, int y, byte cell) {
        cells[y * width + x] = cell;
    }

    /** Returns a copy of the cells for sending. */
    public byte[] cellsCopy() {
        return cells.clone();
    }

    public boolean isLit(int x, int y) {
        int index = y * width + x;
        return (lit[index / BITS_PER_BYTE] & (1 << (index % BITS_PER_BYTE))) != 0;
    }

    /** Replaces the lit bitmask; a mask of the wrong length is ignored. */
    public void setLit(byte[] bits) {
        if (bits.length == lit.length) {
            lit = bits.clone();
        }
    }
}
