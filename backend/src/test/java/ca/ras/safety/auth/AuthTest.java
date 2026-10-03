package ca.ras.safety.auth;

import ca.ras.safety.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "demo.seed-enabled=true", "demo.admin-password=TestAdmin123!",
    "demo.framer-a-password=TestFramer123!", "demo.framer-b-password=TestOther123!"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthTest {
    @Autowired MockMvc mvc;

    @Test void loginRequiresCsrfAndPersistsSession() throws Exception {
        mvc.perform(post("/api/auth/login").param("email", "framer.a@example.test")
            .param("password", "TestFramer123!")) .andExpect(status().isForbidden());
        MvcResult token = mvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) token.getRequest().getSession(false);
        String header = JsonPath.read(token.getResponse().getContentAsString(), "$.headerName");
        String value = JsonPath.read(token.getResponse().getContentAsString(), "$.token");
        MvcResult login = mvc.perform(post("/api/auth/login").session(session)
            .header(header, value).param("email", "framer.a@example.test")
            .param("password", "TestFramer123!")) .andExpect(status().isNoContent()).andReturn();
        MockHttpSession loggedIn = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(loggedIn))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("FRAMER"))
            .andExpect(jsonPath("$.email").value("framer.a@example.test"))
            .andExpect(header().string("Cache-Control", "no-store"));
        MvcResult nextToken = mvc.perform(get("/api/auth/csrf").session(loggedIn)).andReturn();
        String nextValue = JsonPath.read(nextToken.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/auth/logout").session(loggedIn).header(header, nextValue))
            .andExpect(status().isNoContent());
        assertThat(loggedIn.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void wrongPasswordCannotCreateAuthenticatedSession() throws Exception {
        MvcResult token = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) token.getRequest().getSession(false);
        String header = JsonPath.read(token.getResponse().getContentAsString(), "$.headerName");
        String value = JsonPath.read(token.getResponse().getContentAsString(), "$.token");
        mvc.perform(post("/api/auth/login").session(session).header(header, value)
            .param("email", "framer.a@example.test").param("password", "wrong"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
}
