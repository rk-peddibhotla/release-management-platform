
package com.ravikanth.releasemanagement.environment;

public class EnvironmentAlreadyExistsException extends RuntimeException {

    public EnvironmentAlreadyExistsException(
            String name,
            Long applicationId
    ) {
        super(
            "An environment with name '" + name
            + "' already exists for application ID " + applicationId + "."
        );
    }
}
