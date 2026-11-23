
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

        User engineer = new User(
                "Integration Test Engineer",
                "deployment.integration@example.com",
                "test-password-hash",
                UserRole.ENGINEER
        );

        entityManager.persist(engineer);

        Application application = new Application(
                "Deployment Integration Test App",
                "Application created for deployment integration testing."
        );

        entityManager.persist(application);
        entityManager.flush();

        Environment environment = new Environment(
                application,
                "TEST",
                EnvironmentType.TESTING
        );

        entityManager.persist(environment);
        entityManager.flush();

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

        entityManager.flush();
        entityManager.clear();

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

        Release persistedRelease = releaseRepository
                .findById(releaseId)
                .orElseThrow();

        assertEquals(
                ReleaseStatus.COMPLETED,
                persistedRelease.getStatus()
        );

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

    @Test
    void shouldPersistFailedDeploymentAndSuccessfulRetry() {

        // 1. Create an engineer.
        User engineer = new User(
                "Retry Integration Engineer",
                "deployment.retry@example.com",
                "test-password-hash",
                UserRole.ENGINEER
        );

        entityManager.persist(engineer);

        // 2. Create an application.
        Application application = new Application(
                "Deployment Retry Integration App",
                "Application created for deployment retry testing."
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
                "2.0.0",
                "Deployment retry integration test",
                OffsetDateTime.now(),
                engineer
        );

        release.start();

        entityManager.persist(release);
        entityManager.flush();

        Long releaseId = release.getId();
        Long engineerId = engineer.getId();

        // 5. Record the first attempt as FAILED.
        OffsetDateTime firstStartedAt = OffsetDateTime.now().minusMinutes(10);
        OffsetDateTime firstCompletedAt = OffsetDateTime.now().minusMinutes(5);

        Deployment failedDeployment = deploymentService.recordDeployment(
                releaseId,
                engineerId,
                DeploymentStatus.FAILED,
                firstStartedAt,
                firstCompletedAt,
                "Initial deployment failed."
        );

        assertNotNull(failedDeployment.getId());
        assertEquals(1, failedDeployment.getAttemptNumber());
        assertEquals(DeploymentStatus.FAILED, failedDeployment.getStatus());

        entityManager.flush();
        entityManager.clear();

        // 6. Verify the failed attempt was persisted.
        Deployment persistedFailedDeployment = deploymentRepository
                .findById(failedDeployment.getId())
                .orElseThrow();

        assertEquals(1, persistedFailedDeployment.getAttemptNumber());
        assertEquals(
                DeploymentStatus.FAILED,
                persistedFailedDeployment.getStatus()
        );

        // 7. Verify the release remains IN_PROGRESS.
        Release releaseAfterFailure = releaseRepository
                .findById(releaseId)
                .orElseThrow();

        assertEquals(
                ReleaseStatus.IN_PROGRESS,
                releaseAfterFailure.getStatus()
        );

        // 8. Record the second attempt as SUCCESS.
        OffsetDateTime retryStartedAt = OffsetDateTime.now().minusMinutes(3);
        OffsetDateTime retryCompletedAt = OffsetDateTime.now();

        Deployment successfulRetry = deploymentService.recordDeployment(
                releaseId,
                engineerId,
                DeploymentStatus.SUCCESS,
                retryStartedAt,
                retryCompletedAt,
                "Deployment succeeded on retry."
        );

        assertNotNull(successfulRetry.getId());
        assertEquals(2, successfulRetry.getAttemptNumber());
        assertEquals(DeploymentStatus.SUCCESS, successfulRetry.getStatus());

        // 9. Flush and clear to verify persisted data.
        entityManager.flush();
        entityManager.clear();

        List<Deployment> deployments = deploymentRepository
                .findByReleaseIdOrderByAttemptNumberAsc(releaseId);

        assertEquals(2, deployments.size());

        assertEquals(1, deployments.get(0).getAttemptNumber());
        assertEquals(
                DeploymentStatus.FAILED,
                deployments.get(0).getStatus()
        );

        assertEquals(2, deployments.get(1).getAttemptNumber());
        assertEquals(
                DeploymentStatus.SUCCESS,
                deployments.get(1).getStatus()
        );

        // 10. Verify the release is now COMPLETED.
        Release completedRelease = releaseRepository
                .findById(releaseId)
                .orElseThrow();

        assertEquals(
                ReleaseStatus.COMPLETED,
                completedRelease.getStatus()
        );

        // 11. Verify all three audit records were persisted.
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

        assertEquals(3, auditLogs.size());

        assertEquals(
                "DEPLOYMENT_FAILED",
                auditLogs.get(0).getAction()
        );

        assertEquals(
                "DEPLOYMENT_SUCCEEDED",
                auditLogs.get(1).getAction()
        );

        assertEquals(
                "RELEASE_COMPLETED",
                auditLogs.get(2).getAction()
        );
    }
}
