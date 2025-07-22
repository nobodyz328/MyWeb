package com.myweb.website_core.demos.web.interaction;

import com.myweb.website_core.dto.InteractionResponse;
import com.myweb.website_core.dto.PostInteractionStatus;
import com.myweb.website_core.service.PostInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PostInteractionController
 * Tests all REST endpoints for post interactions (likes, bookmarks, status)
 */
@WebMvcTest(PostInteractionController.class)
class PostInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostInteractionService postInteractionService;

    private InteractionResponse successLikeResponse;
    private InteractionResponse successBookmarkResponse;
    private PostInteractionStatus interactionStatus;

    @BeforeEach
    void setUp() {
        // Setup test data
        successLikeResponse = InteractionResponse.success("like", 1L, 1L, 5L, true);
        successBookmarkResponse = InteractionResponse.success("bookmark", 1L, 1L, 3L, true);
        interactionStatus = new PostInteractionStatus(1L, 1L, true, false, 5L, 3L, 10L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleLike_EndpointExists() throws Exception {
        // Arrange
        when(postInteractionService.toggleLike(eq(1L), any(Long.class)))
                .thenReturn(CompletableFuture.completedFuture(successLikeResponse));

        // Act & Assert - Test that endpoint exists and accepts requests
        MvcResult result = mockMvc.perform(post("/api/posts/1/interactions/like")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Verify async response
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operation").value("like"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleBookmark_EndpointExists() throws Exception {
        // Arrange
        when(postInteractionService.toggleBookmark(eq(1L), any(Long.class)))
                .thenReturn(CompletableFuture.completedFuture(successBookmarkResponse));

        // Act & Assert - Test that endpoint exists and accepts requests
        MvcResult result = mockMvc.perform(post("/api/posts/1/interactions/bookmark")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Verify async response
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.operation").value("bookmark"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetInteractionStatus_EndpointExists() throws Exception {
        // Arrange
        when(postInteractionService.getInteractionStatus(eq(1L), any(Long.class)))
                .thenReturn(CompletableFuture.completedFuture(interactionStatus));

        // Act & Assert - Test that endpoint exists and accepts requests
        MvcResult result = mockMvc.perform(get("/api/posts/1/interactions/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Verify async response
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    void testToggleLike_Unauthorized() throws Exception {
        // Act & Assert - Test that unauthenticated requests are rejected
        mockMvc.perform(post("/api/posts/1/interactions/like")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testToggleBookmark_Unauthorized() throws Exception {
        // Act & Assert - Test that unauthenticated requests are rejected
        mockMvc.perform(post("/api/posts/1/interactions/bookmark")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleLike_InvalidPostId() throws Exception {
        // Act & Assert - Test parameter validation
        mockMvc.perform(post("/api/posts/invalid/interactions/like")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleBookmark_InvalidPostId() throws Exception {
        // Act & Assert - Test parameter validation
        mockMvc.perform(post("/api/posts/invalid/interactions/bookmark")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetInteractionStatus_InvalidPostId() throws Exception {
        // Act & Assert - Test parameter validation
        mockMvc.perform(get("/api/posts/invalid/interactions/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleLike_MissingCSRFToken() throws Exception {
        // Act & Assert - Test CSRF protection
        mockMvc.perform(post("/api/posts/1/interactions/like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}