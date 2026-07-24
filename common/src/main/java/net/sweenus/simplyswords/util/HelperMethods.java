package net.sweenus.simplyswords.util;

import dev.architectury.platform.Platform;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import java.util.UUID;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.api.DelegatedWeaponHitContext;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.compat.opac.OpacCompat;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class HelperMethods {

    private static final Random random = new Random();

    public static Random random() {
        return random;
    }

    /*
     * getTargetedEntity taken heavily from ZsoltMolnarrr's CombatSpells
     * https://github.com/ZsoltMolnarrr/SpellEngine/blob/1.19.2/common/src/main/java/net/spell_engine/utils/TargetHelper.java#L136
     */
    public static Entity getTargetedEntity(Entity user, double range) {
        Vec3d rayCastOrigin = user.getEyePos();
        Vec3d userView = user.getRotationVec(1.0F).normalize().multiply(range);
        Vec3d rayCastEnd = rayCastOrigin.add(userView);
        Box searchBox = user.getBoundingBox().expand(range, range, range);
        EntityHitResult hitResult = ProjectileUtil.raycast(user, rayCastOrigin, rayCastEnd, searchBox,
                (target) -> !target.isSpectator() && target.canHit() && target instanceof LivingEntity, range * range);
        if (hitResult != null) {
            return hitResult.getEntity();
        }
        return null;
    }

    public static boolean isWalking(Entity entity) {
        return entity instanceof PlayerEntity player && (!player.isDead() && (player.isSwimming() || player.getVelocity().horizontalLength() > 0.1));
    }


    // Check if we should be able to hit the target
    public static boolean checkFriendlyFire (LivingEntity livingEntity, LivingEntity attackingEntity) {
        if (livingEntity == null || attackingEntity == null)
            return false;
        if (livingEntity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()))
            return false;
        if (!checkEntityBlacklist(livingEntity, attackingEntity))
            return false;
        if (livingEntity == attackingEntity)
            return false;
        if (isMonsterFaction(attackingEntity) && isMonsterFaction(livingEntity)) {
            return false;
        }

        // Check if the player and the living entity are on the same team
        AbstractTeam playerTeam = attackingEntity.getScoreboardTeam();
        AbstractTeam entityTeam = livingEntity.getScoreboardTeam();
        if (HelperMethods.isOpacLoaded() && livingEntity instanceof PlayerEntity
                && attackingEntity instanceof PlayerEntity playerEntity) {
            // Is OpenPAC loaded? And are they team/ally member?
            return OpacCompat.checkOpacFriendlyFire(livingEntity, playerEntity);
        }
        if (playerTeam != null && entityTeam != null && livingEntity.isTeammate(attackingEntity)) {
            // They are on the same team, so friendly fire should not be allowed
            return false;
        }

        if (livingEntity instanceof PlayerEntity playerEntity
                && attackingEntity instanceof PlayerEntity player) {
            if (playerEntity == attackingEntity)
                return false;
            return playerEntity.shouldDamagePlayer(player);
        }
        if (attackingEntity instanceof Tameable attackingTameable) {
            UUID attackerOwnerUuid = attackingTameable.getOwnerUuid();
            if (attackerOwnerUuid != null) {
                if (attackerOwnerUuid.equals(livingEntity.getUuid())) {
                    return false;
                }
                if (livingEntity instanceof Tameable targetTameable) {
                    UUID targetOwnerUuid = targetTameable.getOwnerUuid();
                    if (targetOwnerUuid != null && targetOwnerUuid.equals(attackerOwnerUuid)) {
                        return false;
                    }
                }
            }
        }
        if (livingEntity instanceof Tameable tameable) {
            UUID ownerUuid = tameable.getOwnerUuid();
            if (ownerUuid != null) {
                if (ownerUuid.equals(attackingEntity.getUuid())) {
                    return false;
                }
                if (attackingEntity instanceof Tameable attackingTameable) {
                    UUID attackerOwnerUuid = attackingTameable.getOwnerUuid();
                    if (attackerOwnerUuid != null && attackerOwnerUuid.equals(ownerUuid)) {
                        return false;
                    }
                }
                LivingEntity owner = tameable.getOwner();
                if (owner != null && owner != attackingEntity
                        && (owner instanceof PlayerEntity ownerPlayer)
                        && attackingEntity instanceof PlayerEntity playerEntity) {
                    if (HelperMethods.isOpacLoaded()) {
                        return OpacCompat.checkOpacFriendlyFire(ownerPlayer, playerEntity);
                    }
                    return playerEntity.shouldDamagePlayer(ownerPlayer);
                }
                return !ownerUuid.equals(attackingEntity.getUuid());
            }
            return true;
        }
        return true;
    }

    public static boolean checkAbilityTarget(LivingEntity livingEntity, LivingEntity attackingEntity) {
        return checkFriendlyFire(livingEntity, attackingEntity);
    }

    public static boolean isMonsterFaction(LivingEntity entity) {
        if (entity instanceof Monster) {
            if (entity instanceof Tameable tameable) {
                LivingEntity owner = tameable.getOwner();
                if (owner != null) {
                    return isMonsterFaction(owner);
                }
                UUID ownerUuid = tameable.getOwnerUuid();
                if (ownerUuid != null && entity.getWorld() instanceof ServerWorld world) {
                    Entity ownerEntity = world.getEntity(ownerUuid);
                    if (ownerEntity instanceof LivingEntity ownerLiving) {
                        return isMonsterFaction(ownerLiving);
                    }
                }
                return false;
            }
            return true;
        }
        if (entity instanceof Tameable tameable) {
            LivingEntity owner = tameable.getOwner();
            if (owner != null) {
                return isMonsterFaction(owner);
            }
            UUID ownerUuid = tameable.getOwnerUuid();
            if (ownerUuid != null && entity.getWorld() instanceof ServerWorld world) {
                Entity ownerEntity = world.getEntity(ownerUuid);
                if (ownerEntity instanceof LivingEntity ownerLiving) {
                    return isMonsterFaction(ownerLiving);
                }
            }
        }
        return false;
    }

    public static boolean isOpacLoaded() {
        return Platform.isModLoaded("openpartiesandclaims");
    }

    //Check if the target matches blacklisted entities (expand this to be configurable if there is demand)
    public static boolean checkEntityBlacklist(LivingEntity target, LivingEntity player) {
        if (target == null || player == null) {
            return false;
        }
        return !(target instanceof ArmorStandEntity)
                && !(target instanceof VillagerEntity)
                && !(target instanceof BattleStandardEntity)
                && !(target instanceof BattleStandardDarkEntity);
    }

    //spawnParticle - spawns particles across both client & server
    public static void spawnParticle(World world, ParticleEffect particle, double xpos, double ypos, double zpos,
                                     double xvelocity, double yvelocity, double zvelocity) {
        if (world.isClient) {
            world.addParticle(particle, xpos, ypos, zpos, xvelocity, yvelocity, zvelocity);
        } else if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, xpos, ypos, zpos, 1, xvelocity, yvelocity, zvelocity, 0.1);
        }
    }

    public static void createServerBubbleTrail(ServerWorld serverWorld, ServerPlayerEntity player) {
        if (player.age %10 == 0) {
            double x = player.getX() + (serverWorld.random.nextDouble() - 0.5) * 0.3;
            double y = player.getY() + 0.1;
            double z = player.getZ() + (serverWorld.random.nextDouble() - 0.5) * 0.3;

            serverWorld.spawnParticles(
                    ParticlesRegistry.CUSTOM_BUBBLE.get(), // Updated registry reference
                    x,
                    y,
                    z,
                    2,
                    0.1, // X spread
                    0.05, // Y spread (rise effect)
                    0.1, // Z spread
                    0.02 // Speed
            );
        }
    }




    // playHitSounds
    public static void playHitSounds(LivingEntity attacker, LivingEntity target) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            boolean impactsounds_enabled = Config.general.enableWeaponImpactSounds;
            float impactsounds_volume = Config.general.weaponImpactSoundsVolume;

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
                else if (choose_sound <= 40)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, impactsounds_volume, 1.1f + choose_pitch);
            }
        }
    }

    //Check if item is a unique 2H weapon
    public static boolean isUniqueTwohanded(ItemStack stack) {
        return stack.getItem() instanceof TwoHandedWeapon;
    }

    //Create Box
    public static Box createBox(Entity entity, double radius) {
        return new Box(entity.getX() + radius, entity.getY() + (float) radius / 3, entity.getZ() + radius,
                entity.getX() - radius, entity.getY() - (float) radius / 3, entity.getZ() - radius);
    }

    //Gets the blockpos we are looking at
    public static Vec3d getPositionLookingAt(PlayerEntity player, int range) {
        HitResult result = player.raycast(range, 0, false);
        //System.out.println(result.getType());
        if (!(result.getType() == HitResult.Type.BLOCK)) return null;

        BlockHitResult blockResult = (BlockHitResult) result;
        //System.out.println(blockResult.getBlockPos());
        return blockResult.getPos();
    }

    public static void incrementStatusEffect(
            LivingEntity livingEntity,
            RegistryEntry<StatusEffect> statusEffect,
            int duration,
            int amplifier,
            int amplifierMax) {

        if (livingEntity.hasStatusEffect(statusEffect)) {
            int currentDuration = livingEntity.getStatusEffect(statusEffect).getDuration();
            int currentAmplifier = livingEntity.getStatusEffect(statusEffect).getAmplifier();

            if (currentAmplifier >= amplifierMax) {
                livingEntity.addStatusEffect(new StatusEffectInstance(
                        statusEffect, Math.max(currentDuration, duration), currentAmplifier, false, false, true));
                return;
            }

            livingEntity.addStatusEffect(new StatusEffectInstance(
                    statusEffect, Math.max(currentDuration, duration), Math.min(amplifierMax, currentAmplifier + amplifier), false, false, true));
        }
        livingEntity.addStatusEffect(new StatusEffectInstance(statusEffect, duration, amplifier, false, false, true));
    }

    public static SimplySwordsStatusEffectInstance incrementSimplySwordsStatusEffect(
            LivingEntity livingEntity,
            RegistryEntry<StatusEffect> statusEffect,
            int duration,
            int amplifier,
            int amplifierMax) {

        SimplySwordsStatusEffectInstance statusReturn;
        if (livingEntity.hasStatusEffect(statusEffect)) {
            int currentDuration = livingEntity.getStatusEffect(statusEffect).getDuration();
            int currentAmplifier = livingEntity.getStatusEffect(statusEffect).getAmplifier();

            if (currentAmplifier >= amplifierMax) {
                statusReturn = new SimplySwordsStatusEffectInstance(statusEffect, Math.max(currentDuration, duration),
                        currentAmplifier, false, false, true);
                        livingEntity.addStatusEffect(statusReturn);
                return statusReturn;
            }

            livingEntity.addStatusEffect(new StatusEffectInstance(
                    statusEffect, Math.max(currentDuration, duration), Math.min(amplifierMax, currentAmplifier + amplifier),
                    false, false, true));
        }
        statusReturn = new SimplySwordsStatusEffectInstance(statusEffect, duration, 0,
                false, false, true);
        livingEntity.addStatusEffect(statusReturn);
        return statusReturn;
    }

    public static void decrementStatusEffect(LivingEntity livingEntity, RegistryEntry<StatusEffect> statusEffect) {

        if (livingEntity.hasStatusEffect(statusEffect)) {
            int currentAmplifier = livingEntity.getStatusEffect(statusEffect).getAmplifier();
            int currentDuration = livingEntity.getStatusEffect(statusEffect).getDuration();

            if (currentAmplifier < 1) {
                livingEntity.removeStatusEffect(statusEffect);
                return;
            }

            livingEntity.removeStatusEffect(statusEffect);
            livingEntity.addStatusEffect(new StatusEffectInstance(
                    statusEffect, currentDuration, currentAmplifier - 1, false, false, true));
        }
    }

    // createFootfalls - creates weapon footfall particle effects (footsteps)
    public static void createFootfalls(Entity entity, ItemStack stack, World world, ParticleEffect particle,
                                       ParticleEffect sprintParticle, ParticleEffect passiveParticle, boolean passiveParticles) {
        int stepMod = 7 - (int)(world.getTime() % 7);
        if ((entity instanceof PlayerEntity player) && Config.general.enableWeaponFootfalls && player.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            if (isWalking(player) && !player.isSwimming() && player.isOnGround()) {
                if (stepMod == 6) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    } else {
                        world.addParticle(particle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                } else if (stepMod == 3) {
                    if (player.isSprinting()) {
                        world.addParticle(sprintParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    } else {
                        world.addParticle(particle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                                player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.2,
                                player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                                0, 0.0, 0);
                    }
                }
            }
            if (passiveParticles && Config.general.enablePassiveParticles) {
                float randomy = (float) (Math.random());
                if (stepMod == 1) {
                    world.addParticle(passiveParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX(),
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.4 + randomy,
                            player.getZ() - player.getHandPosOffset(stack.getItem()).getZ(),
                            0, 0.0, 0);
                    world.addParticle(passiveParticle, player.getX() - player.getHandPosOffset(stack.getItem()).getX() + 0.1,
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + randomy,
                            player.getZ() - player.getHandPosOffset(stack.getItem()).getZ() - 0.1,
                            0, 0.0, 0);
                } else if (stepMod == 4) {
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX(),
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + 0.4 + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ(),
                            0, 0.0, 0);
                    world.addParticle(passiveParticle, player.getX() + player.getHandPosOffset(stack.getItem()).getX() - 0.1,
                            player.getY() + player.getHandPosOffset(stack.getItem()).getY() + randomy,
                            player.getZ() + player.getHandPosOffset(stack.getItem()).getZ() + 0.1,
                            0, 0.0, 0);
                }
            }
        }
    }

    public static void spawnOrbitParticles(ServerWorld world, Vec3d center, ParticleEffect particleType, double radius, int particleCount) {
        for (int i = 0; i < particleCount; i++) {
            // Calculate the angle for this particle
            double angle = 2 * Math.PI * i / particleCount;

            // Calculate the x and z coordinates on the orbit
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            double y = center.y;

            // Spawn the particle at the calculated position
            world.spawnParticles(particleType, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    public static void spawnWaistHeightParticles(ServerWorld world, ParticleEffect particle, Entity entity1, Entity entity2, int count) {
        Vec3d startPos = entity1.getPos().add(0, entity1.getHeight() / 2.0, 0);
        Vec3d endPos = entity2.getPos().add(0, entity2.getHeight() / 2.0, 0);
        Vec3d direction = endPos.subtract(startPos);
        double distance = direction.length();
        Vec3d normalizedDirection = direction.normalize();

        for (int i = 0; i < count; i++) {
            double lerpFactor = (double) i / (count - 1);
            Vec3d currentPos = startPos.add(normalizedDirection.multiply(distance * lerpFactor));
            world.spawnParticles(particle,
                    currentPos.x, currentPos.y, currentPos.z,
                    1,
                    0, 0, 0,
                    0.0);
        }
    }

    public static void spawnRainingParticles(ServerWorld world, ParticleEffect particle, Entity entity2, int count, double blocksAbove) {
        Vec3d endPos = entity2.getPos().add(0, entity2.getHeight() / 2.0, 0);
        Vec3d startPos = endPos.add(0, blocksAbove, 0);
        Vec3d direction = endPos.subtract(startPos);
        double distance = direction.length();
        Vec3d normalizedDirection = direction.normalize();

        for (int i = 0; i < count; i++) {
            double lerpFactor = (double) i / (count - 1);
            Vec3d currentPos = startPos.add(normalizedDirection.multiply(distance * lerpFactor));
            world.spawnParticles(particle,
                    currentPos.x, currentPos.y, currentPos.z,
                    1,
                    0, 0, 0,
                    0.0);
        }
    }

    public static void spawnParticlesBetween(ServerPlayerEntity player, BlockPos blockPos, ServerWorld world, ParticleEffect particleType, int particleCount) {
        Vec3d start = player.getPos().add(0, player.getHeight() / 2.0, 0);
        Vec3d end = Vec3d.ofCenter(blockPos);

        double stepSize = 1.0 / particleCount;

        for (int i = 0; i <= particleCount; i++) {
            double t = i * stepSize;

            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;

            world.spawnParticles(
                    particleType,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }



    public static float spellScaledDamage(String spellSchool, Entity entity, float damageModifier, float damageFallback) {
        float scaling = commonSpellAttributeScaling(damageModifier, entity, spellSchool);
        return scaling > 0 ? scaling : damageFallback;
    }

    public static float abilityScaledDamage(String spellSchool, LivingEntity actor, ItemStack stack, float attackScaling, float spellScaling) {
        float scaling = commonSpellAttributeScaling(spellScaling, actor, spellSchool);
        if (scaling > 0f) {
            return scaling;
        }
        return attackScaledDamage(actor, stack, attackScaling);
    }

    public static float abilityScaledDamage(String spellSchool, LivingEntity actor, float attackScaling, float spellScaling) {
        return abilityScaledDamage(spellSchool, actor, actor == null ? ItemStack.EMPTY : actor.getMainHandStack(), attackScaling, spellScaling);
    }

    public static float attackScaledDamage(LivingEntity actor, ItemStack stack, float attackScaling) {
        double attackDamage = actor == null ? 0.0 : getEntityAttackDamage(actor);
        if (attackDamage <= 0.0 && stack != null && !stack.isEmpty()) {
            attackDamage = Math.max(1.0, 1.0 + getAttackFromStack(stack, AttributeModifierSlot.MAINHAND));
        }
        return (float) Math.max(0.0, attackDamage * attackScaling);
    }

    public static float applyAbilityDamageEnchantments(ServerWorld world, ItemStack stack, Entity target, DamageSource damageSource, float damage) {
        float finalDamage = damage;
        if (Config.general.enableAbilityDamageEnchantScaling && world != null && stack != null && !stack.isEmpty() && target != null && damageSource != null) {
            finalDamage = EnchantmentHelper.getDamage(world, stack, target, damageSource, finalDamage);
        }
        return applyNonPlayerAbilityDamageModifier(resolveAbilityDamageActor(damageSource), finalDamage);
    }

    public static float applyNonPlayerAbilityDamageModifier(LivingEntity actor, float damage) {
        if (actor instanceof PlayerEntity || actor == null) {
            return damage;
        }
        return Math.max(0.0F, damage * Math.max(0.0F, Config.general.nonPlayerWeaponAbilityDamageModifier));
    }

    public static LivingEntity resolveAbilityDamageActor(DamageSource damageSource) {
        DelegatedWeaponHitContext delegatedContext = SimplySwordsAPI.getDelegatedWeaponHitContext();
        if (delegatedContext != null && delegatedContext.actor() != null) {
            return delegatedContext.actor();
        }
        if (damageSource != null && damageSource.getAttacker() instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    public static float commonSpellAttributeScaling(float damageModifier, Entity entity, String magicSchool) {
        if ((entity instanceof PlayerEntity player) && Config.general.compatEnableSpellPowerScaling.get())
            return SimplySwordsExpectPlatform.getSpellPowerDamage(damageModifier, player, magicSchool);
        return 0f;
    }

    public static Optional<LivingEntity> findClosestTarget(LivingEntity livingEntity, double maxDistance, double width) {
        World world = livingEntity.getEntityWorld();
        Vec3d eyePosition = livingEntity.getEyePos();
        Vec3d lookVec = livingEntity.getRotationVec(1.0F);
        Vec3d targetVec = eyePosition.add(lookVec.x * maxDistance, lookVec.y * maxDistance, lookVec.z * maxDistance);

        // Calculate the perpendicular vector to the lookVec for the width
        Vec3d perpVec = new Vec3d(-lookVec.z, 0, lookVec.x).normalize().multiply(width / 2.0);

        // Create a search box that extends along the look vector with the specified width
        Box searchBox = new Box(
                eyePosition.subtract(perpVec.x, 1.0, perpVec.z),
                targetVec.add(perpVec.x, 1.0, perpVec.z)
        );

        // Find living entities within the search box, excluding the player
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, searchBox, e -> e != livingEntity);

        // Find the closest living entity to the player
        return entities.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(livingEntity)));
    }

    public static List<LivingEntity> getNearbyLivingEntities(World world, Vec3d position, double radius) {
        Box searchBox = new Box(position.x - radius, position.y - radius, position.z - radius,
                position.x + radius, position.y + radius, position.z + radius);
        return world.getEntitiesByClass(LivingEntity.class, searchBox, entity -> true);
    }

    //Get entity attack damage
    public static double getEntityAttackDamage(LivingEntity livingEntity){
        EntityAttributeInstance attackDamageAttribute = livingEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamageAttribute != null) {
            return attackDamageAttribute.getValue();
        }
        return 0;
    }

    public static double[] getAttackFromSlot(PlayerEntity player, ItemStack stack, Hand hand) {
        double attackValue = 0;
        double attackSpeedValue = 0;
        AttributeModifierSlot attributeModifierSlot = hand == Hand.MAIN_HAND ? AttributeModifierSlot.MAINHAND : AttributeModifierSlot.OFFHAND;
        if (!stack.isEmpty()) {
            AttributeModifiersComponent attributeModifiersComponent = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            for (AttributeModifiersComponent.Entry entry : attributeModifiersComponent.modifiers()) {
                if (entry.attribute() == EntityAttributes.GENERIC_ATTACK_DAMAGE && entry.slot() == attributeModifierSlot) {
                    attackValue += entry.modifier().value();
                }
            }
        }

        return new double[] {attackValue, attackSpeedValue};
    }

    public static double getAttackFromStack(ItemStack stack, AttributeModifierSlot slot) {
        double attackValue = 0;
        if (stack != null && !stack.isEmpty()) {
            AttributeModifiersComponent attributeModifiersComponent = stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);
            for (AttributeModifiersComponent.Entry entry : attributeModifiersComponent.modifiers()) {
                if (entry.attribute() == EntityAttributes.GENERIC_ATTACK_DAMAGE && entry.slot() == slot) {
                    attackValue += entry.modifier().value();
                }
            }
        }
        return attackValue;
    }

    public static void applyDamageWithoutKnockback(LivingEntity target, DamageSource source, float amount) {
        EntityAttributeInstance knockbackResistance = target.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        double originalKnockbackResistance = 0;
        if (knockbackResistance != null) {
            originalKnockbackResistance = knockbackResistance.getValue();
            knockbackResistance.setBaseValue(1.0);
        }
        try {
            target.damage(source, amount);
        } finally {
            if (knockbackResistance != null) {
                knockbackResistance.setBaseValue(originalKnockbackResistance);
            }
        }
    }

    public static void spawnDirectionalParticles(ServerWorld world, ParticleEffect particle, Entity entity, int count, double distance) {
        Vec3d startPos = entity.getPos().add(0, entity.getHeight() / 2.0, 0);

        float pitch = entity.getPitch(1.0F);
        float yaw = entity.getYaw(1.0F);

        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);

        double xDirection = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double yDirection = -Math.sin(pitchRadians);
        double zDirection = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vec3d direction = new Vec3d(xDirection, yDirection, zDirection).normalize();

        for (int i = 0; i < count; i++) {
            double lerpFactor = (double) i / (count - 1);
            Vec3d currentPos = startPos.add(direction.multiply(distance * lerpFactor));
            world.spawnParticles(particle,
                    currentPos.x, currentPos.y, currentPos.z,
                    1,
                    0, 0, 0,
                    0.0);
        }
    }

    public static void spawnDirectionalParticles(ServerWorld world, ParticleEffect particle, Entity entity, Vec3d direction, int count, double distance) {
        Vec3d startPos = entity.getPos().add(0, entity.getHeight() / 2.0, 0);
        Vec3d normalizedDirection = direction.normalize();
        for (int i = 0; i < count; i++) {
            double lerpFactor = count <= 1 ? 0.0 : (double) i / (count - 1);
            Vec3d currentPos = startPos.add(normalizedDirection.multiply(distance * lerpFactor));
            world.spawnParticles(particle, currentPos.x, currentPos.y, currentPos.z, 1, 0, 0, 0, 0.0);
        }
    }

    public static void damageEntitiesInTrajectory(ServerWorld world, Entity sourceEntity, double distance, float damage, DamageSource damageSource) {
        damageEntitiesInTrajectory(world, sourceEntity, ItemStack.EMPTY, distance, damage, damageSource);
    }

    public static void damageEntitiesInTrajectory(ServerWorld world, Entity sourceEntity, ItemStack stack, double distance, float damage, DamageSource damageSource) {
        Vec3d startPos = sourceEntity.getPos().add(0, sourceEntity.getHeight() / 2.0, 0);
        float pitch = sourceEntity.getPitch(1.0F);
        float yaw = sourceEntity.getYaw(1.0F);

        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);

        double xDirection = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double yDirection = -Math.sin(pitchRadians);
        double zDirection = Math.cos(yawRadians) * Math.cos(pitchRadians);
        Vec3d direction = new Vec3d(xDirection, yDirection, zDirection).normalize();

        Vec3d endPos = startPos.add(direction.multiply(distance));

        double boxSize = 0.5;
        Box searchBox = new Box(startPos, endPos).expand(boxSize);

        for (Entity entity : world.getOtherEntities(sourceEntity, searchBox)) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            if (entityBox.intersects(searchBox)) {
                if ((sourceEntity instanceof PlayerEntity livingEntity)
                        && (entity instanceof LivingEntity livingTarget)
                        && HelperMethods.checkFriendlyFire(livingTarget, livingEntity)) {
                    livingTarget.damage(damageSource, applyAbilityDamageEnchantments(world, stack, livingTarget, damageSource, damage));
                }
            }
        }
    }

    public static void damageEntitiesInTrajectory(ServerWorld world, LivingEntity sourceEntity, Vec3d direction, double distance, float damage, DamageSource damageSource) {
        damageEntitiesInTrajectory(world, sourceEntity, ItemStack.EMPTY, direction, distance, damage, damageSource);
    }

    public static void damageEntitiesInTrajectory(ServerWorld world, LivingEntity sourceEntity, ItemStack stack, Vec3d direction, double distance, float damage, DamageSource damageSource) {
        Vec3d startPos = sourceEntity.getPos().add(0, sourceEntity.getHeight() / 2.0, 0);
        Vec3d normalizedDirection = direction.normalize();
        Vec3d endPos = startPos.add(normalizedDirection.multiply(distance));
        Box searchBox = new Box(startPos, endPos).expand(0.5);

        for (Entity entity : world.getOtherEntities(sourceEntity, searchBox)) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            if (entityBox.intersects(searchBox)
                    && entity instanceof LivingEntity livingTarget
                    && HelperMethods.checkAbilityTarget(livingTarget, sourceEntity)) {
                livingTarget.damage(damageSource, applyAbilityDamageEnchantments(world, stack, livingTarget, damageSource, damage));
            }
        }
    }

    // Ignore iFrames without resetting them entirely
    public static boolean damageThroughIframes(Entity targetEntity, DamageSource damageSource, float damage) {
        if (targetEntity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()))
            return false;
        int iframes = targetEntity.timeUntilRegen;
        boolean result = targetEntity.damage(damageSource, damage);
        targetEntity.timeUntilRegen = iframes;
        return result;
    }

    public static boolean isInTag(ItemStack stack, Identifier tagId) {
        // Check if the stack and its item registry entry exist, and if that entry is in the specified tag
        if (stack != null && !stack.isEmpty()) {
            var tag = TagKey.of(Registries.ITEM.getKey(), tagId);
            return stack.getItem().getRegistryEntry().isIn(tag);
        }
        return false;
    }

    public static boolean isHolding(ItemStack stack, LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.MAINHAND).equals(stack)
                || entity.getEquippedStack(EquipmentSlot.OFFHAND).equals(stack);
    }

    public static boolean isHoldingItem(Item item, LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.MAINHAND).getItem().equals(item)
                || entity.getEquippedStack(EquipmentSlot.OFFHAND).getItem().equals(item);
    }

    public static boolean hasItemInInventory(PlayerEntity player, Item item) {
        if (player == null || item == null)
            return false;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }


}
