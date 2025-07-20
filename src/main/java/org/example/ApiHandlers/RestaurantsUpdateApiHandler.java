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
 * کلاس RestaurantsUpdateApiHandler برای مدیریت درخواست‌های PUT به endpoint /restaurants/{restaurant_id}
 * این endpoint برای به‌روزرسانی اطلاعات رستوران متعلق به فروشنده استفاده می‌شود
 */
public class RestaurantsUpdateApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory; // فکتوری برای مدیریت سشن‌های Hibernate
    private final Gson gson;

    /**
     * سازنده کلاس که SessionFactory را دریافت می‌کند
     * @param sessionFactory فکتوری برای ارتباط با پایگاه داده
     */
    public RestaurantsUpdateApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // بررسی اینکه متد درخواست PUT باشد
            if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان مجاز به به‌روزرسانی رستوران هستند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length < 3) {
                sendJson(exchange, 400, jsonError("شناسه رستوران نامعتبر است"));
                return;
            }
            Long restaurantId;
            try {
                restaurantId = Long.parseLong(segments[2]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران باید عدد باشد"));
                return;
            }

            // خواندن داده‌های JSON از بدنه درخواست
            Map<String, Object> body;
            try {
                body = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Object>>() {}.getType());
            } catch (Exception e) {
                sendJson(exchange, 400, jsonError("بدنه درخواست نامعتبر است"));
                return;
            }

            // بررسی وجود حداقل یک فیلد برای به‌روزرسانی
            if (body.isEmpty()) {
                sendJson(exchange, 400, jsonError("حداقل یک فیلد برای به‌روزرسانی باید ارائه شود"));
                return;
            }

            // دریافت رستوران از پایگاه داده و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران یافت نشد"));
                    return;
                }
                // بررسی مالکیت رستوران
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به ویرایش این رستوران نیستید"));
                    return;
                }

                // به‌روزرسانی فیلدهای ارائه‌شده
                session.beginTransaction();
                if (body.containsKey("name")) {
                    String name = body.get("name").toString().trim();
                    if (name.isEmpty()) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("نام رستوران نمی‌تواند خالی باشد"));
                        return;
                    }
                    restaurant.setName(name);
                }
                if (body.containsKey("address")) {
                    String address = body.get("address").toString().trim();
                    if (address.isEmpty()) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("آدرس رستوران نمی‌تواند خالی باشد"));
                        return;
                    }
                    restaurant.setAddress(address);
                }
                if (body.containsKey("phone")) {
                    String phone = body.get("phone").toString().trim();
                    if (phone.isEmpty()) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("شماره تلفن رستوران نمی‌تواند خالی باشد"));
                        return;
                    }
                    String phoneRegex = "^(09\\d{9}|۰۹[۰-۹]{9})$";
                    if (!phone.matches(phoneRegex)) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("فرمت شماره تلفن نامعتبر است"));
                        return;
                    }
                    restaurant.setPhone(phone);
                }
                if (body.containsKey("tax_fee")) {
                    try {
                        int taxFee = ((Number) body.get("tax_fee")).intValue();
                        if (taxFee < 0) {
                            session.getTransaction().rollback();
                            sendJson(exchange, 400, jsonError("مقدار tax_fee نمی‌تواند منفی باشد"));
                            return;
                        }
                        restaurant.setTaxFee(taxFee);
                    } catch (ClassCastException | NullPointerException e) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("مقدار tax_fee نامعتبر است"));
                        return;
                    }
                }
                if (body.containsKey("additional_fee")) {
                    try {
                        int additionalFee = ((Number) body.get("additional_fee")).intValue();
                        if (additionalFee < 0) {
                            session.getTransaction().rollback();
                            sendJson(exchange, 400, jsonError("مقدار additional_fee نمی‌تواند منفی باشد"));
                            return;
                        }
                        restaurant.setAdditionalFee(additionalFee);
                    } catch (ClassCastException | NullPointerException e) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("مقدار additional_fee نامعتبر است"));
                        return;
                    }
                }
                if (body.containsKey("logoBase64")) {
                    try {
                        String logoBase64 = body.get("logoBase64").toString();
                        if (!logoBase64.isEmpty()) {
                            restaurant.setLogoBase64(logoBase64);
                        }
                    } catch (IllegalArgumentException e) {
                        session.getTransaction().rollback();
                        sendJson(exchange, 400, jsonError("فرمت logoBase64 نامعتبر است"));
                        return;
                    }
                }

                // ذخیره تغییرات در پایگاه داده
                try {
                    session.merge(restaurant); // استفاده از merge به جای update برای اطمینان از به‌روزرسانی صحیح
                    session.getTransaction().commit();
                } catch (ConstraintViolationException e) {
                    session.getTransaction().rollback();
                    sendJson(exchange, 409, jsonError("شماره تلفن قبلاً ثبت شده است"));
                    return;
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
                    return;
                }

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "رستوران با موفقیت به‌روزرسانی شد",
                        "restaurant_id", restaurant.getId()
                )));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
