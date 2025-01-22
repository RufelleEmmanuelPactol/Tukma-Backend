package org.tukma.config;
import io.jsonwebtoken.*;
public class JwtUnit {
    private final JwtBuilder transcientState;


    public JwtUnit(String username) {
        transcientState = Jwts.builder();
    }
}
