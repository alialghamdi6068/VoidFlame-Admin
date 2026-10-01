package net.voidflame.menus;

import net.voidflame.core.storage.PlayerSettingsService;
import net.voidflame.core.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
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
    private static final String STATS = "§8VoidFlame §7• §fStats";
    private static final String SETTINGS = "§8VoidFlame §7• §fSettings";
    private static final String QUEUE = "§8VoidFlame §7• §fQueue";
    private static final String UNRANKED = "§8VoidFlame §7• §fUnranked";
    private static final String RANKED = "§8VoidFlame §7• §fRanked";

    private final Map<UUID, Boolean> settingBusy = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClicks = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Boolean>> settingsCache = new ConcurrentHashMap<>();

    private StorageService storage;
    private PlayerSettingsService playerSettings;

    private record Setting(String key, Material material, String label, boolean defaultValue, String on, String off) {}

    private static final List<Setting> SETTINGS_LIST = List.of(
            new Setting("duel_requests", Material.PAPER, "Duel Requests", true, "Enabled", "Disabled"),
            new Setting("party_invites", Material.CAKE, "Party Invites", true, "Enabled", "Disabled"),
            new Setting("explosion_effects", Material.FIREWORK_STAR, "Match Effects", false, "Enabled", "Disabled"),
            new Setting("kit_profile", Material.BOOK, "Kit Profile", false, "Visible", "Hidden"),
            new Setting("personal_level", Material.NAME_TAG, "Personal Level", false, "Enabled", "Disabled"),
            new Setting("friend_requests", Material.PLAYER_HEAD, "Friend Requests", false, "Enabled", "Disabled"),
            new Setting("private_messages", Material.WRITABLE_BOOK, "Private Messages", false, "Friends Only", "Disabled"),
            new Setting("friend_join_notifications", Material.BELL, "Friend Join Notifications", true, "Enabled", "Disabled"),
            new Setting("scoreboard", Material.DARK_OAK_HANGING_SIGN, "Scoreboard", true, "Enabled", "Disabled"),
            new Setting("show_players", Material.ENDER_EYE, "Show Players", true, "Enabled", "Disabled")
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();

        var registration = getServer().getServicesManager().getRegistration(StorageService.class);
        var settingsRegistration = getServer().getServicesManager().getRegistration(PlayerSettingsService.class);

        if (registration == null || (storage = registration.getProvider()) == null
                || settingsRegistration == null || (playerSettings = settingsRegistration.getProvider()) == null) {
            getLogger().severe("VoidFlame-Core storage unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VoidFlame-Menus enabled.");
    }

    public void open(Player player) {
        open(player, MAIN);
    }

    private void open(Player player, String title) {
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        decorate(inventory);

        switch (title) {
            case MAIN -> main(player, inventory);
            case STATS -> stats(player, inventory);
            case SETTINGS -> settings(player, inventory);
            case QUEUE -> queue(player, inventory);
            case UNRANKED, RANKED -> kitQueue(player, inventory, title.equals(RANKED));
            default -> {
            }
        }

        player.openInventory(inventory);
        play(player, Sound.BLOCK_CHEST_OPEN, 0.65f, 1.15f);
    }

    private void main(Player player, Inventory inventory) {
        // Profile/header block
        inventory.setItem(4, head(player, "§d§l" + player.getName(),
                "§7VoidFlameMC Practice",
                "§8Your personal profile",
                "",
                "§fClick §8» §dView Stats"));

        // Primary play row — deliberately compact and centered.
        button(inventory, 10, Material.PAPER, "§d§lUnranked Queue",
                "§7Fast matchmaking with no ELO pressure.",
                "",
                "§dLeft-click §8» §fOpen Queue");
        button(inventory, 11, Material.NETHER_STAR, "§5§lRanked Queue",
                "§7Competitive matchmaking with ELO.",
                "",
                "§dLeft-click §8» §fOpen Queue");

        button(inventory, 13, Material.PLAYER_HEAD, "§b§lParty",
                "§7Create, manage and queue your party.",
                "",
                "§dLeft-click §8» §fOpen Party");

        button(inventory, 15, Material.ENDER_EYE, "§a§lPractice",
                "§7FFA and training modes.",
                "",
                "§dLeft-click §8» §fOpen Practice");

        button(inventory, 16, Material.BOOK, "§e§lKits",
                "§7View kits and edit your layouts.",
                "",
                "§dLeft-click §8» §fOpen Kits");

        // Utility row
        button(inventory, 19, Material.EMERALD, "§a§lStatistics",
                "§7Wins, losses, streak and ELO.",
                "",
                "§dLeft-click §8» §fView Stats");

        button(inventory, 20, Material.PAPER, "§f§lMatch History",
                "§7Review your recent matches.",
                "",
                "§dLeft-click §8» §fView History");

        button(inventory, 21, Material.COMPARATOR, "§b§lSettings",
                "§7Personal gameplay preferences.",
                "",
                "§dLeft-click §8» §fOpen Settings");

        button(inventory, 22, Material.GOLD_INGOT, "§6§lCoin Shop",
                "§7Spend your earned practice coins.",
                "",
                "§dLeft-click §8» §fOpen Shop");

        button(inventory, 23, Material.PAPER, "§c§lReports",
                "§7Report a player or review reports.",
                "",
                "§dLeft-click §8» §fOpen Reports");

        button(inventory, 25, Material.NETHER_STAR, "§5§lVoidFlameMC",
                "§7Practice Network",
                "§fplay.VoidFlame.net");

        if (player.hasPermission("voidflame.arena.manage")) {
            button(inventory, 24, Material.IRON_BARS, "§b§lArena Admin",
                    "§7Manage practice arenas.",
                    "",
                    "§dLeft-click §8» §fOpen Arena Admin");
        }

        if (player.hasPermission("voidflame.ranks.admin")) {
            button(inventory, 18, Material.NAME_TAG, "§5§lRank Admin",
                    "§7Manage ranks and assignments.",
                    "",
                    "§dLeft-click §8» §fOpen Rank Admin");
        }

        button(inventory, 26, Material.BARRIER, "§c§lClose", "§7Close this menu.");
    }

    private void queue(Player player, Inventory inventory) {
        inventory.setItem(4, head(player, "§d§lPlay Practice",
                "§7Choose how you want to play.",
                "",
                "§fFast queues • Clean matchmaking"));

        button(inventory, 10, Material.PAPER, "§d§lUnranked",
                "§7Casual matchmaking.",
                "§7No rating changes.",
                "",
                "§dClick §8» §fChoose Kit");

        button(inventory, 12, Material.NETHER_STAR, "§5§lRanked",
                "§7Competitive matchmaking.",
                "§7ELO is used for matchmaking.",
                "",
                "§dClick §8» §fChoose Kit");

        button(inventory, 14, Material.ENDER_EYE, "§a§lPractice",
                "§7Open practice modes.",
                "",
                "§dClick §8» §fOpen Practice");

        button(inventory, 16, Material.PLAYER_HEAD, "§b§lParty",
                "§7Play with your friends.",
                "",
                "§dClick §8» §fOpen Party");

        button(inventory, 22, Material.BARRIER, "§c§lClose", "§7Close this menu.");
        button(inventory, 18, Material.ARROW, "§7§lBack", "§7Return to the main menu.");
    }

    private void kitQueue(Player player, Inventory inventory, boolean ranked) {
        inventory.setItem(4, head(player, ranked ? "§5§lRanked Queue" : "§d§lUnranked Queue",
                ranked ? "§7Choose a kit for competitive matchmaking." : "§7Choose a kit for casual matchmaking.",
                "",
                ranked ? "§5ELO matchmaking enabled" : "§dNo ELO changes"));

        button(inventory, 10, Material.IRON_SWORD, "§f§lSword", "§7Classic sword practice.", "", "§dClick §8» §fQueue");
        button(inventory, 11, Material.IRON_AXE, "§f§lAxe", "§7Axe practice.", "", "§dClick §8» §fQueue");
        button(inventory, 12, Material.GOLDEN_APPLE, "§e§lUHC", "§7UHC practice.", "", "§dClick §8» §fQueue");
        button(inventory, 14, Material.MACE, "§b§lMace", "§7Mace practice.", "", "§dClick §8» §fQueue");
        button(inventory, 15, Material.TRIDENT, "§3§lSpear & Mace", "§7Spear and mace practice.", "", "§dClick §8» §fQueue");
        button(inventory, 16, Material.END_CRYSTAL, "§c§lCrystal", "§7Crystal practice.", "", "§dClick §8» §fQueue");
        button(inventory, 19, Material.NETHERITE_SWORD, "§8§lNetherite Pot", "§7Netherite potion practice.", "", "§dClick §8» §fQueue");
        button(inventory, 20, Material.SHIELD, "§a§lSMP", "§7SMP-style practice.", "", "§dClick §8» §fQueue");

        button(inventory, 18, Material.ARROW, "§7§lBack", "§7Return to the queue menu.");
        button(inventory, 26, Material.BARRIER, "§c§lClose", "§7Close this menu.");
    }

    private void stats(Player player, Inventory inventory) {
        inventory.setItem(4, head(player, "§d§lYour Statistics",
                "§7VoidFlameMC Practice",
                "",
                "§fTrack your competitive progress."));

        button(inventory, 10, Material.EMERALD, "§a§lMy Stats",
                "§7Wins, losses, streak and ELO.",
                "",
                "§dClick §8» §fOpen Stats");

        button(inventory, 12, Material.PAPER, "§f§lMatch History",
                "§7Recent completed matches.",
                "",
                "§dClick §8» §fOpen History");

        button(inventory, 14, Material.GOLD_INGOT, "§6§lLeaderboard",
                "§7Compare public rankings.",
                "",
                "§dClick §8» §fOpen Leaderboard");

        button(inventory, 16, Material.BOOK, "§b§lProfile",
                "§7Your public practice profile.",
                "",
                "§dClick §8» §fOpen Profile");

        navigation(inventory);
    }

    private void settings(Player player, Inventory inventory) {
        inventory.setItem(4, item(Material.COMPARATOR, "§b§lSettings",
                "§7Customize your practice experience.",
                "",
                "§8Click any option to toggle it."));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < SETTINGS_LIST.size(); i++) {
            Setting setting = SETTINGS_LIST.get(i);
            boolean enabled = getSetting(player, setting);
            String state = enabled ? "§a" + setting.on() : "§c" + setting.off();

            button(inventory, slots[i], setting.material(),
                    (enabled ? "§a" : "§c") + setting.label(),
                    "§7Status: " + state,
                    "",
                    "§dClick §8» §fToggle");
        }

        navigation(inventory);
    }

    private void navigation(Inventory inventory) {
        button(inventory, 18, Material.ARROW, "§7§lBack", "§7Return to the previous menu.");
        button(inventory, 26, Material.BARRIER, "§c§lClose", "§7Close this menu.");
    }

    private void decorate(Inventory inventory) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }

        ItemStack accent = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int slot : new int[]{1, 2, 3, 5, 6, 7, 9, 17, 18, 24, 25}) {
            inventory.setItem(slot, accent.clone());
        }
    }

    private void button(Inventory inventory, int slot, Material material, String name, String... lore) {
        ItemStack stack = item(material, name, lore);
        inventory.setItem(slot, stack);
    }

    private ItemStack head(Player player, String name, String... lore) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        if (stack.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean getSetting(Player player, Setting setting) {
        Map<String, Boolean> values = settingsCache.get(player.getUniqueId());
        return values == null ? setting.defaultValue() : values.getOrDefault(setting.key(), setting.defaultValue());
    }

    private void loadSettings(Player player) {
        UUID id = player.getUniqueId();
        Map<String, Boolean> values = new ConcurrentHashMap<>();
        settingsCache.put(id, values);

        for (Setting setting : SETTINGS_LIST) {
            playerSettings.get(id, setting.key(), setting.defaultValue())
                    .thenAccept(value -> values.put(setting.key(), value));
        }
    }

    private void toggle(Player player, Setting setting) {
        if (settingBusy.putIfAbsent(player.getUniqueId(), true) != null) {
            return;
        }

        boolean value = !getSetting(player, setting);
        playerSettings.set(player.getUniqueId(), setting.key(), value)
                .whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(this, () -> {
                    settingBusy.remove(player.getUniqueId());

                    if (error != null) {
                        player.sendMessage(ChatColor.RED + "Could not save setting.");
                        play(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.7f);
                        return;
                    }

                    settingsCache
                            .computeIfAbsent(player.getUniqueId(), ignoredId -> new ConcurrentHashMap<>())
                            .put(setting.key(), value);

                    play(player, Sound.UI_BUTTON_CLICK, 0.6f, value ? 1.2f : 0.8f);
                    open(player, SETTINGS);
                }));
    }

    private void command(Player player, String command) {
        player.closeInventory();
        play(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        Bukkit.dispatchCommand(player, command);
    }

    private void play(Player player, Sound sound, float volume, float pitch) {
        if (getConfig().getBoolean("settings.sound-enabled", true)) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadSettings(event.getPlayer());
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(MAIN) && !title.equals(QUEUE) && !title.equals(STATS) && !title.equals(SETTINGS)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= 27) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, getConfig().getLong("settings.click-cooldown-ms", 250L));
        Long previous = lastClicks.put(player.getUniqueId(), now);
        if (previous != null && now - previous < cooldown) {
            return;
        }

        play(player, Sound.UI_BUTTON_CLICK, 0.45f, 1.05f);

        if (title.equals(MAIN)) {
            switch (event.getRawSlot()) {
                case 10, 11 -> open(player, QUEUE);
                case 13 -> command(player, "party info");
                case 15 -> command(player, "practice");
                case 16 -> command(player, "kits");
                case 19 -> command(player, "stats");
                case 20 -> command(player, "history");
                case 21 -> open(player, SETTINGS);
                case 22 -> command(player, "coinshop");
                case 23 -> command(player, player.hasPermission("voidflame.report.view") ? "reports" : "report");
                case 24 -> {
                    if (player.hasPermission("voidflame.arena.manage")) {
                        command(player, "arena list");
                    }
                }
                case 18 -> {
                    if (player.hasPermission("voidflame.ranks.admin")) {
                        command(player, "ranks");
                    }
                }
                case 25 -> {
                    player.sendMessage("§d§lVoidFlameMC §8• §fPractice");
                    player.sendMessage("§7Server: §fplay.VoidFlame.net");
                }
                case 4 -> command(player, "stats");
                case 26 -> player.closeInventory();
                default -> {
                }
            }
            return;
        }

        if (title.equals(QUEUE)) {
            switch (event.getRawSlot()) {
                case 10 -> open(player, UNRANKED);
                case 12 -> open(player, RANKED);
                case 14 -> command(player, "practice");
                case 16 -> command(player, "party info");
                case 18 -> open(player, MAIN);
                case 22, 26 -> player.closeInventory();
                default -> {
                }
            }
            return;
        }

        if (title.equals(UNRANKED) || title.equals(RANKED)) {
            boolean ranked = title.equals(RANKED);
            String prefix = ranked ? "queue ranked " : "queue ";
            switch (event.getRawSlot()) {
                case 10 -> command(player, prefix + "sword");
                case 11 -> command(player, prefix + "axe");
                case 12 -> command(player, prefix + "uhc");
                case 14 -> command(player, prefix + "mace");
                case 15 -> command(player, prefix + "spear_mace");
                case 16 -> command(player, prefix + "crystal");
                case 19 -> command(player, prefix + "netherite_pot");
                case 20 -> command(player, prefix + "smp");
                case 18 -> open(player, QUEUE);
                case 26 -> player.closeInventory();
                default -> {}
            }
            return;
        }

        if (title.equals(STATS)) {
            switch (event.getRawSlot()) {
                case 10, 16 -> command(player, "stats");
                case 12 -> command(player, "history");
                case 14 -> command(player, "leaderboard");
                case 18 -> open(player, MAIN);
                case 26 -> player.closeInventory();
                default -> {
                }
            }
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < slots.length; i++) {
            if (event.getRawSlot() == slots[i]) {
                toggle(player, SETTINGS_LIST.get(i));
                return;
            }
        }

        if (event.getRawSlot() == 18) {
            open(player, MAIN);
        } else if (event.getRawSlot() == 26) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        settingsCache.remove(id);
        settingBusy.remove(id);
        lastClicks.remove(id);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        open(player);
        return true;
    }
}
