package com.manaswini.sniplink;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{3,32}");
    private static final int MAX_CODES = 100;

    private final LinkService service;
    private final String configuredBaseUrl;

    public LinkController(LinkService service, @Value("${app.base-url:}") String configuredBaseUrl) {
        this.service = service;
        this.configuredBaseUrl = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
    }

    /** POST /api/links  {"url": "...", "alias": "optional", "expiresInDays": 7} */
    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        String base = baseUrl();
        ShortLink link = service.create(request.url(), request.alias(), request.expiresInDays(), hostOf(base));
        return ResponseEntity.status(HttpStatus.CREATED).body(LinkResponse.created(link, base));
    }

    /**
     * GET /api/links?codes=abc,def
     * Returns only the links whose codes are listed, so nobody can browse other people's links.
     */
    @GetMapping
    public List<LinkResponse> list(@RequestParam(name = "codes", defaultValue = "") String codes) {
        String base = baseUrl();
        List<String> wanted = Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(c -> CODE.matcher(c).matches())
                .distinct()
                .limit(MAX_CODES)
                .toList();
        return service.findAll(wanted).stream().map(l -> LinkResponse.from(l, base)).toList();
    }

    @GetMapping("/{code}")
    public LinkResponse get(@PathVariable("code") String code) {
        return LinkResponse.from(service.get(code), baseUrl());
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(@PathVariable("code") String code,
                                       @RequestHeader(name = "X-Delete-Token", required = false) String token) {
        service.delete(code, token);
        return ResponseEntity.noContent().build();
    }

    private String baseUrl() {
        if (!configuredBaseUrl.isEmpty()) {
            return configuredBaseUrl.endsWith("/")
                    ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1) : configuredBaseUrl;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    private static String hostOf(String base) {
        return URI.create(base).getHost();
    }
}
