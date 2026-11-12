package net.dvmn2.logic.client.editor;

import org.jetbrains.annotations.Nullable;

/**
 * Packing of one cell into a byte, identical to {@code Cells} of the plugin.
 * <pre>
 * bits 0..3  type code
 * bits 4..5  direction: 0 = up, 1 = right, 2 = down, 3 = left
 * bit  6     locked
 * bit  7     reserved, zero
 * </pre>
 */
public final class Cells {

    private static final int TYPE_MASK = 0x0F;
    private static final int DIRECTION_SHIFT = 4;
    private static final int DIRECTION_MASK = 0x03;
    private static final int LOCKED_BIT = 0x40;

    private Cells() {
    }

    public static byte pack(CellType type, int direction, boolean locked) {
        int value = type.code() | ((direction & DIRECTION_MASK) << DIRECTION_SHIFT);
        return (byte) (locked ? value | LOCKED_BIT : value);
    }

    /** Returns the type of a cell, or {@code null} for an unknown code. */
    public static @Nullable CellType type(byte cell) {
        return CellType.fromCode(cell & TYPE_MASK);
    }

    public static int direction(byte cell) {
        return (cell >> DIRECTION_SHIFT) & DIRECTION_MASK;
    }

    public static boolean isLocked(byte cell) {
        return (cell & LOCKED_BIT) != 0;
    }

    public static byte withLocked(byte cell, boolean locked) {
        return (byte) (locked ? cell | LOCKED_BIT : cell & ~LOCKED_BIT);
    }
}
