package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
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
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.LivyatanEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class LivyatanSwordItem extends UniqueSwordItem {
    public LivyatanSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getEntityWorld().isClient()) {
            super.postHit(stack, target, attacker);
            return;
        }
        HelperMethods.playHitSounds(attacker, target);

        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        float abilityDamage = HelperMethods.spellScaledDamage("frost", user, Config.uniqueEffects.livyatan.spellScaling, Config.uniqueEffects.livyatan.damage);
        int duration = Config.uniqueEffects.livyatan.duration;
        float returnDamage = Config.uniqueEffects.livyatan.returnDamage;
        double radius = Config.uniqueEffects.livyatan.radius;
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient()) {
            itemStack = user.getStackInHand(hand);
            LivyatanEntity livyatanEntity = new LivyatanEntity(world, user, itemStack.copy() );
            livyatanEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            livyatanEntity.setYaw(user.getYaw());
            livyatanEntity.setPitch(user.getPitch());
            livyatanEntity.primaryBaseDamage = abilityDamage;
            livyatanEntity.slownessDuration = duration;
            livyatanEntity.primaryReturnDamage = returnDamage;
            livyatanEntity.primaryReturnDamageRadius = radius;
            if (hand == Hand.OFF_HAND)
                livyatanEntity.offhandThrow = true;
            livyatanEntity.setPos(user.getX(), user.getEyeY() - 0.5, user.getZ());
            world.spawnEntity(livyatanEntity);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }
        }

        user.swingHand(hand);

        user.getItemCooldownManager().set(this.getDefaultStack(), 1);
        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SNOWFLAKE, ParticleTypes.SNOWFLAKE, ParticleTypes.WHITE_ASH, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.livyatansworditem.tooltip5").setStyle(Styles.TEXT));
        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "frost");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.LIVYATAN::get));
        }

        @ValidatedFloat.Restrict(min = 0)
        public float returnDamage = 8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 8f;
        @ValidatedInt.Restrict(min = 0)
        public int duration = 100;
        @ValidatedDouble.Restrict(min = 0.5)
        public double radius = 0.5;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.7f;
    }
}