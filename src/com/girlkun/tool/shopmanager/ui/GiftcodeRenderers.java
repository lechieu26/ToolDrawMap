package com.girlkun.tool.shopmanager.ui;

import com.girlkun.tool.shopmanager.services.ShopManagerDAO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class GiftcodeRenderers {

    // Cache: tempId -> Image
    private static final Map<Integer, Image> iconCache = new HashMap<>();
    // Cache: tempId -> iconId
    private static final Map<Integer, Integer> iconIdCache = new HashMap<>();
    // Cache: optionId -> optionName
    private static final Map<Integer, String> optionNameCache = new HashMap<>();

    // Cache: tempId -> itemName
    private static final Map<Integer, String> itemNameCache = new HashMap<>();

    public static String getItemName(int id, ShopManagerDAO dao) {
        if (itemNameCache.containsKey(id)) {
            return itemNameCache.get(id);
        }
        String name = "Item " + id;
        if (dao != null) {
            String dbName = dao.getItemName(id);
            if (dbName != null) {
                name = dbName;
                itemNameCache.put(id, name);
            }
        }
        return name;
    }

    private static final String ICON_PATH = "data/girlkun/icon/x4/"; // User requested x4

    public static Image getItemIcon(int tempId, ShopManagerDAO dao) {
        if (iconCache.containsKey(tempId)) {
            return iconCache.get(tempId);
        }

        // Resolve Icon ID from DB if not in cache
        int iconId = tempId; // default fallback
        if (dao != null) {
            if (iconIdCache.containsKey(tempId)) {
                iconId = iconIdCache.get(tempId);
            } else {
                int dbIconId = dao.getIconId(tempId);
                if (dbIconId != -1) {
                    iconId = dbIconId;
                    iconIdCache.put(tempId, iconId);
                } else {
                    iconIdCache.put(tempId, tempId); // fallback
                }
            }
        }

        try {
            File f = new File(ICON_PATH + iconId + ".png");
            if (f.exists()) {
                BufferedImage img = ImageIO.read(f);
                iconCache.put(tempId, img);
                return img;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        iconCache.put(tempId, null); // cache null to avoid retry
        return null;
    }

    public static String getOptionName(int id, ShopManagerDAO dao) {
        if (optionNameCache.containsKey(id)) {
            return optionNameCache.get(id);
        }
        String name = "Option " + id;
        if (dao != null) {
            String dbName = dao.getOptionName(id);
            if (dbName != null) {
                name = dbName;
                optionNameCache.put(id, name);
            }
        }
        return name;
    }

    // Custom Component for Icon + Quantity + Tooltip
    private static class ItemIcon extends JPanel {
        private Image img;
        private int quantity;
        private List<OptionData> options;
        private ShopManagerDAO dao;

        private static class OptionData {
            int id;
            int param;

            public OptionData(int id, int param) {
                this.id = id;
                this.param = param;
            }
        }

        public ItemIcon(int tempId, int quantity, List<OptionData> options, ShopManagerDAO dao) {
            this.quantity = quantity;
            this.options = options;
            this.dao = dao;
            this.img = getItemIcon(tempId, dao);
            this.setPreferredSize(new Dimension(32, 32));
            this.setOpaque(false);

            // Build tooltip text using cached option names
            StringBuilder tip = new StringBuilder();
            tip.append(
                    "<html><div style='background-color:black; color:white; font-weight:bold; font-family:SansSerif; padding:6px; border-radius:5px;'>");
            tip.append("<div style='font-size:11px; margin-bottom:5px;'>Item: <span style='color:#FFD700;'>")
                    .append(getItemName(tempId, dao)).append("</span></div>");
            tip.append("<div style='font-size:11px; margin-bottom:5px;'>Quantity: <span style='color:#00FF00;'>")
                    .append(quantity).append("</span></div>");
            if (options != null && !options.isEmpty()) {
                tip.append(
                        "<hr style='border:0; height:1px; background-color:white;'/><div style='margin-top:5px;'>Options:</div>");
                for (OptionData opt : options) {
                    String name = getOptionName(opt.id, dao);
                    String formatted = name.replaceAll("#", String.valueOf(opt.param));
                    if (formatted.equals(name)) { // no placeholder
                        formatted = name + ": " + opt.param;
                    }
                    tip.append("<div style='color:#ADD8E6; margin-left:10px;'>- ").append(formatted).append("</div>");
                }
            }
            tip.append("</div></html>");
            this.setToolTipText(tip.toString());
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            tip.setComponent(this);
            tip.setBackground(Color.BLACK);
            tip.setForeground(Color.WHITE);
            tip.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            return tip;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Draw rounded border (more visible)
            g2.setColor(new Color(230, 230, 230)); // BG
            g2.fillRoundRect(0, 0, 31, 31, 8, 8);

            g2.setColor(new Color(150, 150, 150)); // Darker Border
            g2.drawRoundRect(0, 0, 31, 31, 8, 8);

            // 2. Draw Image (Scaled)
            if (img != null) {
                g2.drawImage(img, 4, 4, 24, 24, null);
            } else {
                g2.setColor(Color.gray);
                g2.drawString("?", 12, 20);
            }

            // 3. Draw Quantity
            String q = String.valueOf(quantity);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(q);
            int x = 28 - w;
            int y = 28;

            // Outline
            g2.setColor(Color.BLACK);
            g2.drawString(q, x - 1, y);
            g2.drawString(q, x + 1, y);
            g2.drawString(q, x, y - 1);
            g2.drawString(q, x, y + 1);

            g2.setColor(Color.WHITE);
            g2.drawString(q, x, y);
        }
    }

    public static class DetailRenderer extends JPanel implements TableCellRenderer {
        private ShopManagerDAO dao;

        public DetailRenderer(ShopManagerDAO dao) {
            this.dao = dao;
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
            setOpaque(true);
            // Ensure border on all sides
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }

        public DetailRenderer() {
            this(null);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            removeAll();

            // Handle Selection Color
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }

            if (value instanceof String) {
                String json = (String) value;
                try {
                    JSONParser p = new JSONParser();
                    Object obj = p.parse(json);
                    if (obj instanceof JSONArray) {
                        JSONArray arr = (JSONArray) obj;
                        for (Object o : arr) {
                            JSONObject item = (JSONObject) o;
                            int id = ((Long) item.get("temp_id")).intValue();
                            int qty = ((Long) item.get("quantity")).intValue();

                            List<ItemIcon.OptionData> opts = new ArrayList<>();
                            if (item.containsKey("options")) {
                                JSONArray optArr = (JSONArray) item.get("options");
                                for (Object optObj : optArr) {
                                    JSONObject opt = (JSONObject) optObj;
                                    int oid = ((Long) opt.get("id")).intValue();
                                    int param = ((Long) opt.get("param")).intValue();
                                    opts.add(new ItemIcon.OptionData(oid, param));
                                }
                            }
                            add(new ItemIcon(id, qty, opts, dao));
                        }
                    }
                } catch (Exception e) {
                    // Debug text if parse fails
                    JLabel error = new JLabel(json);
                    error.setForeground(Color.RED);
                    add(error);
                }
            } else if (value != null) {
                add(new JLabel(value.toString()));
            }
            return this;
        }
    }

    public static class CenterRenderer extends DefaultTableCellRenderer {
        public CenterRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }
    }
}
