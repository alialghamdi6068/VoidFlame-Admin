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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancedAdminGui implements Listener {
    private static final String ROOT = "§8VoidFlame §5• §d";
    private final VoidFlameMenusPlugin plugin;
    private final Map<UUID, PendingInput> input = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRestore> pendingRestores = new ConcurrentHashMap<>();
    private final Map<UUID, StaffSpectatorSnapshot> staffSpectators = new ConcurrentHashMap<>();

    public AdvancedAdminGui(VoidFlameMenusPlugin plugin) { this.plugin = plugin; }

    public void openPunishments(Player p) {
        showDialog(p, "Punishments", "Select a moderation workflow.", List.of(
                action("Ban", "Select a player and duration.", "/vfadmin advclick punishments 10", NamedTextColor.RED),
                action("Mute", "Select a player and duration.", "/vfadmin advclick punishments 12", NamedTextColor.GOLD),
                action("Warn", "Select a player and reason.", "/vfadmin advclick punishments 14", NamedTextColor.YELLOW),
                action("Kick", "Select a player and reason.", "/vfadmin advclick punishments 16", NamedTextColor.WHITE),
                action("History", "View punishment history.", "/vfadmin advclick punishments 28", NamedTextColor.AQUA),
                action("Unpunish", "Clear an active punishment.", "/vfadmin advclick punishments 30", NamedTextColor.GREEN),
                action("Back", "Return to Staff.", "/vfadmin", NamedTextColor.GRAY)));
    }

    private void openPlayerSelect(Player staff, String action) {
        if (!AdminAccess.hasAdminAccess(staff)) return;
        List<ActionButton> buttons = new ArrayList<>();
        List<Player> players = sortedPlayers();
        for (int i = 0; i < Math.min(players.size(), 78); i++) {
            Player target = players.get(i);
            buttons.add(action(target.getName(), "Select " + target.getName() + ".", "/vfadmin advclick punish-select:" + action + " " + target.getUniqueId(), NamedTextColor.AQUA));
        }
        buttons.add(action("Back", "Return to punishments.", "/vfadmin punishments", NamedTextColor.GRAY));
        showDialog(staff, action.toUpperCase(Locale.ROOT) + " • Player", "Choose an online player.", buttons);
    }

    private void openDuration(Player staff, String action, Player target) {
        String type = "duration:" + action + ":" + target.getUniqueId();
        showDialog(staff, action.toUpperCase(Locale.ROOT) + " • " + target.getName(),
                "Choose a duration. You will be prompted for a reason in chat.",
                List.of(action("30 Seconds", "Temporary punishment.", "/vfadmin advclick " + type + " 10", NamedTextColor.AQUA),
                        action("10 Minutes", "Temporary punishment.", "/vfadmin advclick " + type + " 12", NamedTextColor.AQUA),
                        action("1 Hour", "Temporary punishment.", "/vfadmin advclick " + type + " 14", NamedTextColor.AQUA),
                        action("1 Day", "Temporary punishment.", "/vfadmin advclick " + type + " 16", NamedTextColor.AQUA),
                        action("Permanent", "No expiration.", "/vfadmin advclick " + type + " 28", NamedTextColor.RED),
                        action("Back", "Return to player selection.", "/vfadmin advclick punishments 10", NamedTextColor.GRAY)));
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
        List<ActionButton> buttons = new ArrayList<>();
        List<Player> players = sortedPlayers();
        for (int i = 0; i < Math.min(players.size(), 78); i++) {
            Player target = players.get(i);
            buttons.add(action(target.getName(), "View recent punishment history.", "/vfadmin advclick history-select " + target.getUniqueId(), NamedTextColor.AQUA));
        }
        buttons.add(action("Back", "Return to punishments.", "/vfadmin punishments", NamedTextColor.GRAY));
        showDialog(staff, "Punishment History", "Select an online player.", buttons);
    }

    public void openPlayerManager(Player p) {
        List<ActionButton> buttons = new ArrayList<>();
        List<Player> players = sortedPlayers();
        for (int i = 0; i < Math.min(players.size(), 78); i++) {
            Player target = players.get(i);
            buttons.add(action(target.getName(), "Open player management.", "/vfadmin advclick player-manager " + target.getUniqueId(), NamedTextColor.AQUA));
        }
        buttons.add(action("Back", "Return to Staff.", "/vfadmin", NamedTextColor.GRAY));
        showDialog(p, "Player Manager", "Select an online player.", buttons);
    }

    private void openPlayerActions(Player staff, Player target) {
        String type = "player-actions:" + target.getUniqueId();
        showDialog(staff, "Player • " + target.getName(),
                "UUID: " + target.getUniqueId() + " • World: " + target.getWorld().getName()
                        + " • Health: " + String.format(Locale.ROOT, "%.1f", target.getHealth()) + " • Ping: " + target.getPing(),
                List.of(action("Core Info", "Show player details.", "/vfadmin advclick " + type + " 10", NamedTextColor.AQUA),
                        action("View Inventory", "Inspect the live inventory contents.", "/vfadmin advclick " + type + " 12", NamedTextColor.GOLD),
                        action("View Ender Chest", "Inspect the ender chest contents.", "/vfadmin advclick " + type + " 14", NamedTextColor.LIGHT_PURPLE),
                        action("Teleport To", "Teleport yourself to the player.", "/vfadmin advclick " + type + " 16", NamedTextColor.GREEN),
                        action("Teleport Here", "Teleport the player to you.", "/vfadmin advclick " + type + " 28", NamedTextColor.YELLOW),
                        action(staffSpectators.containsKey(staff.getUniqueId()) ? "Stop Spectating" : "Spectate",
                                staffSpectators.containsKey(staff.getUniqueId()) ? "Restore your previous game mode and location."
                                        : "Follow target in spectator mode.",
                                staffSpectators.containsKey(staff.getUniqueId())
                                        ? "/vfadmin advclick staff-spectate 10" : "/vfadmin advclick " + type + " 30",
                                NamedTextColor.BLUE),
                        action("Heal", "Restore health and hunger.", "/vfadmin advclick " + type + " 32", NamedTextColor.GREEN),
                        action("Clear Inventory", "Clear the player's inventory.", "/vfadmin advclick " + type + " 34", NamedTextColor.RED),
                        action("Back", "Return to player list.", "/vfadmin advclick player-manager 49", NamedTextColor.GRAY)));
    }

    public void openSecurity(Player p) {
        int rate = AdminServices.securityInt("getMaxActionsPerSecond", 8);
        int join = AdminServices.securityInt("getJoinThreshold", 4);
        int verify = AdminServices.securityInt("getVerifyScore", 5);
        int block = AdminServices.securityInt("getBlockScore", 20);
        boolean on = AdminServices.securityBoolean("isProtectionEnabled", true);
        boolean lock = AdminServices.securityBoolean("isLockdown", false);
        showDialog(p, "Security Control", "AntiBot: " + (on ? "ON" : "OFF") + " • Lockdown: " + (lock ? "ON" : "OFF")
                        + " • Rate: " + rate + "/s • Join threshold: " + join + " • Verify: " + verify + " • Block: " + block,
                List.of(action(on ? "Disable Protection" : "Enable Protection", "Toggle AntiBot protection.", "/vfadmin advclick security-control 10", on ? NamedTextColor.RED : NamedTextColor.GREEN),
                        action(lock ? "Disable Lockdown" : "Enable Lockdown", "Toggle emergency lockdown.", "/vfadmin advclick security-control 12", NamedTextColor.GOLD),
                        action("Cycle Actions / Sec", "Cycle 4 / 8 / 12 / 20.", "/vfadmin advclick security-control 14", NamedTextColor.AQUA),
                        action("Cycle Join Threshold", "Cycle 2 / 4 / 6 / 10.", "/vfadmin advclick security-control 16", NamedTextColor.BLUE),
                        action("Cycle Verify Score", "Cycle 3 / 5 / 8 / 12.", "/vfadmin advclick security-control 28", NamedTextColor.YELLOW),
                        action("Cycle Block Score", "Cycle 10 / 20 / 30 / 50.", "/vfadmin advclick security-control 30", NamedTextColor.RED),
                        action("Whitelist", "Open Security whitelist.", "/antibot gui", NamedTextColor.AQUA),
                        action("Back", "Return to Staff.", "/vfadmin", NamedTextColor.GRAY)));
    }

    public void openBackups(Player p) {
        List<ActionButton> buttons = new ArrayList<>();
        List<Path> backups = AdminServices.backups();
        for (int i = 0; i < Math.min(backups.size(), 70); i++) {
            Path backup = backups.get(i);
            buttons.add(action(backup.getFileName().toString(), "Review restore options for this backup.", "/vfadmin advclick backups " + (i + 100), NamedTextColor.GOLD));
        }
        buttons.add(action("Create Backup", "Create a validated database backup.", "/vfadmin advclick backups 47", NamedTextColor.GREEN));
        buttons.add(action("Back", "Return to Staff.", "/vfadmin", NamedTextColor.GRAY));
        showDialog(p, "Database Backups", backups.isEmpty() ? "No backups found. Create one first." : "Choose a backup.", buttons);
    }

    private void confirmRestore(Player p, Path backup) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cNo permission.")); return; }
        List<Path> backups = AdminServices.backups();
        int index = backups.indexOf(backup);
        if (index < 0) { p.sendMessage(color("&cBackup is no longer available.")); openBackups(p); return; }
        pendingRestores.put(p.getUniqueId(), new PendingRestore(backup.toAbsolutePath().normalize(), System.currentTimeMillis()));
        showDialog(p, "Confirm Restore", "Restore " + backup.getFileName()
                        + "? A safety backup is created first. This confirmation expires in 15 seconds.",
                List.of(action("Confirm Restore", "Proceed with restore.", "/vfadmin advclick restore:" + index + " 20", NamedTextColor.RED),
                        action("Cancel", "Return without changes.", "/vfadmin advclick backups 49", NamedTextColor.GREEN)));
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
        }).whenComplete((data, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (error != null) {
                plugin.getLogger().warning("Could not load server analytics for " + p.getUniqueId() + ": " + error.getMessage());
                p.sendMessage(color("&cAnalytics could not be loaded because a Core query failed. Please try again."));
                return;
            }
            renderAnalytics(p, data);
        }));
    }

    private void renderAnalytics(Player p, Analytics a) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cYou do not have permission.")); return; }
        List<DialogBody> body = List.of(
            DialogBody.plainMessage(Component.text("LIVE SERVER ANALYTICS", NamedTextColor.AQUA), 300),
            DialogBody.plainMessage(Component.text("Player profiles: " + (long)a.players + "  •  Recorded matches: " + (long)a.matches, NamedTextColor.WHITE), 320),
            DialogBody.plainMessage(Component.text("Total wins: " + (long)a.wins + "  •  Total losses: " + (long)a.losses, NamedTextColor.GREEN), 320),
            DialogBody.plainMessage(Component.text("Average ELO: " + String.format(Locale.ROOT, "%.1f", a.elo)
                + "  •  Global win rate: " + String.format(Locale.ROOT, "%.1f%%", a.matches <= 0 ? 0 : a.wins * 100.0 / Math.max(1, a.wins + a.losses)), NamedTextColor.GOLD), 320),
            DialogBody.plainMessage(Component.text("Top kit: " + a.topKit + "  •  Top player: " + a.topPlayer, NamedTextColor.BLUE), 320),
            DialogBody.plainMessage(Component.text("Active duels: " + countActive() + "  •  Queued players: " + countQueue()
                + "  •  FFA players: " + countFfa(), NamedTextColor.GRAY), 320)
        );
        List<ActionButton> actions = List.of(
            analyticsButton("Refresh", "Reload the latest statistics.", "/vfadmin analytics", NamedTextColor.AQUA),
            analyticsButton("Back to Staff", "Return to the main Staff dialog.", "/vfadmin", NamedTextColor.GRAY)
        );
        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text("Staff • Analytics", NamedTextColor.WHITE))
                .externalTitle(Component.text("Staff", NamedTextColor.AQUA))
                .canCloseWithEscape(true)
                .body(body)
                .build())
            .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    private ActionButton analyticsButton(String label, String tooltip, String command, NamedTextColor color) {
        return ActionButton.create(Component.text(label, color), Component.text(tooltip, NamedTextColor.GRAY),
            180, DialogAction.commandTemplate(command));
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

    private void showDialog(Player p, String title, String description, List<ActionButton> actions, List<DialogBody> extraBody) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cJava staff only.")); return; }
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("VOIDFLAME ADMINISTRATION", NamedTextColor.AQUA), 300));
        body.add(DialogBody.plainMessage(Component.text(description, NamedTextColor.GRAY), 320));
        body.addAll(extraBody);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Staff • " + title, NamedTextColor.WHITE))
                        .externalTitle(Component.text("Staff", NamedTextColor.AQUA)).canCloseWithEscape(true)
                        .body(body).build())
                .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    private void showDialog(Player p, String title, String description, List<ActionButton> actions) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cJava staff only.")); return; }
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Staff • " + title, NamedTextColor.WHITE))
                        .externalTitle(Component.text("Staff", NamedTextColor.AQUA)).canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(Component.text("VOIDFLAME ADMINISTRATION", NamedTextColor.AQUA), 300),
                                DialogBody.plainMessage(Component.text(description, NamedTextColor.GRAY), 320)))
                        .build())
                .type(DialogType.multiAction(actions).columns(2).build()));
        p.showDialog(dialog);
    }

    private ActionButton action(String label, String tooltip, String command, NamedTextColor color) {
        return ActionButton.create(Component.text(label, color), Component.text(tooltip, NamedTextColor.GRAY), 180,
                DialogAction.commandTemplate(command));
    }

    public void dialogClick(Player p, String type, String rawAction) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cJava staff only.")); return; }
        try {
            dialogClick(p, type, Integer.parseInt(rawAction));
            return;
        } catch (NumberFormatException ignored) { }
        try {
            UUID id = UUID.fromString(rawAction);
            Player target = Bukkit.getPlayer(id);
            if (target == null) { p.sendMessage(color("&cThat player is no longer online.")); openPunishments(p); return; }
            if (type.startsWith("punish-select:")) {
                String action = type.substring("punish-select:".length());
                if (action.equals("unpunish")) {
                    plugin.punishments().execute(p, new String[]{"unpunish", target.getName()});
                    openPunishments(p);
                } else if (action.equals("warn") || action.equals("kick")) requestReason(p, action, target, null);
                else openDuration(p, action, target);
            } else if (type.equals("history-select")) {
                plugin.punishments().execute(p, new String[]{"history", target.getName()});
            } else if (type.equals("player-manager")) {
                openPlayerActions(p, target);
            } else {
                p.sendMessage(color("&cInvalid Dialog action."));
            }
        } catch (IllegalArgumentException ex) {
            p.sendMessage(color("&cInvalid player identifier."));
        }
    }

    public void dialogClick(Player p, String t, int s) {
        if (!AdminAccess.hasAdminAccess(p)) { p.sendMessage(color("&cJava staff only.")); return; }
        if (s >= 100 && t.equals("backups")) s -= 100;
    if (t.equals("staff-spectate")) {
        if (s == 10) stopStaffSpectating(p);
        return;
    }
    if (s==49) {
        if (t.startsWith("punish-select:") || t.startsWith("duration:")) { openPunishments(p); return; }
        if (t.startsWith("player-actions:")) { openPlayerManager(p); return; }
        if (t.startsWith("inventory-back:")) { openInventoryFromToken(p, t); return; }
        if (t.equals("backups") || t.startsWith("restore:")) { openBackups(p); return; }
        plugin.adminMenu().open(p); return;
    }
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
        if(s==10) openCoreInfo(p, target);
        else if(s==12)showInventoryDialog(p,target,false);
        else if(s==14)showInventoryDialog(p,target,true);
        else if(s==16)p.teleportAsync(target.getLocation());
        else if(s==28) {
            if (isTargetInPracticeActivity(target)) p.sendMessage(color("&cCannot teleport a player while they are queued or in a match/FFA."));
            else target.teleportAsync(p.getLocation());
        }
        else if(s==30) toggleStaffSpectator(p, target);
        else if(s==32) {
            if (isTargetInPracticeActivity(target)) p.sendMessage(color("&cCannot modify combat health while the player is in a match/FFA."));
            else { target.setHealth(target.getMaxHealth()); target.setFoodLevel(20); target.setFireTicks(0); }
        }
        else if(s==34) {
            if (isTargetInPracticeActivity(target)) p.sendMessage(color("&cCannot clear inventory while the player is queued or in a match/FFA."));
            else target.getInventory().clear();
        }
        else if(s==49)openPlayerManager(p);
    } else if(t.startsWith("inventory-slot:")) {
        String[] parts = t.split(":");
        try {
            if (parts.length != 4) throw new IllegalArgumentException("Invalid inventory selection.");
            openInventorySlotActions(p, UUID.fromString(parts[1]), Integer.parseInt(parts[2]), parts[3].equalsIgnoreCase("ender"));
        } catch (IllegalArgumentException error) {
            p.sendMessage(color("&cInvalid inventory slot selection."));
        }
    } else if(t.startsWith("inventory-edit:")) {
        String[] parts = t.split(":");
        try {
            if (parts.length != 4) throw new IllegalArgumentException("Invalid inventory action.");
            editInventorySlot(p, UUID.fromString(parts[1]), Integer.parseInt(parts[2]), parts[3].equalsIgnoreCase("ender"), s);
        } catch (IllegalArgumentException error) {
            p.sendMessage(color("&cInvalid inventory action."));
        }
    } else if(t.equals("security-control")) {
        handleSecurity(p,s);
    } else if(t.equals("backups")) {
        if(s==47){AdminServices.backup().whenComplete((v,e2)->Bukkit.getScheduler().runTask(plugin,()->{p.sendMessage(color(e2==null?"&aBackup created.":"&cBackup failed: "+e2.getMessage()));openBackups(p);}));}
        else {List<Path> bs=AdminServices.backups();if(s>=0&&s<bs.size())confirmRestore(p,bs.get(s));}
    } else if(t.startsWith("restore:")) {
        if (s == 20) {
            PendingRestore pending = pendingRestores.remove(p.getUniqueId());
            long now = System.currentTimeMillis();
            if (pending == null || now < pending.createdAt()
                    || now - pending.createdAt() > 15_000L) {
                p.sendMessage(color("&cRestore confirmation expired or missing. Select the backup and confirm again."));
                openBackups(p);
                return;
            }
            try {
                int index = Integer.parseInt(t.substring("restore:".length()));
                List<Path> bs = AdminServices.backups();
                if (index < 0 || index >= bs.size()) { p.sendMessage(color("&cBackup is no longer available.")); openBackups(p); return; }
                Path backup = bs.get(index).toAbsolutePath().normalize();
                if (!backup.equals(pending.backup())) {
                    p.sendMessage(color("&cThe backup list changed. Select the intended backup and confirm again."));
                    openBackups(p);
                    return;
                }
                AdminServices.restore(backup).whenComplete((v,e2)->Bukkit.getScheduler().runTask(plugin,()->{
                    if (!p.isOnline()) return;
                    if (e2 == null) plugin.audit(p.getName(), "CORE_RESTORE:" + backup.getFileName());
                    p.sendMessage(color(e2==null?"&aRestore completed. A safety backup was made first.":"&cRestore failed: "+e2.getMessage()));
                    openBackups(p);
                }));
            } catch (NumberFormatException ex) { p.sendMessage(color("&cInvalid backup selection.")); openBackups(p); }
        } else if (s == 24) {
            pendingRestores.remove(p.getUniqueId());
            openBackups(p);
        }
    }
    }

    private boolean isTargetInPracticeActivity(Player target) {
        Plugin duels = Bukkit.getPluginManager().getPlugin("VoidFlame-Duels");
        if (duels == null || !duels.isEnabled()) return false;
        UUID id = target.getUniqueId();
        try {
            Object matchManager = duels.getClass().getMethod("matchManager").invoke(duels);
            if (invokeBoolean(matchManager, "isInMatch", id)
                    || invokeBoolean(matchManager, "isDisconnected", id)) return true;
            Object queueManager = duels.getClass().getMethod("queueManager").invoke(duels);
            if (invokeBoolean(queueManager, "isQueued", id)) return true;
            Object ffaManager = duels.getClass().getMethod("ffaManager").invoke(duels);
            return invokeBoolean(ffaManager, "isInFfa", id);
        } catch (ReflectiveOperationException | RuntimeException error) {
            plugin.getLogger().warning("Could not validate whether player " + id
                    + " is active in Practice; blocking destructive staff actions: " + error.getMessage());
            return true;
        }
    }

    private boolean invokeBoolean(Object target, String methodName, UUID playerId)
            throws ReflectiveOperationException {
        Object result = target.getClass().getMethod(methodName, UUID.class).invoke(target, playerId);
        return Boolean.TRUE.equals(result);
    }

    private void toggleStaffSpectator(Player staff, Player target) {
        UUID staffId = staff.getUniqueId();
        StaffSpectatorSnapshot existing = staffSpectators.get(staffId);
        if (existing != null) {
            stopStaffSpectating(staff);
            return;
        }
        if (staff.equals(target)) {
            staff.sendMessage(color("&cYou cannot spectate yourself."));
            return;
        }
        Location previousLocation = staff.getLocation().clone();
        GameMode previousGameMode = staff.getGameMode();
        staffSpectators.put(staffId, new StaffSpectatorSnapshot(previousLocation, previousGameMode));
        staff.setGameMode(GameMode.SPECTATOR);
        staff.setSpectatorTarget(target);
        staff.sendMessage(color("&bNow spectating &f" + target.getName() + "&b. Use Stop Spectating in the player manager to return."));
        plugin.audit(staff.getName(), "STAFF_SPECTATE_START:" + target.getUniqueId());
    }

    private void stopStaffSpectating(Player staff) {
        StaffSpectatorSnapshot existing = staffSpectators.remove(staff.getUniqueId());
        if (existing == null) {
            staff.sendMessage(color("&7You are not currently spectating."));
            return;
        }
        staff.setSpectatorTarget(null);
        staff.setGameMode(existing.gameMode());
        staff.teleportAsync(existing.location());
        staff.sendMessage(color("&aSpectator mode ended. Your previous game mode and location were restored."));
        plugin.audit(staff.getName(), "STAFF_SPECTATE_STOP");
    }

    public void restoreAllSpectators() {
        for (Map.Entry<UUID, StaffSpectatorSnapshot> entry : staffSpectators.entrySet()) {
            Player staff = Bukkit.getPlayer(entry.getKey());
            if (staff != null && staff.isOnline()) {
                staff.setSpectatorTarget(null);
                staff.setGameMode(entry.getValue().gameMode());
                staff.teleport(entry.getValue().location());
            }
        }
        staffSpectators.clear();
    }

    @EventHandler
    public void onStaffQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        StaffSpectatorSnapshot snapshot = staffSpectators.remove(id);
        if (snapshot != null) {
            Player player = event.getPlayer();
            player.setSpectatorTarget(null);
            player.setGameMode(snapshot.gameMode());
            player.teleport(snapshot.location());
        }
        input.remove(id);
        pendingRestores.remove(id);
    }

    private void openCoreInfo(Player staff, Player target) {
        if (!AdminAccess.hasAdminAccess(staff)) { staff.sendMessage(color("&cNo permission.")); return; }
        UUID targetId = target.getUniqueId();
        AdminServices.query("SELECT name,rank,wins,losses,winstreak,best_winstreak,elo,coins,last_join "
                + "FROM player_profiles WHERE uuid=?", targetId.toString())
                .whenComplete((rows, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!staff.isOnline()) return;
                    if (error != null) {
                        plugin.getLogger().warning("Could not load Core profile for " + targetId + ": " + error.getMessage());
                        staff.sendMessage(color("&cCore profile data could not be loaded."));
                        return;
                    }
                    List<DialogBody> body = new ArrayList<>();
                    body.add(DialogBody.plainMessage(Component.text("CORE PLAYER PROFILE", NamedTextColor.AQUA), 300));
                    body.add(DialogBody.plainMessage(Component.text("Player: " + target.getName()
                            + " • UUID: " + targetId, NamedTextColor.WHITE), 300));
                    body.add(DialogBody.plainMessage(Component.text("World: " + target.getWorld().getName()
                            + " • Ping: " + target.getPing() + " ms", NamedTextColor.GRAY), 300));
                    if (rows == null || rows.isEmpty()) {
                        body.add(DialogBody.plainMessage(Component.text("No persistent Core profile exists yet.", NamedTextColor.YELLOW), 300));
                    } else {
                        Map<String, Object> row = rows.getFirst();
                        body.add(DialogBody.plainMessage(Component.text("Rank: " + row.getOrDefault("rank", "player")
                                + " • Coins: " + row.getOrDefault("coins", 0), NamedTextColor.GOLD), 300));
                        body.add(DialogBody.plainMessage(Component.text("ELO: " + row.getOrDefault("elo", 1000)
                                + " • Wins: " + row.getOrDefault("wins", 0)
                                + " • Losses: " + row.getOrDefault("losses", 0), NamedTextColor.GREEN), 300));
                        body.add(DialogBody.plainMessage(Component.text("Winstreak: " + row.getOrDefault("winstreak", 0)
                                + " • Best: " + row.getOrDefault("best_winstreak", 0)
                                + " • Last join: " + row.getOrDefault("last_join", "unknown"), NamedTextColor.GRAY), 300));
                    }
                    showDialog(staff, "Core Player Info", "Persistent player data from VoidFlame-Core.",
                            List.of(action("Back", "Return to player actions.", "/vfadmin advclick player-actions:" + targetId + " 49", NamedTextColor.GRAY)),
                            body);
                }));
    }

    private void showInventoryDialog(Player staff, Player target, boolean enderChest) {
        showInventoryDialog(staff, target, enderChest, 0);
    }

    private void showInventoryDialog(Player staff, Player target, boolean enderChest, int requestedPage) {
        if (!AdminAccess.hasAdminAccess(staff)) { staff.sendMessage(color("&cJava staff only.")); return; }
        Inventory inventory = enderChest ? target.getEnderChest() : target.getInventory();
        int size = enderChest ? inventory.getSize() : target.getInventory().getStorageContents().length;
        int pageSize = 9;
        int pageCount = Math.max(1, (size + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int firstSlot = page * pageSize;
        int lastSlot = Math.min(size, firstSlot + pageSize);
        String inventoryType = enderChest ? "ender" : "player";
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text((enderChest ? "ENDER CHEST" : "PLAYER INVENTORY")
                + " • " + target.getName(), NamedTextColor.AQUA), 300));
        body.add(DialogBody.plainMessage(Component.text("Page " + (page + 1) + " of " + pageCount
                + " • Select a slot to edit it safely.", NamedTextColor.GRAY), 300));
        List<ActionButton> actions = new ArrayList<>();
        for (int slot = firstSlot; slot < lastSlot; slot++) {
            ItemStack item = inventory.getItem(slot);
            String itemName = item == null || item.getType() == Material.AIR ? "Empty"
                    : item.getAmount() + " × " + item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            body.add(DialogBody.plainMessage(Component.text("Slot " + (slot + 1) + ": " + itemName,
                    item == null || item.getType() == Material.AIR ? NamedTextColor.GRAY : NamedTextColor.WHITE), 280));
            actions.add(action("Slot " + (slot + 1), itemName + " • Open slot actions.",
                    "/vfadmin advclick inventory-slot:" + target.getUniqueId() + ":" + slot + ":" + inventoryType + ":" + page + " 10",
                    NamedTextColor.AQUA));
        }
        if (page > 0) actions.add(action("Previous Page", "Show the previous set of slots.",
                "/vfadmin advclick inventory-page:" + target.getUniqueId() + ":" + (page - 1) + ":" + inventoryType + " 10",
                NamedTextColor.BLUE));
        if (page + 1 < pageCount) actions.add(action("Next Page", "Show the next set of slots.",
                "/vfadmin advclick inventory-page:" + target.getUniqueId() + ":" + (page + 1) + ":" + inventoryType + " 10",
                NamedTextColor.BLUE));
        actions.add(action("Refresh", "Refresh this inventory listing.",
                "/vfadmin advclick inventory-page:" + target.getUniqueId() + ":" + page + ":" + inventoryType + " 10",
                NamedTextColor.GREEN));
        actions.add(action("Back", "Return to player management.",
                "/vfadmin advclick player-actions:" + target.getUniqueId() + " 49", NamedTextColor.GRAY));
        showDialog(staff, (enderChest ? "Ender Chest" : "Inventory") + " • " + target.getName(),
                "Select a slot to move its stack to your inventory or place the item in your hand into an empty slot.",
                actions, body);
    }

    private void openInventorySlotActions(Player staff, UUID targetId, int slot, boolean enderChest, int page) {
        if (!AdminAccess.hasAdminAccess(staff)) { staff.sendMessage(color("&cNo permission.")); return; }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            staff.sendMessage(color("&cThat player is no longer online."));
            openPlayerManager(staff);
            return;
        }
        if (staff.getUniqueId().equals(targetId)) {
            staff.sendMessage(color("&cUse your normal inventory to manage your own items."));
            showInventoryDialog(staff, target, enderChest, page);
            return;
        }
        if (isTargetInPracticeActivity(target)) {
            staff.sendMessage(color("&cInventory editing is disabled while the target is queued or in a match/FFA."));
            showInventoryDialog(staff, target, enderChest, page);
            return;
        }
        Inventory inventory = enderChest ? target.getEnderChest() : target.getInventory();
        int size = enderChest ? inventory.getSize() : target.getInventory().getStorageContents().length;
        if (slot < 0 || slot >= size) {
            staff.sendMessage(color("&cInvalid inventory slot."));
            showInventoryDialog(staff, target, enderChest, page);
            return;
        }
        ItemStack item = inventory.getItem(slot);
        boolean occupied = item != null && item.getType() != Material.AIR;
        ItemStack held = staff.getInventory().getItemInMainHand();
        boolean holdingItem = held != null && held.getType() != Material.AIR;
        String type = "inventory-edit:" + targetId + ":" + slot + ":" + (enderChest ? "ender" : "player") + ":" + page;
        List<ActionButton> actions = new ArrayList<>();
        if (occupied) actions.add(action("Take Stack", "Move this stack into your inventory without deleting it.",
                "/vfadmin advclick " + type + " 10", NamedTextColor.GOLD));
        if (!occupied && holdingItem) actions.add(action("Place Held Stack", "Move the item in your main hand into this empty slot.",
                "/vfadmin advclick " + type + " 12", NamedTextColor.GREEN));
        actions.add(action("Back", "Return to the inventory slots.",
                "/vfadmin advclick inventory-page:" + targetId + ":" + page + ":" + (enderChest ? "ender" : "player") + " 10",
                NamedTextColor.GRAY));
        showDialog(staff, (enderChest ? "Ender Chest" : "Inventory") + " • Slot " + (slot + 1),
                "Target: " + target.getName() + " • " + (occupied ? item.getAmount() + " × " + item.getType().name() : "Empty"),
                actions);
    }

    private void editInventorySlot(Player staff, UUID targetId, int slot, boolean enderChest, int action, int page) {
        if (!AdminAccess.hasAdminAccess(staff)) { staff.sendMessage(color("&cNo permission.")); return; }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            staff.sendMessage(color("&cThat player is no longer online."));
            openPlayerManager(staff);
            return;
        }
        if (staff.getUniqueId().equals(targetId)) {
            staff.sendMessage(color("&cYou cannot edit your own inventory through the staff manager."));
            return;
        }
        if (isTargetInPracticeActivity(target)) {
            staff.sendMessage(color("&cInventory editing is disabled while the target is queued or in a match/FFA."));
            showInventoryDialog(staff, target, enderChest, page);
            return;
        }
        Inventory inventory = enderChest ? target.getEnderChest() : target.getInventory();
        int size = enderChest ? inventory.getSize() : target.getInventory().getStorageContents().length;
        if (slot < 0 || slot >= size) {
            staff.sendMessage(color("&cInvalid inventory slot."));
            showInventoryDialog(staff, target, enderChest, page);
            return;
        }
        ItemStack existing = inventory.getItem(slot);
        boolean occupied = existing != null && existing.getType() != Material.AIR;
        if (action == 10) {
            if (!occupied) {
                staff.sendMessage(color("&cThis slot is already empty."));
            } else {
                ItemStack moving = existing.clone();
                inventory.setItem(slot, null);
                Map<Integer, ItemStack> leftovers = staff.getInventory().addItem(moving);
                if (!leftovers.isEmpty()) {
                    inventory.setItem(slot, leftovers.values().iterator().next());
                    staff.sendMessage(color("&eYour inventory was full; the untransferred remainder stayed in the target slot."));
                } else {
                    staff.sendMessage(color("&aMoved the stack to your inventory."));
                }
                plugin.audit(staff.getName(), "PLAYER_INVENTORY_TAKE:" + targetId + ":" + slot + ":" + enderChest);
            }
        } else if (action == 12) {
            ItemStack held = staff.getInventory().getItemInMainHand();
            if (occupied) {
                staff.sendMessage(color("&cThe target slot is no longer empty."));
            } else if (held == null || held.getType() == Material.AIR) {
                staff.sendMessage(color("&cHold an item in your main hand first."));
            } else {
                inventory.setItem(slot, held.clone());
                staff.getInventory().setItemInMainHand(null);
                staff.sendMessage(color("&aPlaced the held stack into the target inventory."));
                plugin.audit(staff.getName(), "PLAYER_INVENTORY_PLACE:" + targetId + ":" + slot + ":" + enderChest);
            }
        } else {
            staff.sendMessage(color("&cInvalid inventory action."));
        }
        showInventoryDialog(staff, target, enderChest, page);
    }

    private void openInventoryPageFromToken(Player staff, String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 4) { staff.sendMessage(color("&cInvalid inventory selection.")); return; }
        try {
            UUID targetId = UUID.fromString(parts[1]);
            int page = Integer.parseInt(parts[2]);
            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                staff.sendMessage(color("&cThat player is no longer online."));
                return;
            }
            showInventoryDialog(staff, target, parts[3].equalsIgnoreCase("ender"), page);
        } catch (IllegalArgumentException error) {
            staff.sendMessage(color("&cInvalid inventory identifier."));
        }
    }

    private void openInventoryFromToken(Player staff, String raw) {
        String[] parts = raw.split(":");
        if (parts.length != 3) { staff.sendMessage(color("&cInvalid inventory selection.")); return; }
        try {
            UUID targetId = UUID.fromString(parts[1]);
            Player target = Bukkit.getPlayer(targetId);
            if (target == null) { staff.sendMessage(color("&cThat player is no longer online.")); return; }
            showInventoryDialog(staff, target, parts[2].equalsIgnoreCase("ender"));
        } catch (IllegalArgumentException error) {
            staff.sendMessage(color("&cInvalid player identifier."));
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

    @EventHandler public void chat(AsyncPlayerChatEvent e) {
        UUID staffId = e.getPlayer().getUniqueId();
        PendingInput req = input.get(staffId);
        if (req == null) return;
        e.setCancelled(true);
        String message = e.getMessage();
        if (System.currentTimeMillis() - req.createdAt() > 30_000L) {
            input.remove(staffId, req);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player staff = Bukkit.getPlayer(staffId);
                if (staff != null) staff.sendMessage(color("&cInput timed out."));
            });
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            input.remove(staffId, req);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player staff = Bukkit.getPlayer(staffId);
                if (staff != null) staff.sendMessage(color("&7Cancelled."));
            });
            return;
        }
        if (!input.remove(staffId, req)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player staff = Bukkit.getPlayer(staffId);
            if (staff == null || !staff.isOnline()) return;
            Player target = Bukkit.getPlayer(req.target());
            if (target == null || !target.isOnline()) {
                staff.sendMessage(color("&cTarget is no longer online."));
                return;
            }
            apply(staff, req.action(), target, req.duration(), message);
        });
    }

    private List<Player> sortedPlayers(){List<Player> p=new ArrayList<>(Bukkit.getOnlinePlayers());p.sort(Comparator.comparing(Player::getName,String.CASE_INSENSITIVE_ORDER));return p;}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    private long num(List<Map<String,Object>> r){return r.isEmpty()?0:((Number)r.get(0).values().iterator().next()).longValue();}
    private double value(List<Map<String,Object>> r,String k){return r.isEmpty()?0:((Number)r.get(0).getOrDefault(k,0)).doubleValue();}
    private record PendingInput(String action,UUID target,String duration,long createdAt){}
    private record PendingRestore(Path backup, long createdAt){}
    private record StaffSpectatorSnapshot(Location location, GameMode gameMode){}
    private record Analytics(double players,double matches,double elo,double wins,double losses,String topKit,String topPlayer){}
}
