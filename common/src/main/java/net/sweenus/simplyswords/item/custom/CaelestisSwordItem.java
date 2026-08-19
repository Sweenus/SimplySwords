package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
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
import net.sweenus.simplyswords.world.CaelestisBreachManager;

import java.util.List;

public class CaelestisSwordItem extends UniqueSwordItem implements UniqueWeaponActiveAbility {
    public CaelestisSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {

            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public boolean canActivate(WeaponAbilityContext context) {
        return context != null
                && context.world() != null
                && context.actor() != null
                && context.actor().isAlive()
                && context.stack() != null
                && !context.stack().isEmpty()
                && context.stack().getDamage() < context.stack().getMaxDamage() - 1
                && !CaelestisBreachManager.hasActiveForActor(context.world(), context.actor().getUuid());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        return CaelestisBreachManager.start(context);
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Math.max(1, Config.uniqueEffects.caelestis.cooldown);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT,
                ParticleTypes.ENCHANT, ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip5",
                Config.uniqueEffects.caelestis.duration / 20,
                Math.round(Config.uniqueEffects.caelestis.maxRadius)).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.caelestissworditem.tooltip8",
                Config.uniqueEffects.caelestis.collapseDuration / 20,
                Config.uniqueEffects.caelestis.betrayalChance).setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.caelestis.cooldown);

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "eldritch");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.CAELESTIS::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 1800;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 900;
        @ValidatedInt.Restrict(min = 1)
        public int expansionDuration = 140;
        @ValidatedInt.Restrict(min = 1)
        public int collapseDuration = 100;
        @ValidatedFloat.Restrict(min = 1.0f)
        public float maxRadius = 20.0f;
        @ValidatedFloat.Restrict(min = 2.0f)
        public float verticalRange = 10.0f;
        @ValidatedInt.Restrict(min = 1)
        public int spawnInterval = 30;
        @ValidatedInt.Restrict(min = 1)
        public int minSpawnPerWave = 2;
        @ValidatedInt.Restrict(min = 1)
        public int maxSpawnPerWave = 3;
        @ValidatedInt.Restrict(min = 1)
        public int maxMinions = 12;
        @ValidatedInt.Restrict(min = 0)
        public int maxTentacles = 12;
        @ValidatedInt.Restrict(min = 1)
        public int tentacleSpawnInterval = 30;
        @ValidatedInt.Restrict(min = 1)
        public int tentacleSlowDuration = 20;
        @ValidatedInt.Restrict(min = 0, max = 4)
        public int tentacleSlowAmplifier = 1;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int betrayalChance = 5;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int unboundTabletDropChance = 1;
        @ValidatedFloat.Restrict(min = 0f)
        public float minionHealthScaling = 0.08f;
        @ValidatedFloat.Restrict(min = 0f)
        public float minionDamageScaling = 0.30f;
        @ValidatedFloat.Restrict(min = 0f)
        public float minionSpellScaling = 2.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float unboundDamageMultiplier = 1.25f;
        @ValidatedInt.Restrict(min = 0)
        public int riftlingWeight = 50;
        @ValidatedInt.Restrict(min = 0)
        public int hollowWeight = 35;
        @ValidatedInt.Restrict(min = 0)
        public int dreadglareWeight = 15;

    }
}
