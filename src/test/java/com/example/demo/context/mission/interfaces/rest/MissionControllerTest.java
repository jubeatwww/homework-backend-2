package com.example.demo.context.mission.interfaces.rest;

import com.example.demo.common.cqrs.query.QueryBus;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MissionController.class)
class MissionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QueryBus queryBus;

    private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Test
    void getMissions_returns200WithMissionList() throws Exception {
        var responses = List.of(
            new MissionResponse(1L, MissionType.CONSECUTIVE_LOGIN,
                MissionType.CONSECUTIVE_LOGIN.getDescription(), 2, 3, false, null, EXPIRED_AT),
            new MissionResponse(2L, MissionType.DIFFERENT_GAMES,
                MissionType.DIFFERENT_GAMES.getDescription(), 3, 3, true,
                LocalDateTime.of(2026, 1, 20, 10, 0), EXPIRED_AT),
            new MissionResponse(3L, MissionType.PLAY_SCORE,
                MissionType.PLAY_SCORE.getDescription(), 500, 1000, false, null, EXPIRED_AT)
        );
        when(queryBus.execute(any())).thenReturn(responses);

        mockMvc.perform(get("/missions").param("userId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].missionType").value("CONSECUTIVE_LOGIN"))
            .andExpect(jsonPath("$[0].progress").value(2))
            .andExpect(jsonPath("$[0].target").value(3))
            .andExpect(jsonPath("$[0].completed").value(false))
            .andExpect(jsonPath("$[0].description").value(MissionType.CONSECUTIVE_LOGIN.getDescription()))
            .andExpect(jsonPath("$[1].completed").value(true))
            .andExpect(jsonPath("$[2].missionType").value("PLAY_SCORE"));
    }

    @Test
    void getMissions_returns200WithEmptyListWhenNoMissions() throws Exception {
        when(queryBus.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/missions").param("userId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMissions_returns400WhenUserIdIsMissing() throws Exception {
        mockMvc.perform(get("/missions"))
            .andExpect(status().isBadRequest());
    }
}
