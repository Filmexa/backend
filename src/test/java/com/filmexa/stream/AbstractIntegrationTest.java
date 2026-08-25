package com.filmexa.stream;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void overrideStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.img-storage-path",
                () -> System.getProperty("java.io.tmpdir") + "/filmexa-integration-test-images");
    }
}
