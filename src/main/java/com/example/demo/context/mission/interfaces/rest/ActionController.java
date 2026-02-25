package com.example.demo.context.mission.interfaces.rest;

import com.example.demo.common.cqrs.command.CommandBus;
import com.example.demo.context.mission.application.command.LaunchGameCommand;
import com.example.demo.context.mission.application.command.LoginCommand;
import com.example.demo.context.mission.application.command.PlayGameCommand;
import com.example.demo.context.mission.domain.exception.IdempotencyKeyRequiredException;
import com.example.demo.context.mission.interfaces.rest.dto.LaunchGameRequest;
import com.example.demo.context.mission.interfaces.rest.dto.LoginRequest;
import com.example.demo.context.mission.interfaces.rest.dto.PlayGameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActionController {

    private final CommandBus commandBus;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        commandBus.execute(new LoginCommand(
            request.userId(),
            request.loginDate(),
            resolveOccurredAt(request.occurredAt())
        ));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/launchGame")
    public ResponseEntity<Void> launchGame(@Valid @RequestBody LaunchGameRequest request) {
        commandBus.execute(new LaunchGameCommand(
            request.userId(),
            request.gameId(),
            resolveOccurredAt(request.occurredAt())
        ));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/play")
    public ResponseEntity<Void> play(
        @RequestHeader(name = "X-Idempotency-Key", required = false) String headerKey,
        @Valid @RequestBody PlayGameRequest request) {
        String idempotencyKey = resolveIdempotencyKey(headerKey, request.idempotencyKey());
        commandBus.execute(new PlayGameCommand(
            request.userId(),
            request.gameId(),
            request.score(),
            idempotencyKey,
            resolveOccurredAt(request.occurredAt())
        ));
        return ResponseEntity.accepted().build();
    }

    private String resolveIdempotencyKey(String headerKey, String bodyKey) {
        if (headerKey != null && !headerKey.isBlank()) return headerKey;
        if (bodyKey != null && !bodyKey.isBlank()) return bodyKey;
        throw new IdempotencyKeyRequiredException();
    }

    private long resolveOccurredAt(Long occurredAt) {
        return occurredAt != null ? occurredAt : System.currentTimeMillis();
    }
}
