package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.map.MobTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.draw_map_scr.models.MobMap;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class MobMapLayer implements Layer {
   private BufferedImage image;
   private DrawMapScr drawMapScr;
   private List<MobMap> mobs;
   private int iFlag;
   private boolean show = true;

   @Override
   public void clear() {
      this.mobs.clear();
   }

   public MobMapLayer(DrawMapScr drawMapScr, List<MobMap> mobs, int w, int h) {
      this.mobs = mobs;
      this.drawMapScr = drawMapScr;
      this.image = new BufferedImage(w * 24, h * 24, 2);
   }

   @Override
   public void setSizeImage(int w, int h, int offset, int dir) {
      this.image = new BufferedImage(w * 24, h * 24, 2);
      switch (dir) {
         case 0:
            for (MobMap mob : this.mobs) {
               mob.setY(mob.getY() + offset * 24);
            }
            break;
         case 1:
            for (MobMap mob : this.mobs) {
               mob.setX(mob.getX() + offset * 24);
            }
      }
   }

   public void put(MobTemplate temp, int x, int y) {
      if (x >= 0 && y >= 0 && x <= this.image.getWidth() && y <= this.image.getHeight()) {
         if (temp.getId() != 4) {
            y = y / 24 * 24;
         }

         int hp = 752002;
         int level = 1;
         this.mobs.add(new MobMap(temp, x, y, level, hp));
      }
   }

   public void drawMobChoose(MobTemplate temp, int x, int y) {
      if (temp != null) {
         if (temp.getId() != 4) {
            y = y / 24 * 24;
         }

         Graphics2D g = this.image.createGraphics();

         try {
            int minX;
            int minY;
            int maxX;
            int maxY;
            if (temp.getType() == 0) {
               minX = x;
               maxX = x;
               minY = y;
               maxY = y;
            } else if (temp.getType() == 1) {
               minX = x - temp.getRangeMove() / 4 * 3;
               maxX = x + temp.getRangeMove() / 4 * 3;
               minY = y;
               maxY = y;
            } else {
               minX = x - temp.getRangeMove() / 4 * 3;
               maxX = x + temp.getRangeMove() / 4 * 3;
               minY = y - temp.getRangeMove() / 4 * 2;
               maxY = y + temp.getRangeMove() / 4 * 2;
            }

            g.drawImage(temp.getImages()[0], x - temp.getImages()[0].getWidth() / 2,
                  y - temp.getImages()[0].getHeight() / 4 * 3, null);
            g.setStroke(new BasicStroke(2.0F));
            this.iFlag++;
            if (this.iFlag >= 10) {
               this.iFlag = 0;
            }

            if (this.iFlag > 5) {
               g.setColor(Color.red);
            } else {
               g.setColor(Color.white);
            }

            g.drawLine(minX - 2, y - 2, minX + 2, y + 2);
            g.drawLine(minX + 2, y - 2, minX - 2, y + 2);
            g.drawLine(maxX - 2, y - 2, maxX + 2, y + 2);
            g.drawLine(maxX + 2, y - 2, maxX - 2, y + 2);
            if (temp.getType() == 4) {
               g.drawLine(x - 2, minY - 2, x + 2, minY + 2);
               g.drawLine(x + 2, minY - 2, x - 2, minY + 2);
               g.drawLine(x - 2, maxY - 2, x + 2, maxY + 2);
               g.drawLine(x + 2, maxY - 2, x - 2, maxY + 2);
            }
         } catch (Exception var9) {
            var9.printStackTrace();
         }

         g.dispose();
      }
   }

   @Override
   public void draw() {
      this.clearImage();
      if (this.isShow()) {
         Graphics2D g = this.image.createGraphics();

         for (MobMap mob : this.mobs) {
            mob.paint(g, mob.equals(this.drawMapScr.mobMapChose));
         }

         g.dispose();
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

   @Override
   public void setShow(boolean show) {
      this.show = show;
   }

   @Override
   public boolean isShow() {
      return this.show;
   }
}
