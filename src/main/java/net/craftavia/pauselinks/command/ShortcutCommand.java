package net.craftavia.pauselinks.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.craftavia.pauselinks.LinkService;
import net.craftavia.pauselinks.Messages;
import net.craftavia.pauselinks.PauseLinksPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

/** /discord, /website, ... — shows a single link from config.yml as a clickable chat line. */
public final class ShortcutCommand implements BasicCommand {

    private final PauseLinksPlugin plugin;
    private final String linkId;

    public ShortcutCommand(final PauseLinksPlugin plugin, final String linkId) {
        this.plugin = plugin;
        this.linkId = linkId;
    }

    @Override
    public void execute(final CommandSourceStack source, final String[] args) {
        final CommandSender sender = source.getSender();
        final LinkService.Entry entry = plugin.links().entries().stream()
                .filter(e -> e.id().equalsIgnoreCase(linkId))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            Messages.send(sender, "single-missing");
            return;
        }

        final Component address = Component.text(entry.target(), NamedTextColor.WHITE, TextDecoration.UNDERLINED);
        Messages.send(sender, "single", clickable(entry.name(), entry), clickable(address, entry),
                Component.text(entry.description()));
    }

    /** Makes a component open the link (or run the command) on click, with a translated hover text. */
    static Component clickable(final Component text, final LinkService.Entry entry) {
        final String target = entry.target();
        final ClickEvent click = entry.isUrl() ? ClickEvent.openUrl(target) : ClickEvent.runCommand(target);
        final Component hover = Messages.msg(entry.isUrl() ? "hover-url" : "hover-command", Component.text(target));
        return text.clickEvent(click).hoverEvent(HoverEvent.showText(hover));
    }

    @Override
    public String permission() {
        return "pauselinks.use";
    }
}
