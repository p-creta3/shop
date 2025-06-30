package shop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
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

public final class ShopPlugin extends JavaPlugin {

    private static ShopPlugin instance;

    @Override
    public void onEnable() {

        instance = this;

        getServer().getPluginManager().registerEvents(new NpcClickListener(), this);
        getServer().getPluginManager().registerEvents(new NpcCreateListener(), this);
        getServer().getPluginManager().registerEvents(new ShopManager(), this);

        MySQLManager.testConnection();

        loadShopsFromDatabase();

        getCommand("교환상점").setExecutor(new ShopCommand());
        Bukkit.getPluginManager().registerEvents(new ShopManager(), this);
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

    public static ShopPlugin getInstance() {
        return instance;
    }
}
