package com.manaswini.sniplink;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<ShortLink> findByShortCodeInOrderByCreatedAtDesc(Collection<String> codes);

    /** Atomic increment in the database, so simultaneous clicks are never lost. */
    @Modifying
    @Query("update ShortLink l set l.clickCount = l.clickCount + 1, l.lastClickedAt = :now where l.id = :id")
    int registerClick(@Param("id") Long id, @Param("now") Instant now);
}
