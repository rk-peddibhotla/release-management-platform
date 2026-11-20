
package com.ravikanth.releasemanagement.release;

public class ReleaseNotFoundException extends RuntimeException {

    public ReleaseNotFoundException(Long id) {
        super("Release with ID " + id + " was not found.");
    }
}
