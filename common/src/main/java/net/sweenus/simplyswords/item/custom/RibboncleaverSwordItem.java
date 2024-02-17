package net.sweenus.simplyswords.item.custom;

import dev.architectury.platform.Platform;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class RibboncleaverSwordItem extends UniqueSwordItem {
    public RibboncleaverSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {

            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        int skillCooldown = 40;//(int) Config.getFloat("celestialSurgeCooldown", "UniqueEffects", ConfigDefaultValues.celestialSurgeCooldown);


        world.playSound(null, user.getBlockPos(), SoundRegistry.ELEMENTAL_BOW_EARTH_SHOOT_IMPACT_03.get(),
                user.getSoundCategory(), 0.4f, 1.3f);
        user.setVelocity(user.getRotationVector().multiply(+1.7));
        user.setVelocity(user.getVelocity().x, 0, user.getVelocity().z); // Prevent user flying to the heavens
        user.velocityModified = true;
        user.getItemCooldownManager().set(this, skillCooldown);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        //HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.FALLING_OBSIDIAN_TEAR,
        //        ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        //Drag weapon particles
        if (entity.isOnGround() && Platform.isModLoaded("bettercombat") && HelperMethods.isWalking(entity)) {

            BlockState blockState =  entity.getSteppingBlockState();
            ParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState);

            double bodyRadians = Math.toRadians(entity.getBodyYaw() + 180);
            Vec3d backwardDirection = new Vec3d(-Math.sin(bodyRadians), 0, Math.cos(bodyRadians)).multiply(1.1);

            double strafeRadians = Math.toRadians(entity.getBodyYaw() + 90);
            Vec3d strafeDirection = new Vec3d(-Math.sin(strafeRadians), 0, Math.cos(strafeRadians));

            Vec3d movementVector = entity.getVelocity();
            double strafeMagnitude = movementVector.dotProduct(strafeDirection.normalize());

            double pivotOffsetFactor = 3;
            Vec3d pivotOffset = strafeDirection.multiply(strafeMagnitude * pivotOffsetFactor);

            Vec3d adjustedBackwardDirection = backwardDirection.subtract(pivotOffset);

            Vec3d handPosOffset = entity.getHandPosOffset(stack.getItem());
            double particleX = entity.getX() + adjustedBackwardDirection.x + handPosOffset.getX();
            double particleY = entity.getY() + handPosOffset.getY();
            double particleZ = entity.getZ() + adjustedBackwardDirection.z + handPosOffset.getZ();

            particleY = entity.isOnGround() ? entity.getY() : particleY;

            world.addParticle(particleEffect, particleX, particleY, particleZ, 0, 0.0, 0);
            world.addParticle(ParticleTypes.POOF, particleX, particleY, particleZ, 0, 0.0, 0);

        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");
        float skillDamageModifier = Config.getFloat("celestialSurgeDamageModifier", "UniqueEffects", ConfigDefaultValues.celestialSurgeDamageModifier);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip3", getAttackDamage() * skillDamageModifier).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip6").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.starsedgesworditem.tooltip8").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
