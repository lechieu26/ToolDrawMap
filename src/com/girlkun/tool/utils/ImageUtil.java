package com.girlkun.tool.utils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageUtil {
   public static void saveImage(BufferedImage image, String pathFolder, String name) {
      try {
         File folder = new File(pathFolder);
         if (!folder.exists()) {
            folder.mkdirs();
         }

         File outputfile = new File(pathFolder + "/" + name + ".png");
         ImageIO.write(image, "png", outputfile);
      } catch (Exception var5) {
      }
   }

   public static void saveImage(byte[] data, String pathFolder, String name) {
      try {
         ByteArrayInputStream bis = new ByteArrayInputStream(data);
         BufferedImage image = ImageIO.read(bis);
         File folder = new File(pathFolder);
         if (!folder.exists()) {
            folder.mkdirs();
         }

         File outputfile = new File(pathFolder + "/" + name + ".png");
         ImageIO.write(image, "png", outputfile);
      } catch (Exception var7) {
      }
   }

   public static BufferedImage trimImage(BufferedImage image) {
      WritableRaster raster = image.getAlphaRaster();
      int width = raster.getWidth();
      int height = raster.getHeight();
      int left = 0;
      int top = 0;
      int right = width - 1;
      int bottom = height - 1;
      int minRight = width - 1;

      int minBottom;
      label81:
      for (minBottom = height - 1; top <= bottom; top++) {
         for (int x = 0; x < width; x++) {
            if (raster.getSample(x, top, 0) != 0) {
               minRight = x;
               minBottom = top;
               break label81;
            }
         }
      }

      label70:
      while (left < minRight) {
         for (int y = height - 1; y > top; y--) {
            if (raster.getSample(left, y, 0) != 0) {
               minBottom = y;
               break label70;
            }
         }

         left++;
      }

      label59:
      while (bottom > minBottom) {
         for (int xx = width - 1; xx >= left; xx--) {
            if (raster.getSample(xx, bottom, 0) != 0) {
               minRight = xx;
               break label59;
            }
         }

         bottom--;
      }

      label48:
      while (right > minRight) {
         for (int yx = bottom; yx >= top; yx--) {
            if (raster.getSample(right, yx, 0) != 0) {
               break label48;
            }
         }

         right--;
      }

      try {
         return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
      } catch (Exception var11) {
         return image;
      }
   }
}
