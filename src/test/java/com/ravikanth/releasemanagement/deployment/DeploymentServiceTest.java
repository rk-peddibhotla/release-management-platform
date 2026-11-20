
package com.ravikanth.releasemanagement.deployment;

import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.release.Release;
import com.ravikanth.releasemanagement.release.ReleaseNotFoundException;
import com.ravikanth.releasemanagement.release.ReleaseRepository;
import com.ravikanth.releasemanagement.release.ReleaseStatus;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DeploymentService deploymentService;

    private Release release;
    private User user;

    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;

    @BeforeEach
    void setUp() {
        release = mock(Release.class);
        user = mock(User.class);

        startedAt = OffsetDateTime.parse("2026-11-19T10:00:00+05:30");
        completedAt = OffsetDateTime.parse("2026-11-19T10:30:00+05:30");
    }

    @Test
    void shouldRecordFirstSuccessfulDeployment() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.empty());
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.SUCCESS,
                startedAt,
                completedAt,
                "Deployment completed"
        );

        assertAll(
                () -> assertEquals(1, result.getAttemptNumber()),
                () -> assertEquals(DeploymentStatus.SUCCESS, result.getStatus()),
                () -> assertEquals("Deployment completed", result.getNotes()),
                () -> assertSame(release, result.getRelease()),
                () -> assertSame(user, result.getDeployedBy())
        );

        verify(release).complete();

        verify(deploymentRepository).save(any(Deployment.class));

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "DEPLOYMENT_SUCCEEDED",
                "Deployment attempt: 1, status: SUCCESS"
        );

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "RELEASE_COMPLETED",
                "Completed after successful deployment attempt: 1"
        );
    }

    @Test
    void shouldRecordFirstFailedDeployment() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.empty());
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.FAILED,
                startedAt,
                completedAt,
                "Deployment failed"
        );

        assertEquals(1, result.getAttemptNumber());
        assertEquals(DeploymentStatus.FAILED, result.getStatus());

        verify(release, never()).complete();

        verify(deploymentRepository).save(any(Deployment.class));

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "DEPLOYMENT_FAILED",
                "Deployment attempt: 1, status: FAILED"
        );

        verify(auditLogService, never()).recordAudit(
                2L,
                "RELEASE",
                1L,
                "RELEASE_COMPLETED",
                "Completed after successful deployment attempt: 1"
        );
    }

    @Test
    void shouldIncrementAttemptNumberForRetry() {
        Deployment previousDeployment = mock(Deployment.class);

        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.of(previousDeployment));
        when(previousDeployment.getAttemptNumber())
                .thenReturn(2);
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.FAILED,
                startedAt,
                completedAt,
                "Third attempt"
        );

        assertEquals(3, result.getAttemptNumber());

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "DEPLOYMENT_FAILED",
                "Deployment attempt: 3, status: FAILED"
        );
    }

    @Test
    void shouldCompleteReleaseAfterSuccessfulRetry() {
        Deployment previousDeployment = mock(Deployment.class);

        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.of(previousDeployment));
        when(previousDeployment.getAttemptNumber())
                .thenReturn(1);
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.SUCCESS,
                startedAt,
                completedAt,
                "Retry succeeded"
        );

        assertEquals(2, result.getAttemptNumber());

        verify(release).complete();

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "DEPLOYMENT_SUCCEEDED",
                "Deployment attempt: 2, status: SUCCESS"
        );

        verify(auditLogService).recordAudit(
                2L,
                "RELEASE",
                1L,
                "RELEASE_COMPLETED",
                "Completed after successful deployment attempt: 2"
        );
    }

    @Test
    void shouldNormalizeDeploymentNotes() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.empty());
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.FAILED,
                startedAt,
                completedAt,
                "  Connection timeout  "
        );

        assertEquals("Connection timeout", result.getNotes());
    }

    @Test
    void shouldConvertBlankNotesToNull() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(true);
        when(deploymentRepository
                .findTopByReleaseIdOrderByAttemptNumberDesc(1L))
                .thenReturn(Optional.empty());
        when(deploymentRepository.save(any(Deployment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deployment result = deploymentService.recordDeployment(
                1L,
                2L,
                DeploymentStatus.FAILED,
                startedAt,
                completedAt,
                "   "
        );

        assertNull(result.getNotes());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExist() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(userRepository, deploymentRepository, auditLogService);
    }

    @Test
    void shouldRejectDeploymentForScheduledRelease() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);

        assertThrows(
                IllegalStateException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(userRepository, deploymentRepository, auditLogService);
    }

    @Test
    void shouldRejectDeploymentForCompletedRelease() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.COMPLETED);

        assertThrows(
                IllegalStateException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(userRepository, deploymentRepository, auditLogService);
    }

    @Test
    void shouldRejectDeploymentForCancelledRelease() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.CANCELLED);

        assertThrows(
                IllegalStateException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(userRepository, deploymentRepository, auditLogService);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(deploymentRepository, auditLogService);
    }

    @Test
    void shouldRejectDeploymentByInactiveUser() {
        when(releaseRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(user.isActive())
                .thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(deploymentRepository, auditLogService);
    }

    @Test
    void shouldRejectInvalidReleaseIds() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                null, 2L, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                0L, 2L, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                -1L, 2L, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectInvalidUserIds() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                1L, null, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                1L, 0L, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.recordDeployment(
                                1L, -1L, DeploymentStatus.SUCCESS,
                                startedAt, completedAt, null
                        )
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectNullDeploymentStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, null,
                        startedAt, completedAt, null
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectNullStartTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        null, completedAt, null
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectNullCompletionTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.recordDeployment(
                        1L, 2L, DeploymentStatus.SUCCESS,
                        startedAt, null, null
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectCompletionTimeBeforeStartTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.recordDeployment(
                        1L,
                        2L,
                        DeploymentStatus.SUCCESS,
                        completedAt,
                        startedAt,
                        null
                )
        );

        verifyNoInteractions(
                releaseRepository,
                userRepository,
                deploymentRepository,
                auditLogService
        );
    }

    @Test
    void shouldGetDeploymentHistoryByReleaseId() {
        Deployment firstAttempt = mock(Deployment.class);
        Deployment secondAttempt = mock(Deployment.class);

        when(releaseRepository.existsById(1L))
                .thenReturn(true);

        when(deploymentRepository
                .findByReleaseIdOrderByAttemptNumberAsc(1L))
                .thenReturn(List.of(firstAttempt, secondAttempt));

        List<Deployment> result =
                deploymentService.getDeploymentsByReleaseId(1L);

        assertEquals(2, result.size());
        assertSame(firstAttempt, result.get(0));
        assertSame(secondAttempt, result.get(1));

        verify(releaseRepository).existsById(1L);
        verify(deploymentRepository)
                .findByReleaseIdOrderByAttemptNumberAsc(1L);
    }

    @Test
    void shouldReturnEmptyDeploymentHistoryWhenNoAttemptsExist() {
        when(releaseRepository.existsById(1L))
                .thenReturn(true);

        when(deploymentRepository
                .findByReleaseIdOrderByAttemptNumberAsc(1L))
                .thenReturn(List.of());

        List<Deployment> result =
                deploymentService.getDeploymentsByReleaseId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForDeploymentHistory() {
        when(releaseRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                ReleaseNotFoundException.class,
                () -> deploymentService.getDeploymentsByReleaseId(1L)
        );

        verifyNoInteractions(deploymentRepository);
    }

    @Test
    void shouldRejectNullReleaseIdWhenGettingDeploymentHistory() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.getDeploymentsByReleaseId(null)
        );

        verifyNoInteractions(releaseRepository, deploymentRepository);
    }

    @Test
    void shouldRejectInvalidReleaseIdsWhenGettingDeploymentHistory() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.getDeploymentsByReleaseId(0L)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.getDeploymentsByReleaseId(-1L)
                )
        );

        verifyNoInteractions(releaseRepository, deploymentRepository);
    }

    @Test
    void shouldGetDeploymentById() {
        Deployment deployment = mock(Deployment.class);

        when(deploymentRepository.findById(1L))
                .thenReturn(Optional.of(deployment));

        Deployment result = deploymentService.getDeploymentById(1L);

        assertSame(deployment, result);
        verify(deploymentRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeploymentDoesNotExist() {
        when(deploymentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                DeploymentNotFoundException.class,
                () -> deploymentService.getDeploymentById(1L)
        );
    }

    @Test
    void shouldRejectNullDeploymentId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> deploymentService.getDeploymentById(null)
        );

        verifyNoInteractions(deploymentRepository);
    }

    @Test
    void shouldRejectInvalidDeploymentIds() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.getDeploymentById(0L)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> deploymentService.getDeploymentById(-1L)
                )
        );

        verifyNoInteractions(deploymentRepository);
    }
}
