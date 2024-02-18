package net.sweenus.simplyswords.mixin;


import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.compat.eldritch_end.EldritchEndCompatMethods;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.sweenus.simplyswords.SimplySwords.minimumEldritchEndVersion;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(at = @At("HEAD"), method = "isDead", cancellable = true)
    public void simplyswords$tick(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!livingEntity.getWorld().isClient()) {
            if (livingEntity instanceof PlayerEntity player) {
                World world = player.getWorld();
                ItemStack stack = player.getMainHandStack();

                if (player.getHealth() <= 0.0F && !player.getItemCooldownManager().isCoolingDown(stack.getItem())
                        && (stack.isOf(ItemsRegistry.WAXWEAVER.get())
                        || stack.isOf(ItemsRegistry.WICKPIERCER.get()))) {

                    int skillCooldown = (int) Config.getFloat("waxweaveCooldown", "UniqueEffects", ConfigDefaultValues.waxweaveCooldown);
                    player.setHealth(player.getMaxHealth());
                    HelperMethods.incrementStatusEffect(player, StatusEffects.RESISTANCE, 100, 2, 3);
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                    world.playSound(null, player.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                            player.getSoundCategory(), 0.7f, 1.0f);
                    world.playSound(null, player.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                            player.getSoundCategory(), 0.8f, 1.0f);
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @ModifyVariable(method = "modifyAppliedDamage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float simplyswords$modifyDamageAmount(float amount, DamageSource source) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!livingEntity.getWorld().isClient()) {
            StatusEffectInstance voidcloakEffect = livingEntity.getStatusEffect(EffectRegistry.VOIDCLOAK.get());
            StatusEffectInstance ribbonwrathEffect = livingEntity.getStatusEffect(EffectRegistry.RIBBONWRATH.get());
            if (voidcloakEffect != null) {
                int amplifier = voidcloakEffect.getAmplifier();
                float reductionFactor = 1 - (amplifier + 1) * 0.10f; // +1 because amplifier starts at 0
                amount *= reductionFactor;
                HelperMethods.decrementStatusEffect(livingEntity, EffectRegistry.VOIDCLOAK.get());
            }
            if (ribbonwrathEffect != null) {
                float reductionFactor = 0.85f;
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
                    && Registries.STATUS_EFFECT.get(new Identifier("simplyswords:voidhunger")) != null)
                EldritchEndCompatMethods.generateVoidcloakStacks(livingEntity);
        }
    }

}