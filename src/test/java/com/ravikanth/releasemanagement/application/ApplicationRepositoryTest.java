
package com.ravikanth.releasemanagement.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void shouldSaveAndFindApplicationByNameIgnoreCase() {
        String applicationName = "Payment Service " + UUID.randomUUID();

        Application application = new Application(
                applicationName,
                "Handles payment processing"
        );

        applicationRepository.save(application);

        var result = applicationRepository
                .findByNameIgnoreCase(applicationName.toLowerCase(Locale.ROOT));

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(applicationName);
    }
}
