package com.ravikanth.releasemanagement.changerequest.dto;

import com.ravikanth.releasemanagement.changerequest.ChangeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateChangeRequestRequest(

        @NotBlank(message = "Change request title is required.")
        @Size(max = 200, message = "Change request title must not exceed 200 characters.")
        String title,

        String description,

        @NotNull(message = "Change type is required.")
        ChangeType changeType,

        OffsetDateTime plannedAt

) {
}
