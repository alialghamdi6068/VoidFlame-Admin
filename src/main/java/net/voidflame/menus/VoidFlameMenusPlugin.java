package net.voidflame.menus;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameMenusPlugin extends JavaPlugin {
    private StaffGui staffGui;

    @Override
    public void onEnable() {
        getCommand("vfadmin").setExecutor(new AdminCommand(this));
        getCommand("staff").setExecutor(new StaffCommand(this));
        getCommand("staffgui").setExecutor(new StaffCommand(this));
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new AdminMenu(this), this);
        staffGui = new StaffGui(this);
        staffGui.onEnable();
        getLogger().info("VoidFlame Admin GUI enabled.");
    }

    @Override
    public void onDisable() {
        if (staffGui != null) staffGui.onDisable();
    }
}
