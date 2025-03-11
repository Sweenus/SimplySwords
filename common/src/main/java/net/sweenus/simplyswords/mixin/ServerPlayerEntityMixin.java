package net.sweenus.simplyswords.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.custom.CaelestisSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    public void simplyswords$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {

            //Effect Resilience
            if (serverPlayer.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RESILIENCE))) {
                HelperMethods.decrementStatusEffect(serverPlayer, EffectRegistry.getReference(EffectRegistry.RESILIENCE));
                cir.setReturnValue(false);
                if (!player.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.MAGISLAM)))
                    serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.MAGIC_SWORD_PARRY_03.get(),
                        SoundCategory.PLAYERS, 0.7f, 0.5f + (serverPlayer.getRandom().nextBetween(1, 5) * 0.1f));
            }

            if (serverPlayer.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT))) {
                StatusEffectInstance astralShiftInstance = player.getStatusEffect(EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT));
                if (astralShiftInstance != null) {
                    int duration = astralShiftInstance.getDuration();

                    if (duration > 10) {
                        HelperMethods.incrementStatusEffect(serverPlayer, EffectRegistry.getReference(EffectRegistry.ASTRAL_SHIFT), duration, (int) Math.max(1, (amount / 10)), 99);
                        AbilityMethods.astralShiftSounds(serverPlayer);
                        cir.setReturnValue(false);
                    }
                }
            }

            if (serverPlayer.getMainHandStack().getItem() instanceof CaelestisSwordItem) {
                if (AbilityMethods.astralShiftPassive(serverPlayer)) {
                    AbilityMethods.astralShiftSounds(serverPlayer);
                    cir.setReturnValue(false);
                }
            }

            // Magiscythe trigger
            if (source.toString().contains("sonic_boom")) {
                for (int i = 0; i < serverPlayer.getInventory().size(); i++) {
                    ItemStack stackInSlot = serverPlayer.getInventory().getStack(i);
                    if (stackInSlot.isOf(ItemsRegistry.DECAYING_RELIC.get())) {
                        ItemStack newItemStack = new ItemStack(ItemsRegistry.MAGISCYTHE.get());
                        newItemStack.applyComponentsFrom(stackInSlot.getComponents());
                        serverPlayer.getInventory().setStack(i, newItemStack);
                        serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                serverPlayer.getSoundCategory(), 0.6f, 0.6f);
                        serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magicythe.event"), true);
                        break;
                    }
                }
            }

        }
    }


    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {

            //Ribboncleaver movespeed debuff
            if (serverPlayer.getMainHandStack().isOf(ItemsRegistry.RIBBONCLEAVER.get()) || serverPlayer.getMainHandStack().isOf(ItemsRegistry.ENIGMA.get())) {
                int frequency = 6;
                if (serverPlayer.age % 20 == 0 && serverPlayer.getMainHandStack().isOf(ItemsRegistry.RIBBONCLEAVER.get()))
                    serverPlayer.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.RIBBONWRATH),
                            30, 0, true, false, false));

                if (player.age % frequency == 0 && player.isSprinting() && player.isOnGround()) {
                    float volume = 0.3f;
                    float pitch = 1.0f + player.getRandom().nextBetween(1, 5) * 0.1f;
                    player.getWorld().playSound(null, player.getBlockPos(),
                            SoundRegistry.OBJECT_IMPACT_THUD.get(), SoundCategory.PLAYERS,volume, pitch);
                }
            }

            //Magiblade repellent
            if (serverPlayer.getMainHandStack().isOf(ItemsRegistry.MAGIBLADE.get())) {
                int frequency = 8;
                double radius = Config.uniqueEffects.magiblade.repelRadius;
                int chance = Config.uniqueEffects.magiblade.repelChance;
                int totalChance = new Random().nextInt(100);
                if (serverPlayer.age % frequency == 0 && totalChance < chance) {
                    Box box = HelperMethods.createBox(player, radius);
                    Entity closestEntity = player.getWorld().getOtherEntities(player, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                            .filter(entity -> {
                                if (entity instanceof LivingEntity livingEntity)
                                    return HelperMethods.checkFriendlyFire(livingEntity, player);
                                return false;
                            })
                            .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)))
                            .orElse(null);

                    if (closestEntity != null) {
                        if ((closestEntity instanceof LivingEntity le)) {
                            if (le.distanceTo(player) > 1) {
                                closestEntity.setVelocity((closestEntity.getX() - player.getX()) / 2, 0, (closestEntity.getZ() - player.getZ()) / 2);
                                float volume = 0.8f;
                                float pitch = 1.0f + player.getRandom().nextBetween(1, 5) * 0.1f;
                                player.getWorld().playSound(null, player.getBlockPos(),
                                        SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, volume, pitch);
                                HelperMethods.spawnWaistHeightParticles((ServerWorld) player.getWorld(), ParticleTypes.ENCHANT, closestEntity, player, 10);
                                HelperMethods.spawnOrbitParticles((ServerWorld) closestEntity.getWorld(), closestEntity.getPos().add(0, closestEntity.getHeight() / 2, 0), ParticleTypes.SCULK_CHARGE_POP, 0.5, 6);
                            }
                        }
                    }
                }
            }

            // Contained Remnant logic
            int frequency = 1200; // 1m
            if (serverPlayer.age % frequency == 0) {
                ItemStack containedRemnant = ItemsRegistry.CONTAINED_REMNANT.get().asItem().getDefaultStack();
                ItemStack tamperedRemnant = ItemsRegistry.TAMPERED_REMNANT.get().asItem().getDefaultStack();
                ItemStack decayingRelic = ItemsRegistry.DECAYING_RELIC.get().asItem().getDefaultStack();
                ItemStack runicTablet = ItemsRegistry.RUNIC_TABLET.get().asItem().getDefaultStack();
                Random random = new Random();
                TagKey<Item> desiredItemsTag = TagKey.of(Registries.ITEM.getKey(), Identifier.of("simplyswords", "conditional_uniques_type_1"));
                TagKey<Item> endItemsTag = TagKey.of(Registries.ITEM.getKey(), Identifier.of("simplyswords", "conditional_uniques_type_2"));
                int chance = random.nextInt(100);

                for (int i = 0; i < serverPlayer.getInventory().size(); i++) {
                    ItemStack stackInSlot = serverPlayer.getInventory().getStack(i);

                    if (stackInSlot.isOf(containedRemnant.getItem()) || stackInSlot.isOf(tamperedRemnant.getItem())) {
                        if (chance < 36 && LootConfig.INSTANCE.enableContainedRemnants.get()) {
                            List<Item> itemsFromTag = Registries.ITEM.stream()
                                    .filter(item -> item.getDefaultStack().isIn(desiredItemsTag))
                                    .toList();
                            List<Item> itemsFromTagEnd = Registries.ITEM.stream()
                                    .filter(item -> item.getDefaultStack().isIn(endItemsTag))
                                    .toList();

                            if (!itemsFromTag.isEmpty() && !itemsFromTagEnd.isEmpty()) {
                                Item randomItem = ItemsRegistry.TAMPERED_REMNANT.get();
                                if (serverPlayer.getWorld().getRegistryKey().equals(World.END)
                                        && serverPlayer.getInventory().getStack(i).isOf(containedRemnant.getItem())
                                        && serverPlayer.getInventory().contains(runicTablet)) {
                                    serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                                            serverPlayer.getSoundCategory(), 0.3f, 0.6f);
                                    serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.contained_remnant.event2"), true);
                                } else {
                                    if (serverPlayer.getInventory().getStack(i).isOf(ItemsRegistry.CONTAINED_REMNANT.get()))
                                        randomItem = itemsFromTag.get(random.nextInt(itemsFromTag.size()));
                                    else if (serverPlayer.getInventory().getStack(i).isOf(ItemsRegistry.TAMPERED_REMNANT.get()))
                                        randomItem = itemsFromTagEnd.get(random.nextInt(itemsFromTagEnd.size()));
                                    serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                            serverPlayer.getSoundCategory(), 0.3f, 0.6f);
                                    serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.contained_remnant.event"), true);
                                }
                                ItemStack newItemStack = new ItemStack(randomItem);
                                serverPlayer.getInventory().setStack(i, newItemStack);
                                break;
                            }
                        }
                    }
                    BlockState playerStandingBlock = serverPlayer.getSteppingBlockState();
                    if (stackInSlot.isOf(decayingRelic.getItem()) && playerStandingBlock.isOf(Blocks.SCULK)) {
                        serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magicythe.event2"), true);
                    }
                    if (stackInSlot.isOf(decayingRelic.getItem()) && playerStandingBlock.isOf(Blocks.SCULK_SENSOR)) {
                        serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magiblade.event2"), true);
                    }
                }
            }
            if (serverPlayer.age % 20 == 0) {
                BlockState playerStandingBlock = serverPlayer.getSteppingBlockState();
                ItemStack decayingRelic = ItemsRegistry.DECAYING_RELIC.get().asItem().getDefaultStack();
                int chance = new Random().nextInt(100);
                for (int i = 0; i < serverPlayer.getInventory().size(); i++) {
                    ItemStack stackInSlot = serverPlayer.getInventory().getStack(i);

                    // Magiblade trigger
                    if (chance < 15 && playerStandingBlock.isOf(Blocks.SCULK_SENSOR) && stackInSlot.isOf(decayingRelic.getItem())) {
                        ItemStack newItemStack = new ItemStack(ItemsRegistry.MAGIBLADE.get());
                        newItemStack.applyComponentsFrom(stackInSlot.getComponents());
                        serverPlayer.getInventory().setStack(i, newItemStack);
                        serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                serverPlayer.getSoundCategory(), 0.6f, 0.6f);
                        serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magiblade.event"), true);
                        break;
                    }

                    // Magispear trigger
                    if (stackInSlot.isOf(decayingRelic.getItem()) && player.hasStatusEffect(StatusEffects.DARKNESS)) {
                        if (chance < 2) {
                            ItemStack newItemStack = new ItemStack(ItemsRegistry.MAGISPEAR.get());
                            newItemStack.applyComponentsFrom(stackInSlot.getComponents());
                            serverPlayer.getInventory().setStack(i, newItemStack);
                            serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                    serverPlayer.getSoundCategory(), 0.6f, 0.6f);
                            serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magispear.event"), true);
                            break;
                        } else if (chance < 11){
                            serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magispear.event2"), true);
                        }
                    }

                }
            }
        }
    }


    @Inject(at = @At("TAIL"), method = "attack")
    public void simplyswords$attack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (target.isAttackable() && target instanceof LivingEntity) {
                if (!target.handleAttack(player)) {
                    ServerWorld serverWorld = (ServerWorld) player.getWorld();
                    //Ribboncleaver Cleave buff
                    if (serverPlayer.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE))) {
                        serverPlayer.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE));
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