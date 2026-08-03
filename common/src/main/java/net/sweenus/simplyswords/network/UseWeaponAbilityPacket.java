package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.sweenus.simplyswords.world.PlayerWeaponAbilityManager;

public class UseWeaponAbilityPacket extends BaseC2SMessage {

    private final Hand hand;
    private final boolean pressed;

    public UseWeaponAbilityPacket(Hand hand, boolean pressed) {
        this.hand = hand;
        this.pressed = pressed;
    }

    public UseWeaponAbilityPacket(PacketByteBuf buf) {
        this.hand = buf.readEnumConstant(Hand.class);
        this.pressed = buf.readBoolean();
    }

    @Override
    public MessageType getType() {
        return SimplySwordsNetwork.USE_WEAPON_ABILITY;
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(hand);
        buf.writeBoolean(pressed);
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayerEntity player) {
            PlayerWeaponAbilityManager.handleInput(player, hand, pressed);
        }
    }
}
