
package com.ravikanth.releasemanagement.application;

public class ApplicationAlreadyExistsException extends RuntimeException {

    public ApplicationAlreadyExistsException(String name) {
        super("An application with name '" + name + "' already exists.");
    }
}
