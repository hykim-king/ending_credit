package com.endit.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

/**
 * <pre>
 * Class Name : DataSourceConfig
 * Description : 외부 설정을 사용하는 Oracle HikariCP DataSource 구성
 *
 * Modification History
 * ------------------------------------------------------------
 * Date         Author      Description
 * ------------------------------------------------------------
 * 2026. 9. 12.  Codex       DB 접속 정보와 HikariCP 설정을 YAML로 분리한 교체안 작성
 * ------------------------------------------------------------
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceConfig {

    /**
     * spring.datasource의 접속 정보로 연결 풀을 만들고,
     * spring.datasource.hikari의 연결 풀 설정을 바인딩한다.
     * DataSourceProperties가 url을 HikariCP의 jdbcUrl로 변환한다.
     *
     * @param properties Oracle 접속 정보
     * @return MyBatis와 트랜잭션 관리자가 사용할 DataSource
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

}
