package com.manaswini.sniplink;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlRulesTest {

    @Test
    void addsHttpsWhenSchemeIsMissing() {
        assertEquals("https://example.com/page", UrlRules.normalizeUrl("example.com/page", "localhost"));
    }

    @Test
    void keepsValidUrlsUnchanged() {
        assertEquals("http://example.com/a?b=1", UrlRules.normalizeUrl("  http://example.com/a?b=1 ", "localhost"));
    }

    @Test
    void rejectsDangerousSchemes() {
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("javascript:alert(1)", "localhost"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("javascript://x.com/%0Aalert(1)", "localhost"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("ftp://example.com/file", "localhost"));
    }

    @Test
    void rejectsBlankAndHostsWithoutDot() {
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("   ", "localhost"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("http://intranet/page", "localhost"));
    }

    @Test
    void rejectsLinksBackToThisService() {
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeUrl("https://sniplink.app/abc", "sniplink.app"));
    }

    @Test
    void aliasRules() {
        assertNull(UrlRules.normalizeAlias(null));
        assertNull(UrlRules.normalizeAlias("   "));
        assertEquals("my-link_1", UrlRules.normalizeAlias("my-link_1"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeAlias("ab"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeAlias("has space"));
        assertThrows(IllegalArgumentException.class, () -> UrlRules.normalizeAlias("API"));
    }
}
