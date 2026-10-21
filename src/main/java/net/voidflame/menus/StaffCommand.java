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
        if (!AdminAccess.hasStaffAccess(player)) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }
        if (args.length > 0) plugin.staffGui().openSection(player, args[0]);
        else plugin.staffGui().openMain(player);
        return true;
    }
}
