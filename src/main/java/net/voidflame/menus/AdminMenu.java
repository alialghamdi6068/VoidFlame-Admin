package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class AdminMenu implements Listener {
    public static final String TITLE = "§8VoidFlame §5• §dAdministration";
    private final VoidFlameMenusPlugin plugin;

    public AdminMenu(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        if (!allowed(p, "voidflame.admin")) { deny(p); return; }
        Inventory inv = gui("main", TITLE);
        button(inv, 10, Material.DIAMOND_SWORD, "§d§lPRACTICE", "§7Duels, kits, arenas, queues, FFA", "§eClick §8» §fOpen Practice Controls");
        button(inv, 12, Material.NAME_TAG, "§6§lMANAGEMENT", "§7Ranks, players, menus, logs, settings", "§eClick §8» §fOpen Management");
        button(inv, 14, Material.REDSTONE, "§b§lSERVER", "§7Worlds, time, weather, flags and server controls", "§eClick §8» §fOpen Server Controls");
        button(inv, 16, Material.SHIELD, "§c§lSECURITY", "§7Security status and protection controls", "§eClick §8» §fOpen Security");
        button(inv, 28, Material.ENDER_CHEST, "§9§lDATA", "§7Core database, backups and recovery", "§eClick §8» §fOpen Data Controls");
        button(inv, 30, Material.NETHER_STAR, "§5§lSTAFF", "§7Staff tools and player moderation", "§eClick §8» §fOpen Staff Controls");
        button(inv, 32, Material.REPEATER, "§7§lRELOAD", "§7Reload only VoidFlame Admin configuration", "§eClick §8» §fReload");
        button(inv, 49, Material.BARRIER, "§c§lCLOSE", "§7Close administration");
        p.openInventory(inv);
    }

    private void openPractice(Player p) {
        Inventory inv = gui("practice", "§8VoidFlame §5• §dPractice");
        button(inv,10,Material.DIAMOND_SWORD,"§d§lDUELS","§7Match controls and duel administration","§eClick §8» §fOpen");
        button(inv,11,Material.CHEST,"§d§lKITS","§7Kit editor and kit administration","§eClick §8» §fOpen");
        button(inv,12,Material.NETHERITE_SWORD,"§d§lARENAS","§7Arena setup and lifecycle","§eClick §8» §fOpen");
        button(inv,13,Material.NETHER_STAR,"§b§lQUEUES","§7Queue and matchmaking controls","§eClick §8» §fOpen");
        button(inv,14,Material.ENDER_PEARL,"§a§lFFA","§7FFA administration","§eClick §8» §fOpen");
        button(inv,15,Material.GOLD_INGOT,"§e§lREWARDS","§7Coins and reward administration","§eClick §8» §fOpen");
        back(inv,49);
        p.openInventory(inv);
    }

    private void openManagement(Player p) {
        Inventory inv = gui("management", "§8VoidFlame §5• §dManagement");
        button(inv,10,Material.NAME_TAG,"§6§lRANKS","§7Rank hierarchy and permissions","§eClick §8» §fOpen");
        button(inv,11,Material.PLAYER_HEAD,"§f§lPLAYERS","§7Player administration and rank assignment","§eClick §8» §fOpen");
        button(inv,12,Material.COMPASS,"§5§lMENUS","§7Menu configuration and entry points","§eClick §8» §fOpen");
        button(inv,13,Material.BOOK,"§7§lLOGS","§7Audit and system logs","§eClick §8» §fOpen");
        button(inv,14,Material.COMPARATOR,"§b§lSETTINGS","§7Practice/server settings","§eClick §8» §fOpen");
        back(inv,49);
        p.openInventory(inv);
    }

    private void openServer(Player p) {
        Inventory inv = gui("server", "§8VoidFlame §5• §dServer");
        button(inv,10,Material.GRASS_BLOCK,"§2§lWORLDS","§7Load, unload, teleport and manage worlds","§eClick §8» §fOpen");
        button(inv,11,Material.CLOCK,"§6§lTIME & WEATHER","§7Set and lock time/weather","§eClick §8» §fOpen");
        button(inv,12,Material.REDSTONE_BLOCK,"§c§lSERVER MANAGER","§7Whitelist and controlled server actions","§eClick §8» §fOpen");
        button(inv,13,Material.IRON_SWORD,"§c§lFLAGS","§7PvP, block placement and mob spawning","§eClick §8» §fOpen");
        back(inv,49);
        p.openInventory(inv);
    }

    private void openSecurity(Player p) {
        Inventory inv = gui("security", "§8VoidFlame §5• §dSecurity");
        button(inv,11,Material.SHIELD,"§c§lSECURITY STATUS","§7Open the configured security status/control command","§eClick §8» §fOpen");
        button(inv,13,Material.BARRIER,"§4§lLOCKDOWN CONTROL","§7Open the configured emergency security controls","§eClick §8» §fOpen");
        button(inv,15,Material.BOOK,"§7§lSECURITY LOGS","§7Open security/audit controls","§eClick §8» §fOpen");
        back(inv,49);
        p.openInventory(inv);
    }

    private void openData(Player p) {
        Inventory inv = gui("data", "§8VoidFlame §5• §dData");
        button(inv,11,Material.ENDER_CHEST,"§9§lCORE DATABASE","§7Database status and administration","§eClick §8» §fOpen");
        button(inv,13,Material.CHEST,"§b§lBACKUP / RECOVERY","§7Core database backup and restore controls","§eClick §8» §fOpen");
        back(inv,49);
        p.openInventory(inv);
    }

    private void route(Player p, String permission, String path) {
        if (!allowed(p, permission) && !allowed(p, "voidflame.admin.full")) { deny(p); return; }
        String command = plugin.getConfig().getString(path + ".command", "");
        if (command == null || command.isBlank()) { p.sendMessage(color("&cThis integration is not configured.")); return; }
        p.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> p.performCommand(command));
    }

    private void click(Player p, String type, int slot) {
        if (slot == 49) { open(p); return; }
        switch (type) {
            case "main" -> {
                if (slot == 10) openPractice(p);
                else if (slot == 12) openManagement(p);
                else if (slot == 14) openServer(p);
                else if (slot == 16) openSecurity(p);
                else if (slot == 28) openData(p);
                else if (slot == 30) new StaffGui(plugin).openMain(p);
                else if (slot == 32) { plugin.reloadConfig(); p.sendMessage(color(plugin.getConfig().getString("messages.reload","&aReloaded."))); open(p); }
            }
            case "practice" -> {
                if(slot==10) route(p,"voidflame.admin.duels","integrations.duels");
                else if(slot==11) route(p,"voidflame.admin.kits","integrations.kits");
                else if(slot==12) route(p,"voidflame.admin.arenas","integrations.arenas");
                else if(slot==13) route(p,"voidflame.admin.queues","integrations.queues");
                else if(slot==14) route(p,"voidflame.admin.ffa","integrations.ffa");
                else if(slot==15) route(p,"voidflame.admin.rewards","integrations.rewards");
            }
            case "management" -> {
                if(slot==10) route(p,"voidflame.admin.ranks","integrations.ranks");
                else if(slot==11) route(p,"voidflame.admin.players","integrations.ranks");
                else if(slot==12) route(p,"voidflame.admin.menus","integrations.menus");
                else if(slot==13) route(p,"voidflame.admin.logs","integrations.logs");
                else if(slot==14) route(p,"voidflame.admin.settings","integrations.settings");
            }
            case "server" -> {
                if(slot==10) new StaffGui(plugin).openWorldsFromAdmin(p);
                else if(slot==11) new StaffGui(plugin).openTimeFromAdmin(p);
                else if(slot==12) new StaffGui(plugin).openServerFromAdmin(p);
                else if(slot==13) new StaffGui(plugin).openFlagsFromAdmin(p);
            }
            case "security" -> {
                if(slot==11) route(p,"voidflame.admin.security","integrations.security");
                else if(slot==13) route(p,"voidflame.admin.security","integrations.security");
                else if(slot==15) route(p,"voidflame.admin.logs","integrations.logs");
            }
            case "data" -> {
                if(slot==11) route(p,"voidflame.admin.database","integrations.database");
                else if(slot==13) route(p,"voidflame.admin.database","integrations.database");
            }
        }
    }

    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        click(p,h.type,e.getRawSlot());
    }

    @EventHandler public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    private Inventory gui(String type, String title) {
        int rows = plugin.getConfig().getInt("gui.rows",6);
        Inventory inv=Bukkit.createInventory(new Holder(type),Math.max(1,Math.min(6,rows))*9,color(title));
        Material filler=Material.matchMaterial(plugin.getConfig().getString("gui.filler","BLACK_STAINED_GLASS_PANE"));
        Material accent=Material.matchMaterial(plugin.getConfig().getString("gui.accent","PURPLE_STAINED_GLASS_PANE"));
        if(filler==null)filler=Material.BLACK_STAINED_GLASS_PANE;
        if(accent==null)accent=Material.PURPLE_STAINED_GLASS_PANE;
        ItemStack pane=item(filler," ");
        ItemStack glow=item(accent," ");
        for(int i=0;i<inv.getSize();i++)inv.setItem(i,pane.clone());
        for(int i=0;i<9&&i<inv.getSize();i++)inv.setItem(i,glow.clone());
        for(int i=Math.max(0,inv.getSize()-9);i<inv.getSize();i++)inv.setItem(i,glow.clone());
        return inv;
    }

    private void button(Inventory inv,int slot,Material material,String name,String... lore){if(slot<inv.getSize())inv.setItem(slot,item(material,name,lore));}
    private void back(Inventory inv,int slot){button(inv,slot,Material.ARROW,"§7§lBACK","§7Return to Administration");}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();if(meta!=null){meta.setDisplayName(color(name));meta.setLore(List.of(lore).stream().map(this::color).toList());i.setItemMeta(meta);}return i;}
    private boolean allowed(Player p,String permission){return p.hasPermission(permission)||p.hasPermission("voidflame.admin.full");}
    private void deny(Player p){p.sendMessage(color(plugin.getConfig().getString("messages.no-permission","&cNo permission.")));}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}

    static final class Holder implements InventoryHolder { final String type; Holder(String type){this.type=type;} @Override public Inventory getInventory(){return null;} }
}