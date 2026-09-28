package net.voidflame.menus;

import net.voidflame.core.storage.StorageService;
import net.voidflame.core.storage.PlayerSettingsService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoidFlameMenusPlugin extends JavaPlugin implements Listener {
    private static final String MAIN = "§8VoidFlame";
    private static final String DUELS = "§8VoidFlame • Duels";
    private static final String STATS = "§8VoidFlame • Stats";
    private static final String SETTINGS = "§8VoidFlame • Settings";
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
        var settingsRegistration = getServer().getServicesManager().getRegistration(PlayerSettingsService.class);
        if (r == null || (storage = r.getProvider()) == null || settingsRegistration == null || (playerSettings = settingsRegistration.getProvider()) == null) {
            getLogger().severe("VoidFlame-Core storage unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VoidFlame-Menus enabled.");
    }

    public void open(Player p) { open(p, MAIN); }

    private void open(Player p, String title) {
        Inventory inv = Bukkit.createInventory(null, 45, title);
        fill(inv);
        if (title.equals(MAIN)) {
            button(inv, 11, Material.DIAMOND_SWORD, "§bDuels", "§7Queue, request and spectate.");
            button(inv, 13, Material.CHEST, "§aKits", "§7Browse and equip kits.");
            button(inv, 15, Material.NETHERITE_HELMET, "§eStats", "§7Your profile and leaderboard.");
            button(inv, 29, Material.NETHER_STAR, "§dPractice", "§7Open practice features.");
            button(inv, 31, Material.BOOK, "§6Server", "§7Server information.");
            button(inv, 33, Material.BARRIER, "§cClose");
            button(inv, 20, Material.COMPARATOR, "§eSettings", "§7Personal settings");
        } else if (title.equals(DUELS)) {
            button(inv, 10, Material.DIAMOND_SWORD, "§bUnranked Queue", "§7Join the standard matchmaking queue.", "§8Click to join.");
            button(inv, 11, Material.NETHER_STAR, "§dRanked Queue", "§7Match by ELO.", "§8Click to join.");
            button(inv, 13, Material.PAPER, "§fDuel Player", "§7Send a direct duel request.");
            button(inv, 16, Material.ENDER_EYE, "§dSpectate", "§7Use /spectate <player>.");
            button(inv, 31, Material.ARROW, "§7Back");
            button(inv, 33, Material.BARRIER, "§cClose");
        } else if (title.equals(STATS)) {
            button(inv, 11, Material.PLAYER_HEAD, "§eMy Stats", "§7Use /stats.");
            button(inv, 15, Material.GOLD_INGOT, "§6Leaderboard", "§7Use /stats top.");
            button(inv, 31, Material.ARROW, "§7Back");
            button(inv, 33, Material.BARRIER, "§cClose");
        } else if (title.equals(SETTINGS)) {
            button(inv, 10, Material.LIME_DYE, "§aConnection", "§7Connected to server", "§7Status: §aOnline");
            for (int i = 0; i < SETTINGS_LIST.size(); i++) {
                Setting s = SETTINGS_LIST.get(i);
                boolean value = getSetting(p, s);
                button(inv, 11 + i, s.material(), "§e" + s.label(), "§7Status: " + (value ? "§a" + s.on() : "§c" + s.off()), "§8Click to toggle");
            }
            button(inv, 31, Material.ARROW, "§7Back");
            button(inv, 33, Material.BARRIER, "§cClose");
        }
        p.openInventory(inv);
    }

    private void fill(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) if (i / 9 == 0 || i / 9 == 4) inv.setItem(i, pane.clone());
    }

    private void button(Inventory inv, int slot, Material material, String name, String... lore) {
        inv.setItem(slot, item(material, name, lore));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); meta.setLore(List.of(lore)); stack.setItemMeta(meta); }
        return stack;
    }

    private boolean getSetting(Player p, Setting s) {
        Map<String, Boolean> values = settingsCache.get(p.getUniqueId());
        if (values == null) return s.defaultValue();
        return values.getOrDefault(s.key(), s.defaultValue());
    }

    private void loadSettings(Player player) {
        UUID id = player.getUniqueId();
        Map<String, Boolean> values = new ConcurrentHashMap<>();
        settingsCache.put(id, values);
        for (Setting setting : SETTINGS_LIST) {
            playerSettings.get(id, setting.key(), setting.defaultValue()).thenAccept(value ->
                    values.put(setting.key(), value));
        }
    }

    private void toggle(Player p, Setting s) {
        if (settingBusy.putIfAbsent(p.getUniqueId(), true) != null) return;
        boolean current = getSetting(p, s);
        boolean value = !current;
        playerSettings.set(p.getUniqueId(), s.key(), value).whenComplete((ignored, error) ->
                Bukkit.getScheduler().runTask(this, () -> {
                    settingBusy.remove(p.getUniqueId());
                    if (error != null) {
                        p.sendMessage(ChatColor.RED + "Could not save setting.");
                        return;
                    }
                    settingsCache.computeIfAbsent(p.getUniqueId(), ignoredId -> new ConcurrentHashMap<>())
                            .put(s.key(), value);
                    open(p, SETTINGS);
                }));
    }

    private void command(Player p, String command) {
        p.closeInventory();
        Bukkit.dispatchCommand(p, command);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadSettings(event.getPlayer());
    }

    @EventHandler public void click(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(MAIN) && !title.equals(DUELS) && !title.equals(STATS) && !title.equals(SETTINGS)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getRawSlot() >= e.getInventory().getSize()) return;
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, getConfig().getLong("settings.click-cooldown-ms", 250L));
        Long previous = lastClicks.put(p.getUniqueId(), now);
        if (previous != null && now - previous < cooldown) return;
        if (title.equals(MAIN)) {
            switch (e.getRawSlot()) {
                case 11 -> open(p, DUELS);
                case 13 -> command(p, "kits");
                case 15 -> open(p, STATS);
                case 20 -> open(p, SETTINGS);
                case 29 -> command(p, "practice");
                case 31 -> command(p, "help");
                case 33 -> p.closeInventory();
                default -> {}
            }
        } else if (title.equals(DUELS)) {
            switch (e.getRawSlot()) {
                case 10 -> command(p, "queue sword");
                case 11 -> command(p, "queue ranked sword");
                case 13 -> command(p, "duel");
                case 16 -> command(p, "spectate");
                case 31 -> open(p, MAIN);
                case 33 -> p.closeInventory();
                default -> {}
            }
        } else if (title.equals(STATS)) {
            switch (e.getRawSlot()) {
                case 11 -> command(p, "stats");
                case 15 -> command(p, "stats top");
                case 31 -> open(p, MAIN);
                case 33 -> p.closeInventory();
                default -> {}
            }
        } else {
            if (e.getRawSlot() >= 11 && e.getRawSlot() < 11 + SETTINGS_LIST.size()) {
                toggle(p, SETTINGS_LIST.get(e.getRawSlot() - 11));
            } else if (e.getRawSlot() == 31) {
                open(p, MAIN);
            } else if (e.getRawSlot() == 33) {
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        settingsCache.remove(event.getPlayer().getUniqueId());
        settingBusy.remove(event.getPlayer().getUniqueId());
        lastClicks.remove(event.getPlayer().getUniqueId());
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        open(p);
        return true;
    }
}
