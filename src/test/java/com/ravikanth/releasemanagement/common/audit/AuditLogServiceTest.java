package com.ravikanth.releasemanagement.common.audit;

import com.ravikanth.releasemanagement.user.User;
import com.ravikanth.releasemanagement.user.UserNotFoundException;
import com.ravikanth.releasemanagement.user.UserRepository;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private UserRepository userRepository;
    private AuditLogService auditLogService;

    private User user;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        userRepository = mock(UserRepository.class);

        auditLogService = new AuditLogService(
                auditLogRepository,
                userRepository
        );

        user = mock(User.class);
    }

    @Test
    void recordAuditShouldSaveAuditLog() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.recordAudit(
                1L,
                "APPLICATION",
                10L,
                "CREATED",
                "Application created"
        );
        assertNotNull(result);
        assertSame(user, result.getUser());
        assertEquals("APPLICATION", result.getEntityType());
        assertEquals(10L, result.getEntityId());
        assertEquals("CREATED", result.getAction());
        assertEquals("{\"message\":\"Application created\"}", result.getDetails());

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void recordAuditShouldNormalizeEntityTypeAndAction() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.recordAudit(
                1L,
                "  application ",
                10L,
                " created ",
                null
        );

        assertEquals("APPLICATION", result.getEntityType());
        assertEquals("CREATED", result.getAction());
        assertEquals("{}", result.getDetails());
    }

    @Test
    void recordAuditShouldTrimDetails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.recordAudit(
                1L,
                "APPLICATION",
                10L,
                "CREATED",
                "  Application created  "
        );

        assertEquals("{\"message\":\"Application created\"}", result.getDetails());
    }

    @Test
    void recordAuditShouldConvertBlankDetailsToEmptyJsonObject() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.recordAudit(
                1L,
                "APPLICATION",
                10L,
                "CREATED",
                "   "
        );

        assertEquals("{}", result.getDetails());
    }

    @Test
    void recordAuditShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> auditLogService.recordAudit(
                        1L,
                        "APPLICATION",
                        10L,
                        "CREATED",
                        null
                )
        );

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void recordAuditShouldRejectNullUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        null, "APPLICATION", 10L, "CREATED", null
                )
        );
    }

    @Test
    void recordAuditShouldRejectInvalidUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        0L, "APPLICATION", 10L, "CREATED", null
                )
        );
    }

    @Test
    void recordAuditShouldRejectBlankEntityType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        1L, " ", 10L, "CREATED", null
                )
        );
    }

    @Test
    void recordAuditShouldRejectNullEntityType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        1L, null, 10L, "CREATED", null
                )
        );
    }

    @Test
    void recordAuditShouldRejectInvalidEntityId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        1L, "APPLICATION", 0L, "CREATED", null
                )
        );
    }

    @Test
    void recordAuditShouldRejectBlankAction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.recordAudit(
                        1L, "APPLICATION", 10L, " ", null
                )
        );
    }

    @Test
    void getAuditHistoryShouldReturnMatchingRecords() {
        var pageable = PageRequest.of(0, 10);
        Page<AuditLog> expectedPage = Page.empty(pageable);

        when(auditLogRepository.findByEntityTypeAndEntityId(
                "APPLICATION",
                10L,
                pageable
        )).thenReturn(expectedPage);

        Page<AuditLog> result = auditLogService.getAuditHistory(
                "APPLICATION",
                10L,
                pageable
        );

        assertSame(expectedPage, result);

        verify(auditLogRepository).findByEntityTypeAndEntityId(
                "APPLICATION",
                10L,
                pageable
        );
    }

    @Test
    void getAuditHistoryShouldNormalizeEntityType() {
        var pageable = PageRequest.of(0, 10);

        when(auditLogRepository.findByEntityTypeAndEntityId(
                "RELEASE",
                5L,
                pageable
        )).thenReturn(Page.empty(pageable));

        auditLogService.getAuditHistory(
                " release ",
                5L,
                pageable
        );

        verify(auditLogRepository).findByEntityTypeAndEntityId(
                "RELEASE",
                5L,
                pageable
        );
    }

    @Test
    void getAuditHistoryShouldRejectBlankEntityType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getAuditHistory(
                        " ",
                        10L,
                        PageRequest.of(0, 10)
                )
        );
    }

    @Test
    void getAuditHistoryShouldRejectInvalidEntityId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getAuditHistory(
                        "APPLICATION",
                        0L,
                        PageRequest.of(0, 10)
                )
        );
    }

    @Test
    void getAuditHistoryByUserShouldReturnMatchingRecords() {
        var pageable = PageRequest.of(0, 10);
        Page<AuditLog> expectedPage = Page.empty(pageable);

        when(userRepository.existsById(1L)).thenReturn(true);

        when(auditLogRepository.findByUserId(1L, pageable))
                .thenReturn(expectedPage);

        Page<AuditLog> result = auditLogService.getAuditHistoryByUser(
                1L,
                pageable
        );

        assertSame(expectedPage, result);

        verify(auditLogRepository).findByUserId(1L, pageable);
    }

    @Test
    void getAuditHistoryByUserShouldThrowWhenUserDoesNotExist() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> auditLogService.getAuditHistoryByUser(
                        1L,
                        PageRequest.of(0, 10)
                )
        );

        verify(auditLogRepository, never()).findByUserId(
                anyLong(),
                any()
        );
    }

    @Test
    void getAuditHistoryByUserShouldRejectInvalidUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> auditLogService.getAuditHistoryByUser(
                        0L,
                        PageRequest.of(0, 10)
                )
        );
    }



        @Test
        void recordAuditShouldEscapeSpecialCharactersInDetails() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog result = auditLogService.recordAudit(
                1L,
                "APPLICATION",
                10L,
                "CREATED",
                "Created \"test\" with path C:\\files\nNext line\tTabbed"
        );

        assertEquals(
                "{\"message\":\"Created \\\"test\\\" with path C:\\\\files\\nNext line\\tTabbed\"}",
                result.getDetails()
        );
        }

















}