package net.sweenus.simplyswords.loot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.UniqueLootRegistry;
import net.sweenus.simplyswords.config.LootConfig;
import net.sweenus.simplyswords.item.UniqueSwordItem;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.util.ModLootTableModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class PityLootManager {
    public enum SimulationMode {
        BASE_ONLY,
        PITY_PROGRESSION
    }

    private PityLootManager() {
    }

    public static void applyGeneratedContainerLoot(Inventory inventory, RegistryKey<net.minecraft.loot.LootTable> table,
                                                   PlayerEntity player, BlockPos pos, World world) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || world == null) return;
        Identifier id = table.getValue();
        if (!isEligibleChestTable(id)) return;

        PlayerPityState state = ((PityStateHolder) serverPlayer).simplyswords$getPityState();
        ChunkPos chunk = new ChunkPos(pos);
        long region = ChunkPos.toLong(Math.floorDiv(chunk.x, 2), Math.floorDiv(chunk.z, 2));
        String dimension = world.getRegistryKey().getValue().toString();

        rollContainerLoot(
                id,
                state,
                serverPlayer.getRandom(),
                true,
                () -> state.creditUnique(dimension, region),
                () -> state.creditTablet(dimension, region),
                stack -> insert(inventory, stack)
        );
    }

    /**
     * Runs the post-loot-table chest injection against isolated state.
     *
     * <p>This is intended for diagnostics such as {@code loot_test}. Pity
     * progression treats each call as a newly credited region without storing
     * synthetic region ids.</p>
     */
    public static List<ItemStack> simulateGeneratedContainerLoot(
            RegistryKey<net.minecraft.loot.LootTable> table,
            PlayerPityState state,
            Random random,
            SimulationMode mode
    ) {
        Identifier id = table.getValue();
        if (!isEligibleChestTable(id)) {
            return List.of();
        }

        List<ItemStack> generated = new ArrayList<>();
        boolean applyPity = mode == SimulationMode.PITY_PROGRESSION;
        PlayerPityState simulationState = state == null ? new PlayerPityState() : state;
        rollContainerLoot(
                id,
                simulationState,
                random,
                applyPity,
                () -> true,
                () -> true,
                stack -> {
                    generated.add(stack);
                    return true;
                }
        );
        return generated;
    }

    public static boolean isEligibleChestTable(Identifier id) {
        return LootConfig.INSTANCE.enableLootDrops.get()
                && id.getPath().contains("chests")
                && !id.getPath().contains("spectrum")
                && (LootConfig.INSTANCE.enableLootInVillages.get()
                || !id.getPath().contains("village"));
    }

    public static float getConfiguredUniqueChance(Identifier id) {
        Float override = LootConfig.INSTANCE.uniqueLootTableOptions.get(id);
        return override == null
                ? LootConfig.INSTANCE.uniqueLootTableWeight.get()
                : override;
    }

    private static void rollContainerLoot(
            Identifier id,
            PlayerPityState state,
            Random random,
            boolean applyPity,
            BooleanSupplier creditUnique,
            BooleanSupplier creditTablet,
            Predicate<ItemStack> output
    ) {
        float uniqueBase = getConfiguredUniqueChance(id);
        if (uniqueBase > 0.0F) {
            boolean credited = applyPity && creditUnique.getAsBoolean();
            float chance = uniqueBase;
            if (credited && state.uniqueMisses() >= LootConfig.INSTANCE.uniqueSoftPityStart.get()) {
                chance += (state.uniqueMisses() - LootConfig.INSTANCE.uniqueSoftPityStart.get() + 1)
                        * LootConfig.INSTANCE.uniqueSoftPityIncrement.get();
            }
            boolean success = credited
                    && state.uniqueMisses() >= LootConfig.INSTANCE.uniqueHardPity.get() - 1;
            success |= random.nextFloat() * 100.0F < Math.min(100.0F, chance);
            if (success) {
                Item unique = chooseUnique(random);
                if (unique != null && output.test(
                        AwakeningApi.initializeNaturalDrop(new ItemStack(unique)))) {
                    if (applyPity) {
                        state.resetUnique();
                    }
                }
            } else if (credited) {
                state.missUnique();
            }
        }

        float tabletBase = LootConfig.INSTANCE.runicLootTableWeight.get();
        if (tabletBase > 0.0F) {
            boolean tabletCredited = applyPity && creditTablet.getAsBoolean();
            boolean tabletSuccess = tabletCredited
                    && state.tabletMisses() >= LootConfig.INSTANCE.tabletHardPity.get() - 1;
            tabletSuccess |= random.nextFloat() * 100.0F < tabletBase;
            if (tabletSuccess && output.test(new ItemStack(ItemsRegistry.RUNIC_TABLET.get()))) {
                if (applyPity) {
                    state.resetTablet();
                }
            } else if (!tabletSuccess && tabletCredited) {
                state.missTablet();
            }
        }
    }

    private static Item chooseUnique(Random random) {
        List<Item> weighted = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item instanceof UniqueSwordItem
                    && ModLootTableModifiers.isLootableUnique(item)
                    && !LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(item)) {
                weighted.add(item);
            }
        }
        for (Map.Entry<Item, Integer> entry : UniqueLootRegistry.entries().entrySet()) {
            if (LootConfig.INSTANCE.disabledUniqueWeaponLoot.contains(entry.getKey())) continue;
            for (int i = 0; i < entry.getValue(); i++) weighted.add(entry.getKey());
        }
        return weighted.isEmpty() ? null : weighted.get(random.nextInt(weighted.size()));
    }

    private static boolean insert(Inventory inventory, ItemStack stack) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack current = inventory.getStack(i);
            if (current.isEmpty()) {
                inventory.setStack(i, stack);
                inventory.markDirty();
                return true;
            }
            if (ItemStack.areItemsAndComponentsEqual(current, stack)
                    && current.getCount() < current.getMaxCount()) {
                int moved = Math.min(stack.getCount(), current.getMaxCount() - current.getCount());
                current.increment(moved);
                stack.decrement(moved);
                if (stack.isEmpty()) {
                    inventory.markDirty();
                    return true;
                }
            }
        }
        return false;
    }
}
