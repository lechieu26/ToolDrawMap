package com.girlkun.tool.entities.map;

import com.girlkun.tool.utils.Util;
import java.awt.image.BufferedImage;

public class BgItemTemplate {
   private BufferedImage image;
   private int id;
   private int imageId;
   private int layer;
   private int dx;
   private int dy;

   public BufferedImage getImage() {
      if (this.image == null) {
         try {
            this.image = Util.getBgImageById(this.imageId, 1);
         } catch (Exception var2) {
         }
      }

      return this.image;
   }

   public BgItemTemplate(int id, int imageId, int layer, int dx, int dy) {
      this.id = id;
      this.imageId = imageId;
      this.layer = layer;
      this.dx = dx;
      this.dy = dy;
   }

   public int getId() {
      return this.id;
   }

   public int getImageId() {
      return this.imageId;
   }

   public int getLayer() {
      return this.layer;
   }

   public int getDx() {
      return this.dx;
   }

   public int getDy() {
      return this.dy;
   }

   public void setImage(BufferedImage image) {
      this.image = image;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setImageId(int imageId) {
      this.imageId = imageId;
   }

   public void setLayer(int layer) {
      this.layer = layer;
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
      } else if (!(o instanceof BgItemTemplate)) {
         return false;
      } else {
         BgItemTemplate other = (BgItemTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.getImageId() != other.getImageId()) {
            return false;
         } else if (this.getLayer() != other.getLayer()) {
            return false;
         } else if (this.getDx() != other.getDx()) {
            return false;
         } else if (this.getDy() != other.getDy()) {
            return false;
         } else {
            Object this$image = this.getImage();
            Object other$image = other.getImage();
            return this$image == null ? other$image == null : this$image.equals(other$image);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof BgItemTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getImageId();
      result = result * 59 + this.getLayer();
      result = result * 59 + this.getDx();
      result = result * 59 + this.getDy();
      Object $image = this.getImage();
      return result * 59 + ($image == null ? 43 : $image.hashCode());
   }

   @Override
   public String toString() {
      return "BgItemTemplate(image="
         + this.getImage()
         + ", id="
         + this.getId()
         + ", imageId="
         + this.getImageId()
         + ", layer="
         + this.getLayer()
         + ", dx="
         + this.getDx()
         + ", dy="
         + this.getDy()
         + ")";
   }
}
