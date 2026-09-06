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
import net.sweenus.simplyswords.world.DawnquiverAbilityManager;
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
    private static final int BLOOD_FRENZY_COLOR = 0xFFE12A42;
    private static final int BLOOD_FRENZY_EMPTY_COLOR = 0x66520B18;
    private static final int BLOOD_FRENZY_BORDER_COLOR = 0xCC26030B;
    private static final int DAWN_CHORUS_COLOR = 0xFFFFD968;
    private static final int DAWN_CHORUS_EMPTY_COLOR = 0x665B4514;
    private static final int DAWN_CHORUS_BORDER_COLOR = 0xCC6D4D0B;
    private static final int DAWN_DRAW_BAR_WIDTH = 112;
    private static final int DAWN_DRAW_BAR_HEIGHT = 7;
    private static final int DAWN_DRAW_NEUTRAL_COLOR = 0xFF837866;
    private static final int DAWN_DRAW_VOLLEY_COLOR = 0xFFFFC84A;
    private static final int DAWN_DRAW_SUNLANCE_COLOR = 0xFF71E6FF;
    private static final int DAWN_DRAW_SERAPHIC_COLOR = 0xFFFFF1A8;
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
        ItemStack bloodwake = simplyswords$selectBloodwake(client);
        if (!bloodwake.isEmpty()) {
            int stacks = bloodwake.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
            renderChargePips(context, client, stacks, 5, BLOOD_FRENZY_COLOR,
                    BLOOD_FRENZY_EMPTY_COLOR, BLOOD_FRENZY_BORDER_COLOR,
                    Text.translatable("hud.simplyswords.bloodwake_frenzy", Math.min(stacks, 5), 5).getString());
            return;
        }
        ItemStack dawnquiver = simplyswords$selectDawnquiver(client);
        if (!dawnquiver.isEmpty()) {
            int stacks = dawnquiver.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(),
                    StoredChargeComponent.DEFAULT).charge();
            int maximum = Math.max(1, dawnquiver.getOrDefault(
                    ComponentTypeRegistry.DAWN_CHORUS_CAPACITY.get(),
                    Config.uniqueEffects.dawnquiver.maxChorus));
            renderDawnquiverHud(context, client, tickCounter, dawnquiver, stacks, maximum);
            return;
        }
        if (stack.isOf(ItemsRegistry.SOULSTEALER.get())) {
            int stacks = stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
            int maximum = Math.max(1, stack.getOrDefault(ComponentTypeRegistry.SOUL_DEBT_CAPACITY.get(),
                    Config.uniqueEffects.soulstealer.maxStacks));
            renderChargePips(context, client, stacks, maximum, SOUL_DEBT_COLOR, SOUL_DEBT_EMPTY_COLOR, SOUL_DEBT_BORDER_COLOR);
            return;
        }
        if (stack.isOf(ItemsRegistry.STORMBRINGER.get())) {
            ParryComponent stormCharge = stack.getOrDefault(ComponentTypeRegistry.PARRY.get(), ParryComponent.DEFAULT);
            int charges = stormCharge.stormCharges();
            int maximum = stormCharge.effectiveStormChargeCapacity(Config.uniqueEffects.stormbringer.maxStormCharges);
            renderChargePips(context, client, charges, maximum, STORM_CHARGE_COLOR, STORM_CHARGE_EMPTY_COLOR, STORM_CHARGE_BORDER_COLOR);
            return;
        }
    }

    @Unique
    private static ItemStack simplyswords$selectBloodwake(MinecraftClient client) {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        boolean mainBloodwake = main.isOf(ItemsRegistry.BLOODWAKE.get());
        boolean offBloodwake = off.isOf(ItemsRegistry.BLOODWAKE.get());
        if (!mainBloodwake) {
            return offBloodwake ? off : ItemStack.EMPTY;
        }
        if (!offBloodwake) {
            return main;
        }
        int mainStacks = main.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
        int offStacks = off.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
        return mainStacks >= offStacks ? main : off;
    }

    @Unique
    private static ItemStack simplyswords$selectDawnquiver(MinecraftClient client) {
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        if (client.player.isUsingItem()) {
            ItemStack active = client.player.getActiveItem();
            if (active.isOf(ItemsRegistry.DAWNQUIVER.get())) {
                return active;
            }
        }
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        boolean mainDawnquiver = main.isOf(ItemsRegistry.DAWNQUIVER.get());
        boolean offDawnquiver = off.isOf(ItemsRegistry.DAWNQUIVER.get());
        if (!mainDawnquiver) {
            return offDawnquiver ? off : ItemStack.EMPTY;
        }
        if (!offDawnquiver) {
            return main;
        }
        int mainStacks = main.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(),
                StoredChargeComponent.DEFAULT).charge();
        int offStacks = off.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(),
                StoredChargeComponent.DEFAULT).charge();
        return mainStacks >= offStacks ? main : off;
    }

    @Unique
    private static void renderDawnquiverHud(DrawContext context, MinecraftClient client,
                                             RenderTickCounter tickCounter, ItemStack stack,
                                             int stacks, int maximum) {
        int clampedStacks = Math.clamp(stacks, 0, maximum);
        int pipWidth = maximum * PIP_SIZE + (maximum - 1) * PIP_GAP;
        int pipX = -pipWidth / 2;

        pushWeaponHudTransform(context);
        for (int i = 0; i < maximum; i++) {
            int x = pipX + i * (PIP_SIZE + PIP_GAP);
            int color = i < clampedStacks ? DAWN_CHORUS_COLOR : DAWN_CHORUS_EMPTY_COLOR;
            context.fill(x, 0, x + PIP_SIZE, PIP_SIZE, color);
            context.drawBorder(x - 1, -1, PIP_SIZE + 2, PIP_SIZE + 2, DAWN_CHORUS_BORDER_COLOR);
        }

        String chorusLabel = Text.translatable("hud.simplyswords.dawnquiver_chorus",
                clampedStacks, maximum).getString();
        context.drawTextWithShadow(client.textRenderer, chorusLabel,
                -client.textRenderer.getWidth(chorusLabel) / 2, -11, DAWN_CHORUS_COLOR);

        boolean drawing = client.player != null
                && client.player.isUsingItem()
                && client.player.getActiveItem().isOf(ItemsRegistry.DAWNQUIVER.get());
        if (drawing) {
            renderDawnquiverDrawBar(context, client, tickCounter, stack, clampedStacks);
        }
        context.getMatrices().pop();
    }

    @Unique
    private static void renderDawnquiverDrawBar(DrawContext context, MinecraftClient client,
                                                 RenderTickCounter tickCounter, ItemStack stack,
                                                 int chorus) {
        int maximumUseTime = stack.getMaxUseTime(client.player);
        float elapsed = Math.max(0, maximumUseTime - client.player.getItemUseTimeLeft())
                + tickCounter.getTickDelta(false);
        float rawProgress = MathHelper.clamp(
                elapsed / Math.max(4.0F, Config.uniqueEffects.dawnquiver.drawDuration), 0.0F, 1.0F);
        float maximumProgress = DawnquiverAbilityManager.maximumDrawProgress(stack);
        float progress = DawnquiverAbilityManager.capDrawProgress(stack, rawProgress);
        float minimum = MathHelper.clamp((float) Config.uniqueEffects.dawnquiver.minimumDraw, 0.0F, 1.0F);
        float piercing = MathHelper.clamp((float) Config.uniqueEffects.dawnquiver.piercingThreshold,
                minimum, 1.0F);
        float full = MathHelper.clamp((float) Config.uniqueEffects.dawnquiver.fullDrawThreshold,
                piercing, 1.0F);
        float renderTime = (client.world == null ? 0.0F : client.world.getTime())
                + tickCounter.getTickDelta(false);
        float pulse = 0.5F + 0.5F * (float) Math.sin(renderTime * 0.28F);

        int tier = DawnquiverAbilityManager.affordableDrawTier(stack, progress);
        boolean atCap = rawProgress >= maximumProgress - 1.0E-4F;
        int activeColor = tier < 0 ? DAWN_DRAW_NEUTRAL_COLOR
                : tier == 0 ? DAWN_DRAW_VOLLEY_COLOR
                : tier == 1 ? DAWN_DRAW_SUNLANCE_COLOR
                : DAWN_DRAW_SERAPHIC_COLOR;
        int barX = -DAWN_DRAW_BAR_WIDTH / 2;
        int barY = -30;

        if (tier == 2) {
            int glowAlpha = 18 + Math.round(pulse * 34.0F);
            context.fill(barX - 3, barY - 3,
                    barX + DAWN_DRAW_BAR_WIDTH + 3, barY + DAWN_DRAW_BAR_HEIGHT + 3,
                    withAlpha(DAWN_DRAW_SERAPHIC_COLOR, glowAlpha));
        }

        renderDawnquiverBarSegment(context, barX, barY, 0.0F, minimum,
                progress, DAWN_DRAW_NEUTRAL_COLOR);
        renderDawnquiverBarSegment(context, barX, barY, minimum, piercing,
                progress, DAWN_DRAW_VOLLEY_COLOR);
        renderDawnquiverBarSegment(context, barX, barY, piercing, full,
                progress, DAWN_DRAW_SUNLANCE_COLOR);
        renderDawnquiverBarSegment(context, barX, barY, full, 1.0F,
                progress, DAWN_DRAW_SERAPHIC_COLOR);
        int capX = barX + Math.round(maximumProgress * DAWN_DRAW_BAR_WIDTH);
        if (capX < barX + DAWN_DRAW_BAR_WIDTH) {
            context.fill(capX, barY, barX + DAWN_DRAW_BAR_WIDTH,
                    barY + DAWN_DRAW_BAR_HEIGHT, 0x66000000);
        }

        context.drawBorder(barX - 1, barY - 1,
                DAWN_DRAW_BAR_WIDTH + 2, DAWN_DRAW_BAR_HEIGHT + 2, DAWN_CHORUS_BORDER_COLOR);
        drawDawnquiverThreshold(context, barX, barY, minimum);
        drawDawnquiverThreshold(context, barX, barY, piercing);
        drawDawnquiverThreshold(context, barX, barY, full);

        float activeStart = tier < 0 ? 0.0F : tier == 0 ? minimum : tier == 1 ? piercing : full;
        float activeEnd = tier < 0 ? minimum : tier == 0 ? piercing : tier == 1 ? full : 1.0F;
        int activeX = barX + Math.round(activeStart * DAWN_DRAW_BAR_WIDTH);
        int activeEndX = barX + Math.round(activeEnd * DAWN_DRAW_BAR_WIDTH);
        int highlight = blendColor(activeColor, 0xFFFFFFFF, 0.25F + pulse * 0.3F);
        if (activeEndX > activeX) {
            context.fill(activeX, barY - 2, activeEndX, barY - 1, highlight);
            context.fill(activeX, barY + DAWN_DRAW_BAR_HEIGHT + 1,
                    activeEndX, barY + DAWN_DRAW_BAR_HEIGHT + 2, highlight);
        }

        int markerX = Math.clamp(barX + Math.round(progress * DAWN_DRAW_BAR_WIDTH),
                barX, barX + DAWN_DRAW_BAR_WIDTH);
        context.fill(markerX - 1, barY - 4, markerX + 2, barY - 3, 0xFFFFFFFF);
        context.fill(markerX, barY - 3, markerX + 1,
                barY + DAWN_DRAW_BAR_HEIGHT + 3, 0xFFFFFFFF);

        Text state;
        int labelColor = activeColor;
        if (tier < 0) {
            state = chorus <= 0 && atCap
                    ? Text.translatable("hud.simplyswords.dawnquiver.base_ready")
                    : Text.translatable("hud.simplyswords.dawnquiver.drawing");
        } else {
            int cost = tier + 1;
            Text tierName = Text.translatable(tier == 0
                    ? "hud.simplyswords.dawnquiver.dawn_volley"
                    : tier == 1
                    ? "hud.simplyswords.dawnquiver.sunlance"
                    : "hud.simplyswords.dawnquiver.seraphic_chorus");
            state = Text.translatable("hud.simplyswords.dawnquiver.ready", tierName, cost);
        }
        String label = state.getString();
        context.drawTextWithShadow(client.textRenderer, label,
                -client.textRenderer.getWidth(label) / 2, barY - 13, labelColor);
    }

    @Unique
    private static void renderDawnquiverBarSegment(DrawContext context, int barX, int barY,
                                                    float start, float end, float progress, int color) {
        int startX = barX + Math.round(start * DAWN_DRAW_BAR_WIDTH);
        int endX = barX + Math.round(end * DAWN_DRAW_BAR_WIDTH);
        if (endX <= startX) {
            return;
        }
        context.fill(startX, barY, endX, barY + DAWN_DRAW_BAR_HEIGHT, withAlpha(color, 52));
        float filledEnd = MathHelper.clamp(progress, start, end);
        int filledEndX = barX + Math.round(filledEnd * DAWN_DRAW_BAR_WIDTH);
        if (filledEndX > startX) {
            context.fill(startX, barY, filledEndX, barY + DAWN_DRAW_BAR_HEIGHT, color);
            context.fill(startX, barY, filledEndX, barY + 1,
                    blendColor(color, 0xFFFFFFFF, 0.38F));
        }
    }

    @Unique
    private static void drawDawnquiverThreshold(DrawContext context, int barX, int barY, float threshold) {
        if (threshold <= 0.0F || threshold >= 1.0F) {
            return;
        }
        int x = barX + Math.round(threshold * DAWN_DRAW_BAR_WIDTH);
        context.fill(x, barY, x + 1, barY + DAWN_DRAW_BAR_HEIGHT, 0xE6FFFFFF);
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
        renderChargePips(context, client, stacks, maxStacks, filledColor, emptyColor, borderColor,
                Math.min(stacks, maxStacks) + "/" + maxStacks);
    }

    @Unique
    private static void renderChargePips(DrawContext context, MinecraftClient client, int stacks, int maxStacks,
                                         int filledColor, int emptyColor, int borderColor, String label) {
        renderChargePips(context, client, stacks, maxStacks, filledColor, emptyColor, borderColor, label, false);
    }

    @Unique
    private static void renderChargePips(DrawContext context, MinecraftClient client, int stacks, int maxStacks,
                                         int filledColor, int emptyColor, int borderColor, String label,
                                         boolean showWhenEmpty) {
        if (stacks <= 0 && !showWhenEmpty) {
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
