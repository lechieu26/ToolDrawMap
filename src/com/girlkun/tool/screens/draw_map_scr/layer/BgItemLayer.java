package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.map.BgItemTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.draw_map_scr.models.BgItemMap;
import com.girlkun.tool.utils.DrawUtil;
import com.girlkun.tool.utils.Util;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BgItemLayer implements Layer {
   private BufferedImage image;
   private List<BgItemMap> bgItemMaps;
   private DrawMapScr drawMapScr;
   private boolean show = true;

   @Override
   public void clear() {
      this.bgItemMaps.clear();
   }

   public BgItemLayer(DrawMapScr drawMapScr, List<BgItemMap> bgItemMaps, int w, int h) {
      this.drawMapScr = drawMapScr;
      this.bgItemMaps = bgItemMaps;
      this.image = new BufferedImage(w * 24, h * 24, 2);
   }

   @Override
   public void setSizeImage(int w, int h, int offset, int dir) {
      this.image = new BufferedImage(w * 24, h * 24, 2);
      switch (dir) {
         case 0:
            for (BgItemMap bg : this.bgItemMaps) {
               bg.setY(bg.getY() + offset * 24);
            }
            break;
         case 1:
            for (BgItemMap bg : this.bgItemMaps) {
               bg.setX(bg.getX() + offset * 24);
            }
      }
   }

   public void putBgItem(BgItemTemplate temp, int x, int y) {
      if (temp != null) {
         x = x / 24 * 24;
         y = y / 24 * 24;
         if (x >= 0 && y >= 0 && x < this.image.getWidth() && y < this.image.getHeight()) {
            BgItemMap newBg = new BgItemMap(temp, x, y);
            this.bgItemMaps.add(newBg);
            if (this.drawMapScr.bGItemList != null) {
               this.drawMapScr.bGItemList.fillToTable();
            }
         }
      }
   }

   @Override
   public void draw() {
      this.clearImage();
      if (this.isShow()) {
         try {
            Graphics2D g = this.image.createGraphics();

            for (BgItemMap bgItemMap : this.bgItemMaps) {
               if (bgItemMap != null) {
                  bgItemMap.draw(g, this.drawMapScr);
                  if (this.drawMapScr.bgChose != null && (this.drawMapScr.bgChose == bgItemMap || this.drawMapScr.bgChose.equals(bgItemMap))) {
                     try {
                        BufferedImage img = bgItemMap.getTemp() != null ? bgItemMap.getTemp().getImage() : null;
                        if (img != null) {
                           int drawX = bgItemMap.getX() + bgItemMap.getTemp().getDx()
                                 - (this.drawMapScr.is3D && bgItemMap.getTemp().getLayer() == 4 ? this.drawMapScr.camera.camX / 10 : 0);
                           int drawY = bgItemMap.getY() + bgItemMap.getTemp().getDy();
                           g.setColor(Color.RED);
                           g.setStroke(new BasicStroke(2.0F));
                           g.drawRect(drawX, drawY, img.getWidth(), img.getHeight());
                        }
                     } catch (Exception var11) {
                        Logger.getLogger(BgItemLayer.class.getName()).log(Level.SEVERE, null, var11);
                     }
                  }
               }
            }

            g.dispose();
         } catch (Exception var12) {
         }
      }
   }

   @Override
   public BufferedImage getBufferedImage() {
      return this.image;
   }

   @Override
   public void clearImage() {
      Graphics2D g = this.image.createGraphics();
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
      if (w + x > this.image.getWidth()) {
         w = this.image.getWidth() - x;
      }

      int h = this.drawMapScr.camera.height;
      if (h + y > this.image.getHeight()) {
         h = this.image.getHeight() - y;
      }

      g.fillRect(x, y, w, h);
      g.dispose();
   }

   public void drawBGChose(BgItemTemplate temp, int x, int y) {
      if (temp != null) {
         Graphics2D g = this.image.createGraphics();

         try {
            DrawUtil.drawImage(
                  g,
                  temp.getImage(),
                  x / 24 * 24 + temp.getDx()
                        - (this.drawMapScr.is3D && temp.getLayer() == 4 ? this.drawMapScr.camera.camX / 10 : 0),
                  y / 24 * 24 + temp.getDy());
         } catch (Exception var6) {
            var6.printStackTrace();
         }

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
