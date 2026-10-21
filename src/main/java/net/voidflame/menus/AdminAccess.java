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
        if (floodgate != null && floodgate.isEnabled()) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Object api = apiClass.getMethod("getInstance").invoke(null);
                Method check = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                return Boolean.TRUE.equals(check.invoke(api, player.getUniqueId()));
            } catch (ReflectiveOperationException | LinkageError ex) {
                return true;
            }
        }

        Plugin geyser = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (geyser == null) geyser = Bukkit.getPluginManager().getPlugin("Geyser");
        if (geyser != null && geyser.isEnabled()) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
                Object api = apiClass.getMethod("api").invoke(null);
                Method check = apiClass.getMethod("isBedrockPlayer", UUID.class);
                return Boolean.TRUE.equals(check.invoke(api, player.getUniqueId()));
            } catch (ReflectiveOperationException | LinkageError ex) {
                return true;
            }
        }
        return false;
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
