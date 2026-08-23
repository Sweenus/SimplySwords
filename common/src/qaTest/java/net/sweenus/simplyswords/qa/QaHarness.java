package net.sweenus.simplyswords.qa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.api.WeaponAbilityActivationSource;
import net.sweenus.simplyswords.api.WeaponAbilityContext;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueWeaponItem;
import net.sweenus.simplyswords.item.interfaces.UniqueWeaponActiveAbility;
import net.sweenus.simplyswords.loot.PityLootManager;
import net.sweenus.simplyswords.loot.PlayerPityState;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityChannelManager;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QaHarness {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String QA_TEAM_NAME = "simplyswords_qa_team";
    private static final int DEFAULT_RUNIC_SAMPLES = 3;
    private static final float QA_ABSORPTION = 2048.0F;
    private static final int ARENA_FLOOR_Y = 192;
    private static final int ARENA_FEET_Y = ARENA_FLOOR_Y + 1;
    private static final Box ARENA_BOX = new Box(-17, ARENA_FLOOR_Y, -17, 18, ARENA_FLOOR_Y + 18, 18);
    private static final double ACTOR_X = 0.5;
    private static final double ACTOR_Z = -1.0;
    private static final double TARGET_X = 0.5;
    private static final double TARGET_Z = 1.5;
    private static final Map<String, ClientState> CLIENTS = new HashMap<>();
    private static Run run;
    private static long lastBootstrapWrite;

    private QaHarness() {}

    public static synchronized int start(ServerCommandSource source, String rawProfile, long seed, int startIndex) {
        return start(source, rawProfile, seed, startIndex, DEFAULT_RUNIC_SAMPLES);
    }

    public static synchronized int start(ServerCommandSource source, String rawProfile, long seed,
                                         int startIndex, int runicSamples) {
        if (run != null) {
            source.sendError(Text.literal("A QA run is already active"));
            return 0;
        }
        String profile = rawProfile.toLowerCase(Locale.ROOT);
        if (!profile.equals("startup") && !profile.equals("fast")
                && !profile.equals("smoke") && !profile.equals("full")) {
            source.sendError(Text.literal("Profile must be startup, fast, smoke, or full"));
            return 0;
        }
        if (runicSamples < 1) {
            source.sendError(Text.literal("Runic sample count must be at least 1"));
            return 0;
        }
        List<ServerPlayerEntity> players = requiredPlayers(source.getServer());
        if (players.size() != 2) {
            source.sendError(Text.literal("SimplyTest1 and SimplyTest2 must both be connected"));
            return 0;
        }
        for (ServerPlayerEntity player : players) {
            ClientState state = CLIENTS.get(player.getUuidAsString());
            if (state == null || state.protocol != QaNetwork.PROTOCOL
                    || System.nanoTime() - state.lastHeartbeat > 15_000_000_000L) {
                source.sendError(Text.literal("QA client handshake missing or stale for " + player.getName().getString()));
                return 0;
            }
        }
        try {
            CasePlan plan = buildCases(profile, seed, runicSamples);
            List<Case> cases = plan.cases;
            if (startIndex < 0 || (cases.isEmpty() ? startIndex != 0 : startIndex >= cases.size())) {
                source.sendError(Text.literal("QA start index must be between 0 and " + (cases.size() - 1)));
                return 0;
            }
            run = new Run(source.getServer(), profile, seed, runicSamples, plan, startIndex);
            run.event("run_started", null, "running", "QA run started", null);
            source.sendFeedback(() -> Text.literal("Simply Swords QA started: " + run.cases.size() + " cases"), true);
            return 1;
        } catch (IOException exception) {
            source.sendError(Text.literal("Could not create QA report: " + exception.getMessage()));
            return 0;
        }
    }

    public static synchronized int abort(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        if (run == null) return 0;
        run.finish("aborted", "Run aborted from the server console");
        run = null;
        return 1;
    }

    public static synchronized int status(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        String value = run == null ? "idle" : run.index + "/" + run.cases.size() + " " + run.activeId();
        context.getSource().sendFeedback(() -> Text.literal("Simply Swords QA: " + value), false);
        return 1;
    }

    public static synchronized void onClientStatus(ServerPlayerEntity player, QaNetwork.ClientStatus packet) {
        CLIENTS.put(player.getUuidAsString(), new ClientState(packet.protocol, System.nanoTime()));
        if (run != null && !packet.caseId.isEmpty()) {
            run.clientEvents.computeIfAbsent(packet.caseId, ignored -> new ClientProgress()).accept(packet);
            if (packet.kind.equals("exception") || packet.kind.equals("error")) {
                run.event("client_error", run.active, "failed", packet.detail,
                        Map.of("player", player.getName().getString(), "kind", packet.kind));
            }
        }
    }

    public static synchronized void tick(MinecraftServer server) {
        long now = System.nanoTime();
        if (now - lastBootstrapWrite >= 1_000_000_000L) {
            lastBootstrapWrite = now;
            writeBootstrap(server, now);
        }
        if (run == null || run.server != server) return;
        try {
            run.tick();
            if (run.finished) run = null;
        } catch (Throwable throwable) {
            String detail = throwable.getClass().getName() + ": " + throwable.getMessage();
            throwable.printStackTrace();
            run.event("run_error", run.active, "failed",
                    detail, null);
            run.finish("failed", detail);
            run = null;
        }
    }

    private static List<ServerPlayerEntity> requiredPlayers(MinecraftServer server) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        for (String name : List.of("SimplyTest1", "SimplyTest2")) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
            if (player != null) result.add(player);
        }
        return result;
    }

    private static CasePlan buildCases(String profile, long seed, int runicSamples) {
        if (profile.equals("startup")) return new CasePlan(List.of(), Map.of(), Map.of(), Map.of());
        List<Item> uniques = Registries.ITEM.stream()
                .filter(item -> item instanceof UniqueWeaponItem)
                .sorted(Comparator.comparing(item -> Registries.ITEM.getId(item).toString())).toList();
        List<Item> runics = Registries.ITEM.stream()
                .filter(item -> item instanceof RunicSwordItem)
                .sorted(Comparator.comparing(item -> Registries.ITEM.getId(item).toString())).toList();
        List<Identifier> runefused = powers(PowerType.RUNEFUSED);
        List<Identifier> nether = powers(PowerType.NETHER);
        List<Identifier> runic = powers(PowerType.RUNIC);
        List<Case> cases = new ArrayList<>();
        Map<String, List<String>> sampledRunefused = new HashMap<>();
        Map<String, List<String>> sampledRunic = new HashMap<>();
        Map<String, List<String>> sampledNether = new HashMap<>();

        int selectedCount = profile.equals("full") ? runicSamples : 1;
        for (Item weapon : uniques) {
            Identifier weaponId = Registries.ITEM.getId(weapon);
            if (!profile.equals("fast") && weapon instanceof UniqueWeaponActiveAbility) {
                for (int level = 0; level < 4; level++) {
                    cases.add(Case.of("awakening_locked_" + level, weapon,
                            null, null, "ability", false, level));
                }
            }
            int awakening = profile.equals("smoke") ? 4 : 8;
            if (profile.equals("fast")) {
                List<Identifier> netherSelection = samplePowers(nether, seed, weaponId, "nether", 1);
                List<Identifier> runefusedSelection = samplePowers(
                        runefused, seed, weaponId, "runefused", 1);
                sampledNether.put(weaponId.toString(), netherSelection.stream()
                        .map(Identifier::toString).toList());
                sampledRunefused.put(weaponId.toString(), runefusedSelection.stream()
                        .map(Identifier::toString).toList());
                if (!netherSelection.isEmpty() && !runefusedSelection.isEmpty()) {
                    addUniqueConfiguration(cases, weapon, runefusedSelection.getFirst(),
                            netherSelection.getFirst(), awakening);
                }
                continue;
            }
            addUniqueConfiguration(cases, weapon, null, null, awakening);
            List<Identifier> netherSelection = profile.equals("smoke")
                    ? samplePowers(nether, seed, weaponId, "nether", 1)
                    : nether;
            sampledNether.put(weaponId.toString(), netherSelection.stream()
                    .map(Identifier::toString).toList());
            for (Identifier power : netherSelection) {
                addUniqueConfiguration(cases, weapon, null, power, awakening);
            }
            List<Identifier> runefusedSelection = samplePowers(
                    runefused, seed, weaponId, "runefused", selectedCount);
            sampledRunefused.put(weaponId.toString(), runefusedSelection.stream()
                    .map(Identifier::toString).toList());
            for (Identifier power : runefusedSelection) {
                addUniqueConfiguration(cases, weapon, power, null, awakening);
            }
        }
        for (Item weapon : runics) {
            Identifier weaponId = Registries.ITEM.getId(weapon);
            List<Identifier> runicSelection = samplePowers(
                    runic, seed, weaponId, "runic", selectedCount);
            sampledRunic.put(weaponId.toString(), runicSelection.stream()
                    .map(Identifier::toString).toList());
            for (Identifier power : runicSelection) {
                cases.add(Case.of("runic", weapon, power, null, "attack", false, 8));
                cases.add(Case.npc("runic_mob_player", weapon, power, null,
                        "attack", "mob", "player", 8));
                if (!profile.equals("smoke")) {
                    cases.add(Case.npc("runic_mob_golem", weapon, power, null,
                            "attack", "mob", "golem", 8));
                }
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        for (Case testCase : cases) {
            if (!ids.add(testCase.id)) throw new IllegalStateException("Duplicate QA case id " + testCase.id);
        }
        return new CasePlan(List.copyOf(cases), Map.copyOf(sampledRunefused),
                Map.copyOf(sampledRunic), Map.copyOf(sampledNether));
    }

    private static void addUniqueConfiguration(List<Case> cases, Item weapon, Identifier runefused,
                                               Identifier nether, int awakening) {
        cases.add(Case.of("combat", weapon, runefused, nether, "attack", false, awakening));
        cases.add(Case.of("ability", weapon, runefused, nether, "ability", false, awakening));
        cases.add(Case.of("friendly_fire", weapon, runefused, nether, "ability", true, awakening));
        addNpcCases(cases, weapon, runefused, nether, awakening);
    }

    private static List<Identifier> samplePowers(List<Identifier> pool, long seed, Identifier weapon,
                                                 String category, int count) {
        return pool.stream()
                .sorted(Comparator.<Identifier>comparingLong(power -> sampleRank(
                                seed, weapon, category, power))
                        .thenComparing(Identifier::toString))
                .limit(Math.min(count, pool.size()))
                .toList();
    }

    private static long sampleRank(long seed, Identifier weapon, String category, Identifier power) {
        long hash = 0xcbf29ce484222325L ^ seed;
        String value = weapon + "|" + category + "|" + power;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return hash ^ hash >>> 33;
    }

    private static void writeBootstrap(MinecraftServer server, long now) {
        JsonObject root = new JsonObject();
        root.addProperty("schema_version", 1);
        root.addProperty("protocol", QaNetwork.PROTOCOL);
        root.addProperty("updated_at", Instant.now().toString());
        JsonObject clients = new JsonObject();
        boolean ready = true;
        for (String name : List.of("SimplyTest1", "SimplyTest2")) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
            ClientState state = player == null ? null : CLIENTS.get(player.getUuidAsString());
            boolean fresh = state != null && now - state.lastHeartbeat <= 15_000_000_000L;
            boolean compatible = state != null && state.protocol == QaNetwork.PROTOCOL;
            JsonObject client = new JsonObject();
            client.addProperty("online", player != null);
            client.addProperty("fresh", fresh);
            client.addProperty("protocol", state == null ? -1 : state.protocol);
            clients.add(name, client);
            ready &= player != null && fresh && compatible;
        }
        root.add("clients", clients);
        root.addProperty("ready", ready);
        Path output = Path.of(System.getProperty("simplyswords.qa.output", "simplyswords-qa"));
        Path destination = output.resolve("qa-bootstrap.json");
        Path temporary = output.resolve("qa-bootstrap.json.tmp");
        try {
            Files.createDirectories(output);
            Files.writeString(temporary, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("SIMPLY_QA_BOOTSTRAP_ERROR: " + exception.getMessage());
        }
    }

    private static void addNpcCases(List<Case> cases, Item weapon, Identifier runic,
                                    Identifier nether, int awakening) {
        cases.add(Case.npc("player_vs_mob", weapon, runic, nether,
                "attack", "player", "mob", awakening));
        cases.add(Case.npc("mob_vs_player", weapon, runic, nether,
                "attack", "mob", "player", awakening));
        cases.add(Case.npc("mob_vs_golem", weapon, runic, nether,
                "attack", "mob", "golem", awakening));
        if (weapon instanceof UniqueWeaponActiveAbility) {
            cases.add(Case.npc("player_ability_mob", weapon, runic, nether,
                    "ability", "player", "mob", awakening));
            cases.add(Case.npc("mob_ability_player", weapon, runic, nether,
                    "ability", "mob", "player", awakening));
            cases.add(Case.npc("mob_ability_golem", weapon, runic, nether,
                    "ability", "mob", "golem", awakening));
        }
    }

    private static List<Identifier> powers(PowerType type) {
        return GemPowerRegistry.REGISTRY.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty() && entry.getValue().applicableTypes().contains(type))
                .map(entry -> entry.getKey().getValue()).sorted().toList();
    }

    private record ClientState(int protocol, long lastHeartbeat) {}

    private record AbilityTiming(boolean channelled, int holdTicks, int observationTicks) {
        private static final AbilityTiming INSTANT = new AbilityTiming(false, 1, 45);
    }

    private static final class ClientProgress {
        private boolean acknowledged;
        private boolean released;
        private String error;

        private void accept(QaNetwork.ClientStatus packet) {
            if (packet.kind.equals("ack")) acknowledged = true;
            if (packet.kind.equals("released")) released = true;
            if (packet.kind.equals("exception") || packet.kind.equals("error")) error = packet.detail;
        }
    }

    private record CasePlan(List<Case> cases, Map<String, List<String>> sampledRunefused,
                            Map<String, List<String>> sampledRunic,
                            Map<String, List<String>> sampledNether) {}

    private record Case(String id, String suite, Identifier weapon, Identifier runic, Identifier nether,
                        String action, boolean friendly, int awakeningLevel,
                        String actorKind, String targetKind) {
        static Case of(String suite, Item weapon, Identifier runic, Identifier nether,
                       String action, boolean friendly, int level) {
            Identifier weaponId = Registries.ITEM.getId(weapon);
            return npc(suite, weapon, runic, nether, action, "player", "player", level, friendly);
        }

        static Case npc(String suite, Item weapon, Identifier runic, Identifier nether,
                        String action, String actorKind, String targetKind, int level) {
            return npc(suite, weapon, runic, nether, action, actorKind, targetKind, level, false);
        }

        private static Case npc(String suite, Item weapon, Identifier runic, Identifier nether,
                                String action, String actorKind, String targetKind,
                                int level, boolean friendly) {
            Identifier weaponId = Registries.ITEM.getId(weapon);
            String id = String.join("|", suite, weaponId.toString(),
                    runic == null ? "empty" : runic.toString(), nether == null ? "empty" : nether.toString(),
                    action, actorKind + "_vs_" + targetKind, friendly ? "team" : "enemy", "awakening=" + level);
            return new Case(id, suite, weaponId, runic, nether, action, friendly, level,
                    actorKind, targetKind);
        }
    }

    private static final class Run {
        private final MinecraftServer server;
        private final String profile;
        private final long seed;
        private final int runicSamples;
        private final List<Case> cases;
        private final Path directory;
        private final BufferedWriter events;
        private final Map<String, ClientProgress> clientEvents = new HashMap<>();
        private final Map<String, String> originalTeams = new HashMap<>();
        private final Map<String, Float> originalAbsorption = new HashMap<>();
        private final Set<String> managedPlayers = new LinkedHashSet<>();
        private int index;
        private int phase;
        private int phaseTicks;
        private int caseAttempt;
        private int passed;
        private int failed;
        private int retries;
        private float targetEffectiveHealth;
        private boolean targetHarmed;
        private LivingEntity targetEntity;
        private ZombieEntity spawnedActor;
        private LivingEntity spawnedTarget;
        private boolean serverActionSucceeded;
        private boolean observedPlayerCooldown;
        private boolean observedChannel;
        private boolean automaticChannelCompletion;
        private ItemStack preparedStack = ItemStack.EMPTY;
        private AbilityTiming activeTiming = AbilityTiming.INSTANT;
        private String placementIssue = "not checked";
        private Case active;
        private boolean finished;
        private boolean preflightComplete;

        private Run(MinecraftServer server, String profile, long seed, int runicSamples,
                    CasePlan plan, int startIndex) throws IOException {
            this.server = server;
            this.profile = profile;
            this.seed = seed;
            this.runicSamples = runicSamples;
            this.cases = plan.cases;
            this.index = startIndex;
            snapshotPlayerTeams();
            String configured = System.getProperty("simplyswords.qa.output", "simplyswords-qa");
            this.directory = Path.of(configured).resolve("run-" + Instant.now().toEpochMilli());
            Files.createDirectories(directory);
            this.events = Files.newBufferedWriter(directory.resolve("qa-events.jsonl"), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            JsonObject manifest = new JsonObject();
            manifest.addProperty("schema_version", 1);
            manifest.addProperty("profile", profile);
            manifest.addProperty("seed", seed);
            manifest.addProperty("runic_sample_count", runicSamples);
            manifest.addProperty("effective_runic_sample_count", profile.equals("full") ? runicSamples : 1);
            manifest.addProperty("cases", cases.size());
            manifest.addProperty("start_index", startIndex);
            manifest.add("sampled_runefused", GSON.toJsonTree(plan.sampledRunefused));
            manifest.add("sampled_runic", GSON.toJsonTree(plan.sampledRunic));
            manifest.add("sampled_nether", GSON.toJsonTree(plan.sampledNether));
            Files.writeString(directory.resolve("qa-manifest.json"), GSON.toJson(manifest) + "\n", StandardCharsets.UTF_8);
        }

        private String activeId() { return active == null ? "" : active.id; }

        private void tick() {
            if (!preflightComplete) {
                preflightComplete = true;
                if (!runPreflightChecks()) {
                    finish("failed", "QA preflight checks failed");
                    return;
                }
            }
            if (index >= cases.size()) {
                finish(failed == 0 ? "passed" : "failed", "QA run completed");
                return;
            }
            if (active == null) {
                active = cases.get(index);
                phase = 0;
                phaseTicks = 0;
                caseAttempt = 1;
                event("case_started", active, "running", "Case started", null);
            }
            List<ServerPlayerEntity> players = requiredPlayers(server);
            if (players.size() != 2) {
                complete(false, "Required client disconnected");
                return;
            }
            ServerPlayerEntity actor = players.get(0);
            ServerPlayerEntity target = players.get(1);
            if (phase == 0) {
                prepare(actor, target, active);
                phase = 1;
                return;
            }
            if (phase == 1) {
                phaseTicks++;
                stabilizeParticipants(actor);
                faceParticipants(actor);
                int settleTicks = caseAttempt > 1 && active.action.equals("attack") ? 25 : 5;
                if (phaseTicks < settleTicks) return;
                if (!placementReady(actor)) {
                    if (phaseTicks >= Math.max(20, settleTicks + 15)) {
                        if (caseAttempt == 1) {
                            retryCase("Combatants did not reach stable ground with clear line of sight: "
                                    + placementIssue);
                        } else {
                            complete(false, "Placement remained unstable after retry: " + placementIssue);
                        }
                    }
                    return;
                }
                equipPreparedWeapon(actor);
                targetEntity.timeUntilRegen = 0;
                targetEffectiveHealth = effectiveHealth(targetEntity);
                targetHarmed = false;
                serverActionSucceeded = true;
                if (active.actorKind.equals("player")) {
                    new QaNetwork.ClientAction(active.id, active.action, targetEntity.getId(),
                            active.action.equals("ability") ? activeTiming.holdTicks : 1).sendTo(actor);
                } else {
                    serverActionSucceeded = active.action.equals("attack")
                            ? spawnedActor.tryAttack(targetEntity)
                            : SimplySwordsAPI.tryActivateWeaponAbility(WeaponAbilityContext.of(
                                    (ServerWorld) spawnedActor.getWorld(), spawnedActor.getMainHandStack(), spawnedActor,
                                    null, targetEntity, Hand.MAIN_HAND, WeaponAbilityActivationSource.MOB));
                }
                phase = 2;
                phaseTicks = 0;
                return;
            }
            phaseTicks++;
            observeDamageAndProtectPlayers();
            if (active.actorKind.equals("player") && active.action.equals("ability")
                    && isCoolingDown(actor, actor.getMainHandStack())) {
                observedPlayerCooldown = true;
            }
            ClientProgress client = clientEvents.get(active.id);
            if (activeTiming.channelled && active.actorKind.equals("player")) {
                boolean channelNow = PlayerWeaponAbilityChannelManager.isChanneling(
                        actor, Hand.MAIN_HAND, actor.getMainHandStack().getItem());
                observedChannel |= channelNow;
                if (observedChannel && !channelNow && (client == null || !client.released)) {
                    automaticChannelCompletion = true;
                }
            }
            if (client != null && client.error != null) {
                complete(false, client.error);
                return;
            }
            int observationTicks = activeTiming.observationTicks;
            if (phaseTicks < observationTicks) return;
            boolean locked = active.awakeningLevel < 4;
            boolean harmed = targetHarmed
                    || effectiveHealth(targetEntity) < targetEffectiveHealth - 0.001F;
            if (active.action.equals("attack") && !active.friendly
                    && (!harmed || (active.actorKind.equals("mob") && !serverActionSucceeded))
                    && caseAttempt == 1) {
                retryCase(!serverActionSucceeded
                        ? "Mob attack was rejected despite valid arena placement"
                        : "Controlled melee attack caused no damage");
                return;
            }
            if (active.actorKind.equals("player") && (client == null || !client.acknowledged)) {
                complete(false, "Client action acknowledgement timed out");
            }
            else if (activeTiming.channelled && active.actorKind.equals("player")
                    && !automaticChannelCompletion && (client == null || !client.released)) {
                complete(false, "Channeled ability did not release or complete automatically");
            }
            else if (active.actorKind.equals("mob") && !serverActionSucceeded) complete(false, "Mob action was rejected by the production API");
            else if (active.friendly && harmed) complete(false, "Friendly target was harmed");
            else if (locked && active.action.equals("ability") && harmed) complete(false, "Ability harmed target below awakening level 4");
            else if (locked && active.action.equals("ability") && observedPlayerCooldown) complete(false, "Ability entered cooldown below awakening level 4");
            else if (!locked && !active.friendly && active.actorKind.equals("player")
                    && active.action.equals("ability") && !observedPlayerCooldown) {
                complete(false, "Unlocked player ability did not enter cooldown");
            }
            else if (active.action.equals("attack") && !active.friendly && !harmed) complete(false, "Enemy target did not take melee damage");
            else complete(true, "Observed expected invariant");
        }

        private boolean runPreflightChecks() {
            boolean valid = checkArenaReset();
            valid &= checkTeamTransitions();
            if (profile.equals("startup")) return valid;
            valid &= check("registry.weapons", cases.stream().anyMatch(value -> value.weapon != null),
                    "At least one unique/runic weapon was enumerated");
            valid &= check("registry.runefused_powers", !powers(PowerType.RUNEFUSED).isEmpty(),
                    "Runefused power registry is populated");
            valid &= check("registry.nether_powers", !powers(PowerType.NETHER).isEmpty(),
                    "Nether power registry is populated");
            valid &= check("config.loot_pity_order",
                    LootConfig.INSTANCE.uniqueHardPity.get() > LootConfig.INSTANCE.uniqueSoftPityStart.get(),
                    "Unique hard pity is greater than soft pity start");
            valid &= check("config.spell_scaling",
                    Config.compatibility.spellScalingDiminishingReturnsStart.get() >= 1.0F
                            && Config.compatibility.spellScalingDiminishingReturnsStrength.get() >= 0.0F,
                    "Spell scaling limits are valid");

            if (LootConfig.INSTANCE.enableLootDrops.get()
                    && LootConfig.INSTANCE.uniqueLootTableWeight.get() > 0.0F) {
                PlayerPityState state = new PlayerPityState();
                state.setUniqueMisses(LootConfig.INSTANCE.uniqueHardPity.get() - 1);
                RegistryKey<net.minecraft.loot.LootTable> table = RegistryKey.of(
                        RegistryKeys.LOOT_TABLE, Identifier.ofVanilla("chests/simple_dungeon"));
                List<ItemStack> generated = PityLootManager.simulateGeneratedContainerLoot(
                        table, state, server.getOverworld().getRandom(),
                        PityLootManager.SimulationMode.PITY_PROGRESSION);
                boolean foundUnique = generated.stream()
                        .anyMatch(stack -> PityLootManager.isRegisteredUniqueLootItem(stack.getItem()));
                valid &= check("loot.unique_hard_pity", foundUnique && state.uniqueMisses() == 0,
                        "Hard pity produces a registered unique and resets its counter");
            } else {
                event("check_skipped", null, "skipped",
                        "loot.unique_hard_pity: unique chest loot disabled", null);
            }
            return valid;
        }

        private boolean checkArenaReset() {
            ServerWorld world = server.getOverworld();
            buildArena(world);
            BlockPos obstruction = new BlockPos(0, ARENA_FEET_Y + 1, 0);
            world.setBlockState(obstruction, Blocks.STONE.getDefaultState(), 3);
            ZombieEntity stray = EntityType.ZOMBIE.create(world);
            if (stray != null) {
                stray.refreshPositionAndAngles(3.5, ARENA_FEET_Y, 0.5, 0, 0);
                world.spawnEntity(stray);
            }
            resetArena(world);
            boolean floorReady = world.getBlockState(new BlockPos(0, ARENA_FLOOR_Y, 0)).isOf(Blocks.BEDROCK);
            boolean laneClear = world.getBlockState(obstruction).isAir();
            boolean entitiesClear = world.getOtherEntities(null, ARENA_BOX).stream()
                    .noneMatch(entity -> !(entity instanceof PlayerEntity));
            return check("arena.reset", floorReady && laneClear && entitiesClear,
                    "Disposable arena repairs terrain and removes stray entities");
        }

        private boolean checkTeamTransitions() {
            List<ServerPlayerEntity> players = requiredPlayers(server);
            if (players.size() != 2) {
                return check("teams.relationship_transitions", false,
                        "Both QA players are required for team setup");
            }
            ServerPlayerEntity actor = players.get(0);
            ServerPlayerEntity target = players.get(1);
            configurePlayerTeams(actor, target, false);
            Team qaTeam = server.getScoreboard().getTeam(QA_TEAM_NAME);
            boolean initialEnemy = server.getScoreboard().getScoreHolderTeam(
                    target.getNameForScoreboard()) != qaTeam;
            configurePlayerTeams(actor, target, true);
            boolean friendly = server.getScoreboard().getScoreHolderTeam(
                    target.getNameForScoreboard()) == qaTeam && !qaTeam.isFriendlyFireAllowed();
            configurePlayerTeams(actor, target, false);
            boolean enemyAgain = server.getScoreboard().getScoreHolderTeam(
                    target.getNameForScoreboard()) != qaTeam;
            return check("teams.relationship_transitions", initialEnemy && friendly && enemyAgain,
                    "Enemy and friendly team transitions are idempotent");
        }

        private boolean check(String id, boolean success, String detail) {
            event("check_finished", null, success ? "passed" : "failed", id + ": " + detail, null);
            return success;
        }

        private void prepare(ServerPlayerEntity actor, ServerPlayerEntity target, Case testCase) {
            ServerWorld world = server.getOverworld();
            clearDelayedAbilityState(world);
            actor.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            discardSpawnedEntities();
            resetArena(world);
            actor.changeGameMode(testCase.actorKind.equals("player") ? GameMode.SURVIVAL : GameMode.SPECTATOR);
            target.changeGameMode(testCase.targetKind.equals("player") ? GameMode.SURVIVAL : GameMode.SPECTATOR);
            actor.clearStatusEffects();
            target.clearStatusEffects();
            actor.setHealth(actor.getMaxHealth());
            target.setHealth(target.getMaxHealth());
            actor.setAbsorptionAmount(QA_ABSORPTION);
            target.setAbsorptionAmount(QA_ABSORPTION);
            actor.setNoGravity(false);
            target.setNoGravity(false);
            actor.setVelocity(Vec3d.ZERO);
            target.setVelocity(Vec3d.ZERO);
            actor.fallDistance = 0;
            target.fallDistance = 0;
            actor.teleport(world,
                    testCase.actorKind.equals("player") ? ACTOR_X : 20.5,
                    ARENA_FEET_Y,
                    testCase.actorKind.equals("player") ? ACTOR_Z : -3.0,
                    0, 0);
            target.teleport(world,
                    testCase.targetKind.equals("player") ? TARGET_X : 20.5,
                    ARENA_FEET_Y,
                    testCase.targetKind.equals("player") ? TARGET_Z : 3.0,
                    180, 0);
            configurePlayerTeams(actor, target, testCase.friendly);

            ItemStack stack = new ItemStack(Registries.ITEM.get(testCase.weapon));
            AwakeningApi.setLevel(stack, testCase.awakeningLevel);
            stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.create(testCase.runic, testCase.nether));
            preparedStack = stack;
            activeTiming = abilityTiming(testCase, actor, stack);
            clearCooldown(actor, stack);
            if (testCase.targetKind.equals("mob")) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(world);
                if (zombie == null) throw new IllegalStateException("Could not create zombie QA target");
                zombie.refreshPositionAndAngles(TARGET_X, ARENA_FEET_Y, TARGET_Z, 180, 0);
                zombie.setAiDisabled(true);
                zombie.setPersistent();
                zombie.setVelocity(Vec3d.ZERO);
                world.spawnEntity(zombie);
                spawnedTarget = zombie;
                targetEntity = zombie;
            } else if (testCase.targetKind.equals("golem")) {
                IronGolemEntity golem = EntityType.IRON_GOLEM.create(world);
                if (golem == null) throw new IllegalStateException("Could not create iron golem QA target");
                golem.refreshPositionAndAngles(TARGET_X, ARENA_FEET_Y, TARGET_Z, 180, 0);
                golem.setAiDisabled(true);
                golem.setPersistent();
                golem.setVelocity(Vec3d.ZERO);
                world.spawnEntity(golem);
                spawnedTarget = golem;
                targetEntity = golem;
            } else {
                targetEntity = target;
            }
            if (testCase.actorKind.equals("mob")) {
                ZombieEntity zombie = EntityType.ZOMBIE.create(world);
                if (zombie == null) throw new IllegalStateException("Could not create zombie QA actor");
                zombie.refreshPositionAndAngles(ACTOR_X, ARENA_FEET_Y, ACTOR_Z, 0, 0);
                zombie.setAiDisabled(true);
                zombie.setPersistent();
                zombie.setVelocity(Vec3d.ZERO);
                zombie.setAbsorptionAmount(QA_ABSORPTION);
                zombie.setTarget(targetEntity);
                world.spawnEntity(zombie);
                spawnedActor = zombie;
                clearCooldown(zombie, stack);
            }
            targetEntity.setAbsorptionAmount(QA_ABSORPTION);
            targetEntity.setNoGravity(false);
            targetEntity.timeUntilRegen = 0;
            serverActionSucceeded = true;
            targetHarmed = false;
            observedPlayerCooldown = false;
            observedChannel = false;
            automaticChannelCompletion = false;
        }

        private AbilityTiming abilityTiming(Case testCase, ServerPlayerEntity actor, ItemStack stack) {
            int effectTicks = effectObservationTicks(testCase);
            if (!testCase.action.equals("ability") || testCase.awakeningLevel < 4
                    || !testCase.actorKind.equals("player")) {
                return new AbilityTiming(false, 1, effectTicks);
            }
            int maxUseTicks = Math.max(0, stack.getMaxUseTime(actor));
            if (maxUseTicks == 0) {
                return new AbilityTiming(false, 1, effectTicks);
            }
            String path = testCase.weapon.getPath();
            int holdTicks;
            if (path.equals("dawnquiver")) {
                holdTicks = Math.max(1, Config.uniqueEffects.dawnquiver.drawDuration);
            } else if (path.endsWith("lichblade")) {
                holdTicks = Math.max(1, Config.uniqueEffects.lichblade.duration + 5);
            } else {
                holdTicks = maxUseTicks;
            }
            int observationTicks = path.equals("dawnquiver")
                    ? holdTicks + effectTicks
                    : Math.max(effectTicks, holdTicks + 45);
            return new AbilityTiming(true, holdTicks, observationTicks);
        }

        private int effectObservationTicks(Case testCase) {
            if (!testCase.action.equals("ability") || testCase.awakeningLevel < 4) return 45;
            return switch (testCase.weapon.getPath()) {
                case "arcanethyst" -> Math.max(45, Math.max(
                        Config.uniqueEffects.arcanethyst.duration,
                        Config.uniqueEffects.arcanethyst.liftTicks
                                + Config.uniqueEffects.arcanethyst.suspendTicks
                                + Config.uniqueEffects.arcanethyst.slamTicks) + 10);
                case "bramblethorn" -> Math.max(45,
                        Config.uniqueEffects.bramblethorn.rootTravelTicks
                                + Config.uniqueEffects.bramblethorn.bindingDuration + 8 + 18 + 10);
                case "brimstone_claymore" -> Math.max(45,
                        Config.uniqueEffects.brimstone_claymore.duration + 18 + 10);
                case "gloampiercer" -> Math.max(45,
                        Config.uniqueEffects.gloampiercer.channelDuration + 10);
                case "shadowsting" -> testCase.actorKind.equals("player")
                        ? Math.max(45, Config.uniqueEffects.shadowsting.duration / 2 + 18) : 45;
                case "stars_edge" -> Math.max(45,
                        (int) Math.ceil(Config.uniqueEffects.stars_edge.initialDashDistance
                                / Math.max(0.1, Config.uniqueEffects.stars_edge.initialDashSpeed))
                                + Config.uniqueEffects.stars_edge.recordingDuration
                                + Config.uniqueEffects.stars_edge.constellationDuration
                                + Math.max(1, Config.uniqueEffects.stars_edge.maxNodes - 1)
                                * Config.uniqueEffects.stars_edge.segmentExplosionInterval + 20);
                case "magispear" -> 55;
                case "dawnquiver" -> Math.max(45,
                        Config.uniqueEffects.dawnquiver.convergenceFormationDelay
                                + Config.uniqueEffects.dawnquiver.maxChorus
                                * Config.uniqueEffects.dawnquiver.convergenceFiringStagger + 30);
                case "icewhisper" -> Math.max(45,
                        Config.uniqueEffects.icewhisper.duration
                                + Config.uniqueEffects.icewhisper.cometFallTicks + 20);
                default -> 45;
            };
        }

        private float effectiveHealth(LivingEntity entity) {
            return entity.getHealth() + entity.getAbsorptionAmount();
        }

        private void observeDamageAndProtectPlayers() {
            if (targetEntity != null
                    && effectiveHealth(targetEntity) < targetEffectiveHealth - 0.001F) {
                targetHarmed = true;
            }
            for (ServerPlayerEntity player : requiredPlayers(server)) {
                if (!player.isAlive()) continue;
                player.setHealth(player.getMaxHealth());
                player.setAbsorptionAmount(QA_ABSORPTION);
                player.timeUntilRegen = 0;
            }
        }

        private boolean isCoolingDown(LivingEntity actor, ItemStack stack) {
            if (actor instanceof PlayerEntity player) {
                return player.getItemCooldownManager().isCoolingDown(stack.getItem());
            }
            return actor.getWorld() instanceof ServerWorld world
                    && WeaponAbilityCooldownManager.isCoolingDown(world, actor, stack);
        }

        private void clearCooldown(LivingEntity actor, ItemStack stack) {
            if (actor instanceof PlayerEntity player) {
                player.getItemCooldownManager().set(stack.getItem(), 0);
            }
            WeaponAbilityCooldownManager.clearCooldown(actor, stack);
        }

        private void equipPreparedWeapon(ServerPlayerEntity actor) {
            LivingEntity actionActor = active.actorKind.equals("player") ? actor : spawnedActor;
            if (actionActor == null || preparedStack.isEmpty()) return;
            actionActor.equipStack(EquipmentSlot.MAINHAND, preparedStack);
            clearCooldown(actionActor, preparedStack);
        }

        private void clearDelayedAbilityState(ServerWorld world) {
            for (ServerPlayerEntity player : requiredPlayers(server)) {
                PlayerWeaponAbilityManager.handleInput(player, Hand.MAIN_HAND, false);
                player.setNoGravity(false);
                player.setVelocity(Vec3d.ZERO);
                player.fallDistance = 0;
            }
        }

        private void buildArena(ServerWorld world) {
            world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, server);
            world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false, server);
            world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
            world.setTimeOfDay(6000);
            world.setWeather(6000, 0, false, false);
            for (int x = -16; x <= 16; x++) {
                for (int z = -16; z <= 16; z++) {
                    world.setBlockState(new BlockPos(x, ARENA_FLOOR_Y, z), Blocks.BEDROCK.getDefaultState(), 3);
                    for (int y = ARENA_FEET_Y; y <= ARENA_FLOOR_Y + 16; y++) {
                        world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 3);
                    }
                }
            }
            removeArenaEntities(world);
        }

        private void resetArena(ServerWorld world) {
            removeArenaEntities(world);
            for (int x = -4; x <= 4; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos floor = new BlockPos(x, ARENA_FLOOR_Y, z);
                    if (!world.getBlockState(floor).isOf(Blocks.BEDROCK)) {
                        world.setBlockState(floor, Blocks.BEDROCK.getDefaultState(), 3);
                    }
                    for (int y = ARENA_FEET_Y; y <= ARENA_FEET_Y + 8; y++) {
                        BlockPos position = new BlockPos(x, y, z);
                        if (!world.getBlockState(position).isAir()) {
                            world.setBlockState(position, Blocks.AIR.getDefaultState(), 3);
                        }
                    }
                }
            }
        }

        private void removeArenaEntities(ServerWorld world) {
            List<Entity> entities = new ArrayList<>(world.getOtherEntities(null, ARENA_BOX));
            entities.stream().filter(entity -> !(entity instanceof PlayerEntity)).forEach(Entity::discard);
        }

        private void stabilizeParticipants(ServerPlayerEntity actor) {
            LivingEntity actionActor = active.actorKind.equals("player") ? actor : spawnedActor;
            reanchorIfMoving(actionActor, ACTOR_X, ACTOR_Z, 0.0F);
            reanchorIfMoving(targetEntity, TARGET_X, TARGET_Z, 180.0F);
        }

        private void reanchorIfMoving(LivingEntity entity, double x, double z, float yaw) {
            if (entity == null) return;
            Vec3d velocity = entity.getVelocity();
            double horizontalVelocity = velocity.x * velocity.x + velocity.z * velocity.z;
            double horizontalOffset = (entity.getX() - x) * (entity.getX() - x)
                    + (entity.getZ() - z) * (entity.getZ() - z);
            if (horizontalVelocity <= 0.0025 && horizontalOffset <= 0.01
                    && Math.abs(entity.getY() - ARENA_FEET_Y) <= 0.05) {
                return;
            }
            if (entity instanceof ServerPlayerEntity player) {
                player.teleport(server.getOverworld(), x, ARENA_FEET_Y, z, yaw, 0.0F);
            } else {
                entity.refreshPositionAndAngles(x, ARENA_FEET_Y, z, yaw, 0.0F);
            }
            entity.setVelocity(Vec3d.ZERO);
            entity.fallDistance = 0;
        }

        private void faceParticipants(ServerPlayerEntity actor) {
            LivingEntity actionActor = active.actorKind.equals("player") ? actor : spawnedActor;
            if (actionActor == null || targetEntity == null) return;
            actionActor.setYaw(0);
            actionActor.setPitch(0);
            targetEntity.setYaw(180);
            targetEntity.setPitch(0);
        }

        private boolean placementReady(ServerPlayerEntity actor) {
            LivingEntity actionActor = active.actorKind.equals("player") ? actor : spawnedActor;
            if (actionActor == null || targetEntity == null || !actionActor.isAlive() || !targetEntity.isAlive()) {
                placementIssue = "participant missing or not alive";
                return false;
            }
            if (actionActor.getWorld() != targetEntity.getWorld() || actionActor.getWorld() != server.getOverworld()) {
                placementIssue = "participants are not in the disposable overworld";
                return false;
            }
            if (!arenaGrounded(actionActor) || !arenaGrounded(targetEntity)) {
                placementIssue = "grounded actor=" + arenaGrounded(actionActor)
                        + " target=" + arenaGrounded(targetEntity)
                        + " flags=" + actionActor.isOnGround() + "/" + targetEntity.isOnGround()
                        + " y=" + actionActor.getY() + "/" + targetEntity.getY();
                return false;
            }
            Vec3d actorVelocity = actionActor.getVelocity();
            Vec3d targetVelocity = targetEntity.getVelocity();
            if (actorVelocity.x * actorVelocity.x + actorVelocity.z * actorVelocity.z > 0.0025
                    || targetVelocity.x * targetVelocity.x + targetVelocity.z * targetVelocity.z > 0.0025) {
                placementIssue = "velocity actor=" + actionActor.getVelocity()
                        + " target=" + targetEntity.getVelocity();
                return false;
            }
            if (!server.getOverworld().getBlockState(actionActor.getBlockPos().down()).isOf(Blocks.BEDROCK)
                    || !server.getOverworld().getBlockState(targetEntity.getBlockPos().down()).isOf(Blocks.BEDROCK)) {
                placementIssue = "support actor=" + actionActor.getBlockPos().down()
                        + " target=" + targetEntity.getBlockPos().down();
                return false;
            }
            double distance = actionActor.squaredDistanceTo(targetEntity);
            if (distance < 2.25 || distance > 9.0 || Math.abs(actionActor.getY() - targetEntity.getY()) > 0.25) {
                placementIssue = "distanceSquared=" + distance + " verticalDelta="
                        + Math.abs(actionActor.getY() - targetEntity.getY());
                return false;
            }
            HitResult obstruction = server.getOverworld().raycast(new RaycastContext(
                    actionActor.getEyePos(), targetEntity.getEyePos(),
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, actionActor));
            if (obstruction.getType() != HitResult.Type.MISS) {
                placementIssue = "block ray hit " + obstruction.getPos();
                return false;
            }
            placementIssue = "ready";
            return true;
        }

        private boolean arenaGrounded(LivingEntity entity) {
            boolean supported = Math.abs(entity.getY() - ARENA_FEET_Y) <= 0.05
                    && server.getOverworld().getBlockState(entity.getBlockPos().down()).isOf(Blocks.BEDROCK);
            return supported && (!(entity instanceof PlayerEntity) || entity.isOnGround());
        }

        private void retryCase(String reason) {
            retries++;
            caseAttempt++;
            event("case_retry", active, "retrying", reason,
                    Map.of("next_attempt", caseAttempt, "max_attempts", 2));
            clientEvents.remove(active.id);
            clearDelayedAbilityState(server.getOverworld());
            discardSpawnedEntities();
            phase = 0;
            phaseTicks = 0;
        }

        private void discardSpawnedEntities() {
            if (spawnedActor != null) spawnedActor.discard();
            if (spawnedTarget != null) spawnedTarget.discard();
            spawnedActor = null;
            spawnedTarget = null;
            targetEntity = null;
        }

        private void snapshotPlayerTeams() {
            for (ServerPlayerEntity player : requiredPlayers(server)) {
                String name = player.getNameForScoreboard();
                managedPlayers.add(name);
                originalAbsorption.put(name, player.getAbsorptionAmount());
                Team team = server.getScoreboard().getScoreHolderTeam(name);
                if (team != null && !QA_TEAM_NAME.equals(team.getName())) {
                    originalTeams.put(name, team.getName());
                }
            }
        }

        private void configurePlayerTeams(ServerPlayerEntity actor, ServerPlayerEntity target, boolean friendly) {
            Team team = server.getScoreboard().getTeam(QA_TEAM_NAME);
            if (team == null) team = server.getScoreboard().addTeam(QA_TEAM_NAME);
            team.setFriendlyFireAllowed(!friendly);
            server.getScoreboard().addScoreHolderToTeam(actor.getNameForScoreboard(), team);
            String targetName = target.getNameForScoreboard();
            if (friendly) {
                server.getScoreboard().addScoreHolderToTeam(targetName, team);
            } else if (server.getScoreboard().getScoreHolderTeam(targetName) == team) {
                server.getScoreboard().clearTeam(targetName);
            }
        }

        private void restorePlayerTeams() {
            Team qaTeam = server.getScoreboard().getTeam(QA_TEAM_NAME);
            for (String name : managedPlayers) {
                if (qaTeam != null && server.getScoreboard().getScoreHolderTeam(name) == qaTeam) {
                    server.getScoreboard().clearTeam(name);
                }
                String originalName = originalTeams.get(name);
                Team original = originalName == null ? null : server.getScoreboard().getTeam(originalName);
                if (original != null && server.getScoreboard().getScoreHolderTeam(name) == null) {
                    server.getScoreboard().addScoreHolderToTeam(name, original);
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
                if (player != null) {
                    player.setAbsorptionAmount(originalAbsorption.getOrDefault(name, 0.0F));
                    player.setNoGravity(false);
                    player.setVelocity(Vec3d.ZERO);
                }
            }
        }

        private void complete(boolean success, String detail) {
            if (success) passed++; else failed++;
            ClientProgress progress = active == null ? null : clientEvents.get(active.id);
            Map<String, Object> lifecycle = new HashMap<>();
            lifecycle.put("observation_ticks", activeTiming.observationTicks);
            lifecycle.put("elapsed_observation_ticks", phaseTicks);
            lifecycle.put("channelled", activeTiming.channelled);
            lifecycle.put("hold_ticks", activeTiming.holdTicks);
            lifecycle.put("channel_started", observedChannel);
            lifecycle.put("release_observed", progress != null && progress.released);
            lifecycle.put("automatic_completion", automaticChannelCompletion);
            lifecycle.put("cooldown_observed", observedPlayerCooldown);
            event("case_finished", active, success ? "passed" : "failed", detail,
                    lifecycle);
            clearDelayedAbilityState(server.getOverworld());
            active = null;
            index++;
            phase = 0;
            phaseTicks = 0;
            discardSpawnedEntities();
            preparedStack = ItemStack.EMPTY;
            activeTiming = AbilityTiming.INSTANT;
        }

        private void finish(String result, String detail) {
            if (finished) return;
            clearDelayedAbilityState(server.getOverworld());
            discardSpawnedEntities();
            restorePlayerTeams();
            event("run_finished", active, result, detail,
                    Map.of("passed", passed, "failed", failed, "retries", retries,
                            "total", cases.size(), "completed", index));
            try { events.close(); } catch (IOException ignored) {}
            JsonObject summary = new JsonObject();
            summary.addProperty("schema_version", 1);
            summary.addProperty("result", result);
            summary.addProperty("profile", profile);
            summary.addProperty("seed", seed);
            summary.addProperty("runic_sample_count", runicSamples);
            summary.addProperty("passed", passed);
            summary.addProperty("failed", failed);
            summary.addProperty("retries", retries);
            summary.addProperty("total", cases.size());
            summary.addProperty("completed", index);
            summary.addProperty("start_index", cases.isEmpty() ? 0 : Math.max(0, index - passed - failed));
            summary.addProperty("detail", detail);
            if (active != null) summary.addProperty("active_case_id", active.id);
            try { Files.writeString(directory.resolve("qa-run.json"), GSON.toJson(summary) + "\n", StandardCharsets.UTF_8); }
            catch (IOException ignored) {}
            finished = true;
        }

        private void event(String type, Case testCase, String status, String detail, Map<String, ?> extra) {
            JsonObject object = new JsonObject();
            object.addProperty("schema_version", 1);
            object.addProperty("timestamp", Instant.now().toString());
            object.addProperty("type", type);
            object.addProperty("status", status);
            object.addProperty("profile", profile);
            object.addProperty("index", index);
            object.addProperty("total", cases.size());
            if (testCase != null) {
                object.addProperty("case_id", testCase.id);
                object.addProperty("suite", testCase.suite);
                object.addProperty("weapon", testCase.weapon.toString());
                object.addProperty("runic", testCase.runic == null ? "" : testCase.runic.toString());
                object.addProperty("nether", testCase.nether == null ? "" : testCase.nether.toString());
                object.addProperty("action", testCase.action);
                object.addProperty("friendly", testCase.friendly);
                object.addProperty("awakening", testCase.awakeningLevel);
                object.addProperty("actor_kind", testCase.actorKind);
                object.addProperty("target_kind", testCase.targetKind);
                object.addProperty("case_attempt", Math.max(1, caseAttempt));
            }
            object.addProperty("detail", detail);
            if (extra != null) object.add("extra", GSON.toJsonTree(extra));
            try {
                events.write(GSON.toJson(object));
                events.newLine();
                events.flush();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not write QA event", exception);
            }
            System.out.println("SIMPLY_QA " + GSON.toJson(object));
        }
    }
}
