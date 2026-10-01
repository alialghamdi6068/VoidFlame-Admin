package net.voidflame.menus;

import net.voidflame.core.api.ArenaService;
import net.voidflame.core.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class AdminMenus implements Listener {
    private static final String SHOP="§8VoidFlame §7• §6Coin Shop";
    private static final String REPORTS="§8VoidFlame §7• §cReports";
    private static final String RANKS="§8VoidFlame §7• §5Rank Admin";
    private static final String ARENAS="§8VoidFlame §7• §bArena Admin";
    private static final String REPORT_PLAYERS="§8VoidFlame §7• §cReport Player";
    private static final String REPORT_STAFF="§8VoidFlame §7• §cStaff Reports";
    private static final String RANK_EDIT="§8VoidFlame §7• §5Rank Editor";
    private static final String ARENA_EDIT="§8VoidFlame §7• §bArena Editor";

    private final VoidFlameMenusPlugin plugin;
    private final StorageService storage;
    private final Map<UUID, Input> input=new ConcurrentHashMap<>();
    private record Input(Type type,String value){ }
    private enum Type { REPORT_REASON, RANK_ID, RANK_PLAYER, RANK_PERMISSION, ARENA_NAME, ARENA_TEMPLATE }

    AdminMenus(VoidFlameMenusPlugin plugin, StorageService storage){
        this.plugin=plugin; this.storage=storage;
    }

    void openShop(Player p){ queryProducts(p); }
    void openReports(Player p){ open(p,REPORTS); }
    void openRanks(Player p){ if(p.hasPermission("voidflame.ranks.admin")) open(p,RANKS); }
    void openArenas(Player p){ if(p.hasPermission("voidflame.arena.manage")) open(p,ARENAS); }

    private void open(Player p,String title){
        Inventory inv=Bukkit.createInventory(null,27,title);
        fill(inv);
        switch(title){
            case REPORTS -> reports(inv,p);
            case RANKS -> ranks(inv);
            case ARENAS -> arenas(inv);
            default -> {}
        }
        p.openInventory(inv);
    }

    private void queryProducts(Player p){
        storage.database().query("SELECT product_id,display_name,material,price,enabled FROM shop_products WHERE enabled=1 ORDER BY product_id LIMIT 21")
            .thenAccept(rows->Bukkit.getScheduler().runTask(plugin,()->{
                Inventory inv=Bukkit.createInventory(null,27,SHOP); fill(inv);
                int slot=0;
                for(var row:rows){
                    if(slot>=21) break;
                    String id=String.valueOf(row.get("product_id"));
                    String name=String.valueOf(row.get("display_name"));
                    Material m=material(String.valueOf(row.get("material")));
                    long price=((Number)row.get("price")).longValue();
                    inv.setItem(slot++,item(m,"§6"+name,"§7Price: §e"+price+" coins","","§dClick §8» §fPurchase"));
                }
                if(rows.isEmpty()) inv.setItem(13,item(Material.BARRIER,"§cShop is empty","§7No products are configured."));
                inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
                p.openInventory(inv);
            }));
    }

    private void reports(Inventory inv,Player p){
        inv.setItem(10,item(Material.PAPER,"§c§lReport Player","§7Select an online player, then enter a reason."));
        if(p.hasPermission("voidflame.report.view"))
            inv.setItem(14,item(Material.WRITABLE_BOOK,"§6§lStaff Reports","§7Review and manage open reports."));
        inv.setItem(18,item(Material.ARROW,"§7§lBack"));
        inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
    }

    private void reportPlayers(Player p){
        Inventory inv=Bukkit.createInventory(null,27,REPORT_PLAYERS); fill(inv);
        int slot=0;
        for(Player target:Bukkit.getOnlinePlayers()){
            if(target.equals(p)) continue;
            if(slot>=21) break;
            inv.setItem(slot++,item(Material.PLAYER_HEAD,"§f"+target.getName(),"§7Click to report this player."));
        }
        inv.setItem(18,item(Material.ARROW,"§7§lBack"));
        inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
        p.openInventory(inv);
    }

    private void staffReports(Player p){
        storage.database().query("SELECT report_id,reporter,target,reason,status FROM reports WHERE status='OPEN' ORDER BY timestamp DESC LIMIT 21")
            .thenAccept(rows->Bukkit.getScheduler().runTask(plugin,()->{
                Inventory inv=Bukkit.createInventory(null,27,REPORT_STAFF); fill(inv);
                int slot=0;
                for(var row:rows){
                    String id=String.valueOf(row.get("report_id"));
                    String target=String.valueOf(row.get("target"));
                    String reason=String.valueOf(row.get("reason"));
                    inv.setItem(slot++,item(Material.REDSTONE,"§c"+target,"§7Reason: §f"+reason,"","§aLeft click §8» §fResolve","§cRight click §8» §fDismiss"));
                    if(slot>=21) break;
                }
                inv.setItem(18,item(Material.ARROW,"§7§lBack"));
                inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
                p.openInventory(inv);
            }));
    }

    private void ranks(Inventory inv){
        storage.database().query("SELECT data_key,data_value FROM module_data WHERE module='ranks' AND data_key LIKE 'rank.%' ORDER BY data_key")
            .thenAccept(rows->Bukkit.getScheduler().runTask(plugin,()->{
                int slot=0;
                for(var row:rows){
                    if(slot>=21) break;
                    String id=String.valueOf(row.get("data_key")).substring(5);
                    inv.setItem(slot++,item(Material.NAME_TAG,"§5"+id,"§7Click to edit this rank."));
                }
                inv.setItem(21,item(Material.EMERALD,"§a§lAssign Rank","§7Enter player then rank in chat."));
                inv.setItem(22,item(Material.ANVIL,"§e§lCreate Rank","§7Enter rank id in chat."));
                inv.setItem(18,item(Material.ARROW,"§7§lBack"));
                inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
            }));
    }

    private void rankEdit(Player p,String id){
        Inventory inv=Bukkit.createInventory(null,27,RANK_EDIT);
        fill(inv);
        inv.setItem(4,item(Material.NAME_TAG,"§5§lRank: §f"+id));
        inv.setItem(10,item(Material.ANVIL,"§e§lRename","§7Enter new display name in chat."));
        inv.setItem(12,item(Material.WRITABLE_BOOK,"§b§lPrefix","§7Enter prefix using & color codes."));
        inv.setItem(14,item(Material.PAPER,"§f§lWeight","§7Enter numeric weight."));
        inv.setItem(16,item(Material.REDSTONE,"§c§lDelete","§7Delete this rank."));
        inv.setItem(19,item(Material.BOOK,"§d§lPermissions","§7Enter permission to add."));
        inv.setItem(21,item(Material.PLAYER_HEAD,"§a§lAssign","§7Enter player name in chat."));
        inv.setItem(18,item(Material.ARROW,"§7§lBack"));
        inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
        p.openInventory(inv);
    }

    private void arenas(Inventory inv,Player p){
        var reg=Bukkit.getServicesManager().getRegistration(ArenaService.class);
        if(reg!=null && reg.getProvider()!=null){
            for(String name:reg.getProvider().allNames()){
                if(name==null) continue;
                int slot=inv.firstEmpty();
                if(slot<0 || slot>=21) break;
                inv.setItem(slot,item(Material.IRON_BARS,"§b"+name,"§7Click to edit."));
            }
        }
        inv.setItem(21,item(Material.EMERALD,"§a§lCreate Arena","§7Enter arena name in chat."));
        inv.setItem(22,item(Material.COMPASS,"§b§lReload","§7Reload arena configuration."));
        inv.setItem(18,item(Material.ARROW,"§7§lBack"));
        inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
    }

    private void arenaEdit(Player p,String name){
        Inventory inv=Bukkit.createInventory(null,27,ARENA_EDIT); fill(inv);
        inv.setItem(4,item(Material.IRON_BARS,"§b§lArena: §f"+name));
        inv.setItem(10,item(Material.ENDER_PEARL,"§b§lSpawn A","§7Set at your current location."));
        inv.setItem(12,item(Material.ENDER_EYE,"§3§lSpawn B","§7Set at your current location."));
        inv.setItem(14,item(Material.BOOK,"§e§lTemplate","§7Enter template name in chat."));
        inv.setItem(16,item(Material.LEVER,"§a§lEnable","§7Enable this arena."));
        inv.setItem(19,item(Material.REDSTONE_TORCH,"§c§lDisable","§7Disable this arena."));
        inv.setItem(21,item(Material.TNT,"§6§lReset","§7Restore the arena template."));
        inv.setItem(23,item(Material.BARRIER,"§c§lDelete","§7Delete this arena."));
        inv.setItem(18,item(Material.ARROW,"§7§lBack"));
        inv.setItem(26,item(Material.BARRIER,"§c§lClose"));
        p.openInventory(inv);
    }

    @EventHandler public void click(InventoryClickEvent e){
        String title=e.getView().getTitle();
        if(!(e.getWhoClicked() instanceof Player p) || e.getRawSlot()<0 || e.getRawSlot()>=27) return;
        if(!Set.of(SHOP,REPORTS,REPORT_PLAYERS,REPORT_STAFF,RANKS,RANK_EDIT,ARENAS,ARENA_EDIT).contains(title)) return;
        e.setCancelled(true);
        int s=e.getRawSlot();
        if(title.equals(SHOP)){
            if(s==26){p.closeInventory();return;}
            ItemStack clicked=e.getCurrentItem();
            if(clicked==null||clicked.getType()==Material.AIR)return;
            ItemMeta meta=clicked.getItemMeta(); if(meta==null)return;
            String name=ChatColor.stripColor(meta.getDisplayName());
            purchase(p,name); return;
        }
        if(title.equals(REPORTS)){
            if(s==10) reportPlayers(p); else if(s==14&&p.hasPermission("voidflame.report.view")) staffReports(p); else if(s==18) plugin.open(p); else if(s==26)p.closeInventory(); return;
        }
        if(title.equals(REPORT_PLAYERS)){
            if(s==18){openReports(p);return;} if(s==26){p.closeInventory();return;}
            ItemMeta meta=e.getCurrentItem()==null?null:e.getCurrentItem().getItemMeta();
            if(meta==null)return;
            String target=ChatColor.stripColor(meta.getDisplayName());
            Player t=Bukkit.getPlayerExact(target); if(t==null){p.sendMessage("§cPlayer is no longer online.");return;}
            input.put(p.getUniqueId(),new Input(Type.REPORT_REASON,t.getUniqueId().toString()));
            p.closeInventory(); p.sendMessage("§eType the report reason in chat. §7Type §ccancel §7to abort."); return;
        }
        if(title.equals(REPORT_STAFF)){
            if(s==18){openReports(p);return;} if(s==26){p.closeInventory();return;}
            List<ItemStack> items=new ArrayList<>();
            for(int i=0;i<21;i++) if(e.getInventory().getItem(i)!=null) items.add(e.getInventory().getItem(i));
            if(s>=items.size())return;
            ItemMeta meta=items.get(s).getItemMeta(); if(meta==null)return;
            String target=ChatColor.stripColor(meta.getDisplayName());
            String id=null;
            storage.database().query("SELECT report_id FROM reports WHERE status='OPEN' AND target=? ORDER BY timestamp DESC LIMIT 1",target)
                .thenAccept(rows->{if(rows.isEmpty())return; String rid=String.valueOf(rows.getFirst().get("report_id")); storage.database().update("UPDATE reports SET status=?,staff=? WHERE report_id=?",(e.isLeftClick()?"RESOLVED":"DISMISSED"),p.getUniqueId().toString(),rid).thenRun(()->Bukkit.getScheduler().runTask(plugin,()->staffReports(p)));});
            return;
        }
        if(title.equals(RANKS)){
            if(s==18){plugin.open(p);return;} if(s==26){p.closeInventory();return;}
            if(s==21){input.put(p.getUniqueId(),new Input(Type.RANK_PLAYER,""));p.closeInventory();p.sendMessage("§eType player name then rank id, e.g. Steve admin.");return;}
            if(s==22){input.put(p.getUniqueId(),new Input(Type.RANK_ID,""));p.closeInventory();p.sendMessage("§eType the new rank id.");return;}
            ItemMeta meta=e.getCurrentItem()==null?null:e.getCurrentItem().getItemMeta(); if(meta!=null) rankEdit(p,ChatColor.stripColor(meta.getDisplayName())); return;
        }
        if(title.equals(RANK_EDIT)){
            if(s==18){openRanks(p);return;} if(s==26){p.closeInventory();return;}
            ItemMeta meta=e.getInventory().getItem(4).getItemMeta(); if(meta==null)return;
            String id=ChatColor.stripColor(meta.getDisplayName()).replace("Rank: ","").trim();
            Type type=s==10?Type.RANK_ID:s==12?Type.RANK_PERMISSION:s==14?Type.RANK_ID:s==16?null:s==21?Type.RANK_PLAYER:null;
            if(s==16){storage.database().execute("DELETE FROM module_data WHERE module='ranks' AND (data_key=? OR data_key=?)","rank."+id,"perm."+id).thenRun(()->openRanks(p));return;}
            if(type!=null){input.put(p.getUniqueId(),new Input(type,id));p.closeInventory();p.sendMessage("§eEnter value in chat. §7Type §ccancel §7to abort.");}
            if(s==21){input.put(p.getUniqueId(),new Input(Type.RANK_PLAYER,id));p.closeInventory();p.sendMessage("§eType player name.");}
            return;
        }
        if(title.equals(ARENAS)){
            if(s==18){plugin.open(p);return;} if(s==26){p.closeInventory();return;}
            if(s==21){input.put(p.getUniqueId(),new Input(Type.ARENA_NAME,""));p.closeInventory();p.sendMessage("§eType new arena name.");return;}
            if(s==22){Bukkit.dispatchCommand(p,"arena reload");return;}
            ItemMeta meta=e.getCurrentItem()==null?null:e.getCurrentItem().getItemMeta(); if(meta!=null)arenaEdit(p,ChatColor.stripColor(meta.getDisplayName())); return;
        }
        if(title.equals(ARENA_EDIT)){
            if(s==18){openArenas(p);return;} if(s==26){p.closeInventory();return;}
            ItemMeta h=e.getInventory().getItem(4).getItemMeta(); if(h==null)return;
            String name=ChatColor.stripColor(h.getDisplayName()).replace("Arena: ","").trim();
            if(s==10)Bukkit.dispatchCommand(p,"arena setspawn "+name+" a");
            else if(s==12)Bukkit.dispatchCommand(p,"arena setspawn "+name+" b");
            else if(s==14){input.put(p.getUniqueId(),new Input(Type.ARENA_TEMPLATE,name));p.closeInventory();p.sendMessage("§eType template name.");}
            else if(s==16)Bukkit.dispatchCommand(p,"arena enable "+name);
            else if(s==19)Bukkit.dispatchCommand(p,"arena disable "+name);
            else if(s==21)Bukkit.dispatchCommand(p,"arena reset "+name);
            else if(s==23)Bukkit.dispatchCommand(p,"arena delete "+name);
        }
    }

    @EventHandler public void chat(AsyncPlayerChatEvent e){
        Input in=input.remove(e.getPlayer().getUniqueId()); if(in==null)return;
        e.setCancelled(true); Player p=e.getPlayer(); String msg=e.getMessage().trim();
        if(msg.equalsIgnoreCase("cancel")){p.sendMessage("§7Cancelled.");return;}
        Bukkit.getScheduler().runTask(plugin,()->handleInput(p,in,msg));
    }

    private void handleInput(Player p,Input in,String msg){
        switch(in.type()){
            case REPORT_REASON -> {
                Player t=Bukkit.getPlayer(UUID.fromString(in.value()));
                if(t==null){p.sendMessage("§cPlayer is offline.");return;}
                storage.database().execute("INSERT INTO reports(report_id,reporter,target,reason,status,staff,timestamp) VALUES(?,?,?,?,'OPEN',NULL,?)",
                    UUID.randomUUID().toString(),p.getUniqueId().toString(),t.getUniqueId().toString(),msg,System.currentTimeMillis());
                p.sendMessage("§aReport submitted.");
            }
            case RANK_ID -> {
                if(in.value().isBlank()){storage.database().execute("INSERT INTO module_data(module,data_key,data_value,updated_at) VALUES('ranks',?,?,datetime('now'))","rank."+msg,
                    Base64.getEncoder().encodeToString(msg.getBytes()),"").thenRun(()->openRanks(p));}
                else storage.database().update("UPDATE module_data SET data_value=? WHERE module='ranks' AND data_key=?","", "rank."+in.value()).thenRun(()->p.sendMessage("§aRank value updated. Use the native rank command for full encoding."));
            }
            case RANK_PLAYER -> {
                String[] a=msg.split("\\s+"); if(a.length<2){p.sendMessage("§cUse: Player rankId");return;}
                String id=a[1].toLowerCase(Locale.ROOT);
                storage.database().execute("INSERT INTO module_data(module,data_key,data_value,updated_at) VALUES('ranks',?,?,datetime('now')) ON CONFLICT(module,data_key) DO UPDATE SET data_value=excluded.data_value,updated_at=datetime('now')","player."+Bukkit.getOfflinePlayer(a[0]).getUniqueId(),id);
                p.sendMessage("§aRank assignment saved.");
            }
            case RANK_PERMISSION -> storage.put("ranks","perm."+in.value(),msg).thenRun(()->p.sendMessage("§aPermission set saved."));
            case ARENA_NAME -> Bukkit.dispatchCommand(p,"arena create "+msg);
            case ARENA_TEMPLATE -> Bukkit.dispatchCommand(p,"arena settemplate "+in.value()+" "+msg);
        }
    }

    private void purchase(Player p,String displayName){
        storage.database().query("SELECT product_id,price FROM shop_products WHERE display_name=? AND enabled=1 LIMIT 1",displayName)
            .thenAccept(rows->{
                if(rows.isEmpty()){p.sendMessage("§cProduct unavailable.");return;}
                String id=String.valueOf(rows.getFirst().get("product_id")); long price=((Number)rows.getFirst().get("price")).longValue();
                storage.database().query("SELECT coins FROM player_profiles WHERE uuid=?",p.getUniqueId().toString()).thenAccept(coinsRows->{
                    long coins=coinsRows.isEmpty()?0:((Number)coinsRows.getFirst().get("coins")).longValue();
                    if(coins<price){p.sendMessage("§cNot enough coins.");return;}
                    storage.database().update("UPDATE player_profiles SET coins=coins-? WHERE uuid=? AND coins>=?",price,p.getUniqueId().toString(),price)
                        .thenCompose(changed->changed==1?storage.database().execute("INSERT OR IGNORE INTO player_shop_purchases(uuid,product_id,purchased_at) VALUES(?,?,?)",p.getUniqueId().toString(),id,System.currentTimeMillis()):java.util.concurrent.CompletableFuture.completedFuture(null))
                        .thenRun(()->Bukkit.getScheduler().runTask(plugin,()->{p.sendMessage("§aPurchase completed.");queryProducts(p);}));
                });
            });
    }

    private void fill(Inventory inv){ItemStack x=item(Material.GRAY_STAINED_GLASS_PANE," ");for(int i=0;i<27;i++)inv.setItem(i,x.clone());}
    private ItemStack item(Material m,String n,String... lore){ItemStack x=new ItemStack(m);ItemMeta meta=x.getItemMeta();if(meta!=null){meta.setDisplayName(n);meta.setLore(List.of(lore));x.setItemMeta(meta);}return x;}
    private Material material(String s){try{return Material.valueOf(s.toUpperCase(Locale.ROOT));}catch(Exception e){return Material.CHEST;}}
}