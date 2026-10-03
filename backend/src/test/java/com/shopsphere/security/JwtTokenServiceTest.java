package com.shopsphere.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import com.shopsphere.config.AppProperties;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.RoleName;
import com.shopsphere.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issuedTokenCarriesUserIdAndRoles() {
        var key = new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(), "HmacSHA256");
        var props = new AppProperties(new AppProperties.Security("unused", 30, List.of()), null, null);
        var service = new JwtTokenService(new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key)), props);

        User user = new User("admin@shopsphere.local", "hash", "Admin");
        ReflectionTestUtils.setField(user, "id", 42L);
        user.getRoles().add(new Role(RoleName.ADMIN));
        user.getRoles().add(new Role(RoleName.CUSTOMER));

        String token = service.issue(user).value();
        Jwt jwt = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin@shopsphere.local");
        assertThat(CurrentUser.id(jwt)).isEqualTo(42L);
        assertThat(CurrentUser.isAdmin(jwt)).isTrue();
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "CUSTOMER");
    }
}
