
package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.common.audit.AuditLog;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentType;
import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.release.ReleaseRepository;
import com.ravikanth.releasemanagement.release.ReleaseStatus;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRole;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DeploymentServiceIntegrationTest {

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistSuccessfulDeploymentAndCompleteRelease() {

        // 1. Create an engineer.
        User engineer = new User(
                "Integration Test Engineer",
                "deployment.integration@example.com",
                "test-password-hash",
                UserRole.ENGINEER
        );

        entityManager.persist(engineer);

        // 2. Create an application.
        Application application = new Application(
                "Deployment Integration Test App",
                "Application created for deployment integration testing."
        );

        entityManager.persist(application);

        entityManager.flush();

        // 3. Create an environment.
        Environment environment = new Environment(
                application,
                "TEST",
                EnvironmentType.TESTING
        );

        entityManager.persist(environment);

        entityManager.flush();

        // 4. Create a release and move it to IN_PROGRESS.
        Release release = new Release(
                environment,
                "1.0.0",
                "Integration test release",
                OffsetDateTime.now(),
                engineer
        );

        release.start();

        entityManager.persist(release);

        entityManager.flush();

        Long releaseId = release.getId();
        Long engineerId = engineer.getId();

        // 5. Record a successful deployment.
        OffsetDateTime startedAt = OffsetDateTime.now().minusMinutes(5);
        OffsetDateTime completedAt = OffsetDateTime.now();

        Deployment deployment = deploymentService.recordDeployment(
                releaseId,
                engineerId,
                DeploymentStatus.SUCCESS,
                startedAt,
                completedAt,
                "Integration test deployment"
        );

        assertNotNull(deployment.getId());
        assertEquals(1, deployment.getAttemptNumber());
        assertEquals(DeploymentStatus.SUCCESS, deployment.getStatus());

        // 6. Flush and clear the persistence context.
        // This ensures the following assertions read persisted data.
        entityManager.flush();
        entityManager.clear();

        // 7. Verify the deployment was persisted.
        Deployment persistedDeployment = deploymentRepository
                .findById(deployment.getId())
                .orElseThrow();

        assertEquals(1, persistedDeployment.getAttemptNumber());
        assertEquals(
                DeploymentStatus.SUCCESS,
                persistedDeployment.getStatus()
        );
        assertEquals(
                "Integration test deployment",
                persistedDeployment.getNotes()
        );

        // 8. Verify the release was completed.
        Release persistedRelease = releaseRepository
                .findById(releaseId)
                .orElseThrow();

        assertEquals(
                ReleaseStatus.COMPLETED,
                persistedRelease.getStatus()
        );

        // 9. Verify both audit records were persisted.
        List<AuditLog> auditLogs = entityManager.createQuery(
                """
                SELECT a
                FROM AuditLog a
                WHERE a.entityType = :entityType
                  AND a.entityId = :entityId
                ORDER BY a.id
                """,
                AuditLog.class
        )
        .setParameter("entityType", "RELEASE")
        .setParameter("entityId", releaseId)
        .getResultList();

        assertEquals(2, auditLogs.size());

        assertEquals(
                "DEPLOYMENT_SUCCEEDED",
                auditLogs.get(0).getAction()
        );

        assertEquals(
                "RELEASE_COMPLETED",
                auditLogs.get(1).getAction()
        );
    }
}
