package com.carrotguy69.stocksurvival.cmd.lifesteal;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.messages.utils.MapFormatters;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.stocksurvival.StockSurvival;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import com.carrotguy69.stocksurvival.messages.SurvivalMessageKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.MISSING_GENERAL;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.PLAYER_NOT_FOUND;

public class SetMaxHP implements CommandExecutor {
    public static CommandExecutor executor = new SetMaxHP();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        String node = "stocksurvival.lifesteal.setmaxhp";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SurvivalMessageKey.NO_ACCESS), Map.of("permission", node));
            return true;
        }

        NetworkPlayer np = null;
        Player p = null;

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(MISSING_GENERAL), Map.of("missing-args", "player, value"));
            return true;
        }

        np = NetworkPlayer.getPlayerByUsername(args[0]);

        if (np == null) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(PLAYER_NOT_FOUND), Map.of("username", args[0]));
            return true;
        }

        if (args.length == 1) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(MISSING_GENERAL), Map.of("missing-args", "value"));
            return true;
        }

        int hp = Integer.parseInt(args[1]);

        if (np.getPlayer() != null) {
            StockSurvival.setPlayerMaxHP(np.getPlayer(), hp);
        }
        else {
            GameStat.setStat(np.getUUID(), StockSurvival.lifestealMaxHPKey, String.valueOf(hp)).sync();
        }

        Map<String, Object> commonMap = MapFormatters.playerFormatter(np);
        commonMap.put("hp", hp);
        commonMap.put("health", hp);
        commonMap.put("hearts", hp);
        commonMap.put("player-hp", hp);
        commonMap.put("player-health", hp);
        commonMap.put("player-hearts", (double) hp / 2);


        MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SurvivalMessageKey.LIFESTEAL_SET_MAX_HP), commonMap);
        return false;
    }

}
