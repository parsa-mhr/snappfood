package org.example.Security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.util.Date;

public class jwtSecurity {
    private static final String SECRET = "my-secret-key"; // حتماً قوی و امن باشه
    private static final long EXPIRATION_TIME = 86400000; // 24 ساعت

    public static String generateToken(Long userId, String role) {
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("role", role)
                .withIssuer("food-app")
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }

    public static DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(SECRET))
                .withIssuer("food-app")
                .build();
        return verifier.verify(token);
    }

    public static Long getUserId(String token) {
        return verifyToken(token).getClaim("userId").asLong();
    }

    public static String getRole(String token) {
        return verifyToken(token).getClaim("role").asString();
    }
}
