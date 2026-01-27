package com.girlkun.tool.utils;

import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class NotifyUtil {
   public static final void showMessageDialog(JFrame parent, String message) {
      try {
         JOptionPane.showMessageDialog(parent, message, "Girlkun75 - Hệ thống thông báo", 0, new ImageIcon(Util.getImageById(8410, 2)));
      } catch (Exception var3) {
      }
   }

   public static final void showMessageDialog(JFrame parent, String message, Image icon) {
      JOptionPane.showMessageDialog(parent, message, "Girlkun75 - Hệ thống thông báo", 0, new ImageIcon(icon));
   }

   public static final String showInputDialog(JFrame parent, String message) {
      try {
         String input = null;
         return String.valueOf(
            JOptionPane.showInputDialog(parent, message, "Girlkun75 - Hệ thống thông báo", 0, new ImageIcon(Util.getImageById(8410, 2)), null, null)
         );
      } catch (Exception var3) {
         var3.printStackTrace();
         return "";
      }
   }

   public static final String showInputDialog(JFrame parent, String message, String defaultText) {
      String input = null;
      return String.valueOf(JOptionPane.showInputDialog(parent, message, "Girlkun75 - Hệ thống thông báo", -1, null, null, defaultText));
   }

   public static final int showConfirmDialog(JFrame parent, String message) {
      try {
         return JOptionPane.showConfirmDialog(parent, message, "Girlkun75 - Hệ thống thông báo", 0, 0, new ImageIcon(Util.getImageById(8410, 2)));
      } catch (Exception var3) {
         var3.printStackTrace();
         return -1;
      }
   }
}
