package net.voidflame.menus;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    public static final String TITLE = "§8Staff";
    private final VoidFlameMenusPlugin plugin;

    public AdminMenu(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        List<DialogBody> body = List.of(
            DialogBody.plainMessage(Component.text("VOIDFLAME ADMINISTRATION", NamedTextColor.AQUA), 300),
            DialogBody.plainMessage(Component.text("Administration tools for authorized Java staff.", NamedTextColor.GRAY), 300),
            DialogBody.plainMessage(Component.text("Choose an area. Each action performs its own permission check.", NamedTextColor.WHITE), 300)
        );
        List<ActionButton> actions = new java.util.ArrayList<>();
        actions.add(adminButton("Practice", "Duels, kits, arenas, queues and FFA.", "/vfadmin practice", NamedTextColor.AQUA));
        actions.add(adminButton("Management", "Players, menus, settings and punishments.", "/vfadmin management", NamedTextColor.BLUE));
        actions.add(adminButton("Server", "Worlds, time, weather and server controls.", "/vfadmin server", NamedTextColor.GREEN));
        actions.add(adminButton("Security", "AntiBot protection and lockdown.", "/vfadmin security", NamedTextColor.RED));
        actions.add(adminButton("Data & Backups", "Core database status and backup/recovery.", "/vfadmin data", NamedTextColor.GOLD));
        actions.add(adminButton("Staff Tools", "Open the staff tools screen.", "/staff", NamedTextColor.AQUA));
        actions.add(adminButton("Analytics", "View server health and player/match metrics.", "/vfadmin analytics", NamedTextColor.BLUE));
        actions.add(adminButton("Reload Admin Config", "Reload this plugin configuration.", "/vfadmin reload", NamedTextColor.GRAY));
        actions.add(adminButton("Return to Spawn", "Close administration and return to spawn.", "/spawn", NamedTextColor.WHITE));
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff", NamedTextColor.WHITE))
                .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                .canCloseWithEscape(true)
                .body(body)
                .build())
            .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    private ActionButton adminButton(String label, String tooltip, String command, NamedTextColor color) {
        return ActionButton.create(Component.text(label, color), Component.text(tooltip, NamedTextColor.GRAY),
            180, DialogAction.commandTemplate(command));
    }

    public void openSection(Player p, String section) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        switch (section.toLowerCase(java.util.Locale.ROOT)) {
            case "main" -> open(p);
            case "practice" -> openPractice(p);
            case "management" -> openManagement(p);
            case "server" -> openServer(p);
            case "security" -> openSecurity(p);
            case "data" -> openData(p);
            case "analytics" -> new AdvancedAdminGui(plugin).openAnalytics(p);
            case "staff" -> plugin.staffGui().openMain(p);
            default -> open(p);
        }
    }

    private void openPractice(Player p) {
        Inventory inv=gui("practice","§8VoidFlame §5• §dPractice");
        button(inv,10,Material.DIAMOND_SWORD,"§d§lDUELS","§7Open duel administration");
        button(inv,11,Material.CHEST,"§d§lKITS","§7Open kit administration/editor");
        button(inv,12,Material.NETHERITE_SWORD,"§d§lARENAS","§7Open arena administration");
        button(inv,13,Material.NETHER_STAR,"§b§lQUEUES","§7Open queue/matchmaking controls");
        button(inv,14,Material.ENDER_PEARL,"§a§lFFA","§7Open FFA controls");
        button(inv,15,Material.GOLD_INGOT,"§e§lREWARDS","§7Open coins/rewards controls");
        back(inv,49); p.openInventory(inv);
    }

    private void openManagement(Player p) {
        Inventory inv=gui("management","§8VoidFlame §5• §dManagement");
        button(inv,10,Material.PLAYER_HEAD,"§f§lPLAYERS","§7Open Staff player manager");
        button(inv,12,Material.COMPASS,"§5§lMENUS","§7Open menu/practice configuration");
        button(inv,14,Material.COMPARATOR,"§b§lSETTINGS","§7Open server/practice settings");
        button(inv,16,Material.BOOK,"§c§lPUNISHMENTS","§7Ban, mute, warn, kick and history","§eClick §8» §fOpen GUI");
        back(inv,49); p.openInventory(inv);
    }

    private void openServer(Player p) {
        Inventory inv=gui("server","§8VoidFlame §5• §dServer");
        button(inv,10,Material.GRASS_BLOCK,"§2§lWORLDS","§7Manage worlds through VoidFlame-Core");
        button(inv,11,Material.CLOCK,"§6§lTIME & WEATHER","§7Set and lock server time/weather");
        button(inv,12,Material.REDSTONE_BLOCK,"§c§lSERVER MANAGER","§7Whitelist and controlled server actions");
        button(inv,13,Material.IRON_SWORD,"§c§lFLAGS","§7PvP, block placement and mob spawning");
        back(inv,49); p.openInventory(inv);
    }

    private void openSecurity(Player p) {
        Inventory inv=gui("security","§8VoidFlame §5• §dSecurity");
        boolean enabled=AdminServices.securityBoolean("isProtectionEnabled",false);
        boolean lockdown=AdminServices.securityBoolean("isLockdown",false);
        button(inv,11,enabled?Material.LIME_DYE:Material.RED_DYE,enabled?"§a§lANTIBOT: ON":"§c§lANTIBOT: OFF","§7Click to toggle protection");
        button(inv,13,lockdown?Material.REDSTONE_BLOCK:Material.IRON_BLOCK,lockdown?"§4§lLOCKDOWN: ON":"§7§lLOCKDOWN: OFF","§7Click to toggle emergency admission lock");
        button(inv,15,Material.SHIELD,"§b§lSECURITY STATUS","§7Level: §f"+AdminServices.securityInt("getProtectionLevel",0),"§7Verifying: §f"+AdminServices.securityInt("getVerifyingCount",0),"§7Whitelist: §f"+AdminServices.securityInt("getWhitelistSize",0));
        button(inv,17,Material.NAME_TAG,"§e§lWHITELIST","§7Open Security whitelist GUI","§eClick §8» §fOpen Security");
        back(inv,49); p.openInventory(inv);
    }

    private void openData(Player p) {
        Inventory inv=gui("data","§8VoidFlame §5• §dData");
        Object db=AdminServices.coreDatabase();
        boolean open=db!=null;
        String path="";
        if(open) try { path=String.valueOf(db.getClass().getMethod("databasePath").invoke(db)); } catch(Exception ignored){}
        button(inv,11,open?Material.LIME_DYE:Material.RED_DYE,open?"§a§lCORE DATABASE: ONLINE":"§c§lCORE DATABASE: OFFLINE","§7SQLite central database","§7Path: §f"+(path.isBlank()?"Unavailable":path));
        button(inv,13,Material.CHEST,"§b§lCREATE BACKUP","§7Create an immediate Core database backup","§eClick §8» §fCreate");
        button(inv,15,Material.RECOVERY_COMPASS,"§c§lRESTORE LATEST","§7Restore the newest validated Core backup","§cClick §8» §fRequires confirmation");
        back(inv,49); p.openInventory(inv);
    }

    private void route(Player p,String permission,String path) {
        if(AdminAccess.isBedrock(p) || !(p.hasPermission(permission) || p.hasPermission("voidflame.admin.full"))){deny(p);return;}
        String command=plugin.getConfig().getString(path+".command","");
        if(command==null||command.isBlank()){p.sendMessage(color("&cThis integration is not configured."));return;}
        p.closeInventory(); plugin.audit(p.getName(),"OPEN_INTEGRATION:"+path);
        Bukkit.getScheduler().runTask(plugin,()->p.performCommand(command));
    }

    private void click(Player p,String type,int slot) {
        if(slot==49){open(p);return;}
        switch(type){
            case "main" -> {
                if(slot==10)openPractice(p);
                else if(slot==12)openManagement(p);
                else if(slot==14)openServer(p);
                else if(slot==16)new AdvancedAdminGui(plugin).openSecurity(p);
                else if(slot==28)openData(p);
                else if(slot==30)plugin.staffGui().openMain(p);
                else if(slot==32){plugin.reloadConfig();plugin.audit(p.getName(),"RELOAD_ADMIN_CONFIG");p.sendMessage(color(plugin.getConfig().getString("messages.reload","&aReloaded.")));open(p);}
                else if(slot==34)new AdvancedAdminGui(plugin).openAnalytics(p);
            }
            case "practice" -> {
                if(slot==10)route(p,"voidflame.admin.duels","integrations.duels");
                else if(slot==11)route(p,"voidflame.admin.kits","integrations.kits");
                else if(slot==12)route(p,"voidflame.admin.arenas","integrations.arenas");
                else if(slot==13)route(p,"voidflame.admin.queues","integrations.queues");
                else if(slot==14)route(p,"voidflame.admin.ffa","integrations.ffa");
                else if(slot==15)route(p,"voidflame.admin.rewards","integrations.rewards");
            }
            case "management" -> {
                if(slot==10)new AdvancedAdminGui(plugin).openPlayerManager(p);
                else if(slot==12)route(p,"voidflame.admin.menus","integrations.menus");
                else if(slot==14)route(p,"voidflame.admin.settings","integrations.settings");
                else if(slot==16)new AdvancedAdminGui(plugin).openPunishments(p);
            }
            case "server" -> {
                StaffGui staff=plugin.staffGui();
                if(slot==10)staff.openWorldsFromAdmin(p);
                else if(slot==11)staff.openTimeFromAdmin(p);
                else if(slot==12)staff.openServerFromAdmin(p);
                else if(slot==13)staff.openFlagsFromAdmin(p);
            }
            case "security" -> {
                if(slot==11){if(AdminServices.security()==null){p.sendMessage(color("&cVoidFlame-Security is not installed."));return;}boolean v=AdminServices.securityBoolean("isProtectionEnabled",false);AdminServices.setSecurity("setProtectionEnabled",!v);plugin.audit(p.getName(),"SECURITY_TOGGLE:"+( !v));openSecurity(p);}
                else if(slot==13){if(AdminServices.security()==null){p.sendMessage(color("&cVoidFlame-Security is not installed."));return;}boolean v=AdminServices.securityBoolean("isLockdown",false);AdminServices.setSecurity("setLockdown",!v);plugin.audit(p.getName(),"SECURITY_LOCKDOWN:"+( !v));openSecurity(p);}
                else if(slot==15){openSecurity(p);}
                else if(slot==17){p.closeInventory();Bukkit.getScheduler().runTask(plugin,()->p.performCommand("antibot gui"));}
            }
            case "data" -> {
                if(slot==11){openData(p);}
                else if(slot==13){AdminServices.backup().whenComplete((ok,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error==null){plugin.audit(p.getName(),"CORE_BACKUP");p.sendMessage(color("&aCore database backup created."));}else p.sendMessage(color("&cBackup failed: &f"+error.getMessage()));}));}
                else if(slot==15){confirmDataRestore(p);}
            }
        }
    }

    private void confirmDataRestore(Player p){
        Inventory inv=gui("restore-confirm","§8VoidFlame §5• §cConfirm Restore");
        button(inv,20,Material.RED_CONCRETE,"§c§lCONFIRM RESTORE","§7Restore the latest Core database backup.","§cThis can replace current database state.");
        button(inv,24,Material.LIME_CONCRETE,"§a§lCANCEL","§7Return without restoring.");
        p.openInventory(inv);
    }

    private void confirmClick(Player p,int slot){
        if(slot==20){AdminServices.restoreLatest().whenComplete((ok,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error==null){plugin.audit(p.getName(),"CORE_RESTORE_LATEST");p.sendMessage(color("&aCore database restore completed. Restart/reload the server if required."));}else p.sendMessage(color("&cRestore failed: &f"+error.getMessage()));openData(p);}));}
        else if(slot==24)openData(p);
    }

    @EventHandler public void onClick(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;
        if(!(e.getView().getTopInventory().getHolder() instanceof Holder h))return;
        e.setCancelled(true);
        if(!AdminAccess.hasAdminAccess(p)){deny(p);p.closeInventory();return;}
        if(e.getClickedInventory()!=e.getView().getTopInventory())return;
        if(h.type.equals("restore-confirm"))confirmClick(p,e.getRawSlot());else click(p,h.type,e.getRawSlot());
    }

    @EventHandler public void onDrag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}

    private Inventory gui(String type,String title){
        int rows=Math.max(1,Math.min(6,plugin.getConfig().getInt("gui.rows",6)));
        Inventory inv=Bukkit.createInventory(new Holder(type),rows*9,color(title));
        Material filler=Material.matchMaterial(plugin.getConfig().getString("gui.filler","AIR"));
        Material accent=Material.matchMaterial(plugin.getConfig().getString("gui.accent","AIR"));
        // Keep empty slots empty by default; decorative panes are opt-in in config.
        if(filler!=null && filler!=Material.AIR)
            for(int i=0;i<inv.getSize();i++)inv.setItem(i,item(filler," "));
        if(accent!=null && accent!=Material.AIR) {
            for(int i=0;i<9;i++)inv.setItem(i,item(accent," "));
            for(int i=inv.getSize()-9;i<inv.getSize();i++)inv.setItem(i,item(accent," "));
        }
        return inv;
    }
    private void button(Inventory inv,int slot,Material material,String name,String... lore){if(slot<inv.getSize())inv.setItem(slot,item(material,name,lore));}
    private void back(Inventory inv,int slot){button(inv,slot,Material.ARROW,"§7§lBACK","§7Return to Administration");}
    private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();if(meta!=null){meta.setDisplayName(color(name));meta.setLore(List.of(lore).stream().map(this::color).toList());i.setItemMeta(meta);}return i;}
    private void deny(Player p){p.sendMessage(color(plugin.getConfig().getString("messages.no-permission","&cNo permission.")));}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    static final class Holder implements InventoryHolder {final String type;Holder(String type){this.type=type;}@Override public Inventory getInventory(){return null;}}
}
