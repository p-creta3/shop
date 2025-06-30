// ShopNpcRegistry.java
package shop.shop.registry;

import java.util.HashMap;
import java.util.Map;

public class ShopNpcRegistry {

    private static final Map<String, String> npcToShop = new HashMap<>();

    public static void linkNpcToShop(String npcIdentifier, String shopName) {
        npcToShop.put(npcIdentifier, shopName);
    }

    public static String getShopName(String npcIdentifier) {
        return npcToShop.get(npcIdentifier);
    }
}
