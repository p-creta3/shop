package shop.shop.listener;

import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import shop.shop.registry.ShopNpcRegistry;

import java.util.Optional;

public class NpcCreateListener implements Listener {

    @EventHandler
    public void onNpcCreateCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        Player player = e.getPlayer();

        // 예: /npc create 상점이름
        if (msg.toLowerCase().startsWith("/npc create ")) {
            String[] parts = msg.split(" ");
            if (parts.length >= 3) {
                String npcName = parts[2];

                // 1틱 후 NPC 생성 완료되었을 때 탐색
                player.getServer().getScheduler().runTaskLater(shop.shop.ShopPlugin.getInstance(), () -> {
                    Optional<Npc> npcOpt = FancyNpcs.getInstance().getNpcManager().getAllNpcs().stream()
                            .filter(npc -> npc.getData().getDisplayName().equalsIgnoreCase(npcName))
                            .findFirst();

                    npcOpt.ifPresent(npc -> {
                        java.util.UUID uuid = npc.getData().getCreator();
                        ShopNpcRegistry.linkNpcToShop(uuid, npcName);
                        player.sendMessage("§aNPC '" + npcName + "' 이(가) 상점과 연결되었습니다!");
                    });
                }, 2L); // 2틱 후 처리
            }
        }
    }
}
