package net.craftavia.pauselinks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogPackTest {

    private static final DialogPack.Settings ON = new DialogPack.Settings(true, "Button", "Title",
            List.of("<gray>info</gray>"), 2, 150, true, "Close");

    @Test
    void writesValidPackAndIsIdempotent(@TempDir final Path world) throws Exception {
        final List<LinkService.Entry> list = List.of(
                new LinkService.Entry("website", null, "<aqua>Website</aqua>", "Home page",
                        URI.create("https://example.com"), null, true, true),
                new LinkService.Entry("cmd", null, "Message", "", null, "/msg <player> hi", true, true));

        assertTrue(DialogPack.write(world, ON, list));
        assertFalse(DialogPack.write(world, ON, list), "same content must not be rewritten");

        final Path pack = world.resolve("datapacks/pauselinks");
        final JsonObject dialog = JsonParser.parseString(
                Files.readString(pack.resolve("data/pauselinks/dialog/menu.json"))).getAsJsonObject();
        assertEquals("minecraft:multi_action", dialog.get("type").getAsString());
        assertEquals(2, dialog.getAsJsonArray("actions").size());
        final JsonObject cmd = dialog.getAsJsonArray("actions").get(1).getAsJsonObject();
        assertEquals("/msg <player> hi", cmd.getAsJsonObject("action").get("command").getAsString());
        // "<player>" must stay visible in the tooltip, not be eaten as a MiniMessage tag.
        assertTrue(cmd.get("tooltip").toString().contains("/msg <player> hi"), cmd.get("tooltip").toString());

        final JsonObject meta = JsonParser.parseString(Files.readString(pack.resolve("pack.mcmeta")))
                .getAsJsonObject().getAsJsonObject("pack");
        assertEquals(80, meta.get("pack_format").getAsInt());
        assertEquals(80, meta.get("min_format").getAsInt());

        final JsonObject tag = JsonParser.parseString(Files.readString(
                pack.resolve("data/minecraft/tags/dialog/pause_screen_additions.json"))).getAsJsonObject();
        assertEquals("pauselinks:menu", tag.getAsJsonArray("values").get(0).getAsString());

        final DialogPack.Settings off = new DialogPack.Settings(false, "", "", List.of(), 2, 150, false, "");
        assertTrue(DialogPack.write(world, off, list));
        assertFalse(Files.exists(pack));
    }

    @Test
    void nameIsParsedOnce() {
        final LinkService.Entry e = new LinkService.Entry("website", null, "<aqua>Web</aqua>", "",
                URI.create("https://example.com"), null, true, true);
        assertSame(e.name(), e.name());
        assertEquals("Web", PlainTextComponentSerializer.plainText().serialize(e.name()));
    }

    @Test
    void regionalClientFallsBackToLanguageFile() {
        final MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key("pauselinks", "test"));
        store.defaultLocale(Locale.ENGLISH);
        store.registerAll(Locale.ENGLISH, Map.of("pauselinks.k", "hello"));
        store.registerAll(Locale.of("tr"), Map.of("pauselinks.k", "merhaba"));

        final Component tr = store.translate(Component.translatable("pauselinks.k"), Locale.of("tr", "TR"));
        final Component de = store.translate(Component.translatable("pauselinks.k"), Locale.of("de", "DE"));
        assertNotNull(tr);
        assertNotNull(de);
        assertEquals("merhaba", PlainTextComponentSerializer.plainText().serialize(tr));
        assertEquals("hello", PlainTextComponentSerializer.plainText().serialize(de));
    }

    @Test
    void defaultLanguagePicksClosestFile() {
        final Map<Locale, Map<String, String>> loaded = new LinkedHashMap<>();
        loaded.put(Locale.of("tr"), Map.of());
        loaded.put(Locale.ENGLISH, Map.of());
        assertEquals(Locale.of("tr"), Messages.pickDefault(loaded, Locale.of("tr", "TR")));
        assertEquals(Locale.ENGLISH, Messages.pickDefault(loaded, Locale.of("fr")));
    }
}
