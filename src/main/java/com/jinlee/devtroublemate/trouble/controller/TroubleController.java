package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.trouble.dto.*;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/troubles")
@Tag(name = "Trouble", description = "트러블 CRUD API")
public class TroubleController {

    private final TroubleService troubleService;

    @Operation(summary = "트러블 생성")
    @PostMapping
    public CreateTroubleResponse create(
            @RequestBody CreateTroubleRequest request
    ) {
        return troubleService.create(request);
    }

    @Operation(summary = "트러블 상세 조회")
    @GetMapping("/{troubleId}")
    public TroubleDetailResponse getDetail(
            @PathVariable Long troubleId
    ) {
        return troubleService.getDetail(troubleId);
    }

    @Operation(summary = "트러블 목록 조회", description = "status, tag, keyword로 필터링")
    @GetMapping
    public List<TroubleSummaryResponse> getTroubles(
            @ModelAttribute TroubleSearchCondition condition
    ) {
        return troubleService.getTroubles(condition);
    }

    @Operation(summary = "트러블 해결 처리")
    @PatchMapping("/{troubleId}/resolution")
    public void resolve(
            @PathVariable Long troubleId,
            @RequestBody ResolveTroubleRequest request
    ) {
        troubleService.resolve(troubleId, request);
    }
}