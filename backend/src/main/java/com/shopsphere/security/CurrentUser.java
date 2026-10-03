package com.shopsphere.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Helper to read the authenticated user's id from the JWT "uid" claim. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        if (jwt == null) {
            throw new IllegalArgumentException("Missing authentication token");
        }
        String uid = jwt.getClaimAsString("uid");
        if (uid == null) {
            throw new IllegalArgumentException("Token has no uid claim");
        }
        return Long.valueOf(uid);
    }

    public static boolean isAdmin(Jwt jwt) {
        var roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
