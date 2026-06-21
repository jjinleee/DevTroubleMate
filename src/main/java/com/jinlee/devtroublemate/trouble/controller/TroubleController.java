package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.trouble.dto.*;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/troubles")
public class TroubleController {

    private final TroubleService troubleService;

    @PostMapping
    public CreateTroubleResponse create(
            @RequestBody CreateTroubleRequest request
    ) {
        return troubleService.create(request);
    }

    @GetMapping("/{troubleId}")
    public TroubleDetailResponse getDetail(
            @PathVariable Long troubleId
    ) {
        return troubleService.getDetail(troubleId);
    }

    @GetMapping
    public List<TroubleSummaryResponse> getTroubles() {
        return troubleService.getTroubles();
    }

    @PatchMapping("/{troubleId}/resolution")
    public void resolve(
            @PathVariable Long troubleId,
            @RequestBody ResolveTroubleRequest request
    ) {
        troubleService.resolve(troubleId, request);
    }
}