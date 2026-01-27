package com.girlkun.tool.screens.draw_map_scr.layer;

import com.girlkun.tool.entities.map.NpcTemplate;
import com.girlkun.tool.screens.draw_map_scr.DrawMapScr;
import com.girlkun.tool.screens.draw_map_scr.models.NpcMap;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class NpcMapLayer implements Layer {
   private BufferedImage image;
   private DrawMapScr drawMapScr;
   private List<NpcMap> npcs;
   private NpcMap npcChose = new NpcMap(null, 0, 0);
   private boolean show = true;

   @Override
   public void clear() {
      this.npcs.clear();
   }

   public NpcMapLayer(DrawMapScr drawMapScr, List<NpcMap> npcs, int w, int h) {
      this.image = this.image;
      this.npcs = npcs;
      this.drawMapScr = drawMapScr;
      this.image = new BufferedImage(w * 24, h * 24, 2);
   }

   @Override
   public void setSizeImage(int w, int h, int offset, int dir) {
      this.image = new BufferedImage(w * 24, h * 24, 2);
      switch (dir) {
         case 0:
            for (NpcMap npc : this.npcs) {
               npc.setY(npc.getY() + offset * 24);
            }
            break;
         case 1:
            for (NpcMap npc : this.npcs) {
               npc.setX(npc.getX() + offset * 24);
            }
      }
   }

   public void put(NpcTemplate temp, int x, int y) {
      if (temp != null) {
         y = y / 24 * 24;
         if (x >= 0 && y >= 0 && x <= this.image.getWidth() && y <= this.image.getHeight()) {
            this.npcs.add(new NpcMap(temp, x, y));
         }
      }
   }

   public void drawNpcChoose(NpcTemplate temp, int x, int y) {
      y = y / 24 * 24;
      Graphics2D g = this.image.createGraphics();

      try {
         this.npcChose.setTemp(temp);
         this.npcChose.setX(x);
         this.npcChose.setY(y);
         this.npcChose.paint(g, false);
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      g.dispose();
   }

   @Override
   public void draw() {
      this.clearImage();
      if (this.isShow()) {
         Graphics2D g = this.image.createGraphics();

         for (NpcMap npc : this.npcs) {
            npc.paint(g, npc.equals(this.drawMapScr.npcMapChose));
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
