package net.lumamc.nexo;

import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.glyphs.Glyph;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class EmojiListener implements Listener {

    private static final Pattern GLYPH_TOKEN = Pattern.compile(":[a-zA-Z0-9_-]+:");
    private final Map<UUID, Map<String, Component>> replacementsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> completionsByPlayer = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Map<String, Component> repl = replacementsByPlayer.get(id);
        if (repl == null || repl.isEmpty()) return;

        String msg = event.getMessage(); // e.g. "/sc hello :sob:"
        String newMsg = replaceEmojiTokensInString(msg, repl);

        if (!newMsg.equals(msg)) {
            event.setMessage(newMsg);
        }
    }

    private static String replaceEmojiTokensInString(String input, Map<String, Component> repl) {
        if (input.indexOf(':') == -1) return input; // no emoji present
        var m = GLYPH_TOKEN.matcher(input);
        if (!m.find()) return input;

        StringBuilder out = new StringBuilder();
        do {
            String token = m.group();
            Component c = repl.get(token);
            String replacement = (c instanceof TextComponent tc) ? tc.content() : token;
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(replacement));
        } while (m.find());

        m.appendTail(out);
        return out.toString();
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        final Map<String, Component> repl = replacementsByPlayer.get(event.getPlayer().getUniqueId());
        if (repl == null || repl.isEmpty()) return;

        Component newMsg = event.message().replaceText(TextReplacementConfig.builder()
                .match(GLYPH_TOKEN)
                .replacement((matchResult, builder) -> {
                    String token = matchResult.group();
                    Component c = repl.get(token);
                    return (c != null) ? c : Component.text(token);
                })
                .build()
        );

        event.message(newMsg);
    }

    public void rebuildFor(Player player) {
        List<Glyph> allowed = getAllowedGlyphs(player);

        Map<String, Component> replyMap = new HashMap<>(allowed.size());
        Set<String> newCompletions = new HashSet<>(allowed.size());

        for (Glyph glyph : allowed) {
            String token = ":" + glyph.getId() + ":";
            replyMap.put(token, Component.text(glyph.getGlyphTag()));
            newCompletions.add(token);
        }

        Set<String> old = completionsByPlayer.get(player.getUniqueId());
        if (old != null && !old.isEmpty()) {
            player.removeCustomChatCompletions(old);
        }
        if (!newCompletions.isEmpty()) {
            player.addCustomChatCompletions(newCompletions);
        }

        replacementsByPlayer.put(player.getUniqueId(), replyMap);
        completionsByPlayer.put(player.getUniqueId(), newCompletions);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        rebuildFor(event.getPlayer());
    }

    @EventHandler
    public void onNexoReload(NexoItemsLoadedEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            rebuildFor(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        Set<String> old = completionsByPlayer.remove(id);
        if (old != null && !old.isEmpty()) {
            player.removeCustomChatCompletions(old);
        }
        replacementsByPlayer.remove(id);
    }

    public static List<Glyph> getAllowedGlyphs(Player player) {
        return NexoPlugin.instance().getFontManager$core().glyphs()
                .stream()
                .filter(Glyph::isEmoji)
                .filter(glyph -> player.hasPermission(glyph.getPermission()))
                .toList();
    }

}
