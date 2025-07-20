package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.Restaurant.MenuCategory;
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
 * کلاس RestaurantMenuRemoveItemApiHandler برای مدیریت درخواست‌های DELETE به endpoint /restaurants/{id}/menu/{title}/{item_id}
 * این endpoint برای حذف یک آیتم از دسته‌بندی منوی مشخص‌شده در رستوران استفاده می‌شود
 */
public class RestaurantMenuRemoveItemApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantMenuRemoveItemApiHandler(SessionFactory sessionFactory) {
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

            // بررسی نوع محتوای درخواست (اختیاری برای DELETE)
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند آیتم را از منو حذف کنند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران، عنوان دسته‌بندی، و شناسه آیتم از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 6 || !segments[3].equals("menu") || segments[5].isEmpty()) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه‌ها نامعتبر هستند"));
                return;
            }
            Long restaurantId;
            String categoryTitle;
            Long itemId;
            try {
                restaurantId = Long.parseLong(segments[2]);
                categoryTitle = segments[4];
                itemId = Long.parseLong(segments[5]);
                if (categoryTitle.isEmpty()) {
                    throw new IllegalArgumentException("عنوان دسته‌بندی نمی‌تواند خالی باشد");
                }
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران، عنوان دسته‌بندی یا شناسه آیتم نامعتبر است: " + e.getMessage()));
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
                    sendJson(exchange, 403, jsonError("شما مجاز به حذف آیتم از این رستوران نیستید"));
                    return;
                }

                // بررسی وجود دسته‌بندی
                MenuCategory menuCategory = session.createQuery(
                        "FROM MenuCategory m WHERE m.restaurant.id = :restaurantId AND m.title = :title",
                        MenuCategory.class)
                        .setParameter("restaurantId", restaurantId)
                        .setParameter("title", categoryTitle)
                        .uniqueResult();
                if (menuCategory == null) {
                    sendJson(exchange, 404, jsonError("دسته‌بندی با عنوان " + categoryTitle + " در رستوران یافت نشد"));
                    return;
                }

                // بررسی وجود آیتم
                MenuItem item = session.get(MenuItem.class, itemId);
                if (item == null) {
                    sendJson(exchange, 404, jsonError("آیتم با شناسه " + itemId + " یافت نشد"));
                    return;
                }
                if (!item.getRestaurant().getId().equals(restaurantId)) {
                    sendJson(exchange, 403, jsonError("آیتم با شناسه " + itemId + " متعلق به این رستوران نیست"));
                    return;
                }

                // بررسی اینکه آیتم در دسته‌بندی وجود دارد
                Query checkQuery = session.createNativeQuery(
                        "SELECT COUNT(*) FROM menu_items_mapping WHERE menu_id = :menuId AND menu_item_id = :menuItemId");
                checkQuery.setParameter("menuId", menuCategory.getId());
                checkQuery.setParameter("menuItemId", itemId);
                Long existingCount = (Long) checkQuery.getSingleResult();
                if (existingCount == null || existingCount == 0) {
                    sendJson(exchange, 404, jsonError("آیتم با شناسه " + itemId + " در دسته‌بندی " + categoryTitle + " یافت نشد"));
                    return;
                }

                // حذف رابطه آیتم از دسته‌بندی
                try {
                    session.beginTransaction();
                    Query deleteQuery = session.createNativeQuery(
                            "DELETE FROM menu_items_mapping WHERE menu_id = :menuId AND menu_item_id = :menuItemId");
                    deleteQuery.setParameter("menuId", menuCategory.getId());
                    deleteQuery.setParameter("menuItemId", itemId);
                    deleteQuery.executeUpdate();
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور هنگام حذف آیتم از دسته‌بندی: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "آیتم با شناسه " + itemId + " با موفقیت از دسته‌بندی " + categoryTitle + " حذف شد")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
