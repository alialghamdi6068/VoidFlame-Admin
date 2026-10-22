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
        if (args.length > 0 && (args[0].equalsIgnoreCase("worldtp") || args[0].equalsIgnoreCase("worldremove"))) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Missing world identifier.");
                return true;
            }
            plugin.staffGui().worldDialogAction(player, args[0].equalsIgnoreCase("worldtp") ? "tp" : "remove", args[1]);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("worldcreate")) {
            plugin.staffGui().worldDialogAction(player, "create", "00000000-0000-0000-0000-000000000000");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("action")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /staffgui action <menu> <slot>");
                return true;
            }
            try {
                plugin.staffGui().dialogAction(player, args[1], Integer.parseInt(args[2]));
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Invalid menu action.");
            }
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("select")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /staffgui select <player-uuid>");
                return true;
            }
            plugin.staffGui().selectTarget(player, args[1]);
            return true;
        }
        if (args.length > 0) plugin.staffGui().openSection(player, args[0]);
        else plugin.staffGui().openMain(player);
        return true;
    }
}
