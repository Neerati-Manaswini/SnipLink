package com.manaswini.sniplink;

public class LinkExpiredException extends RuntimeException {
    public LinkExpiredException(String code) {
        super("The link \"" + code + "\" has expired.");
    }
}
