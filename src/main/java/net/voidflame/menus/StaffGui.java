package net.voidflame.menus;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public final class StaffGui implements Listener {
    private final VoidFlameMenusPlugin plugin;
    public StaffGui(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }
    private final Map<UUID, UUID> targets = new HashMap<>();
    private final Map<UUID, String> pendingSection = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Map<UUID, Long> confirmations = new HashMap<>();
    private final Map<UUID, Action> pending = new HashMap<>();
    private boolean lockTime, lockWeather;
    private long lockedTime = 6000L;
    private boolean storm;
    private final Map<String, Boolean> flags = new HashMap<>();

    enum Action { CLEAR_INV, KICK_ALL, STOP, REMOVE_WORLD }

    public void onEnable() {
        plugin.saveDefaultConfig();
        loadState();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::enforceLocks, 1L, 20L);
        plugin.getLogger().info("VoidFlame Staff GUI enabled.");
    }

    public void onDisable() {
        targets.clear(); pendingSection.clear(); confirmations.clear(); pending.clear();
        saveState();
    }

    private void loadState() {
        flags.put("pvp", plugin.getConfig().getBoolean("flags.pvp", true));
        flags.put("block-place", plugin.getConfig().getBoolean("flags.block-place", true));
        flags.put("mob-spawning", plugin.getConfig().getBoolean("flags.mob-spawning", true));
        lockTime=plugin.getConfig().getBoolean("time-weather.lock-time",false);
        lockWeather=plugin.getConfig().getBoolean("time-weather.lock-weather",false);
        lockedTime=plugin.getConfig().getLong("time-weather.time",6000L);
        storm=plugin.getConfig().getBoolean("time-weather.storm",false);
    }

    private void saveState() {
        flags.forEach((k,v)->plugin.getConfig().set("flags."+k,v));
        plugin.getConfig().set("time-weather.lock-time",lockTime);
        plugin.getConfig().set("time-weather.lock-weather",lockWeather);
        plugin.getConfig().set("time-weather.time",lockedTime);
        plugin.getConfig().set("time-weather.storm",storm);
        plugin.saveConfig();
    }

    private Inventory gui(String type,int rows,String title) {
        Inventory i=Bukkit.createInventory(new Holder(type),rows*9,color(title));
        Material filler=Material.matchMaterial(plugin.getConfig().getString("gui.filler","AIR"));
        // AIR leaves unused slots genuinely empty; only fill when explicitly configured.
        if(filler!=null && filler!=Material.AIR) {
            ItemStack pane=item(filler," ");
            for(int n=0;n<i.getSize();n++) i.setItem(n,pane);
        }
        return i;
    }

    private ItemStack item(Material m,String name,String... lore) {
        ItemStack i=new ItemStack(m); ItemMeta meta=i.getItemMeta();
        if(meta!=null){meta.setDisplayName(color(name));meta.setLore(Arrays.stream(lore).map(this::color).toList());i.setItemMeta(meta);}
        return i;
    }
    private void put(Inventory i,int slot,Material m,String name,String... lore){if(slot<i.getSize())i.setItem(slot,item(m,name,lore));}

    public void openMain(Player p) {
        if (!AdminAccess.hasStaffAccess(p)) { msg(p,"no-permission"); return; }
        Player selected = target(p);
        List<DialogBody> body = List.of(
            DialogBody.plainMessage(Component.text("VOIDFLAME STAFF", NamedTextColor.AQUA), 300),
            DialogBody.plainMessage(Component.text("Choose a staff tool. Actions are checked again by the server.", NamedTextColor.GRAY), 300),
            DialogBody.plainMessage(Component.text("Selected target: " + (selected == null ? "None" : selected.getName()), NamedTextColor.WHITE), 300)
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(staffButton("Player Manager", "Select a player and manage player tools.", "/staffgui players", NamedTextColor.AQUA));
        actions.add(staffButton("Gamemode", "Open target gamemode controls.", "/staffgui gamemode", NamedTextColor.GREEN));
        actions.add(staffButton("Movement", "Open walk and flight speed controls.", "/staffgui speed", NamedTextColor.AQUA));
        actions.add(staffButton("Potion Effects", "Manage target potion effects.", "/staffgui potions", NamedTextColor.BLUE));
        actions.add(staffButton("Experience", "Manage target experience and levels.", "/staffgui xp", NamedTextColor.GOLD));
        actions.add(staffButton("Armor Creator", "Apply an armor set to the selected player.", "/staffgui armor", NamedTextColor.GOLD));
        actions.add(staffButton("Time & Weather", "Configure server time and weather.", "/staffgui time", NamedTextColor.WHITE));
        actions.add(staffButton("Server Manager", "Open server controls and whitelist.", "/staffgui server", NamedTextColor.RED));
        actions.add(staffButton("World Manager", "Manage worlds through VoidFlame-Core.", "/staffgui world", NamedTextColor.GREEN));
        actions.add(staffButton("Heal Target", "Restore health, hunger and remove fire.", "/staffgui heal", NamedTextColor.GREEN));
        actions.add(staffButton("Protection Mode", "Toggle target invulnerability.", "/staffgui protection", NamedTextColor.BLUE));
        actions.add(staffButton("Toggle Flight", "Toggle flight for the selected target.", "/staffgui fly", NamedTextColor.AQUA));
        actions.add(staffButton("Repair Items", "Repair target inventory and armor.", "/staffgui repair", NamedTextColor.GOLD));
        actions.add(staffButton("Clear Inventory", "Clear the selected target inventory.", "/staffgui clearinventory", NamedTextColor.RED));
        actions.add(staffButton("Clear Arrows", "Remove arrows embedded in the target.", "/staffgui cleararrows", NamedTextColor.WHITE));
        actions.add(staffButton("Kick All", "Open the existing confirmation before kicking players.", "/staffgui kickall", NamedTextColor.RED));
        actions.add(staffButton("My Inventory", "Open your own inventory.", "/staffgui myinventory", NamedTextColor.AQUA));
        actions.add(staffButton("Workbench", "Open the workbench.", "/staffgui workbench", NamedTextColor.GOLD));
        actions.add(staffButton("Clear Chat", "Clear public chat.", "/staffgui clearchat", NamedTextColor.GRAY));
        actions.add(staffButton("Server Flags", "Configure PvP, block placement and mob spawning.", "/staffgui flags", NamedTextColor.BLUE));
        actions.add(staffButton("Return to Spawn", "Close staff tools and return to spawn.", "/spawn", NamedTextColor.GRAY));
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff", NamedTextColor.WHITE))
                .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                .canCloseWithEscape(true)
                .body(body)
                .build())
            .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    private void openNativeOptions(Player p, String type, String title, Map<Integer, String> options) {
        if (!AdminAccess.hasStaffAccess(p)) { msg(p, "no-permission"); return; }
        List<ActionButton> actions = new ArrayList<>();
        options.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                actions.add(staffButton(entry.getValue(), "Select " + entry.getValue() + ".", "/staffgui action " + type + " " + entry.getKey(), entry.getKey() == 15 || entry.getKey() == 17 ? NamedTextColor.GRAY : NamedTextColor.AQUA)));
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title, NamedTextColor.WHITE))
                        .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                        .canCloseWithEscape(true)
                        .body(List.of(
                                DialogBody.plainMessage(Component.text("VOIDFLAME STAFF TOOLS", NamedTextColor.AQUA), 300),
                                DialogBody.plainMessage(Component.text("Choose an action. Server-side permission checks remain active.", NamedTextColor.GRAY), 300)))
                        .build())
                .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    public void dialogAction(Player p, String type, int slot) {
        if (!AdminAccess.hasStaffAccess(p)) { msg(p, "no-permission"); return; }
        switch (type.toLowerCase(Locale.ROOT)) {
            case "gamemode" -> { if (slot == 15) openMain(p); else gamemode(p, slot); }
            case "speed" -> speed(p, slot);
            case "potions" -> potions(p, slot);
            case "xp" -> xp(p, slot);
            case "armor" -> armorMenu(p, slot);
            case "server" -> server(p, slot);
            case "time" -> time(p, slot);
            case "flags" -> flagMenu(p, slot);
            case "confirm" -> confirm(p, slot);
            case "player" -> playerManagerDialogAction(p, slot);
            default -> openMain(p);
        }
    }

    private ActionButton staffButton(String label, String tooltip, String command, NamedTextColor color) {
        return ActionButton.create(Component.text(label, color), Component.text(tooltip, NamedTextColor.GRAY),
            180, DialogAction.commandTemplate(command));
    }

    private void playerManagerDialogAction(Player p, int slot) {
        Player t = target(p);
        if (slot == 17) { openMain(p); return; }
        if (slot == 10) { openPlayers(p); return; }
        if (t == null) { p.sendMessage(color("&cSelect a target first.")); openPlayers(p); return; }
        switch (slot) {
            case 11 -> p.sendMessage(color("&8Health: &f" + String.format(Locale.ROOT, "%.1f", t.getHealth())
                    + " &7| Food: &f" + t.getFoodLevel() + " &7| XP: &f" + t.getLevel()
                    + " &7| Mode: &f" + t.getGameMode() + " &7| World: &f" + t.getWorld().getName()));
            case 12 -> { if (require(p, "voidflame.staff.manage")) p.openInventory(t.getInventory()); }
            case 13 -> { if (require(p, "voidflame.staff.basic")) { heal(t); done(p, "Heal Target"); openPlayerManager(p); } }
            case 14 -> { if (require(p, "voidflame.staff.moderate")) confirm(p, Action.CLEAR_INV); }
            case 15 -> { if (require(p, "voidflame.staff.basic")) { p.teleport(t.getLocation()); done(p, "Teleport To"); openPlayerManager(p); } }
            case 16 -> { if (require(p, "voidflame.staff.manage")) {
                if (frozen.contains(t.getUniqueId())) { frozen.remove(t.getUniqueId()); done(p, "Unfreeze"); }
                else { frozen.add(t.getUniqueId()); done(p, "Freeze"); }
                openPlayerManager(p);
            } }
            default -> openPlayerManager(p);
        }
    }

    public void worldDialogAction(Player p, String action, String rawId) {
        if (!require(p, "voidflame.staff.world")) return;
        try {
            UUID id = UUID.fromString(rawId);
            World w = Bukkit.getWorld(id);
            if ("create".equalsIgnoreCase(action)) {
                String name = "StaffWorld_" + System.currentTimeMillis() % 10000;
                try {
                    World created = AdminServices.loadWorld(AdminServices.coreWorlds(), name);
                    if (created != null) p.teleport(created.getSpawnLocation());
                    done(p, "World Created");
                } catch (Exception ex) { msg(p, "world-operation-failed"); }
                openWorlds(p);
                return;
            }
            if (w == null) { p.sendMessage(color("&cWorld is no longer loaded.")); openWorlds(p); return; }
            if ("tp".equalsIgnoreCase(action)) {
                p.teleport(w.getSpawnLocation());
                done(p, "Teleport to World");
            } else if ("remove".equalsIgnoreCase(action)) {
                if (w.equals(p.getWorld())) { msg(p, "cannot-remove-current-world"); return; }
                if (!w.getPlayers().isEmpty()) { msg(p, "world-has-players"); return; }
                if (!Bukkit.unloadWorld(w, false)) { p.sendMessage(color("&cCould not unload world.")); return; }
                deleteFolder(w.getWorldFolder());
                done(p, "World Removed");
            }
            openWorlds(p);
        } catch (IllegalArgumentException ex) {
            p.sendMessage(color("&cInvalid world identifier."));
            openWorlds(p);
        }
    }

    public void selectTarget(Player p, String rawId) {
        if (!AdminAccess.hasStaffAccess(p) || !p.hasPermission("voidflame.staff.players")) {
            msg(p, "no-permission");
            return;
        }
        try {
            UUID selectedId = UUID.fromString(rawId);
            Player selected = Bukkit.getPlayer(selectedId);
            if (selected == null) {
                p.sendMessage(color("&cThat player is no longer online."));
                openPlayers(p);
                return;
            }
            targets.put(p.getUniqueId(), selectedId);
            String requested = pendingSection.remove(p.getUniqueId());
            if (requested != null) openSection(p, requested);
            else openPlayerManager(p);
        } catch (IllegalArgumentException ex) {
            p.sendMessage(color("&cInvalid player selection."));
            openPlayers(p);
        }
    }

    public void openSection(Player p, String section) {
        if (!AdminAccess.hasStaffAccess(p)) { msg(p, "no-permission"); return; }
        String normalized = section.toLowerCase(Locale.ROOT);
        Set<String> targetActions = Set.of("heal", "gamemode", "protection", "fly", "speed", "potions",
            "repair", "armor", "clearinventory", "cleararrows", "xp");
        if (targetActions.contains(normalized) && target(p) == null) {
            pendingSection.put(p.getUniqueId(), normalized);
            openPlayers(p);
            return;
        }
        pendingSection.remove(p.getUniqueId());
        switch (normalized) {
            case "main" -> openMain(p);
            case "players" -> openPlayers(p);
            case "select" -> openPlayers(p);
            case "heal" -> main(p, 10);
            case "gamemode" -> main(p, 11);
            case "protection" -> main(p, 12);
            case "fly" -> main(p, 13);
            case "speed" -> main(p, 14);
            case "potions" -> main(p, 16);
            case "repair" -> main(p, 19);
            case "armor" -> main(p, 20);
            case "clearinventory" -> main(p, 21);
            case "cleararrows" -> main(p, 22);
            case "xp" -> main(p, 23);
            case "myinventory" -> main(p, 24);
            case "workbench" -> main(p, 25);
            case "kickall" -> main(p, 29);
            case "time" -> main(p, 30);
            case "server" -> main(p, 31);
            case "world" -> main(p, 32);
            case "clearchat" -> main(p, 33);
            case "flags" -> main(p, 34);
            default -> openMain(p);
        }
    }

    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p))return;
        if(!(e.getView().getTopInventory().getHolder() instanceof Holder h))return;
        e.setCancelled(true);
        if(!AdminAccess.hasStaffAccess(p)){msg(p,"no-permission");p.closeInventory();return;}
        if(e.getClickedInventory()!=e.getView().getTopInventory()) return;
        int s=e.getRawSlot();
        switch(h.type){
            case "main" -> main(p,s);
            case "players" -> players(p,s);
            case "player" -> playerManager(p,s);
            case "gamemode" -> gamemode(p,s);
            case "speed" -> speed(p,s);
            case "potions" -> potions(p,s);
            case "xp" -> xp(p,s);
            case "armor" -> armorMenu(p,s);
            case "server" -> server(p,s);
            case "world" -> worlds(p,s,e.isRightClick());
            case "time" -> time(p,s);
            case "flags" -> flagMenu(p,s);
            case "confirm" -> confirm(p,s);
        }
    }

    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){targets.entrySet().removeIf(x->x.getValue().equals(e.getPlayer().getUniqueId()));frozen.remove(e.getPlayer().getUniqueId());}

    @EventHandler public void freezeMove(org.bukkit.event.player.PlayerMoveEvent e){
        if(!frozen.contains(e.getPlayer().getUniqueId())) return;
        if(e.getFrom().getX()!=e.getTo().getX() || e.getFrom().getY()!=e.getTo().getY() || e.getFrom().getZ()!=e.getTo().getZ()) e.setTo(e.getFrom());
    }

    private void main(Player p,int s){
        if(!AdminAccess.hasStaffAccess(p)){msg(p,"no-permission");return;}
        if(s==28){if(require(p,"voidflame.staff.players"))openPlayers(p);return;}
        if(s==24){if(require(p,"voidflame.staff.basic"))p.openInventory(p.getInventory());return;}
        if(s==25){if(require(p,"voidflame.staff.basic"))p.openWorkbench(null,true);return;}
        Player t=target(p);
        if(t==null && s!=31 && s!=32 && s!=30 && s!=34 && s!=33){msg(p,"target-required");return;}
        switch(s){
            case 10 -> {if(require(p,"voidflame.staff.basic")){heal(t);done(p,"Heal");}}
            case 11 -> {if(require(p,"voidflame.staff.basic"))openGamemode(p);}
            case 12 -> {if(require(p,"voidflame.staff.manage")){t.setInvulnerable(!t.isInvulnerable());done(p,"Protection Mode");}}
            case 13 -> {if(require(p,"voidflame.staff.manage")){t.setAllowFlight(!t.getAllowFlight());t.setFlying(t.getAllowFlight());done(p,"Fly");}}
            case 14,15 -> {if(require(p,"voidflame.staff.basic"))openSpeed(p);}
            case 16 -> {if(require(p,"voidflame.staff.basic"))openPotions(p);}
            case 19 -> {if(require(p,"voidflame.staff.basic")){for(ItemStack x:t.getInventory().getContents())repair(x);for(ItemStack x:t.getInventory().getArmorContents())repair(x);repair(t.getInventory().getItemInOffHand());done(p,"Item Repair");}}
            case 20 -> {if(require(p,"voidflame.staff.manage"))openArmor(p);}
            case 21 -> {if(require(p,"voidflame.staff.moderate"))confirm(p,Action.CLEAR_INV);}
            case 22 -> {if(require(p,"voidflame.staff.basic")){t.setArrowsInBody(0);done(p,"Clear Arrows from Body");}}
            case 23 -> {if(require(p,"voidflame.staff.basic"))openXp(p);}
            case 29 -> {if(require(p,"voidflame.staff.admin"))confirm(p,Action.KICK_ALL);}
            case 30 -> {if(require(p,"voidflame.staff.server"))openTime(p);}
            case 31 -> {if(require(p,"voidflame.staff.server"))openServer(p);}
            case 32 -> {if(require(p,"voidflame.staff.world"))openWorlds(p);}
            case 33 -> {if(require(p,"voidflame.staff.moderate"))clearChat(p);}
            case 34 -> {if(require(p,"voidflame.staff.admin"))openFlags(p);}
        }
    }

    private boolean require(Player p,String permission){
        if(p.hasPermission(permission) || p.hasPermission("voidflame.admin.full")) return true;
        msg(p,"no-permission");
        return false;
    }

    private void heal(Player t){t.setHealth(t.getMaxHealth());t.setFireTicks(0);t.setFoodLevel(20);t.setSaturation(20);t.setExhaustion(0);}
    private void repair(ItemStack x){if(x==null||x.getType().isAir())return;ItemMeta m=x.getItemMeta();if(m instanceof Damageable d){d.setDamage(0);x.setItemMeta(d);}}
    private Player target(Player p){UUID id=targets.get(p.getUniqueId());return id==null?null:Bukkit.getPlayer(id);}

    private void openPlayers(Player p) {
        if (!AdminAccess.hasStaffAccess(p) || !p.hasPermission("voidflame.staff.players")) {
            msg(p, "no-permission");
            return;
        }
        List<ActionButton> actions = new ArrayList<>();
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        for (Player target : online) {
            if (actions.size() >= 80) break;
            actions.add(staffButton(target.getName(), "Select " + target.getName() + " as the target for the pending staff action.",
                    "/staffgui select " + target.getUniqueId(), NamedTextColor.AQUA));
        }
        actions.add(staffButton("Back to Staff", "Return to the Staff menu.", "/staffgui main", NamedTextColor.GRAY));
        List<DialogBody> body = List.of(
                DialogBody.plainMessage(Component.text("PLAYER SELECTION", NamedTextColor.AQUA), 300),
                DialogBody.plainMessage(Component.text(online.size() + " player(s) online. Select a target; permissions are checked again before actions run.", NamedTextColor.GRAY), 300)
        );
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Select Player", NamedTextColor.WHITE))
                        .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                        .canCloseWithEscape(true)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }
    private void players(Player p,int s){
        if(s==49){openMain(p);return;}ItemStack x=p.getOpenInventory().getTopInventory().getItem(s);
        if(x==null||x.getType()!=Material.PLAYER_HEAD||x.getItemMeta()==null)return;
        String n=ChatColor.stripColor(x.getItemMeta().getDisplayName());Player t=Bukkit.getPlayerExact(n);
        if(t!=null){
            targets.put(p.getUniqueId(),t.getUniqueId());
            String requested = pendingSection.remove(p.getUniqueId());
            if (requested != null) openSection(p, requested);
            else openPlayerManager(p);
        }
    }

    private void openPlayerManager(Player p) {
        if (!require(p, "voidflame.staff.players")) return;
        Player t = target(p);
        String n = t == null ? "No target selected" : "Selected: " + t.getName();
        openNativeOptions(p, "player", "Player Manager • " + n, Map.of(
                10, "Select Player", 11, "View Player Stats", 12, "Open Target Inventory",
                13, "Heal Target", 14, "Clear Target Inventory", 15, "Teleport To Target",
                16, t != null && frozen.contains(t.getUniqueId()) ? "Unfreeze Target" : "Freeze Target",
                17, "Back to Staff"));
    }
    private void playerManager(Player p,int s){
        Player t=target(p);if(s==17){openMain(p);return;}if(t==null){openPlayers(p);return;}
        if(s==10){openPlayers(p);return;}
        if(s==11){p.sendMessage(color("&8Health: &f"+String.format("%.1f",t.getHealth())+" &7| Food: &f"+t.getFoodLevel()+" &7| XP: &f"+t.getLevel()+" &7| Mode: &f"+t.getGameMode()+" &7| World: &f"+t.getWorld().getName()));}
        else if(s==12)p.openInventory(t.getInventory());
        else if(s==13){heal(t);done(p,"Heal Me");}
        else if(s==14)confirm(p,Action.CLEAR_INV);
        else if(s==15){p.teleport(t.getLocation());done(p,"Teleport To");openPlayerManager(p);}
        else if(s==16){if(frozen.contains(t.getUniqueId())){frozen.remove(t.getUniqueId());done(p,"Unfreeze");}else{frozen.add(t.getUniqueId());done(p,"Freeze");}openPlayerManager(p);}
        else if(s==15){p.teleport(t.getLocation());done(p,"Teleport To");openPlayerManager(p);}
        else if(s==16){if(frozen.contains(t.getUniqueId())){frozen.remove(t.getUniqueId());done(p,"Unfreeze");}else{frozen.add(t.getUniqueId());done(p,"Freeze");}openPlayerManager(p);}
    }

    private void openGamemode(Player p){openNativeOptions(p,"gamemode","Gamemode",Map.of(10,"Survival",11,"Creative",12,"Adventure",13,"Spectator",15,"Back"));}
    private void gamemode(Player p,int s){Player t=target(p);if(s==17){openMain(p);return;}if(t==null)return;GameMode g=s==10?GameMode.SURVIVAL:s==11?GameMode.CREATIVE:s==12?GameMode.ADVENTURE:s==13?GameMode.SPECTATOR:null;if(g!=null){t.setGameMode(g);done(p,"Gamemode");openMain(p);}}

    private void openSpeed(Player p){openNativeOptions(p,"speed","Speed Controls",Map.of(10,"Increase Walk Speed",11,"Increase Fly Speed",12,"Reset Speeds",15,"Back"));}
    private void speed(Player p,int s){Player t=target(p);if(s==15){openMain(p);return;}if(t==null)return;float step=(float)plugin.getConfig().getDouble("settings.speed-step",0.1),max=(float)plugin.getConfig().getDouble("settings.max-speed",1.0);if(s==10)t.setWalkSpeed(next(t.getWalkSpeed(),step,max));else if(s==11)t.setFlySpeed(next(t.getFlySpeed(),step,max));else if(s==12){t.setWalkSpeed(0.2f);t.setFlySpeed(0.1f);}done(p,"Speed");openMain(p);}
    private float next(float cur,float step,float max){return cur>=max-0.001?0.1f:Math.min(max,cur+step);}

    private void openPotions(Player p){openNativeOptions(p,"potions","Potion Effects",Map.of(10,"Speed II • 2 minutes",11,"Resistance II • 2 minutes",12,"Jump Boost II • 2 minutes",13,"Haste II • 2 minutes",14,"Clear Effects",15,"Back"));}
    private void potions(Player p,int s){Player t=target(p);if(s==15){openMain(p);return;}if(t==null)return;PotionEffectType type=s==10?PotionEffectType.SPEED:s==11?PotionEffectType.RESISTANCE:s==12?PotionEffectType.JUMP_BOOST:s==13?PotionEffectType.HASTE:null;if(type!=null)t.addPotionEffect(new PotionEffect(type,2400,1,true,false,true));else if(s==14)t.clearActivePotionEffects();done(p,"Potion Effects");openPotions(p);}

    private void openXp(Player p){openNativeOptions(p,"xp","Experience Manager",Map.of(10,"Add 5 Levels",11,"Remove 5 Levels",12,"Reset XP",13,"Add 1000 XP",15,"Back"));}
    private void xp(Player p,int s){Player t=target(p);if(s==15){openMain(p);return;}if(t==null)return;if(s==10)t.giveExpLevels(5);else if(s==11)t.giveExpLevels(-5);else if(s==12){t.setExp(0);t.setLevel(0);t.setTotalExperience(0);}else if(s==13)t.giveExp(1000);done(p,"Experience Manager");openXp(p);}

    private void openArmor(Player p){openNativeOptions(p,"armor","Armor Creator",Map.of(10,"Iron Armor",11,"Diamond Armor",12,"Netherite Armor",13,"Enchanted Netherite • Protection IV",15,"Back"));}
    private void armorMenu(Player p,int s){Player t=target(p);if(s==15){openMain(p);return;}if(t==null)return;Material base=s==10?Material.IRON_CHESTPLATE:s==11?Material.DIAMOND_CHESTPLATE:Material.NETHERITE_CHESTPLATE;if(s==13)base=Material.NETHERITE_CHESTPLATE;Material h=base==Material.IRON_CHESTPLATE?Material.IRON_HELMET:base==Material.DIAMOND_CHESTPLATE?Material.DIAMOND_HELMET:Material.NETHERITE_HELMET;Material l=base==Material.IRON_CHESTPLATE?Material.IRON_LEGGINGS:base==Material.DIAMOND_CHESTPLATE?Material.DIAMOND_LEGGINGS:Material.NETHERITE_LEGGINGS;Material b=base==Material.IRON_CHESTPLATE?Material.IRON_BOOTS:base==Material.DIAMOND_CHESTPLATE?Material.DIAMOND_BOOTS:Material.NETHERITE_BOOTS;ItemStack[] set={new ItemStack(b),new ItemStack(l),new ItemStack(base),new ItemStack(h)};for(ItemStack x:set)if(s==13){x.addUnsafeEnchantment(Enchantment.PROTECTION,4);x.addUnsafeEnchantment(Enchantment.UNBREAKING,3);}t.getInventory().setArmorContents(set);done(p,"Armor Creator");openMain(p);}

    private void openServer(Player p){if(!require(p,"voidflame.staff.server"))return;openNativeOptions(p,"server","Server Manager",Map.of(10,"Reload Admin Configuration",11,"Stop Server",12,"Whitelist: "+(Bukkit.hasWhitelist()?"Enabled":"Disabled"),15,"Back"));}
    private void server(Player p,int s){if(s==15){openMain(p);return;}if(!p.hasPermission("voidflame.staff.server")){msg(p,"no-permission");return;}if(s==10){plugin.reloadConfig();loadState();done(p,"Reload Admin Configuration");openMain(p);}else if(s==11)confirm(p,Action.STOP);else if(s==12){Bukkit.setWhitelist(!Bukkit.hasWhitelist());done(p,"Whitelist");openServer(p);}}

    private void openWorlds(Player p) {
        if (!require(p, "voidflame.staff.world")) return;
        List<ActionButton> actions = new ArrayList<>();
        List<World> worlds = new ArrayList<>(Bukkit.getWorlds());
        worlds.sort(Comparator.comparing(World::getName, String.CASE_INSENSITIVE_ORDER));
        for (World w : worlds) {
            if (actions.size() >= 74) break;
            actions.add(staffButton("Teleport: " + w.getName(), "Teleport to this world's spawn.", "/staffgui worldtp " + w.getUID(), NamedTextColor.GREEN));
            if (actions.size() < 78) actions.add(staffButton("Unload: " + w.getName(), "Unload and delete this world. This is destructive.", "/staffgui worldremove " + w.getUID(), NamedTextColor.RED));
        }
        actions.add(staffButton("Create World", "Create a new managed staff world.", "/staffgui worldcreate", NamedTextColor.AQUA));
        actions.add(staffButton("Back", "Return to Staff.", "/staffgui main", NamedTextColor.GRAY));
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("World Manager", NamedTextColor.WHITE))
                        .externalTitle(Component.text("Staff", NamedTextColor.AQUA)).canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(Component.text("World administration", NamedTextColor.AQUA), 300),
                                DialogBody.plainMessage(Component.text("Teleport and removal are separate actions. Removing a world deletes its folder.", NamedTextColor.GRAY), 300)))
                        .build())
                .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }
    private void worlds(Player p,int s,boolean rightClick){if(s==50){openMain(p);return;}if(s==49){if(!p.hasPermission("voidflame.staff.world"))return;String n="StaffWorld_"+System.currentTimeMillis()%10000;try { Object core=AdminServices.coreWorlds(); World w=AdminServices.loadWorld(core,n); if(w!=null)p.teleport(w.getSpawnLocation()); done(p,"World Created"); } catch(Exception ex){ msg(p,"world-operation-failed"); } return;}ItemStack x=p.getOpenInventory().getTopInventory().getItem(s);if(x==null||x.getItemMeta()==null)return;String n=ChatColor.stripColor(x.getItemMeta().getDisplayName());World w=Bukkit.getWorld(n);if(w==null)return;
        if(rightClick){
            if(!p.hasPermission("voidflame.staff.world")){msg(p,"no-permission");return;}
            if(w.getPlayers().stream().anyMatch(player->!player.equals(p))){msg(p,"world-has-players");return;}
            if(w.equals(p.getWorld())){msg(p,"cannot-remove-current-world");return;}
            Bukkit.unloadWorld(w,false);
            java.io.File folder=w.getWorldFolder();
            deleteFolder(folder);
            done(p,"World Removed");
            openWorlds(p);
        }else p.teleport(w.getSpawnLocation());
    }
    private void deleteFolder(java.io.File f){
        if(!f.exists())return;
        java.io.File[] children=f.listFiles();
        if(children!=null)for(java.io.File child:children)deleteFolder(child);
        f.delete();
    }

    private void openTime(Player p){if(!AdminAccess.hasStaffAccess(p))return;openNativeOptions(p,"time","Time & Weather",Map.of(10,"Day",11,"Noon",12,"Night",13,"Clear Weather",14,"Storm",15,"Toggle Time Lock ("+(lockTime?"On":"Off")+")",16,"Toggle Weather Lock ("+(lockWeather?"On":"Off")+")",17,"Back"));}
    private void time(Player p,int s){if(s==17){openMain(p);return;}if(s==10){lockedTime=1000;}else if(s==11){lockedTime=6000;}else if(s==12){lockedTime=13000;}else if(s==13){storm=false;}else if(s==14){storm=true;}else if(s==15){lockTime=!lockTime;}else if(s==16){lockWeather=!lockWeather;}applyTimeWeather();saveState();done(p,"Time / Weather");openTime(p);}
    private void applyTimeWeather(){for(World w:Bukkit.getWorlds()){if(lockTime)w.setTime(lockedTime);if(lockWeather){w.setStorm(storm);w.setThundering(false);}}}
    private void enforceLocks(){if(lockTime||lockWeather)applyTimeWeather();}

    private void openFlags(Player p){if(!require(p,"voidflame.staff.admin"))return;openNativeOptions(p,"flags","Server Flags",Map.of(10,"PvP: "+flags.get("pvp"),11,"Block Placement: "+flags.get("block-place"),12,"Mob Spawning: "+flags.get("mob-spawning"),15,"Back"));}
    private void flagMenu(Player p,int s){if(!p.hasPermission("voidflame.staff.admin")){msg(p,"no-permission");return;}if(s==15){openMain(p);return;}String k=s==10?"pvp":s==11?"block-place":s==12?"mob-spawning":null;if(k!=null){flags.put(k,!flags.get(k));saveState();done(p,"Fast Flag "+k);openFlags(p);}}

    @EventHandler public void flags(EntityDamageByEntityEvent e){if(!flags.getOrDefault("pvp",true)&&e.getEntity() instanceof Player&&e.getDamager() instanceof Player)e.setCancelled(true);}
    @EventHandler public void blocks(BlockPlaceEvent e){if(!flags.getOrDefault("block-place",true)&&!e.getPlayer().hasPermission("voidflame.staff"))e.setCancelled(true);}
    @EventHandler public void mobs(CreatureSpawnEvent e){if(!flags.getOrDefault("mob-spawning",true)&&e.getSpawnReason()==CreatureSpawnEvent.SpawnReason.NATURAL)e.setCancelled(true);}

    private void confirm(Player p,Action a){if((a==Action.CLEAR_INV||a==Action.KICK_ALL||a==Action.STOP)&&!p.hasPermission("voidflame.staff.admin")){msg(p,"no-permission");return;}pending.put(p.getUniqueId(),a);confirmations.put(p.getUniqueId(),System.currentTimeMillis()+15000);openNativeOptions(p,"confirm","Confirm Action",Map.of(11,"Confirm",15,"Cancel"));}
    private void confirm(Player p,int s){if(s==15){pending.remove(p.getUniqueId());confirmations.remove(p.getUniqueId());openMain(p);return;}if(s!=11)return;Long until=confirmations.get(p.getUniqueId());Action a=pending.get(p.getUniqueId());if(until==null||until<System.currentTimeMillis()||a==null){openMain(p);return;}pending.remove(p.getUniqueId());confirmations.remove(p.getUniqueId());Player t=target(p);switch(a){case CLEAR_INV -> {if(t!=null)t.getInventory().clear();done(p,"Clear Inventory");}case KICK_ALL -> {for(Player x:Bukkit.getOnlinePlayers())if(!x.equals(p)&&(!plugin.getConfig().getBoolean("settings.kick-all-except-staff",true)||!x.hasPermission("voidflame.staff")))x.kickPlayer(color("&cRemoved by staff."));done(p,"Kick All Players");}case STOP -> {done(p,"Stop Server");Bukkit.shutdown();}case REMOVE_WORLD -> { } }if(Bukkit.isPrimaryThread()&&!a.equals(Action.STOP))openMain(p);}

    // Public entry points used by the central Admin console.
    public void openWorldsFromAdmin(Player p){ loadState(); if(!AdminAccess.isBedrock(p) && (p.hasPermission("voidflame.admin") || p.hasPermission("voidflame.admin.full") || p.hasPermission("voidflame.admin.settings"))) openWorlds(p); else msg(p,"no-permission"); }
    public void openTimeFromAdmin(Player p){ loadState(); if(!AdminAccess.isBedrock(p) && (p.hasPermission("voidflame.admin") || p.hasPermission("voidflame.admin.full") || p.hasPermission("voidflame.admin.settings"))) openTime(p); else msg(p,"no-permission"); }
    public void openServerFromAdmin(Player p){ loadState(); if(!AdminAccess.isBedrock(p) && (p.hasPermission("voidflame.admin") || p.hasPermission("voidflame.admin.full") || p.hasPermission("voidflame.admin.settings"))) openServer(p); else msg(p,"no-permission"); }
    public void openFlagsFromAdmin(Player p){ loadState(); if(!AdminAccess.isBedrock(p) && (p.hasPermission("voidflame.admin") || p.hasPermission("voidflame.admin.full") || p.hasPermission("voidflame.admin.settings"))) openFlags(p); else msg(p,"no-permission"); }

    private void clearChat(Player p){if(!p.hasPermission("voidflame.staff.admin")){msg(p,"no-permission");return;}int lines=plugin.getConfig().getInt("settings.clear-chat-lines",100);for(Player x:Bukkit.getOnlinePlayers())for(int n=0;n<lines;n++)x.sendMessage(" ");done(p,"Clear Chat");openMain(p);}
    private void done(Player p,String a){plugin.getLogger().info("Staff action: "+p.getName()+" -> "+a+(target(p)==null?"":" -> "+target(p).getName()));p.sendMessage(color(plugin.getConfig().getString("messages.action","&aAction completed: &f%action%").replace("%action%",a)));}
    private void msg(Player p,String k){p.sendMessage(color(plugin.getConfig().getString("messages."+k,k)));}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}

    static final class Holder implements InventoryHolder {final String type;Holder(String type){this.type=type;}public Inventory getInventory(){return null;}}
}
