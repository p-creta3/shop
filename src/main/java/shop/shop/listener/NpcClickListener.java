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

        String npcIdentifier = npc.getData().getId(); // 고유 식별자
        String shopName = ShopNpcRegistry.getShopName(npcIdentifier); // 등록된 상점 이름 조회

        if (shopName != null) {
            // 상점이 연결된 경우 열기
            Bukkit.getLogger().info("§a[NPC 상점] '" + npc.getData().getDisplayName() + "' NPC와 연결된 상점: " + shopName);
            Bukkit.getScheduler().runTask(shop.shop.ShopPlugin.getInstance(), () -> {
                ShopManager.openShop(player, new String[]{"열기", shopName});
            });
        } else {
            // 연결되지 않은 NPC 클릭 시 무반응 또는 안내
            Bukkit.getLogger().info("§c[NPC 상점] '" + npc.getData().getDisplayName() + "' 은(는) 상점에 연결되어 있지 않습니다.");
            // 원한다면 플레이어에게도 안내 가능
            // player.sendMessage("§c이 NPC는 상점에 연결되어 있지 않습니다.");
        }
    }
}
