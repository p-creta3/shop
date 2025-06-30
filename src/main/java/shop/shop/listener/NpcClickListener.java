package shop.shop.listener;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import shop.shop.manager.ShopManager;
import shop.shop.registry.ShopNpcRegistry;

import java.util.UUID;

public class NpcClickListener implements Listener {

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        UUID npcUUID = player.getUniqueId();

        String shopName = ShopNpcRegistry.getShopName(npcUUID);
        if (shopName != null) {
            Bukkit.getScheduler().runTask(shop.shop.ShopPlugin.getInstance(), () -> {
                ShopManager.openShop(player, new String[]{"열기", shopName});
            });
        }
    }

}
