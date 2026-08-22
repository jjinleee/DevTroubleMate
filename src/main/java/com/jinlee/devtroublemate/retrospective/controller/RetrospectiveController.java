package com.jinlee.devtroublemate.retrospective.controller;

import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveRequest;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveResponse;
import com.jinlee.devtroublemate.retrospective.service.RetrospectiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/troubles/{troubleId}/retrospective")
@Tag(name = "Retrospective", description = "장애 회고 API")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회고 생성")
    public RetrospectiveResponse create(
            @PathVariable Long troubleId,
            @Valid @RequestBody RetrospectiveRequest request
    ) {
        return retrospectiveService.create(troubleId, request);
    }

    @GetMapping
    @Operation(summary = "회고 조회")
    public RetrospectiveResponse get(@PathVariable Long troubleId) {
        return retrospectiveService.get(troubleId);
    }

    @PutMapping
    @Operation(summary = "회고 수정")
    public RetrospectiveResponse update(
            @PathVariable Long troubleId,
            @Valid @RequestBody RetrospectiveRequest request
    ) {
        return retrospectiveService.update(troubleId, request);
    }
}
