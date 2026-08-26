package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
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
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.api.ability.Phase9AbilityTuning;
import net.sweenus.simplyswords.api.ability.Phase9UniqueAbilities;
import net.sweenus.simplyswords.api.ability.UniqueAbilityExecution;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.MagispearAbilityManager;
import net.sweenus.simplyswords.world.Phase9CombatManager;

import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MagispearSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    private static final Map<UUID, Integer> MELEE_HITS = new HashMap<>();
    private static final Map<UUID, Long> PIN_LOCKOUTS = new HashMap<>();
    public MagispearSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
            ServerWorld world = (ServerWorld) attacker.getWorld();
            UniqueAbilityExecution execution = Phase9CombatManager.beginPassive(
                    Phase9UniqueAbilities.MAGISPEAR_SPELLPOINT, world, stack, attacker, target);
            Phase9AbilityTuning tuning = Phase9UniqueAbilities.tuning(execution);
            int hitChance = tuning.integer(Phase9AbilityTuning.Setting.CHANCE,
                    Config.uniqueEffects.magispear.magicChance);
            int hits = MELEE_HITS.merge(attacker.getUuid(), 1, Integer::sum);
            boolean guaranteed = tuning.flag(1 << 7) && hits % 3 == 0;
            if (attacker.isSprinting() && tuning.flag(1 << 3)) hitChance = Math.min(100, hitChance + 12);
            if (guaranteed || attacker.getRandom().nextInt(100) < hitChance) {
                float damage = HelperMethods.abilityScaledDamage("arcane", attacker, stack,
                        Config.uniqueEffects.magispear.magicDamageScaling * (float) tuning.get(
                                Phase9AbilityTuning.Setting.DAMAGE_MULTIPLIER, 1),
                        Config.uniqueEffects.magispear.magicSpellScaling);
                if (tuning.has(Phase9AbilityTuning.Setting.ARMOR_IGNORE)) damage += Math.min(
                        damage * .5F, target.getArmor() * (float) tuning.get(
                                Phase9AbilityTuning.Setting.ARMOR_IGNORE, .1));
                DamageSource damageSource = attacker.getDamageSources().indirectMagic(attacker, attacker);
                target.timeUntilRegen = 0;
                if (tuning.flag(1 << 4)) target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.GLOWING, 50, 0), attacker);
                if (tuning.flag(1 << 6) && PIN_LOCKOUTS.getOrDefault(target.getUuid(), 0L) <= world.getTime()) {
                    target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                            net.minecraft.entity.effect.StatusEffects.SLOWNESS, 25, 1), attacker);
                    PIN_LOCKOUTS.put(target.getUuid(), world.getTime() + 40);
                }
                if (tuning.flag(1 << 8)) target.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.LEVITATION, 12, 0), attacker);
                target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage));
                target.timeUntilRegen = 0;
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.2f, 1.1f);
            }
            Phase9CombatManager.finish(execution, 1);
            if (MELEE_HITS.size() > 64) MELEE_HITS.clear();
            PIN_LOCKOUTS.entrySet().removeIf(entry -> entry.getValue() <= world.getTime());
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.isEmpty() || itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            LivingEntity target = null;
            Entity targeted = HelperMethods.getTargetedEntity(serverPlayer,
                    Math.max(1.0, Config.uniqueEffects.magispear.targetingRange));
            if (targeted instanceof LivingEntity livingTarget
                    && HelperMethods.checkAbilityTarget(livingTarget, serverPlayer)) {
                target = livingTarget;
            }
            WeaponAbilityContext context = WeaponAbilityContext.of(
                    serverWorld, itemStack, serverPlayer, null, target, hand,
                    WeaponAbilityActivationSource.PLAYER);
            if (!SimplySwordsAPI.tryActivateWeaponAbility(context)) {
                return TypedActionResult.fail(itemStack);
            }
            user.swingHand(hand, true);
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return MagispearAbilityManager.canStart(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return MagispearAbilityManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.magispear.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.ASH, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.magispearsworditem.tooltip9").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.magispear.cooldown);
        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.MAGISPEAR::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 120;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 1.1f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 5.88f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 4.0;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int magicChance = 35;
        @ValidatedFloat.Restrict(min = 0f)
        public float magicDamageScaling = 0.16f;
        @ValidatedFloat.Restrict(min = 0f)
        public float magicSpellScaling = 0.83f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwDamageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float throwSpellScaling = 4.27f;
        @ValidatedDouble.Restrict(min = 1.0)
        public double targetingRange = 24.0;
        @ValidatedInt.Restrict(min = 1, max = 8)
        public int rainWaveCount = 6;
        @ValidatedDouble.Restrict(min = 0.25)
        public double rainSplashRadius = 2.0;
        @ValidatedDouble.Restrict(min = 0.0)
        public double inwardPushStrength = 0.35;
        @ValidatedDouble.Restrict(min = 1.0)
        public double diveHeight = 6.0;

    }
}
