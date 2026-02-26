package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse.Criterion;

import java.util.List;

public interface CriteriaComputer {

    MissionType supportedType();

    List<Criterion> compute(Long userId);
}