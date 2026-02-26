package com.example.demo.context.mission.application.query;

import com.example.demo.context.mission.application.port.LoginRecordPort;
import com.example.demo.context.mission.domain.model.MissionType;
import com.example.demo.context.mission.interfaces.rest.dto.MissionResponse.Criterion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsecutiveLoginCriteriaComputer implements CriteriaComputer {

    private final Clock clock;
    private final LoginRecordPort loginRecordPort;

    @Override
    public MissionType supportedType() {
        return MissionType.CONSECUTIVE_LOGIN;
    }

    @Override
    public List<Criterion> compute(Long userId) {
        int days = loginRecordPort.countConsecutiveLoginDays(userId, LocalDate.now(clock));
        return List.of(new Criterion("consecutiveDays", days, 3));
    }
}