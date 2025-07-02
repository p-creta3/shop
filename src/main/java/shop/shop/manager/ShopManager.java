package shop.shop.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import shop.shop.ItemSerializer;
import shop.shop.PlayerTradeTracker;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;

public class ShopManager implements Listener {
    public static final Map<String, Inventory> shops = new HashMap<>();
    private static final Map<String, Map<ItemStack, List<ItemStack>>> itemToMaterials = new HashMap<>();
    private static final Map<String, Map<ItemStack, Integer>> itemToStock = new HashMap<>();

    private static final Map<UUID, String> currentEditingShop = new HashMap<>();
    private static final Map<UUID, String> stockEditingShop = new HashMap<>();
    private static final Map<UUID, ItemStack> selectedItem = new HashMap<>();
    private static final Set<UUID> waitingForStockInput = new HashSet<>();
    private static final Map<UUID, String> editMode = new HashMap<>(); // "edit" or "material"

    public static void createShop(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c사용법: /교환상점 생성 [이름] [줄수]");
            return;
        }

        String name = args[1];
        int rows;
        try {
            rows = Integer.parseInt(args[2]);
            if (rows < 1 || rows > 6) {
                player.sendMessage("§c줄 수는 1~6 사이여야 합니다.");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c줄 수는 숫자여야 합니다.");
            return;
        }

        if (shops.containsKey(name)) {
            player.sendMessage("§c이미 존재하는 상점 이름입니다.");
            return;
        }

        // 인벤토리 생성 및 등록
        Inventory inv = Bukkit.createInventory(null, rows * 9, "§8[상점] " + name);
        shops.put(name, inv);
        player.sendMessage("§a상점 [" + name + "] 이(가) 생성되었습니다.");

        // MySQL에 저장
        try {
            PreparedStatement ps = MySQLManager.getConnection().prepareStatement(
                    "INSERT INTO shops (name, shop_rows) VALUES (?, ?)"
            );
            ps.setString(1, name);
            ps.setInt(2, rows);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            player.sendMessage("§c상점 DB 저장에 실패했습니다.");
        }
    }


    public static void removeShop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /교환상점 제거 [이름]");
            return;
        }
        String name = args[1];
        if (shops.remove(name) != null) {
            player.sendMessage("§a상점 [" + name + "] 제거됨");

            // MySQL에서도 상점과 관련된 데이터 삭제
            try {
                // shops 테이블에서 삭제
                PreparedStatement psShop = MySQLManager.getConnection().prepareStatement(
                        "DELETE FROM shops WHERE name = ?"
                );
                psShop.setString(1, name);
                psShop.executeUpdate();
                psShop.close();

                // shop_items 테이블에서 관련 아이템 삭제
                PreparedStatement psItems = MySQLManager.getConnection().prepareStatement(
                        "DELETE FROM shop_items WHERE shop_name = ?"
                );
                psItems.setString(1, name);
                psItems.executeUpdate();
                psItems.close();

            } catch (SQLException e) {
                e.printStackTrace();
                player.sendMessage("§cMySQL에서 상점 데이터를 삭제하는 도중 오류가 발생했습니다.");
            }

        } else {
            player.sendMessage("§c존재하지 않는 상점입니다.");
        }
    }

    public static void openShop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /교환상점 열기 [이름]");
            return;
        }
        String name = args[1];
        Inventory inv = shops.get(name);
        if (inv != null) {
            player.openInventory(inv);
        } else {
            player.sendMessage("§c존재하지 않는 상점입니다.");
        }
    }

    public static void listShops(Player player) {
        if (shops.isEmpty()) {
            player.sendMessage("§7등록된 상점이 없습니다.");
            return;
        }
        player.sendMessage("§6[등록된 상점 목록]");
        shops.keySet().forEach(n -> player.sendMessage("§f- " + n));
    }

    public static void editShop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /교환상점 설정 [이름]");
            return;
        }
        String name = args[1];
        Inventory inv = shops.get(name);
        if (inv == null) {
            player.sendMessage("§c존재하지 않는 상점입니다.");
            return;
        }
        currentEditingShop.put(player.getUniqueId(), name);
        editMode.put(player.getUniqueId(), "edit");
        player.openInventory(inv);
        player.sendMessage("§e상점 GUI 편집 모드에 진입했습니다.");
    }

    public static void setMaterials(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /교환상점 재료 [이름]");
            return;
        }
        String name = args[1];
        Inventory inv = shops.get(name);
        if (inv == null) {
            player.sendMessage("§c존재하지 않는 상점입니다.");
            return;
        }
        currentEditingShop.put(player.getUniqueId(), name);
        editMode.put(player.getUniqueId(), "material");
        player.openInventory(inv);
        player.sendMessage("§e아이템을 우클릭하여 재료 설정 GUI를 여세요.");
    }

    public static void saveShopItem(String shopName, ItemStack item, Integer stock) {
        String itemBase64 = ItemSerializer.serialize(item);
        if (itemBase64 == null) return;

        try {
            // 중복 방지 위해 기존 데이터 삭제
            PreparedStatement deleteStmt = MySQLManager.getConnection().prepareStatement(
                    "DELETE FROM shop_items WHERE shop_name = ? AND item_base64 = ?"
            );
            deleteStmt.setString(1, shopName);
            deleteStmt.setString(2, itemBase64);
            deleteStmt.executeUpdate();
            deleteStmt.close();

            // 새 아이템 저장
            PreparedStatement insertStmt = MySQLManager.getConnection().prepareStatement(
                    "INSERT INTO shop_items (shop_name, item_base64, stock) VALUES (?, ?, ?)"
            );
            insertStmt.setString(1, shopName);
            insertStmt.setString(2, itemBase64);
            if (stock != null) {
                insertStmt.setInt(3, stock);
            } else {
                insertStmt.setNull(3, java.sql.Types.INTEGER);
            }
            insertStmt.executeUpdate();
            insertStmt.close();

            System.out.println("[ShopPlugin] 상점 '" + shopName + "' 아이템 " + item.getType() + " 저장 완료, 재고: " + (stock != null ? stock : "null"));

        } catch (SQLException e) {
            e.printStackTrace();
            // 필요시 플레이어에게 메시지 전달 가능
        }
    }

    public static void loadShopItems(String shopName, Inventory inv) {
        try {
            PreparedStatement ps = MySQLManager.getConnection().prepareStatement(
                    "SELECT id, item_base64, stock FROM shop_items WHERE shop_name = ?"
            );
            ps.setString(1, shopName);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int itemId = rs.getInt("id");
                String base64 = rs.getString("item_base64");
                int stock = rs.getInt("stock");

                ItemStack item = ItemSerializer.deserialize(base64);
                if (item != null) {
                    inv.addItem(item);

                    // 재고 등록
                    itemToStock.putIfAbsent(shopName, new HashMap<>());
                    itemToStock.get(shopName).put(item, stock);

                    // 재료 불러오기
                    loadMaterialsForItem(itemId, item, shopName);
                }
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void loadMaterialsForItem(int itemId, ItemStack resultItem, String shopName) {
        try {
            PreparedStatement ps = MySQLManager.getConnection().prepareStatement(
                    "SELECT material_base64, amount FROM item_materials WHERE item_id = ?"
            );
            ps.setInt(1, itemId);
            ResultSet rs = ps.executeQuery();

            List<ItemStack> materials = new ArrayList<>();

            while (rs.next()) {
                String matBase64 = rs.getString("material_base64");
                int amount = rs.getInt("amount");

                ItemStack mat = ItemSerializer.deserialize(matBase64);
                if (mat != null) {
                    mat.setAmount(amount);
                    materials.add(mat);
                }
            }

            rs.close();
            ps.close();

            itemToMaterials.putIfAbsent(shopName, new HashMap<>());
            itemToMaterials.get(shopName).put(resultItem, materials);

            System.out.println("[ShopPlugin] 재료 로딩 완료 (" + resultItem.getType() + ")");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static Integer getItemId(String shopName, ItemStack item) {
        String base64 = ItemSerializer.serialize(item);
        if (base64 == null) return null;

        try {
            PreparedStatement ps = MySQLManager.getConnection().prepareStatement(
                    "SELECT id FROM shop_items WHERE shop_name = ? AND item_base64 = ?"
            );
            ps.setString(1, shopName);
            ps.setString(2, base64);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                ps.close();
                return id;
            }

            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static void setStock(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§c사용법: /교환상점 재고 [이름] [횟수]");
            return;
        }

        String name = args[1];
        int maxStock;
        try {
            maxStock = Integer.parseInt(args[2]);
            if (maxStock < 0) throw new NumberFormatException(); // 0 이상은 허용
        } catch (NumberFormatException e) {
            player.sendMessage("§c횟수는 0 이상의 숫자여야 합니다. (0 = 무제한)");
            return;
        }

        Inventory inv = shops.get(name);
        if (inv == null) {
            player.sendMessage("§c존재하지 않는 상점입니다.");
            return;
        }

        Map<ItemStack, Integer> stockMap = new HashMap<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                stockMap.put(item.clone(), maxStock == 0 ? null : maxStock); // 0이면 null 저장 = 무제한
            }
        }

        itemToStock.put(name, stockMap);
        if (maxStock == 0) {
            player.sendMessage("§a[" + name + "] 상점 내 아이템들의 최대 교환 횟수를 §e무제한§a으로 설정했습니다.");
        } else {
            player.sendMessage("§a[" + name + "] 상점 내 아이템들의 최대 교환 횟수를 " + maxStock + "회로 설정했습니다.");
        }
    }

    public static void saveMaterialsForItem(String shopName, ItemStack resultItem, List<ItemStack> materials) {
        Integer itemId = getItemId(shopName, resultItem);
        if (itemId == null) {
            System.out.println("[ShopPlugin] 저장 실패: 아이템 ID 찾을 수 없음");
            return;
        }

        try {
            // 기존 재료 삭제
            PreparedStatement delete = MySQLManager.getConnection().prepareStatement(
                    "DELETE FROM item_materials WHERE item_id = ?"
            );
            delete.setInt(1, itemId);
            delete.executeUpdate();
            delete.close();

            // 새 재료 삽입
            PreparedStatement insert = MySQLManager.getConnection().prepareStatement(
                    "INSERT INTO item_materials (item_id, material_base64, amount) VALUES (?, ?, ?)"
            );

            for (ItemStack mat : materials) {
                String matBase64 = ItemSerializer.serialize(mat);
                insert.setInt(1, itemId);
                insert.setString(2, matBase64);
                insert.setInt(3, mat.getAmount());
                insert.addBatch();
            }

            insert.executeBatch();
            insert.close();
            System.out.println("[ShopPlugin] 재료 저장 완료 (" + resultItem.getType() + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null || e.getCurrentItem() == null) return;

        String title = e.getView().getTitle();
        int slot = e.getRawSlot(); // 현재 열린 GUI 기준 슬롯 번호 (0~35)

        // [1] 재료 설정 GUI 처리
        if (title.startsWith("§e[재료 설정] ")) {
            if (slot == 26) {
                e.setCancelled(true);

                String shopName = currentEditingShop.get(player.getUniqueId());
                ItemStack resultItem = selectedItem.get(player.getUniqueId());

                if (shopName == null || resultItem == null) {
                    player.sendMessage("§c저장할 수 없습니다. (정보 누락)");
                    player.closeInventory();
                    return;
                }

                List<ItemStack> materials = new ArrayList<>();
                Inventory inv = e.getInventory(); // 현재 GUI 인벤토리 (0~26)
                for (int i = 0; i < 26; i++) {
                    ItemStack is = inv.getItem(i);
                    if (is != null && is.getType() != Material.AIR) {
                        materials.add(is.clone());
                    }
                }

                itemToMaterials.putIfAbsent(shopName, new HashMap<>());
                itemToMaterials.get(shopName).put(resultItem, materials);

                saveMaterialsForItem(shopName, resultItem, materials);

                player.sendMessage("§a" + resultItem.getType() + "의 재료가 저장되었습니다.");

                // 상태 초기화
                currentEditingShop.remove(player.getUniqueId());
                selectedItem.remove(player.getUniqueId());
                editMode.remove(player.getUniqueId());

                player.closeInventory();
            } else if (slot >= 0 && slot < 26) {
                e.setCancelled(false); // 재료 입력 허용
            } else {
                e.setCancelled(false); // 플레이어 인벤토리도 허용
            }

            return;
        }

        String shopName = null;
        boolean isEditing = currentEditingShop.containsKey(player.getUniqueId());
        if (isEditing) {
            shopName = currentEditingShop.get(player.getUniqueId());
        } else {
            // 편집 중 아니면 shopName 추출 (일반 상점 열기 시)
            if (title.startsWith("§8[상점] ")) {
                shopName = title.substring("§8[상점] ".length());
            }
        }

        // [2] 편집 모드인 경우
        if (isEditing && shopName != null && title.startsWith("§8[상점] ")) {
            String mode = editMode.getOrDefault(player.getUniqueId(), "edit");

            if ("material".equals(mode)) {
                if (e.getClick() == ClickType.RIGHT) {
                    ItemStack clicked = e.getCurrentItem();
                    if (clicked != null && clicked.getType() != Material.AIR) {
                        selectedItem.put(player.getUniqueId(), clicked.clone());
                        openMaterialGui(player, shopName, clicked.clone());
                    }
                }
                e.setCancelled(true); // material 모드에선 왼클릭 등 차단
            } else {
                e.setCancelled(false); // edit 모드에선 자유 편집 허용
            }
            return; // 편집 모드이면 아래 거래 모드 진입 방지
        }

        // [3] 거래 모드 (일반 상점 열기)
        if (!isEditing && shopName != null && title.startsWith("§8[상점] ")) {
            e.setCancelled(true); // 클릭 막고 수동 처리

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Map<ItemStack, List<ItemStack>> materialMap = itemToMaterials.get(shopName);
            Map<ItemStack, Integer> stockMap = itemToStock.get(shopName);

            if (materialMap == null) {
                player.sendMessage("§c이 아이템은 설정된 재료가 없습니다.");
                return;
            }

            // 재료 찾기
            List<ItemStack> required = null;
            for (ItemStack key : materialMap.keySet()) {
                if (key.isSimilar(clicked)) {
                    required = materialMap.get(key);
                    break;
                }
            }

            if (required == null) {
                player.sendMessage("§c이 아이템은 교환 재료가 없습니다.");
                return;
            }

            // 재료 충분한지 확인
            boolean hasAll = true;
            for (ItemStack req : required) {
                if (!player.getInventory().containsAtLeast(req, req.getAmount())) {
                    hasAll = false;
                    break;
                }
            }

            if (!hasAll) {
                player.sendMessage("§c재료가 부족합니다.");
                return;
            }

            // 재고 확인
            if (stockMap != null && stockMap.containsKey(clicked)) {
                String key = player.getUniqueId().toString() + ":" + clicked.getType();
                Integer max = stockMap.get(clicked);
                if (max != null) {
                    int used = PlayerTradeTracker.getCount(key);
                    if (used >= max) {
                        player.sendMessage("§c이 아이템은 더 이상 교환할 수 없습니다.");
                        return;
                    }
                    PlayerTradeTracker.increment(key);
                }
            }

            // 재료 차감 및 아이템 지급
            for (ItemStack req : required) {
                player.getInventory().removeItem(new ItemStack(req.getType(), req.getAmount()));
            }

            player.getInventory().addItem(clicked.clone());
            player.sendMessage("§a아이템이 성공적으로 교환되었습니다!");
            return;
        }

        // [4] 재고 설정 GUI 처리 (기존 그대로)
        if (title.startsWith("§b[재고 설정] ") && stockEditingShop.containsKey(player.getUniqueId())) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                player.closeInventory();
                selectedItem.put(player.getUniqueId(), clicked.clone());
                waitingForStockInput.add(player.getUniqueId());
                player.sendMessage("§a해당 아이템의 최대 재고 수량을 채팅으로 입력하세요.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();

        if (title.startsWith("§8[상점] ")) {
            String shopName = title.replace("§8[상점] ", "");
            Inventory inv = e.getInventory();

            // 메모리 shops 업데이트
            ShopManager.shops.put(shopName, inv);

            // DB에 아이템 저장
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    Integer stock = null;
                    if (ShopManager.itemToStock.containsKey(shopName)) {
                        Map<ItemStack, Integer> stockMap = ShopManager.itemToStock.get(shopName);
                        for (ItemStack keyItem : stockMap.keySet()) {
                            if (keyItem.isSimilar(item)) {
                                stock = stockMap.get(keyItem);
                                break;
                            }
                        }
                    }

                    ShopManager.saveShopItem(shopName, item, stock);
                }
            }
        }
    }

    @EventHandler
    public void onChatStockInput(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!waitingForStockInput.contains(player.getUniqueId())) return;

        e.setCancelled(true);
        try {
            int amt = Integer.parseInt(e.getMessage());
            if (amt <= 0) throw new NumberFormatException();

            String shopName = stockEditingShop.get(player.getUniqueId());
            ItemStack item = selectedItem.get(player.getUniqueId());

            itemToStock.putIfAbsent(shopName, new HashMap<>());
            itemToStock.get(shopName).put(item, amt);
            player.sendMessage("§a재고가 설정되었습니다: " + item.getType() + " x" + amt);
        } catch (NumberFormatException ex) {
            player.sendMessage("§c숫자만 입력하세요.");
        } finally {
            waitingForStockInput.remove(player.getUniqueId());
            stockEditingShop.remove(player.getUniqueId());
            selectedItem.remove(player.getUniqueId());
        }
    }

    public static void openMaterialGui(Player player, String shopName, ItemStack resultItem) {
        Inventory gui = Bukkit.createInventory(null, 27, "§e[재료 설정] " + resultItem.getType());

        Map<ItemStack, List<ItemStack>> shopMaterials = itemToMaterials.get(shopName);
        if (shopMaterials != null) {
            for (Map.Entry<ItemStack, List<ItemStack>> entry : shopMaterials.entrySet()) {
                if (entry.getKey().isSimilar(resultItem)) {
                    List<ItemStack> materials = entry.getValue();
                    for (int i = 0; i < materials.size() && i < 26; i++) {
                        gui.setItem(i, materials.get(i));
                    }
                    break;
                }
            }
        }

        ItemStack saveButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = saveButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a[ 저장 ]");
            saveButton.setItemMeta(meta);
        }
        gui.setItem(26, saveButton);

        player.openInventory(gui);
    }

    public static void printMaterialInfo(Player player, String shopName) {
        Map<ItemStack, List<ItemStack>> map = itemToMaterials.get(shopName);
        if (map == null) {
            player.sendMessage("§7해당 상점에 설정된 재료가 없습니다.");
            return;
        }

        for (Map.Entry<ItemStack, List<ItemStack>> entry : map.entrySet()) {
            player.sendMessage("§f[" + entry.getKey().getType() + "] 에 필요한 재료:");
            for (ItemStack mat : entry.getValue()) {
                player.sendMessage(" - " + mat.getAmount() + "x " + mat.getType());
            }
        }
    }

    public static void showMaterials(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c사용법: /교환상점 확인 [이름]");
            return;
        }

        String name = args[1];
        if (!shops.containsKey(name)) {
            player.sendMessage("§c존재하지 않는 상점입니다.");
            return;
        }

        Map<ItemStack, List<ItemStack>> map = itemToMaterials.get(name);
        if (map == null || map.isEmpty()) {
            player.sendMessage("§7해당 상점에 등록된 재료 정보가 없습니다.");
            return;
        }

        player.sendMessage("§6[" + name + "] 상점의 아이템 재료 목록:");
        for (Map.Entry<ItemStack, List<ItemStack>> entry : map.entrySet()) {
            ItemStack item = entry.getKey();
            List<ItemStack> materials = entry.getValue();
            player.sendMessage("§f- " + item.getType() + "에 필요한 재료:");
            for (ItemStack mat : materials) {
                player.sendMessage("   §7• " + mat.getAmount() + "x " + mat.getType());
            }
        }
    }

}
