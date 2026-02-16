package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class ShadowstingSwordItem extends UniqueSwordItem {

    public ShadowstingSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public void postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getEntityWorld().isClient()) {
            if (attacker.getRandom().nextInt(100) <= Config.uniqueEffects.shadowsting.chance && attacker instanceof PlayerEntity) {
                attacker.getEntityWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                        attacker.getSoundCategory(), 0.3f, 1.8f);
                float extraDamage = (target.getArmor() * Config.uniqueEffects.shadowsting.damageMulti) / 2;
                target.damage((ServerWorld) attacker.getEntityWorld(), attacker.getDamageSources().indirectMagic(attacker, attacker), extraDamage);
            }
        }
        super.postHit(stack, target, attacker);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_EARTH_ATTACK_01.get(),
                user.getSoundCategory(), 0.4f, 1.6f);
        user.getItemCooldownManager().set(this.getDefaultStack(), Config.uniqueEffects.shadowsting.cooldown);

        int radius = Config.uniqueEffects.shadowsting.radius;
        Box box = new Box(user.getX() + radius, user.getY() + radius, user.getZ() + radius,
                user.getX() - radius, user.getY() - radius, user.getZ() - radius);
        for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
            if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                le.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, Config.uniqueEffects.shadowsting.blindDuration, 3), user);
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
        double targetEntityPositionX = user.getBlockPos().offset(user.getMovementDirection(), 5).getX();
        double targetEntityPositionY = user.getBlockPos().offset(user.getMovementDirection(), 5).getY();
        double targetEntityPositionZ = user.getBlockPos().offset(user.getMovementDirection(), 5).getZ();
        BlockState currentStateHigh = world.getBlockState(user.getBlockPos().up(1).offset(user.getMovementDirection(), 5));
        BlockState state = Blocks.AIR.getDefaultState();
        if (currentStateLow == state && currentStateHigh == state) {
            user.teleport(targetEntityPositionX, targetEntityPositionY, targetEntityPositionZ, false);
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot);
    }

    @Override
    protected void appendItemTooltip(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.shadowmistsworditem.tooltip5").setStyle(Styles.TEXT));

        super.appendItemTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SHADOWSTING::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 200;
        @ValidatedFloat.Restrict(min = 0)
        public float damageMulti = 0.8f;
        @ValidatedInt.Restrict(min = 1)
        public int radius = 4;
        @ValidatedInt.Restrict(min = 0)
        public int blindDuration = 60;

    }
}
