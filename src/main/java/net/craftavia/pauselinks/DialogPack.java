package net.craftavia.pauselinks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Writes the data pack that adds our own button to the pause (Escape) menu.
 * The text of Minecraft's "Server Links..." button comes from the client and cannot be changed,
 * so we add a dialog to the #minecraft:pause_screen_additions tag instead.
 * With a single dialog in that tag, the button text is the dialog's external_title.
 */
public final class DialogPack {

    public static final String FOLDER = "pauselinks";
    private static final String NAMESPACE = "pauselinks";
    private static final String DIALOG_ID = "menu";
    /** Data pack format of 1.21.6, the first version with dialogs. */
    private static final int MIN_FORMAT = 80;
    private static final int MAX_FORMAT = 999;
    private static final GsonComponentSerializer COMPONENTS = GsonComponentSerializer.gson();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public record Settings(boolean enabled, String button, String title, List<String> description, int columns,
                           int buttonWidth, boolean showAddress, String close) {
    }

    private DialogPack() {
    }

    /**
     * Writes the pack to disk when its content changed. Deletes the pack folder when disabled.
     *
     * @return true if files changed (a server restart is needed to apply them)
     */
    public static boolean write(final Path worldFolder, final Settings settings, final List<LinkService.Entry> links)
            throws IOException {
        final Path root = worldFolder.resolve("datapacks").resolve(FOLDER);
        final boolean anyButton = links.stream().anyMatch(LinkService.Entry::pauseMenu);
        if (!settings.enabled() || !anyButton) {
            return delete(root);
        }

        final Map<Path, String> files = new LinkedHashMap<>();
        files.put(root.resolve("pack.mcmeta"), GSON.toJson(mcmeta()) + "\n");
        files.put(root.resolve("data").resolve(NAMESPACE).resolve("dialog").resolve(DIALOG_ID + ".json"),
                GSON.toJson(dialog(settings, links)) + "\n");
        files.put(root.resolve("data").resolve("minecraft").resolve("tags").resolve("dialog")
                .resolve("pause_screen_additions.json"), GSON.toJson(tag()) + "\n");

        boolean changed = false;
        for (final Map.Entry<Path, String> file : files.entrySet()) {
            if (writeIfChanged(file.getKey(), file.getValue())) {
                changed = true;
            }
        }
        return changed;
    }

    private static boolean delete(final Path root) throws IOException {
        if (!Files.exists(root)) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            final List<Path> paths = walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (final Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
        return true;
    }

    private static boolean writeIfChanged(final Path path, final String content) throws IOException {
        if (Files.exists(path) && Files.readString(path, StandardCharsets.UTF_8).equals(content)) {
            return false;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Loads on 1.21.6-1.21.8 (pack_format + supported_formats) and on 1.21.9+ (min_format/max_format;
     * pack_format is still required there because the range starts below format 82).
     */
    private static JsonObject mcmeta() {
        final JsonObject supported = new JsonObject();
        supported.addProperty("min_inclusive", MIN_FORMAT);
        supported.addProperty("max_inclusive", MAX_FORMAT);

        final JsonObject pack = new JsonObject();
        pack.addProperty("description", "PauseLinks pause menu button");
        pack.addProperty("pack_format", MIN_FORMAT);
        pack.add("supported_formats", supported);
        pack.addProperty("min_format", MIN_FORMAT);
        pack.addProperty("max_format", MAX_FORMAT);

        final JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private static JsonObject tag() {
        final JsonArray values = new JsonArray();
        values.add(NAMESPACE + ":" + DIALOG_ID);
        final JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        root.add("values", values);
        return root;
    }

    private static JsonObject dialog(final Settings settings, final List<LinkService.Entry> links) {
        final int width = Math.clamp(settings.buttonWidth(), 60, 400);

        final JsonArray actions = new JsonArray();
        for (final LinkService.Entry entry : links) {
            if (!entry.pauseMenu()) {
                continue;
            }
            final JsonObject button = new JsonObject();
            button.add("label", json(entry.name()));
            final Component tooltip = tooltip(entry, settings.showAddress());
            if (tooltip != null) {
                button.add("tooltip", json(tooltip));
            }
            button.addProperty("width", width);
            button.add("action", action(entry));
            actions.add(button);
        }

        final JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:multi_action");
        root.add("title", json(Messages.mini(settings.title())));
        root.add("external_title", json(Messages.mini(settings.button())));
        root.addProperty("can_close_with_escape", true);
        root.addProperty("pause", false);
        root.addProperty("after_action", "none");

        if (!settings.description().isEmpty()) {
            final JsonArray body = new JsonArray();
            for (final String line : settings.description()) {
                final JsonObject message = new JsonObject();
                message.addProperty("type", "minecraft:plain_message");
                message.addProperty("width", 340);
                message.add("contents", json(Messages.mini(line)));
                body.add(message);
            }
            root.add("body", body);
        }

        root.addProperty("columns", Math.max(1, settings.columns()));
        final JsonObject exit = new JsonObject();
        exit.add("label", json(Messages.mini(settings.close())));
        exit.addProperty("width", width);
        root.add("exit_action", exit);
        root.add("actions", actions);
        return root;
    }

    /** What the button does: run a command or open an address. */
    private static JsonObject action(final LinkService.Entry entry) {
        final JsonObject action = new JsonObject();
        if (entry.isUrl()) {
            action.addProperty("type", "open_url");
            action.addProperty("url", entry.url().toString());
        } else {
            action.addProperty("type", "run_command");
            action.addProperty("command", entry.command());
        }
        return action;
    }

    /** Button tooltip: the description and, if enabled, the target itself. */
    private static Component tooltip(final LinkService.Entry entry, final boolean showAddress) {
        final Component description = entry.description().isBlank() ? null : Messages.mini(entry.description());
        if (!showAddress) {
            return description;
        }
        // Plain text: a "<" in a command must not be parsed as a MiniMessage tag.
        final Component address = Component.text(entry.target(), NamedTextColor.DARK_GRAY);
        if (description == null) {
            return address;
        }
        return description.append(Component.newline()).append(address);
    }

    private static JsonElement json(final Component component) {
        return COMPONENTS.serializeToTree(component);
    }
}
