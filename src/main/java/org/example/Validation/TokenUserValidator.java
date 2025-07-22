package org.example.Validation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.example.Unauthorized.UnauthorizedException;
import org.example.User.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * کلاس TokenUserValidator برای اعتبارسنجی توکن JWT و دریافت کاربر مرتبط
 */
public class TokenUserValidator {
    private final SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
    private static final String SECRET = "your_jwt_secret_key"; // کلید مخفی JWT
    private static final String ISSUER = "aut_food"; // صادرکننده توکن

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public TokenUserValidator(SessionFactory sessionFactory) {
    }

    /**
     * اعتبارسنجی توکن و دریافت کاربر
     * @param token توکن JWT
     * @return شیء User در صورت معتبر بودن توکن
     * @throws UnauthorizedException در صورت نامعتبر بودن توکن
     */
    public User validate(String token) throws UnauthorizedException {
        try {
            // اعتبارسنجی توکن JWT
            DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SECRET))
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);

            // استخراج userId از توکن
            String userId = jwt.getSubject();
            if (userId == null) {
                throw new UnauthorizedException("شناسه کاربر در توکن یافت نشد", "MISSING_USER_ID");
            }

            // دریافت کاربر از پایگاه داده
            try (Session session = sessionFactory.openSession()) {
                User user = session.get(User.class, Long.parseLong(userId));
                if (user == null) {
                    throw new UnauthorizedException("کاربر با شناسه " + userId + " یافت نشد", "MISSING_USER");
                }

                return user;
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("شناسه کاربر نامعتبر است", "INVALID_USER_ID");
            }

        } catch (JWTVerificationException e) {
            String errorCode = e.getMessage().contains("expired") ? "EXPIRED_TOKEN" : "INVALID_TOKEN";
            throw new UnauthorizedException("توکن نامعتبر یا منقضی شده است: " + e.getMessage(), errorCode);
        }
    }
}
