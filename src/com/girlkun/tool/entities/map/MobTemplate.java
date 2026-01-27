package com.girlkun.tool.entities.map;

import com.girlkun.tool.utils.Util;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class MobTemplate {
   private int id;
   private int type;
   private String name;
   private BufferedImage[] images;
   private int rangeMove;
   private int speed;

   public MobTemplate(int id, int type, String name, int rangeMove, int speed) {
      this.id = id;
      this.type = type;
      this.name = name;
      this.rangeMove = rangeMove;
      this.speed = speed;
   }

   public BufferedImage[] getImages() {
      if (this.images == null) {
         try {
            this.images = new BufferedImage[3];
            this.images[0] = Util.getImageMobById(this.id, 1);
            this.images[1] = Util.getImageMobById(this.id, 2);
            this.images[2] = Util.getImageMobById(this.id, 3);
         } catch (Exception var2) {
         }
      }

      return this.images;
   }

   public int getId() {
      return this.id;
   }

   public int getType() {
      return this.type;
   }

   public String getName() {
      return this.name;
   }

   public int getRangeMove() {
      return this.rangeMove;
   }

   public int getSpeed() {
      return this.speed;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setImages(BufferedImage[] images) {
      this.images = images;
   }

   public void setRangeMove(int rangeMove) {
      this.rangeMove = rangeMove;
   }

   public void setSpeed(int speed) {
      this.speed = speed;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MobTemplate)) {
         return false;
      } else {
         MobTemplate other = (MobTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.getType() != other.getType()) {
            return false;
         } else if (this.getRangeMove() != other.getRangeMove()) {
            return false;
         } else if (this.getSpeed() != other.getSpeed()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            return (this$name == null ? other$name == null : this$name.equals(other$name)) ? Arrays.deepEquals(this.getImages(), other.getImages()) : false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof MobTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getType();
      result = result * 59 + this.getRangeMove();
      result = result * 59 + this.getSpeed();
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      return result * 59 + Arrays.deepHashCode(this.getImages());
   }

   @Override
   public String toString() {
      return "MobTemplate(id="
         + this.getId()
         + ", type="
         + this.getType()
         + ", name="
         + this.getName()
         + ", images="
         + Arrays.deepToString(this.getImages())
         + ", rangeMove="
         + this.getRangeMove()
         + ", speed="
         + this.getSpeed()
         + ")";
   }
}
