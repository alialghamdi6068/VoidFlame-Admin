package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Locale;

public final class MetricsCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;

    public MetricsCommand(VoidFlameMenusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("VoidFlame TPS: " + tps());
            sender.sendMessage("Memory: " + memory());
            sender.sendMessage("Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
            return true;
        }
        if (!player.hasPermission("voidflame.admin") && !player.hasPermission("voidflame.admin.full")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        String tps = tps();
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long allocated = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = allocated - free;

        player.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━ VoidFlame Server Status ━━━━━━━━");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "TPS " + ChatColor.GRAY + "» "
                + tpsColor(Bukkit.getTPS().length == 0 ? 20.0D : Bukkit.getTPS()[0]) + tps);
        player.sendMessage(ChatColor.AQUA + "Players " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        player.sendMessage(ChatColor.GREEN + "Memory " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + mb(used) + " MB used"
                + ChatColor.GRAY + " / " + ChatColor.WHITE + mb(max) + " MB max");
        player.sendMessage(ChatColor.YELLOW + "Allocated " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + mb(allocated) + " MB");
        player.sendMessage(ChatColor.BLUE + "CPU " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + cpu());
        player.sendMessage(ChatColor.GRAY + "Worlds " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Bukkit.getWorlds().size());
        player.sendMessage(ChatColor.GRAY + "Threads " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Thread.getAllStackTraces().size());
        player.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        plugin.audit(player.getName(), "VIEW_SERVER_METRICS");
        return true;
    }

    private String tps() {
        double value = Bukkit.getTPS().length == 0 ? 20.0D : Math.min(20.0D, Bukkit.getTPS()[0]);
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String tpsColor(double value) {
        if (value >= 19.0D) return ChatColor.GREEN.toString();
        if (value >= 16.0D) return ChatColor.YELLOW.toString();
        if (value >= 12.0D) return ChatColor.GOLD.toString();
        return ChatColor.RED.toString();
    }

    private String memory() {
        Runtime r = Runtime.getRuntime();
        return mb(r.totalMemory() - r.freeMemory()) + " MB";
    }

    private long mb(long bytes) {
        return bytes / 1024L / 1024L;
    }

    private String cpu() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double load = os.getSystemLoadAverage();
        if (load < 0) return "N/A";
        return String.format(Locale.ROOT, "%.2f load", load);
    }
}
