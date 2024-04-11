package com.winthier.keepinventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class OptOuts {
    protected Set<UUID> players = new HashSet<>();

    public boolean get(Player player) {
        return players.contains(player.getUniqueId());
    }

    public void add(Player player) {
        if (get(player)) return;
        players.add(player.getUniqueId());
    }

    public void remove(Player player) {
        if (!get(player)) return;
        players.remove(player.getUniqueId());
    }

    public void clear() {
        players.clear();
    }
}
