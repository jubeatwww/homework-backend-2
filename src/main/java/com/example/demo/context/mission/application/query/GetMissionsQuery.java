package com.example.demo.context.mission.application.query;

import com.example.demo.common.cqrs.query.Query;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;

import java.util.List;

public record GetMissionsQuery(Long userId) implements Query<List<MissionResponse>> {
}
