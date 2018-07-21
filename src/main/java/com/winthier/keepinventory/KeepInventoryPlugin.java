package com.winthier.keepinventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class KeepInventoryPlugin extends JavaPlugin implements Listener {
    private Set<UUID> optOuts;
    private boolean supportMiniMap = true;

    @Override
    public void onEnable() {
        optOuts = null;
        getDataFolder().mkdirs();
        getServer().getPluginManager().registerEvents(this, this);
        for (Player player: getServer().getOnlinePlayers()) {
            setupPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isOptedOut(event.getEntity())) {
            return;
        } else {
            event.setKeepInventory(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setupPlayer(event.getPlayer());
    }

    private void setupPlayer(Player player) {
        if (!supportMiniMap) return;
        final UUID uuid = player.getUniqueId();
        player.setMetadata("MiniMapSettings", new LazyMetadataValue(this, LazyMetadataValue.CacheStrategy.NEVER_CACHE, () -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("Type", "Boolean");
                    map.put("Value", !getOptOuts().contains(uuid));
                    map.put("Priority", 0);
                    map.put("DisplayName", "Keep Inventory on Death");
                    Runnable onUpdate = () -> {
                        final boolean v = map.get("Value") == Boolean.TRUE;
                        if (!v) {
                            getOptOuts().add(uuid);
                        } else {
                            getOptOuts().remove(uuid);
                        }
                        saveOptOuts();
                    };
                    map.put("OnUpdate", onUpdate);
                    List<Map> list = new ArrayList<>();
                    list.add(map);
                    return list;
        }));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (sender instanceof Player) ? (Player)sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length == 0) {
            if (isOptedOut(player)) {
                player.sendMessage((new StringBuilder()).append(ChatColor.RED).append("You lose your inventory when you die.").toString());
            } else {
                player.sendMessage((new StringBuilder()).append(ChatColor.GREEN).append("You keep your inventory when you die.").toString());
            }
        } else if (args.length == 1) {
            String arg = args[0].toLowerCase();
            if (arg.equals("on")) {
                setOptedOut(player, false);
                player.sendMessage((new StringBuilder()).append(ChatColor.GREEN).append("From now on, you will keep your inventory when you die.").toString());
            } else if (arg.equals("off")) {
                setOptedOut(player, true);
                player.sendMessage((new StringBuilder()).append(ChatColor.RED).append("From now on, you will lose your inventory when you die.").toString());
            } else {
                usage(player);
                return true;
            }
        } else {
            usage(player);
            return true;
        }
        return true;
    }

    void usage(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&oUsage: &a/KeepInventory &2[On|Off]"));
    }

    File optOutsFile() {
        return new File(getDataFolder(), "optouts.yml");
    }

    Set<UUID> getOptOuts() {
        if (optOuts == null) {
            optOuts = new HashSet<>();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(optOutsFile());
            Iterator iterator = config.getStringList("optouts").iterator();
            do {
                if (!iterator.hasNext()) break;
                String entry = (String)iterator.next();
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                    break;
                }
                optOuts.add(uuid);
            } while (true);
        }
        return optOuts;
    }

    void saveOptOuts() {
        if (optOuts == null) return;
        YamlConfiguration config = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        UUID uuid;
        for (Iterator iterator = optOuts.iterator(); iterator.hasNext(); list.add(uuid.toString())) {
            uuid = (UUID)iterator.next();
        }
        config.set("optouts", list);
        try {
            config.save(optOutsFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    boolean isOptedOut(Player player) {
        return getOptOuts().contains(player.getUniqueId());
    }

    void setOptedOut(Player player, boolean val) {
        if (isOptedOut(player) == val) return;
        if (val) {
            getOptOuts().add(player.getUniqueId());
        } else {
            getOptOuts().remove(player.getUniqueId());
        }
        saveOptOuts();
    }
}
