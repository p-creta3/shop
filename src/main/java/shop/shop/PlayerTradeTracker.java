package shop.shop;

import java.util.HashMap;
import java.util.Map;

public class PlayerTradeTracker {
    private static final Map<String, Integer> tradeCounts = new HashMap<>();

    public static int getCount(String key) {
        return tradeCounts.getOrDefault(key, 0);
    }

    public static void increment(String key) {
        tradeCounts.put(key, getCount(key) + 1);
    }

    // (Optional) 초기화, 저장 등 추가 가능
}
