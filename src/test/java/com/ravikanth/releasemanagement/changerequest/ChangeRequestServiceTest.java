
package com.ravikanth.releasemanagement.changerequest;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ChangeRequestService changeRequestService;

    private Environment environment;
    private User user;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        user = mock(User.class);
    }

    // --------------------------------------------------
    // CREATE CHANGE REQUEST
    // --------------------------------------------------

    @Test
    void shouldCreateChangeRequestSuccessfully() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequest result = changeRequestService.createChangeRequest(
                1L,
                "Add payment validation",
                "Validate payment requests before processing.",
                ChangeType.FEATURE,
                2L,
                null
        );

        assertNotNull(result);
        assertEquals("Add payment validation", result.getTitle());
        assertEquals(
                "Validate payment requests before processing.",
                result.getDescription()
        );
        assertEquals(ChangeType.FEATURE, result.getChangeType());
        assertEquals(ChangeRequestStatus.DRAFT, result.getStatus());
        assertSame(environment, result.getEnvironment());
        assertSame(user, result.getRequestedBy());

        verify(changeRequestRepository).save(any(ChangeRequest.class));
    }

    @Test
    void shouldTrimTitleAndDescriptionWhenCreatingChangeRequest() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequest result = changeRequestService.createChangeRequest(
                1L,
                "  Update login security  ",
                "  Improve password validation.  ",
                ChangeType.PATCH,
                2L,
                null
        );

        assertEquals("Update login security", result.getTitle());
        assertEquals("Improve password validation.", result.getDescription());
    }

    @Test
    void shouldSetDescriptionToNullWhenDescriptionIsBlank() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequest result = changeRequestService.createChangeRequest(
                1L,
                "Fix login issue",
                "   ",
                ChangeType.BUG_FIX,
                2L,
                null
        );

        assertNull(result.getDescription());
    }

    @Test
    void shouldAllowCreatingChangeRequestsWithSameTitle() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));

        when(changeRequestRepository.save(any(ChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChangeRequest first = changeRequestService.createChangeRequest(
                1L,
                "Update payment validation",
                "First request.",
                ChangeType.FEATURE,
                2L,
                null
        );

        ChangeRequest second = changeRequestService.createChangeRequest(
                1L,
                "Update payment validation",
                "Second request.",
                ChangeType.PATCH,
                2L,
                null
        );

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getTitle(), second.getTitle());
        assertNotEquals(first.getDescription(), second.getDescription());

        verify(changeRequestRepository, times(2))
                .save(any(ChangeRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentDoesNotExist() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        2L,
                        null
                )
        );

        verify(userRepository, never()).findById(anyLong());
        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenRequesterDoesNotExist() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                com.ravikanth.releasemanagement.user.UserNotFoundException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        2L,
                        null
                )
        );

        verify(changeRequestRepository, never()).save(any(ChangeRequest.class));
    }

    @Test
    void shouldRejectBlankTitleWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "   ",
                        "Some description.",
                        ChangeType.FEATURE,
                        2L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectNullTitleWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        null,
                        "Some description.",
                        ChangeType.FEATURE,
                        2L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectNullChangeTypeWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        null,
                        2L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        null,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        2L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectZeroEnvironmentIdWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        0L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        2L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectNullRequesterIdWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        null,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectZeroRequesterIdWhenCreatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.createChangeRequest(
                        1L,
                        "Fix login issue",
                        "Fix authentication validation.",
                        ChangeType.BUG_FIX,
                        0L,
                        null
                )
        );

        verifyNoInteractions(
                environmentRepository,
                userRepository,
                changeRequestRepository
        );
    }

    // --------------------------------------------------
    // GET CHANGE REQUEST BY ID
    // --------------------------------------------------

    @Test
    void shouldGetChangeRequestByIdSuccessfully() {
        ChangeRequest changeRequest = mock(ChangeRequest.class);

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        ChangeRequest result = changeRequestService.getChangeRequestById(1L);

        assertSame(changeRequest, result);

        verify(changeRequestRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenChangeRequestDoesNotExist() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ChangeRequestNotFoundException.class,
                () -> changeRequestService.getChangeRequestById(1L)
        );

        verify(changeRequestRepository).findById(1L);
    }

    @Test
    void shouldRejectNullChangeRequestIdWhenGettingById() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestById(null)
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldRejectZeroChangeRequestIdWhenGettingById() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestById(0L)
        );

        verifyNoInteractions(changeRequestRepository);
    }

    // --------------------------------------------------
    // GET CHANGE REQUESTS BY ENVIRONMENT
    // --------------------------------------------------

    @Test
    void shouldGetChangeRequestsByEnvironmentSuccessfully() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ChangeRequest> expectedPage = new PageImpl<>(List.of());

        when(environmentRepository.existsById(1L))
                .thenReturn(true);

        when(changeRequestRepository.findByEnvironmentId(1L, pageable))
                .thenReturn(expectedPage);

        Page<ChangeRequest> result =
                changeRequestService.getChangeRequestsByEnvironment(
                        1L,
                        pageable
                );

        assertSame(expectedPage, result);

        verify(environmentRepository).existsById(1L);
        verify(changeRequestRepository)
                .findByEnvironmentId(1L, pageable);
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentDoesNotExistWhenListingChangeRequests() {
        PageRequest pageable = PageRequest.of(0, 10);

        when(environmentRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                com.ravikanth.releasemanagement.environment.EnvironmentNotFoundException.class,
                () -> changeRequestService.getChangeRequestsByEnvironment(
                        1L,
                        pageable
                )
        );

        verify(changeRequestRepository, never())
                .findByEnvironmentId(anyLong(), any());
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenListingChangeRequests() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestsByEnvironment(
                        null,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(
                environmentRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectZeroEnvironmentIdWhenListingChangeRequests() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestsByEnvironment(
                        0L,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(
                environmentRepository,
                changeRequestRepository
        );
    }

    // --------------------------------------------------
    // GET CHANGE REQUESTS BY REQUESTER
    // --------------------------------------------------

    @Test
    void shouldGetChangeRequestsByRequesterSuccessfully() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ChangeRequest> expectedPage = new PageImpl<>(List.of());

        when(userRepository.existsById(2L))
                .thenReturn(true);

        when(changeRequestRepository.findByRequestedById(2L, pageable))
                .thenReturn(expectedPage);

        Page<ChangeRequest> result =
                changeRequestService.getChangeRequestsByRequester(
                        2L,
                        pageable
                );

        assertSame(expectedPage, result);

        verify(userRepository).existsById(2L);
        verify(changeRequestRepository)
                .findByRequestedById(2L, pageable);
    }

    @Test
    void shouldThrowExceptionWhenRequesterDoesNotExistWhenListingChangeRequests() {
        PageRequest pageable = PageRequest.of(0, 10);

        when(userRepository.existsById(2L))
                .thenReturn(false);

        assertThrows(
                com.ravikanth.releasemanagement.user.UserNotFoundException.class,
                () -> changeRequestService.getChangeRequestsByRequester(
                        2L,
                        pageable
                )
        );

        verify(changeRequestRepository, never())
                .findByRequestedById(anyLong(), any());
    }

    @Test
    void shouldRejectNullRequesterIdWhenListingChangeRequests() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestsByRequester(
                        null,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(
                userRepository,
                changeRequestRepository
        );
    }

    @Test
    void shouldRejectZeroRequesterIdWhenListingChangeRequests() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.getChangeRequestsByRequester(
                        0L,
                        PageRequest.of(0, 10)
                )
        );

        verifyNoInteractions(
                userRepository,
                changeRequestRepository
        );
    }

    // --------------------------------------------------
    // UPDATE CHANGE REQUEST
    // --------------------------------------------------

    @Test
    void shouldUpdateDraftChangeRequestSuccessfully() {
        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Old title",
                "Old description",
                ChangeType.FEATURE,
                user,
                null
        );

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        ChangeRequest result = changeRequestService.updateChangeRequest(
                1L,
                "Updated title",
                "Updated description",
                ChangeType.PATCH,
                null
        );

        assertSame(changeRequest, result);
        assertEquals("Updated title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(ChangeType.PATCH, result.getChangeType());
    }

    @Test
    void shouldTrimTitleAndDescriptionWhenUpdatingChangeRequest() {
        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Old title",
                "Old description",
                ChangeType.FEATURE,
                user,
                null
        );

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        ChangeRequest result = changeRequestService.updateChangeRequest(
                1L,
                "  Updated title  ",
                "  Updated description  ",
                ChangeType.BUG_FIX,
                null
        );

        assertEquals("Updated title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        assertEquals(ChangeType.BUG_FIX, result.getChangeType());
    }

    @Test
    void shouldSetDescriptionToNullWhenUpdatingWithBlankDescription() {
        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Old title",
                "Old description",
                ChangeType.FEATURE,
                user,
                null
        );

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        ChangeRequest result = changeRequestService.updateChangeRequest(
                1L,
                "Updated title",
                "   ",
                ChangeType.PATCH,
                null
        );

        assertNull(result.getDescription());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonexistentChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ChangeRequestNotFoundException.class,
                () -> changeRequestService.updateChangeRequest(
                        1L,
                        "Updated title",
                        "Updated description",
                        ChangeType.PATCH,
                        null
                )
        );
    }

    @Test
    void shouldRejectNullChangeRequestIdWhenUpdating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.updateChangeRequest(
                        null,
                        "Updated title",
                        "Updated description",
                        ChangeType.PATCH,
                        null
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldRejectZeroChangeRequestIdWhenUpdating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.updateChangeRequest(
                        0L,
                        "Updated title",
                        "Updated description",
                        ChangeType.PATCH,
                        null
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldRejectBlankTitleWhenUpdatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.updateChangeRequest(
                        1L,
                        "   ",
                        "Updated description",
                        ChangeType.PATCH,
                        null
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldRejectNullTitleWhenUpdatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.updateChangeRequest(
                        1L,
                        null,
                        "Updated description",
                        ChangeType.PATCH,
                        null
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    @Test
    void shouldRejectNullChangeTypeWhenUpdatingChangeRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.updateChangeRequest(
                        1L,
                        "Updated title",
                        "Updated description",
                        null,
                        null
                )
        );

        verifyNoInteractions(changeRequestRepository);
    }

    // --------------------------------------------------
    // SUBMIT CHANGE REQUEST
    // --------------------------------------------------

    @Test
    void shouldSubmitDraftChangeRequestSuccessfully() {
        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Add payment validation",
                "Validate payment requests.",
                ChangeType.FEATURE,
                user,
                null
        );

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        ChangeRequest result =
                changeRequestService.submitChangeRequest(1L, 10L);

        assertSame(changeRequest, result);
        assertEquals(
                ChangeRequestStatus.SUBMITTED,
                result.getStatus()
        );

        verify(changeRequestRepository).findById(1L);

        verify(auditLogService).recordAudit(
                10L,
                "CHANGE_REQUEST",
                null,
                "CHANGE_REQUEST_SUBMITTED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenSubmittingNonexistentChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ChangeRequestNotFoundException.class,
                () -> changeRequestService.submitChangeRequest(1L, 10L)
        );

        verify(auditLogService, never()).recordAudit(
                anyLong(),
                anyString(),
                nullable(Long.class),
                anyString(),
                any()
        );
    }

    @Test
    void shouldRejectNullChangeRequestIdWhenSubmitting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.submitChangeRequest(null, 10L)
        );

        verifyNoInteractions(
                changeRequestRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectZeroChangeRequestIdWhenSubmitting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.submitChangeRequest(0L, 10L)
        );

        verifyNoInteractions(
                changeRequestRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectNullSubmitterIdWhenSubmitting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.submitChangeRequest(1L, null)
        );

        verifyNoInteractions(
                changeRequestRepository,
                auditLogService
        );
    }

    @Test
    void shouldRejectZeroSubmitterIdWhenSubmitting() {
        assertThrows(
                IllegalArgumentException.class,
                () -> changeRequestService.submitChangeRequest(1L, 0L)
        );

        verifyNoInteractions(
                changeRequestRepository,
                auditLogService
        );
    }

    @Test
    void shouldNotSubmitChangeRequestMoreThanOnce() {
        ChangeRequest changeRequest = new ChangeRequest(
                environment,
                "Add payment validation",
                "Validate payment requests.",
                ChangeType.FEATURE,
                user,
                null
        );

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        changeRequestService.submitChangeRequest(1L, 10L);

        assertThrows(
                IllegalStateException.class,
                () -> changeRequestService.submitChangeRequest(1L, 10L)
        );

        assertEquals(
                ChangeRequestStatus.SUBMITTED,
                changeRequest.getStatus()
        );

        verify(auditLogService, times(1)).recordAudit(
                10L,
                "CHANGE_REQUEST",
                null,
                "CHANGE_REQUEST_SUBMITTED",
                null
        );
    }
}
