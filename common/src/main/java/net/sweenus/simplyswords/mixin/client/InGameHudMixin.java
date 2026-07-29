package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.CaelestisBreachVisualEntity;
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
    private static final int WEAPON_HUD_BOTTOM_OFFSET = 68;

    @Unique
    private float simplyswords$displayedHeat;
    @Unique
    private float simplyswords$lastHeatRenderTime;
    @Unique
    private boolean simplyswords$heatDisplayInitialized;
    @Unique
    private float simplyswords$breachTint;
    @Unique
    private float simplyswords$lastBreachRenderTime;

    @Inject(method = "render", at = @At("HEAD"))
    private void simplyswords$renderAstralBreachTint(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !Config.general.enableModernFieldEffects) {
            simplyswords$breachTint = 0.0F;
            simplyswords$lastBreachRenderTime = 0.0F;
            return;
        }

        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        double searchRadius = Math.max(2.0, Config.uniqueEffects.caelestis.maxRadius + 2.0);
        Box search = new Box(cameraPos, cameraPos).expand(
                searchRadius,
                Math.max(2.0, Config.uniqueEffects.caelestis.verticalRange + 2.0),
                searchRadius);
        float target = 0.0F;
        boolean collapsing = false;
        for (CaelestisBreachVisualEntity breach : client.world.getEntitiesByClass(
                CaelestisBreachVisualEntity.class, search, entity -> entity.getRadius() > 0.05F)) {
            double dx = cameraPos.x - breach.getX();
            double dz = cameraPos.z - breach.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > breach.getRadius()
                    || Math.abs(cameraPos.y - breach.getY())
                    > breach.getVerticalRange()) {
                continue;
            }
            float edge = MathHelper.clamp((float) ((breach.getRadius() - distance) / 2.25), 0.0F, 1.0F);
            float radiusStrength = MathHelper.clamp(
                    breach.getRadius() / breach.getMaxRadius(),
                    0.0F, 1.0F);
            float strength = (0.35F + edge * 0.65F) * (0.45F + radiusStrength * 0.55F);
            if (strength > target) {
                target = strength;
                collapsing = breach.getPhase() == CaelestisBreachVisualEntity.PHASE_COLLAPSING;
            }
        }

        float renderTime = client.world.getTime() + tickCounter.getTickDelta(false);
        float elapsed = this.simplyswords$lastBreachRenderTime == 0.0F
                ? 1.0F : MathHelper.clamp(renderTime - this.simplyswords$lastBreachRenderTime, 0.0F, 5.0F);
        float smoothing = 1.0F - (float) Math.exp(-0.28F * elapsed);
        this.simplyswords$breachTint += (target - this.simplyswords$breachTint) * smoothing;
        this.simplyswords$lastBreachRenderTime = renderTime;
        if (this.simplyswords$breachTint < 0.005F) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float pulse = 0.5F + 0.5F * (float) Math.sin(renderTime * (collapsing ? 0.52F : 0.18F));
        int centerAlpha = MathHelper.clamp(
                (int) ((collapsing ? 20.0F + pulse * 10.0F : 15.0F + pulse * 5.0F)
                        * this.simplyswords$breachTint), 0, 36);
        int centerColor = (centerAlpha << 24) | (collapsing ? 0x4A0611 : 0x21052E);
        context.fill(0, 0, width, height, centerColor);

        for (int layer = 0; layer < 6; layer++) {
            int insetX = layer * Math.max(2, width / 90);
            int insetY = layer * Math.max(2, height / 70);
            int thicknessX = Math.max(3, width / 38);
            int thicknessY = Math.max(3, height / 30);
            int alpha = MathHelper.clamp(
                    (int) ((36.0F - layer * 4.0F + pulse * 8.0F)
                            * this.simplyswords$breachTint), 0, 58);
            int color = (alpha << 24) | (collapsing ? 0x720A1D : 0x360848);
            context.fill(insetX, insetY, width - insetX, insetY + thicknessY, color);
            context.fill(insetX, height - insetY - thicknessY, width - insetX, height - insetY, color);
            context.fill(insetX, insetY + thicknessY, insetX + thicknessX,
                    height - insetY - thicknessY, color);
            context.fill(width - insetX - thicknessX, insetY + thicknessY, width - insetX,
                    height - insetY - thicknessY, color);
        }
    }

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
        int x = -width / 2;
        int y = 0;

        pushWeaponHudTransform(context);
        for (int i = 0; i < maxStacks; i++) {
            int pipX = x + i * (PIP_SIZE + PIP_GAP);
            int color = i < clampedStacks ? filledColor : emptyColor;
            context.fill(pipX, y, pipX + PIP_SIZE, y + PIP_SIZE, color);
            context.drawBorder(pipX - 1, y - 1, PIP_SIZE + 2, PIP_SIZE + 2, borderColor);
        }

        String label = clampedStacks + "/" + maxStacks;
        int labelX = -client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, labelX, y - 11, filledColor);
        context.getMatrices().pop();
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

        int x = -HEAT_BAR_WIDTH / 2;
        int y = 0;
        int fillWidth = Math.round(HEAT_BAR_WIDTH * fraction);

        pushWeaponHudTransform(context);
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
        int labelX = -client.textRenderer.getWidth(label) / 2;
        context.drawTextWithShadow(client.textRenderer, label, labelX, y - 11, fillColor);
        context.getMatrices().pop();
    }

    private static void pushWeaponHudTransform(DrawContext context) {
        context.getMatrices().push();
        context.getMatrices().translate(
                context.getScaledWindowWidth() / 2 + Config.gui.xOffset,
                context.getScaledWindowHeight() - WEAPON_HUD_BOTTOM_OFFSET + Config.gui.yOffset,
                0.0F
        );
        float scale = Math.clamp(Config.gui.scale, 0.25F, 4.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
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
