package com.girlkun;

import com.girlkun.database.GirlkunDB;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Log implements Runnable {
   private static Log I;
   private BufferedWriter bw;
   private List<String> querys;

   public static Log gI() {
      if (I == null) {
         I = new Log();
         new Thread(I, "Log sql").start();
      }

      return I;
   }

   private Log() {
      File file = new File("girlkun_log");
      if (!file.exists()) {
         file.mkdir();
      }

      try {
         this.bw = new BufferedWriter(new FileWriter("girlkun_log/sql_" + System.currentTimeMillis() + ".txt"));
      } catch (IOException var3) {
      }

      this.querys = new ArrayList<>();
   }

   public void log(String query) {
      this.querys.add(query);
   }

   @Override
   public void run() {
      while (GirlkunDB.LOG_QUERY) {
         try {
            String q = null;

            while ((q = this.querys.remove(0)) != null) {
               this.bw.write(q + "\n");
               this.bw.flush();
            }
         } catch (Exception var3) {
         }

         try {
            Thread.sleep(1000L);
         } catch (Exception var2) {
         }
      }

      this.dispose();
   }

   private void dispose() {
      if (this.bw != null) {
         try {
            this.bw.close();
         } catch (IOException var2) {
            this.bw = null;
         }
      }

      if (this.querys != null) {
         this.querys.clear();
      }

      this.bw = null;
      this.querys = null;
   }
}
