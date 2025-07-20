package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuItem;
import org.example.Restaurant.Restaurant;
import org.example.User.Seller;
import org.example.User.User;
import org.example.User.UserRole;
import org.example.Validation.TokenUserValidator;
import org.example.Unauthorized.UnauthorizedException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import jakarta.persistence.Query;

import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantMenuItemDeleteApiHandler برای مدیریت درخواست‌های DELETE به endpoint /restaurants/{id}/item/{item_id}
 * این endpoint برای حذف یک آیتم خاص از منوی رستوران استفاده می‌شود
 */
public class RestaurantMenuItemDeleteApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantMenuItemDeleteApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی متد DELETE
            if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
                sendJson(exchange, 405, jsonError("فقط متد DELETE مجاز است"));
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند آیتم منو را حذف کنند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران و شناسه آیتم از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 5 || !segments[3].equals("item")) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه‌ها نامعتبر هستند"));
                return;
            }
            Long restaurantId;
            Long itemId;
            try {
                restaurantId = Long.parseLong(segments[2]);
                itemId = Long.parseLong(segments[4]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران یا آیتم باید عدد باشد: " + e.getMessage()));
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
                    sendJson(exchange, 403, jsonError("شما مجاز به حذف آیتم این رستوران نیستید"));
                    return;
                }

                // دریافت آیتم و بررسی وجود
                MenuItem item = session.get(MenuItem.class, itemId);
                if (item == null || !item.getRestaurant().getId().equals(restaurantId)) {
                    sendJson(exchange, 404, jsonError("آیتم با شناسه " + itemId + " در رستوران یافت نشد"));
                    return;
                }

                // حذف آیتم از پایگاه داده
                try {
                    session.beginTransaction();
                    // حذف از جدول menu_item_keywords با استفاده از SQL بومی
                    Query query = session.createNativeQuery("DELETE FROM menu_item_keywords WHERE menu_item_id = :itemId");
                    query.setParameter("itemId", itemId);
                    query.executeUpdate();

                    // حذف آیتم
                    session.delete(item);
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور هنگام حذف آیتم: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "آیتم با موفقیت از منوی رستوران حذف شد")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
