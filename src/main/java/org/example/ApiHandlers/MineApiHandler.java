package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.Restaurant;
import org.example.User.Seller;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس MineApiHandler برای مدیریت درخواست‌های GET به endpoint /restaurants/mine
 * این endpoint لیست رستوران‌های متعلق به فروشنده را برمی‌گرداند
 */
public class MineApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory; // فکتوری برای مدیریت سشن‌های Hibernate
    private final Gson gson; // شیء Gson برای تبدیل اشیا به JSON

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public MineApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
         gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

    }

    /**
     * متد اصلی برای مدیریت درخواست‌های HTTP
     * @param exchange شیء HttpExchange برای مدیریت درخواست و پاسخ
     */
    @Override
    public void handle(HttpExchange exchange) {
        try {
            // بررسی اینکه متد درخواست GET باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, jsonError("متد مجاز نیست"));
                return;
            }

            // بررسی نوع محتوای درخواست (اختیاری برای GET)
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && !contentType.contains("application/json")) {
                sendJson(exchange, 415, jsonError("نوع رسانه پشتیبانی‌نشده است"));
                return;
            }

            // استخراج هدر Authorization
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, jsonError("هدر Authorization نامعتبر است یا فرمت Bearer مورد انتظار نیست"));
                return;
            }

            // استخراج توکن از هدر
            String token = authHeader.substring("Bearer ".length()).trim();
            if (token.isEmpty()) {
                sendJson(exchange, 401, jsonError("توکن ارائه نشده است"));
                return;
            }

            // اعتبارسنجی توکن و دریافت کاربر
            TokenUserValidator validator = new TokenUserValidator(sessionFactory);
            User user;
            try {
                user = validator.validate(token);
                if (user == null) {
                    sendJson(exchange, 401, jsonError("کاربر مرتبط با توکن یافت نشد"));
                    return;
                }
                // بررسی وضعیت کاربر
            } catch (UnauthorizedException e) {
                String message;
                switch (e.getErrorCode()) {
                    case "INVALID_TOKEN":
                        message = "توکن نامعتبر است";
                        break;
                    case "EXPIRED_TOKEN":
                        message = "توکن منقضی شده است";
                        break;
                    case "MISSING_USER":
                        message = "کاربر مرتبط با توکن یافت نشد";
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

            // بررسی نقش کاربر (فقط فروشنده مجاز است)
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به دسترسی هستند"));
                return;
            }

            // دریافت لیست رستوران‌های متعلق به فروشنده از پایگاه داده
            try (Session session = sessionFactory.openSession()) {
                List<Restaurant> restaurants = session.createQuery(
                        "FROM Restaurant WHERE seller.id = :sellerId", Restaurant.class)
                        .setParameter("sellerId", user.getId())
                        .getResultList();

                // بررسی وجود رستوران
                if (restaurants.isEmpty()) {
                    sendJson(exchange, 404, jsonError("رستورانی برای این فروشنده یافت نشد"));
                    return;
                }

                // تبدیل لیست رستوران‌ها به JSON و ارسال پاسخ
                System.out.println(restaurants);
                String jsonResponse = gson.toJson(restaurants);
                sendJson(exchange, 200, jsonResponse);
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
        }
    }
}
