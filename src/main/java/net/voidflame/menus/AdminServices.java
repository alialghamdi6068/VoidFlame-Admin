package net.voidflame.menus;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class AdminServices {
    private AdminServices() {}

    static Object coreStorage() {
        try {
            Class<?> type = Class.forName("net.voidflame.core.storage.StorageService");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(type);
            return registration == null ? null : registration.getProvider();
        } catch (Exception e) {
            return null;
        }
    }

    static Object coreDatabase() {
        Object storage = coreStorage();
        if (storage == null) return null;
        try { return storage.getClass().getMethod("database").invoke(storage); }
        catch (Exception e) { return null; }
    }

    static CompletableFuture<List<java.util.Map<String,Object>>> query(String sql, Object... params) {
        Object storage = coreStorage();
        if (storage == null) return CompletableFuture.completedFuture(List.of());
        try {
            Method m = storage.getClass().getMethod("query", String.class, Object[].class);
            @SuppressWarnings("unchecked")
            CompletableFuture<List<java.util.Map<String,Object>>> result =
                    (CompletableFuture<List<java.util.Map<String,Object>>>) m.invoke(storage, sql, params);
            return result;
        } catch (Exception e) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    static CompletableFuture<Void> backup() {
        Object db = coreDatabase();
        if (db == null) return CompletableFuture.failedFuture(new IllegalStateException("VoidFlame-Core database unavailable"));
        try {
            Method path = db.getClass().getMethod("databasePath");
            Method backup = db.getClass().getMethod("backup", Path.class);
            Path data = Path.of(String.valueOf(path.invoke(db))).getParent();
            Path backups = data.resolve("backups");
            @SuppressWarnings("unchecked")
            CompletableFuture<Path> future = (CompletableFuture<Path>) backup.invoke(db, backups);
            return future.thenAccept(ignored -> {});
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    static CompletableFuture<Void> restoreLatest() {
        Object db = coreDatabase();
        if (db == null) return CompletableFuture.failedFuture(new IllegalStateException("VoidFlame-Core database unavailable"));
        try {
            Method path = db.getClass().getMethod("databasePath");
            Path database = Path.of(String.valueOf(path.invoke(db)));
            Path backups = database.getParent().resolve("backups");
            if (!Files.isDirectory(backups)) return CompletableFuture.failedFuture(new IllegalStateException("No backup directory exists"));
            Path latest;
            try (var stream = Files.list(backups)) {
                latest = stream.filter(p -> p.getFileName().toString().startsWith("database-") && p.toString().endsWith(".db"))
                        .max(Comparator.comparing(Path::toString)).orElse(null);
            }
            if (latest == null) return CompletableFuture.failedFuture(new IllegalStateException("No database backup found"));
            Method restore = db.getClass().getMethod("restore", Path.class);
            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) restore.invoke(db, latest);
            return future;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    static Object coreWorlds() {
        try {
            Class<?> type = Class.forName("net.voidflame.core.world.WorldService");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(type);
            return registration == null ? null : registration.getProvider();
        } catch (Exception e) {
            return null;
        }
    }

    static World world(Object service, String name) {
        if (service == null) return null;
        try { return (World) service.getClass().getMethod("get", String.class).invoke(service, name); }
        catch (Exception e) { return Bukkit.getWorld(name); }
    }

    static boolean unloadWorld(Object service, String name) throws Exception {
        if (service == null) return Bukkit.unloadWorld(name, true);
        return (Boolean) service.getClass().getMethod("unload", String.class, boolean.class).invoke(service, name, true);
    }

    static World loadWorld(Object service, String name) throws Exception {
        if (service == null) {
            World w = Bukkit.getWorld(name);
            return w != null ? w : Bukkit.createWorld(new org.bukkit.WorldCreator(name));
        }
        return (World) service.getClass().getMethod("load", String.class).invoke(service, name);
    }

    static void enableWorld(Object service, String name) throws Exception {
        if (service != null) service.getClass().getMethod("enable", String.class).invoke(service, name);
    }

    static void disableWorld(Object service, String name) throws Exception {
        if (service != null) service.getClass().getMethod("disable", String.class).invoke(service, name);
    }

    static Object security() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("VoidFlame-Security");
        if (plugin == null) return null;
        return plugin;
    }

    static boolean securityBoolean(String method, boolean fallback) {
        Object service = security();
        if (service == null) return fallback;
        try { return (Boolean) service.getClass().getMethod(method).invoke(service); }
        catch (Exception e) { return fallback; }
    }

    static int securityInt(String method, int fallback) {
        Object service = security();
        if (service == null) return fallback;
        try { return ((Number) service.getClass().getMethod(method).invoke(service)).intValue(); }
        catch (Exception e) { return fallback; }
    }

    static boolean setSecurityInt(String method, int value) { Object service=security(); if(service==null)return false; try{service.getClass().getMethod(method,int.class).invoke(service,value);return true;}catch(Exception e){return false;} }

    static List<Path> backups() { Object db=coreDatabase(); if(db==null)return List.of(); try{Path database=Path.of(String.valueOf(db.getClass().getMethod("databasePath").invoke(db))); Path dir=database.getParent().resolve("backups"); if(!Files.isDirectory(dir))return List.of(); try(var stream=Files.list(dir)){return stream.filter(p->p.getFileName().toString().startsWith("database-")&&p.toString().endsWith(".db")).sorted(Comparator.comparing(Path::toString).reversed()).toList();}}catch(Exception e){return List.of();} }

    static CompletableFuture<Void> restore(Path backup) { Object db=coreDatabase(); if(db==null)return CompletableFuture.failedFuture(new IllegalStateException("VoidFlame-Core database unavailable")); try{Method m=db.getClass().getMethod("restore",Path.class); @SuppressWarnings("unchecked") CompletableFuture<Void> f=(CompletableFuture<Void>)m.invoke(db,backup); return f;}catch(Exception e){return CompletableFuture.failedFuture(e);} }

    static boolean setSecurity(String method, boolean value) {
        Object service = security();
        if (service == null) return false;
        try { service.getClass().getMethod(method, boolean.class).invoke(service, value); return true; }
        catch (Exception e) { return false; }
    }
}
