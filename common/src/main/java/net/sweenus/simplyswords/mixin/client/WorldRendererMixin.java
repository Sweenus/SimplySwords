package net.sweenus.simplyswords.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.client.renderer.ModernFieldRenderer;
import net.sweenus.simplyswords.client.renderer.ObserverStatusVisualRenderer;
import net.sweenus.simplyswords.client.renderer.TerrainFieldOverlayRenderer;
import net.sweenus.simplyswords.client.renderer.TargetHighlight;
import net.sweenus.simplyswords.client.render.IonDeferredRenderController;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.WatcherBatEntity;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.custom.EmberIreSwordItem;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.item.custom.LichbladeSwordItem;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.WatcherWeaponType;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.world.BramblethornAbilityManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void simplyswords$beginTerrainOverlayFrame(
            RenderTickCounter tickCounter, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        TerrainFieldOverlayRenderer.beginWorldFrame();
        ObserverStatusVisualRenderer.beginWorldFrame();
        IonDeferredRenderController.beginFrame();
    }

    @Inject(method = "render", slice = @Slice(from = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/render/WorldRenderer;renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V")),
            at = @At(value = "FIELD", target =
                    "Lnet/minecraft/client/render/WorldRenderer;transparencyPostProcessor:Lnet/minecraft/client/gl/PostEffectProcessor;", ordinal = 0))
    private void simplyswords$drawDeferredIonFields(
            RenderTickCounter tickCounter, boolean renderBlockOutline,
            Camera camera, GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix, Matrix4f projectionMatrix,
            CallbackInfo ci) {
        IonDeferredRenderController.drawDeferred();
    }

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
            ModernFieldRenderer.renderImmolation(matrices, vertexConsumers, player.age, Math.max(0.75F, immolation.getAmplifier()));
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
            if (highlight.style() == TargetHighlight.Style.SOUL) {
                ModernFieldRenderer.renderSoulstealerTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderSoulstealerTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else if (highlight.style() == TargetHighlight.Style.EMBER) {
                ModernFieldRenderer.renderEmberTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderEmberTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else if (highlight.style() == TargetHighlight.Style.BRIMSTONE) {
                ModernFieldRenderer.renderBrimstoneTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderBrimstoneTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else if (highlight.style() == TargetHighlight.Style.WATCHER) {
                ModernFieldRenderer.renderWatcherTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderWatcherTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else if (highlight.style() == TargetHighlight.Style.WAX) {
                ModernFieldRenderer.renderWaxTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderWaxTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            } else if (highlight.style() == TargetHighlight.Style.BRAMBLE) {
                ModernFieldRenderer.renderBrambleTargetLine(matrices, vertexConsumers, player.age, targetOffset);
                ModernFieldRenderer.renderBrambleTargetRing(matrices, vertexConsumers, player.age, targetOffset, highlightedTarget.getWidth());
            }
        }

        vertexConsumers.draw(RenderLayer.getDebugQuads());
    }

    private TargetHighlight getReadyTarget(ClientPlayerEntity player) {
        LivingEntity emberTarget = getReadyEmberTarget(player);
        if (emberTarget != null) {
            return new TargetHighlight(emberTarget, TargetHighlight.Style.EMBER);
        }
        LivingEntity soulstealerTarget = getReadySoulstealerTarget(player);
        if (soulstealerTarget != null) {
            return new TargetHighlight(soulstealerTarget, TargetHighlight.Style.SOUL);
        }
        LivingEntity lichbladeTarget = getReadyLichbladeTarget(player);
        if (lichbladeTarget != null) {
            return new TargetHighlight(lichbladeTarget, TargetHighlight.Style.SOUL);
        }
        LivingEntity watcherTarget = getReadyWatcherTarget(player);
        if (watcherTarget != null) {
            return new TargetHighlight(watcherTarget, TargetHighlight.Style.WATCHER);
        }
        LivingEntity brimstoneTarget = getReadyBrimstoneTarget(player);
        if (brimstoneTarget != null) {
            return new TargetHighlight(brimstoneTarget, TargetHighlight.Style.BRIMSTONE);
        }
        LivingEntity waxweaverTarget = getReadyWaxweaverTarget(player);
        if (waxweaverTarget != null) {
            return new TargetHighlight(waxweaverTarget, TargetHighlight.Style.WAX);
        }
        LivingEntity brambleTarget = getReadyBrambleTarget(player);
        if (brambleTarget != null) {
            return new TargetHighlight(brambleTarget, TargetHighlight.Style.BRAMBLE);
        }
        return null;
    }

    private LivingEntity getReadyEmberTarget(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.EMBERBLADE.get())
                || !AwakeningApi.isAbilityUnlocked(stack)
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return null;
        }
        return EmberIreSwordItem.findPlayerTarget(player);
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
        if (!(stack.getItem() instanceof LichbladeSwordItem)
                || !AwakeningApi.isAbilityUnlocked(stack)
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

    private LivingEntity getReadyWaxweaverTarget(ClientPlayerEntity player) {
        ItemStack stack = getHeldWaxweaver(player);
        if (stack.isEmpty()
                || !AwakeningApi.isAbilityUnlocked(stack)
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || stack.getDamage() >= stack.getMaxDamage() - 1
                || WaxweaverEncasementManager.isEncased(player)) {
            return null;
        }

        return WaxweaverEncasementManager.findPlayerTarget(player);
    }

    private LivingEntity getReadyBrambleTarget(ClientPlayerEntity player) {
        ItemStack stack = getHeldBramblethorn(player);
        if (stack.isEmpty()
                || !AwakeningApi.isAbilityUnlocked(stack)
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return null;
        }
        return BramblethornAbilityManager.findPlayerTarget(player);
    }

    private LivingEntity getReadyWatcherTarget(ClientPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.WATCHER_CLAYMORE.get())
                || player.getItemCooldownManager().isCoolingDown(stack.getItem())
                || player.isUsingItem()
                || stack.getDamage() >= stack.getMaxDamage() - 1) {
            return null;
        }

        double range = Math.max(1.0, Config.uniqueEffects.watcher.activationRange);
        List<WatcherBatEntity> markBats = player.getWorld().getEntitiesByClass(
                WatcherBatEntity.class,
                player.getBoundingBox().expand(range + 2.0),
                bat -> bat.getWatcherOwnerId() == player.getId()
                        && bat.getWatcherWeaponType() == WatcherWeaponType.CLAYMORE
                        && bat.getWatcherMode() == WatcherBatEntity.MODE_MARK
        );
        if (markBats.isEmpty()) {
            return null;
        }

        Set<Integer> markedTargetIds = new HashSet<>();
        for (WatcherBatEntity bat : markBats) {
            if (bat.getWatcherTargetId() >= 0) {
                markedTargetIds.add(bat.getWatcherTargetId());
            }
        }

        LivingEntity preferred = StealSwordItem.findLenientTarget(player, range);
        if (preferred != null && markedTargetIds.contains(preferred.getId())
                && HelperMethods.checkAbilityTarget(preferred, player)) {
            return preferred;
        }

        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int targetId : markedTargetIds) {
            Entity entity = player.getWorld().getEntityById(targetId);
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()
                    || !HelperMethods.checkAbilityTarget(target, player)) {
                continue;
            }
            double distance = target.squaredDistanceTo(player);
            if (distance <= range * range && distance < nearestDistance) {
                nearest = target;
                nearestDistance = distance;
            }
        }
        return nearest;
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

    private ItemStack getHeldWaxweaver(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(ItemsRegistry.WAXWEAVER.get())) {
            return mainHandStack;
        }

        ItemStack offHandStack = player.getOffHandStack();
        if (offHandStack.isOf(ItemsRegistry.WAXWEAVER.get())) {
            return offHandStack;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack getHeldBramblethorn(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(ItemsRegistry.BRAMBLETHORN.get())) {
            return mainHandStack;
        }
        ItemStack offHandStack = player.getOffHandStack();
        return offHandStack.isOf(ItemsRegistry.BRAMBLETHORN.get())
                ? offHandStack : ItemStack.EMPTY;
    }

}
