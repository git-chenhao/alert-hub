package com.alert.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 告警聚合平台主应用类
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
public class AlertHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertHubApplication.class, args);
        System.out.println("========================================");
        System.out.println("  告警聚合平台启动成功!");
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("  H2控制台: http://localhost:8080/h2-console");
        System.out.println("========================================");
    }
}
