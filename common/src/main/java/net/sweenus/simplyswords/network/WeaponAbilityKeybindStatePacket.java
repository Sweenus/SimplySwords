package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityKeybindState;

public class WeaponAbilityKeybindStatePacket extends BaseC2SMessage {

    private final boolean mainhandRebound;
    private final boolean offhandRebound;

    public WeaponAbilityKeybindStatePacket(boolean mainhandRebound, boolean offhandRebound) {
        this.mainhandRebound = mainhandRebound;
        this.offhandRebound = offhandRebound;
    }

    public WeaponAbilityKeybindStatePacket(RegistryByteBuf buf) {
        this.mainhandRebound = buf.readBoolean();
        this.offhandRebound = buf.readBoolean();
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.WEAPON_ABILITY_KEYBIND_STATE;
    }

    @Override
    public void write(RegistryByteBuf buf) {
        buf.writeBoolean(mainhandRebound);
        buf.writeBoolean(offhandRebound);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            PlayerWeaponAbilityKeybindState.updateServerState(player, mainhandRebound, offhandRebound);
        }
    }
}
