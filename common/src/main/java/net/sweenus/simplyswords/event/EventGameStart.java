package net.sweenus.simplyswords.event;

import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class EventGameStart implements ClientPlayerEvent.ClientPlayerJoin {
    @Override
    public void join(ClientPlayerEntity player) {

        //Uncomment the following when a release requires a config regeneration
        /*
        if (SimplySwords.isConfigOutdated && SimplySwordsConfig.getBooleanValue("display_config_outdated_warning")) {
            player.sendMessage(Text.literal("SimplySwords: Your config is outdated. (this message can be disabled in the config)").formatted(Formatting.GOLD), false);
        }
*/

    }
}
