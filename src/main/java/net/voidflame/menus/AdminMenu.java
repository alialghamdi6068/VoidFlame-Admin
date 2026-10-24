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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminMenu implements Listener {
    public static final String TITLE = "§8Staff";
    private final VoidFlameMenusPlugin plugin;
    private final Map<UUID, Long> pendingRestoreConfirmations = new ConcurrentHashMap<>();

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
        if (AdminAccess.hasStaffAccess(p)) actions.add(adminButton("Staff Tools", "Open the staff tools screen.", "/staff", NamedTextColor.AQUA));
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

    private void showAdminDialog(Player p, String title, String description, List<ActionButton> actions) {
        showAdminDialog(p, title, description, actions, List.of());
    }

    private void showAdminDialog(Player p, String title, String description, List<ActionButton> actions, List<DialogBody> extraBody) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        List<DialogBody> body = new java.util.ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("VOIDFLAME ADMINISTRATION", NamedTextColor.AQUA), 300));
        body.add(DialogBody.plainMessage(Component.text(description, NamedTextColor.GRAY), 300));
        body.addAll(extraBody);
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff • " + title, NamedTextColor.WHITE))
                .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                .canCloseWithEscape(true)
                .body(body)
                .build())
            .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    public void openSection(Player p, String section) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        switch (section.toLowerCase(java.util.Locale.ROOT)) {
            case "main" -> open(p);
            case "practice" -> openPractice(p);
            case "management" -> openManagement(p);
            case "server" -> openServer(p);
            case "security" -> plugin.advancedAdminGui().openSecurity(p);
            case "backups" -> plugin.advancedAdminGui().openBackups(p);
            case "data" -> { pendingRestoreConfirmations.remove(p.getUniqueId()); openData(p); }
            case "backup" -> AdminServices.backup().whenComplete((ok, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (error == null) { plugin.audit(p.getName(), "CORE_BACKUP"); p.sendMessage(color("&aCore database backup created.")); }
                else p.sendMessage(color("&cBackup failed: &f" + error.getMessage()));
                openData(p);
            }));
            case "restoreconfirm" -> openRestoreConfirmation(p);
            case "restoreyes" -> restoreLatest(p);
            case "securitytoggle" -> {
                if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
                if (AdminServices.security() == null) { p.sendMessage(color("&cVoidFlame-Security is not installed.")); return; }
                boolean next = !AdminServices.securityBoolean("isProtectionEnabled", false);
                AdminServices.setSecurity("setProtectionEnabled", next);
                plugin.audit(p.getName(), "SECURITY_TOGGLE:" + next);
                openSecurity(p);
            }
            case "securitylockdown" -> {
                if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
                if (AdminServices.security() == null) { p.sendMessage(color("&cVoidFlame-Security is not installed.")); return; }
                boolean next = !AdminServices.securityBoolean("isLockdown", false);
                AdminServices.setSecurity("setLockdown", next);
                plugin.audit(p.getName(), "SECURITY_LOCKDOWN:" + next);
                openSecurity(p);
            }
            case "analytics" -> plugin.advancedAdminGui().openAnalytics(p);
            case "staff" -> plugin.staffGui().openMain(p);
            case "duels" -> route(p, "voidflame.admin.duels", "integrations.duels");
            case "kits" -> route(p, "voidflame.admin.kits", "integrations.kits");
            case "arenas" -> route(p, "voidflame.admin.arenas", "integrations.arenas");
            case "queues" -> route(p, "voidflame.admin.queues", "integrations.queues");
            case "ffa" -> route(p, "voidflame.admin.ffa", "integrations.ffa");
            case "rewards" -> route(p, "voidflame.admin.rewards", "integrations.rewards");
            case "players" -> plugin.advancedAdminGui().openPlayerManager(p);
            case "menus" -> route(p, "voidflame.admin.menus", "integrations.menus");
            case "settings" -> route(p, "voidflame.admin.settings", "integrations.settings");
            case "punishments" -> plugin.advancedAdminGui().openPunishments(p);
            case "worlds" -> plugin.staffGui().openWorldsFromAdmin(p);
            case "timeweather" -> plugin.staffGui().openTimeFromAdmin(p);
            case "servermanager" -> plugin.staffGui().openServerFromAdmin(p);
            case "flags" -> plugin.staffGui().openFlagsFromAdmin(p);
            default -> open(p);
        }
    }

    private void openPractice(Player p) {
        showAdminDialog(p, "Practice", "Duels, kits, arenas, queues, FFA and rewards.",
            List.of(
                adminButton("Duels", "Open duel administration.", "/vfadmin duels", NamedTextColor.AQUA),
                adminButton("Kits", "Open kit administration.", "/vfadmin kits", NamedTextColor.BLUE),
                adminButton("Arenas", "Open arena administration.", "/vfadmin arenas", NamedTextColor.GOLD),
                adminButton("Queues", "Open queue and matchmaking controls.", "/vfadmin queues", NamedTextColor.GREEN),
                adminButton("FFA", "Open FFA controls.", "/vfadmin ffa", NamedTextColor.RED),
                adminButton("Rewards", "Open coin and reward controls.", "/vfadmin rewards", NamedTextColor.GOLD),
                adminButton("Back", "Return to Staff administration.", "/vfadmin", NamedTextColor.GRAY)
            ));
    }

    private void openManagement(Player p) {
        showAdminDialog(p, "Management", "Players, menus, settings and punishments.",
            List.of(
                adminButton("Players", "Open the staff player manager.", "/vfadmin players", NamedTextColor.AQUA),
                adminButton("Menus", "Open menu and practice configuration.", "/vfadmin menus", NamedTextColor.BLUE),
                adminButton("Settings", "Open configured server/practice settings.", "/vfadmin settings", NamedTextColor.GREEN),
                adminButton("Punishments", "Open bans, mutes, warnings and history.", "/vfadmin punishments", NamedTextColor.RED),
                adminButton("Back", "Return to Staff administration.", "/vfadmin", NamedTextColor.GRAY)
            ));
    }

    private void openServer(Player p) {
        showAdminDialog(p, "Server", "Worlds, time, weather and server controls.",
            List.of(
                adminButton("Worlds", "Manage worlds through VoidFlame-Core.", "/vfadmin worlds", NamedTextColor.GREEN),
                adminButton("Time & Weather", "Set and lock server time and weather.", "/vfadmin timeweather", NamedTextColor.GOLD),
                adminButton("Server Manager", "Open whitelist and server controls.", "/vfadmin servermanager", NamedTextColor.RED),
                adminButton("Flags", "Configure PvP, block placement and mob spawning.", "/vfadmin flags", NamedTextColor.BLUE),
                adminButton("Back", "Return to Staff administration.", "/vfadmin", NamedTextColor.GRAY)
            ));
    }

    private void openSecurity(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        boolean enabled = AdminServices.securityBoolean("isProtectionEnabled", false);
        boolean lockdown = AdminServices.securityBoolean("isLockdown", false);
        List<DialogBody> body = List.of(
            DialogBody.plainMessage(Component.text("SECURITY STATUS", NamedTextColor.RED), 300),
            DialogBody.plainMessage(Component.text("AntiBot: " + (enabled ? "ON" : "OFF")
                + " • Lockdown: " + (lockdown ? "ON" : "OFF"), enabled ? NamedTextColor.GREEN : NamedTextColor.GOLD), 300),
            DialogBody.plainMessage(Component.text("Protection level: " + AdminServices.securityInt("getProtectionLevel", 0)
                + " • Verifying: " + AdminServices.securityInt("getVerifyingCount", 0)
                + " • Whitelist: " + AdminServices.securityInt("getWhitelistSize", 0), NamedTextColor.GRAY), 300)
        );
        showAdminDialog(p, "Security", "AntiBot protection and emergency lockdown.",
            List.of(
                adminButton(enabled ? "Disable AntiBot" : "Enable AntiBot", "Toggle AntiBot protection.", "/vfadmin securitytoggle", enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                adminButton(lockdown ? "Disable Lockdown" : "Enable Lockdown", "Toggle emergency admission lockdown.", "/vfadmin securitylockdown", lockdown ? NamedTextColor.GREEN : NamedTextColor.RED),
                adminButton("Security Whitelist", "Open the existing Security whitelist controls.", "/antibot gui", NamedTextColor.AQUA),
                adminButton("Refresh", "Refresh security status.", "/vfadmin security", NamedTextColor.BLUE),
                adminButton("Back", "Return to Staff administration.", "/vfadmin", NamedTextColor.GRAY)
            ), body);
    }

    private void openData(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        Object db = AdminServices.coreDatabase();
        boolean online = db != null;
        String path = "";
        if (online) try { path = String.valueOf(db.getClass().getMethod("databasePath").invoke(db)); } catch (Exception ignored) {}
        showAdminDialog(p, "Data & Backups", "Core database status and safe backup/recovery.",
            List.of(
                adminButton(online ? "Core Database: Online" : "Core Database: Offline",
                    "SQLite central database • " + (path.isBlank() ? "Path unavailable" : path), "/vfadmin data", online ? NamedTextColor.GREEN : NamedTextColor.RED),
                adminButton("Create Backup", "Create a validated Core database backup.", "/vfadmin backup", NamedTextColor.AQUA),
                adminButton("Select Backup", "Choose a specific backup and confirm the exact file to restore.", "/vfadmin backups", NamedTextColor.BLUE),
                adminButton("Restore Latest", "Open confirmation before restoring the latest backup.", "/vfadmin restoreconfirm", NamedTextColor.RED),
                adminButton("Back", "Return to Staff administration.", "/vfadmin", NamedTextColor.GRAY)
            ));
    }

    private void openRestoreConfirmation(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        pendingRestoreConfirmations.put(p.getUniqueId(), System.currentTimeMillis());
        long expiryTicks = Math.max(1L, plugin.getConfig().getLong("security.confirmation-seconds", 15L)) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Long created = pendingRestoreConfirmations.get(p.getUniqueId());
            if (created != null && System.currentTimeMillis() - created >= expiryTicks * 50L) {
                pendingRestoreConfirmations.remove(p.getUniqueId(), created);
            }
        }, expiryTicks);
        showAdminDialog(p, "Confirm Restore", "Restoring replaces the current database state. A safety backup is created first.",
            List.of(
                adminButton("Confirm Restore", "Restore the latest validated Core backup.", "/vfadmin restoreyes", NamedTextColor.RED),
                adminButton("Cancel", "Return without restoring.", "/vfadmin data", NamedTextColor.GREEN)
            ));
    }

    private void restoreLatest(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        Long created = pendingRestoreConfirmations.remove(p.getUniqueId());
        long now = System.currentTimeMillis();
        long expiryMillis = Math.max(1L, plugin.getConfig().getLong("security.confirmation-seconds", 15L)) * 1000L;
        if (created == null || now < created || now - created > expiryMillis) {
            p.sendMessage(color("&cRestore confirmation expired or missing. Open the confirmation screen and try again."));
            openData(p);
            return;
        }
        AdminServices.restoreLatest().whenComplete((ok, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (error == null) {
                plugin.audit(p.getName(), "CORE_RESTORE_LATEST");
                p.sendMessage(color("&aCore database restore completed. Restart/reload the server if required."));
            } else {
                p.sendMessage(color("&cRestore failed: &f" + error.getMessage()));
            }
            openData(p);
        }));
    }

    private void route(Player p,String permission,String path) {
        if(AdminAccess.isBedrock(p) || !(p.hasPermission(permission) || p.hasPermission("voidflame.admin.full"))){deny(p);return;}
        String command=plugin.getConfig().getString(path+".command","");
        if(command==null||command.isBlank()){p.sendMessage(color("&cThis integration is not configured."));return;}
        p.closeInventory(); plugin.audit(p.getName(),"OPEN_INTEGRATION:"+path);
        Bukkit.getScheduler().runTask(plugin,()->p.performCommand(command));
    }

    private void confirmDataRestore(Player p) {
        if (!AdminAccess.hasAdminAccess(p)) { deny(p); return; }
        showAdminDialog(p, "Confirm Database Restore",
                "Restore the latest Core database backup? This operation can replace current data. Continue only if you understand the impact.",
                List.of(
                        adminButton("Confirm Restore", "Restore the latest backup.", "/vfadmin restoreyes", NamedTextColor.RED),
                        adminButton("Cancel", "Return without changing data.", "/vfadmin data", NamedTextColor.GREEN)));
    }

    private void deny(Player p){p.sendMessage(color(plugin.getConfig().getString("messages.no-permission","&cNo permission.")));}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
}
