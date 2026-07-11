package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    private static final int PIP_SIZE = 6;
    private static final int PIP_GAP = 2;
    private static final int SOUL_DEBT_COLOR = 0xFF69F6D7;
    private static final int SOUL_DEBT_EMPTY_COLOR = 0x661B3D42;
    private static final int SOUL_DEBT_BORDER_COLOR = 0xCC0D1D24;

    @Inject(method = "render", at = @At("TAIL"))
    private void simplyswords$renderSoulDebtHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.SOULSTEALER.get())) {
            return;
        }

        int stacks = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
        if (stacks <= 0) {
            return;
        }

        int maxStacks = Math.max(1, Config.uniqueEffects.soulstealer.maxStacks);
        int clampedStacks = Math.min(stacks, maxStacks);
        int width = maxStacks * PIP_SIZE + (maxStacks - 1) * PIP_GAP;
        int x = context.getScaledWindowWidth() / 2 - width / 2;
        int y = context.getScaledWindowHeight() - 68;

        for (int i = 0; i < maxStacks; i++) {
            int pipX = x + i * (PIP_SIZE + PIP_GAP);
            int color = i < clampedStacks ? SOUL_DEBT_COLOR : SOUL_DEBT_EMPTY_COLOR;
            context.fill(pipX, y, pipX + PIP_SIZE, y + PIP_SIZE, color);
            context.drawBorder(pipX - 1, y - 1, PIP_SIZE + 2, PIP_SIZE + 2, SOUL_DEBT_BORDER_COLOR);
        }

        String label = clampedStacks + "/" + maxStacks;
        int labelX = context.getScaledWindowWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, labelX, y - 11, SOUL_DEBT_COLOR);
    }
}
