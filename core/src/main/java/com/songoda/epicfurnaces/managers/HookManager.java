package com.songoda.epicfurnaces.managers;

import com.google.common.base.Preconditions;
import com.songoda.epicfurnaces.EpicFurnaces;
import com.songoda.epicfurnaces.hooks.ProtectionPluginHook;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class HookManager {
    private final EpicFurnaces instance;
    private Set<ProtectionPluginHook> protectionHooks;

    public HookManager(EpicFurnaces instance) {
        this.protectionHooks = new HashSet<>();
        this.instance = instance;
        protectionHooks.clear();
    }

    public void register(Supplier<ProtectionPluginHook> hookSupplier) {
        this.registerProtectionHook(hookSupplier.get());
    }

    private void registerProtectionHook(ProtectionPluginHook hook) {
        Preconditions.checkNotNull(hook, "Cannot register null hooks");
        Preconditions.checkNotNull(hook.getPlugin(), "Protection plugin hooks returns null plugin instance (#getPlugin())");

        JavaPlugin hookPlugin = hook.getPlugin();
        for (ProtectionPluginHook existingHook : protectionHooks) {
            if (existingHook.getPlugin().equals(hookPlugin)) {
                throw new IllegalArgumentException("Hook already registered");
            }
        }

        FileConfiguration configuration = instance.getConfiguration("hooks");
        configuration.addDefault("hooks." + hookPlugin.getName(), true);
        if (!configuration.getBoolean("hooks." + hookPlugin.getName(), true)) return;
        configuration.options().copyDefaults(true);
        instance.save("hooks");

        protectionHooks.add(hook);
        instance.getLogger().info("Registered protection hooks for plugin: " + hook.getPlugin().getName() + " v" + hook.getPlugin().getDescription().getVersion());
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission(instance.getDescription().getName() + ".bypass")) {
            return true;
        }

        for (ProtectionPluginHook hook : protectionHooks) {
            if (!hook.canBuild(player, location)) {
                return false;
            }
        }
        return true;
    }
}
