package com.girlkun.tool.entities.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TilesetType {
   public int id;
   public Map<Integer, List<Integer>> tileType = new HashMap<>();

   public TilesetType() {
      for (int i = 1; i <= 100; i++) {
         this.tileType.put(i, new ArrayList<>());
      }
   }
}
