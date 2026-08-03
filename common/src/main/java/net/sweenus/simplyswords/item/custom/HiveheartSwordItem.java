package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.HivemindSwarmManager;

import java.util.List;

public class HiveheartSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public HiveheartSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            int skillCooldown = Config.uniqueEffects.hiveheart.cooldown;
            float skillDamage = Config.uniqueEffects.hiveheart.beeDamageScaling;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            SimplySwordsBeeEntity beeEntity = EntityRegistry.SIMPLYBEEENTITY.get().spawn(
                    serverWorld,
                    attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3),
                    SpawnReason.MOB_SUMMONED);
                if (beeEntity != null && target != null) {
                    beeEntity.setTarget(target);
                    beeEntity.setAngryAt(target.getUuid());
                    beeEntity.setAngerTime(200);
                    beeEntity.shouldAngerAt(target);
                    beeEntity.setInvulnerable(true);
                    beeEntity.setOwner(attacker);
                    double attackDamage = 1 + HelperMethods.abilityScaledDamage("nature", attacker, stack,
                            skillDamage, Config.uniqueEffects.hiveheart.beeSpellScaling);
                    EntityAttributeInstance attackAttribute = beeEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                }
            }
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
        if (!world.isClient() && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            if (hand != Hand.MAIN_HAND || serverPlayer.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                return TypedActionResult.fail(stack);
            }
            HivemindSwarmManager.activate(serverWorld, serverPlayer);
            serverPlayer.getItemCooldownManager().set(stack.getItem(), Config.uniqueEffects.hiveheart.activeCooldown);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        HivemindSwarmManager.activate(context.world(), context.actor());
        return true;
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.hiveheart.activeCooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_HONEY,
                ParticleTypes.LANDING_HONEY, ParticleTypes.LANDING_HONEY, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip5", Config.uniqueEffects.hiveheart.cooldown / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip7").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.hiveheart.activeCooldown);
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "nature");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HIVEHEART::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float beeDamageScaling = 0.88f;
        @ValidatedFloat.Restrict(min = 0f)
        public float beeSpellScaling = 1.76f;
        @ValidatedInt.Restrict(min = 0)
        public int activeCooldown = 300;
        @ValidatedInt.Restrict(min = 0)
        public int swarmBeeCount = 8;
        @ValidatedDouble.Restrict(min = 1.0)
        public double swarmRadius = 8.0;
        @ValidatedInt.Restrict(min = 20)
        public int swarmLifetime = 240;
        @ValidatedInt.Restrict(min = 1)
        public int stingsPerBee = 10;
        @ValidatedDouble.Restrict(min = 0.0)
        public double stingDamageScaling = 0.01;
        @ValidatedDouble.Restrict(min = 0.0)
        public double stingSpellScaling = 0.02;
        @ValidatedInt.Restrict(min = 1)
        public int stingIntervalTicks = 10;
        @ValidatedInt.Restrict(min = 0)
        public int maxSlowAmplifier = 3;

    }
}
