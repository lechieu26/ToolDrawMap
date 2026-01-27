package com.girlkun.tool.entities.item;

import com.girlkun.tool.main.Manager;

public class ItemOption {
   private ItemOptionTemplate itemOptionTemplate;
   private int param;

   public ItemOption(int optionId, int param) {
      this.itemOptionTemplate = Manager.gI().getItemOptionTemplates().get(optionId);
      this.param = param;
   }

   public ItemOptionTemplate getItemOptionTemplate() {
      return this.itemOptionTemplate;
   }

   public int getParam() {
      return this.param;
   }

   public void setItemOptionTemplate(ItemOptionTemplate itemOptionTemplate) {
      this.itemOptionTemplate = itemOptionTemplate;
   }

   public void setParam(int param) {
      this.param = param;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ItemOption)) {
         return false;
      } else {
         ItemOption other = (ItemOption)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getParam() != other.getParam()) {
            return false;
         } else {
            Object this$itemOptionTemplate = this.getItemOptionTemplate();
            Object other$itemOptionTemplate = other.getItemOptionTemplate();
            return this$itemOptionTemplate == null ? other$itemOptionTemplate == null : this$itemOptionTemplate.equals(other$itemOptionTemplate);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ItemOption;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getParam();
      Object $itemOptionTemplate = this.getItemOptionTemplate();
      return result * 59 + ($itemOptionTemplate == null ? 43 : $itemOptionTemplate.hashCode());
   }

   @Override
   public String toString() {
      return "ItemOption(itemOptionTemplate=" + this.getItemOptionTemplate() + ", param=" + this.getParam() + ")";
   }
}
