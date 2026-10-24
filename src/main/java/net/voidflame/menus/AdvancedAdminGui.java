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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AdvancedAdminGui implements Listener {
    private static final String ROOT = "§8VoidFlame §5• §d";
    private final VoidFlameMenusPlugin plugin;
    private final Map<UUID, PendingInput> input = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRestore> pendingRestores = new ConcurrentHashMap<>();

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
                        action("Spectate", "Follow target in spectator mode.", "/vfadmin advclick " + type + " 30", NamedTextColor.BLUE),
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
    if (s==49) {
        if (t.startsWith("punish-select:") || t.startsWith("duration:")) { openPunishments(p); return; }
        if (t.startsWith("player-actions:")) { openPlayerManager(p); return; }
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
        if(s==10){p.sendMessage(color("&bCore info for &f"+target.getName()+" &7» &f"+target.getUniqueId()));p.sendMessage(color("&7World: &f"+target.getWorld().getName()+" &7Ping: &f"+target.getPing()));}
        else if(s==12)showInventoryDialog(p,target,false);
        else if(s==14)showInventoryDialog(p,target,true);
        else if(s==16)p.teleportAsync(target.getLocation());
        else if(s==28)target.teleportAsync(p.getLocation());
        else if(s==30){p.setGameMode(org.bukkit.GameMode.SPECTATOR);p.setSpectatorTarget(target);}
        else if(s==32){target.setHealth(target.getMaxHealth());target.setFoodLevel(20);target.setFireTicks(0);}
        else if(s==34)target.getInventory().clear();
        else if(s==49)openPlayerManager(p);
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

    private void showInventoryDialog(Player staff, Player target, boolean enderChest) {
        if (!AdminAccess.hasAdminAccess(staff)) { staff.sendMessage(color("&cJava staff only.")); return; }
        ItemStack[] contents = enderChest ? target.getEnderChest().getContents() : target.getInventory().getStorageContents();
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text((enderChest ? "ENDER CHEST" : "PLAYER INVENTORY") + " • " + target.getName(), NamedTextColor.AQUA), 300));
        boolean found = false;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) continue;
            found = true;
            body.add(DialogBody.plainMessage(Component.text("Slot " + (slot + 1) + ": " + item.getAmount() + " × "
                    + item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '), NamedTextColor.WHITE), 300));
        }
        if (!found) body.add(DialogBody.plainMessage(Component.text("No items in this inventory.", NamedTextColor.GRAY), 300));
        String type = "player-actions:" + target.getUniqueId();
        List<ActionButton> actions = List.of(
                action("Refresh", "Refresh this inventory listing.", "/vfadmin advclick " + type + (enderChest ? " 14" : " 12"), NamedTextColor.AQUA),
                action("Back", "Return to player management.", "/vfadmin advclick " + type + " 49", NamedTextColor.GRAY));
        showDialog(staff, (enderChest ? "Ender Chest" : "Inventory") + " • " + target.getName(),
                "Read-only item inspection. Item editing uses Minecraft inventory screens and is not available in Dialogs.",
                actions, body);
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
        PendingInput req=input.get(e.getPlayer().getUniqueId()); if(req==null)return;
        e.setCancelled(true);
        if(System.currentTimeMillis()-req.createdAt>30_000L){input.remove(e.getPlayer().getUniqueId());e.getPlayer().sendMessage(color("&cInput timed out."));return;}
        if(e.getMessage().equalsIgnoreCase("cancel")){input.remove(e.getPlayer().getUniqueId());e.getPlayer().sendMessage(color("&7Cancelled."));return;}
        input.remove(e.getPlayer().getUniqueId());
        Player target=Bukkit.getPlayer(req.target); if(target==null){e.getPlayer().sendMessage(color("&cTarget is no longer online."));return;}
        Bukkit.getScheduler().runTask(plugin,()->apply(e.getPlayer(),req.action,target,req.duration,e.getMessage()));
    }

    private List<Player> sortedPlayers(){List<Player> p=new ArrayList<>(Bukkit.getOnlinePlayers());p.sort(Comparator.comparing(Player::getName,String.CASE_INSENSITIVE_ORDER));return p;}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    private long num(List<Map<String,Object>> r){return r.isEmpty()?0:((Number)r.get(0).values().iterator().next()).longValue();}
    private double value(List<Map<String,Object>> r,String k){return r.isEmpty()?0:((Number)r.get(0).getOrDefault(k,0)).doubleValue();}
    private record PendingInput(String action,UUID target,String duration,long createdAt){}
    private record PendingRestore(Path backup, long createdAt){}
    private record Analytics(double players,double matches,double elo,double wins,double losses,String topKit,String topPlayer){}
}
