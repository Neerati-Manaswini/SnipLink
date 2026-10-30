package com.manaswini.sniplink;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:sniplinktest;DB_CLOSE_DELAY=-1")
class LinkApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private String create(String json) throws Exception {
        return mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void createRedirectAndCountClicks() throws Exception {
        create("{\"url\":\"https://example.com/page\",\"alias\":\"demo-link\"}");

        mvc.perform(get("/demo-link"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));

        mvc.perform(get("/api/links/demo-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(1));
    }

    @Test
    void duplicateAliasReturns409() throws Exception {
        create("{\"url\":\"https://example.com/a\",\"alias\":\"taken-one\"}");
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/b\",\"alias\":\"taken-one\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unknownCodeRedirectsHomeWithMessage() throws Exception {
        mvc.perform(get("/nope123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/?error=notfound"));
    }

    @Test
    void deleteRequiresTheCreatorsToken() throws Exception {
        String body = create("{\"url\":\"https://example.com/x\",\"alias\":\"delete-me\"}");
        String token = JsonPath.read(body, "$.deleteToken");

        mvc.perform(delete("/api/links/delete-me")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/links/delete-me").header("X-Delete-Token", token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/links/delete-me")).andExpect(status().isNotFound());
    }
}
