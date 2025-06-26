package shop.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static shop.shop.ShopManager.shops;

public final class ShopPlugin extends JavaPlugin {
    @Override
    public void onEnable() {

        MySQLManager.connect();

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
        try {
            Statement st = MySQLManager.getConnection().createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM shops");

            while (rs.next()) {
                String name = rs.getString("name");
                int rows = rs.getInt("shop_rows");

                Inventory inv = Bukkit.createInventory(null, rows * 9, "§8[상점] " + name);
                shops.put(name, inv);
            }

            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
