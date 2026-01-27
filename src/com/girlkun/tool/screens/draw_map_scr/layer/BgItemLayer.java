package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.map.BgItemTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.draw_map_scr.models.BgItemMap;
import com.girlkun.tool.utils.DrawUtil;
import com.girlkun.tool.utils.Util;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
      x = x / 24 * 24;
      y = y / 24 * 24;
      if (x >= 0 && y >= 0 && x < this.image.getWidth() && y < this.image.getHeight()) {
         this.bgItemMaps.add(0, new BgItemMap(temp, x, y));
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
                  if (this.drawMapScr.bgChose != null && this.drawMapScr.bgChose.equals(bgItemMap)) {
                     try {
                        BufferedImage img = Util.getBgImageById(bgItemMap.getTemp().getImageId(), 1);
                        BufferedImage i = new BufferedImage(img.getWidth(), img.getHeight(), 2);
                        Graphics2D gi = (Graphics2D) i.getGraphics();
                        gi.drawImage(img, 0, 0, null);
                        WritableRaster raster = i.getRaster();

                        for (int xx = 0; xx < i.getWidth(); xx++) {
                           for (int yy = 0; yy < i.getHeight(); yy++) {
                              int[] pixels = raster.getPixel(xx, yy, (int[]) null);
                              pixels[0] = 255;
                              pixels[1] = 0;
                              pixels[2] = 0;
                              raster.setPixel(xx, yy, pixels);
                           }
                        }

                        g.drawImage(i, bgItemMap.getX() + bgItemMap.getTemp().getDx(),
                              bgItemMap.getY() + bgItemMap.getTemp().getDy(), null);
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
