// ShopNpcRegistry.java
package shop.shop.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopNpcRegistry {
    private static final Map<UUID, String> npcToShop = new HashMap<>();

    public static void linkNpcToShop(UUID npcUUID, String shopName) {
        npcToShop.put(npcUUID, shopName);
    }

    public static String getShopName(UUID npcUUID) {
        return npcToShop.get(npcUUID);
    }
}
