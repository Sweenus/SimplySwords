package net.sweenus.simplyswords.neoforge.gametest;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.registries.UpgradeOrbTypeRegistry;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.sweenus.simplyswords.api.SpellScalingProfile;
import net.sweenus.simplyswords.gametest.CasterGearEnhancement;

import java.util.Map;
import java.util.Optional;

public final class IronsCasterGearEnhancer implements CasterGearEnhancement.Enhancer {

    private static final Map<SpellScalingProfile, RegistryKey<UpgradeOrbType>> ORBS_BY_SCHOOL = Map.of(
            SpellScalingProfile.FIRE, UpgradeOrbTypeRegistry.FIRE_SPELL_POWER,
            SpellScalingProfile.FROST, UpgradeOrbTypeRegistry.ICE_SPELL_POWER,
            SpellScalingProfile.LIGHTNING, UpgradeOrbTypeRegistry.LIGHTNING_SPELL_POWER,
            SpellScalingProfile.ARCANE, UpgradeOrbTypeRegistry.ENDER_SPELL_POWER,
            SpellScalingProfile.SOUL, UpgradeOrbTypeRegistry.BLOOD_SPELL_POWER,
            SpellScalingProfile.HEALING, UpgradeOrbTypeRegistry.HOLY_SPELL_POWER,
            SpellScalingProfile.NATURE, UpgradeOrbTypeRegistry.NATURE_SPELL_POWER,
            SpellScalingProfile.EVOCATION, UpgradeOrbTypeRegistry.EVOCATION_SPELL_POWER
    );

    public static void register() {
        CasterGearEnhancement.register(new IronsCasterGearEnhancer());
    }

    @Override
    public String enhance(ServerWorld world, ItemStack armour, SpellScalingProfile profile) {
        RegistryKey<UpgradeOrbType> key = ORBS_BY_SCHOOL.get(profile);
        if (key == null) {
            return "no " + profile.id() + " upgrade orb exists";
        }
        if (ServerConfigs.UPGRADE_BLACKLIST_ITEMS.contains(armour.getItem())) {
            return "blacklisted for upgrades";
        }
        int cap = Math.max(0, ServerConfigs.MAX_UPGRADES.get());
        if (cap == 0) {
            return "upgrade cap is zero";
        }
        Registry<UpgradeOrbType> registry = UpgradeOrbTypeRegistry.upgradeTypeRegistry(world.getRegistryManager());
        Optional<RegistryEntry.Reference<UpgradeOrbType>> orb = registry.getEntry(key);
        if (orb.isEmpty()) {
            return "upgrade orb " + key.getValue() + " is not registered";
        }

        String slot = UpgradeUtils.getRelevantEquipmentSlot(armour);
        UpgradeData data = UpgradeData.getUpgradeData(armour);
        for (int applied = 0; applied < cap; applied++) {
            data = data.addUpgrade(armour, orb.get(), slot);
        }
        UpgradeData.set(armour, data);
        return key.getValue().getPath() + "x" + data.getTotalUpgrades();
    }
}
