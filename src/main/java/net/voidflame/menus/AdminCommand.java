package net.voidflame.menus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AdminCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;
    public AdminCommand(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("voidflame.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            player.sendMessage(ChatColor.GREEN + "VoidFlame Admin configuration reloaded.");
            return true;
        }
        new AdminMenu(plugin).open(player);
        return true;
    }
}
