package org.example.ApiHandlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.example.ApiHandlers.SendJson.jsonError;
import static org.example.ApiHandlers.SendJson.sendJson;

/**
 * کلاس RestaurantMenuAddItemApiHandler برای مدیریت درخواست‌های PUT به endpoint /restaurants/{id}/menu/{title}
 * این endpoint برای افزودن آیتم موجود به دسته‌بندی منوی رستوران با عنوان مشخص استفاده می‌شود
 */
public class RestaurantMenuAddItemApiHandler implements HttpHandler {
    private final SessionFactory sessionFactory;
    private final Gson gson;

    public RestaurantMenuAddItemApiHandler(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            // لاگ برای عیب‌یابی
            System.out.println("Request Method: " + exchange.getRequestMethod() + ", Path: " + exchange.getRequestURI().getPath());

            // بررسی متد PUT
            if (!exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                sendJson(exchange, 405, jsonError("فقط متد PUT مجاز است"));
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
                sendJson(exchange, 403, jsonError("فقط فروشندگان می‌توانند آیتم به منو اضافه کنند"));
                return;
            }

            // بررسی نوع کاربر (Seller)
            if (!(user instanceof Seller)) {
                sendJson(exchange, 403, jsonError("کاربر باید از نوع فروشنده باشد"));
                return;
            }

            // استخراج شناسه رستوران و عنوان دسته‌بندی از مسیر
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");
            if (segments.length != 5 || !segments[3].equals("menu")) {
                sendJson(exchange, 400, jsonError("مسیر یا شناسه‌ها نامعتبر هستند"));
                return;
            }
            Long restaurantId;
            String categoryTitle;
            try {
                restaurantId = Long.parseLong(segments[2]);
                categoryTitle = segments[4];
                if (categoryTitle.isEmpty()) {
                    throw new IllegalArgumentException("عنوان دسته‌بندی نمی‌تواند خالی باشد");
                }
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, jsonError("شناسه رستوران یا عنوان دسته‌بندی نامعتبر است: " + e.getMessage()));
                return;
            }

            // خواندن داده‌های JSON از بدنه درخواست
            Map<String, Object> body;
            try {
                body = gson.fromJson(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Object>>() {}.getType());
            } catch (Exception e) {
                sendJson(exchange, 400, jsonError("بدنه درخواست JSON نامعتبر است"));
                return;
            }

            // بررسی فیلد اجباری item_id
            if (!body.containsKey("item_id")) {
                sendJson(exchange, 400, jsonError("فیلد اجباری item_id وجود ندارد"));
                return;
            }
            Long itemId;
            try {
                itemId = ((Number) body.get("item_id")).longValue();
            } catch (ClassCastException | NullPointerException e) {
                sendJson(exchange, 400, jsonError("مقدار item_id نامعتبر است"));
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
                    sendJson(exchange, 403, jsonError("شما مجاز به افزودن آیتم به این رستوران نیستید"));
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

                // بررسی اینکه آیتم قبلاً در دسته‌بندی نیست
                Query checkQuery = session.createNativeQuery(
                        "SELECT COUNT(*) FROM menu_items_mapping WHERE menu_id = :menuId AND menu_item_id = :menuItemId");
                checkQuery.setParameter("menuId", menuCategory.getId());
                checkQuery.setParameter("menuItemId", itemId);
                Long existingCount = (Long) checkQuery.getSingleResult();
                if (existingCount != null && existingCount > 0) {
                    sendJson(exchange, 409, jsonError("آیتم با شناسه " + itemId + " قبلاً در دسته‌بندی " + categoryTitle + " وجود دارد"));
                    return;
                }

                // افزودن آیتم به دسته‌بندی
                try {
                    session.beginTransaction();
                    Query mappingQuery = session.createNativeQuery(
                            "INSERT INTO menu_items_mapping (menu_id, menu_item_id) VALUES (:menuId, :menuItemId)");
                    mappingQuery.setParameter("menuId", menuCategory.getId());
                    mappingQuery.setParameter("menuItemId", itemId);
                    mappingQuery.executeUpdate();
                    session.getTransaction().commit();
                } catch (Exception e) {
                    session.getTransaction().rollback();
                    e.printStackTrace();
                    sendJson(exchange, 500, jsonError("خطای داخلی سرور هنگام افزودن آیتم به دسته‌بندی: " + e.getMessage()));
                    return;
                }

                // به‌روزرسانی منوی رستوران
                restaurant.loadMenu(session);

                // ارسال پاسخ موفقیت‌آمیز
                sendJson(exchange, 200, gson.toJson(Map.of(
                        "message", "آیتم با شناسه " + itemId + " با موفقیت به دسته‌بندی " + categoryTitle + " اضافه شد")));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("خطای داخلی سرور: " + e.getMessage()));
        }
    }
}
