package com.girlkun.tool.screens.draw_map_scr.models;

import com.girlkun.tool.entities.map.NpcTemplate;
import com.girlkun.tool.utils.CharInfo;
import com.girlkun.tool.utils.Util;
import java.awt.Graphics2D;

public class NpcMap {
   private NpcTemplate temp;
   private int x;
   private int y;
   private long lastTimeUpdate;
   private int timeUpdate = 150;
   private int dY;

   public NpcMap(NpcTemplate temp, int x, int y) {
      this.temp = temp;
      this.x = x;
      this.y = y;
   }

   public void paint(Graphics2D g, boolean changeColor) {
      if (Util.canDoLastTime(this.lastTimeUpdate, this.timeUpdate)) {
         this.lastTimeUpdate = System.currentTimeMillis();
         this.dY++;
         if (this.dY > 2) {
            this.dY = 0;
         }
      }

      try {
         g.drawImage(
            changeColor ? Util.changeRed(this.temp.getILeg()) : this.temp.getILeg(),
            this.x + this.temp.getPLeg().getPi()[1].getDx(),
            this.y - CharInfo.CharInfo[0][1][2] + this.temp.getPLeg().getPi()[1].getDy(),
            null
         );
         g.drawImage(
            changeColor ? Util.changeRed(this.temp.getIBody()) : this.temp.getIBody(),
            this.x + this.temp.getPBody().getPi()[1].getDx(),
            this.y - CharInfo.CharInfo[0][2][2] + this.temp.getPBody().getPi()[1].getDy() + this.dY,
            null
         );
         g.drawImage(
            changeColor ? Util.changeRed(this.temp.getIHead()) : this.temp.getIHead(),
            this.x + this.temp.getPHead().getPi()[0].getDx() - 4,
            this.y - CharInfo.CharInfo[0][0][2] + this.temp.getPHead().getPi()[0].getDy() + this.dY,
            null
         );
      } catch (Exception var4) {
      }
   }

   public NpcTemplate getTemp() {
      return this.temp;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public long getLastTimeUpdate() {
      return this.lastTimeUpdate;
   }

   public int getTimeUpdate() {
      return this.timeUpdate;
   }

   public int getDY() {
      return this.dY;
   }

   public void setTemp(NpcTemplate temp) {
      this.temp = temp;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setLastTimeUpdate(long lastTimeUpdate) {
      this.lastTimeUpdate = lastTimeUpdate;
   }

   public void setTimeUpdate(int timeUpdate) {
      this.timeUpdate = timeUpdate;
   }

   public void setDY(int dY) {
      this.dY = dY;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof NpcMap)) {
         return false;
      } else {
         NpcMap other = (NpcMap)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getX() != other.getX()) {
            return false;
         } else if (this.getY() != other.getY()) {
            return false;
         } else if (this.getLastTimeUpdate() != other.getLastTimeUpdate()) {
            return false;
         } else if (this.getTimeUpdate() != other.getTimeUpdate()) {
            return false;
         } else if (this.getDY() != other.getDY()) {
            return false;
         } else {
            Object this$temp = this.getTemp();
            Object other$temp = other.getTemp();
            return this$temp == null ? other$temp == null : this$temp.equals(other$temp);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof NpcMap;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getX();
      result = result * 59 + this.getY();
      long $lastTimeUpdate = this.getLastTimeUpdate();
      result = result * 59 + (int)($lastTimeUpdate >>> 32 ^ $lastTimeUpdate);
      result = result * 59 + this.getTimeUpdate();
      result = result * 59 + this.getDY();
      Object $temp = this.getTemp();
      return result * 59 + ($temp == null ? 43 : $temp.hashCode());
   }

   @Override
   public String toString() {
      return "NpcMap(temp="
         + this.getTemp()
         + ", x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", lastTimeUpdate="
         + this.getLastTimeUpdate()
         + ", timeUpdate="
         + this.getTimeUpdate()
         + ", dY="
         + this.getDY()
         + ")";
   }
}
