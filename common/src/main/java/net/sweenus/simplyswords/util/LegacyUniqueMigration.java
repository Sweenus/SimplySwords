package net.sweenus.simplyswords.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.AwakeningApi;
import net.sweenus.simplyswords.api.AwakeningFormRegistry;
import net.sweenus.simplyswords.item.component.AwakeningRouteComponent;
import net.sweenus.simplyswords.item.component.RelicAttunementComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

public final class LegacyUniqueMigration {
    private LegacyUniqueMigration() {
    }

    public static ItemStack convertStack(ItemStack source) {
        ItemStack result;
        int level;
        int route = RelicAttunementComponent.NONE;
        Identifier awakeningRoute = null;
        if (source.isOf(ItemsRegistry.WAKING_LICHBLADE.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.SLUMBERING_LICHBLADE.get(), source.getCount());
            level = 4;
            awakeningRoute = AwakeningFormRegistry.LICHBLADE_ROUTE;
        } else if (source.isOf(ItemsRegistry.AWAKENED_LICHBLADE.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.SLUMBERING_LICHBLADE.get(), source.getCount());
            level = 8;
            awakeningRoute = AwakeningFormRegistry.LICHBLADE_ROUTE;
        } else if (source.isOf(ItemsRegistry.RIGHTEOUS_RELIC.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.DORMANT_RELIC.get(), source.getCount());
            level = 4;
            route = RelicAttunementComponent.SUN;
        } else if (source.isOf(ItemsRegistry.TAINTED_RELIC.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.DORMANT_RELIC.get(), source.getCount());
            level = 4;
            route = RelicAttunementComponent.HARBINGER;
        } else if (source.isOf(ItemsRegistry.SUNFIRE.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.DORMANT_RELIC.get(), source.getCount());
            level = 8;
            route = RelicAttunementComponent.SUN;
        } else if (source.isOf(ItemsRegistry.HARBINGER.get())) {
            result = source.copyComponentsToNewStack(ItemsRegistry.DORMANT_RELIC.get(), source.getCount());
            level = 8;
            route = RelicAttunementComponent.HARBINGER;
        } else {
            return source;
        }
        AwakeningApi.setLevel(result, level);
        if (awakeningRoute != null) {
            result.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(),
                    new AwakeningRouteComponent(awakeningRoute));
        }
        if (route != RelicAttunementComponent.NONE) {
            result.set(ComponentTypeRegistry.RELIC_ATTUNEMENT.get(), new RelicAttunementComponent(route));
            result.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(), new AwakeningRouteComponent(
                    route == RelicAttunementComponent.SUN
                            ? AwakeningFormRegistry.SUN_ROUTE
                            : AwakeningFormRegistry.HARBINGER_ROUTE
            ));
        }
        return result;
    }

    public static boolean migratePlayerSlot(ItemStack source, Entity holder, int slot) {
        ItemStack converted = convertStack(source);
        if (converted == source || !(holder instanceof PlayerEntity player)
                || slot < 0 || slot >= player.getInventory().size()) {
            return false;
        }
        player.getInventory().setStack(slot, converted);
        return true;
    }
}
