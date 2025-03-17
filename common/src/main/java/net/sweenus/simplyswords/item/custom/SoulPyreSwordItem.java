package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.TwoHandedWeapon;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.RelocationComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class SoulPyreSwordItem extends UniqueSwordItem implements TwoHandedWeapon {
    public SoulPyreSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            HelperMethods.playHitSounds(attacker, target);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            double range = Config.uniqueEffects.soulpyre.range;
            double radius = Config.uniqueEffects.soulpyre.radius;
            int ignite_duration = Config.uniqueEffects.soulpyre.igniteDuration / 20;
            int resistance_duration = Config.uniqueEffects.soulpyre.resistanceDuration;
            int relocationDuration = Config.uniqueEffects.soulpyre.duration;
            //Position swap target & player
            LivingEntity target = (LivingEntity) HelperMethods.getTargetedEntity(user, range);
            if (target != null && HelperMethods.checkFriendlyFire(target, user)) {
                ItemStack stack = user.getStackInHand(hand);
                double rememberX = target.getX();
                double rememberY = target.getY();
                double rememberZ = target.getZ();
                target.teleport(user.getX(), user.getY(), user.getZ(), true);
                user.teleport(rememberX, rememberY, rememberZ, true);
                world.playSoundFromEntity(null, user, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_01.get(),
                        user.getSoundCategory(), 0.3f, 1f);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, resistance_duration, 0), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, resistance_duration, 0), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, relocationDuration, 3), user);
                target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.FREEZE), relocationDuration - 10, 0), user);
                stack.set(ComponentTypeRegistry.RELOCATION.get(), new RelocationComponent(user.getX(), user.getY(), user.getZ(), target.getUuid(), relocationDuration, true));
                //AOE ignite & pull
                Box box = new Box(rememberX + radius, rememberY + radius, rememberZ + radius,
                        rememberX - radius, rememberY - radius, rememberZ - radius);
                for (Entity entity : world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY)) {
                    if ((entity instanceof LivingEntity le) && HelperMethods.checkFriendlyFire(le, user)) {
                        le.setVelocity((rememberX - le.getX()) / 4, (rememberY - le.getY()) / 4, (rememberZ - le.getZ()) / 4);
                        le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), user);
                        world.playSoundFromEntity(null, le, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                                le.getSoundCategory(), 0.1f, 3f);
                        le.setOnFireFor(ignite_duration);
                    }
                }
                user.getItemCooldownManager().set(this, relocationDuration);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && (entity instanceof PlayerEntity player)) {
            RelocationComponent component = stack.apply(ComponentTypeRegistry.RELOCATION.get(), RelocationComponent.DEFAULT, RelocationComponent::tickDown);
            if (component != null) {
                if (component.ready()) {
                    Entity relocateTarget = ((ServerWorld) world).getEntity(component.relocateTarget());
                    if (relocateTarget instanceof LivingEntity livingTarget) {
                        livingTarget.teleport(player.getX(), player.getY(), player.getZ(), true);
                    }
                    player.teleport(component.relocateX(), component.relocateY(), component.relocateZ(), true);
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_SWORD_SCIFI_ATTACK_03.get(),
                            player.getSoundCategory(), 0.3f, 1f);
                    stack.set(ComponentTypeRegistry.RELOCATION.get(), null);
                } else if (component.almostReady()) {
                    world.playSoundFromEntity(null, player, SoundRegistry.ELEMENTAL_BOW_RECHARGE.get(),
                            player.getSoundCategory(), 0.3f, 0.4f);
                }
            }
        }
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SOUL_FIRE_FLAME,
                ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.MYCELIUM, true);
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SMALL_FLAME,
                ParticleTypes.SMALL_FLAME, ParticleTypes.MYCELIUM, false);
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.SMOKE, ParticleTypes.SMOKE,
                ParticleTypes.MYCELIUM, false);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip6").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip7", Config.uniqueEffects.soulpyre.duration / 20).setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip8").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.soulpyresworditem.tooltip9").setStyle(Styles.TEXT));

        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULPYRE::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int duration = 120;
        @ValidatedDouble.Restrict(min = 1.0)
        public double radius = 8.0;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 32.0;

        @ValidatedInt.Restrict(min = 0)
        public int igniteDuration = 120;
        @ValidatedInt.Restrict(min = 0)
        public int resistanceDuration = 60;
    }
}