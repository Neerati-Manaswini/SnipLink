package com.manaswini.sniplink;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** JSON returned to the browser. deleteToken is only present in the response to a create request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LinkResponse(String code, String shortUrl, String originalUrl, long clicks,
                           Instant createdAt, Instant expiresAt, Instant lastClickedAt,
                           boolean expired, String deleteToken) {

    public static LinkResponse from(ShortLink link, String baseUrl) {
        return build(link, baseUrl, null);
    }

    public static LinkResponse created(ShortLink link, String baseUrl) {
        return build(link, baseUrl, link.getDeleteToken());
    }

    private static LinkResponse build(ShortLink l, String baseUrl, String token) {
        return new LinkResponse(l.getShortCode(), baseUrl + "/" + l.getShortCode(), l.getOriginalUrl(),
                l.getClickCount(), l.getCreatedAt(), l.getExpiresAt(), l.getLastClickedAt(),
                l.isExpired(Instant.now()), token);
    }
}
