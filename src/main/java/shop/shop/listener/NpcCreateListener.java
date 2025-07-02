package shop.shop.listener;

import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.util.Vector;
import shop.shop.registry.ShopNpcRegistry;

import java.util.Optional;

public class NpcCreateListener implements Listener {

    @EventHandler
    public void onNpcLinkCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();
        String msg = e.getMessage().toLowerCase();

        if (msg.equalsIgnoreCase("/교환상점 npc")) {
            e.setCancelled(true); // 명령어 기본 처리 방지

            Optional<Npc> targetNpcOpt = FancyNpcs.getInstance().getNpcManager().getAllNpcs().stream()
                    .filter(npc -> {
                        if (!npc.getData().getLocation().getWorld().equals(player.getWorld())) return false;

                        double distance = npc.getData().getLocation().distance(player.getEyeLocation());
                        if (distance > 5) return false;

                        Vector direction = player.getEyeLocation().getDirection().normalize();
                        Vector toNpc = npc.getData().getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();

                        double dot = direction.dot(toNpc);
                        return dot > 0.95;
                    })
                    .findFirst();

            if (targetNpcOpt.isPresent()) {
                Npc targetNpc = targetNpcOpt.get();
                String identifier = targetNpc.getData().getId();
                String displayName = targetNpc.getData().getDisplayName();

                ShopNpcRegistry.linkNpcToShop(identifier, displayName);
                player.sendMessage("§aNPC '" + displayName + "' 이(가) 교환상점에 연결되었습니다!");
            } else {
                player.sendMessage("§c시선 방향 5블럭 이내에 NPC가 없습니다.");
            }
        }
    }

    // 간단한 시선 판단 함수 (시야 각도 기반)
    private boolean isLookingAt(Player player, Entity target) {
        Vector direction = player.getLocation().getDirection().normalize();
        Vector toEntity = target.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        double dot = direction.dot(toEntity);
        return dot > 0.95; // 1.0에 가까울수록 정확히 바라봄
    }
}
