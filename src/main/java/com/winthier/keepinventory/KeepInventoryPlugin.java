package com.winthier.keepinventory;

import com.winthier.connect.Redis;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.event.ClickEvent.suggestCommand;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public final class KeepInventoryPlugin extends JavaPlugin implements Listener {
    private final OptOuts optOuts = new OptOuts();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadOptOut(player, () -> { });
        }
    }

    @Override
    public void onDisable() {
        optOuts.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (sender instanceof Player) ? (Player) sender : null;
        if (player == null) {
            sender.sendMessage("Player expected");
            return true;
        }
        if (args.length > 1) return false;
        if (args.length == 0) {
            notify(player);
            return true;
        }
        switch (args[0]) {
        case "on":
            optOuts.remove(player);
            storeOptOut(player, false, () -> notify(player));
            return true;
        case "off":
            optOuts.add(player);
            storeOptOut(player, true, () -> notify(player));
            return true;
        default:
            usage(player);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1
            ? Stream.of("on", "off").filter(a -> a.contains(args[0])).collect(Collectors.toList())
            : Collections.emptyList();
    }

    private void notify(Player player) {
        final var message = optOuts.get(player)
            ? text("You lose your inventory when you die", RED)
            : text("You keep your inventory when you die", GREEN);
        player.sendMessage(message
                           .hoverEvent(textOfChildren(text("/keepinventory", GRAY),
                                                      text(" [on|off]", GRAY, ITALIC)))
                           .clickEvent(suggestCommand("/keepinventory "))
                           .insertion("/keepinventory "));
    }

    private void usage(Player player) {
        player.sendMessage(textOfChildren(text("Usage: ", GRAY),
                                          text("/keepinventory ", YELLOW),
                                          text("[on|off]", GOLD, ITALIC))
                           .hoverEvent(textOfChildren(text("/keepinventory", GRAY),
                                                      text(" [on|off]", GRAY, ITALIC)))
                           .clickEvent(suggestCommand("/keepinventory "))
                           .insertion("/keepinventory "));
    }

    private static String getPlayerKey(Player player) {
        return "KeepInventory." + player.getUniqueId();
    }

    private void loadOptOut(Player player, Runnable callback) {
        final String key = getPlayerKey(player);
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                final boolean doOptOut = Redis.get(key) != null;
                Bukkit.getScheduler().runTask(this, () -> {
                        if (!player.isOnline()) return;
                        if (doOptOut) {
                            optOuts.add(player);
                            getLogger().info(player.getName() + " is opted out");
                        } else {
                            optOuts.remove(player);
                        }
                        if (callback != null) callback.run();
                    });
            });
    }

    private void storeOptOut(Player player, boolean doOptOut, Runnable callback) {
        final String key = getPlayerKey(player);
        final String value = player.getName();
        final int durationSeconds = 60 * 60 * 24; // 1 day
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                if (doOptOut) {
                    Redis.set(key, value, durationSeconds);
                } else {
                    Redis.del(key);
                }
                Bukkit.getScheduler().runTask(this, () -> {
                        if (!player.isOnline()) return;
                        if (callback != null) callback.run();
                    });
            });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        loadOptOut(player, () -> {
                if (!optOuts.get(player)) return;
                notify(player);
            });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        optOuts.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)) return;
        if (optOuts.get(player)) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        switch (player.getWorld().getDifficulty()) {
        case PEACEFUL: case EASY:
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            break;
        case NORMAL:
            event.setNewLevel(Math.max(0, player.getLevel() - 1));
            event.setNewExp(0);
            break;
        case HARD: default: break;
        }
    }
}
