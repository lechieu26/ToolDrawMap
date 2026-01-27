package com.girlkun.tool.entities;

import com.girlkun.tool.utils.Util;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class EffectTemplate {
   private int id;
   private int[][] axisSubImage;
   private int[][][] axisFrame;
   private BufferedImage imageOri;
   private BufferedImage[] frame;

   public int getSizeFrame() {
      return this.axisFrame.length;
   }

   public BufferedImage getFrame(int f) {
      if (this.frame == null) {
         this.frame = new BufferedImage[this.axisFrame.length];
      }

      if (this.frame[f] == null) {
         BufferedImage image = new BufferedImage(1000, 1000, 2);
         Graphics2D g = image.createGraphics();

         for (int i = 0; i < this.axisFrame[f].length; i++) {
            try {
               int imgInfo = this.axisFrame[f][i][2];
               BufferedImage subImage = this.imageOri
                  .getSubimage(this.axisSubImage[imgInfo][1], this.axisSubImage[imgInfo][2], this.axisSubImage[imgInfo][3], this.axisSubImage[imgInfo][4]);
               g.drawImage(subImage, 500 + this.axisFrame[f][i][0], 500 + this.axisFrame[f][i][1], null);
            } catch (Exception var7) {
               var7.printStackTrace();
            }
         }

         g.dispose();
         this.frame[f] = Util.trimImage(image);
      }

      return this.frame[f];
   }

   public int getId() {
      return this.id;
   }

   public int[][] getAxisSubImage() {
      return this.axisSubImage;
   }

   public int[][][] getAxisFrame() {
      return this.axisFrame;
   }

   public BufferedImage getImageOri() {
      return this.imageOri;
   }

   public BufferedImage[] getFrame() {
      return this.frame;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setAxisSubImage(int[][] axisSubImage) {
      this.axisSubImage = axisSubImage;
   }

   public void setAxisFrame(int[][][] axisFrame) {
      this.axisFrame = axisFrame;
   }

   public void setImageOri(BufferedImage imageOri) {
      this.imageOri = imageOri;
   }

   public void setFrame(BufferedImage[] frame) {
      this.frame = frame;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EffectTemplate)) {
         return false;
      } else {
         EffectTemplate other = (EffectTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (!Arrays.deepEquals(this.getAxisSubImage(), other.getAxisSubImage())) {
            return false;
         } else if (!Arrays.deepEquals(this.getAxisFrame(), other.getAxisFrame())) {
            return false;
         } else {
            Object this$imageOri = this.getImageOri();
            Object other$imageOri = other.getImageOri();
            return (this$imageOri == null ? other$imageOri == null : this$imageOri.equals(other$imageOri))
               ? Arrays.deepEquals(this.getFrame(), other.getFrame())
               : false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EffectTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + Arrays.deepHashCode(this.getAxisSubImage());
      result = result * 59 + Arrays.deepHashCode(this.getAxisFrame());
      Object $imageOri = this.getImageOri();
      result = result * 59 + ($imageOri == null ? 43 : $imageOri.hashCode());
      return result * 59 + Arrays.deepHashCode(this.getFrame());
   }

   @Override
   public String toString() {
      return "EffectTemplate(id="
         + this.getId()
         + ", axisSubImage="
         + Arrays.deepToString(this.getAxisSubImage())
         + ", axisFrame="
         + Arrays.deepToString(this.getAxisFrame())
         + ", imageOri="
         + this.getImageOri()
         + ", frame="
         + Arrays.deepToString(this.getFrame())
         + ")";
   }
}
