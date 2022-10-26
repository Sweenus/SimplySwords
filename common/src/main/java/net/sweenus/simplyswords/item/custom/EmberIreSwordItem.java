package net.sweenus.simplyswords.item.custom;


import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.sweenus.simplyswords.config.SimplySwordsConfig;
import net.sweenus.simplyswords.registry.SoundRegistry;

import java.util.List;

public class EmberIreSwordItem extends SwordItem {
    public EmberIreSwordItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.world.isClient()) {
            ServerWorld world = (ServerWorld) attacker.world;
            int fhitchance = (int) SimplySwordsConfig.getFloatValue("ember_ire_chance");
            int fduration = (int) SimplySwordsConfig.getFloatValue("ember_ire_duration");

            boolean impactsounds_enabled = (SimplySwordsConfig.getBooleanValue("enable_weapon_impact_sounds"));

            if (impactsounds_enabled) {
                int choose_sound = (int) (Math.random() * 30);
                float choose_pitch = (float) Math.random() * 2;
                if (choose_sound <= 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_01.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 20 && choose_sound > 10)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_02.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 30 && choose_sound > 20)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_03.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
                if (choose_sound <= 40 && choose_sound > 30)
                    world.playSoundFromEntity(null, target, SoundRegistry.MAGIC_SWORD_ATTACK_WITH_BLOOD_04.get(), SoundCategory.PLAYERS, 0.5f, 1.1f + choose_pitch);
            }


            if (attacker.getRandom().nextInt(100) <= fhitchance) {
                attacker.setOnFireFor(fduration / 20);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, fduration, 2), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, fduration, 1), attacker);
                attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, fduration, 1), attacker);
                world.playSoundFromEntity(null, attacker, SoundRegistry.MAGIC_SWORD_SPELL_01.get(), SoundCategory.PLAYERS, 0.5f, 2f);
            }
        }

            return super.postHit(stack, target, attacker);

        }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack)).formatted(Formatting.GOLD, Formatting.BOLD, Formatting.UNDERLINE);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {

        if (!user.world.isClient()) {
            if (user.hasStatusEffect(StatusEffects.STRENGTH) && user.isOnFire()) {

                Box box = new Box(user.getX() + 40, user.getY() +40, user.getZ() + 40, user.getX() - 40, user.getY() - 40, user.getZ() - 40);
                for(Entity e: world.getOtherEntities(user, box, EntityPredicates.VALID_ENTITY))
                {
                    if (e != null && user != null){
                        if (e instanceof FireballEntity){
                            boolean bl = e.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
                            e.world.createExplosion(null, e.getX(), e.getY(), e.getZ(), 1f, bl, bl ? Explosion.DestructionType.DESTROY : Explosion.DestructionType.NONE);
                            e.discard();
                        }
                    }
                }

                ServerWorld sworld = (ServerWorld)user.world;
                BlockPos position = (user.getBlockPos());
                Vec3d rotation = user.getRotationVec(1f);
                Vec3d newPos = user.getPos().add(rotation);

                //Entity fireball = EntityType.FIREBALL.spawn((ServerWorld) world, null, null, null, newPos, SpawnReason.TRIGGERED, true, true);
                FireballEntity fireball = new FireballEntity(EntityType.FIREBALL, (ServerWorld) world);
                fireball.updatePosition(newPos.getX(), (user.getY()) +1.5, newPos.getZ());
                fireball.setOwner(user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20, 4), user);
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 2), user);
                sworld.spawnEntity(fireball);
                fireball.setVelocity(rotation);
                user.removeStatusEffect(StatusEffects.STRENGTH);
                user.removeStatusEffect(StatusEffects.SPEED);
                user.removeStatusEffect(StatusEffects.HASTE);
                world.playSound(null, position, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(), SoundCategory.PLAYERS, 0.3f, 2f);
                user.extinguish();


            }
        }
        return super.use(world,user,hand);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, World world, List<Text> tooltip, TooltipContext tooltipContext) {

        //1.19.x

        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip2"));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip3"));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip4"));
        tooltip.add(Text.translatable("item.simplyswords.emberiresworditem.tooltip5"));
        /*

        //1.18.2
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip1").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip2"));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip3"));
        tooltip.add(new LiteralText(""));
        tooltip.add(new TranslatableText("item.simplyswords.onrightclick").formatted(Formatting.BOLD, Formatting.GREEN));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip4"));
        tooltip.add(new TranslatableText("item.simplyswords.emberiresworditem.tooltip5"));

         */
    }

}
