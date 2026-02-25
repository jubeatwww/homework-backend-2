package com.example.demo.context.mission.interfaces.rest;

import com.example.demo.common.cqrs.command.CommandBus;
import com.example.demo.context.mission.domain.exception.GameNotFoundException;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActionController.class)
class ActionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommandBus commandBus;

    // ── POST /login ──────────────────────────────────────────────────────────

    @Test
    void login_returns202ForValidRequest() throws Exception {
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "loginDate": "2026-01-10"}
                    """))
            .andExpect(status().isAccepted());

        verify(commandBus).execute(any());
    }

    @Test
    void login_returns400WhenUserIdIsMissing() throws Exception {
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginDate": "2026-01-10"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void login_returns400WhenLoginDateIsMissing() throws Exception {
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void login_returns404WhenUserNotFound() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(commandBus).execute(any());

        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 99, "loginDate": "2026-01-10"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ── POST /launchGame ─────────────────────────────────────────────────────

    @Test
    void launchGame_returns202ForValidRequest() throws Exception {
        mockMvc.perform(post("/launchGame")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10}
                    """))
            .andExpect(status().isAccepted());

        verify(commandBus).execute(any());
    }

    @Test
    void launchGame_returns400WhenGameIdIsMissing() throws Exception {
        mockMvc.perform(post("/launchGame")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void launchGame_returns404WhenGameNotFound() throws Exception {
        doThrow(new GameNotFoundException(99L)).when(commandBus).execute(any());

        mockMvc.perform(post("/launchGame")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 99}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ── POST /play ───────────────────────────────────────────────────────────

    @Test
    void play_returns202ForValidRequest() throws Exception {
        mockMvc.perform(post("/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10, "score": 500, "idempotencyKey": "key-500"}
                    """))
            .andExpect(status().isAccepted());

        verify(commandBus).execute(any());
    }

    @Test
    void play_returns400WhenScoreIsNegative() throws Exception {
        mockMvc.perform(post("/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10, "score": -1}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void play_returns400WhenScoreIsMissing() throws Exception {
        mockMvc.perform(post("/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void play_acceptsIdempotencyKeyFromHeader() throws Exception {
        mockMvc.perform(post("/play")
                .header("X-Idempotency-Key", "header-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10, "score": 100}
                    """))
            .andExpect(status().isAccepted());

        verify(commandBus).execute(any());
    }

    @Test
    void play_acceptsIdempotencyKeyFromBody() throws Exception {
        mockMvc.perform(post("/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10, "score": 100, "idempotencyKey": "body-key"}
                    """))
            .andExpect(status().isAccepted());

        verify(commandBus).execute(any());
    }

    @Test
    void play_returns400WhenIdempotencyKeyMissing() throws Exception {
        mockMvc.perform(post("/play")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": 1, "gameId": 10, "score": 100}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }
}
