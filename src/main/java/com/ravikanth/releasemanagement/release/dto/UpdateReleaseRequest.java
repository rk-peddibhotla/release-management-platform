package com.ravikanth.releasemanagement.release.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record UpdateReleaseRequest(

        @NotBlank(message = "Release version is required.")
        @Size(max = 50, message = "Release version must not exceed 50 characters.")
        String version,

        String description,

        OffsetDateTime scheduledAt

) {
}