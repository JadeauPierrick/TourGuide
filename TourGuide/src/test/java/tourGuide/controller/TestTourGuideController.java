package tourGuide.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tourGuide.helper.InternalTestHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TestTourGuideController {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext web;

    @BeforeAll
    static void setUp() {
        InternalTestHelper.setInternalUserNumber(1);
    }

    @BeforeEach
    private void setUpPerTest() {
        mockMvc = MockMvcBuilders.webAppContextSetup(web).build();
    }

    @Test
    public void index() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
    }

    @Test
    public void getLocation() throws Exception {
        mockMvc.perform(get("/getLocation")
                        .param("userName","internalUser0"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getNearbyAttractions() throws Exception {
        mockMvc.perform(get("/getNearbyAttractions")
                        .param("userName","internalUser0"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getRewards() throws Exception {
        mockMvc.perform(get("/getRewards")
                        .param("userName", "internalUser0"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getAllCurrentLocations() throws Exception {
        mockMvc.perform(get("/getAllCurrentLocations"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getTripDeals() throws Exception {
        mockMvc.perform(get("/getTripDeals")
                        .param("userName", "internalUser0"))
                        .andExpect(status().isOk());
    }

    @Test
    public void getUserPreferences() throws Exception {
        mockMvc.perform(get("/getUserPreferences")
                        .param("userName", "internalUser0"))
                        .andExpect(status().isOk());
    }

    @Test
    public void updateUserPreferences() throws Exception {
        mockMvc.perform(put("/updateUserPreferences")
                        .param("userName", "internalUser0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"attractionProximity\":\"150\", \"lowerPricePoint\":\"\", \"highPricePoint\":\"\", \"tripDuration\":\"15\", \"ticketQuantity\":\"3\", \"numberOfAdults\":\"2\", \"numberOfChildren\":\"1\" }")
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
    }
}
