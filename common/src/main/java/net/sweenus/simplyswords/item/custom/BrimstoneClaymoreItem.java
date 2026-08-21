package net.sweenus.simplyswords.item.custom;

import net.sweenus.simplyswords.api.SimplySwordsAPI;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.interfaces.TwoHandedWeapon;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;
import net.sweenus.simplyswords.world.BrimstoneClaymoreAbilityManager;

import java.util.List;

public class BrimstoneClaymoreItem extends UniqueSwordItem implements TwoHandedWeapon, UniqueWeaponActiveAbility {
    public BrimstoneClaymoreItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return useFromDefaultInput(world, user, hand);
    }

    @Override
    public TypedActionResult<ItemStack> startPlayerAbility(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (hand == Hand.OFF_HAND || itemStack.getDamage() >= itemStack.getMaxDamage() - 1) {
            return TypedActionResult.fail(itemStack);
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity serverPlayer) {
            LivingEntity target = StealSwordItem.findLenientTarget(user, Config.uniqueEffects.brimstone_claymore.range);
            if (target == null || !activateBrimstone(serverWorld, serverPlayer, target, itemStack)) {
                return TypedActionResult.fail(itemStack);
            }
            SimplySwordsAPI.setWeaponCooldown(serverPlayer, itemStack, Config.uniqueEffects.brimstone_claymore.cooldown);
        }
        user.swingHand(hand);
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public boolean activate(WeaponAbilityContext context) {
        if (!canActivate(context)) {
            return false;
        }
        return activateBrimstone(context.world(), context.actor(), context.target(), context.stack());
    }

    @Override
    public int getActivationCooldownTicks(ItemStack stack, WeaponAbilityContext context) {
        return Config.uniqueEffects.brimstone_claymore.cooldown;
    }

    private static boolean activateBrimstone(ServerWorld world, LivingEntity owner, LivingEntity target, ItemStack stack) {
        if (target == null || !target.isAlive() || !HelperMethods.checkAbilityTarget(target, owner)) {
            return false;
        }
        BrimstoneClaymoreAbilityManager.start(world, owner, target, stack);
        return true;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!net.sweenus.simplyswords.api.AwakeningApi.isAbilityUnlocked(stack)) {
            return super.postHit(stack, target, attacker);
        }
        if (!attacker.getWorld().isClient()) {
            ServerWorld world = (ServerWorld) attacker.getWorld();
            int fhitchance = Config.uniqueEffects.brimstone_claymore.chance;
            HelperMethods.playHitSounds(attacker, target);

            if (attacker.getRandom().nextInt(100) <= fhitchance && attacker instanceof PlayerEntity player) {
                int choose_sound = (int) (Math.random() * 3);
                List<LivingEntity> nearbyEntities = HelperMethods.getNearbyLivingEntities(world, target.getPos(), 3);
                DamageSource damageSource = player.getDamageSources().indirectMagic(player, player);

                for (LivingEntity livingEntity : nearbyEntities) {
                    if (HelperMethods.checkAbilityTarget(livingEntity, attacker)) {
                        HelperMethods.spawnWaistHeightParticles(world, ParticleTypes.LAVA, attacker, target, 3);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.LAVA, 1, 3);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, 2, 6);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.POOF, 1, 10);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.EXPLOSION, 0.5, 2);
                        HelperMethods.spawnOrbitParticles(world, livingEntity.getPos(), ParticleTypes.WARPED_SPORE, 1, 10);
                        livingEntity.setOnFireFor(3);
                        livingEntity.takeKnockback(1, 0.1, 0.1);
                        livingEntity.timeUntilRegen = 0;
                        float damage = HelperMethods.abilityScaledDamage("fire", attacker, stack,
                                Config.uniqueEffects.brimstone_claymore.hitDamageScaling,
                                Config.uniqueEffects.brimstone_claymore.hitSpellScaling);
                        livingEntity.damage(damageSource, HelperMethods.applyAbilityDamageEnchantments(world, stack, livingEntity, damageSource, damage));
                        livingEntity.timeUntilRegen = 0;
                    }
                }

                if (choose_sound <= 1) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_01.get(),
                            target.getSoundCategory(), 0.5f, 1.2f);
                }
                if (choose_sound == 2) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_02.get(),
                            target.getSoundCategory(), 0.7f, 1.1f);
                }
                if (choose_sound == 3) {
                    world.playSoundFromEntity(null, target, SoundRegistry.ELEMENTAL_BOW_FIRE_SHOOT_IMPACT_03.get(),
                            target.getSoundCategory(), 0.9f, 1f);
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.FALLING_LAVA, ParticleTypes.FALLING_LAVA,
                ParticleTypes.SMOKE, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, net.minecraft.world.World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext tooltipContext) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.firesworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.brimstoneclaymoreitem.tooltip4").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, itemStack, Config.uniqueEffects.brimstone_claymore.cooldown);

        super.appendTooltip(itemStack, world, tooltip, tooltipContext);
        net.sweenus.simplyswords.client.util.TooltipUtils.appendSpellScaleTooltip(tooltip, "fire");
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.BRIMSTONE_CLAYMORE::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 15;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 600;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 22.0;
        @ValidatedInt.Restrict(min = 1)
        public int duration = 120;
        @ValidatedFloat.Restrict(min = 0.5f)
        public float baseRadius = 2.5f;
        @ValidatedFloat.Restrict(min = 0.5f)
        public float maxRadius = 6.0f;
        @ValidatedInt.Restrict(min = 1)
        public int pulseInterval = 20;
        @ValidatedFloat.Restrict(min = 0f)
        public float hitDamageScaling = 0.8f;
        @ValidatedFloat.Restrict(min = 0f)
        public float hitSpellScaling = 4.19f;
        @ValidatedFloat.Restrict(min = 0f)
        public float pulseDamageScaling = 0.28f;
        @ValidatedFloat.Restrict(min = 0f)
        public float radiusGrowthPerHit = 0.35f;
        @ValidatedFloat.Restrict(min = 0f)
        public float finalDamageScaling = 1.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float spellScaling = 5.3395f;
        @ValidatedDouble.Restrict(min = 0.0)
        public double targetJumpRange = 8.0;

    }
}
