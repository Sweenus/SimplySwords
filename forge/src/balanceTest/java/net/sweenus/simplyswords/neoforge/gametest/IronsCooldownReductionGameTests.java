package net.sweenus.simplyswords.neoforge.gametest;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.sweenus.simplyswords.api.SimplySwordsAPI;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.registry.ItemsRegistry;
import net.sweenus.simplyswords.world.WeaponAbilityCooldownManager;

public final class IronsCooldownReductionGameTests {

    @GameTest(templateName = "balance_empty", tickLimit = 180)
    @EmptyTemplate(value = "4x4x4")
    @TestHolder("simplyswords.irons_cooldown_reduction")
    public static void cooldownReductionMatchesIronsAndHonorsPolicy(TestContext context) {
        ZombieEntity actor = context.spawnMob(EntityType.ZOMBIE, 1, 1, 1);
        EntityAttributeInstance attribute = actor.getAttributeInstance(AttributeRegistry.COOLDOWN_REDUCTION);
        context.assertTrue(attribute != null, "Iron's cooldown reduction attribute is missing");
        if (attribute == null) {
            return;
        }

        ItemStack weapon = new ItemStack(ItemsRegistry.FLAMEWIND.get());
        int baseCooldown = 100;
        boolean previousToggle = Config.general.compatEnableIronsCooldownReduction.getUnconditional();
        try {
            Config.general.compatEnableIronsCooldownReduction.accept(true);
            attribute.setBaseValue(1.5D);

            int expected = Utils.applyCooldownReduction(baseCooldown, actor);
            int actual = SimplySwordsAPI.getEffectiveWeaponCooldownTicks(weapon, actor, baseCooldown);
            context.assertTrue(actual == expected, "Simply Swords cooldown does not match Iron's helper");
            context.assertTrue(actual < baseCooldown, "Iron's cooldown reduction did not shorten the cooldown");

            Config.general.compatEnableIronsCooldownReduction.accept(false);
            context.assertTrue(
                    SimplySwordsAPI.getEffectiveWeaponCooldownTicks(weapon, actor, baseCooldown) == baseCooldown,
                    "Global cooldown reduction toggle was ignored");

            Config.general.compatEnableIronsCooldownReduction.accept(true);
            SimplySwordsAPI.setWeaponCooldown(actor, weapon, baseCooldown);
            context.assertTrue(
                    WeaponAbilityCooldownManager.isCoolingDown(context.getWorld(), actor, weapon),
                    "Reduced cooldown was not committed");

            context.runAtTick(Math.max(1, actual - 1), () -> context.assertTrue(
                    WeaponAbilityCooldownManager.isCoolingDown(context.getWorld(), actor, weapon),
                    "Committed cooldown expired too early"));
            context.runAtTick(actual + 1L, () -> {
                try {
                    context.assertTrue(
                            !WeaponAbilityCooldownManager.isCoolingDown(context.getWorld(), actor, weapon),
                            "Committed cooldown lasted longer than the reduced duration");
                    context.complete();
                } finally {
                    Config.general.compatEnableIronsCooldownReduction.accept(previousToggle);
                }
            });
        } catch (RuntimeException | Error failure) {
            Config.general.compatEnableIronsCooldownReduction.accept(previousToggle);
            throw failure;
        }
    }

}
