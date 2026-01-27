package com.girlkun.tool.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ChangeNameFile {
   public static void main(String[] args) {
      try {
         String folderName = "C:\\Users\\admin\\Desktop\\cbro\\data\\girlkun\\res\\x1 - Copy";
         String newFolderName = "C:\\Users\\admin\\Desktop\\test752002";
         File folder = new File(folderName);
         if (!folder.exists()) {
            return;
         }

         File newFolder = new File(newFolderName);
         if (!newFolder.exists()) {
            newFolder.mkdirs();
         }

         for (File file : folder.listFiles()) {
            String fileName = file.getName();
            fileName = fileName + ".png";
            File newFile = new File(newFolder.getAbsoluteFile() + "/" + fileName);
            Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }

         System.out.println("done");
      } catch (Exception var11) {
         var11.printStackTrace();
      }
   }
}
