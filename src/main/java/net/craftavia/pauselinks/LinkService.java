package net.craftavia.pauselinks;

import net.kyori.adventure.text.Component;
import org.bukkit.ServerLinks;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads the links from config.yml and writes them to the server's link list.
 * The list is sent to a player while connecting (configuration phase).
 */
public final class LinkService {

    /**
     * One link: either opens an address (url) or runs an in-game command.
     * When type is null the custom name is shown; otherwise the client shows its own translation.
     */
    public record Entry(String id, ServerLinks.Type type, String nameTemplate, String description,
                        URI url, String command, boolean pauseMenu, boolean chat, Component nameComponent) {

        public Entry(final String id, final ServerLinks.Type type, final String nameTemplate, final String description,
                     final URI url, final String command, final boolean pauseMenu, final boolean chat) {
            // Parse the MiniMessage name once instead of on every join and every /links.
            this(id, type, nameTemplate, description, url, command, pauseMenu, chat, Messages.mini(nameTemplate));
        }

        public Component name() {
            return nameComponent;
        }

        /** Command entries cannot be put in Minecraft's "Server Links..." list. */
        public boolean isUrl() {
            return url != null;
        }

        public String target() {
            return isUrl() ? url.toString() : command;
        }
    }

    /** List and flags in one object so a reload never exposes a half-updated state to other threads. */
    private record State(List<Entry> entries, boolean serverLinks, boolean chatList) {
    }

    private final PauseLinksPlugin plugin;
    private volatile State state = new State(List.of(), true, true);

    public LinkService(final PauseLinksPlugin plugin) {
        this.plugin = plugin;
    }

    public List<Entry> entries() {
        return state.entries();
    }

    public boolean chatList() {
        return state.chatList();
    }

    /** Loads config.yml. Called on the main thread at startup and on /links reload. */
    public int load(final FileConfiguration config) {
        final State loaded = new State(parse(config, warning -> plugin.getSLF4JLogger().warn(warning)),
                config.getBoolean("server-links", true), config.getBoolean("chat-list", true));
        state = loaded;
        return loaded.entries().size();
    }

    /** Parses the links. Also used during bootstrap, so it must not touch the Bukkit server. */
    public static List<Entry> parse(final FileConfiguration config, final Consumer<String> warn) {
        final ConfigurationSection root = config.getConfigurationSection("links");
        if (root == null) {
            warn.accept("config.yml has no 'links' section, no links will be sent.");
            return List.of();
        }

        final List<Entry> loaded = new ArrayList<>();
        for (final String id : root.getKeys(false)) {
            final ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null || !sec.getBoolean("enabled", true)) {
                continue;
            }

            final String command = cleanCommand(sec.getString("command", ""));
            final String rawUrl = sec.getString("url", "").trim();
            URI url = null;
            if (command == null) {
                url = parseUrl(rawUrl);
                if (url == null) {
                    warn.accept("Link '" + id + "' has no valid url or command (" + rawUrl
                            + "). URLs must start with http:// or https://, skipping it.");
                    continue;
                }
            }

            final String typeName = sec.getString("type", "").trim();
            ServerLinks.Type type = null;
            if (!typeName.isEmpty() && command == null) {
                try {
                    type = ServerLinks.Type.valueOf(typeName.toUpperCase(Locale.ROOT));
                } catch (final IllegalArgumentException ex) {
                    warn.accept("Unknown type '" + typeName + "' on link '" + id + "', using the custom name.");
                }
            }

            loaded.add(new Entry(id, type, sec.getString("name", id), sec.getString("description", ""), url, command,
                    sec.getBoolean("pause-menu", true), sec.getBoolean("chat", true)));
        }
        return List.copyOf(loaded);
    }

    /** Settings of the pause menu screen. */
    public static DialogPack.Settings screenSettings(final FileConfiguration config) {
        final ConfigurationSection sec = config.getConfigurationSection("pause-button");
        if (sec == null) {
            return new DialogPack.Settings(false, "", "", List.of(), 2, 150, false, "");
        }

        final List<String> description = new ArrayList<>();
        if (sec.isList("description")) {
            description.addAll(sec.getStringList("description"));
        } else {
            final String single = sec.getString("description", "");
            if (!single.isBlank()) {
                description.add(single);
            }
        }

        return new DialogPack.Settings(
                sec.getBoolean("enabled", true),
                sec.getString("button", "Server Links..."),
                sec.getString("title", "Server Links"),
                List.copyOf(description),
                sec.getInt("columns", 2),
                sec.getInt("button-width", 150),
                sec.getBoolean("show-address", false),
                sec.getString("close", "Close"));
    }

    /** Updates the server-wide list (everyone who connects from now on gets it). */
    public void applyGlobal() {
        apply(plugin.getServer().getServerLinks());
    }

    /** Clears the given list and writes the configured links into it. */
    public void apply(final ServerLinks links) {
        final State current = state;
        clear(links);
        if (!current.serverLinks()) {
            return;
        }
        for (final Entry entry : current.entries()) {
            if (!entry.pauseMenu() || !entry.isUrl()) {
                continue;
            }
            if (entry.type() != null) {
                links.addLink(entry.type(), entry.url());
            } else {
                links.addLink(entry.name(), entry.url());
            }
        }
    }

    /** Empties the server's list on shutdown so no stale links stay behind. */
    public void clearGlobal() {
        clear(plugin.getServer().getServerLinks());
    }

    private static void clear(final ServerLinks links) {
        for (final ServerLinks.ServerLink existing : links.getLinks()) {
            links.removeLink(existing);
        }
    }

    /** "/spawn" or "spawn" -> "/spawn"; null when empty. */
    private static String cleanCommand(final String raw) {
        final String command = raw == null ? "" : raw.trim();
        if (command.isEmpty()) {
            return null;
        }
        return command.startsWith("/") ? command : "/" + command;
    }

    private static URI parseUrl(final String raw) {
        final String lower = raw.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null;
        }
        try {
            return new URI(raw);
        } catch (final URISyntaxException ex) {
            return null;
        }
    }
}
