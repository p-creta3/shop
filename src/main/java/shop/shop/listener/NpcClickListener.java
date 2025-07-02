package shop.shop.listener;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import shop.shop.ShopPlugin;
import shop.shop.manager.ShopManager;
import shop.shop.registry.ShopNpcRegistry;
import org.bukkit.ChatColor;

public class NpcClickListener implements Listener {

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        Npc npc = event.getNpc();

        String npcDisplayName = npc.getData().getDisplayName();
        // 색상 코드를 제거한 이름으로 조회
        String cleanName = ChatColor.stripColor(npcDisplayName);
        String shopName = ShopNpcRegistry.getShopName(cleanName);

        if (shopName != null) {
            Bukkit.getLogger().info("§a[NPC 상점] '" + npcDisplayName + "' NPC와 연결된 상점: " + shopName);
            Bukkit.getScheduler().runTask(ShopPlugin.getInstance(), () -> {
                ShopManager.openShop(player, new String[]{"열기", shopName});
            });
        } else {
            Bukkit.getLogger().info("§c[NPC 상점] '" + npcDisplayName + "' 은(는) 상점에 연결되어 있지 않습니다.");
            // player.sendMessage("§c이 NPC는 상점에 연결되어 있지 않습니다.");
        }
    }
}

