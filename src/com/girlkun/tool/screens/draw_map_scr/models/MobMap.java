package com.girlkun.tool.screens.draw_map_scr.models;

import com.girlkun.tool.entities.map.MobTemplate;
import com.girlkun.tool.utils.Util;
import java.awt.Graphics2D;

public class MobMap {
   private MobTemplate temp;
   private int x;
   private int y;
   private int level;
   private int hp;
   private long lastTimeNextF;
   private int f;
   private int timeNextF = 50;
   private int minY;
   private int maxY;
   private int cY;
   private int dirY = 1;
   private int minX;
   private int maxX;
   private int cX;
   private int dirX = 1;

   public void setX(int x) {
      this.x = x;
      this.cX = x;
      this.cY = this.y;
      if (this.temp.getType() == 0) {
         this.minX = this.cX;
         this.maxX = this.cX;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else if (this.temp.getType() == 1) {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY - this.temp.getRangeMove() / 4 * 2;
         this.maxY = this.cY + this.temp.getRangeMove() / 4 * 2;
      }
   }

   public void setY(int y) {
      this.y = y;
      this.cX = this.x;
      this.cY = y;
      if (this.temp.getType() == 0) {
         this.minX = this.cX;
         this.maxX = this.cX;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else if (this.temp.getType() == 1) {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY - this.temp.getRangeMove() / 4 * 2;
         this.maxY = this.cY + this.temp.getRangeMove() / 4 * 2;
      }
   }

   public MobMap(MobTemplate temp, int x, int y, int level, int hp) {
      this.temp = temp;
      this.x = x;
      this.y = y;
      this.level = level;
      this.hp = hp;
      this.cX = x;
      this.cY = y;
      if (this.temp.getType() == 0) {
         this.minX = this.cX;
         this.maxX = this.cX;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else if (this.temp.getType() == 1) {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY;
         this.maxY = this.cY;
      } else {
         this.minX = this.cX - this.temp.getRangeMove() / 4 * 3;
         this.maxX = this.cX + this.temp.getRangeMove() / 4 * 3;
         this.minY = this.cY - this.temp.getRangeMove() / 4 * 2;
         this.maxY = this.cY + this.temp.getRangeMove() / 4 * 2;
      }
   }

   public void paint(Graphics2D g, boolean changeColor) {
      if (Util.canDoLastTime(this.lastTimeNextF, this.timeNextF)) {
         this.lastTimeNextF = System.currentTimeMillis();
         this.f++;
         if (this.f > 2) {
            this.f = 0;
         }

         if (this.temp.getType() != 0) {
            this.cX = this.cX + this.dirX * this.temp.getSpeed();
         }

         if (this.temp.getType() == 4) {
            this.cY = this.cY + this.dirY * this.temp.getSpeed();
         }

         if (this.cX < this.minX || this.cX > this.maxX) {
            this.dirX *= -1;
         }

         if (this.cY < this.minY || this.cY > this.maxY) {
            this.dirY *= -1;
         }
      }

      try {
         if (this.dirX > 0) {
            g.drawImage(
               changeColor ? Util.changeRed(this.temp.getImages()[this.f]) : this.temp.getImages()[this.f],
               this.cX - this.temp.getImages()[this.f].getWidth() / 2,
               this.cY - this.temp.getImages()[this.f].getHeight() / 4 * 3,
               null
            );
         } else {
            int w = this.temp.getImages()[this.f].getWidth();
            int h = this.temp.getImages()[this.f].getHeight();
            int aX = this.cX - w / 2;
            int aY = this.cY - h / 4 * 3;
            g.drawImage(changeColor ? Util.changeRed(this.temp.getImages()[this.f]) : this.temp.getImages()[this.f], aX + w, aY, -w, h, null);
         }
      } catch (Exception var7) {
      }
   }

   public MobTemplate getTemp() {
      return this.temp;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getLevel() {
      return this.level;
   }

   public int getHp() {
      return this.hp;
   }

   public long getLastTimeNextF() {
      return this.lastTimeNextF;
   }

   public int getF() {
      return this.f;
   }

   public int getTimeNextF() {
      return this.timeNextF;
   }

   public int getMinY() {
      return this.minY;
   }

   public int getMaxY() {
      return this.maxY;
   }

   public int getCY() {
      return this.cY;
   }

   public int getDirY() {
      return this.dirY;
   }

   public int getMinX() {
      return this.minX;
   }

   public int getMaxX() {
      return this.maxX;
   }

   public int getCX() {
      return this.cX;
   }

   public int getDirX() {
      return this.dirX;
   }

   public void setTemp(MobTemplate temp) {
      this.temp = temp;
   }

   public void setLevel(int level) {
      this.level = level;
   }

   public void setHp(int hp) {
      this.hp = hp;
   }

   public void setLastTimeNextF(long lastTimeNextF) {
      this.lastTimeNextF = lastTimeNextF;
   }

   public void setF(int f) {
      this.f = f;
   }

   public void setTimeNextF(int timeNextF) {
      this.timeNextF = timeNextF;
   }

   public void setMinY(int minY) {
      this.minY = minY;
   }

   public void setMaxY(int maxY) {
      this.maxY = maxY;
   }

   public void setCY(int cY) {
      this.cY = cY;
   }

   public void setDirY(int dirY) {
      this.dirY = dirY;
   }

   public void setMinX(int minX) {
      this.minX = minX;
   }

   public void setMaxX(int maxX) {
      this.maxX = maxX;
   }

   public void setCX(int cX) {
      this.cX = cX;
   }

   public void setDirX(int dirX) {
      this.dirX = dirX;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MobMap)) {
         return false;
      } else {
         MobMap other = (MobMap)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getX() != other.getX()) {
            return false;
         } else if (this.getY() != other.getY()) {
            return false;
         } else if (this.getLevel() != other.getLevel()) {
            return false;
         } else if (this.getHp() != other.getHp()) {
            return false;
         } else if (this.getLastTimeNextF() != other.getLastTimeNextF()) {
            return false;
         } else if (this.getF() != other.getF()) {
            return false;
         } else if (this.getTimeNextF() != other.getTimeNextF()) {
            return false;
         } else if (this.getMinY() != other.getMinY()) {
            return false;
         } else if (this.getMaxY() != other.getMaxY()) {
            return false;
         } else if (this.getCY() != other.getCY()) {
            return false;
         } else if (this.getDirY() != other.getDirY()) {
            return false;
         } else if (this.getMinX() != other.getMinX()) {
            return false;
         } else if (this.getMaxX() != other.getMaxX()) {
            return false;
         } else if (this.getCX() != other.getCX()) {
            return false;
         } else if (this.getDirX() != other.getDirX()) {
            return false;
         } else {
            Object this$temp = this.getTemp();
            Object other$temp = other.getTemp();
            return this$temp == null ? other$temp == null : this$temp.equals(other$temp);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof MobMap;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getX();
      result = result * 59 + this.getY();
      result = result * 59 + this.getLevel();
      result = result * 59 + this.getHp();
      long $lastTimeNextF = this.getLastTimeNextF();
      result = result * 59 + (int)($lastTimeNextF >>> 32 ^ $lastTimeNextF);
      result = result * 59 + this.getF();
      result = result * 59 + this.getTimeNextF();
      result = result * 59 + this.getMinY();
      result = result * 59 + this.getMaxY();
      result = result * 59 + this.getCY();
      result = result * 59 + this.getDirY();
      result = result * 59 + this.getMinX();
      result = result * 59 + this.getMaxX();
      result = result * 59 + this.getCX();
      result = result * 59 + this.getDirX();
      Object $temp = this.getTemp();
      return result * 59 + ($temp == null ? 43 : $temp.hashCode());
   }

   @Override
   public String toString() {
      return "MobMap(temp="
         + this.getTemp()
         + ", x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", level="
         + this.getLevel()
         + ", hp="
         + this.getHp()
         + ", lastTimeNextF="
         + this.getLastTimeNextF()
         + ", f="
         + this.getF()
         + ", timeNextF="
         + this.getTimeNextF()
         + ", minY="
         + this.getMinY()
         + ", maxY="
         + this.getMaxY()
         + ", cY="
         + this.getCY()
         + ", dirY="
         + this.getDirY()
         + ", minX="
         + this.getMinX()
         + ", maxX="
         + this.getMaxX()
         + ", cX="
         + this.getCX()
         + ", dirX="
         + this.getDirX()
         + ")";
   }
}
