package com.girlkun.tool.screens.part_scr.models;

import com.girlkun.tool.utils.Util;
import java.awt.image.BufferedImage;

public class PartInfo {
   private BufferedImage imageIcon;
   private int iconId;
   private int dx;
   private int dy;

   public PartInfo(int iconId, int dx, int dy) {
      try {
         this.imageIcon = Util.getImageById(iconId, 2);
      } catch (Exception var5) {
      }

      this.iconId = iconId;
      this.dx = dx;
      this.dy = dy;
   }

   public void setIconId(int iconId) {
      try {
         this.iconId = iconId;
         this.imageIcon = Util.getImageById(iconId, 2);
      } catch (Exception var3) {
      }
   }

   public BufferedImage getImageIcon() {
      return this.imageIcon;
   }

   public int getIconId() {
      return this.iconId;
   }

   public int getDx() {
      return this.dx;
   }

   public int getDy() {
      return this.dy;
   }

   public void setImageIcon(BufferedImage imageIcon) {
      this.imageIcon = imageIcon;
   }

   public void setDx(int dx) {
      this.dx = dx;
   }

   public void setDy(int dy) {
      this.dy = dy;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PartInfo)) {
         return false;
      } else {
         PartInfo other = (PartInfo)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getIconId() != other.getIconId()) {
            return false;
         } else if (this.getDx() != other.getDx()) {
            return false;
         } else if (this.getDy() != other.getDy()) {
            return false;
         } else {
            Object this$imageIcon = this.getImageIcon();
            Object other$imageIcon = other.getImageIcon();
            return this$imageIcon == null ? other$imageIcon == null : this$imageIcon.equals(other$imageIcon);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PartInfo;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getIconId();
      result = result * 59 + this.getDx();
      result = result * 59 + this.getDy();
      Object $imageIcon = this.getImageIcon();
      return result * 59 + ($imageIcon == null ? 43 : $imageIcon.hashCode());
   }

   @Override
   public String toString() {
      return "PartInfo(imageIcon=" + this.getImageIcon() + ", iconId=" + this.getIconId() + ", dx=" + this.getDx() + ", dy=" + this.getDy() + ")";
   }
}
