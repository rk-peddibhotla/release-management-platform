
package com.ravikanth.releasemanagement.environment;

public class EnvironmentNotFoundException extends RuntimeException {

    public EnvironmentNotFoundException(Long id) {
        super("Environment with ID " + id + " was not found.");
    }

    public EnvironmentNotFoundException(
            String name,
            Long applicationId
    ) {
        super(
            "Environment with name '" + name
            + "' was not found for application ID " + applicationId + "."
        );
    }
}
