package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class EmberIreSwordItem extends UniqueSwordItem {
    public EmberIreSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;
    private static DefaultParticleType particleWalk = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particleSprint = ParticleTypes.FALLING_LAVA;
    private static DefaultParticleType particlePassive = ParticleTypes.SMOKE;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int fhitchance = (int) SimplySwords.uniqueEffectsConfig.emberIreChance;
            int fduration = (int) SimplySwords.uniqueEffectsConfig.emberIreDuration;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                attacker.setOnFireFor(fduration / 20);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, fduration, 0), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, fduration, 1), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 0), attacker);
                world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_01.get(),
                        attacker.getSoundCategory(), 0.5f, 2f);
                particlePassive = ParticleTypes.LAVA;
                particleWalk = ParticleTypes.CAMPFIRE_COSY_SMOKE;
                particleSprint = ParticleTypes.CAMPFIRE_COSY_SMOKE;
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            if (user.hasStatusEffect(StatusEffects.STRENGTH) && user.hasStatusEffect(StatusEffects.HASTE) && user.hasStatusEffect(StatusEffects.SPEED)) {

                ServerWorld sWorld = (ServerWorld) user.getWorld();
                BlockPos position = (user.getBlockPos());
                Vec3d rotation = user.getRotationVec(1f);
                Vec3d newPos = user.getPos().add(rotation);

                FireballEntity fireball = new FireballEntity(EntityType.FIREBALL, world);
                fireball.updatePosition(newPos.getX(), (user.getY()) + 1.5, newPos.getZ());
                fireball.setOwner(user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 4), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 2), user);
                sWorld.spawnEntity(fireball);
                fireball.setVelocity(rotation);
                user.removeStatusEffect(StatusEffects.STRENGTH);
                user.removeStatusEffect(StatusEffects.SPEED);
                user.removeStatusEffect(StatusEffects.HASTE);
                world.playSound(null, position, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                        user.getSoundCategory(), 0.3f, 2f);
                user.extinguish();
            }
        }
        return super.use(world, user, hand);
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
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, particleWalk, particleSprint, particlePassive, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip5").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
