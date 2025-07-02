package shop.shop.registry;

import java.util.HashMap;
import java.util.Map;

public class ShopNpcRegistry {

    // NPC ID → Shop 이름
    private static final Map<String, String> npcToShop = new HashMap<>();

    // 등록 메서드
    public static void linkNpcToShop(String npcIdentifier, String shopName) {
        npcToShop.put(npcIdentifier, shopName);
    }

    // 별칭: register()도 추가 (static import용)
    public static void register(String npcIdentifier, String shopName) {
        linkNpcToShop(npcIdentifier, shopName);
    }

    // 조회
    public static String getShopName(String npcIdentifier) {
        return npcToShop.get(npcIdentifier);
    }

    // Map 자체에 접근하고 싶을 때 (예: clear())
    public static Map<String, String> getNpcToShop() {
        return npcToShop;
    }
}
