package net.sweenus.simplyswords;

import dev.architectury.event.events.client.ClientPlayerEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.sweenus.simplyswords.event.EventGameStart;

@Environment(EnvType.CLIENT)
public class SimplySwordsClient {

    public static void init() {

        //ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(new EventGameStart());

    }
}
