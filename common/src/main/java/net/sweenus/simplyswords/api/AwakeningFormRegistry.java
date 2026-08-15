package net.sweenus.simplyswords.api;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.item.component.AwakeningRouteComponent;
import net.sweenus.simplyswords.item.component.RelicAttunementComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.ItemsRegistry;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

//
// Runtime registry and resolver for addon-defined Runic Forge form families.
//
public final class AwakeningFormRegistry {
    public static final Identifier LICHBLADE_ROUTE = Identifier.of(SimplySwords.MOD_ID, "lichblade");
    public static final Identifier SUN_ROUTE = Identifier.of(SimplySwords.MOD_ID, "sun");
    public static final Identifier HARBINGER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "harbinger");
    public static final Identifier STORMSCALE_ROUTE = Identifier.of(SimplySwords.MOD_ID, "stormscale");
    public static final Identifier IONBOUND_ROUTE = Identifier.of(SimplySwords.MOD_ID, "ionbound_stormscale");
    public static final Identifier WATCHER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "watcher_claymore");
    public static final Identifier DEVOURER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "the_devourer");
    public static final Identifier WRAITHFANG_ROUTE = Identifier.of(SimplySwords.MOD_ID, "wraithfang");
    public static final Identifier WRAITHMAW_ROUTE = Identifier.of(SimplySwords.MOD_ID, "wraithmaw");
    public static final Identifier WICKPIERCER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "wickpiercer");
    public static final Identifier GLOAMPIERCER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "gloampiercer");
    public static final Identifier SOULRENDER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "soulrender");
    public static final Identifier SOULSTALKER_ROUTE = Identifier.of(SimplySwords.MOD_ID, "soulstalker");

    private static final Map<Item, AwakeningFormFamily> FAMILIES = new IdentityHashMap<>();
    private static boolean builtinsRegistered;

    private AwakeningFormRegistry() {
    }

    public static synchronized void register(AwakeningFormFamily family) {
        for (Item member : family.members()) {
            AwakeningFormFamily existing = FAMILIES.get(member);
            if (existing != null && existing != family) {
                throw new IllegalStateException("Item is already registered to an awakening form family: " + member);
            }
        }
        for (Item member : family.members()) {
            FAMILIES.put(member, family);
            AwakeningProfileRegistry.register(member, family.profile());
        }
    }

    //
    // Register Simply Swords' evolving weapons through the API
    //
    public static synchronized void registerBuiltins() {
        if (builtinsRegistered) {
            return;
        }

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.SLUMBERING_LICHBLADE.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "slumbering_lichblade"))
                .basePresentation(
                        "item.simplyswords.slumbering_lichblade",
                        AwakeningFormRarity.UNIQUE,
                        0.0F)
                .selectionLevel(0)
                .persistentProgression(true)
                .alias(ItemsRegistry.WAKING_LICHBLADE.get())
                .alias(ItemsRegistry.AWAKENED_LICHBLADE.get())
                .route(LICHBLADE_ROUTE,
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "waking_lichblade"),
                                4,
                                ItemsRegistry.SLUMBERING_LICHBLADE.get(),
                                "item.simplyswords.waking_lichblade",
                                AwakeningFormRarity.UNIQUE,
                                0.5F),
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "awakened_lichblade"),
                                8,
                                ItemsRegistry.SLUMBERING_LICHBLADE.get(),
                                "item.simplyswords.awakened_lichblade",
                                AwakeningFormRarity.LEGENDARY,
                                1.0F))
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.STORMSCALE.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "stormscale"))
                .basePresentation("item.simplyswords.stormscale", AwakeningFormRarity.UNIQUE, 0.0F)
                .selectionLevel(4)
                .route(STORMSCALE_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "awakened_stormscale"), 4,
                        ItemsRegistry.STORMSCALE.get(), "item.simplyswords.stormscale",
                        AwakeningFormRarity.UNIQUE, 0.0F))
                .route(IONBOUND_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "ionbound_stormscale"), 4,
                        ItemsRegistry.IONBOUND_STORMSCALE.get(), "item.simplyswords.ionbound_stormscale",
                        AwakeningFormRarity.LEGENDARY, 1.0F))
                .routeHandler(context -> context.world().isThundering() ? IONBOUND_ROUTE : STORMSCALE_ROUTE)
                .routeTransitionHandler((context, currentRoute) ->
                        STORMSCALE_ROUTE.equals(currentRoute) && context.world().isThundering()
                                ? IONBOUND_ROUTE : currentRoute)
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.WATCHER_CLAYMORE.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "watcher_claymore"))
                .basePresentation("item.simplyswords.watcher_claymore", AwakeningFormRarity.UNIQUE, 0.0F)
                .selectionLevel(4)
                .route(WATCHER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "awakened_watcher_claymore"), 4,
                        ItemsRegistry.WATCHER_CLAYMORE.get(), "item.simplyswords.watcher_claymore",
                        AwakeningFormRarity.UNIQUE, 0.0F))
                .route(DEVOURER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "the_devourer"), 4,
                        ItemsRegistry.THE_DEVOURER.get(), "item.simplyswords.the_devourer",
                        AwakeningFormRarity.LEGENDARY, 1.0F))
                .routeHandler(context -> isDeepDarkForge(context) ? DEVOURER_ROUTE : WATCHER_ROUTE)
                .routeTransitionHandler((context, currentRoute) ->
                        WATCHER_ROUTE.equals(currentRoute) && isDeepDarkForge(context)
                                ? DEVOURER_ROUTE : currentRoute)
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.WRAITHFANG.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "wraithfang"))
                .basePresentation("item.simplyswords.wraithfang", AwakeningFormRarity.UNIQUE, 0.0F)
                .selectionLevel(0)
                .route(WRAITHFANG_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "spectral_wraithfang"), 0,
                        ItemsRegistry.WRAITHFANG.get(), "item.simplyswords.wraithfang",
                        AwakeningFormRarity.UNIQUE, 0.0F))
                .route(WRAITHMAW_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "wraithmaw"), 0,
                        ItemsRegistry.WRAITHMAW.get(), "item.simplyswords.wraithmaw",
                        AwakeningFormRarity.LEGENDARY, 1.0F))
                .routeHandler(context -> WRAITHFANG_ROUTE)
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.WICKPIERCER.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "wickpiercer"))
                .basePresentation("item.simplyswords.wickpiercer", AwakeningFormRarity.UNIQUE, 0.0F)
                .selectionLevel(0)
                .route(WICKPIERCER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "spectral_wickpiercer"), 0,
                        ItemsRegistry.WICKPIERCER.get(), "item.simplyswords.wickpiercer",
                        AwakeningFormRarity.UNIQUE, 0.0F))
                .route(GLOAMPIERCER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "gloampiercer"), 0,
                        ItemsRegistry.GLOAMPIERCER.get(), "item.simplyswords.gloampiercer",
                        AwakeningFormRarity.LEGENDARY, 1.0F))
                .routeHandler(context -> WICKPIERCER_ROUTE)
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.SOULRENDER.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "soulrender"))
                .basePresentation("item.simplyswords.soulrender", AwakeningFormRarity.UNIQUE, 0.0F)
                .selectionLevel(0)
                .route(SOULRENDER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "spectral_soulrender"), 0,
                        ItemsRegistry.SOULRENDER.get(), "item.simplyswords.soulrender",
                        AwakeningFormRarity.UNIQUE, 0.0F))
                .route(SOULSTALKER_ROUTE, new AwakeningFormStage(
                        Identifier.of(SimplySwords.MOD_ID, "soulstalker"), 0,
                        ItemsRegistry.SOULSTALKER.get(), "item.simplyswords.soulstalker",
                        AwakeningFormRarity.LEGENDARY, 1.0F))
                .routeHandler(context -> SOULRENDER_ROUTE)
                .build());

        register(AwakeningFormFamily.builder(
                        ItemsRegistry.DORMANT_RELIC.get(),
                        AwakeningProfile.DEFAULT,
                        Identifier.of(SimplySwords.MOD_ID, "dormant_relic"))
                .basePresentation(
                        "item.simplyswords.dormant_relic",
                        AwakeningFormRarity.UNIQUE,
                        0.0F)
                .selectionLevel(4)
                .persistentProgression(true)
                .alias(ItemsRegistry.RIGHTEOUS_RELIC.get())
                .alias(ItemsRegistry.TAINTED_RELIC.get())
                .alias(ItemsRegistry.SUNFIRE.get())
                .alias(ItemsRegistry.HARBINGER.get())
                .route(SUN_ROUTE,
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "righteous_relic"),
                                4,
                                ItemsRegistry.DORMANT_RELIC.get(),
                                "item.simplyswords.righteous_relic",
                                AwakeningFormRarity.UNIQUE,
                                0.25F),
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "sunfire"),
                                8,
                                ItemsRegistry.DORMANT_RELIC.get(),
                                "item.simplyswords.sunfire",
                                AwakeningFormRarity.LEGENDARY,
                                0.75F))
                .route(HARBINGER_ROUTE,
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "tainted_relic"),
                                4,
                                ItemsRegistry.DORMANT_RELIC.get(),
                                "item.simplyswords.tainted_relic",
                                AwakeningFormRarity.UNIQUE,
                                0.5F),
                        new AwakeningFormStage(
                                Identifier.of(SimplySwords.MOD_ID, "harbinger"),
                                8,
                                ItemsRegistry.DORMANT_RELIC.get(),
                                "item.simplyswords.harbinger",
                                AwakeningFormRarity.LEGENDARY,
                                1.0F))
                .routeHandler(context -> {
                    ServerWorld overworld = context.world().getServer().getWorld(World.OVERWORLD);
                    return overworld == null || overworld.isDay() ? SUN_ROUTE : HARBINGER_ROUTE;
                })
                .build());

        builtinsRegistered = true;
    }

    private static boolean isDeepDarkForge(AwakeningFormContext context) {
        RegistryEntry<Biome> biome = context.world().getBiome(context.forgePos());
        return biome.matchesKey(BiomeKeys.DEEP_DARK);
    }

    public static Optional<AwakeningFormFamily> get(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(FAMILIES.get(stack.getItem()));
    }

    public static boolean isPersistentProgression(ItemStack stack) {
        return get(stack).map(AwakeningFormFamily::persistentProgression).orElse(false);
    }

    public static Optional<Identifier> getRoute(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        AwakeningRouteComponent route = stack.get(ComponentTypeRegistry.AWAKENING_ROUTE.get());
        if (route != null) return Optional.of(route.route());
        Optional<Identifier> legacyRoute = getLegacyRelicRoute(stack);
        if (legacyRoute.isPresent()) return legacyRoute;
        return get(stack)
                .filter(family -> family.routes().size() == 1
                        && AwakeningApi.getLevel(stack) >= family.selectionLevel())
                .map(family -> family.routes().keySet().iterator().next());
    }

    public static Optional<AwakeningFormStage> getStage(ItemStack stack) {
        Optional<AwakeningFormFamily> familyOptional = get(stack);
        if (familyOptional.isEmpty()) return Optional.empty();
        AwakeningFormFamily family = familyOptional.get();
        int level = AwakeningApi.getLevel(stack);
        if (level < family.selectionLevel()) return Optional.of(family.baseStage());
        Optional<Identifier> route = getRoute(stack);
        if (route.isEmpty() && family.routes().size() == 1) {
            route = Optional.of(family.routes().keySet().iterator().next());
        }
        return Optional.of(route
                .filter(family::containsRoute)
                .map(id -> family.resolveStage(id, level))
                .orElse(family.baseStage()));
    }

    public static void ensureInitialized(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.contains(ComponentTypeRegistry.AWAKENING_ROUTE.get())) {
            return;
        }
        Optional<Identifier> legacy = getLegacyRelicRoute(stack);
        if (legacy.isPresent()) {
            stack.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(), new AwakeningRouteComponent(legacy.get()));
            return;
        }
        get(stack).filter(family -> family.selectionLevel() == 0 && family.routes().size() == 1)
                .ifPresent(family -> stack.set(
                        ComponentTypeRegistry.AWAKENING_ROUTE.get(),
                        new AwakeningRouteComponent(family.routes().keySet().iterator().next())
                ));
    }

    public static ItemStack resolvePreview(
            ItemStack configuredStack,
            ItemStack sourceStack,
            int originalLevel,
            int targetLevel,
            ServerPlayerEntity player,
            BlockPos forgePos
    ) {
        Optional<AwakeningFormFamily> familyOptional = get(configuredStack);
        if (familyOptional.isEmpty()) return configuredStack;
        AwakeningFormFamily family = familyOptional.get();
        ItemStack result = configuredStack;

        if (targetLevel < family.selectionLevel()) {
            return toBaseForm(family, result, targetLevel);
        }

        Identifier route = getRoute(result)
                .filter(family::containsRoute)
                .orElse(null);
        if (route != null && family.transitionHandler() != null
                && player.getWorld() instanceof ServerWorld serverWorld) {
            Identifier transitioned = family.transitionHandler().transitionRoute(new AwakeningFormContext(
                    serverWorld, player, forgePos, sourceStack, originalLevel, targetLevel), route);
            if (transitioned != null && family.containsRoute(transitioned)) {
                route = transitioned;
            }
        }
        if (route == null) {
            if (!(player.getWorld() instanceof ServerWorld serverWorld)) return result;
            try {
                route = family.handler().selectRoute(new AwakeningFormContext(
                        serverWorld,
                        player,
                        forgePos,
                        sourceStack,
                        originalLevel,
                        targetLevel
                ));
            } catch (RuntimeException exception) {
                SimplySwords.LOGGER.error("Addon awakening route resolver failed for {}", family.baseItem(), exception);
                return toBaseForm(family, result, targetLevel);
            }
            if (route == null || !family.containsRoute(route)) {
                SimplySwords.LOGGER.error("Addon awakening route resolver returned an unknown route {} for {}",
                        route, family.baseItem());
                return toBaseForm(family, result, targetLevel);
            }
        }

        result.set(ComponentTypeRegistry.AWAKENING_ROUTE.get(), new AwakeningRouteComponent(route));
        syncLegacyRoute(family, result, route);
        AwakeningFormStage stage = family.resolveStage(route, targetLevel);
        if (result.getItem() != stage.item()) {
            result = result.copyComponentsToNewStack(stage.item(), result.getCount());
        }
        AwakeningApi.setLevel(result, targetLevel);
        clampDamage(result);
        return result;
    }

    public static void notifyCommitted(
            ItemStack sourceStack,
            ItemStack resultStack,
            int originalLevel,
            int targetLevel,
            ServerPlayerEntity player,
            BlockPos forgePos
    ) {
        AwakeningFormFamily family = get(sourceStack).or(() -> get(resultStack)).orElse(null);
        if (family == null || !(player.getWorld() instanceof ServerWorld serverWorld)) return;
        try {
            family.handler().onCommitted(new AwakeningFormCommitContext(
                    serverWorld,
                    player,
                    forgePos,
                    sourceStack,
                    resultStack,
                    originalLevel,
                    targetLevel
            ));
        } catch (RuntimeException exception) {
            SimplySwords.LOGGER.error("Addon awakening commit callback failed for {}", family.baseItem(), exception);
        }
    }

    private static void clampDamage(ItemStack stack) {
        if (!stack.isDamageable()) return;
        int maxDamage = stack.getMaxDamage();
        int damage = stack.getOrDefault(DataComponentTypes.DAMAGE, 0);
        stack.set(DataComponentTypes.DAMAGE, Math.clamp(damage, 0, Math.max(0, maxDamage - 1)));
    }

    private static ItemStack toBaseForm(
            AwakeningFormFamily family,
            ItemStack stack,
            int targetLevel
    ) {
        stack.remove(ComponentTypeRegistry.AWAKENING_ROUTE.get());
        clearLegacyRoute(family, stack);
        if (stack.getItem() != family.baseItem()) {
            stack = stack.copyComponentsToNewStack(family.baseItem(), stack.getCount());
        }
        if (family.baseItem() == ItemsRegistry.STORMSCALE.get()) {
            stack.remove(ComponentTypeRegistry.ION_CUBES.get());
        }
        AwakeningApi.setLevel(stack, targetLevel);
        clampDamage(stack);
        return stack;
    }

    private static Optional<Identifier> getLegacyRelicRoute(ItemStack stack) {
        if (!stack.isOf(ItemsRegistry.DORMANT_RELIC.get())) return Optional.empty();
        RelicAttunementComponent legacy = stack.get(
                ComponentTypeRegistry.RELIC_ATTUNEMENT.get());
        if (legacy == null) return Optional.empty();
        if (legacy.isSun()) return Optional.of(SUN_ROUTE);
        if (legacy.isHarbinger()) return Optional.of(HARBINGER_ROUTE);
        return Optional.empty();
    }

    private static void clearLegacyRoute(AwakeningFormFamily family, ItemStack stack) {
        if (family.baseItem() == ItemsRegistry.DORMANT_RELIC.get()) {
            stack.remove(ComponentTypeRegistry.RELIC_ATTUNEMENT.get());
        }
    }

    private static void syncLegacyRoute(
            AwakeningFormFamily family,
            ItemStack stack,
            Identifier route
    ) {
        if (family.baseItem() != ItemsRegistry.DORMANT_RELIC.get()) return;
        if (SUN_ROUTE.equals(route)) {
            stack.set(ComponentTypeRegistry.RELIC_ATTUNEMENT.get(),
                    new RelicAttunementComponent(RelicAttunementComponent.SUN));
        } else if (HARBINGER_ROUTE.equals(route)) {
            stack.set(ComponentTypeRegistry.RELIC_ATTUNEMENT.get(),
                    new RelicAttunementComponent(RelicAttunementComponent.HARBINGER));
        }
    }
}
