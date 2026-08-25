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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
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
    private static final Identifier WAX_RHYTHM = Identifier.of(SimplySwords.MOD_ID, "wax_rhythm");
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
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof SimplySwordsAxolotlEntity axolotl
                    && target.getUuid().equals(axolotl.getOwnerUuid())
                    && axolotl.getMasteryGuardRadius() > 0
                    && axolotl.squaredDistanceTo(target) <= axolotl.getMasteryGuardRadius()
                    * axolotl.getMasteryGuardRadius()) {
                return amount * .85F;
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

    public static void onDamageApplied(LivingEntity owner, DamageSource source) {
        if (!(owner.getWorld() instanceof ServerWorld world)
                || !(source.getAttacker() instanceof LivingEntity attacker)) return;
        ItemStack stack = owner.getMainHandStack();
        if (!stack.isOf(ItemsRegistry.WAXWEAVER.get())) return;
        UniqueAbilityExecution execution = beginPassive(Phase7UniqueAbilities.WAXWEAVER_REVIVAL,
                world, stack, owner, attacker);
        if (!WaxweaverEncasementManager.tryReactiveShell(world, owner, attacker, stack,
                Phase7UniqueAbilities.tuning(execution), execution)) {
            UniqueAbilityApi.finish(execution, Phase7UniqueAbilities.FINISH, 0);
        }
    }

    public static void applyWaxRhythm(ServerWorld world, LivingEntity actor, int duration, double bonus) {
        EntityAttributeInstance attackSpeed = actor.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (attackSpeed == null) return;
        attackSpeed.removeModifier(WAX_RHYTHM);
        attackSpeed.addTemporaryModifier(new EntityAttributeModifier(WAX_RHYTHM, Math.max(0, bonus),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        ATTRIBUTES.computeIfAbsent(world, ignored -> new ArrayList<>()).add(
                new ExpiringAttribute(actor.getUuid(), world.getTime() + Math.max(1, duration), WAX_RHYTHM));
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
        int total = net.sweenus.simplyswords.api.SimplySwordsAPI.getEffectiveWeaponCooldownTicks(
                new ItemStack(ItemsRegistry.CHOMPOLOTL.get()), owner,
                net.sweenus.simplyswords.config.Config.uniqueEffects.chompolotl.cooldown * 10);
        int remaining = Math.round(player.getItemCooldownManager().getCooldownProgress(
                ItemsRegistry.CHOMPOLOTL.get(), 0) * total);
        player.getItemCooldownManager().set(ItemsRegistry.CHOMPOLOTL.get(),
                Math.max(0, remaining - refundTicks));
    }

    public static boolean hasScheduled(ServerWorld world) {
        List<SecondSkin> skins = SECOND_SKINS.get(world);
        List<ExpiringAttribute> attributes = ATTRIBUTES.get(world);
        return skins != null && !skins.isEmpty() || attributes != null && !attributes.isEmpty();
    }

    public static void tick(ServerWorld world) {
        List<SecondSkin> skins = SECOND_SKINS.get(world);
        if (skins != null) {
            skins.removeIf(skin -> {
                if (world.getTime() < skin.at) return false;
                net.minecraft.entity.Entity entity = world.getEntity(skin.ownerId);
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    living.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,
                            skin.duration, Math.max(0, skin.absorption / 4 - 1), false, true, true));
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
                    EntityAttributeInstance attackSpeed = living.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                    if (attackSpeed != null) attackSpeed.removeModifier(attribute.id);
                }
                return true;
            });
            if (attributes.isEmpty()) ATTRIBUTES.remove(world);
        }
    }

    private record SecondSkin(UUID ownerId, long at, int duration, int absorption) {
    }

    private record ExpiringAttribute(UUID ownerId, long at, Identifier id) {
    }

    private record KillWindow(long startedAt, int count) {
    }
}
