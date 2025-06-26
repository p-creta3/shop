package shop.shop;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLManager {

    private static final String HOST = "localhost";
    private static final String DATABASE = "shop_db";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";
    private static final String URL = "jdbc:mysql://" + HOST + "/" + DATABASE + "?useSSL=false&characterEncoding=UTF-8";

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            Bukkit.getLogger().info("[Shop] MySQL 연결 성공!");
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("[Shop] MySQL 연결 실패!");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
