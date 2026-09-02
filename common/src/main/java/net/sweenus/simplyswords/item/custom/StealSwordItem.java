package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.ability.Phase8AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase8UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsSkeletonMinionEntity;
import net.sweenus.simplyswords.entity.SimplySwordsWolfMinionEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.Phase8CombatManager;

import java.util.List;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.function.Predicate;

public class StealSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    private static final double BACKSTAB_DISTANCE = 1.35;
    private static final double TARGET_LENIENCY = 0.75;
    private static final ThreadLocal<Boolean> SUPPRESS_SOUL_DEBT_GAIN = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, IdentityHashMap<ItemStack, Integer>> DEBT_PROC_COUNTERS = new HashMap<>();
    private static final Map<UUID, Map<UUID, MarkedAsset>> MARKED_ASSETS = new HashMap<>();
    private static final Map<UUID, Long> VEILED_ACTORS = new HashMap<>();
    private static final Map<UUID, ServerWorld> STATE_WORLDS = new HashMap<>();

    public StealSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient() && !SUPPRESS_SOUL_DEBT_GAIN.get()) {
            ServerWorld sworld = (ServerWorld) attacker.getWorld();
            STATE_WORLDS.put(attacker.getUuid(), sworld);
            HelperMethods.playHitSounds(attacker, target);
            UniqueAbilityExecution execution = Phase8CombatManager.beginPassive(
                    Phase8UniqueAbilities.SOULSTEALER_DEBT, sworld, stack, attacker, target);
            Phase8AbilityTuning tuning = Phase8UniqueAbilities.tuning(execution);

            int chance = soulDebtChance(Config.uniqueEffects.soulstealer.chance, tuning);
            Map<UUID, MarkedAsset> assets = MARKED_ASSETS.computeIfAbsent(attacker.getUuid(), ignored -> new HashMap<>());
            assets.entrySet().removeIf(entry -> entry.getValue().expiry <= sworld.getTime());
            MarkedAsset asset = assets.get(target.getUuid());
            int hits = asset == null ? 1 : asset.hits + 1;
            if (tuning.flag(1 << 5)) {
                int markHits = tuning.integer(s("SOULSTEALER_MARK_HITS"), 2);
                int markDuration = tuning.integer(s("SOULSTEALER_MARK_DURATION_TICKS"), 80);
                if (hits >= markHits) {
                    chance = Math.min(100, chance + tuning.integer(s("SOULSTEALER_MARK_CHANCE_BONUS"), 15));
                }
                assets.put(target.getUuid(), new MarkedAsset(hits, sworld.getTime() + markDuration));
            }
            if (assets.size() > 32) assets.entrySet().stream()
                    .min(Map.Entry.comparingByValue(java.util.Comparator.comparingLong(MarkedAsset::expiry)))
                    .ifPresent(entry -> assets.remove(entry.getKey()));
            if (assets.isEmpty()) MARKED_ASSETS.remove(attacker.getUuid());
            if (attacker.getRandom().nextInt(100) < chance) {
                int hitDebt = tuning.has(s("SOULSTEALER_HIT_DEBT_OVERRIDE"))
                        ? tuning.integer(s("SOULSTEALER_HIT_DEBT_OVERRIDE"), 1)
                        : Config.uniqueEffects.soulstealer.hitStacks;
                addSoulDebt(stack, hitDebt, tuning, attacker);
                if (tuning.flag(1 << 3)) {
                    IdentityHashMap<ItemStack, Integer> counters = DEBT_PROC_COUNTERS.computeIfAbsent(
                            attacker.getUuid(), ignored -> new IdentityHashMap<>());
                    int count = counters.merge(stack, 1, Integer::sum);
                    int interval = tuning.integer(s("SOULSTEALER_COMPOUND_INTERVAL"), 3);
                    if (count >= interval) {
                        counters.put(stack, 0);
                        addSoulDebt(stack, tuning.integer(s("SOULSTEALER_COMPOUND_DEBT"), 1), tuning, attacker);
                    }
                }
                spawnSoulDebtGainEffects(sworld, target, attacker, stack);
            }
            if (!target.isAlive()) {
                addSoulDebt(stack, killDebt(tuning), tuning, attacker);
                spawnSoulDebtGainEffects(sworld, target, attacker, stack);
            }
            UniqueAbilityApi.finish(execution, Phase8UniqueAbilities.FINISH, 1);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && world instanceof ServerWorld serverWorld
                && user instanceof ServerPlayerEntity player) {
            WeaponAbilityContext context = WeaponAbilityContext.of(serverWorld, stack, player, null,
                    null, hand, WeaponAbilityActivationSource.PLAYER);
            return SimplySwordsAPI.tryActivateWeaponAbility(context)
                    ? TypedActionResult.success(stack, false) : TypedActionResult.fail(stack);
        }
        return TypedActionResult.success(stack, true);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        if (context == null || !UniqueWeaponActiveAbility.super.canActivate(context)) {
            return false;
        }
        return getSoulDebt(context.stack()) > 0;
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        int stacks = getSoulDebt(context.stack());
        UniqueAbilityExecution approach = Phase8CombatManager.beginActive(
                Phase8UniqueAbilities.SOULSTEALER_APPROACH, context, Config.uniqueEffects.soulstealer.cooldown);
        UniqueAbilityApi.takeStartedExecution();
        Phase8AbilityTuning approachTuning = Phase8UniqueAbilities.tuning(approach);
        LivingEntity target = findSoulstealerTarget(context.actor(), context.target(), approachTuning);
        if (target == null) {
            UniqueAbilityApi.cancel(approach);
            return false;
        }
        Vec3d strikePos = findBackstabPosition(context.world(), context.actor(), target, approachTuning);
        if (strikePos == null) {
            UniqueAbilityApi.cancel(approach);
            return false;
        }
        UniqueAbilityApi.start(approach);
        WeaponAbilityContext resolved = WeaponAbilityContext.of(context.world(), context.stack(), context.actor(),
                context.sourcePlayer(), target, context.hand(), context.activationSource());
        UniqueAbilityExecution reap = Phase8CombatManager.beginActive(
                Phase8UniqueAbilities.SOULSTEALER_REAP, resolved, Config.uniqueEffects.soulstealer.cooldown);
        Phase8AbilityTuning reapTuning = Phase8UniqueAbilities.tuning(reap);
        if (approachTuning.flag(1 << 12)) grantVeil(context.actor(), context.world(), approachTuning);
        Vec3d targetOrigin = target.getPos();
        Runnable action = () -> completeSoulReap(context.world(), context.actor(), context.stack(), target,
                strikePos, targetOrigin, stacks, approachTuning, reapTuning, approach, reap);
        if (approachTuning.flag(1 << 14)) {
            Phase8CombatManager.scheduleAction(context.world(), context.actor(), 1, action, () -> {
                UniqueAbilityApi.cancel(approach);
                UniqueAbilityApi.cancel(reap);
            });
        } else {
            action.run();
        }
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.soulstealer.cooldown;
    }

    private static void completeSoulReap(ServerWorld world, LivingEntity actor, ItemStack stack,
                                         LivingEntity target, Vec3d strikePos, Vec3d targetOrigin, int stacks,
                                         Phase8AbilityTuning approachTuning, Phase8AbilityTuning reapTuning,
                                         UniqueAbilityExecution approach, UniqueAbilityExecution reap) {
        if (!actor.isAlive() || !target.isAlive() || actor.getWorld() != world || target.getWorld() != world) {
            applyFailedReapCost(stack, stacks, reapTuning);
            UniqueAbilityApi.finish(approach, Phase8UniqueAbilities.FINISH, 0);
            Phase8CombatManager.scheduleFinish(world, reap, 1, 0);
            return;
        }
        Vec3d resolvedStrike = strikePos;
        if (approachTuning.flag(1 << 14)) {
            double followRange = approachTuning.get(s("SOULSTEALER_TETHER_RANGE"), 3);
            Vec3d movement = target.getPos().subtract(targetOrigin);
            if (movement.lengthSquared() > followRange * followRange) {
                movement = movement.normalize().multiply(followRange);
            }
            Vec3d followed = strikePos.add(movement);
            if (isSafePosition(world, actor, followed)) resolvedStrike = followed;
        }
        if (!isSafePosition(world, actor, resolvedStrike)) {
            applyFailedReapCost(stack, stacks, reapTuning);
            spawnFailEffects(world, actor);
            UniqueAbilityApi.finish(approach, Phase8UniqueAbilities.FINISH, 0);
            Phase8CombatManager.scheduleFinish(world, reap, 1, 0);
            return;
        }
        Vec3d departure = actor.getPos();
        boolean damaged = performSoulReap(world, actor, stack, target, resolvedStrike, stacks,
                approachTuning, reapTuning, reap);
        if (approachTuning.flag(1 << 17)) {
            Phase8CombatManager.scheduleReturn(world, actor, departure,
                    approachTuning.integer(s("SOULSTEALER_RETURN_DELAY_TICKS"), 12),
                    approachTuning.integer(s("SOULSTEALER_RETURN_RESISTANCE_TICKS"), 40),
                    approachTuning.integer(s("SOULSTEALER_RETURN_RESISTANCE_AMPLIFIER"), 1));
        }
        UniqueAbilityApi.finish(approach, Phase8UniqueAbilities.FINISH, damaged ? 1 : 0);
        Phase8CombatManager.scheduleFinish(world, reap, 1, damaged ? 1 : 0);
    }

    private static boolean performSoulReap(ServerWorld world, LivingEntity actor, ItemStack stack,
                                           LivingEntity target, Vec3d strikePos, int stacks,
                                           Phase8AbilityTuning approachTuning,
                                           Phase8AbilityTuning reapTuning,
                                           UniqueAbilityExecution execution) {
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        Vec3d strikeEyePos = strikePos.add(0.0, actor.getEyeHeight(actor.getPose()), 0.0);
        float[] rotation = getFacingRotation(strikeEyePos, lookTarget);
        spawnDepartureEffects(world, actor);
        if (actor instanceof ServerPlayerEntity player) {
            player.networkHandler.requestTeleport(strikePos.x, strikePos.y, strikePos.z, rotation[0], rotation[1]);
        } else {
            actor.refreshPositionAndAngles(strikePos.x, strikePos.y, strikePos.z, rotation[0], rotation[1]);
        }
        actor.setYaw(rotation[0]);
        actor.setPitch(rotation[1]);
        actor.setHeadYaw(rotation[0]);
        actor.setBodyYaw(rotation[0]);
        actor.swingHand(Hand.MAIN_HAND, true);
        actor.setVelocity(0.0, 0.0, 0.0);
        actor.velocityModified = true;

        ReapProfile profile = reapProfile(stacks, approachTuning, reapTuning);
        int consumed = profile.consumed;
        float multiplier = profile.multiplier;
        float damage = HelperMethods.abilityScaledDamage("soul", actor, stack,
                multiplier, multiplier * Config.uniqueEffects.soulstealer.spellScalingPerMultiplier
                        * (float) reapTuning.get(s("SOULSTEALER_SPELL_MULTIPLIER"), 1));
        if (reapTuning.has(s("SOULSTEALER_ARMOR_IGNORE_RATIO"))) {
            float toughness = target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS) > 0
                    ? (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS) : 0;
            float ignored = target.getArmor()
                    * (float) reapTuning.get(s("SOULSTEALER_ARMOR_IGNORE_RATIO"), .1);
            damage *= armorIgnoreMultiplier(damage, target.getArmor(), toughness, ignored);
        }
        damage = HelperMethods.applyNonPlayerAbilityDamageModifier(actor, damage);
        DamageSource damageSource = SimplySwordsAPI.getWeaponDamageSource(actor);
        target.timeUntilRegen = 0;
        boolean damaged = target.damage(damageSource, damage);
        if (damaged) {
            setSoulDebt(stack, Math.max(0, stacks - consumed));
            SUPPRESS_SOUL_DEBT_GAIN.set(true);
            try {
                stack.getItem().postHit(stack, target, actor);
            } finally {
                SUPPRESS_SOUL_DEBT_GAIN.set(false);
            }
            if (!target.isAlive()) {
                addSoulDebt(stack, killDebt(reapTuning), reapTuning, actor);
                if (reapTuning.flag(1 << 24)) {
                    addSoulDebt(stack, reapTuning.integer(s("SOULSTEALER_DEATH_TAX_DEBT"), 2),
                            reapTuning, actor);
                }
                int refund = 0;
                if (approachTuning.flag(1 << 15)) refund += approachTuning.integer(
                        s("SOULSTEALER_PREDATORY_REFUND_TICKS"), 80);
                if (reapTuning.flag(1 << 24)) refund += reapTuning.integer(
                        s("SOULSTEALER_DEATH_TAX_REFUND_TICKS"), 40);
                Phase8CombatManager.scheduleCooldownRefund(world, actor, stack, refund);
                UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                        Phase8UniqueAbilities.KILL, target, 1, damage);
            }
            if (reapTuning.flag(1 << 21)) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,
                        reapTuning.integer(s("SOULSTEALER_WITHER_DURATION_TICKS"), 60),
                        reapTuning.integer(s("SOULSTEALER_WITHER_AMPLIFIER"), 0)), actor);
            }
            if (approachTuning.flag(1 << 13)) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,
                        approachTuning.integer(s("SOULSTEALER_HAMSTRING_DURATION_TICKS"), 30),
                        approachTuning.integer(s("SOULSTEALER_HAMSTRING_AMPLIFIER"), 2)), actor);
            }
            if (reapTuning.flag(1 << 23)) {
                addSoulDebt(stack, reapTuning.integer(s("SOULSTEALER_RESIDUAL_DEBT"), 1),
                        reapTuning, actor);
            }
            UniqueAbilityApi.emit(execution, net.sweenus.simplyswords.api.ability.UniqueAbilityPhase.HIT,
                    Phase8UniqueAbilities.HIT, target, 1, damage);
            spawnBackstabEffects(world, target, actor, stacks);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                    SoundCategory.PLAYERS, 0.65F, 0.75F + world.random.nextFloat() * 0.2F);
            return true;
        } else {
            applyFailedReapCost(stack, stacks, reapTuning);
            spawnFailEffects(world, actor);
        }
        return false;
    }

    static ReapProfile reapProfile(int stacks, Phase8AbilityTuning approachTuning,
                                   Phase8AbilityTuning reapTuning) {
        return reapProfile(stacks, approachTuning, reapTuning,
                Config.uniqueEffects.soulstealer.maxStacks,
                Config.uniqueEffects.soulstealer.minBackstabMultiplier,
                Config.uniqueEffects.soulstealer.maxBackstabMultiplier);
    }

    static ReapProfile reapProfile(int stacks, Phase8AbilityTuning approachTuning,
                                   Phase8AbilityTuning reapTuning, int configuredMaximum,
                                   float minimumMultiplier, float maximumMultiplier) {
        int maximum = maximumDebt(configuredMaximum, reapTuning);
        int consumed = reapTuning.flag(1 << 26)
                ? Math.min(stacks, reapTuning.integer(s("SOULSTEALER_INSTALLMENT_MAX_SPEND"), 3)) : stacks;
        float multiplier = getBackstabMultiplier(consumed, maximum, minimumMultiplier, maximumMultiplier);
        double debtBonus = reapTuning.get(s("SOULSTEALER_DAMAGE_PER_DEBT_BONUS"), 0) * consumed;
        if (reapTuning.flag(1 << 25)) {
            int foreclosureCap = reapTuning.integer(s("SOULSTEALER_FORECLOSURE_STACK_CAP"), 12);
            debtBonus += reapTuning.get(s("SOULSTEALER_FORECLOSURE_DAMAGE_PER_DEBT"), .2)
                    * Math.min(consumed, foreclosureCap);
        }
        multiplier *= 1.0F + (float) debtBonus;
        if (reapTuning.flag(1 << 22) && stacks == maximum && consumed == stacks) {
            multiplier *= reapTuning.get(s("SOULSTEALER_EXACT_PAYMENT_MULTIPLIER"), 1.25);
        }
        if (reapTuning.flag(1 << 26)) {
            multiplier *= reapTuning.get(s("SOULSTEALER_INSTALLMENT_DAMAGE_MULTIPLIER"), .8);
        }
        if (approachTuning.flag(1 << 16)) {
            multiplier = Math.min(multiplier,
                    (float) approachTuning.get(s("SOULSTEALER_PURSUIT_DAMAGE_CAP"), 3));
        }
        if (approachTuning.flag(1 << 17)) {
            multiplier *= approachTuning.get(s("SOULSTEALER_ESCAPE_DAMAGE_MULTIPLIER"), .75);
        }
        return new ReapProfile(consumed, multiplier);
    }

    private static void applyFailedReapCost(ItemStack stack, int stacks, Phase8AbilityTuning tuning) {
        if (!tuning.flag(1 << 25)) return;
        int spent = (int) Math.ceil(stacks * tuning.get(
                s("SOULSTEALER_FORECLOSURE_FAILURE_SPEND"), .5));
        setSoulDebt(stack, Math.max(0, stacks - spent));
    }

    public static LivingEntity findSoulstealerTarget(PlayerEntity player) {
        return findSoulstealerTarget(player, null, Phase8AbilityTuning.EMPTY);
    }

    private static LivingEntity findSoulstealerTarget(LivingEntity actor, LivingEntity preferred,
                                                       Phase8AbilityTuning tuning) {
        double visibleRange = Config.uniqueEffects.soulstealer.range
                + tuning.get(s("SOULSTEALER_TARGET_RANGE_BONUS"), 0);
        double pursuitRange = tuning.flag(1 << 16)
                ? tuning.get(s("SOULSTEALER_PURSUIT_RANGE"), 8) : 0;
        double searchRange = Math.max(visibleRange, pursuitRange);
        if (preferred != null && isTargetInApproach(actor, preferred, visibleRange, pursuitRange, tuning)) {
            return preferred;
        }
        Vec3d origin = actor.getEyePos();
        Vec3d facing = actor.getRotationVec(1.0F).normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity entity : actor.getWorld().getOtherEntities(actor,
                actor.getBoundingBox().expand(searchRange), candidate -> candidate instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity target)
                    || !isTargetInApproach(actor, target, visibleRange, pursuitRange, tuning)) {
                continue;
            }
            Vec3d offset = target.getBoundingBox().getCenter().subtract(origin);
            double distance = offset.length();
            if (distance <= 0.001) continue;
            double angle = Math.acos(MathHelper.clamp(facing.dotProduct(offset.normalize()), -1, 1));
            double score = angle * angle * 16 + distance * distance;
            if (score < bestScore) {
                bestScore = score;
                best = target;
            }
        }
        return best;
    }

    private static boolean isTargetInApproach(LivingEntity actor, LivingEntity target, double visibleRange,
                                               double pursuitRange, Phase8AbilityTuning tuning) {
        if (!isValidSoulstealerTarget(target, actor)) return false;
        Vec3d offset = target.getBoundingBox().getCenter().subtract(actor.getEyePos());
        double distance = offset.length();
        boolean visible = actor.canSee(target);
        if (visible ? distance > visibleRange : pursuitRange <= 0 || distance > pursuitRange) return false;
        Vec3d facing = actor.getRotationVec(1.0F).normalize();
        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(
                facing.dotProduct(offset.normalize()), -1, 1)));
        double base = Math.toDegrees(Math.atan((target.getWidth() * .5 + TARGET_LENIENCY)
                / Math.max(.001, distance)));
        return angle <= base + tuning.get(s("SOULSTEALER_TARGET_ANGLE_BONUS"), 0);
    }

    public static LivingEntity findLenientTarget(PlayerEntity player, double range) {
        return findLenientTarget(player, range, target -> isValidSoulstealerTarget(target, player));
    }

    public static LivingEntity findLenientTarget(PlayerEntity player, double range,
                                                  Predicate<LivingEntity> targetPredicate) {
        if (player == null || targetPredicate == null) {
            return null;
        }
        Entity targeted = HelperMethods.getTargetedEntity(player, range);
        if (targeted instanceof LivingEntity livingTarget
                && targetPredicate.test(livingTarget)) {
            return livingTarget;
        }

        Vec3d origin = player.getEyePos();
        Vec3d end = origin.add(player.getRotationVec(1.0F).normalize().multiply(range));
        Box searchBox = player.getBoundingBox().stretch(player.getRotationVec(1.0F).normalize().multiply(range)).expand(range);
        LivingEntity closestTarget = null;
        double closestDistance = range * range;
        for (Entity entity : player.getWorld().getOtherEntities(player, searchBox, entity -> entity instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity livingTarget) || !targetPredicate.test(livingTarget)) {
                continue;
            }

            Optional<Vec3d> hitPos = livingTarget.getBoundingBox().expand(TARGET_LENIENCY).raycast(origin, end);
            if (hitPos.isEmpty()) {
                continue;
            }

            double distance = origin.squaredDistanceTo(hitPos.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTarget = livingTarget;
            }
        }
        return closestTarget;
    }

    private static boolean isValidSoulstealerTarget(LivingEntity target, LivingEntity actor) {
        return target.isAlive()
                && !(target instanceof SimplySwordsSkeletonMinionEntity)
                && !(target instanceof SimplySwordsWolfMinionEntity)
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkAbilityTarget(target, actor);
    }

    private static Vec3d findBackstabPosition(ServerWorld world, LivingEntity actor, LivingEntity target) {
        return findBackstabPosition(world, actor, target, Phase8AbilityTuning.EMPTY);
    }

    private static Vec3d findBackstabPosition(ServerWorld world, LivingEntity actor, LivingEntity target,
                                              Phase8AbilityTuning tuning) {
        Vec3d behind = target.getRotationVec(1.0F);
        behind = new Vec3d(behind.x, 0.0, behind.z);
        if (behind.horizontalLengthSquared() < 0.001) {
            behind = target.getPos().subtract(actor.getPos());
        }
        if (behind.horizontalLengthSquared() < 0.001) {
            behind = new Vec3d(0.0, 0.0, 1.0);
        }
        behind = behind.normalize().multiply(-BACKSTAB_DISTANCE);
        Vec3d side = new Vec3d(-behind.z, 0.0, behind.x).normalize();
        List<Vec3d> candidates = new java.util.ArrayList<>();
        candidates.add(target.getPos().add(behind));
        candidates.add(target.getPos().add(behind).add(side.multiply(0.45)));
        candidates.add(target.getPos().add(behind).subtract(side.multiply(0.45)));
        candidates.add(target.getPos().add(behind.normalize().multiply(BACKSTAB_DISTANCE * 0.75)));
        int searchCap = tuning.integer(s("SOULSTEALER_SEARCH_CAP"), 24);
        int checks = Math.clamp((int) Math.round(4
                * tuning.get(s("SOULSTEALER_SEARCH_MULTIPLIER"), 1)), 4, searchCap);
        for (int i = 4; i < checks; i++) {
            double angle = MathHelper.TAU * i / checks;
            candidates.add(target.getPos().add(Math.cos(angle) * BACKSTAB_DISTANCE, 0,
                    Math.sin(angle) * BACKSTAB_DISTANCE));
        }

        for (Vec3d candidate : candidates) {
            Vec3d grounded = new Vec3d(candidate.x, target.getY(), candidate.z);
            if (isSafePosition(world, actor, grounded)) {
                return grounded;
            }
        }
        return null;
    }

    private static boolean isSafePosition(ServerWorld world, LivingEntity actor, Vec3d pos) {
        Box actorBox = actor.getBoundingBox().offset(pos.subtract(actor.getPos()));
        return world.isSpaceEmpty(actor, actorBox) && !world.getBlockState(BlockPos.ofFloored(pos)).isLiquid();
    }

    private static float[] getFacingRotation(Vec3d fromEye, Vec3d to) {
        Vec3d diff = to.subtract(fromEye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new float[]{yaw, pitch};
    }

    static float getBackstabMultiplier(int stacks, int maxStacks) {
        return getBackstabMultiplier(stacks, maxStacks,
                Config.uniqueEffects.soulstealer.minBackstabMultiplier,
                Config.uniqueEffects.soulstealer.maxBackstabMultiplier);
    }

    static float getBackstabMultiplier(int stacks, int maxStacks, float minMultiplier, float maxMultiplier) {
        maxStacks = Math.max(1, maxStacks);
        maxMultiplier = Math.max(minMultiplier, maxMultiplier);
        if (maxStacks <= 1) {
            return maxMultiplier;
        }
        float progress = MathHelper.clamp((float) (Math.min(stacks, maxStacks) - 1) / (float) (maxStacks - 1), 0.0F, 1.0F);
        return MathHelper.lerp(progress, minMultiplier, maxMultiplier);
    }

    public static int getSoulDebt(ItemStack stack) {
        return stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
    }

    public static int maximumDebt(Phase8AbilityTuning tuning) {
        return maximumDebt(Config.uniqueEffects.soulstealer.maxStacks, tuning);
    }

    static int maximumDebt(int configuredMaximum, Phase8AbilityTuning tuning) {
        return Math.max(1, configuredMaximum
                + tuning.integer(s("SOULSTEALER_MAX_DEBT_BONUS"), 0));
    }

    private static void addSoulDebt(ItemStack stack, int amount, Phase8AbilityTuning tuning,
                                    LivingEntity collector) {
        if (amount <= 0) {
            return;
        }
        int maxStacks = maximumDebt(tuning);
        int current = getSoulDebt(stack);
        int updated = Math.min(maxStacks, current + amount);
        setSoulDebt(stack, updated);
        if (collector != null && tuning.flag(1 << 6) && updated > current) {
            int baseMaximum = Math.max(1, Config.uniqueEffects.soulstealer.maxStacks);
            int overflow = Math.max(0, updated - baseMaximum) - Math.max(0, current - baseMaximum);
            if (overflow > 0) {
                int perStack = tuning.integer(s("SOULSTEALER_COLLECTOR_TICKS_PER_STACK"), 40);
                int cap = tuning.integer(s("SOULSTEALER_COLLECTOR_DURATION_CAP_TICKS"), 120);
                StatusEffectInstance existing = collector.getStatusEffect(StatusEffects.ABSORPTION);
                int remaining = existing == null || existing.getAmplifier() > 0 ? 0 : existing.getDuration();
                collector.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                        Math.min(cap, remaining + overflow * perStack), 0), collector);
            }
        }
    }

    public static void setSoulDebt(ItemStack stack, int amount) {
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), new StoredChargeComponent(Math.max(0, amount)));
    }

    static int soulDebtChance(int configuredChance, Phase8AbilityTuning tuning) {
        if (tuning.flag(1 << 7)) return 100;
        double chance = configuredChance + tuning.get(s("SOULSTEALER_CHANCE_BONUS"), 0);
        chance *= tuning.get(s("SOULSTEALER_CHANCE_MULTIPLIER"), 1);
        return Math.clamp((int) Math.round(chance), 0, 100);
    }

    static int killDebt(Phase8AbilityTuning tuning) {
        return killDebt(Config.uniqueEffects.soulstealer.killStacks, tuning);
    }

    static int killDebt(int configuredKillDebt, Phase8AbilityTuning tuning) {
        int base = tuning.has(s("SOULSTEALER_KILL_DEBT_OVERRIDE"))
                ? tuning.integer(s("SOULSTEALER_KILL_DEBT_OVERRIDE"), 3)
                : configuredKillDebt;
        return Math.max(0, base + tuning.integer(s("SOULSTEALER_KILL_DEBT_BONUS"), 0));
    }

    static float armorIgnoreMultiplier(float damage, float armor, float toughness, float ignored) {
        if (ignored <= 0 || armor <= 0 || damage <= 0) return 1.0F;
        float full = afterArmor(damage, armor, toughness);
        if (full <= 0) return 1.0F;
        return afterArmor(damage, Math.max(0, armor - ignored), toughness) / full;
    }

    private static float afterArmor(float damage, float armor, float toughness) {
        float divisor = 2.0F + toughness / 4.0F;
        float effective = MathHelper.clamp(armor - damage / divisor, armor * .2F, 20.0F);
        return damage * (1.0F - effective / 25.0F);
    }

    private static void grantVeil(LivingEntity actor, ServerWorld world, Phase8AbilityTuning tuning) {
        int duration = tuning.integer(s("SOULSTEALER_VEIL_DURATION_TICKS"), 20);
        if (actor.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration, 0), actor)) {
            VEILED_ACTORS.put(actor.getUuid(), world.getTime() + duration);
            STATE_WORLDS.put(actor.getUuid(), world);
        }
    }

    public static void onAttack(LivingEntity actor) {
        if (actor == null || !(actor.getWorld() instanceof ServerWorld world)) return;
        Long expiry = VEILED_ACTORS.remove(actor.getUuid());
        if (expiry != null && expiry >= world.getTime()) actor.removeStatusEffect(StatusEffects.INVISIBILITY);
    }

    public static void clearActor(LivingEntity actor) {
        if (actor == null) return;
        UUID actorId = actor.getUuid();
        DEBT_PROC_COUNTERS.remove(actorId);
        MARKED_ASSETS.remove(actorId);
        VEILED_ACTORS.remove(actorId);
        STATE_WORLDS.remove(actorId);
        MARKED_ASSETS.values().forEach(assets -> assets.remove(actorId));
        MARKED_ASSETS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public static void clearAllState() {
        DEBT_PROC_COUNTERS.clear();
        MARKED_ASSETS.clear();
        VEILED_ACTORS.clear();
        STATE_WORLDS.clear();
    }

    public static void clearWorld(ServerWorld world) {
        if (world == null) return;
        List<UUID> actors = STATE_WORLDS.entrySet().stream()
                .filter(entry -> entry.getValue() == world)
                .map(Map.Entry::getKey).toList();
        actors.forEach(actorId -> {
            DEBT_PROC_COUNTERS.remove(actorId);
            MARKED_ASSETS.remove(actorId);
            VEILED_ACTORS.remove(actorId);
            STATE_WORLDS.remove(actorId);
        });
    }

    private static Phase8AbilityTuning.Setting s(String name) {
        return Phase8AbilityTuning.Setting.valueOf(name);
    }

    private static void spawnSoulDebtGainEffects(ServerWorld world, LivingEntity target, LivingEntity attacker, ItemStack stack) {
        Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        Vec3d attackerPos = attacker.getPos().add(0.0, Math.max(0.35, attacker.getHeight() * 0.55), 0.0);
        Vec3d delta = attackerPos.subtract(targetPos);
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            Vec3d pos = targetPos.add(delta.multiply((double) i / steps));
            world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.01);
        }
        world.spawnParticles(ParticleTypes.SCULK_SOUL, targetPos.x, targetPos.y, targetPos.z, Math.min(6, 1 + getSoulDebt(stack)), 0.18, 0.18, 0.18, 0.02);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                SoundCategory.PLAYERS, 0.32F, 1.55F + world.random.nextFloat() * 0.25F);
    }

    private static void spawnDepartureEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, actor.getHeight() * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 14, 0.28, 0.35, 0.28, 0.08);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 5, 0.16, 0.2, 0.16, 0.025);
        world.playSound(null, actor.getX(), actor.getY(), actor.getZ(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.6F, 0.85F);
    }

    private static void spawnBackstabEffects(ServerWorld world, LivingEntity target, LivingEntity actor, int stacks) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.55), 0.0);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.08, 0.05, 0.08, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 16, 0.28, 0.18, 0.28, 0.02);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 10 + Math.min(10, stacks * 2), 0.32, 0.28, 0.32, 0.04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 4 + Math.min(8, stacks), 0.22, 0.22, 0.22, 0.03);
        Vec3d actorPos = actor.getPos().add(0.0, Math.max(0.4, actor.getHeight() * 0.5), 0.0);
        Vec3d delta = actorPos.subtract(pos);
        for (int i = 1; i <= 6; i++) {
            Vec3d trail = pos.add(delta.multiply(i / 6.0));
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, trail.x, trail.y, trail.z, 1, 0.02, 0.02, 0.02, 0.025);
        }
    }

    private static void spawnFailEffects(ServerWorld world, LivingEntity actor) {
        Vec3d pos = actor.getPos().add(0.0, 0.8, 0.0);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 3, 0.12, 0.1, 0.12, 0.006);
        world.playSound(null, actor.getX(), actor.getY(), actor.getZ(), SoundRegistry.DARK_SWORD_BLOCK.get(),
                SoundCategory.PLAYERS, 0.35F, 1.55F);
    }

    private record MarkedAsset(int hits, long expiry) {
    }

    record ReapProfile(int consumed, float multiplier) {
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.soulstealer.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULSTEALER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 20;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 20.0;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 5;
        @ValidatedInt.Restrict(min = 0)
        public int hitStacks = 1;
        @ValidatedInt.Restrict(min = 0)
        public int killStacks = 2;
        @ValidatedFloat.Restrict(min = 0f)
        public float minBackstabMultiplier = 2.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float maxBackstabMultiplier = 5.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScalingPerMultiplier = 3.098f;
    }
}
