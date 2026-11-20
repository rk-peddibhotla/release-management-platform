
package com.ravikanth.releasemanagement.deployment;

public class DeploymentNotFoundException extends RuntimeException {

    public DeploymentNotFoundException(Long id) {
        super("Deployment with ID " + id + " was not found.");
    }
}
