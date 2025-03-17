package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class WatcherSwordItem extends UniqueSwordItem {
    public WatcherSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();

            int watcherChance = Config.uniqueEffects.watcher.watcherChance;
            int omenChance = Config.uniqueEffects.watcher.omenChance;

            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= watcherChance) {
                double hradius = Config.uniqueEffects.watcher.watcherRadius;
                double vradius = Config.uniqueEffects.watcher.watcherRadius / 2.0;
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                float rAmount = Config.uniqueEffects.watcher.watcherRestoreAmount;
                Box box = new Box(x + hradius, y + vradius, z + hradius,
                        x - hradius, y - vradius, z - hradius);

                for (Entity entity : world.getOtherEntities(attacker, box, EntityPredicates.VALID_ENTITY)) {
                    if (entity instanceof LivingEntity && HelperMethods.checkFriendlyFire((LivingEntity) entity, attacker)) {
                        entity.damage(attacker.getDamageSources().indirectMagic(attacker, attacker), rAmount);
                        attacker.heal(rAmount);
                        BlockPos position2 = entity.getBlockPos();
                        world.playSound(null, position2, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                entity.getSoundCategory(), 0.05f, 1.2f);
                    }
                }
            }

            if (attacker.getRandom().nextInt(100) <= omenChance) {
                BlockPos position = target.getBlockPos();
                float overallAbsorptionCap = Config.uniqueEffects.abilityAbsorptionCap;
                float absorptionCap = Config.uniqueEffects.watcher.omenAbsorptionCap;
                float threshold = Config.uniqueEffects.watcher.omenInstantKillThreshold * target.getMaxHealth();
                float remainingHealth = target.getHealth();

                if (remainingHealth <= threshold) {
                    attacker.setAbsorptionAmount(Math.min(Math.min(absorptionCap, overallAbsorptionCap), attacker.getAbsorptionAmount() + remainingHealth));
                    world.playSound(null, position, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                            target.getSoundCategory(), 0.7f, 1.2f);
                    target.damage(attacker.getDamageSources().indirectMagic(attacker, attacker), 1000);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip3", (Config.uniqueEffects.watcher.omenInstantKillThreshold * 100)).setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip5").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip7").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    @Override
    protected Identifier getConfigPath() {
        return Identifier.of("simplyswords.unique_effects.watcher");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.WATCHER_CLAYMORE::get, ItemsRegistry.WATCHING_WARGLAIVE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int watcherChance = 5;
        @ValidatedDouble.Restrict(min = 1.0)
        public double watcherRadius = 8.0;
        @ValidatedFloat.Restrict(min = 0f)
        public float watcherRestoreAmount = 0.5f;

        @ValidatedFloat.Restrict(min = 0, max = 100)
        public float omenAbsorptionCap = 20f;
        @ValidatedInt.Restrict(min = 0, max = 100)
        public int omenChance = 5;
        @ValidatedFloat.Restrict(min = 0f, max = 1f)
        public float omenInstantKillThreshold = 0.25f;
    }
}