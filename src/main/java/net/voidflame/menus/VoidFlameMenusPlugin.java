package net.voidflame.menus;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameMenusPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getCommand("vfadmin").setExecutor(new AdminCommand(this));
        getCommand("staff").setExecutor(new StaffCommand(this));
        getCommand("staffgui").setExecutor(new StaffCommand(this));
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new AdminMenu(this), this);
        getServer().getPluginManager().registerEvents(new StaffGui(this), this);
        getLogger().info("Unified admin GUI enabled.");
    }
}
