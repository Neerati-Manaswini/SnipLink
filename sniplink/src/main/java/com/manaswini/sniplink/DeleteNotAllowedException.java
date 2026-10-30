package com.manaswini.sniplink;

public class DeleteNotAllowedException extends RuntimeException {
    public DeleteNotAllowedException() {
        super("You can only delete links you created in this browser.");
    }
}
