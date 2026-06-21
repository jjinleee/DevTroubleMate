package com.jinlee.devtroublemate.trouble.service;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleRequest;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleResponse;
import com.jinlee.devtroublemate.trouble.dto.TroubleDetailResponse;
import com.jinlee.devtroublemate.trouble.dto.TroubleSummaryResponse;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TroubleService {

    private final TroubleRepository troubleRepository;

    public CreateTroubleResponse create(CreateTroubleRequest request) {

        Trouble trouble = Trouble.builder()
                .title(request.title())
                .description(request.description())
                .rawLog(request.rawLog())
                .build();

        Trouble saved = troubleRepository.save(trouble);

        return new CreateTroubleResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public TroubleDetailResponse getDetail(Long troubleId) {
        Trouble trouble = troubleRepository.findById(troubleId)
                .orElseThrow(() -> new IllegalArgumentException("장애를 찾을 수 없습니다."));

        return TroubleDetailResponse.from(trouble);
    }

    @Transactional(readOnly = true)
    public List<TroubleSummaryResponse> getTroubles() {
        return troubleRepository.findAll()
                .stream()
                .map(TroubleSummaryResponse::from)
                .toList();
    }
}