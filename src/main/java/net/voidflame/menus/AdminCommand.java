package net.voidflame.menus;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final VoidFlameMenusPlugin plugin;

    public AdminCommand(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.isOp() && !player.hasPermission("voidflame.admin") && !player.hasPermission("voidflame.admin.full")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.audit(player.getName(), "RELOAD_ADMIN_CONFIG");
            player.sendMessage(ChatColor.GREEN + "VoidFlame Admin configuration reloaded.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("punish")) {
            String[] punishmentArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
            plugin.punishments().execute(player, punishmentArgs);
            return true;
        }

        plugin.audit(player.getName(), "OPEN_ADMIN_CONSOLE");
        new AdminMenu(plugin).open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return List.of("punish", "reload");
    }
}
