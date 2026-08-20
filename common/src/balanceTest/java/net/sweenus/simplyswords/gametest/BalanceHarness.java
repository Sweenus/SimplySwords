package net.sweenus.simplyswords.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.MovementType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.util.AbilityScalingProbe;
import net.sweenus.simplyswords.util.HelperMethods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

final class BalanceHarness {
    private static final int PROGRESS_INTERVAL_JOBS = 500;
    private static final int CHECKPOINT_INTERVAL_JOBS = 2000;
    private static final int DRAW_TICKS = 90;
    // Stable UUID: PlayerManager caches a PlayerAdvancements per UUID and never evicts it.
    private static final UUID BALANCE_PLAYER_UUID = UUID.fromString("5b91e3a2-0000-4000-8000-5eee00000001");

    private final TestContext context;
    private final ServerWorld world;
    private final List<BalanceJob> jobs;
    private final List<BalanceRunResult> results = new ArrayList<>();
    private final long startedAtNanos = System.nanoTime();
    private int jobIndex;
    private ActiveRun active;
    private boolean finished;

    BalanceHarness(TestContext context) {
        this.context = context;
        this.world = context.getWorld();
        verifyPlayerAttributes();
        this.jobs = createJobs();
        buildArena();
        holdAtNoon();
        SimplySwords.LOGGER.info("Balance harness starting: {} jobs, {} nominal ticks",
                jobs.size(), jobs.stream().mapToLong(BalanceHarness::jobTicks).sum());
    }

    private void verifyPlayerAttributes() {
        ItemStack weapon = firstRegisteredWeapon();
        if (weapon.isEmpty()) {
            return;
        }
        BalancePlayerEntity probe = createPlayer();
        try {
            float bare = HelperMethods.attackScaledDamage(probe, ItemStack.EMPTY, 1.0F);
            BalanceEquipment.equip(probe, EquipmentSlot.MAINHAND, weapon);
            float armed = HelperMethods.attackScaledDamage(probe, weapon, 1.0F);
            BalanceEquipment.equip(probe, EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            if (armed <= bare + 1.0E-4F) {
                throw new IllegalStateException("Harness player is not receiving equipment attributes: attack damage"
                        + " holding " + Registries.ITEM.getId(weapon.getItem()) + " was " + armed
                        + ", no better than bare-handed " + bare + ". Balance numbers would be meaningless.");
            }
        } finally {
            probe.discard();
        }
    }

    private static ItemStack firstRegisteredWeapon() {
        for (AbilityBalanceSpec spec : AbilityBalanceCatalog.all()) {
            Identifier id = Identifier.of("simplyswords", spec.weaponId());
            if (Registries.ITEM.containsId(id)) {
                return new ItemStack(Registries.ITEM.get(id));
            }
        }
        return ItemStack.EMPTY;
    }

    private static long jobTicks(BalanceJob job) {
        return job.scenario().rotation() ? 1200L : job.spec().durationTicks();
    }

    boolean isFinished() {
        return finished;
    }

    void tick() {
        if (finished) {
            return;
        }
        try {
            if (active == null) {
                if (jobIndex >= jobs.size()) {
                    finish();
                    return;
                }
                active = start(jobs.get(jobIndex));
                if (active.completed) {
                    completeActive();
                }
                return;
            }
            active.tick();
            if (active.completed) {
                completeActive();
            }
        } catch (Throwable throwable) {
            finished = true;
            context.throwGameTestException("Balance harness failed: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private ActiveRun start(BalanceJob job) {
        purgeStrayEntities();
        context.killAllEntities();
        AbilityBalanceSpec spec = job.spec();
        Identifier weaponId = Identifier.of("simplyswords", spec.weaponId());
        if (!Registries.ITEM.containsId(weaponId)) {
            return new ActiveRun(job, "Weapon is not registered in this mod set");
        }
        Item weapon = Registries.ITEM.get(weaponId);
        ItemStack stack = AwakeningApi.initializeFullyAwakened(new ItemStack(weapon));
        DamageRecorder recorder = new DamageRecorder();
        BalancePlayerEntity player = createPlayer();
        Vec3d targetCenter = context.getAbsolute(new Vec3d(8.0, 1.0, 9.0));
        Vec3d playerPosition = targetCenter.add(0.0, 0.0, -spec.targetDistance());
        player.setPosition(playerPosition);
        player.setNoGravity(true);
        player.setOnGround(true);

        EquipmentResult equipment = BalanceEquipment.apply(world, player, stack, job.build(), spec.spellProfile());
        if (!equipment.available()) {
            player.discard();
            return new ActiveRun(job, equipment.notes());
        }

        // Ability managers re-resolve their owner with world.getEntity each tick and abort when it is absent.
        Entity previous = world.getEntity(BALANCE_PLAYER_UUID);
        if (previous != null) {
            previous.discard();
        }
        if (!world.spawnEntity(player)) {
            throw new IllegalStateException("Harness player could not be spawned; abilities would silently record"
                    + " no damage because their managers resolve the owner from the world");
        }

        int targetCount = job.scenario() == BalanceScenarioType.FIVE_TARGET_CLUSTER ? 5 : 1;
        List<BalanceTargetEntity> targets = spawnTargets(targetCenter, targetCount, recorder);
        BalanceTargetEntity primaryTarget = targets.getFirst();
        aimAt(player, primaryTarget.getEyePos());
        ItemStack equippedWeapon = player.getEquippedStack(EquipmentSlot.MAINHAND);
        float attackCandidate = Math.max(0.0F, HelperMethods.attackScaledDamage(player, equippedWeapon, 1.0F));
        float rawSpell = Math.max(0.0F, equipment.rawSpellPower());
        float adjustedSpell = HelperMethods.applySpellDamageDiminishingReturns(rawSpell, attackCandidate);
        float rawRatio = attackCandidate > 0.0F ? rawSpell / attackCandidate : 0.0F;
        float adjustedRatio = attackCandidate > 0.0F ? adjustedSpell / attackCandidate : 0.0F;
        int duration = job.scenario().rotation() ? 1200 : spec.durationTicks();
        String notes = spec.notes() + "; " + equipment.notes() + "; category=" + spec.category()
                + "; activation=" + spec.activationMode();
        BranchAccumulator branches = new BranchAccumulator();
        AbilityScalingProbe.setSink((profile, kind, spellBranch, meleeBranch, chosen, spellWon) ->
                branches.record(kind, spellBranch, meleeBranch, spellWon));
        return new ActiveRun(job, player, equippedWeapon, targets, recorder, duration, rawRatio, adjustedRatio,
                branches, notes);
    }

    private BalancePlayerEntity createPlayer() {
        GameProfile profile = new GameProfile(BALANCE_PLAYER_UUID, "balance-test-player");
        ConnectedClientData clientData = ConnectedClientData.createDefault(profile, false);
        BalancePlayerEntity player = new BalancePlayerEntity(world.getServer(), world, profile);
        ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
        player.networkHandler = new ServerPlayNetworkHandler(world.getServer(), connection, player, clientData) {
            @Override
            public void sendPacket(Packet<?> packet) {
            }

            @Override
            public void send(Packet<?> packet, PacketCallbacks callbacks) {
            }
        };
        return player;
    }

    private static void aimAt(ServerPlayerEntity player, Vec3d target) {
        Vec3d delta = target.subtract(player.getEyePos());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
        player.bodyYaw = yaw;
    }

    private List<BalanceTargetEntity> spawnTargets(Vec3d center, int count, DamageRecorder recorder) {
        List<Vec3d> offsets = List.of(
                Vec3d.ZERO,
                new Vec3d(1.25, 0.0, 0.0),
                new Vec3d(-1.25, 0.0, 0.0),
                new Vec3d(0.0, 0.0, 1.25),
                new Vec3d(0.0, 0.0, -1.25)
        );
        List<BalanceTargetEntity> targets = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            BalanceTargetEntity target = new BalanceTargetEntity(world, recorder, center.add(offsets.get(index)));
            if (!world.spawnEntity(target)) {
                throw new IllegalStateException("Could not spawn balance target " + index);
            }
            targets.add(target);
        }
        return targets;
    }

    private void completeActive() {
        results.add(active.result());
        active.dispose();
        active = null;
        jobIndex++;
        if (jobIndex % PROGRESS_INTERVAL_JOBS == 0) {
            logProgress();
        }
        if (jobIndex % CHECKPOINT_INTERVAL_JOBS == 0) {
            writeCheckpoint();
        }
    }

    // killAllEntities works by calling kill(), which BalanceTargetEntity survives.
    private void purgeStrayEntities() {
        List<Entity> stray = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ServerPlayerEntity)) {
                stray.add(entity);
            }
        }
        stray.forEach(Entity::discard);
    }

    private void logProgress() {
        double elapsed = (System.nanoTime() - startedAtNanos) / 1.0E9;
        double rate = jobIndex / Math.max(0.001, elapsed);
        double remaining = (jobs.size() - jobIndex) / Math.max(0.001, rate);
        SimplySwords.LOGGER.info("Balance progress: job {}/{} ({}%) - {} elapsed, ~{} remaining",
                jobIndex, jobs.size(), (jobIndex * 100) / Math.max(1, jobs.size()),
                formatDuration(elapsed), formatDuration(remaining));
    }

    private void writeCheckpoint() {
        try {
            BalanceReportWriter.write(results);
        } catch (IOException exception) {
            SimplySwords.LOGGER.warn("Could not write balance checkpoint report", exception);
        }
    }

    private static String formatDuration(double seconds) {
        long total = (long) Math.max(0.0, seconds);
        return String.format(Locale.ROOT, "%d:%02d:%02d", total / 3600L, (total % 3600L) / 60L, total % 60L);
    }

    private void finish() throws IOException {
        BalanceReportWriter.write(results);
        finished = true;
        SimplySwords.LOGGER.info("Balance harness complete: {} jobs in {}",
                jobs.size(), formatDuration((System.nanoTime() - startedAtNanos) / 1.0E9));
        context.complete();
    }

    // Frozen so day-gated abilities behave the same for every job in a sweep.
    private void holdAtNoon() {
        world.setTimeOfDay(6000L);
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
    }

    private void buildArena() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    private static List<BalanceJob> createJobs() {
        Set<String> weaponFilter = Arrays.stream(
                        System.getProperty("simplyswords.balance.weapon", "").split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        String buildFilter = System.getProperty("simplyswords.balance.build", "").trim().toUpperCase(Locale.ROOT);
        String scenarioFilter = System.getProperty("simplyswords.balance.scenario", "").trim().toUpperCase(Locale.ROOT);
        int isolatedRepetitions = positiveProperty("simplyswords.balance.isolated-repetitions", 100);
        int rotationRepetitions = positiveProperty("simplyswords.balance.rotation-repetitions", 20);
        int reliabilityRepetitions = positiveProperty("simplyswords.balance.reliability-repetitions", 100);
        List<BalanceJob> jobs = new ArrayList<>();
        for (AbilityBalanceSpec spec : AbilityBalanceCatalog.all()) {
            if (!weaponFilter.isEmpty() && !weaponFilter.contains(spec.weaponId())) {
                continue;
            }
            for (BalanceBuildProfile build : BalanceBuildProfile.values()) {
                if (!buildFilter.isEmpty() && !build.name().equals(buildFilter)) {
                    continue;
                }
                addJobs(jobs, spec, build, BalanceScenarioType.ISOLATED_ABILITY, isolatedRepetitions, scenarioFilter);
                addJobs(jobs, spec, build, BalanceScenarioType.SINGLE_TARGET_ROTATION, rotationRepetitions, scenarioFilter);
                addJobs(jobs, spec, build, BalanceScenarioType.FIVE_TARGET_CLUSTER, rotationRepetitions, scenarioFilter);
                if (spec.reliabilitySensitive()) {
                    addJobs(jobs, spec, build, BalanceScenarioType.RELIABILITY, reliabilityRepetitions, scenarioFilter);
                }
            }
        }
        if (jobs.isEmpty()) {
            throw new IllegalArgumentException("Balance filters selected no scenarios");
        }
        return jobs;
    }

    private static void addJobs(List<BalanceJob> jobs, AbilityBalanceSpec spec, BalanceBuildProfile build,
                                BalanceScenarioType scenario, int repetitions, String scenarioFilter) {
        if (!scenarioFilter.isEmpty() && !scenario.name().equals(scenarioFilter)) {
            return;
        }
        for (int repetition = 0; repetition < repetitions; repetition++) {
            jobs.add(new BalanceJob(spec, build, scenario, repetition));
        }
    }

    private static int positiveProperty(String key, int fallback) {
        return Math.max(1, Integer.getInteger(key, fallback));
    }

    private final class ActiveRun {
        private final BalanceJob job;
        private final BalancePlayerEntity player;
        private final ItemStack weapon;
        private final List<BalanceTargetEntity> targets;
        private final DamageRecorder recorder;
        private final int duration;
        private final float rawRatio;
        private final float adjustedRatio;
        private final String notes;
        private final boolean available;
        private int elapsed;
        private int casts;
        private int swings;
        private int setupTicks;
        private int drawTicks = -1;
        private boolean completed;

        private final BranchAccumulator branches;

        private ActiveRun(BalanceJob job, BalancePlayerEntity player, ItemStack weapon,
                          List<BalanceTargetEntity> targets, DamageRecorder recorder, int duration,
                          float rawRatio, float adjustedRatio, BranchAccumulator branches, String notes) {
            this.branches = branches;
            this.job = job;
            this.player = player;
            this.weapon = weapon;
            this.targets = targets;
            this.recorder = recorder;
            this.duration = duration;
            this.rawRatio = rawRatio;
            this.adjustedRatio = adjustedRatio;
            this.notes = notes;
            this.available = true;
            if (job.spec().setup() == ScenarioSetup.PRIME_WITH_MELEE) {
                recorder.markMeleeTick(world.getTime());
                player.primeAttackCharge();
                player.attack(targets.getFirst());
                setupTicks = 12;
            }
        }

        private ActiveRun(BalanceJob job, String notes) {
            this.branches = null;
            this.job = job;
            this.player = null;
            this.weapon = ItemStack.EMPTY;
            this.targets = List.of();
            this.recorder = new DamageRecorder();
            this.duration = job.scenario().rotation() ? 1200 : job.spec().durationTicks();
            this.rawRatio = 0.0F;
            this.adjustedRatio = 0.0F;
            this.notes = notes;
            this.available = false;
            this.completed = true;
        }

        void tick() {
            if (setupTicks > 0) {
                setupTicks--;
                if (setupTicks == 0) {
                    recorder.clear();
                }
                return;
            }
            keepAimed();
            // The world does not tick a manually spawned player, so drive what abilities depend on.
            player.getItemCooldownManager().update();
            ManaTopUp.refill(player);
            applyPlayerMovement();
            if (job.scenario().rotation()) {
                runRotationTick();
            } else if (elapsed == 0) {
                activate();
            }
            elapsed++;
            if (elapsed >= duration) {
                completed = true;
            }
        }

        // Dash and leap abilities deliver their damage along the path the actor travels.
        private void applyPlayerMovement() {
            Vec3d velocity = player.getVelocity();
            if (velocity.lengthSquared() < 1.0E-6) {
                return;
            }
            player.move(MovementType.SELF, velocity);
            player.setVelocity(velocity.multiply(0.6));
        }

        private void runRotationTick() {
            // getAttackCooldownProgressPerTick already returns ticks per swing, not a fraction.
            int cadence = Math.max(1, (int) Math.ceil(player.getAttackCooldownProgressPerTick()));
            if (elapsed > 0 && elapsed % cadence == 0) {
                recorder.markMeleeTick(world.getTime());
                player.primeAttackCharge();
                player.attack(targets.getFirst());
                swings++;
            }
            activate();
        }

        private void activate() {
            if (drawTicks >= 0) {
                if (--drawTicks <= 0) {
                    weapon.getItem().onStoppedUsing(weapon, world, player, 0);
                    drawTicks = -1;
                    casts++;
                }
                return;
            }
            WeaponAbilityContext abilityContext = WeaponAbilityContext.of(
                    world,
                    weapon,
                    player,
                    null,
                    targets.getFirst(),
                    Hand.MAIN_HAND,
                    WeaponAbilityActivationSource.PLAYER
            );
            if (SimplySwordsAPI.tryActivateWeaponAbility(abilityContext)) {
                casts++;
                // Charge states are dropped unless the owner is still holding the item down.
                if (job.spec().activationMode() == ActivationMode.SUMMON
                        || job.spec().activationMode() == ActivationMode.CHANNEL) {
                    player.setCurrentHand(Hand.MAIN_HAND);
                }
                return;
            }
            // Charge weapons decline the ability API and run through use/onStoppedUsing instead.
            if (job.spec().activationMode() == ActivationMode.HOLD_RELEASE
                    && !player.getItemCooldownManager().isCoolingDown(weapon.getItem())
                    && weapon.getItem().use(world, player, Hand.MAIN_HAND).getResult().isAccepted()) {
                drawTicks = DRAW_TICKS;
            }
        }

        private void keepAimed() {
            if (player.isAlive() && !targets.isEmpty() && targets.getFirst().isAlive()) {
                aimAt(player, targets.getFirst().getEyePos());
            }
        }

        void dispose() {
            AbilityScalingProbe.setSink(null);
            if (player != null) {
                player.discard();
            }
        }

        BalanceRunResult result() {
            List<DamageEvent> events = recorder.events();
            boolean reliability = job.scenario() == BalanceScenarioType.RELIABILITY;
            int castsHit = reliability && events.stream().anyMatch(event -> !event.melee()) ? casts : 0;
            return new BalanceRunResult(job.spec(), job.build(), job.scenario(),
                    job.scenario() == BalanceScenarioType.FIVE_TARGET_CLUSTER ? 5 : 1,
                    duration, casts, castsHit, swings, events, rawRatio, adjustedRatio,
                    branches == null ? BranchStats.EMPTY : branches.snapshot(), notes, available);
        }
    }
}
