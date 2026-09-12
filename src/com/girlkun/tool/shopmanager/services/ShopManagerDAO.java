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
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;

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

    // Get active connection or reconnect - Synchronized for thread safety
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        // Safe check validation (MySQL may drop idle connections)
        try {
            if (!connection.isValid(2)) {
                // If invalid, try to reconnect directly
                String url = config.toConnectionString();
                connection = DriverManager.getConnection(url, config.user, config.password);
            }
        } catch (Exception e) {
            // Fallback: force reconnect
            String url = config.toConnectionString();
            connection = DriverManager.getConnection(url, config.user, config.password);
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
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tab_shop WHERE shop_id = ?")) {
                    stmt.setInt(1, shopId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        list.add(new TabShop(rs.getInt("id"), rs.getInt("shop_id"), rs.getString("NAME")));
                    }
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tab_shop WHERE shop_id = ?")) {
                    stmt.setInt(1, shopId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        list.add(new TabShop(rs.getInt("id"), rs.getInt("shop_id"), rs.getString("tab_name")));
                    }
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
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO tab_shop (shop_id, NAME) VALUES (?, ?)")) {
                    stmt.setInt(1, tab.shopId);
                    stmt.setString(2, tab.tabName);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO tab_shop (shop_id, tab_name, tab_index, items) VALUES (?, ?, ?, ?)")) {
                    stmt.setInt(1, tab.shopId);
                    stmt.setString(2, tab.tabName);
                    stmt.setInt(3, tabIndex);
                    stmt.setString(4, "[]");
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTab(TabShop tab) {
        try {
            Connection conn = getConnection();
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE tab_shop SET NAME = ? WHERE id = ?")) {
                    stmt.setString(1, tab.tabName);
                    stmt.setInt(2, tab.id);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE tab_shop SET tab_name = ? WHERE id = ?")) {
                    stmt.setString(1, tab.tabName);
                    stmt.setInt(2, tab.id);
                    stmt.executeUpdate();
                }
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
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement tStmt = conn
                        .prepareStatement("SELECT id, NAME FROM tab_shop WHERE shop_id = ?")) {
                    tStmt.setInt(1, shopId);
                    ResultSet rs = tStmt.executeQuery();
                    while (rs.next()) {
                        tabs.add(new TabShop(rs.getInt("id"), shopId, rs.getString("NAME")));
                    }
                }
            } else {
                try (PreparedStatement tStmt = conn
                        .prepareStatement("SELECT id, tab_name FROM tab_shop WHERE shop_id = ?")) {
                    tStmt.setInt(1, shopId);
                    ResultSet rs = tStmt.executeQuery();
                    while (rs.next()) {
                        tabs.add(new TabShop(rs.getInt("id"), shopId, rs.getString("tab_name")));
                    }
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
            // 1. Get JSON (if TOMAHAWK)
            String jsonStr = "";
            if (config.dbType == DbConfig.DB_TOMAHAWK) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT items FROM tab_shop WHERE id = ?")) {
                    stmt.setInt(1, tabId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        jsonStr = rs.getString("items");
                    }
                }
                if (jsonStr == null || jsonStr.isEmpty())
                    return result;
            }

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
            String sellTypeTable = (config.dbType == DbConfig.DB_NRO_ARN) ? "shop_sell_type" : "type_sell_item_shop";
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM " + sellTypeTable)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    sellTypeNames.put(rs.getInt("id"), rs.getString("name"));
            } catch (Exception e) {
                // Ignore if table doesn't exist just in case
            }

            // 3. Parse JSON or DB
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM item_shop WHERE tab_id = ? ORDER BY sort_order ASC, id ASC")) {
                    stmt.setInt(1, tabId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        int itemShopId = rs.getInt("id");
                        ItemData data = new ItemData();
                        data.temp_id = rs.getInt("temp_id");
                        data.is_new = rs.getBoolean("is_new");
                        data.is_sell = rs.getBoolean("is_sell");
                        data.type_sell = rs.getInt("type_sell");
                        data.cost = rs.getInt("cost");
                        data.item_spec = rs.getInt("icon_spec");

                        DisplayItem display = new DisplayItem();
                        display.id = data.temp_id;
                        display.name = itemNames.getOrDefault(data.temp_id, "Item " + data.temp_id);
                        display.cost = data.cost;
                        display.sellType = data.type_sell;

                        try (PreparedStatement optStmt = conn.prepareStatement("SELECT option_id, param FROM item_shop_option WHERE item_shop_id = ?")) {
                            optStmt.setInt(1, itemShopId);
                            ResultSet optRs = optStmt.executeQuery();
                            while (optRs.next()) {
                                ItemOption opt = new ItemOption();
                                opt.id = optRs.getInt("option_id");
                                opt.param = optRs.getInt("param");
                                data.options.add(opt);

                                display.options.add(new DisplayItem.ItemOptionDisplay(
                                        opt.id,
                                        optionNames.getOrDefault(opt.id, "Option " + opt.id),
                                        opt.param));
                            }
                        }

                        if (sellTypeNames.containsKey(data.type_sell)) {
                            display.sellTypeName = sellTypeNames.get(data.type_sell);
                        } else if (data.type_sell == 0) {
                            display.sellTypeName = "Vàng";
                        } else {
                            display.sellTypeName = String.valueOf(data.type_sell);
                        }

                        result.rawItems.add(data);
                        result.displayItems.add(display);
                    }
                }
            } else {
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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<TypeSell> getTypeSells() {
        List<TypeSell> list = new ArrayList<>();
        if (config.dbType == DbConfig.DB_NRO_ARN) {
            try {
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM shop_sell_type ORDER BY id")) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        list.add(new TypeSell(rs.getInt("id"), rs.getString("name")));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (list.isEmpty()) {
                list.add(new TypeSell(0, "Vàng"));
            }
            return list;
        }

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
        if (config.dbType == DbConfig.DB_NRO_ARN) {
            try {
                Connection conn = getConnection();
                // 1. Get all item_shop ids for this tab_id
                List<Integer> itemShopIds = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM item_shop WHERE tab_id = ?")) {
                    stmt.setInt(1, tabId);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        itemShopIds.add(rs.getInt("id"));
                    }
                }
                
                // 2. Delete from item_shop_option
                if (!itemShopIds.isEmpty()) {
                    StringBuilder placeholders = new StringBuilder("?");
                    for (int i = 1; i < itemShopIds.size(); i++) {
                        placeholders.append(",?");
                    }
                    try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM item_shop_option WHERE item_shop_id IN (" + placeholders + ")")) {
                        for (int i = 0; i < itemShopIds.size(); i++) {
                            stmt.setInt(i + 1, itemShopIds.get(i));
                        }
                        stmt.executeUpdate();
                    }
                }

                // 3. Delete from item_shop
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM item_shop WHERE tab_id = ?")) {
                    stmt.setInt(1, tabId);
                    stmt.executeUpdate();
                }

                // 4. Insert items
                int sortOrder = 0;
                for (ItemData item : items) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO item_shop (tab_id, temp_id, is_new, is_sell, type_sell, cost, icon_spec, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        stmt.setInt(1, tabId);
                        stmt.setInt(2, item.temp_id);
                        stmt.setBoolean(3, item.is_new);
                        stmt.setBoolean(4, item.is_sell);
                        stmt.setInt(5, item.type_sell);
                        stmt.setInt(6, item.cost);
                        stmt.setInt(7, item.item_spec);
                        stmt.setInt(8, sortOrder++);
                        stmt.executeUpdate();

                        ResultSet rs = stmt.getGeneratedKeys();
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            if (item.options != null && !item.options.isEmpty()) {
                                try (PreparedStatement optStmt = conn.prepareStatement("INSERT INTO item_shop_option (item_shop_id, option_id, param) VALUES (?, ?, ?)")) {
                                    for (ItemOption opt : item.options) {
                                        optStmt.setInt(1, newId);
                                        optStmt.setInt(2, opt.id);
                                        optStmt.setInt(3, opt.param);
                                        optStmt.addBatch();
                                    }
                                    optStmt.executeBatch();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
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
                    if (config.dbType != DbConfig.DB_NRO_ARN) {
                        gc.type = rs.getInt("type");
                    } else {
                        gc.type = 0;
                    }
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
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO giftcode (code, count_left, detail, datecreate, expired) VALUES (?, ?, ?, ?, ?)")) {
                    stmt.setString(1, gc.code);
                    stmt.setInt(2, gc.countLeft);
                    stmt.setString(3, gc.detail);
                    stmt.setTimestamp(4, gc.dateCreate);
                    stmt.setTimestamp(5, gc.expired);
                    stmt.executeUpdate();
                }
            } else {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateGiftcode(Giftcode gc) {
        try {
            Connection conn = getConnection();
            if (config.dbType == DbConfig.DB_NRO_ARN) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE giftcode SET code=?, count_left=?, detail=?, expired=? WHERE id=?")) {
                    stmt.setString(1, gc.code);
                    stmt.setInt(2, gc.countLeft);
                    stmt.setString(3, gc.detail);
                    stmt.setTimestamp(4, gc.expired);
                    stmt.setInt(5, gc.id);
                    stmt.executeUpdate();
                }
            } else {
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

    // --- Cải Trang (Disguise) Template ---
    public static class CaiTrangTemplate {
        public int id;
        public String name;
        public int iconId;
        public int head;
        public int body;
        public int leg;

        public CaiTrangTemplate(int id, String name, int iconId, int head, int body, int leg) {
            this.id = id;
            this.name = name;
            this.iconId = iconId;
            this.head = head;
            this.body = body;
            this.leg = leg;
        }

        @Override
        public String toString() {
            return name + " (ID: " + id + ")";
        }
    }

    public List<CaiTrangTemplate> getAllCaiTrang() {
        List<CaiTrangTemplate> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name, icon_id, head, body, leg FROM item_template WHERE type = 5 ORDER BY id ASC")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new CaiTrangTemplate(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("icon_id"),
                            rs.getInt("head"),
                            rs.getInt("body"),
                            rs.getInt("leg")));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading cai trang: " + e.getMessage());
        }
        return list;
    }

    // --- Mob Template ---
    public static class MobTemplate {
        public int id;
        public String name;

        public MobTemplate(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public List<MobTemplate> getMobTemplates() {
        List<MobTemplate> list = new ArrayList<>();
        // Add "All" option first
        list.add(new MobTemplate(-1, "Tất cả quái"));
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name FROM mob_template ORDER BY name")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new MobTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading mob templates: " + e.getMessage());
        }
        return list;
    }

    // --- Map Template ---
    public static class MapTemplate {
        public int id;
        public String name;

        public MapTemplate(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    public List<MapTemplate> getMapTemplates() {
        List<MapTemplate> list = new ArrayList<>();
        // Add "All" option first
        list.add(new MapTemplate(-1, "Tất cả map"));
        try {
            Connection conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, name FROM map_template ORDER BY name")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(new MapTemplate(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading map templates: " + e.getMessage());
        }
        return list;
    }

    public boolean isBossTemplateTableExists(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM boss_template LIMIT 1")) {
            stmt.executeQuery();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getNextBossId() {
        try {
            Connection conn = getConnection();
            if (isBossTemplateTableExists(conn)) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT MAX(id) as max_id FROM boss_template")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int maxId = rs.getInt("max_id");
                            if (!rs.wasNull()) {
                                return maxId + 1;
                            }
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("SELECT MIN(boss_id) as min_id FROM boss_config")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int minId = rs.getInt("min_id");
                            if (!rs.wasNull() && minId < 0) {
                                return minId - 1;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public List<BossConfig> getAllBossConfigs() {
        List<BossConfig> list = new ArrayList<>();
        try {
            Connection conn = getConnection();
            if (isBossTemplateTableExists(conn)) {
                Map<Integer, BossConfig> map = new LinkedHashMap<>();
                // 1. boss_template
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_template ORDER BY id ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        BossConfig b = new BossConfig();
                        b.bossId = rs.getInt("id");
                        b.bossName = rs.getString("name");
                        b.bossType = rs.getString("type");
                        b.subType = rs.getString("sub_type");
                        b.gender = rs.getByte("gender");
                        b.enabled = rs.getBoolean("enabled");
                        b.spawnCount = rs.getInt("spawn_count");
                        b.respawnDelay = rs.getInt("respawn_delay");
                        b.despawnTimeout = rs.getInt("despawn_timeout");
                        b.isNotify = rs.getBoolean("is_notify");
                        b.isZone01SpawnDisabled = rs.getBoolean("is_zone_0_1_disabled");
                        b.requireTaskId = (Integer) rs.getObject("require_task_id");
                        b.extraConfig = rs.getString("extra_config");
                        b.secondsRest = b.respawnDelay;
                        b.isNotifyDisabled = !b.isNotify;
                        map.put(b.bossId, b);
                    }
                }

                // 2. boss_form
                Map<Integer, BossFormConfig> formsById = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_form ORDER BY boss_id ASC, form_order ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        BossFormConfig form = new BossFormConfig();
                        form.id = rs.getInt("id");
                        form.bossId = rs.getInt("boss_id");
                        form.formOrder = rs.getInt("form_order");
                        form.name = rs.getString("name");
                        form.hpMin = rs.getLong("hp_min");
                        form.hpMax = rs.getLong("hp_max");
                        form.dame = rs.getInt("dame");
                        form.outfitHead = rs.getShort("outfit_head");
                        form.outfitBody = rs.getShort("outfit_body");
                        form.outfitLeg = rs.getShort("outfit_leg");
                        form.outfitBag = rs.getShort("outfit_bag");
                        form.outfitAura = rs.getShort("outfit_aura");
                        form.outfitEff = rs.getShort("outfit_eff");
                        form.textStart = rs.getString("text_start");
                        form.textMid = rs.getString("text_mid");
                        form.textEnd = rs.getString("text_end");

                        formsById.put(form.id, form);
                        BossConfig b = map.get(form.bossId);
                        if (b != null) {
                            b.forms.add(form);
                        }
                    }
                }

                // 3. boss_skill
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_skill ORDER BY form_id ASC, id ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        BossSkillConfig sk = new BossSkillConfig(
                                rs.getInt("id"),
                                rs.getInt("form_id"),
                                rs.getInt("skill_id"),
                                rs.getInt("skill_level"),
                                rs.getInt("cooldown")
                        );
                        BossFormConfig form = formsById.get(sk.formId);
                        if (form != null) {
                            form.skills.add(sk);
                        }
                    }
                }

                // 4. boss_map
                Map<Integer, List<String>> mapsByBoss = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_map ORDER BY boss_id ASC, id ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int bid = rs.getInt("boss_id");
                        int mid = rs.getInt("map_id");
                        mapsByBoss.computeIfAbsent(bid, k -> new ArrayList<>()).add(String.valueOf(mid));
                    }
                }
                for (Map.Entry<Integer, List<String>> entry : mapsByBoss.entrySet()) {
                    BossConfig b = map.get(entry.getKey());
                    if (b != null) {
                        b.mapJoin = String.join(", ", entry.getValue());
                    }
                }

                // 5. boss_appear_together
                Map<Integer, List<String>> togetherByBoss = new HashMap<>();
                Map<Integer, Integer> parentBySubBoss = new HashMap<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_appear_together ORDER BY boss_id ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int bid = rs.getInt("boss_id");
                        int sid = rs.getInt("sub_boss_id");
                        togetherByBoss.computeIfAbsent(bid, k -> new ArrayList<>()).add(String.valueOf(sid));
                        parentBySubBoss.put(sid, bid);
                    }
                }
                for (Map.Entry<Integer, List<String>> entry : togetherByBoss.entrySet()) {
                    BossConfig b = map.get(entry.getKey());
                    if (b != null) {
                        b.bossesAppearTogether = String.join(", ", entry.getValue());
                    }
                }
                for (Map.Entry<Integer, Integer> entry : parentBySubBoss.entrySet()) {
                    BossConfig sub = map.get(entry.getKey());
                    BossConfig parent = map.get(entry.getValue());
                    if (sub != null) {
                        sub.parentBossId = entry.getValue();
                        if (parent != null) {
                            sub.parentBossName = parent.bossName;
                        }
                    }
                }

                // 6. boss_reward
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_reward ORDER BY boss_id ASC, id ASC");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        BossRewardConfig r = new BossRewardConfig(
                                rs.getInt("id"),
                                rs.getInt("boss_id"),
                                rs.getInt("item_id"),
                                rs.getInt("quantity_min"),
                                rs.getInt("quantity_max"),
                                rs.getDouble("rate"),
                                rs.getString("item_options"),
                                rs.getInt("event_point"),
                                rs.getInt("active_point")
                        );
                        BossConfig b = map.get(r.bossId);
                        if (b != null) {
                            b.rewards.add(r);
                        }
                    }
                }

                // Populate compatibility fields
                for (BossConfig b : map.values()) {
                    if (b.forms.isEmpty()) {
                        BossFormConfig def = new BossFormConfig(0, b.bossName);
                        b.forms.add(def);
                    }
                    BossFormConfig f0 = b.forms.get(0);
                    b.dame = f0.dame;
                    b.hp = (f0.hpMin == f0.hpMax) ? String.valueOf(f0.hpMin) : (f0.hpMin + ", " + f0.hpMax);
                    b.outfit = f0.outfitHead + "," + f0.outfitBody + "," + f0.outfitLeg + "," + f0.outfitBag + "," + f0.outfitAura + "," + f0.outfitEff;
                    b.textS = f0.textStart;
                    b.textM = f0.textMid;
                    b.textE = f0.textEnd;
                    list.add(b);
                }
            } else {
                // Fallback for boss_config
                try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM boss_config ORDER BY boss_id, level_index")) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        BossConfig b = new BossConfig();
                        b.bossId = rs.getInt("boss_id");
                        b.bossName = rs.getString("boss_name");
                        b.gender = rs.getByte("gender");
                        b.outfit = rs.getString("outfit");
                        b.dame = rs.getLong("dame");
                        b.hp = rs.getString("hp");
                        b.mapJoin = rs.getString("map_join");
                        b.skills = rs.getString("skills");
                        b.textS = rs.getString("text_s");
                        b.textM = rs.getString("text_m");
                        b.textE = rs.getString("text_e");
                        b.secondsRest = rs.getInt("seconds_rest");
                        b.respawnDelay = b.secondsRest;
                        b.appearType = rs.getByte("appear_type");
                        b.bossesAppearTogether = rs.getString("bosses_appear_together");
                        b.levelIndex = rs.getByte("level_index");
                        b.bossType = rs.getString("boss_type");
                        b.isNotifyDisabled = rs.getBoolean("is_notify_disabled");
                        b.isNotify = !b.isNotifyDisabled;
                        b.isZone01SpawnDisabled = rs.getBoolean("is_zone01_spawn_disabled");
                        b.spawnCount = rs.getInt("spawn_count");
                        b.rewardConfig = rs.getString("reward_config");
                        b.enabled = rs.getBoolean("enabled");
                        list.add(b);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading boss configs: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public void saveBossConfig(BossConfig b) {
        try {
            Connection conn = getConnection();
            if (isBossTemplateTableExists(conn)) {
                conn.setAutoCommit(false);
                try {
                    // 1. boss_template
                    String sqlTemplate = "REPLACE INTO boss_template (" +
                            "id, name, type, sub_type, gender, enabled, spawn_count, " +
                            "respawn_delay, despawn_timeout, is_notify, is_zone_0_1_disabled, " +
                            "require_task_id, extra_config) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlTemplate)) {
                        stmt.setInt(1, b.bossId);
                        stmt.setString(2, b.bossName);
                        stmt.setString(3, b.bossType != null ? b.bossType : "NORMAL");
                        stmt.setString(4, b.subType != null ? b.subType : "DEFAULT");
                        stmt.setByte(5, b.gender);
                        stmt.setBoolean(6, b.enabled);
                        stmt.setInt(7, b.spawnCount > 0 ? b.spawnCount : 1);
                        stmt.setInt(8, b.respawnDelay > 0 ? b.respawnDelay : 300);
                        stmt.setInt(9, b.despawnTimeout > 0 ? b.despawnTimeout : 900);
                        stmt.setBoolean(10, b.isNotify);
                        stmt.setBoolean(11, b.isZone01SpawnDisabled);
                        stmt.setObject(12, b.requireTaskId);
                        stmt.setString(13, b.extraConfig);
                        stmt.executeUpdate();
                    }

                    // 2. boss_map
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_map WHERE boss_id = ?")) {
                        ps.setInt(1, b.bossId);
                        ps.executeUpdate();
                    }
                    if (b.mapJoin != null && !b.mapJoin.trim().isEmpty()) {
                        String[] parts = b.mapJoin.split("[,;\\s]+");
                        String sqlMap = "INSERT INTO boss_map (boss_id, map_id, zone_id) VALUES (?, ?, -1)";
                        for (String p : parts) {
                            String s = p.trim();
                            if (!s.isEmpty()) {
                                try {
                                    int mapId = Integer.parseInt(s);
                                    try (PreparedStatement ps = conn.prepareStatement(sqlMap)) {
                                        ps.setInt(1, b.bossId);
                                        ps.setInt(2, mapId);
                                        ps.executeUpdate();
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }

                    // 3. boss_appear_together
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_appear_together WHERE boss_id = ?")) {
                        ps.setInt(1, b.bossId);
                        ps.executeUpdate();
                    }
                    if (b.bossesAppearTogether != null && !b.bossesAppearTogether.trim().isEmpty()) {
                        String[] parts = b.bossesAppearTogether.split("[,;\\s]+");
                        String sqlTogether = "INSERT IGNORE INTO boss_appear_together (boss_id, sub_boss_id) VALUES (?, ?)";
                        Set<Integer> validIds = new HashSet<>();
                        try (PreparedStatement psCheck = conn.prepareStatement("SELECT id FROM boss_template")) {
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                while (rsCheck.next()) validIds.add(rsCheck.getInt("id"));
                            }
                        }
                        Set<Integer> added = new HashSet<>();
                        for (String p : parts) {
                            String s = p.trim();
                            if (!s.isEmpty()) {
                                try {
                                    int subId = Integer.parseInt(s);
                                    if (validIds.contains(subId) && subId != b.bossId && !added.contains(subId)) {
                                        added.add(subId);
                                        try (PreparedStatement ps = conn.prepareStatement(sqlTogether)) {
                                            ps.setInt(1, b.bossId);
                                            ps.setInt(2, subId);
                                            ps.executeUpdate();
                                        }
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }

                    // 4. boss_form & boss_skill
                    List<Integer> oldFormIds = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM boss_form WHERE boss_id = ?")) {
                        ps.setInt(1, b.bossId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                oldFormIds.add(rs.getInt("id"));
                            }
                        }
                    }
                    for (int fid : oldFormIds) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_skill WHERE form_id = ?")) {
                            ps.setInt(1, fid);
                            ps.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_form WHERE boss_id = ?")) {
                        ps.setInt(1, b.bossId);
                        ps.executeUpdate();
                    }

                    if (b.forms != null && !b.forms.isEmpty()) {
                        String sqlForm = "INSERT INTO boss_form (boss_id, form_order, name, hp_min, hp_max, dame, " +
                                "outfit_head, outfit_body, outfit_leg, outfit_bag, outfit_aura, outfit_eff, " +
                                "text_start, text_mid, text_end) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        String sqlSkill = "INSERT INTO boss_skill (form_id, skill_id, skill_level, cooldown) VALUES (?, ?, ?, ?)";

                        for (int i = 0; i < b.forms.size(); i++) {
                            BossFormConfig form = b.forms.get(i);
                            form.bossId = b.bossId;
                            form.formOrder = i;

                            try (PreparedStatement ps = conn.prepareStatement(sqlForm, Statement.RETURN_GENERATED_KEYS)) {
                                ps.setInt(1, b.bossId);
                                ps.setInt(2, i);
                                ps.setString(3, (form.name != null && !form.name.trim().isEmpty()) ? form.name : b.bossName);
                                ps.setLong(4, form.hpMin);
                                ps.setLong(5, form.hpMax >= form.hpMin ? form.hpMax : form.hpMin);
                                ps.setInt(6, (int) form.dame);
                                ps.setShort(7, form.outfitHead);
                                ps.setShort(8, form.outfitBody);
                                ps.setShort(9, form.outfitLeg);
                                ps.setShort(10, form.outfitBag);
                                ps.setShort(11, form.outfitAura);
                                ps.setShort(12, form.outfitEff);
                                ps.setString(13, form.textStart != null ? form.textStart : "[]");
                                ps.setString(14, form.textMid != null ? form.textMid : "[]");
                                ps.setString(15, form.textEnd != null ? form.textEnd : "[]");
                                ps.executeUpdate();

                                try (ResultSet rsKey = ps.getGeneratedKeys()) {
                                    if (rsKey.next()) {
                                        int formId = rsKey.getInt(1);
                                        form.id = formId;

                                        if (form.skills != null) {
                                            for (BossSkillConfig sk : form.skills) {
                                                try (PreparedStatement psk = conn.prepareStatement(sqlSkill)) {
                                                    psk.setInt(1, formId);
                                                    psk.setInt(2, sk.skillId);
                                                    psk.setInt(3, sk.skillLevel);
                                                    psk.setInt(4, sk.cooldown);
                                                    psk.executeUpdate();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. boss_reward
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_reward WHERE boss_id = ?")) {
                        ps.setInt(1, b.bossId);
                        ps.executeUpdate();
                    }
                    if (b.rewards != null && !b.rewards.isEmpty()) {
                        String sqlReward = "INSERT INTO boss_reward (boss_id, item_id, quantity_min, quantity_max, rate, item_options, event_point, active_point) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        for (BossRewardConfig r : b.rewards) {
                            try (PreparedStatement ps = conn.prepareStatement(sqlReward)) {
                                ps.setInt(1, b.bossId);
                                ps.setInt(2, r.itemId);
                                ps.setInt(3, r.quantityMin > 0 ? r.quantityMin : 1);
                                ps.setInt(4, r.quantityMax >= r.quantityMin ? r.quantityMax : r.quantityMin);
                                ps.setDouble(5, r.rate > 0 ? r.rate : 100.0);
                                ps.setString(6, r.itemOptions != null ? r.itemOptions : "[]");
                                ps.setInt(7, r.eventPoint);
                                ps.setInt(8, r.activePoint);
                                ps.executeUpdate();
                            }
                        }
                    }

                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } else {
                // Fallback for boss_config
                String sql = "REPLACE INTO boss_config (" +
                        "boss_id, boss_name, gender, outfit, dame, hp, map_join, skills, " +
                        "text_s, text_m, text_e, seconds_rest, appear_type, bosses_appear_together, " +
                        "level_index, boss_type, is_notify_disabled, is_zone01_spawn_disabled, " +
                        "spawn_count, max_damage_per_hit, damage_divisor, damage_flat_reduction, " +
                        "dodge_rate, pierce_reverse, auto_leave_timeout, auto_leave_reset_on_player, " +
                        "auto_leave_random_min, auto_leave_random_max, append_random_name, " +
                        "done_chat_s_to_afk, skip_notify_at_level, skip_move_at_level, " +
                        "special_abilities, reward_config, custom_class, enabled) VALUES (" +
                        "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, b.bossId);
                    stmt.setString(2, b.bossName);
                    stmt.setByte(3, b.gender);
                    stmt.setString(4, b.outfit);
                    stmt.setLong(5, b.dame);
                    stmt.setString(6, b.hp);
                    stmt.setString(7, b.mapJoin);
                    stmt.setString(8, b.skills);
                    stmt.setString(9, b.textS);
                    stmt.setString(10, b.textM);
                    stmt.setString(11, b.textE);
                    stmt.setInt(12, b.respawnDelay > 0 ? b.respawnDelay : b.secondsRest);
                    stmt.setByte(13, b.appearType);
                    stmt.setString(14, b.bossesAppearTogether);
                    stmt.setByte(15, b.levelIndex);
                    stmt.setString(16, b.bossType);
                    stmt.setBoolean(17, !b.isNotify);
                    stmt.setBoolean(18, b.isZone01SpawnDisabled);
                    stmt.setInt(19, b.spawnCount);
                    stmt.setObject(20, b.maxDamagePerHit);
                    stmt.setObject(21, b.damageDivisor);
                    stmt.setObject(22, b.damageFlatReduction);
                    stmt.setObject(23, b.dodgeRate);
                    stmt.setBoolean(24, b.pierceReverse);
                    stmt.setObject(25, b.autoLeaveTimeout);
                    stmt.setBoolean(26, b.autoLeaveResetOnPlayer);
                    stmt.setObject(27, b.autoLeaveRandomMin);
                    stmt.setObject(28, b.autoLeaveRandomMax);
                    stmt.setBoolean(29, b.appendRandomName);
                    stmt.setBoolean(30, b.doneChatSToAfk);
                    stmt.setObject(31, b.skipNotifyAtLevel);
                    stmt.setObject(32, b.skipMoveAtLevel);
                    stmt.setString(33, b.specialAbilities);
                    stmt.setString(34, b.rewardConfig);
                    stmt.setString(35, b.customClass);
                    stmt.setBoolean(36, b.enabled);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.out.println("Error saving boss config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteBoss(int bossId) {
        try {
            Connection conn = getConnection();
            if (isBossTemplateTableExists(conn)) {
                conn.setAutoCommit(false);
                try {
                    // 1. Delete skills
                    List<Integer> formIds = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM boss_form WHERE boss_id = ?")) {
                        ps.setInt(1, bossId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) formIds.add(rs.getInt("id"));
                        }
                    }
                    for (int fid : formIds) {
                        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_skill WHERE form_id = ?")) {
                            ps.setInt(1, fid);
                            ps.executeUpdate();
                        }
                    }
                    // 2. Delete forms
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_form WHERE boss_id = ?")) {
                        ps.setInt(1, bossId);
                        ps.executeUpdate();
                    }
                    // 3. Delete maps
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_map WHERE boss_id = ?")) {
                        ps.setInt(1, bossId);
                        ps.executeUpdate();
                    }
                    // 4. Delete appear together
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_appear_together WHERE boss_id = ? OR sub_boss_id = ?")) {
                        ps.setInt(1, bossId);
                        ps.setInt(2, bossId);
                        ps.executeUpdate();
                    }
                    // 5. Delete rewards
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_reward WHERE boss_id = ?")) {
                        ps.setInt(1, bossId);
                        ps.executeUpdate();
                    }
                    // 6. Delete template
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_template WHERE id = ?")) {
                        ps.setInt(1, bossId);
                        ps.executeUpdate();
                    }
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM boss_config WHERE boss_id = ?")) {
                    ps.setInt(1, bossId);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.out.println("Error deleting boss: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getDbType() {
        return config != null ? config.dbType : DbConfig.DB_TOMAHAWK;
    }
}
