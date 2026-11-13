package com.medico.security;

import com.medico.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long hours;

    public JwtService(@Value("${medico.jwt-secret}") String secret, @Value("${medico.jwt-hours}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.hours = hours;
    }

    public record Claims(Long id, Role role) {}

    public String create(Long userId, Role role) {
        return Jwts.builder().subject(String.valueOf(userId)).claim("role", role.name()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + hours * 3_600_000L))
                .signWith(key).compact();
    }

    public Claims parse(String token) {
        var p = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String r = p.get("role", String.class);
        return new Claims(Long.valueOf(p.getSubject()), r == null ? Role.PATIENT : Role.valueOf(r));
    }
}
