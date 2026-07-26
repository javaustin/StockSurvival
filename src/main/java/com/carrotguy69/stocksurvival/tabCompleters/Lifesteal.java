package com.carrotguy69.stocksurvival.tabCompleters;

import com.carrotguy69.cxyz.tabCompleters.AnyPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Lifesteal implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> options = null;
        List<String> results = new ArrayList<>();

        if (args.length <= 1) {
            options = List.of("getMaxHP", "setMaxHP");
        }

        if (args.length == 2) {
            options = AnyPlayer.getAllUsernames();
        }

        if (args.length >= 3) {
            options = List.of();
        }

        for (String option : options) {
            if (option.startsWith(args[args.length - 1]))
                results.add(option);
        }

        return results;
    }
}
