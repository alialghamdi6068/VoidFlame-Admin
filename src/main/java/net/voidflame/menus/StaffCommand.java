package net.voidflame.menus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;

    public StaffCommand(VoidFlameMenusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        if (!player.hasPermission("voidflame.staff")
                && !player.hasPermission("voidflame.admin")
                && !player.hasPermission("voidflame.admin.full")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        plugin.staffGui().openMain(player);
        return true;
    }
}
