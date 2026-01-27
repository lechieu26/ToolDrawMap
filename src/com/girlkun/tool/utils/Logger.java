package com.girlkun.tool.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {
   public static final String RESET = "\u001b[0m";
   public static final String RED = "\u001b[4;31m";
   public static final String GREEN = "\u001b[0;32m";
   public static final String PURPLE = "\u001b[0;35m";
   public static final String BLUE = "\u001b[0;34m";
   public static final String YELLOW = "\u001b[33m";

   public static void log(String text) {
      System.out.print(text);
   }

   public static void log(String color, String text) {
      System.out.print(color + text + "\u001b[0m");
   }

   public static void success(String text) {
      System.out.print("\u001b[0;32m" + text + "\u001b[0m");
   }

   public static void warning(String text) {
      System.out.print("\u001b[0;34m" + text + "\u001b[0m");
   }

   public static void error(String text) {
      System.out.print("\u001b[4;31m" + text + "\u001b[0m");
   }

   public static void logException(Class clazz, Exception ex, String... log) {
      try {
         if (log != null && log.length > 0) {
            log("\u001b[0;35m", log[0] + "\n");
         }

         StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
         String nameMethod = stackTraceElements[1].getMethodName();
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         ex.printStackTrace(pw);
         String detail = sw.toString();
         String[] arr = detail.split("\n");
         warning("Có lỗi tại class: ");
         error(clazz.getName());
         warning(" - tại phương thức: ");
         error(nameMethod + "\n");
         warning("Chi tiết lỗi:\n");

         for (String str : arr) {
            error(str + "\n");
         }

         log("--------------------------------------------------------\n");
      } catch (Exception var13) {
      }
   }
}
