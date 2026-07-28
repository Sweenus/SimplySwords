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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
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
import net.sweenus.simplyswords.entity.BattleStandardEntity;
import net.sweenus.simplyswords.item.custom.LivyatanSwordItem;
import net.sweenus.simplyswords.item.custom.MoltenEdgeSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.power.powers.NecromanticArsenalPower;
import net.sweenus.simplyswords.item.ContainedRemnantItem;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.GemPowerFiller;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.util.List;
import java.util.Optional;

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

    public static void onWeaponSwing(ItemStack stack, ServerWorld world, LivingEntity user, Hand hand) {
        if (stack == null || stack.isEmpty() || world == null || user == null || !user.isAlive()) {
            return;
        }
        getComponent(stack).onSwing(stack, world, user, hand);
        if (stack.getItem() instanceof LivyatanSwordItem livyatan) {
            livyatan.onSwing(stack, world, user, hand);
        }
        if (stack.getItem() instanceof MoltenEdgeSwordItem moltenEdge) {
            moltenEdge.onSwing(stack, world, user, hand);
        }
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
        return ability.getActivationCooldownTicks(context.stack(), context);
    }

    public static boolean tryActivateWeaponAbility(WeaponAbilityContext context) {
        if (context == null
                || context.stack() == null
                || context.stack().isEmpty()
                || !(context.stack().getItem() instanceof UniqueWeaponActiveAbility ability)
                || !ability.canActivate(context)) {
            return false;
        }

        if (isWeaponAbilityCoolingDown(context)) {
            return false;
        }

        if (!ability.activate(context)) {
            return false;
        }

        int cooldown = Math.max(1, ability.getActivationCooldownTicks(context.stack(), context));
        if (context.actor() instanceof ServerPlayerEntity player) {
            player.getItemCooldownManager().set(context.stack().getItem(), cooldown);
        } else {
            WeaponAbilityCooldownManager.setCooldown(context.world(), context.actor(), context.stack(), cooldown);
        }
        return true;
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
        if (Config.general.enableUniqueGemSockets) {
            GemPowerComponent component = getComponent(stack);
            if (component.canBeFilled()) {
                if (otherStack.getItem() instanceof GemPowerFiller gemPowerFiller) {
                    ValidationResult<GemPowerComponent> result = gemPowerFiller.fill(otherStack, component);
                    if (result.isValid()) {
                        stack.set(ComponentTypeRegistry.GEM_POWER.get(), result.get());
                        player.getWorld().playSoundFromEntity(null, player, SoundEvents.BLOCK_ANVIL_USE, player.getSoundCategory(), 1, 1);
                        otherStack.decrement(1);
                    }
                }
            }
        }
    }

    // netherSocketChance & runeSocketChance determine how likely these sockets are to appear on the item. An int of 50 = 50% chance for the socket to appear.
    public static void inventoryTickGemSocketLogic (ItemStack stack, World world, Entity entity,
                                                    int runeSocketChance, int netherSocketChance) {
        if (!stack.contains(ComponentTypeRegistry.GEM_POWER.get()) && Config.general.enableUniqueGemSockets) {
            float runeSocketRoll = (float) (Math.random() * 100);
            float netherSocketRoll = (float) (Math.random() * 100);
            stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.createEmpty(runeSocketRoll > runeSocketChance, netherSocketRoll > netherSocketChance));
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

}
