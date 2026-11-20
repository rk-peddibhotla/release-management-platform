package com.ravikanth.releasemanagement.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User with ID " + id + " was not found.");
    }

    public UserNotFoundException(String email) {
        super("User with email '" + email + "' was not found.");
    }
}