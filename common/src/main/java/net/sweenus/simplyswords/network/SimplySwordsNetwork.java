package net.sweenus.simplyswords.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.MessageType;
import dev.architectury.networking.simple.SimpleNetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.sweenus.simplyswords.SimplySwords;

public final class SimplySwordsNetwork {

    public static final SimpleNetworkManager NETWORK = SimpleNetworkManager.create(SimplySwords.MOD_ID);
    public static final MessageType USE_WEAPON_ABILITY = NETWORK.registerC2S("use_weapon_ability", UseWeaponAbilityPacket::new);
    public static final MessageType WEAPON_ABILITY_KEYBIND_STATE = NETWORK.registerC2S("weapon_ability_keybind_state", WeaponAbilityKeybindStatePacket::new);
    public static final MessageType PLAYER_MOVEMENT_INTENT = NETWORK.registerC2S("player_movement_intent", PlayerMovementIntentPacket::new);
    public static final MessageType SOULSTALKER_LEAP_LAUNCH = NETWORK.registerS2C("soulstalker_leap_launch", SoulstalkerLeapLaunchPacket::new);

    private SimplySwordsNetwork() {
    }

    public static void init() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(
                    ObserverStatusEffectsPacket.ID,
                    ObserverStatusEffectsPacket.CODEC);
        }
    }
}
