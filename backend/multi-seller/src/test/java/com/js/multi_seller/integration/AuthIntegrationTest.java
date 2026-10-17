package com.js.multi_seller.integration;

import com.js.multi_seller.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void sellerCanLogInWithValidCredentials() throws Exception {
        mockMvc.perform(login("metro@example.com", "metro123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.name").value("Metro Construction Mart"))
                .andExpect(jsonPath("$.sellerStatus").value("APPROVED"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void buyerCanLogInWithValidCredentials() throws Exception {
        mockMvc.perform(login("priya@example.com", "priya123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andExpect(jsonPath("$.name").value("Priya Sharma"));
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        mockMvc.perform(login("PRIYA@EXAMPLE.COM", "priya123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BUYER"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(login("metro@example.com", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void unknownEmailIsRejectedWithSameMessage() throws Exception {
        mockMvc.perform(login("nobody@example.com", "whatever"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void blankCredentialsFailValidation() throws Exception {
        mockMvc.perform(login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/seller/listings").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void bearerPrefixIsRequired() throws Exception {
        mockMvc.perform(get("/api/seller/listings").header(HttpHeaders.AUTHORIZATION, "token-without-prefix"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(startsWith("Authorization: Bearer")));
    }

    @Test
    void buyerTokenCannotManageListings() throws Exception {
        String buyerToken = tokenFor("priya@example.com", "priya123");

        mockMvc.perform(get("/api/seller/listings").header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private String tokenFor(String email, String password) throws Exception {
        String body = mockMvc.perform(login(email, password))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.token");
    }
}
