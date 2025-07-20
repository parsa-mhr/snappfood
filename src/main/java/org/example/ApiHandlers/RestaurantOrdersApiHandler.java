package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Details.Cart;
import org.example.Details.OrderStatus;
import org.example.Details.OrderDTO;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantOrdersApiHandler برای مدیریت درخواست‌های GET به endpoint
 * /restaurants/{id}/orders
 * این endpoint برای دریافت لیست سفارشات رستوران با شناسه مشخص استفاده می‌شود
 */
public class RestaurantOrdersApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantOrdersApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی متد GET
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405, jsonError("فقط متد GET مجاز است"));
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند سفارشات رستوران را مشاهده کنند"));
                return;
            }

            // استخراج شناسه رستوران از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 4 || !segments[3].equals("orders")) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه رستوران نامعتبر است"));
                return;
            }
            Long restaurantId;
            try {
                restaurantId = Long.parseLong(segments[2]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران باید عدد باشد: " + e.getMessage()));
                return;
            }

            // دریافت رستوران و بررسی مالکیت
            try (Session session = sessionFactory.openSession()) {
                Restaurant restaurant = session.get(Restaurant.class, restaurantId);
                if (restaurant == null) {
                    sendJson(exchange, 404, jsonError("رستوران با شناسه " + restaurantId + " یافت نشد"));
                    return;
                }
                if (!restaurant.getSeller().getId().equals(user.getId())) {
                    sendJson(exchange, 403, jsonError("شما مجاز به مشاهده سفارشات این رستوران نیستید"));
                    return;
                }

                // دریافت لیست سفارشات
                String hql = "FROM Cart c WHERE c.restaurant.id = :restaurantId";
                List<Cart> orders = session.createQuery(hql, Cart.class)
                        .setParameter("restaurantId", restaurantId)
                        .list();

                // تبدیل سفارشات به DTO
                List<OrderDTO> orderList = orders.stream().map(cart -> new OrderDTO(
                        cart.getId(),
                        cart.getDeliveryAddress(),
                        cart.getCustomer().getId(),
                        cart.getRestaurant().getId(),
                        cart.getCoupon() != null ? cart.getCoupon().getId() : null,
                        cart.getItems().stream().map(MenuItem::getId).collect(Collectors.toList()),
                        cart.getRawPrice(),
                        cart.getTaxFee(),
                        cart.getAdditionalFee(),
                        cart.getCourierFee(),
                        cart.getPayPrice(),
                        cart.getCourier() != null ? cart.getCourier().getId() : null,
                        cart.getStatus() != null ? cart.getStatus().toString() : "UNKNOWN",
                        cart.getCreatedAt(),
                        cart.getUpdatedAt()))
                        .collect(Collectors.toList());

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "لیست سفارشات با موفقیت دریافت شد",
                        "orders", orderList)));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
