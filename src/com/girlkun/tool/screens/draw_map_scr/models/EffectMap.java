package com.girlkun.tool.screens.draw_map_scr.models;

import com.girlkun.tool.entities.EffectTemplate;
import com.girlkun.tool.utils.Util;
import java.awt.Graphics2D;

public class EffectMap {
   private EffectTemplate temp;
   private int x;
   private int y;
   private int layer;
   private int loop;
   private int loopCount;
   private int type;
   private int indexFrom;
   private int indexTo;
   private long lastTimeNextFrame;
   private int timeNextFrame = 50;
   private int f;
   private int iLoopCount;

   public EffectMap(EffectTemplate temp, int x, int y, int layer, int loop, int loopCount) {
      this.temp = temp;
      this.x = x;
      this.y = y;
      this.layer = layer;
      this.loop = loop;
      this.loopCount = loopCount;
   }

   public void paint(Graphics2D g, boolean changeColor) {
      try {
         if (System.currentTimeMillis() - this.lastTimeNextFrame > (long)this.timeNextFrame) {
            this.f++;
            if (this.f >= this.temp.getSizeFrame()) {
               if (this.loopCount == -1) {
                  this.f = 0;
               } else {
                  this.iLoopCount++;
                  if (this.iLoopCount >= this.loopCount) {
                     this.iLoopCount = 0;
                     this.f = 0;
                  } else {
                     this.f = this.temp.getSizeFrame() - 1;
                  }
               }
            }

            this.lastTimeNextFrame = System.currentTimeMillis();
         }

         int cy = this.y - this.temp.getFrame(this.f).getHeight();
         if (this.layer < 1) {
            cy += 240;
         }

         g.drawImage(
            changeColor ? Util.changeRed(this.temp.getFrame()[this.f]) : this.temp.getFrame(this.f),
            this.x - this.temp.getFrame(this.f).getWidth() / 2,
            cy,
            null
         );
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public EffectTemplate getTemp() {
      return this.temp;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getLayer() {
      return this.layer;
   }

   public int getLoop() {
      return this.loop;
   }

   public int getLoopCount() {
      return this.loopCount;
   }

   public int getType() {
      return this.type;
   }

   public int getIndexFrom() {
      return this.indexFrom;
   }

   public int getIndexTo() {
      return this.indexTo;
   }

   public long getLastTimeNextFrame() {
      return this.lastTimeNextFrame;
   }

   public int getTimeNextFrame() {
      return this.timeNextFrame;
   }

   public int getF() {
      return this.f;
   }

   public int getILoopCount() {
      return this.iLoopCount;
   }

   public void setTemp(EffectTemplate temp) {
      this.temp = temp;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setLayer(int layer) {
      this.layer = layer;
   }

   public void setLoop(int loop) {
      this.loop = loop;
   }

   public void setLoopCount(int loopCount) {
      this.loopCount = loopCount;
   }

   public void setType(int type) {
      this.type = type;
   }

   public void setIndexFrom(int indexFrom) {
      this.indexFrom = indexFrom;
   }

   public void setIndexTo(int indexTo) {
      this.indexTo = indexTo;
   }

   public void setLastTimeNextFrame(long lastTimeNextFrame) {
      this.lastTimeNextFrame = lastTimeNextFrame;
   }

   public void setTimeNextFrame(int timeNextFrame) {
      this.timeNextFrame = timeNextFrame;
   }

   public void setF(int f) {
      this.f = f;
   }

   public void setILoopCount(int iLoopCount) {
      this.iLoopCount = iLoopCount;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof EffectMap)) {
         return false;
      } else {
         EffectMap other = (EffectMap)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getX() != other.getX()) {
            return false;
         } else if (this.getY() != other.getY()) {
            return false;
         } else if (this.getLayer() != other.getLayer()) {
            return false;
         } else if (this.getLoop() != other.getLoop()) {
            return false;
         } else if (this.getLoopCount() != other.getLoopCount()) {
            return false;
         } else if (this.getType() != other.getType()) {
            return false;
         } else if (this.getIndexFrom() != other.getIndexFrom()) {
            return false;
         } else if (this.getIndexTo() != other.getIndexTo()) {
            return false;
         } else if (this.getLastTimeNextFrame() != other.getLastTimeNextFrame()) {
            return false;
         } else if (this.getTimeNextFrame() != other.getTimeNextFrame()) {
            return false;
         } else if (this.getF() != other.getF()) {
            return false;
         } else if (this.getILoopCount() != other.getILoopCount()) {
            return false;
         } else {
            Object this$temp = this.getTemp();
            Object other$temp = other.getTemp();
            return this$temp == null ? other$temp == null : this$temp.equals(other$temp);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof EffectMap;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getX();
      result = result * 59 + this.getY();
      result = result * 59 + this.getLayer();
      result = result * 59 + this.getLoop();
      result = result * 59 + this.getLoopCount();
      result = result * 59 + this.getType();
      result = result * 59 + this.getIndexFrom();
      result = result * 59 + this.getIndexTo();
      long $lastTimeNextFrame = this.getLastTimeNextFrame();
      result = result * 59 + (int)($lastTimeNextFrame >>> 32 ^ $lastTimeNextFrame);
      result = result * 59 + this.getTimeNextFrame();
      result = result * 59 + this.getF();
      result = result * 59 + this.getILoopCount();
      Object $temp = this.getTemp();
      return result * 59 + ($temp == null ? 43 : $temp.hashCode());
   }

   @Override
   public String toString() {
      return "EffectMap(temp="
         + this.getTemp()
         + ", x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", layer="
         + this.getLayer()
         + ", loop="
         + this.getLoop()
         + ", loopCount="
         + this.getLoopCount()
         + ", type="
         + this.getType()
         + ", indexFrom="
         + this.getIndexFrom()
         + ", indexTo="
         + this.getIndexTo()
         + ", lastTimeNextFrame="
         + this.getLastTimeNextFrame()
         + ", timeNextFrame="
         + this.getTimeNextFrame()
         + ", f="
         + this.getF()
         + ", iLoopCount="
         + this.getILoopCount()
         + ")";
   }
}
