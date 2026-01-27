package com.girlkun.tool.entities.shop;

import com.girlkun.tool.entities.item.ItemOption;
import com.girlkun.tool.entities.item.ItemTemplate;
import com.girlkun.tool.main.Manager;
import java.util.ArrayList;
import java.util.List;

public class ItemShop {
   private TabShop tabShop;
   private int id;
   private ItemTemplate itemTemplate;
   private boolean isNew;
   private boolean isSell;
   private int typeSell;
   private int cost;
   private long createTime;
   private List<ItemOption> options;
   private ItemTemplate itemSpec;

   public ItemShop(TabShop tabShop, int id, int tempId, boolean isNew, boolean isSell, int typeSell, int cost, ItemTemplate itemSpec, long createTime) {
      this.tabShop = tabShop;
      this.id = id;
      this.itemTemplate = Manager.gI().getItemTemplates().get(tempId);
      this.isNew = isNew;
      this.isSell = isSell;
      this.typeSell = typeSell;
      this.cost = cost;
      this.itemSpec = itemSpec;
      this.createTime = createTime;
      this.options = new ArrayList<>();
   }

   public TabShop getTabShop() {
      return this.tabShop;
   }

   public int getId() {
      return this.id;
   }

   public ItemTemplate getItemTemplate() {
      return this.itemTemplate;
   }

   public boolean isNew() {
      return this.isNew;
   }

   public boolean isSell() {
      return this.isSell;
   }

   public int getTypeSell() {
      return this.typeSell;
   }

   public int getCost() {
      return this.cost;
   }

   public long getCreateTime() {
      return this.createTime;
   }

   public List<ItemOption> getOptions() {
      return this.options;
   }

   public ItemTemplate getItemSpec() {
      return this.itemSpec;
   }

   public void setTabShop(TabShop tabShop) {
      this.tabShop = tabShop;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setItemTemplate(ItemTemplate itemTemplate) {
      this.itemTemplate = itemTemplate;
   }

   public void setNew(boolean isNew) {
      this.isNew = isNew;
   }

   public void setSell(boolean isSell) {
      this.isSell = isSell;
   }

   public void setTypeSell(int typeSell) {
      this.typeSell = typeSell;
   }

   public void setCost(int cost) {
      this.cost = cost;
   }

   public void setCreateTime(long createTime) {
      this.createTime = createTime;
   }

   public void setOptions(List<ItemOption> options) {
      this.options = options;
   }

   public void setItemSpec(ItemTemplate itemSpec) {
      this.itemSpec = itemSpec;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ItemShop)) {
         return false;
      } else {
         ItemShop other = (ItemShop)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.isNew() != other.isNew()) {
            return false;
         } else if (this.isSell() != other.isSell()) {
            return false;
         } else if (this.getTypeSell() != other.getTypeSell()) {
            return false;
         } else if (this.getCost() != other.getCost()) {
            return false;
         } else if (this.getCreateTime() != other.getCreateTime()) {
            return false;
         } else {
            Object this$tabShop = this.getTabShop();
            Object other$tabShop = other.getTabShop();
            if (this$tabShop == null ? other$tabShop == null : this$tabShop.equals(other$tabShop)) {
               Object this$itemTemplate = this.getItemTemplate();
               Object other$itemTemplate = other.getItemTemplate();
               if (this$itemTemplate == null ? other$itemTemplate == null : this$itemTemplate.equals(other$itemTemplate)) {
                  Object this$options = this.getOptions();
                  Object other$options = other.getOptions();
                  if (this$options == null ? other$options == null : this$options.equals(other$options)) {
                     Object this$itemSpec = this.getItemSpec();
                     Object other$itemSpec = other.getItemSpec();
                     return this$itemSpec == null ? other$itemSpec == null : this$itemSpec.equals(other$itemSpec);
                  } else {
                     return false;
                  }
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
      return other instanceof ItemShop;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + (this.isNew() ? 79 : 97);
      result = result * 59 + (this.isSell() ? 79 : 97);
      result = result * 59 + this.getTypeSell();
      result = result * 59 + this.getCost();
      long $createTime = this.getCreateTime();
      result = result * 59 + (int)($createTime >>> 32 ^ $createTime);
      Object $tabShop = this.getTabShop();
      result = result * 59 + ($tabShop == null ? 43 : $tabShop.hashCode());
      Object $itemTemplate = this.getItemTemplate();
      result = result * 59 + ($itemTemplate == null ? 43 : $itemTemplate.hashCode());
      Object $options = this.getOptions();
      result = result * 59 + ($options == null ? 43 : $options.hashCode());
      Object $itemSpec = this.getItemSpec();
      return result * 59 + ($itemSpec == null ? 43 : $itemSpec.hashCode());
   }

   @Override
   public String toString() {
      return "ItemShop(tabShop="
         + this.getTabShop()
         + ", id="
         + this.getId()
         + ", itemTemplate="
         + this.getItemTemplate()
         + ", isNew="
         + this.isNew()
         + ", isSell="
         + this.isSell()
         + ", typeSell="
         + this.getTypeSell()
         + ", cost="
         + this.getCost()
         + ", createTime="
         + this.getCreateTime()
         + ", options="
         + this.getOptions()
         + ", itemSpec="
         + this.getItemSpec()
         + ")";
   }
}
