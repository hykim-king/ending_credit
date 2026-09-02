package com.endit.config;

import javax.sql.DataSource;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {
	final Logger log = LoggerFactory.getLogger(getClass());
	
	@Bean
	public DataSource dataSource() {
		HikariDataSource ds=new HikariDataSource();
		
		//Oracle datasource-개인
		ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
		ds.setJdbcUrl("jdbc:oracle:thin:@//localhost:1521/XEPDB1");
		ds.setUsername("endit_test");
		ds.setPassword("qwer1234");
		
		//Oracle datasource-공용
//		ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
//		ds.setJdbcUrl("jdbc:oracle:thin:@//192.168.100.30:1522/XE");
//		ds.setUsername("enditpcwk");
//		ds.setPassword("qwer1234");
		
		//Hikari
		ds.setPoolName("PCWK-HikariCP");
		ds.setMaximumPoolSize(10);
		ds.setMinimumIdle(5);
		ds.setIdleTimeout(600000);
		ds.setMaxLifetime(1800000);
		ds.setConnectionTimeout(30000);
		ds.setValidationTimeout(5000);
		ds.setAutoCommit(true);
		log.debug("DataSouceConfig dataSource: {}", ds);
		return ds;
	}
	
	
}
