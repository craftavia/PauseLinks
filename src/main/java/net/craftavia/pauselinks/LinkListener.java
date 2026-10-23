package net.craftavia.pauselinks;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLinksSendEvent;

/**
 * Writes the configured list into the links sent to a connecting player,
 * so players joining right after /links reload get the new list.
 */
public final class LinkListener implements Listener {

    private final LinkService links;

    public LinkListener(final LinkService links) {
        this.links = links;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLinksSend(final PlayerLinksSendEvent event) {
        links.apply(event.getLinks());
    }
}
