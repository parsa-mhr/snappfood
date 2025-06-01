package org.example.Validation;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.example.ApiHandlers.SendJson;
import org.example.Security.TokenBlacklist;
import org.example.Security.jwtSecurity;
import org.example.Unauthorized.UnauthorizedException;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class TokenUserValidator {

    private final SessionFactory sessionFactory;

    public TokenUserValidator(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public User validate(String authHeader) throws UnauthorizedException {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException();
        }

        String token = authHeader.substring(7);
        Long userId;
        String jti = jwtSecurity.getJti(token);
        if (TokenBlacklist.isBlacklisted(jti)) {
            throw new UnauthorizedException();
        }
        try {
            userId = jwtSecurity.getUserId(token); // این متد JWT رو verify می‌کنه
        } catch (TokenExpiredException e) {
            throw new UnauthorizedException();
        } catch (JWTVerificationException e) {
            throw new UnauthorizedException();
        } catch (Exception e) {
            throw new UnauthorizedException();
        }

        try (Session session = sessionFactory.openSession()) {
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new UnauthorizedException();
            }
            return user;
        }
    }
}
