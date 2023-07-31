package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class WatcherSwordItem extends UniqueSwordItem {
    public WatcherSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();

            int watcherChance = (int) SimplySwords.uniqueEffectsConfig.watcherChance;
            int omenChance = (int) SimplySwords.uniqueEffectsConfig.omenChance;

            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= watcherChance) {
                int hradius = (int) (SimplySwords.uniqueEffectsConfig.watcherRadius);
                int vradius = (int) (SimplySwords.uniqueEffectsConfig.watcherRadius / 2);
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                float rAmount = SimplySwords.uniqueEffectsConfig.watcherRestoreAmount;
                Box box = new Box(x + hradius, y + vradius, z + hradius,
                        x - hradius, y - vradius, z - hradius);

                for (Entity entity : world.getOtherEntities(attacker, box, EntityPredicates.VALID_ENTITY)) {
                    if (entity instanceof LivingEntity && HelperMethods.checkFriendlyFire((LivingEntity) entity, attacker)) {
                        entity.damage(attacker.getDamageSources().magic(), rAmount);
                        attacker.heal(rAmount);
                        BlockPos position2 = entity.getBlockPos();
                        world.playSound(null, position2, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_02.get(),
                                entity.getSoundCategory(), 0.05f, 1.2f);
                    }
                }
            }

            if (attacker.getRandom().nextInt(100) <= omenChance) {
                BlockPos position = target.getBlockPos();
                float overallAbsorptionCap = SimplySwords.uniqueEffectsConfig.abilityAbsorptionCap;
                float absorptionCap = SimplySwords.uniqueEffectsConfig.omenAbsorptionCap;
                float threshold = SimplySwords.uniqueEffectsConfig.omenInstantKillThreshold * target.getMaxHealth();
                float remainingHealth = Math.min(target.getHealth(), absorptionCap);

                if (remainingHealth <= threshold) {
                    attacker.setAbsorptionAmount(Math.min(overallAbsorptionCap, attacker.getAbsorptionAmount() + remainingHealth));
                    world.playSound(null, position, SoundRegistry.ELEMENTAL_BOW_SCIFI_SHOOT_IMPACT_03.get(),
                            target.getSoundCategory(), 0.7f, 1.2f);
                    target.damage(attacker.getDamageSources().magic(), 1000);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.ENCHANT, ParticleTypes.ENCHANT,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip3",
                (SimplySwords.uniqueEffectsConfig.omenInstantKillThreshold * 100)).setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip5").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.watchersworditem.tooltip7").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
