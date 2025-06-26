//package shop.shop;
//
//import org.bukkit.Bukkit;
//import org.bukkit.Material;
//import org.bukkit.entity.Player;
//import org.bukkit.event.Listener;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.inventory.InventoryClickEvent;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
//
//public class ShopGUI implements Listener {
//
//    public static void openShop(Player player) {
//        int size = ShopPlugin.getShopManager().getShopRows() * 9;
//        Inventory inv = Bukkit.createInventory(null, size, "§a상점");
//
//        // 중앙에 다이아몬드
//        int centerSlot = size / 2;
//        inv.setItem(centerSlot, ShopPlugin.getShopManager().getShopItem());
//
//        player.openInventory(inv);
//    }
//
//    @EventHandler
//    public void onInventoryClick(InventoryClickEvent event) {
//        if (!event.getView().getTitle().equals("§a상점")) return;
//
//        event.setCancelled(true);
//        if (event.getCurrentItem() == null) return;
//
//        if (event.getCurrentItem().getType() == Material.DIAMOND) {
//            Player player = (Player) event.getWhoClicked();
//            player.sendMessage("§a다이아몬드를 구매했습니다!");
//        }
//    }
//}
