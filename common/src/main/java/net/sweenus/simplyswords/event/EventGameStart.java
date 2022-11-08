package net.sweenus.simplyswords.event;

import com.google.gson.JsonObject;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.sweenus.simplyswords.SimplySwords;
import net.sweenus.simplyswords.config.Config;
import net.sweenus.simplyswords.config.SimplySwordsConfig;

public class EventGameStart implements ClientPlayerEvent.ClientPlayerJoin {
    @Override
    public void join(ClientPlayerEntity player) {

        //player.sendMessage(Text.literal("We have connected!").formatted(Formatting.DARK_RED), false);

        if (SimplySwords.isConfigOutdated) {
            player.sendMessage(Text.literal("You are missing some Simply Swords features!").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            player.sendMessage(Text.literal("Please delete the Simply Swords config folder in your Minecraft directory > config").formatted(Formatting.GOLD), false);
        }


    }
}
