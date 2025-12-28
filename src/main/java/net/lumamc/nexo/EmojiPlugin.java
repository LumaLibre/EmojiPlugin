package net.lumamc.nexo;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class EmojiPlugin extends JavaPlugin {

    private EmojiListener emojiListener = new EmojiListener();

    @Override public void onEnable() {
        emojiListener = new EmojiListener();
        Bukkit.getPluginManager().registerEvents(emojiListener, this);
        hookLuckPermsIfPresent();
    }

    @Override public void onDisable() {
        emojiListener = null;
    }

    private void hookLuckPermsIfPresent() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            getLogger().warning("LuckPerms not found - skipping permission-change hook.");
            return;
        }

        final LuckPerms lp;
        try {
            lp = LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            getLogger().warning("LuckPerms seems enabled but API isn't ready - skipping hook.");
            return;
        }

        lp.getEventBus().subscribe(this, UserDataRecalculateEvent.class, (event) -> {
            UUID uuid = event.getUser().getUniqueId();
            Bukkit.getScheduler().runTask(this, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    emojiListener.rebuildFor(player);
                }
            });
        });
        getLogger().info("Hooked LuckPerms permission-change events.");
    }
}
