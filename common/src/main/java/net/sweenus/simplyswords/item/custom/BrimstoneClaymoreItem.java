package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.ability.BuiltinUniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityApi;
import net.sweenus.simplyswords.api.ability.UniqueAbilityContext;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.BrimstoneClaymoreAbilityManager;
import net.sweenus.simplyswords.world.BrimstoneEruptionManager;

import java.util.List;

public class BrimstoneClaymoreItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public BrimstoneClaymoreItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (hand == Hand.OFF_HAND || itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            LivingEntity target = StealSwordItem.findLenientTarget(user, Config.uniqueEffects.brimstone_claymore.range);
            if (target == null) {
                return TypedActionResult.fail(itemStack);
            }
            WeaponAbilityContext context = WeaponAbilityContext.of(serverWorld, itemStack, serverPlayer, null,
                    target, hand, WeaponAbilityActivationSource.PLAYER);
            if (!SimplySwordsAPI.tryActivateWeaponAbility(context)) return TypedActionResult.fail(itemStack);
        }
        user.swingHand(hand);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        return BrimstoneClaymoreAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.brimstone_claymore.cooldown;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);
            UniqueAbilityExecution execution = UniqueAbilityApi.begin(
                    BuiltinUniqueAbilities.BRIMSTONE_ERUPTION,
                    UniqueAbilityContext.passive(world, stack, attacker, target, null),
                    tuning -> tuning
                            .set(BuiltinUniqueAbilities.BRIMSTONE_PROC_CHANCE,
                                    Math.clamp(Config.uniqueEffects.brimstone_claymore.chance, 0, 100))
                            .set(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_DAMAGE_SCALING,
                                    (double) Config.uniqueEffects.brimstone_claymore.hitDamageScaling)
                            .set(BuiltinUniqueAbilities.BRIMSTONE_ERUPTION_SPELL_SCALING,
                                    (double) Config.uniqueEffects.brimstone_claymore.hitSpellScaling));
            UniqueAbilityApi.takeStartedExecution();
            int chance = execution.tuning().get(BuiltinUniqueAbilities.BRIMSTONE_PROC_CHANCE);
            int roll = attacker.getRandom().nextInt(100);
            boolean passed = passesEruptionRoll(chance, roll);
            UniqueAbilityApi.reportRoll(execution, BuiltinUniqueAbilities.BRIMSTONE_PROC_CHANCE, chance, roll, passed);
            if (!passed) {
                UniqueAbilityApi.cancel(execution);
            } else {
                UniqueAbilityApi.start(execution);
                int affected = BrimstoneEruptionManager.erupt(execution);
                UniqueAbilityApi.finish(execution, execution.definition().id(), affected);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    static boolean passesEruptionRoll(int chance, int roll) {
        return chance > 0 && roll >= 0 && roll < Math.min(100, chance);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA,
                ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.brimstoneclaymoreitem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.brimstone_claymore.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.BRIMSTONE_CLAYMORE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 600;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 22.0;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 120;
        @ValidatedFloat.Restrict(min = 0.5f)
        public float baseRadius = 2.5f;
        @ValidatedFloat.Restrict(min = 0.5f)
        public float maxRadius = 6.0f;
        @ValidatedInt.Restrict(min = 1)
        public int pulseInterval = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float hitDamageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float hitSpellScaling = 4.19f;
        @ValidatedFloat.Restrict(min = 0f)
        public float pulseDamageScaling = 0.28f;
        @ValidatedFloat.Restrict(min = 0f)
        public float radiusGrowthPerHit = 0.35f;
        @ValidatedFloat.Restrict(min = 0f)
        public float finalDamageScaling = 1.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 5.3395f;
        @ValidatedDouble.Restrict(min = 0.0)
        public double targetJumpRange = 8.0;

    }
}
