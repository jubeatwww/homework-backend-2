package com.example.demo.context.mission.application.query;

import com.example.demo.common.cqrs.query.QueryHandler;
import com.example.demo.context.mission.application.port.UserQueryPort;
import com.example.demo.context.mission.domain.exception.UserNotFoundException;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.domain.repository.MissionRepository;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GetMissionsQueryHandler implements QueryHandler<GetMissionsQuery, List<MissionResponse>> {

    private final UserQueryPort userQueryPort;
    private final MissionRepository missionRepository;
    private final Map<MissionType, CriteriaComputer> computers;

    public GetMissionsQueryHandler(UserQueryPort userQueryPort,
                                   MissionRepository missionRepository,
                                   List<CriteriaComputer> computerList) {
        this.userQueryPort = userQueryPort;
        this.missionRepository = missionRepository;
        this.computers = computerList.stream()
            .collect(Collectors.toMap(CriteriaComputer::supportedType, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> handle(GetMissionsQuery query) {
        if (!userQueryPort.userExists(query.userId())) {
            throw new UserNotFoundException(query.userId());
        }
        return missionRepository.findByUserId(query.userId()).stream()
            .map(mission -> MissionResponse.from(
                mission,
                computers.get(mission.getMissionType()).compute(query.userId())))
            .toList();
    }
}
