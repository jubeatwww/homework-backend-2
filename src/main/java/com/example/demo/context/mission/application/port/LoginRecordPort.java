package com.example.demo.context.mission.application.port;

import java.time.LocalDate;

public interface LoginRecordPort {

    boolean recordLogin(Long userId, LocalDate loginDate);

    int countConsecutiveLoginDays(Long userId, LocalDate asOfDate);
}
