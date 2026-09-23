package io.github.sefiraat.networks.listeners;

import com.ytdd9527.networksexpansion.utils.itemstacks.ItemStackUtil;
import io.github.sefiraat.networks.Networks;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Refreshes outdated player heads (legacy {@code SkullOwner} format from before 1.20.5) in
 * any GUI of this addon whenever a player opens it.
 */
public class MenuSkullRefreshListener implements Listener {

    @EventHandler
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlockMenu blockMenu)) {
            return;
        }

        final SlimefunItem slimefunItem = blockMenu.getPreset().getSlimefunItem();
        if (slimefunItem == null || slimefunItem.getAddon() != Networks.getInstance()) {
            return;
        }

        ItemStackUtil.refreshOutdatedSkulls(blockMenu);
    }
}
