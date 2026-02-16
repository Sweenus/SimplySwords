package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
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

public class SoulrenderSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public SoulrenderSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getEntityWorld();
            int hitChance = Config.uniqueEffects.soulrender.chance;
            int duration = Config.uniqueEffects.soulrender.duration;
            int maxStacks = Config.uniqueEffects.soulrender.maxStacks;
            ParticleEffect particleSelect  = ParticleTypes.ASH;
            int particleCount = 8; // Number of particles along the line

            if (attacker.getRandom().nextInt(100) <= hitChance) {
                particleSelect  = ParticleTypes.SMOKE;
                HelperMethods.spawnOrbitParticles(world, target.getEntityPos(), particleSelect, 0.5f, particleCount);
                HelperMethods.spawnOrbitParticles(world, target.getEntityPos().add(0,0.2,0), ParticleTypes.SOUL, 0.4f, 5);

                int choose_sound = (int) (Math.random() * 30);
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);
                else if (choose_sound <= 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                            target.getSoundCategory(), 0.4f, 1.5f);

                if (target.hasStatusEffect(StatusEffects.WEAKNESS)) {
                    int a = (target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1);

                    if ((target.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() <= 0)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0), attacker);
                }

                if (target.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    int a = (target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1);

                    if ((target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() < maxStacks)) {
                        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, a), attacker);
                    }
                } else {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 0), attacker);
                }
            }
            HelperMethods.spawnWaistHeightParticles(world, particleSelect, attacker, target, particleCount);
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.getEntityWorld().isClient()) {
            float heal_amount = Config.uniqueEffects.soulrender.healMulti;
            int healamp = 0;
            double hradius = Config.uniqueEffects.soulrender.radius;
            double vradius = Config.uniqueEffects.soulrender.radius / 2.0;
            double x = user.getX();
            double y = user.getY();
            double z = user.getZ();
            ServerWorld sworld = (ServerWorld) user.getEntityWorld();
            Box box = new Box(x + hradius, y + vradius, z + hradius, x - hradius, y - vradius, z - hradius);

            for (Entity entity : sworld.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                if ((entity instanceof LivingEntity le) && le.hasStatusEffect(StatusEffects.SLOWNESS)
                        && le.hasStatusEffect(StatusEffects.WEAKNESS) && HelperMethods.checkFriendlyFire(le, user)) {

                    healamp += (le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier());
                    float scaling = HelperMethods.commonSpellAttributeScaling(Config.uniqueEffects.soulrender.spellScaling, entity, "soul");
                    float multiplier = scaling > 0f ? scaling : Config.uniqueEffects.soulrender.damageMulti;
                    le.damage((ServerWorld) le.getEntityWorld(), user.getDamageSources().indirectMagic(user, user), le.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() * multiplier);
                    le.removeStatusEffect(StatusEffects.WEAKNESS);
                    le.removeStatusEffect(StatusEffects.SLOWNESS);
                    world.playSoundFromEntity(null, entity, SoundRegistry.DARK_SWORD_SPELL.get(),
                            entity.getSoundCategory(), 0.1f, 2f);
                }
            }
            if (healamp > 0) {
                float heal = ((float)healamp * heal_amount);

                if (heal < 1f) heal = 1f;
                else if (heal > 6f) heal = 6f;

                user.heal(heal);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL, ParticleTypes.SCULK_SOUL,
                ParticleTypes.WARPED_SPORE, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.rendsworditem.tooltip6").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "soul");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULRENDER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 85;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 500;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 8;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 10.0;

        @ValidatedFloat.Restrict(min = 0f)
        public float healMulti = 0.5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damageMulti = 3f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.4f;
    }
}