package com.example.flight.crawl.admin;

import com.example.flight.config.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataSourceStatusControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DataSourceStatusService service = new DataSourceStatusService("docker compose run crawler", "", "config-key");
        DataSourceStatusController controller = new DataSourceStatusController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsConfiguredAerodataboxStatusWhenConfigKeyExists() throws Exception {
        mockMvc.perform(get("/api/admin/data-sources/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("aerodatabox"))
                .andExpect(jsonPath("$[0].configured").value(true))
                .andExpect(jsonPath("$[0].label").value("AeroDataBox"))
                .andExpect(jsonPath("$.length()").value(1));
    }
}
