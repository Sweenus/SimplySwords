package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.renderer.BattleStandardFieldRenderer;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void simplyswords$renderFirstPersonImmolationField(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        float tickDelta = tickCounter.getTickDelta(false);
        Vec3d cameraPos = camera.getPos();
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY()) - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ()) - cameraPos.z;

        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        matrices.translate(x, y, z);

        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();

        StatusEffectInstance immolation = player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.IMMOLATION));
        if (immolation != null && client.options.getPerspective().isFirstPerson()) {
            BattleStandardFieldRenderer.renderImmolation(matrices, vertexConsumers, player.age, Math.max(0.75F, immolation.getAmplifier()));
        }

        TargetHighlight highlight = getReadyTarget(player);
        if (highlight != null) {
            LivingEntity highlightedTarget = highlight.target();
            Vec3d playerPos = new Vec3d(
                    MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                    MathHelper.lerp(tickDelta, player.prevY, player.getY()),
                    MathHelper.lerp(tickDelta, player.prevZ, player.getZ()));
            Vec3d targetPos = new Vec3d(
                    MathHelper.lerp(tickDelta, highlightedTarget.prevX, highlightedTarget.getX()),
                    MathHelper.lerp(tickDelta, highlightedTarget.prevY, highlightedTarget.getY()),
                    MathHelper.lerp(tickDelta, highlightedTarget.prevZ, highlightedTarget.getZ()));
            Vec3d targetOffset = targetPos.subtract(playerPos);
            if (highlight.brimstone()) {
                BattleStandardFieldRenderer.renderBrimstoneTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                BattleStandardFieldRenderer.renderBrimstoneTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else {
                BattleStandardFieldRenderer.renderSoulstealerTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                BattleStandardFieldRenderer.renderSoulstealerTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            }
        }

        vertexConsumers.draw(RenderLayer.getDebugQuads());
    }

    private TargetHighlight getReadyTarget(ClientPlayerEntity player) {
        LivingEntity soulstealerTarget = getReadySoulstealerTarget(player);
        if (soulstealerTarget != null) {
            return new TargetHighlight(soulstealerTarget, false);
        }
        LivingEntity lichbladeTarget = getReadyLichbladeTarget(player);
        if (lichbladeTarget != null) {
            return new TargetHighlight(lichbladeTarget, false);
        }
        LivingEntity brimstoneTarget = getReadyBrimstoneTarget(player);
        return brimstoneTarget == null ? null : new TargetHighlight(brimstoneTarget, true);
    }

    private LivingEntity getReadySoulstealerTarget(ClientPlayerEntity player) {
        ItemStack stack = getHeldSoulstealer(player);
        if (stack.isEmpty()
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge() <= 0) {
            return null;
        }

        return StealSwordItem.findSoulstealerTarget(player);
    }

    private LivingEntity getReadyLichbladeTarget(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if ((!stack.isOf(ItemsRegistry.WAKING_LICHBLADE.get()) && !stack.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get()))
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || player.isUsingItem()
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return null;
        }

        return StealSwordItem.findLenientTarget(player, Config.uniqueEffects.lichblade.range);
    }

    private LivingEntity getReadyBrimstoneTarget(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.BRIMSTONE_CLAYMORE.get())
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return null;
        }

        return StealSwordItem.findLenientTarget(player, Config.uniqueEffects.brimstone_claymore.range);
    }

    private ItemStack getHeldSoulstealer(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(ItemsRegistry.SOULSTEALER.get())) {
            return mainHandStack;
        }

        ItemStack offHandStack = player.getOffHandStack();
        if (offHandStack.isOf(ItemsRegistry.SOULSTEALER.get())) {
            return offHandStack;
        }
        return ItemStack.EMPTY;
    }

    private record TargetHighlight(LivingEntity target, boolean brimstone) {
    }
}
