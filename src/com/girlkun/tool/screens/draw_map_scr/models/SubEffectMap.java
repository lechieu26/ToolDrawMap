package com.girlkun.tool.screens.draw_map_scr.models;

import com.girlkun.tool.entities.EffectTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.utils.Util;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class SubEffectMap extends EffectMap {
   private int typeEff;
   private BufferedImage img;
   private int[] x;
   private int[] y;
   private int[] vx;
   private int[] vy;
   private DrawMapScr drawMapScr;

   public SubEffectMap(EffectTemplate temp, int x, int y, int layer, int loop, int loopCount) {
      super(temp, x, y, layer, loop, loopCount);
   }

   public SubEffectMap() {
      super(null, -1, -1, 3, -1, -1);
   }

   public int getTypeEff() {
      return this.typeEff;
   }

   public SubEffectMap(int typeEff, DrawMapScr drawMapScr) {
      this();
      this.drawMapScr = drawMapScr;

      try {
         this.typeEff = typeEff;
         this.img = Util.getImageBEffectByType(this.typeEff);
         int sum;
         switch (this.typeEff) {
            case 1:
            case 2:
            case 5:
            case 6:
            case 7:
            case 11:
               sum = Util.range(30, 50);
               if (this.typeEff == 11) {
                  sum = 100;
               }

               this.x = new int[sum];
               this.y = new int[sum];
               this.vx = new int[sum];
               this.vy = new int[sum];

               for (int i = 0; i < sum; i++) {
                  this.x[i] = Util.range(-10, drawMapScr.mapWidth + 10);
                  this.y[i] = Util.range(0, drawMapScr.mapHeight);
                  this.vx[i] = Util.range(-3, 3);
                  this.vy[i] = Util.range(1, 4);
                  if (typeEff == 11) {
                     this.vx[i] = Math.abs(Util.range(1, 3));
                     this.vy[i] = Math.abs(Util.range(1, 3));
                  }
               }
            case 3:
            case 8:
            case 9:
            case 10:
            default:
               break;
            case 4:
               sum = Util.range(30, 40);
               this.x = new int[sum];
               this.y = new int[sum];
               this.vx = new int[sum];
               this.vy = new int[sum];

               for (int ix = 0; ix < sum; ix++) {
                  this.x[ix] = Util.range(0, drawMapScr.mapWidth);
                  this.y[ix] = Util.range(0, 50);
                  this.vx[ix] = 0;
                  this.vy[ix] = 0;
               }
               break;
            case 12:
               sum = 500;
               this.x = new int[sum];
               this.y = new int[sum];
               this.vx = new int[sum];
               this.vy = new int[sum];

               for (int i = 0; i < sum; i++) {
                  this.x[i] = Util.range(-10, drawMapScr.mapWidth + 10);
                  this.y[i] = Util.range(0, drawMapScr.mapHeight);
                  this.vx[i] = -12;
                  this.vy[i] = 12;
               }
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }
   }

   @Override
   public void paint(Graphics2D g, boolean changeColor) {
      if (this.img != null) {
         if (this.typeEff == 12) {
            this.paintMua(g);
         } else {
            int num = this.typeEff == 4 ? 2 : (this.typeEff != 11 ? 4 : 3);
            this.paintLacay1(g, num);
         }
      }
   }

   public void paintMua(Graphics2D g) {
      for (int i = 0; i < this.x.length; i++) {
         g.drawImage(this.img, this.x[i], this.y[i], null);
         this.x[i] = this.x[i] + this.vx[i];
         this.y[i] = this.y[i] + this.vy[i];
         if (this.x[i] < -10) {
            this.x[i] = Util.range(-10, this.drawMapScr.mapWidth + 10);
         }

         if (this.y[i] > this.drawMapScr.mapHeight + 10) {
            this.y[i] = 0;
         }
      }
   }

   public void paintLacay1(Graphics2D g, int num) {
      for (int i = 0; i < this.x.length; i++) {
         g.drawImage(
            this.img.getSubimage(0, this.img.getHeight() / num * Util.range(0, num - 1), this.img.getWidth(), this.img.getHeight() / num),
            this.x[i],
            this.y[i],
            null
         );
         this.x[i] = this.x[i] + this.vx[i];
         this.y[i] = this.y[i] + this.vy[i];
         if (this.x[i] < -10) {
            this.x[i] = this.drawMapScr.mapWidth + 10;
            this.y[i] = -10;
            this.x[i] = Util.range(-10, this.drawMapScr.mapWidth + 10);
            this.vx[i] = Util.range(-3, 3);
            this.vy[i] = Util.range(1, 4);
            if (this.typeEff == 11) {
               this.vx[i] = Math.abs(Util.range(1, 3));
               this.vy[i] = Math.abs(Util.range(1, 3));
            }
         } else if (this.x[i] > this.drawMapScr.mapWidth + 10) {
            this.x[i] = Util.range(-10, this.drawMapScr.mapWidth + 10);
            this.x[i] = -10;
            this.y[i] = -10;
            this.vx[i] = Util.range(-3, 3);
            this.vy[i] = Util.range(1, 4);
            if (this.typeEff == 11) {
               this.vx[i] = Math.abs(Util.range(1, 3));
               this.vy[i] = Math.abs(Util.range(1, 3));
            }
         }

         if (this.y[i] > this.drawMapScr.mapHeight + 10) {
            this.y[i] = Util.range(0, this.drawMapScr.mapHeight);
            this.y[i] = -10;
            this.vx[i] = Util.range(-3, 3);
            this.vy[i] = Util.range(1, 4);
            if (this.typeEff == 11) {
               this.vx[i] = Math.abs(Util.range(1, 3));
               this.vy[i] = Math.abs(Util.range(1, 3));
            }
         }
      }
   }
}
