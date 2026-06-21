package com.jinlee.devtroublemate.trouble.controller;

import com.jinlee.devtroublemate.trouble.dto.CreateTroubleRequest;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleResponse;
import com.jinlee.devtroublemate.trouble.service.TroubleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}