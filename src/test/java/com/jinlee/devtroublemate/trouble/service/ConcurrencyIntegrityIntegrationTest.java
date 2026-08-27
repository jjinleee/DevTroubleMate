package com.jinlee.devtroublemate.trouble.service;

import com.jinlee.devtroublemate.ai.exception.AIRetryNotAllowedException;
import com.jinlee.devtroublemate.ai.service.AIAnalysisService;
import com.jinlee.devtroublemate.retrospective.dto.RetrospectiveRequest;
import com.jinlee.devtroublemate.retrospective.exception.RetrospectiveAlreadyExistsException;
import com.jinlee.devtroublemate.retrospective.repository.RetrospectiveRepository;
import com.jinlee.devtroublemate.retrospective.service.RetrospectiveService;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ConcurrencyIntegrityIntegrationTest {

    @Autowired
    private TroubleService troubleService;

    @Autowired
    private TroubleRepository troubleRepository;

    @Autowired
    private TroubleTagRepository troubleTagRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Autowired
    private RetrospectiveRepository retrospectiveRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private AIAnalysisService aiAnalysisService;

    @AfterEach
    void cleanUp() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            retrospectiveRepository.deleteAllInBatch();
            troubleTagRepository.deleteAllInBatch();
            tagRepository.deleteAllInBatch();
            troubleRepository.deleteAllInBatch();
        });
    }

    @Test
    void allowOnlyOneConcurrentAIRetry() throws Exception {
        Trouble saved = troubleRepository.saveAndFlush(trouble("동시 AI 재시도"));
        CountDownLatch firstAnalysisStarted = new CountDownLatch(1);
        CountDownLatch finishFirstAnalysis = new CountDownLatch(1);

        doAnswer(invocation -> {
            Trouble trouble = invocation.getArgument(0);
            trouble.startAIProcessing();
            firstAnalysisStarted.countDown();
            assertThat(finishFirstAnalysis.await(3, TimeUnit.SECONDS)).isTrue();
            trouble.completeAIProcessing();
            return null;
        }).when(aiAnalysisService).analyzeAndSave(
                org.mockito.ArgumentMatchers.any(Trouble.class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> troubleService.retryAIAnalysis(saved.getId()));
            assertThat(firstAnalysisStarted.await(3, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> troubleService.retryAIAnalysis(saved.getId()));
            Thread.sleep(200);
            assertThat(second.isDone()).isFalse();

            finishFirstAnalysis.countDown();
            first.get(3, TimeUnit.SECONDS);

            assertThatThrownBy(() -> second.get(3, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(AIRetryNotAllowedException.class);
            verify(aiAnalysisService, times(1)).analyzeAndSave(
                    org.mockito.ArgumentMatchers.any(Trouble.class),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyList()
            );
        } finally {
            finishFirstAnalysis.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void createOnlyOneTagForConcurrentRequests() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> insertTagConcurrently("Concurrency", ready, start));
            Future<?> second = executor.submit(() -> insertTagConcurrently("Concurrency", ready, start));
            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            first.get(3, TimeUnit.SECONDS);
            second.get(3, TimeUnit.SECONDS);

            assertThat(tagRepository.countByName("Concurrency")).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void detectConcurrentTroubleModificationWithOptimisticLock() throws Exception {
        Trouble saved = troubleRepository.saveAndFlush(trouble("동시 수정"));
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch update = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> updateTrouble(saved.getId(), loaded, update, true));
            Future<Boolean> second = executor.submit(() -> updateTrouble(saved.getId(), loaded, update, false));
            assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
            update.countDown();

            assertThat(List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            update.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void allowOnlyOneRetrospectiveForConcurrentRequests() throws Exception {
        Trouble saved = troubleRepository.saveAndFlush(trouble("동시 회고 생성"));
        RetrospectiveRequest request = new RetrospectiveRequest("대응 과정", "재발 방지");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> createRetrospective(saved.getId(), request, ready, start));
            Future<Boolean> second = executor.submit(() -> createRetrospective(saved.getId(), request, ready, start));
            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(retrospectiveRepository.count()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void insertTagConcurrently(String name, CountDownLatch ready, CountDownLatch start) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            ready.countDown();
            await(start);
            tagRepository.insertIfAbsent(name);
        });
    }

    private boolean updateTrouble(
            Long troubleId,
            CountDownLatch loaded,
            CountDownLatch update,
            boolean archive
    ) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Trouble trouble = troubleRepository.findById(troubleId).orElseThrow();
                loaded.countDown();
                await(update);
                if (archive) {
                    trouble.archive();
                } else {
                    trouble.resolve("원인", "해결", null);
                }
            });
            return true;
        } catch (ObjectOptimisticLockingFailureException exception) {
            return false;
        }
    }

    private boolean createRetrospective(
            Long troubleId,
            RetrospectiveRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        await(start);
        try {
            retrospectiveService.create(troubleId, request);
            return true;
        } catch (RetrospectiveAlreadyExistsException exception) {
            return false;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", exception);
        }
    }

    private Trouble trouble(String title) {
        return Trouble.builder()
                .title(title)
                .description("설명")
                .rawLog("로그")
                .build();
    }
}
