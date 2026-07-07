package com.carrotguy69.stocksurvival.cmd.protection;

import com.carrotguy69.cxyz.CXYZ;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.messages.utils.MapFormatters;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.ObjectUtils;
import com.carrotguy69.stocksurvival.StockSurvival;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.COMMAND_PLAYER_ONLY;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.FORFEIT;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.FORFEIT_CONFIRM;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.UNPROTECTED;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.NO_ACCESS;

public class Forfeit implements CommandExecutor {
    public static CommandExecutor executor = new Forfeit();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        /*
        SYNTAX:
            /forfeit [-confirm]
        */

        String node = "stocksurvival.protection.forfeit";
        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(NO_ACCESS), Map.of("permission", node));
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(COMMAND_PLAYER_ONLY), Map.of());
            return true;
        }

        Map<String, Object> commonMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(p.getUniqueId()));

        boolean confirmed = ObjectUtils.containsIgnoreCase(Arrays.asList(args), "-confirm");

        args = ObjectUtils.removeItem(args, "-confirm");

        GameStat stat = GameStat.getStat(p.getUniqueId(), "survival-forfeit-protetion");

        if (stat == null) {
            stat = new GameStat(p.getUniqueId(), "survival-forfeit-protection", "false");
        }

        if (stat.getValue().equalsIgnoreCase("true") || !StockSurvival.isProtected(p)) {
            MessageUtils.sendParsedMessage(p, MessageGrabber.grab(UNPROTECTED), commonMap);
            return true;
        }

        if (!confirmed) {
            MessageUtils.sendParsedMessage(p, MessageGrabber.grab(FORFEIT_CONFIRM), commonMap);
            return true;
        }

        stat.setValue("true");


        CXYZ.statUUIDMap.put(p.getUniqueId(), stat);
        stat.sync();

        MessageUtils.sendParsedMessage(p, MessageGrabber.grab(FORFEIT), commonMap);

        return false;
    }

}
