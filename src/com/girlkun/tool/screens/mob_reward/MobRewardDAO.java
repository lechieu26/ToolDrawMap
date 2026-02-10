package com.girlkun.tool.screens.mob_reward;

import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO để truy cập bảng mob_reward trong database
 */
public class MobRewardDAO {

    private static MobRewardDAO instance;

    public static MobRewardDAO gI() {
        if (instance == null) {
            instance = new MobRewardDAO();
        }
        return instance;
    }

    /**
     * Lấy danh sách tất cả MobReward
     */
    public List<MobRewardModel> getAll() {
        List<MobRewardModel> list = new ArrayList<>();

        String sql = """
                SELECT mr.*,
                       COALESCE(it.name, '') as item_name,
                       COALESCE(mt.name, '') as mob_name,
                       COALESCE(map.name, '') as map_name
                FROM mob_reward mr
                LEFT JOIN item_template it ON mr.item_template_id = it.id
                LEFT JOIN mob_template mt ON mr.mob_id = mt.id
                LEFT JOIN map_template map ON mr.map_id = map.id
                ORDER BY mr.id DESC
                """;

        try (Connection con = ShopManagerDAO.gI().getConnection();
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                MobRewardModel m = new MobRewardModel();
                m.id = rs.getInt("id");
                m.mobId = rs.getInt("mob_id");
                m.mapId = rs.getInt("map_id");
                m.itemTemplateId = rs.getInt("item_template_id");
                m.rate = rs.getInt("rate");
                m.quantityMin = rs.getInt("quantity_min");
                m.quantityMax = rs.getInt("quantity_max");
                m.gender = rs.getInt("gender");
                m.eventKey = rs.getString("event_key");
                m.mapType = rs.getString("map_type");
                m.conditionType = rs.getString("condition_type");
                m.optionsJson = rs.getString("options_json");
                m.isRandomRange = rs.getBoolean("is_random_range");
                m.randomRange = rs.getInt("random_range");
                m.notifyGlobal = rs.getBoolean("notify_global");
                m.description = rs.getString("description");
                m.isActive = rs.getBoolean("is_active");

                // Display names
                m.itemName = rs.getString("item_name");
                m.mobName = rs.getString("mob_name");
                m.mapName = rs.getString("map_name");

                list.add(m);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Tìm kiếm theo điều kiện
     */
    public List<MobRewardModel> search(String keyword, String eventKey, String mapType) {
        List<MobRewardModel> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT mr.*,
                       COALESCE(it.name, '') as item_name,
                       COALESCE(mt.name, '') as mob_name,
                       COALESCE(map.name, '') as map_name
                FROM mob_reward mr
                LEFT JOIN item_template it ON mr.item_template_id = it.id
                LEFT JOIN mob_template mt ON mr.mob_id = mt.id
                LEFT JOIN map_template map ON mr.map_id = map.id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (it.name LIKE ? OR mr.description LIKE ? OR mr.item_template_id = ?)");
            params.add("%" + keyword + "%");
            params.add("%" + keyword + "%");
            try {
                params.add(Integer.parseInt(keyword));
            } catch (NumberFormatException e) {
                params.add(-99999);
            }
        }

        if (eventKey != null && !eventKey.isEmpty() && !eventKey.equals("Tất cả")) {
            sql.append(" AND mr.event_key = ?");
            params.add(eventKey);
        }

        if (mapType != null && !mapType.isEmpty() && !mapType.equals("Tất cả")) {
            sql.append(" AND mr.map_type = ?");
            params.add(mapType);
        }

        sql.append(" ORDER BY mr.id DESC");

        try (Connection con = ShopManagerDAO.gI().getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MobRewardModel m = new MobRewardModel();
                    m.id = rs.getInt("id");
                    m.mobId = rs.getInt("mob_id");
                    m.mapId = rs.getInt("map_id");
                    m.itemTemplateId = rs.getInt("item_template_id");
                    m.rate = rs.getInt("rate");
                    m.quantityMin = rs.getInt("quantity_min");
                    m.quantityMax = rs.getInt("quantity_max");
                    m.gender = rs.getInt("gender");
                    m.eventKey = rs.getString("event_key");
                    m.mapType = rs.getString("map_type");
                    m.conditionType = rs.getString("condition_type");
                    m.optionsJson = rs.getString("options_json");
                    m.isRandomRange = rs.getBoolean("is_random_range");
                    m.randomRange = rs.getInt("random_range");
                    m.notifyGlobal = rs.getBoolean("notify_global");
                    m.description = rs.getString("description");
                    m.isActive = rs.getBoolean("is_active");

                    m.itemName = rs.getString("item_name");
                    m.mobName = rs.getString("mob_name");
                    m.mapName = rs.getString("map_name");

                    list.add(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Thêm mới MobReward
     */
    public boolean add(MobRewardModel m) {
        String sql = """
                INSERT INTO mob_reward (mob_id, map_id, item_template_id, rate, quantity_min, quantity_max,
                                        gender, event_key, map_type, condition_type, options_json,
                                        is_random_range, random_range, notify_global, description, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = ShopManagerDAO.gI().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, m.mobId);
            ps.setInt(2, m.mapId);
            ps.setInt(3, m.itemTemplateId);
            ps.setInt(4, m.rate);
            ps.setInt(5, m.quantityMin);
            ps.setInt(6, m.quantityMax);
            ps.setInt(7, m.gender);
            setNullableString(ps, 8, m.eventKey);
            setNullableString(ps, 9, m.mapType);
            setNullableString(ps, 10, m.conditionType);
            setNullableString(ps, 11, m.optionsJson);
            ps.setBoolean(12, m.isRandomRange);
            ps.setInt(13, m.randomRange);
            ps.setBoolean(14, m.notifyGlobal);
            setNullableString(ps, 15, m.description);
            ps.setBoolean(16, m.isActive);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật MobReward
     */
    public boolean update(MobRewardModel m) {
        String sql = """
                UPDATE mob_reward SET
                    mob_id = ?, map_id = ?, item_template_id = ?, rate = ?, quantity_min = ?, quantity_max = ?,
                    gender = ?, event_key = ?, map_type = ?, condition_type = ?, options_json = ?,
                    is_random_range = ?, random_range = ?, notify_global = ?, description = ?, is_active = ?
                WHERE id = ?
                """;

        try (Connection con = ShopManagerDAO.gI().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, m.mobId);
            ps.setInt(2, m.mapId);
            ps.setInt(3, m.itemTemplateId);
            ps.setInt(4, m.rate);
            ps.setInt(5, m.quantityMin);
            ps.setInt(6, m.quantityMax);
            ps.setInt(7, m.gender);
            setNullableString(ps, 8, m.eventKey);
            setNullableString(ps, 9, m.mapType);
            setNullableString(ps, 10, m.conditionType);
            setNullableString(ps, 11, m.optionsJson);
            ps.setBoolean(12, m.isRandomRange);
            ps.setInt(13, m.randomRange);
            ps.setBoolean(14, m.notifyGlobal);
            setNullableString(ps, 15, m.description);
            ps.setBoolean(16, m.isActive);
            ps.setInt(17, m.id);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set String value to PreparedStatement, convert empty string to NULL
     */
    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    /**
     * Xóa MobReward
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM mob_reward WHERE id = ?";

        try (Connection con = ShopManagerDAO.gI().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Toggle active status
     */
    public boolean toggleActive(int id, boolean active) {
        String sql = "UPDATE mob_reward SET is_active = ? WHERE id = ?";

        try (Connection con = ShopManagerDAO.gI().getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách event keys có sẵn
     */
    public List<String> getEventKeys() {
        List<String> list = new ArrayList<>();
        list.add("Tất cả");
        list.add("");
        list.add("CHRISTMAS");
        list.add("HALLOWEEN");
        list.add("LUNNAR_NEW_YEAR");
        list.add("INTERNATIONAL_WOMANS_DAY");
        list.add("HUNG_VUONG");
        list.add("TRUNG_THU");
        return list;
    }

    /**
     * Lấy danh sách map types có sẵn
     */
    public List<String> getMapTypes() {
        List<String> list = new ArrayList<>();
        list.add("Tất cả");
        list.add("");
        list.add("MAP_COLD");
        list.add("MAP_SKH");
        list.add("MAP_PORATA");
        list.add("MAP_TUONG_LAI");
        list.add("MAP_NGHIA_DIA");
        list.add("MAP_HUY_DIET");
        list.add("MAP_HANH_TINH_THUC_VAT");
        list.add("MAP_PHO_BAN");
        list.add("MAP_HALLOWEEN");
        list.add("MAP_SKY_PEAR");
        list.add("MAP_NAPPA");
        return list;
    }

    /**
     * Lấy danh sách condition types có sẵn
     */
    public List<String> getConditionTypes() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("FULL_SET_THAN");
        list.add("IS_BUMA");
        list.add("IS_QUAN_DI_BIEN");
        list.add("USE_MAYDO");
        list.add("USE_MAYDO2");
        list.add("HAS_NTK");
        list.add("DROP_SET_KICH_HOAT");
        list.add("DROP_SET_KICH_HOAT_VIP");
        return list;
    }
}
