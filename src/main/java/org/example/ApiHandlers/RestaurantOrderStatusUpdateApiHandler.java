package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Restaurant.Restaurant;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantOrderStatusUpdateApiHandler برای مدیریت درخواست‌های PATCH به endpoint /restaurants/orders/{order_id}
 * این endpoint برای تغییر وضعیت یک سفارش خاص در رستوران استفاده می‌شود
 */
public class RestaurantOrderStatusUpdateApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantOrderStatusUpdateApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // بررسی متد PATCH
            if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
                sendJson(exchange, 405, jsonError("فقط متد PATCH مجاز است"));
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

            // استخراج توکن
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
                    case "INACTIVE_USER":
                        message = "حساب کاربر غیرفعال است";
                        break;
                    default:
                        message = "خطای احراز هویت: " + e.getMessage();
                }
                sendJson(exchange, 401, jsonError(message));
                return;
            }

            // بررسی نقش کاربر
            if (user.getRole() == null || user.getRole() != UserRole.seller) {
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند وضعیت سفارش را تغییر دهند"));
                return;
            }

            // استخراج شناسه سفارش از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 4 || !segments[2].equals("orders")) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه سفارش نامعتبر است"));
                return;
            }
            Long orderId;
            try {
                orderId = Long.parseLong(segments[3]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه سفارش باید عدد باشد: " + e.getMessage()));
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

            // بررسی فیلد status
            if (!body.containsKey("status")) {
                sendJson(exchange, 400, jsonError("فیلد status الزامی است"));
                return;
            }

            // اعتبارسنجی مقدار status
            OrderStatus newStatus;
            try {
                newStatus = OrderStatus.valueOf(body.get("status").toUpperCase());
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, jsonError("وضعیت نامعتبر است: " + body.get("status")));
                return;
            }

            // دریافت و به‌روزرسانی سفارش
            try (Session session = sessionFactory.openSession()) {
                Cart order = session.get(Cart.class, orderId);
                if (order == null) {
                    sendJson(exchange, 404, jsonError("سفارش با شناسه " + orderId + " یافت نشد"));
                    return;
                }

                // بررسی مالکیت رستوران
                Restaurant restaurant = order.getRestaurant();
                if (restaurant == null || !restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به تغییر وضعیت این سفارش نیستید"));
                    return;
                }

                // به‌روزرسانی وضعیت و زمان
                Transaction transaction = session.beginTransaction();
                try {
                    order.setStatus(newStatus);
                    order.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    session.update(order);
                    transaction.commit();
                } catch (Exception e) {
                    transaction.rollback();
                    sendJson(exchange, 500, jsonError("خطا در به‌روزرسانی وضعیت سفارش: " + e.getMessage()));
                    return;
                }

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "وضعیت سفارش با موفقیت به " + newStatus + " تغییر کرد",
                        "order_id", orderId)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
