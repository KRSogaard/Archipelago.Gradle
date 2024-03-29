package build.archipelago.packageservice.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class HealthControllerTest {

    @Autowired
    private HealthController controller;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testContextLoads() throws Exception {
        assertNotNull(controller);
    }

    @Test
    public void testHealthControllerReturnsHealthy() throws Exception {
        this.mockMvc.perform(get(HealthController.HEALTH_PATH))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(HealthController.HEALTHY_REPLY)));
    }
}