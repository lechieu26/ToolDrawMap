package com.girlkun.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class ResultSetImpl implements GirlkunResultSet {
   private Map<String, Object>[] data;
   private Object[][] values;
   private int indexData = -1;

   public ResultSetImpl(ResultSet rs) throws Exception {
      try {
         rs.last();
         int nRow = rs.getRow();
         rs.beforeFirst();
         ResultSetMetaData rsmd = rs.getMetaData();
         int nColumn = rsmd.getColumnCount();
         this.data = new HashMap[nRow];

         for (int i = 0; i < this.data.length; i++) {
            this.data[i] = new HashMap<>();
         }

         this.values = new Object[nRow][nColumn];

         for (int index = 0; rs.next(); index++) {
            for (int i = 1; i <= nColumn; i++) {
               String tableName = rsmd.getTableName(i);
               String columnName = rsmd.getColumnName(i);
               Object columnValue = rs.getObject(i);
               this.data[index].put(columnName.toLowerCase(), columnValue);
               this.data[index].put(tableName.toLowerCase() + "." + columnName.toLowerCase(), columnValue);
               this.values[index][i - 1] = columnValue;
            }
         }
      } catch (Exception var17) {
         throw var17;
      } finally {
         if (rs != null) {
            try {
               rs.getStatement().close();
               rs.close();
            } catch (Exception var16) {
            }
         }
      }
   }

   @Override
   public void dispose() {
      for (Map map : this.data) {
         map.clear();
         Object var12 = null;
      }

      this.data = null;

      for (Object[] obj : this.values) {
         for (Object o : obj) {
            o = null;
         }

         obj = null;
      }

      this.values = (Object[][]) null;
   }

   @Override
   public boolean next() throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else {
         this.indexData++;
         return this.indexData < this.data.length;
      }
   }

   @Override
   public boolean first() throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else {
         this.indexData++;
         return this.indexData == this.data.length - 1;
      }
   }

   @Override
   public boolean gotoResult(int index) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData >= 0 && this.indexData < this.data.length) {
         this.indexData = index;
         return true;
      } else {
         throw new Exception("Index out of bound");
      }
   }

   @Override
   public boolean gotoFirst() throws Exception {
      if (this.data != null && this.data.length != 0) {
         this.indexData = 0;
         return true;
      } else {
         throw new Exception("No data available");
      }
   }

   @Override
   public void gotoBeforeFirst() {
      this.indexData = -1;
   }

   @Override
   public boolean gotoLast() throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else {
         this.indexData = this.data.length - 1;
         return true;
      }
   }

   @Override
   public int getRows() throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else {
         return this.data.length;
      }
   }

   @Override
   public byte getByte(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.values[this.indexData][column - 1];
         if (val instanceof Number) {
            return ((Number) val).byteValue();
         }
         return Byte.parseByte(String.valueOf(val));
      }
   }

   @Override
   public byte getByte(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.data[this.indexData].get(column.toLowerCase());
         if (val instanceof Number) {
            return ((Number) val).byteValue();
         }
         return Byte.parseByte(String.valueOf(val));
      }
   }

   @Override
   public int getInt(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.values[this.indexData][column - 1];
         if (val instanceof Number) {
            return ((Number) val).intValue();
         }
         return Integer.parseInt(String.valueOf(val));
      }
   }

   @Override
   public int getInt(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.data[this.indexData].get(column.toLowerCase());
         if (val instanceof Number) {
            return ((Number) val).intValue();
         }
         return Integer.parseInt(String.valueOf(val));
      }
   }

   @Override
   public float getFloat(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Float) this.values[this.indexData][column - 1];
      }
   }

   @Override
   public float getFloat(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Float) this.data[this.indexData].get(column.toLowerCase());
      }
   }

   @Override
   public double getDouble(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Double) this.values[this.indexData][column - 1];
      }
   }

   @Override
   public double getDouble(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Double) this.data[this.indexData].get(column.toLowerCase());
      }
   }

   @Override
   public long getLong(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.values[this.indexData][column - 1];
         if (val instanceof Number) {
            return ((Number) val).longValue();
         }
         return Long.parseLong(String.valueOf(val));
      }
   }

   @Override
   public long getLong(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.data[this.indexData].get(column.toLowerCase());
         if (val instanceof Number) {
            return ((Number) val).longValue();
         }
         return Long.parseLong(String.valueOf(val));
      }
   }

   @Override
   public String getString(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return String.valueOf(this.values[this.indexData][column - 1]);
      }
   }

   @Override
   public String getString(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return String.valueOf(this.data[this.indexData].get(column.toLowerCase()));
      }
   }

   @Override
   public Object getObject(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return this.values[this.indexData][column - 1];
      }
   }

   @Override
   public Object getObject(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return this.data[this.indexData].get(column.toLowerCase());
      }
   }

   @Override
   public boolean getBoolean(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         try {
            return (Integer) this.values[this.indexData][column - 1] == 1;
         } catch (Exception var3) {
            return (Boolean) this.values[this.indexData][column - 1];
         }
      }
   }

   @Override
   public boolean getBoolean(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         try {
            return (Integer) this.data[this.indexData].get(column.toLowerCase()) == 1;
         } catch (Exception var3) {
            return (Boolean) this.data[this.indexData].get(column.toLowerCase());
         }
      }
   }

   @Override
   public Timestamp getTimestamp(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Timestamp) this.values[this.indexData][column - 1];
      }
   }

   @Override
   public Timestamp getTimestamp(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         return (Timestamp) this.data[this.indexData].get(column.toLowerCase());
      }
   }

   @Override
   public short getShort(int column) throws Exception {
      if (this.values == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.values[this.indexData][column - 1];
         if (val instanceof Number) {
            return ((Number) val).shortValue();
         }
         return Short.parseShort(String.valueOf(val));
      }
   }

   @Override
   public short getShort(String column) throws Exception {
      if (this.data == null) {
         throw new Exception("No data available");
      } else if (this.indexData == -1) {
         throw new Exception("Results need to be prepared in advance");
      } else {
         Object val = this.data[this.indexData].get(column.toLowerCase());
         if (val instanceof Number) {
            return ((Number) val).shortValue();
         }
         return Short.parseShort(String.valueOf(val));
      }
   }
}
