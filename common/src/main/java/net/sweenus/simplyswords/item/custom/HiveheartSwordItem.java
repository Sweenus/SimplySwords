package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.SimplySwordsBeeEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class HiveheartSwordItem extends UniqueSwordItem {
    public HiveheartSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getEntityWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) attacker.getEntityWorld();
            int skillCooldown = Config.uniqueEffects.hiveheart.cooldown;
            float skillDamage = Config.uniqueEffects.hiveheart.damage;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker instanceof PlayerEntity player && !player.getItemCooldownManager().isCoolingDown(stack)) {
            SimplySwordsBeeEntity beeEntity = EntityRegistry.SIMPLYBEEENTITY.get().spawn(
                    serverWorld,
                    attacker.getBlockPos().up(4).offset(attacker.getMovementDirection(), 3),
                    SpawnReason.MOB_SUMMONED);
                if (beeEntity != null && target != null) {
                    beeEntity.setTarget(target);
                    beeEntity.setInvulnerable(true);
                    beeEntity.setOwner(attacker);
                    double attackDamage = (1 + skillDamage * HelperMethods.getEntityAttackDamage(attacker));
                    EntityAttributeInstance attackAttribute = beeEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
                    if (attackAttribute != null)
                        attackAttribute.setBaseValue(attackDamage);
                    player.getItemCooldownManager().set(stack, skillCooldown);
                }
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {

        int regenDuration = Config.uniqueEffects.hiveheart.duration;
        int skillCooldown = Config.uniqueEffects.hiveheart.cooldown;
        HelperMethods.incrementStatusEffect(user, StatusEffects.REGENERATION, regenDuration, 1, 3);
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(null, user.getBlockPos(), SoundRegistry.SPELL_MISC_02.get(),
                user.getSoundCategory(), 0.8f, 1.0f);
        user.getItemCooldownManager().set(stack, skillCooldown * 10);

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_HONEY,
                ParticleTypes.LANDING_HONEY, ParticleTypes.LANDING_HONEY, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip6", Config.uniqueEffects.hiveheart.cooldown / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip9").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.hiveheartsworditem.tooltip10").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HIVEHEART::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 60;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 1.1f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 450;

    }
}
