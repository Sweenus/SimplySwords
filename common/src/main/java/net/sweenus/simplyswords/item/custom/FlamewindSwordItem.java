package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.sweenus.simplyswords.client.util.TooltipUtils;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.effect.instance.SimplySwordsStatusEffectInstance;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.Comparator;
import java.util.List;

public class FlamewindSwordItem extends UniqueSwordItem {
    public FlamewindSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {

            ServerWorld serverWorld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient() && world instanceof  ServerWorld serverWorld) {
            int flameSeedDuration = 101;
            int flameSeedSpreadCap = Config.uniqueEffects.flamewind.spreadCap;
            int skillCooldown = Config.uniqueEffects.flamewind.cooldown;

            Box box = HelperMethods.createBox(user, 10);
            Entity closestEntity = world.getOtherEntities(user, box, EntityPredicates.VALID_LIVING_ENTITY).stream()
                    .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(user)))
                    .orElse(null);

            if (closestEntity != null) {
                if ((closestEntity instanceof LivingEntity ee)) {
                    if (HelperMethods.checkFriendlyFire(ee, user) && !ee.hasStatusEffect(EffectRegistry.getReference(EffectRegistry.FLAMESEED))) {

                        SoundEvent soundSelect = SoundRegistry.SPELL_FIRE.get();
                        int particleCount = 20; // Number of particles along the line
                        HelperMethods.spawnWaistHeightParticles(serverWorld, ParticleTypes.LAVA, user, ee, particleCount);
                        world.playSound(null, user.getBlockPos(), soundSelect,
                                user.getSoundCategory(), 0.3f, 1.3f);

                        SimplySwordsStatusEffectInstance flamSeedEffect = new SimplySwordsStatusEffectInstance(
                                EffectRegistry.getReference(EffectRegistry.FLAMESEED), flameSeedDuration, 0, false,
                                false, true);
                        flamSeedEffect.setSourceEntity(user);
                        flamSeedEffect.setAdditionalData(flameSeedSpreadCap);
                        ee.addStatusEffect(flamSeedEffect);
                        user.getItemCooldownManager().set(this, skillCooldown);
                    }
                }
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FLAME,
                ParticleTypes.FLAME, ParticleTypes.ASH, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip7").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.flamewindsworditem.tooltip11", Config.uniqueEffects.flamewind.spreadCap).setStyle(Styles.TEXT));
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
        TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.FLAMEWIND::get));
        }

        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 980;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 5f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 0.4f;
        @ValidatedFloat.Restrict(min = 0f)
        public float detonationDamage = 15f;
        @ValidatedInt.Restrict(min = 0)
        public int maxHaste = 10;
        @ValidatedInt.Restrict(min = 1)
        public int spreadCap = 6;

    }
}
