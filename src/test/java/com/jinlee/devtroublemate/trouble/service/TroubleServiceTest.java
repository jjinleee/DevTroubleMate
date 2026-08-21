package com.jinlee.devtroublemate.trouble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.domain.TroubleStatus;
import com.jinlee.devtroublemate.trouble.dto.ResolveTroubleRequest;
import com.jinlee.devtroublemate.trouble.dto.TroubleDetailResponse;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TroubleServiceTest {

    @Mock
    private TroubleRepository troubleRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private TroubleTagRepository troubleTagRepository;
    @Mock
    private AIAnalysisService aiAnalysisService;
    @Mock
    private AIAnalysisRepository aiAnalysisRepository;

    private TroubleService troubleService;

    @BeforeEach
    void setUp() {
        troubleService = new TroubleService(
                troubleRepository,
                tagRepository,
                troubleTagRepository,
                aiAnalysisService,
                aiAnalysisRepository,
                new ObjectMapper()
        );
    }

    @Test
    void getTroubleDetail() {
        Trouble trouble = trouble(1L);
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleTagRepository.findAllWithTagByTroubleIdIn(List.of(1L)))
                .thenReturn(List.of());
        when(aiAnalysisRepository.findTopByTroubleOrderByCreatedAtDesc(trouble))
                .thenReturn(Optional.empty());

        TroubleDetailResponse response = troubleService.getDetail(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("JWT 오류");
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.tags()).isEmpty();
        assertThat(response.aiAnalysis()).isNull();
    }

    @Test
    void throwExceptionWhenGettingMissingTrouble() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> troubleService.getDetail(999L))
                .isInstanceOf(TroubleNotFoundException.class)
                .hasMessageContaining("troubleId=999");
    }

    @Test
    void resolveTrouble() {
        Trouble trouble = trouble(1L);
        ResolveTroubleRequest request = new ResolveTroubleRequest(
                "토큰 만료 설정 오류",
                "만료 설정 수정",
                "https://docs.example.com/jwt"
        );
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));

        troubleService.resolve(1L, request);

        assertThat(trouble.getStatus()).isEqualTo(TroubleStatus.RESOLVED);
        assertThat(trouble.getActualCause()).isEqualTo(request.actualCause());
        assertThat(trouble.getSolution()).isEqualTo(request.solution());
        assertThat(trouble.getReferenceLink()).isEqualTo(request.referenceLink());
        verify(troubleRepository).findById(1L);
    }

    @Test
    void throwExceptionWhenResolvingMissingTrouble() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> troubleService.resolve(
                999L,
                new ResolveTroubleRequest("원인", "해결", null)
        ))
                .isInstanceOf(TroubleNotFoundException.class)
                .hasMessageContaining("troubleId=999");
    }

    private Trouble trouble(Long id) {
        Trouble trouble = Trouble.builder()
                .title("JWT 오류")
                .description("로그인 실패")
                .rawLog("ERROR 401")
                .build();
        ReflectionTestUtils.setField(trouble, "id", id);
        return trouble;
    }
}
