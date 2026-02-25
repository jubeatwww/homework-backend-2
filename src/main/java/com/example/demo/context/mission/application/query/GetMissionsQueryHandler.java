package com.example.demo.context.mission.application.query;

import com.example.demo.common.cqrs.query.QueryHandler;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetMissionsQueryHandler implements QueryHandler<GetMissionsQuery, List<MissionResponse>> {

    private final MissionRepository missionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> handle(GetMissionsQuery query) {
        return missionRepository.findByUserId(query.userId()).stream()
            .map(MissionResponse::from)
            .toList();
    }
}
