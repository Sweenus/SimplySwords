package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;

public class HarbingerSwordItem extends UniqueSwordItem {

    public HarbingerSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

	@Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.getWorld().isClient() && attacker.getRandom().nextInt(100) <= Config.uniqueEffects.abyssalStandard.chance && attacker instanceof PlayerEntity) {
            attacker.getWorld().playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(),
                    attacker.getSoundCategory(), 0.3f, 1.6f);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0), attacker);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!user.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) user.getWorld();
            BlockState currentState = world.getBlockState(user.getBlockPos().up(4).offset(user.getMovementDirection(), 3));
            BlockState state = Blocks.AIR.getDefaultState();
            if (currentState == state) {
                world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                        user.getSoundCategory(), 0.4f, 0.8f);
                BattleStandardDarkEntity banner = EntityRegistry.BATTLESTANDARDDARK.get().spawn(
                        serverWorld,
                        user.getBlockPos().up(4).offset(user.getMovementDirection(), 3),
                        SpawnReason.MOB_SUMMONED);
                if (banner != null) {
                    banner.setVelocity(0, -1, 0);
                    banner.ownerEntity = user;
                    banner.decayRate = 3;
                    banner.standardType = "harbinger";
                    banner.setCustomName(Text.translatable("entity.simplyswords.battlestandard.name", user.getName()));
                }
                user.getItemCooldownManager().set(this.getDefaultStack().getItem(), Config.uniqueEffects.abyssalStandard.cooldown);
            }
        }
        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip3").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip4").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip5").setStyle(Styles.TEXT));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip6").setStyle(Styles.TEXT));
        HelperMethods.appendSpellScaleTooltip(tooltip, "soul");
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.HARBINGER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 700;
        @ValidatedFloat.Restrict(min = 0f)
        public float damage = 3f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 1.2f;

    }
}