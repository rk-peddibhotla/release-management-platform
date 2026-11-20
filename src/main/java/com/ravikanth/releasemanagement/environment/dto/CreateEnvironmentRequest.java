
package com.ravikanth.releasemanagement.environment.dto;

import com.ravikanth.releasemanagement.environment.EnvironmentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEnvironmentRequest(

        @NotBlank(message = "Environment name is required.")
        @Size(max = 50, message = "Environment name must not exceed 50 characters.")
        String name,

        @NotNull(message = "Environment type is required.")
        EnvironmentType environmentType

) {}
