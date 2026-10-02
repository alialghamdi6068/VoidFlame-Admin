package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

final class PunishmentService implements Listener {
    private final VoidFlameMenusPlugin plugin;
    private Object storage;

    PunishmentService(VoidFlameMenusPlugin plugin) {
        this.plugin = plugin;
        this.storage = resolveStorage();
    }

    boolean execute(Player actor, String[] args) {
        if (!plugin.getConfig().getBoolean("punishments.enabled", true)) {
            actor.sendMessage(color("&cPunishments are disabled."));
            return true;
        }
        if (!actor.hasPermission("voidflame.admin.punishments") && !actor.hasPermission("voidflame.admin.full")) {
            actor.sendMessage(color("&cYou do not have permission."));
            return true;
        }
        if (args.length == 0) {
            help(actor);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("history")) return history(actor, args);
        if (action.equals("unpunish") || action.equals("unban") || action.equals("unmute")) return unpunish(actor, args);
        if (!Set.of("ban", "tempban", "mute", "tempmute", "warn", "kick").contains(action)) {
            help(actor);
            return true;
        }
        if (args.length < 2) {
            actor.sendMessage(color("&cUsage: /vfadmin punish " + action + " <player> [duration] [reason]"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        UUID targetId = target.getUniqueId();
        String targetName = target.getName() == null ? args[1] : target.getName();
        if (targetId.equals(actor.getUniqueId()) && !action.equals("warn")) {
            actor.sendMessage(color("&cYou cannot punish yourself."));
            return true;
        }

        String durationToken = null;
        int reasonStart = 2;
        if (action.equals("tempban") || action.equals("tempmute")) {
            if (args.length < 3) {
                actor.sendMessage(color("&cA duration is required."));
                return true;
            }
            durationToken = args[2];
            reasonStart = 3;
        }
        String reason = reasonStart >= args.length ? plugin.getConfig().getString("punishments.default-reason", "No reason provided")
                : String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length));
        long expires = durationToken == null ? 0L : parseDuration(durationToken);
        if (durationToken != null && expires <= 0L) {
            actor.sendMessage(color("&cInvalid duration. Use 10m, 2h, 7d or 30s."));
            return true;
        }

        String type = action.toUpperCase(Locale.ROOT);
        hierarchyAllowed(actor, targetId).thenAccept(allowed -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!allowed) {
                actor.sendMessage(color("&cYou cannot punish a player with an equal or higher staff rank."));
                return;
            }
            insert(targetId, targetName, actor, type, reason, expires).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                applyOnlineAction(action, targetId, reason, expires);
                actor.sendMessage(color("&aPunishment applied: &f" + type + " &ato &f" + targetName));
                plugin.audit(actor.getName(), "PUNISH:" + type + ":" + targetName + ":" + reason);
            }));
        }));
        return true;
    }

    private CompletableFuture<Boolean> hierarchyAllowed(Player actor, UUID target) {
        if (actor.hasPermission("voidflame.admin.full")) return CompletableFuture.completedFuture(true);
        return query("SELECT COALESCE((SELECT weight FROM ranks r JOIN player_ranks pr ON pr.rank_id=r.rank_id WHERE pr.uuid=?), 0) AS actor_weight, " +
                "COALESCE((SELECT weight FROM ranks r JOIN player_ranks pr ON pr.rank_id=r.rank_id WHERE pr.uuid=?), 0) AS target_weight",
                actor.getUniqueId().toString(), target.toString()).thenApply(rows -> {
            if (rows.isEmpty()) return true;
            Number a = (Number) rows.get(0).get("actor_weight");
            Number t = (Number) rows.get(0).get("target_weight");
            return a == null || t == null || a.intValue() > t.intValue();
        });
    }

    private CompletableFuture<Void> insert(UUID targetId, String targetName, Player actor, String type, String reason, long expiresAfter) {
        long now = System.currentTimeMillis();
        long expires = expiresAfter <= 0 ? 0L : now + expiresAfter;
        return update("INSERT INTO punishments(uuid,type,actor_uuid,actor_name,reason,created_at,expires_at,active) VALUES(?,?,?,?,?,?,?,1)",
                targetId.toString(), type, actor.getUniqueId().toString(), actor.getName(), reason, now, expires == 0L ? null : expires);
    }

    private void applyOnlineAction(String action, UUID targetId, String reason, long expiresAfter) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) return;
        String reasonText = reason.isBlank() ? "No reason provided" : reason;
        switch (action) {
            case "ban", "tempban" -> target.kickPlayer(color("&cYou are banned from VoidFlame.\n&7Reason: &f" + reasonText));
            case "mute", "tempmute" -> target.sendMessage(color("&cYou are muted. &7Reason: &f" + reasonText));
            case "warn" -> target.sendMessage(color("&eWarning: &f" + reasonText));
            case "kick" -> target.kickPlayer(color("&cYou have been kicked.\n&7Reason: &f" + reasonText));
        }
    }

    private CompletableFuture<List<Map<String,Object>>> history(Player actor, String[] args) {
        if (args.length < 2) {
            actor.sendMessage(color("&cUsage: /vfadmin punish history <player>"));
            return CompletableFuture.completedFuture(List.of());
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        return query("SELECT type,actor_name,reason,created_at,expires_at,active FROM punishments WHERE uuid=? ORDER BY created_at DESC LIMIT 50",
                target.getUniqueId().toString()).thenAccept(rows -> Bukkit.getScheduler().runTask(plugin, () -> {
            actor.sendMessage(color("&5&lVoidFlame Punishment History &7» &f" + args[1]));
            if (rows.isEmpty()) actor.sendMessage(color("&7No punishment history."));
            for (Map<String,Object> row : rows) {
                actor.sendMessage(color("&8• &d" + row.get("type") + " &7by &f" + row.get("actor_name")
                        + " &8| &7" + row.get("reason") + " &8| &7active=" + row.get("active")));
            }
        }));
    }

    private boolean unpunish(Player actor, String[] args) {
        if (args.length < 2) {
            actor.sendMessage(color("&cUsage: /vfadmin punish unpunish <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        hierarchyAllowed(actor, target.getUniqueId()).thenAccept(allowed -> {
            if (!allowed) { actor.sendMessage(color("&cYou cannot modify an equal or higher staff player.")); return; }
            update("UPDATE punishments SET active=0 WHERE uuid=? AND active=1", target.getUniqueId().toString())
                    .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                        actor.sendMessage(color("&aActive punishments cleared for &f" + args[1]));
                        plugin.audit(actor.getName(), "UNPUNISH:" + args[1]);
                    }));
        });
        return true;
    }

    private void check(Player player, String type, java.util.function.Consumer<Boolean> result) {
        query("SELECT 1 FROM punishments WHERE uuid=? AND type IN (?) AND active=1 AND (expires_at IS NULL OR expires_at=0 OR expires_at>?) LIMIT 1",
                player.getUniqueId().toString(), type, System.currentTimeMillis()).thenAccept(rows -> result.accept(!rows.isEmpty()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        check(event.getPlayer(), "BAN", banned -> {
            if (banned) Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().kickPlayer(color("&cYou are banned from VoidFlame.")));
        });
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        check(player, "MUTE", muted -> { if (muted) event.setCancelled(true); });
        check(player, "TEMPMUTE", muted -> { if (muted) event.setCancelled(true); });
    }

    private void help(Player p) {
        p.sendMessage(color("&5&lVoidFlame Punishments"));
        p.sendMessage(color("&d/vfadmin punish ban <player> [reason]"));
        p.sendMessage(color("&d/vfadmin punish tempban <player> <duration> [reason]"));
        p.sendMessage(color("&d/vfadmin punish mute <player> [reason]"));
        p.sendMessage(color("&d/vfadmin punish tempmute <player> <duration> [reason]"));
        p.sendMessage(color("&d/vfadmin punish warn <player> [reason]"));
        p.sendMessage(color("&d/vfadmin punish kick <player> [reason]"));
        p.sendMessage(color("&d/vfadmin punish history <player>"));
        p.sendMessage(color("&d/vfadmin punish unpunish <player>"));
    }

    private long parseDuration(String token) {
        try {
            long value = Long.parseLong(token.substring(0, token.length() - 1));
            return switch (Character.toLowerCase(token.charAt(token.length() - 1))) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                default -> -1L;
            };
        } catch (Exception e) { return -1L; }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<Map<String,Object>>> query(String sql, Object... params) {
        Object provider = storage == null ? resolveStorage() : storage;
        if (provider == null) return CompletableFuture.completedFuture(List.of());
        try {
            Method m = provider.getClass().getMethod("query", String.class, Object[].class);
            return (CompletableFuture<List<Map<String,Object>>>) m.invoke(provider, sql, params);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private CompletableFuture<Void> update(String sql, Object... params) {
        Object provider = storage == null ? resolveStorage() : storage;
        if (provider == null) return CompletableFuture.failedFuture(new IllegalStateException("VoidFlame-Core unavailable"));
        try {
            Method m = provider.getClass().getMethod("query", String.class, Object[].class);
            @SuppressWarnings("unchecked")
            CompletableFuture<List<Map<String,Object>>> f = (CompletableFuture<List<Map<String,Object>>>) m.invoke(provider, sql, params);
            return f.thenApply(ignored -> null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private Object resolveStorage() {
        try {
            Class<?> type = Class.forName("net.voidflame.core.storage.StorageService");
            var registration = Bukkit.getServicesManager().getRegistration(type);
            return registration == null ? null : registration.getProvider();
        } catch (Exception e) { return null; }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
