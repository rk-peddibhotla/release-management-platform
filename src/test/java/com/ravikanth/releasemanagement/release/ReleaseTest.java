
package com.ravikanth.releasemanagement.release;

import com.ravikanth.releasemanagement.environment.Environment;
import com.ravikanth.releasemanagement.user.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ReleaseTest {

    @Test
    void shouldCompleteReleaseAfterSuccessfulDeployment() {
        Environment environment = mock(Environment.class);
        User user = mock(User.class);

        Release release = new Release(
                environment,
                "1.0.0",
                "Initial release",
                null,
                user
        );

        release.start();

        assertEquals(
                ReleaseStatus.IN_PROGRESS,
                release.getStatus()
        );

        release.complete();

        assertEquals(
                ReleaseStatus.COMPLETED,
                release.getStatus()
        );
    }
}
