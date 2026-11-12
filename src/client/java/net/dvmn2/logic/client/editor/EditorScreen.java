package net.dvmn2.logic.client.editor;

import net.dvmn2.logic.client.network.LogicProtocol;
import net.dvmn2.logic.client.network.SavePayload;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full-screen circuit editor: a zoomable grid, a palette of tools and live signal display.
 *
 * <p>Left mouse paints with the selected tool, right mouse rotates a cell, dragging with the right or middle
 * button pans, the wheel zooms, {@code R} rotates the tool. Closing the screen (Escape or the save button)
 * sends the result to the server; the cancel button and a close from the server send nothing more.
 * Client thread only.
 */
public final class EditorScreen extends Screen {

    private static final int PANEL_WIDTH = 100;
    private static final int TOP_HEIGHT = 32;
    private static final int BOTTOM_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PALETTE_GAP = 2;
    private static final int PALETTE_MARGIN = 4;
    private static final int MAX_ROW_HEIGHT = 14;
    private static final int MIN_ROW_HEIGHT = 10;
    private static final double MIN_CELL = 6.0;
    private static final double MAX_CELL = 48.0;
    private static final double MAX_INITIAL_CELL = 28.0;
    private static final double ZOOM_STEP = 1.15;
    private static final double PAN_THRESHOLD = 4.0;
    private static final int LABEL_MIN_SIZE = 22;
    private static final int GRID_MIN_SIZE = 8;
    private static final int TEXT_HEIGHT = 8;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_VIEW = 0xFF101010;
    private static final int COLOR_BOARD = 0xFF181818;
    private static final int COLOR_GRID = 0xFF2C2C2C;
    private static final int COLOR_INK = 0xFFEEEEEE;
    private static final int COLOR_INK_LIT = 0xFF102010;
    private static final int COLOR_LOCK = 0xFFFFD700;
    private static final int COLOR_LOCK_TOOL = 0xFF6A5A10;
    private static final int COLOR_HOVER = 0xFFFFFFFF;

    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};
    private static final String[] ARROWS = {"\u2191", "\u2192", "\u2193", "\u2190"};

    private record Tool(CellType type, boolean lock) {
    }

    private final EditorModel model;
    private final List<Tool> tools = new ArrayList<>();
    private int selectedTool;
    private int direction;
    private double cell = MAX_INITIAL_CELL;
    private double offsetX;
    private double offsetY;
    private boolean placed;
    private boolean painting;
    private boolean strokeStarted;
    private boolean strokeLockValue;
    private int lastX = -1;
    private int lastY = -1;
    private boolean dragging;
    private boolean panning;
    private double pressX;
    private double pressY;
    private boolean finished;
    private ButtonWidget directionButton;

    public EditorScreen(EditorModel model) {
        super(Text.translatable(model.isSetup() ? "logic.title.setup" : "logic.title.edit", model.name()));
        this.model = model;
    }

    /** UUID of the circuit being edited. */
    public UUID circuit() {
        return model.circuit();
    }

    /** Shows new signals. */
    public void setSignals(byte[] bits) {
        model.setLit(bits);
    }

    /** Closes the editor without sending anything; used when the server ended the session. */
    public void closeByServer() {
        finished = true;
        close();
    }

    @Override
    protected void init() {
        buildTools();
        int bottomY = height - BOTTOM_HEIGHT + (BOTTOM_HEIGHT - BUTTON_HEIGHT) / 2;
        directionButton = addDrawableChild(ButtonWidget.builder(directionText(), button -> rotateTool())
                .dimensions(PALETTE_MARGIN, bottomY, PANEL_WIDTH - 2 * PALETTE_MARGIN, BUTTON_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("logic.button.save"), button -> close())
                .dimensions(width / 2 - 105, bottomY, 100, BUTTON_HEIGHT)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("logic.button.cancel"), button -> cancel())
                .dimensions(width / 2 + 5, bottomY, 100, BUTTON_HEIGHT)
                .build());
        if (!placed) {
            fitToView();
            placed = true;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        if (!finished) {
            finished = true;
            EditorController.send(new SavePayload(LogicProtocol.PROTOCOL, model.circuit(),
                    LogicProtocol.ACTION_SAVE, model.width(), model.height(), model.cellsCopy()));
        }
        super.removed();
    }

    private void cancel() {
        finished = true;
        EditorController.send(new SavePayload(LogicProtocol.PROTOCOL, model.circuit(),
                LogicProtocol.ACTION_CANCEL, 0, 0, new byte[0]));
        close();
    }

    // ---------- Tools ----------

    private void buildTools() {
        tools.clear();
        tools.add(new Tool(CellType.EMPTY, false));
        for (CellType type : CellType.values()) {
            if (type.isPlaceable() && (model.isSetup() || model.isAllowed(type))) {
                tools.add(new Tool(type, false));
            }
        }
        if (model.isSetup()) {
            tools.add(new Tool(CellType.INPUT, false));
            tools.add(new Tool(CellType.OUTPUT, false));
            tools.add(new Tool(CellType.EMPTY, true));
        }
        selectedTool = Math.min(selectedTool, tools.size() - 1);
        if (selectedTool == 0 && tools.size() > 1) {
            selectedTool = 1;
        }
    }

    private Text directionText() {
        return Text.translatable("logic.button.direction", ARROWS[direction]);
    }

    private void rotateTool() {
        direction = (direction + 1) % DX.length;
        directionButton.setMessage(directionText());
    }

    private int rowHeight() {
        int available = height - TOP_HEIGHT - BOTTOM_HEIGHT - 2 * PALETTE_MARGIN;
        int row = available / Math.max(1, tools.size()) - PALETTE_GAP;
        return Math.max(MIN_ROW_HEIGHT, Math.min(MAX_ROW_HEIGHT, row));
    }

    private void selectToolAt(double y) {
        int index = (int) Math.floor((y - TOP_HEIGHT - PALETTE_MARGIN) / (rowHeight() + PALETTE_GAP));
        if (index >= 0 && index < tools.size()) {
            selectedTool = index;
        }
    }

    // ---------- View ----------

    private int viewLeft() {
        return PANEL_WIDTH;
    }

    private int viewTop() {
        return TOP_HEIGHT;
    }

    private int viewRight() {
        return width;
    }

    private int viewBottom() {
        return height - BOTTOM_HEIGHT;
    }

    private boolean inView(double x, double y) {
        return x >= viewLeft() && x < viewRight() && y >= viewTop() && y < viewBottom();
    }

    private void fitToView() {
        double viewWidth = viewRight() - viewLeft();
        double viewHeight = viewBottom() - viewTop();
        double fit = Math.min(viewWidth / model.width(), viewHeight / model.height());
        cell = Math.max(MIN_CELL, Math.min(MAX_INITIAL_CELL, fit));
        offsetX = viewLeft() + (viewWidth - model.width() * cell) / 2.0;
        offsetY = viewTop() + (viewHeight - model.height() * cell) / 2.0;
    }

    private int cellX(double screenX) {
        return (int) Math.floor((screenX - offsetX) / cell);
    }

    private int cellY(double screenY) {
        return (int) Math.floor((screenY - offsetY) / cell);
    }

    private boolean inBoard(int x, int y) {
        return x >= 0 && y >= 0 && x < model.width() && y < model.height();
    }

    // ---------- Rendering ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        renderGrid(context, mouseX, mouseY);
        renderPalette(context);
        renderHeader(context, mouseX, mouseY);
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY) {
        int left = viewLeft();
        int top = viewTop();
        context.fill(left, top, viewRight(), viewBottom(), COLOR_VIEW);
        context.enableScissor(left, top, viewRight(), viewBottom());

        int boardLeft = (int) Math.floor(offsetX);
        int boardTop = (int) Math.floor(offsetY);
        int boardRight = (int) Math.floor(offsetX + model.width() * cell);
        int boardBottom = (int) Math.floor(offsetY + model.height() * cell);
        context.fill(boardLeft, boardTop, boardRight, boardBottom, COLOR_BOARD);

        int firstX = Math.max(0, cellX(left));
        int lastCellX = Math.min(model.width() - 1, cellX(viewRight()));
        int firstY = Math.max(0, cellY(top));
        int lastCellY = Math.min(model.height() - 1, cellY(viewBottom()));
        int size = (int) Math.round(cell);

        if (size >= GRID_MIN_SIZE) {
            for (int x = firstX; x <= lastCellX + 1; x++) {
                int px = (int) Math.floor(offsetX + x * cell);
                context.fill(px, boardTop, px + 1, boardBottom, COLOR_GRID);
            }
            for (int y = firstY; y <= lastCellY + 1; y++) {
                int py = (int) Math.floor(offsetY + y * cell);
                context.fill(boardLeft, py, boardRight, py + 1, COLOR_GRID);
            }
        }
        for (int y = firstY; y <= lastCellY; y++) {
            for (int x = firstX; x <= lastCellX; x++) {
                drawCell(context, x, y,
                        (int) Math.floor(offsetX + x * cell), (int) Math.floor(offsetY + y * cell),
                        (int) Math.floor(offsetX + (x + 1) * cell), (int) Math.floor(offsetY + (y + 1) * cell));
            }
        }
        if (inView(mouseX, mouseY)) {
            int hx = cellX(mouseX);
            int hy = cellY(mouseY);
            if (inBoard(hx, hy)) {
                int px = (int) Math.floor(offsetX + hx * cell);
                int py = (int) Math.floor(offsetY + hy * cell);
                int next = (int) Math.floor(offsetX + (hx + 1) * cell);
                int nextY = (int) Math.floor(offsetY + (hy + 1) * cell);
                drawFrame(context, px, py, next - px, nextY - py, COLOR_HOVER);
            }
        }
        context.disableScissor();
    }

    private void drawCell(DrawContext context, int x, int y, int left, int top, int right, int bottom) {
        byte value = model.get(x, y);
        CellType type = Cells.type(value);
        boolean locked = Cells.isLocked(value);
        if (type != null && type != CellType.EMPTY) {
            boolean lit = model.isLit(x, y);
            context.fill(left + 1, top + 1, right, bottom, lit ? type.litColor() : type.color());
            drawSymbol(context, type, Cells.direction(value), lit, left, top, right, bottom);
        }
        if (locked) {
            context.fill(right - 5, top + 1, right - 1, top + 5, COLOR_LOCK);
        }
    }

    private void drawSymbol(DrawContext context, CellType type, int dir, boolean lit,
            int left, int top, int right, int bottom) {
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;
        int half = (right - left) / 2 - 3;
        int ink = lit ? COLOR_INK_LIT : COLOR_INK;
        boolean labelled = right - left >= LABEL_MIN_SIZE && !labelText(type).getString().isEmpty();
        if (type.isDirectional() && half >= 3) {
            if (labelled) {
                int shift = half - half / 2;
                drawArrow(context, centerX + DX[dir] * shift, centerY + DY[dir] * shift, dir, half / 2, ink);
            } else {
                drawArrow(context, centerX, centerY, dir, half, ink);
            }
        } else if (type == CellType.BRIDGE && half >= 3) {
            context.fill(centerX - half, centerY, centerX + half + 1, centerY + 1, ink);
            context.fill(centerX, centerY - half, centerX + 1, centerY + half + 1, ink);
        } else if (type == CellType.OUTPUT && half >= 3) {
            drawFrame(context, centerX - half / 2, centerY - half / 2, half + 1, half + 1, ink);
        }
        if (labelled) {
            Text label = labelText(type);
            context.drawText(textRenderer, label, centerX - textRenderer.getWidth(label) / 2,
                    centerY - TEXT_HEIGHT / 2, ink, false);
        }
    }

    private static void drawFrame(DrawContext context, int x, int y, int frameWidth, int frameHeight, int color) {
        context.fill(x, y, x + frameWidth, y + 1, color);
        context.fill(x, y + frameHeight - 1, x + frameWidth, y + frameHeight, color);
        context.fill(x, y + 1, x + 1, y + frameHeight - 1, color);
        context.fill(x + frameWidth - 1, y + 1, x + frameWidth, y + frameHeight - 1, color);
    }

    private static Text labelText(CellType type) {
        return Text.translatable("logic.label." + type.key());
    }

    private static void drawArrow(DrawContext context, int centerX, int centerY, int dir, int half, int color) {
        int fx = DX[dir];
        int fy = DY[dir];
        int thick = Math.max(1, half / 4);
        int head = Math.max(2, half / 2);
        int tailX = centerX - fx * half;
        int tailY = centerY - fy * half;
        int neckX = centerX + fx * (half - head);
        int neckY = centerY + fy * (half - head);
        if (fy == 0) {
            context.fill(Math.min(tailX, neckX), centerY - thick / 2,
                    Math.max(tailX, neckX) + 1, centerY - thick / 2 + thick, color);
        } else {
            context.fill(centerX - thick / 2, Math.min(tailY, neckY),
                    centerX - thick / 2 + thick, Math.max(tailY, neckY) + 1, color);
        }
        for (int k = 0; k < head; k++) {
            int along = half - head + k;
            int wing = head - k;
            int px = centerX + fx * along;
            int py = centerY + fy * along;
            if (fy == 0) {
                context.fill(px, centerY - wing, px + 1, centerY + wing + 1, color);
            } else {
                context.fill(centerX - wing, py, centerX + wing + 1, py + 1, color);
            }
        }
    }

    private void renderPalette(DrawContext context) {
        int rowHeight = rowHeight();
        int y = TOP_HEIGHT + PALETTE_MARGIN;
        for (int i = 0; i < tools.size(); i++) {
            Tool tool = tools.get(i);
            int left = PALETTE_MARGIN;
            int rowWidth = PANEL_WIDTH - 2 * PALETTE_MARGIN;
            int color = tool.lock() ? COLOR_LOCK_TOOL : tool.type().color();
            context.fill(left, y, left + rowWidth, y + rowHeight, color);
            if (i == selectedTool) {
                drawFrame(context, left, y, rowWidth, rowHeight, COLOR_HOVER);
            }
            Text label = tool.lock()
                    ? Text.translatable("logic.tool.lock")
                    : Text.translatable("logic.cell." + tool.type().key());
            context.drawText(textRenderer, label, left + 4, y + (rowHeight - TEXT_HEIGHT) / 2 + 1, COLOR_TEXT, true);
            y += rowHeight + PALETTE_GAP;
        }
    }

    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, PANEL_WIDTH + 4, 6, COLOR_TEXT, true);
        context.drawText(textRenderer, Text.translatable("logic.hint"), PANEL_WIDTH + 4, 6 + TEXT_HEIGHT + 4,
                COLOR_INK, false);
        if (inView(mouseX, mouseY)) {
            int hx = cellX(mouseX);
            int hy = cellY(mouseY);
            if (inBoard(hx, hy)) {
                boolean locked = Cells.isLocked(model.get(hx, hy));
                Text hover = Text.translatable(locked ? "logic.hover.locked" : "logic.hover", hx, hy);
                context.drawText(textRenderer, hover, width - textRenderer.getWidth(hover) - 6, 6, COLOR_TEXT, true);
            }
        }
    }

    // ---------- Input ----------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        double x = click.x();
        double y = click.y();
        int button = click.button();
        if (button == 0 && x < PANEL_WIDTH && y >= TOP_HEIGHT && y < height - BOTTOM_HEIGHT) {
            selectToolAt(y);
            return true;
        }
        if (!inView(x, y)) {
            return false;
        }
        if (button == 0) {
            painting = true;
            strokeStarted = false;
            lastX = -1;
            lastY = -1;
            paintStroke(x, y);
            return true;
        }
        if (button == 1 || button == 2) {
            dragging = true;
            panning = button == 2;
            pressX = x;
            pressY = y;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (painting && click.button() == 0) {
            paintStroke(click.x(), click.y());
            return true;
        }
        if (dragging && (click.button() == 1 || click.button() == 2)) {
            if (!panning && Math.hypot(click.x() - pressX, click.y() - pressY) > PAN_THRESHOLD) {
                panning = true;
            }
            if (panning) {
                offsetX += deltaX;
                offsetY += deltaY;
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        int button = click.button();
        if (button == 0 && painting) {
            painting = false;
            return true;
        }
        if ((button == 1 || button == 2) && dragging) {
            if (button == 1 && !panning) {
                rotateCellAt(pressX, pressY);
            }
            dragging = false;
            panning = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!inView(mouseX, mouseY) || verticalAmount == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        double factor = verticalAmount > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
        double next = Math.max(MIN_CELL, Math.min(MAX_CELL, cell * factor));
        double gridX = (mouseX - offsetX) / cell;
        double gridY = (mouseY - offsetY) / cell;
        cell = next;
        offsetX = mouseX - gridX * cell;
        offsetY = mouseY - gridY * cell;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_R) {
            rotateTool();
            return true;
        }
        return super.keyPressed(input);
    }

    // ---------- Editing ----------

    private void paintStroke(double screenX, double screenY) {
        int gx = cellX(screenX);
        int gy = cellY(screenY);
        if (lastX < 0) {
            paintCell(gx, gy);
        } else {
            int steps = Math.max(Math.abs(gx - lastX), Math.abs(gy - lastY));
            for (int step = 1; step <= steps; step++) {
                paintCell(lastX + Math.round((float) (gx - lastX) * step / steps),
                        lastY + Math.round((float) (gy - lastY) * step / steps));
            }
        }
        lastX = gx;
        lastY = gy;
    }

    private void paintCell(int x, int y) {
        if (!inBoard(x, y)) {
            return;
        }
        byte old = model.get(x, y);
        Tool tool = tools.get(selectedTool);
        boolean setup = model.isSetup();
        if (tool.lock()) {
            if (!strokeStarted) {
                strokeLockValue = !Cells.isLocked(old);
                strokeStarted = true;
            }
            model.set(x, y, Cells.withLocked(old, strokeLockValue));
            return;
        }
        if (Cells.isLocked(old) && !setup) {
            return;
        }
        CellType type = tool.type();
        if (type == CellType.EMPTY) {
            model.set(x, y, Cells.pack(CellType.EMPTY, 0, false));
            return;
        }
        boolean fixed = setup && (type == CellType.INPUT || type == CellType.OUTPUT);
        model.set(x, y, Cells.pack(type, direction, fixed));
    }

    private void rotateCellAt(double screenX, double screenY) {
        int x = cellX(screenX);
        int y = cellY(screenY);
        if (!inBoard(x, y)) {
            return;
        }
        byte old = model.get(x, y);
        CellType type = Cells.type(old);
        if (type == null || !type.isDirectional() || Cells.isLocked(old) && !model.isSetup()) {
            return;
        }
        model.set(x, y, Cells.pack(type, (Cells.direction(old) + 1) % DX.length, Cells.isLocked(old)));
    }
}
