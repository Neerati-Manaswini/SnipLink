package com.manaswini.sniplink;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;

/**
 * The heart of the app: GET /{code} sends the visitor to the original URL and counts the click.
 * A 302 (temporary) redirect is used on purpose: browsers cache 301s, which would hide repeat clicks.
 * The path only matches plain codes, so /style.css or /app.js still reach the static files.
 */
@Controller
public class RedirectController {

    private final LinkService service;

    public RedirectController(LinkService service) {
        this.service = service;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code) {
        if (code.length() < 3 || code.length() > 32) {
            return home("notfound");
        }
        try {
            String target = service.resolve(code);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
        } catch (LinkNotFoundException e) {
            return home("notfound");
        } catch (LinkExpiredException e) {
            return home("expired");
        }
    }

    private ResponseEntity<Void> home(String reason) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/?error=" + reason)).build();
    }
}
