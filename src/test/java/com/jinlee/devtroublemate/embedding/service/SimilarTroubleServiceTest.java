package com.jinlee.devtroublemate.embedding.service;

import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarTroubleServiceTest {

    @Mock
    private TroubleRepository troubleRepository;

    @Mock
    private TroubleEmbeddingRepository troubleEmbeddingRepository;

    @InjectMocks
    private SimilarTroubleService similarTroubleService;

    @Test
    void throwExceptionWhenTroubleDoesNotExist() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> similarTroubleService.findSimilarTroubles(999L, 5))
                .isInstanceOf(TroubleNotFoundException.class)
                .hasMessageContaining("troubleId=999");

        verifyNoInteractions(troubleEmbeddingRepository);
    }
}
