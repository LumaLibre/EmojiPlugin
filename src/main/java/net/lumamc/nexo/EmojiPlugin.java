package net.lumamc.nexo;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class EmojiPlugin extends JavaPlugin {

    private static EmojiPlugin instance;
    private EmojiListener emojiListener;
    private PacketAdapter protocolLibListener;

    @Override public void onEnable() {
        instance = this;
        emojiListener = new EmojiListener(!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"));
        Bukkit.getPluginManager().registerEvents(emojiListener, this);
        hookProtocolLibIfPresent();
        hookLuckPermsIfPresent();
    }

    @Override public void onDisable() {
        protocolLibListener = null;
        emojiListener = null;
        instance = null;
    }

    public static EmojiPlugin getInstance() {
        return instance;
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

    private void hookProtocolLibIfPresent() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().info("ProtocolLib not found - skipping packet-level chat hook.");
            return;
        }

        protocolLibListener = emojiListener.createProtocolLibListener();
        ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener);

        getLogger().info("Hooked ProtocolLib packet-level chat.");
    }

}
