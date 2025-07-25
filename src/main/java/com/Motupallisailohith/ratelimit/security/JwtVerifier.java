package com.Motupallisailohith.ratelimit.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URL;

/**
 * Verifies incoming JWTs against a remote JWK Set.
 */
public class JwtVerifier {
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwtVerifier(URL jwkSetUrl) throws Exception {
        // Load the JWK set from your Auth server
       JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(jwkSetUrl).build();

        // Only accept RS256-signed tokens
        JWSKeySelector<SecurityContext> keySelector =
            new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);

        // Set up the processor  
        processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);
    }

    /**
     * Validate signature (and standard claims) then return the 'sub' as subject.
     * @throws JwtException on any verification or parsing error
     */
    public JwtClaims verify(String token) throws JwtException {
        try {
            SecurityContext ctx = null;
            JWTClaimsSet claims = processor.process(token, ctx);
            // (Optional) Validate exp, nbf, aud, etc. here
            return new JwtClaims(claims.getSubject());
        }
        catch (BadJWTException | JOSEException e) {
            throw new JwtException("Invalid or expired JWT", e);
        }
        catch (Exception e) {
            throw new JwtException("Failed to process JWT", e);
        }
    }

    /** Holds just the JWT subject for downstream use. */
    public static class JwtClaims {
        private final String subject;
        public JwtClaims(String subject) { this.subject = subject; }
        public String getSubject() { return subject; }
    }
}
