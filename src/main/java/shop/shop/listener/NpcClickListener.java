package shop.shop.listener;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import shop.shop.manager.ShopManager;
import shop.shop.registry.ShopNpcRegistry;

public class NpcClickListener implements Listener {

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        Npc npc = event.getNpc(); // ✅ Npc 객체 가져오기

        String npcIdentifier = npc.getData().getId(); // ✅ 식별자 기반으로
        String shopName = ShopNpcRegistry.getShopName(npcIdentifier); // ✅ 해당 ID로 상점 검색

        if (shopName != null) {
            Bukkit.getScheduler().runTask(shop.shop.ShopPlugin.getInstance(), () -> {
                ShopManager.openShop(player, new String[]{"열기", shopName});
            });
        }
    }
}
