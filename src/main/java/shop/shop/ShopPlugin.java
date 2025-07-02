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
import shop.shop.registry.ShopNpcRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import static shop.shop.manager.ShopManager.shops;

public final class ShopPlugin extends JavaPlugin {

    private static ShopPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        // 데이터베이스에서 상점 로드
        loadShopsFromDatabase();

        // 데이터베이스에서 NPC-상점 매핑 로드
        ShopNpcRegistry.loadNpcShopsFromDB();

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new NpcClickListener(), this);
        getServer().getPluginManager().registerEvents(new NpcCreateListener(), this);
        getServer().getPluginManager().registerEvents(new ShopManager(), this);

        // MySQL 연결 테스트
        MySQLManager.testConnection();

        // NPC 매핑 리로드 (5초 지연)
        Bukkit.getScheduler().runTaskLater(this, ShopPlugin::reloadMappings, 100L);

        // 명령어 등록
        getCommand("교환상점").setExecutor(new ShopCommand());

        getLogger().info("[교환상점] 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        getLogger().info("[교환상점] 플러그인이 비활성화되었습니다.");
    }

    public static void loadShopsFromDatabase() {
        try (Connection conn = MySQLManager.getConnection();
             PreparedStatement st = conn.prepareStatement("SELECT * FROM shops");
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                int rows = rs.getInt("shop_rows");

                Inventory inv = Bukkit.createInventory(null, rows * 9, "§8[상점] " + name);
                shops.put(name, inv);
                ShopManager.loadShopItems(name, inv);
                getInstance().getLogger().info("[ShopPlugin] 상점 '" + name + "' 로드됨 (행: " + rows + ")");
            }

        } catch (SQLException e) {
            getInstance().getLogger().log(Level.SEVERE, "[ShopPlugin] 상점 데이터 로드 중 오류 발생", e);
        }
    }

    public static void reloadMappings() {
        ShopNpcRegistry.getNpcToShop().clear();
        getInstance().getLogger().info("[ShopPlugin] NPC-상점 매핑 초기화");

        // 데이터베이스에서 기존 매핑 로드
        ShopNpcRegistry.loadNpcShopsFromDB();

        // FancyNpcsPlugin에서 NPC 목록 가져오기
        if (FancyNpcsPlugin.get() == null || FancyNpcsPlugin.get().getNpcManager() == null) {
            getInstance().getLogger().warning("[ShopPlugin] FancyNpcsPlugin 또는 NpcManager가 로드되지 않음");
            return;
        }

        for (Npc npc : FancyNpcsPlugin.get().getNpcManager().getAllNpcs()) {
            String displayName = npc.getData().getDisplayName();
            String cleanName = org.bukkit.ChatColor.stripColor(displayName);
            String shopName = parseShopName(cleanName);

            if (shopName != null && !shopName.isEmpty() && shops.containsKey(shopName)) {
                ShopNpcRegistry.register(cleanName, shopName);
                getInstance().getLogger().info("[ShopPlugin] NPC '" + cleanName + "' -> 상점 '" + shopName + "' 매핑됨");
            } else {
                getInstance().getLogger().warning("[ShopPlugin] NPC '" + cleanName + "'에서 상점 이름 추출 실패 또는 상점 '" + shopName + "'이 존재하지 않음");
            }
        }
    }

    private static String parseShopName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return null;
        }

        // [shop] 접두어 처리
        if (displayName.startsWith("[shop]")) {
            return displayName.substring(6).trim();
        }

        // 상점: 접두어 처리
        if (displayName.startsWith("상점:")) {
            return displayName.substring(3).trim();
        }

        // 영문, 한글, 숫자, 언더바만 포함된 경우 그대로 사용
        if (displayName.matches("^[a-zA-Z0-9_가-힣]+$")) {
            return displayName;
        }

        // ":"로 구분된 경우 뒷부분 추출
        if (displayName.contains(":")) {
            String[] parts = displayName.split(":", 2);
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }

        return null;
    }

    public static ShopPlugin getInstance() {
        return instance;
    }
}