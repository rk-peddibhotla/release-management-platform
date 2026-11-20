package com.ravikanth.releasemanagement.application;

import com.ravikanth.releasemanagement.common.audit.AuditLogService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void shouldCreateApplicationWithNormalizedName() {
        when(applicationRepository.existsByNameIgnoreCase("Payment Service"))
                .thenReturn(false);

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(
                "  Payment Service  ",
                "  Handles payment processing  ",
                1L
        );

        assertThat(result.getName()).isEqualTo("Payment Service");
        assertThat(result.getDescription())
                .isEqualTo("Handles payment processing");

        verify(applicationRepository)
                .existsByNameIgnoreCase("Payment Service");

        verify(applicationRepository)
                .save(any(Application.class));
    }

    @Test
    void shouldRecordAuditWhenCreatingApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.existsByNameIgnoreCase("Payment Service"))
                .thenReturn(false);

        when(applicationRepository.save(any(Application.class)))
                .thenReturn(application);

        applicationService.createApplication(
                "Payment Service",
                "Handles payments",
                1L
        );

        verify(auditLogService).recordAudit(
                1L,
                "APPLICATION",
                null,
                "APPLICATION_CREATED",
                null
        );
    }

    @Test
    void shouldRejectDuplicateApplicationName() {
        when(applicationRepository.existsByNameIgnoreCase("payment service"))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(
                " payment service ",
                "Handles payment processing",
                1L
        ))
                .isInstanceOf(ApplicationAlreadyExistsException.class)
                .hasMessage(
                        "An application with name 'payment service' already exists."
                );

        verify(applicationRepository)
                .existsByNameIgnoreCase("payment service");

        verify(applicationRepository, never())
                .save(any(Application.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullApplicationName() {
        assertThatThrownBy(() -> applicationService.createApplication(
                null,
                "Description",
                1L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application name cannot be blank.");

        verify(applicationRepository, never())
                .save(any(Application.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectBlankApplicationName() {
        assertThatThrownBy(() -> applicationService.createApplication(
                "   ",
                "Description",
                1L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application name cannot be blank.");

        verify(applicationRepository, never())
                .save(any(Application.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldReturnApplicationWhenIdExists() {
        Application application = new Application(
                "Payment Service",
                "Handles payment processing"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        Application result =
                applicationService.getApplicationById(1L);

        assertThat(result.getName())
                .isEqualTo("Payment Service");

        verify(applicationRepository)
                .findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenApplicationIdDoesNotExist() {
        when(applicationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.getApplicationById(99L)
        )
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage(
                        "Application with ID 99 was not found."
                );
    }

    @Test
    void shouldReturnApplicationWhenNameExists() {
        Application application = new Application(
                "Payment Service",
                "Handles payment processing"
        );

        when(applicationRepository.findByNameIgnoreCase("Payment Service"))
                .thenReturn(Optional.of(application));

        Application result =
                applicationService.getApplicationByName(
                        "  Payment Service  "
                );

        assertThat(result.getName())
                .isEqualTo("Payment Service");

        verify(applicationRepository)
                .findByNameIgnoreCase("Payment Service");
    }

    @Test
    void shouldThrowExceptionWhenApplicationNameDoesNotExist() {
        when(applicationRepository.findByNameIgnoreCase("Unknown Service"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.getApplicationByName(
                        "Unknown Service"
                )
        )
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage(
                        "Application with name 'Unknown Service' was not found."
                );
    }

    @Test
    void shouldReturnApplicationsPage() {
        Application application1 = new Application(
                "Payment Service",
                "Handles payments"
        );

        Application application2 = new Application(
                "Order Service",
                "Handles orders"
        );

        Page<Application> page = new PageImpl<>(
                java.util.List.of(application1, application2)
        );

        PageRequest pageable = PageRequest.of(0, 2);

        when(applicationRepository.findAll(pageable))
                .thenReturn(page);

        Page<Application> result =
                applicationService.getAllApplications(pageable);

        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getContent().get(0).getName())
                .isEqualTo("Payment Service");

        assertThat(result.getContent().get(1).getName())
                .isEqualTo("Order Service");

        verify(applicationRepository)
                .findAll(pageable);
    }

    @Test
    void shouldUpdateApplicationDetails() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(applicationRepository.existsByNameIgnoreCase("Billing Service"))
                .thenReturn(false);

        Application result =
                applicationService.updateApplication(
                        1L,
                        "  Billing Service  ",
                        "  Handles billing  ",
                        1L
                );

        assertThat(result.getName())
                .isEqualTo("Billing Service");

        assertThat(result.getDescription())
                .isEqualTo("Handles billing");

        verify(applicationRepository)
                .findById(1L);

        verify(applicationRepository)
                .existsByNameIgnoreCase("Billing Service");
    }

    @Test
    void shouldRecordAuditWhenUpdatingApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(applicationRepository.existsByNameIgnoreCase("Billing Service"))
                .thenReturn(false);

        applicationService.updateApplication(
                1L,
                "Billing Service",
                "Handles billing",
                2L
        );

        verify(auditLogService).recordAudit(
                2L,
                "APPLICATION",
                null,
                "APPLICATION_UPDATED",
                null
        );
    }

    @Test
    void shouldRejectDuplicateApplicationNameWhenUpdating() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(applicationRepository.existsByNameIgnoreCase("Billing Service"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        1L,
                        "Billing Service",
                        "Handles billing",
                        1L
                )
        )
                .isInstanceOf(ApplicationAlreadyExistsException.class)
                .hasMessage(
                        "An application with name 'Billing Service' already exists."
                );

        verify(applicationRepository)
                .findById(1L);

        verify(applicationRepository)
                .existsByNameIgnoreCase("Billing Service");

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldAllowUpdateWhenKeepingSameApplicationName() {
        Application application = new Application(
                "Payment Service",
                "Old description"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(applicationRepository.existsByNameIgnoreCase("payment service"))
                .thenReturn(true);

        Application result =
                applicationService.updateApplication(
                        1L,
                        "  payment service  ",
                        "  New description  ",
                        1L
                );

        assertThat(result.getName())
                .isEqualTo("payment service");

        assertThat(result.getDescription())
                .isEqualTo("New description");

        verify(applicationRepository)
                .findById(1L);

        verify(applicationRepository)
                .existsByNameIgnoreCase("payment service");
    }

    @Test
    void shouldRejectNullApplicationIdWhenUpdating() {
        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        null,
                        "Payment Service",
                        "Description",
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectZeroApplicationIdWhenUpdating() {
        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        0L,
                        "Payment Service",
                        "Description",
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNegativeApplicationIdWhenUpdating() {
        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        -1L,
                        "Payment Service",
                        "Description",
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullApplicationNameWhenUpdating() {
        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        1L,
                        null,
                        "Description",
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application name cannot be blank.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectBlankApplicationNameWhenUpdating() {
        assertThatThrownBy(() ->
                applicationService.updateApplication(
                        1L,
                        "   ",
                        "Description",
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application name cannot be blank.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldDeactivateApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        Application result =
                applicationService.deactivateApplication(
                        1L,
                        1L
                );

        assertThat(result.isActive())
                .isFalse();

        verify(applicationRepository)
                .findById(1L);
    }

    @Test
    void shouldRecordAuditWhenDeactivatingApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        applicationService.deactivateApplication(
                1L,
                2L
        );

        verify(auditLogService).recordAudit(
                2L,
                "APPLICATION",
                null,
                "APPLICATION_DEACTIVATED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenDeactivatingMissingApplication() {
        when(applicationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.deactivateApplication(
                        99L,
                        1L
                )
        )
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage(
                        "Application with ID 99 was not found."
                );

        verify(applicationRepository)
                .findById(99L);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullApplicationIdWhenDeactivating() {
        assertThatThrownBy(() ->
                applicationService.deactivateApplication(
                        null,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectZeroApplicationIdWhenDeactivating() {
        assertThatThrownBy(() ->
                applicationService.deactivateApplication(
                        0L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNegativeApplicationIdWhenDeactivating() {
        assertThatThrownBy(() ->
                applicationService.deactivateApplication(
                        -1L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldActivateApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        application.deactivate();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        Application result =
                applicationService.activateApplication(
                        1L,
                        1L
                );

        assertThat(result.isActive())
                .isTrue();

        verify(applicationRepository)
                .findById(1L);
    }

    @Test
    void shouldRecordAuditWhenActivatingApplication() {
        Application application = new Application(
                "Payment Service",
                "Handles payments"
        );

        application.deactivate();

        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        applicationService.activateApplication(
                1L,
                2L
        );

        verify(auditLogService).recordAudit(
                2L,
                "APPLICATION",
                null,
                "APPLICATION_ACTIVATED",
                null
        );
    }

    @Test
    void shouldThrowExceptionWhenActivatingMissingApplication() {
        when(applicationRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                applicationService.activateApplication(
                        99L,
                        1L
                )
        )
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessage(
                        "Application with ID 99 was not found."
                );

        verify(applicationRepository)
                .findById(99L);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNullApplicationIdWhenActivating() {
        assertThatThrownBy(() ->
                applicationService.activateApplication(
                        null,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectZeroApplicationIdWhenActivating() {
        assertThatThrownBy(() ->
                applicationService.activateApplication(
                        0L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void shouldRejectNegativeApplicationIdWhenActivating() {
        assertThatThrownBy(() ->
                applicationService.activateApplication(
                        -1L,
                        1L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");

        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(auditLogService);
    }
}