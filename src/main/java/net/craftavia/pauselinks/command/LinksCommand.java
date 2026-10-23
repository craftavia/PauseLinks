package net.craftavia.pauselinks.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.craftavia.pauselinks.LinkService;
import net.craftavia.pauselinks.Messages;
import net.craftavia.pauselinks.PauseLinksPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** /links — shows the list in chat; reload/status subcommands for admins. */
public final class LinksCommand implements BasicCommand {

    static final String ADMIN = "pauselinks.admin";

    private final PauseLinksPlugin plugin;

    public LinksCommand(final PauseLinksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final CommandSourceStack source, final String[] args) {
        final CommandSender sender = source.getSender();
        if (args.length > 0) {
            final String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("reload")) {
                if (!sender.hasPermission(ADMIN)) {
                    Messages.send(sender, "no-permission");
                    return;
                }
                plugin.reload(sender);
                return;
            }
            if (sub.equals("status")) {
                if (!sender.hasPermission(ADMIN)) {
                    Messages.send(sender, "no-permission");
                    return;
                }
                Messages.send(sender, "status",
                        Component.text(plugin.links().entries().size()),
                        Component.text(plugin.getPluginMeta().getVersion()));
                return;
            }
        }
        sendList(sender);
    }

    private void sendList(final CommandSender sender) {
        final LinkService service = plugin.links();
        if (!service.chatList() && sender instanceof Player) {
            Messages.send(sender, "list-disabled");
            return;
        }

        final List<LinkService.Entry> entries = service.entries().stream()
                .filter(LinkService.Entry::chat)
                .toList();
        if (entries.isEmpty()) {
            Messages.send(sender, "list-empty");
            return;
        }

        sender.sendMessage(Component.empty());
        Messages.send(sender, "header");
        sender.sendMessage(Component.empty());
        for (final LinkService.Entry entry : entries) {
            Messages.send(sender, "line", ShortcutCommand.clickable(entry.name(), entry),
                    Component.text(entry.description()));
        }
        sender.sendMessage(Component.empty());
        Messages.send(sender, "footer");
        sender.sendMessage(Component.empty());
    }

    @Override
    public @NotNull Collection<String> suggest(final CommandSourceStack source, final String[] args) {
        if (args.length <= 1 && source.getSender().hasPermission(ADMIN)) {
            final String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "status").stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "pauselinks.use";
    }
}
