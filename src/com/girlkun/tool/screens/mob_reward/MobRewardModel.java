package com.girlkun.tool.screens.mob_reward;

import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện cho một cấu hình drop item từ Database
 * Tương ứng với bảng `mob_reward`
 */
public class MobRewardModel {

    public int id;
    public int mobId = -1; // -1 = tất cả quái
    public int mapId = -1; // -1 = tất cả map
    public int itemTemplateId; // ID item sẽ rơi
    public int rate = 100; // Tỉ lệ 1/rate
    public int quantityMin = 1; // Số lượng min
    public int quantityMax = 1; // Số lượng max
    public int gender = -1; // -1 = all, 0/1/2 = TDS/NM/XD
    public String eventKey = ""; // Tên sự kiện (CHRISTMAS, HALLOWEEN...)
    public String mapType = ""; // Loại map (MAP_COLD, MAP_SKH...)
    public String conditionType = ""; // Điều kiện đặc biệt
    public String optionsJson = ""; // JSON options
    public boolean isRandomRange; // Random từ itemTemplateId đến id + randomRange
    public int randomRange; // Phạm vi random
    public boolean notifyGlobal; // Thông báo toàn server
    public String description = ""; // Mô tả
    public boolean isActive = true; // Đang hoạt động

    // Item name for display (loaded from item_template)
    public String itemName = "";
    public String mobName = "";
    public String mapName = "";

    public MobRewardModel() {
    }

    @Override
    public String toString() {
        return String.format("[%d] Item: %s, Rate: 1/%d", id,
                itemName.isEmpty() ? String.valueOf(itemTemplateId) : itemName, rate);
    }

    /**
     * Get gender display name
     */
    public String getGenderName() {
        return switch (gender) {
            case 0 -> "Trái Đất";
            case 1 -> "Namếc";
            case 2 -> "Xayda";
            default -> "Tất cả";
        };
    }

    /**
     * Option item class
     */
    public static class ItemOption {
        public int id;
        public int param;

        public ItemOption(int id, int param) {
            this.id = id;
            this.param = param;
        }
    }

    /**
     * Parse options from JSON string
     */
    public List<ItemOption> parseOptions() {
        List<ItemOption> options = new ArrayList<>();
        if (optionsJson == null || optionsJson.isEmpty()) {
            return options;
        }
        try {
            String content = optionsJson.trim();
            if (content.startsWith("[")) {
                content = content.substring(1);
            }
            if (content.endsWith("]")) {
                content = content.substring(0, content.length() - 1);
            }

            if (content.isEmpty())
                return options;

            String[] objects = content.split("\\},\\{");

            for (String obj : objects) {
                obj = obj.replace("{", "").replace("}", "");

                int id = 0;
                int param = 0;

                String[] pairs = obj.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].replace("\"", "").trim();
                        String value = kv[1].replace("\"", "").trim();

                        if ("id".equals(key)) {
                            id = Integer.parseInt(value);
                        } else if ("param".equals(key)) {
                            param = Integer.parseInt(value);
                        }
                    }
                }

                if (id > 0 || param > 0) {
                    options.add(new ItemOption(id, param));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing options JSON: " + optionsJson);
        }
        return options;
    }
}
