package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.List;

public class TempestSwordItem extends UniqueSwordItem {
    public TempestSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {

            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);
            SoundEvent soundSelect;
            ParticleEffect particleSelect;
            StatusEffect statusSelect;

            List<SoundEvent> sounds = new ArrayList<>();
            sounds.add(SoundRegistry.SPELL_FIRE.get());
            sounds.add(SoundRegistry.ELEMENTAL_SWORD_WATER_ATTACK_03.get());
            sounds.add(SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get());
            sounds.add(SoundRegistry.ELEMENTAL_BOW_WATER_SHOOT_IMPACT_02.get());

            List<ParticleEffect> particles = new ArrayList<>();
            particles.add(ParticleTypes.SMOKE);
            particles.add(ParticleTypes.CLOUD);
            particles.add(ParticleTypes.SMOKE);
            particles.add(ParticleTypes.CLOUD);

            List<StatusEffect> status = new ArrayList<>();
            status.add(EffectRegistry.FIRE_VORTEX.get());
            status.add(EffectRegistry.FROST_VORTEX.get());
            status.add(EffectRegistry.FIRE_VORTEX.get());
            status.add(EffectRegistry.FROST_VORTEX.get());

            int random      = attacker.getRandom().nextInt(3);
            soundSelect     = sounds.get(random);
            particleSelect  = particles.get(random);
            statusSelect    = status.get(random);


            int particleCount = 10; // Number of particles along the line
            HelperMethods.spawnWaistHeightParticles(serverWorld, particleSelect, attacker, target, particleCount);
            serverWorld.playSound(null, attacker.getBlockPos(), soundSelect,
                    attacker.getSoundCategory(), 0.2f, 1.3f);

            SimplySwordsStatusEffectInstance effect = HelperMethods.incrementSimplySwordsStatusEffect(
                    target, statusSelect, 500, 1, 10);

            effect.setSourceEntity(attacker);
            effect.setAdditionalData((int) getAttackDamage() / 3);
            target.addStatusEffect(effect);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {

            int skillCooldown = 200;
            Box box = HelperMethods.createBox(user, 15);
            boolean soundHasPlayed = false;
            for (Entity entity : serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

                    if (le.hasStatusEffect(EffectRegistry.FIRE_VORTEX.get()) && le.hasStatusEffect(EffectRegistry.FROST_VORTEX.get())) {
                        StatusEffectInstance frostVortex = le.getStatusEffect(EffectRegistry.FROST_VORTEX.get());
                        StatusEffectInstance fireVortex  = le.getStatusEffect(EffectRegistry.FIRE_VORTEX.get());
                        int totalAmplifier = 0;
                        if (fireVortex != null && frostVortex != null)
                            totalAmplifier = fireVortex.getAmplifier() + frostVortex.getAmplifier();

                        int particleCount = 10; // Number of particles along the line
                        HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.CLOUD, le, user, particleCount);
                        if (!soundHasPlayed) {
                            serverWorld.playSound(null, le.getBlockPos(), SoundRegistry.MAGIC_SHAMANIC_NORDIC_22.get(),
                                    le.getSoundCategory(), 0.4f, 1.3f);
                            soundHasPlayed = true;
                        }

                        SimplySwordsStatusEffectInstance status = HelperMethods.incrementSimplySwordsStatusEffect(user, EffectRegistry.ELEMENTAL_VORTEX.get(), 1200, totalAmplifier, 30);
                        status.setAdditionalData(Math.max(1, totalAmplifier));
                        status.setSourceEntity(user);
                        le.removeStatusEffect(EffectRegistry.FIRE_VORTEX.get());
                        le.removeStatusEffect(EffectRegistry.FROST_VORTEX.get());
                        user.getItemCooldownManager().set(this, skillCooldown);
                    }
                }
            }

        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.BUBBLE,
                ParticleTypes.BUBBLE_COLUMN_UP, ParticleTypes.BUBBLE_POP, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip8").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.waxweaversworditem.tooltip9", Config.getFloat("waxweaveCooldown", "UniqueEffects", ConfigDefaultValues.waxweaveCooldown) / 20).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.wickpiercersworditem.tooltip6", Config.getFloat("flickerFuryDuration", "UniqueEffects", ConfigDefaultValues.flickerFuryDuration) / 20).setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
