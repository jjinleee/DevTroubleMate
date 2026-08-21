package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.embedding.dto.SimilarTroubleResponse;
import com.jinlee.devtroublemate.embedding.service.SimilarTroubleService;
import com.jinlee.devtroublemate.trouble.dto.*;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/troubles")
@Tag(name = "Trouble", description = "트러블 CRUD API")
public class TroubleController {

    private final TroubleService troubleService;
    private final SimilarTroubleService similarTroubleService;

    @Operation(summary = "트러블 생성")
    @PostMapping
    public CreateTroubleResponse create(
            @Valid @RequestBody CreateTroubleRequest request
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
    public PageResponse<TroubleSummaryResponse> getTroubles(
            @ModelAttribute TroubleSearchCondition condition,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return troubleService.getTroubles(condition, pageable);
    }

    @Operation(summary = "트러블 해결 처리")
    @PatchMapping("/{troubleId}/resolution")
    public void resolve(
            @PathVariable Long troubleId,
            @Valid @RequestBody ResolveTroubleRequest request
    ) {
        troubleService.resolve(troubleId, request);
    }

    @Operation(summary = "유사 장애 검색", description = "기존 장애와 의미적으로 유사한 장애를 조회합니다.")
    @GetMapping("/{troubleId}/similar")
    public ResponseEntity<List<SimilarTroubleResponse>> findSimilarTroubles(
            @PathVariable Long troubleId,
            @RequestParam(defaultValue = "5") int limit
    ) {

        return ResponseEntity.ok(
                similarTroubleService.findSimilarTroubles(troubleId, limit)
        );
    }
}
