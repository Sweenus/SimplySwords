package net.sweenus.simplyswords.neoforge.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.gametest.ManaTopUp;

public final class IronsManaTopUp implements ManaTopUp.Refiller {

    private static final float TEST_MANA = 100000.0F;

    public static void register() {
        ManaTopUp.register(new IronsManaTopUp());
    }

    @Override
    public void refill(ServerPlayerEntity player) {
        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getMana() < TEST_MANA) {
            magicData.setMana(TEST_MANA);
        }
    }
}
