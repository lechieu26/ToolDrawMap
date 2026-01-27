package com.girlkun.tool.entities.item;

public class ItemTemplate {
   private int id;
   private int type;
   private int gender;
   private String name;
   private String description;
   private int iconId;
   private int part;
   private boolean isUpToUp;
   private long powerRequire;
   private int gold;
   private int gem;

   public int getId() {
      return this.id;
   }

   public int getType() {
      return this.type;
   }

   public int getGender() {
      return this.gender;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public int getIconId() {
      return this.iconId;
   }

   public int getPart() {
      return this.part;
   }

   public boolean isUpToUp() {
      return this.isUpToUp;
   }

   public long getPowerRequire() {
      return this.powerRequire;
   }

   public int getGold() {
      return this.gold;
   }

   public int getGem() {
      return this.gem;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setGender(int gender) {
      this.gender = gender;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setIconId(int iconId) {
      this.iconId = iconId;
   }

   public void setPart(int part) {
      this.part = part;
   }

   public void setUpToUp(boolean isUpToUp) {
      this.isUpToUp = isUpToUp;
   }

   public void setPowerRequire(long powerRequire) {
      this.powerRequire = powerRequire;
   }

   public void setGold(int gold) {
      this.gold = gold;
   }

   public void setGem(int gem) {
      this.gem = gem;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ItemTemplate)) {
         return false;
      } else {
         ItemTemplate other = (ItemTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.getType() != other.getType()) {
            return false;
         } else if (this.getGender() != other.getGender()) {
            return false;
         } else if (this.getIconId() != other.getIconId()) {
            return false;
         } else if (this.getPart() != other.getPart()) {
            return false;
         } else if (this.isUpToUp() != other.isUpToUp()) {
            return false;
         } else if (this.getPowerRequire() != other.getPowerRequire()) {
            return false;
         } else if (this.getGold() != other.getGold()) {
            return false;
         } else if (this.getGem() != other.getGem()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            if (this$name == null ? other$name == null : this$name.equals(other$name)) {
               Object this$description = this.getDescription();
               Object other$description = other.getDescription();
               return this$description == null ? other$description == null : this$description.equals(other$description);
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ItemTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getType();
      result = result * 59 + this.getGender();
      result = result * 59 + this.getIconId();
      result = result * 59 + this.getPart();
      result = result * 59 + (this.isUpToUp() ? 79 : 97);
      long $powerRequire = this.getPowerRequire();
      result = result * 59 + (int)($powerRequire >>> 32 ^ $powerRequire);
      result = result * 59 + this.getGold();
      result = result * 59 + this.getGem();
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $description = this.getDescription();
      return result * 59 + ($description == null ? 43 : $description.hashCode());
   }

   public ItemTemplate(
      int id, int type, int gender, String name, String description, int iconId, int part, boolean isUpToUp, long powerRequire, int gold, int gem
   ) {
      this.id = id;
      this.type = type;
      this.gender = gender;
      this.name = name;
      this.description = description;
      this.iconId = iconId;
      this.part = part;
      this.isUpToUp = isUpToUp;
      this.powerRequire = powerRequire;
      this.gold = gold;
      this.gem = gem;
   }

   public ItemTemplate() {
   }

   @Override
   public String toString() {
      return "ItemTemplate(id="
         + this.getId()
         + ", type="
         + this.getType()
         + ", gender="
         + this.getGender()
         + ", name="
         + this.getName()
         + ", description="
         + this.getDescription()
         + ", iconId="
         + this.getIconId()
         + ", part="
         + this.getPart()
         + ", isUpToUp="
         + this.isUpToUp()
         + ", powerRequire="
         + this.getPowerRequire()
         + ", gold="
         + this.getGold()
         + ", gem="
         + this.getGem()
         + ")";
   }
}
