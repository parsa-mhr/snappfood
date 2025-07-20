package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import org.hibernate.exception.ConstraintViolationException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantsApiHandler برای مدیریت درخواست‌های POST به endpoint
 * /restaurants
 * این endpoint برای ایجاد یک رستوران جدید توسط فروشنده استفاده می‌شود
 */
public class RestaurantsApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory; // فکتوری برای مدیریت سشن‌های Hibernate
    private final Gson gson; // شیء Gson برای تبدیل اشیا به JSON

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * 
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public RestaurantsApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    /**
     * متد اصلی برای مدیریت درخواست‌های HTTP
     * 
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
                    default:
                        message = "خطای احراز هویت: " + e.getMessage();
                }
                sendJson(exchange, 401, jsonError(message));
                return;
            }

            // بررسی نقش کاربر (فقط فروشنده مجاز است)
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به ایجاد رستوران هستند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // خواندن داده‌های JSON از بدنه درخواست
            Map<String, Object> body = gson.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                    new TypeToken<Map<String, Object>>() {
                    }.getType());

            // بررسی فیلدهای اجباری
            if (!body.containsKey("name") || !body.containsKey("address") ||
                    !body.containsKey("phone") || !body.containsKey("tax_fee") ||
                    !body.containsKey("additional_fee")) {
                sendJson(exchange, 400,
                        jsonError("فیلدهای اجباری name، address، phone، tax_fee یا additional_fee وجود ندارند"));
                return;
            }

            // ساخت رستوران
            Restaurant restaurant = new Restaurant();
            restaurant.setName(body.get("name").toString());
            restaurant.setAddress(body.get("address").toString());
            restaurant.setPhone(body.get("phone").toString());

            // تنظیم tax_fee و additional_fee
            try {
                restaurant.setTaxFee(((Number) body.get("tax_fee")).intValue());
                restaurant.setAdditionalFee(((Number) body.get("additional_fee")).intValue());
            } catch (ClassCastException | NullPointerException e) {
                sendJson(exchange, 400, jsonError("مقدار tax_fee یا additional_fee نامعتبر است"));
                return;
            }

            // تبدیل logoBase64 به byte[]
            if (body.containsKey("logoBase64")) {
                try {
                    String logoBase64 = body.get("logoBase64").toString();
                    restaurant.setLogoBase64(logoBase64); // تبدیل به byte[] در متد setLogoBase64 انجام می‌شود
                } catch (IllegalArgumentException e) {
                    sendJson(exchange, 400, jsonError("فرمت logoBase64 نامعتبر است"));
                    return;
                }
            }

            restaurant.setSeller((Seller) user);

            // ذخیره رستوران در پایگاه داده
            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();
                session.persist(restaurant);
                session.getTransaction().commit();
            } catch (ConstraintViolationException e) {
                sendJson(exchange, 409, jsonError("شماره تلفن قبلاً ثبت شده است"));
                return;
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
                return;
            }

            // ارسال پاسخ موفقیت‌آمیز
            sendJson(exchange, 201, gson.toJson(Map.of(
                    "message", "رستوران با موفقیت ایجاد شد",
                    "restaurant_id", restaurant.getId())));

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور"));
        }
    }
}
