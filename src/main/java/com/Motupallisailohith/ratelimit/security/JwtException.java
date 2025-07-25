package com.Motupallisailohith.ratelimit.security;

/**
 * Indicates a problem verifying or parsing a JWT.
 */
public class JwtException extends Exception {
    public JwtException(String message) {
        super(message);
    }
    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
