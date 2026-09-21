package com.nexa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration bout-en-bout : démarre un vrai contexte Spring (base H2 isolée,
 * définie dans application-test.properties) et simule un parcours utilisateur complet
 * à travers la vraie chaîne HTTP -> Security -> Controller -> Service -> Repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String registerAndGetToken(String email, String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "motdepasse123", "fullName", name));

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void parcoursComplet_inscription_publication_like_commentaire() throws Exception {
        String token = registerAndGetToken("integration@nexa.dev", "Utilisateur Intégration");

        // 1. Créer une publication
        String postBody = objectMapper.writeValueAsString(Map.of("text", "Mon post de test", "imageUrl", ""));
        String postResponse = mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(postBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Mon post de test"))
                .andReturn().getResponse().getContentAsString();

        long postId = objectMapper.readTree(postResponse).get("id").asLong();

        // 2. Le fil d'actualité doit contenir cette publication
        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Mon post de test"));

        // 3. Aimer la publication
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        // 4. Commenter la publication
        String commentBody = objectMapper.writeValueAsString(Map.of("text", "Super post !"));
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(commentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Super post !"));

        // 5. Vérifier les compteurs mis à jour dans le fil
        mockMvc.perform(get("/api/posts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].likeCount").value(1))
                .andExpect(jsonPath("$[0].commentCount").value(1))
                .andExpect(jsonPath("$[0].likedByMe").value(true));
    }

    @Test
    void accesSansToken_estRefuse() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscriptionAvecMemeEmail_estRefusee() throws Exception {
        registerAndGetToken("doublon@nexa.dev", "Premier");

        String body = objectMapper.writeValueAsString(Map.of(
                "email", "doublon@nexa.dev", "password", "autrepass123", "fullName", "Second"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }
}
