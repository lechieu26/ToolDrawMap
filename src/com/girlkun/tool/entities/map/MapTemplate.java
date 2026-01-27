package com.girlkun.tool.entities.map;

public class MapTemplate {
   private int id;
   private String name;

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setName(String name) {
      this.name = name;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MapTemplate)) {
         return false;
      } else {
         MapTemplate other = (MapTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            return this$name == null ? other$name == null : this$name.equals(other$name);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof MapTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      Object $name = this.getName();
      return result * 59 + ($name == null ? 43 : $name.hashCode());
   }

   @Override
   public String toString() {
      return "MapTemplate(id=" + this.getId() + ", name=" + this.getName() + ")";
   }

   public MapTemplate(int id, String name) {
      this.id = id;
      this.name = name;
   }
}
