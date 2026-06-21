package com.jinlee.devtroublemate.trouble.service;

import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleRequest;
import com.jinlee.devtroublemate.trouble.dto.CreateTroubleResponse;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}