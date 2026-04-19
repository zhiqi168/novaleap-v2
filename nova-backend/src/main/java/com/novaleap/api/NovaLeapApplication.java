package com.novaleap.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// TODO: 等待配置好真实的 MySQL 服务后再移除 (exclude = DataSourceAutoConfiguration.class)，否则未连接数据库会报错阻碍启动。
//@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.novaleap.api.mapper")

public class NovaLeapApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovaLeapApplication.class, args);
    }
}
