package net.sweenus.simplyswords.item.custom;


import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.EntityRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.List;

public class HarbingerSwordItem extends UniqueSwordItem {

    private static int stepMod = 0;
    int skillCooldown = (int) (SimplySwordsConfig.getFloatValue("abyssalstandard_cooldown"));
    int abilityChance =  (int) (SimplySwordsConfig.getFloatValue("abyssalstandard_chance"));

    public HarbingerSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {



        HelperMethods.playHitSounds(attacker, target);
        if (!attacker.world.isClient()) {

            if (attacker.getRandom().nextInt(100) <= abilityChance && (attacker instanceof PlayerEntity player)) {
                attacker.world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.3f, 1.6f);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0), attacker);
            }
        }

            return super.postHit(stack, target, attacker);
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        ItemStack itemStack = user.getStackInHand(hand);
        if (!user.world.isClient()) {
            ServerWorld serverWorld = (ServerWorld) user.world;
            BlockState currentState = world.getBlockState(user.getBlockPos().up(4).offset(user.getMovementDirection(), 3));
            BlockState state = Blocks.AIR.getDefaultState();
            if (currentState == state ) {
                world.playSoundFromEntity(null, user, SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_02.get(),
                        SoundCategory.PLAYERS, 0.4f, 0.8f);
                BattleStandardDarkEntity banner = EntityRegistry.BATTLESTANDARDDARK.get().spawn(serverWorld, null,
                        Text.translatable( "entity.simplyswords.battlestandard.name",user.getName()), user,
                        user.getBlockPos().up(4).offset(user.getMovementDirection(), 3),
                        SpawnReason.MOB_SUMMONED, true, true);
                if (banner != null) {
                    banner.setVelocity(0, -1, 0);
                    banner.ownerEntity = user;
                    banner.decayRate = 3;
                    banner.standardType = "harbinger";
                }
                user.getItemCooldownManager().set(this.getDefaultStack().getItem(), skillCooldown);
            }
        }

        return super.use(world, user, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {

        if (stepMod > 0)
            stepMod --;
        if (stepMod <= 0)
            stepMod = 7;
        HelperMethods.createFootfalls(entity, stack, world, stepMod, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, ParticleTypes.MYCELIUM, true);

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.DARK_RED, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip2"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip3"));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip5"));
        tooltip.add(Text.translatable("item.simplyswords.harbingersworditem.tooltip6"));

        super.appendTooltip(itemStack,world, tooltip, tooltipContext);
    }

}
