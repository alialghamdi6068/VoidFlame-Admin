package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Locale;

public final class MetricsCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;

    public MetricsCommand(VoidFlameMenusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("voidflame.admin.metrics")
                && !sender.hasPermission("voidflame.admin.full")
                && !sender.hasPermission("voidflame.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        double[] tps = Bukkit.getTPS();
        double currentTps = tps.length == 0 ? 20.0D : Math.min(20.0D, tps[0]);
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        long committed = runtime.totalMemory();

        sender.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━ VoidFlame Server Status ━━━━━━━━");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "TPS " + ChatColor.GRAY + "» "
                + tpsColor(currentTps) + format(currentTps)
                + ChatColor.GRAY + " | 1m " + format(tps.length > 1 ? Math.min(20.0D, tps[1]) : currentTps)
                + " | 5m " + format(tps.length > 2 ? Math.min(20.0D, tps[2]) : currentTps));
        sender.sendMessage(ChatColor.AQUA + "Players " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sender.sendMessage(ChatColor.GREEN + "Heap " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + mb(used) + " MB used"
                + ChatColor.GRAY + " / " + ChatColor.WHITE + mb(committed) + " MB committed"
                + ChatColor.GRAY + " / " + ChatColor.WHITE + mb(max) + " MB max"
                + ChatColor.GRAY + " (" + percent(used, max) + "%)");
        sender.sendMessage(ChatColor.BLUE + "CPU " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + processCpu() + ChatColor.GRAY + " process | "
                + ChatColor.WHITE + systemCpu() + ChatColor.GRAY + " system");
        sender.sendMessage(ChatColor.YELLOW + "Runtime " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + uptime() + ChatColor.GRAY + " | Java "
                + ChatColor.WHITE + System.getProperty("java.version"));
        sender.sendMessage(ChatColor.GRAY + "Worlds " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Bukkit.getWorlds().size()
                + ChatColor.GRAY + " | chunks " + ChatColor.WHITE + chunks()
                + ChatColor.GRAY + " | entities " + ChatColor.WHITE + entities());
        sender.sendMessage(ChatColor.GRAY + "Threads " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + Thread.getAllStackTraces().size()
                + ChatColor.GRAY + " | loaded plugins " + ChatColor.WHITE + Bukkit.getPluginManager().getPlugins().length);
        sender.sendMessage(ChatColor.GOLD + "GC " + ChatColor.GRAY + "» "
                + ChatColor.WHITE + gcCollections() + ChatColor.GRAY + " collections");
        sender.sendMessage(ChatColor.DARK_PURPLE + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        plugin.audit(sender.getName(), "VIEW_SERVER_METRICS");
        return true;
    }

    private String processCpu() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            double value = os.getProcessCpuLoad();
            if (value >= 0) return String.format(Locale.ROOT, "%.1f%%", value * 100.0D);
        }
        return "N/A";
    }

    private String systemCpu() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            double value = os.getCpuLoad();
            if (value >= 0) return String.format(Locale.ROOT, "%.1f%%", value * 100.0D);
        }
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double load = os.getSystemLoadAverage();
        return load < 0 ? "N/A" : String.format(Locale.ROOT, "%.2f load", load);
    }

    private String uptime() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        return days > 0 ? days + "d " + hours + "h " + minutes + "m"
                : hours + "h " + minutes + "m " + seconds + "s";
    }

    private long chunks() {
        return Bukkit.getWorlds().stream().mapToLong(World::getLoadedChunks).sum();
    }

    private long entities() {
        return Bukkit.getWorlds().stream().mapToLong(w -> w.getEntities().size()).sum();
    }

    private long gcCollections() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(value -> value >= 0)
                .sum();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String tpsColor(double value) {
        if (value >= 19.0D) return ChatColor.GREEN.toString();
        if (value >= 16.0D) return ChatColor.YELLOW.toString();
        if (value >= 12.0D) return ChatColor.GOLD.toString();
        return ChatColor.RED.toString();
    }

    private String percent(long used, long max) {
        if (max <= 0) return "0.0";
        return String.format(Locale.ROOT, "%.1f", used * 100.0D / max);
    }

    private long mb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}
