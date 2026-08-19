package net.sweenus.simplyswords.gametest;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.util.HelperMethods;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

final class BalanceEquipment {

    private static final Map<LoadoutKey, ResolvedLoadout> LOADOUT_CACHE = new HashMap<>();

    // The harness player never ticks, so worn gear must be applied as attributes by hand.
    private static final Map<EquipmentSlot, Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier>>
            APPLIED_MODIFIERS = new EnumMap<>(EquipmentSlot.class);

    private BalanceEquipment() {
    }

    private record LoadoutKey(BalanceBuildProfile build, SpellScalingProfile profile, Identifier weapon) {
    }

    private record ResolvedLoadout(boolean available, String unavailableNotes,
                                   Map<EquipmentSlot, Item> armor, Item offhand) {
    }

    static EquipmentResult apply(ServerWorld world, ServerPlayerEntity player, ItemStack weapon,
                                 BalanceBuildProfile build, SpellScalingProfile profile) {
        clearEquipment(player);
        if (build.sharpness()) {
            Registry<Enchantment> enchantments = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            enchantments.getEntry(Enchantments.SHARPNESS).ifPresent(entry -> weapon.addEnchantment(entry, 5));
        }
        equip(player, EquipmentSlot.MAINHAND, weapon);
        float neutralSpellPower = rawSpellPower(player, profile);
        if (build.gearFamily() == GearFamily.NONE) {
            return EquipmentResult.available(build.sharpness() ? "Sharpness V" : "Neutral equipment", neutralSpellPower);
        }

        LoadoutKey key = new LoadoutKey(build, profile, Registries.ITEM.getId(weapon.getItem()));
        ResolvedLoadout loadout = LOADOUT_CACHE.get(key);
        if (loadout == null) {
            loadout = resolveLoadout(world, player, build, profile);
            LOADOUT_CACHE.put(key, loadout);
            clearEquipment(player);
            equip(player, EquipmentSlot.MAINHAND, weapon);
        }
        if (!loadout.available()) {
            return EquipmentResult.unavailable(loadout.unavailableNotes());
        }

        List<String> equipped = new ArrayList<>();
        List<String> enhancements = new ArrayList<>();
        loadout.armor().forEach((slot, item) -> {
            ItemStack stack = new ItemStack(item);
            if (build.gearTier() == GearTier.MAX) {
                applySpellEnchantments(world, stack, profile);
                String note = CasterGearEnhancement.enhance(world, stack, profile);
                if (!note.isBlank() && !enhancements.contains(note)) {
                    enhancements.add(note);
                }
            }
            equip(player, slot, stack);
            equipped.add(Registries.ITEM.getId(item).toString());
        });
        if (loadout.offhand() != null) {
            equip(player, EquipmentSlot.OFFHAND, new ItemStack(loadout.offhand()));
            equipped.add("offhand=" + Registries.ITEM.getId(loadout.offhand()));
        }

        float spellPower = rawSpellPower(player, profile);
        if (spellPower <= neutralSpellPower + 1.0E-4F) {
            return EquipmentResult.unavailable("Caster gear contributed no spell power over an unequipped player"
                    + " (spellPower=" + format(spellPower) + " vs neutral=" + format(neutralSpellPower) + ")");
        }
        String notes = String.join(";", equipped)
                + (enhancements.isEmpty() ? "" : ";upgrades=" + String.join("+", enhancements))
                + ";spellPower=" + format(spellPower);
        return EquipmentResult.available(notes, spellPower);
    }

    private static ResolvedLoadout resolveLoadout(ServerWorld world, ServerPlayerEntity player,
                                                  BalanceBuildProfile build, SpellScalingProfile profile) {
        Predicate<Identifier> familyFilter = familyFilter(build.gearFamily());
        Map<EquipmentSlot, ItemStack> selected = selectArmor(player, profile, familyFilter);
        if (selected.size() < 4) {
            return new ResolvedLoadout(false, "No complete " + build.gearFamily().name().toLowerCase(Locale.ROOT)
                    + " caster armor set is installed", Map.of(), null);
        }
        Map<EquipmentSlot, Item> armor = new EnumMap<>(EquipmentSlot.class);
        selected.forEach((slot, stack) -> armor.put(slot, stack.getItem()));
        selected.forEach((slot, stack) -> {
            if (build.gearTier() == GearTier.MAX) {
                applySpellEnchantments(world, stack, profile);
                CasterGearEnhancement.enhance(world, stack, profile);
            }
            equip(player, slot, stack);
        });

        Item offhandItem = null;
        if (build.offhand()) {
            ItemStack offhand = selectOffhand(player, profile, familyFilter);
            if (!offhand.isEmpty()) {
                offhandItem = offhand.getItem();
            } else if (build == BalanceBuildProfile.IRONS_MAX_OFFHAND) {
                return new ResolvedLoadout(false, "No legal Iron's offhand spell-power item is installed", Map.of(), null);
            }
        }
        return new ResolvedLoadout(true, "", armor, offhandItem);
    }

    private static Map<EquipmentSlot, ItemStack> selectArmor(ServerPlayerEntity player, SpellScalingProfile profile,
                                                              Predicate<Identifier> familyFilter) {
        Map<EquipmentSlot, ItemStack> selected = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack best = ItemStack.EMPTY;
            float bestPower = Float.NEGATIVE_INFINITY;
            for (Item item : Registries.ITEM) {
                Identifier id = Registries.ITEM.getId(item);
                if (!(item instanceof ArmorItem armor) || armor.getSlotType() != slot || !familyFilter.test(id)) {
                    continue;
                }
                ItemStack candidate = new ItemStack(item);
                equip(player, slot, candidate);
                float power = averageSpellPower(player, profile);
                if (power > bestPower) {
                    bestPower = power;
                    best = candidate;
                }
            }
            equip(player, slot, ItemStack.EMPTY);
            if (!best.isEmpty()) {
                selected.put(slot, best);
                equip(player, slot, best);
            }
        }
        return selected;
    }

    private static ItemStack selectOffhand(ServerPlayerEntity player, SpellScalingProfile profile,
                                           Predicate<Identifier> familyFilter) {
        float baseline = averageSpellPower(player, profile);
        return Registries.ITEM.stream()
                .filter(item -> familyFilter.test(Registries.ITEM.getId(item)))
                .map(ItemStack::new)
                .filter(stack -> {
                    equip(player, EquipmentSlot.OFFHAND, stack);
                    return averageSpellPower(player, profile) > baseline + 0.001F;
                })
                .max(Comparator.comparingDouble(stack -> {
                    equip(player, EquipmentSlot.OFFHAND, stack);
                    return averageSpellPower(player, profile);
                }))
                .orElseGet(() -> {
                    equip(player, EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    return ItemStack.EMPTY;
                });
    }

    static void equip(ServerPlayerEntity player, EquipmentSlot slot, ItemStack stack) {
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> previous = APPLIED_MODIFIERS.remove(slot);
        if (previous != null) {
            player.getAttributes().removeModifiers(previous);
        }
        ItemStack equipped = stack == null ? ItemStack.EMPTY : stack;
        player.equipStack(slot, equipped);
        if (equipped.isEmpty()) {
            return;
        }
        ImmutableMultimap.Builder<RegistryEntry<EntityAttribute>, EntityAttributeModifier> builder =
                ImmutableMultimap.builder();
        equipped.applyAttributeModifiers(slot, builder::put);
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiers = builder.build();
        if (!modifiers.isEmpty()) {
            player.getAttributes().addTemporaryModifiers(modifiers);
            APPLIED_MODIFIERS.put(slot, modifiers);
        }
    }

    private static void applySpellEnchantments(ServerWorld world, ItemStack stack, SpellScalingProfile profile) {
        Registry<Enchantment> registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        registry.streamEntries()
                .filter(entry -> isRelevantSpellEnchantment(entry, profile))
                .forEach(entry -> stack.addEnchantment(entry, entry.value().getMaxLevel()));
    }

    private static boolean isRelevantSpellEnchantment(RegistryEntry.Reference<Enchantment> entry,
                                                       SpellScalingProfile profile) {
        String path = entry.registryKey().getValue().getPath().toLowerCase(Locale.ROOT);
        return path.contains(profile.id()) || path.contains("spell_power") || path.contains("spellpower");
    }

    private static Predicate<Identifier> familyFilter(GearFamily family) {
        return switch (family) {
            case IRONS -> id -> id.getNamespace().equals("irons_spellbooks");
            case SPELL_POWER -> id -> !id.getNamespace().equals("minecraft")
                    && !id.getNamespace().equals("simplyswords")
                    && !id.getNamespace().equals("irons_spellbooks");
            case ANY_CASTER -> id -> !id.getNamespace().equals("minecraft") && !id.getNamespace().equals("simplyswords");
            case NONE -> id -> false;
        };
    }

    private static float averageSpellPower(ServerPlayerEntity player, SpellScalingProfile profile) {
        float total = 0.0F;
        for (int i = 0; i < 8; i++) {
            total += rawSpellPower(player, profile);
        }
        return total / 8.0F;
    }

    private static float rawSpellPower(ServerPlayerEntity player, SpellScalingProfile profile) {
        return HelperMethods.commonSpellAttributeScaling(1.0F, player, profile);
    }

    private static void clearEquipment(ServerPlayerEntity player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            equip(player, slot, ItemStack.EMPTY);
        }
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
