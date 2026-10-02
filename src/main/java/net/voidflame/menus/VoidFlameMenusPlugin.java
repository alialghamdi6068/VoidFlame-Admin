package net.voidflame.menus;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameMenusPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getCommand("vfadmin").setExecutor(new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new AdminMenu(this), this);
        getLogger().info("Unified admin GUI enabled.");
    }
}
