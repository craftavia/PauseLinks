package net.craftavia.pauselinks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.craftavia.pauselinks.command.LinksCommand;
import net.craftavia.pauselinks.command.ShortcutCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.Translator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PauseLinksPlugin extends JavaPlugin {

    private final AtomicBoolean reloading = new AtomicBoolean();
    private Messages messages;
    private LinkService links;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        messages.saveDefaults();
        try {
            messages.apply(messages.read(), defaultLocale(getConfig()));
        } catch (final Exception ex) {
            getSLF4JLogger().error("Could not read the message files", ex);
        }

        links = new LinkService(this);
        final int count = links.load(getConfig());
        links.applyGlobal();

        getServer().getPluginManager().registerEvents(new LinkListener(links), this);
        final ConfigurationSection shortcuts = getConfig().getConfigurationSection("shortcut-commands");
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register("links", "Shows the server links",
                    List.of("serverlinks", "pauselinks"), new LinksCommand(this));
            if (shortcuts == null) {
                return;
            }
            for (final String name : shortcuts.getKeys(false)) {
                final ConfigurationSection sec = shortcuts.getConfigurationSection(name);
                if (sec == null) {
                    continue;
                }
                final String linkId = sec.getString("link", name);
                event.registrar().register(name.toLowerCase(Locale.ROOT), "Shows the " + linkId + " link",
                        sec.getStringList("aliases"), new ShortcutCommand(this, linkId));
            }
        });

        getSLF4JLogger().info("Loaded {} links.", count);
    }

    @Override
    public void onDisable() {
        if (links != null) {
            links.clearGlobal();
        }
        if (messages != null) {
            messages.unregister();
        }
    }

    public LinkService links() {
        return links;
    }

    private static Locale defaultLocale(final FileConfiguration config) {
        final Locale locale = Translator.parseLocale(config.getString("default-language", "en"));
        return locale == null ? Locale.ENGLISH : locale;
    }

    /** Reads the files off the main thread, then applies them on it and re-sends the list to online players. */
    public void reload(final CommandSender sender) {
        // Two reloads must not run reloadConfig/file writes at the same time; the second one is ignored.
        if (!reloading.compareAndSet(false, true)) {
            return;
        }
        getServer().getAsyncScheduler().runNow(this, task -> {
            final Map<Locale, Map<String, String>> newMessages;
            try {
                reloadConfig();
                newMessages = messages.read();
            } catch (final Exception ex) {
                reloading.set(false);
                getSLF4JLogger().error("Could not read the configuration", ex);
                getServer().getGlobalRegionScheduler().run(this, t -> Messages.send(sender, "reload-failed"));
                return;
            }

            boolean packChanged = false;
            try {
                packChanged = DialogPack.write(
                        PauseLinksBootstrap.worldFolder(getDataFolder().toPath()),
                        LinkService.screenSettings(getConfig()),
                        LinkService.parse(getConfig(), warning -> getSLF4JLogger().warn(warning)));
            } catch (final Exception ex) {
                getSLF4JLogger().error("Could not write the pause menu data pack", ex);
            }

            final boolean notifyPack = packChanged;
            getServer().getGlobalRegionScheduler().run(this, t -> {
                reloading.set(false);
                messages.apply(newMessages, defaultLocale(getConfig()));
                final int count = links.load(getConfig());
                links.applyGlobal();
                // Send the new list to online players too, so they do not need to reconnect.
                for (final Player player : getServer().getOnlinePlayers()) {
                    player.sendLinks(getServer().getServerLinks());
                }
                sender.sendMessage(Messages.msg("reloaded", Component.text(count)));
                if (notifyPack) {
                    Messages.send(sender, "pack-updated");
                }
            });
        });
    }
}
