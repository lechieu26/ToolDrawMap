package com.girlkun.tool.screens.part_scr.models;

import com.girlkun.database.GirlkunDB;
import com.girlkun.result.GirlkunResultSet;
import java.util.Arrays;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

public class Part {
   private int id;
   private int type;
   private PartInfo[] pi;

   public Part(int id, int type) {
      this.id = id;
      this.type = type;
      if (type == 0) {
         this.pi = new PartInfo[3];
      } else if (type == 1) {
         this.pi = new PartInfo[17];
      } else if (type == 2) {
         this.pi = new PartInfo[14];
      }
   }

   public static Part getPart(int id, int type) throws Exception {
      Part part = null;

      try {
         JSONArray dataArray = null;
         JSONValue jv = new JSONValue();
         GirlkunResultSet rs = GirlkunDB.executeQuery("GIRLKUN", "select * from part where id = ? and type = ?", new Object[]{id, type});
         if (rs.first()) {
            part = new Part(id, rs.getInt("type"));
            dataArray = (JSONArray)JSONValue.parse(rs.getString("data"));

            for (int i = 0; i < part.pi.length; i++) {
               JSONArray data = (JSONArray)JSONValue.parse(String.valueOf(dataArray.get(i)));
               part.pi[i] = new PartInfo(
                  Integer.parseInt(String.valueOf(data.get(0))), Integer.parseInt(String.valueOf(data.get(1))), Integer.parseInt(String.valueOf(data.get(2)))
               );
            }
         }

         return part;
      } catch (Exception var8) {
         throw var8;
      }
   }

   public static Part getPart(int id) throws Exception {
      Part part = null;

      try {
         JSONArray dataArray = null;
         JSONValue jv = new JSONValue();
         GirlkunResultSet rs = GirlkunDB.executeQuery("GIRLKUN", "select * from part where id = ?", new Object[]{id});
         if (rs.first()) {
            part = new Part(id, rs.getInt("type"));
            dataArray = (JSONArray)JSONValue.parse(rs.getString("data"));

            for (int i = 0; i < part.pi.length; i++) {
               JSONArray data = (JSONArray)JSONValue.parse(String.valueOf(dataArray.get(i)));
               part.pi[i] = new PartInfo(
                  Integer.parseInt(String.valueOf(data.get(0))), Integer.parseInt(String.valueOf(data.get(1))), Integer.parseInt(String.valueOf(data.get(2)))
               );
            }
         }

         return part;
      } catch (Exception var7) {
         throw var7;
      }
   }

   public int getId() {
      return this.id;
   }

   public int getType() {
      return this.type;
   }

   public PartInfo[] getPi() {
      return this.pi;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setPi(PartInfo[] pi) {
      this.pi = pi;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Part)) {
         return false;
      } else {
         Part other = (Part)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else {
            return this.getType() != other.getType() ? false : Arrays.deepEquals(this.getPi(), other.getPi());
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Part;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getType();
      return result * 59 + Arrays.deepHashCode(this.getPi());
   }

   @Override
   public String toString() {
      return "Part(id=" + this.getId() + ", type=" + this.getType() + ", pi=" + Arrays.deepToString(this.getPi()) + ")";
   }
}
