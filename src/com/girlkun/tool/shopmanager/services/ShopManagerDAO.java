package com.girlkun.tool.shopmanager.services;

import com.girlkun.tool.shopmanager.models.*;
import com.girlkun.tool.shopmanager.utils.ConfigManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManagerDAO {

    private static ShopManagerDAO instance;
    private Connection connection;
    private DbConfig config;

    public static ShopManagerDAO gI() {
        if (instance == null) {
            instance = new ShopManagerDAO();
        }
        return instance;
    }

    private ShopManagerDAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Load driver error: " + e.getMessage());
        }
        this.config = ConfigManager.load();
    }

    public void reloadConfig() {
        close(); // Close old connection
        this.config = ConfigManager.load();
    }

    // Connect in background
    public void initBackground() {
        new Thread(() -> {
            try {
                connect();
                System.out.println("ShopManager: Connected to DB in background.");
            } catch (Exception e) {
                System.out.println("ShopManager: Background connect failed: " + e.getMessage());
            }
        }).start();
    }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        String url = config.toConnectionString();
        connection = DriverManager.getConnection(url, config.user, config.password);
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
        }
        connection = null;
    }

    // Get active connection or reconnect
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        // Safe check validation
        try {
            if (!connection.isValid(2)) {
                close();
                connect();
            }
        } catch (Exception e) {
            close();
            connect();
        }
        return connection;
    }

    public boolean checkConnection() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    // --- DAO Methods (Refactored to NOT close Connection) ---

    public List<Shop> getShops() {
        List<Shop> shops = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT s.id, s.npc_id, s.tag_name, s.type_shop, n.name AS npc_name " +
                            "FROM shop s JOIN npc_template n ON s.npc_id = n.id")) {

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Shop shop = new Shop();
                    shop.id = rs.getInt("id");
                    shop.npcId = rs.getInt("npc_id");
                    shop.tagName = rs.getString("tag_name");
                    shop.typeShop = rs.getInt("type_shop");
                    shop.npcName = rs.getString("npc_name");
                    shops.add(shop);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shops;
    }

    public void addShop(Shop shop) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn
                    .prepareStatement("INSERT INTO shop (npc_id, tag_name, type_shop) VALUES (?, ?, ?)")) {
                stmt.setInt(1, shop.npcId);
                stmt.setString(2, shop.tagName);
                stmt.setInt(3, shop.typeShop);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateShop(Shop shop) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn
                    .prepareStatement("UPDATE shop SET npc_id=?, tag_name=?, type_shop=? WHERE id=?")) {
                stmt.setInt(1, shop.npcId);
                stmt.setString(2, shop.tagName);
                stmt.setInt(3, shop.typeShop);
                stmt.setInt(4, shop.id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteShop(int id) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM shop WHERE id=?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<NpcTemplate> getNpcs() {
        List<NpcTemplate> npcs = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM npc_template")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    npcs.add(new NpcTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return npcs;
    }

    // --- Tab Shop ---
    public int countTabsByShopId(int shopId) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM tab_shop WHERE shop_id = ?")) {
                stmt.setInt(1, shopId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<TabShop> getTabsByShopId(int shopId) {
        List<TabShop> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tab_shop WHERE shop_id = ?")) {
                stmt.setInt(1, shopId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new TabShop(rs.getInt("id"), rs.getInt("shop_id"), rs.getString("tab_name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addTab(TabShop tab, int tabIndex) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO tab_shop (shop_id, tab_name, tab_index, items) VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, tab.shopId);
                stmt.setString(2, tab.tabName);
                stmt.setInt(3, tabIndex);
                stmt.setString(4, "[]");
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTab(TabShop tab) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE tab_shop SET tab_name = ? WHERE id = ?")) {
                stmt.setString(1, tab.tabName);
                stmt.setInt(2, tab.id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteTab(int id) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM tab_shop WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TabShop> getTabsByShopTagName(String tagName) {
        List<TabShop> tabs = new ArrayList<>();
        try {
            Connection conn = getConnection();
            // 1. Get shop id
            int shopId = -1;
            try (PreparedStatement sStmt = conn.prepareStatement("SELECT id FROM shop WHERE tag_name = ?")) {
                sStmt.setString(1, tagName);
                ResultSet rs = sStmt.executeQuery();
                if (rs.next())
                    shopId = rs.getInt(1);
            }
            if (shopId == -1)
                return tabs;

            // 2. Get tabs
            try (PreparedStatement tStmt = conn
                    .prepareStatement("SELECT id, tab_name FROM tab_shop WHERE shop_id = ?")) {
                tStmt.setInt(1, shopId);
                ResultSet rs = tStmt.executeQuery();
                while (rs.next()) {
                    tabs.add(new TabShop(rs.getInt("id"), shopId, rs.getString("tab_name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tabs;
    }

    // --- Items ---
    public List<ItemTemplate> getItemTemplates(String search) {
        List<ItemTemplate> items = new ArrayList<>();
        try {
            Connection conn = getConnection();
            String query = "SELECT id, name FROM item_template";
            if (search != null && !search.isEmpty()) {
                query += " WHERE name LIKE ?";
            }
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                if (search != null && !search.isEmpty()) {
                    stmt.setString(1, "%" + search + "%");
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    items.add(new ItemTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<ItemOptionTemplate> getItemOptionTemplates() {
        List<ItemOptionTemplate> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM item_option_template")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new ItemOptionTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static class ItemResult {
        public List<ItemData> rawItems;
        public List<DisplayItem> displayItems;
    }

    public ItemResult getItemDataAndDisplayByTabId(int tabId) {
        ItemResult result = new ItemResult();
        result.rawItems = new ArrayList<>();
        result.displayItems = new ArrayList<>();

        try {
            Connection conn = getConnection();
            // 1. Get JSON
            String jsonStr = "";
            try (PreparedStatement stmt = conn.prepareStatement("SELECT items FROM tab_shop WHERE id = ?")) {
                stmt.setInt(1, tabId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    jsonStr = rs.getString("items");
                }
            }
            if (jsonStr == null || jsonStr.isEmpty())
                return result;

            // 2. Load ItemNames and OptionNames
            Map<Integer, String> itemNames = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM item_template")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    itemNames.put(rs.getInt("id"), rs.getString("name"));
            }

            Map<Integer, String> optionNames = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM item_option_template")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    optionNames.put(rs.getInt("id"), rs.getString("name"));
            }

            Map<Integer, String> sellTypeNames = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM type_sell_item_shop")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    sellTypeNames.put(rs.getInt("id"), rs.getString("name"));
            } catch (Exception e) {
                // Ignore if table doesn't exist just in case
            }

            // 3. Parse JSON
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(jsonStr);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                for (Object itemObj : arr) {
                    JSONObject itemJson = (JSONObject) itemObj;

                    ItemData data = new ItemData();
                    data.cost = getInt(itemJson, "cost");
                    data.type_sell = getInt(itemJson, "type_sell");
                    data.is_new = getBool(itemJson, "is_new");
                    data.is_sell = getBool(itemJson, "is_sell");
                    data.temp_id = getInt(itemJson, "temp_id");
                    data.item_spec = getInt(itemJson, "item_spec");

                    DisplayItem display = new DisplayItem();
                    display.id = data.temp_id;
                    display.name = itemNames.getOrDefault(data.temp_id, "Item " + data.temp_id);
                    display.cost = data.cost;
                    display.sellType = data.type_sell;

                    JSONArray optionsArr = (JSONArray) itemJson.get("options");
                    if (optionsArr != null) {
                        for (Object optObj : optionsArr) {
                            JSONObject optJson = (JSONObject) optObj;
                            ItemOption opt = new ItemOption();
                            opt.id = getInt(optJson, "id");
                            opt.param = getInt(optJson, "param");
                            data.options.add(opt);

                            display.options.add(new DisplayItem.ItemOptionDisplay(
                                    opt.id,
                                    optionNames.getOrDefault(opt.id, "Option " + opt.id),
                                    opt.param));
                        }
                    }

                    // Populate sellTypeName
                    if (sellTypeNames.containsKey(data.type_sell)) {
                        display.sellTypeName = sellTypeNames.get(data.type_sell);
                    } else if (data.type_sell == 0) {
                        display.sellTypeName = "Vàng";
                    } else {
                        // Debug log
                        System.out.println(
                                "DEBUG: Missing sellType: " + data.type_sell + " in map: " + sellTypeNames.keySet());
                        display.sellTypeName = String.valueOf(data.type_sell);
                    }

                    result.rawItems.add(data);
                    result.displayItems.add(display);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<TypeSell> getTypeSells() {
        List<TypeSell> list = new ArrayList<>();
        boolean hasGold = false;
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM type_sell_item_shop")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    list.add(new TypeSell(id, rs.getString("name")));
                    if (id == 0)
                        hasGold = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!hasGold) {
            list.add(0, new TypeSell(0, "Vàng"));
        }

        // Optional: Sort by ID
        list.sort((o1, o2) -> Integer.compare(o1.id, o2.id));

        return list;
    }

    public void updateItemsJson(int tabId, List<ItemData> items) {
        JSONArray jsonArr = new JSONArray();
        for (ItemData item : items) {
            JSONObject itemObj = new JSONObject();
            itemObj.put("cost", item.cost);
            itemObj.put("type_sell", item.type_sell);
            itemObj.put("is_new", item.is_new);
            itemObj.put("is_sell", item.is_sell);
            itemObj.put("temp_id", item.temp_id);
            itemObj.put("item_spec", item.item_spec);

            JSONArray optArr = new JSONArray();
            if (item.options != null) {
                for (ItemOption opt : item.options) {
                    JSONObject optObj = new JSONObject();
                    optObj.put("id", opt.id);
                    optObj.put("param", opt.param);
                    optArr.add(optObj);
                }
            }
            itemObj.put("options", optArr);
            jsonArr.add(itemObj);
        }

        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE tab_shop SET items = ? WHERE id = ?")) {
                stmt.setString(1, jsonArr.toJSONString());
                stmt.setInt(2, tabId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getInt(JSONObject obj, String key) {
        if (!obj.containsKey(key))
            return 0;
        Object val = obj.get(key);
        if (val instanceof Long)
            return ((Long) val).intValue();
        if (val instanceof Integer)
            return (Integer) val;
        return 0;
    }

    private boolean getBool(JSONObject obj, String key) {
        if (!obj.containsKey(key))
            return false;
        Object val = obj.get(key);
        if (val instanceof Boolean)
            return (Boolean) val;
        return false;
    }

    // --- Giftcode ---
    public List<Giftcode> getGiftcodes() {
        List<Giftcode> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM giftcode ORDER BY id ASC")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Giftcode gc = new Giftcode();
                    gc.id = rs.getInt("id");
                    gc.code = rs.getString("code");
                    gc.countLeft = rs.getInt("count_left");
                    gc.detail = rs.getString("detail");
                    gc.dateCreate = rs.getTimestamp("datecreate");
                    gc.expired = rs.getTimestamp("expired");
                    gc.type = rs.getInt("type");
                    list.add(gc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addGiftcode(Giftcode gc) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO giftcode (code, count_left, detail, datecreate, expired, type) VALUES (?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, gc.code);
                stmt.setInt(2, gc.countLeft);
                stmt.setString(3, gc.detail);
                stmt.setTimestamp(4, gc.dateCreate);
                stmt.setTimestamp(5, gc.expired);
                stmt.setInt(6, gc.type);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGiftcode(Giftcode gc) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE giftcode SET code=?, count_left=?, detail=?, expired=?, type=? WHERE id=?")) {
                stmt.setString(1, gc.code);
                stmt.setInt(2, gc.countLeft);
                stmt.setString(3, gc.detail);
                stmt.setTimestamp(4, gc.expired);
                stmt.setInt(5, gc.type);
                stmt.setInt(6, gc.id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteGiftcode(int id) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM giftcode WHERE id=?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String formatGiftcodeDetail(String detailJson) {
        if (detailJson == null || detailJson.trim().isEmpty() || detailJson.equals("[]"))
            return "";
        StringBuilder sb = new StringBuilder();
        try {
            Connection conn = getConnection();
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(detailJson);
            if (obj instanceof JSONArray) {
                JSONArray arr = (JSONArray) obj;
                int idx = 1;
                for (Object o : arr) {
                    JSONObject item = (JSONObject) o;
                    int tempId = getInt(item, "temp_id");
                    int qty = getInt(item, "quantity");

                    String itemName = "Item " + tempId;
                    try (PreparedStatement s = conn.prepareStatement("SELECT name FROM item_template WHERE id = ?")) {
                        s.setInt(1, tempId);
                        ResultSet rs = s.executeQuery();
                        if (rs.next())
                            itemName = rs.getString("name");
                    }

                    sb.append(idx).append(". ").append(itemName).append("\n");
                    sb.append("   - Số lượng: ").append(qty).append("\n");

                    if (item.containsKey("options")) {
                        JSONArray opts = (JSONArray) item.get("options");
                        if (opts != null && !opts.isEmpty()) {
                            sb.append("   - Chỉ số:\n");
                            for (Object optObj : opts) {
                                JSONObject opt = (JSONObject) optObj;
                                int id = getInt(opt, "id");
                                int param = getInt(opt, "param");

                                String optName = "Option " + id;
                                try (PreparedStatement s = conn
                                        .prepareStatement("SELECT name FROM item_option_template WHERE id = ?")) {
                                    s.setInt(1, id);
                                    ResultSet rs = s.executeQuery();
                                    if (rs.next())
                                        optName = rs.getString("name");
                                }
                                sb.append("     + ").append(optName).append(": ").append(param).append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                    idx++;
                }
            }
        } catch (Exception e) {
            return "Error parsing: " + e.getMessage();
        }
        return sb.toString().trim();
    }

    public int getIconId(int tempId) {
        int iconId = -1;
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT icon_id FROM item_template WHERE id = ?")) {
                stmt.setInt(1, tempId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    iconId = rs.getInt("icon_id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iconId;
    }

    public String getOptionName(int id) {
        String name = "Option " + id;
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM item_option_template WHERE id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    name = rs.getString("name");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    public String getItemName(int id) {
        String name = "Item " + id;
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM item_template WHERE id = ?")) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    name = rs.getString("name");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    // --- NPC Template with full info ---
    public static class NpcFullInfo {
        public int id;
        public String name;
        public int head;
        public int body;
        public int leg;
        public int avatar;

        public NpcFullInfo(int id, String name, int head, int body, int leg, int avatar) {
            this.id = id;
            this.name = name;
            this.head = head;
            this.body = body;
            this.leg = leg;
            this.avatar = avatar;
        }
    }

    public List<NpcFullInfo> getNpcsWithFullInfo() {
        List<NpcFullInfo> npcs = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, NAME, head, body, leg, avatar FROM npc_template ORDER BY id ASC")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    npcs.add(new NpcFullInfo(
                            rs.getInt("id"),
                            rs.getString("NAME"),
                            rs.getInt("head"),
                            rs.getInt("body"),
                            rs.getInt("leg"),
                            rs.getInt("avatar")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return npcs;
    }

    public NpcFullInfo getNpcById(int npcId) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, NAME, head, body, leg, avatar FROM npc_template WHERE id = ?")) {
                stmt.setInt(1, npcId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new NpcFullInfo(
                            rs.getInt("id"),
                            rs.getString("NAME"),
                            rs.getInt("head"),
                            rs.getInt("body"),
                            rs.getInt("leg"),
                            rs.getInt("avatar"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Part Data ---
    public static class PartData {
        public int id;
        public int type; // 0=head, 1=body, 2=leg
        public String data; // JSON data

        public PartData(int id, int type, String data) {
            this.id = id;
            this.type = type;
            this.data = data;
        }
    }

    public PartData getPartData(int partId) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, TYPE, DATA FROM part WHERE id = ?")) {
                stmt.setInt(1, partId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new PartData(
                            rs.getInt("id"),
                            rs.getInt("TYPE"),
                            rs.getString("DATA"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getMaxPartId() {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT MAX(id) FROM part")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getMaxNpcId() {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT MAX(id) FROM npc_template")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void addNewNpc(int id, String name) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO npc_template (id, NAME, head, body, leg, avatar) VALUES (?, ?, -1, -1, -1, -1)")) {
                stmt.setInt(1, id);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNpcName(int id, String newName) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE npc_template SET NAME = ? WHERE id = ?")) {
                stmt.setString(1, newName);
                stmt.setInt(2, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNpcAvatar(int id, int avatarId) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE npc_template SET avatar = ? WHERE id = ?")) {
                stmt.setInt(1, avatarId);
                stmt.setInt(2, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertOrUpdatePart(int id, int type, String data) {
        try {
            Connection conn = getConnection();
            // Check if exists
            boolean exists = false;
            try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM part WHERE id = ?")) {
                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                if (rs.next())
                    exists = true;
            }

            if (exists) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE part SET TYPE = ?, DATA = ? WHERE id = ?")) {
                    stmt.setInt(1, type);
                    stmt.setString(2, data);
                    stmt.setInt(3, id);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO part (id, TYPE, DATA) VALUES (?, ?, ?)")) {
                    stmt.setInt(1, id);
                    stmt.setInt(2, type);
                    stmt.setString(3, data);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNpcTemplateParts(int npcId, int head, int body, int leg) {
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE npc_template SET head = ?, body = ?, leg = ? WHERE id = ?")) {
                stmt.setInt(1, head);
                stmt.setInt(2, body);
                stmt.setInt(3, leg);
                stmt.setInt(4, npcId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Find part IDs that contain a specific icon ID in their data.
     * Searches for the iconId in the JSON DATA column of the part table.
     */
    public List<Integer> findPathsByIconId(int iconId) {
        List<Integer> pathIds = new ArrayList<>();
        try {
            Connection conn = getConnection();
            // Search for iconId in the JSON data - look for pattern like [iconId,
            String searchPattern = "[" + iconId + ",";
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id FROM part WHERE DATA LIKE ?")) {
                stmt.setString(1, "%" + searchPattern + "%");
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    pathIds.add(rs.getInt("id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pathIds;
    }

    /**
     * Get the minimum (most negative) Boss ID from npc_template.
     * Boss IDs are negative integers.
     * Returns 0 if no boss found.
     */
    public int getMinBossId() {
        try {
            Connection conn = getConnection();
            // Query for minimum ID where ID is negative (boss IDs are negative)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT MIN(id) as min_id FROM npc_template WHERE id < 0")) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int minId = rs.getInt("min_id");
                    if (!rs.wasNull()) {
                        return minId;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // No boss found, return 0 as default
    }

    // --- Skill Template ---
    public static class SkillTemplate {
        public int id;
        public String name;

        public SkillTemplate(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public List<SkillTemplate> getAllSkills() {
        List<SkillTemplate> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM skill_template ORDER BY name")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new SkillTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading skills: " + e.getMessage());
        }
        return list;
    }
}
