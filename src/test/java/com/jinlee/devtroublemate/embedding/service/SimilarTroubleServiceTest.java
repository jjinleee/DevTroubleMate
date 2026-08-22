package com.jinlee.devtroublemate.embedding.service;

import com.jinlee.devtroublemate.embedding.repository.TroubleEmbeddingRepository;
import com.jinlee.devtroublemate.embedding.exception.TroubleEmbeddingNotFoundException;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
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

        assertThatThrownBy(() -> similarTroubleService.findSimilarTroubles(999L, 5, 0.0))
                .isInstanceOf(TroubleNotFoundException.class)
                .hasMessageContaining("troubleId=999");

        verifyNoInteractions(troubleEmbeddingRepository);
    }

    @Test
    void throwExceptionWhenEmbeddingDoesNotExist() {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleEmbeddingRepository.existsByTrouble(trouble)).thenReturn(false);

        assertThatThrownBy(() -> similarTroubleService.findSimilarTroubles(1L, 5, 0.7))
                .isInstanceOf(TroubleEmbeddingNotFoundException.class)
                .hasMessageContaining("troubleId=1");
    }

    @Test
    void returnFilteredSimilarTroubles() {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleEmbeddingRepository.existsByTrouble(trouble)).thenReturn(true);
        when(troubleEmbeddingRepository.findSimilarTroubles(null, 3, 0.8))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "유사 장애", "RESOLVED", 0.91}));

        var responses = similarTroubleService.findSimilarTroubles(1L, 3, 0.8);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).troubleId()).isEqualTo(2L);
        assertThat(responses.get(0).similarity()).isEqualTo(0.91);
    }
}
