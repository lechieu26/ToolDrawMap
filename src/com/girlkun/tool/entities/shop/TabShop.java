package com.girlkun.tool.entities.shop;

import java.util.ArrayList;
import java.util.List;

public class TabShop {
   private Shop shop;
   private int id;
   private String name;
   private List<ItemShop> items;

   public TabShop(Shop shop, int id, String name) {
      this.shop = shop;
      this.id = id;
      this.name = name;
      this.items = new ArrayList<>();
   }

   public Shop getShop() {
      return this.shop;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public List<ItemShop> getItems() {
      return this.items;
   }

   public void setShop(Shop shop) {
      this.shop = shop;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setItems(List<ItemShop> items) {
      this.items = items;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof TabShop)) {
         return false;
      } else {
         TabShop other = (TabShop)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else {
            Object this$shop = this.getShop();
            Object other$shop = other.getShop();
            if (this$shop == null ? other$shop == null : this$shop.equals(other$shop)) {
               Object this$name = this.getName();
               Object other$name = other.getName();
               if (this$name == null ? other$name == null : this$name.equals(other$name)) {
                  Object this$items = this.getItems();
                  Object other$items = other.getItems();
                  return this$items == null ? other$items == null : this$items.equals(other$items);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof TabShop;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      Object $shop = this.getShop();
      result = result * 59 + ($shop == null ? 43 : $shop.hashCode());
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $items = this.getItems();
      return result * 59 + ($items == null ? 43 : $items.hashCode());
   }

   @Override
   public String toString() {
      return "TabShop(shop=" + this.getShop() + ", id=" + this.getId() + ", name=" + this.getName() + ", items=" + this.getItems() + ")";
   }
}
