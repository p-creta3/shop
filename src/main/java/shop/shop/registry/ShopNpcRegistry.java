package shop.shop.registry;

import org.bukkit.ChatColor;
import shop.shop.manager.MySQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopNpcRegistry {

    private static final Map<String, String> npcToShop = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ShopNpcRegistry.class.getName());

    // NPC 이름으로 상점 연결
    public static void linkNpcToShop(String npcDisplayName, String shopName) {
        String cleanName = ChatColor.stripColor(npcDisplayName);
        npcToShop.put(cleanName, shopName);

        try (Connection conn = MySQLManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO npc_shops (npc_display_name, shop_name) VALUES (?, ?) ON DUPLICATE KEY UPDATE shop_name = ?")) {
            ps.setString(1, cleanName);
            ps.setString(2, shopName);
            ps.setString(3, shopName);
            ps.executeUpdate();
            logger.info("[ShopNpcRegistry] NPC '" + cleanName + "' -> 상점 '" + shopName + "' 데이터베이스에 저장됨");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ShopNpcRegistry] NPC-상점 매핑 저장 중 오류 발생", e);
        }
    }

    public static void loadNpcShopsFromDB() {
        npcToShop.clear();
        logger.info("[ShopNpcRegistry] NPC-상점 매핑 초기화");

        try (Connection conn = MySQLManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT npc_display_name, shop_name FROM npc_shops");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String displayName = rs.getString("npc_display_name");
                String shopName = rs.getString("shop_name");
                npcToShop.put(displayName, shopName);
                logger.info("[ShopNpcRegistry] NPC '" + displayName + "' -> 상점 '" + shopName + "' 데이터베이스에서 로드됨");
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[ShopNpcRegistry] NPC-상점 매핑 로드 중 오류 발생", e);
        }
    }

    public static String getShopName(String npcDisplayName) {
        String cleanName = ChatColor.stripColor(npcDisplayName);
        String shopName = npcToShop.get(cleanName);
        if (shopName == null) {
            logger.warning("[ShopNpcRegistry] NPC '" + cleanName + "'에 매핑된 상점 없음");
        }
        return shopName;
    }

    public static void register(String npcIdentifier, String shopName) {
        linkNpcToShop(npcIdentifier, shopName);
    }

    public static Map<String, String> getNpcToShop() {
        return npcToShop;
    }
}