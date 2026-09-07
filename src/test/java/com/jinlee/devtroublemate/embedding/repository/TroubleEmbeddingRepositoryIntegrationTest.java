package com.jinlee.devtroublemate.embedding.repository;

import com.jinlee.devtroublemate.embedding.domain.TroubleEmbedding;
import com.jinlee.devtroublemate.embedding.dto.SimilarTroubleResponse;
import com.jinlee.devtroublemate.embedding.service.SimilarTroubleService;
import com.jinlee.devtroublemate.global.config.JpaAuditingConfig;
import com.jinlee.devtroublemate.global.config.QueryDslConfig;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.repository.TroubleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@Import({JpaAuditingConfig.class, QueryDslConfig.class, SimilarTroubleService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TroubleEmbeddingRepositoryIntegrationTest {

    private static final int EMBEDDING_DIMENSIONS = 1536;
    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("devtroublemate_embedding_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configurePostgreSQL(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TroubleRepository troubleRepository;

    @Autowired
    private TroubleEmbeddingRepository troubleEmbeddingRepository;

    @Autowired
    private SimilarTroubleService similarTroubleService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storeAndLoad1536DimensionVector() {
        Trouble trouble = saveTrouble("벡터 저장 테스트");
        float[] vector = vector(0.8f, 0.6f);
        saveEmbedding(trouble, vector);
        entityManager.clear();

        Trouble reloadedTrouble = troubleRepository.findById(trouble.getId()).orElseThrow();
        TroubleEmbedding reloaded = troubleEmbeddingRepository.findByTrouble(reloadedTrouble).orElseThrow();

        assertThat(reloaded.getEmbedding()).hasSize(EMBEDDING_DIMENSIONS);
        assertThat(reloaded.getEmbedding()[0]).isCloseTo(0.8f, within(0.0001f));
        assertThat(reloaded.getEmbedding()[1]).isCloseTo(0.6f, within(0.0001f));
    }

    @Test
    void orderByCosineSimilarityAndExcludeTargetAndArchivedTrouble() {
        SearchFixture fixture = saveSearchFixture();

        List<Object[]> rows = troubleEmbeddingRepository.findSimilarTroubles(fixture.target().getId(), 10);

        assertThat(rows).extracting(row -> ((Number) row[0]).longValue())
                .containsExactly(
                        fixture.nearest().getId(),
                        fixture.medium().getId(),
                        fixture.orthogonal().getId()
                )
                .doesNotContain(fixture.target().getId(), fixture.archived().getId());
        assertThat(((Number) rows.get(0)[3]).doubleValue()).isGreaterThan(((Number) rows.get(1)[3]).doubleValue());
        assertThat(((Number) rows.get(1)[3]).doubleValue()).isGreaterThan(((Number) rows.get(2)[3]).doubleValue());
        assertThat(((Number) rows.get(0)[3]).doubleValue()).isCloseTo(0.9938837, within(0.000001));
        assertThat(((Number) rows.get(1)[3]).doubleValue()).isCloseTo(Math.sqrt(0.5), within(0.000001));
        assertThat(((Number) rows.get(2)[3]).doubleValue()).isCloseTo(0.0, within(0.000001));
    }

    @Test
    void limitResultToTopK() {
        SearchFixture fixture = saveSearchFixture();

        List<Object[]> rows = troubleEmbeddingRepository.findSimilarTroubles(fixture.target().getId(), 2);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> ((Number) row[0]).longValue())
                .containsExactly(fixture.nearest().getId(), fixture.medium().getId());
    }

    @Test
    void filterByMinimumSimilarityInService() {
        SearchFixture fixture = saveSearchFixture();

        List<SimilarTroubleResponse> result = similarTroubleService.findSimilarTroubles(
                fixture.target().getId(),
                10,
                0.8
        );

        assertThat(result).extracting(SimilarTroubleResponse::troubleId)
                .containsExactly(fixture.nearest().getId());
        assertThat(result.get(0).similarity()).isGreaterThanOrEqualTo(0.8);
    }

    private SearchFixture saveSearchFixture() {
        Trouble target = saveTrouble("기준 장애");
        Trouble nearest = saveTrouble("가장 유사한 장애");
        Trouble medium = saveTrouble("중간 유사도 장애");
        Trouble orthogonal = saveTrouble("직교 장애");
        Trouble archived = Trouble.builder()
                .title("보관된 동일 장애")
                .description("설명")
                .rawLog("로그")
                .build();
        archived.archive();
        troubleRepository.saveAndFlush(archived);

        saveEmbedding(target, vector(1.0f, 0.0f));
        saveEmbedding(nearest, vector(0.9f, 0.1f));
        saveEmbedding(medium, vector(0.7f, 0.7f));
        saveEmbedding(orthogonal, vector(0.0f, 1.0f));
        saveEmbedding(archived, vector(1.0f, 0.0f));
        troubleEmbeddingRepository.flush();

        return new SearchFixture(target, nearest, medium, orthogonal, archived);
    }

    private Trouble saveTrouble(String title) {
        return troubleRepository.saveAndFlush(Trouble.builder()
                .title(title)
                .description("설명")
                .rawLog("로그")
                .build());
    }

    private void saveEmbedding(Trouble trouble, float[] vector) {
        troubleEmbeddingRepository.saveAndFlush(TroubleEmbedding.builder()
                .trouble(trouble)
                .inputText(trouble.getTitle())
                .embedding(vector)
                .modelName("test-embedding-model")
                .build());
    }

    private float[] vector(float first, float second) {
        float[] vector = new float[EMBEDDING_DIMENSIONS];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }

    private record SearchFixture(
            Trouble target,
            Trouble nearest,
            Trouble medium,
            Trouble orthogonal,
            Trouble archived
    ) {
    }
}
