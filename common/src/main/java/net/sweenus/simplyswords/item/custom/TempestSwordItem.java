package net.sweenus.simplyswords.item.custom;

import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.RegistrySupplier;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.List;

public class TempestSwordItem extends UniqueSwordItem {
    public TempestSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {

            int vortexMaxStacks = Config.uniqueEffects.vortex.maxStacks;
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);
            SoundEvent soundSelect;
            ParticleEffect particleSelect;
            RegistryEntry<StatusEffect> statusSelect;

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

            List<RegistryEntry<StatusEffect>> status = new ArrayList<>();
            status.add(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX));
            status.add(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX));
            status.add(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX));
            status.add(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX));

            int random      = attacker.getRandom().nextInt(3);
            soundSelect     = sounds.get(random);
            particleSelect  = particles.get(random);
            statusSelect    = status.get(random);


            int particleCount = 10; // Number of particles along the line
            HelperMethods.spawnWaistHeightParticles(serverWorld, particleSelect, attacker, target, particleCount);
            serverWorld.playSound(null, attacker.getBlockPos(), soundSelect,
                    attacker.getSoundCategory(), 0.2f, 1.3f);

            SimplySwordsStatusEffectInstance effect = HelperMethods.incrementSimplySwordsStatusEffect(
                    target, statusSelect, 500, 1, vortexMaxStacks);

            effect.setSourceEntity(attacker);
            effect.setAdditionalData((int) HelperMethods.getEntityAttackDamage(attacker) / 3);
            target.addStatusEffect(effect);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {

            int vortexMaxSize = Config.uniqueEffects.vortex.maxSize;
            int vortexDuration = Config.uniqueEffects.vortex.duration;
            int skillCooldown = 200;
            Box box = HelperMethods.createBox(user, 15);
            boolean soundHasPlayed = false;
            for (Entity entity : serverWorld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {

                    if (le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX)) && le.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX))) {
                        StatusEffectInstance frostVortex = le.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX));
                        StatusEffectInstance fireVortex  = le.getStatusEffect(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX));
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

                        SimplySwordsStatusEffectInstance status = HelperMethods.incrementSimplySwordsStatusEffect(user, EffectRegistry.getReference(EffectRegistry.ELEMENTAL_VORTEX), vortexDuration, totalAmplifier, vortexMaxSize);
                        status.setAdditionalData(Math.max(1, totalAmplifier));
                        status.setSourceEntity(user);
                        le.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FIRE_VORTEX));
                        le.removeStatusEffect(EffectRegistry.getReference(EffectRegistry.FROST_VORTEX));
                        user.getItemCooldownManager().set(this, skillCooldown);
                    }
                }
            }

        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.BUBBLE,
                ParticleTypes.BUBBLE_COLUMN_UP, ParticleTypes.BUBBLE_POP, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip9").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip10").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.tempestsworditem.tooltip11").setStyle(Styles.TEXT));
        if (Platform.isModLoaded("spell_power") || Platform.isModLoaded("irons_spellbooks")) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleFrost"));
            tooltip.add(Text.translatable("item.simplyswords.compat.scaleFire"));
        }

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.TEMPEST::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int duration = 1200;
        @ValidatedInt.Restrict(min = 1)
        public int maxSize = 30;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 10;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.3f;

    }
}