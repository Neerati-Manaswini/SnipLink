package com.manaswini.sniplink;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

@Service
public class LinkService {

    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    private final ShortLinkRepository repository;

    public LinkService(ShortLinkRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ShortLink create(String rawUrl, String rawAlias, Integer expiresInDays, String ownHost) {
        String url = UrlRules.normalizeUrl(rawUrl, ownHost);
        String alias = UrlRules.normalizeAlias(rawAlias);

        ShortLink link = new ShortLink();
        link.setOriginalUrl(url);
        link.setCreatedAt(Instant.now());
        link.setDeleteToken(CodeGenerator.random(20));
        if (expiresInDays != null) {
            link.setExpiresAt(link.getCreatedAt().plus(expiresInDays, ChronoUnit.DAYS));
        }

        if (alias != null) {
            if (repository.existsByShortCode(alias)) {
                throw new AliasTakenException(alias);
            }
            link.setShortCode(alias);
        } else {
            link.setShortCode(uniqueCode());
        }

        try {
            return repository.saveAndFlush(link);
        } catch (DataIntegrityViolationException e) {
            throw new AliasTakenException(link.getShortCode());   // lost a race with another request
        }
    }

    /** Looks up the destination for a short code and records the click. */
    @Transactional
    public String resolve(String code) {
        ShortLink link = repository.findByShortCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        Instant now = Instant.now();
        if (link.isExpired(now)) {
            throw new LinkExpiredException(code);
        }
        repository.registerClick(link.getId(), now);
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortLink get(String code) {
        return repository.findByShortCode(code).orElseThrow(() -> new LinkNotFoundException(code));
    }

    @Transactional(readOnly = true)
    public List<ShortLink> findAll(Collection<String> codes) {
        return codes.isEmpty() ? List.of() : repository.findByShortCodeInOrderByCreatedAtDesc(codes);
    }

    @Transactional
    public void delete(String code, String token) {
        ShortLink link = get(code);
        boolean allowed = token != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), link.getDeleteToken().getBytes(StandardCharsets.UTF_8));
        if (!allowed) {
            throw new DeleteNotAllowedException();
        }
        repository.delete(link);
    }

    private String uniqueCode() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = CodeGenerator.random(CODE_LENGTH);
            if (!repository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique code. Please try again.");
    }
}
