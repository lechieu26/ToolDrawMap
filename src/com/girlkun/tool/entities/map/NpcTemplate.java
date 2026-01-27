package com.girlkun.tool.entities.map;

import com.girlkun.tool.screens.part_scr.models.Part;
import com.girlkun.tool.utils.Util;
import java.awt.image.BufferedImage;

public class NpcTemplate {
   private int id;
   private String name;
   private int head;
   private int body;
   private int leg;
   private int avatar;
   private Part pHead;
   private Part pBody;
   private Part pLeg;
   private BufferedImage iHead;
   private BufferedImage iBody;
   private BufferedImage iLeg;
   private boolean eH;
   private boolean eB;
   private boolean eL;

   public NpcTemplate(int id, String name, int head, int body, int leg, int avatar) {
      this.id = id;
      this.name = name;
      this.head = head;
      this.body = body;
      this.leg = leg;
      this.avatar = avatar;
   }

   public BufferedImage getIHead() {
      if (this.iHead == null && !this.eH) {
         try {
            this.pHead = Part.getPart(this.head);
            this.iHead = Util.getImageById(this.pHead.getPi()[0].getIconId(), 1);
         } catch (Exception var2) {
            this.eH = true;
         }
      }

      return this.iHead;
   }

   public BufferedImage getIBody() {
      if (this.iBody == null && !this.eB) {
         try {
            this.pBody = Part.getPart(this.body);
            this.iBody = Util.getImageById(this.pBody.getPi()[1].getIconId(), 1);
         } catch (Exception var2) {
            this.eB = true;
         }
      }

      return this.iBody;
   }

   public BufferedImage getILeg() {
      if (this.iLeg == null && !this.eL) {
         try {
            this.pLeg = Part.getPart(this.leg);
            this.iLeg = Util.getImageById(this.pLeg.getPi()[1].getIconId(), 1);
         } catch (Exception var2) {
            this.eL = true;
         }
      }

      return this.iLeg;
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public int getHead() {
      return this.head;
   }

   public int getBody() {
      return this.body;
   }

   public int getLeg() {
      return this.leg;
   }

   public int getAvatar() {
      return this.avatar;
   }

   public Part getPHead() {
      return this.pHead;
   }

   public Part getPBody() {
      return this.pBody;
   }

   public Part getPLeg() {
      return this.pLeg;
   }

   public boolean isEH() {
      return this.eH;
   }

   public boolean isEB() {
      return this.eB;
   }

   public boolean isEL() {
      return this.eL;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setHead(int head) {
      this.head = head;
   }

   public void setBody(int body) {
      this.body = body;
   }

   public void setLeg(int leg) {
      this.leg = leg;
   }

   public void setAvatar(int avatar) {
      this.avatar = avatar;
   }

   public void setPHead(Part pHead) {
      this.pHead = pHead;
   }

   public void setPBody(Part pBody) {
      this.pBody = pBody;
   }

   public void setPLeg(Part pLeg) {
      this.pLeg = pLeg;
   }

   public void setIHead(BufferedImage iHead) {
      this.iHead = iHead;
   }

   public void setIBody(BufferedImage iBody) {
      this.iBody = iBody;
   }

   public void setILeg(BufferedImage iLeg) {
      this.iLeg = iLeg;
   }

   public void setEH(boolean eH) {
      this.eH = eH;
   }

   public void setEB(boolean eB) {
      this.eB = eB;
   }

   public void setEL(boolean eL) {
      this.eL = eL;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof NpcTemplate)) {
         return false;
      } else {
         NpcTemplate other = (NpcTemplate)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getId() != other.getId()) {
            return false;
         } else if (this.getHead() != other.getHead()) {
            return false;
         } else if (this.getBody() != other.getBody()) {
            return false;
         } else if (this.getLeg() != other.getLeg()) {
            return false;
         } else if (this.getAvatar() != other.getAvatar()) {
            return false;
         } else if (this.isEH() != other.isEH()) {
            return false;
         } else if (this.isEB() != other.isEB()) {
            return false;
         } else if (this.isEL() != other.isEL()) {
            return false;
         } else {
            Object this$name = this.getName();
            Object other$name = other.getName();
            if (this$name == null ? other$name == null : this$name.equals(other$name)) {
               Object this$pHead = this.getPHead();
               Object other$pHead = other.getPHead();
               if (this$pHead == null ? other$pHead == null : this$pHead.equals(other$pHead)) {
                  Object this$pBody = this.getPBody();
                  Object other$pBody = other.getPBody();
                  if (this$pBody == null ? other$pBody == null : this$pBody.equals(other$pBody)) {
                     Object this$pLeg = this.getPLeg();
                     Object other$pLeg = other.getPLeg();
                     if (this$pLeg == null ? other$pLeg == null : this$pLeg.equals(other$pLeg)) {
                        Object this$iHead = this.getIHead();
                        Object other$iHead = other.getIHead();
                        if (this$iHead == null ? other$iHead == null : this$iHead.equals(other$iHead)) {
                           Object this$iBody = this.getIBody();
                           Object other$iBody = other.getIBody();
                           if (this$iBody == null ? other$iBody == null : this$iBody.equals(other$iBody)) {
                              Object this$iLeg = this.getILeg();
                              Object other$iLeg = other.getILeg();
                              return this$iLeg == null ? other$iLeg == null : this$iLeg.equals(other$iLeg);
                           } else {
                              return false;
                           }
                        } else {
                           return false;
                        }
                     } else {
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof NpcTemplate;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + this.getHead();
      result = result * 59 + this.getBody();
      result = result * 59 + this.getLeg();
      result = result * 59 + this.getAvatar();
      result = result * 59 + (this.isEH() ? 79 : 97);
      result = result * 59 + (this.isEB() ? 79 : 97);
      result = result * 59 + (this.isEL() ? 79 : 97);
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $pHead = this.getPHead();
      result = result * 59 + ($pHead == null ? 43 : $pHead.hashCode());
      Object $pBody = this.getPBody();
      result = result * 59 + ($pBody == null ? 43 : $pBody.hashCode());
      Object $pLeg = this.getPLeg();
      result = result * 59 + ($pLeg == null ? 43 : $pLeg.hashCode());
      Object $iHead = this.getIHead();
      result = result * 59 + ($iHead == null ? 43 : $iHead.hashCode());
      Object $iBody = this.getIBody();
      result = result * 59 + ($iBody == null ? 43 : $iBody.hashCode());
      Object $iLeg = this.getILeg();
      return result * 59 + ($iLeg == null ? 43 : $iLeg.hashCode());
   }

   @Override
   public String toString() {
      return "NpcTemplate(id="
         + this.getId()
         + ", name="
         + this.getName()
         + ", head="
         + this.getHead()
         + ", body="
         + this.getBody()
         + ", leg="
         + this.getLeg()
         + ", avatar="
         + this.getAvatar()
         + ", pHead="
         + this.getPHead()
         + ", pBody="
         + this.getPBody()
         + ", pLeg="
         + this.getPLeg()
         + ", iHead="
         + this.getIHead()
         + ", iBody="
         + this.getIBody()
         + ", iLeg="
         + this.getILeg()
         + ", eH="
         + this.isEH()
         + ", eB="
         + this.isEB()
         + ", eL="
         + this.isEL()
         + ")";
   }

   public NpcTemplate() {
   }
}
