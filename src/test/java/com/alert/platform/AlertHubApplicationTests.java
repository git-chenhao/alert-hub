package com.alert.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用启动测试
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class AlertHubApplicationTests {

    @Test
    void contextLoads() {
        // 测试 Spring 上下文是否正常加载
    }
}
