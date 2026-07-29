package net.sweenus.simplyswords.mixin;


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
import net.sweenus.simplyswords.world.MoltenEdgeAbilityManager;
import net.sweenus.simplyswords.world.SoulPyreAbilityManager;
import net.sweenus.simplyswords.world.StormsEdgeAbilityManager;
import net.sweenus.simplyswords.world.ThunderbrandAbilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sweenus.simplyswords.SimplySwords.minimumEldritchEndVersion;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

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
                && StormsEdgeAbilityManager.blocksIncomingDamage(livingEntity, source)) {
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
        if (!livingEntity.getWorld().isClient()) {
            amount = MoltenEdgeAbilityManager.modifyOutgoingDamage(source, amount);
            StatusEffectInstance voidcloakEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            StatusEffectInstance ribbonwrathEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONWRATH));
            StatusEffectInstance soulTetherEffect = livingEntity.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SOULTETHER));
            if (voidcloakEffect != null) {
                int amplifier = voidcloakEffect.getAmplifier();
                float reductionFactor = 1 - (amplifier + 1) * 0.10f; // +1 because amplifier starts at 0
                amount *= reductionFactor;
                HelperMethods.decrementStatusEffect(livingEntity, EffectRegistry.getReference(EffectRegistry.VOIDCLOAK));
            }
            if (ribbonwrathEffect != null) {
                float reductionFactor = 0.85f;
                amount *= reductionFactor;
            }
            if (soulTetherEffect != null) {
                float reductionFactor = 0.50f;
                amount *= reductionFactor;
            }
            amount = WeaponImplicitRegistry.modifyDamage(livingEntity, source, amount);
            amount = MoltenEdgeAbilityManager.modifyIncomingDamage(livingEntity, amount);
        }
        return amount;
    }

    @Inject(at = @At("TAIL"), method = "damage")
    public void simplyswords$applyWeaponImplicitOnDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (Boolean.TRUE.equals(cir.getReturnValue()) && !livingEntity.getWorld().isClient()) {
            MoltenEdgeAbilityManager.gainHeatFromIncomingDamage(livingEntity, amount, true);
            WeaponImplicitRegistry.onDamageApplied(livingEntity, source, amount);
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
        MoltenEdgeAbilityManager.resetWielder(livingEntity);
        SoulPyreAbilityManager.onDeath(livingEntity, damageSource);
        FlameSeedEffect.triggerDeathDetonation(livingEntity);
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
