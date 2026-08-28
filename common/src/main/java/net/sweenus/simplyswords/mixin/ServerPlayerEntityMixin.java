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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.StackReplacement;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.ContainedRemnantItem;
import net.sweenus.simplyswords.item.custom.WickpiercerSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.AbilityMethods;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.NecromanticArsenalManager;
import net.sweenus.simplyswords.world.WolfPackManager;
import net.sweenus.simplyswords.util.MinionTargeting;
import net.sweenus.simplyswords.world.Phase4LichbladeManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;
import net.sweenus.simplyswords.world.RevivalCandleVisualManager;
import net.sweenus.simplyswords.world.MagispearAbilityManager;
import net.sweenus.simplyswords.world.IonboundStormscaleAbilityManager;
import net.sweenus.simplyswords.world.ShadowstingShadowDanceManager;
import net.sweenus.simplyswords.world.SoulkeeperLanternManager;
import net.sweenus.simplyswords.world.StormbringerParryManager;
import net.sweenus.simplyswords.world.StormsEdgeAbilityManager;
import net.sweenus.simplyswords.world.DreadwhisperAbilityManager;
import net.sweenus.simplyswords.world.ThunderbrandAbilityManager;
import net.sweenus.simplyswords.world.Phase2CombatStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow public abstract ServerWorld getServerWorld();

    @Inject(at = @At("HEAD"), method = "damage", cancellable = true)
    public void simplyswords$damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (IonboundStormscaleAbilityManager.handleIncomingDamage(serverPlayer, source, amount)) {
                cir.setReturnValue(false);
                return;
            }
            if (MagispearAbilityManager.blocksIncomingDamage(serverPlayer, source)) {
                cir.setReturnValue(false);
                return;
            }
            if (StormsEdgeAbilityManager.blocksIncomingDamage(serverPlayer, source)) {
                cir.setReturnValue(false);
                return;
            }
            if (DreadwhisperAbilityManager.blocksIncomingDamage(serverPlayer, source)) {
                cir.setReturnValue(false);
                return;
            }
            if (ThunderbrandAbilityManager.handleIncomingDamage(serverPlayer, source, amount)) {
                cir.setReturnValue(false);
                return;
            }
            if (!source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                if (ShadowstingShadowDanceManager.isActive(serverPlayer)) {
                    cir.setReturnValue(false);
                    return;
                }

                if (StormbringerParryManager.handleIncomingDamage(serverPlayer, source)) {
                    cir.setReturnValue(false);
                    return;
                }

                if (WeaponImplicitRegistry.tryDeflectIncomingDamage(serverPlayer, source, amount)) {
                    cir.setReturnValue(false);
                    return;
                }

                //Effect Resilience
                if (serverPlayer.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RESILIENCE))) {
                    HelperMethods.decrementStatusEffect(serverPlayer, EffectRegistry.getReference(EffectRegistry.RESILIENCE));
                    cir.setReturnValue(false);
                    if (player.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.RIBBONCLEAVE)))
                        serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.MAGIC_SWORD_PARRY_03.get(),
                            SoundCategory.PLAYERS, 0.7f, 0.5f + (serverPlayer.getRandom().nextBetween(1, 5) * 0.1f));
                }

            }

            // Magiscythe trigger
            if (source.toString().contains("sonic_boom")) {
                for (int i = 0; i < serverPlayer.getInventory().size(); i++) {
                    ItemStack stackInSlot = serverPlayer.getInventory().getStack(i);
                    if (stackInSlot.isOf(ItemsRegistry.DECAYING_RELIC.get())) {
                        ItemStack newItemStack = StackReplacement.copyTo(stackInSlot, ItemsRegistry.MAGISCYTHE.get());
                        AwakeningApi.setLevel(newItemStack, AwakeningApi.getLevel(stackInSlot));
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

    @Inject(at = @At("TAIL"), method = "damage")
    public void simplyswords$retargetNecromanticArsenalMinions(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if ((Object) this instanceof ServerPlayerEntity serverPlayer
                && source.getAttacker() instanceof LivingEntity attacker
                && attacker != serverPlayer) {
            NecromanticArsenalManager.retargetMinions(serverPlayer, attacker);
            WolfPackManager.retargetMinions(serverPlayer, attacker);
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    public void simplyswords$tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            RevivalCandleVisualManager.tickPlayer(serverPlayer);
            ShadowstingShadowDanceManager.tickPlayer(serverPlayer);
            SoulkeeperLanternManager.tickPlayer(serverPlayer);
            PlayerWeaponAbilityChannelManager.tickPlayer(serverPlayer);
            Phase4LichbladeManager.tickOwner(serverPlayer);
            StormbringerParryManager.tickPlayer(serverPlayer);

            //Ribboncleaver movespeed debuff
            ItemStack heldUnique = serverPlayer.getMainHandStack();
            if (AwakeningApi.isAbilityUnlocked(heldUnique)
                    && (heldUnique.isOf(ItemsRegistry.RIBBONCLEAVER.get())
                    || heldUnique.isOf(ItemsRegistry.ENIGMA.get()))) {
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

            // Contained Remnant logic
            int frequency = Config.general.containedRemnantTransformFrequency;
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
                                AwakeningApi.initializeNaturalDrop(newItemStack);
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
                        ItemStack newItemStack = StackReplacement.copyTo(stackInSlot, ItemsRegistry.MAGIBLADE.get());
                        AwakeningApi.setLevel(newItemStack, AwakeningApi.getLevel(stackInSlot));
                        serverPlayer.getInventory().setStack(i, newItemStack);
                        serverPlayer.getWorld().playSoundFromEntity(null, serverPlayer, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                serverPlayer.getSoundCategory(), 0.6f, 0.6f);
                        serverPlayer.sendMessageToClient(Text.translatable("item.simplyswords.magiblade.event"), true);
                        break;
                    }

                    // Magispear trigger
                    if (stackInSlot.isOf(decayingRelic.getItem()) && player.hasStatusEffect(StatusEffects.DARKNESS)) {
                        if (chance < 2) {
                            ItemStack newItemStack = StackReplacement.copyTo(stackInSlot, ItemsRegistry.MAGISPEAR.get());
                            AwakeningApi.setLevel(newItemStack, AwakeningApi.getLevel(stackInSlot));
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
            // Check for axolotls on the player's shoulders
            NbtCompound leftShoulder = player.getShoulderEntityLeft();
            NbtCompound rightShoulder = player.getShoulderEntityRight();
            AbilityMethods.applyAxolotlBuff(serverPlayer, leftShoulder, rightShoulder);


            // Chomp'olotl passive particles
            if (HelperMethods.isHoldingItem(ItemsRegistry.CHOMPOLOTL.get(), serverPlayer) && Config.general.enablePassiveParticles)
                HelperMethods.createServerBubbleTrail(getServerWorld(), serverPlayer);

            // Contained Remnant hint messages
            if (HelperMethods.isHoldingItem(ItemsRegistry.CONTAINED_REMNANT.get(), serverPlayer))
                ContainedRemnantItem.checkNearbyBlocks(serverPlayer);


        }
    }


    @Inject(at = @At("TAIL"), method = "attack")
    public void simplyswords$attack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (target.isAttackable() && target instanceof LivingEntity livingTarget) {
                MinionTargeting.recordLastAttack(serverPlayer, livingTarget);
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

                    if (serverPlayer.getMainHandStack().getItem() instanceof WickpiercerSwordItem) {
                        ItemStack wickpiercerStack = serverPlayer.getMainHandStack();
                        float damageModifier = HelperMethods.abilityScaledDamage("fire",
                                serverPlayer, wickpiercerStack, Config.uniqueEffects.wickpiercer.damageScaling,
                                Config.uniqueEffects.wickpiercer.spellScaling);
                        Phase2CombatStateManager.applyPhoenixBlow(serverPlayer, livingTarget,
                                wickpiercerStack, damageModifier);
                        if (serverPlayer.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FRENZY))) {
                            target.timeUntilRegen = 0;
                            Phase2CombatStateManager.applyWickFrenzyHit(serverPlayer, livingTarget,
                                    wickpiercerStack, damageModifier);
                        }
                    }
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "attack", cancellable = true)
    public void simplyswords$preventShadowDanceAttack(Entity target, CallbackInfo ci) {
        if (ShadowstingShadowDanceManager.isActive((ServerPlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }


}
