package org.tukma.auth.exceptions;

public class NullUserException extends RuntimeException {

    public NullUserException(String message) {
        super("Current token does not have a valid user, with explicit error message: " + message);
    }
}
