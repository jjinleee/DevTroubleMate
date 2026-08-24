package com.jinlee.devtroublemate.trouble.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinlee.devtroublemate.ai.repository.AIAnalysisRepository;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.ai.domain.AIProcessingStatus;
import com.jinlee.devtroublemate.ai.exception.AIRetryNotAllowedException;
import com.jinlee.devtroublemate.ai.event.AIAnalysisRequestedEvent;
import com.jinlee.devtroublemate.common.dto.PageResponse;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.tag.domain.Tag;
import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.domain.TroubleStatus;
import com.jinlee.devtroublemate.trouble.dto.ResolveTroubleRequest;
import com.jinlee.devtroublemate.trouble.dto.TroubleDetailResponse;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import com.jinlee.devtroublemate.trouble.dto.TroubleSummaryResponse;
import com.jinlee.devtroublemate.trouble.dto.UpdateTroubleRequest;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

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
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TroubleService troubleService;

    @BeforeEach
    void setUp() {
        troubleService = new TroubleService(
                troubleRepository,
                tagRepository,
                troubleTagRepository,
                aiAnalysisService,
                aiAnalysisRepository,
                new ObjectMapper(),
                eventPublisher
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
    void retryFailedAIAnalysis() {
        Trouble trouble = trouble(1L);
        trouble.failAIProcessing("OPENAI_TIMEOUT", "시간 초과");
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleTagRepository.findAllByTrouble(trouble)).thenReturn(List.of());
        doAnswer(invocation -> {
            trouble.startAIProcessing();
            trouble.completeAIProcessing();
            return null;
        }).when(aiAnalysisService).analyzeAndSave(trouble, "JWT 오류", "로그인 실패", "ERROR 401", List.of());

        var response = troubleService.retryAIAnalysis(1L);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.lastErrorCode()).isNull();
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.COMPLETED);
    }

    @Test
    void retryPendingAIAnalysis() {
        Trouble trouble = trouble(1L);
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleTagRepository.findAllByTrouble(trouble)).thenReturn(List.of());
        doAnswer(invocation -> {
            trouble.startAIProcessing();
            trouble.completeAIProcessing();
            return null;
        }).when(aiAnalysisService).analyzeAndSave(trouble, "JWT 오류", "로그인 실패", "ERROR 401", List.of());

        var response = troubleService.retryAIAnalysis(1L);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(trouble.getAiProcessingStatus()).isEqualTo(AIProcessingStatus.COMPLETED);
    }

    @Test
    void rejectRetryWhenAnalysisIsCompleted() {
        Trouble trouble = trouble(1L);
        trouble.startAIProcessing();
        trouble.completeAIProcessing();
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));

        assertThatThrownBy(() -> troubleService.retryAIAnalysis(1L))
                .isInstanceOf(AIRetryNotAllowedException.class);
    }

    @Test
    void updateTroubleAndRefreshAnalysis() {
        Trouble trouble = trouble(1L);
        TroubleTag existingTag = TroubleTag.builder()
                .trouble(trouble)
                .tag(Tag.builder().name("Old").build())
                .build();
        UpdateTroubleRequest request = new UpdateTroubleRequest(
                "수정 제목",
                "수정 설명",
                "수정 로그",
                List.of(" Spring ", "Spring")
        );
        Tag spring = Tag.builder().name("Spring").build();
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(troubleTagRepository.findAllByTrouble(trouble)).thenReturn(List.of(existingTag));
        when(tagRepository.findByName("Spring")).thenReturn(Optional.of(spring));

        troubleService.update(1L, request);

        assertThat(trouble.getTitle()).isEqualTo("수정 제목");
        assertThat(trouble.getDescription()).isEqualTo("수정 설명");
        assertThat(trouble.getRawLog()).isEqualTo("수정 로그");
        verify(troubleTagRepository).deleteAll(List.of(existingTag));
        verify(tagRepository).findByName("Spring");
        verify(eventPublisher).publishEvent(new AIAnalysisRequestedEvent(1L));
    }

    @Test
    void throwExceptionWhenUpdatingMissingTrouble() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> troubleService.update(
                999L,
                new UpdateTroubleRequest("제목", "설명", "로그", List.of("태그"))
        ))
                .isInstanceOf(TroubleNotFoundException.class)
                .hasMessageContaining("troubleId=999");
    }

    @Test
    void getTroublesWithPagination() {
        Trouble first = trouble(1L);
        Trouble second = trouble(2L);
        TroubleSearchCondition condition = new TroubleSearchCondition("OPEN", null, "JWT");
        Pageable pageable = PageRequest.of(1, 2);
        when(troubleRepository.search(condition, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 5));
        when(troubleTagRepository.findAllWithTagByTroubleIdIn(List.of(1L, 2L)))
                .thenReturn(List.of());

        PageResponse<TroubleSummaryResponse> response =
                troubleService.getTroubles(condition, pageable);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.last()).isFalse();
        verify(troubleRepository).search(condition, pageable);
    }

    @Test
    void getEmptyTroublePage() {
        TroubleSearchCondition condition = new TroubleSearchCondition(null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(troubleRepository.search(condition, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<TroubleSummaryResponse> response =
                troubleService.getTroubles(condition, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.last()).isTrue();
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

    @Test
    void archiveTroubleWithoutChangingResolutionStatus() {
        Trouble trouble = trouble(1L);
        trouble.resolve("원인", "해결", null);
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));

        troubleService.archive(1L);

        assertThat(trouble.getArchivedAt()).isNotNull();
        assertThat(trouble.getStatus()).isEqualTo(TroubleStatus.RESOLVED);
    }

    @Test
    void restoreTrouble() {
        Trouble trouble = trouble(1L);
        trouble.archive();
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));

        troubleService.restore(1L);

        assertThat(trouble.getArchivedAt()).isNull();
    }

    @Test
    void throwExceptionWhenArchivingMissingTrouble() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> troubleService.archive(999L))
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
