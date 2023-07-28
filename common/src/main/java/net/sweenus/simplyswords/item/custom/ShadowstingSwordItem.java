package net.sweenus.simplyswords.item.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class ShadowstingSwordItem extends UniqueSwordItem {
    private static int stepMod = 0;
    int skillCooldown = (int) SimplySwords.uniqueEffectsConfig.shadowmistCooldown;
    int abilityChance = (int) SimplySwords.uniqueEffectsConfig.shadowmistChance;
    int damageArmorMultiplier = (int) (SimplySwords.uniqueEffectsConfig.shadowmistDamageMulti * 2);
    int blindDuration = (int) (SimplySwords.uniqueEffectsConfig.shadowmistBlindDuration);
    int radius = (int) (SimplySwords.uniqueEffectsConfig.shadowmistRadius);

    public ShadowstingSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient()) {
            if (attacker.getRandom().nextInt(100) <= abilityChance && attacker instanceof PlayerEntity) {
                attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.8f);
                int extraDamage = (target.getArmor() * damageArmorMultiplier) / 2;
                target.damage(attacker.getDamageSources().magic(), extraDamage);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                user.getSoundCategory(), 0.4f, 1.6f);
        user.getItemCooldownManager().set(this.getDefaultStack().getItem(), skillCooldown);

        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, blindDuration, 3), user);
            }
        }

        double xpos = user.getX() - (radius + 1);
        double ypos = user.getY();
        double zpos = user.getZ() - (radius + 1);

        for (int i = radius * 2; i > 0; i--) {
            for (int j = radius * 2; j > 0; j--) {
                float choose = (float) (Math.random() * 1);
                HelperMethods.spawnParticle(world, ParticleTypes.LARGE_SMOKE,
                        xpos + i + choose, ypos + 0.4, zpos + j + choose,
                        0, 0.1, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        xpos + i + choose, ypos + 0.1, zpos + j + choose,
                        0, 0, 0);
                HelperMethods.spawnParticle(world, ParticleTypes.SMOKE,
                        xpos + i + choose, ypos, zpos + j + choose,
                        0, 0.1, 0);
            }
        }
        BlockState currentStateLow = world.getBlockState(user.getBlockPos().offset(user.getMovementDirection(), 5));
        double targetPositionX = user.getBlockPos().offset(user.getMovementDirection(), 5).getX();
        double targetPositionY = user.getBlockPos().offset(user.getMovementDirection(), 5).getY();
        double targetPositionZ = user.getBlockPos().offset(user.getMovementDirection(), 5).getZ();
        BlockState currentStateHigh = world.getBlockState(user.getBlockPos().up(1).offset(user.getMovementDirection(), 5));
        BlockState state = Blocks.AIR.getDefaultState();
        if (currentStateLow == state && currentStateHigh == state) {
            user.teleport(targetPositionX, targetPositionY, targetPositionZ);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(RIGHTCLICK));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip5").setStyle(TEXT));

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
