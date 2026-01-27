package com.girlkun.tool.entities.map;

public class HeadAvatar {
   private int head;
   private int avatar;

   public int getHead() {
      return this.head;
   }

   public int getAvatar() {
      return this.avatar;
   }

   public void setHead(int head) {
      this.head = head;
   }

   public void setAvatar(int avatar) {
      this.avatar = avatar;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof HeadAvatar)) {
         return false;
      } else {
         HeadAvatar other = (HeadAvatar)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            return this.getHead() != other.getHead() ? false : this.getAvatar() == other.getAvatar();
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof HeadAvatar;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getHead();
      return result * 59 + this.getAvatar();
   }

   @Override
   public String toString() {
      return "HeadAvatar(head=" + this.getHead() + ", avatar=" + this.getAvatar() + ")";
   }

   public HeadAvatar(int head, int avatar) {
      this.head = head;
      this.avatar = avatar;
   }
}
