package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.ai.dto.AIProcessingResponse;
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
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

    @Operation(summary = "트러블 수정", description = "트러블과 태그를 수정하고 AI 분석 및 임베딩을 갱신합니다.")
    @PutMapping("/{troubleId}")
    public ResponseEntity<Void> update(
            @PathVariable Long troubleId,
            @Valid @RequestBody UpdateTroubleRequest request
    ) {
        troubleService.update(troubleId, request);
        return ResponseEntity.noContent().build();
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

    @Operation(summary = "트러블 보관")
    @PatchMapping("/{troubleId}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long troubleId) {
        troubleService.archive(troubleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "트러블 복원")
    @PatchMapping("/{troubleId}/restore")
    public ResponseEntity<Void> restore(@PathVariable Long troubleId) {
        troubleService.restore(troubleId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "실패한 AI 분석 재시도")
    @PostMapping("/{troubleId}/ai-analysis/retry")
    public AIProcessingResponse retryAIAnalysis(@PathVariable Long troubleId) {
        return troubleService.retryAIAnalysis(troubleId);
    }

    @Operation(summary = "유사 장애 검색", description = "기존 장애와 의미적으로 유사한 장애를 조회합니다.")
    @GetMapping("/{troubleId}/similar")
    public ResponseEntity<List<SimilarTroubleResponse>> findSimilarTroubles(
            @PathVariable Long troubleId,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
            @Max(value = 20, message = "limit은 20 이하여야 합니다.")
            int limit,
            @RequestParam(defaultValue = "0.0")
            @DecimalMin(value = "0.0", message = "minSimilarity는 0.0 이상이어야 합니다.")
            @DecimalMax(value = "1.0", message = "minSimilarity는 1.0 이하여야 합니다.")
            double minSimilarity
    ) {

        return ResponseEntity.ok(
                similarTroubleService.findSimilarTroubles(troubleId, limit, minSimilarity)
        );
    }
}
