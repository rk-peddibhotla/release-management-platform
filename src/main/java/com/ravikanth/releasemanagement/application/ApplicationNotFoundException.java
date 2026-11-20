
package com.ravikanth.releasemanagement.application;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id) {
        super("Application with ID " + id + " was not found.");
    }

    public ApplicationNotFoundException(String name) {
        super("Application with name '" + name + "' was not found.");
    }
}
