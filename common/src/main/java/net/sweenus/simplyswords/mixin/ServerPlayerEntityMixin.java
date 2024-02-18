package net.sweenus.simplyswords.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    public void simplyswords$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {

            //Effect Resilience
            if (serverPlayer.hasStatusEffect(EffectRegistry.RESILIENCE.get())) {
                HelperMethods.decrementStatusEffect(serverPlayer, EffectRegistry.RESILIENCE.get());
                cir.setReturnValue(false);
                serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.MAGIC_SWORD_PARRY_03.get(),
                        SoundCategory.PLAYERS, 0.7f, 0.5f + (serverPlayer.getRandom().nextBetween(1, 5) * 0.1f));
            }
        }
    }


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
                    ServerWorld serverWorld = (ServerWorld) player.getWorld();
                    //Ribboncleaver Cleave buff
                    if (serverPlayer.hasStatusEffect(EffectRegistry.RIBBONCLEAVE.get())) {
                        serverPlayer.removeStatusEffect(EffectRegistry.RIBBONCLEAVE.get());
                        HelperMethods.spawnOrbitParticles(serverWorld, target.getPos().add(0, 0.3, 0),
                                ParticleTypes.POOF, 0.5, 6);
                        HelperMethods.spawnOrbitParticles(serverWorld, target.getPos().add(0, 0.5, 0),
                                ParticleTypes.ENCHANTED_HIT, 0.5, 6);
                        serverWorld.playSound(null, target.getBlockPos(),
                                SoundRegistry.MAGIC_SWORD_PARRY_01.get(), SoundCategory.PLAYERS,0.8f, 1.0f);
                    }

                }
            }
        }
    }


}
