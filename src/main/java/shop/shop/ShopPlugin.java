package shop.shop;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import shop.shop.command.ShopCommand;
import shop.shop.listener.NpcClickListener;
import shop.shop.listener.NpcCreateListener;
import shop.shop.manager.MySQLManager;
import shop.shop.manager.ShopManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static shop.shop.manager.ShopManager.shops;
import static shop.shop.registry.ShopNpcRegistry.*;

public final class ShopPlugin extends JavaPlugin {

    private static ShopPlugin instance;

    @Override
    public void onEnable() {

        instance = this;

        getServer().getPluginManager().registerEvents(new NpcClickListener(), this);
        getServer().getPluginManager().registerEvents(new NpcCreateListener(), this);
        getServer().getPluginManager().registerEvents(new ShopManager(), this);

        MySQLManager.testConnection();

        reloadMappings();

        loadShopsFromDatabase();

        getCommand("교환상점").setExecutor(new ShopCommand());

        getLogger().info("[교환상점] 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[교환상점] 플러그인이 비활성화되었습니다.");
    }

    public static void loadShopsFromDatabase() {
        try (Connection conn = MySQLManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM shops")) {

            while (rs.next()) {
                String name = rs.getString("name");
                int rows = rs.getInt("shop_rows");

                Inventory inv = Bukkit.createInventory(null, rows * 9, "§8[상점] " + name);
                shops.put(name, inv);

                ShopManager.loadShopItems(name, inv); // 내부에서도 커넥션 새로 만들게 해야 함
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void reloadMappings() {
        getNpcToShop().clear();  // 직접 Map 접근 가능
        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            String id = npc.getData().getId();
            String name = npc.getData().getDisplayName();
            String shopName = parseShopName(name);

            if (shopName != null && !shopName.isEmpty()) {
                register(id, shopName);
                Bukkit.getLogger().info("[ShopPlugin] NPC '" + name + "' -> 상점 '" + shopName + "' 자동 매핑");
            } else {
                Bukkit.getLogger().warning("[ShopPlugin] NPC '" + name + "'에서 상점 이름 추출 실패");
            }
        }
    }

    private static String parseShopName(String displayName) {
        // 예: "[shop]weapon_shop" → "weapon_shop"
        if (displayName.startsWith("[shop]")) {
            return displayName.substring(6);
        }

        // 예: "무기상점" → 그대로 사용
        if (displayName.matches("^[a-zA-Z0-9_가-힣]+$")) {
            return displayName;
        }

        // 예: "상점:armor_shop" → ":" 뒤 부분 추출
        if (displayName.contains(":")) {
            return displayName.split(":")[1];
        }

        // 기타 상황: null 반환
        return null;
    }



    public static ShopPlugin getInstance() {
        return instance;
    }
}
