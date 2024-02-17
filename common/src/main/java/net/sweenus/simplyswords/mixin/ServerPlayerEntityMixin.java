package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {


    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {

            //Ribboncleaver movespeed debuff
            if (serverPlayer.getMainHandStack().isOf(ItemsRegistry.RIBBONCLEAVER.get())) {
                int frequency = 6;
                if (serverPlayer.age % 20 == 0)
                    serverPlayer.addStatusEffect(new StatusEffectInstance(EffectRegistry.RIBBONWRATH.get(),
                            30, 0, true, false, false));

                if (player.age % frequency == 0 && player.isSprinting() && player.isOnGround()) {
                    float volume = 0.3f;
                    float pitch = 1.0f + player.getRandom().nextBetween(1, 5) * 0.1f;
                    player.getWorld().playSound(null, player.getBlockPos(),
                            SoundRegistry.OBJECT_IMPACT_THUD.get(), SoundCategory.PLAYERS,volume, pitch);
                }

            }

        }
    }


    @Inject(at = @At("TAIL"), method = "attack")
    public void simplyswords$attack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (target.isAttackable()) {
                if (!target.handleAttack(player)) {

                    //Ribboncleaver Cleave buff
                    if (serverPlayer.hasStatusEffect(EffectRegistry.RIBBONCLEAVE.get())) {
                        serverPlayer.heal( (float) serverPlayer.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                        serverPlayer.removeStatusEffect(EffectRegistry.RIBBONCLEAVE.get());
                    }

                }
            }
        }
    }


}
