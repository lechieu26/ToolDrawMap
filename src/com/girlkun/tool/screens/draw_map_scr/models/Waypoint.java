package com.girlkun.tool.screens.draw_map_scr.models;

public class Waypoint {
   private String name;
   private int x;
   private int y;
   private int w;
   private int h;
   private boolean enter;
   private boolean offline;
   private int mapGo;
   private int goX;
   private int goY;

   public String getName() {
      return this.name;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getW() {
      return this.w;
   }

   public int getH() {
      return this.h;
   }

   public boolean isEnter() {
      return this.enter;
   }

   public boolean isOffline() {
      return this.offline;
   }

   public int getMapGo() {
      return this.mapGo;
   }

   public int getGoX() {
      return this.goX;
   }

   public int getGoY() {
      return this.goY;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public void setW(int w) {
      this.w = w;
   }

   public void setH(int h) {
      this.h = h;
   }

   public void setEnter(boolean enter) {
      this.enter = enter;
   }

   public void setOffline(boolean offline) {
      this.offline = offline;
   }

   public void setMapGo(int mapGo) {
      this.mapGo = mapGo;
   }

   public void setGoX(int goX) {
      this.goX = goX;
   }

   public void setGoY(int goY) {
      this.goY = goY;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof Waypoint)) {
         return false;
      } else {
         Waypoint other = (Waypoint)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getX() != other.getX()) {
            return false;
         } else if (this.getY() != other.getY()) {
            return false;
         } else if (this.getW() != other.getW()) {
            return false;
         } else if (this.getH() != other.getH()) {
            return false;
         } else if (this.isEnter() != other.isEnter()) {
            return false;
         } else if (this.isOffline() != other.isOffline()) {
            return false;
         } else if (this.getMapGo() != other.getMapGo()) {
            return false;
         } else if (this.getGoX() != other.getGoX()) {
            return false;
         } else if (this.getGoY() != other.getGoY()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            return this$name == null ? other$name == null : this$name.equals(other$name);
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof Waypoint;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getX();
      result = result * 59 + this.getY();
      result = result * 59 + this.getW();
      result = result * 59 + this.getH();
      result = result * 59 + (this.isEnter() ? 79 : 97);
      result = result * 59 + (this.isOffline() ? 79 : 97);
      result = result * 59 + this.getMapGo();
      result = result * 59 + this.getGoX();
      result = result * 59 + this.getGoY();
      Object $name = this.getName();
      return result * 59 + ($name == null ? 43 : $name.hashCode());
   }

   @Override
   public String toString() {
      return "Waypoint(name="
         + this.getName()
         + ", x="
         + this.getX()
         + ", y="
         + this.getY()
         + ", w="
         + this.getW()
         + ", h="
         + this.getH()
         + ", enter="
         + this.isEnter()
         + ", offline="
         + this.isOffline()
         + ", mapGo="
         + this.getMapGo()
         + ", goX="
         + this.getGoX()
         + ", goY="
         + this.getGoY()
         + ")";
   }

   public Waypoint(String name, int x, int y, int w, int h, boolean enter, boolean offline, int mapGo, int goX, int goY) {
      this.name = name;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.enter = enter;
      this.offline = offline;
      this.mapGo = mapGo;
      this.goX = goX;
      this.goY = goY;
   }
}
