package com.girlkun.tool.shopmanager.models;

import java.util.ArrayList;
import java.util.List;

public class ItemData {
    public int cost;
    public int type_sell;
    public boolean is_new;
    public int temp_id;
    public int item_spec;
    public boolean is_sell;
    public List<ItemOption> options = new ArrayList<>();
}
