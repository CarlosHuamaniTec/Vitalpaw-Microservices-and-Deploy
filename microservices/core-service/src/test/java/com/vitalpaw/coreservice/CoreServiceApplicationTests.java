package com.vitalpaw.coreservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CoreServiceApplication.class)
@ActiveProfiles("test")
class CoreServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}