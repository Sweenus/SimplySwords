package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.entity.BattleStandardDarkEntity;
import net.sweenus.simplyswords.entity.BattleStandardEntity;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(SimplySwords.MOD_ID, Registry.ENTITY_TYPE_KEY);

    public static final RegistrySupplier<EntityType<BattleStandardEntity>> BATTLESTANDARD = ENTITIES.register("battlestandard", BattleStandardEntity.TYPE);
    public static final RegistrySupplier<EntityType<BattleStandardDarkEntity>> BATTLESTANDARDDARK = ENTITIES.register("battlestandarddark", BattleStandardDarkEntity.TYPE);


}
