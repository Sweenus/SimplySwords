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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.StarsEdgeAbilityManager;

import java.util.List;

public class StarsEdgeSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public StarsEdgeSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            float skillDamageModifier = Config.uniqueEffects.stars_edge.damageScaling;
            float skillLifestealModifier = Config.uniqueEffects.stars_edge.lifestealModifier;
            ServerWorld world = (ServerWorld) attacker.getWorld();
            DamageSource damageSource = world.getDamageSources().generic();
            float abilityDamage = HelperMethods.abilityScaledDamage("arcane", attacker, stack,
                    skillDamageModifier, Config.uniqueEffects.stars_edge.spellScaling);
            abilityDamage = HelperMethods.applyNonPlayerWeaponHitDamageModifier(attacker, abilityDamage);
            if (attacker instanceof PlayerEntity player)
                damageSource = attacker.getDamageSources().playerAttack(player);

            HelperMethods.playHitSounds(attacker, target);

            if (world.isDay()) {
                target.timeUntilRegen = 0;
                target.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, target, damageSource, abilityDamage));
            }
            else if (world.isNight()) {
                attacker.heal(abilityDamage * skillLifestealModifier);
            }

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return StarsEdgeAbilityManager.canActivate(context);
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return StarsEdgeAbilityManager.activate(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return context != null && StarsEdgeAbilityManager.isActive(context.actor())
                ? Math.max(1, Config.uniqueEffects.stars_edge.activeReactivationCooldown)
                : Config.uniqueEffects.stars_edge.cooldown;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_OBSIDIAN_TEAR,
                ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {

        float skillDamageModifier = Config.uniqueEffects.stars_edge.damageScaling;

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip2", skillDamageModifier).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip6").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.stars_edge.cooldown);

        appendAbilityManaCostTooltip(tooltip, itemStack);

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendWeaponSpellScaleTooltip(tooltip, itemStack, "arcane");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.STARS_EDGE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 240;
        @ValidatedInt.Restrict(min = 1)
        public int activeReactivationCooldown = 240;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageScaling = 0.155f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.725f;
        @ValidatedFloat.Restrict(min = 0f)
        public float lifestealModifier = 0.10f;
        @ValidatedInt.Restrict(min = 1)
        public int recordingDuration = 120;
        @ValidatedDouble.Restrict(min = 0.1)
        public double initialDashDistance = 6.0;
        @ValidatedDouble.Restrict(min = 0.1)
        public double initialDashSpeed = 1.5;
        @ValidatedDouble.Restrict(min = 0.25)
        public double nodeSpacing = 2.0;
        @ValidatedInt.Restrict(min = 2, max = 16)
        public int maxNodes = 12;
        @ValidatedFloat.Restrict(min = 0f)
        public float constellationDamageScaling = 0.27f;
        @ValidatedFloat.Restrict(min = 0f)
        public float constellationSpellScaling = 1.30f;
        @ValidatedInt.Restrict(min = 1)
        public int constellationDuration = 100;
        @ValidatedInt.Restrict(min = 1)
        public int constellationDamageInterval = 10;
        @ValidatedDouble.Restrict(min = 0.1)
        public double constellationDamageWidth = 1.5;
        @ValidatedInt.Restrict(min = 1)
        public int segmentExplosionInterval = 5;
        @ValidatedDouble.Restrict(min = 0.1)
        public double segmentExplosionRadius = 2.5;

    }
}
