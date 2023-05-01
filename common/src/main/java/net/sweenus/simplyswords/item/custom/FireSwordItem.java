package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
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
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("brimstone_chance");
            HelperMethods.playHitSounds(attacker, target);


            if (attacker.getRandom().nextInt(100) <= fhitchance && (attacker instanceof PlayerEntity player)) {
                int choose_sound = (int) (Math.random() * 3);

                BlockPos position = target.getBlockPos();
                for (int i = 0; i < 5 * choose_sound; i++) {
                    HelperMethods.spawnParticle(world, ParticleTypes.LAVA, position.getX(), position.getY()+0.5, position.getZ(), choose_sound, 0.5 + choose_sound, choose_sound);
                    HelperMethods.spawnParticle(world, ParticleTypes.SMOKE, position.getX(), position.getY()+0.5, position.getZ(), 0, 0, 0);
                }

                world.createExplosion(attacker, target.getX(), target.getY(), target.getZ(), choose_sound, Explosion.DestructionType.NONE);
                target.setOnFireFor(3);

                if (choose_sound <= 1)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(), SoundCategory.PLAYERS, 0.5f, 1.2f);
                if (choose_sound <= 2 && choose_sound > 1)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(), SoundCategory.PLAYERS, 0.7f, 1.1f);
                if (choose_sound <= 3 && choose_sound > 2)
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.9f, 1f);
            }
        }

        return super.postHit(stack, target, attacker);

    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA, ParticleTypes.SMOKE, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip2"));

        /*
        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.firesworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.firesworditem.tooltip2"));

         */
        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
