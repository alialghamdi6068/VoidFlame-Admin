package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

final class AdminAccess {
    private AdminAccess() {}

    static boolean isBedrock(Player player) {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if (floodgate == null || !floodgate.isEnabled()) return false;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method check = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            return Boolean.TRUE.equals(check.invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException | LinkageError ex) {
            // Fail closed: if Floodgate is installed but its identity API cannot be checked,
            // do not grant administrative access based on an uncertain client identity.
            return true;
        }
    }

    static boolean hasAdminAccess(Player player) {
        return !isBedrock(player)
                && (player.hasPermission("voidflame.admin")
                || player.hasPermission("voidflame.admin.full"));
    }

    static boolean hasStaffAccess(Player player) {
        return !isBedrock(player)
                && (player.hasPermission("voidflame.staff")
                || player.hasPermission("voidflame.admin")
                || player.hasPermission("voidflame.admin.full"));
    }
}
