package net.sweenus.simplyswords.item.custom;

import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.settings.ItemStackTooltipAppender;
import net.sweenus.simplyswords.config.settings.TooltipSettings;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.item.component.StoredChargeComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.HelperMethods;
import net.sweenus.simplyswords.util.Styles;

import java.util.List;
import java.util.Optional;

public class StealSwordItem extends UniqueSwordItem {
    private static final double BACKSTAB_DISTANCE = 1.35;
    private static final double TARGET_LENIENCY = 0.75;
    private static final ThreadLocal<Boolean> SUPPRESS_SOUL_DEBT_GAIN = ThreadLocal.withInitial(() -> false);

    public StealSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient()) {
            ServerWorld sworld = (ServerWorld) attacker.getWorld();
            HelperMethods.playHitSounds(attacker, target);

            if (!SUPPRESS_SOUL_DEBT_GAIN.get() && attacker.getRandom().nextInt(100) < Config.uniqueEffects.soulstealer.chance) {
                addSoulDebt(stack, Config.uniqueEffects.soulstealer.hitStacks);
                spawnSoulDebtGainEffects(sworld, target, attacker, stack);
            }
            if (!SUPPRESS_SOUL_DEBT_GAIN.get() && !target.isAlive() && Config.uniqueEffects.soulstealer.killStacks > 0) {
                addSoulDebt(stack, Config.uniqueEffects.soulstealer.killStacks);
                spawnSoulDebtGainEffects(sworld, target, attacker, stack);
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient() && world instanceof ServerWorld sworld && user instanceof ServerPlayerEntity serverPlayer) {
            int stacks = getSoulDebt(itemStack);
            LivingEntity target = stacks <= 0 ? null : findBackstabTarget(sworld, serverPlayer);
            Vec3d strikePos = target == null ? null : findBackstabPosition(sworld, serverPlayer, target);
            if (stacks <= 0 || target == null || strikePos == null) {
                spawnFailEffects(sworld, serverPlayer);
                return TypedActionResult.fail(itemStack);
            }

            performSoulReap(sworld, serverPlayer, itemStack, target, strikePos, stacks);
            serverPlayer.getItemCooldownManager().set(itemStack.getItem(), Config.uniqueEffects.soulstealer.cooldown);
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    private static void performSoulReap(ServerWorld world, ServerPlayerEntity player, ItemStack stack, LivingEntity target, Vec3d strikePos, int stacks) {
        Vec3d lookTarget = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        Vec3d strikeEyePos = strikePos.add(0.0, player.getEyeHeight(player.getPose()), 0.0);
        float[] rotation = getFacingRotation(strikeEyePos, lookTarget);
        spawnDepartureEffects(world, player);
        player.networkHandler.requestTeleport(strikePos.x, strikePos.y, strikePos.z, rotation[0], rotation[1]);
        player.setYaw(rotation[0]);
        player.setPitch(rotation[1]);
        player.setHeadYaw(rotation[0]);
        player.setBodyYaw(rotation[0]);
        player.swingHand(Hand.MAIN_HAND, true);
        player.setVelocity(0.0, 0.0, 0.0);
        player.velocityModified = true;

        float multiplier = getBackstabMultiplier(stacks);
        float damage = (float) (HelperMethods.getEntityAttackDamage(player) * multiplier);
        DamageSource damageSource = player.getDamageSources().playerAttack(player);
        target.timeUntilRegen = 0;
        boolean damaged = target.damage(damageSource, damage);
        if (damaged) {
            setSoulDebt(stack, 0);
            SUPPRESS_SOUL_DEBT_GAIN.set(true);
            try {
                stack.getItem().postHit(stack, target, player);
            } finally {
                SUPPRESS_SOUL_DEBT_GAIN.set(false);
            }
            if (!target.isAlive() && Config.uniqueEffects.soulstealer.killStacks > 0) {
                addSoulDebt(stack, Config.uniqueEffects.soulstealer.killStacks);
            }
            spawnBackstabEffects(world, target, player, stacks);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_03.get(),
                    SoundCategory.PLAYERS, 0.65F, 0.75F + world.random.nextFloat() * 0.2F);
        } else {
            spawnFailEffects(world, player);
        }
    }

    private static LivingEntity findBackstabTarget(ServerWorld world, ServerPlayerEntity player) {
        return findSoulstealerTarget(player);
    }

    public static LivingEntity findSoulstealerTarget(PlayerEntity player) {
        double range = Config.uniqueEffects.soulstealer.range;
        Entity targeted = HelperMethods.getTargetedEntity(player, range);
        if (targeted instanceof LivingEntity livingTarget
                && isValidSoulstealerTarget(livingTarget, player)) {
            return livingTarget;
        }

        Vec3d origin = player.getEyePos();
        Vec3d end = origin.add(player.getRotationVec(1.0F).normalize().multiply(range));
        Box searchBox = player.getBoundingBox().stretch(player.getRotationVec(1.0F).normalize().multiply(range)).expand(range);
        LivingEntity closestTarget = null;
        double closestDistance = range * range;
        for (Entity entity : player.getWorld().getOtherEntities(player, searchBox, entity -> entity instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity livingTarget) || !isValidSoulstealerTarget(livingTarget, player)) {
                continue;
            }

            Optional<Vec3d> hitPos = livingTarget.getBoundingBox().expand(TARGET_LENIENCY).raycast(origin, end);
            if (hitPos.isEmpty()) {
                continue;
            }

            double distance = origin.squaredDistanceTo(hitPos.get());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTarget = livingTarget;
            }
        }
        return closestTarget;
    }

    private static boolean isValidSoulstealerTarget(LivingEntity target, PlayerEntity player) {
        return target.isAlive()
                && EntityPredicates.VALID_LIVING_ENTITY.test(target)
                && HelperMethods.checkFriendlyFire(target, player);
    }

    private static Vec3d findBackstabPosition(ServerWorld world, ServerPlayerEntity player, LivingEntity target) {
        Vec3d behind = target.getRotationVec(1.0F);
        behind = new Vec3d(behind.x, 0.0, behind.z);
        if (behind.horizontalLengthSquared() < 0.001) {
            behind = target.getPos().subtract(player.getPos());
        }
        if (behind.horizontalLengthSquared() < 0.001) {
            behind = new Vec3d(0.0, 0.0, 1.0);
        }
        behind = behind.normalize().multiply(-BACKSTAB_DISTANCE);
        Vec3d side = new Vec3d(-behind.z, 0.0, behind.x).normalize();
        Vec3d[] candidates = new Vec3d[]{
                target.getPos().add(behind),
                target.getPos().add(behind).add(side.multiply(0.45)),
                target.getPos().add(behind).subtract(side.multiply(0.45)),
                target.getPos().add(behind.normalize().multiply(BACKSTAB_DISTANCE * 0.75))
        };

        for (Vec3d candidate : candidates) {
            Vec3d grounded = new Vec3d(candidate.x, target.getY(), candidate.z);
            if (isSafePosition(world, player, grounded)) {
                return grounded;
            }
        }
        return null;
    }

    private static boolean isSafePosition(ServerWorld world, ServerPlayerEntity player, Vec3d pos) {
        Box playerBox = player.getBoundingBox().offset(pos.subtract(player.getPos()));
        return world.isSpaceEmpty(player, playerBox) && !world.getBlockState(BlockPos.ofFloored(pos)).isLiquid();
    }

    private static float[] getFacingRotation(Vec3d fromEye, Vec3d to) {
        Vec3d diff = to.subtract(fromEye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new float[]{yaw, pitch};
    }

    private static float getBackstabMultiplier(int stacks) {
        int maxStacks = Math.max(1, Config.uniqueEffects.soulstealer.maxStacks);
        float minMultiplier = Config.uniqueEffects.soulstealer.minBackstabMultiplier;
        float maxMultiplier = Math.max(minMultiplier, Config.uniqueEffects.soulstealer.maxBackstabMultiplier);
        if (maxStacks <= 1) {
            return maxMultiplier;
        }
        float progress = MathHelper.clamp((float) (Math.min(stacks, maxStacks) - 1) / (float) (maxStacks - 1), 0.0F, 1.0F);
        return MathHelper.lerp(progress, minMultiplier, maxMultiplier);
    }

    private static int getSoulDebt(ItemStack stack) {
        return stack.getOrDefault(ComponentTypeRegistry.STORED_CHARGE.get(), StoredChargeComponent.DEFAULT).charge();
    }

    private static void addSoulDebt(ItemStack stack, int amount) {
        if (amount <= 0) {
            return;
        }
        int maxStacks = Math.max(1, Config.uniqueEffects.soulstealer.maxStacks);
        int current = getSoulDebt(stack);
        setSoulDebt(stack, Math.min(maxStacks, current + amount));
    }

    private static void setSoulDebt(ItemStack stack, int amount) {
        stack.set(ComponentTypeRegistry.STORED_CHARGE.get(), new StoredChargeComponent(Math.max(0, amount)));
    }

    private static void spawnSoulDebtGainEffects(ServerWorld world, LivingEntity target, LivingEntity attacker, ItemStack stack) {
        Vec3d targetPos = target.getPos().add(0.0, Math.max(0.35, target.getHeight() * 0.55), 0.0);
        Vec3d attackerPos = attacker.getPos().add(0.0, Math.max(0.35, attacker.getHeight() * 0.55), 0.0);
        Vec3d delta = attackerPos.subtract(targetPos);
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            Vec3d pos = targetPos.add(delta.multiply((double) i / steps));
            world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0.03, 0.03, 0.03, 0.01);
        }
        world.spawnParticles(ParticleTypes.SCULK_SOUL, targetPos.x, targetPos.y, targetPos.z, Math.min(6, 1 + getSoulDebt(stack)), 0.18, 0.18, 0.18, 0.02);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundRegistry.DARK_SWORD_ATTACK_WITH_BLOOD_01.get(),
                SoundCategory.PLAYERS, 0.32F, 1.55F + world.random.nextFloat() * 0.25F);
    }

    private static void spawnDepartureEffects(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, player.getHeight() * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 14, 0.28, 0.35, 0.28, 0.08);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 5, 0.16, 0.2, 0.16, 0.025);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.DARK_SWORD_UNFOLD.get(),
                SoundCategory.PLAYERS, 0.6F, 0.85F);
    }

    private static void spawnBackstabEffects(ServerWorld world, LivingEntity target, ServerPlayerEntity player, int stacks) {
        Vec3d pos = target.getPos().add(0.0, Math.max(0.45, target.getHeight() * 0.55), 0.0);
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.08, 0.05, 0.08, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 16, 0.28, 0.18, 0.28, 0.02);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 10 + Math.min(10, stacks * 2), 0.32, 0.28, 0.32, 0.04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 4 + Math.min(8, stacks), 0.22, 0.22, 0.22, 0.03);
        Vec3d playerPos = player.getPos().add(0.0, Math.max(0.4, player.getHeight() * 0.5), 0.0);
        Vec3d delta = playerPos.subtract(pos);
        for (int i = 1; i <= 6; i++) {
            Vec3d trail = pos.add(delta.multiply(i / 6.0));
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, trail.x, trail.y, trail.z, 1, 0.02, 0.02, 0.02, 0.025);
        }
    }

    private static void spawnFailEffects(ServerWorld world, ServerPlayerEntity player) {
        Vec3d pos = player.getPos().add(0.0, 0.8, 0.0);
        world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 3, 0.12, 0.1, 0.12, 0.006);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundRegistry.DARK_SWORD_BLOCK.get(),
                SoundCategory.PLAYERS, 0.35F, 1.55F);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        HelperMethods.createFootfalls(entity, stack, world, ParticleTypes.NAUTILUS, ParticleTypes.NAUTILUS,
                ParticleTypes.MYCELIUM, true);
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public void appendTooltip(ItemStack itemStack, TooltipContext tooltipContext, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip1").setStyle(Styles.ABILITY));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip2").setStyle(Styles.TEXT));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("item.simplyswords.onrightclick").setStyle(Styles.RIGHT_CLICK));
        tooltip.add(Text.translatable("item.simplyswords.stealsworditem.tooltip5").setStyle(Styles.TEXT));
        appendAbilityCooldownTooltip(tooltip, Config.uniqueEffects.soulstealer.cooldown);
        super.appendTooltip(itemStack, tooltipContext, tooltip, type);
    }

    public static class EffectSettings extends TooltipSettings {

        public EffectSettings() {
            super(new ItemStackTooltipAppender(ItemsRegistry.SOULSTEALER::get));
        }

        @ValidatedInt.Restrict(min = 0, max = 100)
        public int chance = 25;
        @ValidatedInt.Restrict(min = 0)
        public int cooldown = 20;
        @ValidatedDouble.Restrict(min = 1.0)
        public double range = 20.0;
        @ValidatedInt.Restrict(min = 1)
        public int maxStacks = 5;
        @ValidatedInt.Restrict(min = 0)
        public int hitStacks = 1;
        @ValidatedInt.Restrict(min = 0)
        public int killStacks = 2;
        @ValidatedFloat.Restrict(min = 0f)
        public float minBackstabMultiplier = 2.0f;
        @ValidatedFloat.Restrict(min = 0f)
        public float maxBackstabMultiplier = 5.0f;
    }
}
