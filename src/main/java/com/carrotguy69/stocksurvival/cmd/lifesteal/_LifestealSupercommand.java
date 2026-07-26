package com.carrotguy69.stocksurvival.cmd.lifesteal;

import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.utils.ObjectUtils;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import com.carrotguy69.stocksurvival.messages.SurvivalMessageKey;
import com.carrotguy69.stocksurvival.tabCompleters.Lifesteal;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class _LifestealSupercommand implements CommandExecutor {
    public static CommandExecutor executor = new _LifestealSupercommand();
    public static TabCompleter tabCompleter = new Lifesteal();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node = "stocksurvival.lifesteal";

        if (!sender.hasPermission(node)) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SurvivalMessageKey.NO_ACCESS), Map.of("permission", node));
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendParsedMessage(sender, MessageGrabber.grab(SurvivalMessageKey.MISSING_GENERAL), Map.of("missing-args", "subcommand"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "getmaxhp":
                GetMaxHP.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
                break;

            case "setmaxhp":
                SetMaxHP.executor.onCommand(sender, command, label, ObjectUtils.slice(args, 1));
                break;

            default:
                break;
        }

        return true;
    }
}
