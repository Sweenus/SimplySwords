package net.sweenus.simplyswords.registry;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.sweenus.simplyswords.api.SimplySwordsAPI;

public class TransformationRegistry {

    public static void register() {
        SimplySwordsAPI.registerTransformation(Blocks.SWEET_BERRY_BUSH, new Identifier("simplyswords", "bramblethorn"));
        SimplySwordsAPI.registerTransformation(Blocks.FURNACE, new Identifier("simplyswords", "hearthflame"));
        SimplySwordsAPI.registerTransformation(Blocks.CANDLE, new Identifier("simplyswords", "wickpiercer"));
        SimplySwordsAPI.registerTransformation(Blocks.BOOKSHELF, new Identifier("simplyswords", "waxweaver"));
        SimplySwordsAPI.registerTransformation(Blocks.AMETHYST_BLOCK, new Identifier("simplyswords", "arcanethyst"));
        SimplySwordsAPI.registerTransformation(Blocks.RED_WOOL, new Identifier("simplyswords", "ribboncleaver"));
        SimplySwordsAPI.registerTransformation(Blocks.ICE, new Identifier("simplyswords", "frostfall"));
        SimplySwordsAPI.registerTransformation(Blocks.LAVA_CAULDRON, new Identifier("simplyswords", "molten_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.MAGMA_BLOCK, new Identifier("simplyswords", "brimstone_claymore"));
        SimplySwordsAPI.registerTransformation(Blocks.FIRE, new Identifier("simplyswords", "emberlash"));
        SimplySwordsAPI.registerTransformation(Blocks.SNOW_BLOCK, new Identifier("simplyswords", "icewhisper"));
        SimplySwordsAPI.registerTransformation(Blocks.SCULK_SHRIEKER, new Identifier("simplyswords", "shadowsting"));
        SimplySwordsAPI.registerTransformation(Blocks.SPORE_BLOSSOM, new Identifier("simplyswords", "toxic_longsword"));
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BLOCK, new Identifier("simplyswords", "mjolnir"));
        SimplySwordsAPI.registerTransformation(Blocks.ANVIL, new Identifier("simplyswords", "stormbringer"));
        SimplySwordsAPI.registerTransformation(Blocks.BEE_NEST, new Identifier("simplyswords", "hiveheart"));
        SimplySwordsAPI.registerTransformation(Blocks.IRON_BARS, new Identifier("simplyswords", "twisted_blade"));
        SimplySwordsAPI.registerTransformation(Blocks.CAMPFIRE, new Identifier("simplyswords", "emberblade"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_LANTERN, new Identifier("simplyswords", "soulkeeper"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SOIL, new Identifier("simplyswords", "soulstealer"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_CAMPFIRE, new Identifier("simplyswords", "soulpyre"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_SAND, new Identifier("simplyswords", "soulrender"));
        SimplySwordsAPI.registerTransformation(Blocks.SOUL_FIRE, new Identifier("simplyswords", "slumbering_lichblade"));
        SimplySwordsAPI.registerTransformation(Blocks.BLAST_FURNACE, new Identifier("simplyswords", "flamewind"));
        SimplySwordsAPI.registerTransformation(Blocks.OBSIDIAN, new Identifier("simplyswords", "watcher_claymore"));
        SimplySwordsAPI.registerTransformation(Blocks.CRYING_OBSIDIAN, new Identifier("simplyswords", "watching_warglaive"));
        SimplySwordsAPI.registerTransformation(Blocks.POWDER_SNOW, new Identifier("simplyswords", "livyatan"));
        SimplySwordsAPI.registerTransformation(Blocks.END_PORTAL_FRAME, new Identifier("simplyswords", "caelestis"));
        SimplySwordsAPI.registerTransformation(Blocks.TUBE_CORAL_BLOCK, new Identifier("simplyswords", "chompolotl"));
        SimplySwordsAPI.registerTransformation(Blocks.FIRE_CORAL_BLOCK, new Identifier("simplyswords", "tempest"));
        SimplySwordsAPI.registerTransformation(Blocks.SUSPICIOUS_SAND, new Identifier("simplyswords", "dormant_relic"));
        SimplySwordsAPI.registerTransformation(Blocks.CYAN_BANNER, new Identifier("simplyswords", "whisperwind"));
        SimplySwordsAPI.registerTransformation(Blocks.SKELETON_SKULL, new Identifier("simplyswords", "wraithfang"));
        SimplySwordsAPI.registerTransformation(Blocks.CAULDRON, new Identifier("simplyswords", "thunderbrand"));
        SimplySwordsAPI.registerTransformation(Blocks.QUARTZ_BLOCK, new Identifier("simplyswords", "stars_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.REDSTONE_LAMP, new Identifier("simplyswords", "storms_edge"));
        SimplySwordsAPI.registerTransformation(Blocks.LIGHTNING_ROD, new Identifier("simplyswords", "stormscale"));
    }

}
