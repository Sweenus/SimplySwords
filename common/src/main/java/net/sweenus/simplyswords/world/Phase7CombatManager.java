package net.sweenus.simplyswords.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase7AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase7UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityDefinition;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.minecraft.entity.damage.DamageSource;
import net.sweenus.simplyswords.entity.SimplySwordsAxolotlEntity;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.SimplySwords;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Phase7CombatManager {
    private static final Map<ServerWorld, List<SecondSkin>> SECOND_SKINS = new HashMap<>();
    private static final Map<UUID, Integer> HIVE_PROCS = new HashMap<>();
    private static final Map<ServerWorld, List<ExpiringAttribute>> ATTRIBUTES = new HashMap<>();
    private static final Map<UUID, KillWindow> AXOLOTL_KILLS = new HashMap<>();
    private static final Map<ServerWorld, Map<UUID, Long>> HIVE_REFUNDS = new HashMap<>();
    private static final Map<UUID, Long> CHOMP_CLEANSE_LOCKOUTS = new HashMap<>();
    private static final Map<UUID, Long> CHOMP_HELPFUL_LOCKOUTS = new HashMap<>();
    private static final Identifier WAX_RHYTHM = Identifier.of(SimplySwords.MOD_ID, "wax_rhythm");
    private static final Identifier WAX_FRENZY_DAMAGE = Identifier.of(SimplySwords.MOD_ID, "wax_frenzy_damage");
    private static final Identifier WAX_FRENZY_SPEED = Identifier.of(SimplySwords.MOD_ID, "wax_frenzy_speed");
    private static final Identifier HIVE_ESCORT_SPEED = Identifier.of(SimplySwords.MOD_ID, "hive_escort_speed");
    private Phase7CombatManager() {
    }

    public static UniqueAbilityExecution beginActive(UniqueAbilityDefinition definition,
                                                     WeaponAbilityContext context, int cooldown) {
        return UniqueAbilityApi.begin(definition, UniqueAbilityContext.active(context), tuning -> tuning
                .set(Phase7UniqueAbilities.TUNING, Phase7AbilityTuning.EMPTY)
                .set(Phase7UniqueAbilities.COOLDOWN_TICKS, cooldown));
    }

    public static UniqueAbilityExecution beginPassive(UniqueAbilityDefinition definition, ServerWorld world,
                                                      ItemStack stack, LivingEntity actor, LivingEntity target) {
        UniqueAbilityExecution execution = UniqueAbilityApi.begin(definition,
                UniqueAbilityContext.passive(world, stack, actor, target, null),
                tuning -> tuning.set(Phase7UniqueAbilities.TUNING, Phase7AbilityTuning.EMPTY));
        UniqueAbilityApi.takeStartedExecution();
        UniqueAbilityApi.start(execution);
        return execution;
    }

    public static float modifyIncomingDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target.getWorld() instanceof ServerWorld world)
                || !source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_PROJECTILE)) return amount;
        if (target instanceof net.minecraft.server.network.ServerPlayerEntity player) {
            for (net.minecraft.nbt.NbtCompound shoulder : List.of(
                    player.getShoulderEntityLeft(), player.getShoulderEntityRight())) {
                if ("simplyswords:simplyaxolotlentity".equals(shoulder.getString("id"))
                        && shoulder.getDouble("MasteryGuardRadius") > 0) {
                    return amount * (shoulder.contains("MasteryGuardMultiplier")
                            ? shoulder.getFloat("MasteryGuardMultiplier") : .85F);
                }
            }
        }
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
                if (entity instanceof SimplySwordsAxolotlEntity axolotl
                    && target.getUuid().equals(axolotl.getOwnerUuid())
                    && axolotl.getMasteryGuardRadius() > 0
                    && axolotl.squaredDistanceTo(target) <= axolotl.getMasteryGuardRadius()
                    * axolotl.getMasteryGuardRadius()) {
                return amount * axolotl.getMasteryGuardMultiplier();
            }
        }
        return amount;
    }

    public static void scheduleSecondSkin(ServerWorld world, LivingEntity entity, int delay,
                                          int duration, int absorption) {
        SECOND_SKINS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new SecondSkin(entity.getUuid(), world.getTime() + Math.max(0, delay),
                        Math.max(1, duration), Math.max(1, absorption)));
    }

    public static int nextHiveProc(LivingEntity actor) {
        return HIVE_PROCS.merge(actor.getUuid(), 1, (current, one) -> current >= 3 ? 1 : current + 1);
    }

    public static boolean claimHiveRefund(ServerWorld world, UUID releaseId, long expiresAt) {
        if (world == null || releaseId == null) return true;
        Map<UUID, Long> claims = HIVE_REFUNDS.computeIfAbsent(world, ignored -> new HashMap<>());
        if (claims.containsKey(releaseId)) return false;
        claims.put(releaseId, Math.max(world.getTime() + 1, expiresAt));
        return true;
    }

    public static void onDamageApplied(LivingEntity owner, DamageSource source) {
        triggerShoulderRescue(owner);
        triggerHelpfulFriend(owner);
        if (source.getAttacker() instanceof LivingEntity attacker) triggerHelpfulFriend(attacker);
        if (!(owner.getWorld() instanceof ServerWorld world)
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        ItemStack stack = owner.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.WAXWEAVER.get())
                || !net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) return;
        UniqueAbilityExecution execution = beginPassive(Phase7UniqueAbilities.WAXWEAVER_REVIVAL,
                world, stack, owner, attacker);
        if (!WaxweaverEncasementManager.tryReactiveShell(world, owner, attacker, stack,
                Phase7UniqueAbilities.tuning(execution), execution)) {
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
        }
    }

    public static boolean cleanse(LivingEntity owner, int effectCount, int lockoutTicks) {
        if (owner == null || effectCount <= 0 || !(owner.getWorld() instanceof ServerWorld world)
                || world.getTime() < CHOMP_CLEANSE_LOCKOUTS.getOrDefault(owner.getUuid(), 0L)) return false;
        List<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>> harmful =
                owner.getStatusEffects().stream()
                        .filter(effect -> effect.getEffectType().value().getCategory()
                                == net.minecraft.entity.effect.StatusEffectCategory.HARMFUL)
                        .map(net.minecraft.entity.effect.StatusEffectInstance::getEffectType)
                        .limit(effectCount).toList();
        harmful.forEach(owner::removeStatusEffect);
        if (harmful.isEmpty()) return false;
        CHOMP_CLEANSE_LOCKOUTS.put(owner.getUuid(), world.getTime() + Math.max(0, lockoutTicks));
        return true;
    }

    private static void triggerHelpfulFriend(LivingEntity actor) {
        if (!(actor instanceof net.minecraft.server.network.ServerPlayerEntity player)) return;
        ServerWorld world = player.getServerWorld();
        if (world.getTime() < CHOMP_HELPFUL_LOCKOUTS.getOrDefault(player.getUuid(), 0L)) return;
        int absorption = 0;
        int duration = 0;
        int lockout = 0;
        for (net.minecraft.nbt.NbtCompound shoulder : List.of(
                player.getShoulderEntityLeft(), player.getShoulderEntityRight())) {
            if (!"simplyswords:simplyaxolotlentity".equals(shoulder.getString("id"))) continue;
            absorption = Math.max(absorption, shoulder.getInt("MasteryHelpfulAbsorption"));
            duration = Math.max(duration, shoulder.getInt("MasteryHelpfulDuration"));
            lockout = Math.max(lockout, shoulder.getInt("MasteryHelpfulLockout"));
        }
        if (absorption <= 0 || duration <= 0) return;
        Phase4AbsorptionTracker.grant(player, absorption, duration, absorption);
        CHOMP_HELPFUL_LOCKOUTS.put(player.getUuid(), world.getTime() + Math.max(0, lockout));
    }

    private static void triggerShoulderRescue(LivingEntity actor) {
        if (!(actor instanceof net.minecraft.server.network.ServerPlayerEntity player)) return;
        for (net.minecraft.nbt.NbtCompound shoulder : List.of(
                player.getShoulderEntityLeft(), player.getShoulderEntityRight())) {
            if (!"simplyswords:simplyaxolotlentity".equals(shoulder.getString("id"))
                    || !shoulder.getBoolean("MasteryRescue")) continue;
            double threshold = shoulder.getDouble("MasteryRescueThreshold");
            if (threshold <= 0 || player.getHealth() / player.getMaxHealth() >= threshold) continue;
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                    net.minecraft.entity.effect.StatusEffects.RESISTANCE,
                    shoulder.getInt("MasteryRescueDuration"),
                    shoulder.getInt("MasteryRescueAmplifier"), false, true, true));
            shoulder.putBoolean("MasteryRescue", false);
            return;
        }
    }

    public static void applyWaxRhythm(ServerWorld world, LivingEntity actor, int duration, double bonus) {
        applyTemporaryModifier(world, actor, EntityAttributes.GENERIC_ATTACK_SPEED,
                WAX_RHYTHM, Math.max(0, bonus),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, duration);
    }

    public static void applyHiveEscort(ServerWorld world, LivingEntity actor, int duration,
                                       double speedMultiplier) {
        applyTemporaryModifier(world, actor, EntityAttributes.GENERIC_MOVEMENT_SPEED,
                HIVE_ESCORT_SPEED, Math.max(0, speedMultiplier - 1),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, duration);
    }

    public static void applyWaxFrenzy(ServerWorld world, LivingEntity actor, int duration,
                                      int stacks, double multiplier) {
        if (stacks <= 0 || multiplier <= 1) {
            clearWaxFrenzy(actor);
            return;
        }
        applyTemporaryModifier(world, actor, EntityAttributes.GENERIC_ATTACK_DAMAGE,
                WAX_FRENZY_DAMAGE, frenzyDamageBonus(stacks, multiplier),
                EntityAttributeModifier.Operation.ADD_VALUE, duration);
        applyTemporaryModifier(world, actor, EntityAttributes.GENERIC_ATTACK_SPEED,
                WAX_FRENZY_SPEED, frenzySpeedBonus(stacks, multiplier),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, duration);
    }

    public static double frenzyDamageBonus(int stacks, double multiplier) {
        return Math.max(0, 3 * (Math.max(0, stacks) + 1) * (multiplier - 1));
    }

    public static double frenzySpeedBonus(int stacks, double multiplier) {
        return Math.max(0, .1 * (Math.max(0, stacks) + 1) * (multiplier - 1));
    }

    public static void clearWaxFrenzy(LivingEntity actor) {
        if (actor == null) return;
        removeModifier(actor, WAX_FRENZY_DAMAGE);
        removeModifier(actor, WAX_FRENZY_SPEED);
        ATTRIBUTES.values().forEach(attributes -> attributes.removeIf(attribute ->
                attribute.ownerId.equals(actor.getUuid())
                        && (attribute.id.equals(WAX_FRENZY_DAMAGE)
                        || attribute.id.equals(WAX_FRENZY_SPEED))));
        ATTRIBUTES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void onAxolotlKill(ServerWorld world, LivingEntity owner, int required,
                                    int windowTicks, int refundTicks) {
        if (!(owner instanceof net.minecraft.entity.player.PlayerEntity player) || required <= 0) return;
        KillWindow current = AXOLOTL_KILLS.get(owner.getUuid());
        int count = current != null && world.getTime() - current.startedAt <= windowTicks
                ? current.count + 1 : 1;
        if (count < required) {
            AXOLOTL_KILLS.put(owner.getUuid(), new KillWindow(
                    current != null && world.getTime() - current.startedAt <= windowTicks
                            ? current.startedAt : world.getTime(), count));
            return;
        }
        AXOLOTL_KILLS.remove(owner.getUuid());
        net.sweenus.simplyswords.api.SimplySwordsAPI.reduceWeaponCooldown(player,
                new ItemStack(ItemsRegistry.CHOMPOLOTL.get()),
                net.sweenus.simplyswords.config.Config.uniqueEffects.chompolotl.cooldown * 10,
                refundTicks);
    }

    public static boolean hasScheduled(ServerWorld world) {
        List<SecondSkin> skins = SECOND_SKINS.get(world);
        List<ExpiringAttribute> attributes = ATTRIBUTES.get(world);
        return skins != null && !skins.isEmpty() || attributes != null && !attributes.isEmpty();
    }

    public static void tick(ServerWorld world) {
        Map<UUID, Long> refunds = HIVE_REFUNDS.get(world);
        if (refunds != null) {
            refunds.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
            if (refunds.isEmpty()) HIVE_REFUNDS.remove(world);
        }
        List<SecondSkin> skins = SECOND_SKINS.get(world);
        if (skins != null) {
            skins.removeIf(skin -> {
                if (world.getTime() < skin.at) return false;
                net.minecraft.entity.Entity entity = world.getEntity(skin.ownerId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    Phase4AbsorptionTracker.grant(living, skin.absorption, skin.duration, skin.absorption);
                }
                return true;
            });
            if (skins.isEmpty()) SECOND_SKINS.remove(world);
        }
        List<ExpiringAttribute> attributes = ATTRIBUTES.get(world);
        if (attributes != null) {
            attributes.removeIf(attribute -> {
                if (world.getTime() < attribute.at) return false;
                net.minecraft.entity.Entity entity = world.getEntity(attribute.ownerId);
                if (entity instanceof LivingEntity living) {
                    removeModifier(living, attribute.id);
                }
                return true;
            });
            if (attributes.isEmpty()) ATTRIBUTES.remove(world);
        }
    }

    public static void clear(ServerWorld world) {
        if (world == null) return;
        SECOND_SKINS.remove(world);
        HIVE_REFUNDS.remove(world);
        List<ExpiringAttribute> attributes = ATTRIBUTES.remove(world);
        if (attributes != null) {
            for (ExpiringAttribute attribute : attributes) {
                net.minecraft.entity.Entity entity = world.getEntity(attribute.ownerId);
                if (entity instanceof LivingEntity living) removeModifier(living, attribute.id);
            }
        }
    }

    public static void clearAll() {
        List<ServerWorld> worlds = new ArrayList<>(SECOND_SKINS.keySet());
        for (ServerWorld world : ATTRIBUTES.keySet()) if (!worlds.contains(world)) worlds.add(world);
        for (ServerWorld world : worlds) clear(world);
        HIVE_PROCS.clear();
        AXOLOTL_KILLS.clear();
        HIVE_REFUNDS.clear();
        CHOMP_CLEANSE_LOCKOUTS.clear();
        CHOMP_HELPFUL_LOCKOUTS.clear();
        HivemindSwarmManager.clearAll();
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        SECOND_SKINS.values().forEach(skins -> skins.removeIf(skin -> skin.ownerId.equals(actorId)));
        ATTRIBUTES.values().forEach(attributes -> attributes.removeIf(attribute -> {
            if (!attribute.ownerId.equals(actorId)) return false;
            removeModifier(actor, attribute.id);
            return true;
        }));
        SECOND_SKINS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ATTRIBUTES.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        HIVE_PROCS.remove(actorId);
        AXOLOTL_KILLS.remove(actorId);
        CHOMP_CLEANSE_LOCKOUTS.remove(actorId);
        CHOMP_HELPFUL_LOCKOUTS.remove(actorId);
        HivemindSwarmManager.clearActor(actor);
    }

    private static void applyTemporaryModifier(ServerWorld world, LivingEntity actor,
                                               RegistryEntry<EntityAttribute> attribute,
                                               Identifier id, double amount,
                                               EntityAttributeModifier.Operation operation,
                                               int duration) {
        EntityAttributeInstance instance = actor.getAttributeInstance(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        instance.addTemporaryModifier(new EntityAttributeModifier(id, amount, operation));
        List<ExpiringAttribute> attributes = ATTRIBUTES.computeIfAbsent(world, ignored -> new ArrayList<>());
        attributes.removeIf(existing -> existing.ownerId.equals(actor.getUuid()) && existing.id.equals(id));
        attributes.add(new ExpiringAttribute(actor.getUuid(),
                world.getTime() + Math.max(1, duration), id));
    }

    private static void removeModifier(LivingEntity actor, Identifier id) {
        EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.removeModifier(id);
        EntityAttributeInstance attackDamage = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.removeModifier(id);
        EntityAttributeInstance movementSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) movementSpeed.removeModifier(id);
    }

    private record SecondSkin(UUID ownerId, long at, int duration, int absorption) {
    }

    private record ExpiringAttribute(UUID ownerId, long at, Identifier id) {
    }

    private record KillWindow(long startedAt, int count) {
    }
}
