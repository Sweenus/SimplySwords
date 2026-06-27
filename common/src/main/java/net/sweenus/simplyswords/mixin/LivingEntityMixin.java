package net.sweenus.simplyswords.mixin;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatMethods;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.item.interfaces.RevivalWeapon;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
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

    @Shadow public abstract void setHealth(float health);

    @Inject(at = @At("HEAD"), method = "tryUseTotem", cancellable = true)
    public void simplyswords$tryRevive(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if(livingEntity instanceof PlayerEntity player && !player.getWorld().isClient()) {
            ItemStack mainhand = player.getStackInHand(Hand.MAIN_HAND);
            if (mainhand.getItem() instanceof RevivalWeapon revivalWeapon) {
                if(revivalWeapon.canRevive(player, mainhand, source)) {
                    setHealth(revivalWeapon.getReviveHealth(player, mainhand, source));
                    revivalWeapon.postRevive(player, mainhand, source);
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @ModifyVariable(method = "modifyAppliedDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float simplyswords$modifyDamageAmount(float amount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!livingEntity.getWorld().isClient()) {
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
        }
        return amount;
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

}