package net.sweenus.simplyswords.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerLootComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.loot.PityLootManager;
import net.sweenus.simplyswords.loot.PityStateHolder;
import net.sweenus.simplyswords.loot.PlayerPityState;
import net.sweenus.simplyswords.power.GemPower;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.Styles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class SimplySwordsCommands {

    private static final DynamicCommandExceptionType UNKNOWN_POWER = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown Simply Swords gem power: " + value));
    private static final DynamicCommandExceptionType WRONG_POWER_TYPE = new DynamicCommandExceptionType(value ->
            Text.literal("Gem power cannot be applied to this item type: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_RUNIC_WEAPON = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown Simply Swords runic weapon: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_UNIQUE_WEAPON = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown Simply Swords unique weapon: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_LOOT_TABLE = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown loot table: " + value));
    private static final DynamicCommandExceptionType UNSUPPORTED_LOOT_TABLE = new DynamicCommandExceptionType(value ->
            Text.literal("Loot testing only supports chest and entity tables: " + value));
    private static final DynamicCommandExceptionType CHEST_LOOT_TABLE_REQUIRED = new DynamicCommandExceptionType(value ->
            Text.literal("A chest-context loot table is required: " + value));
    private static final DynamicCommandExceptionType ENTITY_LOOT_SOURCE_REQUIRED = new DynamicCommandExceptionType(value ->
            Text.literal("Testing an entity loot table requires an entity command source: " + value));
    private static final DynamicCommandExceptionType EMPTY_HAND = new DynamicCommandExceptionType(value ->
            Text.literal("Hold the item you want to test in your main hand: " + value));

    private static List<Item> cachedUniqueWeapons;
    private static final float RANDOM_POWER_CHANCE = 0.5F;
    private static final int MIN_LOOT_TEST_ROLLS = 1;
    private static final int MAX_LOOT_TEST_ROLLS = 200_000;
    private static final int DEFAULT_LOOT_TEST_CHESTS = 16;
    private static final int MAX_LOOT_TEST_CHESTS = 64;
    private static final int MAX_PITY_MISSES = 100_000;
    private static final List<EntityType<? extends MobEntity>> HOSTILE_MOBS = List.of(
            EntityType.HUSK,
            EntityType.VINDICATOR,
            EntityType.PIGLIN
    );

    private SimplySwordsCommands() {
    }

    public static void register() {
        CommandRegistrationEvent.EVENT.register(SimplySwordsCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        var weaponArgument = configureSpawnArguments(CommandManager.argument("weapon", IdentifierArgumentType.identifier())
                .suggests((context, builder) -> suggestUniqueWeapons(builder)));
        var countedWeaponArgument = configureSpawnArguments(CommandManager.argument("weapon", IdentifierArgumentType.identifier())
                .suggests((context, builder) -> suggestUniqueWeapons(builder)));
        var countArgument = configureSpawnArguments(CommandManager.argument("count", IntegerArgumentType.integer(1, 50)))
                .then(countedWeaponArgument);
        var spawnHostileCommand = CommandManager.literal("spawn_hostile")
                .executes(SimplySwordsCommands::spawnHostile)
                .then(countArgument)
                .then(weaponArgument);
        var lootTestCommand = CommandManager.literal("loot_test")
                .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                        .suggests(SimplySwordsCommands::suggestLootTables)
                        .then(CommandManager.argument(
                                        "rolls",
                                        IntegerArgumentType.integer(
                                                MIN_LOOT_TEST_ROLLS,
                                                MAX_LOOT_TEST_ROLLS
                                        ))
                                .executes(SimplySwordsCommands::runLootTest)));
        var lootTestChestCommand = CommandManager.literal("loot_test_chest")
                .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                        .suggests(SimplySwordsCommands::suggestChestLootTables)
                        .executes(context -> giveLootTestChests(
                                context, DEFAULT_LOOT_TEST_CHESTS))
                        .then(CommandManager.argument(
                                        "count",
                                        IntegerArgumentType.integer(1, MAX_LOOT_TEST_CHESTS))
                                .executes(context -> giveLootTestChests(
                                        context,
                                        IntegerArgumentType.getInteger(context, "count")))));
        var componentTestCommand = CommandManager.literal("component_test")
                .executes(SimplySwordsCommands::runComponentTest);
        var pityStatusCommand = CommandManager.literal("status")
                .executes(SimplySwordsCommands::showPityStatus)
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(SimplySwordsCommands::showPityStatus));
        var pityIgnoreRegionsCommand = CommandManager.literal("ignore_regions")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(SimplySwordsCommands::setPityRegionBypass)
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(SimplySwordsCommands::setPityRegionBypass)));
        var pitySetCommand = CommandManager.literal("set")
                .then(CommandManager.literal("unique")
                        .then(CommandManager.argument(
                                        "misses",
                                        IntegerArgumentType.integer(0, MAX_PITY_MISSES))
                                .executes(context -> setPityMisses(context, PityTrack.UNIQUE))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> setPityMisses(context, PityTrack.UNIQUE)))))
                .then(CommandManager.literal("tablet")
                        .then(CommandManager.argument(
                                        "misses",
                                        IntegerArgumentType.integer(0, MAX_PITY_MISSES))
                                .executes(context -> setPityMisses(context, PityTrack.TABLET))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> setPityMisses(context, PityTrack.TABLET)))));
        var pityResetCommand = CommandManager.literal("reset")
                .then(CommandManager.literal("unique")
                        .executes(context -> resetPity(context, PityTrack.UNIQUE))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> resetPity(context, PityTrack.UNIQUE))))
                .then(CommandManager.literal("tablet")
                        .executes(context -> resetPity(context, PityTrack.TABLET))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> resetPity(context, PityTrack.TABLET))))
                .then(CommandManager.literal("all")
                        .executes(context -> resetPity(context, PityTrack.ALL))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> resetPity(context, PityTrack.ALL))));
        var pityCommand = CommandManager.literal("pity")
                .then(pityStatusCommand)
                .then(pityIgnoreRegionsCommand)
                .then(pitySetCommand)
                .then(pityResetCommand);

        dispatcher.register(CommandManager.literal("simplyswords")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("power")
                        .then(CommandManager.literal("runefused_gem")
                                .then(CommandManager.argument("power", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> suggestPowers(builder, PowerType.RUNEFUSED))
                                        .executes(context -> givePoweredGem(context, PowerType.RUNEFUSED))))
                        .then(CommandManager.literal("netherfused_gem")
                                .then(CommandManager.argument("power", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> suggestPowers(builder, PowerType.NETHER))
                                        .executes(context -> givePoweredGem(context, PowerType.NETHER))))
                        .then(CommandManager.literal("runic_weapon")
                                .then(CommandManager.argument("weapon", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> suggestRunicWeapons(builder))
                                        .then(CommandManager.argument("power", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> suggestPowers(builder, PowerType.RUNIC))
                                                .executes(SimplySwordsCommands::givePoweredRunicWeapon)))))
                .then(spawnHostileCommand)
                .then(lootTestCommand)
                .then(lootTestChestCommand)
                .then(componentTestCommand)
                .then(pityCommand));
    }

    //
    // Encodes the held stack and every Simply Swords component on it, decodes the result,
    // and reports whether the round trip produced something equal to what we started with.
    //
    // This is the exact comparison storage mods make: Refined Storage rebuilds a stack from
    // its item id and component map and matches it against what it has in storage. A
    // component whose equals()/hashCode() is wrong makes that match fail, and the player
    // gets a fuzzy-matched substitute instead of the item they asked for.
    //
    private static int runComponentTest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            throw EMPTY_HAND.create("component_test");
        }

        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, source.getRegistryManager());
        List<Text> lines = new ArrayList<>();
        boolean allPassed = true;

        for (Component<?> component : stack.getComponents()) {
            Identifier typeId = Registries.DATA_COMPONENT_TYPE.getId(component.type());
            if (typeId == null || !typeId.getNamespace().equals(SimplySwords.MOD_ID)) continue;
            ComponentResult result = roundTripComponent(component, ops);
            allPassed &= result.passed();
            lines.add(Text.literal(" " + (result.passed() ? "✔ " : "✘ ") + typeId + " — " + result.detail())
                    .formatted(result.passed() ? Formatting.GREEN : Formatting.RED));
        }

        if (lines.isEmpty()) {
            lines.add(Text.literal(" (no Simply Swords components on this stack)").formatted(Formatting.GRAY));
        }

        boolean stackPassed = roundTripStack(stack, ops);
        allPassed &= stackPassed;
        lines.add(Text.literal(" " + (stackPassed ? "✔ " : "✘ ") + "whole stack (ItemStack.CODEC round trip)")
                .formatted(stackPassed ? Formatting.GREEN : Formatting.RED));

        final boolean passed = allPassed;
        final Text header = Text.literal("Component round-trip test: " + stack.getName().getString())
                .formatted(passed ? Formatting.GREEN : Formatting.RED);
        source.sendFeedback(() -> header, false);
        for (Text line : lines) {
            source.sendFeedback(() -> line, false);
        }
        return passed ? 1 : 0;
    }

    private record ComponentResult(boolean passed, String detail) {
    }

    private static <T> ComponentResult roundTripComponent(Component<T> component, RegistryOps<NbtElement> ops) {
        DataResult<NbtElement> encoded = component.encode(ops);
        if (encoded.error().isPresent()) {
            return new ComponentResult(false, "encode failed: " + encoded.error().get().message());
        }
        DataResult<T> decoded = component.type().getCodecOrThrow()
                .parse(ops, encoded.getOrThrow());
        if (decoded.error().isPresent()) {
            return new ComponentResult(false, "decode failed: " + decoded.error().get().message());
        }

        T original = component.value();
        T roundTripped = decoded.getOrThrow();
        boolean equal = original.equals(roundTripped);
        boolean hashEqual = original.hashCode() == roundTripped.hashCode();
        if (equal && hashEqual) {
            return new ComponentResult(true, "equals + hashCode agree");
        }
        if (!equal) {
            return new ComponentResult(false, "decoded value is not equal to the original");
        }
        return new ComponentResult(false, "equals() agrees but hashCode() differs");
    }

    private static boolean roundTripStack(ItemStack stack, RegistryOps<NbtElement> ops) {
        DataResult<NbtElement> encoded = ItemStack.CODEC.encodeStart(ops, stack);
        if (encoded.error().isPresent()) return false;
        DataResult<ItemStack> decoded = ItemStack.CODEC.parse(ops, encoded.getOrThrow());
        if (decoded.error().isPresent()) return false;
        return ItemStack.areItemsAndComponentsEqual(stack, decoded.getOrThrow());
    }

    private static <T extends ArgumentBuilder<ServerCommandSource, T>> T configureSpawnArguments(T builder) {
        return builder
                .executes(SimplySwordsCommands::spawnHostile)
                .then(CommandManager.literal("runefused")
                        .then(CommandManager.argument("runefused_power", IdentifierArgumentType.identifier())
                                .suggests((context, suggestions) -> suggestPowers(suggestions, PowerType.RUNEFUSED))
                                .executes(SimplySwordsCommands::spawnHostile)
                                .then(CommandManager.literal("netherfused")
                                        .then(CommandManager.argument("netherfused_power", IdentifierArgumentType.identifier())
                                                .suggests((context, suggestions) -> suggestPowers(suggestions, PowerType.NETHER))
                                                .executes(SimplySwordsCommands::spawnHostile)))))
                .then(CommandManager.literal("netherfused")
                        .then(CommandManager.argument("netherfused_power", IdentifierArgumentType.identifier())
                                .suggests((context, suggestions) -> suggestPowers(suggestions, PowerType.NETHER))
                                .executes(SimplySwordsCommands::spawnHostile)));
    }

    private static int givePoweredGem(CommandContext<ServerCommandSource> context, PowerType powerType) throws CommandSyntaxException {
        Identifier power = getPower(context, "power", powerType);
        ItemStack stack = powerType == PowerType.NETHER
                ? new ItemStack(ItemsRegistry.NETHERFUSED_GEM.get())
                : new ItemStack(ItemsRegistry.RUNEFUSED_GEM.get());
        stack.set(ComponentTypeRegistry.GEM_POWER.get(), powerType == PowerType.NETHER
                ? GemPowerComponent.nether(power)
                : GemPowerComponent.runic(power));
        giveStack(context.getSource(), stack, power);
        return 1;
    }

    private static int givePoweredRunicWeapon(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Identifier power = getPower(context, "power", PowerType.RUNIC);
        Identifier weaponId = normalizeSimplySwordsId(IdentifierArgumentType.getIdentifier(context, "weapon"));
        Item item = Registries.ITEM.get(weaponId);
        if (!(item instanceof RunicSwordItem)) {
            throw UNKNOWN_RUNIC_WEAPON.create(weaponId);
        }

        ItemStack stack = new ItemStack(item);
        stack.set(ComponentTypeRegistry.GEM_POWER.get(), GemPowerComponent.runic(power));
        WeaponImplicitRegistry.getOrCreateWeaponImplicit(stack);
        giveStack(context.getSource(), stack, power);
        return 1;
    }

    private static Identifier getPower(CommandContext<ServerCommandSource> context, String argument, PowerType powerType) throws CommandSyntaxException {
        Identifier powerId = normalizeSimplySwordsId(IdentifierArgumentType.getIdentifier(context, argument));
        if (!GemPowerRegistry.REGISTRY.contains(powerId)) {
            throw UNKNOWN_POWER.create(powerId);
        }

        GemPower power = GemPowerRegistry.REGISTRY.get(powerId);
        if (power == null || power.isEmpty()) {
            throw UNKNOWN_POWER.create(powerId);
        }
        if (!power.applicableTypes().contains(powerType)) {
            throw WRONG_POWER_TYPE.create(powerId);
        }

        return powerId;
    }

    private static void giveStack(ServerCommandSource source, ItemStack stack, Identifier power) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Text itemName = stack.getName();
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        source.sendFeedback(() -> Text.literal("Gave " + itemName.getString() + " with power " + power), true);
    }

    private static Identifier normalizeSimplySwordsId(Identifier id) {
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            Identifier simplySwordsId = Identifier.of(SimplySwords.MOD_ID, id.getPath());
            if (GemPowerRegistry.REGISTRY.contains(simplySwordsId) || Registries.ITEM.containsId(simplySwordsId)) {
                return simplySwordsId;
            }
        }
        return id;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPowers(com.mojang.brigadier.suggestion.SuggestionsBuilder builder, PowerType powerType) {
        Stream<Identifier> ids = GemPowerRegistry.REGISTRY.getIds().stream()
                .filter(id -> {
                    GemPower power = GemPowerRegistry.REGISTRY.get(id);
                    return power != null && !power.isEmpty() && power.applicableTypes().contains(powerType);
                });
        return CommandSource.suggestIdentifiers(ids, builder);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRunicWeapons(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        Stream<Identifier> ids = Registries.ITEM.getIds().stream()
                .filter(id -> id.getNamespace().equals(SimplySwords.MOD_ID))
                .filter(id -> Registries.ITEM.get(id) instanceof RunicSwordItem);
        return CommandSource.suggestIdentifiers(ids, builder);
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestUniqueWeapons(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        Stream<Identifier> ids = Registries.ITEM.getIds().stream()
                .filter(id -> Registries.ITEM.get(id) instanceof UniqueSwordItem);
        return CommandSource.suggestIdentifiers(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestLootTables(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) {
        var lootTables = context.getSource().getServer().getReloadableRegistries();
        Stream<Identifier> ids = lootTables.getIds(RegistryKeys.LOOT_TABLE).stream()
                .filter(id -> {
                    LootTable table = lootTables.getLootTable(
                            RegistryKey.of(RegistryKeys.LOOT_TABLE, id)
                    );
                    return table.getType() == LootContextTypes.CHEST
                            || table.getType() == LootContextTypes.ENTITY;
                });
        return CommandSource.suggestIdentifiers(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestChestLootTables(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder) {
        var lootTables = context.getSource().getServer().getReloadableRegistries();
        Stream<Identifier> ids = lootTables.getIds(RegistryKeys.LOOT_TABLE).stream()
                .filter(id -> lootTables.getLootTable(
                        RegistryKey.of(RegistryKeys.LOOT_TABLE, id)
                ).getType() == LootContextTypes.CHEST);
        return CommandSource.suggestIdentifiers(ids, builder);
    }

    private static List<Item> getUniqueWeapons() {
        if (cachedUniqueWeapons == null) {
            cachedUniqueWeapons = new ArrayList<>();
            for (Identifier id : Registries.ITEM.getIds()) {
                if (id.getNamespace().equals(SimplySwords.MOD_ID)) {
                    Item item = Registries.ITEM.get(id);
                    if (item instanceof UniqueSwordItem) {
                        cachedUniqueWeapons.add(item);
                    }
                }
            }
        }
        return cachedUniqueWeapons;
    }

    private static int giveLootTestChests(
            CommandContext<ServerCommandSource> context,
            int count) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Identifier rawTableId = IdentifierArgumentType.getIdentifier(context, "table");
        Identifier tableId = normalizeLootTableId(rawTableId);
        var lootTables = source.getServer().getReloadableRegistries();

        if (!lootTables.getIds(RegistryKeys.LOOT_TABLE).contains(tableId)) {
            throw UNKNOWN_LOOT_TABLE.create(tableId);
        }

        RegistryKey<LootTable> tableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE, tableId);
        if (lootTables.getLootTable(tableKey).getType() != LootContextTypes.CHEST) {
            throw CHEST_LOOT_TABLE_REQUIRED.create(tableId);
        }

        ItemStack stack = new ItemStack(Items.CHEST, count);
        stack.set(
                DataComponentTypes.CONTAINER_LOOT,
                new ContainerLootComponent(tableKey, 0L)
        );
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        source.sendFeedback(
                () -> Text.literal("Gave " + count + " loot test chests using " + tableId
                        + ". Place and open a fresh chest for each real loot roll."),
                false
        );
        return count;
    }

    private static int showPityStatus(
            CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = getPityPlayer(context);
        PlayerPityState state = getPityState(player);
        ServerWorld world = player.getServerWorld();
        ChunkPos chunk = new ChunkPos(player.getBlockPos());
        int regionX = Math.floorDiv(chunk.x, 2);
        int regionZ = Math.floorDiv(chunk.z, 2);
        long region = ChunkPos.toLong(regionX, regionZ);
        String dimension = world.getRegistryKey().getValue().toString();
        Text playerName = player.getDisplayName();

        source.sendFeedback(
                () -> Text.literal("Pity status for ").append(playerName).append(":"),
                false
        );
        source.sendFeedback(
                () -> Text.literal(" Unique misses=" + state.uniqueMisses()
                        + " | soft pity=" + LootConfig.INSTANCE.uniqueSoftPityStart.get()
                        + " | hard pity=" + LootConfig.INSTANCE.uniqueHardPity.get()),
                false
        );
        source.sendFeedback(
                () -> Text.literal(" Tablet misses=" + state.tabletMisses()
                        + " | hard pity=" + LootConfig.INSTANCE.tabletHardPity.get()),
                false
        );
        source.sendFeedback(
                () -> Text.literal(" Ignore regions=" + state.ignoresRegionRestriction()
                        + " (session only)"),
                false
        );
        source.sendFeedback(
                () -> Text.literal(" Current region=" + dimension + " [" + regionX + ", " + regionZ + "]"
                        + " | unique credited=" + state.hasCreditedUnique(dimension, region)
                        + " | tablet credited=" + state.hasCreditedTablet(dimension, region)),
                false
        );
        return 1;
    }

    private static int setPityRegionBypass(
            CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = getPityPlayer(context);
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        getPityState(player).setIgnoreRegionRestriction(enabled);
        Text playerName = player.getDisplayName();
        source.sendFeedback(
                () -> Text.literal("Region-independent pity testing for ")
                        .append(playerName)
                        .append(enabled
                                ? " is enabled for this login session."
                                : " is disabled."),
                false
        );
        return 1;
    }

    private static int setPityMisses(
            CommandContext<ServerCommandSource> context,
            PityTrack track) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = getPityPlayer(context);
        PlayerPityState state = getPityState(player);
        int misses = IntegerArgumentType.getInteger(context, "misses");
        if (track == PityTrack.UNIQUE) {
            state.setUniqueMisses(misses);
        } else {
            state.setTabletMisses(misses);
        }
        Text playerName = player.getDisplayName();
        source.sendFeedback(
                () -> Text.literal("Set " + track.label + " pity misses for ")
                        .append(playerName)
                        .append(" to " + misses + "."),
                false
        );
        return 1;
    }

    private static int resetPity(
            CommandContext<ServerCommandSource> context,
            PityTrack track) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = getPityPlayer(context);
        PlayerPityState state = getPityState(player);
        if (track == PityTrack.UNIQUE || track == PityTrack.ALL) {
            state.clearUniqueProgress();
        }
        if (track == PityTrack.TABLET || track == PityTrack.ALL) {
            state.clearTabletProgress();
        }
        Text playerName = player.getDisplayName();
        source.sendFeedback(
                () -> Text.literal("Reset " + track.label + " pity counters and region history for ")
                        .append(playerName)
                        .append("."),
                false
        );
        return 1;
    }

    private static ServerPlayerEntity getPityPlayer(
            CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            return EntityArgumentType.getPlayer(context, "player");
        } catch (IllegalArgumentException ignored) {
            return context.getSource().getPlayerOrThrow();
        }
    }

    private static PlayerPityState getPityState(ServerPlayerEntity player) {
        return ((PityStateHolder) player).simplyswords$getPityState();
    }

    private enum PityTrack {
        UNIQUE("unique"),
        TABLET("tablet"),
        ALL("all");

        private final String label;

        PityTrack(String label) {
            this.label = label;
        }
    }

    private static int runLootTest(
            CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Identifier rawTableId = IdentifierArgumentType.getIdentifier(context, "table");
        Identifier tableId = normalizeLootTableId(rawTableId);
        int rolls = IntegerArgumentType.getInteger(context, "rolls");
        var lootTables = source.getServer().getReloadableRegistries();

        if (!lootTables.getIds(RegistryKeys.LOOT_TABLE).contains(tableId)) {
            throw UNKNOWN_LOOT_TABLE.create(tableId);
        }

        RegistryKey<LootTable> tableKey = RegistryKey.of(
                RegistryKeys.LOOT_TABLE,
                tableId
        );
        LootTable lootTable = lootTables.getLootTable(tableKey);
        LootContextParameterSet lootContext = createLootTestContext(
                source,
                tableId,
                lootTable
        );

        if (!rawTableId.equals(tableId)) {
            source.sendFeedback(
                    () -> Text.literal(
                            "Normalized loot table id: "
                                    + tableId
                                    + " (from "
                                    + rawTableId
                                    + ")"
                    ),
                    false
            );
        }

        source.sendFeedback(
                () -> Text.literal("Loot test: " + tableId + " | rolls=" + rolls),
                false
        );

        if (lootTable.getType() == LootContextTypes.CHEST) {
            sendChestLootTestConfiguration(source, tableId);
            ChestLootTestStats chestStats = simulateChestLootTest(
                    lootTable,
                    lootContext,
                    tableKey,
                    world,
                    rolls
            );
            sendLootTestStats(source, "Independent base-chance rolls", chestStats.base);
            sendLootTestStats(source, "Consecutive pity-progression rolls", chestStats.pity);
        } else {
            LootTestStats directStats = simulateDirectLootTest(
                    lootTable,
                    lootContext,
                    world,
                    rolls
            );
            sendLootTestStats(source, "Direct loot-table rolls", directStats);
        }
        return 1;
    }

    private static ChestLootTestStats simulateChestLootTest(
            LootTable lootTable,
            LootContextParameterSet lootContext,
            RegistryKey<LootTable> tableKey,
            ServerWorld world,
            int rolls
    ) {
        LootTestStats baseStats = new LootTestStats(rolls);
        LootTestStats pityStats = new LootTestStats(rolls);
        PlayerPityState baseState = new PlayerPityState();
        PlayerPityState pityState = new PlayerPityState();
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> directLoot = lootTable.generateLoot(
                    lootContext,
                    world.getRandom().nextLong()
            );
            List<ItemStack> baseLoot = new ArrayList<>(directLoot);
            baseLoot.addAll(PityLootManager.simulateGeneratedContainerLoot(
                    tableKey,
                    baseState,
                    world.getRandom(),
                    PityLootManager.SimulationMode.BASE_ONLY
            ));
            baseStats.record(baseLoot);

            List<ItemStack> pityLoot = new ArrayList<>(directLoot);
            pityLoot.addAll(PityLootManager.simulateGeneratedContainerLoot(
                    tableKey,
                    pityState,
                    world.getRandom(),
                    PityLootManager.SimulationMode.PITY_PROGRESSION
            ));
            pityStats.record(pityLoot);
        }
        return new ChestLootTestStats(baseStats, pityStats);
    }

    private static LootTestStats simulateDirectLootTest(
            LootTable lootTable,
            LootContextParameterSet lootContext,
            ServerWorld world,
            int rolls
    ) {
        LootTestStats stats = new LootTestStats(rolls);
        for (int i = 0; i < rolls; i++) {
            stats.record(lootTable.generateLoot(
                    lootContext,
                    world.getRandom().nextLong()
            ));
        }
        return stats;
    }

    private static void sendChestLootTestConfiguration(
            ServerCommandSource source,
            Identifier tableId
    ) {
        if (!LootConfig.INSTANCE.enableLootDrops.get()) {
            source.sendFeedback(
                    () -> Text.literal("Chest pity injection is disabled by the loot config."),
                    false
            );
            return;
        }
        if (!tableId.getPath().contains("chests")) {
            source.sendFeedback(
                    () -> Text.literal(
                            "Chest pity injection requires a loot table id containing 'chests'."
                    ),
                    false
            );
            return;
        }
        if (tableId.getPath().contains("spectrum")) {
            source.sendFeedback(
                    () -> Text.literal("Chest pity injection excludes Spectrum loot tables."),
                    false
            );
            return;
        }
        if (!LootConfig.INSTANCE.enableLootInVillages.get()
                && tableId.getPath().contains("village")) {
            source.sendFeedback(
                    () -> Text.literal("Chest pity injection is disabled for village loot tables."),
                    false
            );
            return;
        }

        float baseChance = PityLootManager.getConfiguredUniqueChance(tableId);
        source.sendFeedback(
                () -> Text.literal(
                        "Unique base chance="
                                + String.format(Locale.ROOT, "%.4f%%", baseChance)
                                + " | soft pity="
                                + LootConfig.INSTANCE.uniqueSoftPityStart.get()
                                + " | increment="
                                + String.format(
                                        Locale.ROOT,
                                        "%.4f percentage points",
                                        LootConfig.INSTANCE.uniqueSoftPityIncrement.get())
                                + " | hard pity="
                                + LootConfig.INSTANCE.uniqueHardPity.get()
                ),
                false
        );
        if (baseChance <= 0.0F) {
            source.sendFeedback(
                    () -> Text.literal(
                            "The configured unique chance is 0%; unique pity is inactive for this table."
                    ),
                    false
            );
        }
    }

    private static void sendLootTestStats(
            ServerCommandSource source,
            String label,
            LootTestStats stats
    ) {
        source.sendFeedback(() -> Text.literal(label + ":"), false);
        source.sendFeedback(
                () -> Text.literal(
                        " Rolls with a registered unique: "
                                + stats.rollsWithUnique
                                + "/"
                                + stats.rolls
                                + " ("
                                + formatPercentage(stats.rollsWithUnique, stats.rolls)
                                + ")"
                ),
                false
        );
        source.sendFeedback(
                () -> Text.literal(
                        " Total registered unique weapons dropped: "
                                + stats.totalUniqueWeapons
                ),
                false
        );

        if (stats.weaponCounts.isEmpty()) {
            source.sendFeedback(
                    () -> Text.literal(" No registered unique weapons dropped in this simulation."),
                    false
            );
            return;
        }

        List<Map.Entry<Identifier, Long>> sorted = new ArrayList<>(
                stats.weaponCounts.entrySet()
        );
        sorted.sort(
                Comparator.<Map.Entry<Identifier, Long>>comparingLong(
                                Map.Entry::getValue
                        )
                        .reversed()
                        .thenComparing(entry -> entry.getKey().toString())
        );
        for (Map.Entry<Identifier, Long> entry : sorted) {
            Identifier itemId = entry.getKey();
            long count = entry.getValue();
            source.sendFeedback(
                    () -> Text.literal(
                            " - "
                                    + itemId
                                    + ": "
                                    + count
                                    + " ("
                                    + formatPercentage(count, stats.rolls)
                                    + " per roll)"
                    ),
                    false
            );
        }
    }

    private static final class LootTestStats {
        private final int rolls;
        private final Map<Identifier, Long> weaponCounts = new HashMap<>();
        private long totalUniqueWeapons;
        private int rollsWithUnique;

        private LootTestStats(int rolls) {
            this.rolls = rolls;
        }

        private void record(List<ItemStack> generated) {
            boolean foundUnique = false;
            for (ItemStack generatedStack : generated) {
                if (generatedStack == null || generatedStack.isEmpty()
                        || !PityLootManager.isRegisteredUniqueLootItem(generatedStack.getItem())) {
                    continue;
                }
                Identifier itemId = Registries.ITEM.getId(generatedStack.getItem());

                long count = Math.max(1, generatedStack.getCount());
                weaponCounts.merge(itemId, count, Long::sum);
                totalUniqueWeapons += count;
                foundUnique = true;
            }
            if (foundUnique) {
                rollsWithUnique++;
            }
        }
    }

    private record ChestLootTestStats(LootTestStats base, LootTestStats pity) {
    }

    private static LootContextParameterSet createLootTestContext(
            ServerCommandSource source,
            Identifier tableId,
            LootTable lootTable) throws CommandSyntaxException {
        LootContextParameterSet.Builder builder =
                new LootContextParameterSet.Builder(source.getWorld())
                        .add(LootContextParameters.ORIGIN, source.getPosition());

        if (lootTable.getType() == LootContextTypes.CHEST) {
            builder.addOptional(LootContextParameters.THIS_ENTITY, source.getEntity());
            return builder.build(LootContextTypes.CHEST);
        }
        if (lootTable.getType() != LootContextTypes.ENTITY) {
            throw UNSUPPORTED_LOOT_TABLE.create(tableId);
        }

        Entity entity = source.getEntity();
        if (entity == null) {
            throw ENTITY_LOOT_SOURCE_REQUIRED.create(tableId);
        }

        DamageSource damageSource = entity instanceof PlayerEntity player
                ? source.getWorld().getDamageSources().playerAttack(player)
                : source.getWorld().getDamageSources().generic();
        builder.add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
                .addOptional(LootContextParameters.ATTACKING_ENTITY, entity)
                .addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, entity);
        if (entity instanceof ServerPlayerEntity player) {
            builder.addOptional(LootContextParameters.LAST_DAMAGE_PLAYER, player);
            builder.luck(player.getLuck());
        }
        return builder.build(LootContextTypes.ENTITY);
    }

    private static Identifier normalizeLootTableId(Identifier id) {
        String namespace = id.getNamespace();
        String path = id.getPath();

        if (namespace.equals(Identifier.DEFAULT_NAMESPACE)
                && path.startsWith(Identifier.DEFAULT_NAMESPACE + "/")) {
            path = path.substring((Identifier.DEFAULT_NAMESPACE + "/").length());
        }
        if (!path.contains("/")) {
            path = "chests/" + path;
        }

        int chestIndex = path.indexOf("chests/");
        int entityIndex = path.indexOf("entities/");
        int tablePathIndex;
        if (chestIndex < 0) {
            tablePathIndex = entityIndex;
        } else if (entityIndex < 0) {
            tablePathIndex = chestIndex;
        } else {
            tablePathIndex = Math.min(chestIndex, entityIndex);
        }
        if (tablePathIndex > 0) {
            path = path.substring(tablePathIndex);
        }

        return Identifier.of(namespace, path);
    }

    private static String formatPercentage(long count, int total) {
        if (total <= 0) {
            return "0.0000%";
        }
        return String.format(
                Locale.ROOT,
                "%.4f%%",
                count * 100.0 / total
        );
    }

    private static int spawnHostile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();
        List<Item> weapons = getUniqueWeapons();
        int count = 1;
        try {
            count = IntegerArgumentType.getInteger(context, "count");
        } catch (IllegalArgumentException ignored) {
        }

        Item selectedWeapon = null;
        try {
            Identifier weaponId = normalizeSimplySwordsId(IdentifierArgumentType.getIdentifier(context, "weapon"));
            Item item = Registries.ITEM.get(weaponId);
            if (!(item instanceof UniqueSwordItem)) {
                throw UNKNOWN_UNIQUE_WEAPON.create(weaponId);
            }
            selectedWeapon = item;
        } catch (IllegalArgumentException ignored) {
        }

        if (selectedWeapon == null && weapons.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No Simply Swords unique weapons found."), false);
            return 0;
        }

        Identifier forcedRunic = null;
        Identifier forcedNether = null;
        try {
            forcedRunic = getPower(context, "runefused_power", PowerType.RUNEFUSED);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            forcedNether = getPower(context, "netherfused_power", PowerType.NETHER);
        } catch (IllegalArgumentException ignored) {
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            EntityType<? extends MobEntity> mobType = HOSTILE_MOBS.get(rng.nextInt(HOSTILE_MOBS.size()));
            double offsetX = (rng.nextDouble() - 0.5) * 6.0;
            double offsetZ = (rng.nextDouble() - 0.5) * 6.0;
            BlockPos spawnPos = BlockPos.ofFloored(player.getX() + offsetX, player.getY(), player.getZ() + offsetZ);

            MobEntity mob = mobType.spawn(world, spawnPos, SpawnReason.COMMAND);
            if (mob == null) continue;

            Item weapon = selectedWeapon != null
                    ? selectedWeapon
                    : weapons.get(rng.nextInt(weapons.size()));
            ItemStack weaponStack = new ItemStack(weapon);
            AwakeningApi.initializeFullyAwakened(weaponStack);

            Identifier runicPower = forcedRunic;
            Identifier netherPower = forcedNether;
            if (runicPower == null && rng.nextFloat() <= RANDOM_POWER_CHANCE) {
                runicPower = GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED);
            }
            if (netherPower == null && rng.nextFloat() <= RANDOM_POWER_CHANCE) {
                netherPower = GemPowerRegistry.gemRandomPower(PowerType.NETHER);
            }
            weaponStack.set(ComponentTypeRegistry.GEM_POWER.get(), new GemPowerComponent(
                    true,
                    true,
                    runicPower != null ? runicPower : GemPower.EMPTY_ID,
                    netherPower != null ? netherPower : GemPower.EMPTY_ID
            ));

            mob.equipStack(EquipmentSlot.MAINHAND, weaponStack);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);

            if (count == 1) {
                Text mobName = mob.getName();
                Text weaponName = weaponStack.getName();
                net.minecraft.text.MutableText msg = Text.literal("Spawned ").append(mobName)
                        .append(" with ").append(weaponName);
                if (runicPower == null && netherPower == null) {
                    msg.append(" (empty sockets)");
                } else {
                    msg.append(" (");
                    boolean first = true;
                    if (runicPower != null) {
                        msg.append(Text.translatable("item.simplyswords.uniquesworditem.runefused_power." + runicPower.getPath()).setStyle(Styles.RUNIC));
                        first = false;
                    }
                    if (netherPower != null) {
                        if (!first) msg.append(" | ");
                        msg.append(Text.translatable("item.simplyswords.uniquesworditem.netherfused_power." + netherPower.getPath()).setStyle(Styles.NETHERFUSED));
                    }
                    msg.append(")");
                }
                final Text finalMsg = msg;
                source.sendFeedback(() -> finalMsg, false);
            }
        }

        if (count > 1) {
            final int spawned = count;
            if (selectedWeapon != null) {
                final Text selectedWeaponName = selectedWeapon.getName();
                source.sendFeedback(() -> Text.literal("Spawned " + spawned + " hostile mobs with ")
                        .append(selectedWeaponName).append("."), true);
            } else {
                source.sendFeedback(() -> Text.literal("Spawned " + spawned + " hostile mobs with random Simply Swords weapons."), true);
            }
        }
        return count;
    }
}
