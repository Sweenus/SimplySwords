package net.sweenus.simplyswords.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.block.RunicForgeBlock;

public final class BlocksRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(SimplySwords.MOD_ID, RegistryKeys.BLOCK);

    public static final RegistrySupplier<RunicForgeBlock> RUNIC_FORGE = BLOCKS.register(
            "runic_forge",
            () -> new RunicForgeBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
                    .strength(3.5F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE)
                    .luminance(state -> 7))
    );

    private BlocksRegistry() {
    }
}
