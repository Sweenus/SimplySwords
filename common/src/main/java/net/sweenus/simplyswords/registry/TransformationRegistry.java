package net.sweenus.simplyswords.registry;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

public class TransformationRegistry {

    public static void register() {
        SimplySwordsAPI.registerTransformation(Blocks.SWEET_BERRY_BUSH, Identifier.of("simplyswords", "bramblethorn"));
        SimplySwordsAPI.registerTransformation(Blocks.FURNACE, Identifier.of("simplyswords", "hearthflame"));
        SimplySwordsAPI.registerTransformation(Blocks.CANDLE, Identifier.of("simplyswords", "wickpiercer"));
        SimplySwordsAPI.registerTransformation(Blocks.BOOKSHELF, Identifier.of("simplyswords", "waxweaver"));
        SimplySwordsAPI.registerTransformation(Blocks.AMETHYST_BLOCK, Identifier.of("simplyswords", "arcanethyst"));
        SimplySwordsAPI.registerTransformation(Blocks.RED_WOOL, Identifier.of("simplyswords", "ribboncleaver"));
        SimplySwordsAPI.registerTransformation(Blocks.ICE, Identifier.of("simplyswords", "frostfall"));
        SimplySwordsAPI.registerTransformation(Blocks.LAVA_CAULDRON, Identifier.of("simplyswords", "molten_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.MAGMA_BLOCK, Identifier.of("simplyswords", "brimstone_claymore"));
        SimplySwordsAPI.registerTransformation(Blocks.FIRE, Identifier.of("simplyswords", "emberlash"));
        SimplySwordsAPI.registerTransformation(Blocks.SNOW_BLOCK, Identifier.of("simplyswords", "icewhisper"));
        SimplySwordsAPI.registerTransformation(Blocks.SCULK_SHRIEKER, Identifier.of("simplyswords", "shadowsting"));
        SimplySwordsAPI.registerTransformation(Blocks.SPORE_BLOSSOM, Identifier.of("simplyswords", "toxic_longsword"));
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BLOCK, Identifier.of("simplyswords", "mjolnir"));
        SimplySwordsAPI.registerTransformation(Blocks.ANVIL, Identifier.of("simplyswords", "stormbringer"));
        SimplySwordsAPI.registerTransformation(Blocks.BEE_NEST, Identifier.of("simplyswords", "hiveheart"));
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BARS, Identifier.of("simplyswords", "twisted_blade"));
        SimplySwordsAPI.registerTransformation(Blocks.CAMPFIRE, Identifier.of("simplyswords", "emberblade"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_LANTERN, Identifier.of("simplyswords", "soulkeeper"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SOIL, Identifier.of("simplyswords", "soulstealer"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_CAMPFIRE, Identifier.of("simplyswords", "soulpyre"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SAND, Identifier.of("simplyswords", "soulrender"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_FIRE, Identifier.of("simplyswords", "slumbering_lichblade"));
        SimplySwordsAPI.registerTransformation(Blocks.BLAST_FURNACE, Identifier.of("simplyswords", "flamewind"));
        SimplySwordsAPI.registerTransformation(Blocks.OBSIDIAN, Identifier.of("simplyswords", "watcher_claymore"));
        SimplySwordsAPI.registerTransformation(Blocks.CRYING_OBSIDIAN, Identifier.of("simplyswords", "watching_warglaive"));
        SimplySwordsAPI.registerTransformation(Blocks.POWDER_SNOW, Identifier.of("simplyswords", "livyatan"));
        SimplySwordsAPI.registerTransformation(Blocks.END_PORTAL_FRAME, Identifier.of("simplyswords", "caelestis"));
        SimplySwordsAPI.registerTransformation(Blocks.TUBE_CORAL_BLOCK, Identifier.of("simplyswords", "chompolotl"));
        SimplySwordsAPI.registerTransformation(Blocks.FIRE_CORAL_BLOCK, Identifier.of("simplyswords", "tempest"));
        SimplySwordsAPI.registerTransformation(Blocks.SUSPICIOUS_SAND, Identifier.of("simplyswords", "dormant_relic"));
        SimplySwordsAPI.registerTransformation(Blocks.CYAN_BANNER, Identifier.of("simplyswords", "whisperwind"));
        SimplySwordsAPI.registerTransformation(Blocks.SKELETON_SKULL, Identifier.of("simplyswords", "wraithfang"));
        SimplySwordsAPI.registerTransformation(Blocks.CAULDRON, Identifier.of("simplyswords", "thunderbrand"));
        SimplySwordsAPI.registerTransformation(Blocks.QUARTZ_BLOCK, Identifier.of("simplyswords", "stars_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.COPPER_BULB, Identifier.of("simplyswords", "storms_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.LIGHTNING_ROD, Identifier.of("simplyswords", "stormscale"));
        SimplySwordsAPI.registerTransformation(Blocks.NETHER_WART_BLOCK, Identifier.of("simplyswords", "bloodwake"));
    }

}
