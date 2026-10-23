package net.craftavia.pauselinks;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The pause menu data pack must be written before the worlds load, otherwise a change would only
 * show up one restart later. That is why it is generated in the bootstrap phase.
 */
public final class PauseLinksBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(final BootstrapContext context) {
        try {
            final Path dataFolder = context.getDataDirectory();
            final Path configFile = dataFolder.resolve("config.yml");
            if (!Files.exists(configFile)) {
                Files.createDirectories(dataFolder);
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            }
            if (!Files.exists(configFile)) {
                return;
            }

            final FileConfiguration config;
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                config = YamlConfiguration.loadConfiguration(reader);
            }
            final boolean changed = DialogPack.write(worldFolder(dataFolder), LinkService.screenSettings(config),
                    LinkService.parse(config, warning -> context.getLogger().warn(warning)));
            if (changed) {
                context.getLogger().info("Pause menu data pack updated.");
            }
        } catch (final Exception ex) {
            context.getLogger().warn("Could not write the pause menu data pack: {}", ex.toString());
        }
    }

    /** plugins/PauseLinks -> server root -> level-name from server.properties. */
    static Path worldFolder(final Path dataFolder) {
        // getDataDirectory() can be relative; without toAbsolutePath() getParent() may return null.
        final Path serverRoot = dataFolder.toAbsolutePath().getParent().getParent();
        String world = "world";
        final Path properties = serverRoot.resolve("server.properties");
        if (Files.exists(properties)) {
            final Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(properties, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (final IOException ignored) {
                // keep the default "world"
            }
            final String name = props.getProperty("level-name");
            if (name != null && !name.isBlank()) {
                world = name.trim();
            }
        }
        return serverRoot.resolve(world);
    }
}
