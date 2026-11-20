package com.ravikanth.releasemanagement.common.audit;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindAuditLogByEntity() {
        User user = createUser();

        AuditLog auditLog = new AuditLog(
                user,
                "APPLICATION",
                100L,
                "CREATED",
                "{\"name\":\"Payment Service\"}"
        );

        auditLogRepository.saveAndFlush(auditLog);

        Page<AuditLog> result =
                auditLogRepository.findByEntityTypeAndEntityId(
                        "APPLICATION",
                        100L,
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEntityType())
                .isEqualTo("APPLICATION");
        assertThat(result.getContent().get(0).getEntityId())
                .isEqualTo(100L);
        assertThat(result.getContent().get(0).getAction())
                .isEqualTo("CREATED");
    }

    @Test
    void shouldFindAuditLogsByUser() {
        User user = createUser();

        AuditLog firstLog = new AuditLog(
                user,
                "APPLICATION",
                100L,
                "CREATED",
                "{\"name\":\"Payment Service\"}"
        );

        AuditLog secondLog = new AuditLog(
                user,
                "RELEASE",
                200L,
                "CREATED",
                "{\"version\":\"1.0.0\"}"
        );

        auditLogRepository.save(firstLog);
        auditLogRepository.save(secondLog);
        auditLogRepository.flush();

        Page<AuditLog> result =
                auditLogRepository.findByUserId(
                        user.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(AuditLog::getAction)
                .containsExactlyInAnyOrder("CREATED", "CREATED");
    }

    private User createUser() {
        User user = new User(
                "Audit Test User",
                "audit-" + UUID.randomUUID() + "@example.com",
                "test-password-hash",
                UserRole.ADMIN
        );

        return userRepository.saveAndFlush(user);
    }
}