package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.EffectTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.draw_map_scr.models.EffectMap;
import com.girlkun.tool.screens.draw_map_scr.models.SubEffectMap;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class EffectMapLayer implements Layer {
   private BufferedImage[] image;
   private int indexImage;
   private DrawMapScr drawMapScr;
   private List<EffectMap> effects;
   private List<SubEffectMap> subEffectMaps;
   private long lastTimeNextF;
   private int timeNextF = 50;
   private int f;
   private boolean show = true;

   @Override
   public void clear() {
      this.effects.clear();
      this.subEffectMaps.clear();
   }

   @Override
   public void setSizeImage(int w, int h, int offset, int dir) {
      this.image = new BufferedImage[5];
      this.image[0] = new BufferedImage(w * 24, h * 24, 2);
      this.image[1] = new BufferedImage(w * 24, h * 24, 2);
      this.image[2] = new BufferedImage(w * 24, h * 24, 2);
      this.image[3] = new BufferedImage(w * 24, h * 24, 2);
      this.image[4] = new BufferedImage(w * 24, h * 24, 2);
      switch (dir) {
         case 0:
            for (EffectMap eff : this.effects) {
               eff.setY(eff.getY() + offset * 24);
            }
            break;
         case 1:
            for (EffectMap eff : this.effects) {
               eff.setX(eff.getX() + offset * 24);
            }
      }
   }

   public EffectMapLayer(DrawMapScr drawMapScr, List<EffectMap> effects, List<SubEffectMap> subEffectMaps, int w,
         int h) {
      this.subEffectMaps = subEffectMaps;
      this.effects = effects;
      this.drawMapScr = drawMapScr;
      this.image = new BufferedImage[5];
      this.image[0] = new BufferedImage(w * 24, h * 24, 2);
      this.image[1] = new BufferedImage(w * 24, h * 24, 2);
      this.image[2] = new BufferedImage(w * 24, h * 24, 2);
      this.image[3] = new BufferedImage(w * 24, h * 24, 2);
      this.image[4] = new BufferedImage(w * 24, h * 24, 2);
   }

   public void put(EffectTemplate temp, int x, int y) {
      int layer = this.drawMapScr.layerEff + 1;
      if (x >= 0 && y >= 0 && x <= this.image[layer].getWidth() && y <= this.image[layer].getHeight()) {
         if (this.drawMapScr.layerEff < 1) {
            y -= 240;
         }

         this.effects.add(new EffectMap(temp, x, y, this.drawMapScr.layerEff, -1, 1));
      }
   }

   public void drawWpChose(EffectTemplate temp, int x, int y) {
      if (temp != null) {
         if (System.currentTimeMillis() - this.lastTimeNextF >= (long) this.timeNextF) {
            this.f++;
            this.lastTimeNextF = System.currentTimeMillis();
         }

         if (this.f > temp.getSizeFrame() - 1) {
            this.f = 0;
         }

         Graphics2D g = this.image[this.drawMapScr.layerEff + 1].createGraphics();
         g.drawImage(temp.getFrame(this.f), x - temp.getFrame(this.f).getWidth() / 2,
               y - temp.getFrame(this.f).getHeight(), null);
         g.dispose();
      }
   }

   @Override
   public void draw() {
      try {
         this.clearImage();
         if (!this.isShow()) {
            return;
         }

         for (int i = 0; i < 5; i++) {
            Graphics2D g = this.image[i].createGraphics();

            for (EffectMap eff : this.effects) {
               if (eff.getLayer() + 1 == i) {
                  eff.paint(g, eff.equals(this.drawMapScr.effChose));
               }
            }

            g.dispose();
         }

         Graphics2D g = this.image[4].createGraphics();

         for (SubEffectMap effx : this.subEffectMaps) {
            if (effx.getLayer() + 1 == 4) {
               effx.paint(g, effx.equals(this.drawMapScr.effChose));
            }
         }

         g.dispose();
      } catch (Exception var5) {
      }
   }

   @Override
   public BufferedImage getBufferedImage() {
      BufferedImage image = this.image[this.indexImage];
      this.indexImage++;
      if (this.indexImage > 4) {
         this.indexImage = 0;
      }

      return image;
   }

   @Override
   public void clearImage() {
      for (int i = 0; i < 5; i++) {
         Graphics2D g = this.image[i].createGraphics();
         g.setComposite(AlphaComposite.Clear);
         int x = -this.drawMapScr.camera.camX;
         if (x < 0) {
            x = 0;
         }

         int y = -this.drawMapScr.camera.camY;
         if (y < 0) {
            y = 0;
         }

         int w = this.drawMapScr.camera.width;
         if (w + x > this.image[i].getWidth()) {
            w = this.image[i].getWidth() - x;
         }

         int h = this.drawMapScr.camera.height;
         if (h + y > this.image[i].getHeight()) {
            h = this.image[i].getHeight() - y;
         }

         g.fillRect(x, y, w, h);
         g.dispose();
      }
   }

   @Override
   public void setShow(boolean show) {
      this.show = show;
   }

   @Override
   public boolean isShow() {
      return this.show;
   }
}
