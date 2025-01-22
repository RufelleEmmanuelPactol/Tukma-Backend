package org.tukma.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtCompilationUnit {

    private static final String SECRET_KEY = "IHFS%I&Je`9g*UL{iwd<r$:.&_z%c<FP^,bH<I[{b&\"6|m%!+%yXcXt(pb4v/O-";
    private static final long expiration_time = 86400000; // this is one day
    private static final byte[] secretKeyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_16);
    private static final SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes);


    public static TransientJwt startTransientState() {return new TransientJwt();}

    public static class TransientJwt {

        protected TransientJwt() {}
        private final JwtBuilder builder = Jwts.builder();
        public void addClaim(String key, String value) {
            builder.claim(key, value);
        }

        public String toString() {

            return builder.issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expiration_time))
                    .signWith(secretKey)
                    .compact();

        }

        public void addUsername(String identifier) {
            addClaim("subject", identifier);
        }
    }


    public static Claims resurrect(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println(e);
            return null;
        }
    }



}
