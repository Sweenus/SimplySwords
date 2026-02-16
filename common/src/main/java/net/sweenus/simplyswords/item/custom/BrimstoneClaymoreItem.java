package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class BrimstoneClaymoreItem extends UniqueSwordItem implements TwoHandedWeapon {
    public BrimstoneClaymoreItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getEntityWorld();
            int fhitchance = Config.uniqueEffects.brimstone_claymore.chance;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance && attacker instanceof PlayerEntity player) {
                int choose_sound = (int) (Math.random() * 3);
                List<LivingEntity> nearbyEntities = HelperMethods.getNearbyLivingEntities(world, target.getEntityPos(), 3);
                DamageSource damageSource = player.getDamageSources().indirectMagic(player, player);

                for (LivingEntity livingEntity : nearbyEntities) {
                    if (HelperMethods.checkFriendlyFire(livingEntity, attacker)) {
                        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.LAVA, attacker, target, 3);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getEntityPos(), ParticleTypes.LAVA, 1, 3);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getEntityPos(), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 2, 6);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getEntityPos(), ParticleTypes.POOF, 1, 10);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getEntityPos(), ParticleTypes.EXPLOSION, 0.5, 2);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getEntityPos(), ParticleTypes.WARPED_SPORE, 1, 10);
                        livingEntity.setOnFireFor(3);
                        livingEntity.takeKnockback(1, 0.1, 0.1);
                        livingEntity.timeUntilRegen = 0;
                        livingEntity.damage((ServerWorld) livingEntity.getEntityWorld(), damageSource, (float) HelperMethods.getEntityAttackDamage(attacker));
                        livingEntity.timeUntilRegen = 0;
                    }
                }

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
        super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA,
                ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip2").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.BRIMSTONE_CLAYMORE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;

    }
}