package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.component.MoltenHeatComponent;
import net.sweenus.simplyswords.item.component.ParryComponent;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    private static final int STORM_CHARGE_COLOR = 0xFF83E8FF;
    private static final int STORM_CHARGE_EMPTY_COLOR = 0x66112B42;
    private static final int STORM_CHARGE_BORDER_COLOR = 0xCC08263D;
    private static final int HEAT_BAR_WIDTH = 100;
    private static final int HEAT_BAR_HEIGHT = 8;
    private static final int HEAT_YELLOW_COLOR = 0xFFFFD52A;
    private static final int HEAT_ORANGE_COLOR = 0xFFFF8A00;
    private static final int HEAT_RED_COLOR = 0xFFFF2600;
    private static final int HEAT_EMPTY_COLOR = 0xAA271009;
    private static final int HEAT_BORDER_COLOR = 0xE06B1D08;
    private static final float HEAT_SMOOTHING_RATE = 0.43F;

    @Unique
    private float simplyswords$displayedHeat;
    @Unique
    private float simplyswords$lastHeatRenderTime;
    @Unique
    private boolean simplyswords$heatDisplayInitialized;

    @Inject(method = "render", at = @At("TAIL"))
    private void simplyswords$renderSoulDebtHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            simplyswords$heatDisplayInitialized = false;
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        ItemStack moltenEdge = simplyswords$selectMoltenEdge(client);
        if (!moltenEdge.isEmpty()) {
            MoltenHeatComponent heat = moltenEdge.getOrDefault(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
            renderHeatBar(context, client, tickCounter, heat);
            return;
        }
        simplyswords$heatDisplayInitialized = false;
        if (stack.isOf(ItemsRegistry.SOULSTEALER.get())) {
            int stacks = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
            renderChargePips(context, client, stacks, Math.max(1, Config.uniqueEffects.soulstealer.maxStacks), SOUL_DEBT_COLOR, SOUL_DEBT_EMPTY_COLOR, SOUL_DEBT_BORDER_COLOR);
            return;
        }
        if (stack.isOf(ItemsRegistry.STORMBRINGER.get())) {
            int charges = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT).stormCharges();
            renderChargePips(context, client, charges, Math.max(1, Config.uniqueEffects.stormbringer.maxStormCharges), STORM_CHARGE_COLOR, STORM_CHARGE_EMPTY_COLOR, STORM_CHARGE_BORDER_COLOR);
        }
    }

    @Unique
    private static ItemStack simplyswords$selectMoltenEdge(MinecraftClient client) {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        boolean mainIsMoltenEdge = mainHand.isOf(ItemsRegistry.MOLTEN_EDGE.get());
        boolean offIsMoltenEdge = offHand.isOf(ItemsRegistry.MOLTEN_EDGE.get());
        if (!mainIsMoltenEdge) {
            return offIsMoltenEdge ? offHand : ItemStack.EMPTY;
        }
        if (!offIsMoltenEdge) {
            return mainHand;
        }

        long worldTime = client.world == null ? 0L : client.world.getTime();
        int ventDrain = Math.max(1, Config.uniqueEffects.molten_edge.ventDrainPerTick);
        MoltenHeatComponent mainHeat = mainHand.getOrDefault(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
        MoltenHeatComponent offHeat = offHand.getOrDefault(ComponentTypeRegistry.MOLTEN_HEAT.get(), MoltenHeatComponent.DEFAULT);
        boolean mainVenting = mainHeat.isVentingAt(worldTime, ventDrain);
        boolean offVenting = offHeat.isVentingAt(worldTime, ventDrain);
        if (mainVenting != offVenting) {
            return mainVenting ? mainHand : offHand;
        }
        return mainHeat.heatAt(worldTime, ventDrain) >= offHeat.heatAt(worldTime, ventDrain)
                ? mainHand
                : offHand;
    }

    private static void renderChargePips(DrawContext context, MinecraftClient client, int stacks, int maxStacks, int filledColor, int emptyColor, int borderColor) {
        if (stacks <= 0) {
            return;
        }

        int clampedStacks = Math.min(stacks, maxStacks);
        int width = maxStacks * PIP_SIZE + (maxStacks - 1) * PIP_GAP;
        int x = context.getScaledWindowWidth() / 2 - width / 2;
        int y = context.getScaledWindowHeight() - 68;

        for (int i = 0; i < maxStacks; i++) {
            int pipX = x + i * (PIP_SIZE + PIP_GAP);
            int color = i < clampedStacks ? filledColor : emptyColor;
            context.fill(pipX, y, pipX + PIP_SIZE, y + PIP_SIZE, color);
            context.drawBorder(pipX - 1, y - 1, PIP_SIZE + 2, PIP_SIZE + 2, borderColor);
        }

        String label = clampedStacks + "/" + maxStacks;
        int labelX = context.getScaledWindowWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, labelX, y - 11, filledColor);
    }

    private void renderHeatBar(DrawContext context, MinecraftClient client, RenderTickCounter tickCounter, MoltenHeatComponent component) {
        long worldTime = client.world == null ? 0L : client.world.getTime();
        int ventDrain = Math.max(1, Config.uniqueEffects.molten_edge.ventDrainPerTick);
        float targetHeat = component.heatAt(worldTime, ventDrain);
        float renderTime = worldTime + tickCounter.getTickDelta(false);
        if (!simplyswords$heatDisplayInitialized) {
            simplyswords$displayedHeat = targetHeat;
            simplyswords$lastHeatRenderTime = renderTime;
            simplyswords$heatDisplayInitialized = true;
        } else {
            float elapsedTicks = renderTime - simplyswords$lastHeatRenderTime;
            if (elapsedTicks < 0.0F || elapsedTicks > 5.0F) {
                simplyswords$displayedHeat = targetHeat;
            } else {
                float smoothing = 1.0F - (float) Math.exp(-HEAT_SMOOTHING_RATE * elapsedTicks);
                simplyswords$displayedHeat += (targetHeat - simplyswords$displayedHeat) * smoothing;
                if (Math.abs(targetHeat - simplyswords$displayedHeat) < 0.01F) {
                    simplyswords$displayedHeat = targetHeat;
                }
            }
            simplyswords$lastHeatRenderTime = renderTime;
        }

        float fraction = Math.clamp(simplyswords$displayedHeat / MoltenHeatComponent.MAX_HEAT, 0.0F, 1.0F);
        float danger = Math.max(0.0F, (fraction - 0.75F) / 0.25F);
        float pulseSpeed = fraction >= 0.9F ? 0.72F : 0.34F;
        float pulse = 0.5F + 0.5F * (float) Math.sin(renderTime * pulseSpeed);
        float flash = fraction >= 1.0F ? 0.25F + pulse * 0.65F : danger * pulse * 0.28F;

        int fillColor = fraction <= 0.5F
                ? blendColor(HEAT_YELLOW_COLOR, HEAT_ORANGE_COLOR, fraction * 2.0F)
                : blendColor(HEAT_ORANGE_COLOR, HEAT_RED_COLOR, (fraction - 0.5F) * 2.0F);
        fillColor = blendColor(fillColor, 0xFFFFFFFF, flash);
        int borderColor = blendColor(HEAT_BORDER_COLOR, 0xFFFFD27A, danger * pulse * 0.55F);

        int x = context.getScaledWindowWidth() / 2 - HEAT_BAR_WIDTH / 2;
        int y = context.getScaledWindowHeight() - 68;
        int fillWidth = Math.round(HEAT_BAR_WIDTH * fraction);

        if (danger > 0.0F) {
            int glowAlpha = Math.clamp((int) ((28.0F + 74.0F * pulse) * danger), 0, 120);
            int glowColor = withAlpha(fillColor, glowAlpha);
            context.fill(x - 4, y - 4, x + HEAT_BAR_WIDTH + 4, y + HEAT_BAR_HEIGHT + 4, withAlpha(glowColor, glowAlpha / 3));
            context.fill(x - 2, y - 2, x + HEAT_BAR_WIDTH + 2, y + HEAT_BAR_HEIGHT + 2, glowColor);
        }

        context.fill(x, y, x + HEAT_BAR_WIDTH, y + HEAT_BAR_HEIGHT, HEAT_EMPTY_COLOR);
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + HEAT_BAR_HEIGHT, fillColor);
            if (fraction >= 0.9F) {
                context.fill(x, y, x + fillWidth, y + 2, withAlpha(0xFFFFFFFF, 45 + (int) (pulse * 80.0F)));
            }
        }
        context.drawBorder(x - 1, y - 1, HEAT_BAR_WIDTH + 2, HEAT_BAR_HEIGHT + 2, borderColor);

        Text labelText = Text.translatable(
                component.isVentingAt(worldTime, ventDrain)
                        ? "hud.simplyswords.molten_edge.venting"
                        : "hud.simplyswords.molten_edge.heat",
                Math.round(simplyswords$displayedHeat)
        );
        String label = labelText.getString();
        int labelX = context.getScaledWindowWidth() / 2 - client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, labelX, y - 11, fillColor);
    }

    private static int blendColor(int first, int second, float progress) {
        float t = Math.clamp(progress, 0.0F, 1.0F);
        int a = Math.round(((first >>> 24) & 0xFF) + (((second >>> 24) & 0xFF) - ((first >>> 24) & 0xFF)) * t);
        int r = Math.round(((first >>> 16) & 0xFF) + (((second >>> 16) & 0xFF) - ((first >>> 16) & 0xFF)) * t);
        int g = Math.round(((first >>> 8) & 0xFF) + (((second >>> 8) & 0xFF) - ((first >>> 8) & 0xFF)) * t);
        int b = Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int color, int alpha) {
        return Math.clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }
}
