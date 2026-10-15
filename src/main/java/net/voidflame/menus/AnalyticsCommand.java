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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AnalyticsCommand implements CommandExecutor {
    private final VoidFlameMenusPlugin plugin;
    public AnalyticsCommand(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.isOp())) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        double[] tps=Bukkit.getTPS();
        double current=tps.length>0?clampTps(tps[0]):20.0D;
        Runtime runtime=Runtime.getRuntime();
        long used=runtime.totalMemory()-runtime.freeMemory(), max=runtime.maxMemory();
        double mspt=averageTickTime();
        String health=health(current,mspt,used,max);

        sender.sendMessage(ChatColor.DARK_PURPLE+"⚡ VoidFlame Analytics");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE+"Server: "+healthColor(health)+health+ChatColor.GRAY+" | TPS "+tpsColor(current)+fmt(current)+ChatColor.GRAY+" | MSPT "+ChatColor.WHITE+fmt(mspt));
        sender.sendMessage(ChatColor.WHITE+"RAM: "+mb(used)+" / "+mb(max)+" MB "+ChatColor.GRAY+"("+percent(used,max)+"%)");
        sender.sendMessage(ChatColor.WHITE+"CPU: "+cpu(processCpu())+" "+ChatColor.GRAY+"| System: "+cpu(systemCpu()));
        sender.sendMessage(ChatColor.WHITE+"Players: "+Bukkit.getOnlinePlayers().size()+"/"+Bukkit.getMaxPlayers()+" | Entities: "+entities()+" | Chunks: "+chunks());
        sender.sendMessage(ChatColor.WHITE+"Worlds: "+Bukkit.getWorlds().size()+" | Threads: "+threadCount()+" | Java: "+Runtime.version().feature());
        sender.sendMessage(ChatColor.WHITE+"Uptime: "+uptime()+" | Disk Free: "+diskFree()+" | GC: "+gcCollections());

        var profilesFuture = AdminServices.query("SELECT COUNT(*) AS c FROM player_profiles_smp");
        var accountsFuture = AdminServices.query("SELECT COUNT(*) AS c FROM economy_accounts");
        var listingsFuture = AdminServices.query("SELECT COUNT(*) AS c FROM market_listings WHERE status='ACTIVE' AND expires_at>?", System.currentTimeMillis());
        var ordersFuture = AdminServices.query("SELECT COUNT(*) AS c FROM buy_orders WHERE status='OPEN' AND expires_at>?", System.currentTimeMillis());
        var friendsFuture = AdminServices.query("SELECT COUNT(*) AS c FROM social_relationships WHERE relationship_type='FRIEND' AND status='ACCEPTED'");
        var topKillsFuture = AdminServices.query("SELECT last_known_name AS name,kills,deaths,playtime_seconds FROM player_profiles_smp ORDER BY kills DESC,deaths ASC LIMIT 5");
        var topBalancesFuture = AdminServices.query("SELECT COALESCE(s.last_known_name,e.uuid) AS name,e.balance FROM economy_accounts e LEFT JOIN player_profiles_smp s ON s.uuid=e.uuid ORDER BY e.balance DESC LIMIT 5");
        java.util.concurrent.CompletableFuture.allOf(profilesFuture, accountsFuture, listingsFuture, ordersFuture,
                friendsFuture, topKillsFuture, topBalancesFuture).thenAccept(ignored ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<Map<String,Object>> profileRows = profilesFuture.join();
                    List<Map<String,Object>> accountRows = accountsFuture.join();
                    List<Map<String,Object>> listingRows = listingsFuture.join();
                    List<Map<String,Object>> orderRows = ordersFuture.join();
                    List<Map<String,Object>> friendRows = friendsFuture.join();
                    List<Map<String,Object>> topKillRows = topKillsFuture.join();
                    List<Map<String,Object>> topBalanceRows = topBalancesFuture.join();

                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.AQUA + "Core SMP Metrics");
                    sender.sendMessage(ChatColor.WHITE + "SMP profiles: " + number(profileRows, "c")
                            + " | Economy accounts: " + number(accountRows, "c"));
                    sender.sendMessage(ChatColor.WHITE + "Active listings: " + number(listingRows, "c")
                            + " | Open buyer orders: " + number(orderRows, "c"));
                    sender.sendMessage(ChatColor.WHITE + "Accepted friendship records: " + number(friendRows, "c"));
                    sender.sendMessage(ChatColor.WHITE + "Top Kills: " + formatSmpPlayers(topKillRows));
                    sender.sendMessage(ChatColor.WHITE + "Top Balances: " + formatBalances(topBalanceRows));
                    plugin.audit(sender.getName(), "VIEW_SMP_ANALYTICS");
                })).exceptionally(error -> {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                            ChatColor.RED + "Core SMP analytics unavailable: " + error.getMessage()));
                    return null;
                });
        return true;
    }

    private String formatSmpPlayers(List<Map<String,Object>> rows) {
        if (rows.isEmpty()) return "N/A";
        StringBuilder out = new StringBuilder();
        for (Map<String,Object> row : rows) {
            if (out.length() > 0) out.append(", ");
            out.append(row.get("name")).append(" (").append(row.get("kills")).append(" kills / ")
                    .append(row.get("deaths")).append(" deaths)");
        }
        return out.toString();
    }
    private String formatBalances(List<Map<String,Object>> rows) {
        if (rows.isEmpty()) return "N/A";
        StringBuilder out = new StringBuilder();
        for (Map<String,Object> row : rows) {
            if (out.length() > 0) out.append(", ");
            out.append(row.get("name")).append(" $").append(fmtNumber(row.get("balance")));
        }
        return out.toString();
    }
    private String number(List<Map<String,Object>> rows,String key){return rows.isEmpty()?"0":String.valueOf(rows.get(0).getOrDefault(key,0));}
    private String fmtNumber(Object value){return value instanceof Number n?String.format(Locale.ROOT,"%.0f",n.doubleValue()):String.valueOf(value);}

    private double processCpu(){if(ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os){double v=os.getProcessCpuLoad();return v<0?-1:v*100.0D;}return -1.0D;}
    private double systemCpu(){if(ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os){double v=os.getCpuLoad();return v<0?-1:v*100.0D;}return -1.0D;}
    private double averageTickTime(){try{Object v=Bukkit.getServer().getClass().getMethod("getAverageTickTime").invoke(Bukkit.getServer());if(v instanceof Number n)return Math.max(0,n.doubleValue());}catch(ReflectiveOperationException ignored){}return 1000.0D/Math.max(0.1D,Bukkit.getTPS()[0]);}
    private int threadCount(){return ManagementFactory.getThreadMXBean().getThreadCount();}
    private long chunks(){long n=0;for(World w:Bukkit.getWorlds())n+=w.getLoadedChunks().length;return n;}
    private long entities(){long n=0;for(World w:Bukkit.getWorlds())n+=w.getEntities().size();return n;}
    private long gcCollections(){long n=0;for(GarbageCollectorMXBean b:ManagementFactory.getGarbageCollectorMXBeans())if(b.getCollectionCount()>=0)n+=b.getCollectionCount();return n;}
    private String uptime(){long s=ManagementFactory.getRuntimeMXBean().getUptime()/1000;long d=s/86400;s%=86400;long h=s/3600;s%=3600;long m=s/60;return d>0?d+"d "+h+"h "+m+"m":h+"h "+m+"m";}
    private String diskFree(){return mb(plugin.getDataFolder().getUsableSpace())+" MB";}
    private String health(double tps,double mspt,long used,long max){double ram=max<=0?0:used*100.0/max;if(tps<16||mspt>50||ram>=90)return"DEGRADED";if(tps<19||mspt>30||ram>=80)return"WARNING";return"HEALTHY";}
    private String healthColor(String h){return switch(h){case"HEALTHY"->ChatColor.GREEN.toString();case"WARNING"->ChatColor.YELLOW.toString();default->ChatColor.RED.toString();};}
    private String tpsColor(double v){return v>=19?ChatColor.GREEN.toString():v>=16?ChatColor.YELLOW.toString():ChatColor.RED.toString();}
    private double clampTps(double v){return Math.min(20,Math.max(0,v));}
    private String cpu(double v){return v<0?"N/A":String.format(Locale.ROOT,"%.1f%%",v);}
    private String fmt(double v){return String.format(Locale.ROOT,"%.2f",v);}
    private String percent(long u,long m){return m<=0?"0.0":String.format(Locale.ROOT,"%.1f",u*100.0/m);}
    private long mb(long b){return b/1024/1024;}
}
