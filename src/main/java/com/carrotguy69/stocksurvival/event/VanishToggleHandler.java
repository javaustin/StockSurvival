package com.carrotguy69.stocksurvival.event;

import com.carrotguy69.cxyz.events.custom.VanishToggleEvent;
import com.carrotguy69.cxyz.events.custom.base.EventHandler;
import com.carrotguy69.cxyz.messages.utils.MapFormatters;
import com.carrotguy69.cxyz.webhook.DiscordEmbed;
import com.carrotguy69.cxyz.webhook.DiscordWebhook;
import com.carrotguy69.stocksurvival.StockSurvival;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import org.bukkit.ChatColor;

import java.util.Map;

import static com.carrotguy69.cxyz.CXYZ.f;
import static com.carrotguy69.cxyz.messages.MessageUtils.formatPlaceholders;
import static com.carrotguy69.stocksurvival.StockSurvival.publicChat;
import static com.carrotguy69.stocksurvival.StockSurvival.publicWebhookUrl;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.ON_LEAVE;

public class VanishToggleHandler implements EventHandler<VanishToggleEvent> {
    @Override
    public boolean handle(VanishToggleEvent e) {

        if (e.getToggle() && e.getPlayer().getPlayer() != null) {
            // fake a leave message (vanish is enabled)

            Map<String, Object> commonMap = MapFormatters.playerFormatter(e.getPlayer());

            publicChat.sendChannelMessage(MessageGrabber.grab(ON_LEAVE), commonMap);


            // discord message
            DiscordEmbed embed = new DiscordEmbed();
            embed.setTitle("");
            embed.setDescription("**" + ChatColor.stripColor(f(formatPlaceholders(MessageGrabber.grab(ON_LEAVE), commonMap))) + "**");
            embed.setColor(0xff7070);

            new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();
        }

        else {
            // fake a join message (for unvanish)
            if (e.getPlayer().getPlayer() != null)
                StockSurvival.doOnJoin(e.getPlayer().getPlayer());
        }

        return false;
    }
}
