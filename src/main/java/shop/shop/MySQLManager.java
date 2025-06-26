package shop.shop;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLManager {
    private static Connection connection;

    public static void connect() {
        String host = "localhost";
        String database = "shop_db";
        String user = "root";
        String password = "1234";
        String url = "jdbc:mysql://" + host + "/" + database + "?useSSL=false&characterEncoding=UTF-8";

        try {
            connection = DriverManager.getConnection(url, user, password);
            Bukkit.getLogger().info("[Shop] MySQL 연결 성공!");
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("[Shop] MySQL 연결 실패!");
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

