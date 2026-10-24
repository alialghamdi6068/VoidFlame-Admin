package net.voidflame.menus;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameMenusPlugin extends JavaPlugin {
    private StaffGui staffGui;
    private AdminAudit audit;
    private PunishmentService punishments;
    private AdvancedAdminGui advancedAdminGui;
    private AdminMenu adminMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        audit = new AdminAudit(this);
        punishments = new PunishmentService(this);
        advancedAdminGui = new AdvancedAdminGui(this);
        adminMenu = new AdminMenu(this);
        getServer().getPluginManager().registerEvents(punishments, this);
        getCommand("vfadmin").setExecutor(new AdminCommand(this));
        getCommand("vfadmin").setTabCompleter(new AdminCommand(this));
        getCommand("vfanalytics").setExecutor(new AnalyticsCommand(this));
        getCommand("staff").setExecutor(new StaffCommand(this));
        getCommand("staffgui").setExecutor(new StaffCommand(this));
        getServer().getPluginManager().registerEvents(adminMenu, this);
        getServer().getPluginManager().registerEvents(advancedAdminGui, this);
        staffGui = new StaffGui(this);
        staffGui.onEnable();
        getLogger().info("VoidFlame Admin GUI enabled.");
    }

    @Override
    public void onDisable() {
        if (staffGui != null) staffGui.onDisable();
        if (audit != null) audit.shutdown();
    }

    PunishmentService punishments() { return punishments; }

    AdvancedAdminGui advancedAdminGui() { return advancedAdminGui; }

    AdminMenu adminMenu() { return adminMenu; }

    StaffGui staffGui() { return staffGui; }

    void audit(String actor, String action) {
        if (audit != null && getConfig().getBoolean("logging.file.enabled", true)) audit.log(actor, action);
    }
}
