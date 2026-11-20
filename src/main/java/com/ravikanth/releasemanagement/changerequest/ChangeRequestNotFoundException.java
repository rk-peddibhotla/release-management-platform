
package com.ravikanth.releasemanagement.changerequest;

public class ChangeRequestNotFoundException extends RuntimeException {

    public ChangeRequestNotFoundException(Long id) {
        super("Change request with ID " + id + " was not found.");
    }
}
