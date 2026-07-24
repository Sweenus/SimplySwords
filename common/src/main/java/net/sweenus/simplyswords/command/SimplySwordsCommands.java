package net.sweenus.simplyswords.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.api.WeaponImplicitRegistry;
import net.sweenus.simplyswords.item.RunicSwordItem;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.power.GemPower;
import net.sweenus.simplyswords.power.GemPowerComponent;
import net.sweenus.simplyswords.power.PowerType;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.GemPowerRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class SimplySwordsCommands {

    private static final DynamicCommandExceptionType UNKNOWN_POWER = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown Simply Swords gem power: " + value));
    private static final DynamicCommandExceptionType WRONG_POWER_TYPE = new DynamicCommandExceptionType(value ->
            Text.literal("Gem power cannot be applied to this item type: " + value));
    private static final DynamicCommandExceptionType UNKNOWN_RUNIC_WEAPON = new DynamicCommandExceptionType(value ->
            Text.literal("Unknown Simply Swords runic weapon: " + value));

    private static List<Item> cachedUniqueWeapons;
    private static final float SOCKET_CHANCE = 0.5F;
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
                .then(CommandManager.literal("spawn_hostile")
                        .executes(context -> spawnHostile(context, 1))
                        .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 50))
                                .executes(context -> spawnHostile(context, IntegerArgumentType.getInteger(context, "count")))
                                .then(CommandManager.literal("runefused")
                                        .then(CommandManager.argument("runefused_power", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> suggestPowers(builder, PowerType.RUNEFUSED))
                                                .executes(context -> spawnHostile(context, IntegerArgumentType.getInteger(context, "count")))
                                                .then(CommandManager.literal("netherfused")
                                                        .then(CommandManager.argument("netherfused_power", IdentifierArgumentType.identifier())
                                                                .suggests((context, builder) -> suggestPowers(builder, PowerType.NETHER))
                                                                .executes(context -> spawnHostile(context, IntegerArgumentType.getInteger(context, "count")))))))
                                .then(CommandManager.literal("netherfused")
                                        .then(CommandManager.argument("netherfused_power", IdentifierArgumentType.identifier())
                                                .suggests((context, builder) -> suggestPowers(builder, PowerType.NETHER))
                                                .executes(context -> spawnHostile(context, IntegerArgumentType.getInteger(context, "count"))))))));
    }

    private static int givePoweredGem(CommandContext<ServerCommandSource> context, PowerType powerType) throws CommandSyntaxException {
        RegistryEntry<GemPower> power = getPower(context, "power", powerType);
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
        RegistryEntry<GemPower> power = getPower(context, "power", PowerType.RUNIC);
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

    private static RegistryEntry<GemPower> getPower(CommandContext<ServerCommandSource> context, String argument, PowerType powerType) throws CommandSyntaxException {
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

        RegistryEntry<GemPower> entry = GemPowerRegistry.REGISTRY.getHolder(powerId);
        if (entry == null) {
            throw UNKNOWN_POWER.create(powerId);
        }
        return entry;
    }

    private static void giveStack(ServerCommandSource source, ItemStack stack, RegistryEntry<GemPower> power) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Text itemName = stack.getName();
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }

        Identifier powerId = GemPowerRegistry.REGISTRY.getId(power.value());
        source.sendFeedback(() -> Text.literal("Gave " + itemName.getString() + " with power " + powerId), true);
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

    private static int spawnHostile(CommandContext<ServerCommandSource> context, int count) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = source.getWorld();
        List<Item> weapons = getUniqueWeapons();

        if (weapons.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No Simply Swords unique weapons found."), false);
            return 0;
        }

        RegistryEntry<GemPower> forcedRunic = null;
        RegistryEntry<GemPower> forcedNether = null;
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

            Item weapon = weapons.get(rng.nextInt(weapons.size()));
            ItemStack weaponStack = new ItemStack(weapon);

            RegistryEntry<GemPower> runicPower = forcedRunic;
            RegistryEntry<GemPower> netherPower = forcedNether;
            if (runicPower == null && rng.nextFloat() <= SOCKET_CHANCE) {
                runicPower = GemPowerRegistry.gemRandomPower(PowerType.RUNEFUSED);
            }
            if (netherPower == null && rng.nextFloat() <= SOCKET_CHANCE) {
                netherPower = GemPowerRegistry.gemRandomPower(PowerType.NETHER);
            }
            if (runicPower != null || netherPower != null) {
                weaponStack.set(ComponentTypeRegistry.GEM_POWER.get(),
                        GemPowerComponent.create(runicPower, netherPower));
            }

            mob.equipStack(EquipmentSlot.MAINHAND, weaponStack);
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }

        final int spawned = count;
        source.sendFeedback(() -> Text.literal("Spawned " + spawned + " hostile mob" + (spawned == 1 ? "" : "s") + " with random Simply Swords weapons."), true);
        return spawned;
    }
}
