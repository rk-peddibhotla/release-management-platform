
package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestRepository;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestStatus;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.environment.EnvironmentRepository;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReleaseService releaseService;

    private Environment environment;
    private User user;

    private static final Long ENVIRONMENT_ID = 1L;
    private static final Long RELEASE_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long PERFORMED_BY_ID = 3L;
    private static final Long CHANGE_REQUEST_ID = 2L;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        user = mock(User.class);
    }

    // --------------------------------------------------
    // CREATE RELEASE
    // --------------------------------------------------

    @Test
    void shouldCreateReleaseSuccessfully() {
        OffsetDateTime scheduledAt = OffsetDateTime.now();

        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.of(environment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                ENVIRONMENT_ID, "1.0.0"
        )).thenReturn(false);
        when(releaseRepository.save(any(Release.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Release result = releaseService.createRelease(
                ENVIRONMENT_ID,
                "1.0.0",
                "Initial release",
                scheduledAt,
                USER_ID
        );

        assertNotNull(result);
        assertEquals("1.0.0", result.getVersion());
        assertEquals("Initial release", result.getDescription());
        assertEquals(scheduledAt, result.getScheduledAt());
        assertEquals(ReleaseStatus.SCHEDULED, result.getStatus());
        assertSame(environment, result.getEnvironment());
        assertSame(user, result.getCreatedBy());

        verify(releaseRepository).save(any(Release.class));
        verify(auditLogService).recordAudit(
                USER_ID,
                "RELEASE",
                result.getId(),
                "RELEASE_CREATED",
                null
        );
    }

    @Test
    void shouldTrimVersionAndDescriptionWhenCreatingRelease() {
        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.of(environment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                ENVIRONMENT_ID, "2.0.0"
        )).thenReturn(false);
        when(releaseRepository.save(any(Release.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Release result = releaseService.createRelease(
                ENVIRONMENT_ID,
                "  2.0.0  ",
                "  Release notes  ",
                null,
                USER_ID
        );

        assertEquals("2.0.0", result.getVersion());
        assertEquals("Release notes", result.getDescription());
    }

    @Test
    void shouldSetDescriptionToNullWhenDescriptionIsBlank() {
        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.of(environment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                ENVIRONMENT_ID, "1.0.0"
        )).thenReturn(false);
        when(releaseRepository.save(any(Release.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Release result = releaseService.createRelease(
                ENVIRONMENT_ID,
                "1.0.0",
                "   ",
                null,
                USER_ID
        );

        assertNull(result.getDescription());
    }

    @Test
    void shouldAllowSameVersionInDifferentEnvironments() {
        Environment anotherEnvironment = mock(Environment.class);

        when(environmentRepository.findById(2L))
                .thenReturn(Optional.of(anotherEnvironment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "1.0.0"
        )).thenReturn(false);
        when(releaseRepository.save(any(Release.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Release result = releaseService.createRelease(
                2L, "1.0.0", null, null, USER_ID
        );

        assertEquals("1.0.0", result.getVersion());
        assertSame(anotherEnvironment, result.getEnvironment());
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentDoesNotExist() {
        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "1.0.0", null, null, USER_ID
                )
        );

        verifyNoInteractions(userRepository, releaseRepository);
    }

    @Test
    void shouldThrowExceptionWhenCreatorDoesNotExist() {
        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.of(environment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.user.UserNotFoundException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "1.0.0", null, null, USER_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectDuplicateReleaseVersion() {
        when(environmentRepository.findById(ENVIRONMENT_ID))
                .thenReturn(Optional.of(environment));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                ENVIRONMENT_ID, "1.0.0"
        )).thenReturn(true);

        assertThrows(
                ReleaseAlreadyExistsException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "1.0.0", null, null, USER_ID
                )
        );

        verify(releaseRepository, never()).save(any(Release.class));
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectBlankVersionWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "   ", null, null, USER_ID
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                releaseRepository
        );
    }

    @Test
    void shouldRejectNullVersionWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, null, null, null, USER_ID
                )
        );
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        null, "1.0.0", null, null, USER_ID
                )
        );
    }

    @Test
    void shouldRejectZeroEnvironmentIdWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        0L, "1.0.0", null, null, USER_ID
                )
        );
    }

    @Test
    void shouldRejectNullCreatorIdWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "1.0.0", null, null, null
                )
        );
    }

    @Test
    void shouldRejectZeroCreatorIdWhenCreatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.createRelease(
                        ENVIRONMENT_ID, "1.0.0", null, null, 0L
                )
        );
    }

    // --------------------------------------------------
    // GET RELEASE
    // --------------------------------------------------

    @Test
    void shouldGetReleaseByIdSuccessfully() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));

        Release result = releaseService.getReleaseById(RELEASE_ID);

        assertSame(release, result);
        verify(releaseRepository).findById(RELEASE_ID);
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExist() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.getReleaseById(RELEASE_ID)
        );
    }

    @Test
    void shouldRejectNullReleaseIdWhenRetrievingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.getReleaseById(null)
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectZeroReleaseIdWhenRetrievingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.getReleaseById(0L)
        );

        verifyNoInteractions(releaseRepository);
    }

    // --------------------------------------------------
    // GET RELEASES BY ENVIRONMENT
    // --------------------------------------------------

    @Test
    void shouldGetReleasesByEnvironmentSuccessfully() {
        Release release = mock(Release.class);
        Page<Release> expectedPage = new PageImpl<>(List.of(release));
        var pageable = PageRequest.of(0, 10);

        when(environmentRepository.existsById(ENVIRONMENT_ID))
                .thenReturn(true);
        when(releaseRepository.findByEnvironmentId(
                ENVIRONMENT_ID, pageable
        )).thenReturn(expectedPage);

        Page<Release> result =
                releaseService.getReleasesByEnvironment(
                        ENVIRONMENT_ID, pageable
                );

        assertSame(expectedPage, result);
        assertEquals(1, result.getTotalElements());

        verify(releaseRepository).findByEnvironmentId(
                ENVIRONMENT_ID, pageable
        );
    }

    @Test
    void shouldReturnEmptyPageWhenEnvironmentHasNoReleases() {
        Page<Release> emptyPage = Page.empty();
        var pageable = PageRequest.of(0, 10);

        when(environmentRepository.existsById(ENVIRONMENT_ID))
                .thenReturn(true);
        when(releaseRepository.findByEnvironmentId(
                ENVIRONMENT_ID, pageable
        )).thenReturn(emptyPage);

        Page<Release> result =
                releaseService.getReleasesByEnvironment(
                        ENVIRONMENT_ID, pageable
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentDoesNotExistForReleaseListing() {
        when(environmentRepository.existsById(ENVIRONMENT_ID))
                .thenReturn(false);

        assertThrows(
                com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException.class,
                () -> releaseService.getReleasesByEnvironment(
                        ENVIRONMENT_ID,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenGettingReleases() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.getReleasesByEnvironment(
                        null, PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(environmentRepository, releaseRepository);
    }

    @Test
    void shouldRejectZeroEnvironmentIdWhenGettingReleases() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.getReleasesByEnvironment(
                        0L, PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(environmentRepository, releaseRepository);
    }

    // --------------------------------------------------
    // UPDATE RELEASE
    // --------------------------------------------------

    @Test
    void shouldUpdateReleaseSuccessfully() {
        Release release = mock(Release.class);
        OffsetDateTime scheduledAt = OffsetDateTime.now();

        when(release.getId()).thenReturn(RELEASE_ID);
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(2L);
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "2.0.0"
        )).thenReturn(false);

        Release result = releaseService.updateRelease(
                RELEASE_ID,
                "2.0.0",
                "Updated release",
                scheduledAt,
                PERFORMED_BY_ID
        );

        assertSame(release, result);

        verify(release).updateDetails(
                "2.0.0",
                "Updated release",
                scheduledAt
        );

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "RELEASE_UPDATED",
                null
        );
    }

    @Test
    void shouldTrimVersionAndDescriptionWhenUpdatingRelease() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(2L);
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "3.0.0"
        )).thenReturn(false);

        releaseService.updateRelease(
                RELEASE_ID,
                "  3.0.0  ",
                "  Updated notes  ",
                null,
                PERFORMED_BY_ID
        );

        verify(release).updateDetails(
                "3.0.0",
                "Updated notes",
                null
        );
    }

    @Test
    void shouldSetDescriptionToNullWhenUpdatingWithBlankDescription() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(2L);
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "1.0.0"
        )).thenReturn(false);

        releaseService.updateRelease(
                RELEASE_ID,
                "1.0.0",
                "   ",
                null,
                PERFORMED_BY_ID
        );

        verify(release).updateDetails(
                "1.0.0",
                null,
                null
        );
    }

    @Test
    void shouldRejectUpdatingReleaseWithDuplicateVersion() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(2L);
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "2.0.0"
        )).thenReturn(true);
        when(release.getVersion()).thenReturn("1.0.0");

        assertThrows(
                ReleaseAlreadyExistsException.class,
                () -> releaseService.updateRelease(
                        RELEASE_ID,
                        "2.0.0",
                        null,
                        null,
                        PERFORMED_BY_ID
                )
        );

        verify(release, never()).updateDetails(
                anyString(),
                nullable(String.class),
                nullable(OffsetDateTime.class)
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldAllowUpdatingReleaseWithoutChangingItsVersion() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(2L);
        when(releaseRepository.existsByEnvironmentIdAndVersionIgnoreCase(
                2L, "1.0.0"
        )).thenReturn(true);
        when(release.getVersion()).thenReturn("1.0.0");

        Release result = releaseService.updateRelease(
                RELEASE_ID,
                "1.0.0",
                "Description changed",
                null,
                PERFORMED_BY_ID
        );

        assertSame(release, result);

        verify(release).updateDetails(
                "1.0.0",
                "Description changed",
                null
        );
    }

    @Test
    void shouldRejectUpdatingNonScheduledRelease() {
        Release release = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.IN_PROGRESS);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.updateRelease(
                        RELEASE_ID,
                        "2.0.0",
                        null,
                        null,
                        PERFORMED_BY_ID
                )
        );

        verify(release, never()).updateDetails(
                anyString(),
                nullable(String.class),
                nullable(OffsetDateTime.class)
        );
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForUpdate() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.updateRelease(
                        RELEASE_ID,
                        "2.0.0",
                        null,
                        null,
                        PERFORMED_BY_ID
                )
        );

        verify(releaseRepository, never())
                .existsByEnvironmentIdAndVersionIgnoreCase(
                        anyLong(), anyString()
                );
    }

    @Test
    void shouldRejectNullReleaseIdWhenUpdatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.updateRelease(
                        null, "2.0.0", null, null, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectZeroReleaseIdWhenUpdatingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.updateRelease(
                        0L, "2.0.0", null, null, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectBlankOrNullVersionWhenUpdatingRelease() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.updateRelease(
                                RELEASE_ID, null, null, null,
                                PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.updateRelease(
                                RELEASE_ID, "   ", null, null,
                                PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    // --------------------------------------------------
    // CANCEL RELEASE
    // --------------------------------------------------

    @Test
    void shouldCancelReleaseSuccessfully() {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn(RELEASE_ID);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));

        Release result = releaseService.cancelRelease(
                RELEASE_ID, PERFORMED_BY_ID
        );

        assertSame(release, result);
        verify(release).cancel();

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "RELEASE_CANCELLED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForCancellation() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.cancelRelease(
                        RELEASE_ID, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullReleaseIdWhenCancellingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.cancelRelease(
                        null, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectZeroReleaseIdWhenCancellingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.cancelRelease(
                        0L, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    @Test
    void shouldRejectNegativeReleaseIdWhenCancellingRelease() {
        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.cancelRelease(
                        -1L, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    // --------------------------------------------------
    // ADD CHANGE REQUEST
    // --------------------------------------------------

    @Test
    void shouldAddApprovedChangeRequestToReleaseSuccessfully() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        when(release.getId()).thenReturn(RELEASE_ID);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));

        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.APPROVED);
        when(changeRequest.getRelease())
                .thenReturn(null);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(changeRequest.getEnvironment())
                .thenReturn(environment);
        when(environment.getId())
                .thenReturn(10L);

        Release result = releaseService.addChangeRequest(
                RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
        );

        assertSame(release, result);
        verify(changeRequest).assignToRelease(release);

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "CHANGE_REQUEST_ADDED_TO_RELEASE",
                "Change request ID: " + CHANGE_REQUEST_ID
        );
    }

    @Test
    void shouldRejectAddingDraftChangeRequestToRelease() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.DRAFT);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).assignToRelease(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectAddingSubmittedChangeRequestToRelease() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.SUBMITTED);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).assignToRelease(any());
    }

    @Test
    void shouldRejectAddingRejectedChangeRequestToRelease() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.REJECTED);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).assignToRelease(any());
    }

    @Test
    void shouldRejectAddingChangeRequestAlreadyAssignedToAnotherRelease() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        Release anotherRelease = mock(Release.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.APPROVED);
        when(changeRequest.getRelease())
                .thenReturn(anotherRelease);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).assignToRelease(any());
    }

    @Test
    void shouldRejectAddingChangeRequestFromDifferentEnvironment() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);
        Environment anotherEnvironment = mock(Environment.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getStatus())
                .thenReturn(ChangeRequestStatus.APPROVED);
        when(changeRequest.getRelease())
                .thenReturn(null);
        when(release.getEnvironment())
                .thenReturn(environment);
        when(changeRequest.getEnvironment())
                .thenReturn(anotherEnvironment);
        when(environment.getId()).thenReturn(10L);
        when(anotherEnvironment.getId()).thenReturn(20L);

        assertThrows(
                IllegalArgumentException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).assignToRelease(any());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForAssignment() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldThrowExceptionWhenChangeRequestDoesNotExistForAssignment() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(mock(Release.class)));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.changerequest.ChangeRequestNotFoundException.class,
                () -> releaseService.addChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );
    }

    @Test
    void shouldRejectInvalidReleaseIdWhenAddingChangeRequest() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                null, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                0L, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                -1L, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository, changeRequestRepository);
    }

    @Test
    void shouldRejectInvalidChangeRequestIdWhenAddingToRelease() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                RELEASE_ID, null, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                RELEASE_ID, 0L, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.addChangeRequest(
                                RELEASE_ID, -1L, PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository, changeRequestRepository);
    }

    // --------------------------------------------------
    // REMOVE CHANGE REQUEST
    // --------------------------------------------------

    @Test
    void shouldRemoveChangeRequestFromReleaseSuccessfully() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getRelease())
                .thenReturn(release);
        when(release.getId())
                .thenReturn(RELEASE_ID);

        Release result = releaseService.removeChangeRequest(
                RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
        );

        assertSame(release, result);
        verify(changeRequest).removeFromRelease();

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "CHANGE_REQUEST_REMOVED_FROM_RELEASE",
                "Change request ID: " + CHANGE_REQUEST_ID
        );
    }

    @Test
    void shouldRejectRemovingUnassignedChangeRequest() {
        Release release = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getRelease())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.removeChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).removeFromRelease();
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectRemovingChangeRequestFromAnotherRelease() {
        Release release = mock(Release.class);
        Release anotherRelease = mock(Release.class);
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.of(changeRequest));
        when(release.getStatus())
                .thenReturn(ReleaseStatus.SCHEDULED);
        when(changeRequest.getRelease())
                .thenReturn(anotherRelease);
        when(anotherRelease.getId())
                .thenReturn(3L);

        assertThrows(
                IllegalStateException.class,
                () -> releaseService.removeChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verify(changeRequest, never()).removeFromRelease();
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForRemoval() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.removeChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldThrowExceptionWhenChangeRequestDoesNotExistForRemoval() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(mock(Release.class)));
        when(changeRequestRepository.findById(CHANGE_REQUEST_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.changerequest.ChangeRequestNotFoundException.class,
                () -> releaseService.removeChangeRequest(
                        RELEASE_ID, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                )
        );
    }

    @Test
    void shouldRejectInvalidIdsWhenRemovingChangeRequest() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.removeChangeRequest(
                                null, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.removeChangeRequest(
                                0L, CHANGE_REQUEST_ID, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.removeChangeRequest(
                                RELEASE_ID, null, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.removeChangeRequest(
                                RELEASE_ID, 0L, PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository, changeRequestRepository);
    }

    // --------------------------------------------------
    // START RELEASE
    // --------------------------------------------------

    @Test
    void shouldStartDeploymentSuccessfully() {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn(RELEASE_ID);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));

        Release result = releaseService.startDeployment(
                RELEASE_ID, PERFORMED_BY_ID
        );

        assertSame(release, result);
        verify(release).start();

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "RELEASE_STARTED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForDeployment() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.startDeployment(
                        RELEASE_ID, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectInvalidIdWhenStartingDeployment() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.startDeployment(
                                null, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.startDeployment(
                                0L, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.startDeployment(
                                -1L, PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository);
    }

    // --------------------------------------------------
    // COMPLETE RELEASE
    // --------------------------------------------------

    @Test
    void shouldCompleteReleaseSuccessfully() {
        Release release = mock(Release.class);
        when(release.getId()).thenReturn(RELEASE_ID);

        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.of(release));

        Release result = releaseService.completeRelease(
                RELEASE_ID, PERFORMED_BY_ID
        );

        assertSame(release, result);
        verify(release).complete();

        verify(auditLogService).recordAudit(
                PERFORMED_BY_ID,
                "RELEASE",
                RELEASE_ID,
                "RELEASE_COMPLETED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenReleaseDoesNotExistForCompletion() {
        when(releaseRepository.findById(RELEASE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> releaseService.completeRelease(
                        RELEASE_ID, PERFORMED_BY_ID
                )
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectInvalidIdWhenCompletingRelease() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.completeRelease(
                                null, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.completeRelease(
                                0L, PERFORMED_BY_ID
                        )
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> releaseService.completeRelease(
                                -1L, PERFORMED_BY_ID
                        )
                )
        );

        verifyNoInteractions(releaseRepository);
    }
}
