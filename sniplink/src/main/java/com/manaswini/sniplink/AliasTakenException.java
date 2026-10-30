package com.manaswini.sniplink;

public class AliasTakenException extends RuntimeException {
    public AliasTakenException(String alias) {
        super("The alias \"" + alias + "\" is already taken. Try another one.");
    }
}
