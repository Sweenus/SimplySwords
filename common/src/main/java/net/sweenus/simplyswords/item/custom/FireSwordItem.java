package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class FireSwordItem extends UniqueSwordItem {
    public FireSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int fhitchance = (int) SimplySwords.uniqueEffectsConfig.brimstoneChance;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance && attacker instanceof PlayerEntity) {
                int choose_sound = (int) (Math.random() * 3);

                BlockPos position = target.getBlockPos();
                for (int i = 0; i < 5 * choose_sound; i++) {
                    HelperMethods.spawnParticle(world, ParticleTypes.LAVA, position.getX(), position.getY() + 0.5, position.getZ(),
                            choose_sound, 0.5 + choose_sound, choose_sound);
                    HelperMethods.spawnParticle(world, ParticleTypes.SMOKE, position.getX(), position.getY() + 0.5, position.getZ(),
                            0, 0, 0);
                }
                world.createExplosion(attacker, target.getX(), target.getY(), target.getZ(), choose_sound, World.ExplosionSourceType.NONE);
                target.setOnFireFor(3);

                if (choose_sound <= 1) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                            target.getSoundCategory(), 0.5f, 1.2f);
                }
                if (choose_sound == 2) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                            target.getSoundCategory(), 0.7f, 1.1f);
                }
                if (choose_sound == 3) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                            target.getSoundCategory(), 0.9f, 1f);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA,
                ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style TEXT = HelperMethods.getStyle("text");
        Style ABILITY = HelperMethods.getStyle("ability");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip2").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
