package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancedAdminGui implements Listener {
    private static final String ROOT = "§8VoidFlame §5• §d";
    private final VoidFlameMenusPlugin plugin;
    private final Map<UUID, PendingInput> input = new ConcurrentHashMap<>();

    public AdvancedAdminGui(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    public void openPunishments(Player p) {
        Inventory inv = gui("punishments", ROOT + "Punishments");
        button(inv, 10, Material.PLAYER_HEAD, "§c§lBAN", "§7Select a player, then choose duration.");
        button(inv, 12, Material.CHAIN_COMMAND_BLOCK, "§6§lMUTE", "§7Select a player, then choose duration.");
        button(inv, 14, Material.YELLOW_DYE, "§e§lWARN", "§7Select a player and enter a reason.");
        button(inv, 16, Material.IRON_BOOTS, "§f§lKICK", "§7Select a player and enter a reason.");
        button(inv, 28, Material.BOOK, "§b§lHISTORY", "§7Select an online player to view history.");
        button(inv, 30, Material.LIME_DYE, "§a§lUNPUNISH", "§7Select a player and clear active punishments.");
        button(inv, 49, Material.ARROW, "§7§lBACK", "§7Return to administration.");
        p.openInventory(inv);
    }

    private void openPlayerSelect(Player staff, String action) {
        List<Player> players = sortedPlayers();
        Inventory inv = gui("punish-select:" + action, ROOT + action.toUpperCase(Locale.ROOT) + " • Player");
        for (int i = 0; i < Math.min(players.size(), 45); i++) {
            Player target = players.get(i);
            button(inv, i, Material.PLAYER_HEAD, "§f" + target.getName(), "§7Click to continue.");
        }
        button(inv, 49, Material.ARROW, "§7§lBACK");
        staff.openInventory(inv);
    }

    private void openDuration(Player staff, String action, Player target) {
        Inventory inv = gui("duration:" + action + ":" + target.getUniqueId(), ROOT + action.toUpperCase(Locale.ROOT) + " • " + target.getName());
        button(inv, 10, Material.CLOCK, "§f30 Seconds", "§7Temporary punishment.");
        button(inv, 12, Material.CLOCK, "§f10 Minutes", "§7Temporary punishment.");
        button(inv, 14, Material.CLOCK, "§f1 Hour", "§7Temporary punishment.");
        button(inv, 16, Material.CLOCK, "§f1 Day", "§7Temporary punishment.");
        button(inv, 28, Material.BARRIER, "§cPermanent", "§7No expiration.");
        button(inv, 30, Material.WRITABLE_BOOK, "§eCustom Reason", "§7Choose duration first, then type reason.");
        button(inv, 49, Material.ARROW, "§7§lBACK");
        staff.openInventory(inv);
    }

    private void requestReason(Player staff, String action, Player target, String duration) {
        input.put(staff.getUniqueId(), new PendingInput(action, target.getUniqueId(), duration, System.currentTimeMillis()));
        staff.closeInventory();
        staff.sendMessage(color("&dVoidFlame &7» &fType the punishment reason in chat."));
        staff.sendMessage(color("&7Type &ccancel &7to cancel. You have 30 seconds."));
    }

    private void apply(Player staff, String action, Player target, String duration, String reason) {
        List<String> args = new ArrayList<>();
        args.add(action);
        args.add(target.getName());
        if (duration != null) args.add(duration);
        if (reason != null && !reason.isBlank()) args.addAll(Arrays.asList(reason.split("\\s+")));
        plugin.punishments().execute(staff, args.toArray(String[]::new));
    }

    private void openHistory(Player staff) {
        List<Player> players = sortedPlayers();
        Inventory inv = gui("history-select", ROOT + "Punishment History");
        for (int i = 0; i < Math.min(players.size(), 45); i++) {
            Player target = players.get(i);
            button(inv, i, Material.PLAYER_HEAD, "§f" + target.getName(), "§7Click to view recent punishments.");
        }
        button(inv, 49, Material.ARROW, "§7§lBACK");
        staff.openInventory(inv);
    }

    public void openPlayerManager(Player p) {
        Inventory inv = gui("player-manager", ROOT + "Player Manager");
        List<Player> players = sortedPlayers();
        for (int i=0;i<Math.min(players.size(),45);i++) {
            Player target=players.get(i);
            button(inv,i,Material.PLAYER_HEAD,"§f"+target.getName(),"§7Click to manage this player.");
        }
        button(inv,49,Material.ARROW,"§7§lBACK");
        p.openInventory(inv);
    }

    private void openPlayerActions(Player staff, Player target) {
        Inventory inv=gui("player-actions:"+target.getUniqueId(),ROOT+"Player • "+target.getName());
        button(inv,10,Material.BOOK,"§b§lCORE INFO","§7UUID: §f"+target.getUniqueId(),"§7World: §f"+target.getWorld().getName(),"§7Health: §f"+String.format(Locale.ROOT,"%.1f",target.getHealth()),"§7Ping: §f"+target.getPing());
        button(inv,12,Material.CHEST,"§e§lINVENTORY","§7Open and edit live inventory.");
        button(inv,14,Material.ENDER_CHEST,"§5§lENDER CHEST","§7Open and edit ender chest.");
        button(inv,16,Material.COMPASS,"§a§lTELEPORT TO","§7Teleport yourself to the player.");
        button(inv,28,Material.ENDER_PEARL,"§d§lTELEPORT HERE","§7Teleport the player to you.");
        button(inv,30,Material.SPYGLASS,"§6§lSPECTATE","§7Set yourself to spectator and follow target.");
        button(inv,32,Material.HEART_OF_THE_SEA,"§c§lHEAL","§7Restore health and hunger.");
        button(inv,34,Material.BARRIER,"§c§lCLEAR INVENTORY","§7Clear the player's inventory.");
        button(inv,49,Material.ARROW,"§7§lBACK");
        staff.openInventory(inv);
    }

    public void openSecurity(Player p) {
        Inventory inv = gui("security-control", ROOT + "Security Control");
        int rate = AdminServices.securityInt("getMaxActionsPerSecond", 8);
        int join = AdminServices.securityInt("getJoinThreshold", 4);
        int verify = AdminServices.securityInt("getVerifyScore", 5);
        int block = AdminServices.securityInt("getBlockScore", 20);
        boolean on = AdminServices.securityBoolean("isProtectionEnabled", true);
        boolean lock = AdminServices.securityBoolean("isLockdown", false);
        button(inv, 10, on ? Material.LIME_DYE : Material.RED_DYE, on ? "§a§lPROTECTION ON" : "§c§lPROTECTION OFF", "§7Click to toggle.");
        button(inv, 12, lock ? Material.REDSTONE_BLOCK : Material.IRON_BLOCK, lock ? "§4§lLOCKDOWN ON" : "§7§lLOCKDOWN OFF", "§7Click to toggle.");
        button(inv, 14, Material.COMPARATOR, "§b§lACTIONS / SEC", "§7Current: §f" + rate, "§eClick §8» §fCycle 4 / 8 / 12 / 20");
        button(inv, 16, Material.SPAWNER, "§6§lJOIN THRESHOLD", "§7Current: §f" + join, "§eClick §8» §fCycle 2 / 4 / 6 / 10");
        button(inv, 28, Material.REDSTONE, "§c§lVERIFY SCORE", "§7Current: §f" + verify, "§eClick §8» §fCycle 3 / 5 / 8 / 12");
        button(inv, 30, Material.TNT, "§4§lBLOCK SCORE", "§7Current: §f" + block, "§eClick §8» §fCycle 10 / 20 / 30 / 50");
        button(inv, 32, Material.SHIELD, "§b§lSTATUS", "§7Level: §f" + AdminServices.securityInt("getProtectionLevel", 0), "§7Verifying: §f" + AdminServices.securityInt("getVerifyingCount", 0), "§7Whitelist: §f" + AdminServices.securityInt("getWhitelistSize", 0));
        button(inv, 34, Material.NAME_TAG, "§e§lWHITELIST", "§7Open Security whitelist.");
        button(inv, 49, Material.ARROW, "§7§lBACK");
        p.openInventory(inv);
    }

    public void openBackups(Player p) {
        Inventory inv = gui("backups", ROOT + "Database Backups");
        List<Path> backups = AdminServices.backups();
        if (backups.isEmpty()) {
            button(inv, 22, Material.BARRIER, "§c§lNO BACKUPS", "§7Create a backup first.");
        } else {
            for (int i = 0; i < Math.min(backups.size(), 45); i++) {
                Path b = backups.get(i);
                button(inv, i, Material.CHEST, "§b" + b.getFileName(), "§7Click to restore this backup.", "§cA safety backup is created first.");
            }
        }
        button(inv, 47, Material.CHEST, "§a§lCREATE BACKUP", "§7Create a new validated backup.");
        button(inv, 49, Material.ARROW, "§7§lBACK");
        p.openInventory(inv);
    }

    private void confirmRestore(Player p, Path backup) {
        Inventory inv = gui("restore:" + backup.toAbsolutePath(), ROOT + "Confirm Restore");
        button(inv, 20, Material.RED_CONCRETE, "§c§lCONFIRM RESTORE", "§7" + backup.getFileName(), "§cA safety backup is created first.");
        button(inv, 24, Material.LIME_CONCRETE, "§a§lCANCEL");
        p.openInventory(inv);
    }

    public void openAnalytics(Player p) {
        AdminServices.query("SELECT COUNT(*) c FROM player_profiles").thenCombine(
                AdminServices.query("SELECT COUNT(*) c FROM duel_matches"), (players, matches) -> new long[]{num(players), num(matches)}
        ).thenCombine(AdminServices.query("SELECT COALESCE(AVG(elo),0) v FROM player_profiles"), (a, elo) -> {
            a[0] = a[0]; a[1] = a[1]; return new double[]{a[0], a[1], value(elo, "v")};
        }).thenCombine(AdminServices.query("SELECT COALESCE(SUM(wins),0) v FROM player_profiles"), (a, wins) -> {
            return new double[]{a[0], a[1], a[2], value(wins, "v")};
        }).thenCombine(AdminServices.query("SELECT COALESCE(SUM(losses),0) v FROM player_profiles"), (a, losses) -> new double[]{a[0], a[1], a[2], a[3], value(losses, "v")}).thenCombine(AdminServices.query("SELECT kit, COUNT(*) c FROM duel_matches GROUP BY kit ORDER BY c DESC LIMIT 1"), (a, kit) -> {
            String topKit = kit.isEmpty() ? "N/A" : String.valueOf(kit.get(0).getOrDefault("kit", "N/A"));
            return new Analytics(a[0], a[1], a[2], a[3], a[4], topKit, "N/A");
        }).thenCombine(AdminServices.query("SELECT name,wins FROM player_profiles ORDER BY wins DESC LIMIT 1"), (a, top) -> {
            String topPlayer = top.isEmpty() ? "N/A" : String.valueOf(top.get(0).getOrDefault("name", "N/A")) + " (" + top.get(0).getOrDefault("wins", 0) + ")";
            return new Analytics(a.players,a.matches,a.elo,a.wins,a.losses,a.topKit,a.topPlayer);
        }).thenAccept(data -> Bukkit.getScheduler().runTask(plugin, () -> renderAnalytics(p, data)));
    }

    private void renderAnalytics(Player p, Analytics a) {
        Inventory inv = gui("analytics", ROOT + "Analytics");
        button(inv, 10, Material.PLAYER_HEAD, "§b§lPLAYERS", "§7Profiles: §f" + (long)a.players);
        button(inv, 12, Material.GOLDEN_SWORD, "§d§lDUELS", "§7Recorded matches: §f" + (long)a.matches);
        button(inv, 14, Material.GOLD_INGOT, "§6§lTOTAL WINS", "§f" + (long)a.wins);
        button(inv, 16, Material.EXPERIENCE_BOTTLE, "§a§lAVERAGE ELO", "§f" + String.format(Locale.ROOT, "%.1f", a.elo));
        button(inv, 28, Material.CHEST, "§e§lTOP KIT", "§f" + a.topKit);
        button(inv, 29, Material.PLAYER_HEAD, "§6§lTOP PLAYER", "§f" + a.topPlayer);
        button(inv, 30, Material.IRON_SWORD, "§c§lACTIVE DUELS", "§7Live: §f" + countActive());
        button(inv, 32, Material.HOPPER, "§b§lQUEUE", "§7Queued players: §f" + countQueue());
        button(inv, 34, Material.NETHER_STAR, "§a§lFFA", "§7Online FFA players: §f" + countFfa());
        button(inv, 40, Material.COMPARATOR, "§f§lWIN RATE", "§7Global: §f" + String.format(Locale.ROOT, "%.1f%%", a.matches <= 0 ? 0 : a.wins * 100.0 / Math.max(1, a.wins + a.losses)));
        button(inv, 49, Material.ARROW, "§7§lBACK");
        p.openInventory(inv);
    }

    private long countActive() {
        return Bukkit.getWorlds().stream().flatMap(w -> w.getPlayers().stream()).filter(x -> x.getScoreboardTags().contains("voidflame-duel")).count();
    }
    private long countQueue() {
        return Bukkit.getOnlinePlayers().stream().filter(x -> x.getScoreboardTags().contains("voidflame-queue")).count();
    }
    private long countFfa() {
        return Bukkit.getOnlinePlayers().stream().filter(x -> x.getScoreboardTags().contains("voidflame-ffa")).count();
    }

    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        int s=e.getRawSlot();
        if (s==49) { new AdminMenu(plugin).open(p); return; }
        String t=h.type;
        if (t.equals("punishments")) {
            if(s==10)openPlayerSelect(p,"ban"); else if(s==12)openPlayerSelect(p,"mute"); else if(s==14)openPlayerSelect(p,"warn"); else if(s==16)openPlayerSelect(p,"kick"); else if(s==28)openHistory(p); else if(s==30)openPlayerSelect(p,"unpunish");
        } else if(t.startsWith("punish-select:")) {
            String action=t.substring("punish-select:".length());
            List<Player> players=sortedPlayers(); if(s>=0&&s<players.size()){Player target=players.get(s); if(action.equals("unpunish")){plugin.punishments().execute(p,new String[]{"unpunish",target.getName()});openPunishments(p);} else if(action.equals("warn")||action.equals("kick")) requestReason(p,action,target,null); else openDuration(p,action,target);}
        } else if(t.startsWith("duration:")) {
            String[] x=t.split(":",4); String action=x[1]; Player target=Bukkit.getPlayer(UUID.fromString(x[2])); if(target==null){openPunishments(p);return;}
            String d=s==10?"30s":s==12?"10m":s==14?"1h":s==16?"1d":null;
            if(s==28){requestReason(p,action,target,null);} else if(d!=null){requestReason(p,action,target,d);}
        } else if(t.equals("history-select")) {
            List<Player> players=sortedPlayers(); if(s>=0&&s<players.size()){plugin.punishments().execute(p,new String[]{"history",players.get(s).getName()});}
        } else if(t.equals("player-manager")) {
            List<Player> players=sortedPlayers(); if(s>=0&&s<players.size())openPlayerActions(p,players.get(s));
        } else if(t.startsWith("player-actions:")) {
            Player target=Bukkit.getPlayer(UUID.fromString(t.substring("player-actions:".length()))); if(target==null){openPlayerManager(p);return;}
            if(s==10){p.sendMessage(color("&bCore info for &f"+target.getName()+" &7» &f"+target.getUniqueId()));p.sendMessage(color("&7World: &f"+target.getWorld().getName()+" &7Ping: &f"+target.getPing()));}
            else if(s==12)p.openInventory(target.getInventory());
            else if(s==14)p.openInventory(target.getEnderChest());
            else if(s==16)p.teleportAsync(target.getLocation());
            else if(s==28)target.teleportAsync(p.getLocation());
            else if(s==30){p.setGameMode(org.bukkit.GameMode.SPECTATOR);p.setSpectatorTarget(target);}
            else if(s==32){target.setHealth(target.getMaxHealth());target.setFoodLevel(20);target.setFireTicks(0);}
            else if(s==34)target.getInventory().clear();
            else if(s==49)openPlayerManager(p);
        } else if(t.equals("security-control")) {
            handleSecurity(p,s);
        } else if(t.equals("backups")) {
            if(s==47){AdminServices.backup().whenComplete((v,e2)->Bukkit.getScheduler().runTask(plugin,()->{p.sendMessage(color(e2==null?"&aBackup created.":"&cBackup failed: "+e2.getMessage()));openBackups(p);}));}
            else {List<Path> bs=AdminServices.backups();if(s>=0&&s<bs.size())confirmRestore(p,bs.get(s));}
        } else if(t.startsWith("restore:")) {
            if(s==20){Path b=Path.of(t.substring("restore:".length()));AdminServices.restore(b).whenComplete((v,e2)->Bukkit.getScheduler().runTask(plugin,()->{p.sendMessage(color(e2==null?"&aRestore completed. A safety backup was made first.":"&cRestore failed: "+e2.getMessage()));openBackups(p);}));}
            else if(s==24)openBackups(p);
        }
    }

    private void handleSecurity(Player p,int s) {
        if(s==10){boolean v=AdminServices.securityBoolean("isProtectionEnabled",true);AdminServices.setSecurity("setProtectionEnabled",!v);}
        else if(s==12){boolean v=AdminServices.securityBoolean("isLockdown",false);AdminServices.setSecurity("setLockdown",!v);}
        else if(s==14){int v=AdminServices.securityInt("getMaxActionsPerSecond",8);int n=v>=20?4:v+4;AdminServices.setSecurityInt("setMaxActionsPerSecond",n);}
        else if(s==16){int v=AdminServices.securityInt("getJoinThreshold",4);int n=v>=10?2:v+2;AdminServices.setSecurityInt("setJoinThreshold",n);}
        else if(s==28){int v=AdminServices.securityInt("getVerifyScore",5);int n=v>=12?3:v+3;AdminServices.setSecurityInt("setVerifyScore",n);}
        else if(s==30){int v=AdminServices.securityInt("getBlockScore",20);int n=v>=50?10:v+10;AdminServices.setSecurityInt("setBlockScore",n);}
        else if(s==34){p.closeInventory();Bukkit.getScheduler().runTask(plugin,()->p.performCommand("antibot gui"));return;}
        openSecurity(p);
    }

    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}

    @EventHandler public void chat(AsyncPlayerChatEvent e) {
        PendingInput req=input.get(e.getPlayer().getUniqueId()); if(req==null)return;
        e.setCancelled(true);
        if(System.currentTimeMillis()-req.createdAt>30_000L){input.remove(e.getPlayer().getUniqueId());e.getPlayer().sendMessage(color("&cInput timed out."));return;}
        if(e.getMessage().equalsIgnoreCase("cancel")){input.remove(e.getPlayer().getUniqueId());e.getPlayer().sendMessage(color("&7Cancelled."));return;}
        input.remove(e.getPlayer().getUniqueId());
        Player target=Bukkit.getPlayer(req.target); if(target==null){e.getPlayer().sendMessage(color("&cTarget is no longer online."));return;}
        Bukkit.getScheduler().runTask(plugin,()->apply(e.getPlayer(),req.action,target,req.duration,e.getMessage()));
    }

    private List<Player> sortedPlayers(){List<Player> p=new ArrayList<>(Bukkit.getOnlinePlayers());p.sort(Comparator.comparing(Player::getName,String.CASE_INSENSITIVE_ORDER));return p;}
    private Inventory gui(String type,String title){Inventory i=Bukkit.createInventory(new Holder(type),54,color(title));ItemStack f=item(Material.BLACK_STAINED_GLASS_PANE," ");for(int n=0;n<54;n++)i.setItem(n,f.clone());for(int n=0;n<9;n++)i.setItem(n,item(Material.PURPLE_STAINED_GLASS_PANE," "));for(int n=45;n<54;n++)i.setItem(n,item(Material.PURPLE_STAINED_GLASS_PANE," "));return i;}
    private void button(Inventory i,int s,Material m,String n,String... l){if(s<i.getSize())i.setItem(s,item(m,n,l));}
    private ItemStack item(Material m,String n,String... l){ItemStack i=new ItemStack(m);ItemMeta md=i.getItemMeta();if(md!=null){md.setDisplayName(color(n));md.setLore(Arrays.stream(l).map(this::color).toList());i.setItemMeta(md);}return i;}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    private long num(List<Map<String,Object>> r){return r.isEmpty()?0:((Number)r.get(0).values().iterator().next()).longValue();}
    private double value(List<Map<String,Object>> r,String k){return r.isEmpty()?0:((Number)r.get(0).getOrDefault(k,0)).doubleValue();}
    private record PendingInput(String action,UUID target,String duration,long createdAt){}
    private record Analytics(double players,double matches,double elo,double wins,double losses,String topKit,String topPlayer){}
    static final class Holder implements InventoryHolder {final String type;Holder(String type){this.type=type;}public Inventory getInventory(){return null;}}
}
