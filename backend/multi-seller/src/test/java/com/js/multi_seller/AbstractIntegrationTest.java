package com.js.multi_seller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final Map<Long, String[]> SELLER_CREDENTIALS = Map.of(
            1L, new String[]{"buildright@example.com", "buildright123"},
            2L, new String[]{"metro@example.com", "metro123"},
            3L, new String[]{"rajasthan@example.com", "rajasthan123"},
            4L, new String[]{"newhorizon@example.com", "newhorizon123"},
            5L, new String[]{"clearance@example.com", "clearance123"});

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected String sellerBearer(long sellerId) throws Exception {
        String[] credentials = SELLER_CREDENTIALS.get(sellerId);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + credentials[0] + "\",\"password\":\"" + credentials[1] + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return "Bearer " + token;
    }
}
