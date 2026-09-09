package net.sweenus.simplyswords.api;

import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.compat.SpellScalingComponents;
import net.sweenus.simplyswords.SimplySwordsExpectPlatform;
import net.sweenus.simplyswords.api.render.*;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.entity.SimplySwordsSkeletonMinionEntity;
import net.sweenus.simplyswords.entity.SimplySwordsWolfMinionEntity;
import net.sweenus.simplyswords.item.custom.LivyatanSwordItem;
import net.sweenus.simplyswords.item.custom.MoltenEdgeSwordItem;
import net.sweenus.simplyswords.item.custom.StealSwordItem;
import net.sweenus.simplyswords.item.custom.WraithmawSwordItem;
import net.sweenus.simplyswords.item.custom.GloampiercerSwordItem;
import net.sweenus.simplyswords.item.custom.SoulstalkerSwordItem;
import net.sweenus.simplyswords.item.custom.RiftmaneSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.mixin.ItemCooldownEntryAccessor;
import net.sweenus.simplyswords.mixin.ItemCooldownManagerAccessor;
import net.sweenus.simplyswords.power.powers.NecromanticArsenalPower;
import net.sweenus.simplyswords.item.ContainedRemnantItem;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.IgnoredEntities;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.world.ChainLightningVisualManager;
import net.sweenus.simplyswords.world.AbilityVisualManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;
import net.sweenus.simplyswords.world.WaxweaverEncasementManager;
import net.sweenus.simplyswords.world.WraithmawAbilityManager;
import net.sweenus.simplyswords.world.GloampiercerAbilityManager;
import net.sweenus.simplyswords.world.SoulstalkerAbilityManager;
import net.sweenus.simplyswords.world.RiftmaneAbilityManager;
import net.sweenus.simplyswords.world.PlayerMovementIntentManager;
import net.sweenus.simplyswords.api.render.ObserverStatusVisualStyle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class SimplySwordsAPI {

    private static final ThreadLocal<DelegatedWeaponHitContext> DELEGATED_WEAPON_HIT_CONTEXT = new ThreadLocal<>();


    // Battle Standard
    // Allows for the summoning of Battle Standard Entities. Can be configured with positive & negative effects (nullable)
    public static BattleStandardEntity spawnBattleStandard(PlayerEntity user, int decayRate, String standardType, int height, int distance,
                                                           String positiveEffect, String positiveEffectSecondary,
                                                           int positiveEffectAmplifier,
                                                           String negativeEffect, String negativeEffectSecondary,
                                                           int negativeEffectAmplifier,
                                                           boolean dealsDamage, boolean doesHealing) {

        if (!user.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) user.getWorld();
            BlockState currentState = world.getBlockState(user.getBlockPos().up(height).offset(user.getMovementDirection(), distance));
            BlockState state = Blocks.AIR.getDefaultState();
            if (currentState == state) {
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                        user.getSoundCategory(), 0.4f, 0.8f);
                BattleStandardEntity banner = EntityRegistry.BATTLESTANDARD.get().spawn(
                        world,
                        user.getBlockPos().up(height).offset(user.getMovementDirection(), distance),
                        SpawnReason.MOB_SUMMONED);
                if (banner != null) {
                    banner.setVelocity(0, -1, 0);
                    banner.ownerEntity = user;
                    banner.decayRate = decayRate;
                    banner.standardType = standardType;
                    banner.doesHealing = doesHealing;
                    banner.dealsDamage = dealsDamage;
                    banner.negativeEffect = negativeEffect;
                    banner.negativeEffectSecondary = negativeEffectSecondary;
                    banner.positiveEffect = positiveEffect;
                    banner.positiveEffectSecondary = positiveEffectSecondary;
                    banner.positiveEffectAmplifier = positiveEffectAmplifier;
                    banner.negativeEffectAmplifier = negativeEffectAmplifier;
                    banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
                    return banner;
                }
            }
        }
        return null;
    }

    public static GemPowerComponent getComponent(ItemStack stack) {
        return stack.getOrDefault(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.DEFAULT);
    }

    //
    // Whether an identify-on-click item still needs its power rolled: either it has no gem
    // power component at all, or it has one whose sockets are all empty because an earlier
    // roll failed (for instance if the power pool was not yet available).
    //
    // Callers use this rather than a bare stack.contains(GEM_POWER) check, so a stack that
    // picked up an empty component can never be permanently stuck as "unidentified".
    //
    // Only for items that identify on click - gems and runic weapons. Unique weapons use an
    // empty component legitimately to mean "has sockets, nothing socketed yet".
    //
    public static boolean needsGemPowerRoll(ItemStack stack) {
        GemPowerComponent component = stack.get(ComponentTypeRegistry.GEM_POWER.get());
        return component == null || component.isEmpty();
    }

    public static void onWeaponSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        if (stack == null || stack.isEmpty() || world == null || user == null || !user.isAlive()
                || WaxweaverEncasementManager.isEncased(user)
                || IncapacitatingStatusEffectRegistry.isIncapacitated(user)) {
            return;
        }
        AdditionalGemSocketApi.ensureInitialized(stack);
        getComponent(stack).onSwing(stack, world, user, hand);
        if (!AwakeningApi.isAbilityUnlocked(stack)) {
            return;
        }
        if (stack.getItem() instanceof LivyatanSwordItem livyatan) {
            livyatan.onSwing(stack, world, user, hand);
        }
        if (stack.getItem() instanceof MoltenEdgeSwordItem moltenEdge) {
            moltenEdge.onSwing(stack, world, user, hand);
        }
        if (stack.getItem() instanceof WraithmawSwordItem) {
            WraithmawAbilityManager.onSwing(stack, world, user);
        }
        if (stack.getItem() instanceof GloampiercerSwordItem) {
            GloampiercerAbilityManager.onSwing(stack, world, user);
        }
        if (stack.getItem() instanceof SoulstalkerSwordItem) {
            SoulstalkerAbilityManager.onSwing(stack, world, user, hand);
        }
        if (stack.getItem() instanceof RiftmaneSwordItem) {
            RiftmaneAbilityManager.onSwing(stack, world, user);
        }
    }


    //Opts an addon weapon into awakening and Runic Forge handling.
    public static void registerAwakeningProfile(Item item, AwakeningProfile profile) {
        AwakeningProfileRegistry.register(item, profile);
    }

    //
    // Registers an addon weapon family whose Runic Forge outcome may vary by
    // awakening level and authoritative server context.
    //
    public static void registerAwakeningFormFamily(AwakeningFormFamily family) {
        AwakeningFormRegistry.register(family);
    }

    //Adds an addon unique to the pity-controlled chest pool.
    public static void registerUniqueLoot(Item item, int weight) {
        UniqueLootRegistry.register(item, weight);
    }

    //
    // Opts a status effect into client presentation synchronization for
    // observers of affected living entities.
    //
    public static void registerObserverSyncedStatusEffect(Identifier effectId) {
        ObserverStatusEffectSyncRegistry.register(effectId);
    }

    // Registers an addon effect that prevents affected entities from moving or acting.
    public static void registerIncapacitatingStatusEffect(Identifier effectId) {
        IncapacitatingStatusEffectRegistry.register(effectId);
    }

    // Registers an observer-synced effect that produces a local, presentation-only vanilla storm.
    public static void registerLocalStormStatusEffect(Identifier effectId) {
        LocalStormStatusEffectRegistry.register(effectId);
    }

    // Registers an observer-synced visual rendered directly around affected living entities.
    public static void registerObserverStatusVisual(Identifier effectId, ObserverStatusVisualStyle style) {
        ObserverStatusVisualRegistry.register(effectId, style);
    }

    public static void registerSpellScalingDefinition(SpellScalingDefinition definition) {
        SpellScalingRegistry.register(definition);
    }

    public static Optional<SpellScalingDefinition> getSpellScalingDefinition(Identifier id) {
        return id == null ? Optional.empty() : SpellScalingRegistry.get(id);
    }

    // Returns neutral when input has not been received recently.
    public static PlayerMovementIntent getPlayerMovementIntent(ServerPlayerEntity player) {
        return PlayerMovementIntentManager.get(player);
    }

    // Gem Sockets
    // When each method is added to an item class, allows for gem sockets to appear on the item.
    // Each method needs to be called in its respective Override method. (Eg. inventoryTickGemSocketLogic goes in inventoryTick)

    // Performs postHit socket effects
    public static void postHitGemSocketLogic(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            GemPowerComponent component = getComponent(stack);
            component.postHit(stack, target, attacker);
        }
    }

    public static DelegatedWeaponHitContext getDelegatedWeaponHitContext() {
        return DELEGATED_WEAPON_HIT_CONTEXT.get();
    }

    public static boolean canActivateWeaponAbility(WeaponAbilityContext context) {
        return context != null
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.actor() != null
                && !WaxweaverEncasementManager.isEncased(context.actor())
                && !IncapacitatingStatusEffectRegistry.isIncapacitated(context.actor())
                && AwakeningApi.isAbilityUnlocked(context.stack())
                && context.stack().getItem() instanceof UniqueWeaponActiveAbility ability
                && ability.canActivate(context)
                && !isWeaponAbilityCoolingDown(context);
    }

    public static int getWeaponAbilityCooldownTicks(WeaponAbilityContext context) {
        if (context == null
                || context.stack() == null
                || context.stack().isEmpty()
                || !(context.stack().getItem() instanceof UniqueWeaponActiveAbility ability)) {
            return 20;
        }
        return resolveWeaponAbilityCooldownTicks(ability, context);
    }

    public static boolean tryActivateWeaponAbility(WeaponAbilityContext context) {
        if (context == null
                || context.stack() == null
                || context.stack().isEmpty()
                || context.actor() == null
                || WaxweaverEncasementManager.isEncased(context.actor())
                || IncapacitatingStatusEffectRegistry.isIncapacitated(context.actor())
                || !AwakeningApi.isAbilityUnlocked(context.stack())
                || !(context.stack().getItem() instanceof UniqueWeaponActiveAbility ability)
                || !ability.canActivate(context)) {
            return false;
        }

        if (isWeaponAbilityCoolingDown(context)) {
            return false;
        }

        UniqueAbilityApi.clearStartedExecution();
        boolean activated;
        try {
            activated = ability.activate(context);
        } catch (RuntimeException exception) {
            UniqueAbilityExecution failedExecution = UniqueAbilityApi.takeStartedExecution();
            if (failedExecution != null) {
                UniqueAbilityApi.cancel(failedExecution);
            }
            throw exception;
        }
        UniqueAbilityExecution execution = UniqueAbilityApi.takeStartedExecution();
        if (!activated) {
            if (execution != null) {
                UniqueAbilityApi.cancel(execution);
            }
            return false;
        }

        int cooldown = ability.getActivationCooldownTicks(context.stack(), context);
        if (execution != null) {
            UniqueAbilityApi.start(execution);
            cooldown = execution.cooldownTicks(cooldown);
        }
        setWeaponCooldown(context.actor(), context.stack(), cooldown);
        if (execution != null) {
            double fraction = execution.takeCooldownRefundFraction();
            if (fraction > 0) {
                int remaining = getRemainingWeaponCooldownTicks(context.actor(), context.stack());
                reduceWeaponCooldown(context.actor(), context.stack(), remaining,
                        (int) Math.round(remaining * fraction));
            }
        }
        return true;
    }

    private static int resolveWeaponAbilityCooldownTicks(UniqueWeaponActiveAbility ability,
                                                         WeaponAbilityContext context) {
        return getEffectiveWeaponCooldownTicks(
                context.stack(), context.actor(), ability.getActivationCooldownTicks(context.stack(), context));
    }

    //Shortens an existing weapon cooldown instead of replacing it.
    public static void reduceWeaponCooldown(LivingEntity actor, ItemStack stack,
                                            int totalCooldownTicks, int reductionTicks) {
        if (actor == null || stack == null || stack.isEmpty()
                || totalCooldownTicks <= 0 || reductionTicks <= 0) {
            return;
        }
        if (actor instanceof PlayerEntity player) {
            ItemCooldownManagerAccessor cooldownManager =
                    (ItemCooldownManagerAccessor) player.getItemCooldownManager();
            Object entry = cooldownManager.simplyswords$getEntries().get(stack.getItem());
            int remaining = entry instanceof ItemCooldownEntryAccessor cooldownEntry
                    ? Math.max(0, cooldownEntry.simplyswords$getEndTick() - cooldownManager.simplyswords$getTick())
                    : 0;
            if (remaining <= 0) {
                return;
            }
            player.getItemCooldownManager().set(stack.getItem(), Math.max(0, remaining - reductionTicks));
        } else if (actor.getWorld() instanceof ServerWorld world) {
            WeaponAbilityCooldownManager.reduceCooldown(world, actor, stack, reductionTicks);
        }
    }

    private static int getRemainingWeaponCooldownTicks(LivingEntity actor, ItemStack stack) {
        if (actor instanceof PlayerEntity player) {
            ItemCooldownManagerAccessor manager = (ItemCooldownManagerAccessor) player.getItemCooldownManager();
            Object entry = manager.simplyswords$getEntries().get(stack.getItem());
            return entry instanceof ItemCooldownEntryAccessor cooldown
                    ? Math.max(0, cooldown.simplyswords$getEndTick() - manager.simplyswords$getTick()) : 0;
        }
        return actor.getWorld() instanceof ServerWorld world
                ? WeaponAbilityCooldownManager.remainingTicks(world, actor, stack) : 0;
    }

    public static int getEffectiveWeaponCooldownTicks(ItemStack stack, LivingEntity actor, int baseCooldownTicks) {
        if (baseCooldownTicks <= 0) {
            return 0;
        }

        int cooldown = Math.max(1, baseCooldownTicks);
        if (stack == null || stack.isEmpty() || actor == null
                || !Config.compatibility.ironsSpells.get().enableCooldownReduction.get()) {
            return cooldown;
        }
        if (stack.getItem() instanceof UniqueWeaponActiveAbility ability
                && !ability.usesSpellCooldownReduction(stack)) {
            return cooldown;
        }
        return Math.max(1, SimplySwordsExpectPlatform.applySpellCooldownReduction(cooldown, actor));
    }

    public static void setWeaponCooldown(LivingEntity actor, ItemStack stack, int baseCooldownTicks) {
        if (actor == null || stack == null || stack.isEmpty() || baseCooldownTicks < 0) {
            return;
        }
        if (baseCooldownTicks == 0) {
            if (actor instanceof PlayerEntity player) {
                player.getItemCooldownManager().set(stack.getItem(), 0);
            } else {
                WeaponAbilityCooldownManager.clearCooldown(actor, stack);
            }
            return;
        }

        int cooldown = getEffectiveWeaponCooldownTicks(stack, actor, baseCooldownTicks);
        if (actor instanceof PlayerEntity player) {
            player.getItemCooldownManager().set(stack.getItem(), cooldown);
        } else if (actor.getWorld() instanceof ServerWorld world) {
            WeaponAbilityCooldownManager.setCooldown(world, actor, stack, cooldown);
        }
    }

    private static boolean isWeaponAbilityCoolingDown(WeaponAbilityContext context) {
        if (context == null || context.stack() == null || context.stack().isEmpty() || context.actor() == null) {
            return true;
        }
        if (context.actor() instanceof ServerPlayerEntity player) {
            return player.getItemCooldownManager().isCoolingDown(context.stack().getItem());
        }
        return context.world() == null || WeaponAbilityCooldownManager.isCoolingDown(context.world(), context.actor(), context.stack());
    }

    public static boolean applyDelegatedWeaponHit(ItemStack stack, LivingEntity target, ServerPlayerEntity owner,
                                                  LivingEntity actor, float damage) {
        return applyDelegatedWeaponHit(stack, target, (LivingEntity) owner, actor, damage);
    }

    public static boolean applyDelegatedWeaponHit(ItemStack stack, LivingEntity target, LivingEntity owner,
                                                  LivingEntity actor, float damage) {
        if (stack == null || stack.isEmpty() || target == null || owner == null || actor == null
                || !(owner.getWorld() instanceof ServerWorld world) || target.getWorld() != world) {
            return false;
        }
        if (target instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (IgnoredEntities.isIgnored(target)) {
            return false;
        }

        Vec3d facing = actor.getRotationVec(1.0F);
        if (facing.lengthSquared() < 0.0001) {
            facing = target.getPos().subtract(actor.getPos());
        }
        if (facing.lengthSquared() < 0.0001) {
            facing = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        facing = facing.normalize();

        DelegatedWeaponHitContext context = new DelegatedWeaponHitContext(owner instanceof ServerPlayerEntity sp ? sp : null, actor, actor.getPos(), facing);
        DELEGATED_WEAPON_HIT_CONTEXT.set(context);
        try {
            DamageSource source = owner instanceof ServerPlayerEntity sp
                    ? owner.getDamageSources().playerAttack(sp)
                    : owner.getDamageSources().mobAttack(owner);
            float modifiedDamage = WeaponImplicitRegistry.modifyDamage(stack, target, source, damage);
            target.timeUntilRegen = 0;
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(source, modifiedDamage));
            target.timeUntilRegen = 0;
            if (!damaged[0]) {
                return false;
            }

            EnchantmentHelper.onTargetDamaged(world, target, source, stack);
            WeaponImplicitRegistry.onHit(stack, target, owner, modifiedDamage);
            Item item = stack.getItem();
            if (item instanceof SwordItem) {
                NecromanticArsenalPower.runSuppressed(() -> item.postHit(stack, target, owner));
            }
            return true;
        } finally {
            DELEGATED_WEAPON_HIT_CONTEXT.remove();
        }
    }

    public static boolean applyEntityWeaponHit(ItemStack stack, LivingEntity target, LivingEntity actor, float damage) {
        if (stack == null || stack.isEmpty() || target == null || actor == null
                || !(actor.getWorld() instanceof ServerWorld world) || target.getWorld() != world
                || !target.isAlive()) {
            return false;
        }
        if (IgnoredEntities.isIgnored(target)) {
            return false;
        }

        Vec3d facing = actor.getRotationVec(1.0F);
        if (facing.lengthSquared() < 0.0001) {
            facing = target.getPos().subtract(actor.getPos());
        }
        if (facing.lengthSquared() < 0.0001) {
            facing = Vec3d.fromPolar(0.0F, actor.getYaw());
        }
        facing = facing.normalize();

        DelegatedWeaponHitContext context = new DelegatedWeaponHitContext(actor instanceof ServerPlayerEntity player ? player : null, actor, actor.getPos(), facing);
        DELEGATED_WEAPON_HIT_CONTEXT.set(context);
        try {
            DamageSource source = getWeaponDamageSource(actor);
            float modifiedDamage = WeaponImplicitRegistry.modifyDamage(stack, target, source, damage);
            target.timeUntilRegen = 0;
            boolean[] damaged = {false};
            WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = target.damage(source, modifiedDamage));
            target.timeUntilRegen = 0;
            if (!damaged[0]) {
                return false;
            }

            EnchantmentHelper.onTargetDamaged(world, target, source, stack);
            WeaponImplicitRegistry.onHit(stack, target, actor, modifiedDamage);
            Item item = stack.getItem();
            if (item instanceof SwordItem) {
                NecromanticArsenalPower.runSuppressed(() -> item.postHit(stack, target, actor));
            }
            return true;
        } finally {
            DELEGATED_WEAPON_HIT_CONTEXT.remove();
        }
    }

    public static void applyEntityWeaponPostHit(ItemStack stack, LivingEntity target, LivingEntity actor, float damage) {
        if (stack == null || stack.isEmpty() || target == null || actor == null || actor.getWorld().isClient()
                || !(stack.getItem() instanceof SwordItem item)) {
            return;
        }
        DelegatedWeaponHitContext context = new DelegatedWeaponHitContext(actor instanceof ServerPlayerEntity player ? player : null,
                actor, actor.getPos(), actor.getRotationVec(1.0F));
        DELEGATED_WEAPON_HIT_CONTEXT.set(context);
        try {
            float hitDamage = net.sweenus.simplyswords.util.HelperMethods.applyNonPlayerWeaponHitDamageModifier(actor, damage);
            WeaponImplicitRegistry.onHit(stack, target, actor, hitDamage);
            NecromanticArsenalPower.runSuppressed(() -> item.postHit(stack, target, actor));
        } finally {
            DELEGATED_WEAPON_HIT_CONTEXT.remove();
        }
    }

    public static DamageSource getWeaponDamageSource(LivingEntity actor) {
        if (actor instanceof PlayerEntity player) {
            return player.getDamageSources().playerAttack(player);
        }
        return actor.getDamageSources().mobAttack(actor);
    }

    // Adds the relevant socket information to the item tooltip
    public static void appendTooltipGemSocketLogic(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        GemPowerComponent component = getComponent(itemStack);

        if (!component.isEmpty()) {
            tooltip.add(Text.literal(""));
        }

        component.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    // Allows for the socketing of gems
    public static void onClickedGemSocketLogic (ItemStack stack, ItemStack otherStack, PlayerEntity player) {
        onClickedGemSocketLogic(stack, otherStack, player, StackReference.EMPTY);
    }

    // Allows for the socketing of gems while retaining access to the cursor in Creative mode
    public static void onClickedGemSocketLogic(ItemStack stack, ItemStack otherStack,
                                               PlayerEntity player,
                                               StackReference cursorStackReference) {
        if (Config.general.enableUniqueGemSockets
                || AdditionalGemSocketApi.hasAdditionalSockets(stack)) {
            GemPowerComponent component = getComponent(stack);
            if (component.canBeFilled()) {
                if (otherStack.getItem() instanceof GemPowerFiller gemPowerFiller) {
                    Item incomingGemItem = otherStack.getItem();
                    ValidationResult<GemPowerComponent> result = gemPowerFiller.fill(otherStack, component);
                    if (result.isValid()) {
                        stack.set(ComponentTypeRegistry.GEM_POWER.get(), result.get());
                        player.getWorld().playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, player.getSoundCategory(), 1, 1);
                        otherStack.decrement(1);
                        returnDisplacedGem(
                                player,
                                component,
                                incomingGemItem,
                                cursorStackReference
                        );
                    }
                }
            }
        }
    }

    private static void returnDisplacedGem(PlayerEntity player, GemPowerComponent weaponComponent,
                                           Item incomingGemItem,
                                           StackReference cursorStackReference) {
        ItemStack displacedGem = createDisplacedGem(weaponComponent, incomingGemItem);
        if (displacedGem.isEmpty()) {
            return;
        }

        if (player.getWorld().isClient()) {
            if (!player.isCreative()) {
                return;
            }
            if (!player.getInventory().insertStack(displacedGem)
                    && cursorStackReference != StackReference.EMPTY) {
                cursorStackReference.set(displacedGem);
            }
            return;
        }

        giveOrDropStack(player, displacedGem);
    }

    private static ItemStack createDisplacedGem(GemPowerComponent weaponComponent,
                                                Item incomingGemItem) {
        if (incomingGemItem == ItemsRegistry.RUNEFUSED_GEM.get()
                && weaponComponent.hasRunicSlotFilled()) {
            ItemStack displacedRunic = new ItemStack(ItemsRegistry.RUNEFUSED_GEM.get());
            displacedRunic.set(ComponentTypeRegistry.GEM_POWER.get(),
                    GemPowerComponent.runic(weaponComponent.runicPower()));
            return displacedRunic;
        }
        if (incomingGemItem == ItemsRegistry.NETHERFUSED_GEM.get()
                && weaponComponent.hasNetherSlotFilled()) {
            ItemStack displacedNether = new ItemStack(ItemsRegistry.NETHERFUSED_GEM.get());
            displacedNether.set(ComponentTypeRegistry.GEM_POWER.get(),
                    GemPowerComponent.nether(weaponComponent.netherPower()));
            return displacedNether;
        }
        return ItemStack.EMPTY;
    }

    private static void giveOrDropStack(PlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    public static void inventoryTickGemSocketLogic (ItemStack stack, World world, Entity entity,
                                                    int runeSocketChance, int netherSocketChance) {
        // Server-side only: rolling on both sides gave the client a different socket layout
        // than the server, and rewrote the component on stacks a storage mod was tracking.
        if (!world.isClient && !stack.contains(ComponentTypeRegistry.GEM_POWER.get()) && Config.general.enableUniqueGemSockets) {
            float runeSocketRoll = world.getRandom().nextFloat() * 100;
            float netherSocketRoll = world.getRandom().nextFloat() * 100;
            stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.createEmpty(
                    runeSocketRoll < runeSocketChance,
                    netherSocketRoll < netherSocketChance
            ));
        }
        if (!world.isClient && (entity instanceof LivingEntity user) &&
                (user.getEquippedStack(EquipmentSlot.MAINHAND) == stack || user.getEquippedStack(EquipmentSlot.OFFHAND) == stack)) {
            GemPowerComponent component = getComponent(stack);
            component.inventoryTick(stack, world, user, 0, true);
        }
    }

    // Add transformations to the ContainedRemnantItem
    public static void registerTransformation(Block block, Identifier identifier) {
        ContainedRemnantItem.addTransformation(block, identifier);
    }

    // Register/Assign weapon implicits. If you don't require your own custom implicits, you can instead assign them via tag data under 'resources/data/simplyswords/tags/item/implicit/...'

    public static void registerWeaponType(Item item, Identifier weaponType) {
        WeaponImplicitRegistry.registerWeaponType(item, weaponType);
    }

    public static void registerWeaponType(TagKey<Item> itemTag, Identifier weaponType) {
        WeaponImplicitRegistry.registerWeaponType(itemTag, weaponType);
    }

    public static void registerWeaponImplicit(WeaponImplicitDefinition definition) {
        WeaponImplicitRegistry.registerWeaponImplicit(definition);
    }

    public static Optional<net.sweenus.simplyswords.item.component.WeaponImplicitComponent> getOrCreateWeaponImplicit(ItemStack stack) {
        return WeaponImplicitRegistry.getOrCreateWeaponImplicit(stack);
    }

    public static void appendWeaponImplicitTooltip(ItemStack stack, List<Text> tooltip) {
        tooltip.addAll(WeaponImplicitRegistry.buildTooltipLines(stack));
    }

    public static void appendWeaponImplicitTooltip(ItemStack stack, List<Text> tooltip, boolean includeRange) {
        tooltip.addAll(WeaponImplicitRegistry.buildTooltipLines(stack, includeRange));
    }

    public static float applyWeaponImplicitDamage(ItemStack stack, LivingEntity target, DamageSource source, float amount) {
        return WeaponImplicitRegistry.modifyDamage(stack, target, source, amount);
    }

    public static void applyWeaponImplicitOnHit(ItemStack stack, LivingEntity target, LivingEntity attacker, float damage) {
        WeaponImplicitRegistry.onHit(stack, target, attacker, damage);
    }

    public static float scaleAbilityDamage(SpellScalingProfile school, LivingEntity actor, ItemStack stack,
                                           float attackScaling, float spellScaling) {
        return HelperMethods.abilityScaledDamage(school, actor, stack, attackScaling, spellScaling);
    }

    public static float scaleAbilityDamage(Identifier scalingProfileId, LivingEntity actor, ItemStack stack,
                                           float attackScaling, float spellScaling) {
        return HelperMethods.abilityScaledDamage(scalingProfileId, actor, stack, attackScaling, spellScaling);
    }

    public static float scaleAbilityValue(SpellScalingProfile school, LivingEntity actor, ItemStack stack,
                                          float baseValue, float spellScaling) {
        return HelperMethods.abilityScaledValue(school, actor, stack, baseValue, spellScaling);
    }

    public static float scaleAbilityValue(Identifier scalingProfileId, LivingEntity actor, ItemStack stack,
                                          float baseValue, float spellScaling) {
        return HelperMethods.abilityScaledValue(scalingProfileId, actor, stack, baseValue, spellScaling);
    }

    public static float scaleAbilityDamageFromValue(SpellScalingProfile school, LivingEntity actor, ItemStack stack,
                                                     float attackDamage, float spellScaling) {
        return HelperMethods.abilityScaledDamageFromValue(school, actor, stack, attackDamage, spellScaling);
    }

    public static float scaleAbilityDamageFromValue(Identifier scalingProfileId, LivingEntity actor, ItemStack stack,
                                                     float attackDamage, float spellScaling) {
        return HelperMethods.abilityScaledDamageFromValue(scalingProfileId, actor, stack, attackDamage, spellScaling);
    }

    public static boolean applyAbilityMagicDamage(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                   LivingEntity target, float damage, SpellScalingProfile profile) {
        SpellScalingProfile resolved = profile == null ? SpellScalingProfile.ARCANE : profile;
        return applyAbilityMagicDamage(world, actor, stack, target, damage, resolved.registryId());
    }

    public static boolean applyAbilityMagicDamage(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                   LivingEntity target, float damage, Identifier scalingProfileId) {
        return applyAbilityMagicDamageInternal(world, actor, stack, target, damage, scalingProfileId, false);
    }

    public static boolean applyAbilityMagicDamageThroughIframes(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                                 LivingEntity target, float damage,
                                                                 SpellScalingProfile profile) {
        SpellScalingProfile resolved = profile == null ? SpellScalingProfile.ARCANE : profile;
        return applyAbilityMagicDamageThroughIframes(world, actor, stack, target, damage, resolved.registryId());
    }

    public static boolean applyAbilityMagicDamageThroughIframes(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                                 LivingEntity target, float damage,
                                                                 Identifier scalingProfileId) {
        return applyAbilityMagicDamageInternal(world, actor, stack, target, damage, scalingProfileId, true);
    }

    private static boolean applyAbilityMagicDamageInternal(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                            LivingEntity target, float damage,
                                                            Identifier scalingProfileId, boolean bypassIframes) {
        if (world == null || actor == null || target == null || scalingProfileId == null
                || actor.getWorld() != world || target.getWorld() != world
                || !actor.isAlive() || !target.isAlive()
                || !HelperMethods.checkAbilityTarget(target, actor)) {
            return false;
        }
        ItemStack scalingStack = stack == null ? ItemStack.EMPTY : stack;
        Identifier effectiveScalingId = SpellScalingComponents.weaponComponent(scalingStack, scalingProfileId);
        DamageSource source = SimplySwordsExpectPlatform.getAbilityMagicDamageSource(world, actor, effectiveScalingId);
        if (source == null) {
            source = world.getDamageSources().indirectMagic(actor, actor);
        }
        float finalDamage = HelperMethods.applyAbilityDamageEnchantments(
                world, scalingStack, target, source, Math.max(0.0F, damage));
        float resistance = Math.max(0.0F,
                SimplySwordsExpectPlatform.getAbilityMagicResistanceMultiplier(target, effectiveScalingId));
        float adjustedDamage = finalDamage * resistance;
        DamageSource resolvedSource = source;
        boolean[] damaged = {false};
        WeaponImplicitRegistry.runSuppressed(() -> damaged[0] = bypassIframes
                ? HelperMethods.damageThroughIframes(target, resolvedSource, adjustedDamage)
                : target.damage(resolvedSource, adjustedDamage));
        return damaged[0];
    }

    public static LivingEntity findLenientAbilityTarget(PlayerEntity player, double range,
                                                        Predicate<LivingEntity> predicate) {
        return StealSwordItem.findLenientTarget(player, range, predicate);
    }

    public static List<LivingEntity> findAbilityChainTargets(ServerWorld world, LivingEntity actor,
                                                             LivingEntity firstTarget, int count, double range) {
        return ChainLightningVisualManager.chainTargets(world, actor, firstTarget, count, range);
    }

    public static List<LivingEntity> findAbilityChainTargetsFromPosition(ServerWorld world, LivingEntity actor,
                                                                         Vec3d origin, int count, double range) {
        return ChainLightningVisualManager.chainTargetsFromPosition(world, actor, origin, count, range);
    }

    public static boolean applyAbilityBoltDamage(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                 LivingEntity target, float damage) {
        return ChainLightningVisualManager.damageBoltTarget(world, actor, stack, target, damage);
    }

    public static boolean applyAbilityBoltDamageWithoutKnockback(ServerWorld world, LivingEntity actor, ItemStack stack,
                                                                 LivingEntity target, float damage) {
        return ChainLightningVisualManager.damageBoltTargetWithoutKnockback(world, actor, stack, target, damage);
    }

    public static void spawnAbilityLightningBolt(ServerWorld world, Vec3d start, Vec3d end,
                                                  LightningBoltStyle style) {
        if (world == null || start == null || end == null || style == null) {
            return;
        }
        ChainLightningVisualManager.spawnBolt(world, start, end,
                new ChainLightningVisualManager.LightningVisualSettings(
                        style.color(), style.lifetimeTicks(), style.thickness(), style.branches()),
                style.illuminate());
    }

    public static UUID spawnAbilityLightningPhenomenon(ServerWorld world, Vec3d start, Vec3d end,
                                                        LightningPhenomenonStyle style) {
        return AbilityVisualManager.spawnLightningPhenomenon(world, start, end, style);
    }

    public static UUID spawnAbilityLightningPhenomenon(ServerWorld world, Entity start, float startYOffset,
                                                        Entity end, float endYOffset,
                                                        LightningPhenomenonStyle style) {
        return AbilityVisualManager.spawnLightningPhenomenon(world, start, startYOffset, end, endYOffset, style);
    }

    public static UUID spawnAbilityLightningPhenomenon(ServerWorld world, Vec3d start,
                                                        Entity end, float endYOffset,
                                                        LightningPhenomenonStyle style) {
        return AbilityVisualManager.spawnLightningPhenomenon(world, start, end, endYOffset, style);
    }

    public static UUID spawnAbilityStormVolume(ServerWorld world, Vec3d position,
                                                StormVolumeStyle style) {
        return AbilityVisualManager.spawnStormVolume(world, position, style);
    }

    public static UUID spawnAbilityStormVolume(ServerWorld world, Entity anchor,
                                                StormVolumeStyle style) {
        return AbilityVisualManager.spawnStormVolume(world, anchor, style);
    }

    public static UUID spawnAbilitySurfaceDischarge(ServerWorld world, Vec3d origin, Vec3d direction,
                                                     double length, SurfaceDischargeStyle style) {
        return AbilityVisualManager.spawnSurfaceDischarge(world, origin, direction, length, style);
    }

    public static UUID spawnAbilityShockFront(ServerWorld world, Vec3d origin, Vec3d direction,
                                               ShockFrontStyle style) {
        return AbilityVisualManager.spawnShockFront(world, origin, direction, style);
    }

    public static void discardAbilityVisual(ServerWorld world, UUID visualId) {
        AbilityVisualManager.discard(world, visualId);
    }

    public static boolean isValidAbilityTarget(LivingEntity target, LivingEntity actor) {
        return target != null
                && actor != null
                && !(target instanceof SimplySwordsSkeletonMinionEntity)
                && !(target instanceof SimplySwordsWolfMinionEntity)
                && HelperMethods.checkAbilityTarget(target, actor);
    }

    public static Optional<LivingEntity> findClosestAbilityTarget(LivingEntity actor, double range, double width) {
        return actor == null ? Optional.empty() : HelperMethods.findClosestTarget(actor, range, width);
    }

    public static void spawnAbilityOrbitParticles(ServerWorld world, Vec3d centre, ParticleEffect particle,
                                                  double radius, int count) {
        HelperMethods.spawnOrbitParticles(world, centre, particle, radius, count);
    }

}
