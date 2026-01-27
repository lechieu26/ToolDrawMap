package com.girlkun.tool.shopmanager.models;

import java.util.List;

public class GiftcodeItem {
    public int temp_id;
    public int quantity;
    public List<GiftcodeItemOption> options;

    public static class GiftcodeItemOption {
        public Integer id;
        public Integer param;
    }
}
