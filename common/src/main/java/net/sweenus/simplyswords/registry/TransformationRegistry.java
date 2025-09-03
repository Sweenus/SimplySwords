package net.sweenus.simplyswords.registry;

import net.minecraft.block.Blocks;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

public class TransformationRegistry {

    public static void register() {
        SimplySwordsAPI.registerTransformation(Blocks.SWEET_BERRY_BUSH, ItemsRegistry.BRAMBLETHORN.get());
        SimplySwordsAPI.registerTransformation(Blocks.FURNACE, ItemsRegistry.HEARTHFLAME.get());
        SimplySwordsAPI.registerTransformation(Blocks.CANDLE, ItemsRegistry.WICKPIERCER.get());
        SimplySwordsAPI.registerTransformation(Blocks.BOOKSHELF, ItemsRegistry.WAXWEAVER.get());
        SimplySwordsAPI.registerTransformation(Blocks.AMETHYST_BLOCK, ItemsRegistry.ARCANETHYST.get());
        SimplySwordsAPI.registerTransformation(Blocks.RED_WOOL, ItemsRegistry.RIBBONCLEAVER.get());
        SimplySwordsAPI.registerTransformation(Blocks.ICE, ItemsRegistry.FROSTFALL.get());
        SimplySwordsAPI.registerTransformation(Blocks.LAVA_CAULDRON, ItemsRegistry.MOLTEN_EDGE.get());
        SimplySwordsAPI.registerTransformation(Blocks.MAGMA_BLOCK, ItemsRegistry.BRIMSTONE_CLAYMORE.get());
        SimplySwordsAPI.registerTransformation(Blocks.FIRE, ItemsRegistry.EMBERLASH.get());
        SimplySwordsAPI.registerTransformation(Blocks.SNOW_BLOCK, ItemsRegistry.ICEWHISPER.get());
        SimplySwordsAPI.registerTransformation(Blocks.SCULK_SHRIEKER, ItemsRegistry.SHADOWSTING.get());
        SimplySwordsAPI.registerTransformation(Blocks.SPORE_BLOSSOM, ItemsRegistry.TOXIC_LONGSWORD.get());
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BLOCK, ItemsRegistry.MJOLNIR.get());
        SimplySwordsAPI.registerTransformation(Blocks.ANVIL, ItemsRegistry.STORMBRINGER.get());
        SimplySwordsAPI.registerTransformation(Blocks.BEE_NEST, ItemsRegistry.HIVEHEART.get());
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BARS, ItemsRegistry.TWISTED_BLADE.get());
        SimplySwordsAPI.registerTransformation(Blocks.CAMPFIRE, ItemsRegistry.EMBERBLADE.get());
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_LANTERN, ItemsRegistry.SOULKEEPER.get());
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SOIL, ItemsRegistry.SOULSTEALER.get());
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_CAMPFIRE, ItemsRegistry.SOULPYRE.get());
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SAND, ItemsRegistry.SOULRENDER.get());
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_FIRE, ItemsRegistry.SLUMBERING_LICHBLADE.get());
        SimplySwordsAPI.registerTransformation(Blocks.BLAST_FURNACE, ItemsRegistry.FLAMEWIND.get());
        SimplySwordsAPI.registerTransformation(Blocks.OBSIDIAN, ItemsRegistry.WATCHER_CLAYMORE.get());
        SimplySwordsAPI.registerTransformation(Blocks.CRYING_OBSIDIAN, ItemsRegistry.WATCHING_WARGLAIVE.get());
        SimplySwordsAPI.registerTransformation(Blocks.POWDER_SNOW, ItemsRegistry.LIVYATAN.get());
        SimplySwordsAPI.registerTransformation(Blocks.END_PORTAL_FRAME, ItemsRegistry.CAELESTIS.get());
        SimplySwordsAPI.registerTransformation(Blocks.TUBE_CORAL_BLOCK, ItemsRegistry.CHOMPOLOTL.get());
        SimplySwordsAPI.registerTransformation(Blocks.FIRE_CORAL_BLOCK, ItemsRegistry.TEMPEST.get());
        SimplySwordsAPI.registerTransformation(Blocks.SUSPICIOUS_SAND, ItemsRegistry.DORMANT_RELIC.get());
        SimplySwordsAPI.registerTransformation(Blocks.CYAN_BANNER, ItemsRegistry.WHISPERWIND.get());
        SimplySwordsAPI.registerTransformation(Blocks.SKELETON_SKULL, ItemsRegistry.WRAITHFANG.get());
        SimplySwordsAPI.registerTransformation(Blocks.CAULDRON, ItemsRegistry.THUNDERBRAND.get());
        SimplySwordsAPI.registerTransformation(Blocks.QUARTZ_BLOCK, ItemsRegistry.STARS_EDGE.get());
    }

}
