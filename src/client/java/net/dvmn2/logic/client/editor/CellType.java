package net.dvmn2.logic.client.editor;

import org.jetbrains.annotations.Nullable;

/**
 * Kind of a circuit cell as the editor draws it. The code is part of the wire format and MUST equal
 * {@code CellType.code()} of the plugin.
 */
public enum CellType {
    EMPTY(0, "empty", 0xFF1E1E1E, 0xFF1E1E1E, false),
    WIRE(1, "wire", 0xFF5A5A5A, 0xFF55FF55, true),
    INPUT(2, "input", 0xFF8A2A2A, 0xFFFF5555, true),
    OUTPUT(3, "output", 0xFF8A5A1A, 0xFFFFAA33, false),
    AND(4, "and", 0xFF2A4A8A, 0xFF55CCFF, true),
    OR(5, "or", 0xFF2A6A8A, 0xFF55CCFF, true),
    NOT(6, "not", 0xFF5A2A8A, 0xFFCC88FF, true),
    XOR(7, "xor", 0xFF2A8A6A, 0xFF55FFCC, true),
    NAND(8, "nand", 0xFF3A3A9A, 0xFF8899FF, true),
    NOR(9, "nor", 0xFF3A7A9A, 0xFF88DDFF, true),
    XNOR(10, "xnor", 0xFF2A8A4A, 0xFF88FFAA, true),
    SPLITTER(11, "splitter", 0xFF7A6A2A, 0xFFFFEE55, true),
    BRIDGE(12, "bridge", 0xFF6A6A6A, 0xFFBBBBBB, false);

    private static final CellType[] BY_CODE = values();

    private final int code;
    private final String key;
    private final int color;
    private final int litColor;
    private final boolean directional;

    CellType(int code, String key, int color, int litColor, boolean directional) {
        this.code = code;
        this.key = key;
        this.color = color;
        this.litColor = litColor;
        this.directional = directional;
    }

    /** Returns the type for a code, or {@code null} when the code is unknown. */
    public static @Nullable CellType fromCode(int code) {
        return code >= 0 && code < BY_CODE.length ? BY_CODE[code] : null;
    }

    public int code() {
        return code;
    }

    /** Suffix of the translation keys {@code logic.cell.*} and {@code logic.label.*}. */
    public String key() {
        return key;
    }

    /** Body colour of an unlit cell, ARGB. */
    public int color() {
        return color;
    }

    /** Body colour of a lit cell, ARGB. */
    public int litColor() {
        return litColor;
    }

    /** Returns true when the cell has a meaningful direction. */
    public boolean isDirectional() {
        return directional;
    }

    /** Returns true for types an editor may place when the creator allowed them. */
    public boolean isPlaceable() {
        return this != EMPTY && this != INPUT && this != OUTPUT;
    }

    /** Returns the bit of this type inside an allowed-types mask. */
    public int bit() {
        return 1 << code;
    }
}
