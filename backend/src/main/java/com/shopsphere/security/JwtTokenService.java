package com.shopsphere.security;

import com.shopsphere.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Issues signed JWT access tokens. Claims: sub=email, uid=user id, roles=[ADMIN|CUSTOMER]. */
@Service
public class JwtTokenService {

    public static final String ISSUER = "shopsphere-ai";

    private final JwtEncoder encoder;
    private final Duration ttl;

    public JwtTokenService(JwtEncoder encoder, com.shopsphere.config.AppProperties props) {
        this.encoder = encoder;
        this.ttl = Duration.ofMinutes(props.security().jwtExpirationMinutes() > 0 ? props.security().jwtExpirationMinutes() : 60);
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        List<String> roles = user.getRoles().stream().map(r -> r.getName().name()).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
