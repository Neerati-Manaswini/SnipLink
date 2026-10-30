package com.manaswini.sniplink;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank(message = "Enter a URL to shorten.")
        @Size(max = 2048, message = "That URL is too long.")
        String url,

        @Pattern(regexp = "^$|[A-Za-z0-9_-]{3,32}", message = "Alias must be 3-32 characters: letters, numbers, - or _.")
        String alias,

        @Min(value = 1, message = "Expiry must be between 1 and 365 days.")
        @Max(value = 365, message = "Expiry must be between 1 and 365 days.")
        Integer expiresInDays) {
}
