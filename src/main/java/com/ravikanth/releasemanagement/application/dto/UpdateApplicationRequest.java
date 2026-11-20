package com.ravikanth.releasemanagement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateApplicationRequest(
        @NotBlank(message = "Application name is required.")
        @Size(max = 150, message = "Application name must not exceed 150 characters.")
        String name,

        @Size(max = 10000, message = "Description must not exceed 10000 characters.")
        String description
) {}