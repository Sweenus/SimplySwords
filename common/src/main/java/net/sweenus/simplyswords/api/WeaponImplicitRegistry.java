package net.sweenus.simplyswords.api;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.entity.DancingBladeVisualEntity;
import net.sweenus.simplyswords.entity.ThrownSpearEntity;
import net.sweenus.simplyswords.entity.ThrownSwordEntity;
import net.sweenus.simplyswords.item.UniqueWeaponItem;
import net.sweenus.simplyswords.item.component.WeaponImplicitComponent;
import net.sweenus.simplyswords.registry.ComponentTypeRegistry;
import net.sweenus.simplyswords.registry.EffectRegistry;
import net.sweenus.simplyswords.registry.ParticlesRegistry;
import net.sweenus.simplyswords.registry.SoundRegistry;
import net.sweenus.simplyswords.util.BleedHelper;
import net.sweenus.simplyswords.world.ImplicitStatusVisualManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class WeaponImplicitRegistry {
    private static final String CUTLASS_PLUNDERED_TAG = "simplyswords_cutlass_plundered";

    private static final Identifier RAPIER = id("rapier");
    private static final Identifier CUTLASS = id("cutlass");
    private static final Identifier SAI = id("sai");
    private static final Identifier DAGGER = id("dagger");
    private static final Identifier CLAYMORE = id("claymore");
    private static final Identifier LONGSWORD = id("longsword");
    private static final Identifier GREATHAMMER = id("greathammer");
    private static final Identifier HAMMER = id("hammer");
    private static final Identifier KATANA = id("katana");
    private static final Identifier SPEAR = id("spear");
    private static final Identifier GLAIVE = id("glaive");
    private static final Identifier HALBERD = id("halberd");
    private static final Identifier WARGLAIVE = id("warglaive");
    private static final Identifier CHAKRAM = id("chakram");
    private static final Identifier SCYTHE = id("scythe");
    private static final Identifier GREATAXE = id("greataxe");
    private static final Identifier TWINBLADE = id("twinblade");

    private static final Map<Identifier, WeaponImplicitDefinition> DEFINITIONS_BY_ID = new HashMap<>();
    private static final Map<Identifier, WeaponImplicitDefinition> DEFINITIONS_BY_TYPE = new HashMap<>();
    private static final Map<Identifier, Identifier> ITEM_TYPES = new HashMap<>();
    private static final List<TagRegistration> TAG_TYPES = new ArrayList<>();
    private static final ThreadLocal<Boolean> SUPPRESS_IMPLICITS = ThreadLocal.withInitial(() -> false);

    private WeaponImplicitRegistry() {
    }

    public static void registerBuiltins() {
        registerWeaponImplicit(chanceDefinition(id("armor_pierce"), RAPIER, 15, 35, "armor_pierce", WeaponImplicitRegistry::modifyArmorPierceDamage, null, null));
        registerWeaponImplicit(chanceDefinition(id("spear_armor_pierce"), SPEAR, 15, 35, "armor_pierce", WeaponImplicitRegistry::modifyArmorPierceDamage, null, null));
        registerWeaponImplicit(chanceDefinition(id("cutlass_plunder"), CUTLASS, 1, 3, "plunder", null, WeaponImplicitRegistry::applyPlunder, null));
        registerWeaponImplicit(chanceDefinition(id("glaive_bleed"), GLAIVE, 10, 60, "bleed", null, WeaponImplicitRegistry::applyBleed, null));
        registerWeaponImplicit(percentDefinition(id("backstab"), SAI, 20, 40, "backstab", WeaponImplicitRegistry::modifyBackstabDamage, null));
        registerWeaponImplicit(percentDefinition(id("dagger_backstab"), DAGGER, 20, 40, "backstab", WeaponImplicitRegistry::modifyBackstabDamage, null));
        registerWeaponImplicit(chanceDefinition(id("claymore_deflect"), CLAYMORE, 5, 15, "deflect", null, null, WeaponImplicitRegistry::tryDeflect));
        registerWeaponImplicit(chanceDefinition(id("longsword_deflect"), LONGSWORD, 5, 15, "deflect", null, null, WeaponImplicitRegistry::tryDeflect));
        registerWeaponImplicit(percentDefinition(id("greathammer_sunder"), GREATHAMMER, 2, 10, "sunder", null, WeaponImplicitRegistry::applySunder));
        registerWeaponImplicit(percentDefinition(id("hammer_sunder"), HAMMER, 2, 6, "sunder", null, WeaponImplicitRegistry::applySunder));
        registerWeaponImplicit(chanceDefinition(id("katana_double_damage"), KATANA, 5, 15, "double_damage", WeaponImplicitRegistry::modifyDoubleDamage, null, null));
        registerWeaponImplicit(chanceDefinition(id("chakram_haste"), CHAKRAM, 5, 25, "chakram_haste", null, WeaponImplicitRegistry::applyChakramHaste, null));
        registerWeaponImplicit(chanceDefinition(id("scythe_execute"), SCYTHE, 5, 15, "execute", WeaponImplicitRegistry::modifyExecuteDamage, null, null));
        registerWeaponImplicit(chanceDefinition(id("greataxe_bleed"), GREATAXE, 10, 60, "bleed", null, WeaponImplicitRegistry::applyBleed, null));
        registerWeaponImplicit(chanceDefinition(id("halberd_bleed"), HALBERD, 10, 60, "bleed", null, WeaponImplicitRegistry::applyBleed, null));
        registerWeaponImplicit(chanceDefinition(id("twinblade_haste"), TWINBLADE, 5, 25, "twinblade_haste", null, WeaponImplicitRegistry::applyChakramHaste, null));
        registerWeaponImplicit(chanceDefinition(id("warglaive_double_strike"), WARGLAIVE, 5, 15, "double_strike", null, WeaponImplicitRegistry::applyDoubleStrike, null));

        registerUniqueOverrides();
        for (Identifier type : List.of(RAPIER, CUTLASS, SAI, DAGGER, CLAYMORE, LONGSWORD, GREATHAMMER, HAMMER, KATANA, SPEAR, GLAIVE, HALBERD, WARGLAIVE, CHAKRAM, SCYTHE, GREATAXE, TWINBLADE)) {
            registerWeaponType(TagKey.of(RegistryKeys.ITEM, new Identifier(SimplySwords.MOD_ID, "implicit/" + type.getPath())), type);
        }
    }

    public static void registerWeaponImplicit(WeaponImplicitDefinition definition) {
        DEFINITIONS_BY_ID.put(definition.id(), definition);
        DEFINITIONS_BY_TYPE.put(definition.weaponType(), definition);
    }

    public static void registerWeaponType(Item item, Identifier weaponType) {
        ITEM_TYPES.put(Registries.ITEM.getId(item), normalizeType(weaponType));
    }

    public static void registerWeaponType(TagKey<Item> itemTag, Identifier weaponType) {
        TAG_TYPES.add(new TagRegistration(itemTag, normalizeType(weaponType)));
    }

    public static Optional<WeaponImplicitComponent> getOrCreateWeaponImplicit(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !Config.general.enableWeaponImplicits) {
            return Optional.empty();
        }

        Identifier weaponType = resolveWeaponType(stack);
        if (weaponType == null) {
            return Optional.empty();
        }

        WeaponImplicitDefinition definition = DEFINITIONS_BY_TYPE.get(weaponType);
        if (definition == null) {
            return Optional.empty();
        }

        WeaponImplicitComponent existing = ComponentTypeRegistry.WEAPON_IMPLICIT.get(stack);
        if (existing != null && existing.implicitId().equals(definition.id()) && existing.weaponType().equals(definition.weaponType())) {
            return Optional.of(existing);
        }

        int value = rollImplicitValue(stack, definition);
        WeaponImplicitComponent component = new WeaponImplicitComponent(definition.id(), definition.weaponType(), value);
        ComponentTypeRegistry.WEAPON_IMPLICIT.set(stack, component);
        return Optional.of(component);
    }

    public static Optional<WeaponImplicitComponent> peekWeaponImplicit(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !Config.general.enableWeaponImplicits) {
            return Optional.empty();
        }
        WeaponImplicitComponent component = ComponentTypeRegistry.WEAPON_IMPLICIT.get(stack);
        return component == null ? Optional.empty() : Optional.of(component);
    }

    public static Text formatTooltip(ItemStack stack) {
        return formatTooltip(stack, false);
    }

    // Read-only. Tooltips render on the client every frame, so this must never write a
    // component to the stack — that would roll a fresh random value from the render thread
    // and desync the stack from the server and from any storage mod tracking it.
    // The stack gets its implicit from inventoryTick / onCraft / onClicked instead; until
    // then the tooltip shows the range preview.
    public static Text formatTooltip(ItemStack stack, boolean includeRange) {
        Optional<WeaponImplicitComponent> component = peekWeaponImplicit(stack);
        WeaponImplicitDefinition currentDefinition = resolveDefinition(stack);
        if (component.isPresent()) {
            WeaponImplicitDefinition definition = DEFINITIONS_BY_ID.get(component.get().implicitId());
            if (definition != null && currentDefinition != null && componentMatchesDefinition(component.get(), currentDefinition)) {
                Text text = definition.formatTooltip(component.get());
                return includeRange ? appendRange(text, definition) : text;
            }
        }

        // No implicit yet, or the stored one is stale for this weapon type.
        return currentDefinition == null ? null : formatRangePreview(currentDefinition);
    }

    public static List<Text> buildTooltipLines(ItemStack stack) {
        Text line = formatTooltip(stack);
        if (line == null) {
            return List.of();
        }
        return List.of(Text.literal(""), Text.translatable("tooltip.simplyswords.implicit.header"), line);
    }

    public static List<Text> buildTooltipLines(ItemStack stack, boolean includeRange) {
        Text line = formatTooltip(stack, includeRange);
        if (line == null) {
            return List.of();
        }
        return List.of(Text.literal(""), Text.translatable("tooltip.simplyswords.implicit.header"), line);
    }

    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (SUPPRESS_IMPLICITS.get() || target.getWorld().isClient() || !Config.general.enableWeaponImplicits) {
            return amount;
        }

        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        return modifyDamage(getSourceWeaponStack(source, attacker), target, source, amount);
    }

    public static float modifyDamage(ItemStack stack, LivingEntity target, DamageSource source, float amount) {
        if (SUPPRESS_IMPLICITS.get() || target.getWorld().isClient() || !Config.general.enableWeaponImplicits) {
            return amount;
        }

        Optional<WeaponImplicitComponent> component = getOrCreateWeaponImplicit(stack);
        if (component.isEmpty()) {
            return amount;
        }

        WeaponImplicitDefinition definition = DEFINITIONS_BY_ID.get(component.get().implicitId());
        if (definition == null || definition.damageHandler() == null) {
            return amount;
        }
        return definition.damageHandler().modify(stack, component.get(), target, source, amount);
    }

    public static void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker, float damage) {
        if (SUPPRESS_IMPLICITS.get() || target.getWorld().isClient() || !Config.general.enableWeaponImplicits) {
            return;
        }
        Optional<WeaponImplicitComponent> component = getOrCreateWeaponImplicit(stack);
        if (component.isEmpty()) {
            return;
        }
        WeaponImplicitDefinition definition = DEFINITIONS_BY_ID.get(component.get().implicitId());
        if (definition != null && definition.hitHandler() != null) {
            definition.hitHandler().onHit(stack, component.get(), target, attacker, damage);
        }
    }

    public static void onDamageApplied(LivingEntity target, DamageSource source, float damage) {
        if (!(source.getAttacker() instanceof LivingEntity attacker)) {
            return;
        }
        onHit(getSourceWeaponStack(source, attacker), target, attacker, damage);
    }

    public static boolean tryDeflectIncomingDamage(LivingEntity bearer, DamageSource source, float amount) {
        if (SUPPRESS_IMPLICITS.get() || bearer.getWorld().isClient() || !Config.general.enableWeaponImplicits
                || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        for (ItemStack stack : List.of(bearer.getMainHandStack(), bearer.getOffHandStack())) {
            Optional<WeaponImplicitComponent> component = getOrCreateWeaponImplicit(stack);
            if (component.isEmpty()) {
                continue;
            }
            WeaponImplicitDefinition definition = DEFINITIONS_BY_ID.get(component.get().implicitId());
            if (definition != null && definition.incomingDamageHandler() != null
                    && definition.incomingDamageHandler().shouldCancel(stack, component.get(), bearer, source, amount)) {
                return true;
            }
        }
        return false;
    }

    public static void runSuppressed(Runnable runnable) {
        SUPPRESS_IMPLICITS.set(true);
        try {
            runnable.run();
        } finally {
            SUPPRESS_IMPLICITS.set(false);
        }
    }

    public static void spawnBleedParticles(LivingEntity target, int stacks, boolean tickPulse) {
        if (!(target.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        int cappedStacks = net.minecraft.util.math.MathHelper.clamp(stacks, 1, 10);
        int dropletCount = tickPulse ? 3 + cappedStacks : 7 + cappedStacks * 2;
        double y = target.getBodyY(tickPulse ? 0.74 : 0.8);
        serverWorld.spawnParticles(ParticlesRegistry.DRIPPING_BLOOD.get(), target.getX(), y, target.getZ(), dropletCount, 0.28, 0.18, 0.28, 0.065);
    }

    private static WeaponImplicitDefinition chanceDefinition(Identifier id, Identifier weaponType, int min, int max, String key,
                                                            WeaponImplicitDefinition.DamageHandler damage,
                                                            WeaponImplicitDefinition.HitHandler hit,
                                                            WeaponImplicitDefinition.IncomingDamageHandler incoming) {
        return new WeaponImplicitDefinition(id, weaponType, min, max, damage, hit, incoming,
                component -> Text.translatable("tooltip.simplyswords.implicit." + key, component.value()));
    }

    private static WeaponImplicitDefinition percentDefinition(Identifier id, Identifier weaponType, int min, int max, String key,
                                                             WeaponImplicitDefinition.DamageHandler damage,
                                                             WeaponImplicitDefinition.HitHandler hit) {
        return chanceDefinition(id, weaponType, min, max, key, damage, hit, null);
    }

    private static WeaponImplicitDefinition resolveDefinition(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !Config.general.enableWeaponImplicits) {
            return null;
        }
        Identifier weaponType = resolveWeaponType(stack);
        return weaponType == null ? null : DEFINITIONS_BY_TYPE.get(weaponType);
    }

    private static Text appendRange(Text text, WeaponImplicitDefinition definition) {
        return Text.empty().append(text).append(" [" + definition.minValue() + "-" + definition.maxValue() + "]");
    }

    private static Text formatRangePreview(WeaponImplicitDefinition definition) {
        return Text.translatable("tooltip.simplyswords.implicit." + tooltipKey(definition), definition.minValue() + "-" + definition.maxValue());
    }

    private static int rollImplicitValue(ItemStack stack, WeaponImplicitDefinition definition) {
        int min = definition.minValue();
        int max = definition.maxValue();
        if (stack.getItem() instanceof UniqueWeaponItem) {
            int rangeSize = max - min + 1;
            int topRollCount = Math.max(1, (int) Math.ceil(rangeSize * 0.1D));
            min = max - topRollCount + 1;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static boolean componentMatchesDefinition(WeaponImplicitComponent component, WeaponImplicitDefinition definition) {
        return component.implicitId().equals(definition.id()) && component.weaponType().equals(definition.weaponType());
    }

    private static String tooltipKey(WeaponImplicitDefinition definition) {
        String id = definition.id().getPath();
        if (id.contains("armor_pierce")) {
            return "armor_pierce";
        }
        if (id.contains("bleed")) {
            return "bleed";
        }
        if (id.contains("plunder")) {
            return "plunder";
        }
        if (id.contains("backstab")) {
            return "backstab";
        }
        if (id.contains("deflect")) {
            return "deflect";
        }
        if (id.contains("sunder")) {
            return "sunder";
        }
        if (id.contains("double_damage")) {
            return "double_damage";
        }
        if (id.contains("double_strike")) {
            return "double_strike";
        }
        if (id.contains("haste")) {
            return "twinblade_haste";
        }
        if (id.contains("execute")) {
            return "execute";
        }
        return "generic";
    }

    private static float modifyArmorPierceDamage(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, DamageSource source, float amount) {
        if (missedRoll(target, component.value())) {
            return amount;
        }
        float armorCompensation = Math.min(amount * 1.5F, target.getArmor() * 0.18F);
        spawnProcParticles(target, ParticleTypes.ENCHANTED_HIT);
        return amount + armorCompensation;
    }

    private static float modifyBackstabDamage(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, DamageSource source, float amount) {
        LivingEntity attacker = source.getAttacker() instanceof LivingEntity living ? living : null;
        if (attacker == null || !isBehindTarget(attacker, target)) {
            return amount;
        }
        spawnProcParticles(target, ParticleTypes.CRIT);
        return amount * (1.0F + component.value() / 100.0F);
    }

    private static float modifyDoubleDamage(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, DamageSource source, float amount) {
        if (missedRoll(target, component.value())) {
            return amount;
        }
        spawnProcParticles(target, ParticleTypes.CRIT);
        return amount * 2.0F;
    }

    private static float modifyExecuteDamage(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, DamageSource source, float amount) {
        float executeThreshold = target.getMaxHealth() * 0.1F;
        if (target.getHealth() - amount > executeThreshold || missedRoll(target, component.value())) {
            return amount;
        }
        spawnProcParticles(target, ParticleTypes.SOUL);
        return Math.max(amount, target.getHealth() + target.getAbsorptionAmount());
    }

    private static void applyBleed(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage) {
        if (missedRoll(target, component.value())) {
            return;
        }
        BleedHelper.apply(target, attacker, damage);
    }

    private static void applyPlunder(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage) {
        if (!(target.getWorld() instanceof ServerWorld serverWorld) || target.getCommandTags().contains(CUTLASS_PLUNDERED_TAG) || missedRoll(target, component.value())) {
            return;
        }

        DamageSource source = attacker instanceof PlayerEntity player
                ? attacker.getDamageSources().playerAttack(player)
                : attacker.getDamageSources().mobAttack(attacker);
        Identifier lootTableId = target.getLootTable();
        LootTable lootTable = serverWorld.getServer().getLootManager().getLootTable(lootTableId);
        LootContextParameterSet.Builder contextBuilder = new LootContextParameterSet.Builder(serverWorld)
                .add(LootContextParameters.THIS_ENTITY, target)
                .add(LootContextParameters.ORIGIN, target.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, source)
                .addOptional(LootContextParameters.KILLER_ENTITY, attacker)
                .addOptional(LootContextParameters.DIRECT_KILLER_ENTITY, attacker);
        if (attacker instanceof PlayerEntity player) {
            contextBuilder.addOptional(LootContextParameters.LAST_DAMAGE_PLAYER, player);
            contextBuilder.luck(player.getLuck());
        }

        List<ItemStack> loot = lootTable.generateLoot(contextBuilder.build(LootContextTypes.ENTITY), target.getLootTableSeed()).stream()
                .filter(generatedStack -> !generatedStack.isEmpty())
                .toList();
        if (loot.isEmpty()) {
            return;
        }

        ItemStack plundered = loot.get(target.getRandom().nextInt(loot.size())).copy();
        ItemEntity itemEntity = target.dropStack(plundered, target.getHeight() * 0.55F);
        if (itemEntity == null) {
            return;
        }
        Vec3d away = target.getPos().subtract(attacker.getPos());
        if (away.lengthSquared() < 0.001) {
            away = new Vec3d(target.getRandom().nextDouble() - 0.5, 0.0, target.getRandom().nextDouble() - 0.5);
        }
        Vec3d velocity = away.normalize().multiply(0.18).add(0.0, 0.28, 0.0);
        itemEntity.setVelocity(velocity);
        itemEntity.velocityModified = true;
        target.addCommandTag(CUTLASS_PLUNDERED_TAG);
        serverWorld.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.65F, 1.45F);
        serverWorld.playSound(null, target.getBlockPos(), SoundRegistry.MAGIC_SWORD_SPELL_02.get(), SoundCategory.PLAYERS, 0.35F, 1.7F);
        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getBodyY(0.65), target.getZ(), 8, 0.28, 0.25, 0.28, 0.04);
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, target.getX(), target.getBodyY(0.65), target.getZ(), 12, 0.35, 0.3, 0.35, 0.08);
    }

    private static void applySunder(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage) {
        StatusEffectInstance currentSunder = target.getStatusEffect(EffectRegistry.getReference(EffectRegistry.SUNDERED_ARMOR));
        int current = currentSunder == null ? 0 : currentSunder.getAmplifier() + 1;
        int next = Math.min(50, current + component.value());
        target.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.SUNDERED_ARMOR), 200, Math.max(0, next - 1), false, false, true), attacker);
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            ImplicitStatusVisualManager.refresh(serverWorld, target);
            serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.58), target.getZ(), 8, 0.28, 0.18, 0.28, 0.05);
            serverWorld.spawnParticles(ParticleTypes.POOF, target.getX(), target.getBodyY(0.58), target.getZ(), 4, 0.22, 0.14, 0.22, 0.02);
        }
        spawnProcParticles(target, ParticleTypes.ANGRY_VILLAGER);
    }

    private static void applyChakramHaste(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage) {
        if (missedRoll(attacker, component.value())) {
            return;
        }
        StatusEffectInstance currentHaste = attacker.getStatusEffect(EffectRegistry.getReference(EffectRegistry.IMPLICIT_HASTE));
        int current = currentHaste == null ? 0 : currentHaste.getAmplifier() + 1;
        int next = Math.min(5, current + 1);
        attacker.addStatusEffect(new StatusEffectInstance(EffectRegistry.getReference(EffectRegistry.IMPLICIT_HASTE), 120, next - 1, false, false, true), attacker);
        spawnProcParticles(attacker, ParticleTypes.ENCHANT);
    }

    private static void applyDoubleStrike(ItemStack stack, WeaponImplicitComponent component, LivingEntity target, LivingEntity attacker, float damage) {
        if (!target.isAlive() || missedRoll(target, component.value())) {
            return;
        }
        DamageSource source = attacker instanceof PlayerEntity player
                ? attacker.getDamageSources().playerAttack(player)
                : attacker.getDamageSources().mobAttack(attacker);
        target.timeUntilRegen = 0;
        runSuppressed(() -> target.damage(source, damage));
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.45F, 1.35F);
            serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.55), target.getZ(), 1, 0.12, 0.08, 0.12, 0.0);
            serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55), target.getZ(), 8, 0.25, 0.18, 0.25, 0.05);
        }
    }

    private static boolean tryDeflect(ItemStack stack, WeaponImplicitComponent component, LivingEntity bearer, DamageSource source, float amount) {
        if (missedRoll(bearer, component.value())) {
            return false;
        }
        if (bearer.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, bearer.getBlockPos(), SoundRegistry.MAGIC_SWORD_PARRY_01.get(), SoundCategory.PLAYERS, 0.55F, 1.25F);
            serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, bearer.getX(), bearer.getBodyY(0.55), bearer.getZ(), 8, 0.35, 0.25, 0.35, 0.03);
        }
        return true;
    }

    private static boolean missedRoll(LivingEntity entity, int chance) {
        return chance <= 0 || entity.getRandom().nextInt(100) >= chance;
    }

    private static boolean isBehindTarget(LivingEntity attacker, LivingEntity target) {
        Vec3d targetFacing = target.getRotationVec(1.0F).normalize();
        Vec3d targetToAttacker = attacker.getPos().subtract(target.getPos()).normalize();
        return targetFacing.dotProduct(targetToAttacker) < -0.45;
    }

    private static ItemStack getSourceWeaponStack(DamageSource source, LivingEntity attacker) {
        if (source.getSource() instanceof ThrownSwordEntity thrownSword) {
            return thrownSword.getWeaponStack();
        }
        if (source.getSource() instanceof ThrownSpearEntity thrownSpear) {
            return thrownSpear.getWeaponStack();
        }
        if (source.getSource() instanceof DancingBladeVisualEntity dancingBlade) {
            return dancingBlade.getWeaponStack();
        }
        return attacker == null ? ItemStack.EMPTY : attacker.getMainHandStack();
    }

    private static Identifier resolveWeaponType(ItemStack stack) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        Identifier explicit = ITEM_TYPES.get(itemId);
        if (explicit != null) {
            return explicit;
        }
        for (TagRegistration tagRegistration : TAG_TYPES) {
            if (stack.isIn(tagRegistration.tag())) {
                return tagRegistration.weaponType();
            }
        }
        if (!(stack.getItem() instanceof SwordItem)) {
            return null;
        }
        String path = itemId.getPath();
        return resolvePathType(path);
    }

    private static void registerUniqueOverrides() {
        registerPath("watcher_claymore", CLAYMORE);
        registerPath("brimstone_claymore", CLAYMORE);
        registerPath("storms_edge", TWINBLADE);
        registerPath("stormbringer", LONGSWORD);
        registerPath("bramblethorn", RAPIER);
        registerPath("watching_warglaive", WARGLAIVE);
        registerPath("toxic_longsword", LONGSWORD);
        registerPath("emberblade", LONGSWORD);
        registerPath("frostfall", HAMMER);
        registerPath("soulpyre", GREATAXE);
        registerPath("molten_edge", GREATAXE);
        registerPath("livyatan", GREATAXE);
        registerPath("icewhisper", HALBERD);
        registerPath("arcanethyst", HALBERD);
        registerPath("thunderbrand", HALBERD);
        registerPath("hearthflame", GREATHAMMER);
        registerPath("twisted_blade", CLAYMORE);
        registerPath("soulrender", SCYTHE);
        registerPath("soulkeeper", GREATHAMMER);
        registerPath("soulstealer", DAGGER);
        registerPath("mjolnir", HAMMER);
        registerPath("slumbering_lichblade", CLAYMORE);
        registerPath("waking_lichblade", CLAYMORE);
        registerPath("awakened_lichblade", CLAYMORE);
        registerPath("shadowsting", RAPIER);
        registerPath("dormant_relic", LONGSWORD);
        registerPath("tainted_relic", LONGSWORD);
        registerPath("righteous_relic", LONGSWORD);
        registerPath("sunfire", LONGSWORD);
        registerPath("harbinger", LONGSWORD);
        registerPath("whisperwind", KATANA);
        registerPath("emberlash", DAGGER);
        registerPath("waxweaver", CLAYMORE);
        registerPath("hiveheart", GREATHAMMER);
        registerPath("stars_edge", TWINBLADE);
        registerPath("sword_on_a_stick", SPEAR);
        registerPath("wickpiercer", SPEAR);
        registerPath("tempest", CHAKRAM);
        registerPath("flamewind", GLAIVE);
        registerPath("stormscale", GLAIVE);
        registerPath("ribboncleaver", CLAYMORE);
        registerPath("magiscythe", SCYTHE);
        registerPath("magiblade", TWINBLADE);
        registerPath("magispear", SPEAR);
        registerPath("enigma", CLAYMORE);
        registerPath("caelestis", CLAYMORE);
        registerPath("wraithfang", CUTLASS);
        registerPath("bloodwake", CUTLASS);
        registerPath("chompolotl", CHAKRAM);
        registerPath("dreadtide", TWINBLADE);
    }

    private static void registerPath(String path, Identifier weaponType) {
        ITEM_TYPES.put(new Identifier(SimplySwords.MOD_ID, path), weaponType);
    }

    private static Identifier resolvePathType(String path) {
        for (Identifier type : List.of(GREATHAMMER, GREATAXE, TWINBLADE, LONGSWORD, WARGLAIVE, CLAYMORE, CHAKRAM, CUTLASS, RAPIER, KATANA, SCYTHE, HALBERD, GLAIVE, HAMMER, SPEAR, DAGGER, SAI)) {
            if (path.endsWith("_" + type.getPath()) || path.equals(type.getPath()) || path.contains("/" + type.getPath())) {
                return type;
            }
        }
        return null;
    }

    private static Identifier normalizeType(Identifier weaponType) {
        return new Identifier(weaponType.getNamespace(), weaponType.getPath());
    }

    private static Identifier id(String path) {
        return new Identifier(SimplySwords.MOD_ID, path);
    }

    private static void spawnProcParticles(LivingEntity entity, net.minecraft.particle.ParticleEffect particle) {
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(particle, entity.getX(), entity.getBodyY(0.55), entity.getZ(), 6, 0.28, 0.22, 0.28, 0.025);
        }
    }

    private record TagRegistration(TagKey<Item> tag, Identifier weaponType) {
    }
}
