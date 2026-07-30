package net.sweenus.simplyswords.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.screen.RunicForgeScreenHandler;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class RunicForgeScreen extends HandledScreen<RunicForgeScreenHandler> {
    private static final Identifier TEXTURE =
            Identifier.of(SimplySwords.MOD_ID, "textures/gui/container/runic_forge.png");
    private static final long CHANNEL_DURATION_MS = 800L;
    private static final int GLOW = 0xCC74E7FF;
    private static final int GLOW_SOFT = 0x5574E7FF;

    private final int[] previousCounts = new int[RunicForgeScreenHandler.FORGE_SLOT_COUNT];
    private final long[] animationStarts = new long[RunicForgeScreenHandler.FORGE_SLOT_COUNT];

    public RunicForgeScreen(RunicForgeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
        playerInventoryTitleY = 74;
        Arrays.fill(previousCounts, -1);
    }

    @Override
    protected void init() {
        super.init();
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        titleY = 5;
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        long now = Util.getMeasuringTimeMs();
        for (int i = 0; i < RunicForgeScreenHandler.FORGE_SLOT_COUNT; i++) {
            int count = handler.getSlot(i).getStack().getCount();
            if (previousCounts[i] >= 0 && count > previousCounts[i] && i != RunicForgeScreenHandler.WEAPON_SLOT) {
                animationStarts[i] = now;
            }
            previousCounts[i] = count;
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);

        long now = Util.getMeasuringTimeMs();
        for (int slotIndex = 1; slotIndex < RunicForgeScreenHandler.FORGE_SLOT_COUNT; slotIndex++) {
            long elapsed = now - animationStarts[slotIndex];
            if (animationStarts[slotIndex] == 0L || elapsed < 0L || elapsed > CHANNEL_DURATION_MS) continue;
            float progress = Math.min(1.0F, elapsed / (float) CHANNEL_DURATION_MS);
            Slot source = handler.getSlot(slotIndex);
            Slot weapon = handler.getSlot(RunicForgeScreenHandler.WEAPON_SLOT);
            int startX = x + source.x + 8;
            int startY = y + source.y + 8;
            int endX = x + weapon.x + 8;
            int endY = y + weapon.y + 8;
            if (slotIndex >= RunicForgeScreenHandler.TABLET_START) {
                drawTravelingChannel(context, progress,
                        startX, startY,
                        startX, y + 50,
                        endX, y + 50,
                        endX, endY);
            } else {
                drawTravelingChannel(context, progress, startX, startY, endX, endY);
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        long now = Util.getMeasuringTimeMs();
        boolean weaponGlow = false;
        for (int i = 1; i < RunicForgeScreenHandler.FORGE_SLOT_COUNT; i++) {
            long elapsed = now - animationStarts[i];
            if (animationStarts[i] == 0L || elapsed < 0L || elapsed > CHANNEL_DURATION_MS) continue;
            Slot slot = handler.getSlot(i);
            float pulse = 0.45F + 0.55F * (float) Math.sin(elapsed * 0.025D);
            drawSlotOutline(context, slot.x - 1, slot.y - 1, 18, withAlpha(GLOW, pulse));
            weaponGlow |= elapsed > CHANNEL_DURATION_MS * 0.55F;
        }
        if (weaponGlow) {
            Slot weapon = handler.getSlot(RunicForgeScreenHandler.WEAPON_SLOT);
            drawSlotOutline(context, weapon.x - 1, weapon.y - 1, 18, GLOW);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        if (handler.getCursorStack().isEmpty() && focusedSlot != null && focusedSlot.hasStack()) {
            ItemStack tooltipStack = focusedSlot == handler.getSlot(RunicForgeScreenHandler.WEAPON_SLOT)
                    ? handler.getPreviewStack()
                    : focusedSlot.getStack();
            if (tooltipStack.isEmpty()) {
                tooltipStack = focusedSlot.getStack();
            }
            // Hand the real or server-synchronised preview stack to DrawContext so
            // Simply Tooltips can render its complete component-aware tooltip.
            context.drawItemTooltip(textRenderer, tooltipStack, mouseX, mouseY);
            return;
        }
        super.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private static void drawTravelingChannel(DrawContext context, float progress, int... points) {
        List<int[]> samples = new ArrayList<>();
        for (int point = 0; point + 3 < points.length; point += 2) {
            int x0 = points[point];
            int y0 = points[point + 1];
            int x1 = points[point + 2];
            int y1 = points[point + 3];
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            for (int i = point == 0 ? 0 : 1; i <= steps; i++) {
                float t = steps == 0 ? 1.0F : i / (float) steps;
                samples.add(new int[]{
                        Math.round(x0 + (x1 - x0) * t),
                        Math.round(y0 + (y1 - y0) * t)
                });
            }
        }
        if (samples.isEmpty()) return;
        int head = Math.clamp(Math.round((samples.size() - 1) * progress), 0, samples.size() - 1);
        int tail = Math.max(0, head - 16);
        for (int i = tail; i <= head; i++) {
            int x = samples.get(i)[0];
            int y = samples.get(i)[1];
            float alpha = (i - tail + 1) / (float) Math.max(1, head - tail + 1);
            context.fill(x - 1, y - 1, x + 2, y + 2, withAlpha(GLOW_SOFT, alpha));
            context.fill(x, y, x + 1, y + 1, withAlpha(GLOW, alpha));
        }
    }

    private static void drawSlotOutline(DrawContext context, int x, int y, int size, int color) {
        context.fill(x, y, x + size, y + 1, color);
        context.fill(x, y + size - 1, x + size, y + size, color);
        context.fill(x, y, x + 1, y + size, color);
        context.fill(x + size - 1, y, x + size, y + size, color);
    }

    private static int withAlpha(int color, float multiplier) {
        int alpha = Math.clamp(Math.round(((color >>> 24) & 0xFF) * multiplier), 0, 255);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
