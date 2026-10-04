package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Locale;

public final class AnalyticsCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;

    public AnalyticsCommand(VoidFlameMenusPlugin plugin) {
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
        double current = tps.length > 0 ? clampTps(tps[0]) : 20.0D;
        double one = tps.length > 1 ? clampTps(tps[1]) : current;
        double five = tps.length > 2 ? clampTps(tps[2]) : current;
        double fifteen = tps.length > 3 ? clampTps(tps[3]) : current;

        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long committed = runtime.totalMemory();
        long max = runtime.maxMemory();

        double processCpu = processCpu();
        double systemCpu = systemCpu();
        double mspt = averageTickTime();
        String health = health(current, mspt, used, max);

        sender.sendMessage(ChatColor.DARK_PURPLE + "⚡ VoidFlame Analytics");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "TPS: " + tpsColor(current) + fmt(current)
                + ChatColor.GRAY + " | MSPT: " + ChatColor.WHITE + fmt(mspt) + "ms");
        sender.sendMessage(ChatColor.WHITE + "RAM: " + mb(used) + " / " + mb(max) + " MB"
                + ChatColor.GRAY + " (" + percent(used, max) + "%)");
        sender.sendMessage(ChatColor.WHITE + "CPU: " + cpu(processCpu) + ChatColor.GRAY + " | System: " + cpu(systemCpu));
        sender.sendMessage(ChatColor.WHITE + "Players: " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        sender.sendMessage(ChatColor.WHITE + "Entities: " + entities());
        sender.sendMessage(ChatColor.WHITE + "Chunks: " + chunks());
        sender.sendMessage(ChatColor.WHITE + "Worlds: " + Bukkit.getWorlds().size());
        sender.sendMessage(ChatColor.WHITE + "Threads: " + threadCount());
        sender.sendMessage(ChatColor.WHITE + "Java: " + Runtime.version().feature());
        sender.sendMessage(ChatColor.WHITE + "Uptime: " + uptime());
        sender.sendMessage(ChatColor.WHITE + "Disk Free: " + diskFree());
        sender.sendMessage(ChatColor.WHITE + "GC Collections: " + gcCollections());
        sender.sendMessage(ChatColor.WHITE + "Loaded Plugins: " + Bukkit.getPluginManager().getPlugins().length);
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "Status: " + healthColor(health) + health);
        plugin.audit(sender.getName(), "VIEW_SERVER_ANALYTICS");
        return true;
    }

    private double processCpu() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            return os.getProcessCpuLoad() * 100.0D;
        }
        return -1.0D;
    }

    private double systemCpu() {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
            return os.getCpuLoad() * 100.0D;
        }
        return -1.0D;
    }

    private double averageTickTime() {
        try {
            Object value = Bukkit.getServer().getClass().getMethod("getAverageTickTime").invoke(Bukkit.getServer());
            if (value instanceof Number number) return Math.max(0.0D, number.doubleValue());
        } catch (ReflectiveOperationException ignored) {
        }
        return 1000.0D / Math.max(0.1D, Bukkit.getTPS()[0]);
    }

    private int threadCount() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.getThreadCount();
    }

    private long chunks() {
        long total = 0L;
        for (World world : Bukkit.getWorlds()) total += world.getLoadedChunks().length;
        return total;
    }

    private long entities() {
        long total = 0L;
        for (World world : Bukkit.getWorlds()) total += world.getEntities().size();
        return total;
    }

    private long gcCollections() {
        long total = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() >= 0L) total += bean.getCollectionCount();
        }
        return total;
    }

    private String uptime() {
        long seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = (seconds % 3600L) / 60L;
        return days > 0 ? days + "d " + hours + "h " + minutes + "m"
                : hours + "h " + minutes + "m";
    }

    private String diskFree() {
        long bytes = plugin.getDataFolder().getUsableSpace();
        return mb(bytes) + " MB";
    }

    private String health(double tps, double mspt, long used, long max) {
        double ram = max <= 0 ? 0.0D : used * 100.0D / max;
        if (tps < 16.0D || mspt > 50.0D || ram >= 90.0D) return "DEGRADED";
        if (tps < 19.0D || mspt > 30.0D || ram >= 80.0D) return "WARNING";
        return "HEALTHY";
    }

    private String healthColor(String health) {
        return switch (health) {
            case "HEALTHY" -> ChatColor.GREEN.toString();
            case "WARNING" -> ChatColor.YELLOW.toString();
            default -> ChatColor.RED.toString();
        };
    }

    private String tpsColor(double value) {
        if (value >= 19.0D) return ChatColor.GREEN.toString();
        if (value >= 16.0D) return ChatColor.YELLOW.toString();
        return ChatColor.RED.toString();
    }

    private double clampTps(double value) {
        return Math.min(20.0D, Math.max(0.0D, value));
    }

    private String cpu(double value) {
        return value < 0.0D ? "N/A" : String.format(Locale.ROOT, "%.1f%%", value);
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String percent(long used, long max) {
        return max <= 0 ? "0.0" : String.format(Locale.ROOT, "%.1f", used * 100.0D / max);
    }

    private long mb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}
