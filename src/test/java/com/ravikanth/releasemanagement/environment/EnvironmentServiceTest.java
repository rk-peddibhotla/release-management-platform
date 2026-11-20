package com.ravikanth.releasemanagement.environment;

import com.ravikanth.releasemanagement.application.Application;
import com.ravikanth.releasemanagement.application.ApplicationNotFoundException;
import com.ravikanth.releasemanagement.application.ApplicationRepository;
import com.ravikanth.releasemanagement.common.audit.AuditLogService;


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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EnvironmentService environmentService;

    private Application application;
    private Environment environment;

    @BeforeEach
    void setUp() {
        application = org.mockito.Mockito.mock(Application.class);

        environment = new Environment(
                application,
                "Production",
                EnvironmentType.PRODUCTION
        );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateEnvironment() {
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Production"
                ))
                .thenReturn(false);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Environment result =
                environmentService.createEnvironment(
                        1L,
                        " Production ",
                        EnvironmentType.PRODUCTION,
                        10L
                );

        assertThat(result.getApplication())
                .isSameAs(application);

        assertThat(result.getName())
                .isEqualTo("Production");

        assertThat(result.getEnvironmentType())
                .isEqualTo(EnvironmentType.PRODUCTION);

        assertThat(result.isActive())
                .isTrue();

        verify(environmentRepository)
                .save(any(Environment.class));

        verify(auditLogService).recordAudit(
                eq(10L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_CREATED"),
                eq(null)
        );
    }

    @Test
    void shouldRejectBlankEnvironmentNameWhenCreating() {
        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        1L,
                        "   ",
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment name cannot be blank.");

        verify(environmentRepository, never())
                .save(any());

        verify(auditLogService, never())
                .recordAudit(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectNullEnvironmentNameWhenCreating() {
        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        1L,
                        null,
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment name cannot be blank.");
    }

    @Test
    void shouldRejectInvalidApplicationIdWhenCreating() {
        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        0L,
                        "Production",
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");
    }

    @Test
    void shouldRejectNullApplicationIdWhenCreating() {
        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        null,
                        "Production",
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");
    }

    @Test
    void shouldRejectNullEnvironmentTypeWhenCreating() {
        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        1L,
                        "Production",
                        null,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment type cannot be null.");
    }

    @Test
    void shouldRejectMissingApplicationWhenCreating() {
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        1L,
                        "Production",
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(ApplicationNotFoundException.class);

        verify(environmentRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectDuplicateEnvironmentNameWhenCreating() {
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "production"
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                environmentService.createEnvironment(
                        1L,
                        " production ",
                        EnvironmentType.PRODUCTION,
                        10L
                )
        )
                .isInstanceOf(EnvironmentAlreadyExistsException.class);

        verify(environmentRepository, never())
                .save(any());

        verify(auditLogService, never())
                .recordAudit(any(), any(), any(), any(), any());
    }

    @Test
    void shouldNormalizeEnvironmentNameWhenCreating() {
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(false);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Environment result =
                environmentService.createEnvironment(
                        1L,
                        "  Staging  ",
                        EnvironmentType.STAGING,
                        10L
                );

        assertThat(result.getName())
                .isEqualTo("Staging");
    }

    @Test
    void shouldRecordAuditWhenCreatingEnvironment() {
        when(applicationRepository.findById(1L))
                .thenReturn(Optional.of(application));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Testing"
                ))
                .thenReturn(false);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        environmentService.createEnvironment(
                1L,
                "Testing",
                EnvironmentType.TESTING,
                25L
        );

        verify(auditLogService).recordAudit(
                eq(25L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_CREATED"),
                eq(null)
        );
    }

    // -------------------------------------------------------------------------
    // GET BY ID
    // -------------------------------------------------------------------------

    @Test
    void shouldGetEnvironmentById() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        Environment result =
                environmentService.getEnvironmentById(1L);

        assertThat(result)
                .isSameAs(environment);
    }

    @Test
    void shouldRejectInvalidEnvironmentIdWhenGetting() {
        assertThatThrownBy(() ->
                environmentService.getEnvironmentById(0L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenGetting() {
        assertThatThrownBy(() ->
                environmentService.getEnvironmentById(null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectMissingEnvironmentWhenGetting() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                environmentService.getEnvironmentById(1L)
        )
                .isInstanceOf(EnvironmentNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // GET BY APPLICATION
    // -------------------------------------------------------------------------


       @Test
        void shouldGetEnvironmentsByApplication() {
        PageRequest pageable = PageRequest.of(0, 10);

        Page<Environment> page = new PageImpl<>(
                List.of(environment),
                pageable,
                1
        );

        when(applicationRepository.existsById(1L))
                .thenReturn(true);

        when(environmentRepository.findByApplicationId(1L, pageable))
                .thenReturn(page);

        Page<Environment> result =
                environmentService.getEnvironmentsByApplication(
                        1L,
                        pageable
                );

        assertThat(result.getContent())
                .containsExactly(environment);

        verify(applicationRepository).existsById(1L);

        verify(environmentRepository)
                .findByApplicationId(1L, pageable);

        verify(environmentRepository, never())
                .findAll(pageable);
        }


    @Test
    void shouldRejectInvalidApplicationIdWhenGettingEnvironments() {
        PageRequest pageable =
                PageRequest.of(0, 10);

        assertThatThrownBy(() ->
                environmentService.getEnvironmentsByApplication(
                        0L,
                        pageable
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Application ID must be positive.");
    }


        @Test
        void shouldRejectMissingApplicationWhenGettingEnvironments() {
        PageRequest pageable = PageRequest.of(0, 10);

        when(applicationRepository.existsById(1L))
                .thenReturn(false);

        assertThatThrownBy(() ->
                environmentService.getEnvironmentsByApplication(
                        1L,
                        pageable
                )
        )
                .isInstanceOf(ApplicationNotFoundException.class);

        verify(applicationRepository).existsById(1L);

        verify(environmentRepository, never())
                .findByApplicationId(1L, pageable);

        verify(environmentRepository, never())
                .findAll(pageable);
        }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Test
    void shouldUpdateEnvironmentDetails() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(false);

        Environment result =
                environmentService.updateEnvironment(
                        1L,
                        " Staging ",
                        EnvironmentType.STAGING,
                        10L
                );

        assertThat(result.getName())
                .isEqualTo("Staging");

        assertThat(result.getEnvironmentType())
                .isEqualTo(EnvironmentType.STAGING);

        verify(auditLogService).recordAudit(
                eq(10L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_UPDATED"),
                eq(null)
        );
    }

    @Test
    void shouldRejectInvalidEnvironmentIdWhenUpdating() {
        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        0L,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenUpdating() {
        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        null,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectBlankEnvironmentNameWhenUpdating() {
        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        1L,
                        "   ",
                        EnvironmentType.STAGING,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment name cannot be blank.");
    }

    @Test
    void shouldRejectNullEnvironmentTypeWhenUpdating() {
        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        1L,
                        "Staging",
                        null,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment type cannot be null.");
    }

    @Test
    void shouldRejectMissingEnvironmentWhenUpdating() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        1L,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                )
        )
                .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void shouldRejectDuplicateEnvironmentNameWhenUpdating() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(true);

        assertThatThrownBy(() ->
                environmentService.updateEnvironment(
                        1L,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                )
        )
                .isInstanceOf(EnvironmentAlreadyExistsException.class);

        verify(auditLogService, never())
                .recordAudit(any(), any(), any(), any(), any());
    }

    @Test
    void shouldAllowUpdateWhenKeepingSameEnvironmentName() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "production"
                ))
                .thenReturn(true);

        Environment result =
                environmentService.updateEnvironment(
                        1L,
                        "production",
                        EnvironmentType.PRODUCTION,
                        10L
                );

        assertThat(result.getName())
                .isEqualTo("production");

        verify(auditLogService).recordAudit(
                eq(10L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_UPDATED"),
                eq(null)
        );
    }

    @Test
    void shouldRecordAuditWhenUpdatingEnvironment() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(false);

        environmentService.updateEnvironment(
                1L,
                "Staging",
                EnvironmentType.STAGING,
                20L
        );

        verify(auditLogService).recordAudit(
                eq(20L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_UPDATED"),
                eq(null)
        );
    }

    @Test
    void shouldNormalizeEnvironmentNameWhenUpdating() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Testing"
                ))
                .thenReturn(false);

        Environment result =
                environmentService.updateEnvironment(
                        1L,
                        "  Testing  ",
                        EnvironmentType.TESTING,
                        10L
                );

        assertThat(result.getName())
                .isEqualTo("Testing");
    }

    @Test
    void shouldKeepEnvironmentActiveAfterUpdate() {
        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(false);

        Environment result =
                environmentService.updateEnvironment(
                        1L,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                );

        assertThat(result.isActive())
                .isTrue();
    }

    @Test
    void shouldKeepEnvironmentInactiveAfterUpdate() {
        environment.deactivate();

        when(application.getId())
                .thenReturn(1L);

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        when(environmentRepository
                .existsByApplicationIdAndNameIgnoreCase(
                        1L,
                        "Staging"
                ))
                .thenReturn(false);

        Environment result =
                environmentService.updateEnvironment(
                        1L,
                        "Staging",
                        EnvironmentType.STAGING,
                        10L
                );

        assertThat(result.isActive())
                .isFalse();
    }

    // -------------------------------------------------------------------------
    // DEACTIVATE
    // -------------------------------------------------------------------------

    @Test
    void shouldDeactivateEnvironment() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        Environment result =
                environmentService.deactivateEnvironment(
                        1L,
                        10L
                );

        assertThat(result.isActive())
                .isFalse();

        verify(auditLogService).recordAudit(
                eq(10L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_DEACTIVATED"),
                eq(null)
        );
    }

    @Test
    void shouldRejectInvalidEnvironmentIdWhenDeactivating() {
        assertThatThrownBy(() ->
                environmentService.deactivateEnvironment(
                        0L,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenDeactivating() {
        assertThatThrownBy(() ->
                environmentService.deactivateEnvironment(
                        null,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectMissingEnvironmentWhenDeactivating() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                environmentService.deactivateEnvironment(
                        1L,
                        10L
                )
        )
                .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void shouldRecordAuditWhenDeactivatingEnvironment() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        environmentService.deactivateEnvironment(
                1L,
                20L
        );

        verify(auditLogService).recordAudit(
                eq(20L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_DEACTIVATED"),
                eq(null)
        );
    }

    // -------------------------------------------------------------------------
    // ACTIVATE
    // -------------------------------------------------------------------------

    @Test
    void shouldActivateEnvironment() {
        environment.deactivate();

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        Environment result =
                environmentService.activateEnvironment(
                        1L,
                        10L
                );

        assertThat(result.isActive())
                .isTrue();

        verify(auditLogService).recordAudit(
                eq(10L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_ACTIVATED"),
                eq(null)
        );
    }

    @Test
    void shouldRejectInvalidEnvironmentIdWhenActivating() {
        assertThatThrownBy(() ->
                environmentService.activateEnvironment(
                        0L,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectNullEnvironmentIdWhenActivating() {
        assertThatThrownBy(() ->
                environmentService.activateEnvironment(
                        null,
                        10L
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Environment ID must be positive.");
    }

    @Test
    void shouldRejectMissingEnvironmentWhenActivating() {
        when(environmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                environmentService.activateEnvironment(
                        1L,
                        10L
                )
        )
                .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void shouldRecordAuditWhenActivatingEnvironment() {
        environment.deactivate();

        when(environmentRepository.findById(1L))
                .thenReturn(Optional.of(environment));

        environmentService.activateEnvironment(
                1L,
                20L
        );

        verify(auditLogService).recordAudit(
                eq(20L),
                eq("ENVIRONMENT"),
                eq(null),
                eq("ENVIRONMENT_ACTIVATED"),
                eq(null)
        );
    }








}
