
package com.ravikanth.releasemanagement.release;

public class ReleaseAlreadyExistsException extends RuntimeException {

    public ReleaseAlreadyExistsException(
            String version,
            Long environmentId
    ) {
        super(
                "Release version '" + version
                + "' already exists in environment "
                + environmentId + "."
        );
    }
}
