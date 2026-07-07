package com.carrotguy69.stocksurvival.cmd.protection;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.messages.utils.MapFormatters;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.TimeUtils;
import com.carrotguy69.stocksurvival.StockSurvival;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.UNPROTECTED;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.MISSING_GENERAL;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.NO_ACCESS;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.PLAYER_IS_OFFLINE;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.PLAYER_NOT_FOUND;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.PROTECTION_VIEW;

public class View implements CommandExecutor {
    public static CommandExecutor executor = new View();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        /*
        SYNTAX:
            /protection view [player]
        */

        String node = "stocksurvival.protection.view";
        String node2 = "stocksurvival.protection.view.others";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(NO_ACCESS), Map.of("permission", node));
            return true;
        }

        NetworkPlayer np = null;
        Player p = null;

        if (args.length == 0 && !(sender instanceof Player)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(MISSING_GENERAL), Map.of("missing-args", "player"));
            return true;
        }

        if (args.length == 0 || !sender.hasPermission(node2)) {
            p = (Player) sender;
            np = NetworkPlayer.resolvePlayer(p.getUniqueId());
        }

        else {
            np = NetworkPlayer.getPlayerByUsername(args[0]);
            if (np != null)
                p = np.getPlayer();

            if (p == null && np != null) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(PLAYER_IS_OFFLINE), MapFormatters.playerFormatter(np));
                return true;
            }

            else if (np == null) {
                MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(PLAYER_NOT_FOUND), Map.of("username", args[0]));
                return true;
            }
        }

        Map<String, Object> commonMap = MapFormatters.playerFormatter(np);
        commonMap.put("time-remaining", TimeUtils.countdownShort(StockSurvival.getProtectionTimeLeft(p)));

        GameStat stat = GameStat.getStat(p.getUniqueId(), "survival-forfeit-protetion");

        if (stat == null) {
            stat = new GameStat(p.getUniqueId(), "survival-forfeit-protection", "false");
        }


        if (stat.getValue().equalsIgnoreCase("true") || !StockSurvival.isProtected(p)) {
            MessageUtils.sendParsedMessage(p, MessageGrabber.grab(UNPROTECTED), commonMap);
            return true;
        }



        MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(PROTECTION_VIEW), commonMap);

        return false;
    }
}
