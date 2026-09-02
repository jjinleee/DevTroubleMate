package com.jinlee.devtroublemate.trouble.repository;

import com.jinlee.devtroublemate.global.config.JpaAuditingConfig;
import com.jinlee.devtroublemate.global.config.QueryDslConfig;
import com.jinlee.devtroublemate.tag.domain.Tag;
import com.jinlee.devtroublemate.tag.domain.TroubleTag;
import com.jinlee.devtroublemate.tag.repository.TagRepository;
import com.jinlee.devtroublemate.tag.repository.TroubleTagRepository;
import com.jinlee.devtroublemate.trouble.domain.Trouble;
import com.jinlee.devtroublemate.trouble.dto.TroubleSearchCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QueryDslConfig.class, JpaAuditingConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TroubleRepositoryIntegrationTest {

    private static final DockerImageName PGVECTOR_IMAGE = DockerImageName
            .parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(PGVECTOR_IMAGE)
            .withDatabaseName("devtroublemate_repository_test")
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
    private TagRepository tagRepository;

    @Autowired
    private TroubleTagRepository troubleTagRepository;

    @Test
    void filterByStatus() {
        Trouble open = saveTrouble("열린 장애", "설명");
        Trouble resolved = trouble("해결 장애", "설명");
        resolved.resolve("원인", "해결", null);
        troubleRepository.saveAndFlush(resolved);

        Page<Trouble> result = search(
                new TroubleSearchCondition("RESOLVED", null, null, false),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(Trouble::getId)
                .containsExactly(resolved.getId())
                .doesNotContain(open.getId());
    }

    @Test
    void filterByTagWithoutDuplicateTroubleRows() {
        Trouble springTrouble = saveTrouble("Spring 장애", "설명");
        Trouble dockerTrouble = saveTrouble("Docker 장애", "설명");
        Tag spring = saveTag("Spring");
        Tag backend = saveTag("Backend");
        Tag docker = saveTag("Docker");
        link(springTrouble, spring);
        link(springTrouble, backend);
        link(dockerTrouble, docker);
        troubleTagRepository.flush();

        Page<Trouble> result = search(
                new TroubleSearchCondition(null, "Spring", null, false),
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(Trouble::getId)
                .containsExactly(springTrouble.getId());
    }

    @Test
    void filterByCaseInsensitiveKeywordInTitleOrDescription() {
        Trouble titleMatch = saveTrouble("JWT TOKEN 만료", "인증 실패");
        Trouble descriptionMatch = saveTrouble("로그인 장애", "jwt token 서명 검증 실패");
        saveTrouble("데이터베이스 장애", "연결 시간 초과");

        Page<Trouble> result = search(
                new TroubleSearchCondition(null, null, "Jwt ToKeN", false),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(Trouble::getId)
                .containsExactlyInAnyOrder(titleMatch.getId(), descriptionMatch.getId());
    }

    @Test
    void separateArchivedAndActiveTroubles() {
        Trouble active = saveTrouble("활성 장애", "설명");
        Trouble archived = trouble("보관 장애", "설명");
        archived.archive();
        troubleRepository.saveAndFlush(archived);

        Page<Trouble> activeResult = search(
                new TroubleSearchCondition(null, null, null, false),
                PageRequest.of(0, 10)
        );
        Page<Trouble> archivedResult = search(
                new TroubleSearchCondition(null, null, null, true),
                PageRequest.of(0, 10)
        );

        assertThat(activeResult.getContent()).extracting(Trouble::getId)
                .containsExactly(active.getId());
        assertThat(archivedResult.getContent()).extracting(Trouble::getId)
                .containsExactly(archived.getId());
    }

    @Test
    void applyPaginationCountAndRequestedSort() {
        Trouble gamma = saveTrouble("Gamma", "설명");
        Trouble alpha = saveTrouble("Alpha", "설명");
        Trouble beta = saveTrouble("Beta", "설명");
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title"));

        Page<Trouble> result = search(
                new TroubleSearchCondition(null, null, null, false),
                pageable
        );

        assertThat(result.getContent()).extracting(Trouble::getId)
                .containsExactly(alpha.getId(), beta.getId())
                .doesNotContain(gamma.getId());
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void useIdDescendingAsStableTieBreaker() {
        Trouble first = saveTrouble("같은 제목", "첫 번째");
        Trouble second = saveTrouble("같은 제목", "두 번째");
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));

        Page<Trouble> result = search(
                new TroubleSearchCondition(null, null, null, false),
                pageable
        );

        assertThat(result.getContent()).extracting(Trouble::getId)
                .containsExactly(second.getId(), first.getId());
    }

    private Page<Trouble> search(TroubleSearchCondition condition, PageRequest pageable) {
        return troubleRepository.search(condition, pageable);
    }

    private Trouble saveTrouble(String title, String description) {
        return troubleRepository.saveAndFlush(trouble(title, description));
    }

    private Trouble trouble(String title, String description) {
        return Trouble.builder()
                .title(title)
                .description(description)
                .rawLog("ERROR log")
                .build();
    }

    private Tag saveTag(String name) {
        return tagRepository.saveAndFlush(Tag.builder().name(name).build());
    }

    private void link(Trouble trouble, Tag tag) {
        troubleTagRepository.save(TroubleTag.builder()
                .trouble(trouble)
                .tag(tag)
                .build());
    }
}
