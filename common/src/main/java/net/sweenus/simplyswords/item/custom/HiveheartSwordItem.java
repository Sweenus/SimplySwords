package net.sweenus.simplyswords.item.custom;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.ConfigDefaultValues;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class HiveheartSwordItem extends UniqueSwordItem {
    public HiveheartSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    private static int stepMod = 0;

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            int skillCooldown = (int) Config.getFloat("hivemindCooldown", "UniqueEffects", ConfigDefaultValues.hivemindCooldown);
            float skillDamage = Config.getFloat("hivemindDamage", "UniqueEffects", ConfigDefaultValues.hivemindDamage);
            HelperMethods.playHitSounds(attacker, target);

            if (attacker instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            SimplySwordsBeeEntity beeEntity = EntityRegistry.SIMPLYBEEENTITY.get().spawn(
                    serverWorld,
                    attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3),
                    SpawnReason.MOB_SUMMONED);
                if (beeEntity != null && target != null) {
                    beeEntity.setTarget(target);
                    beeEntity.setAngryAt(target.getUuid());
                    beeEntity.setAngerTime(200);
                    beeEntity.shouldAngerAt(target);
                    beeEntity.setInvulnerable(true);
                    beeEntity.setOwner(attacker);
                    double attackDamage = (1 + skillDamage * this.getAttackDamage());
                    EntityAttributeInstance attackAttribute = beeEntity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    player.getItemCooldownManager().set(stack.getItem(), skillCooldown);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        int regenDuration = (int) Config.getFloat("hivemindDuration", "UniqueEffects", ConfigDefaultValues.hivemindDuration);
        int skillCooldown = (int) Config.getFloat("hivemindCooldown", "UniqueEffects", ConfigDefaultValues.hivemindCooldown);
        HelperMethods.incrementStatusEffect(user, StatusEffects.REGENERATION, regenDuration, 1, 3);
        ItemStack stack = user.getMainHandStack();
        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                user.getSoundCategory(), 0.8f, 1.0f);
        user.getItemCooldownManager().set(stack.getItem(), skillCooldown * 10);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (stepMod > 0) stepMod--;
        if (stepMod <= 0) stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.FALLING_HONEY,
                ParticleTypes.LANDING_HONEY, ParticleTypes.LANDING_HONEY, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        Style RIGHTCLICK = HelperMethods.getStyle("rightclick");
        Style ABILITY = HelperMethods.getStyle("ability");
        Style TEXT = HelperMethods.getStyle("text");

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip1").setStyle(ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip2").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip3").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip4").setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip5").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip6", Config.getFloat("hivemindCooldown", "UniqueEffects", ConfigDefaultValues.hivemindCooldown) / 20).setStyle(TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip7").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip8").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip9").setStyle(TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip10").setStyle(TEXT));
        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
    }
}
