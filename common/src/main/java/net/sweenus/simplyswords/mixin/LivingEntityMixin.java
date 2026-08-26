package net.sweenus.simplyswords.mixin;


import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.IncapacitatingStatusEffectRegistry;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatMethods;
import net.sweenus.simplyswords.effect.FlameSeedEffect;
import net.sweenus.simplyswords.item.custom.StormbringerSwordItem;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.RunicSlashManager;
import net.sweenus.simplyswords.world.WatcherAbilityManager;
import net.sweenus.simplyswords.world.BramblethornAbilityManager;
import net.sweenus.simplyswords.world.BloodwakeAbilityManager;
import net.sweenus.simplyswords.world.IonboundStormscaleAbilityManager;
import net.sweenus.simplyswords.world.HivemindSwarmManager;
import net.sweenus.simplyswords.world.MagispearAbilityManager;
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;
import net.sweenus.simplyswords.world.ObserverStatusEffectSyncManager;
import net.sweenus.simplyswords.world.Phase4PassiveManager;
import net.sweenus.simplyswords.world.Phase4LichbladeManager;
import net.sweenus.simplyswords.world.Phase4StandardManager;
import net.sweenus.simplyswords.world.Phase5CombatManager;
import net.sweenus.simplyswords.world.Phase7CombatManager;
import net.sweenus.simplyswords.world.Phase8CombatManager;
import net.sweenus.simplyswords.world.DreadwhisperAbilityManager;
import net.sweenus.simplyswords.world.GloamMechanicsManager;
import net.sweenus.simplyswords.world.SoulPyreAbilityManager;
import net.sweenus.simplyswords.world.StormsEdgeAbilityManager;
import net.sweenus.simplyswords.world.ThunderbrandAbilityManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sweenus.simplyswords.SimplySwords.minimumEldritchEndVersion;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected boolean jumping;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void simplyswords$suppressIncapacitatedMovementInput(CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        boolean incapacitated = IncapacitatingStatusEffectRegistry.isIncapacitated(living);
        if (!incapacitated && !GloamMechanicsManager.isGrasped(living)) return;
        living.forwardSpeed = 0.0F;
        living.sidewaysSpeed = 0.0F;
        living.upwardSpeed = 0.0F;
        jumping = false;
        if (incapacitated) {
            living.stopUsingItem();
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void simplyswords$clampIncapacitatedVelocity(CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (!IncapacitatingStatusEffectRegistry.isIncapacitated(living)) return;
        var velocity = living.getVelocity();
        living.setVelocity(0.0, Math.min(0.0, velocity.y), 0.0);
        living.velocityModified = true;
    }

    @Inject(method = "onStatusEffectApplied", at = @At("TAIL"))
    private void simplyswords$syncObserverEffectApplied(StatusEffectInstance effect, Entity source, CallbackInfo ci) {
        ObserverStatusEffectSyncManager.syncApplied((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "onStatusEffectUpgraded", at = @At("TAIL"))
    private void simplyswords$syncObserverEffectUpgraded(StatusEffectInstance effect, boolean reapplyEffect,
                                                          Entity source, CallbackInfo ci) {
        ObserverStatusEffectSyncManager.syncApplied((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "onStatusEffectRemoved", at = @At("TAIL"))
    private void simplyswords$syncObserverEffectRemoved(StatusEffectInstance effect, CallbackInfo ci) {
        ObserverStatusEffectSyncManager.syncRemoved((LivingEntity) (Object) this, effect);
    }

    @Inject(at = @At("HEAD"), method = "tryUseTotem", cancellable = true)
    public void simplyswords$tryRevive(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if(!livingEntity.getWorld().isClient()) {
            ItemStack mainhand = livingEntity.getStackInHand(Hand.MAIN_HAND);
            if (mainhand.getItem() instanceof RevivalWeapon revivalWeapon) {
                if(revivalWeapon.canRevive(livingEntity, mainhand, source)) {
                    livingEntity.setHealth(revivalWeapon.getReviveHealth(livingEntity, mainhand, source));
                    revivalWeapon.postRevive(livingEntity, mainhand, source);
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    public void simplyswords$handleIncomingAbilityDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!(livingEntity instanceof ServerPlayerEntity)
                && IonboundStormscaleAbilityManager.handleIncomingDamage(livingEntity, source, amount)) {
            cir.setReturnValue(false);
            return;
        }
        if (!(livingEntity instanceof ServerPlayerEntity)
                && MagispearAbilityManager.blocksIncomingDamage(livingEntity, source)) {
            cir.setReturnValue(false);
            return;
        }
        if (!(livingEntity instanceof ServerPlayerEntity)
                && StormsEdgeAbilityManager.blocksIncomingDamage(livingEntity, source)) {
            cir.setReturnValue(false);
            return;
        }
        if (!(livingEntity instanceof ServerPlayerEntity)
                && DreadwhisperAbilityManager.blocksIncomingDamage(livingEntity, source)) {
            cir.setReturnValue(false);
            return;
        }
        if (!livingEntity.getWorld().isClient()
                && !(livingEntity instanceof ServerPlayerEntity)
                && ThunderbrandAbilityManager.handleIncomingDamage(livingEntity, source, amount)) {
            cir.setReturnValue(false);
            return;
        }
        if (livingEntity.getWorld().isClient()
                || livingEntity instanceof ServerPlayerEntity
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(livingEntity instanceof MobEntity mob)
                || !(livingEntity.getWorld() instanceof net.minecraft.server.world.ServerWorld world)
                || !(source.getAttacker() instanceof LivingEntity attacker)
                || attacker != mob.getTarget()
                || !HelperMethods.checkAbilityTarget(attacker, mob)) {
            return;
        }

        ItemStack stack = mob.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.STORMBRINGER.get())) {
            return;
        }

        WeaponAbilityContext context = WeaponAbilityContext.of(world, stack, mob, null, attacker,
                Hand.MAIN_HAND, WeaponAbilityActivationSource.MOB);
        if (SimplySwordsAPI.tryActivateWeaponAbility(context)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "modifyAppliedDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float simplyswords$modifyDamageAmount(float amount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (source.getAttacker() instanceof LivingEntity attacker
                && WaxweaverEncasementManager.isEncased(attacker)) {
            return 0.0F;
        }
        if (!livingEntity.getWorld().isClient()) {
            amount = MoltenEdgeAbilityManager.modifyOutgoingDamage(source, amount);
            amount = Phase4PassiveManager.modifyOutgoingDamage(livingEntity, source, amount);
            StatusEffectInstance voidcloakEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            StatusEffectInstance ribbonwrathEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONWRATH));
            StatusEffectInstance soulTetherEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER));
            if (voidcloakEffect != null) {
                amount = net.sweenus.simplyswords.world.Phase10WeaponManager.modifyVoidcloakDamage(
                        livingEntity, source, amount, voidcloakEffect);
            }
            if (ribbonwrathEffect != null) {
                float reductionFactor = 0.85f;
                amount *= reductionFactor;
                amount = net.sweenus.simplyswords.world.Phase10WeaponManager.modifyIncomingDamage(
                        livingEntity, source, amount);
            }
            if (soulTetherEffect != null) amount = SoulPyreAbilityManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = WeaponImplicitRegistry.modifyDamage(livingEntity, source, amount);
            amount = DreadwhisperAbilityManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = MoltenEdgeAbilityManager.modifyIncomingDamage(livingEntity, amount);
            amount = WatcherAbilityManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = net.sweenus.simplyswords.world.Phase2CombatStateManager.modifyIncomingDamage(
                    livingEntity, source, amount);
            amount = Phase4StandardManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = Phase4PassiveManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = Phase5CombatManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = WaxweaverEncasementManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = BramblethornAbilityManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = HivemindSwarmManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = Phase7CombatManager.modifyIncomingDamage(livingEntity, source, amount);
            amount = Phase8CombatManager.modifyIncomingDamage(livingEntity, source, amount);
        }
        return amount;
    }

    @Inject(at = @At("TAIL"), method = "damage")
    public void simplyswords$applyWeaponImplicitOnDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (Boolean.TRUE.equals(cir.getReturnValue()) && !livingEntity.getWorld().isClient()) {
            BramblethornAbilityManager.onBoundTargetDamaged(livingEntity, source, amount);
            MoltenEdgeAbilityManager.gainHeatFromIncomingDamage(livingEntity, amount, true);
            WeaponImplicitRegistry.onDamageApplied(livingEntity, source, amount);
            BloodwakeAbilityManager.onTargetDamaged(livingEntity, source);
            Phase4PassiveManager.onDamageApplied(livingEntity, source);
            Phase4LichbladeManager.onOwnerDamaged(livingEntity);
            Phase5CombatManager.onDamageApplied(livingEntity, source);
            HivemindSwarmManager.onOwnerDamaged(livingEntity, source);
            Phase7CombatManager.onDamageApplied(livingEntity, source);
            if (source.isIn(DamageTypeTags.IS_PLAYER_ATTACK) && source.getAttacker() instanceof ServerPlayerEntity player) {
                ItemStack stack = source.getWeaponStack();
                if (stack == null || !stack.isOf(ItemsRegistry.STORMBRINGER.get())) {
                    stack = player.getMainHandStack();
                }
                StormbringerSwordItem.tryTriggerChainLightningOnMeleeDamage(stack, livingEntity, player);
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!livingEntity.getWorld().isClient()) {

            if (SimplySwords.passVersionCheck("eldritch_end", minimumEldritchEndVersion)
                    && Registries.STATUS_EFFECT.get(Identifier.of("simplyswords:voidhunger")) != null)
                EldritchEndCompatMethods.generateVoidcloakStacks(livingEntity);
        }
    }

    @Inject(at = @At("HEAD"), method = "onDeath")
    public void simplyswords$triggerFlameSeedOnDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        WaxweaverEncasementManager.onTargetDeath(livingEntity);
        MoltenEdgeAbilityManager.resetWielder(livingEntity);
        SoulPyreAbilityManager.onDeath(livingEntity, damageSource);
        FlameSeedEffect.triggerDeathDetonation(livingEntity);
        GloamMechanicsManager.onTargetDeath(livingEntity);
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void simplyswords$preventGloamGraspJump(CallbackInfo ci) {
        if (GloamMechanicsManager.isGrasped((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "damage")
    public void simplyswords$queueFlameSeedDeathDetonation(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        FlameSeedEffect.queueLethalDeathDetonation(livingEntity, amount);
    }

    @Inject(
            method = "damage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;onDeath(Lnet/minecraft/entity/damage/DamageSource;)V")
    )
    public void simplyswords$triggerFlameSeedBeforeDeath(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        FlameSeedEffect.triggerDeathDetonation(livingEntity);
    }

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("TAIL"))
    public void simplyswords$triggerRunicPowerOnSwing(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!(livingEntity.getWorld() instanceof ServerWorld world) || RunicSlashManager.isSuppressed()) {
            return;
        }

        ItemStack stack = livingEntity.getStackInHand(hand);
        if (!stack.isEmpty()) {
            SimplySwordsAPI.onWeaponSwing(stack, world, livingEntity, hand);
        }
    }

}
