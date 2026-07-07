package com.carrotguy69.stocksurvival.event;

import com.carrotguy69.cxyz.events.custom.PublicChatEvent;
import com.carrotguy69.cxyz.events.custom.base.EventHandler;
import com.carrotguy69.cxyz.webhook.DiscordWebhook;
import com.carrotguy69.stocksurvival.StockSurvival;

public class ChatHandler implements EventHandler<PublicChatEvent> {

    @Override
    public boolean handle(PublicChatEvent e) {
        // cxyz should do the blacklist filter and other appropriate things
        new DiscordWebhook()
                .setURL(StockSurvival.publicWebhookUrl)
                .setContent("**" + e.getSender().getDisplayName() + ":** " + e.getContent())
                .send();

        return false;
    }
}
