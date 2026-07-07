package com.carrotguy69.stocksurvival.cmd.protection;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.utils.ObjectUtils;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.NO_ACCESS;

public class _ProtectionSupercommand implements CommandExecutor {
    public static CommandExecutor executor = new _ProtectionSupercommand();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        /*
        SYNTAX:
            /protection view [player]
            /protection forfeit [-confirm]
        */

        String node = "stocksurvival.protection";
        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(NO_ACCESS), Map.of("permission", node));
            return true;
        }

        String subcommand = args.length > 0 ? args[0] : "view";

        if (subcommand.equalsIgnoreCase("forfeit")) {
            Forfeit.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
        }
        else {
            View.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
        }

        return true;
    }
}
