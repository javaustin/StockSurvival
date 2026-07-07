package com.carrotguy69.stocksurvival.tabCompleters;

import com.carrotguy69.cxyz.tabCompleters.AnyPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Protection implements TabCompleter {

    public static TabCompleter tabCompleter = new Protection();

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        String node2 = "stocksurvival.protection.view.others";

        List<String> options = new ArrayList<>(List.of("forfeit", "view"));
        List<String> results = new ArrayList<>();

        if (args.length == 0) {
            return options;
        }

        if (args[0].equalsIgnoreCase("forfeit")) {
            options = List.of("-confirm");
        }

        if (args[0].equalsIgnoreCase("view") && sender.hasPermission(node2)) {
            options = AnyPlayer.getAllUsernames();
        }

        if (args.length >= 2) {
            options = List.of();
        }

        for (String s : options) {
            if (s.startsWith(args[args.length - 1])) {
                results.add(s);
            }
        }

        return results;
    }
}
