package net.sweenus.simplyswords.network;

import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import net.sweenus.simplyswords.SimplySwords;

public final class SimplySwordsNetwork {

    public static final SimpleNetworkManager NETWORK = SimpleNetworkManager.create(SimplySwords.MOD_ID);
    public static final MessageType USE_WEAPON_ABILITY = NETWORK.registerC2S("use_weapon_ability", UseWeaponAbilityPacket::new);
    public static final MessageType WEAPON_ABILITY_KEYBIND_STATE = NETWORK.registerC2S("weapon_ability_keybind_state", WeaponAbilityKeybindStatePacket::new);
    public static final MessageType OBSERVER_STATUS_EFFECTS = NETWORK.registerS2C("observer_status_effects", ObserverStatusEffectsPacket::new);

    private SimplySwordsNetwork() {
    }

    public static void init() {
        // Referencing the message types initializes Architectury's receivers.
    }
}
