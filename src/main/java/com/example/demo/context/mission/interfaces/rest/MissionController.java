package com.example.demo.context.mission.interfaces.rest;

import com.example.demo.common.cqrs.query.QueryBus;
import com.example.demo.context.mission.application.query.GetMissionsQuery;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final QueryBus queryBus;

    @GetMapping
    public ResponseEntity<List<MissionResponse>> getMissions(@RequestParam Long userId) {
        List<MissionResponse> missions = queryBus.execute(new GetMissionsQuery(userId));
        return ResponseEntity.ok(missions);
    }
}
