package com.girlkun.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class GirlkunDatasource extends HikariDataSource {
   public String name;

   public GirlkunDatasource(String name, HikariConfig configuration) {
      super(configuration);
      this.name = name;
   }
}
