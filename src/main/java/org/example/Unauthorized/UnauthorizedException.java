package org.example.Unauthorized;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() {
        super("Unauthorized request");
    }
}
