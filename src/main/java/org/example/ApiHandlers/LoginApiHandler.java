package org.example.ApiHandlers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.User.User;
import org.example.Unauthorized.UnauthorizedException;
import org.example.Validation.ExistUser;
import org.hibernate.SessionFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس LoginApiHandler برای مدیریت درخواست‌های POST به endpoint /auth/login
 * این endpoint برای ورود کاربر و تولید توکن JWT استفاده می‌شود
 */
public class LoginApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;
    private static final String SECRET = "your_jwt_secret_key"; // کلید مخفی JWT
    private static final String ISSUER = "aut_food"; // صادرکننده توکن
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24 ساعت

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public LoginApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    /**
     * متد اصلی برای مدیریت درخواست‌های HTTP
     * @param exchange شیء HttpExchange برای مدیریت درخواست و پاسخ
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            // بررسی اینکه متد درخواست POST باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                return;
            }

            // بررسی نوع محتوای درخواست
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.contains("application/json")) {
                sendJson(exchange, 415, jsonError("نوع رسانه پشتیبانی‌نشده است"));
                return;
            }

            // خواندن بدنه درخواست
            Map<String, String> body;
            try {
                body = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
            } catch (Exception e) {
                sendJson(exchange, 400, jsonError("بدنه درخواست نامعتبر است"));
                return;
            }

            // بررسی فیلدهای اجباری
            if (!body.containsKey("phone") || !body.containsKey("password")) {
                sendJson(exchange, 400, jsonError("فیلدهای phone و password الزامی هستند"));
                return;
            }

            // اعتبارسنجی کاربر
            ExistUser validator = new ExistUser(sessionFactory);
            User user;
            try {
                user = validator.validate(body.get("phone"), body.get("password"));
            } catch (UnauthorizedException e) {
                String message;
                switch (e.getErrorCode()) {
                    case "MISSING_PHONE":
                        message = "شماره تلفن ارائه نشده است";
                        break;
                    case "MISSING_PASSWORD":
                        message = "رمز عبور ارائه نشده است";
                        break;
                    case "INVALID_PHONE":
                        message = "فرمت شماره تلفن نامعتبر است";
                        break;
                    case "USER_NOT_FOUND":
                        message = "کاربر با این شماره تلفن یافت نشد";
                        break;
                    case "INVALID_PASSWORD":
                        message = "رمز عبور نادرست است";
                        break;
                    case "INACTIVE_USER":
                        message = "حساب کاربر غیرفعال است";
                        break;
                    default:
                        message = "خطای احراز هویت: " + e.getMessage();
                }
                sendJson(exchange, 401, jsonError(message));
                return;
            }

            // تولید توکن JWT
            String token = JWT.create()
                    .withSubject(String.valueOf(user.getId()))
                    .withIssuer(ISSUER)
                    .withExpiresAt(new java.util.Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .sign(Algorithm.HMAC256(SECRET));

            // ارسال پاسخ موفقیت‌آمیز
            sendJson(exchange, 200, gson.toJson(Map.of(
                    "message", "ورود با موفقیت انجام شد",
                    "token", token,
                    "user_id", user.getId() != null ? user.getId().toString() : "null",
                    "role", user.getRole().toString()
            )));

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
        }
    }
}
