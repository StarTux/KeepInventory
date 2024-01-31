package com.winthier.keepinventory;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class KeepInventoryPlugin extends JavaPlugin implements Listener {
    private OptOuts optOuts = null;
    private Gson gson = new Gson();

    @Override
    public void onEnable() {
        optOuts = loadOptOuts();
        Bukkit.getPluginManager().registerEvents(this, this);
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
            player.sendMessage(isOptedOut(player)
                               ? Component.text("You lose your inventory when you die", NamedTextColor.RED)
                               : Component.text("You keep your inventory when you die", NamedTextColor.GREEN));
            return true;
        }
        switch (args[0]) {
        case "on":
            setOptedOut(player, false);
            player.sendMessage(Component.text("You will keep your inventory when you die", NamedTextColor.GREEN));
            return true;
        case "off":
            setOptedOut(player, true);
            player.sendMessage(Component.text("You will lose your inventory when you die", NamedTextColor.RED));
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

    void usage(Player player) {
        player.sendMessage(Component.text()
                           .append(Component.text("Usage: ", NamedTextColor.GRAY))
                           .append(Component.text("/keepinventory ", NamedTextColor.YELLOW))
                           .append(Component.text("[on|off]", NamedTextColor.GOLD, TextDecoration.ITALIC))
                           .build());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY)) return;
        if (isOptedOut(player)) return;
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

    public OptOuts getOptOuts() {
        if (optOuts == null) {
            optOuts = loadOptOuts();
        }
        return optOuts;
    }

    public boolean isOptedOut(Player player) {
        return getOptOuts().players.containsKey(player.getUniqueId());
    }

    public void setOptedOut(Player player, boolean val) {
        if (isOptedOut(player) == val) return;
        if (val) {
            getOptOuts().players.put(player.getUniqueId(), player.getName());
        } else {
            getOptOuts().players.remove(player.getUniqueId());
        }
        saveOptOuts();
    }

    private File getOptOutsFile() {
        return new File(getDataFolder(), "optouts.json");
    }

    private OptOuts loadOptOuts() {
        try (FileReader fr = new FileReader(getOptOutsFile())) {
            OptOuts result = gson.fromJson(fr, OptOuts.class);
            return result != null ? result : new OptOuts();
        } catch (FileNotFoundException fnfr) {
            return new OptOuts();
        } catch (IOException ioe) {
            throw new IllegalStateException("Loading OptOuts", ioe);
        }
    }

    private void saveOptOuts() {
        if (optOuts == null) return;
        getDataFolder().mkdirs();
        try (FileWriter fw = new FileWriter(getOptOutsFile())) {
            gson.toJson(optOuts, fw);
        } catch (IOException ioe) {
            throw new IllegalStateException("Saving OptOuts", ioe);
        }
    }
}
