package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.annotations.Translation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ChanceDurationSettings;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;
import java.util.Optional;

public class EmberIreSwordItem extends UniqueSwordItem {
    public EmberIreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    private static SimpleParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static SimpleParticleType particlePassive = ParticleTypes.SMOKE;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!world.isClient && remainingUseTicks %10 == 0 && remainingUseTicks < getMaxUseTime(stack, user) - 5) {
            world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(),
                    user.getSoundCategory(), 0.2f, 1.1f - (remainingUseTicks * 0.001f));

            if (remainingUseTicks < 20) {
                onStoppedUsing(stack, world, user, remainingUseTicks);
            }

        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient && user.getEquippedStack(EquipmentSlot.MAINHAND) == stack) {
            Optional<LivingEntity> targetEntityReturn = HelperMethods.findClosestTarget(user, 18, 3);
            double damageAmount = HelperMethods.getEntityAttackDamage(user) * 0.3;
            if (targetEntityReturn.isPresent() && HelperMethods.checkFriendlyFire(targetEntityReturn.get(), user)) {
                LivingEntity targetEntity = targetEntityReturn.get();
                SoundEvent soundSelect = SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get();
                int particleCount = 20;
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.SMOKE, user, targetEntity, particleCount);
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.POOF, user, targetEntity, particleCount);
                HelperMethods.spawnWaistHeightParticles((ServerWorld) world, ParticleTypes.ASH, user, targetEntity, particleCount);
                world.playSound(null, user.getBlockPos(), soundSelect,
                        user.getSoundCategory(), 0.4f, 1.5f);
                DamageSource damageSource = user.getDamageSources().generic();
                if (user instanceof PlayerEntity player) {
                    damageSource = user.getDamageSources().playerAttack(player);
                    player.getItemCooldownManager().set(stack.getItem(), 10);
                }

                final float minAdditionalDamage = 0.0f;
                final float maxAdditionalDamage = (float) (HelperMethods.getEntityAttackDamage(user) * 3);
                float chargeRatio = 1.0f - ((float) remainingUseTicks / getMaxUseTime(stack, user));
                float additionalDamage = minAdditionalDamage + (maxAdditionalDamage - minAdditionalDamage) * chargeRatio;
                float finalDamage = (float) damageAmount + additionalDamage;
                targetEntity.timeUntilRegen = 0;
                targetEntity.damage(damageSource, finalDamage);

                world.playSound(null, targetEntity.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        user.getSoundCategory(), 0.4f, 1.1f);
                HelperMethods.spawnOrbitParticles((ServerWorld) world, targetEntity.getPos(), ParticleTypes.EXPLOSION, 1, 1 );
                HelperMethods.spawnOrbitParticles((ServerWorld) world, targetEntity.getPos(), ParticleTypes.POOF, 1, 20 );
                user.setVelocity(user.getRotationVector().negate().multiply(+1.1));
                user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z);
                user.velocityModified = true;

                int hitChance = Config.uniqueEffects.emberblade.chance;
                int duration = Config.uniqueEffects.emberblade.duration;

                if (user.getRandom().nextInt((int) (250 - (chargeRatio * 100))) <= hitChance) {
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1), user);
                    user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 0), user);
                    world.playSoundFromEntity(null, user, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                            user.getSoundCategory(), 0.5f, 2f);
                    particlePassive = ParticleTypes.LAVA;
                    particleWalk = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                    particleSprint = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                }

            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 80;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if ((entity instanceof PlayerEntity player)) {
            if (!player.hasStatusEffect(StatusEffects.STRENGTH) && !player.isOnFire()) {
                particlePassive = ParticleTypes.SMOKE;
                particleWalk = ParticleTypes.FALLING_LAVA;
                particleSprint = ParticleTypes.FALLING_LAVA;
            }
        }
        int stepMod = 7 - (int)(world.getTime() % 7);
        HelperMethods.createFootfalls(entity, stack, world, particleWalk, particleSprint, particlePassive, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclickheld").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip9").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    @Translation(prefix = "", negate = true)
    public static class EffectSettings extends ChanceDurationSettings {

        public EffectSettings() {
            super(30, 150, new ItemStackTooltipAppender(ItemsRegistry.EMBERBLADE::get));
        }
    }
}
