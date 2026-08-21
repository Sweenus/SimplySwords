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

import java.util.List;
import java.util.Random;

public class MagispearSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
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
            float hitChance = Config.uniqueEffects.magispear.magicChance;
            int random = new Random().nextInt(100);
            if (random < hitChance) {
                float damage = HelperMethods.abilityScaledDamage("arcane", attacker, stack,
                        Config.uniqueEffects.magispear.magicDamageScaling,
                        Config.uniqueEffects.magispear.magicSpellScaling);
                DamageSource damageSource = attacker.getDamageSources().indirectMagic(attacker, attacker);
                target.timeUntilRegen = 0;
                target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, damage));
                target.timeUntilRegen = 0;
                world.playSound(null, attacker.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.2f, 1.1f);
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
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
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

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "arcane");
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
