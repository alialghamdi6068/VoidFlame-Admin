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
    public static final String TITLE = "§8VoidFlame §7• §5Administration";
    private final VoidFlameMenusPlugin plugin;

    public AdminMenu(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        if (!player.hasPermission("voidflame.admin")) return;
        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE);
        fill(inv);
        button(inv, 10, Material.CHEST, "§d§lKITS", "§7Kit creation, layouts, defaults", "§eClick §8» §fOpen Kit Administration");
        button(inv, 11, Material.NETHERITE_SWORD, "§d§lARENAS", "§7Arena lifecycle and kit restrictions", "§eClick §8» §fOpen Arena Administration");
        button(inv, 12, Material.DIAMOND_SWORD, "§d§lDUELS", "§7Duels, reports and match controls", "§eClick §8» §fOpen Duels");
        button(inv, 13, Material.NETHER_STAR, "§b§lQUEUES", "§7Queue and matchmaking controls", "§eClick §8» §fOpen Duel Queue");
        button(inv, 14, Material.ENDER_PEARL, "§a§lFFA", "§7Free For All controls", "§eClick §8» §fOpen FFA");
        button(inv, 15, Material.NAME_TAG, "§6§lRANKS", "§7Ranks, colors and player assignments", "§eClick §8» §fOpen Rank Administration");
        button(inv, 16, Material.COMPASS, "§5§lMENUS", "§7Unified menu entry point", "§eClick §8» §fMenu controls");
        button(inv, 19, Material.GOLD_INGOT, "§e§lREWARDS", "§7Coins and reward configuration", "§eClick §8» §fCoin Shop");
        button(inv, 20, Material.PLAYER_HEAD, "§f§lPLAYERS", "§7Player and rank management", "§eClick §8» §fRank/player tools");
        button(inv, 21, Material.SHIELD, "§c§lSECURITY", "§7AntiBot, rate limits and security", "§eClick §8» §fSecurity status");
        button(inv, 22, Material.COMPARATOR, "§b§lSETTINGS", "§7Practice settings and configuration", "§eClick §8» §fPractice Settings");
        button(inv, 23, Material.REDSTONE, "§a§lWORLDS", "§7World enable/load/spawn controls", "§eClick §8» §fWorld Manager");
        button(inv, 24, Material.ENDER_CHEST, "§9§lDATABASE", "§7Core backup and recovery tools", "§eClick §8» §fDatabase status");
        button(inv, 25, Material.REPEATER, "§7§lRELOAD", "§7Reload supported modules", "§eClick §8» §fReload");
        button(inv, 28, Material.NETHER_STAR, "§5§lSTAFF", "§7Staff controls and moderation tools", "§eClick §8» §fOpen Staff Control");
        button(inv, 49, Material.BARRIER, "§c§lCLOSE", "§7Close administration");
        player.openInventory(inv);
    }

    private void action(Player p, int slot) {
        String command = switch (slot) {
            case 10 -> "kit admin";
            case 11 -> "arena";
            case 12 -> "duels";
            case 13 -> "duels";
            case 14 -> "ffa";
            case 15 -> "ranks";
            case 16 -> "duels";
            case 19 -> "coinshop";
            case 20 -> "ranks";
            case 21 -> "antibot status";
            case 22 -> "settings";
            case 23 -> "vfworld list";
            case 24 -> "vfrestore";
            case 25 -> "vfadmin reload";
            default -> null;
        };
        if (slot == 28) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> new StaffGui(plugin).openMain(p));
            return;
        }
        if (command == null) return;
        p.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> p.performCommand(command));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot == 49) { p.closeInventory(); return; }
        action(p, slot);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    private void fill(Inventory inv) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack accent = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, pane.clone());
        for (int i : new int[]{1,2,3,4,5,6,7,10,11,12,13,14,15,16,19,20,21,22,23,24,25,46,47,48,50,51,52}) inv.setItem(i, accent.clone());
    }

    private void button(Inventory inv, int slot, Material material, String name, String... lore) {
        inv.setItem(slot, item(material, name, lore));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
