package shop.shop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6[교환상점 명령어 목록]");
            player.sendMessage("§e/교환상점 생성 §f[이름] [줄수]");
            player.sendMessage("§e/교환상점 제거 §f[이름]");
            player.sendMessage("§e/교환상점 열기 §f[이름]");
            player.sendMessage("§e/교환상점 목록");
            player.sendMessage("§e/교환상점 설정 §f[이름]");
            player.sendMessage("§e/교환상점 재료 §f[이름]");
            player.sendMessage("§e/교환상점 재고 §f[이름] [줄수]");
            player.sendMessage("§e/교환상점 확인 §f[이름]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "생성" -> ShopManager.createShop(player, args);
            case "제거" -> ShopManager.removeShop(player, args);
            case "열기" -> ShopManager.openShop(player, args);
            case "목록" -> ShopManager.listShops(player);
            case "설정" -> ShopManager.editShop(player, args);
            case "재료" -> ShopManager.setMaterials(player, args);
            case "재고" -> ShopManager.setStock(player, args);
            case "확인" -> ShopManager.showMaterials(player, args);
            default -> player.sendMessage("§c존재하지 않는 서브 명령어입니다.");
        }
        return true;
    }
}
