package net.voidflame.menus;

import net.voidflame.core.storage.PlayerSettingsService;
import net.voidflame.core.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoidFlameMenusPlugin extends JavaPlugin implements Listener {
    private static final String MAIN = "§8VoidFlame §7• §fPractice";
    private static final String DUELS = "§8VoidFlame §7• §fDuels";
    private static final String STATS = "§8VoidFlame §7• §fStats";
    private static final String SETTINGS = "§8VoidFlame §7• §fSettings";

    private final Map<UUID, Boolean> settingBusy = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClicks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> settingsCache = new ConcurrentHashMap<>();
    private StorageService storage;
    private PlayerSettingsService playerSettings;

    private record Setting(String key, Material material, String label, boolean defaultValue, String on, String off) {}
    private static final List<Setting> SETTINGS_LIST = List.of(
            new Setting("duel_requests", Material.IRON_SWORD, "Duel Requests", true, "Enabled", "Disabled"),
            new Setting("party_invites", Material.CAKE, "Party Invites", true, "Enabled", "Disabled"),
            new Setting("explosion_effects", Material.WIND_CHARGE, "Explosion Effects", false, "Enabled", "Disabled"),
            new Setting("kit_profile", Material.BOOK, "Kit Profile", false, "Visible", "Hidden"),
            new Setting("personal_level", Material.NAME_TAG, "Personal Level", false, "Enabled", "Disabled"),
            new Setting("friend_requests", Material.PLAYER_HEAD, "Friend Requests", false, "Enabled", "Disabled"),
            new Setting("private_messages", Material.WRITABLE_BOOK, "Private Messages", false, "Friends Only", "Disabled"),
            new Setting("friend_join_notifications", Material.BELL, "Friend Join Notifications", true, "Enabled", "Disabled"),
            new Setting("scoreboard", Material.DARK_OAK_HANGING_SIGN, "Scoreboard", true, "Enabled", "Disabled"),
            new Setting("show_players", Material.ENDER_EYE, "Show Players", true, "Enabled", "Disabled")
    );

    @Override public void onEnable() {
        saveDefaultConfig();
        var r = getServer().getServicesManager().getRegistration(StorageService.class);
        var s = getServer().getServicesManager().getRegistration(PlayerSettingsService.class);
        if (r == null || (storage = r.getProvider()) == null || s == null || (playerSettings = s.getProvider()) == null) {
            getLogger().severe("VoidFlame-Core storage unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VoidFlame-Menus enabled.");
    }

    public void open(Player player) { open(player, MAIN); }

    private void open(Player player, String title) {
        Inventory inv = Bukkit.createInventory(null, 54, title);
        decorate(inv);
        switch (title) {
            case MAIN -> main(player, inv);
            case DUELS -> duels(inv);
            case STATS -> stats(inv);
            case SETTINGS -> settings(player, inv);
            default -> {}
        }
        player.openInventory(inv);
    }

    private void main(Player p, Inventory inv) {
        inv.setItem(4, head(p, "§d§l" + p.getName(),
                "§7VoidFlameMC Practice", "", "§fPing: §d" + p.getPing() + "ms",
                "§fStatus: §aOnline", "", "§8Your practice profile"));

        button(inv, 20, Material.DIAMOND_SWORD, "§b§lUnranked", "§7Casual matchmaking.", "§7Choose a kit after opening.", "§8No rating change");
        button(inv, 22, Material.NETHER_STAR, "§d§lRanked", "§7Competitive matchmaking.", "§7ELO-based queue.", "§8Climb your rating");
        button(inv, 24, Material.PLAYER_HEAD, "§6§lParty", "§7Fight with your friends.", "§7Team duels, FFA and more.");

        button(inv, 29, Material.CHEST, "§a§lKits", "§7Browse kits and layouts.", "§eClick to open");
        button(inv, 31, Material.GOLD_INGOT, "§6§lCoin Shop", "§7Spend your practice coins.", "§eClick to open");
        button(inv, 33, Material.EMERALD, "§e§lStats", "§7Wins, losses, streak and ELO.", "§eClick to open");

        button(inv, 38, Material.ENDER_EYE, "§d§lPractice", "§7FFA and training.", "§eClick to open");
        button(inv, 40, Material.BOOK, "§f§lMatch History", "§7Review completed matches.", "§eClick to open");
        button(inv, 42, Material.COMPARATOR, "§e§lSettings", "§7Personal gameplay settings.", "§eClick to open");

        button(inv, 47, Material.PAPER, "§c§lReports", "§7Report a player.", "§eClick to open");
        button(inv, 49, Material.NETHER_STAR, "§5§lServer", "§7VoidFlameMC Practice", "§7play.VoidFlame.net");
        button(inv, 51, Material.BARRIER, "§c§lClose", "§7Close this menu.");

        if (p.hasPermission("voidflame.arena.manage"))
            button(inv, 45, Material.IRON_BARS, "§b§lArena Admin", "§7Manage practice arenas.");
        if (p.hasPermission("voidflame.ranks.admin"))
            button(inv, 53, Material.NAME_TAG, "§5§lRank Admin", "§7Manage ranks and assignments.");
    }

    private void duels(Inventory inv) {
        button(inv, 20, Material.DIAMOND_SWORD, "§b§lUnranked Queue", "§7Casual matchmaking.", "§eClick to open queue");
        button(inv, 22, Material.NETHER_STAR, "§d§lRanked Queue", "§7ELO matchmaking.", "§eClick to open queue");
        button(inv, 24, Material.PAPER, "§f§lDuel Player", "§7Send a direct duel request.");
        button(inv, 31, Material.ENDER_EYE, "§d§lSpectate", "§7Watch an active duel.");
        button(inv, 49, Material.ARROW, "§7§lBack");
        button(inv, 51, Material.BARRIER, "§c§lClose");
    }

    private void stats(Inventory inv) {
        button(inv, 20, Material.PLAYER_HEAD, "§e§lMy Stats", "§7Wins, losses, streak and ELO.", "§eClick to open");
        button(inv, 22, Material.PAPER, "§b§lMatch History", "§7Recent completed matches.", "§eClick to open");
        button(inv, 24, Material.GOLD_INGOT, "§6§lLeaderboard", "§7Top players.", "§eClick to open");
        button(inv, 49, Material.ARROW, "§7§lBack");
        button(inv, 51, Material.BARRIER, "§c§lClose");
    }

    private void settings(Player p, Inventory inv) {
        button(inv, 4, Material.COMPASS, "§e§lSettings", "§7Customize your practice experience.");
        int[] slots = {19, 21, 23, 25, 28, 30, 32, 34, 37, 39};
        for (int i = 0; i < SETTINGS_LIST.size(); i++) {
            Setting s = SETTINGS_LIST.get(i);
            boolean value = getSetting(p, s);
            button(inv, slots[i], s.material(), "§f" + s.label(),
                    "§7Status: " + (value ? "§a" + s.on() : "§c" + s.off()), "§8Click to toggle");
        }
        button(inv, 49, Material.ARROW, "§7§lBack");
        button(inv, 51, Material.BARRIER, "§c§lClose");
    }

    private void decorate(Inventory inv) {
        ItemStack border = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack accent = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inv.getSize(); slot++) {
            int row = slot / 9, col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(slot, border.clone());
        }
        for (int slot : new int[]{1,2,3,5,6,7,46,48,50,52}) inv.setItem(slot, accent.clone());
    }

    private void button(Inventory inv, int slot, Material material, String name, String... lore) {
        inv.setItem(slot, item(material, name, lore));
    }

    private ItemStack head(Player p, String name, String... lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        if (stack.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(p);
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean getSetting(Player p, Setting s) {
        Map<String, Boolean> values = settingsCache.get(p.getUniqueId());
        return values == null ? s.defaultValue() : values.getOrDefault(s.key(), s.defaultValue());
    }

    private void loadSettings(Player p) {
        UUID id = p.getUniqueId();
        Map<String, Boolean> values = new ConcurrentHashMap<>();
        settingsCache.put(id, values);
        for (Setting s : SETTINGS_LIST) playerSettings.get(id, s.key(), s.defaultValue()).thenAccept(v -> values.put(s.key(), v));
    }

    private void toggle(Player p, Setting s) {
        if (settingBusy.putIfAbsent(p.getUniqueId(), true) != null) return;
        boolean value = !getSetting(p, s);
        playerSettings.set(p.getUniqueId(), s.key(), value).whenComplete((ignored, error) ->
                Bukkit.getScheduler().runTask(this, () -> {
                    settingBusy.remove(p.getUniqueId());
                    if (error != null) { p.sendMessage(ChatColor.RED + "Could not save setting."); return; }
                    settingsCache.computeIfAbsent(p.getUniqueId(), id -> new ConcurrentHashMap<>()).put(s.key(), value);
                    open(p, SETTINGS);
                }));
    }

    private void command(Player p, String command) {
        p.closeInventory();
        Bukkit.dispatchCommand(p, command);
    }

    @EventHandler public void onJoin(PlayerJoinEvent e) { loadSettings(e.getPlayer()); }

    @EventHandler public void click(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(MAIN) && !title.equals(DUELS) && !title.equals(STATS) && !title.equals(SETTINGS)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() < 0 || e.getRawSlot() >= e.getInventory().getSize()) return;

        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, getConfig().getLong("settings.click-cooldown-ms", 250L));
        Long previous = lastClicks.put(p.getUniqueId(), now);
        if (previous != null && now - previous < cooldown) return;

        if (title.equals(MAIN)) {
            switch (e.getRawSlot()) {
                case 20 -> command(p, "queue sword");
                case 22 -> command(p, "queue ranked sword");
                case 24 -> command(p, "party info");
                case 29 -> command(p, "kits");
                case 31 -> command(p, "coinshop");
                case 33 -> open(p, STATS);
                case 38 -> command(p, "practice");
                case 40 -> command(p, "history");
                case 42 -> open(p, SETTINGS);
                case 45 -> { if (p.hasPermission("voidflame.arena.manage")) command(p, "arena list"); }
                case 47 -> command(p, p.hasPermission("voidflame.report.view") ? "reports" : "report");
                case 49 -> {
                    p.sendMessage("§d§lVoidFlameMC §8• §fPractice");
                    p.sendMessage("§7Server: §fplay.VoidFlame.net");
                    p.sendMessage("§7Queue: §f/queue §8• §7Duels: §f/duel §8• §7Stats: §f/stats");
                }
                case 51 -> p.closeInventory();
                case 53 -> { if (p.hasPermission("voidflame.ranks.admin")) command(p, "ranks"); }
                default -> {}
            }
        } else if (title.equals(DUELS)) {
            switch (e.getRawSlot()) {
                case 20 -> command(p, "queue sword");
                case 22 -> command(p, "queue ranked sword");
                case 24 -> command(p, "duel");
                case 31 -> command(p, "spectate");
                case 49 -> open(p, MAIN);
                case 51 -> p.closeInventory();
                default -> {}
            }
        } else if (title.equals(STATS)) {
            switch (e.getRawSlot()) {
                case 20 -> command(p, "stats");
                case 22 -> command(p, "history");
                case 24 -> command(p, "stats top");
                case 49 -> open(p, MAIN);
                case 51 -> p.closeInventory();
                default -> {}
            }
        } else {
            int[] slots = {19,21,23,25,28,30,32,34,37,39};
            for (int i = 0; i < slots.length; i++) if (e.getRawSlot() == slots[i]) { toggle(p, SETTINGS_LIST.get(i)); return; }
            if (e.getRawSlot() == 49) open(p, MAIN);
            else if (e.getRawSlot() == 51) p.closeInventory();
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        settingsCache.remove(id);
        settingBusy.remove(id);
        lastClicks.remove(id);
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        open(p);
        return true;
    }
}
