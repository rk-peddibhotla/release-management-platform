
package com.ravikanth.releasemanagement.approval;

import com.ravikanth.releasemanagement.changerequest.ChangeRequest;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestNotFoundException;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestRepository;
import com.ravikanth.releasemanagement.changerequest.ChangeRequestStatus;
import com.ravikanth.releasemanagement.changerequest.ChangeType;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;
import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;
import com.ravikanth.releasemanagement.user.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private ChangeRequestRepository changeRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private User requester;

    @Mock
    private User approver;

    @Mock
    private Environment environment;

    @InjectMocks
    private ApprovalService approvalService;

    private ChangeRequest changeRequest;

    @BeforeEach
    void setUp() {
        changeRequest = new ChangeRequest(
                environment,
                "Deploy payment fix",
                "Fix payment processing issue",
                ChangeType.BUG_FIX,
                requester,
                OffsetDateTime.now()
        );
    }

    @Test
    void shouldApproveSubmittedChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval savedApproval = approvalService.approveChangeRequest(
                1L,
                2L,
                "Approved for deployment."
        );

        assertNotNull(savedApproval);
        assertEquals(
                ChangeRequestStatus.APPROVED,
                changeRequest.getStatus()
        );
        assertEquals(
                ApprovalDecision.APPROVED,
                savedApproval.getDecision()
        );
        assertEquals(
                approver,
                savedApproval.getApprover()
        );
        assertEquals(
                "Approved for deployment.",
                savedApproval.getComments()
        );

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                "Approved for deployment."
        );
    }

    @Test
    void shouldRejectSubmittedChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval savedApproval = approvalService.rejectChangeRequest(
                1L,
                2L,
                "Rejected because the change is not ready."
        );

        assertNotNull(savedApproval);
        assertEquals(
                ChangeRequestStatus.REJECTED,
                changeRequest.getStatus()
        );
        assertEquals(
                ApprovalDecision.REJECTED,
                savedApproval.getDecision()
        );
        assertEquals(
                approver,
                savedApproval.getApprover()
        );
        assertEquals(
                "Rejected because the change is not ready.",
                savedApproval.getComments()
        );

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_REJECTED",
                "Rejected because the change is not ready."
        );
    }

    @Test
    void shouldAllowAdminToApprove() {
        User admin = mock(User.class);

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(admin));

        when(requester.getId()).thenReturn(1L);

        when(admin.getId()).thenReturn(2L);
        when(admin.getRole()).thenReturn(UserRole.ADMIN);
        when(admin.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.approveChangeRequest(
                1L,
                2L,
                "Approved by admin."
        );

        assertNotNull(approval);
        assertEquals(
                ApprovalDecision.APPROVED,
                approval.getDecision()
        );
        assertEquals(
                ChangeRequestStatus.APPROVED,
                changeRequest.getStatus()
        );

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                "Approved by admin."
        );
    }

    @Test
    void shouldAllowNullComments() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.approveChangeRequest(
                1L,
                2L,
                null
        );

        assertNotNull(approval);
        assertNull(approval.getComments());

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                null
        );
    }

    @Test
    void shouldConvertBlankCommentsToNull() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.approveChangeRequest(
                1L,
                2L,
                "   "
        );

        assertNotNull(approval);
        assertNull(approval.getComments());

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                null
        );
    }

    @Test
    void shouldTrimApprovalComments() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.approveChangeRequest(
                1L,
                2L,
                "   Approved   "
        );

        assertNotNull(approval);
        assertEquals(
                "Approved",
                approval.getComments()
        );

        verify(approvalRepository).save(any(Approval.class));

        verify(auditLogService).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                "Approved"
        );
    }

    @Test
    void shouldRecordApproverAndDecisionOnApproval() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.approveChangeRequest(
                1L,
                2L,
                "Approved."
        );

        assertNotNull(approval);
        assertEquals(
                approver,
                approval.getApprover()
        );
        assertEquals(
                ApprovalDecision.APPROVED,
                approval.getDecision()
        );

        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    void shouldRecordApproverAndDecisionOnRejection() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        Approval approval = approvalService.rejectChangeRequest(
                1L,
                2L,
                "Rejected."
        );

        assertNotNull(approval);
        assertEquals(
                approver,
                approval.getApprover()
        );
        assertEquals(
                ApprovalDecision.REJECTED,
                approval.getDecision()
        );

        verify(approvalRepository).save(any(Approval.class));
    }

    @Test
    void shouldOnlyRecordOneAuditEntryForApproval() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        approvalService.approveChangeRequest(
                1L,
                2L,
                "Approved."
        );

        verify(auditLogService, times(1)).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_APPROVED",
                "Approved."
        );
    }

    @Test
    void shouldOnlyRecordOneAuditEntryForRejection() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(requester.getId()).thenReturn(1L);

        when(approver.getId()).thenReturn(2L);
        when(approver.getRole()).thenReturn(UserRole.APPROVER);
        when(approver.isActive()).thenReturn(true);

        when(approvalRepository.save(any(Approval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        changeRequest.submit();

        approvalService.rejectChangeRequest(
                1L,
                2L,
                "Rejected."
        );

        verify(auditLogService, times(1)).recordAudit(
                2L,
                "CHANGE_REQUEST",
                1L,
                "CHANGE_REQUEST_REJECTED",
                "Rejected."
        );
    }

    @Test
    void shouldPreventRequesterFromApprovingOwnChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(requester));

        when(requester.getId()).thenReturn(1L);
        when(requester.getRole()).thenReturn(UserRole.APPROVER);
        when(requester.isActive()).thenReturn(true);

        changeRequest.submit();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        1L,
                        "Self approval."
                )
        );

        assertEquals(
                "Users cannot approve or reject their own change requests.",
                exception.getMessage()
        );

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldPreventRequesterFromRejectingOwnChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(requester));

        when(requester.getId()).thenReturn(1L);
        when(requester.getRole()).thenReturn(UserRole.APPROVER);
        when(requester.isActive()).thenReturn(true);

        changeRequest.submit();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.rejectChangeRequest(
                        1L,
                        1L,
                        "Self rejection."
                )
        );

        assertEquals(
                "Users cannot approve or reject their own change requests.",
                exception.getMessage()
        );

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectInactiveApprover() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(approver.isActive()).thenReturn(false);

        changeRequest.submit();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Inactive users cannot approve or reject change requests.",
                exception.getMessage()
        );

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectEngineerAttemptingApproval() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(approver));

        when(approver.getRole()).thenReturn(UserRole.ENGINEER);
        when(approver.isActive()).thenReturn(true);

        changeRequest.submit();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Only approvers and admins can decide change requests.",
                exception.getMessage()
        );

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectDecisionForDraftChangeRequest() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Only submitted change requests can receive a decision.",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectDecisionForAlreadyApprovedChangeRequest() {
        changeRequest.submit();
        changeRequest.approve();

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Second approval."
                )
        );

        assertEquals(
                "Only submitted change requests can receive a decision.",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectDecisionForAlreadyRejectedChangeRequest() {
        changeRequest.submit();
        changeRequest.reject();

        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> approvalService.rejectChangeRequest(
                        1L,
                        2L,
                        "Second rejection."
                )
        );

        assertEquals(
                "Only submitted change requests can receive a decision.",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectWhenChangeRequestDoesNotExist() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.empty());

        ChangeRequestNotFoundException exception = assertThrows(
                ChangeRequestNotFoundException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Change request with ID 1 was not found.",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectWhenApproverDoesNotExist() {
        when(changeRequestRepository.findById(1L))
                .thenReturn(Optional.of(changeRequest));

        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        changeRequest.submit();

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "User with ID 2 was not found.",
                exception.getMessage()
        );

        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullChangeRequestId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approveChangeRequest(
                        null,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Change request ID must be positive.",
                exception.getMessage()
        );

        verifyNoInteractions(changeRequestRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectZeroChangeRequestId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approveChangeRequest(
                        0L,
                        2L,
                        "Approval."
                )
        );

        assertEquals(
                "Change request ID must be positive.",
                exception.getMessage()
        );

        verifyNoInteractions(changeRequestRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullApproverId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        null,
                        "Approval."
                )
        );

        assertEquals(
                "Approver ID must be positive.",
                exception.getMessage()
        );

        verifyNoInteractions(changeRequestRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectZeroApproverId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> approvalService.approveChangeRequest(
                        1L,
                        0L,
                        "Approval."
                )
        );

        assertEquals(
                "Approver ID must be positive.",
                exception.getMessage()
        );

        verifyNoInteractions(changeRequestRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(approvalRepository);
        verifyNoInteractions(auditLogService);
    }
}
