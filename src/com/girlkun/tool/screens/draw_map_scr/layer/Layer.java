package com.girlkun.tool.screens.draw_map_scr.layer;

import java.awt.image.BufferedImage;

public interface Layer {
   void draw();

   BufferedImage getBufferedImage();

   void clearImage();

   void setShow(boolean var1);

   boolean isShow();

   void setSizeImage(int var1, int var2, int var3, int var4);

   void clear();
}
