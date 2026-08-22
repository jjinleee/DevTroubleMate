package com.jinlee.devtroublemate.retrospective.service;

import com.jinlee.devtroublemate.retrospective.domain.Retrospective;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveRequest;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveAlreadyExistsException;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveNotFoundException;
import com.jinlee.devtroublemate.retrospective.repository.RetrospectiveRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.exception.TroubleNotFoundException;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrospectiveServiceTest {

    @Mock RetrospectiveRepository retrospectiveRepository;
    @Mock TroubleRepository troubleRepository;
    @InjectMocks RetrospectiveService retrospectiveService;

    @Test
    void createRetrospective() {
        Trouble trouble = trouble(1L);
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble));
        when(retrospectiveRepository.existsByTroubleId(1L)).thenReturn(false);
        when(retrospectiveRepository.save(any())).thenAnswer(invocation -> {
            Retrospective saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        var response = retrospectiveService.create(
                1L,
                new RetrospectiveRequest("대응 과정", "자동화 필요")
        );

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.troubleId()).isEqualTo(1L);
        assertThat(response.lesson()).isEqualTo("자동화 필요");
    }

    @Test
    void rejectDuplicateRetrospective() {
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble(1L)));
        when(retrospectiveRepository.existsByTroubleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> retrospectiveService.create(
                1L, new RetrospectiveRequest("내용", "교훈")))
                .isInstanceOf(RetrospectiveAlreadyExistsException.class);
    }

    @Test
    void throwWhenRetrospectiveDoesNotExist() {
        when(troubleRepository.findById(1L)).thenReturn(Optional.of(trouble(1L)));
        when(retrospectiveRepository.findByTroubleId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.get(1L))
                .isInstanceOf(RetrospectiveNotFoundException.class);
    }

    @Test
    void throwWhenTroubleDoesNotExist() {
        when(troubleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retrospectiveService.get(999L))
                .isInstanceOf(TroubleNotFoundException.class);
    }

    private Trouble trouble(Long id) {
        Trouble trouble = Trouble.builder().title("제목").description("설명").rawLog("로그").build();
        ReflectionTestUtils.setField(trouble, "id", id);
        return trouble;
    }
}
