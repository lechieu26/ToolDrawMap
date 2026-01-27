package com.girlkun.tool.entities.shop;

import com.girlkun.tool.entities.map.NpcTemplate;
import com.girlkun.tool.main.Manager;
import java.util.ArrayList;
import java.util.List;

public class Shop {
   private int id;
   private NpcTemplate npc;
   private String tagName;
   private int typeShop;
   private List<TabShop> tabShops;

   public Shop(int id, int npcId, String tagName, int typeShop) {
      this.id = id;

      try {
         this.npc = Manager.gI().getNpcTemplateById(npcId);
      } catch (Exception var6) {
      }

      this.tagName = tagName;
      this.typeShop = typeShop;
      this.tabShops = new ArrayList<>();
   }

   public int getId() {
      return this.id;
   }

   public NpcTemplate getNpc() {
      return this.npc;
   }

   public String getTagName() {
      return this.tagName;
   }

   public int getTypeShop() {
      return this.typeShop;
   }

   public List<TabShop> getTabShops() {
      return this.tabShops;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setNpc(NpcTemplate npc) {
      this.npc = npc;
   }

   public void setTagName(String tagName) {
      this.tagName = tagName;
   }

   public void setTypeShop(int typeShop) {
      this.typeShop = typeShop;
   }

   public void setTabShops(List<TabShop> tabShops) {
      this.tabShops = tabShops;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Shop)) {
         return false;
      } else {
         Shop other = (Shop)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.getTypeShop() != other.getTypeShop()) {
            return false;
         } else {
            Object this$npc = this.getNpc();
            Object other$npc = other.getNpc();
            if (this$npc == null ? other$npc == null : this$npc.equals(other$npc)) {
               Object this$tagName = this.getTagName();
               Object other$tagName = other.getTagName();
               if (this$tagName == null ? other$tagName == null : this$tagName.equals(other$tagName)) {
                  Object this$tabShops = this.getTabShops();
                  Object other$tabShops = other.getTabShops();
                  return this$tabShops == null ? other$tabShops == null : this$tabShops.equals(other$tabShops);
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
      return other instanceof Shop;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getTypeShop();
      Object $npc = this.getNpc();
      result = result * 59 + ($npc == null ? 43 : $npc.hashCode());
      Object $tagName = this.getTagName();
      result = result * 59 + ($tagName == null ? 43 : $tagName.hashCode());
      Object $tabShops = this.getTabShops();
      return result * 59 + ($tabShops == null ? 43 : $tabShops.hashCode());
   }

   @Override
   public String toString() {
      return "Shop(id="
         + this.getId()
         + ", npc="
         + this.getNpc()
         + ", tagName="
         + this.getTagName()
         + ", typeShop="
         + this.getTypeShop()
         + ", tabShops="
         + this.getTabShops()
         + ")";
   }
}
